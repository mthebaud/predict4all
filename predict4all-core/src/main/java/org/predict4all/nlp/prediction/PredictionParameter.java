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

package org.predict4all.nlp.prediction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.language.french.FrenchDefaultCorrectionRuleGenerator;
import org.predict4all.nlp.words.correction.CorrectionRule;
import org.predict4all.nlp.words.correction.CorrectionRuleNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains parameters to configure how {@link WordPredictor} is working.<br>
 * Changes to an instance of {@link PredictionParameter} while the predictor is running could be not reflected as some values are cached internally.
 *
 * @author Mathieu THEBAUD
 */
public class PredictionParameter {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    @Expose
    private boolean addNewWordsEnabled = true;

    @Expose
    private int minUseCountToValidateNewWord = 10;

    @Expose
    private boolean dynamicModelEnabled = true;

    @Expose
    private int minCountToProvidePrediction = 0;

    @Expose
    private int minCountToProvideCorrection = 0;

    @Expose
    private double dynamicModelMinimumWeight = 0.05;

    @Expose
    private boolean enableWordCorrection = false;

    @Expose
    private double correctionMaxCost = 3.5;

    @Expose
    private double correctionDefaultFactor = 0.5;

    @Expose
    private double correctionDefaultCost = 1.0;

    @Expose
    private boolean enableDebugInformation = false;

    @Expose
    private CorrectionRuleNode correctionRulesRoot;

    @Expose
    private Map<String, String> customParameters;

    private LanguageModel languageModel;

    public PredictionParameter(LanguageModel languageModel) {
        super();
        this.languageModel = languageModel;
    }

    /**
     * Enable/disable that new words can be added to dictionary while using the word predictor or while training the dynamic model.<br>
     * Note that added word could appears in predictions only if they appeared enough times, as suggestion by {@link #getMinUseCountToValidateNewWord()}
     *
     * @return true if new word add is enable
     */
    public boolean isAddNewWordsEnabled() {
        return addNewWordsEnabled;
    }

    /**
     * Enable/disable that new words can be added to dictionary while using the word predictor or while training the dynamic model.<br>
     * Note that added word could appears in predictions only if they appeared enough times, as suggestion by {@link #getMinUseCountToValidateNewWord()}
     *
     * @param addNewWordsEnabled true if you want the new word to be learned by dictionary
     * @return this prediction parameter
     */
    public PredictionParameter setAddNewWordsEnabled(boolean addNewWordsEnabled) {
        this.addNewWordsEnabled = addNewWordsEnabled;
        return this;
    }

    /**
     * Enable/disable the use of dynamic model when predicting next words.<br>
     * If enabled, it should always be combined with an instance of {@link org.predict4all.nlp.ngram.dictionary.DynamicNGramDictionary} given to {@link WordPredictor}.<br>
     * The model can be trained via the {@link WordPredictor#trainDynamicModel(String, boolean)}
     *
     * @return true if dynamic model is enabled
     */
    public boolean isDynamicModelEnabled() {
        return dynamicModelEnabled;
    }

    /**
     * Enable/disable the use of dynamic model when predicting next words.<br>
     * If enabled, it should always be combined with an instance of {@link org.predict4all.nlp.ngram.dictionary.DynamicNGramDictionary} given to {@link WordPredictor}.<br>
     * The model can be trained via the {@link WordPredictor#trainDynamicModel(String, boolean)}
     *
     * @param dynamicModelEnabled to enable/disable dynamic model
     * @return this prediction parameter
     */
    public PredictionParameter setDynamicModelEnabled(boolean dynamicModelEnabled) {
        this.dynamicModelEnabled = dynamicModelEnabled;
        return this;
    }

    /**
     * Useful to set the minimum weight for dynamic model when interpolating both static and dynamic model.<br>
     * Value should ranger between 0.0 and 0.5.
     * Should generally be low (0.05) - it helps the dynamic model to be more "used" when having few items.
     *
     * @return the dynamic model weight
     */
    public double getDynamicModelMinimumWeight() {
        return dynamicModelMinimumWeight;
    }

