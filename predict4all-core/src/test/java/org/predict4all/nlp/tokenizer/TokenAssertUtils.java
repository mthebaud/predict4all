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

package org.predict4all.nlp.tokenizer;

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.parser.token.Token;

import static org.junit.Assert.*;

public class TokenAssertUtils {
    public static void assertTerm(Token token, EquivalenceClass type, String result) {
        assertTrue(token.isEquivalenceClass());
        assertFalse(token.isSeparator());
        assertFalse(token.isWord());
        assertEquals(type, token.getEquivalenceClass());
        assertEquals(result, token.getText());
    }

    public static void assertWord(Token token, String result) {
        assertTrue(token.isWord());
        assertFalse(token.isSeparator());
        assertFalse(token.isEquivalenceClass());
        assertEquals(result, token.getText());
    }

    public static void assertSeparator(Token token, Separator type) {
        assertTrue(token.isSeparator());
        assertFalse(token.isWord());
        assertFalse(token.isEquivalenceClass());
        assertEquals(type, token.getSeparator());
    }
}
