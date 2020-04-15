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

package org.predict4all.nlp.language;

import org.predict4all.nlp.parser.matcher.TokenMatcher;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;

import java.util.Set;

/**
 * Represent a model specific to the input language.<br>
 * This model is useful to better perform on NLP task by using specific
 * parameters from a language.<br>
 * E.G. {@link #getAverageWordLength()} is useful to optimize tokenizer.
 *
 * @author Mathieu THEBAUD
 */
public interface LanguageModel {

    // INFO
    // ========================================================================

    /**
     * @return identifier for this language model (e.g. ISO code)
     */
    String getId();
    // ========================================================================

    // LANGUAGE SPECIFIC
    // ========================================================================

    /**
     * @return the average word length for this language (can be round to the upper value)
     */
    int getAverageWordLength();

    /**
     * Average total vocabulary size (different existing words)
     *
     * @return the average vocabulary size for this language.
     */
    int getAverageVocabularySize();

    Set<String> getValidOneCharWords();

    TokenMatcher[] getTokenMatchersForSemanticAnalysis();

    TokenMatcher[] getTokenMatchersForNGram();
    // ========================================================================

    // DICTIONARIES
    //========================================================================
    StopWordDictionary getStopWordDictionary(TrainingConfiguration configuration);

    BaseWordDictionary getBaseWordDictionary(TrainingConfiguration configuration);
    //========================================================================
}