    /**
     * Useful to set the minimum weight for dynamic model when interpolating both static and dynamic model.<br>
     * Value should ranger between 0.0 and 0.5.
     * Should generally be low (0.05) - it helps the dynamic model to be more "used" when having few items.
     *
     * @param dynamicModelMinimumWeight minimum weight for dynamic model
     * @return this prediction parameter
     */
    public PredictionParameter setDynamicModelMinimumWeight(double dynamicModelMinimumWeight) {
        this.dynamicModelMinimumWeight = dynamicModelMinimumWeight;
        return this;
    }

    /**
     * The language model to be used to predict words.<br>
     * Typically implementations are provided by the framework (see {@link org.predict4all.nlp.language.french.FrenchLanguageModel})<br>
     * It should always be filled
     *
     * @return the selected language model
     */
    public LanguageModel getLanguageModel() {
        return languageModel;
    }

    /**
     * The language model to be used to predict words.<br>
     * Typically implementations are provided by the framework (see {@link org.predict4all.nlp.language.french.FrenchLanguageModel})<br>
     * It should always be filled
     *
     * @param languageModel the language model for predictor
     * @return this prediction parameter
     */
    public PredictionParameter setLanguageModel(LanguageModel languageModel) {
        this.languageModel = languageModel;
        return this;
    }

    /**
     * Minimum new word use count to be displayed in the prediction.<br>
     * This avoid having typing errors displayed as prediction results. Use are considered as when the predictor detect these word in user text.<br>
     * Setting this value to 0 will allow every typed word to be then directly predicted.<br>
     * Note that {@link #isAddNewWordsEnabled()} should be true to add new words to dictionary.
     *
     * @return the min use count of new word
     */
    public int getMinUseCountToValidateNewWord() {
        return minUseCountToValidateNewWord;
    }

    /**
     * Minimum new word use count to be displayed in the prediction.<br>
     * This avoid having typing errors displayed as prediction results. Use are considered as when the predictor detect these word in user text.<br>
     * Setting this value to 0 will allow every typed word to be then directly predicted.<br>
     * Note that {@link #isAddNewWordsEnabled()} should be true to add new words to dictionary.
     *
     * @param minUseCountToValidateNewWord the min use count of new word
     * @return this prediction parameter
     */
    public PredictionParameter setMinUseCountToValidateNewWord(int minUseCountToValidateNewWord) {
        this.minUseCountToValidateNewWord = minUseCountToValidateNewWord;
        return this;
    }

    /**
     * To enable/disable word correction.<br>
     * Enabling this parameter will use {@link #getCorrectionRulesRoot()} to determine the rules to be used by the correction algorithms. Enable correction without rule has no effect.
     *
     * @return true if the word is enabled
     */
    public boolean isEnableWordCorrection() {
        return enableWordCorrection;
    }

    /**
     * To enable/disable word correction.<br>
     * Enabling this parameter will use {@link #getCorrectionRulesRoot()} to determine the rules to be used by the correction algorithms. Enable correction without rule has no effect.
     *
     * @param enableWordCorrection to enable/disable word correction
     * @return this prediction parameter
     */
    public PredictionParameter setEnableWordCorrection(boolean enableWordCorrection) {
        this.enableWordCorrection = enableWordCorrection;
        return this;
    }

    /**
     * Determine how much correction will be applied to a same input.<br>
     * The higher this value, the more correction will be applied. This could result in displayed too much correction to the user.
     *
     * @return the max correction cost.
     */
    public double getCorrectionMaxCost() {
        return correctionMaxCost;
    }

