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

package org.predict4all.nlp.parser;

import org.predict4all.nlp.Separator;
import org.predict4all.nlp.io.TokenFileOutputStream;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.parser.token.SeparatorToken;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.parser.token.WordToken;
import org.predict4all.nlp.trainer.TrainerTask;
import org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.utils.progressindicator.LoggingProgressIndicator;
import org.predict4all.nlp.utils.progressindicator.NoOpProgressIndicator;
import org.predict4all.nlp.utils.progressindicator.ProgressIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This takes a raw text and to create tokens from it. The tokens are purely lowest text unit, like words and punctuation (space included).<br>
 * The resulting tokens can be then used by other NLP task to generate/use data.<br>
 *
 * @author Mathieu THEBAUD
 */
public class Tokenizer {
    public static final DecimalFormat TOKEN_COUNT_FORMAT = new DecimalFormat("###,###,###,###,###");
    private static final Logger LOGGER = LoggerFactory.getLogger(Tokenizer.class);

    private static final String UTF8_BOM = "\uFEFF";

    private final LanguageModel languageModel;

    public Tokenizer(LanguageModel languageModel) {
        this.languageModel = Predict4AllUtils.checkNull(languageModel, "Tokenizer needs a language model, languageModel shouldn't be null");
    }

    // PUBLIC API
    //========================================================================
    public List<Token> tokenize(String rawText) throws IOException {
        List<Token> list;
        tokenize(new StringProducer() {
            private boolean hasNext = true;

            @Override
            public void close() {
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public String next() {
                hasNext = false;
                return rawText;
            }

            @Override
            public boolean hasNext() {
                return hasNext;
            }
        }, new TokenListAppender(list = new ArrayList<>()), NoOpProgressIndicator.INSTANCE);
        return list;
    }

    public List<TrainerTask> tokenize(TrainingCorpus corpus) {
        corpus.initStep(TrainingStep.PARSER);
        LoggingProgressIndicator progressIndicator = new LoggingProgressIndicator("Tokenization", corpus.getTotalCountFor(TrainingStep.PARSER));
        return corpus.getDocuments(TrainingStep.PARSER).stream().map(d -> new TokenizeTask(progressIndicator, d, corpus)).collect(Collectors.toList());
    }
    //========================================================================

    // TASK
    //========================================================================
    private class TokenizeTask extends TrainerTask {
        private final TrainingCorpus trainingCorpus;

        public TokenizeTask(ProgressIndicator progressIndicator, AbstractTrainingDocument document, TrainingCorpus trainingCorpus) {
            super(progressIndicator, document);
            this.trainingCorpus = trainingCorpus;
        }

        @Override
        public void run() throws Exception {
            try (TokenFileOutputStream tokenFileOuputStream = new TokenFileOutputStream(document.getOutputFile())) {
                try (StringProducer stringProducer = getProducerFor(document)) {
                    int tokenCount = tokenize(stringProducer, tokenFileOuputStream, progressIndicator);
                    document.writeInformations(tokenCount);
                }
            }
        }

        @SuppressWarnings("resource")
        public StringProducer getProducerFor(AbstractTrainingDocument document) throws IOException {
            // FIXME : encoding should be set by user
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(document.getInputFile()), trainingCorpus.getEncoding()));
            return new StringProducer() {
                private String cachedLine;

                @Override
                public void close() throws Exception {
                    bufferedReader.close();
                }

                @Override
                public String next() {
                    if (hasNext()) {
                        String line = this.cachedLine;
                        this.cachedLine = null;
                        if (line.startsWith(UTF8_BOM)) {
                            return line.substring(1);
                        }
                        return line;
                    } else {
                        return null;
                    }
                }

                @Override
                public boolean hasNext() {
                    if (this.cachedLine != null)
                        return true;
                    else {
                        try {
                            this.cachedLine = bufferedReader.readLine();
                            return this.cachedLine != null;
                        } catch (IOException e) {
                            return false;
                        }
                    }
                }

                @Override
                public int size() {
                    return document.getCount();
                }
            };
        }
    }
    //========================================================================

    // PRIVATE API
    // ========================================================================
    private int tokenize(StringProducer stringProducer, TokenAppender tokenAppender, ProgressIndicator progressIndicator) throws IOException {
        LOGGER.debug("Start tokenization for {} string", stringProducer.size());
        long start = System.currentTimeMillis();

        int tokenCount = 0;
        StringBuilder currentContent = new StringBuilder(this.languageModel.getAverageWordLength());

        while (stringProducer.hasNext()) {
            String rawText = stringProducer.next() + (stringProducer.hasNext() ? "\n" : "");
            for (int i = 0; i < rawText.length(); i++) {
                // Check there is a token separator (fake line separator between iterator  tokens)
                char charAt = rawText.charAt(i);
                Separator charSep = Separator.getSeparatorFor(charAt);
                // Token separator : finish last word and add the separator
                if (charSep != null) {
                    if (currentContent.length() > 0) {
                        tokenCount++;
                        tokenAppender.append(WordToken.create(currentContent.toString()));
                    }
                    // Add token separator and reinitialize current content
                    tokenAppender.append(SeparatorToken.create(charSep));
                    tokenCount++;
                    currentContent = new StringBuilder(this.languageModel.getAverageWordLength());
                } else {
                    currentContent.append(charAt);
                }
            }
            progressIndicator.increment();
        }
        if (currentContent.length() > 0) {
            tokenCount++;
            tokenAppender.append(WordToken.create(currentContent.toString()));
        }
        LOGGER.debug("{} tokens created in {} s,", TOKEN_COUNT_FORMAT.format(tokenCount), (System.currentTimeMillis() - start) / 1000.0);
        return tokenCount;
    }
    // ========================================================================
}
