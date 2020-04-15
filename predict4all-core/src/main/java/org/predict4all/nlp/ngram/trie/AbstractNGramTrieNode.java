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

import gnu.trove.procedure.TObjectProcedure;
import org.predict4all.nlp.ngram.trie.map.TrieNodeMap;

import java.nio.channels.FileChannel;

/**
 * Represent a node in a trie structure to represent ngrams. Trie structure is used to save memory because information about ngram are very redundant.<br>
 * <br>
 * For example, the trie for sentences <i>"this is sentence. this is what"</i> will contains following nodes (showing 3 gram only):
 * <p>
 * - this<br>
 * --- is<br>
 * ----- sentence<br>
 * ----- what<br>
 * <p>
 * Trie node can be static or dynamic, where both has different application :<br>
 * <ul>
 * <li><strong>Static</strong> : static trie node are node loaded on demand while browsing the trie structure. Their frequencies and backoff weight are pre-computed and used "as it" by the dictionary. Static node doesn't support
 * insertion/remove. They are useful to browse huge ngram trie with a limited memory use.</li>
 * <li><strong>Dynamic</strong> : dynamic trie node are fully loaded (which mean than the whole trie is loaded into memory) and they support insertion/removal. Their frequencies and bow are computed but can be dynamically computed because the count
 * values are loaded. They are useful to train a ngram model (counting) or when the ngram trie is small (e.g. user ngram model).</li>
 * </ul>
 *
 * @param <T> node children type (typically this node type)
 * @author Mathieu THEBAUD
 */
public abstract class AbstractNGramTrieNode<T extends AbstractNGramTrieNode<?>> {
    private static final int INTEGER_BYTE_SIZE = 4;
    private static final int DOUBLE_BYTE_SIZE = 8;

    // ATTRIBUTE
    // ========================================================================
    /**
     * Static node byte size (3 integer, 2 double).<br>
     * Integer : word id, children size, children position.<br>
     * Double : frequency, backoff weight.
     */
    public static final int STATIC_TRIE_NODE_SIZE_BYTE = 3 * INTEGER_BYTE_SIZE + 2 * DOUBLE_BYTE_SIZE;

    /**
     * Dynamic node byte size (4 integer)
     * Integer : word id, children size, children position, count
     */
    public static final int DYNAMIC_TRIE_NODE_SIZE_BYTE = 4 * INTEGER_BYTE_SIZE;

    /**
     * Contains the children nodes position in file.<br>
     * Position in a {@link FileChannel} is a long type, but to save memory the value is stored as an int (trie file never contains more than {@link Integer#MAX_VALUE} byte)
     */
    protected int childrenPosition = -1;

    /**
     * Represent the children node for this node.<br>
     * Each child is stored by its value (= word id) and represent the possible next value.<br>
     * To save memory, the map is created on demand, so even if this node has children, the map can be null if children are not loaded yet.
     */
    protected TrieNodeMap<T> children;

    /**
     * Computed frequency for this node
     */
    protected double frequency;

    /**
     * Backoff weight for this node children frequencies
     */
    protected double childrenBackoffWeight = 1.0;
    // ========================================================================

    // SIMPLE API
    // ========================================================================

    /**
     * @return this node computed frequency
     */
    public double getFrequency() {
        return frequency;
    }

    /**
     * @return this node children backoff weight
     */
    public double getChildrenBackoffWeight() {
        return childrenBackoffWeight;
    }

    /**
     * @return this node children (can be null if this node has no children, or if children are not loaded)
     */
    public TrieNodeMap<T> getChildren() {
        return this.children;
    }

    /**
     * @return the different children count (not the total children count)
     */
    public abstract int getChildrenSize();
    // ========================================================================

    // COMPACT
    // ========================================================================
    private static final TObjectProcedure<AbstractNGramTrieNode<?>> COMPACT_CHILD_PROCEDURE = node -> {
        node.compact();
        return true;
    };

    /**
     * compact the children of this node (if this node has children)
     */
    public void compact() {
        if (children != null) {
            children.compact();
            this.children.forEachValue(COMPACT_CHILD_PROCEDURE);
        }
    }
    // ========================================================================
}
