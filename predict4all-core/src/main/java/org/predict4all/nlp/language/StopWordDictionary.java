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

import org.predict4all.nlp.words.WordDictionary;

import java.io.IOException;

/**
 * A language specific dictionary : contains every stop words for a language
 *
 * @author Mathieu THEBAUD
 */
public interface StopWordDictionary {

    boolean isInitialized();

    void initialize(WordDictionary wordDictionary) throws IOException;

    boolean containsWord(int wordId);
}
