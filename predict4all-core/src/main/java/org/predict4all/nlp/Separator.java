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
import gnu.trove.map.hash.TCharObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent chars between words.<br>
 * This is preferred to regex pattern because separator are fully controlled.<br>
 * <strong>If you add any new separator, watch the last used id</strong>
 *
 * @author Mathieu THEBAUD
 */
public enum Separator {
    SPACE(0, false, ' ', (char) 160, (char) 8202, (char) 8201, (char) 8239), //
    EXCLAMATION(1, true, '!'), //
    QUESTION(2, true, '?'), //
    POINT(3, true, '.', '…', '_'), //
    NEWLINE(4, true, '\n', '\r'), //
    TAB(5, false, '\t'), //
    COMMA(6, false, ','), //
    APOSTROPHE(7, false, '\'', '’', '´', '‘', 'ˈ', '′'), //
    LBRACKET(8, false, '('), //
    RBRACKET(9, false, ')'), //
    SEMICOLON(10, false, ';'), //
    QUOTE(11, false, '"', '«', '»', '“', '”', '″'), //
    EQUAL(12, false, '='), //
    OPEN_HOOK(13, false, '['), //
    CLOSE_HOOK(23, false, ']'), //
    INF_SUP(14, false, '<', '>'), //
    SLASH(15, false, '/', '⁄'), //
    BACKSLASH(16, false, '\\'), //
    HASH(17, false, '#'), //
    TWOPOINT(18, false, ':'), //
    CURRENCY_EURO_SYMBOL(19, false, '€'), //
    PERCENT(20, false, '%', '‰'), //
    HYPHEN(21, false, '-', (char) 173, (char) 8722, (char) 8212, (char) 8208, (char) 8211), //
    PIPE(24, false, '|'), //

    // TODO : better parsing
    OTHER(22, false, '+', '~', '°', '$', '*', '¡', '}', '¿', '&', '@', '•', '×', '{', '→');//

    private static final Logger LOGGER = LoggerFactory.getLogger(Separator.class);

    private final char[] chars;
    private final char officialChar;
    private final boolean sentenceSeparator;
    private String officialCharString;
    private final int id;

    private Separator(int id, boolean sentenceSeparator, char... chars) {
        this.id = id;
        this.sentenceSeparator = sentenceSeparator;
        this.chars = chars;
        this.officialChar = chars[0];
    }

    public int getId() {
        return id;
    }

    public byte getIdByte() {
        return (byte) id;
    }

    public char getOfficialChar() {
        return officialChar;
    }

    public boolean isSentenceSeparator() {
        return sentenceSeparator;
    }

    public String getOfficialCharString() {
        if (officialCharString == null) {
            this.officialCharString = String.valueOf(this.getOfficialChar());
        }
        return this.officialCharString;
    }

    // TOKEN CACHING
    // ========================================================================
    private static final TCharObjectHashMap<Separator> TOKEN_SEPARATORS = new TCharObjectHashMap<>(Separator.values().length);

    static {
        Separator[] values = Separator.values();
        for (Separator sep : values) {
            for (char c : sep.chars) {
                if (!TOKEN_SEPARATORS.containsKey(c)) {
                    TOKEN_SEPARATORS.put(c, sep);
                } else {
                    LOGGER.error("Found duplicate char : {} = {}", c, sep);
                }
            }
        }
        TOKEN_SEPARATORS.compact();
    }

    public static Separator getSeparatorFor(char c) {
        return TOKEN_SEPARATORS.get(c);
    }
    // ========================================================================

    // ID CACHING
    // ========================================================================
    private static final TByteObjectHashMap<Separator> SEPARATORS_BY_IDS = new TByteObjectHashMap<>();

    static {
        Separator[] separators = Separator.values();
        for (Separator separator : separators) {
            SEPARATORS_BY_IDS.put(separator.getIdByte(), separator);
        }
        SEPARATORS_BY_IDS.compact();
    }

    public static Separator getSeparatorById(byte id) {
        return SEPARATORS_BY_IDS.get(id);
    }
    // ========================================================================

}
