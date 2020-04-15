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

public interface GeneratingCorrectionI {
    String getEndPart(int index);

    Separator getEndSeparator(int index);

    StringBuilder getCurrentPart();

    void appendToCurrentPart(CharSequence charSequence);

    int getCurrentPartLength();

    String substringInCurrentPart(int startIndex, int endIndex);

    int indexOfInCurrentPart(String str, int startIndexInclusive);

    void currentPartFinishedAndNewPartStarted(Separator separator, StringBuilder newPartStarted);

    void changeCurrentPartTo(StringBuilder currentPart);

    double getEndFactor();

    void endCorrection(double factor);

    GeneratingCorrectionI appendDebugInformationForCurrentPart(StringBuilder before, Pair<StringBuilder, StringBuilder> result, CachedPrecomputedCorrectionRule rule);

    StringBuilder getDebugInformation();

    String getKey();

    int getPartCount();

    GeneratingCorrectionI clone();

}
