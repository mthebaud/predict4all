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

import com.google.gson.annotations.Expose;
import org.predict4all.nlp.prediction.PredictionParameter;

import java.util.Arrays;
import java.util.Collection;

/**
 * This correction is the most convenient way to create correction rules as it allow direct modification and has helping methods.<br>
 * The {@link WordCorrectionGenerator} will then generate {@link CachedPrecomputedCorrectionRule} to use this rule.<br>
 * Note that a single builder instance can result in multiple correction rule : correction rule should never be directly configured by user as this correction rule is more understandable.<br>
 * Correction rule work as the following : you define errors which are the part replaced, and replacements which are the part correcting errors.
 * The corrector will then try to replace every errors in the input text with every replacements to generate valid words : this method allow correction algorithm to work
 * very early during prediction process : it could for example correct the first two letters of a six letter word while a classic string distance algorithm would be lost.<br>
 * However, this algorithm is expensive as it generate many possible word. That's why {@link org.predict4all.nlp.words.correction.WordCorrectionGenerator} try to stop processing as early as possible.<br>
 * Moreover, parameters like {@link #getCost()} or {@link #getMaxIndexFromEnd()} etc allow the rule to be restrictive to a certain area of the input.<br>
 *
 * @author Mathieu THEBAUD
 */
public class CorrectionRule {
    @Expose
    private String[] errors = {};

    @Expose
    private String[] replacements = {};

    @Expose
    private Double factor;

    @Expose
    private Integer maxIndexFromStart;

    @Expose
    private Integer maxIndexFromEnd;

    @Expose
    private Integer minIndexFromStart;

    @Expose
    private Integer minIndexFromEnd;

    @Expose
    private Double cost;

    @Expose
    private boolean bidirectional;

    private CorrectionRule() {
    }

    // CONFIGURATION
    //========================================================================

    /**
     * Initialize empty correction rule builder
     *
     * @return initialized builder
     */
    public static CorrectionRule ruleBuilder() {
        return new CorrectionRule();
    }

    /**
     * Determine the errors for this correction rule.<br>
     * Error define the part of the text that could be replaced.<br>
     * For example, if error is "a", every "a" char in user input could be potnetially replaced with {@link #withReplacement(String...)}
     * Errors <strong>should not contains any word separator</strong>(space, etc...)
     *
     * @param errors the errors
     * @return this correction rule
     */
    public CorrectionRule withError(String... errors) {
        this.errors = errors;
        return this;
    }

    /**
     * Determine the errors for this correction rule.<br>
     * Error define the part of the text that could be replaced.<br>
     * For example, if error is "a", every "a" char in user input could be potnetially replaced with {@link #withReplacement(String...)}
     * Errors <strong>should not contains any word separator</strong>(space, etc...)
     *
     * @return the errors
     */
    public String[] getErrors() {
        return errors;
    }

    /**
     * Determine the replacements that could be used to correct the {@link #getErrors()}.<br>
     * Replacement could contains <strong>at most one word separator</strong> (space, etc...) : this allow correction merged words.
     *
     * @param replacements the replacements
     * @return this correction rule
     */
    public CorrectionRule withReplacement(String... replacements) {
        this.replacements = replacements;
        return this;
    }

    /**
     * Determine the replacements that could be used to correct the {@link #getErrors()}.<br>
     * Replacement could contains <strong>at most one word separator</strong> (space, etc...) : this allow correction merged words.
     *
     * @return the replacements
     */
    public String[] getReplacements() {
        return replacements;
    }

    /**
     * The correction factor will influence how much the correction "counts" relatively to a correct word.
     *
     * @param factor this correction factor, set to null will use default value in {@link PredictionParameter#getCorrectionDefaultFactor()}
     * @return this correction rule
     */
    public CorrectionRule withFactor(double factor) {
        this.factor = factor;
        return this;
    }

    /**
     * The correction factor will influence how much the correction "counts" relatively to a correct word.
     *
     * @return this correction factor
     */
    public Double getFactor() {
        return factor;
    }


    /**
     * The correction cost will influence how many correction are cumulated for a same input.<br>
     * Typically, correction costs are added to check that they are bellow {@link PredictionParameter#getCorrectionMaxCost()}
     *
     * @param cost this correction cost, set to null will use default value in {@link PredictionParameter#getCorrectionDefaultCost()}
     * @return this correction rule
     */
    public CorrectionRule withCost(double cost) {
        this.cost = cost;
        return this;
    }

    /**
     * The correction cost will influence how many correction are cumulated for a same input.<br>
     * Typically, correction costs are added to check that they are bellow {@link PredictionParameter#getCorrectionMaxCost()}
     *
     * @return this correction cost
     */
    public Double getCost() {
        return cost;
    }

    /**
     * Helper method to allow having the same errors and replacements.<br>
     * This is useful if you want to create a confusion set, if the user always invert two letter or group without difference.<br>
     * After creating a confusion set, {@link #getErrors()} and {@link #getReplacements()} will be the same.
     *
     * @param elements the confusion set elements
     * @return this correction rule
     */
    public CorrectionRule withConfusionSet(String... elements) {
        this.errors = elements;
        this.replacements = elements;
        return this;
    }

