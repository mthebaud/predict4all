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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.parser.TokenizerUtils;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.utils.BiIntegerKey;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.words.correction.WordCorrectionGenerator;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(BlockJUnit4ClassRunner.class)
public class WordPrefixDetectorTest {
    private static final Set<Integer> EMPTY_INT_HASHSET = new HashSet<>();
    private WordDictionary wordDictionary;
    private WordPrefixDetector wordPrefixDetector;
    private WordCorrectionGenerator wordCorrectionGenerator;
    private PredictionParameter predictionParameter;

    @Before
    public void setUp() {
        this.wordDictionary = mock(WordDictionary.class);
        wordCorrectionGenerator = mock(WordCorrectionGenerator.class);
        predictionParameter = mock(PredictionParameter.class);
        wordPrefixDetector = new WordPrefixDetector(wordDictionary, wordCorrectionGenerator, predictionParameter);
    }

    //
    @Test
    public void testSimpleNoWordStartWithSeparatorEnd() {
        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("un", Separator.SPACE, "mot", Separator.SPACE));
        assertNull(this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET));
    }

    @Test
    public void testSimpleWordStartWithUnknownWord() {
        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("un", Separator.SPACE, "mot", Separator.SPACE, "inco"));
        final WordPrefixDetected match = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);
        assertNotNull(match);
        assertEquals("inco", match.getLongestWordPrefix());
        assertEquals(1, match.getTokenCount());
        assertEquals(0, match.getWords().size());
    }

    @SafeVarargs
    private static Map<BiIntegerKey, NextWord> createMap(Pair<BiIntegerKey, NextWord>... nextWords) {
        Map<BiIntegerKey, NextWord> map = new HashMap<>();
        for (Pair<BiIntegerKey, NextWord> pair : nextWords) {
            map.put(pair.getLeft(), pair.getRight());
        }
        return map;
    }

    @Test
    public void testSimpleWordStartWithSimpleWord() {
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("inco"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));

        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("un", Separator.SPACE, "mot", Separator.SPACE, "inco"));
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);

        assertNotNull(longestMatchingWords);
        assertEquals("inco", longestMatchingWords.getLongestWordPrefix());
        assertEquals(1, longestMatchingWords.getTokenCount());
        assertEquals(1, longestMatchingWords.getWords().size());
        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(150)));
        assertEquals(150, longestMatchingWords.getWords().get(BiIntegerKey.of(150)).getWordId1());
    }

    @Test
    public void testSimpleWordStartWithSimpleWordDouble() {
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("New"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(160), NextWord.createUnique(160, 1.0, false, null)),
                        Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));

        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("à", Separator.SPACE, "New"));
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);

        assertNotNull(longestMatchingWords);
        assertEquals("New", longestMatchingWords.getLongestWordPrefix());
        assertEquals(1, longestMatchingWords.getTokenCount());
        assertEquals(2, longestMatchingWords.getWords().size());

        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(150)));
        assertEquals(150, longestMatchingWords.getWords().get(BiIntegerKey.of(150)).getWordId1());
        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(160)));
        assertEquals(160, longestMatchingWords.getWords().get(BiIntegerKey.of(160)).getWordId1());
    }

    @Test
    public void testSimpleWordStartWithDoubleWord() {
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("New"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(160), NextWord.createUnique(160, 1.0, false, null)),
                        Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("New-"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));

        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("à", Separator.SPACE, "New", Separator.HYPHEN));
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);

        assertNotNull(longestMatchingWords);
        assertEquals("New-", longestMatchingWords.getLongestWordPrefix());
        assertEquals(2, longestMatchingWords.getTokenCount());
        assertEquals(1, longestMatchingWords.getWords().size());
        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(150)));
        assertEquals(150, longestMatchingWords.getWords().get(BiIntegerKey.of(150)).getWordId1());
    }

    @Test
    public void testSimpleWordStartSpecialWord() {
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("aujourd"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));

        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("aujourd"));
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);

        assertNotNull(longestMatchingWords);
        assertEquals(1, longestMatchingWords.getTokenCount());
        assertEquals(1, longestMatchingWords.getWords().size());
        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(150)));
        assertEquals(150, longestMatchingWords.getWords().get(BiIntegerKey.of(150)).getWordId1());
    }

    @Test
    public void testSimpleWordStartSpecialWordWithSepEnd() {
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("aujourd'"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));

        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("aujourd", Separator.APOSTROPHE));
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);

        assertNotNull(longestMatchingWords);
        assertEquals(2, longestMatchingWords.getTokenCount());
        assertEquals(1, longestMatchingWords.getWords().size());
        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(150)));
        assertEquals(150, longestMatchingWords.getWords().get(BiIntegerKey.of(150)).getWordId1());
    }

    @Test
    public void testWordStartedSentenceUpperCase() {
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("bonj"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));

        List<Token> tokens = Arrays.asList(TokenizerUtils.createTokens("Bonj"));
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);

        assertNotNull(longestMatchingWords);
        assertEquals(1, longestMatchingWords.getTokenCount());
        assertEquals(1, longestMatchingWords.getWords().size());
        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(150)));
        assertEquals(150, longestMatchingWords.getWords().get(BiIntegerKey.of(150)).getWordId1());
    }

    @Test
    public void testWordStartedComplexSentenceUpperCase() {
        when(this.wordDictionary.getValidWordForPredictionByPrefix(eq("bonj"), any(), anyInt(), any()))
                .thenReturn(createMap(Pair.of(BiIntegerKey.of(150), NextWord.createUnique(150, 1.0, false, null))));

        List<Token> tokens = Arrays.asList(
                TokenizerUtils.createTokens("test", Separator.POINT, Separator.POINT, Separator.POINT, Separator.SPACE, Separator.SPACE, "Bonj"));
        WordPrefixDetected longestMatchingWords = this.wordPrefixDetector.getLongestMatchingWords(tokens, 100, EMPTY_INT_HASHSET);

        assertNotNull(longestMatchingWords);
        assertEquals(1, longestMatchingWords.getTokenCount());
        assertEquals(1, longestMatchingWords.getWords().size());
        assertNotNull(longestMatchingWords.getWords().get(BiIntegerKey.of(150)));
        assertEquals(150, longestMatchingWords.getWords().get(BiIntegerKey.of(150)).getWordId1());
    }

}
