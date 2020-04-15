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

import org.predict4all.nlp.ngram.trie.AbstractNGramTrieNode;
import org.predict4all.nlp.ngram.trie.StaticNGramTrieNode;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 * Represent a static ngram dictionary where trie node are loaded "on demand"
 * while browsing through the nodes.<br>
 * <br>
 * This dictionary is read only and cannot be updated or saved : methods like
 * {@link #updateProbabilities(double[])}, {@link #putAndIncrementBy(int[], int)} are not
 * supported by this dictionary. <br>
 * This dictionary is created from a saved {@link TrainingNGramDictionary}.
 *
 * @author Mathieu THEBAUD
 */
public class StaticNGramTrieDictionary extends AbstractNGramDictionary<StaticNGramTrieNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticNGramTrieDictionary.class);

    /**
     * File channel where trie node are read on demand
     */
    private FileChannel fileChannel;

    public StaticNGramTrieDictionary() {
        super(new StaticNGramTrieNode(), -1);
    }

    // IMPLEMENTATIONS
    // ========================================================================
    @Override
    public StaticNGramTrieNode getNodeForPrefix(int[] prefix, int index) {
        try {
            return rootNode.getNodeFor(this.fileChannel, prefix, index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean checkChildrenLoading(StaticNGramTrieNode node) {
        try {
            return node.checkChildrenLoading(fileChannel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ========================================================================

    // IO
    // ========================================================================
    @Override
    public void close() throws Exception {
        this.fileChannel.close();
        LOGGER.info("Static ngram dictionary closed");
    }

    @Override
    protected void openDictionary(File dictionaryFile) throws IOException {
        long start = System.currentTimeMillis();
        LOGGER.info("Will open static ngram dictionary from {}", dictionaryFile);

        this.fileChannel = FileChannel.open(dictionaryFile.toPath(), StandardOpenOption.READ);

        // Read general information + root node
        ByteBuffer buffWrite = ByteBuffer
                .allocate(AbstractNGramTrieNode.STATIC_TRIE_NODE_SIZE_BYTE + DICTIONARY_INFORMATION_BYTE_COUNT);
        fileChannel.read(buffWrite, 0);
        ((Buffer) buffWrite).flip();// ByteBuffer - Buffer - JDK 9+ covariant type
        this.readDictionaryInformation(buffWrite);
        rootNode.readNodeInformation(buffWrite);

        // Load unigram (because unigram level is always needed and shouldn't be loaded on demand)
        this.checkChildrenLoading(rootNode);

        LOGGER.info("Root information and root level loaded in {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Create a static ngram dictionary from a given file.
     *
     * @param dictionaryFile file that contains the dictionary
     * @return the static ngram dictionary, initialized and ready to use
     * @throws IOException if dictionary can't be loaded
     */
    public static StaticNGramTrieDictionary open(File dictionaryFile) throws IOException {
        StaticNGramTrieDictionary ngramTrieDic = new StaticNGramTrieDictionary();
        ngramTrieDic.openDictionary(dictionaryFile);
        return ngramTrieDic;
    }
    // ========================================================================

    // NO SUPPORTED
    // ========================================================================

    @Override
    public void putAndIncrementBy(int[] ngram, int index, int increment) {
        throw new UnsupportedOperationException("Can't put any new ngram in a static ngram trie dictionary");
    }

    @Override
    public void putAndIncrementBy(int[] ngram, int increment) {
        throw new UnsupportedOperationException("Can't put any new ngram in a static ngram trie dictionary");
    }

    @Override
    public void saveDictionary(File dictionaryFile) throws IOException {
        throw new UnsupportedOperationException("NGram static dictionary is read-only");
    }

    @Override
    public void updateProbabilities(double[] d) {
        throw new UnsupportedOperationException("Can't update probabilities in static ngram dictionary");
    }

    @Override
    public void updateProbabilities(int[] prefix, int prefixIndex, double[] d) {
        throw new UnsupportedOperationException("Can't update probabilities in static ngram dictionary");
    }

    @Override
    public double[] computeD(TrainingConfiguration configuration) {
        throw new UnsupportedOperationException("Can't compute D in static ngram dictionary");
    }
    // ========================================================================

}
