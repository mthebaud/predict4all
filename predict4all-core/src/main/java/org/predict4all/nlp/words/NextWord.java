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

package org.predict4all.nlp.words;

import org.predict4all.nlp.Separator;

public class NextWord {
    private final boolean correction;
    private final double factor;
    private final int wordId1, wordId2;
    private final StringBuilder debugInformation;
    private final Separator separator;

    private NextWord(double factor, final boolean correction, int wordId1, int wordId2, final Separator separator, StringBuilder debugInformation) {
        super();
        this.factor = factor;
        this.wordId1 = wordId1;
        this.wordId2 = wordId2;
        this.separator = separator;
        this.correction = correction;
        this.debugInformation = debugInformation;
    }

    private NextWord(double factor, final boolean correction, int wordId1, StringBuilder debugInformation) {
        this(factor, correction, wordId1, -1, null, debugInformation);
    }

    public static NextWord createUnique(int wordId, double factor, final boolean correction, StringBuilder debugInformation) {
        return new NextWord(factor, correction, wordId, debugInformation);
    }

    public static NextWord createDouble(int wordId1, int wordId2, final Separator separator, double factor, final boolean correction,
                                        StringBuilder debugInformation) {
        return new NextWord(factor, correction, wordId1, wordId2, separator, debugInformation);
    }

    public double getFactor() {
        return factor;
    }

    public int getWordId1() {
        return wordId1;
    }

    public int getWordId2() {
        return wordId2;
    }

    public StringBuilder getDebugInformation() {
        return debugInformation;
    }

    public boolean isUnique() {
        return wordId2 < 0;
    }

    public boolean isDouble() {
        return wordId2 >= 0;
    }

    public Separator getSeparator() {
        return separator;
    }

    public boolean isCorrection() {
        return correction;
    }

    @Override
    public String toString() {
        return "NextWord [factor=" + factor + ", wordId1=" + wordId1 + ", wordId2=" + wordId2 + ", debugInformation=" + debugInformation + "]";
    }

}
