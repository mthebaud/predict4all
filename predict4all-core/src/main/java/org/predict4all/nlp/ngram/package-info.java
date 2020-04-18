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

/**
 * Package containing everything about the NGram model used in Predict4All.<br>
 * Contains the ngram training algorithm in {@link org.predict4all.nlp.ngram.NGramDictionaryGenerator}<br>
 * Also contains {@link org.predict4all.nlp.ngram.trie.AbstractNGramTrieNode} : a trie structure that can be implemented in both ways : dynamic or static.<br>
 * This trie structure allow having a huge number of ngram available for probability computation without loading them into memory.
 *
 * @author Mathieu THEBAUD
 */
package org.predict4all.nlp.ngram;