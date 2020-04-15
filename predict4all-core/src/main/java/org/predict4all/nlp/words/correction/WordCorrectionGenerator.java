/*
 * Copyright 2020 - Mathieu THEBAUD
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.predict4all.nlp.words.correction;

import org.predict4all.nlp.Separator;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.ngram.dictionary.AbstractNGramDictionary;
import org.predict4all.nlp.ngram.trie.AbstractNGramTrieNode;
import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.utils.BiIntegerKey;
import org.predict4all.nlp.utils.DaemonThreadFactory;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.words.NextWord;
import org.predict4all.nlp.words.WordDictionary;
import org.predict4all.nlp.words.model.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.predict4all.nlp.utils.Predict4AllUtils.getOrDefault;

/**
 * Generate possible correction from a input text and tokens.<br>
 * Correction are based on rule ({@link CorrectionRule}) and generation is done using a thread pool.<br>
 * Result correction could be unique word or double word (for example, the error might be a merged word)
 *
 * @author Mathieu THEBAUD
 */
public class WordCorrectionGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordCorrectionGenerator.class);
    private final static int[] EMPTY_ARRAY = new int[]{};
    private static final int MAX_PART_COUNT = 2;
    private static final double CORRECTION_DOUBLE_MIN_LEFT_FREQUENCY = 1E-6;

    private final PredictionParameter predictionParameter;
    private final WordDictionary wordDictionary;
    private final AbstractNGramDictionary<? extends AbstractNGramTrieNode<?>> staticNgramDictionary;
    private final ExecutorService threadPool;
    private final Map<CorrectionRuleNode, Map<CachedPrecomputedCorrectionRule, CachedPrecomputedCorrectionRule>> cachedRules;

    public WordCorrectionGenerator(WordDictionary wordDictionary, AbstractNGramDictionary<? extends AbstractNGramTrieNode<?>> staticNgramDictionary,
                                   PredictionParameter predictionParameter) {
        super();
        this.wordDictionary = wordDictionary;
        this.staticNgramDictionary = staticNgramDictionary;
        this.predictionParameter = predictionParameter;
        this.cachedRules = new HashMap<>();
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DaemonThreadFactory());
        LOGGER.info("Correction thread pool initialized with {} core", Runtime.getRuntime().availableProcessors());
    }

    public void dispose() {
        this.threadPool.shutdown();
    }

    private Set<CachedPrecomputedCorrectionRule> getCachedPrecomputedRuleFor(PredictionParameter parameters) {
        CorrectionRuleNode correctionRulesRoot = parameters.getCorrectionRulesRoot();
        Map<CachedPrecomputedCorrectionRule, CachedPrecomputedCorrectionRule> initialized = this.cachedRules.get(correctionRulesRoot);
        if (initialized == null) {
            initialized = new HashMap<>();
            if (correctionRulesRoot != null) {
                this.navigateRuleTreeAndGenerateCachedPrecomputedRules(initialized, correctionRulesRoot);
                LOGGER.info("Initialized {} correction rule from rule tree", initialized.size());
            }
            this.cachedRules.put(correctionRulesRoot, initialized);
        }
        return initialized.keySet();
    }

    private void navigateRuleTreeAndGenerateCachedPrecomputedRules(Map<CachedPrecomputedCorrectionRule, CachedPrecomputedCorrectionRule> rules, CorrectionRuleNode node) {
        if (node.isEnabled()) {
            if (node.getType() == CorrectionRuleNodeType.LEAF) {
                this.buildAndAddTo(node.getCorrectionRule(), predictionParameter, rules, (previous, duplicated) -> {
                    LOGGER.debug("Duplicated rule found : {} vs {}", previous.getSrcBuilder(), duplicated.getSrcBuilder());
                });
            } else {
                for (CorrectionRuleNode child : node.getChildren()) {
                    navigateRuleTreeAndGenerateCachedPrecomputedRules(rules, child);
                }
            }
        }
    }

    public void addCorrectionsFor(final String prefixRaw, final Map<BiIntegerKey, NextWord> words, Set<Integer> wordIdsToExclude) {
        Collection<CachedPrecomputedCorrectionRule> rules = getCachedPrecomputedRuleFor(predictionParameter);

        if (!rules.isEmpty() && (predictionParameter.getMinCountToProvideCorrection() <= 0 || Predict4AllUtils.length(prefixRaw) >= predictionParameter.getMinCountToProvideCorrection())) {

            long start = System.currentTimeMillis();

            final String prefix = prefixRaw.toLowerCase();

            Map<String, GeneratingCorrectionI> result = new ConcurrentHashMap<>(10_000);
            LOGGER.info("Estimated number of different parallel tasks : {}", (prefix.length() + 1) * rules.size());
            generateCorrection(0, 0.0, 0.0, rules, 0, new HashSet<>(100), result,
                    new SimpleGeneratingCorrection(prefix, this.predictionParameter.isEnableDebugInformation()));
            AtomicInteger countWordSimple = new AtomicInteger(0), countWordDouble = new AtomicInteger(0);
            Map<BiIntegerKey, Boolean> biResultChecking = new HashMap<>();

            result.forEach((_key, rCorrection) -> {
                // Unique word
                if (rCorrection.getPartCount() == 1) {
                    SortedMap<String, Word> wordsFromDict = wordDictionary.getExactWordsWithPrefixExist(rCorrection.getEndPart(0));
                    if (wordsFromDict != null && !wordsFromDict.isEmpty()) {
                        wordsFromDict.forEach((pf, word) -> {
                            if (word.isValidToBePredicted(predictionParameter) && !wordIdsToExclude.contains(word.getID())) {
                                BiIntegerKey key = BiIntegerKey.of(word.getID());
                                NextWord previous = words.get(key);
                                if (previous == null || rCorrection.getEndFactor() > previous.getFactor()) {
                                    NextWord nextWord = NextWord.createUnique(word.getID(), rCorrection.getEndFactor(), true,
                                            this.predictionParameter.isEnableDebugInformation()
                                                    ? new StringBuilder("Correction sans séparation\n").append(rCorrection.getDebugInformation())
                                                    : null);
                                    words.put(key, nextWord);
                                    if (previous == null)
                                        countWordSimple.incrementAndGet();
                                }
                            }
                        });
                    }
                }
                // Double word
                else {
                    Word exactMatchesForLeft = wordDictionary.getWord(rCorrection.getEndPart(0));
                    if (exactMatchesForLeft.getNGramTag() != Tag.UNKNOWN && exactMatchesForLeft.isValidToBePredicted(predictionParameter)) {
                        SortedMap<String, Word> exactMatchesForRight = wordDictionary.getExactWordsWithPrefixExist(rCorrection.getEndPart(1));
                        if (!exactMatchesForRight.isEmpty()) {
                            exactMatchesForRight.forEach((pf, wordR) -> {
                                if (wordR.isValidToBePredicted(predictionParameter) && !wordIdsToExclude.contains(wordR.getID())) {
                                    BiIntegerKey key = BiIntegerKey.of(exactMatchesForLeft.getID(), wordR.getID());
                                    NextWord previous = words.get(key);
                                    // Check bigram presence
                                    if (previous == null || rCorrection.getEndFactor() > previous.getFactor()) {
                                        if (biResultChecking.computeIfAbsent(key, key_ -> {
                                            return this.staticNgramDictionary
                                                    .getNodeForPrefix(new int[]{exactMatchesForLeft.getID(), wordR.getID()}, 0) != null;
                                        })) {
                                            NextWord nextWord = NextWord
                                                    .createDouble(exactMatchesForLeft.getID(), wordR.getID(), rCorrection.getEndSeparator(0),
                                                            rCorrection.getEndFactor(), true,
                                                            predictionParameter.isEnableDebugInformation()
                                                                    ? new StringBuilder("Correction avec séparation\n")
                                                                    .append(rCorrection.getDebugInformation())
                                                                    : null);
                                            words.put(key, nextWord);
                                            if (previous == null)
                                                countWordDouble.incrementAndGet();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            });
            LOGGER.info("Generate {} different modified for prefix \"{}\" in {} ms, simple = {}, double = {}", result.size(), prefix,
                    System.currentTimeMillis() - start, countWordSimple.get(), countWordDouble.get());
        }
    }

    private void generateCorrection(int ruleCount, double totalCost, double totalFactor, Collection<CachedPrecomputedCorrectionRule> rules,
                                    final int previousCorrectionIndex, HashSet<CachedPrecomputedCorrectionRule> rulestoIgnore, Map<String, GeneratingCorrectionI> result,
                                    GeneratingCorrectionI currentCorrection) {

        List<Callable<Void>> tasks = ruleCount == 0 ? new ArrayList<>() : null;

        // Try to apply correction from the current prefix part without any correction (from start index to end) - if possible
        if (totalCost < predictionParameter.getCorrectionMaxCost() || predictionParameter.getCorrectionMaxCost() < 0) {
            for (int correctionStartIndex = previousCorrectionIndex; correctionStartIndex <= currentCorrection
                    .getCurrentPartLength(); correctionStartIndex++) {
                for (CachedPrecomputedCorrectionRule rule : rules) {
                    if (canApplyRule(correctionStartIndex, currentCorrection, rulestoIgnore, rule)) {
                        // Execute in a ThreadPool or directly
                        if (tasks != null) {
                            final int correctionStartIndexF = correctionStartIndex;
                            tasks.add(() -> {
                                applyCorrectionRule(ruleCount, totalCost, totalFactor, rules, correctionStartIndexF, rule, rulestoIgnore, result,
                                        currentCorrection);
                                return null;
                            });
                        } else {
                            applyCorrectionRule(ruleCount, totalCost, totalFactor, rules, correctionStartIndex, rule, rulestoIgnore, result,
                                    currentCorrection);
                        }
                    }
                }
            }
        }

        // Execute in ThreadPool
        if (tasks != null) {
            try {
                threadPool.invokeAll(tasks);
            } catch (Exception e) {
                LOGGER.error("Couldn't execute correction generation tasks", e);
            }
        }

        // Result are stored with the final factor - always keep the lowest factor
        if (ruleCount > 0) {
            currentCorrection.endCorrection((totalFactor / (ruleCount * 1.0)) / totalCost);
            String key = currentCorrection.getKey();
            GeneratingCorrectionI previousCr = result.get(key);
            if (previousCr == null || previousCr.getEndFactor() < currentCorrection.getEndFactor()) {
                result.put(key, currentCorrection);
                if (predictionParameter.isEnableDebugInformation()) {
                    currentCorrection.getDebugInformation().append("coût = ").append(totalCost).append(", poids = ").append(totalFactor)
                            .append(", poids final = ").append(currentCorrection.getEndFactor());
                }
            }
        }
    }

    private boolean canApplyRule(int correctionStartIndex, final GeneratingCorrectionI currentCorrection, HashSet<CachedPrecomputedCorrectionRule> rulestoIgnore,
                                 CachedPrecomputedCorrectionRule rule) {
        return
                //
                (rule.getMaxIndexFromStart() < 0 || correctionStartIndex < rule.getMaxIndexFromStart()) && //
                        // Min index from start
                        (rule.getMinIndexFromStart() < 0 || correctionStartIndex >= rule.getMinIndexFromStart()) && //
                        // Min index from end : start index is applied from end, but based on error length
                        (rule.getMinIndexFromEnd() < 0 || correctionStartIndex >= currentCorrection.getCurrentPartLength()
                                - (rule.getMinIndexFromEnd() * Math.max(1, rule.getError().length())))
                        && //
                        !rulestoIgnore.contains(rule);
    }

    private void applyCorrectionRule(int ruleCount, double totalCost, double totalFactor, Collection<CachedPrecomputedCorrectionRule> rules, int startIndexInclusive,
                                     CachedPrecomputedCorrectionRule rule, HashSet<CachedPrecomputedCorrectionRule> toIgnore, Map<String, GeneratingCorrectionI> result,
                                     final GeneratingCorrectionI currentCorrection) {
        // Worth to try applying rule ?
        if (predictionParameter.getCorrectionMaxCost() < 0 || rule.getCost() + totalCost < predictionParameter.getCorrectionMaxCost()) {

            // Search for error in current part
            String error = rule.getError();
            int errorStartIndex = currentCorrection.indexOfInCurrentPart(error, startIndexInclusive);

            // Check there is an error and that error is located in a correct place
            if (errorStartIndex != -1 && (rule.getMaxIndexFromEnd() < 0 || errorStartIndex < currentCorrection.getCurrentPartLength()
                    - rule.getMaxIndexFromEnd() * Math.max(1, rule.getError().length()))) {

                // Check and generate a new prefix (if possible)
                Pair<StringBuilder, StringBuilder> prefixLeftAndRight = checkPrefixForRuleAndGenerateParts(currentCorrection, errorStartIndex, rule);
                if (prefixLeftAndRight != null) {

                    // Get the rest of the word (part where other corrections will be applied)
                    String partOnRight = currentCorrection.substringInCurrentPart(errorStartIndex + error.length(),
                            currentCorrection.getCurrentPartLength());

                    // Generate the result (one or many words)
                    GeneratingCorrectionI nCurrentCorrection = currentCorrection.clone();
                    nCurrentCorrection.changeCurrentPartTo(prefixLeftAndRight.getLeft());
                    boolean correctionGeneratedAnotherWords = prefixLeftAndRight.getRight() != null
                            && (partOnRight.length() + prefixLeftAndRight.getRight().length()) > 0;
                    if (correctionGeneratedAnotherWords) {
                        nCurrentCorrection.currentPartFinishedAndNewPartStarted(rule.getReplacementSeparator(),
                                prefixLeftAndRight.getRight().append(partOnRight));
                    } else {
                        nCurrentCorrection.appendToCurrentPart(partOnRight);
                    }
                    nCurrentCorrection.appendDebugInformationForCurrentPart(currentCorrection.getCurrentPart(), prefixLeftAndRight, rule);

                    // Determine the next index where other rules will be applied.
                    // If another word were generated : apply from the word start (index = 0)
                    // Else shift to the next index (even if the error removed chars)
                    int nextIndexWithReplacement = correctionGeneratedAnotherWords ? 0
                            : errorStartIndex + (error.length() > 0 ? rule.getReplacement().length() : 1);

                    // Try to apply again correction rules
                    generateCorrection(ruleCount + 1, totalCost + rule.getCost(), totalFactor + rule.getFactor(), rules, nextIndexWithReplacement,
                            error.length() <= 0 ? duplicateAndAdd(toIgnore, rule) : toIgnore, result, nCurrentCorrection);
                }
            }
        }
    }

    /**
     * This will apply the correction on the given current correction and check if the generated prefix is correct.<br>
     * Will only check the left part of the prefix, because on the right part, new corrections could be applied later.<br>
     * This can generate one or two word depending if the correction rule add a separator.
     *
     * @param currentCorrection
     * @param errorStartIndex
     * @param rule
     * @return
     */
    private Pair<StringBuilder, StringBuilder> checkPrefixForRuleAndGenerateParts(GeneratingCorrectionI currentCorrection, int errorStartIndex,
                                                                                  CachedPrecomputedCorrectionRule rule) {

        // Get the part before the error
        String currentPartSubString = currentCorrection.substringInCurrentPart(0, errorStartIndex);

        // Rule will create two parts (left and right = two words) - apply only if allowed
        if (rule.getReplacementSeparator() != null) {
            if (currentCorrection.getPartCount() < MAX_PART_COUNT) {

                // Left part - previous prefix part and error correction (+separator if needed)
                StringBuilder leftPart = new StringBuilder(currentPartSubString).append(rule.getReplacementLeftPart());
                boolean separatorAddedOnLeft = false;
                if (this.isSeparatorShouldBeKeptWithLeftPart(rule.getReplacementSeparator())) {
                    leftPart.append(rule.getReplacementSeparator().getOfficialChar());
                }

                // Check left part - word should exist and be frequent enough
                Word wordLeft = wordDictionary.getWord(leftPart.toString());
                if (wordLeft != null && wordLeft.getNGramTag() != Tag.UNKNOWN && isNotDoubleSeparatorOn(wordLeft, rule)) {
                    if (isProbabilityOfLeftWordCorrect(wordLeft)) {
                        // Check that there is words on right starting with the prefix
                        // ONLY with the right part in error correction because other correction rules will be applied later on the word rest
                        return rule.getReplacementRightPart().length() <= 1
                                || !wordDictionary.getExactWordsWithPrefixExist(rule.getReplacementRightPart()).isEmpty()
                                ? Pair.of(leftPart, new StringBuilder(rule.getReplacementRightPart()))
                                : null;
                    }
                }
            }
        }

        // Rule will create only one word - check that the left part with error corrected exists
        else {
            StringBuilder leftPart = new StringBuilder(currentPartSubString).append(rule.getReplacement());
            return leftPart.length() <= 1 || !wordDictionary.getExactWordsWithPrefixExist(leftPart.toString()).isEmpty() ? Pair.of(leftPart, null)
                    : null;
        }
        return null;
    }

    // TOOLS
    //========================================================================

    /**
     * Check that the word on left will not create a double separator situation.<br>
     * If the word on left already ends with a separator, there shouldn't be another separator added.
     *
     * @param wordLeft
     * @param rule
     * @return
     */
    private boolean isNotDoubleSeparatorOn(Word wordLeft, CachedPrecomputedCorrectionRule rule) {
        return rule.getReplacementSeparator() == Separator.APOSTROPHE || !wordLeft.getWord().endsWith(Separator.APOSTROPHE.getOfficialCharString());
    }

    /**
     * Duplicate the given set, add the element only on the duplicated version, and return it.
     *
     * @param toIgnore
     * @param rule
     * @return
     */
    private HashSet<CachedPrecomputedCorrectionRule> duplicateAndAdd(HashSet<CachedPrecomputedCorrectionRule> toIgnore, CachedPrecomputedCorrectionRule rule) {
        HashSet<CachedPrecomputedCorrectionRule> dupl = new HashSet<>(toIgnore);
        dupl.add(rule);
        return dupl;
    }

    private boolean isSeparatorShouldBeKeptWithLeftPart(Separator separator) {
        return separator == Separator.APOSTROPHE;
    }

    private boolean isProbabilityOfLeftWordCorrect(Word word) {
        //TODO : cache prob in IntDouble map
        double leftWordProb = this.staticNgramDictionary.getProbability(EMPTY_ARRAY, 0, 0, word.getID());
        return leftWordProb > CORRECTION_DOUBLE_MIN_LEFT_FREQUENCY;
    }
    //========================================================================

    // RULE GENERATION
    //========================================================================
    private void buildAndAddTo(CorrectionRule correctionRule, PredictionParameter predictionParameter, Map<CachedPrecomputedCorrectionRule, CachedPrecomputedCorrectionRule> rulesCollection) {
        buildAndAddTo(correctionRule, predictionParameter, rulesCollection, null);
    }

    private void buildAndAddTo(CorrectionRule correctionRule, PredictionParameter predictionParameter, Map<CachedPrecomputedCorrectionRule, CachedPrecomputedCorrectionRule> rulesCollection,
                               BiConsumer<CachedPrecomputedCorrectionRule, CachedPrecomputedCorrectionRule> duplicatedRuleConsumer) {
        Consumer<CachedPrecomputedCorrectionRule> adder = rule -> {
            CachedPrecomputedCorrectionRule previous = rulesCollection.get(rule);
            if (previous == null) {
                rulesCollection.put(rule, rule);
            } else if (duplicatedRuleConsumer != null) {
                duplicatedRuleConsumer.accept(previous, rule);
            }
        };
        for (String error : correctionRule.getErrors()) {
            for (String replacement : correctionRule.getReplacements()) {
                if (!error.equals(replacement)) {
                    CachedPrecomputedCorrectionRule rule = new CachedPrecomputedCorrectionRule(correctionRule, error, replacement,
                            getOrDefault(correctionRule.getFactor(), predictionParameter.getCorrectionDefaultFactor()),
                            getOrDefault(correctionRule.getCost(), predictionParameter.getCorrectionDefaultCost()),
                            getOrDefault(correctionRule.getMaxIndexFromStart(), -1),
                            getOrDefault(correctionRule.getMinIndexFromStart(), -1),
                            getOrDefault(correctionRule.getMaxIndexFromEnd(), -1),
                            getOrDefault(correctionRule.getMinIndexFromEnd(), -1));
                    adder.accept(rule);
                    if (correctionRule.isBidirectional()) {
                        adder.accept(rule.opposite());
                    }
                }
            }
        }
    }
    //========================================================================


}
