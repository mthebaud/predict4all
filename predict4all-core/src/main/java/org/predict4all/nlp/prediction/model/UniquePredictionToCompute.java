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

public class UniquePredictionToCompute extends AbstractPredictionToCompute {
    private final int wordId;

    public UniquePredictionToCompute(int wordId, double factor, boolean correction, StringBuilder debugInformation) {
        super();
        this.wordId = wordId;
        this.factor = factor;
        this.correction = correction;
        this.debugInformation = debugInformation;
    }

    @Override
    public int getWordId() {
        return wordId;
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public void computePrediction(WordDictionary wordDictionary) {
        this.prediction = wordDictionary.getWord(wordId).getWord();
    }
}
