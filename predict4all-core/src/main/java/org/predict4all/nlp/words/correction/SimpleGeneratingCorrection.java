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

package org.predict4all.nlp.words.correction;

import org.predict4all.nlp.Separator;
import org.predict4all.nlp.utils.Pair;

public class SimpleGeneratingCorrection implements GeneratingCorrectionI {
    private String firstPart, secondPart;
    private Separator separator;
    private StringBuilder currentPart;
    private double endFactor;
    private final StringBuilder debugInformation;

    public SimpleGeneratingCorrection(String currentPart, boolean debug) {
        this.currentPart = currentPart != null ? new StringBuilder(currentPart) : null;
        this.debugInformation = debug ? new StringBuilder() : null;
    }

    private SimpleGeneratingCorrection(SimpleGeneratingCorrection other, boolean debug) {
        this((String) null, debug);
        this.currentPart = new StringBuilder(other.currentPart);
        this.firstPart = other.firstPart;
        this.secondPart = other.secondPart;
        this.separator = other.separator;
        this.endFactor = other.endFactor;
        if (this.debugInformation != null) {
            this.debugInformation.append(other.debugInformation);
        }
    }

    @Override
    public String getEndPart(int index) {
        return index == 0 ? this.firstPart : this.secondPart;
    }

    @Override
    public Separator getEndSeparator(int index) {
        return separator;
    }

    @Override
    public double getEndFactor() {
        return this.endFactor;
    }

    @Override
    public void endCorrection(double factor) {
        this.endFactor = factor;
        if (this.firstPart != null) {
            this.secondPart = this.currentPart.toString();
        } else {
            this.firstPart = this.currentPart.toString();
        }
        this.currentPart = null;
    }

    @Override
    public GeneratingCorrectionI appendDebugInformationForCurrentPart(StringBuilder before, Pair<StringBuilder, StringBuilder> result,
                                                                      CachedPrecomputedCorrectionRule rule) {
        if (this.debugInformation != null) {
            debugInformation.append(before).append(" > ");
            debugInformation.append(result.getLeft());
            if (result.getRight() != null) {
                debugInformation
                        .append(rule.getReplacementSeparator() != Separator.APOSTROPHE ? rule.getReplacementSeparator().getOfficialChar() : "");
                debugInformation.append(result.getRight());
            }
            debugInformation.append("   (").append(rule.getError()).append(" > ").append(rule.getReplacement()).append(" / ").append(rule.getCost())
                    .append(")").append("\n");
        }
        return this;
    }

    @Override
    public StringBuilder getDebugInformation() {
        return debugInformation;
    }

    @Override
    public String getKey() {
        return secondPart != null ? new StringBuilder(this.firstPart).append(separator.getOfficialChar()).append(this.secondPart).toString()
                : this.firstPart;
    }

    @Override
    public int getPartCount() {
        return secondPart != null || (firstPart != null && currentPart != null) ? 2 : 1;
    }

    @Override
    public void currentPartFinishedAndNewPartStarted(Separator separator, StringBuilder newPartStarted) {
        if (this.separator != null) {
            throw new IllegalArgumentException("This class is not used to store generating correction with more than two parts");
        }
        this.separator = separator;
        this.firstPart = this.currentPart.toString();
        this.currentPart = newPartStarted;
    }

    @Override
    public void appendToCurrentPart(CharSequence charSequence) {
        this.currentPart.append(charSequence);
    }

    @Override
    public int getCurrentPartLength() {
        return this.currentPart.length();
    }

    @Override
    public String substringInCurrentPart(int startIndex, int endIndex) {
        return this.currentPart.substring(startIndex, endIndex);
    }

    @Override
    public int indexOfInCurrentPart(String str, int startIndexInclusive) {
        return this.currentPart.indexOf(str, startIndexInclusive);
    }

    @Override
    public void changeCurrentPartTo(StringBuilder currentPart) {
        this.currentPart = currentPart;
    }

    @Override
    public GeneratingCorrectionI clone() {
        return new SimpleGeneratingCorrection(this, debugInformation != null);
    }

    @Override
    public String toString() {
        return "SimpleGeneratingCorrection [firstPart=" + firstPart + ", secondPart=" + secondPart + ", separator=" + separator + ", currentPart="
                + currentPart + ", endFactor=" + endFactor + ", debugInformation=" + debugInformation + "]";
    }

    @Override
    public StringBuilder getCurrentPart() {
        return currentPart;
    }
}
