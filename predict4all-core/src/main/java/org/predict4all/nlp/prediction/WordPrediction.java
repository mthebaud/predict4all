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

/**
 * Represent a predictor from {@link WordPredictor}
 * <p>
 * This prediction have various information : the text to display, the text to insert, a score...<br>
 * Debug information will be filled only if enabled (see {@link PredictionParameter#setEnableDebugInformation(boolean)}
 *
 * @author Mathieu THEBAUD
 * @see WordPredictor
 */
public class WordPrediction {
    private final String predictionToDisplay;
    private final String predictionToInsert;
    private final boolean insertSpacePossible;
    private final double score;
    private final int previousCharCountToRemove;
    private final boolean correction;
    private final int wordId;
    private final String debugInformation;

    WordPrediction(String predictionToDisplay, String predictionToInsert, boolean insertSpacePossible, double score,
                   int previousCharCountToRemove, boolean correction, int wordId, String debugInfo) {
        super();
        this.predictionToDisplay = predictionToDisplay;
        this.predictionToInsert = predictionToInsert;
        this.insertSpacePossible = insertSpacePossible;
        this.score = score;
        this.previousCharCountToRemove = previousCharCountToRemove;
        this.correction = correction;
        this.wordId = wordId;
        this.debugInformation = debugInfo;
    }

    /**
     * @return the whole word represented by this prediction, as it should be displayed in the UI.<br>
     * This String can sometimes be uppercase (for example, when a new sentence is started)<br>
     */
    public String getPredictionToDisplay() {
        return predictionToDisplay;
    }

    /**
     * @return the part of the prediction that should be inserted in resulting text.<br>
     * It can differs from the {@link #getPredictionToDisplay()} if the word was already started by user (ex : "to ho" : home (displayed) / me (to insert))
     */
    public String getPredictionToInsert() {
        return predictionToInsert;
    }

    /**
     * @return true if an automatic space can be inserted after the prediction is selected.<br>
     * This is most of the time as most of the words can be followed by spaces. However, some special predicted words will return false (ex : "j'" in French should never insert a space after)
     */
    public boolean isInsertSpacePossible() {
        return insertSpacePossible;
    }

    /**
     * @return the score for this prediction (between 0.0 - 1.0). A higher score indicates a higher probability that the user wants this word.
     * Every predictions from a {@link WordPrediction} will sum to ~1.0 or bellow (as they are normalized on a bigger list than the returned results)
     */
    public double getScore() {
        return score;
    }

    /**
     * @return the number of char before the caret to remove before inserting the prediction.<br>
     * This is mostly superior to 0 when the prediction is a correction ({@link #isCorrection()}): the word start can be incorrect and should be removed to insert the corrected version of the word.
     */
    public int getPreviousCharCountToRemove() {
        return previousCharCountToRemove;
    }

    /**
     * @return true if the prediction is a correction : it differs from the user input.<br>
     * This can be true only if {@link PredictionParameter#setEnableWordCorrection(boolean)} is true
     */
    public boolean isCorrection() {
        return correction;
    }

    /**
     * @return the word ID represented by this prediction.<br>
     * As the word ID should be ignored and are mainly used internally, they can be useful if you want to improve the prediction while the user is typing :
     * you can use the prediction IDs to exclude these words from the next prediction result (as the prediction were already shown to user).<br>
     * <strong>Be careful</strong> : you should take care have a clean ID set : reset it when user go back, insert a space, etc.
     */
    public int getWordId() {
        return wordId;
    }

    /**
     * @return debug information on this prediction<br>
     * Filled only if {@link PredictionParameter#setEnableDebugInformation(boolean)} is true
     */
    public String getDebugInformation() {
        return debugInformation;
    }

    @Override
    public String toString() {
        return this.getPredictionToDisplay() + " = " + this.getScore() + " (insert = " + this.getPredictionToInsert() + ", remove = " + this.getPreviousCharCountToRemove() + ", space = " + this.isInsertSpacePossible() + ")";
    }
}
