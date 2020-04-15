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

package org.predict4all.nlp.language.french.matcher;

import org.predict4all.nlp.Separator;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.matcher.*;
import org.predict4all.nlp.parser.matcher.TokenRegexMatcher.TokenRegexMatcherBuilder;
import org.predict4all.nlp.parser.token.Token;

import java.io.IOException;
import java.util.regex.Pattern;

public class SpecialWordMatcher implements TokenMatcher {

    private static final TokenRegexMatcher WORD_TODAY = TokenRegexMatcherBuilder.start().then(Pattern.quote("aujourd")).then(Separator.APOSTROPHE)
            .then("hui").build();

    @Override
    public PatternMatched match(Token token, TokenProvider tokenFis) throws IOException {
        TokenRegexResult match = TermMatcherUtils.matchRegexPattern(token, WORD_TODAY, tokenFis, Pattern.CASE_INSENSITIVE);
        if (match != null) {
            return new PatternMatched("aujourd'hui", match.getLastMatchedToken());
        }
        return null;
    }
}
