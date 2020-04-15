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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.predict4all.nlp.Predict4AllInfo;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.language.french.FrenchLanguageModel;
import org.predict4all.nlp.trainer.DataTrainer;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageDataModelTrainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageDataModelTrainer.class);

    @SuppressWarnings("serial")
    private static final Map<String, LanguageModel> LANGUAGES = new HashMap<String, LanguageModel>() {
        {
            put("fr", new FrenchLanguageModel());
        }
    };

    public static void main(String[] args) throws Exception {
        LOGGER.info("PREDICT4ALL {} - {}", Predict4AllInfo.VERSION, Predict4AllInfo.BUILD_DATE);
        LOGGER.info("Language model trainer script");

        for (String language : LANGUAGES.keySet()) {
            LOGGER.info("Available language : {}", language);
        }

        LanguageDataModelTrainerArgs config = new LanguageDataModelTrainerArgs();
        final JCommander commandParsing = JCommander.newBuilder().programName("").addObject(config).build();

        try {
            commandParsing.parse(args);

            LanguageModel languageModel = LANGUAGES.get(Predict4AllUtils.lowerCase(config.getLanguage()));
            if (languageModel != null) {
                File ngramDic = new File(config.getNgramDictionary());
                File dic = new File(config.getWordDictionary());
                if (checkFileExists(config, ngramDic) && checkFileExists(config, dic)) {
                    File configFile = new File(config.getConfiguration());
                    if (configFile.exists()) {
                        List<String> input = config.getInput();
                        if (input != null && input.size() == 1) {
                            File workingDirectory = Files.createTempDirectory("predict4all-training-").toFile();
                            TrainingConfiguration trainingConfiguration = TrainingConfiguration.loadFrom(configFile, new File(input.get(0)));
                            LOGGER.info(
                                    "Will launch PREDICT4ALL data trainer, for\n\tCorpus = {}\n\tLanguage = {}\n\tWorking directory = {}\n\tOutput dictionary = {}\n\tOutput ngram = {}",
                                    trainingConfiguration.getCorpus(), languageModel.getId(), workingDirectory.getAbsolutePath(), dic, ngramDic);
                            DataTrainer dt = new DataTrainer(workingDirectory, dic, ngramDic, null, languageModel, trainingConfiguration);
                            dt.launchNGramTraining(TrainingStep.PARSER);
                        } else {
                            LOGGER.error("Bad usage, please give one input directory (main param)");
                            System.exit(-1);
                        }
                    } else {
                        LOGGER.error("Missing training configuration file ({}), please check the \"-config\" param", configFile.getAbsolutePath());
                        System.exit(-1);
                    }
                }
            } else {
                LOGGER.error("Language {} not found, check the available language list", config.getLanguage());
                System.exit(-1);
            }
        } catch (ParameterException pe) {
            StringBuilder usage = new StringBuilder();
            commandParsing.usage(usage);
            LOGGER.error("Invalid usage, check command line usage : \n{}", usage);
            System.exit(-1);
        }
    }

    private static boolean checkFileExists(LanguageDataModelTrainerArgs config, File file) {
        if (file.exists() && !config.isOverwrite()) {
            LOGGER.error("Data file {} already exists, training aborted (you can use the \"-overwrite\" param to ignore existing data file", file);
            return false;
        } else {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            return true;
        }
    }

}
