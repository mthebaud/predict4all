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
 * The dictionary itself is identified with an UUID to verify consistency when using user dictionary.<br>
 * Note that {@link Word} added to {@link WordDictionary} cannot be removed : their ID should be consistent and they could have been used in a {@link org.predict4all.nlp.ngram.dictionary.AbstractNGramDictionary} :
 * however, you can disable a word with {@link Word#setForceInvalid(boolean, boolean)}
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

    // SIMPLE PUBLIC API
    // ========================================================================

    /**
     * To get a word ID.<br>
     * Note that this method will never return null : it can however return {@link Tag#UNKNOWN} id if there is no word in the dictionary for the given text.
     *
     * @param wordStr the word content
     * @return word ID if found in the dictionary or {@link Tag#UNKNOWN} id
     */
    public int getWordId(String wordStr) {
        return getWord(wordStr).getID();
    }

    /**
     * To get the word entity from text.<br>
     * Note that this method will never return null : it can however return {@link Tag#UNKNOWN} id if there is no word in the dictionary for the given text.
     *
     * @param wordStr the word content
     * @return word if found in the dictionary or {@link TagWord} with {@link Tag#UNKNOWN}
     */
    public Word getWord(String wordStr) {
        Word word = this.wordTrie.get(wordStr);
        return word != null ? word : this.wordsById.get(Tag.UNKNOWN.getId());
    }

    /**
     * To get a word entity from id.<br>
     * Contrary to other {@link #getWord(String)} method, this return null if there is no word for the given ID
     *
     * @param wordId the word ID
     * @return the word associated with the given ID
     */
    public Word getWord(int wordId) {
        return this.wordsById.get(wordId);
    }

    /**
     * All the existing words in this dictionary.<br>
     * Words can be special words as {@link TagWord}, {@link EquivalenceClassWord}, etc.<br>
     * They can also be {@link SimpleWord} from a trained model, and {@link UserWord} if they are word "learned" when using the predictor.<br>
     * Note that if you ony want the possible words for final user, you should use {@link Word#isValidToBePredicted(PredictionParameter)} to filter out invalid words.<br>
     *
     * @return all possible words collection.<br>
     * The returned Collection is <strong>ready-only</strong>
     */
    public Collection<Word> getAllWords() {
        return Collections.unmodifiableCollection(wordsById.valueCollection());
    }

    /**
     * The word count stored in this dictionary.
     *
     * @return the stored word count
     */
    public int size() {
        return this.wordsById.size();
    }

    /**
     * To manually add an user word to this dictionary.<br>
     * This will create the associated word entity.<br>
     * <strong>This doesn't check that a previous word was in the dictionary with the same word : you should check it before calling this method (use {@link #getWord(String)})</strong>
     *
     * @param wordStr the word to add
     * @return the created user word
     */
    public Word putUserWord(String wordStr) {
        UserWord cWord = new UserWord(this.generateWordID(), wordStr);
        this.putNewWordToDictionary(cWord);
        return cWord;
    }
    // ========================================================================

    // PREFIX API
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
        Map<BiIntegerKey, NextWord> words = new HashMap<>(wantedWordCount);

        // Get base from prefix map : factor = 1.0 because it's exact prefix matching
        searchAndAdd(wordPrefix, predictionParameter, wordIdsToExclude, words, "From WordDictionary (exact word start)");

        // Try to find a capitalized version (if needed)
        if (words.size() < wantedWordCount && !Predict4AllUtils.isCapitalized(wordPrefix)) {
            searchAndAdd(Predict4AllUtils.capitalize(wordPrefix), predictionParameter, wordIdsToExclude, words,
                    "From WordDictionary (capitalized version of prefix)");
        }

        // Try to find a lower case version (if needed)
        if (words.size() < wantedWordCount && Predict4AllUtils.containsUpperCase(wordPrefix)) {
            searchAndAdd(Predict4AllUtils.lowerCase(wordPrefix), predictionParameter, wordIdsToExclude, words, "From WordDictionary (lowercase version of prefix)");
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


    public SortedMap<String, Word> getExactWordsWithPrefixExist(String prefix) {
        return Collections.unmodifiableSortedMap(wordTrie.prefixMap(prefix));
    }

    int getIDGeneratorState() {
        return this.idGenerator.get();
    }
    // ========================================================================

    // INSERT WORD API
    // ========================================================================
    public int putUserWord(Token token) {
        return this.putNewWordToDictionary(new UserWord(this.generateWordID(), token.getText()));
    }

    public void incrementUserWord(int wordId) {
        Word word = this.wordsById.get(wordId);
        if (word != null && word.isUserWord()) {
            UserWord uW = (UserWord) word;
            uW.incrementUsageCount();
        }
    }

    void compact() {
        wordsById.compact();
    }

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
    // ========================================================================

    // IO
    // ========================================================================

    /**
     * Create a word dictionary from a word dictionary data file previously created with the training algorithm.<br>
     * This method should not be called on user dictionary file, use {@link #loadUserDictionary(File)} instead.
     *
     * @param languageModel  the language model contained in this dictionary
     * @param dictionaryFile the dictionary data file
     * @return the loaded dictionary
     * @throws IOException if the data file doesn't exist or if IO problem happens
     */
    public static WordDictionary loadDictionary(LanguageModel languageModel, File dictionaryFile)
            throws IOException {
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

    /**
     * To load user dictionary on an existing trained dictionary.<br>
     * This will supplement this dictionary with custom word from user, or existing word with modified parameters.<br>
     * This should be called on dictionary previously saved with {@link #saveUserDictionary(File)}
     *
     * @param userDictionaryFile the user dictionary data file
     * @throws IOException                     if the data file doesn't exist or if IO problem happens
     * @throws WordDictionaryMatchingException if the loaded word dictionary doesn't match this dictionary : the user dictionary should always be loaded on the same trained dictionary used to save it
     */
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

    /**
     * To save this dictionary modified words.<br>
     * This will saved into the given file : the {@link UserWord} added to the dictionary,
     * but also every {@link Word} that was modified (e.g. if {@link Word#setProbFactor(double, boolean)}, {@link Word#setForceInvalid(boolean, boolean)} etc... was called).<br>
     * This file can later be loaded with {@link #loadUserDictionary(File)}
     *
     * @param userDictionaryFile the user dictionary data file
     * @throws IOException if saving failed
     * @see Word to see exactly how {@link Word#isModifiedByUserOrSystem()} works
     */
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

    private int generateWordID() {
        return this.idGenerator.incrementAndGet();
    }

    private void updateIDGenerator(int id) {
        this.idGenerator.accumulateAndGet(id, Math::max);
    }
    // ========================================================================

}
