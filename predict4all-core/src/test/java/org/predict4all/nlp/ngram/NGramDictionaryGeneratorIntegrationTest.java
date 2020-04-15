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

package org.predict4all.nlp.ngram;

import gnu.trove.set.hash.TIntHashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.language.BaseWordDictionary;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.language.french.FrenchLanguageModel;
import org.predict4all.nlp.ngram.dictionary.AbstractNGramDictionary;
import org.predict4all.nlp.ngram.dictionary.StaticNGramTrieDictionary;
import org.predict4all.nlp.parser.Tokenizer;
import org.predict4all.nlp.parser.matcher.TokenConverter;
import org.predict4all.nlp.trainer.TrainerTask;
import org.predict4all.nlp.trainer.configuration.NGramPruningMethod;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.words.WordDictionary;
import org.predict4all.nlp.words.WordDictionaryGenerator;
import org.predict4all.nlp.words.model.Word;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(BlockJUnit4ClassRunner.class)
public class NGramDictionaryGeneratorIntegrationTest {
    private LanguageModel languageModel;
    private TrainingConfiguration trainingConfiguration;
    private BaseWordDictionary baseWordDictionary;

    private static final Consumer<List<TrainerTask>> DUMB_TASK_EXECUTOR = (tasks) -> tasks.forEach(t -> {
        try {
            t.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    @Before
    public void setUp() {
        this.languageModel = mock(LanguageModel.class);
        this.baseWordDictionary = mock(BaseWordDictionary.class);
        this.trainingConfiguration = mock(TrainingConfiguration.class);
        when(this.languageModel.getBaseWordDictionary(trainingConfiguration)).thenReturn(baseWordDictionary);
        when(trainingConfiguration.getDirectlyValidWordCountThreshold()).thenReturn(0);
        when(trainingConfiguration.getSmoothingDiscountValueUpperBound()).thenReturn(0.9);
        when(trainingConfiguration.getSmoothingDiscountValueLowerBound()).thenReturn(0.1);
    }

    private Pair<WordDictionary, AbstractNGramDictionary<?>> createAndLoadDictionary(String fileName) throws Exception {
        TrainingCorpus corpus = new TrainingCorpus(1, new File("./src/test/resources/" + fileName), new File("./build/tmp/" + fileName + "/output"),
                "UTF-8");
        Tokenizer tokenizer = new Tokenizer(this.languageModel);
        TokenConverter tokenConverter = new TokenConverter(FrenchLanguageModel.MATCHERS_NGRAM_FR);
        DUMB_TASK_EXECUTOR.accept(tokenizer.tokenize(corpus));
        DUMB_TASK_EXECUTOR.accept(tokenConverter.executeTokenPatternMatching(corpus));
        File dictionaryFile = new File("./build/tmp/" + fileName + "/output/dict.bin");

        WordDictionaryGenerator wdg = new WordDictionaryGenerator(languageModel, trainingConfiguration);
        wdg.createWordDictionary(corpus, DUMB_TASK_EXECUTOR, dictionaryFile);

        WordDictionary loadedDictionary = WordDictionary.loadDictionary(languageModel, dictionaryFile);

        File ngramFile = new File("./build/tmp/" + fileName + "/output/ngrams.bin");
        NGramDictionaryGenerator ngramDictionaryGenerator = new NGramDictionaryGenerator(languageModel, trainingConfiguration, loadedDictionary);
        ngramDictionaryGenerator.executeNGramTraining(corpus, ngramFile, DUMB_TASK_EXECUTOR);

        StaticNGramTrieDictionary ngramDictionary = StaticNGramTrieDictionary.open(ngramFile);

        return Pair.of(loadedDictionary, ngramDictionary);
    }

    @Test
    public void testSimpleNGramDictionaryNoPruningContent() throws Exception {
        when(this.trainingConfiguration.getPruningMethod()).thenReturn(NGramPruningMethod.NONE);
        when(this.trainingConfiguration.getNgramOrder()).thenReturn(4);
        Pair<WordDictionary, AbstractNGramDictionary<?>> dictionnaries = createAndLoadDictionary("it_ngramdic_1");

        WordDictionary wordDictionary = dictionnaries.getLeft();
        AbstractNGramDictionary<?> ngramDictionary = dictionnaries.getRight();

        TIntHashSet nextWord1 = ngramDictionary
                .getNextWord(new int[]{wordDictionary.getWord("le").getID(), wordDictionary.getWord("chien").getID()});
        assertEquals(2, nextWord1.size());
        assertTrue(nextWord1.contains(wordDictionary.getWord("mange").getID()));
        assertTrue(nextWord1.contains(wordDictionary.getWord("qui").getID()));

        TIntHashSet nextWord2 = ngramDictionary.getNextWord(new int[]{wordDictionary.getWord("chien").getID()});
        assertEquals(3, nextWord2.size());
        assertTrue(nextWord2.contains(wordDictionary.getWord("mange").getID()));
        assertTrue(nextWord2.contains(wordDictionary.getWord("qui").getID()));
        assertTrue(nextWord2.contains(wordDictionary.getWord("doit").getID()));

        TIntHashSet nextWord3 = ngramDictionary.getNextWord(new int[]{wordDictionary.getWord("nourriture").getID()});
        assertNull(nextWord3);

        TIntHashSet nextWord4 = ngramDictionary.getNextWord(new int[]{Tag.START.getId()});
        assertEquals(4, nextWord4.size());
        assertTrue(nextWord4.contains(wordDictionary.getWord("le").getID()));
        assertTrue(nextWord4.contains(wordDictionary.getWord("car").getID()));
        assertTrue(nextWord4.contains(wordDictionary.getWord("un").getID()));
        assertTrue(nextWord4.contains(wordDictionary.getWord("mon").getID()));
    }

    @Test
    public void testSimpleNGramDictionaryNoPruningProbabilities() throws Exception {
        when(this.trainingConfiguration.getPruningMethod()).thenReturn(NGramPruningMethod.NONE);
        when(this.trainingConfiguration.getNgramOrder()).thenReturn(4);
        Pair<WordDictionary, AbstractNGramDictionary<?>> dictionnaries = createAndLoadDictionary("it_ngramdic_2");

        WordDictionary wordDictionary = dictionnaries.getLeft();
        AbstractNGramDictionary<?> ngramDictionary = dictionnaries.getRight();

        //int[] prefix = new int[] { wordDictionary.getWord("le").getID(), wordDictionary.getWord("chien").getID() };
        Consumer<int[]> testingProbs = prefix -> {
            double probMax = ngramDictionary.getProbability(prefix, 0, prefix.length, wordDictionary.getWord("part").getID());
            double probInterEq1 = ngramDictionary.getProbability(prefix, 0, prefix.length, wordDictionary.getWord("mange").getID());
            double probInterEq2 = ngramDictionary.getProbability(prefix, 0, prefix.length, wordDictionary.getWord("va").getID());

            assertTrue(probMax > probInterEq1);
            assertTrue(probMax > probInterEq2);
            assertEquals(probInterEq1, probInterEq2, 1E-10);
        };

        List<int[]> testingPrefixs = Arrays.asList(new int[]{wordDictionary.getWord("le").getID(), wordDictionary.getWord("chien").getID()},
                new int[]{wordDictionary.getWord("chien").getID()},
                new int[]{Tag.START.getId(), wordDictionary.getWord("le").getID(), wordDictionary.getWord("chien").getID()},
                new int[]{wordDictionary.getWord("un").getID(), wordDictionary.getWord("chien").getID()});

        for (int[] prefix : testingPrefixs) {
            testingProbs.accept(prefix);

            double sum = 0.0;
            Collection<Word> allWords = wordDictionary.getAllWords();
            for (Word word : allWords) {
                sum += ngramDictionary.getProbability(prefix, 0, prefix.length, word.getID());
            }
            assertEquals(1.0, sum, 1E-10);
        }

    }

    @Test
    public void testNGramPruningWDFull() throws Exception {
        when(this.trainingConfiguration.getPruningMethod()).thenReturn(NGramPruningMethod.WEIGHTED_DIFFERENCE_FULL_PROB);
        when(this.trainingConfiguration.getNgramOrder()).thenReturn(4);
        when(this.trainingConfiguration.getNgramPruningWeightedDifferenceThreshold()).thenReturn(0.05);
        Pair<WordDictionary, AbstractNGramDictionary<?>> dictionnaries = createAndLoadDictionary("it_ngramdic_3");

        WordDictionary wordDictionary = dictionnaries.getLeft();
        AbstractNGramDictionary<?> ngramDictionary = dictionnaries.getRight();

        List<int[]> testingPrefixs = Arrays.asList(new int[]{wordDictionary.getWord("est").getID(), wordDictionary.getWord("plus").getID()},
                new int[]{wordDictionary.getWord("plus").getID()},
                new int[]{Tag.START.getId(), wordDictionary.getWord("il").getID(), wordDictionary.getWord("est").getID()},
                new int[]{wordDictionary.getWord("en").getID(), wordDictionary.getWord("avec").getID()});

        for (int[] prefix : testingPrefixs) {
            double sum = 0.0;
            Collection<Word> allWords = wordDictionary.getAllWords();
            for (Word word : allWords) {
                sum += ngramDictionary.getProbability(prefix, 0, prefix.length, word.getID());
            }
            assertEquals(1.0, sum, 1E-10);
        }
    }

    @Test
    public void testNGramPruningWDRaw() throws Exception {
        when(this.trainingConfiguration.getPruningMethod()).thenReturn(NGramPruningMethod.WEIGHTED_DIFFERENCE_RAW_PROB);
        when(this.trainingConfiguration.getNgramOrder()).thenReturn(4);
        when(this.trainingConfiguration.getNgramPruningWeightedDifferenceThreshold()).thenReturn(0.05);
        Pair<WordDictionary, AbstractNGramDictionary<?>> dictionnaries = createAndLoadDictionary("it_ngramdic_3");

        WordDictionary wordDictionary = dictionnaries.getLeft();
        AbstractNGramDictionary<?> ngramDictionary = dictionnaries.getRight();

        List<int[]> testingPrefixs = Arrays.asList(new int[]{wordDictionary.getWord("est").getID(), wordDictionary.getWord("plus").getID()},
                new int[]{wordDictionary.getWord("plus").getID()},
                new int[]{Tag.START.getId(), wordDictionary.getWord("il").getID(), wordDictionary.getWord("est").getID()},
                new int[]{wordDictionary.getWord("en").getID(), wordDictionary.getWord("avec").getID()});

        for (int[] prefix : testingPrefixs) {
            double sum = 0.0;
            Collection<Word> allWords = wordDictionary.getAllWords();
            for (Word word : allWords) {
                sum += ngramDictionary.getProbability(prefix, 0, prefix.length, word.getID());
            }
            assertEquals(1.0, sum, 1E-10);
        }
    }
}
