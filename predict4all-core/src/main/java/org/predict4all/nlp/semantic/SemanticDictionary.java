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
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.prediction.model.AbstractPredictionToCompute;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.utils.SingleThreadDoubleAdder;
import org.predict4all.nlp.words.WordDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
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
    // NEW ADDED METHODS TO INTEGRATE SEMANTIC (By C. BEN KHELIL)
    //========================================================================
    /**
     * method used to display the predicted words and their scores on the LSA side.
     * @param semanticPredictions
     * @param ksr
     */
    public static void displaySemanticPredictions(List<Pair<String, Double>> semanticPredictions, int ksr) {
        System.out.println("Top Semantic Predictions:");
            // Sort the list based on scores in descending order
            Collections.sort(semanticPredictions, Comparator.comparing(Pair::getRight, Comparator.reverseOrder()));

            int count = 0;
            for (Pair<String, Double> pair : semanticPredictions) {
                if (count < ksr) {
                    System.out.println("Word: " + pair.getLeft() + ", Score: " + pair.getRight());
                    count++;
                } else {
                    break; // Stop after displaying the first three
                }
            }
        }

    /**
     * Method to return semantic score for the word, 0 otherwise
     * @param semanticPredictions
     * @param word
     * @return
     */
    public static double findSemanticScore(List<Pair<String, Double>> semanticPredictions, String word) {
        for (Pair<String, Double> pair : semanticPredictions) {
            if (pair.getLeft().equals(word)) {
                return pair.getRight();
            }
        }
        return 0.0; // Return 0 if no semantic score is found for the word
    }

    /**
     * This two following methods can be used to implement your own logic to determine if a word is meaningful for the context.
     * @param tokens
     * @return
     */
    public static List<String> extractMeaningfulWords(List<Token> tokens) {
        List<String> meaningfulWords = new ArrayList<>();
        for (Token token : tokens) {
            String word = token.getText();

            // Implement your own logic to determine if a word is meaningful
            // You can use dictionary-based approaches, language models, or other techniques
            if (isWordMeaningful(word)) {
                meaningfulWords.add(word);
            }
        }
        return meaningfulWords;
    }

    /**
     * @param word
     * @return
     */
    private static boolean isWordMeaningful(String word) {
        // This is just a basic example, you can modify it to suit your needs
        return word.length() >1;
    }

    /**
     * Method to filter the prediction list to keep only the words common between the n-gram dictionary and those of the LSA.
     * A file containing the uncommon words will be generated.
     * @param predictions
     * @param semanticPredictions
     * @param dictionary
     * @return
     */
    public static List<AbstractPredictionToCompute>  filterPredictions(List<AbstractPredictionToCompute> predictions,List<Pair<String,Double>> semanticPredictions, WordDictionary dictionary) {
        List<AbstractPredictionToCompute> filteredPredictions = new ArrayList<>();
        List<AbstractPredictionToCompute> stopWords = new ArrayList<>();
        for (AbstractPredictionToCompute prediction : predictions) {
            boolean found =false;
            String wordPredicted = String.valueOf(dictionary.getWord(prediction.getWordId()));
            for (Pair<String, Double> semanticPrediction : semanticPredictions) {
                String word = semanticPrediction.getLeft();
                if (word.equals(wordPredicted)) {
                    filteredPredictions.add(prediction);
                    found=true;
                    break; // Move on to the next prediction
                }
            }
            if (!found)
                stopWords.add(prediction);
        }

        // to generate file containing stopwords
        String filePath = "stopwords.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (AbstractPredictionToCompute word : stopWords) {
                writer.write(String.valueOf(dictionary.getWord(word.getWordId())));
                writer.newLine();
            }
            System.out.println("Words written to stopwords.txt successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to stopwords.txt: " + e.getMessage());
        }


        return filteredPredictions;
    }

    /**
     * Method that calculates the geometric interpolation between n-gram predictions and LSA.
     * @param predictions
     * @param semanticPredictions
     * @return
     */
    public static List<Pair<String, Double>> geometric_interpolation(List<AbstractPredictionToCompute> predictions, List<Pair<String,Double>> semanticPredictions) {
        //if we want to filter predictions and generate file containing words that do not exist in the dictionary
        //predictions=filterPredictions(predictions,semanticPredictions,dictionary);
        List<Pair<String, Double>> interpolatedList = new ArrayList<>();
        for (AbstractPredictionToCompute prediction : predictions) {
            String word = prediction.getPrediction();
            double score = prediction.getScore();
            // Find the corresponding semantic prediction for the word
            double semanticScore = findSemanticScore(semanticPredictions, word);

            double interpolatedScore = score * semanticScore;

            // Check if the word exists in the interpolatedList
            boolean wordExists = false;
            for (Pair<String, Double> pair : interpolatedList) {
                if (pair.getLeft().equals(word)) {
                    pair = Pair.of(pair.getLeft(), pair.getRight() * interpolatedScore);
                    wordExists = true;
                    break;
                }
            }
            // If the word doesn't exist, add it to the interpolatedList
            if (!wordExists) {
                interpolatedList.add(Pair.of(word, interpolatedScore));
            }
        }

        // Calculate the sum of all scores
        double sum = 0.0;
        for (Pair<String, Double> pair : interpolatedList) {
            double score = pair.getRight();
            sum += score;
        }

        List<Pair<String, Double>> normalizedList = new ArrayList<>();
        Map<String, Boolean> keyExists = new HashMap<>(); // Map to track whether key exists

        for (Pair<String, Double> pair : interpolatedList) {
            String key = pair.getLeft();
            double score = pair.getRight();
            double normalizedScore = score / sum;

            // Check if the key exists in the map
            if (!keyExists.containsKey(key)) {
                keyExists.put(key, true); // Add key to map
                normalizedList.add(new Pair<>(key, normalizedScore)); // Add the normalized pair
            }
        }

        /*
        // if we want to display the list:
        System.out.println("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-* After interpolation normalization    -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
        normalizedList.stream().sorted((p1,p2)->Double.compare(p2.getRight(),p1.getRight())).limit(5).forEach(
                p -> System.out.println(p.getLeft()+" : "+p.getRight()));
        System.out.println("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-* End interpolation normalization    -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
         */

        //return interpolatedList;
        return normalizedList;
    }

    /**
     * Method that calculates the Linear interpolation between n-gram predictions and LSA
     * @param predictions
     * @param semanticPredictions
     * @return
     */
    public static List<Pair<String, Double>> linear_interpolation(List<AbstractPredictionToCompute> predictions,List<Pair<String,Double>> semanticPredictions){
        //if we want to filter predictions and generate file containing words that do not exist in the dictionary
        //predictions=filterPredictions(predictions,semanticPredictions,dictionary);
        List<Pair<String, Double>> interpolatedList = new ArrayList<>();
        for (AbstractPredictionToCompute prediction : predictions) {
            String word = prediction.getPrediction();
            double score = prediction.getScore();
            // Find the corresponding semantic prediction for the word
            double semanticScore = findSemanticScore(semanticPredictions, word);

            double interpolatedScore = score + semanticScore;

            // Check if the word exists in the interpolatedList
            boolean wordExists = false;
            for (Pair<String, Double> pair : interpolatedList) {
                if (pair.getLeft().equals(word)) {
                    pair = Pair.of(pair.getLeft(), pair.getRight() * interpolatedScore);
                    wordExists = true;
                    break;
                }
            }

            // If the word doesn't exist, add it to the interpolatedList
            if (!wordExists) {
                interpolatedList.add(Pair.of(word, interpolatedScore));
            }
        }

        // Calculate the sum of all scores
        double sum = 0.0;
        for (Pair<String, Double> pair : interpolatedList) {
            double score = pair.getRight();
            sum += score;
        }

        List<Pair<String, Double>> normalizedList = new ArrayList<>();
        Map<String, Boolean> keyExists = new HashMap<>(); // Map to track whether key exists

        for (Pair<String, Double> pair : interpolatedList) {
            String key = pair.getLeft();
            double score = pair.getRight();
            double normalizedScore = score / sum;

            // Check if the key exists in the map
            if (!keyExists.containsKey(key)) {
                keyExists.put(key, true); // Add key to map
                normalizedList.add(new Pair<>(key, normalizedScore)); // Add the normalized pair
            }
        }

        /*
        // if we want to display the list:
        System.out.println("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-* After interpolation normalization    -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
        normalizedList.stream().sorted((p1,p2)->Double.compare(p2.getRight(),p1.getRight())).limit(5).forEach(
                p -> System.out.println(p.getLeft()+" : "+p.getRight()));
        System.out.println("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-* End interpolation normalization    -*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
         */

        //return interpolatedList;
        return normalizedList;
    }

}
