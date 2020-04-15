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

package org.predict4all.nlp.trainer;

import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.ngram.NGramDictionaryGenerator;
import org.predict4all.nlp.ngram.debug.NGramDebugger;
import org.predict4all.nlp.parser.Tokenizer;
import org.predict4all.nlp.parser.matcher.TokenConverter;
import org.predict4all.nlp.semantic.SemanticDictionaryGenerator;
import org.predict4all.nlp.trainer.DataTrainerResult.Builder;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.words.WordDictionary;
import org.predict4all.nlp.words.WordDictionaryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to create prediction data to be used with a word predictor.
 *
 * @author Mathieu THEBAUD
 */
public class DataTrainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTrainer.class);

    public static final String FILENAME_WORD_DICTIONARY = "words.bin", FILENAME_NGRAM_DICTIONARY = "ngrams.bin",
            FILENAME_LSA_DICTIONARY = "semantic.bin";
    public static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("##0.00");

    private NGramDebugger ngramDebugBeforePruning, ngramDebugAfterPruning;
    private String debugPrefix;

    private final File outputDictionary, outputNGram, outputSemantic;

    private final LanguageModel languageModel;
    private final TrainingConfiguration trainingConfiguration;
    private final TrainingCorpus corpus;
    private final File workingDirectory;

    public DataTrainer(File workingDirectory, File outputDictionary, File outputNGram, File outputSemantic, final LanguageModel languageModel,
                       final TrainingConfiguration trainingConfiguration) throws IOException {
        int concurrencyLevel = Runtime.getRuntime().availableProcessors();
        this.outputDictionary = outputDictionary;
        this.outputNGram = outputNGram;
        this.outputSemantic = outputSemantic;
        this.workingDirectory = workingDirectory;
        this.workingDirectory.mkdirs();
        this.corpus = new TrainingCorpus(concurrencyLevel, trainingConfiguration.getCorpus(), workingDirectory, "UTF-8");
        this.languageModel = languageModel;
        this.trainingConfiguration = trainingConfiguration;
    }

    public void setNgramDebugBeforePruning(NGramDebugger ngramDebugBeforePruning) {
        this.ngramDebugBeforePruning = ngramDebugBeforePruning;
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

    // PUBLIC API
    //========================================================================
    public DataTrainerResult launchNGramTraining(TrainingStep initialStep) throws Exception {
        return launchTraining(initialStep, false);
    }

    public DataTrainerResult launchLSATraining(TrainingStep initialStep) throws Exception {
        return launchTraining(initialStep, true);
    }
    //========================================================================

    // TRAINING TASKS
    //========================================================================
    private DataTrainerResult launchTraining(TrainingStep initialStep, boolean semantic) throws Exception {
        long startTotal = System.currentTimeMillis();

        Builder dataTrainerResultBuilder = DataTrainerResult.builder();

        // Init
        Tokenizer simpleTextTokenizer = new Tokenizer(languageModel);
        TokenConverter tokenConverter = new TokenConverter(
                semantic ? languageModel.getTokenMatchersForSemanticAnalysis() : languageModel.getTokenMatchersForNGram());
        WordDictionaryGenerator wordDictionaryGenerator = new WordDictionaryGenerator(languageModel, trainingConfiguration);

        // Thread pool
        LOGGER.info("Will run on {} processors", corpus.getConcurrencyLevel());
        ExecutorService executorService = createExecutorService();

        try {
            long start = System.currentTimeMillis();

            // TOKENIZE/CLEAN
            if (initialStep.ordinal() <= TrainingStep.PARSER.ordinal()) {
                executeTasksBlocking(executorService, simpleTextTokenizer.tokenize(corpus));
                LOGGER.info("Raw parsing took {} s", (System.currentTimeMillis() - start) / 1000.0);
            }

            if (initialStep.ordinal() <= TrainingStep.TOKEN_CONVERT.ordinal() || semantic) {
                start = System.currentTimeMillis();
                executeTasksBlocking(executorService, tokenConverter.executeTokenPatternMatching(corpus));
                LOGGER.info("Token convert took {} s", (System.currentTimeMillis() - start) / 1000.0);
            }

            if (initialStep.ordinal() <= TrainingStep.WORDS_DICTIONARY.ordinal() || semantic) {
                start = System.currentTimeMillis();
                wordDictionaryGenerator.createWordDictionary(corpus, tasks -> executeTasksBlocking(executorService, tasks),
                        semantic ? null : outputDictionary);
                LOGGER.info("Word dictionary generation took process took {} s", (System.currentTimeMillis() - start) / 1000.0);
            }

            if (initialStep.ordinal() <= TrainingStep.NGRAM_DICTIONARY.ordinal() && !semantic) {
                WordDictionary wordDictionary = WordDictionary.loadDictionary(languageModel, outputDictionary);
                NGramDictionaryGenerator nGramDictionaryGenerator = new NGramDictionaryGenerator(languageModel, trainingConfiguration,
                        wordDictionary);
                nGramDictionaryGenerator.setNgramDebugAfterPruning(ngramDebugAfterPruning);
                nGramDictionaryGenerator.setNgramDebugBeforePruning(ngramDebugBeforePruning);
                nGramDictionaryGenerator.setDebugPrefix(debugPrefix);
                start = System.currentTimeMillis();
                Map<Integer, Pair<Integer, Integer>> ngramCounts = nGramDictionaryGenerator.executeNGramTraining(corpus, outputNGram,
                        tasks -> executeTasksBlocking(executorService, tasks));
                dataTrainerResultBuilder.withNgramCounts(ngramCounts);
                LOGGER.info("Ngram dictionary process took {} s", (System.currentTimeMillis() - start) / 1000.0);
            }

            if (initialStep.ordinal() <= TrainingStep.SEMANTIC_DICTIONARY.ordinal() && semantic) {
                start = System.currentTimeMillis();
                SemanticDictionaryGenerator lsaGenerator = new SemanticDictionaryGenerator(languageModel,
                        WordDictionary.loadDictionary(languageModel, outputDictionary), trainingConfiguration);
                lsaGenerator.executeLSATrainingForR(corpus, outputSemantic, tasks -> executeTasksBlocking(executorService, tasks));
                LOGGER.info("Semantic dictionary process took {} s", (System.currentTimeMillis() - start) / 1000.0);
            }
        } finally {
            executorService.shutdown();
        }
        LOGGER.info("Whole training process took {} s", (System.currentTimeMillis() - startTotal) / 1000.0);
        return dataTrainerResultBuilder.build();
    }
    //========================================================================

    // THREADING
    //========================================================================
    private <T extends TrainerTask> void executeTasksBlocking(ExecutorService executorService, List<T> tasks) {
        try {
            executorService.invokeAll(tasks).forEach(future -> {
                try {
                    future.get();
                } catch (Throwable t) {
                    if (t.getCause() != null) {
                        LOGGER.error("Problem in task execution", t.getCause());
                    } else {
                        LOGGER.error("Problem in task execution", t);
                    }
                }
            });
        } catch (InterruptedException e) {
            LOGGER.error("Problem in task execution", e);
        }
    }

    private ExecutorService createExecutorService() {
        return Executors.newFixedThreadPool(corpus.getConcurrencyLevel(), new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "DataTrainer-Thread-" + threadNumber.getAndIncrement());
                t.setDaemon(false);
                t.setPriority(Thread.MAX_PRIORITY);
                return t;
            }
        });
    }
    //========================================================================
}
