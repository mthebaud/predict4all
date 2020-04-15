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

import org.predict4all.nlp.utils.Predict4AllUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Utils methods for french language.
 *
 * @author Mathieu THEBAUD
 */
// TODO : localize everything, each method should take a language param
public class FrenchLanguageUtils {
    private FrenchLanguageUtils() {
    }

    public static final DecimalFormat TWO_DIGIT_FORMAT_ALWAYS = new DecimalFormat("00");
    public static final DecimalFormat TWO_DIGIT_FORMAT_SOMETIMES = new DecimalFormat("#0");
    public static final DecimalFormat FOUR_DIGIT_FORMAT_ALWAYS = new DecimalFormat("0000");

    public static final List<String> WEEK_DAYS = Arrays.asList("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche");
    public static final Map<String, String> MONTHS = Predict4AllUtils.createMap(//
            "janvier", "janvier", //
            "février", "février", //
            "fevrier", "février", //
            "mars", "mars", //
            "avril", "avril", //
            "mai", "mai", //
            "juin", "juin", //
            "juillet", "juillet", //
            "août", "août", //
            "aout", "août", //
            "septembre", "septembre", //
            "octobre", "octobre", //
            "novembre", "novembre", //
            "décembre", "décembre", //
            "decembre", "décembre");

    public static final List<String> ABBREVIATIONS = Arrays.asList("M", "etc", "Mme");

    public static String getWeekDaysOrRegex() {
        return String.join("|", WEEK_DAYS);
    }

    public static String getMonthOrRegex() {
        return String.join("|", MONTHS.keySet());
    }

    public static String getAbbreviationOrRegex() {
        return String.join("|", ABBREVIATIONS);
    }

    public static int convertWrittenYearToExactYear(int yearValue) {
        Calendar.getInstance().get(Calendar.YEAR);
        // Year : between 0-20 -> 2000+ // TODO : should be based on current year + 5 ?
        if (yearValue >= 0 && yearValue <= 20) {
            yearValue += 2000;
        }
        // Year : between 20-99 -> 1900+
        else if (yearValue > 20 && yearValue <= 99) {
            yearValue += 1900;
        }
        // Above 100 -> raw
        return yearValue;
    }
}
