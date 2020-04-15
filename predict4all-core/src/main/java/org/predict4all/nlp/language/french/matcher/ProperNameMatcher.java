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

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.matcher.*;
import org.predict4all.nlp.parser.matcher.TokenRegexMatcher.TokenRegexMatcherBuilder;
import org.predict4all.nlp.parser.token.Token;

import java.io.IOException;

public class ProperNameMatcher implements TokenMatcher {

    private static final String REGEX_PROPER_NAME = "\\p{Lu}\\p{L}+";

    private static final TokenRegexMatcher PROPER_NAME = TokenRegexMatcherBuilder.start().capture(REGEX_PROPER_NAME).build();

    private static final TokenRegexMatcher PROPER_NAME_TWO_PART = TokenRegexMatcherBuilder.start().capture(REGEX_PROPER_NAME).then(Separator.SPACE)
            .capture(REGEX_PROPER_NAME).build();

    private static final TokenRegexMatcher PROPER_NAME_THREE_PART = TokenRegexMatcherBuilder.start().capture(REGEX_PROPER_NAME).then(Separator.SPACE)
            .capture(REGEX_PROPER_NAME).then(Separator.SPACE).capture(REGEX_PROPER_NAME).build();

    private static final TokenRegexMatcher PROPER_NAME_FOUR_PART = TokenRegexMatcherBuilder.start().capture(REGEX_PROPER_NAME).then(Separator.SPACE)
            .capture(REGEX_PROPER_NAME).then(Separator.SPACE).capture(REGEX_PROPER_NAME).then(Separator.SPACE).capture(REGEX_PROPER_NAME).build();

    private static final TokenRegexMatcher[] MATCHERS = {PROPER_NAME_FOUR_PART, PROPER_NAME_THREE_PART, PROPER_NAME_TWO_PART, PROPER_NAME};

    @Override
    public PatternMatched match(Token token, TokenProvider tokenFis) throws IOException {
        for (TokenRegexMatcher regexMatcher : MATCHERS) {
            TokenRegexResult match = TermMatcherUtils.matchRegexPattern(token, regexMatcher, tokenFis, 0);
            if (match != null) {
                return new PatternMatched(EquivalenceClass.PROPER_NAME,
                        String.join(Separator.SPACE.getOfficialCharString(), match.getExtractedTokenValues()), match.getLastMatchedToken());
            }
        }
        return null;
    }
}
