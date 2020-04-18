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

package org.predict4all.nlp.prediction;

import org.predict4all.nlp.Separator;
import org.predict4all.nlp.ngram.NGramWordPredictorUtils;
import org.predict4all.nlp.ngram.dictionary.AbstractNGramDictionary;
import org.predict4all.nlp.ngram.trie.AbstractNGramTrieNode;
import org.predict4all.nlp.ngram.trie.DynamicNGramTrieNode;
import org.predict4all.nlp.parser.Tokenizer;
import org.predict4all.nlp.parser.matcher.TokenConverter;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.prediction.model.AbstractPredictionToCompute;
import org.predict4all.nlp.prediction.model.DoublePredictionToCompute;
import org.predict4all.nlp.prediction.model.UniquePredictionToCompute;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.utils.BiIntegerKey;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.utils.Triple;
import org.predict4all.nlp.words.NextWord;
import org.predict4all.nlp.words.WordDictionary;
import org.predict4all.nlp.words.WordPrefixDetected;
import org.predict4all.nlp.words.WordPrefixDetector;
import org.predict4all.nlp.words.correction.WordCorrectionGenerator;
import org.predict4all.nlp.words.model.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main entry point of PREDICT4ALL API.<br>
 * Instance of {@link WordPredictor} can predict next words, current word ends and even current corrections.<br>
 * The predictor mainly relies on two item : ngram dictionary and word dictionary to search for word and existing sequences.<br>
 * Additionally, a dynamic model can be provided to combine both static ngrams originated from an already learned generic model and a dynamic model specific to user, profil, application...<br>
 * The predictor configuration is located in {@link PredictionParameter} : the instance provided on {@link WordPredictor} creation can be later modified.
 * Note that modifications are not guaranteed to be used as the predictor could cache some configurations.<br>
 * Disposing the {@link WordPredictor#dispose()} will guarantee that there is no memory leak related to word prediction.<br>
 * <strong>WordPredictor is not Thread safe : you should synchronize manually your call to the instance</strong> : a good implementation pattern is to use a debounce function to call prediction.
 *
 * @author Mathieu THEBAUD
 */
public class WordPredictor {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordPredictor.class);

    private static final DecimalFormat DEBUG_DF = new DecimalFormat("0.0000");
    private static final int WANTED_COUNT_FACTOR = 3;
    private static final Set<Integer> EMPTY_INT_SET = new HashSet<Integer>(1) {
        @Override
        public boolean contains(Object o) {
            return false;
        }
    };
    private static final int NGRAM_MAX_LAST_TEXT_LENGTH = 70;

    private static final TrainingConfiguration configuration = TrainingConfiguration.defaultConfiguration();

    private final NGramWordPredictorUtils ngramWordPredictorUtils;
    private final WordPrefixDetector wordPrefixDetector;

    private final Tokenizer tokenizer;
    private final TokenConverter termConverter;
    private final WordCorrectionGenerator wordCorrectionGenerator;

    private final WordDictionary wordDictionary;
    private final PredictionParameter predictionParameter;
    private final AbstractNGramDictionary<? extends AbstractNGramTrieNode<?>> staticNgramDictionary;
    private final AbstractNGramDictionary<? extends DynamicNGramTrieNode> dynamicNGramDictionary;

    // CONSTRUCTOR
    //========================================================================

    /**
     * Create a predictor with dynamic model
     *
     * @param predictionParameter    prediction parameters to use
     * @param wordDictionary         word dictionary to use
     * @param staticNgramDictionary  main ngram dictionary to use, should be already opened
     * @param dynamicNGramDictionary dynamic ngram dictionary to use. This ngram dictionary would also be the one modified by {@link #trainDynamicModel(String, boolean)}
     */
    public WordPredictor(PredictionParameter predictionParameter, WordDictionary wordDictionary,
                         AbstractNGramDictionary<? extends AbstractNGramTrieNode<?>> staticNgramDictionary,
                         AbstractNGramDictionary<? extends DynamicNGramTrieNode> dynamicNGramDictionary) {
        super();
        this.predictionParameter = Predict4AllUtils.checkNull(predictionParameter, "Word predictor requires PredictionParameter to work");
        this.wordDictionary = Predict4AllUtils.checkNull(wordDictionary, "Word predictor requires WordDictionary to work");
        this.staticNgramDictionary = Predict4AllUtils.checkNull(staticNgramDictionary, "Word predictor requires a static ngram dictionary to work");
        this.dynamicNGramDictionary = dynamicNGramDictionary;
        this.wordCorrectionGenerator = new WordCorrectionGenerator(wordDictionary, staticNgramDictionary, this.predictionParameter);
        this.ngramWordPredictorUtils = new NGramWordPredictorUtils(wordDictionary, predictionParameter);
        this.wordPrefixDetector = new WordPrefixDetector(wordDictionary, wordCorrectionGenerator, predictionParameter);
        this.termConverter = new TokenConverter(this.predictionParameter.getLanguageModel().getTokenMatchersForNGram());
        this.tokenizer = new Tokenizer(this.predictionParameter.getLanguageModel());
    }

    /**
     * Create a predictor without dynamic model
     *
     * @param predictionParameter   prediction parameters to use
     * @param wordDictionary        word dictionary to use
     * @param staticNgramDictionary main ngram dictionary to use, should be already opened
     */
    public WordPredictor(PredictionParameter predictionParameter, WordDictionary wordDictionary,
                         AbstractNGramDictionary<? extends AbstractNGramTrieNode<?>> staticNgramDictionary) {
        this(predictionParameter, wordDictionary, staticNgramDictionary, null);
    }
    //========================================================================

    // PREDICT PUBLIC API
    //========================================================================

    /**
     * See {@link #predict(String, String, int, Set)}
     *
     * @param textBeforeCaret text before caret
     * @param textAfterCaret  text after the caret (can be null)
     * @param wantedCount     wanted prediction result count (typically the number of prediction displayed in UI)
     * @return the prediction result for the given input parameter. The result might be empty but never null
     * @throws Exception if an error occurs while predicting next words (most probable : {@link IOException} while reading from dictionary)
     */
    public WordPredictionResult predict(String textBeforeCaret, String textAfterCaret, int wantedCount) throws Exception {
        return _predict(textBeforeCaret, textAfterCaret, wantedCount, null);
    }

    /**
     * See {@link #predict(String, String, int, Set)}
     *
     * @param textBeforeCaret text before caret
     * @param wantedCount     wanted prediction result count (typically the number of prediction displayed in UI)
     * @return the prediction result for the given input parameter. The result might be empty but never null
     * @throws Exception if an error occurs while predicting next words (most probable : {@link IOException} while reading from dictionary)
     */
    public WordPredictionResult predict(String textBeforeCaret, int wantedCount) throws Exception {
        return _predict(textBeforeCaret, null, wantedCount, null);
    }


    /**
     * See {@link #predict(String, String, int, Set)} called with default wanted size (5)
     *
     * @param textBeforeCaret text before caret
     * @return the prediction result for the given input parameter. The result might be empty but never null
     * @throws Exception if an error occurs while predicting next words (most probable : {@link IOException} while reading from dictionary)
     */
    public WordPredictionResult predict(String textBeforeCaret) throws Exception {
        return _predict(textBeforeCaret, null, 5, null);
    }

    /**
     * See {@link #predict(String, String, int, Set)} called with default wanted size (5)
     *
     * @param textBeforeCaret  text before caret
     * @param wordIdsToExclude word IDs to be excluded from the result list (can be null)
     * @return the prediction result for the given input parameter. The result might be empty but never null
     * @throws Exception if an error occurs while predicting next words (most probable : {@link IOException} while reading from dictionary)
     */
    public WordPredictionResult predict(String textBeforeCaret, Set<Integer> wordIdsToExclude) throws Exception {
        return _predict(textBeforeCaret, null, 5, wordIdsToExclude);
    }

    /**
     * Try to predict the best <code>wantedCount</code> words for a given <code>textBeforeCaret</code>.<br>
     * Will go through ngram dictionary (and eventually through dynamic dictionary) to find the best matching word.<br>
     * This may predict next word if the predictor detect that there is no current word (e.g. the given  <code>textBeforeCaret</code> ends with a space)
     * or it may also try to complete the current word end.<br>
     * If a word is already started and {@link PredictionParameter#isEnableWordCorrection()}, the given {@link WordPrediction} could be correction of the already started word.<br>
     * <code>textAfterCaret</code> is not useful to predict next words, but it used to determine {@link WordPredictionResult#getNextCharCountToRemove()}<br>
     * <code><strong>wantedCount</strong></code> should be given according to real needs : prediction computing will be longer if wantedCount it too high.<br>
     * <code>wordIdsToExclude</code> can be useful to exclude already seen words if the current word is the same from the last prediction call.
     *
     * @param textBeforeCaret  text before caret
     * @param textAfterCaret   text after the caret (can be null)
     * @param wantedCount      wanted prediction result count (typically the number of prediction displayed in UI)
     * @param wordIdsToExclude word IDs to be excluded from the result list (can be null)
     * @return the prediction result for the given input parameter. The result might be empty but never null
     * @throws Exception if an error occurs while predicting next words (most probable : {@link IOException} while reading from dictionary)
     */
    public WordPredictionResult predict(String textBeforeCaret, String textAfterCaret, int wantedCount, Set<Integer> wordIdsToExclude) throws Exception {
        return _predict(textBeforeCaret, textAfterCaret, wantedCount, wordIdsToExclude);
    }
    //========================================================================

    // TRAIN PUBLIC API
    //========================================================================

    /**
     * Train the current dynamic model (if present and enabled).<br>
     * This allow the dynamic model to integrate the given text as validated input for next predictions.<br>
     * This will also update the "seen" word ({@link Word#getUsageCount()} and add non existing word to dictionary if {@link PredictionParameter#isAddNewWordsEnabled()} is true.<br>
     * <strong>Always train your dynamic model with correct input</strong><br>
     * This method doesn't save the dynamic model, it should be then saved with {@link org.predict4all.nlp.ngram.dictionary.DynamicNGramDictionary#saveDictionary(File)}
     *
     * @param rawText            the training text
     * @param ignoreLastSentence set to true if the last sentence in the given input should be ignored.<br>
     *                           This should typically be set to true when using this method to do inline training (training while typing)
     * @throws IOException if error occurs while training
     */
    public void trainDynamicModel(String rawText, boolean ignoreLastSentence) throws IOException {
        _trainDynamicModel(rawText, ignoreLastSentence);
    }

    /**
     * See {@link #trainDynamicModel(String, boolean)} called with <code>ignoreLastSentence</code> to false
     *
     * @param rawText the training text
     * @throws IOException if error occurs while training
     */
    public void trainDynamicModel(String rawText) throws IOException {
        _trainDynamicModel(rawText, false);
    }
    //========================================================================

    // GETTER
    //========================================================================

    /**
     * @return prediction parameter used by this predictor (never null)
     */
    public PredictionParameter getPredictionParameter() {
        return predictionParameter;
    }

    /**
     * @return word dictionary used by this predictor (never null)
     */
    public WordDictionary getWordDictionary() {
        return wordDictionary;
    }

    /**
     * @return static ngram dictionary used by this predictor (never null)
     */
    public AbstractNGramDictionary<? extends AbstractNGramTrieNode<?>> getStaticNgramDictionary() {
        return staticNgramDictionary;
    }

    /**
     * @return dynamic ngram dictionary used by this predictor (can be null if dynamic model is not used)
     */
    public AbstractNGramDictionary<? extends DynamicNGramTrieNode> getDynamicNGramDictionary() {
        return dynamicNGramDictionary;
    }
    //========================================================================

    // DISPOSE
    //========================================================================

    /**
     * Try to close/dispose word predictor resources.<br>
     * This will close ngram dictionary and dynamic dictionary (equivalent to call {@link AbstractNGramDictionary#close()} on both)
     * and also the {@link WordCorrectionGenerator} if it was enabled.
     */
    public void dispose() {
        try {
            this.staticNgramDictionary.close();
        } catch (Exception e) {
            LOGGER.error("NGram static dictionary closing failed", e);
        }
        try {
            if (this.dynamicNGramDictionary != null)
                this.dynamicNGramDictionary.close();
        } catch (Exception e) {
            LOGGER.error("NGram dynamic dictionary closing failed", e);
        }
        try {
            if (this.wordCorrectionGenerator != null)
                this.wordCorrectionGenerator.dispose();
        } catch (Exception e) {
            LOGGER.error("NGram dynamic dictionary closing failed", e);
        }
        LOGGER.info("Word predictor disposed");
    }
    //========================================================================

    // MAIN PREDICTION METHOD
    //========================================================================

    /**
     * See {@link #predict(String, String, int, Set)} documentation
     *
     * @param textBeforeCaret
     * @param textAfterCaret
     * @param wantedCount
     * @param wordIdsToExclude
     * @return
     * @throws Exception
     */
    private WordPredictionResult _predict(String textBeforeCaret, String textAfterCaret, int wantedCount, Set<Integer> wordIdsToExclude) throws Exception {
        if (predictionParameter.isEnableDebugInformation()) {
            LOGGER.warn("Predictor debug is enabled, never enable this configuration in production !");
        }
        wordIdsToExclude = wordIdsToExclude != null ? wordIdsToExclude : EMPTY_INT_SET;

        LOGGER.debug("Predict for \"{}\" (wanted count = {})", textBeforeCaret, wantedCount);
        long startTotal = System.currentTimeMillis();

        // Parse raw text to get token
        final Pair<List<Token>, Boolean> tokenForPrediction = getTokenForPrediction(textBeforeCaret);
        List<Token> tokens = tokenForPrediction.getLeft();
        LOGGER.debug("Tokens for prediction are : {}", tokens);

        // Check if a word start
        long startLongestMatch = System.currentTimeMillis();
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, wantedCount * WANTED_COUNT_FACTOR, wordIdsToExclude);
        LOGGER.debug("Longest match detection took {} ms, found matching word : {} - {}", System.currentTimeMillis() - startLongestMatch,
                longestMatchingWords != null, longestMatchingWords != null ? longestMatchingWords.getLongestWordPrefix() : null);

        // Check min count to provide a prediction
        if (predictionParameter.getMinCountToProvidePrediction() <= 0 ||
                (longestMatchingWords != null && longestMatchingWords.getLongestWordPrefix().length() >= predictionParameter.getMinCountToProvidePrediction())) {

            // Generate prefix (a bigger prefix is generated if user model should be trained)
            final int wantedPrefixLenght = this.staticNgramDictionary.getMaxOrder();
            final Triple<int[], Boolean, Boolean> prefixAndUnknownWord = this.ngramWordPredictorUtils.createPrefixFor(tokens, longestMatchingWords,
                    wantedPrefixLenght, this.predictionParameter.isAddNewWordsEnabled());
            final int[] prefixForNGram = prefixAndUnknownWord.getLeft();

            // Collect each possible word from both ngram dictionary
            long startDebug = System.currentTimeMillis();
            final Map<BiIntegerKey, NextWord> nextWords = getNextWords(wantedCount, wordIdsToExclude, longestMatchingWords, prefixForNGram);
            LOGGER.debug("getNextWords in {} ms", System.currentTimeMillis() - startDebug);

            // Transform ngram to predictions
            startDebug = System.currentTimeMillis();
            List<AbstractPredictionToCompute> predictions = transformNextWordsToPrediction(prefixForNGram, nextWords, longestMatchingWords != null,
                    wordIdsToExclude);
            LOGGER.debug("transformNextWordsToPrediction in {} ms", System.currentTimeMillis() - startDebug);
            startDebug = System.currentTimeMillis();
            double predSum = computeProbabilities(prefixForNGram, predictions);
            LOGGER.debug("computeProbabilities2 in {} ms", System.currentTimeMillis() - startDebug);
            startDebug = System.currentTimeMillis();
            Collections.sort(predictions);
            LOGGER.debug("sort in {} ms", System.currentTimeMillis() - startDebug);

            LOGGER.debug("Prediction prob sum before normalization = {}", predSum);
            List<AbstractPredictionToCompute> predSubList = predictions.subList(0, Math.min(predictions.size(), wantedCount));

            // Remove prediction that have the same written text (keep the most probable)
            boolean capitalize = handleDoubleWordByCase(tokens, longestMatchingWords, predSubList);

            // Normalize prediction score (but only on returned prediction, total predictions list sum to 1, returned sum bellow 1)
            List<WordPrediction> wordPredictions = new ArrayList<>(predSubList.size());
            for (AbstractPredictionToCompute prediction : predSubList) {
                createWordPrediction(longestMatchingWords, predSum, capitalize, wordPredictions, prediction);
            }

            // First generate prediction for
            long time = System.currentTimeMillis() - startTotal;
            LOGGER.debug("Prediction took {} ms ({} results)", time, predictions.size());

            return new WordPredictionResult(null, Predict4AllUtils.countEndUntilNextSeparator(textAfterCaret), wordPredictions);
        } else {
            return new WordPredictionResult(null, 0, Collections.emptyList());
        }
    }
    //========================================================================


    // PROBABILITIES
    //========================================================================
    private double computeProbabilities(final int[] prefixForNGram, final List<AbstractPredictionToCompute> predictions) {
        // For each word, compute interpolation weight
        Pair<Double, Double> probInter = this.computeProbInterpolation();
        final double staticNgramWeight = probInter.getLeft();
        final double dynamicNGramWeight = probInter.getRight();

        //TODO : could be parallel because ngram dictionaries are already loaded with #getNextWords... not sure this affirmation is correct... - will check later
        double probSum = 0.0;
        for (AbstractPredictionToCompute prediction : predictions) {
            if (!prediction.isDouble()) {
                prediction.setScore(computeProb(prefixForNGram, staticNgramWeight, dynamicNGramWeight,
                        (prediction).getWordId(), prediction));
            } else {
                DoublePredictionToCompute mp = (DoublePredictionToCompute) prediction;
                mp.setScore(computeProb(mp.getFirstPrefix(), staticNgramWeight, dynamicNGramWeight,
                        mp.getFirstWordId(), prediction)
                        * computeProb(mp.getSecondPrefix(), staticNgramWeight, dynamicNGramWeight,
                        mp.getSecondWordId(), prediction));
            }
            double probFactor = this.wordDictionary.getWord(prediction.getWordId()).getProbFactor();
            prediction.setScore(prediction.getScore() * probFactor);
            if (predictionParameter.isEnableDebugInformation()) {
                prediction.getDebugInformation().append("\nWDF = ").append(probFactor);
            }
            probSum += prediction.getScore();
        }
        return probSum;
    }


    private double computeProb(final int[] prefixForNGram, final double staticNgramWeight, final double dynamicNGramWeight, int wordId, AbstractPredictionToCompute prediction) {
        // Prob from base
        double staticProb = this.staticNgramDictionary.getProbability(prefixForNGram, 0, prefixForNGram.length, wordId);
        double probability = staticNgramWeight * staticProb;
        if (this.predictionParameter.isEnableDebugInformation()) {
            prediction.getDebugInformation().append("\n").append(DEBUG_DF.format(staticNgramWeight)).append(" * ")
                    .append(DEBUG_DF.format(staticProb));
        }
        // Prob from dynamic (linear interpolation)
        if (this.dynamicNGramDictionary != null && this.predictionParameter.isDynamicModelEnabled()) {
            final double dynProb = this.dynamicNGramDictionary.getProbability(prefixForNGram, 0, prefixForNGram.length, wordId);
            probability += dynamicNGramWeight * dynProb;
            if (this.predictionParameter.isEnableDebugInformation()) {
                prediction.getDebugInformation().append(" + ").append(DEBUG_DF.format(dynamicNGramWeight)).append(" * ")
                        .append(DEBUG_DF.format(dynProb));
            }
        }
        if (this.predictionParameter.isEnableDebugInformation()) {
            prediction.getDebugInformation().append(" * ").append(DEBUG_DF.format(prediction.getFactor()));
        }
        return prediction.getFactor() * probability;
    }

    private Pair<Double, Double> computeProbInterpolation() {
        if (this.predictionParameter.isDynamicModelEnabled() && this.dynamicNGramDictionary != null) {
            DynamicNGramTrieNode dynRoot = (DynamicNGramTrieNode) this.dynamicNGramDictionary.getRoot();
            double dynamicWeight = Math.max((1.0 * dynRoot.getChildrenCountSum()) / (1.0 * this.staticNgramDictionary.getRoot().getChildrenSize()),
                    this.predictionParameter.getDynamicModelMinimumWeight());
            return Pair.of(1.0 - dynamicWeight, dynamicWeight);
        } else {
            return Pair.of(1.0, 0.0);
        }
    }
    //========================================================================

    // UTILS
    // ========================================================================

    /**
     * Tokenize a given raw text into token that will be used for prediction.<br>
     * The given text should be "as raw as possible" : every character in the raw text typed by user should be kept (space, comma, etc...).<br>
     * This will also execute a term detection on tokens, to be able to use generic term in ngram prefix.
     *
     * @param rawText the raw text currently typed (should be given "as it")
     * @return a pair containing the resulting token list (never null, but can be empty) on left, and a boolean indicating if the last token found is a token separator (space, comma, etc...)
     * @throws IOException if a problem happens when reading/writing tokens
     */
    private Pair<List<Token>, Boolean> getTokenForPrediction(String rawText) throws IOException {
        long startTokenize = System.currentTimeMillis();
        // Keep only a part of the raw text : tokenizing can be expensive
        String textForNGram = rawText.substring(Math.max(0, rawText.length() - NGRAM_MAX_LAST_TEXT_LENGTH), rawText.length());
        List<Token> tokens = tokenizer.tokenize(textForNGram);
        boolean lastTokenSeparator = !tokens.isEmpty() && tokens.get(tokens.size() - 1).isSeparator();
        // Execute a term detection
        tokens = termConverter.executeTermDetection(tokens);
        LOGGER.debug("Tokenization before word prediction took {} ms", System.currentTimeMillis() - startTokenize);
        return Pair.of(tokens, lastTokenSeparator);
    }

    private boolean handleDoubleWordByCase(List<Token> tokens, WordPrefixDetected longestMatchingWords, List<AbstractPredictionToCompute> predSubList) {
        boolean capitalize = longestMatchingWords != null ? longestMatchingWords.isCapitalizedWord() : this.wordPrefixDetector.isNextWordsCapitalized(tokens, null, 0);
        Map<String, Long> predictionCountsForLowerCaseWord = predSubList//
                .stream().peek(p -> p.computePrediction(wordDictionary))//
                .collect(Collectors.groupingBy(p -> Predict4AllUtils.lowerCase(p.getPrediction()), Collectors.counting()));
        ListIterator<AbstractPredictionToCompute> predIterator = predSubList.listIterator(predSubList.size());
        while (predIterator.hasPrevious()) {
            AbstractPredictionToCompute pred = predIterator.previous();
            String lowerCasePredText = Predict4AllUtils.lowerCase(pred.getPrediction());
            Long predCount = predictionCountsForLowerCaseWord.remove(lowerCasePredText);
            if (predCount != null && predCount > 1) {
                predIterator.remove();
            }
        }
        return capitalize;
    }

    private void createWordPrediction(WordPrefixDetected longestMatchingWords, double predSum, boolean capitalize, List<WordPrediction> wordPredictions, AbstractPredictionToCompute prediction) {
        String predictionToDisplay = capitalize ? Predict4AllUtils.capitalize(prediction.getPrediction()) : prediction.getPrediction();
        int previousCharCountToRemove = longestMatchingWords == null
                || predictionToDisplay.startsWith(longestMatchingWords.getLongestWordPrefix()) ? 0
                : longestMatchingWords.getLongestWordPrefix().length();
        String predictionToInsert = longestMatchingWords == null
                || !predictionToDisplay.startsWith(longestMatchingWords.getLongestWordPrefix()) ? predictionToDisplay
                : predictionToDisplay.substring(longestMatchingWords.getLongestWordPrefix().length());
        boolean insertSpacePossible = Separator
                .getSeparatorFor(predictionToDisplay.charAt(predictionToDisplay.length() - 1)) != Separator.APOSTROPHE;
        wordPredictions.add(new WordPrediction(predictionToDisplay, predictionToInsert, insertSpacePossible, prediction.getScore() / predSum,
                previousCharCountToRemove, prediction.isCorrection(), prediction.getWordId(),
                prediction.getDebugInformation() != null ? prediction.getDebugInformation().toString() : null));//TODO : should take two word id if it's a double word prediction
    }
    // ========================================================================


    // WORDS TO PREDICT
    //========================================================================
    private Map<BiIntegerKey, NextWord> getNextWords(int wantedCount, Set<Integer> wordIdsToExclude, WordPrefixDetected longestMatchingWords, final int[] prefixForNGram) throws IOException {
        // Get the base from detected prefix : if there is a prefix, the prefix detector already got the possible words
        Map<BiIntegerKey, NextWord> nextWords = longestMatchingWords != null ? longestMatchingWords.getWords() : new HashMap<>(wantedCount * WANTED_COUNT_FACTOR);
        // Get next words from base model (if there is no prefix)
        if (longestMatchingWords == null) {
            this.staticNgramDictionary.listNextWords(prefixForNGram, this.wordDictionary, predictionParameter, wordIdsToExclude, nextWords, wantedCount * WANTED_COUNT_FACTOR, true);
            // Get next words from dynamic model (if enabled)
            if (this.predictionParameter.isDynamicModelEnabled() && this.dynamicNGramDictionary != null) {
                this.dynamicNGramDictionary.listNextWords(prefixForNGram, this.wordDictionary, predictionParameter, wordIdsToExclude, nextWords, wantedCount * WANTED_COUNT_FACTOR, false);
            }
        }
        return nextWords;
    }

    private List<AbstractPredictionToCompute> transformNextWordsToPrediction(final int[] prefixForNGram, final Map<BiIntegerKey, NextWord> nextWords, boolean enableDoublePrediction, Set<Integer> wordIdsToExclude) {
        final List<AbstractPredictionToCompute> predictions = new ArrayList<>();
        nextWords.forEach((key, nextWord) -> {
            // Prediction for single word
            if (nextWord.isUnique()) {
                int wordId = nextWord.getWordId1();
                predictions.add(new UniquePredictionToCompute(wordId, nextWord.getFactor(), nextWord.isCorrection(), nextWord.getDebugInformation()));

                // If the wanted word is written - combine prediction for multiple words
                // BE CAREFUL : it could be possible to go further BUT combine 2+ words with this model has really bad performance !
                boolean endWithApostrophe = Predict4AllUtils.endsWith(wordDictionary.getWord(wordId).getWord(),
                        Separator.APOSTROPHE.getOfficialCharString());
                if (enableDoublePrediction && endWithApostrophe) {
                    int[] modifiedPrefix = new int[prefixForNGram.length];
                    System.arraycopy(prefixForNGram, 1, modifiedPrefix, 0, prefixForNGram.length - 1);
                    modifiedPrefix[modifiedPrefix.length - 1] = wordId;

                    final int wantedCount = 1;
                    final Map<BiIntegerKey, NextWord> nextWordsAfterWrittenWord = new HashMap<>(10);
                    staticNgramDictionary.listNextWords(modifiedPrefix, wordDictionary, predictionParameter, wordIdsToExclude,
                            nextWordsAfterWrittenWord, wantedCount, false);
                    nextWordsAfterWrittenWord.forEach((key2, nextWord2) -> {
                        if (nextWord2.isUnique()) {
                            predictions.add(new DoublePredictionToCompute(wordId, nextWord2.getWordId1(), false, prefixForNGram,
                                    modifiedPrefix, nextWord.getFactor() * nextWord2.getFactor(), nextWord.isCorrection(),
                                    predictionParameter.isEnableDebugInformation() ? new StringBuilder(nextWord.getDebugInformation()) : null));//TODO - Factor + factor /2 ?
                        } else {
                            throw new IllegalStateException("List next word should never return not unique predictions");
                        }
                    });
                }
            } else {
                int[] modifiedPrefix = new int[prefixForNGram.length];
                System.arraycopy(prefixForNGram, 1, modifiedPrefix, 0, prefixForNGram.length - 1);
                modifiedPrefix[modifiedPrefix.length - 1] = nextWord.getWordId1();
                predictions.add(
                        new DoublePredictionToCompute(nextWord.getWordId1(), nextWord.getWordId2(), nextWord.getSeparator() != Separator.APOSTROPHE,
                                prefixForNGram, modifiedPrefix, nextWord.getFactor(), nextWord.isCorrection(), nextWord.getDebugInformation()));
            }
        });
        return predictions;
    }
    //========================================================================

    // TRAINING PRIVATE
    //========================================================================
    private void _trainDynamicModel(String rawText, boolean ignoreLastSentence) throws IOException {
        if (this.dynamicNGramDictionary != null && this.predictionParameter.isDynamicModelEnabled()) {
            long start = System.currentTimeMillis();

            // Parse raw text to get token
            List<Token> tokens = this.termConverter.executeTermDetection(tokenizer.tokenize(rawText));

            // Divide in sentences (and remove separators)
            List<List<Token>> sentences = divideInSentencesAndRemoveSeparator(tokens);

            // For each sentence : train dynamic process
            for (int i = 0; i < sentences.size(); i++) {
                List<Token> sentence = sentences.get(i);
                if (!sentence.isEmpty() && (!ignoreLastSentence || i != sentences.size() - 1)) {
                    for (int endIndex = 1; endIndex <= sentence.size(); endIndex++) {
                        List<Token> sentencePart = sentence.subList(0, endIndex);
                        final Triple<int[], Boolean, Boolean> prefixAndUnknownWord = this.ngramWordPredictorUtils.createPrefixFor(sentencePart, null,
                                this.dynamicNGramDictionary.getMaxOrder() + 1, this.predictionParameter.isAddNewWordsEnabled());
                        // Skip first "START" elements because START shouldn't be "overtrained"
                        this.trainDynamicNGramModel(prefixAndUnknownWord,
                                sentencePart.size() == 1 ? this.dynamicNGramDictionary.getMaxOrder() - 2 : 0);
                    }

                }
            }
            LOGGER.info("Trained dynamic prediction model in {} ms", System.currentTimeMillis() - start);
        }
    }

    private void trainDynamicNGramModel(Triple<int[], Boolean, Boolean> prefixAndUnknownWord, int trainingStartOrder) {
        if (this.dynamicNGramDictionary != null && this.predictionParameter.isDynamicModelEnabled()) {
            // Create or increment user word counts
            final int[] predictionPrefix = prefixAndUnknownWord.getLeft();
            wordDictionary.incrementUserWord(predictionPrefix[predictionPrefix.length - 1]);

            // If there is only start tag
            // OR
            // If there is an unknown word and learning new word from user is not enabled : skip learning ngram

            // TODO : if there is a unknown word in first positions, the last ngram part are not trained while they should be training as unigram, bigram...
            // should be the ngram unknown word position to be able to start training order after that
            if (prefixAndUnknownWord.getMiddle() || (prefixAndUnknownWord.getRight() && !predictionParameter.isAddNewWordsEnabled())) {
                LOGGER.info("Skip user training because there is only start tag = {}, or because new word learning is disabled = {}",
                        prefixAndUnknownWord.getMiddle(), prefixAndUnknownWord.getRight());
                return;
            }
            for (int i = trainingStartOrder; i < this.dynamicNGramDictionary.getMaxOrder(); i++) {
                this.dynamicNGramDictionary.putAndIncrementBy(predictionPrefix, i, 1);
                this.dynamicNGramDictionary.updateProbabilities(predictionPrefix, i, this.dynamicNGramDictionary.computeD(configuration));
            }
        }
    }

    private List<List<Token>> divideInSentencesAndRemoveSeparator(List<Token> tokens) {
        List<List<Token>> sentences = new ArrayList<>();
        List<Token> currentSentence = new ArrayList<>();
        sentences.add(currentSentence);
        for (Token token : tokens) {
            if (token.isSeparator() && token.getSeparator().isSentenceSeparator()) {
                currentSentence = new ArrayList<>();
                sentences.add(currentSentence);
            } else if (!token.isSeparator()) {
                currentSentence.add(token);
            }
        }
        return sentences;
    }
    //========================================================================


}
