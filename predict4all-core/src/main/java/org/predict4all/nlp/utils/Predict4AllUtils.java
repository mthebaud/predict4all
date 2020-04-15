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

package org.predict4all.nlp.utils;

import org.predict4all.nlp.Separator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Contains different utils methods that are used in NLP taks.
 *
 * @author Mathieu THEBAUD
 */
public class Predict4AllUtils {

    private Predict4AllUtils() {
    }

    /**
     * @param currentVal the current value (can be null)
     * @param newVal     the new value (can be null)
     * @param <T>        type to test
     * @return <strong>newVal</strong> if <strong>currentVal</strong> is null, else <strong>currentVal</strong>
     */
    public static <T> T getOrDefault(T currentVal, T newVal) {
        return currentVal != null ? currentVal : newVal;
    }

    /**
     * Throws a {@link IllegalArgumentException} if a given object is null
     *
     * @param param   the reference to test
     * @param message the exception message
     * @param <T>     type to test
     * @return given param
     */
    public static <T> T checkNull(T param, String message) throws IllegalArgumentException {
        if (param == null) {
            throw new IllegalArgumentException(message);
        } else {
            return param;
        }
    }

    @SafeVarargs
    public static <T> Map<T, T> createMap(T... keyValues) {
        Map<T, T> map = new HashMap<>(keyValues.length);
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    public static int[] toPrimitive(final Integer[] array) {
        if (array == null) {
            return null;
        }
        final int[] retArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            retArray[i] = array[i].intValue();
        }
        return retArray;
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    public static String capitalize(final String str) {
        int length;
        if (str == null || (length = str.length()) == 0) {
            return str;
        }
        final char firstC = str.charAt(0);
        if (Character.isTitleCase(firstC)) {
            return str;
        }
        return new StringBuilder(length).append(Character.toTitleCase(firstC)).append(str.substring(1)).toString();
    }

    public static String upperCase(final String str) {
        return str != null ? str.toUpperCase() : null;
    }

    public static String lowerCase(final String str) {
        return str != null ? str.toLowerCase() : null;
    }

    public static boolean isCapitalized(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return Character.isUpperCase(str.charAt(0));
    }

    public static boolean isFullUpperCase(final String str) {
        if (str == null || isEmpty(str)) {
            return false;
        }
        final int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isUpperCase(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsUpperCase(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int c = 0; c < str.length(); c++) {
            if (Character.isUpperCase(str.charAt(c))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static int length(String str) {
        return str != null ? str.length() : 0;
    }

    public static boolean endsWith(String str, String end) {
        return str != null && end != null ? str.endsWith(end) : str == end;
    }

    public static String uncapitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toLowerCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            // already capitalized
            return str;
        }
        final int newCodePoints[] = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen; ) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint; // copy the remaining ones
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

    public static boolean strEquals(String str1, String str2) {
        if (str1 == str2) {
            return true;
        }
        if (str1 == null || str2 == null || str1.length() != str2.length()) {
            return false;
        }
        return str1.equals(str2);
    }

    public static boolean strEqualsIgnoreCase(String str1, String str2) {
        if (str1 == str2) {
            return true;
        }
        if (str1 == null || str2 == null || str1.length() != str2.length()) {
            return false;
        }
        return str1.equalsIgnoreCase(str2);
    }

    public static String[] strSplit(String str, String splitter) {
        return str == null || splitter == null ? new String[]{str} : str.split(Pattern.quote(splitter));
    }

    public static String getEndUntilNextSeparator(String text) {
        return getUntilNextSeparator(text, 1);
    }

    public static String getStartUntilNextSeparator(String text) {
        return getUntilNextSeparator(text, -1);
    }

    public static int countStartUntilNextSeparator(String text) {
        return countUntilNextSeparator(text, -1);
    }

    public static int countEndUntilNextSeparator(String text) {
        return countUntilNextSeparator(text, 1);
    }

    //TODO : handle simple quote + dash
    private static String getUntilNextSeparator(String text, int direction) {
        StringBuilder currentText = new StringBuilder();
        if (text != null) {
            for (int i = direction > 0 ? 0 : text.length() - 1; i < text.length() && i >= 0; i += direction) {
                char currentChar = text.charAt(i);
                if (Separator.getSeparatorFor(currentChar) != null) {
                    return currentText.toString();
                } else {
                    if (direction > 0)
                        currentText.append(currentChar);
                    else currentText.insert(0, currentChar);
                }
            }
        }
        return currentText.toString();
    }

    private static int countUntilNextSeparator(String text, int direction) {
        int count = 0;
        if (text != null) {
            for (int i = direction > 0 ? 0 : text.length() - 1; i < text.length() && i >= 0; i += direction) {
                char currentChar = text.charAt(i);
                if (Separator.getSeparatorFor(currentChar) != null) {
                    return count;
                } else {
                    count++;
                }
            }
        }
        return count;
    }
}
