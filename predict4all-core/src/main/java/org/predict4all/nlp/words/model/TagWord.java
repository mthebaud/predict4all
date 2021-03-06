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

import org.predict4all.nlp.Tag;
import org.predict4all.nlp.prediction.PredictionParameter;

public class TagWord extends AbstractWord {

    private final byte tagId;

    public TagWord(Tag tag) {
        super(tag.getId());
        this.tagId = tag.getIdByte();
    }

    @Override
    public byte getNGramTagId() {
        return this.tagId;
    }

    @Override
    public boolean isNGramTag() {
        return true;
    }

    @Override
    public boolean isValidForSaving() {
        return false;
    }

    @Override
    public boolean isValidToBePredicted(PredictionParameter predictionParameter) {
        return false;
    }

    @Override
    public String getWord() {
        return getNGramTag().toString();
    }

    @Override
    public byte getType() {
        return Word.TYPE_NGRAM_TAG;
    }

    @Override
    public Word clone(int newId) {
        return this;
    }
}
