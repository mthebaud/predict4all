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
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.words.model.*;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class WordFileInputStream extends DataInputStream {

    public WordFileInputStream(File outputFile) throws IOException {
        super(new BufferedInputStream(new GZIPInputStream(new FileInputStream(outputFile))));
    }

    public Word readWord() throws IOException {
        try {
            byte type = this.readByte();
            int wordID = this.readInt();
            String word = null;
            EquivalenceClass ec = null;
            Tag tag = null;
            boolean modifiedByUser = false, modifiedBySystem = false, forceInvalid = false, forceValid = false;
            int count = 0;
            long date = 0;
            double probFactor = 1.0;
            if (type == Word.TYPE_SIMPLE || type == Word.TYPE_USER_WORD) {
                word = this.readUTF();
                modifiedByUser = this.readBoolean();
                modifiedBySystem = this.readBoolean();
                probFactor = this.readDouble();
                forceInvalid = this.readBoolean();
                forceValid = this.readBoolean();
            }
            if (type == Word.TYPE_EQUIVALENCE_CLASS) {
                ec = EquivalenceClass.getECById(this.readByte());
            }
            if (type == Word.TYPE_NGRAM_TAG) {
                tag = Tag.getById(this.readByte());
            }
            if (type == Word.TYPE_USER_WORD) {
                count = this.readInt();
                date = this.readLong();
            }
            Word w = null;
            if (type == Word.TYPE_NGRAM_TAG)
                w = new TagWord(tag);
            else if (type == Word.TYPE_EQUIVALENCE_CLASS)
                w = new EquivalenceClassWord(ec);
            else if (type == Word.TYPE_SIMPLE)
                w = modifiedByUser || modifiedBySystem
                        ? SimpleWord.createModified(wordID, word, modifiedByUser, modifiedBySystem, probFactor, forceInvalid, forceValid)
                        : SimpleWord.create(wordID, word);
            else if (type == Word.TYPE_USER_WORD)
                w = new UserWord(wordID, word, probFactor, forceInvalid, forceValid, date, count);
            return w;
        } catch (EOFException eof) {
            return null;
        }
    }
}
