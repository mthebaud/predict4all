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

package org.predict4all.nlp.semantic;

/**
 * @author Mathieu THEBAUD
 */
//See commit : a693f9a8a1675f7f7f17defe4d69fd8c1b5b7935 (in private repo) to see how semantic was integrated in prediction
public interface SemanticDictionaryConfiguration {
    double getSemanticDensityMinBound();

    double getSemanticDensityMaxBound();

    double getSemanticContrastFactor();
}