    /**
     * Determine how much correction will be applied to a same input.<br>
     * The higher this value, the more correction will be applied. This could result in displayed too much correction to the user.
     *
     * @param correctionMaxCost the max correction cost
     * @return this prediction parameter
     */
    public PredictionParameter setCorrectionMaxCost(double correctionMaxCost) {
        this.correctionMaxCost = correctionMaxCost;
        return this;
    }

    /**
     * The default factor applied to correction rule if {@link CorrectionRule#getFactor()} is null.
     * The correction factor will influence how much the correction "counts" relatively to a correct word.
     *
     * @return correction default factor
     */
    public double getCorrectionDefaultFactor() {
        return correctionDefaultFactor;
    }

    /**
     * The default factor applied to correction rule if {@link CorrectionRule#getFactor()} is null.
     * The correction factor will influence how much the correction "counts" relatively to a correct word.
     *
     * @param correctionDefaultFactor correction default factor
     * @return this prediction parameter
     */
    public PredictionParameter setCorrectionDefaultFactor(double correctionDefaultFactor) {
        this.correctionDefaultFactor = correctionDefaultFactor;
        return this;
    }

    /**
     * The default cost applied to correction rule if {@link CorrectionRule#getCost()} is null.
     * The correction cost will influence how many correction are cumulated for a same input.<br>
     * Typically, correction costs are added to check that they are bellow {@link #getCorrectionMaxCost()}
     *
     * @return correction default cost
     */
    public double getCorrectionDefaultCost() {
        return correctionDefaultCost;
    }

    /**
     * The default cost applied to correction rule if {@link CorrectionRule#getCost()} is null.
     * The correction cost will influence how many correction are cumulated for a same input.<br>
     * Typically, correction costs are added to check that they are bellow {@link #getCorrectionMaxCost()}
     *
     * @param correctionDefaultCost correction default cost
     * @return this prediction parameter
     */
    public PredictionParameter setCorrectionDefaultCost(double correctionDefaultCost) {
        this.correctionDefaultCost = correctionDefaultCost;
        return this;
    }

    /**
     * The correction rules to apply if {@link #isEnableWordCorrection()} is enabled.<br>
     * Correction rule are organised as a tree to allow enabling/disabled a whole part of the tree.<br>
     * Correction rules can be created programmatically, or {@link FrenchDefaultCorrectionRuleGenerator} can be used.
     *
     * @return the correction rule node root
     */
    public CorrectionRuleNode getCorrectionRulesRoot() {
        return correctionRulesRoot;
    }

    /**
     * The correction rules to apply if {@link #isEnableWordCorrection()} is enabled.<br>
     * Correction rule are organised as a tree to allow enabling/disabled a whole part of the tree.<br>
     * Correction rules can be created programmatically, or {@link FrenchDefaultCorrectionRuleGenerator} can be used.
     *
     * @param correctionRulesRoot the correction rule node root
     * @return this prediction parameter
     */
    public PredictionParameter setCorrectionRulesRoot(CorrectionRuleNode correctionRulesRoot) {
        this.correctionRulesRoot = correctionRulesRoot;
        return this;
    }

    /**
     * A minimum count of char before having prediction result.<br>
     * This built-in feature allow prediction to be displayed only after a certain amount of user input.<br>
     * Typically, setting this value to 1 will disable next word prediction and will only predict current typed word prediction.
     *
     * @return minimum count before provide prediction
     */
    public int getMinCountToProvidePrediction() {
        return minCountToProvidePrediction;
    }

    /**
     * A minimum count of char before having prediction result.<br>
     * This built-in feature allow prediction to be displayed only after a certain amount of user input.<br>
     * Typically, setting this value to 1 will disable next word prediction and will only predict current typed word prediction.
     *
     * @param minCountToProvidePrediction minimum count before provide prediction
     * @return this prediction parameter
     */
    public PredictionParameter setMinCountToProvidePrediction(int minCountToProvidePrediction) {
        this.minCountToProvidePrediction = minCountToProvidePrediction;
        return this;
    }

