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

package org.predict4all.nlp.trainer.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.predict4all.nlp.Tag;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TrainingConfiguration {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    @Expose
    private int ngramOrder = 4;

    /**
     * The threshold to replace upper case word.<br>
     * If threshold is 35%, then if more than 35% of a word occurrence appears
     * in lower case, the word will be replaced by its lower case version
     * everywhere.
     */
    @Expose
    private double upperCaseReplacementThreshold = 0.35;

    /**
     * The threshold to convert the case of a given word to the valid dictionary case.<br>
     * This means that if a word in the valid dictionary has a frequency higher than this threshold,
     * invalid case word will be converted to the valid dictionary case.<br>
     * 1.0 = never replace any word case with the valid dictionary case
     *
     * @return the frequency threshold to use the valid dictionary case
     */
    @Expose
    private double convertCaseFromDictionaryModelThreshold = 1E-8;

    /**
     * The threshold to consider a word as unknown.<br>
     * In the training corpora, if a word count is equals or bellow this threshold,
     * it will be replaced by {@link Tag#UNKNOWN}.
     * if 0, every word are considered "known"
     */
    @Expose
    private int unknownWordCountThreshold = 0;

    /**
     * The threshold to directly consider a word as known.<br>
     * The will skip vocabulary and case check for the word and add it directly to valid word list.
     */
    @Expose
    private int directlyValidWordCountThreshold = 20;

    @Expose
    private NGramPruningMethod pruningMethod = NGramPruningMethod.ORDER_COUNT;

    @Expose
    private double ngramPruningWeightedDifferenceThreshold = 1E-4;

    @Expose
    private int ngramPruningCountThreshold = 2;

    // TODO : should be a list/array ?
    @Expose
    private int[] ngramPruningOrderCountThresholds = new int[]{-1, 2, 3, 4};

    @Expose
    private double smoothingDiscountValue = -1.0;

    @Expose
    private double smoothingDiscountValueLowerBound = 0.1;

    @Expose
    private double smoothingDiscountValueUpperBound = 1.0;

    @Expose
    private int lsaWindowSize = 100;

    @Expose
    private int lsaFrequentWordSize = 5_000;

    @Expose
    private int lsaVocabularySize = 80_000;

    @Expose
    private int lsaTargetSvdSize = 200;

    @Expose
    private int lsaDensitySize = 100;

    @Expose
    private String baseWordDictionaryPath;

    @Expose
    private String stopWordDictionaryPath;

    private transient File corpus;

    private TrainingConfiguration(File corpus) {
        super();
        this.corpus = corpus;
    }

    private TrainingConfiguration(TrainingConfiguration other) {
        super();
        this.ngramOrder = other.ngramOrder;
        this.upperCaseReplacementThreshold = other.upperCaseReplacementThreshold;
        this.convertCaseFromDictionaryModelThreshold = other.convertCaseFromDictionaryModelThreshold;
        this.unknownWordCountThreshold = other.unknownWordCountThreshold;
        this.directlyValidWordCountThreshold = other.directlyValidWordCountThreshold;
        this.pruningMethod = other.pruningMethod;
        this.ngramPruningWeightedDifferenceThreshold = other.ngramPruningWeightedDifferenceThreshold;
        this.ngramPruningCountThreshold = other.ngramPruningCountThreshold;
        this.ngramPruningOrderCountThresholds = other.ngramPruningOrderCountThresholds;
        this.smoothingDiscountValue = other.smoothingDiscountValue;
        this.smoothingDiscountValueLowerBound = other.smoothingDiscountValueLowerBound;
        this.smoothingDiscountValueUpperBound = other.smoothingDiscountValueUpperBound;
        this.lsaWindowSize = other.lsaWindowSize;
        this.lsaFrequentWordSize = other.lsaFrequentWordSize;
        this.lsaVocabularySize = other.lsaVocabularySize;
        this.lsaTargetSvdSize = other.lsaTargetSvdSize;
        this.lsaDensitySize = other.lsaDensitySize;
        this.baseWordDictionaryPath = other.baseWordDictionaryPath;
        this.stopWordDictionaryPath = other.stopWordDictionaryPath;
        this.corpus = other.corpus;
    }

    public File getCorpus() {
        return corpus;
    }

    private void setCorpus(File corpus) {
        this.corpus = corpus;
    }

    public int getNgramOrder() {
        return ngramOrder;
    }

    public void setNgramOrder(int ngramOrder) {
        this.ngramOrder = ngramOrder;
    }

    public double getUpperCaseReplacementThreshold() {
        return upperCaseReplacementThreshold;
    }

    public void setUpperCaseReplacementThreshold(double upperCaseReplacementThreshold) {
        this.upperCaseReplacementThreshold = upperCaseReplacementThreshold;
    }

    public double getConvertCaseFromDictionaryModelThreshold() {
        return convertCaseFromDictionaryModelThreshold;
    }

    public void setConvertCaseFromDictionaryModelThreshold(double convertCaseFromDictionaryModelThreshold) {
        this.convertCaseFromDictionaryModelThreshold = convertCaseFromDictionaryModelThreshold;
    }

    public int getUnknownWordCountThreshold() {
        return unknownWordCountThreshold;
    }

    public void setUnknownWordCountThreshold(int unknownWordCountThreshold) {
        this.unknownWordCountThreshold = unknownWordCountThreshold;
    }

    public int getDirectlyValidWordCountThreshold() {
        return directlyValidWordCountThreshold;
    }

    public void setDirectlyValidWordCountThreshold(int directlyValidWordCountThreshold) {
        this.directlyValidWordCountThreshold = directlyValidWordCountThreshold;
    }

    public NGramPruningMethod getPruningMethod() {
        return pruningMethod;
    }

    public void setPruningMethod(NGramPruningMethod pruningMethod) {
        this.pruningMethod = pruningMethod;
    }

    public double getNgramPruningWeightedDifferenceThreshold() {
        return ngramPruningWeightedDifferenceThreshold;
    }

    public void setNgramPruningWeightedDifferenceThreshold(double ngramPruningWeightedDifferenceThreshold) {
        this.ngramPruningWeightedDifferenceThreshold = ngramPruningWeightedDifferenceThreshold;
    }

    public int getNgramPruningCountThreshold() {
        return ngramPruningCountThreshold;
    }

    public void setNgramPruningCountThreshold(int ngramPruningCountThreshold) {
        this.ngramPruningCountThreshold = ngramPruningCountThreshold;
    }

    public int[] getNgramPruningOrderCountThresholds() {
        return ngramPruningOrderCountThresholds;
    }

    public void setNgramPruningOrderCountThresholds(int[] ngramPruningOrderCountThresholds) {
        this.ngramPruningOrderCountThresholds = ngramPruningOrderCountThresholds;
    }

    public double getSmoothingDiscountValue() {
        return smoothingDiscountValue;
    }

    public void setSmoothingDiscountValue(double smoothingDiscountValue) {
        this.smoothingDiscountValue = smoothingDiscountValue;
    }

    public double getSmoothingDiscountValueLowerBound() {
        return smoothingDiscountValueLowerBound;
    }

    public void setSmoothingDiscountValueLowerBound(double smoothingDiscountValueLowerBound) {
        this.smoothingDiscountValueLowerBound = smoothingDiscountValueLowerBound;
    }

    public double getSmoothingDiscountValueUpperBound() {
        return smoothingDiscountValueUpperBound;
    }

    public void setSmoothingDiscountValueUpperBound(double smoothingDiscountValueUpperBound) {
        this.smoothingDiscountValueUpperBound = smoothingDiscountValueUpperBound;
    }

    public int getLsaWindowSize() {
        return lsaWindowSize;
    }

    public void setLsaWindowSize(int lsaWindowSize) {
        this.lsaWindowSize = lsaWindowSize;
    }

    public int getLsaFrequentWordSize() {
        return lsaFrequentWordSize;
    }

    public void setLsaFrequentWordSize(int lsaFrequentWordSize) {
        this.lsaFrequentWordSize = lsaFrequentWordSize;
    }

    public int getLsaVocabularySize() {
        return lsaVocabularySize;
    }

    public void setLsaVocabularySize(int lsaVocabularySize) {
        this.lsaVocabularySize = lsaVocabularySize;
    }

    public int getLsaTargetSvdSize() {
        return lsaTargetSvdSize;
    }

    public void setLsaTargetSvdSize(int lsaTargetSvdSize) {
        this.lsaTargetSvdSize = lsaTargetSvdSize;
    }

    public int getLsaDensitySize() {
        return lsaDensitySize;
    }

    public void setLsaDensitySize(int lsaDensitySize) {
        this.lsaDensitySize = lsaDensitySize;
    }

    public String getBaseWordDictionaryPath() {
        return baseWordDictionaryPath;
    }

    public void setBaseWordDictionaryPath(String baseWordDictionaryPath) {
        this.baseWordDictionaryPath = baseWordDictionaryPath;
    }

    public String getStopWordDictionaryPath() {
        return stopWordDictionaryPath;
    }

    public void setStopWordDictionaryPath(String stopWordDictionaryPath) {
        this.stopWordDictionaryPath = stopWordDictionaryPath;
    }

    // BUILDER
    //========================================================================
    public static TrainingConfiguration defaultConfiguration() {
        return defaultConfiguration(null);
    }

    public static TrainingConfiguration defaultConfiguration(File corpus) {
        return new TrainingConfiguration(corpus);
    }

    public static TrainingConfiguration from(TrainingConfiguration other) {
        return new TrainingConfiguration(other);
    }
    //========================================================================

    // IO
    //========================================================================
    public void saveTo(File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            GSON.toJson(this, pw);
        }
    }

    public static TrainingConfiguration loadFrom(File file, File corpus) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Scanner scan = new Scanner(file, StandardCharsets.UTF_8.name())) {
            while (scan.hasNextLine()) {
                content.append(scan.nextLine()).append("\n");
            }
        }
        TrainingConfiguration loaded = GSON.fromJson(content.toString(), TrainingConfiguration.class);
        loaded.setCorpus(corpus);
        return loaded;
    }
    //========================================================================

}
