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

import org.predict4all.nlp.language.BaseWordDictionary;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * French dictionary based on <a href="http://www.lexique.org/">Lexique.org</a>
 *
 * @author Mathieu THEBAUD
 */
public class FrenchBaseWordDictionary implements BaseWordDictionary {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrenchBaseWordDictionary.class);

    private HashMap<String, Double> words;
    private double sum;
    private final String baseWordDictionaryPath;

    public FrenchBaseWordDictionary(String baseWordDictionaryPath) {
        super();
        this.baseWordDictionaryPath = baseWordDictionaryPath;
    }

    @Override
    public boolean containsWord(String word) {
        return words.containsKey(word);
    }

    @Override
    public double getFrequency(String word) {
        Double oc = words.get(word);
        return oc != null ? oc / sum : -1.0;
    }

    @Override
    public void initialize() throws IOException {
        LOGGER.info("Will initialize french base word dictionary from {}", this.baseWordDictionaryPath);
        words = new HashMap<>(120_000);
        long start = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(this.baseWordDictionaryPath), StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            int filledLineCount = 0;
            while ((line = br.readLine()) != null) {
                if (Predict4AllUtils.isNotBlank(line)) {
                    filledLineCount++;
                    String[] split = line.split("\t");
                    double val = Double.parseDouble(split[9]);
                    String word = split[0];
                    Double previousFreq = words.get(word);
                    if (previousFreq == null || val > previousFreq && word.length() > 1) {
                        words.put(word, val);
                    }
                }
            }
            LOGGER.info("Base word dictionary loaded, {} words loaded ({} lines) in {} ms", words.size(), filledLineCount,
                    System.currentTimeMillis() - start);
        }
        sum = words.values().parallelStream().mapToDouble(s -> s).sum();
    }

    @Override
    public boolean isInitialized() {
        return words != null;
    }
}
