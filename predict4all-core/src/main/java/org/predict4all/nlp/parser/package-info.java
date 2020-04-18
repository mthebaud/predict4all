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

/**
 * Package mainly focus on classes to convert a raw input text (as {@link java.lang.String}) to {@link org.predict4all.nlp.parser.token.Token} that can be used by Predict4All.<br>
 * This package is used by both training algorithms and predictor : this allow consistency among parsing and using user input.<br>
 *     Both word and token stream
 *
 * @author Mathieu THEBAUD
 */
package org.predict4all.nlp.parser;