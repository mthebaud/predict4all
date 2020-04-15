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

package org.predict4all.nlp.ngram;

import org.predict4all.nlp.Tag;
import org.predict4all.nlp.parser.token.TagToken;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.parser.token.WordToken;
import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.utils.Triple;
import org.predict4all.nlp.words.WordDictionary;
import org.predict4all.nlp.words.WordPrefixDetected;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utils class useful when predicting words with an ngram dictionaries.
 *
 * @author Mathieu THEBAUD
 */
public class NGramWordPredictorUtils {
    private static final int NGRAM_MAX_LAST_TOKEN_COUNT_FACTOR = 4;

    /**
     * Word dictionary associated with ngram dictionaries
     */
    private final WordDictionary wordDictionary;

    /**
     * Prediction parameter
     */
    private final PredictionParameter predictionParameter;

    public NGramWordPredictorUtils(WordDictionary wordDictionary, PredictionParameter predictionParameter) {
        super();
        this.wordDictionary = wordDictionary;
        this.predictionParameter = predictionParameter;
    }

    // PUBLIC API
    // ========================================================================

    /**
     * Create the prefix for a given raw context (token list) : the context is meant to be used for ngram trie exploring.<br>
     * The context takes care of using only the last sentence, to detect the current written word, and to retrieve a context of the wanted order.<br>
     *
     * @param rawTokensList              raw token list (retrieved from parsing a raw text input)
     * @param wordPrefixFound            if a word is started, should contains the started prefix
     * @param wantedOrder                the wanted prefix order
     * @param addUnknownWordToDictionary if true, when a word in the given token list is unknown, the method {@link WordDictionary#putUserWord(Token)} will be called to retrieve the word id
     * @return a triple containing the prefix on left : containing the last (wantedOrder-1) words in the given token list.<br>
     * Resulting prefix will contains {@link Tag#UNKNOWN} if the token list contains unknown word and addUnknownWordToDictionary is false, and will contains {@link Tag#START} if the given token list is not long enough, or if the current
     * sentence is too short.<br>
     * The boolean on middle is true if there is only "START" tag in the resulting prefix<br>
     * The boolean on right is true if we found a unknown word in the prefix (even if it's added to dictionary)<br>
     */
    public Triple<int[], Boolean, Boolean> createPrefixFor(final List<Token> rawTokensList, WordPrefixDetected wordPrefixFound, final int wantedOrder, final boolean addUnknownWordToDictionary) {
        // If a word is started, we should freeze the end of the token list for each token that compose the word
        final int lastTokenToFreezeCount = wordPrefixFound != null ? wordPrefixFound.getTokenCount() : 0;

        // Create a sublist that only keep the minimum token count (optimization)
        List<Token> tokens = createTokenListForPrediction(rawTokensList, lastTokenToFreezeCount, wantedOrder);

        // Keep only the last sentence to execute prediction
        keepOnlyLastSentenceAndLowerCase(tokens, lastTokenToFreezeCount);

        // Remove all the separator from list, but keep the last tokens if needed
        removeSeparatorsFromTokenList(tokens, lastTokenToFreezeCount);

        // Complete with start tag when needed
        while (tokens.size() - lastTokenToFreezeCount + 1 < wantedOrder) {
            tokens.add(0, TagToken.create(Tag.START));
        }

        // Create context list : n-1 last words
        boolean foundUnknown = false;
        boolean onlyStartTag = true;
        int[] contextPrefix = new int[wantedOrder - 1];
        int tokenIndexStart = tokens.size() - wantedOrder - lastTokenToFreezeCount + 1;
        for (int tokenIndex = 0; tokenIndex < wantedOrder - 1; tokenIndex++) {
            // Get token
            final Token token = tokens.get(tokenIndexStart + tokenIndex);
            contextPrefix[tokenIndex] = Tag.UNKNOWN.getId();

            // Get word from dictionary
            final int wordId = token.getWordId(wordDictionary);
            foundUnknown |= wordId == Tag.UNKNOWN.getId();

            // If word is unknown and could be used to create a user word (excluding first and last tokens)
            if (wordId == Tag.UNKNOWN.getId() && tokenIndexStart + tokenIndex > 0 && (wordPrefixFound == null || tokenIndex < wantedOrder - 2)
                    && addUnknownWordToDictionary && wordDictionary.isTokenValidToCreateUserWord(token)) {
                contextPrefix[tokenIndex] = this.wordDictionary.putUserWord(token);
            } else {
                contextPrefix[tokenIndex] = wordId;
            }
            onlyStartTag &= wordId == Tag.START.getId();
        }
        return Triple.of(contextPrefix, onlyStartTag, foundUnknown);

    }
    // ========================================================================


    // PRIVATE
    //========================================================================

    /**
     * Go through tokens and keep only the last sentence in the given token list (remove every sentence before)
     *
     * @param tokens                 the tokens list
     * @param lastTokenToFreezeCount the number of last token to keep
     */
    private void keepOnlyLastSentenceAndLowerCase(List<Token> tokens, int lastTokenToFreezeCount) {
        // Go back to search the last sentence separator (except lastTokenToFreezeCount)
        int lastSentenceSeparatorIndex = -1;
        for (int i = tokens.size() - 1 - lastTokenToFreezeCount; i >= 0; i--) {
            if (tokens.get(i).isSeparator() && tokens.get(i).getSeparator().isSentenceSeparator()) {
                lastSentenceSeparatorIndex = i;
                break;
            }
        }
        // Delete all items before last sentence start
        int i = 0;
        Iterator<Token> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            if (i <= lastSentenceSeparatorIndex) {
                iterator.remove();
            } else {
                break;
            }
            i++;
        }
        // The first token become lower case (if needed)
        if (!tokens.isEmpty()) {
            Token firstToken = tokens.get(0);
            if (firstToken.isWord() && Predict4AllUtils.isCapitalized(firstToken.getText())) {
                tokens.set(0, WordToken.create(Predict4AllUtils.lowerCase(firstToken.getText())));
            }
        }
    }

    /**
     * Remove all the separator tokens from the list, but keep the last token count
     *
     * @param rawTokensList token list where separator will be removed
     * @param keepLastCount the number of last token to keep
     */
    private void removeSeparatorsFromTokenList(List<Token> rawTokensList, int keepLastCount) {
        Iterator<Token> iterator = rawTokensList.iterator();
        int stopOnIndex = rawTokensList.size() - keepLastCount;
        int i = 0;
        while (iterator.hasNext() && i < stopOnIndex) {
            if (iterator.next().isSeparator()) {
                iterator.remove();
            }
            i++;
        }
    }

    /**
     * Create a list of token from the original raw token list, that will only keep the x last tokens.
     *
     * @param rawTokensList the raw token list (source)
     * @param keepLastCount the token count to keep (plus the initial computed count).
     * @param ngramMaxOrder the max order in the ngram dictionary
     * @return a sub list of the given list, that only contains the minimum needed token count (raw token list).
     */
    private ArrayList<Token> createTokenListForPrediction(final List<Token> rawTokensList, int keepLastCount, int ngramMaxOrder) {
        return new ArrayList<>(rawTokensList.subList(
                Math.max(0, rawTokensList.size() - keepLastCount - NGRAM_MAX_LAST_TOKEN_COUNT_FACTOR * ngramMaxOrder),
                rawTokensList.size()));
    }
    //========================================================================
}
