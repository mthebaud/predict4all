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

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.exception.WordDictionaryMatchingException;
import org.predict4all.nlp.io.WordFileInputStream;
import org.predict4all.nlp.io.WordFileOutputStream;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.utils.BiIntegerKey;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.words.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represent a word dictionary.<br>
 * This dictionary identify each sequence of chars as an unique "word" and keep information for this word.<br>
 * Each word are identified by a single int ID to save memory and space.<br>
 * The dictionary itself is identified with an UUID to verify consistency when using user dictionary.
 *
 * @author Mathieu THEBAUD
 */
public class WordDictionary {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordDictionary.class);

    /**
     * ID Generator : useful to generate ID when words are added to dictionary
     */
    private final AtomicInteger idGenerator;

    /**
     * Map that contains each word by ID
     */
    private final TIntObjectHashMap<Word> wordsById;

    /**
     * Useful to search words by their String
     */
    private final PatriciaTrie<Word> wordTrie;

    /**
     * Unique ID : useful to link dictionary and user dictionary
     */
    private final String dictionaryId;

    public WordDictionary(LanguageModel languageModel, final String dictionaryId) {
        this.wordsById = new TIntObjectHashMap<>(languageModel.getAverageVocabularySize());
        this.wordTrie = new PatriciaTrie<>();
        this.idGenerator = new AtomicInteger(0);
        this.dictionaryId = dictionaryId;
        for (EquivalenceClass equivalenceClass : EquivalenceClass.values()) {
            this.putExistingWord(new EquivalenceClassWord(equivalenceClass));
        }
        for (Tag tag : Tag.values()) {
            this.putExistingWord(new TagWord(tag));
        }
    }

    // SIMPLE API
    // ========================================================================
    public int getWordId(String text) {
        return getWord(text).getID();
    }

    public Word getWord(String text) {
        Word word = this.wordTrie.get(text);
        return word != null ? word : this.wordsById.get(Tag.UNKNOWN.getId());
    }

    public Word getWord(int wordId) {
        return this.wordsById.get(wordId);
    }
    // ========================================================================

    // QUERY API
    // ========================================================================

    /**
     * Returns all the words that starts with a given prefix.<br>
     * The returned list is not sorted.
     *
     * @param wordPrefix          prefix
     * @param predictionParameter prediction parameters (used to validate word to predict)
     * @param wantedWordCount     the number of word wanted (used to stop search)
     * @param wordIdsToExclude    to exclude word from the result set (by id)
     * @return the list of all word starting with the given prefix
     */
    public Map<BiIntegerKey, NextWord> getValidWordForPredictionByPrefix(String wordPrefix, PredictionParameter predictionParameter, int wantedWordCount, Set<Integer> wordIdsToExclude) {
        LOGGER.info("Request for \"{}\"", wordPrefix);

        Map<BiIntegerKey, NextWord> words = new HashMap<>(wantedWordCount);

        // Get base from prefix map : factor = 1.0 because it's exact prefix matching
        searchAndAdd(wordPrefix, predictionParameter, wordIdsToExclude, words, "Du dictionnaire par le début de mot");

        // Try to find a capitalized version (if needed)
        // TODO : prediction configuration
        if (words.size() < wantedWordCount && !Predict4AllUtils.isCapitalized(wordPrefix)) {
            searchAndAdd(Predict4AllUtils.capitalize(wordPrefix), predictionParameter, wordIdsToExclude, words,
                    "Du dictionnaire (version capitalisée)");
        }

        // Try to find a lower case version (if needed)
        // TODO : prediction configuration
        if (words.size() < wantedWordCount && Predict4AllUtils.containsUpperCase(wordPrefix)) {
            searchAndAdd(Predict4AllUtils.lowerCase(wordPrefix), predictionParameter, wordIdsToExclude, words, "Du dictionnaire (version lowercase)");
        }

        return words;
    }

    private void searchAndAdd(String wordPrefix, PredictionParameter predictionParameter, Set<Integer> wordIdsToExclude,
                              Map<BiIntegerKey, NextWord> words, String debugInfo) {
        wordTrie.prefixMap(wordPrefix)//
                .values()//
                .stream()//
                .filter(w -> w.isValidToBePredicted(predictionParameter) && !wordIdsToExclude.contains(w.getID()))//
                .forEach(w ->
                        words.put(BiIntegerKey.of(w.getID()), NextWord.createUnique(w.getID(), 1.0, false,
                                predictionParameter.isEnableDebugInformation() ? new StringBuilder(debugInfo) : null))
                );
    }

    public Collection<Word> getAllWords() {
        return wordsById.valueCollection();
    }

    public int size() {
        return this.wordsById.size();
    }

    public boolean isExactWordWithPrefixExist(String prefix) {
        SortedMap<String, Word> prefixMap = wordTrie.prefixMap(prefix);
        return prefixMap != null && !prefixMap.isEmpty();
    }

    public SortedMap<String, Word> getExactWordsWithPrefixExist(String prefix) {
        return wordTrie.prefixMap(prefix);
    }

    public int getIDGeneratorState() {
        return this.idGenerator.get();
    }
    // ========================================================================

    // INSERT API
    // ========================================================================
    public int putUserWord(Token token) {
        return this.putNewWordToDictionary(new UserWord(this.generateWordID(), token.getText()));
    }

    public Word putUserWord(String word) {
        UserWord cWord = new UserWord(this.generateWordID(), word);
        this.putNewWordToDictionary(cWord);
        return cWord;
    }

    public void incrementUserWord(int wordId) {
        Word word = this.wordsById.get(wordId);
        if (word != null && word.isUserWord()) {
            UserWord uW = (UserWord) word;
            uW.incrementUsageCount();
        }
    }

    public boolean isWordExists(String text) {
        return this.wordTrie.get(text) != null;
    }

    // TODO : method to "pseudo validate" new words (length, special char, etc...)
    public boolean isTokenValidToCreateUserWord(Token token) {
        String txt = token.getText();
        if (Predict4AllUtils.length(txt) > 2) {
            return true;
        }
        return false;
    }

    public int putWordTraining(String word) {
        return this.putNewWordToDictionary(SimpleWord.create(this.generateWordID(), word));
    }

    private int putNewWordToDictionary(Word word) {
        this.wordsById.put(word.getID(), word);
        if (word.getWord() != null)
            this.wordTrie.put(word.getWord(), word);
        return word.getID();
    }

    private int generateWordID() {
        return this.idGenerator.incrementAndGet();
    }

    public void compact() {
        wordsById.compact();
    }
    // ========================================================================

    // IO
    // ========================================================================
    public static WordDictionary loadDictionary(LanguageModel languageModel, File dictionaryFile)
            throws IOException, WordDictionaryMatchingException {
        long start = System.currentTimeMillis();
        String dicId = null;
        try (WordFileInputStream wfs = new WordFileInputStream(dictionaryFile)) {
            dicId = wfs.readUTF();
        }
        WordDictionary loadedDictionary = new WordDictionary(languageModel, dicId);
        int wCount = loadWordsFromFile(dictionaryFile, loadedDictionary);
        LOGGER.info("{} words read from dictionary in {} ms", wCount, System.currentTimeMillis() - start);

        return loadedDictionary;
    }

    public void loadUserDictionary(File userDictionaryFile) throws IOException, WordDictionaryMatchingException {
        long start = System.currentTimeMillis();
        int wCount = loadWordsFromFile(userDictionaryFile, this);
        LOGGER.info("{} words read from user dictionary in {} ms", wCount, System.currentTimeMillis() - start);
    }

    private static int loadWordsFromFile(File dictionaryFile, WordDictionary loadedDictionary)
            throws IOException, FileNotFoundException, WordDictionaryMatchingException {
        int wCount = 0;
        try (WordFileInputStream wfs = new WordFileInputStream(dictionaryFile)) {
            // Read ID and check consistency
            String dictionaryID = wfs.readUTF();
            loadedDictionary.updateIDGenerator(wfs.readInt());
            if (dictionaryID.equals(loadedDictionary.dictionaryId)) {
                // Load words
                Word word = wfs.readWord();
                while (word != null) {
                    loadedDictionary.putExistingWord(word);
                    wCount++;
                    word = wfs.readWord();
                }
            } else {
                throw new WordDictionaryMatchingException();
            }
        }
        loadedDictionary.compact();
        return wCount;
    }

    public void saveUserDictionary(File userDictionaryFile) throws IOException {
        long start = System.currentTimeMillis();
        int wCount = 0;
        Collection<Word> words = this.getAllWords();
        try (WordFileOutputStream wfos = new WordFileOutputStream(userDictionaryFile)) {
            wfos.writeUTF(dictionaryId);
            wfos.writeInt(getIDGeneratorState());
            for (Word word : words) {
                if ((word.isUserWord() || word.isModifiedByUserOrSystem()) && word.isValidForSaving()) {
                    LOGGER.debug("Save user/modified word : {} (ID = {})", word, word.getID());
                    wfos.writeWord(word);
                    wCount++;
                }
            }
        }
        LOGGER.info("{} words saved to user dictionary in {} ms", wCount, System.currentTimeMillis() - start);
    }
    // ========================================================================

    // UTILS
    // ========================================================================
    private void putExistingWord(Word word) {
        wordsById.put(word.getID(), word);
        if (!word.isEquivalenceClass() && !word.isNGramTag()) {
            this.wordTrie.put(word.getWord(), word);
        }
        updateIDGenerator(word.getID());
    }

    private void updateIDGenerator(int id) {
        this.idGenerator.accumulateAndGet(id, Math::max);
    }
    // ========================================================================

}
