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

package org.predict4all.nlp.words.model;

import org.predict4all.nlp.prediction.PredictionParameter;

public class SimpleWord extends AbstractWord {
    protected final String word;
    protected double probFactor = 1.0;
    protected boolean modifiedByUser = false, modifiedBySystem = false;
    protected boolean forceValid, forceInvalid;

    protected SimpleWord(int id, String word) {
        super(id);
        this.word = word;
    }

    protected SimpleWord(int id, String word, boolean modifiedByUser, boolean modifiedBySystem, double probFactor, boolean forceInvalid,
                         boolean forceValid) {
        this(id, word);
        this.modifiedByUser = modifiedByUser;
        this.modifiedBySystem = modifiedBySystem;
        this.probFactor = probFactor;
        this.forceInvalid = forceInvalid;
        this.forceValid = forceValid;
    }

    @Override
    public String getWord() {
        return word;
    }

    @Override
    public boolean isValidToBePredicted(PredictionParameter predictionParameter) {
        return isForceInvalid() ? false : true;
    }

    @Override
    public double getProbFactor() {
        return probFactor;
    }

    @Override
    public void setProbFactor(double factor, boolean modificationByUser) {
        if (modificationByUser)
            this.modifiedByUser |= factor != probFactor;
        else this.modifiedBySystem |= factor != probFactor;
        this.probFactor = factor;
    }

    @Override
    public byte getType() {
        return Word.TYPE_SIMPLE;
    }

    @Override
    public boolean isModifiedByUser() {
        return modifiedByUser;
    }

    @Override
    public boolean isModifiedBySystem() {
        return modifiedBySystem;
    }

    @Override
    public void setModifiedByUser(boolean modified) {
        this.modifiedByUser = modified;
    }

    @Override
    public boolean isModifiedByUserOrSystem() {
        return this.modifiedBySystem || this.modifiedByUser;
    }

    @Override
    public boolean isForceValid() {
        return forceValid;
    }

    @Override
    public void setForceValid(boolean forceValid, boolean modificationByUser) {
        if (modificationByUser)
            this.modifiedByUser |= this.forceValid != forceValid;
        else this.modifiedBySystem |= this.forceValid != forceValid;
        this.forceValid = forceValid;
    }

    @Override
    public boolean isForceInvalid() {
        return forceInvalid;
    }

    @Override
    public void setForceInvalid(boolean forceInvalid, boolean modificationByUser) {
        if (modificationByUser)
            this.modifiedByUser |= this.forceInvalid != forceInvalid;
        else this.modifiedBySystem |= this.forceInvalid != forceInvalid;
        this.forceInvalid = forceInvalid;
    }

    public static SimpleWord create(final int id, final String word) {
        return new SimpleWord(id, word);
    }

    public static SimpleWord createModified(final int id, final String word, boolean modifiedByUser, boolean modifiedBySystem, double probFactor,
                                            boolean forceInvalid, boolean forceValid) {
        return new SimpleWord(id, word, modifiedByUser, modifiedBySystem, probFactor, forceInvalid, forceValid);
    }

    @Override
    public Word clone(int newId) {
        return new SimpleWord(newId, word, modifiedByUser, modifiedBySystem, probFactor, forceInvalid, forceValid);
    }

}