    /**
     * Max index (from word start), exclusive (e.g. maxIndexFromStart = 1, only the first char)<br>
     * Useful to restrain correction to the word start.
     *
     * @param maxIndexFromStart the max index from start
     * @return this correction rule
     */
    public CorrectionRule withMaxIndexFromStart(int maxIndexFromStart) {
        this.maxIndexFromStart = maxIndexFromStart;
        return this;
    }

    /**
     * Max index (from word start), exclusive (e.g. maxIndexFromStart = 1, only the first char)<br>
     * Useful to restrain correction to the word start.
     *
     * @return the max index from start
     */
    public Integer getMaxIndexFromStart() {
        return maxIndexFromStart;
    }

    /**
     * Max index (from word end), exclusive (e.g. maxIndexFromEnd = 2, never apply the rule on the last two char)<br>
     * Useful to ignore word ends.
     *
     * @param maxIndexFromEnd the max index from end
     * @return this correction rule
     */
    public CorrectionRule withMaxIndexFromEnd(int maxIndexFromEnd) {
        this.maxIndexFromEnd = maxIndexFromEnd;
        return this;
    }

    /**
     * Max index (from word end), exclusive (e.g. maxIndexFromEnd = 2, never apply the rule on the last two char)<br>
     * Useful to ignore word ends.
     *
     * @return the max index from end
     */
    public Integer getMaxIndexFromEnd() {
        return maxIndexFromEnd;
    }

    /**
     * Min index, from start, inclusive (inclusive from word start, e.g. if = 1, never correct the first char)<br>
     * Useful to correct only the "middle" area of a word
     *
     * @param minIndexFromStart the min index from start
     * @return this correction rule
     */
    public CorrectionRule withMinIndexFromStart(int minIndexFromStart) {
        this.minIndexFromStart = minIndexFromStart;
        return this;
    }

    /**
     * Min index, from start, inclusive (inclusive from word start, e.g. if = 1, never correct the first char)<br>
     * Useful to correct only the "middle" area of a word
     *
     * @return the min index from start
     */
    public Integer getMinIndexFromStart() {
        return minIndexFromStart;
    }

    /**
     * Min index, from end, inclusive (inclusive from word end, e.g. if = 1, only correct the last char)<br>
     * Useful to correct only the last part of a word.
     *
     * @param minIndexFromEnd the min index from end
     * @return this correction rule
     */
    public CorrectionRule withMinIndexFromEnd(int minIndexFromEnd) {
        this.minIndexFromEnd = minIndexFromEnd;
        return this;
    }

    /**
     * Min index, from end, inclusive (inclusive from word end, e.g. if = 1, only correct the last char)<br>
     * Useful to correct only the last part of a word.
     *
     * @return the min index from end
     */
    public Integer getMinIndexFromEnd() {
        return minIndexFromEnd;
    }

    /**
     * Bidirectional indicate that the resulting rule will be acting in both way : errors will be replaced with replacements and replacements will be replaced with errors.<br>
     * This is quite the same as {@link #withConfusionSet(String...)} with a difference is that in a confusion set,
     * errors can be replaced by errors and replacements can be replaced with replacements, which is not the case in bidirectional rules.<br>
     * If you have only one error and replacement, confusion set and bidirectional will result in the same rule.
     *
     * @param bidirectional if the generated rule will act in both way
     * @return this correction rule
     */
    public CorrectionRule withBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
        return this;
    }

    /**
     * Bidirectional indicate that the resulting rule will be acting in both way : errors will be replaced with replacements and replacements will be replaced with errors.<br>
     * This is quite the same as {@link #withConfusionSet(String...)} with a difference is that in a confusion set,
     * errors can be replaced by errors and replacements can be replaced with replacements, which is not the case in bidirectional rules.<br>
     * If you have only one error and replacement, confusion set and bidirectional will result in the same rule.
     *
     * @return if the generated rule will act in both way
     */
    public boolean isBidirectional() {
        return bidirectional;
    }

    /**
     * Helper to add this correction rule to a collection (useful for chaining calls)
     *
     * @param rulesCollection the collection (should not be null)
     */
    public void addTo(Collection<CorrectionRule> rulesCollection) {
        rulesCollection.add(this);
    }
    //========================================================================

    // DEBUG
    //========================================================================
    @Override
    public String toString() {
        return "[" + (errors != null ? "errors=" + Arrays.toString(errors) + ", " : "")
                + (replacements != null ? "replacements=" + Arrays.toString(replacements) + ", " : "")
                + (factor != null ? "factor=" + factor + ", " : "")
                + (maxIndexFromStart != null ? "maxIndexFromStart=" + maxIndexFromStart + ", " : "")
                + (maxIndexFromEnd != null ? "maxIndexFromEnd=" + maxIndexFromEnd + ", " : "")
                + (minIndexFromStart != null ? "minIndexFromStart=" + minIndexFromStart + ", " : "")
                + (minIndexFromEnd != null ? "minIndexFromEnd=" + minIndexFromEnd + ", " : "") + (cost != null ? "cost=" + cost + ", " : "")
                + "bidirectionnal=" + bidirectional + "]";
    }
    //========================================================================

}
