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
import org.predict4all.nlp.prediction.WordPrediction;
import org.predict4all.nlp.prediction.WordPredictionResult;
import org.predict4all.nlp.prediction.WordPredictor;
import org.predict4all.nlp.words.WordDictionary;
import org.predict4all.nlp.words.correction.CorrectionRuleNode;
import org.predict4all.nlp.words.correction.CorrectionRuleNodeType;
import org.predict4all.nlp.words.model.Word;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Working example for <a href="https://github.com/mthebaud/predict4all">PREDICT4ALL</a>.
 * Mainly working quick examples.<br>
 * See {@link ConsoleWordPredictor} for more detailed example.
 *
 * @author Mathieu THEBAUD
 */
public class QuickExample {
    private static final File FILE_NGRAMS = new File("fr_ngrams.bin");
    private static final File FILE_WORDS = new File("fr_words.bin");

    public static void main(String[] args) throws Exception {
        System.out.println(FILE_NGRAMS.getAbsolutePath());
        LanguageModel languageModel = new FrenchLanguageModel();
        PredictionParameter predictionParameter = new PredictionParameter(languageModel);

        WordDictionary dictionary = WordDictionary.loadDictionary(languageModel, FILE_WORDS);
        try (StaticNGramTrieDictionary ngramDictionary = StaticNGramTrieDictionary.open(FILE_NGRAMS)) {
            showOptimizedWordPrediction(predictionParameter, dictionary, ngramDictionary);
            showSimpleNextWordPrediction(predictionParameter, dictionary, ngramDictionary);
            showCurrentWordPrediction(predictionParameter, dictionary, ngramDictionary);
            showCorrectionWordPrediction(predictionParameter, dictionary, ngramDictionary);
            showDynamicUserModel(predictionParameter, dictionary, ngramDictionary);
            disableUserWord(predictionParameter, dictionary, ngramDictionary);
        }
    }

    private static void disableUserWord(PredictionParameter predictionParameter, WordDictionary dictionary, StaticNGramTrieDictionary ngramDictionary) {
        Word maisonWord = dictionary.getWord("maison");
        maisonWord.setForceInvalid(true, true);

        dictionary.getAllWords().stream()
                .filter(w -> w.isValidToBePredicted(predictionParameter)) // don't want to display to the user the word that would never appears in prediction
                .filter(Word::isUserWord) // get only the user added words
                .forEach(w -> System.out.println(w.getWord()));
    }

    private static void showSimpleNextWordPrediction(PredictionParameter predictionParameter, WordDictionary dictionary, StaticNGramTrieDictionary ngramDictionary) throws Exception {
        WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary);
        WordPredictionResult predictionResult = wordPredictor.predict("j'aime manger des ");
        for (WordPrediction prediction : predictionResult.getPredictions()) {
            System.out.println(prediction);
        }
    }

    private static void showCurrentWordPrediction(PredictionParameter predictionParameter, WordDictionary dictionary, StaticNGramTrieDictionary ngramDictionary) throws Exception {
        WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary);
        WordPredictionResult predictionResult = wordPredictor.predict("je te r");
        for (WordPrediction prediction : predictionResult.getPredictions()) {
            System.out.println(prediction);
        }
    }

    private static void showOptimizedWordPrediction(PredictionParameter predictionParameter, WordDictionary dictionary, StaticNGramTrieDictionary ngramDictionary) throws Exception {
        WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary);
        WordPredictionResult predictionResult = wordPredictor.predict("je te r");
        Set<Integer> alreadyDisplayedWordIds = new HashSet<>();
        for (WordPrediction prediction : predictionResult.getPredictions()) {
            System.out.println(prediction);
            alreadyDisplayedWordIds.add(prediction.getWordId());
        }
        predictionResult = wordPredictor.predict("je te re", null, 5, alreadyDisplayedWordIds);
        for (WordPrediction prediction : predictionResult.getPredictions()) {
            System.out.println(prediction);
            alreadyDisplayedWordIds.add(prediction.getWordId());
        }
    }

    private static void showCorrectionWordPrediction(PredictionParameter predictionParameter, WordDictionary dictionary, StaticNGramTrieDictionary ngramDictionary) throws Exception {
        CorrectionRuleNode root = new CorrectionRuleNode(CorrectionRuleNodeType.NODE);
        root.addChild(FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType.ACCENTS.generateNodeFor(predictionParameter));
        predictionParameter.setCorrectionRulesRoot(root);
        predictionParameter.setEnableWordCorrection(true);

        WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary);
        WordPredictionResult predictionResult = wordPredictor.predict("il eta");
        for (WordPrediction prediction : predictionResult.getPredictions()) {
            System.out.println(prediction);
        }
    }

    private static void showDynamicUserModel(PredictionParameter predictionParameter, WordDictionary dictionary, StaticNGramTrieDictionary ngramDictionary) throws Exception {
        DynamicNGramDictionary dynamicNGramDictionary = new DynamicNGramDictionary(4);
        WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary, dynamicNGramDictionary);
        WordPredictionResult predictionResult = wordPredictor.predict("je vais à la ");
        for (WordPrediction prediction : predictionResult.getPredictions()) {
            System.out.println(prediction);
        }
        wordPredictor.trainDynamicModel("je vais à la gare", false);
        predictionResult = wordPredictor.predict("je vais à la ");
        for (WordPrediction prediction : predictionResult.getPredictions()) {
            System.out.println(prediction);
        }
        // Ngram saving/loading
        dynamicNGramDictionary.saveDictionary(new File("fr_user_ngrams.bin"));
        DynamicNGramDictionary.load(new File("fr_user_ngrams.bin"));
        // Dictionary saving/loading
        dictionary.saveUserDictionary(new File("fr_user_words.bin"));
        dictionary.loadUserDictionary(new File("fr_user_words.bin"));
    }
}
