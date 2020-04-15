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

package org.predict4all.nlp.ngram.trie;

import org.predict4all.nlp.Tag;
import org.predict4all.nlp.ngram.trie.map.TrieNodeMap;
import org.predict4all.nlp.utils.Pair;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Represent a dynamic trie node structure : this trie node is useful when the ngram count has to be retrieved.<br>
 * Dynamic trie node children are always fully loaded (they are not loaded on demand) and their frequencies can change.<br>
 * Because dynamic trie node are used to be saved and loaded as {@link StaticNGramTrieNode} or {@link DynamicNGramTrieNode} they contains two write method : {@link #writeStaticNode(FileChannel, int)} if they are saved to be loaded as
 * {@link StaticNGramTrieNode} and
 * {@link #writeDynamicNode(FileChannel, int)} if they are saved to be loaded as {@link DynamicNGramTrieNode} : one save static information about the node (frequency, bow), the other only save dynamic information (count) because frequencies are
 * dynamically computed.
 *
 * @author Mathieu THEBAUD
 */
public class DynamicNGramTrieNode extends AbstractNGramTrieNode<DynamicNGramTrieNode> {

    /**
     * Count how many times this final ngram represented by this node appeared in the training corpora.<br>
     * The count doesn't include children count : if a bigram doesn't appears, its count can be 0, while the bigram children can have count > 0.
     */
    private int count;

    // GET/PUT
    // ========================================================================

    /**
     * Retrieve a node for a given prefix (if exists)
     *
     * @param prefix   the prefix
     * @param index    prefix index (=0 = full prefix)
     * @param maxIndex max index for search (negative = whole search)
     * @return the node for the given prefix, or null if there is no node for the given prefix
     */
    public DynamicNGramTrieNode getNodeFor(int[] prefix, int index, int maxIndex) {
        if (index < prefix.length && (maxIndex < 0 || index < maxIndex)) {
            int wordId = prefix[index];
            if (children != null) {
                DynamicNGramTrieNode child = this.children.get(wordId);
                if (child != null) {
                    return child.getNodeFor(prefix, index + 1, maxIndex);
                }
            }
            return null;
        } else {
            return this;
        }
    }

    /**
     * Put a ngram into this trie structure, create node if needed, and increment the existing one.
     *
     * @param ngram     the ngram to put in the trie
     * @param index     the ngram insertion index
     * @param increment the increment to be added to the final ngram
     */
    public void putAndIncrementBy(int[] ngram, int index, int increment) {
        if (index < ngram.length) {
            int wordId = ngram[index];
            checkChildrenInitialization();
            DynamicNGramTrieNode next = children.get(wordId);
            if (next == null) {
                next = new DynamicNGramTrieNode();
                children.put(wordId, next);
            }
            next.putAndIncrementBy(ngram, index + 1, increment);
        } else {
            this.count += increment;
        }
    }

    /**
     * Check that the children map is initialized
     */
    private void checkChildrenInitialization() {
        if (children == null)
            children = new TrieNodeMap<>();
    }
    // ========================================================================

    // SIMPLE API
    // ========================================================================
    public int getCount() {
        return count;
    }

    @Override
    public int getChildrenSize() {
        return children != null ? children.size() : 0;
    }
    // ========================================================================

    // PROBABILITIES
    // ========================================================================

    /**
     * Compute frequency for this node using absolute discounting.<br>
     * Compute this node frequency, backoff weight, and then compute the frequency for node children.
     *
     * @param level     the current level
     * @param d         d parameter used for absolute discounting formula
     * @param recursive if true, will call {@link #computeProbabilityForChildren(int, double[], boolean)} on this node children
     */
    public void computeProbabilityForChildren(final int level, final double[] d, final boolean recursive) {
        if (children != null) {
            double childrenCountTotal = this.getChildrenCountSum();
            final double dValue = level == 0 ? 0.0 : d[level];
            children.forEachValue(child -> {
                child.frequency = childrenCountTotal > 0 ? ((Math.max(0.0, 1.0 * child.count - dValue)) / childrenCountTotal) : 0.0;
                if (recursive) {
                    child.computeProbabilityForChildren(level + 1, d, recursive);
                }
                return true;
            });
            if (level > 0) {
                this.childrenBackoffWeight = childrenCountTotal > 0 ? ((this.getChildrenSize() * dValue) / childrenCountTotal) : 1.0;
            }
        }
    }

    /**
     * Detect each unique trie leaves for a wanted order, and then call the found callback with the found prefix and word id
     *
     * @param prefix        the current prefix (prefix containing browsed node to get to this node)
     * @param wordId        this node word id
     * @param currentOrder  the current browsing order
     * @param wantedOrder   the wanted order : all leaves found on this order will be given to foundCallback
     * @param foundCallback method to call a leaf is detected : called with prefix to get to the node and the node id
     */
    public void listTrieLeaves(int prefix[], int wordId, int currentOrder, int wantedOrder, BiConsumer<int[], Integer> foundCallback) {
        if (currentOrder == wantedOrder) {
            foundCallback.accept(prefix, wordId);
        } else if (children != null && currentOrder < wantedOrder) {
            this.children.forEachEntry((childId, node) -> {
                int[] prefixToUse = prefix;
                if (currentOrder < prefix.length) {
                    prefixToUse = Arrays.copyOf(prefix, prefix.length);
                    prefixToUse[currentOrder] = childId;
                }
                node.listTrieLeaves(prefixToUse, childId, currentOrder + 1, wantedOrder, foundCallback);
                return true;
            });
        }
    }

    public void pruningCountingNGram(int currentOrder, int wantedOrder, int countThreshold) {
        if (currentOrder >= wantedOrder) {
            return;
        } else if (children != null && currentOrder < wantedOrder) {
            List<Integer> childrenToDelete = new ArrayList<>();
            this.children.forEachEntry((childId, node) -> {
                if (currentOrder + 1 == wantedOrder && node.count < countThreshold && node.isEmpty()) {
                    childrenToDelete.add(childId);
                }
                node.pruningCountingNGram(currentOrder + 1, wantedOrder, countThreshold);
                return true;
            });
            childrenToDelete.forEach(this.children::remove);
        }
    }

    private boolean isEmpty() {
        return children == null || children.size() == 0;
    }

    // ========================================================================

    // COUNTING
    // ========================================================================

    /**
     * @return sum of all children count (or 0.0 if this ngram has no child)
     */
    public double getChildrenCountSum() {
        AtomicInteger count = new AtomicInteger(0);
        if (children != null) {
            this.children.forEachValue(n -> {
                count.addAndGet(n.count);
                return true;
            });
        }
        return count.get();
    }

    /**
     * Count the number of ngram on a order : count the total count (occurence count) and the unique count (difference ngram count)
     *
     * @param order         the current order
     * @param wantedOrder   the wanted order
     * @param totalCounter  count incremented with each wanted order ngram count
     * @param uniqueCounter count incremented with the number of different ngram for an order
     */
    public void countNGram(int order, int wantedOrder, AtomicInteger totalCounter, AtomicInteger uniqueCounter) {
        if (order == wantedOrder) {
            totalCounter.addAndGet(count);
            if (count > 0) {
                uniqueCounter.incrementAndGet();
            }
        } else if (children != null) {
            children.forEachEntry((id, node) -> {
                //if (id != NGramTag.START.getWordId()) {
                node.countNGram(order + 1, wantedOrder, totalCounter, uniqueCounter);
                //}
                return true;
            });
        }
    }

    /**
     * Count the number of ngram with a count == 1 or == 2.<br>
     * This ignore ngram containing {@link Tag#START}
     *
     * @param order  ngram order (1 = unigram, 2 brigram, etc)
     * @param count1 count incremented for each ngram with count == 1
     * @param count2 count incremented for each ngram with count == 2
     */
    public void countOneAndTwoOccurenceNGrams(int order, AtomicInteger[] count1, AtomicInteger[] count2) {
        if (count == 1)
            count1[order - 1].incrementAndGet();
        if (count == 2)
            count2[order - 1].incrementAndGet();
        if (children != null) {
            children.forEachEntry((id, n) -> {
                if (id != Tag.START.getId()) {
                    n.countOneAndTwoOccurenceNGrams(order + 1, count1, count2);
                }
                return true;
            });
        }
    }

    public void exploreChildren(int level, int maxLevelInclusive, BiConsumer<Integer, DynamicNGramTrieNode> consumer) {
        if (level <= maxLevelInclusive) {
            if (children != null) {
                children.forEachEntry((id, n) -> {
                    consumer.accept(id, n);
                    n.exploreChildren(level + 1, maxLevelInclusive, consumer);
                    return true;
                });
            }
        }
    }
    // ========================================================================

    // READING
    // ========================================================================

    /**
     * Read all children from a given file channel, then load recursively all the children.
     *
     * @param fileChannel  the file channel containing trie structure
     * @param childrenSize the children count for this node.
     * @throws IOException if children can't be loaded
     */
    public void readAllChildren(FileChannel fileChannel, final int childrenSize) throws IOException {
        if (childrenPosition >= 0) {
            ByteBuffer buffWrite = ByteBuffer.allocate(childrenSize * DYNAMIC_TRIE_NODE_SIZE_BYTE);
            fileChannel.read(buffWrite, childrenPosition);
            ((Buffer) buffWrite).flip();// ByteBuffer - Buffer - JDK 9+ covariant type
            children = new TrieNodeMap<>();
            for (int i = 0; i < childrenSize; i++) {
                DynamicNGramTrieNode trieNode = new DynamicNGramTrieNode();
                Pair<Integer, Integer> idAndChildrenSize = trieNode.readNodeInformation(buffWrite);
                children.put(idAndChildrenSize.getLeft(), trieNode);
                trieNode.readAllChildren(fileChannel, idAndChildrenSize.getRight());
            }
            children.compact();
        }
    }

    /**
     * Read this node information from a given buffer.
     *
     * @param buffWrite the buffer where node information are read
     * @return a pair containing this node word id (on left) and this node children size (on right)
     */
    public Pair<Integer, Integer> readNodeInformation(ByteBuffer buffWrite) {
        int wordId = buffWrite.getInt();
        int childrenSize = buffWrite.getInt();
        this.childrenPosition = buffWrite.getInt();
        this.count = buffWrite.getInt();
        return Pair.of(wordId, childrenSize);
    }
    // ========================================================================

    // WRITING
    // ========================================================================

    /**
     * Write this node to be loaded as a {@link StaticNGramTrieNode} (doesn't write count, but write static frequency and bow)
     *
     * @param fileChannel the file channel where node is written
     * @param wordId      this node word id
     */
    private void writeStaticNode(FileChannel fileChannel, int wordId) {
        try {
            ByteBuffer buffWrite = ByteBuffer.allocateDirect(STATIC_TRIE_NODE_SIZE_BYTE);
            buffWrite.putInt(wordId);
            buffWrite.putInt(this.getChildrenSize());
            buffWrite.putInt(this.childrenPosition);
            buffWrite.putDouble(this.frequency);
            buffWrite.putDouble(this.childrenBackoffWeight);
            ((Buffer) buffWrite).flip();// ByteBuffer - Buffer - JDK 9+ covariant type
            fileChannel.write(buffWrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write this node to be loaded as a {@link DynamicNGramTrieNode} (doesn't write static frequency, but write count)
     *
     * @param fileChannel the file channel where node is written
     * @param wordId      this node word id
     */
    private void writeDynamicNode(FileChannel fileChannel, int wordId) {
        try {
            ByteBuffer buffWrite = ByteBuffer.allocateDirect(DYNAMIC_TRIE_NODE_SIZE_BYTE);
            buffWrite.putInt(wordId);
            buffWrite.putInt(this.getChildrenSize());
            buffWrite.putInt(this.childrenPosition);
            buffWrite.putInt(count);
            ((Buffer) buffWrite).flip();// ByteBuffer - Buffer - JDK 9+ covariant type
            fileChannel.write(buffWrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a trie level with {@link #writeLevel(FileChannel, int, int, int, BiConsumer)} with {@link #writeDynamicNode(FileChannel, int)} as save method.<br>
     * Result trie file should be read as {@link DynamicNGramTrieNode}
     *
     * @param fileChannel file channel where the trie structure will be saved
     * @param wordId      this node word id (to be saved with node information)
     * @param level       the current level
     * @param wantedLevel the wanted level to be saved
     * @throws IOException if an exception happen while writing level
     */
    public void writeLevelForDynamicUse(FileChannel fileChannel, int wordId, int level, int wantedLevel) throws IOException {
        this.writeLevel(fileChannel, wordId, level, wantedLevel, (node, id) -> node.writeDynamicNode(fileChannel, id));
    }

    /**
     * Write a trie level with {@link #writeLevel(FileChannel, int, int, int, BiConsumer)} with {@link #writeStaticNode(FileChannel, int)} as save method.<br>
     * Result trie file should be read as {@link StaticNGramTrieNode}
     *
     * @param fileChannel file channel where the trie structure will be saved
     * @param wordId      this node word id (to be saved with node information)
     * @param level       the current level
     * @param wantedLevel the wanted level to be saved
     * @throws IOException if an exception happen while writing level
     */
    public void writeLevelForStaticUse(final FileChannel fileChannel, int wordId, int level, int wantedLevel) throws IOException {
        this.writeLevel(fileChannel, wordId, level, wantedLevel, (node, id) -> node.writeStaticNode(fileChannel, id));
    }

    /**
     * To write a trie level : if the current level is the level we want to save, the save method is called, if not, this go through children until the right level is reached.
     *
     * @param fileChannel file channel where the trie structure will be saved
     * @param wordId      this node word id (to be saved with node information)
     * @param level       the current level
     * @param wantedLevel the wanted level to be saved
     * @param saveMethod  the save method, called once wanted level is reached
     * @throws IOException if an exception happen while writing level
     */
    private void writeLevel(FileChannel fileChannel, int wordId, int level, int wantedLevel, BiConsumer<DynamicNGramTrieNode, Integer> saveMethod)
            throws IOException {
        if (level == wantedLevel) {
            saveMethod.accept(this, wordId);
        } else if (children != null && !children.isEmpty()) {
            if (level == wantedLevel - 1) {
                this.childrenPosition = (int) fileChannel.position();
            }
            boolean success = children.forEachEntry((id, node) -> {
                try {
                    node.writeLevel(fileChannel, id, level + 1, wantedLevel, saveMethod);
                    return true;
                } catch (IOException e) {
                    return false;
                }
            });
            if (!success) {
                throw new IOException("Children saving failed");
            }
        }
    }
    // ========================================================================
}
