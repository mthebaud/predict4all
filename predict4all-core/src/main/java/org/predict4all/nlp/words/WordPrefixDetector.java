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

import org.predict4all.nlp.Separator;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.utils.BiIntegerKey;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.words.correction.WordCorrectionGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Useful to detect if a existing word is started in a token list.<br>
 * It's important to detect if a word is already started when predicting next word,
 * because the prediction result should always takes care of giving prediction result that starts like the already started word.<br>
 * <br>
 * Because word are allowed to have word separator inside (hyphen, etc...), started word detection is much more complicated that just checking if the token list ends with a token separator.
 *
 * @author Mathieu THEBAUD
 */
public class WordPrefixDetector {
    private static final int PREFIX_SEARCH_MAX_TOKEN_COUNT = 5;

    private final WordDictionary wordDictionary;
    private final WordCorrectionGenerator wordCorrectionGenerator;
    private final PredictionParameter predictionParameter;

    public WordPrefixDetector(WordDictionary wordDictionary, WordCorrectionGenerator wordCorrectionGenerator,
                              PredictionParameter predictionParameter) {
        super();
        this.wordDictionary = wordDictionary;
        this.wordCorrectionGenerator = wordCorrectionGenerator;
        this.predictionParameter = predictionParameter;
    }

    /**
     * Try to detect if the end of the given sentence finish with a word already started.<br>
     * This is much more precise than just checking if the last token is a separator, because a word could have separator inside (e.g. : "New York" has a space, "là-bas" has a hyphen).
     *
     * @param rawTokensList       list of tokens to take into account (should be as raw as possible, include separators, etc...).
     * @param wantedNextWordCount the word count wanted (used to optimize computing)
     * @param wordIdsToExclude    words to exclude from result (by ids)
     * @return null if no word is started, or {@link WordPrefixDetected} that contains information on the found prefix
     */
    public WordPrefixDetected getLongestMatchingWords(List<Token> rawTokensList, int wantedNextWordCount, Set<Integer> wordIdsToExclude) {
        Map<BiIntegerKey, NextWord> result = null;

        StringBuilder accumulation = new StringBuilder();
        String lastCorrectionWordStart = null;
        int tokenLength = 0;

        /*
         * Detect if a new sentence started : if a new sentence is started, this means that we will have to search lower case version of current prefix because sentence starts with a capitalized word.
         * If not, we search for a word with the exact matching case (because capitalized is intentional)
         */
        boolean newSentenceStarted = isNewSentenceStarted(rawTokensList, 1);

        /*
         * Go back in the string
         * The end index is computed to avoid going back too far in the list.
         */
        int endIndex = Math.max(0, rawTokensList.size() - PREFIX_SEARCH_MAX_TOKEN_COUNT);
        for (int i = rawTokensList.size() - 1; i >= endIndex; i--) {

            // Create string for accumulation
            Token token = rawTokensList.get(i);
            accumulation.insert(0, token.getTextForType());

            // Consider that words never contains any space... // TODO : should be configured and not just set
            if (token.getSeparator() != Separator.SPACE) {
                // Check if a word starts with the current accumulation (try to correct it if correction is enabled)
                String accumulationStr = newSentenceStarted ? Predict4AllUtils.lowerCase(accumulation.toString()) : accumulation.toString();
                // multipleTokens : i < rawTokensList.size() - 1 (removed)
                Map<BiIntegerKey, NextWord> wordByPrefix = wordDictionary.getValidWordForPredictionByPrefix(accumulationStr, this.predictionParameter, wantedNextWordCount, wordIdsToExclude);
                // Try correction on first token only (and if it's enabled)
                if (predictionParameter.isEnableWordCorrection() && predictionParameter.getCorrectionMaxCost() > 0 && rawTokensList.size() - i <= 1) {
                    this.wordCorrectionGenerator.addCorrectionsFor(accumulationStr, wordByPrefix, wordIdsToExclude);
                }
                if (!wordByPrefix.isEmpty()) {
                    // Save the result
                    result = wordByPrefix;
                    lastCorrectionWordStart = accumulation.toString();//accumulationStr; - was using lower cased string
                    tokenLength = rawTokensList.size() - i;
                }
                // If there was a result but the accumulation after doesn't, stop to the previous result
                else if (result != null) {
                    break;
                }
            } else {
                break;
            }
        }

        // Word from dictionary matched
        if (lastCorrectionWordStart != null) {
            return new WordPrefixDetected(lastCorrectionWordStart, tokenLength, result,
                    isNextWordsCapitalized(rawTokensList, lastCorrectionWordStart, tokenLength));
        } else if (!rawTokensList.isEmpty()) {
            // Not a word from dictionary, but a word is started (doesn't finish with a separator)
            Token lastToken = rawTokensList.get(rawTokensList.size() - 1);
            if (!lastToken.isSeparator()) {
                return new WordPrefixDetected(lastToken.getText(), 1, new HashMap<>(), isNextWordsCapitalized(rawTokensList, lastToken.getText(), 1));
            }
        }
        // Not a word at all (known or unknown)
        return null;
    }

    //	public boolean isNextWordsFullUppercase(List<Token> tokens, String longestMatchingWords) {
    //		// Full upper case : only if a word is already started and upper case (more than the first char)
    //		return longestMatchingWords != null && longestMatchingWords.length() > 1 && Predict4AllUtils.isFullUpperCase(longestMatchingWords);
    //	}

    public boolean isNextWordsCapitalized(List<Token> tokens, String longestMatchingWords, int tokenCount) {
        // Word is started : capitalize only if the currently started prefix is capitalized
        if (longestMatchingWords != null) {
            return Predict4AllUtils.isCapitalized(longestMatchingWords);
        }
        // Word is not started : capitalize if a new sentence is started
        else {
            return isNewSentenceStarted(tokens, tokenCount);
        }
    }

    /**
     * Check if a new sentence started.<br>
     * We consider that a new sentence started for the following conditions (OR) :
     * <ul>
     * <li>An empty text is given (= no tokens)</li>
     * <li>The last character is a sentence separator</li>
     * <li>A word is started, but all character before are separator, and ends with a sentence separator</li>
     * </ul>
     * Some example of started sentence : "the", "my end. ", "this is it. Th"
     *
     * @param tokens     token list
     * @param tokenCount token count
     * @return true if a sentence started.
     */
    private boolean isNewSentenceStarted(List<Token> tokens, int tokenCount) {
        if (!tokens.isEmpty()) {
            int fromIndex = tokens.size() - 1 - tokenCount;
            if (fromIndex >= 0) {
                for (int i = fromIndex; i >= 0; i--) {
                    Token token = tokens.get(i);
                    if (!token.isSeparator()) {
                        return false;
                    } else if (token.getSeparator().isSentenceSeparator()) {
                        return true;
                    }
                }
            }
        }
        return true;
    }
}
