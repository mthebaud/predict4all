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

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.token.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractRecursiveMatcher implements TokenMatcher {
    private final boolean allowEndWithSeparator;
    private final Separator termSeparator;
    private final String wordRegex;
    private final EquivalenceClass equivalenceClass;

    public AbstractRecursiveMatcher(boolean allowEndWithSeparator, Separator tokenSeparator, String wordRegex, EquivalenceClass equivalenceClass) {
        super();
        this.allowEndWithSeparator = allowEndWithSeparator;
        this.termSeparator = tokenSeparator;
        this.wordRegex = wordRegex;
        this.equivalenceClass = equivalenceClass;
    }

    public AbstractRecursiveMatcher(boolean allowEndWithSeparator, Separator termSeparator, String wordRegex) {
        this(allowEndWithSeparator, termSeparator, wordRegex, null);
    }

    @Override
    public PatternMatched match(Token token, TokenProvider tokenFis) throws IOException {
        Pattern pattern = TermMatcherUtils.getPattern(this.wordRegex, 0);
        List<String> founds = new ArrayList<>(4);
        boolean wantWord = true;
        Token lastMatchToken = null;

        // Continue while we found alternation of wanted word then separator
        while (token != null) {
            if (wantWord && token.isWord() && pattern.matcher(token.getText()).matches()) {
                founds.add(token.getText());
                lastMatchToken = token;
                wantWord = false;
            } else if (!wantWord && token.isSeparator() && token.getSeparator() == termSeparator) {
                wantWord = true;
                if (this.allowEndWithSeparator) {
                    lastMatchToken = token;
                }
            } else {
                break;
            }
            token = token.getNext(tokenFis);
        }

        // Should have found at least two words, and check if finished with separator is allowed
        if (founds.size() > 1 && (this.allowEndWithSeparator || !wantWord)) {
            return new PatternMatched(equivalenceClass, this.createMatchedString(founds), lastMatchToken);
        }
        return null;
    }

    protected abstract String createMatchedString(List<String> words);
}
