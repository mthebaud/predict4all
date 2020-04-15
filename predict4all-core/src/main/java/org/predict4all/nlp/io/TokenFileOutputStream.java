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

package org.predict4all.nlp.io;

import org.predict4all.nlp.parser.TokenAppender;
import org.predict4all.nlp.parser.token.Token;

import java.io.*;

public class TokenFileOutputStream extends DataOutputStream implements TokenAppender {

    public TokenFileOutputStream(File outputFile) throws FileNotFoundException, IOException {
        super(new BufferedOutputStream(new FileOutputStream(outputFile)));
    }

    public void writeToken(Token token) throws IOException {
        byte tokenType = getTokenTypeFor(token);
        this.writeByte(tokenType);
        if (token.isSeparator()) {
            this.writeByte(token.getSeparator().getId());
        } else if (token.isEquivalenceClass()) {
            this.writeByte(token.getEquivalenceClass().getId());
            this.writeUTF(token.getText());
        } else if (token.isWord()) {
            this.writeUTF(token.getText());
        } else if (token.isTag()) {
            this.writeByte(token.getTag().getId());
        }
    }

    private byte getTokenTypeFor(Token token) {
        if (token.isSeparator())
            return Token.TYPE_SEPARATOR;
        if (token.isWord())
            return Token.TYPE_WORD;
        if (token.isEquivalenceClass())
            return Token.TYPE_EQUIVALENCE_CLASS;
        if (token.isTag())
            return Token.TYPE_TAG;
        throw new IllegalArgumentException("Token type " + token.getClass() + " can't be written (unknow type)");
    }

    @Override
    public void append(Token token) throws IOException {
        this.writeToken(token);
    }
}
