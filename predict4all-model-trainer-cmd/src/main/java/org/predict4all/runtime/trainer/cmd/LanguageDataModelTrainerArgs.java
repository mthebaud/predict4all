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

package org.predict4all.runtime.trainer.cmd;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class LanguageDataModelTrainerArgs {
    // TODO : encoding
    // TODO : stop word dictionary
    // TODO : Lexique dictionary

    //TODO : exclude/include filter

    @Parameter(description = "<input> Directory where the training corpus is located (must contains text files)")
    private List<String> input = new ArrayList<>();

    @Parameter(names = "-overwrite", description = "If set, will ovewrite existing data files")
    private boolean overwrite = false;

    @Parameter(names = "-language", description = "Model language (ISO 639-1)", required = true)
    private String language;

    @Parameter(names = "-word-dictionary", description = "Output path for the generated word dictionary", required = true)
    private String wordDictionary;

    @Parameter(names = "-ngram-dictionary", description = "Output path for the generated ngram dictionary", required = true)
    private String ngramDictionary;

    @Parameter(names = "-config", description = "Path to the training configuration (json configuration file)", required = true)
    private String configuration;

    public List<String> getInput() {
        return input;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public String getLanguage() {
        return language;
    }

    public String getWordDictionary() {
        return wordDictionary;
    }

    public String getNgramDictionary() {
        return ngramDictionary;
    }

    public String getConfiguration() {
        return configuration;
    }
}
