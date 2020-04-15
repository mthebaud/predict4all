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

package org.predict4all.nlp.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.parser.TokenAssertionError;
import org.predict4all.nlp.parser.TokenizerUtils;
import org.predict4all.nlp.parser.token.EquivalenceClassToken;
import org.predict4all.nlp.parser.token.Token;

import static org.junit.Assert.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class TokenizerUtilsTest {

    @Test
    public void testAssertTokenEqualsValid() {
        TokenizerUtils.assertEquals(TokenizerUtils.createTokensList("test1", "test2", "test3", "test4"), "test1", "test2", "test3", "test4");
    }

    @Test
    public void testAssertTokenEqualsInvalid() {
        try {
            TokenizerUtils.assertEquals(TokenizerUtils.createTokensList("test1", "test2", "testDiff"), "test4", "test1", "test2", "test3", "test4");
            fail("Assert equals on token should fail");
        } catch (TokenAssertionError e) {
            // OK
        }
    }

    @Test
    public void testAssertLess() {
        try {
            TokenizerUtils.assertEquals(TokenizerUtils.createTokensList("test1", "test2", "test3", "test4", "test5"), "test1", "test2", "test3",
                    "test4");
            fail("Assert equals on token should fail");
        } catch (TokenAssertionError e) {
            // OK
        }
    }

    @Test
    public void testAssertMore() {
        try {
            TokenizerUtils.assertEquals(TokenizerUtils.createTokensList("test1", "test2", "test3"), "test1", "test2", "test3", "test4", "test5");
            fail("Assert equals on token should fail");
        } catch (TokenAssertionError e) {
            // OK
        }
    }

    @Test
    public void testCreateTokenValid() {
        Token[] tokens = TokenizerUtils.createTokens("test", Separator.BACKSLASH, "152",
                EquivalenceClassToken.create("15.25%", EquivalenceClass.PERCENT));
        assertTrue(tokens[0].isWord());
        assertEquals("test", tokens[0].getText());
        assertFalse(tokens[1].isWord());
        assertTrue(tokens[1].isSeparator());
        assertEquals(Separator.BACKSLASH, tokens[1].getSeparator());
        assertEquals("152", tokens[2].getText());
        assertTrue(tokens[2].isWord());
        assertFalse(tokens[2].isSeparator());
        assertEquals("15.25%", tokens[3].getText());
        assertEquals(EquivalenceClass.PERCENT, tokens[3].getEquivalenceClass());
        assertTrue(tokens[3].isEquivalenceClass());
        assertFalse(tokens[3].isWord());
    }

    @Test
    public void testCreateTokenInvalidType() {
        try {
            TokenizerUtils.createTokens("test", 153);
            fail("Create should fail with incorrect type");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}
