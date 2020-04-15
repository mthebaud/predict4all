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

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.token.*;

import java.io.*;

public class TokenFileInputStream extends DataInputStream implements TokenProvider {

    public TokenFileInputStream(File outputFile) throws IOException {
        super(new BufferedInputStream(new FileInputStream(outputFile)));
    }

    public Token readToken() throws IOException {
        try {
            byte typeValue = this.readByte();
            if (typeValue == Token.TYPE_WORD) {
                return WordToken.create(this.readUTF());
            } else if (typeValue == Token.TYPE_EQUIVALENCE_CLASS) {
                EquivalenceClass ecVal = EquivalenceClass.getECById(this.readByte());
                String text = this.readUTF();
                return EquivalenceClassToken.create(text, ecVal);
            } else if (typeValue == Token.TYPE_SEPARATOR) {
                return SeparatorToken.create(Separator.getSeparatorById(this.readByte()));
            } else if (typeValue == Token.TYPE_TAG) {
                return TagToken.create(Tag.getById(this.readByte()));
            } else {
                throw new IOException("Unknown token type byte : " + typeValue);
            }
        } catch (EOFException eof) {
            return null;
        }
    }

    @Override
    public Token getNext() throws IOException {
        return this.readToken();
    }
}
