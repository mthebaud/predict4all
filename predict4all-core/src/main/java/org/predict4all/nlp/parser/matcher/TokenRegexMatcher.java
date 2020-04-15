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

import org.predict4all.nlp.Separator;

import java.util.regex.Pattern;

public class TokenRegexMatcher {
    private final String regex;
    private final boolean optional;
    private final boolean captureValue;
    private TokenRegexMatcher next;

    private TokenRegexMatcher(String regex, boolean optional, boolean captureValue) {
        super();
        this.regex = regex;
        this.optional = optional;
        this.captureValue = captureValue;
    }

    public String getRegex() {
        return regex;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isCaptureValue() {
        return captureValue;
    }

    public TokenRegexMatcher getNext() {
        return next;
    }

    public static class TokenRegexMatcherBuilder {
        private TokenRegexMatcher first;
        private TokenRegexMatcher current;

        public static TokenRegexMatcherBuilder start() {
            return new TokenRegexMatcherBuilder();
        }

        public TokenRegexMatcher build() {
            return this.first;
        }

        private TokenRegexMatcherBuilder addMatcher(TokenRegexMatcher matcher) {
            if (current != null) {
                current.next = matcher;
            }
            current = matcher;
            if (first == null) {
                first = current;
            }
            return this;
        }

        public TokenRegexMatcherBuilder then(String regex) {
            return addMatcher(new TokenRegexMatcher(regex, false, false));
        }

        public TokenRegexMatcherBuilder then(Separator stopChar) {
            return addMatcher(new TokenRegexMatcher(Pattern.quote(stopChar.getOfficialCharString()), false, false));
        }

        public TokenRegexMatcherBuilder optional(String regex) {
            return addMatcher(new TokenRegexMatcher(regex, true, false));
        }

        public TokenRegexMatcherBuilder optional(Separator stopChar) {
            return optional(Pattern.quote(stopChar.getOfficialCharString()));
        }

        public TokenRegexMatcherBuilder capture(String regex) {
            return addMatcher(new TokenRegexMatcher(regex, false, true));
        }

        public TokenRegexMatcherBuilder or(Separator... stopChars) {
            StringBuilder then = new StringBuilder();
            for (Separator stopChar : stopChars) {
                then.append(then.length() > 0 ? "|" : "");
                then.append(Pattern.quote(stopChar.getOfficialCharString()));
            }
            return then(then.toString());
        }

        public TokenRegexMatcherBuilder or(Separator c1, String c2) {
            return then(Pattern.quote(c1.getOfficialCharString()) + "|" + Pattern.quote(c2));
        }

        public TokenRegexMatcherBuilder or(String... values) {
            StringBuilder then = new StringBuilder();
            for (String value : values) {
                then.append(then.length() > 0 ? "|" : "");
                then.append(Pattern.quote(value));
            }
            return then(then.toString());
        }

    }
}
