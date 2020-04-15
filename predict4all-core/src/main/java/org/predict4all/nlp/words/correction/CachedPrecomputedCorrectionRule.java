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

/**
 * Cached version of a {@link CorrectionRule} : this rule is to meant to be directly used in {@link WordCorrectionGenerator}.<br>
 * It only contains information and should not be modified once generated from a {@link CorrectionRule}
 *
 * @author Mathieu THEBAUD
 */
public class CachedPrecomputedCorrectionRule {
    private final String error, replacement;
    private final double factor;
    private final int maxIndexFromStart;
    private final int maxIndexFromEnd;
    private final int minIndexFromStart;
    private final int minIndexFromEnd;
    private final double cost;
    private final int replacementSeparatorIndex;
    private final Separator replacementSeparator;
    private final String replacementLeftPart, replacementRightPart;
    private final CorrectionRule srcBuilder;

    CachedPrecomputedCorrectionRule(CorrectionRule srcBuilder, String error, String replacement, double factor, double cost, int maxIndexFromStart,
                                    int minIndexFromStart, final int maxIndexFromEnd, final int minIndexFromEnd) {
        super();
        this.srcBuilder = srcBuilder;
        this.error = error;
        this.replacement = replacement;
        this.factor = factor;
        this.cost = cost;
        this.maxIndexFromStart = maxIndexFromStart;
        this.minIndexFromStart = minIndexFromStart;
        this.maxIndexFromEnd = maxIndexFromEnd;
        this.minIndexFromEnd = minIndexFromEnd;

        // Find replacement separator
        int replacementSIndex = -1;
        Separator replacementS = null;
        for (int i = 0; i < replacement.length(); i++) {
            char c = replacement.charAt(i);
            Separator separator = Separator.getSeparatorFor(c);
            if (separator != null) {
                replacementSIndex = i;
                replacementS = separator;
                break;
            }
        }
        this.replacementSeparatorIndex = replacementSIndex;
        this.replacementSeparator = replacementS;
        this.replacementLeftPart = this.replacementSeparatorIndex >= 0 ? this.replacement.substring(0, replacementSeparatorIndex) : null;
        this.replacementRightPart = this.replacementSeparatorIndex >= 0
                ? this.replacement.substring(replacementSeparatorIndex + 1, this.replacement.length())
                : null;
    }

    public double getFactor() {
        return factor;
    }

    public String getError() {
        return error;
    }

    public String getReplacement() {
        return replacement;
    }

    public int getMaxIndexFromStart() {
        return maxIndexFromStart;
    }

    public int getMinIndexFromStart() {
        return minIndexFromStart;
    }

    public int getMaxIndexFromEnd() {
        return maxIndexFromEnd;
    }

    public int getMinIndexFromEnd() {
        return minIndexFromEnd;
    }

    public double getCost() {
        return cost;
    }


    public int getReplacementSeparatorIndex() {
        return replacementSeparatorIndex;
    }

    public String getReplacementLeftPart() {
        return replacementLeftPart;
    }

    public String getReplacementRightPart() {
        return replacementRightPart;
    }

    public Separator getReplacementSeparator() {
        return replacementSeparator;
    }

    public CorrectionRule getSrcBuilder() {
        return srcBuilder;
    }

    // CREATE DERIVATIVES
    //========================================================================
    public CachedPrecomputedCorrectionRule opposite() {
        return new CachedPrecomputedCorrectionRule(srcBuilder, replacement, error, factor, cost, maxIndexFromStart, minIndexFromStart, maxIndexFromEnd,
                minIndexFromEnd);
    }

    public static CachedPrecomputedCorrectionRule transitive(CachedPrecomputedCorrectionRule first, CachedPrecomputedCorrectionRule second) {
        return new CachedPrecomputedCorrectionRule(null, first.getError(), second.getReplacement(), (first.getFactor() + second.getFactor()) / 2.0,
                (first.getCost() + second.getCost()) / 2.0, first.getMaxIndexFromStart(), first.getMinIndexFromStart(), first.getMaxIndexFromEnd(),
                first.getMinIndexFromEnd());
    }

    public boolean transitivePossible(CachedPrecomputedCorrectionRule other) {
        return this.maxIndexFromEnd == other.maxIndexFromEnd && this.maxIndexFromStart == other.maxIndexFromEnd
                && this.minIndexFromEnd == other.minIndexFromEnd && this.minIndexFromStart == other.minIndexFromStart
                && !this.error.equals(other.replacement);
    }
    //========================================================================


    // GENERATED
    //========================================================================
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(cost);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((error == null) ? 0 : error.hashCode());
        temp = Double.doubleToLongBits(factor);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + maxIndexFromEnd;
        result = prime * result + maxIndexFromStart;
        result = prime * result + minIndexFromEnd;
        result = prime * result + minIndexFromStart;
        result = prime * result + ((replacement == null) ? 0 : replacement.hashCode());
        result = prime * result + ((replacementLeftPart == null) ? 0 : replacementLeftPart.hashCode());
        result = prime * result + ((replacementRightPart == null) ? 0 : replacementRightPart.hashCode());
        result = prime * result + ((replacementSeparator == null) ? 0 : replacementSeparator.hashCode());
        result = prime * result + replacementSeparatorIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CachedPrecomputedCorrectionRule other = (CachedPrecomputedCorrectionRule) obj;
        if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost))
            return false;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (!error.equals(other.error))
            return false;
        if (Double.doubleToLongBits(factor) != Double.doubleToLongBits(other.factor))
            return false;
        if (maxIndexFromEnd != other.maxIndexFromEnd)
            return false;
        if (maxIndexFromStart != other.maxIndexFromStart)
            return false;
        if (minIndexFromEnd != other.minIndexFromEnd)
            return false;
        if (minIndexFromStart != other.minIndexFromStart)
            return false;
        if (replacement == null) {
            if (other.replacement != null)
                return false;
        } else if (!replacement.equals(other.replacement))
            return false;
        if (replacementLeftPart == null) {
            if (other.replacementLeftPart != null)
                return false;
        } else if (!replacementLeftPart.equals(other.replacementLeftPart))
            return false;
        if (replacementRightPart == null) {
            if (other.replacementRightPart != null)
                return false;
        } else if (!replacementRightPart.equals(other.replacementRightPart))
            return false;
        if (replacementSeparator != other.replacementSeparator)
            return false;
        if (replacementSeparatorIndex != other.replacementSeparatorIndex)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[" + (error != null ? "error=" + error + ", " : "") + (replacement != null ? "replacement=" + replacement : "") + "]";
    }
    //========================================================================


}
