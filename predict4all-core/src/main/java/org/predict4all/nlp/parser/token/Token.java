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

package org.predict4all.nlp.parser.token;

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.words.WordDictionary;

import java.io.IOException;

/**
 * Represent the lowest unit when parsing a text. The unit can be words, but can also a special type : token separator, tags, or equivalence classes.<br>
 * Token separators are the special character that are used as separators between words in natural language : space, comma, etc... (see {@link Separator})<br>
 * Equivalence classes are a special "word" tokens that already have a semantic information : they can be date, numbers, etc... (see {@link EquivalenceClass})<br>
 *
 * @author Mathieu THEBAUD
 */
public interface Token {
    byte TYPE_SEPARATOR = 0, TYPE_WORD = 1, TYPE_EQUIVALENCE_CLASS = 2, TYPE_TAG = 3;

    boolean isWord();

    boolean isSeparator();

    boolean isEquivalenceClass();

    boolean isTag();

    Tag getTag();

    String getText();

    Separator getSeparator();

    Token getNext(TokenProvider nextTokenProvider) throws IOException;

    void clearNextCache();

    EquivalenceClass getEquivalenceClass();

    String getTextForType();

    int getWordId(WordDictionary dictionary);
}
