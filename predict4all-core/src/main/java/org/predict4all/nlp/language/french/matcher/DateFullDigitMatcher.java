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

public class DateFullDigitMatcher implements TokenMatcher {
    private static final TokenRegexMatcher MATCHER_FULL_YEAR = TokenRegexMatcherBuilder.start().capture("[0-3]?\\d{1}")
            .or(Separator.SLASH, Separator.HYPHEN, Separator.POINT).capture("[0-1]?\\d{1}").or(Separator.SLASH, Separator.HYPHEN, Separator.POINT)
            .capture("\\d{2,4}").build();

    @Override
    public PatternMatched match(Token token, TokenProvider tokenFis) throws IOException {
        TokenRegexResult match = TermMatcherUtils.matchRegexPattern(token, MATCHER_FULL_YEAR, tokenFis, 0);
        if (match != null) {
            int dayValue = Integer.parseInt(match.getExtractedValue(0));
            int monthValue = Integer.parseInt(match.getExtractedValue(1));
            int yearValue = Integer.parseInt(match.getExtractedValue(2));
            // TODO : should check day/month value conditions ?
            return new PatternMatched(EquivalenceClass.DATE_FULL_DIGIT,
                    FrenchLanguageUtils.TWO_DIGIT_FORMAT_ALWAYS.format(dayValue) + Separator.SLASH.getOfficialChar()
                            + FrenchLanguageUtils.TWO_DIGIT_FORMAT_ALWAYS.format(monthValue) + Separator.SLASH.getOfficialChar()
                            + FrenchLanguageUtils.FOUR_DIGIT_FORMAT_ALWAYS.format(FrenchLanguageUtils.convertWrittenYearToExactYear(yearValue)),
                    match.getLastMatchedToken());
        }
        return null;
    }
}
