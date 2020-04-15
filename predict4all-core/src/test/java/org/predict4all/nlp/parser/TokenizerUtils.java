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
import org.predict4all.nlp.parser.token.EquivalenceClassToken;
import org.predict4all.nlp.parser.token.SeparatorToken;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.parser.token.WordToken;

import java.util.Arrays;
import java.util.List;

public class TokenizerUtils {
    private TokenizerUtils() {
    }

    public static void assertEquals(List<Token> content, Object... expected) {
        // Convert to TextToken
        Token[] expectedTokens = createTokens(expected);
        int i = 0;

        for (Token current : content) {
            // Less expected than actual
            if (i >= expectedTokens.length) {
                throw new TokenAssertionError("There is more actual result than expected, expected count is " + expectedTokens.length);
            }
            if (!current.equals(expectedTokens[i])) {
                throw new TokenAssertionError("On index " + i + "\n\texpected : " + expectedTokens[i] + "\n\tactual : " + current);
            }
            i++;
        }
        // More expected than actual (we are at the end, but there is no "actual" left)
        if (i <= expectedTokens.length - 1) {
            throw new TokenAssertionError(
                    "There is more expected result than actual, expected count is " + expectedTokens.length + ", actual count is " + content.size());
        }
    }

    public static Token[] createTokens(Object... tokens) {
        // Convert to TextToken
        Token[] converted = new Token[tokens.length];
        for (int j = 0; j < tokens.length; j++) {
            if (tokens[j] instanceof String) {
                converted[j] = WordToken.create((String) tokens[j]);
            } else if (tokens[j] instanceof Separator) {
                converted[j] = SeparatorToken.create((Separator) tokens[j]);
            } else if (tokens[j] instanceof EquivalenceClassToken) {
                converted[j] = (Token) tokens[j];
            } else {
                throw new IllegalArgumentException("Invalid given type : " + tokens[j].getClass());
            }
        }
        return converted;
    }

    public static List<Token> createTokensList(Object... tokens) {
        return Arrays.asList(createTokens(tokens));
    }
}
