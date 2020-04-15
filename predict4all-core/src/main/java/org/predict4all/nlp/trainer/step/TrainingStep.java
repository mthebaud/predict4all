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

package org.predict4all.nlp.trainer.step;

import org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;

import java.io.File;
import java.util.function.BiFunction;

/**
 * Represent the possible training steps.<br>
 * This allow training to be stopped and started again at a specific step : going to converted tokens, and then running WORDS_DICTIONARY multiple times.
 * This is useful when testing algorithms on big corpus, it allows testing without doing the whole training again.
 * <p>
 * <strong>ORDINAL IS IMPORTANT</strong>
 *
 * @author Mathieu THEBAUD
 */
public enum TrainingStep {
    PARSER(null, "raw-tokens", (file, corpus) -> new ParserTrainingDocument(corpus.getEncoding(), file, corpus.getOutputDirectory())), //
    TOKEN_CONVERT(PARSER, "converted-tokens", (file, corpus) -> new TokenConverterTrainingDocument(file, corpus.getOutputDirectory())), //
    WORDS_DICTIONARY(TOKEN_CONVERT, "clean-tokens", (file, corpus) -> new WordDictionaryTrainingDocument(file, corpus.getOutputDirectory())), //
    NGRAM_DICTIONARY(WORDS_DICTIONARY, null, (file, corpus) -> new NGramTrainingDocument(file, corpus.getOutputDirectory())), //
    SEMANTIC_DICTIONARY(WORDS_DICTIONARY, null, (file, corpus) -> new SemanticTrainingDocument(file, corpus.getOutputDirectory()));

    private final BiFunction<File, TrainingCorpus, AbstractTrainingDocument> stepDocumentSupplier;
    private final String outputDirectoryName;
    private final TrainingStep previousStep;

    private TrainingStep(TrainingStep previousStep, String outputDirectoryName,
                         BiFunction<File, TrainingCorpus, AbstractTrainingDocument> stepDocumentSupplier) {
        this.previousStep = previousStep;
        this.outputDirectoryName = outputDirectoryName;
        this.stepDocumentSupplier = stepDocumentSupplier;
    }

    public String getOutputDirectoryName() {
        return outputDirectoryName;
    }

    public TrainingStep getPreviousStep() {
        return previousStep;
    }

    public AbstractTrainingDocument getStep(File documentFile, TrainingCorpus corpus) {
        return this.stepDocumentSupplier.apply(documentFile, corpus);
    }
}
