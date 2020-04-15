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

package org.predict4all.nlp;

/**
 * Represent a specific value in a corpus.<br>
 * Useful to tag specific part of the corpus without any semantic information.<br>
 * <ul>
 * <li>START : represent a sentence start</li>
 * <li>UNKNOWN : represent a word/expression out of vocabulary</li>
 * </ul>
 *
 * @author Mathieu THEBAUD
 */
public enum Tag {
    START(EquivalenceClass.getMaxIdValue() + 1), //
    UNKNOWN(EquivalenceClass.getMaxIdValue() + 2);

    private final int id;

    private Tag(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public byte getIdByte() {
        return (byte) id;
    }

    public static Tag getById(byte id) {
        return id == START.id ? START : id == UNKNOWN.id ? UNKNOWN : null;
    }
}
