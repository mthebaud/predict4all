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
import org.predict4all.nlp.language.french.FrenchLanguageUtils;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.matcher.*;
import org.predict4all.nlp.parser.matcher.TokenRegexMatcher.TokenRegexMatcherBuilder;
import org.predict4all.nlp.parser.token.Token;

import java.io.IOException;
import java.util.regex.Pattern;

public class DateDayMonthMatcher implements TokenMatcher {
    private static final TokenRegexMatcher MATCHER_SIMPLE = TokenRegexMatcherBuilder.start().capture("[0-3]?\\d{1}").then(Separator.SPACE)
            .capture(FrenchLanguageUtils.getMonthOrRegex()).build();

    @Override
    public PatternMatched match(Token token, TokenProvider tokenFis) throws IOException {
        TokenRegexResult match = TermMatcherUtils.matchRegexPattern(token, MATCHER_SIMPLE, tokenFis, Pattern.CASE_INSENSITIVE);
        if (match != null) {
            int dayValue = Integer.parseInt(match.getExtractedValue(0));
            return new PatternMatched(
                    EquivalenceClass.DATE_DAY_MONTH, FrenchLanguageUtils.TWO_DIGIT_FORMAT_SOMETIMES.format(dayValue)
                    + Separator.SPACE.getOfficialChar() + FrenchLanguageUtils.MONTHS.get(match.getExtractedValue(1).toLowerCase()),
                    match.getLastMatchedToken());
        }
        return null;
    }
}
