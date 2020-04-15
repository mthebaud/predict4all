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

public abstract class AbstractPredictionToCompute implements Comparable<AbstractPredictionToCompute> {
    protected String prediction;
    private double score;
    protected double factor;
    protected boolean correction;
    protected StringBuilder debugInformation;

    // BASE
    //========================================================================
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getFactor() {
        return factor;
    }

    public StringBuilder getDebugInformation() {
        return debugInformation;
    }

    public boolean isCorrection() {
        return correction;
    }

    public String getPrediction() {
        return prediction;
    }

    public boolean isPredictionInitialized() {
        return prediction != null;
    }

    @Override
    public int compareTo(AbstractPredictionToCompute o) {
        return Double.compare(o.getScore(), this.getScore());
    }
    //========================================================================

    // ABSTRACT
    //========================================================================
    public abstract int getWordId();

    public abstract void computePrediction(WordDictionary wordDictionary);

    public abstract boolean isDouble();
    //========================================================================
}
