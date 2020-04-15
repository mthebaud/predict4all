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

package org.predict4all.nlp.parser.matcher;

import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.token.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TermMatcherUtils {
    private TermMatcherUtils() {
    }

    public static TokenRegexResult matchRegexPattern(Token token, TokenRegexMatcher tokenRegexMatcher, TokenProvider tokenProvider, int flags)
            throws IOException {
        List<String> values = new ArrayList<>();
        Token current = token;
        Token lastMatchedToken = current;
        while (tokenRegexMatcher != null) {
            Pattern pattern = getPattern(tokenRegexMatcher.getRegex(), flags);
            if (current != null || tokenRegexMatcher.isOptional()) {
                //Region matched : go to the next token
                if (current != null && pattern.matcher(current.getText()).matches()) {
                    if (tokenRegexMatcher.isCaptureValue()) {
                        values.add(current.getText());
                    }
                    lastMatchedToken = current;
                    current = current.getNext(tokenProvider);
                }
                //Region didn't match, and it's not optional : failed
                else if (!tokenRegexMatcher.isOptional()) {
                    return null;
                }
                //If region didn't match but it's optional, we don't go to the next token
            } else {
                return null;
            }
            tokenRegexMatcher = tokenRegexMatcher.getNext();
        }
        return new TokenRegexResult(lastMatchedToken, values);
    }

    // PATTERN CACHING
    //========================================================================
    private static Map<String, Pattern> CACHED_PATTERN = new HashMap<>(20);

    public static Pattern getPattern(String regex, int flags) {
        if (CACHED_PATTERN.containsKey(regex)) {
            return CACHED_PATTERN.get(regex);
        } else {
            Pattern pattern = Pattern.compile(regex, flags);
            CACHED_PATTERN.put(regex, pattern);
            return pattern;
        }
    }
    //========================================================================
}
