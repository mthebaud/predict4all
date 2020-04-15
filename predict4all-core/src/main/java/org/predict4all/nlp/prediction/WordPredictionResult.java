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

import java.util.List;

/**
 * Contains the result from {@link WordPredictor}.
 *
 * @author Mathieu THEBAUD
 * @see WordPredictor
 */
public class WordPredictionResult {
    private final String debugInformation;
    private final int nextCharCountToRemove;
    private final List<WordPrediction> predictions;

    WordPredictionResult(String debugInfo, int nextCharCountToRemove, List<WordPrediction> predictions) {
        super();
        this.debugInformation = debugInfo;
        this.nextCharCountToRemove = nextCharCountToRemove;
        this.predictions = predictions;
    }

    /**
     * @return the possible next words sorted so that the first item in the list is the prediction with the highest score.<br>
     * This list can be empty but will never be null.<br>
     * Note that the predictor will do its best to fill the wanted prediction count.
     */
    public List<WordPrediction> getPredictions() {
        return predictions;
    }

    /**
     * @return debug information on this prediction (if enabled, see {@link PredictionParameter#setEnableDebugInformation(boolean)})
     */
    public String getDebugInformation() {
        return debugInformation;
    }

    /**
     * @return the char count to remove before inserting the prediction {@link WordPrediction#getPredictionToInsert()}<br>
     * This will be computed only if {@link WordPredictor#predict(String)} was called with <code>textAfterCaret</code> parameter.
     */
    public int getNextCharCountToRemove() {
        return nextCharCountToRemove;
    }
}
