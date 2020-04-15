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

import org.predict4all.nlp.ngram.dictionary.TrainingNGramDictionary;
import org.predict4all.nlp.ngram.trie.map.TrieNodeMap;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Represent a static ngram trie node : when node are used only to retrieve information and compute probabilities, but children are never updated.<br>
 * This node is particular because children node are loaded on demand from a {@link FileChannel}.<br>
 * This node is produced in a read only version : to create this node, {@link DynamicNGramTrieNode} and {@link TrainingNGramDictionary} should be used.
 *
 * @author Mathieu THEBAUD
 */
public class StaticNGramTrieNode extends AbstractNGramTrieNode<StaticNGramTrieNode> {
    /**
     * Contains the children size read from file for this node.<br>
     * This information is important because it is used when loading children (> loaded size can be computed)
     */
    private int childrenSize;

    // GET NODE
    // ========================================================================

    /**
     * Will try to retrieve a node for a given prefix.<br>
     * Load needed node on demand while browsing the trie.<br>
     * Children of the returned node are not loaded yet.
     *
     * @param fileChannel the file channel containing the trie structure.
     * @param prefix      ngram prefix
     * @param index       index in prefix (0 = full prefix)
     * @return the node found for the given prefix (or null if the trie doesn't contains any node for the given prefix)
     * @throws IOException if children can't be read for a node
     */
    public StaticNGramTrieNode getNodeFor(FileChannel fileChannel, int[] prefix, int index) throws IOException {
        if (index < prefix.length) {
            int wordId = prefix[index];
            if (checkChildrenLoading(fileChannel)) {
                StaticNGramTrieNode child = this.children.get(wordId);
                if (child != null) {
                    return child.getNodeFor(fileChannel, prefix, index + 1);
                }
            }
            return null;
        } else {
            return this;
        }
    }

    /**
     * Check that children for this node are loaded.<br>
     * If not, try to load children from the given fileChannel.
     *
     * @param fileChannel file channel containing the trie structure.
     * @return true if there is children and children are loaded.
     * @throws IOException if there is children, but they can't be loaded
     */
    public boolean checkChildrenLoading(FileChannel fileChannel) throws IOException {
        if (childrenPosition >= 0 && children == null) {
            children = new TrieNodeMap<>();
            this.readChildren(fileChannel);
            children.compact();
            return true;
        } else {
            return children != null;
        }
    }
    // ========================================================================

    // IO
    // ========================================================================

    /**
     * Read the whole children list from given file channel.
     *
     * @param fileChannel file channel containing the trie structure.
     * @throws IOException if children can't be read
     */
    private void readChildren(FileChannel fileChannel) throws IOException {
        ByteBuffer buffWrite = ByteBuffer.allocate(childrenSize * STATIC_TRIE_NODE_SIZE_BYTE);
        fileChannel.read(buffWrite, childrenPosition);
        ((Buffer) buffWrite).flip();// ByteBuffer - Buffer - JDK 9+ covariant type
        for (int i = 0; i < childrenSize; i++) {
            StaticNGramTrieNode trieNode = new StaticNGramTrieNode();
            int wordId = trieNode.readNodeInformation(buffWrite);
            children.put(wordId, trieNode);
        }
    }

    /**
     * Read the node information contained into the given buffer to this node (without any check).<br>
     *
     * @param buffWrite the buffer containing node information.
     * @return this node word id (read from buffer)
     */
    public int readNodeInformation(ByteBuffer buffWrite) {
        int wordId = buffWrite.getInt();
        this.childrenSize = buffWrite.getInt();
        this.childrenPosition = buffWrite.getInt();
        this.frequency = buffWrite.getDouble();
        this.childrenBackoffWeight = buffWrite.getDouble();
        return wordId;
    }

    @Override
    public int getChildrenSize() {
        return this.childrenSize;
    }
    // ========================================================================
}
