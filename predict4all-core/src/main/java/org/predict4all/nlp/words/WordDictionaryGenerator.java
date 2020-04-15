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

package org.predict4all.nlp.words;

import org.predict4all.nlp.Tag;
import org.predict4all.nlp.io.TokenFileInputStream;
import org.predict4all.nlp.io.TokenFileOutputStream;
import org.predict4all.nlp.io.WordFileOutputStream;
import org.predict4all.nlp.language.BaseWordDictionary;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.parser.token.TagToken;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.parser.token.WordToken;
import org.predict4all.nlp.trainer.TrainerTask;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.utils.progressindicator.LoggingProgressIndicator;
import org.predict4all.nlp.utils.progressindicator.ProgressIndicator;
import org.predict4all.nlp.words.model.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This will generate a word dictionary from a {@link TrainingCorpus} : this will detect different word in training corpus and try to filter out words :
 * match lower/upper case words, filter on a {@link BaseWordDictionary}, exclude low count words, etc.
 *
 * @author Mathieu THEBAUD
 */
public class WordDictionaryGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordDictionaryGenerator.class);

    public static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("##0.00");
    public static final DecimalFormat COUNT_FORMAT = new DecimalFormat("###,###,###,###,###,###,###,###");

    private final LanguageModel languageModel;
    private final TrainingConfiguration trainingConfiguration;
    private final BaseWordDictionary baseWordDictionary;

    public WordDictionaryGenerator(LanguageModel languageModel, TrainingConfiguration trainingConfiguration) throws IOException {
        this.languageModel = Predict4AllUtils.checkNull(languageModel, "WordsConverter needs a language model, languageModel shouldn't be null");
        this.trainingConfiguration = Predict4AllUtils.checkNull(trainingConfiguration, "WordsConverter needs a training configuration, trainingConfiguration shouldn't be null");
        baseWordDictionary = languageModel.getBaseWordDictionary(trainingConfiguration);
        if (!baseWordDictionary.isInitialized()) {
            baseWordDictionary.initialize();
        }
    }

    // PUBLIC API
    //========================================================================
    public void createWordDictionary(TrainingCorpus corpus, Consumer<List<TrainerTask>> taskExecutor, File dictionaryOuputFile) throws FileNotFoundException, IOException {
        corpus.initStep(TrainingStep.WORDS_DICTIONARY);
        LoggingProgressIndicator progressIndicator = new LoggingProgressIndicator("Words converter and dictionary generation",
                corpus.getTotalCountFor(TrainingStep.WORDS_DICTIONARY) * 2);

        // Count all words
        final ConcurrentHashMap<String, LongAdder> wordCounts = new ConcurrentHashMap<>();
        taskExecutor.accept(corpus.getDocuments(TrainingStep.WORDS_DICTIONARY).stream().map(d -> new CountWordTask(wordCounts, progressIndicator, d))
                .collect(Collectors.toList()));
        long totalWordCount = wordCounts.values().parallelStream().mapToLong(LongAdder::sum).sum();
        LOGGER.info("Word count created, found {} differents words (before validation), corpus contained {} total words", wordCounts.size(),
                COUNT_FORMAT.format(totalWordCount));

        // Replace each invalid word and detect word for dictionary
        final ConcurrentHashMap<String, Boolean> finalWordSet = new ConcurrentHashMap<>();
        taskExecutor.accept(corpus.getDocuments(TrainingStep.WORDS_DICTIONARY).stream()
                .map(d -> new ReplaceInvalidWordsAndCreateDictionaryTask(progressIndicator, d, wordCounts, finalWordSet, totalWordCount))
                .collect(Collectors.toList()));

        // Create word dictionary
        LOGGER.info("Detected {} different valid words in corpus", finalWordSet.size());
        if (dictionaryOuputFile != null) {
            long start = System.currentTimeMillis();
            // Add only valid word to dictionary
            String dictionaryID = UUID.randomUUID().toString();
            WordDictionary generatedWordDictionary = new WordDictionary(languageModel, dictionaryID);
            finalWordSet.forEach((word, count) -> generatedWordDictionary.putWordTraining(word));
            generatedWordDictionary.compact();

            // Save dictionary file
            LOGGER.info("Will save word dictionary to {}", dictionaryOuputFile);
            try (WordFileOutputStream wfos = new WordFileOutputStream(dictionaryOuputFile)) {
                Collection<Word> words = generatedWordDictionary.getAllWords();
                wfos.writeUTF(dictionaryID);
                wfos.writeInt(generatedWordDictionary.getIDGeneratorState());
                for (Word word : words) {
                    if (word.isValidForSaving()) {
                        wfos.writeWord(word);
                    }
                }
            }
            LOGGER.info("Word dictionary saved to {}, {} words added to dictionary and saved in {} ms", dictionaryOuputFile,
                    generatedWordDictionary.size(), System.currentTimeMillis() - start);
        } else {
            LOGGER.info("Word dictionary will not be saved because dictionaryOutputFile is null");
        }
    }
    //========================================================================

    // TASKS
    //========================================================================
    private class ReplaceInvalidWordsAndCreateDictionaryTask extends TrainerTask {
        private final ConcurrentHashMap<String, LongAdder> wordCounts;
        private final ConcurrentHashMap<String, Boolean> finalWordSet;
        private final long totalWordCount;

        public ReplaceInvalidWordsAndCreateDictionaryTask(ProgressIndicator progressIndicator, AbstractTrainingDocument document,
                                                          final ConcurrentHashMap<String, LongAdder> wordCounts, final ConcurrentHashMap<String, Boolean> finalWordCounts,
                                                          long totalWordCount) {
            super(progressIndicator, document);
            this.wordCounts = wordCounts;
            this.totalWordCount = totalWordCount;
            this.finalWordSet = finalWordCounts;
        }

        @Override
        public void run() throws Exception {
            try (TokenFileInputStream tokenFis = new TokenFileInputStream(document.getInputFile())) {
                try (TokenFileOutputStream tokenFos = new TokenFileOutputStream(document.getOutputFile())) {
                    Token token = tokenFis.readToken();
                    boolean sentenceStart = true;
                    while (token != null) {
                        tokenFos.writeToken(getReplacingTokenFor(token, sentenceStart, wordCounts, finalWordSet, totalWordCount));
                        sentenceStart = token.isSeparator() && (sentenceStart || token.getSeparator().isSentenceSeparator());
                        token = token.getNext(tokenFis);
                        progressIndicator.increment();
                    }
                    document.writeInformations(document.getCount());// Same token count I/O
                }
            }
        }
    }

    private class CountWordTask extends TrainerTask {
        private final ConcurrentHashMap<String, LongAdder> wordCounts;

        public CountWordTask(ConcurrentHashMap<String, LongAdder> wordCounts, ProgressIndicator progressIndicator,
                             AbstractTrainingDocument document) {
            super(progressIndicator, document);
            this.wordCounts = wordCounts;
        }

        @Override
        public void run() throws Exception {
            try (TokenFileInputStream tokenFis = new TokenFileInputStream(document.getInputFile())) {
                Token token = tokenFis.readToken();
                boolean sentenceStart = true;
                while (token != null) {
                    if (token.isWord()) {
                        String tokenText = token.getText();
                        if (sentenceStart && Character.isUpperCase(tokenText.charAt(0))) {
                            tokenText = tokenText.toLowerCase();
                        }
                        wordCounts.computeIfAbsent(tokenText, k -> new LongAdder()).increment();
                    }
                    sentenceStart = token.isSeparator() && (sentenceStart || token.getSeparator().isSentenceSeparator());
                    token = token.getNext(tokenFis);
                    progressIndicator.increment();
                }
            }
        }
    }
    //========================================================================

    // PRIVATE API
    //========================================================================
    //TODO : explain the logic here
    private Token getReplacingTokenFor(Token token, boolean sentenceStart, ConcurrentHashMap<String, LongAdder> wordCounts,
                                       ConcurrentHashMap<String, Boolean> finalWordSet, long totalWordCount) {
        /*
         * Check every word, and check only term where the word is used in prediction
         */
        if (token.isWord()) {
            String replacingWord = lowerCaseWordIfNeeded(wordCounts, token.getText(), sentenceStart);
            LongAdder countAdder = wordCounts.get(replacingWord);
            long count = countAdder != null ? countAdder.sum() : 0;
            replacingWord = checkWordCount(getReplacingWord(replacingWord, count), count);

            // Replace only deleted or changed token
            if (replacingWord == null) {
                return TagToken.create(Tag.UNKNOWN);
            } else if (!token.getText().equals(replacingWord)) {
                addToDictionarySet(replacingWord, finalWordSet);
                return WordToken.create(replacingWord);
            } else {
                addToDictionarySet(token.getText(), finalWordSet);
            }
        }
        return token;
    }

    private void addToDictionarySet(String word, ConcurrentHashMap<String, Boolean> finalWordSet) {
        if (word != null && (Predict4AllUtils.length(word) > 1 || languageModel.getValidOneCharWords().contains(Predict4AllUtils.lowerCase(word)))) {
            finalWordSet.put(word, true);
        }
    }

    private String checkWordCount(final String word, long count) {
        return word != null && count > this.trainingConfiguration.getUnknownWordCountThreshold() ? word : null;
    }

    private String getReplacingWord(final String word, long count) {
        // If the word is directly correct (contained in dict or its count is really high)
        if (this.baseWordDictionary.containsWord(word) || count > this.trainingConfiguration.getDirectlyValidWordCountThreshold()) {
            return word;
        }
        // Check if a version of the uncapitalized/lowercase word is present in dict and with a high frequency
        // Return the original word if there is a other version of the word present in the dict but shouldn't be replaced
        if (Character.isUpperCase(word.charAt(0))) {
            // Check if uncapitalized word is in the existing dictionary
            String uncapitalizedWord = Predict4AllUtils.uncapitalize(word);
            double freqUncapitalized = this.baseWordDictionary.getFrequency(uncapitalizedWord);
            if (freqUncapitalized > 0) {
                return freqUncapitalized > this.trainingConfiguration.getConvertCaseFromDictionaryModelThreshold() ? uncapitalizedWord : word;
            }
        }
        // Check if lower case word is in dictionary and should replace word
        if (Predict4AllUtils.containsUpperCase(word)) {
            String lowerCaseWord = Predict4AllUtils.lowerCase(word);
            double freqLowerCase = this.baseWordDictionary.getFrequency(lowerCaseWord);
            if (freqLowerCase > 0) {
                return freqLowerCase > this.trainingConfiguration.getConvertCaseFromDictionaryModelThreshold() ? lowerCaseWord : word;
            }
        }

        // Didn't find any way to validate the word
        return null;
    }

    private String lowerCaseWordIfNeeded(ConcurrentHashMap<String, LongAdder> wordCounts, final String word, boolean sentenceStart) {
        if (Character.isUpperCase(word.charAt(0))) {
            String loweredWord = word.toLowerCase();
            //Lower case every sentence start word
            if (sentenceStart) {
                return loweredWord;
            } else if (wordCounts.containsKey(loweredWord)) {
                long countLowerCase = wordCounts.get(loweredWord).sum();
                long countUpperCase = wordCounts.get(word).sum();
                double percentLowerCase = (1.0 * countLowerCase) / (countLowerCase + countUpperCase);
                if (percentLowerCase >= trainingConfiguration.getUpperCaseReplacementThreshold()) {
                    return loweredWord;
                }
            }
        }
        return word;
    }
    //========================================================================

}
