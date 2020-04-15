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

package org.predict4all.nlp.language.french;

import org.predict4all.nlp.language.AbstractLanguageModel;
import org.predict4all.nlp.language.BaseWordDictionary;
import org.predict4all.nlp.language.StopWordDictionary;
import org.predict4all.nlp.language.french.matcher.*;
import org.predict4all.nlp.parser.matcher.TokenMatcher;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FrenchLanguageModel extends AbstractLanguageModel {

    private FrenchStopWordDictionary frenchStopWordDictionary;
    private FrenchBaseWordDictionary frenchBaseWordDictionary;

    @Override
    public String getId() {
        return "fr";
    }

    @Override
    public int getAverageWordLength() {
        return 5;
    }

    @Override
    public int getAverageVocabularySize() {
        return 110_000;
    }

    //TODO "79e" matcher
    public static final TokenMatcher[] MATCHERS_SEMANTIC_ANALYSIS_FR = { //
            new DateFullDigitMatcher(), // 12/05/2017
            new DateFullTextMatcher(), // 12 mai 2017
            new DateMonthYearMatcher(), // mai 2017
            new DateDayMonthMatcher(), // 12 mai
            new PercentMatcher(), // 12.56% || 12%
            new SpecialWordMatcher(), // aujourd'hui
            new DateWeekDayMatcher(), // mercredi
            new NumberDecimalMatcher(), // 12.56
            new NumberIntMatcher(), // 12
            new ApostropheMatcher(), // l'
            new AcronymMatcher(), // U.S.A
            new ProperNameMatcher(), // Paris
            new HyphenMatcher(), // sous-marin
    };

    public static final TokenMatcher[] MATCHERS_NGRAM_FR = { //
            // Equivalence classes
            new PercentMatcher(), // 12.56% || 12%
            new SpecialWordMatcher(), // aujourd'hui
            new NumberDecimalMatcher(), // 12.56
            new NumberIntMatcher(), // 12
            // Not equivalence classes
            new ApostropheMatcher(), // l'
            new AcronymMatcher(), // U.S.A
            new HyphenMatcher(), // sous-marin
    };

    @Override
    public TokenMatcher[] getTokenMatchersForSemanticAnalysis() {
        return MATCHERS_SEMANTIC_ANALYSIS_FR;
    }

    @Override
    public TokenMatcher[] getTokenMatchersForNGram() {
        return MATCHERS_NGRAM_FR;
    }

    private final static HashSet<String> VALID_ONE_CHAR_WORDS = new HashSet<>(Arrays.asList("à", "a", "ô", "y"));

    @Override
    public Set<String> getValidOneCharWords() {
        return VALID_ONE_CHAR_WORDS;
    }

    @Override
    public StopWordDictionary getStopWordDictionary(TrainingConfiguration configuration) {
        if (frenchStopWordDictionary == null) {
            frenchStopWordDictionary = new FrenchStopWordDictionary(configuration.getStopWordDictionaryPath());
        }
        return frenchStopWordDictionary;
    }

    @Override
    public BaseWordDictionary getBaseWordDictionary(TrainingConfiguration configuration) {
        if (frenchBaseWordDictionary == null) {
            frenchBaseWordDictionary = new FrenchBaseWordDictionary(configuration.getBaseWordDictionaryPath());
        }
        return frenchBaseWordDictionary;
    }

}
