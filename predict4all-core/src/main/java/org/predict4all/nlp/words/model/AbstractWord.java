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

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.prediction.PredictionParameter;

public abstract class AbstractWord implements Word {

    private final int id;

    public AbstractWord(int id) {
        super();
        this.id = id;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public EquivalenceClass getEquivalenceClass() {
        return EquivalenceClass.getECById(getEquivalenceClassId());
    }

    @Override
    public boolean isEquivalenceClass() {
        return false;
    }

    @Override
    public byte getEquivalenceClassId() {
        return -1;
    }

    @Override
    public boolean isNGramTag() {
        return false;
    }

    @Override
    public byte getNGramTagId() {
        return -1;
    }

    @Override
    public Tag getNGramTag() {
        return Tag.getById(getNGramTagId());
    }

    @Override
    public String toString() {
        if (this.isNGramTag())
            return "" + getNGramTag();
        else if (this.isEquivalenceClass())
            return "" + getEquivalenceClassId();
        else return this.getWord();
    }

    @Override
    public boolean isUserWord() {
        return false;
    }

    @Override
    public boolean isValidForSaving() {
        return true;
    }

    @Override
    public boolean isValidToBePredicted(PredictionParameter predictionParameter) {
        return true;
    }

    @Override
    public double getProbFactor() {
        return 1.0;
    }

    @Override
    public void setProbFactor(double factor, boolean modificationByUser) {
    }

    @Override
    public boolean isModifiedByUser() {
        return false;
    }

    @Override
    public void setModifiedByUser(boolean modified) {
    }

    @Override
    public boolean isModifiedBySystem() {
        return false;
    }

    @Override
    public void setModifiedBySystem(boolean modifiedBySystem) {
    }

    @Override
    public boolean isModifiedByUserOrSystem() {
        return false;
    }

    @Override
    public boolean isForceValid() {
        return false;
    }

    @Override
    public void setForceValid(boolean forceValid, boolean modificationByUser) {
    }

    @Override
    public boolean isForceInvalid() {
        return false;
    }

    @Override
    public void setForceInvalid(boolean forceInvalid, boolean modificationByUser) {
    }

    @Override
    public int getUsageCount() {
        return 0;
    }

    @Override
    public void incrementUsageCount() {
    }

    @Override
    public long getLastUseDate() {
        return 0;
    }

}
