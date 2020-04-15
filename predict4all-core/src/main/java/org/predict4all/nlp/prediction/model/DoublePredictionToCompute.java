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

package org.predict4all.nlp.prediction.model;

import org.predict4all.nlp.words.WordDictionary;

/**
 * Represent the prediction for two word in a row.<br>
 * Could have been generic (more than two, but for computing performance, limit combination to two word only)
 *
 * @author Mathieu THEBAUD
 */
public class DoublePredictionToCompute extends AbstractPredictionToCompute {
    private final boolean spaceBetween;
    private final int[] firstPrefix, secondPrefix;
    private final int firstWordId, secondWordId;

    public DoublePredictionToCompute(int firstWordId, int secondWordId, boolean spaceBetween, int[] firstPrefix, int[] secondPrefix, double factor,
                                     boolean correction, StringBuilder debugInformation) {
        super();
        this.spaceBetween = spaceBetween;
        this.firstPrefix = firstPrefix;
        this.secondPrefix = secondPrefix;
        this.firstWordId = firstWordId;
        this.secondWordId = secondWordId;
        this.factor = factor;
        this.correction = correction;
        this.debugInformation = debugInformation;
    }

    @Override
    public int getWordId() {
        return this.firstWordId;
    }

    public int[] getFirstPrefix() {
        return firstPrefix;
    }

    public int[] getSecondPrefix() {
        return secondPrefix;
    }

    public int getFirstWordId() {
        return firstWordId;
    }

    public int getSecondWordId() {
        return secondWordId;
    }

    public boolean isSpaceBetween() {
        return spaceBetween;
    }

    @Override
    public boolean isDouble() {
        return true;
    }

    @Override
    public void computePrediction(WordDictionary wordDictionary) {
        this.prediction = wordDictionary.getWord(firstWordId).getWord() + (spaceBetween ? " " : "") + wordDictionary.getWord(secondWordId).getWord();
    }

}
