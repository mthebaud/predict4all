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

/**
 * Represents the whole data training process managed by the main {@link org.predict4all.nlp.trainer.DataTrainer}.<br>
 * Training is done with different steps :
 * <ul>
 *     <li>{@link org.predict4all.nlp.parser.Tokenizer}</li>
 *     <li>{@link org.predict4all.nlp.parser.matcher.TokenConverter}</li>
 *     <li>{@link org.predict4all.nlp.words.WordDictionaryGenerator}</li>
 *     <li>{@link org.predict4all.nlp.ngram.NGramDictionaryGenerator}</li>
 * </ul>
 * Note that the {@link org.predict4all.nlp.trainer.DataTrainer} use {@link org.predict4all.nlp.trainer.corpus.TrainingCorpus} and {@link org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument} :
 * this abstraction level is useful to be able to train the model on same corpus without having to go through every training step : really useful when developing new training algorithms.
 *
 * @author Mathieu THEBAUD
 */
package org.predict4all.nlp.trainer;