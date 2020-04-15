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

public class UserWord extends SimpleWord {
    private long updateDate;
    private int count;

    public UserWord(int id, String word) {
        super(id, word);
        wordUsed();
    }

    private void wordUsed() {
        this.updateDate = System.currentTimeMillis();
    }

    public UserWord(int id, String word, double probFactor, boolean forceInvalid, boolean forceValid, long updateDate, int count) {
        super(id, word, true, false, probFactor, forceInvalid, forceValid);
        this.updateDate = updateDate;
        this.count = count;
    }

    @Override
    public int getUsageCount() {
        return count;
    }

    @Override
    public void incrementUsageCount() {
        wordUsed();
        this.count++;
    }

    @Override
    public long getLastUseDate() {
        return updateDate;
    }

    @Override
    public boolean isUserWord() {
        return true;
    }

    @Override
    public boolean isValidToBePredicted(PredictionParameter predictionParameter) {
        return isForceInvalid() ? false : isForceValid() ? true : count >= predictionParameter.getMinUseCountToValidateNewWord();
    }

    @Override
    public String toString() {
        return getWord() + " [" + count + "]";
    }

    @Override
    public byte getType() {
        return Word.TYPE_USER_WORD;
    }

    public static UserWord create(int id, String word) {
        return new UserWord(id, word);
    }

    public static UserWord create(int id, String word, double probFactor, boolean forceInvalid, boolean forceValid, long updateDate, int count) {
        return new UserWord(id, word, probFactor, forceInvalid, forceValid, updateDate, count);
    }

    @Override
    public Word clone(int newId) {
        return new UserWord(newId, word, probFactor, forceInvalid, forceValid, updateDate, count);
    }
}