    /**
     * A minimum count of char before having correction result integrated.<br>
     * Note that this has not effect if {@link #isEnableWordCorrection()} is disabled.<br>
     * Typically, setting this value to 3 will allow predictor to check for correction only once user typed 3 chars.
     *
     * @return minimum count before provide correction
     */
    public int getMinCountToProvideCorrection() {
        return minCountToProvideCorrection;
    }

    /**
     * A minimum count of char before having correction result integrated.<br>
     * Note that this has not effect if {@link #isEnableWordCorrection()} is disabled.<br>
     * Typically, setting this value to 3 will allow predictor to check for correction only once user typed 3 chars.
     *
     * @param minCountToProvideCorrection minimum count before provide correction
     * @return this prediction parameter
     */
    public PredictionParameter setMinCountToProvideCorrection(int minCountToProvideCorrection) {
        this.minCountToProvideCorrection = minCountToProvideCorrection;
        return this;
    }


    /**
     * To enable debug information while predictor is working.<br>
     * This should be enabled carefully : debug is expensive in memory and computing cost as it creates a lot of String instance.<br>
     * Typically this should never be enabled when using the predictor in production.<br>
     * Enabling debug will fill {@link WordPredictionResult#getDebugInformation()} and {@link WordPrediction#getDebugInformation()}
     *
     * @return debug enable/disable
     */
    public boolean isEnableDebugInformation() {
        return enableDebugInformation;
    }

    /**
     * To enable debug information while predictor is working.<br>
     * This should be enabled carefully : debug is expensive in memory and computing cost as it creates a lot of String instance.<br>
     * Typically this should never be enabled when using the predictor in production.<br>
     * Enabling debug will fill {@link WordPredictionResult#getDebugInformation()} and {@link WordPrediction#getDebugInformation()}
     *
     * @param enableDebugInformation debug enable/disable
     * @return this prediction parameter
     */
    public PredictionParameter setEnableDebugInformation(boolean enableDebugInformation) {
        this.enableDebugInformation = enableDebugInformation;
        return this;
    }

    /**
     * Free to use String,String map to had your custom parameters.<br>
     * This is just a helper as {@link PredictionParameter} can be loaded/saved with {@link #saveTo(File)} and {@link #loadFrom(LanguageModel, File)}
     * It allows you to add your custom prediction relative configuration parameter without having to save them in a different file
     *
     * @return the custom parameters on this prediction parameters
     */
    public Map<String, String> getCustomParameters() {
        if (customParameters == null) {
            this.customParameters = new HashMap<>();
        }
        return customParameters;
    }

    // IO
    //========================================================================

    /**
     * Save this prediction parameters to a config file that could be later loaded with {@link #loadFrom(LanguageModel, File)}<br>
     * This implementation used JSON to save values.
     *
     * @param file path to the prediction parameter file
     * @throws IOException if saving fails
     */
    public void saveTo(File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            GSON.toJson(this, pw);
        }
    }

    /**
     * Load prediction parameters from a JSON config file (see {@link #saveTo(File)}).
     *
     * @param languageModel the language model associated to the loaded prediction parameters
     * @param file          path to the prediction parameter file
     * @return the loaded {@link PredictionParameter}
     * @throws IOException if loading fails
     */
    public static PredictionParameter loadFrom(LanguageModel languageModel, File file) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8.name())) {
            PredictionParameter param = GSON.fromJson(inputStreamReader, PredictionParameter.class);
            param.setLanguageModel(languageModel);
            return param;
        }
    }
    //========================================================================

    // WIP SEMANTIC
    //========================================================================
    //    @Expose
    //    private boolean semanticModelEnabled = false;
    //
    //    @Expose
    //    private int semanticInputContextSize = 50;
    //
    //    @Expose
    //    private double semanticDensityMinBound = 0.1, semanticDensityMaxBound = 0.4;
    //
    //    @Expose
    //    private double semanticContrastFactor = 5;
    //========================================================================

}
