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

import org.junit.Before;
import org.predict4all.nlp.language.BaseWordDictionary;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.language.french.FrenchLanguageModel;
import org.predict4all.nlp.ngram.dictionary.StaticNGramTrieDictionary;
import org.predict4all.nlp.trainer.DataTrainer;
import org.predict4all.nlp.trainer.configuration.NGramPruningMethod;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.words.WordDictionary;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractWordPredictionIntegrationTest {
    protected PredictionParameter predictionParameter;
    protected LanguageModel languageModel;
    protected TrainingConfiguration trainingConfiguration;
    protected BaseWordDictionary baseWordDictionary;
    protected WordPredictor wordPredictor;

    @Before
    public void setUp() {
        this.languageModel = mock(LanguageModel.class);
        this.baseWordDictionary = mock(BaseWordDictionary.class);
        this.trainingConfiguration = mock(TrainingConfiguration.class);
        this.predictionParameter = mock(PredictionParameter.class);
        when(this.languageModel.getBaseWordDictionary(trainingConfiguration)).thenReturn(baseWordDictionary);
        when(trainingConfiguration.getDirectlyValidWordCountThreshold()).thenReturn(0);
        when(trainingConfiguration.getSmoothingDiscountValue()).thenReturn(0.5);
        when(this.predictionParameter.getLanguageModel()).thenReturn(languageModel);
        when(this.predictionParameter.isEnableDebugInformation()).thenReturn(true);
    }

    protected WordPredictor trainFullPredictionModel(String fileName) throws Exception {
        when(trainingConfiguration.getCorpus()).thenReturn(new File("./src/test/resources/" + fileName));
        when(this.trainingConfiguration.getPruningMethod()).thenReturn(NGramPruningMethod.NONE);
        when(this.trainingConfiguration.getNgramOrder()).thenReturn(4);
        when(this.languageModel.getTokenMatchersForNGram()).thenReturn(FrenchLanguageModel.MATCHERS_NGRAM_FR);

        File ngramFile = new File("./build/tmp/" + fileName + "/output/ngrams.bin");
        File wordFile = new File("./build/tmp/" + fileName + "/output/words.bin");
        DataTrainer dt = new DataTrainer(new File("./build/tmp/" + fileName + "/output/working/"), wordFile, ngramFile, null, languageModel,
                trainingConfiguration);
        dt.launchNGramTraining(TrainingStep.PARSER);

        WordPredictor predictor = new WordPredictor(predictionParameter, WordDictionary.loadDictionary(languageModel, wordFile),
                StaticNGramTrieDictionary.open(ngramFile));
        return predictor;
    }

    protected static final String IGNORE_ORDER = "{-}";
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.0000");

    protected void assertPredictionEquals(String input, int count, String... expecteds) throws Exception {
        WordPredictionResult result = this.wordPredictor.predict(input, null, count);

        StringBuilder strDebug = new StringBuilder();
        strDebug.append("For input \"").append(input).append("\" (wanted count ").append(count).append(")");
        strDebug.append("\n\t EXPECTED : ")
                .append(Arrays.stream(expecteds).map(s -> s.equals(IGNORE_ORDER) ? "//" : s).collect(Collectors.joining(" ")));
        strDebug.append("\n\t RESULT : ").append(result.getPredictions().stream()
                .map(wp -> wp.getPredictionToDisplay() + " (" + DECIMAL_FORMAT.format(wp.getScore()) + ")").collect(Collectors.joining(" ")));
        strDebug.append("\n ERROR : ");

        Set<String> nextResultIgnoreOrder = null;
        List<WordPrediction> predictions = result.getPredictions();
        int pExpected = 0;
        for (int p = 0; p < predictions.size(); p++) {
            String predictionToDisplay = predictions.get(p).getPredictionToDisplay();
            if ((nextResultIgnoreOrder == null || nextResultIgnoreOrder.isEmpty()) && pExpected < expecteds.length
                    && Predict4AllUtils.strEquals(IGNORE_ORDER, expecteds[pExpected])) {
                Set<String> tmpSet = new HashSet<>();
                for (pExpected = pExpected + 1; pExpected < expecteds.length; pExpected++) {
                    String nextIgnoreOrder = expecteds[pExpected];
                    if (Predict4AllUtils.strEquals(nextIgnoreOrder, IGNORE_ORDER)) {
                        nextResultIgnoreOrder = tmpSet;
                        pExpected++;
                        break;
                    } else {
                        tmpSet.add(Predict4AllUtils.lowerCase(nextIgnoreOrder));
                    }
                }
                if (nextResultIgnoreOrder == null) {
                    throw new IllegalStateException(strDebug.toString() + "Missing end of ignore order tag : " + IGNORE_ORDER);
                }
            }
            if (nextResultIgnoreOrder == null || nextResultIgnoreOrder.isEmpty()) {
                if (pExpected < expecteds.length) {
                    String currentExpected = expecteds[pExpected];
                    if (!Predict4AllUtils.strEqualsIgnoreCase(predictionToDisplay, currentExpected)) {
                        throw new AssertionError(strDebug.toString() + "An expected prediction \"" + currentExpected + "\" is incorrect : \""
                                + result.getPredictions().get(p) + "\" (in exact order)");
                    }
                } else {
                    throw new AssertionError(
                            strDebug.toString() + "There is less expected prediction than result prediction, fill up your expected predictions");
                }
                pExpected++;
            } else {
                if (nextResultIgnoreOrder.contains(Predict4AllUtils.lowerCase(predictionToDisplay))) {
                    nextResultIgnoreOrder.remove(Predict4AllUtils.lowerCase(predictionToDisplay));
                } else {
                    throw new AssertionError(strDebug.toString() + "Was expecting one of the following : " + nextResultIgnoreOrder + " but found : \""
                            + result.getPredictions().get(p) + "\"");
                }
            }
        }
        if (nextResultIgnoreOrder != null && !nextResultIgnoreOrder.isEmpty()) {
            throw new AssertionError(strDebug.toString() + "Was expecting to find one of the following : " + nextResultIgnoreOrder
                    + " but there is no predictions left");
        }
    }
}
