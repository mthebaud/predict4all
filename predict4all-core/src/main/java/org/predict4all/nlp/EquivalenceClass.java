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

import gnu.trove.map.hash.TByteObjectHashMap;

/**
 * Represent a equivalence class type that can be used when training a language model.<br>
 * Useful to group same kind of element in a corpus under a same concept instead of textual data.<br>3
 * These are especially used in semantic data.
 *
 * @author Mathieu THEBAUD
 */
public enum EquivalenceClass {
    PERCENT(0), //
    DATE_DAY_MONTH(1), //
    DATE_MONTH_YEAR(2), //
    DATE_FULL_DIGIT(3), //
    DATE_FULL_TEXT(4), //
    DATE_WEEK_DAY(5), // + token transform
    DATE_MONTH(6), // TODO : matcher
    DATE_HOUR(7), // TODO : matcher
    MONEY_AMOUNT(8), // TODO : matcher
    INTEGER(9), //
    DECIMAL(10), //
    ACRONYM(11), //
    PROPER_NAME(12), //
    MISC(13), //
    CUSTOM(14);

    /**
     * ID is important to identify this EC, because ordinal is not reliable (if order change...)
     */
    private final int id;

    private EquivalenceClass(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public byte getIdByte() {
        return (byte) id;
    }

    public static int getMaxIdValue() {
        int max = 0;
        for (EquivalenceClass term : EquivalenceClass.values()) {
            max = Math.max(max, term.getId());
        }
        return max;
    }

    // CACHE IDS
    // ========================================================================
    private static final TByteObjectHashMap<EquivalenceClass> EC_BY_IDS = new TByteObjectHashMap<>();

    static {
        EquivalenceClass[] terms = EquivalenceClass.values();
        for (EquivalenceClass ec : terms) {
            EC_BY_IDS.put(ec.getIdByte(), ec);
        }
        EC_BY_IDS.compact();
    }

    public static EquivalenceClass getECById(byte id) {
        return EC_BY_IDS.get(id);
    }
    // ========================================================================
}
