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

package org.predict4all.nlp.language.french;

import gnu.trove.set.hash.TIntHashSet;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.language.StopWordDictionary;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.words.WordDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FrenchStopWordDictionary implements StopWordDictionary {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrenchStopWordDictionary.class);

    private TIntHashSet stopWordIds;
    private final String stopWordDictionaryPath;

    public FrenchStopWordDictionary(String stopWordDictionaryPath) {
        super();
        this.stopWordDictionaryPath = stopWordDictionaryPath;
    }

    @Override
    public boolean containsWord(int wordId) {
        return this.stopWordIds.contains(wordId);
    }

    @Override
    public boolean isInitialized() {
        return stopWordIds != null;
    }

    @Override
    public void initialize(WordDictionary wordDictionary) throws IOException {
        stopWordIds = new TIntHashSet();
        long start = System.currentTimeMillis();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                final int wordId = wordDictionary.getWordId(line);
                if (wordId != Tag.UNKNOWN.getId()) {
                    stopWordIds.add(wordId);
                }
            }
            stopWordIds.compact();
            LOGGER.info("French stop word dictionary initialized in {} ms, {} stop words loaded", System.currentTimeMillis() - start,
                    stopWordIds.size());
        }
    }

    private InputStream getInputStream() throws FileNotFoundException {
        if (Predict4AllUtils.isNotBlank(this.stopWordDictionaryPath)) {
            return new FileInputStream(this.stopWordDictionaryPath);
        } else {
            return this.getClass().getResourceAsStream("/language/fr/stopword.txt");
        }
    }
}
