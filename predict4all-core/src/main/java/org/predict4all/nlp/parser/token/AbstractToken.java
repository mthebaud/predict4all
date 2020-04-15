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

package org.predict4all.nlp.parser.token;

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.words.WordDictionary;

import java.io.IOException;

abstract class AbstractToken implements Token {

    private Token next;
    private int cachedWordId = -1;

    @Override
    public int getWordId(WordDictionary dictionary) {
        // Try to find the word id if not cached yet or if cached was unknown
        if (this.cachedWordId < 0 || this.cachedWordId == Tag.UNKNOWN.getId()) {
            // NGram tag : get ID
            if (this.isTag()) {
                this.cachedWordId = getTag().getId();
            }
            // Equivalence class ID
            else if (this.isEquivalenceClass()) {
                this.cachedWordId = getEquivalenceClass().getId();
            }
            // Try to get the word ID (or a lower case version of the word)
            else {
                final String text = getText();
                this.cachedWordId = dictionary.getWordId(text);
                if (this.cachedWordId == Tag.UNKNOWN.getId() && Predict4AllUtils.containsUpperCase(text)) {
                    this.cachedWordId = dictionary.getWordId(Predict4AllUtils.lowerCase(text));
                }
            }
        }
        return this.cachedWordId;
    }

    protected AbstractToken() {
        super();
    }

    public String getText() {
        return null;
    }

    public Separator getSeparator() {
        return null;
    }

    @Override
    public Token getNext(TokenProvider nextTokenProvider) throws IOException {
        if (this.next == null) {
            this.next = nextTokenProvider.getNext();
        }
        return this.next;
    }

    @Override
    public void clearNextCache() {
        this.next = null;
    }

    public boolean isWord() {
        return false;
    }

    @Override
    public String getTextForType() {
        return isSeparator() ? this.getSeparator().getOfficialCharString() : this.getText();
    }

    public boolean isSeparator() {
        return false;
    }

    @Override
    public boolean isEquivalenceClass() {
        return false;
    }

    @Override
    public EquivalenceClass getEquivalenceClass() {
        return null;
    }

    @Override
    public boolean isTag() {
        return false;
    }

    @Override
    public Tag getTag() {
        return null;
    }

    @Override
    public String toString() {
        return "(\"" + this.getText() + "\"/" + this.getSeparator() + "/" + this.getEquivalenceClass() + ")";
    }
}
