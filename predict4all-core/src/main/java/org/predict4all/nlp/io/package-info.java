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
 * Contains custom {@link java.io.InputStream} and {@link java.io.OutputStream} to save/load Predict4All specific items ({@link org.predict4all.nlp.parser.token.Token} and {@link org.predict4all.nlp.words.model.Word}).<br>
 * Note that NGram are saved without these stream as they are designed to be loaded on demand with a {@link java.nio.channels.FileChannel}.<br>
 * Both token and word streams extends {@link java.io.DataOutputStream} or {@link java.io.DataInputStream} : this was done for optimization, this method is much more optimized that using any other serialization methods.
 *
 * @author Mathieu THEBAUD
 */
package org.predict4all.nlp.io;