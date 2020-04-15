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

package org.predict4all.nlp.semantic;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.predict4all.nlp.prediction.model.AbstractPredictionToCompute;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.utils.SingleThreadDoubleAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Represents a semantic dictionary to be used to predict next words.<br>
 * <strong>WARNING : THIS IS A WIP</strong>
 *
 * @author Mathieu THEBAUD
 */
public class SemanticDictionary {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticDictionary.class);
    private static final int NO_VALUE_ENTRY = -1;
    private final TIntIntHashMap rowIndexes;
    private final double[][] semanticMatrix;
    private final TIntDoubleHashMap densitiesMap;
    private final int rowCount, columnCount;

    private SemanticDictionary(TIntIntHashMap rowIndexes, double[][] semanticMatrix, TIntDoubleHashMap densitiesMap) {
        super();
        this.rowIndexes = rowIndexes;
        this.semanticMatrix = semanticMatrix;
        this.rowCount = semanticMatrix.length;
        this.columnCount = semanticMatrix[0].length;
        this.densitiesMap = densitiesMap;
    }

    public TIntDoubleHashMap getDensitiesMap() {
        return densitiesMap;
    }

    private Pair<Double, TIntDoubleHashMap> computeScoreMapFor(double[] wordRow, final List<AbstractPredictionToCompute> predictions, final double constrastFactor) {
        TIntDoubleHashMap scoreMap = new TIntDoubleHashMap(rowCount, 0.9f, NO_VALUE_ENTRY, NO_VALUE_ENTRY);
        double minVal = 0.0;
        // Compute each probabilities (each cosine angle)
        for (AbstractPredictionToCompute prediction : predictions) {
            int rowIndex = rowIndexes.get(prediction.getWordId());
            if (rowIndex >= 0) {
                final double angle = cosineAngle(wordRow, semanticMatrix[rowIndex]);
                scoreMap.put(prediction.getWordId(), angle);
                minVal = Math.min(minVal, angle);
            }
        }

        // Transform value to contrast and sum to be able to normalize
        final double minValFinal = minVal;
        SingleThreadDoubleAdder sum = new SingleThreadDoubleAdder(0.0);
        scoreMap.transformValues(value -> sum.addAndReturnAdded(Math.pow(value - minValFinal, constrastFactor)));

        return Pair.of(sum.sum(), scoreMap);
    }

    public Pair<Double, TIntDoubleHashMap> getSimilarityCosineFor(Collection<Integer> wordIds, final List<AbstractPredictionToCompute> predictions,
                                                                  final double constrastFactor) {
        double[] wordFactors = new double[this.columnCount];
        // Create a row that sum up all words factors into one
        boolean foundKnownWord = false;
        for (int wordId : wordIds) {
            int rowIndexForWord = this.rowIndexes.get(wordId);
            if (rowIndexForWord >= 0) {
                foundKnownWord |= true;
                final double[] rowForWord = semanticMatrix[rowIndexForWord];
                for (int c = 0; c < columnCount; c++) {
                    wordFactors[c] += rowForWord[c];
                }
            }
        }
        // Normalize then compute
        if (foundKnownWord) {
            normalizeRow(wordFactors);
            return computeScoreMapFor(wordFactors, predictions, constrastFactor);
        } else {
            return null;
        }
    }

    // UTILS VECTOR
    // ========================================================================
    static double cosineAngle(double[] row1, double[] row2) {
        double prod = 0;
        for (int c = 0; c < row1.length; c++) {
            prod += row1[c] * row2[c];
        }
        return prod;
    }

    private static double length(double[] v) {
        double sum = 0;
        for (int i = 0; i < v.length; i++) {
            sum += (v[i]) * (v[i]);
        }
        if (sum < 0) {
            throw new ArithmeticException("Vector val sum is negative, int overflow ?");
        }
        return Math.sqrt(sum);
    }

    public static void normalizeRow(double[] row) {
        final double length = length(row);
        if (length > 0) {
            for (int c = 0; c < row.length; c++) {
                row[c] = row[c] / length;
            }
        }
    }
    //========================================================================

    // LOADING
    //========================================================================
    public static SemanticDictionary loadDictionary(File semanticDataFile, SemanticDictionaryConfiguration configuration) throws IOException {
        long start = System.currentTimeMillis();
        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(semanticDataFile)))) {
            // Read base information
            final int rowCount = dis.readInt();
            final int columnCount = dis.readInt();

            // Read indexes
            final TIntIntHashMap rowIndexes = new TIntIntHashMap(rowCount, 0.9f, NO_VALUE_ENTRY, NO_VALUE_ENTRY);
            for (int i = 0; i < rowCount; i++) {
                rowIndexes.put(dis.readInt(), dis.readInt());
            }
            rowIndexes.compact();
            LOGGER.info("Read {} matrix row indexes from semantic file", rowIndexes.size());

            // Read matrix
            final double[][] matrix = new double[rowCount][columnCount];
            for (int r = 0; r < rowCount; r++) {
                for (int c = 0; c < columnCount; c++) {
                    matrix[r][c] = dis.readDouble();
                }
            }
            LOGGER.info("Read {}x{} semantic matrix in {} ms", rowCount, columnCount, System.currentTimeMillis() - start);

            // Read densities
            long startD = System.currentTimeMillis();
            double min = 0.0, max = 0.0;
            final double[] densities = new double[rowCount];
            for (int r = 0; r < rowCount; r++) {
                densities[r] = dis.readDouble();
                min = Math.min(min, densities[r]);
                max = Math.max(max, densities[r]);
            }
            //Scale between bounds
            double tmin = Double.MAX_VALUE, tmax = Double.MIN_VALUE;
            for (int r = 0; r < rowCount; r++) {
                densities[r] = configuration.getSemanticDensityMinBound()
                        + ((densities[r] / (max)) * (configuration.getSemanticDensityMaxBound() - configuration.getSemanticDensityMinBound()));
                tmin = Math.min(tmin, densities[r]);
                tmax = Math.max(tmax, densities[r]);
            }
            LOGGER.info("LSA densities scaled in {} ms, min = {}, max = {}", System.currentTimeMillis() - startD, tmin, tmax);
            // Convert to map
            TIntDoubleHashMap densitiesMap = new TIntDoubleHashMap(densities.length);
            rowIndexes.forEachKey(wordId -> {
                densitiesMap.put(wordId, densities[rowIndexes.get(wordId)]);
                return true;
            });
            densitiesMap.compact();

            LOGGER.info("Semantic matrix and densities loaded in {} ms", System.currentTimeMillis() - start);
            return new SemanticDictionary(rowIndexes, matrix, densitiesMap);
        }
    }
    //========================================================================

}
