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

package org.predict4all.nlp.ngram;

import org.predict4all.nlp.Tag;
import org.predict4all.nlp.io.TokenFileInputStream;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.ngram.debug.NGramDebugger;
import org.predict4all.nlp.ngram.dictionary.TrainingNGramDictionary;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.trainer.TrainerTask;
import org.predict4all.nlp.trainer.configuration.NGramPruningMethod;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.utils.progressindicator.LoggingProgressIndicator;
import org.predict4all.nlp.utils.progressindicator.ProgressIndicator;
import org.predict4all.nlp.words.WordDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Use this generator to train an ngram model.<br>
 * It will load texts from a {@link TrainingCorpus} and generate a ngram file that could be later opened with a {@link org.predict4all.nlp.ngram.dictionary.StaticNGramTrieDictionary}
 *
 * @author Mathieu THEBAUD
 */
public class NGramDictionaryGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NGramDictionaryGenerator.class);

    private final LanguageModel languageModel;
    private final TrainingConfiguration trainingConfiguration;
    private final int maxOrder;
    private final WordDictionary wordDictionary;

    private String debugPrefix;
    private NGramDebugger ngramDebugBeforePruning, ngramDebugAfterPruning;

    public NGramDictionaryGenerator(LanguageModel languageModel, TrainingConfiguration trainingConfiguration, WordDictionary wordDictionary) {
        this.wordDictionary = wordDictionary;
        this.languageModel = languageModel;
        this.trainingConfiguration = trainingConfiguration;
        this.maxOrder = this.trainingConfiguration.getNgramOrder();
    }

    // PUBLIC API
    //========================================================================
    public Map<Integer, Pair<Integer, Integer>> executeNGramTraining(TrainingCorpus corpus, File ngramOutputFile,
                                                                     Consumer<List<TrainerTask>> blockingTaskExecutor) throws IOException {
        long startInsert = System.currentTimeMillis();

        // Init
        corpus.initStep(TrainingStep.NGRAM_DICTIONARY);
        ProgressIndicator progressIndicator = new LoggingProgressIndicator("Generating ngrams",
                corpus.getTotalCountFor(TrainingStep.NGRAM_DICTIONARY));

        // Detect and count every ngrams
        ConcurrentHashMap<NGramKey, LongAdder> ngramCounts = new ConcurrentHashMap<>(8_000_000, 0.9f, Runtime.getRuntime().availableProcessors());
        blockingTaskExecutor.accept(corpus.getDocuments(TrainingStep.NGRAM_DICTIONARY).stream()
                .map(d -> new TrainingNGramDictionaryTask(progressIndicator, d, ngramCounts)).collect(Collectors.toList()));
        LOGGER.info("NGram generation tasks finished in {} s, will now insert to dictionary", (System.currentTimeMillis() - startInsert) / 1000.0);

        // Insert detect ngrams into dictionary
        TrainingNGramDictionary ngramDictionary = TrainingNGramDictionary.create(this.maxOrder);
        ProgressIndicator progressIndicatorInsert = new LoggingProgressIndicator("Generating ngram dictionary", ngramCounts.size());
        ngramCounts.forEach((ngram, sum) -> {
            ngramCounts.remove(ngram);
            ngramDictionary.putAndIncrementBy(ngram.ngram, sum.intValue());
            progressIndicatorInsert.increment();
        });
        LOGGER.info("Every ngram inserted in dictionary, will now compact");
        ngramDictionary.compact();
        LOGGER.info("Dictionary compacted");

        // Prune dictionary if needed and compute probabilities
        ngramDictionary.countNGrams();

        if (this.ngramDebugBeforePruning != null) {
            this.ngramDebugBeforePruning.debug(wordDictionary,
                    Predict4AllUtils.isNotBlank(debugPrefix) ? ngramDictionary.getNodeForPrefix(Arrays.stream(this.debugPrefix.split(" "))
                            .filter(s -> Predict4AllUtils.isNotBlank(s)).mapToInt(w -> wordDictionary.getWordId(w)).toArray(), 0)
                            : ngramDictionary.getRoot());
        }

        // NO PRUNING
        if (this.trainingConfiguration.getPruningMethod() == NGramPruningMethod.NONE) {
            ngramDictionary.updateProbabilities(ngramDictionary.computeD(this.trainingConfiguration));
        }
        // OTHER PRUNING METHOD
        else {
            switch (this.trainingConfiguration.getPruningMethod()) {
                case WEIGHTED_DIFFERENCE_RAW_PROB:
                case WEIGHTED_DIFFERENCE_FULL_PROB:
                    ngramDictionary.pruneNGramsWeightedDifference(this.trainingConfiguration.getNgramPruningWeightedDifferenceThreshold(),
                            this.trainingConfiguration, this.trainingConfiguration.getPruningMethod());
                    break;
                case RAW_COUNT:
                    ngramDictionary.pruneNGramsCount(this.trainingConfiguration.getNgramPruningCountThreshold(), this.trainingConfiguration);
                    break;
                case ORDER_COUNT:
                    ngramDictionary.pruneNGramsOrderCount(this.trainingConfiguration.getNgramPruningOrderCountThresholds(), this.trainingConfiguration);
                    break;
                default:
                    throw new IllegalArgumentException("Pruning method " + this.trainingConfiguration.getPruningMethod() + " not implemented");
            }
        }

        if (this.ngramDebugAfterPruning != null) {
            this.ngramDebugAfterPruning.debug(wordDictionary,
                    Predict4AllUtils.isNotBlank(debugPrefix) ? ngramDictionary.getNodeForPrefix(Arrays.stream(this.debugPrefix.split(" "))
                            .filter(s -> Predict4AllUtils.isNotBlank(s)).mapToInt(w -> wordDictionary.getWordId(w)).toArray(), 0)
                            : ngramDictionary.getRoot());
        }

        if (ngramOutputFile != null) {
            ngramDictionary.saveDictionary(ngramOutputFile);
        }
        return ngramDictionary.countNGrams();
    }
    //========================================================================

    // DEBUG
    //========================================================================
    public NGramDebugger getNgramDebugBeforePruning() {
        return ngramDebugBeforePruning;
    }

    public void setNgramDebugBeforePruning(NGramDebugger ngramDebugBeforePruning) {
        this.ngramDebugBeforePruning = ngramDebugBeforePruning;
    }

    public NGramDebugger getNgramDebugAfterPruning() {
        return ngramDebugAfterPruning;
    }

    public void setNgramDebugAfterPruning(NGramDebugger ngramDebugAfterPruning) {
        this.ngramDebugAfterPruning = ngramDebugAfterPruning;
    }

    public String getDebugPrefix() {
        return debugPrefix;
    }

    public void setDebugPrefix(String debugPrefix) {
        this.debugPrefix = debugPrefix;
    }
    //========================================================================

    // TASKS
    //========================================================================
    private class TrainingNGramDictionaryTask extends TrainerTask {
        private final ConcurrentHashMap<NGramKey, LongAdder> ngramCounts;

        public TrainingNGramDictionaryTask(ProgressIndicator progressIndicator, AbstractTrainingDocument document,
                                           ConcurrentHashMap<NGramKey, LongAdder> ngramCounts) {
            super(progressIndicator, document);
            this.ngramCounts = ngramCounts;
        }

        @Override
        public void run() throws Exception {
            try (TokenFileInputStream tfis = new TokenFileInputStream(document.getInputFile())) {
                List<int[]> ngrams = generateNGramForDocument(tfis, null, progressIndicator, false);
                for (int[] ngram : ngrams) {
                    ngramCounts.computeIfAbsent(new NGramKey(ngram), k -> new LongAdder()).increment();
                }
            }
        }

    }
    //========================================================================

    // TRAINING UTILS
    // ========================================================================
    private List<int[]> generateNGramForDocument(final TokenProvider tokenFis, final File outputFile, final ProgressIndicator progressIndicator,
                                                 final boolean userTraining) throws IOException {

        List<int[]> generatedNGrams = new ArrayList<>(500);

        Token currentSentenceStart = null;

        // Create and count all ngrams
        Token token = tokenFis.getNext();
        while (token != null) {
            // End of sentence is detected (or end of the tokens)
            if ((token.isSeparator() && token.getSeparator().isSentenceSeparator()) || token.getNext(tokenFis) == null) {
                generateNGramForSentence(generatedNGrams, tokenFis, currentSentenceStart, token);
                currentSentenceStart = token.getNext(tokenFis);
            }
            // Current sentence continues...
            else {
                currentSentenceStart = Predict4AllUtils.getOrDefault(currentSentenceStart, token);
            }
            token = token.getNext(tokenFis);
            progressIndicator.increment();
        }

        return generatedNGrams;
    }

    private void generateNGramForSentence(List<int[]> ngramsList, TokenProvider tokenProvider, Token start, Token end) throws IOException {
        Token current = start;

        // First, create a list that contains all the tokens without separators
        List<Token> tokens = new ArrayList<>();
        while (current != null) {
            if (!current.isSeparator()) {
                tokens.add(current);
            }
            if (current == end) {
                break;
            }
            current = current.getNext(tokenProvider);
        }
        if (!tokens.isEmpty()) {
            // For each tokens, create ngrams for wanted order (n) (start at index -1 because we want START tag)
            for (int i = -1; i < tokens.size(); i++) {
                for (int order = 1; order <= maxOrder; order++) {
                    List<int[]> ngrams = generateNGramsFromToken(order, tokens, i);
                    if (ngrams != null) {
                        ngramsList.addAll(ngrams);
                    }
                }
            }
        }
    }

    private List<int[]> generateNGramsFromToken(final int order, final List<Token> tokens, final int startIndex) {
        List<int[]> ngrams = new ArrayList<>(order);
        for (int j = 0; j < order; j++) {
            if (j + startIndex >= 0) {
                if (j + startIndex < tokens.size()) {
                    Token token = tokens.get(j + startIndex);
                    // Get word id : get Term id if used in prediction, or just word id if not
                    int wordId = token.getWordId(wordDictionary);
                    // Future ngram contains unknown word : not useful to continue
                    if (wordId == Tag.UNKNOWN.getId()) {
                        return null;
                    } else {
                        if (j == 0) {
                            ngrams.add(createArrayAndSetFirst(order, wordId));
                        } else {
                            for (int[] ngram : ngrams) {
                                ngram[j] = wordId;
                            }
                        }
                    }
                }
                // Not enough tokens to create ngram of the wanted order
                else {
                    return null;
                }
            } else {
                createOrAddNGramTag(order, ngrams, j, Tag.START);
            }
        }
        return ngrams;

    }

    private int[] createArrayAndSetFirst(int length, int wordId) {
        int[] array = new int[length];
        array[0] = wordId;
        return array;
    }

    private void createOrAddNGramTag(int order, List<int[]> ngrams, int insertIndex, Tag tag) {
        if (insertIndex == 0) {
            ngrams.add(createArrayAndSetFirst(order, (int) tag.getId()));
        } else {
            for (int[] ngram : ngrams) {
                ngram[insertIndex] = tag.getId();
            }
        }
    }
    // ========================================================================
}
