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

package org.predict4all.nlp.ngram.dictionary;

import gnu.trove.set.hash.TIntHashSet;
import org.predict4all.nlp.ngram.trie.AbstractNGramTrieNode;
import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.utils.BiIntegerKey;
import org.predict4all.nlp.words.NextWord;
import org.predict4all.nlp.words.WordDictionary;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represent an ngram dictionary in an abstract way : dictionary can be static or dynamic.<br>
 * Each type of dictionary can or can't support operation, such as dictionary saving, or updating probabilities.<br>
 * <br>
 * The dictionary has a {@link #maxOrder} that represents the max order gram that can be found in the dictionary.
 * Order in a ngram correspond to the ngram rank : 1 = unigram, 2 bigram, etc...
 * Order in dictionary is not bounded to a maximum value, but in practice, order is never more than 5.<br>
 * <br>
 * Dictionary are represented as a trie, with also different kind of trie availabe. Each type of dictionary is associated
 * with a different type of {@link AbstractNGramTrieNode} (e.g. dynamic dictionary is associated with a dynamic trie node).
 *
 * @param <T> type of trie node stored in this dictionary.
 * @author Mathieu THEBAUD
 */
public abstract class AbstractNGramDictionary<T extends AbstractNGramTrieNode<T>> implements AutoCloseable {
    /**
     * Byte count needed to save general information about this dictionary. (e.g. max order)
     */
    protected static final int DICTIONARY_INFORMATION_BYTE_COUNT = 4;

    /**
     * Max order possible to store in this dictionary.<br>
     * Could be retrieved by opening the dictionary, or set by user as a limit.
     */
    protected int maxOrder = -1;

    /**
     * Root node of this dictionary (this node contains as children the whole vocabulary)
     */
    protected final T rootNode;

    /**
     * Construct a dictionary with a given root node and a max possible order.
     *
     * @param rootNode  the root node to use for this dictionary
     * @param maxOrderP max possible order for this dictionary.
     */
    public AbstractNGramDictionary(T rootNode, int maxOrderP) {
        super();
        this.rootNode = rootNode;
        this.maxOrder = maxOrderP;
    }

    // SIMPLE API
    // ========================================================================

    /**
     * @return the root for this dictionary
     */
    public T getRoot() {
        return rootNode;
    }

    /**
     * @return the max possible order for this dictionary
     */
    public int getMaxOrder() {
        return this.maxOrder;
    }

    /**
     * Compact the nodes in this dictionary (this will call {@link AbstractNGramTrieNode#compact()} on root)
     */
    public void compact() {
        this.rootNode.compact();
    }
    // ========================================================================

    // ABSTRACT
    // ========================================================================

    /**
     * Use to retrieve a node for a given prefix.<br>
     * For example, for prefix = [1,2] will return the trie node corresponding to {2}.<br>
     * The children of the given node may have not been loaded.
     *
     * @param prefix the node prefix
     * @param index  first word in prefix index (to take the full prefix, index should be = 0)
     * @return the node found for the given prefix, or null if there is no existing node for such prefix
     */
    public abstract T getNodeForPrefix(int[] prefix, int index);

    /**
     * To check that the children of a given node are loaded into memory (and can be used)
     *
     * @param node the node to check children loading on
     * @return true if there is children for this node, and these children are loaded.
     */
    public abstract boolean checkChildrenLoading(T node);

    /**
     * Add a given ngram to the dictionary and to increment its count.<br>
     * If the ngram is already in the dictionary, will just increment its count.
     *
     * @param ngram     the ngram to put in dictionary
     * @param index     index for ngram start (index when the ngram become valid : for example, if we want to skip the first ngram word, just set index = 1)
     * @param increment the increment value
     */
    public abstract void putAndIncrementBy(int[] ngram, int index, int increment);

    /**
     * Add a given ngram to the dictionary and to increment its count.<br>
     * If the ngram is already in the dictionary, will just increment its count.<br>
     * This will call {@link #putAndIncrementBy(int[], int, int)} with a index = 0
     *
     * @param ngram the ngram to put in dictionary
     * @param increment the increment value
     */
    public abstract void putAndIncrementBy(int[] ngram, int increment);

    /**
     * Save this dictionary to a file.<br>
     * Will save the dictionary relative with id only, this means that the same word dictionary should be loaded if this dictionary is opened later.
     *
     * @param dictionaryFile the file where dictionary should be saved.
     * @throws IOException if dictionary can't be saved
     */
    public abstract void saveDictionary(File dictionaryFile) throws IOException;

    /**
     * Open a dictionary from a file.<br>
     * To use the dictionary, the same {@link WordDictionary} used to save it should be used.
     *
     * @param dictionaryFile the file containing a dictionary.
     * @throws IOException if dictionary can't be opened
     */
    protected abstract void openDictionary(File dictionaryFile) throws IOException;

    /**
     * Update the whole probabilities in this dictionary.<br>
     * Can take a while if there is a lot of nodes in the dictionary.
     *
     * @param d the d parameter for absolute discounting algorithm.
     */
    public abstract void updateProbabilities(double[] d);

    /**
     * Update probabilities in this dictionary for a specific ngram prefix : this will update the probabilities of the prefix children, and update the backoff weight of the parent node.<br>
     * This is much more optimized than {@link #updateProbabilities(double[])}
     *
     * @param prefix      prefix of the node that should be updated
     * @param prefixIndex prefix start index (0 = full prefix, 1 = skip the first word in prefix, etc...)
     * @param d           the d parameter for absolute discounting algorithm.
     */
    public abstract void updateProbabilities(int[] prefix, int prefixIndex, double[] d);

    /**
     * Compute the optimal value for d (absolute discounting parameter).<br>
     * Usually d is computed with formula :<br>
     * <strong>D = C1 / (C1 + 2 * C2)</strong><br>
     * Where C1 = number of ngram with count == 1, and C2 = number of ngram with count == 2.
     * Theses values are computed for each order (0 index = unigram, 1 index = bigram, etc.)
     *
     * @param configuration configuration to use to compute D (can set min/max values and a D value)
     * @return computed d value for this dictionary
     */
    public abstract double[] computeD(TrainingConfiguration configuration);
    // ========================================================================

    // NEXT WORDS
    // ========================================================================

    /**
     * Will go through each ngram dictionary order to find the next possible words for a given prefix<br>
     * Will first go through the highest order for the given prefix (e.g. prefix length == 3 = order is 4),
     * and if the wantedCount is not reached, will go to the lower order to find new next possible.
     *
     * @param prefix              the prefix to detect word after (words ids, represent a ngram prefix)
     * @param wordDictionary      word dictionary (useful only if prefixDetected is not null)
     * @param predictionParameter prediction parameter (can be used to validate words)
     * @param wordsToExclude      a list of words that shouldn't be included in the result set
     * @param resultSet           set that will contains every next words found
     * @param wantedCount         wanted next word count (an higher count will take more time)
     * @param unigramLevel        if true, this will go to unigram level (whole vocabulary) if the is not enough / this can be time consuming as unigram level contains the whole word dictionary
     */
    public void listNextWords(final int[] prefix, WordDictionary wordDictionary,
                              PredictionParameter predictionParameter, Set<Integer> wordsToExclude, Map<BiIntegerKey, NextWord> resultSet, int wantedCount,
                              boolean unigramLevel) {
        // TODO : if wantedCount > vocabSize : return filtered vocab
        // Go from max order (prefix.length) to min order (1-gram, or 2-gram if unigram is disabled) and retrieve prefix children
        AtomicInteger foundCount = new AtomicInteger(0);
        int maxLength = unigramLevel ? prefix.length : prefix.length - 1;
        for (int i = 0; i <= maxLength && foundCount.get() < wantedCount; i++) {
            final int iF = i;
            T nodeFor = this.getNodeForPrefix(prefix, i);
            if (nodeFor != null && this.checkChildrenLoading(nodeFor)) {
                if (nodeFor.getChildren() != null) {
                    nodeFor.getChildren().forEachKey(wid -> {
                        if (wordDictionary.getWord(wid).isValidToBePredicted(predictionParameter) && !wordsToExclude.contains(wid)) {
                            // TODO : validate word with prefix words
                            BiIntegerKey key = BiIntegerKey.of(wid);
                            if (!resultSet.containsKey(key)) {
                                resultSet.put(key,
                                        NextWord.createUnique(wid, 1.0, false,
                                                predictionParameter.isEnableDebugInformation()
                                                        ? new StringBuilder("Du modèle ").append(prefix.length - iF + 1).append("-GRAM (")
                                                        .append(convertPrefixToString(prefix, iF, wordDictionary)).append(")")
                                                        : null));
                                foundCount.incrementAndGet();
                            }
                        }
                        return true;
                    });
                }
            }
        }
    }

    private String convertPrefixToString(int[] prefix, final int index, WordDictionary wd) {
        StringBuilder sb = new StringBuilder();
        for (int i = index; i < prefix.length; i++) {
            if (i != index)
                sb.append(", ");
            sb.append(wd.getWord(prefix[i]));
        }
        return sb.toString();
    }

    /**
     * Return the immediate next words for a given prefix (without any filter)
     *
     * @param prefix the prefix (previous N words)
     * @return a set containing the next word for the given prefix, or null if there is no existing ngram in the dictionary for this prefix
     * @throws IOException if children can't be read
     */
    public TIntHashSet getNextWord(final int[] prefix) throws IOException {
        T nodeFor = this.getNodeForPrefix(prefix, 0);
        if (nodeFor != null) {
            if (this.checkChildrenLoading(nodeFor)) {
                TIntHashSet wordIds = new TIntHashSet(nodeFor.getChildren().size());
                nodeFor.getChildren().forEachKey(wid -> {
                    wordIds.add(wid);
                    return true;
                });
                return wordIds;
            }
        }
        return null;
    }
    // ========================================================================

    // PROBABILITY
    // ========================================================================

    /**
     * Return the probability of a word for a given prefix.<br>
     * Given index = 0 and length = prefix.length will return the maximum order probability (e.g. prefix.length = 3, will return probability for order 3)
     *
     * @param prefix the word before the given word (prefix)
     * @param index  the index in the given prefix (will change the result order)
     * @param length the given prefix length (will change the result order).
     * @param wordId the word we want the probability for
     * @return the probability for the given word (0.0 - 1.0)
     */
    public double getProbability(final int[] prefix, final int index, final int length, final int wordId) {
        T nodeForPrefix = this.getNodeForPrefix(prefix, index);
        double f, nodeBow;
        if (nodeForPrefix != null) {
            T child = this.checkChildrenLoading(nodeForPrefix) ? nodeForPrefix.getChildren().get(wordId) : null;
            f = child != null ? child.getFrequency() : 0.0;
            nodeBow = nodeForPrefix.getChildrenBackoffWeight();
        } else {
            f = 0.0;
            nodeBow = 1.0;
        }
        return f + (length > 0 ? nodeBow * getProbability(prefix, index + 1, length - 1, wordId) : 0.0);
    }

    public double getRawProbability(final int[] prefix, final int index, final int length, final int wordId) {
        T nodeForPrefix = this.getNodeForPrefix(prefix, index);
        if (nodeForPrefix != null) {
            T child = this.checkChildrenLoading(nodeForPrefix) ? nodeForPrefix.getChildren().get(wordId) : null;
            return child != null ? child.getFrequency() : 0.0;
        } else {
            throw new IllegalArgumentException("Requested a raw probability for a unknown ngram");
        }
    }
    // ========================================================================

    // DICTIONARY INFO
    // ========================================================================

    /**
     * Read the general information for this dictionary from a given buffer (doesn't do any check)
     *
     * @param byteBuffer the byte buffer where dictionary information are read
     */
    protected void readDictionaryInformation(ByteBuffer byteBuffer) {
        this.maxOrder = byteBuffer.getInt();
    }

    /**
     * Write the general information for this dictionary to a given buffer
     *
     * @param buffWrite the byte buffer where information are written
     */
    protected void writeDictionaryInfo(ByteBuffer buffWrite) {
        buffWrite.putInt(this.maxOrder);
    }
    // ========================================================================
}
