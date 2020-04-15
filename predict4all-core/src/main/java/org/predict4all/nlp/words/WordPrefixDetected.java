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

package org.predict4all.nlp.words;

import org.predict4all.nlp.utils.BiIntegerKey;

import java.util.Map;

/**
 * Contains information about a started word (found in dictionary)
 *
 * @author Mathieu THEBAUD
 */
public class WordPrefixDetected {

    /**
     * Prefix detected that was used to create the match
     */
    private final String longestWordPrefix;

    /**
     * To indicate that the prefix should be considered upper case
     */
    private boolean capitalizedWord;

    /**
     * Number of token for the word match (can be > 1 if word contains separators, etc...)
     * <strong>WARNING</strong> : this will be 0 if the longest word prefix is the unique found word.
     */
    private final int tokenCount;

    /**
     * Contains the next words for a matching result.<br>
     * Contains unique or double word combination that will be used in prediction.
     */
    private Map<BiIntegerKey, NextWord> words;

    public WordPrefixDetected(String longestWordPrefix, int tokenCount, Map<BiIntegerKey, NextWord> words, final boolean capitalizedWord) {
        super();
        this.longestWordPrefix = longestWordPrefix;
        this.words = words;
        this.capitalizedWord = capitalizedWord;
        this.tokenCount = tokenCount;
    }

    public String getLongestWordPrefix() {
        return longestWordPrefix;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public boolean isCapitalizedWord() {
        return capitalizedWord;
    }

    public Map<BiIntegerKey, NextWord> getWords() {
        return words;
    }

    @Override
    public String toString() {
        return "WordPrefixDetected [longestWordPrefix=" + longestWordPrefix + ", capitalizedWord=" + capitalizedWord + ", tokenCount=" + tokenCount
                + ", words=" + words + "]";
    }
}
