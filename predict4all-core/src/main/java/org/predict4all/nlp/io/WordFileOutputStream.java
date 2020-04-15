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

import org.predict4all.nlp.words.model.UserWord;
import org.predict4all.nlp.words.model.Word;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class WordFileOutputStream extends DataOutputStream {

    public WordFileOutputStream(File outputFile) throws IOException {
        super(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(outputFile))));
    }

    public void writeWord(Word word) throws IOException {
        byte type = word.getType();
        this.writeByte(type);
        this.writeInt(word.getID());
        if (type == Word.TYPE_SIMPLE || type == Word.TYPE_USER_WORD) {
            this.writeUTF(word.getWord());
            this.writeBoolean(word.isModifiedByUser());
            this.writeBoolean(word.isModifiedBySystem());
            this.writeDouble(word.getProbFactor());
            this.writeBoolean(word.isForceInvalid());
            this.writeBoolean(word.isForceValid());
        }
        if (type == Word.TYPE_USER_WORD) {
            UserWord userWord = (UserWord) word;
            this.writeInt(userWord.getUsageCount());
            this.writeLong(userWord.getLastUseDate());
        }
        if (type == Word.TYPE_EQUIVALENCE_CLASS) {
            this.writeByte(word.getEquivalenceClassId());
        }
        if (type == Word.TYPE_NGRAM_TAG) {
            this.writeByte(word.getNGramTagId());
        }
    }
}
