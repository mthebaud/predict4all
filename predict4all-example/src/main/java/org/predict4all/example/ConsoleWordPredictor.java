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

package org.predict4all.example;

import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.language.french.FrenchDefaultCorrectionRuleGenerator;
import org.predict4all.nlp.language.french.FrenchLanguageModel;
import org.predict4all.nlp.ngram.dictionary.DynamicNGramDictionary;
import org.predict4all.nlp.ngram.dictionary.StaticNGramTrieDictionary;
import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.prediction.WordPredictor;
import org.predict4all.nlp.utils.Predict4AllUtils;
import org.predict4all.nlp.words.WordDictionary;
import org.predict4all.nlp.words.correction.CorrectionRule;
import org.predict4all.nlp.words.correction.CorrectionRuleNode;
import org.predict4all.nlp.words.correction.CorrectionRuleNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Full working example that predict next word from the current input stream.<br>
 * It demonstrate :
 * <ul>
 *     <li>How to load/use WordPredictor</li>
 *     <li>How to load/use/save dynamic models</li>
 *     <li>How to create and use default correction rules</li>
 *     <li>How to create custom correction rules</li>
 * </ul>
 *
 * @author Mathieu THEBAUD
 */
public class ConsoleWordPredictor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleWordPredictor.class);

    private static final File FILE_NGRAMS = new File("fr_ngrams.bin");
    private static final File FILE_WORDS = new File("fr_words.bin");
    private static final File USER_FILE_WORDS = new File("fr_user_ngrams.bin");
    private static final File USER_FILE_NGRAMS = new File("fr_ngram_words.bin");

    public static void main(String[] args) throws Exception {
        // Check default files exist
        checkDefaultFileExist(FILE_NGRAMS);
        checkDefaultFileExist(FILE_WORDS);

        // Init default configuration
        LanguageModel languageModel = new FrenchLanguageModel();
        PredictionParameter predictionParameter = new PredictionParameter(languageModel);

        // Configure correction rules
        configureCorrectionRules(predictionParameter);

        // Load dictionary (and user dictionary if exists)
        WordDictionary wordDictionary = WordDictionary.loadDictionary(languageModel, FILE_WORDS);
        if (USER_FILE_WORDS.exists()) {
            wordDictionary.loadUserDictionary(USER_FILE_WORDS);
            LOGGER.info("User dictionary loaded");
        }

        // Load dynamic ngram (if exists) or create a new one
        DynamicNGramDictionary dynamicNGramDictionary = USER_FILE_NGRAMS.exists() ? DynamicNGramDictionary.load(USER_FILE_NGRAMS) : new DynamicNGramDictionary(4);
        LOGGER.info("Dynamic ngram model loaded/created");
        try (StaticNGramTrieDictionary staticNGramTrieDictionary = StaticNGramTrieDictionary.open(FILE_NGRAMS)) {

            // Create predictor
            WordPredictor wordPredictor = new WordPredictor(predictionParameter, wordDictionary, staticNGramTrieDictionary, dynamicNGramDictionary);

            // Start taking input word input to test
            try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
                while (true) {
                    int lastAnswer = getAnswer(scanner);
                    switch (lastAnswer) {
                        case 1:
                            LOGGER.info("Enter the test input (type \"/menu\" to come back to choices):");
                            String textBeforeCaret;
                            while (!"/menu".equals(textBeforeCaret = scanner.nextLine())) {
                                LOGGER.info("Text before caret {}", textBeforeCaret);
                                wordPredictor.predict(textBeforeCaret).getPredictions().forEach(p -> LOGGER.info("{}", p));
                            }
                            break;
                        case 2:
                            LOGGER.info("Enter the text to train the dynamic model (type \"/menu\" to come back to choices):");
                            String textToTrainDynamicModel;
                            while (!"/menu".equals(textToTrainDynamicModel = scanner.nextLine())) {
                                wordPredictor.trainDynamicModel(textToTrainDynamicModel);
                                LOGGER.info("Model trained");
                            }
                            break;
                        case 3:
                            wordDictionary.saveUserDictionary(USER_FILE_WORDS);
                            dynamicNGramDictionary.saveDictionary(USER_FILE_NGRAMS);
                            LOGGER.info("Data file saved");
                            System.exit(0);
                            break;
                        case 4:
                            System.exit(0);
                            break;
                    }
                }
            }
        }
    }


    private static void configureCorrectionRules(PredictionParameter predictionParameter) {
        // Add some default rules : simulate kind of GBoard Android behavior
        CorrectionRuleNode root = new CorrectionRuleNode(CorrectionRuleNodeType.NODE);
        root.addChild(FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType.ACCENTS.generateNodeFor(predictionParameter));
        root.addChild(FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType.WORD_SPACE_APOSTROPHE.generateNodeFor(predictionParameter));

        // Add some default rules : add language specific rules
        root.addChild(FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType.M_FRONT_MBP.generateNodeFor(predictionParameter));
        root.addChild(FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType.WORD_ENDINGS.generateNodeFor(predictionParameter));

        // Simulate a custom rule add : our user sometimes forget "h" at the word start (like "homme")
        CorrectionRuleNode hRule = new CorrectionRuleNode(CorrectionRuleNodeType.LEAF);
        hRule.setCorrectionRule(CorrectionRule.ruleBuilder().withError("").withReplacement("h").withMaxIndexFromStart(1));
        root.addChild(hRule);

        // Simulate a custom rule : our user is always confused between "ai" and "é" and "è"
        CorrectionRuleNode eaiConfusionRule = new CorrectionRuleNode(CorrectionRuleNodeType.LEAF);
        eaiConfusionRule.setCorrectionRule(CorrectionRule.ruleBuilder().withConfusionSet("ai", "é", "è"));
        root.addChild(eaiConfusionRule);

        // Set information on prediction parameters
        predictionParameter.setCorrectionRulesRoot(root);
        predictionParameter.setEnableWordCorrection(true);
    }

    private static int getAnswer(Scanner scanner) {
        LOGGER.info("What do you want to test ?\n\t1 - Test prediction/completion\n\t2 - Train the dynamic model\n\t3 - Save and exit\n\t4 - Exit (without saving)\nType \"/menu\" to come back to choices.");
        do {
            String input = null;
            try {
                input = Predict4AllUtils.getOrDefault(scanner.nextLine(), "").trim();
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= 4) return choice;
            } catch (NumberFormatException nfe) {
            }
            LOGGER.warn("\"{}\" is not a correct choice, please enter a correct number between 1 - 4", input);
        } while (true);
    }

    private static void checkDefaultFileExist(File file) {
        if (!file.exists()) {
            LOGGER.error("Default data file {} doesn't exist", file.getAbsolutePath());
            System.exit(-1);
        }
    }

}
