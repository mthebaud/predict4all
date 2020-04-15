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
import org.predict4all.nlp.ngram.trie.DynamicNGramTrieNode;
import org.predict4all.nlp.trainer.configuration.NGramPruningMethod;
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
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represent a training dictionary : a ngram dictionary used while training an
 * ngram model.<br>
 * This dictionary is useful because it supports dynamic insertion and
 * probabilities computing... It is always use {@link DynamicNGramTrieNode}.<br>
 * <p>
 * The default training dictionary is not meant to be opened : it saves the trie
 * structure into a file to be then loaded as a
 * {@link StaticNGramTrieDictionary}. However, {@link DynamicNGramDictionary}
 * implements a dynamic dictionary that can be saved/opened with dynamic nodes.
 *
 * @author Mathieu THEBAUD
 */
public class TrainingNGramDictionary extends AbstractNGramDictionary<DynamicNGramTrieNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingNGramDictionary.class);
    public static final DecimalFormat NGRAM_COUNT_FORMAT = new DecimalFormat("###,###,###,###,###");

    protected TrainingNGramDictionary(int maxOrderP) {
        this(new DynamicNGramTrieNode(), maxOrderP);
    }

    protected TrainingNGramDictionary(DynamicNGramTrieNode root, int maxOrderP) {
        super(root, maxOrderP);
    }

    // IMPLEMENTATIONS
    // ========================================================================
    @Override
    public DynamicNGramTrieNode getNodeForPrefix(int[] prefix, int index) {
        return rootNode.getNodeFor(prefix, index, -1);
    }

    @Override
    public boolean checkChildrenLoading(DynamicNGramTrieNode node) {
        return node.getChildren() != null;
    }

    @Override
    public void putAndIncrementBy(int[] ngram, int increment) {
        this.putAndIncrementBy(ngram, 0, increment);
    }

    @Override
    public void putAndIncrementBy(int[] ngram, int index, int increment) {
        this.rootNode.putAndIncrementBy(ngram, index, increment);
    }
    // ========================================================================

    // IO
    // ========================================================================
    @Override
    public void saveDictionary(File dictionaryFile) throws IOException {
        // Save all ngrams block
        try (FileChannel fileChannel = FileChannel.open(dictionaryFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            // Shift position in file to save space for root block and dictionary
            // information
            fileChannel.position(getRootBlockSize() + DICTIONARY_INFORMATION_BYTE_COUNT);

            // Save each ngram level
            for (int order = this.maxOrder; order >= 1; order--) {
                LOGGER.info("Save NGram level {}", order);
                executeWriteLevelOnRoot(fileChannel, order);
            }

            // Save general information
            LOGGER.info("All level saved, will now save general information and root block");
            fileChannel.position(0);
            ByteBuffer buffWrite = ByteBuffer.allocateDirect(DICTIONARY_INFORMATION_BYTE_COUNT);
            writeDictionaryInfo(buffWrite);
            ((Buffer) buffWrite).flip();// ByteBuffer - Buffer - JDK 9+ covariant type
            fileChannel.write(buffWrite);

            // Save root level
            this.executeWriteLevelOnRoot(fileChannel, 0);
        }
    }

    /**
     * Call the correct node method to save a trie level to file.
     *
     * @param fileChannel the file channel where trie is saved
     * @param level       the level to save
     * @throws IOException if writing fail
     */
    protected void executeWriteLevelOnRoot(FileChannel fileChannel, int level) throws IOException {
        rootNode.writeLevelForStaticUse(fileChannel, -1, 0, level);
    }

    /**
     * @return should return the byte count needed to save the root block (useful to
     * shift data in file to save the root in first position in file)
     */
    protected long getRootBlockSize() {
        return AbstractNGramTrieNode.STATIC_TRIE_NODE_SIZE_BYTE;
    }
    // ========================================================================

    // PROBABILITIES
    // ========================================================================
    @Override
    public void updateProbabilities(double[] d) {
        long startProb = System.currentTimeMillis();
        LOGGER.info("Start computing ngram probabilities with d={}", d);
        rootNode.computeProbabilityForChildren(0, d, true);
        LOGGER.info("NGram probabilities computed in {} ms", System.currentTimeMillis() - startProb);
    }

    @Override
    public void updateProbabilities(int[] prefix, int prefixIndex, double[] d) {
        int level = prefix.length - prefixIndex - 1;
        final DynamicNGramTrieNode nodeFor = rootNode.getNodeFor(prefix, prefixIndex, prefix.length - 1);
        if (nodeFor != null) {
            nodeFor.computeProbabilityForChildren(level, d, false);
        } else {
            throw new IllegalArgumentException("No existing node for " + Arrays.toString(prefix) + " : can't compute probabilities");
        }
    }

    @Override
    public double[] computeD(TrainingConfiguration configuration) {
        if (configuration.getSmoothingDiscountValue() > 0) {
            double[] d = new double[this.getMaxOrder()];
            Arrays.fill(d, configuration.getSmoothingDiscountValue());
            return d;
        }
        AtomicInteger[] n1Count = new AtomicInteger[this.getMaxOrder()];
        Arrays.setAll(n1Count, i -> new AtomicInteger(0));
        AtomicInteger[] n2Count = new AtomicInteger[this.getMaxOrder()];
        Arrays.setAll(n2Count, i -> new AtomicInteger(0));
        LOGGER.info("D will be computed automatically, count ngram with count=1 and count=2 to compute d");
        rootNode.countOneAndTwoOccurenceNGrams(0, n1Count, n2Count);
        double[] dArray = new double[this.getMaxOrder()];
        for (int i = 0; i < this.getMaxOrder(); i++) {
            double d = (n1Count[i].get() * 1.0) / ((n1Count[i].get() * 1.0) + 2.0 * n2Count[i].get());
            d = Double.isNaN(d) ? 0.5 : d;
            dArray[i] = Math.min(Math.max(d, configuration.getSmoothingDiscountValueLowerBound()),
                    configuration.getSmoothingDiscountValueUpperBound());
            LOGGER.info("[ORDER {}] Found {} ngram with count=1, {} ngram with count=2, d is set to {}", i + 1, n1Count[i].get(), n2Count[i].get(),
                    dArray[i]);
        }
        return dArray;
    }

    /**
     * Execute a pruning on the dictionary.<br>
     * Pruning is implemented with a "weighted difference" algorithm : difference is computed between high order model and a lower order model (e.g. difference between 4-gram - 3gram, then 3-gram - 2-gram) and if the difference is bellow a certain
     * level (threshold), the high order model is deleted.<br>
     * Difference pruning is executed for max order to bigram level, probabilities are computed again after the pruning.
     *
     * @param thresholdPruning pruning threshold (every ngram with prob difference
     *                         bellow this threshold are deleted)
     * @param configuration    training configuration ({@link #computeD(TrainingConfiguration)} configuration)
     * @param pruningMethod    pruning method to use
     */
    public void pruneNGramsWeightedDifference(final double thresholdPruning, TrainingConfiguration configuration, NGramPruningMethod pruningMethod) {
        this.updateProbabilities(this.computeD(configuration));
        if (thresholdPruning > 0.0) {
            LOGGER.info("Start pruning ngrams using weighted difference ({}) and threshold {}", pruningMethod, thresholdPruning);
            for (int order = maxOrder; order > 1; order--) {
                List<Pair<int[], Integer>> toDelete = new ArrayList<>(20_000);
                rootNode.listTrieLeaves(new int[order - 1], -1, 0, order, (ngram, wantedWord) -> {
                    double orderProb = pruningMethod == NGramPruningMethod.WEIGHTED_DIFFERENCE_FULL_PROB
                            ? this.getProbability(ngram, 0, ngram.length, wantedWord)
                            : this.getRawProbability(ngram, 0, ngram.length, wantedWord);
                    double lowerOrderProb = pruningMethod == NGramPruningMethod.WEIGHTED_DIFFERENCE_FULL_PROB
                            ? this.getProbability(ngram, 1, ngram.length - 1, wantedWord)
                            : this.getRawProbability(ngram, 1, ngram.length - 1, wantedWord);
                    double weightDiff = (orderProb) * (Math.log(orderProb) - Math.log(lowerOrderProb));
                    if (weightDiff <= thresholdPruning) {
                        toDelete.add(Pair.of(ngram, wantedWord));
                    }
                });
                LOGGER.info("Found {} {}-gram to prune", toDelete.size(), order);

                // Delete each pruned ngram
                for (Pair<int[], Integer> ngramToDelete : toDelete) {
                    this.getNodeForPrefix(ngramToDelete.getLeft(), 0).getChildren().remove(ngramToDelete.getRight());
                }
                LOGGER.info("{} {}-gram removed, will now compact and compute probabilities again", toDelete.size(), order);
                this.compact();
                this.updateProbabilities(this.computeD(configuration));
            }
        } else {
            LOGGER.info("Ignore ngram pruning because threshold = {}", thresholdPruning);
        }
    }

    public void pruneNGramsCount(int countThreshold, TrainingConfiguration configuration) {
        LOGGER.info("Start pruning ngrams using raw count and threshold {}", countThreshold);
        double[] d = this.computeD(configuration);
        for (int order = maxOrder; order > 1; order--) {
            rootNode.pruningCountingNGram(0, order, countThreshold);
            this.compact();
        }
        this.updateProbabilities(d);
    }

    public void pruneNGramsOrderCount(int[] counts, TrainingConfiguration configuration) {
        LOGGER.info("Start pruning ngrams using order counts and threshold {}", Arrays.toString(counts));
        double[] d = this.computeD(configuration);
        for (int order = maxOrder; order > 1; order--) {
            rootNode.pruningCountingNGram(0, order, counts[order - 1]);
            this.compact();
        }
        this.updateProbabilities(d);
    }
    // ========================================================================

    // NOT SUPPORTED
    // ========================================================================
    @Override
    public void close() throws Exception {
    }

    @Override
    protected void openDictionary(File dictionaryFile) throws IOException {
        throw new UnsupportedOperationException("Training ngram dictionary can't be opened");
    }
    // ========================================================================

    // SPECIFIC
    // ========================================================================

    /**
     * Create an empty training ngram trie dictionary
     *
     * @param maxOrder the max possible order for the dictionary
     * @return an new empty dictionary
     */
    public static TrainingNGramDictionary create(int maxOrder) {
        return new TrainingNGramDictionary(maxOrder);
    }

    public Map<Integer, Pair<Integer, Integer>> countNGrams() {
        Map<Integer, Pair<Integer, Integer>> ngramCounts = new HashMap<>();
        for (int order = 1; order <= maxOrder; order++) {
            AtomicInteger uniqueCounter = new AtomicInteger(0);
            AtomicInteger totalCounter = new AtomicInteger(0);
            rootNode.countNGram(0, order, totalCounter, uniqueCounter);
            ngramCounts.put(order, Pair.of(uniqueCounter.get(), totalCounter.get()));
            LOGGER.info("{} total {}-gram detected, with {} unique {}-gram", NGRAM_COUNT_FORMAT.format(totalCounter.get()), order,
                    NGRAM_COUNT_FORMAT.format(uniqueCounter.get()), order);
        }
        return ngramCounts;
    }
    // ========================================================================

}
