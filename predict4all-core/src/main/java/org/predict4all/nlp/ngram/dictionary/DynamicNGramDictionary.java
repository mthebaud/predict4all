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
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * Represent a {@link TrainingNGramDictionary} that can also be opened to be trained again.<br>
 * This type of dictionary is useful when using a dynamic user model : the dynamic user dictionary is loaded and trained during each session, and then saved to be used in the next sessions.
 *
 * @author Mathieu THEBAUD
 */
public class DynamicNGramDictionary extends TrainingNGramDictionary {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicNGramDictionary.class);

    public DynamicNGramDictionary(int maxOrderP) {
        super(maxOrderP);
    }

    private DynamicNGramDictionary() {
        super(-1);
    }

    @Override
    protected void openDictionary(File dictionaryFile) throws IOException {
        long start = System.currentTimeMillis();
        LOGGER.info("Will open dynamic ngram dictionary from {}", dictionaryFile);

        try (FileChannel fileChannel = FileChannel.open(dictionaryFile.toPath(), StandardOpenOption.READ)) {

            // Load dictionary information
            ByteBuffer buffWrite = ByteBuffer.allocate(AbstractNGramTrieNode.DYNAMIC_TRIE_NODE_SIZE_BYTE + DICTIONARY_INFORMATION_BYTE_COUNT);
            fileChannel.read(buffWrite, 0);
            ((Buffer) buffWrite).flip();// ByteBuffer - Buffer - JDK 9+ covariant type
            this.readDictionaryInformation(buffWrite);

            // Read full dictionary
            LOGGER.info("Start loading full ngram tree for dynamic ngram dictionary");
            final Pair<Integer, Integer> rootNodeInfo = rootNode.readNodeInformation(buffWrite);
            rootNode.readAllChildren(fileChannel, rootNodeInfo.getRight());
            this.compact();
        }

        // Probabilities are dynamically computed
        final double d[] = this.computeD(null);
        this.updateProbabilities(d);

        LOGGER.info("Root information and root level loaded in {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Create and open a existing dynamic ngram dictionary.
     *
     * @param dictionaryFile file containing the dynamic ngram dictionary
     * @return the loaded dynamic dictionary
     * @throws IOException if dictionary can't be loaded
     */
    public static DynamicNGramDictionary load(File dictionaryFile) throws IOException {
        DynamicNGramDictionary ngramTrieDic = new DynamicNGramDictionary();
        ngramTrieDic.openDictionary(dictionaryFile);
        return ngramTrieDic;
    }

    @Override
    protected long getRootBlockSize() {
        return AbstractNGramTrieNode.DYNAMIC_TRIE_NODE_SIZE_BYTE;
    }

    @Override
    protected void executeWriteLevelOnRoot(FileChannel fileChannel, int n) throws IOException {
        rootNode.writeLevelForDynamicUse(fileChannel, -1, 0, n);
    }

    @Override
    public double[] computeD(TrainingConfiguration configuration) {
        double[] d = new double[this.getMaxOrder()];
        Arrays.fill(d, 0.5);
        return d;
    }

    public TIntHashSet getWordUsed() {
        TIntHashSet wordsUsed = new TIntHashSet(rootNode.getChildrenSize() + 10);
        this.rootNode.exploreChildren(0, 1, (id, node) -> wordsUsed.add(id));
        return wordsUsed;
    }
}
