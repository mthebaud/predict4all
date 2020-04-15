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

package org.predict4all.nlp.parser.matcher;

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.token.Token;

import java.io.IOException;

/**
 * Represent a matcher that will try to detect if a given token match a specific pattern.<br>
 * If so, the {@link PatternMatched} contains the the normalized representation of the matched tokens and eventually an {@link EquivalenceClass}.
 *
 * @author Mathieu THEBAUD
 */
public interface TokenMatcher {
    PatternMatched match(Token token, TokenProvider provider) throws IOException;
}
