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

package org.predict4all.nlp.parser;

import org.predict4all.nlp.parser.token.Token;

import java.io.IOException;
import java.util.List;

public class TokenListAppender implements TokenAppender {
    private final List<Token> list;

    public TokenListAppender(List<Token> list) {
        this.list = list;
    }

    @Override
    public void append(Token token) throws IOException {
        this.list.add(token);
    }

    @Override
    public void close() throws Exception {
    }

}
