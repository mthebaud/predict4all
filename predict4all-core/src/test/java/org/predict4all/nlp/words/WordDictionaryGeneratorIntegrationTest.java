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

package org.predict4all.nlp.words;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.exception.WordDictionaryMatchingException;
import org.predict4all.nlp.language.BaseWordDictionary;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.language.french.FrenchLanguageModel;
import org.predict4all.nlp.parser.Tokenizer;
import org.predict4all.nlp.parser.matcher.TokenConverter;
import org.predict4all.nlp.trainer.TrainerTask;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.words.model.SimpleWord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests to generate a word dictionary
 *
 * @author Mathieu THEBAUD
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class WordDictionaryGeneratorIntegrationTest {
    private WordDictionaryGenerator wordDictionaryGenerator;
    private TrainingConfiguration trainingConfiguration;
    private LanguageModel languageModel;
    private BaseWordDictionary baseWordDictionary;

    @Before
    public void setUp() throws IOException {
        this.trainingConfiguration = mock(TrainingConfiguration.class);
        this.languageModel = mock(FrenchLanguageModel.class);
        when(this.languageModel.getTokenMatchersForNGram()).thenReturn(FrenchLanguageModel.MATCHERS_NGRAM_FR);
        this.baseWordDictionary = mock(BaseWordDictionary.class);
        when(this.languageModel.getBaseWordDictionary(trainingConfiguration)).thenReturn(baseWordDictionary);
        this.wordDictionaryGenerator = new WordDictionaryGenerator(languageModel, this.trainingConfiguration);
    }

    @Test
    public void testWordDictionaryGenerationKeepUppeBaseDictionaryCaseFrequencyLow() throws Exception {

        when(this.trainingConfiguration.getConvertCaseFromDictionaryModelThreshold()).thenReturn(1E-8);
        when(this.baseWordDictionary.getFrequency("paris")).thenReturn(1E-9);
        when(this.trainingConfiguration.getDirectlyValidWordCountThreshold()).thenReturn(10);

        // Configure
        when(this.baseWordDictionary.containsWord("ceci")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("Ceci")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("est")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("un")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("test")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("Je")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("je")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("vais")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("à")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("paris")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("demain")).thenReturn(true);

        // Integration test : generating word dictionary
        WordDictionary loadedDictionary = createAndLoadDictionary("it_worddic_1");

        assertEquals("ceci", loadedDictionary.getWord("ceci").getWord());
        assertEquals(Tag.UNKNOWN, loadedDictionary.getWord("Ceci").getNGramTag());

        assertEquals("est", loadedDictionary.getWord("est").getWord());
        assertTrue(loadedDictionary.getWord("est").getClass().equals(SimpleWord.class));

        assertEquals("je", loadedDictionary.getWord("je").getWord());
        assertEquals(Tag.UNKNOWN, loadedDictionary.getWord("Je").getNGramTag());

        assertEquals("Paris", loadedDictionary.getWord("Paris").getWord());
        assertEquals(Tag.UNKNOWN, loadedDictionary.getWord("paris").getNGramTag());
    }

    @Test
    public void testWordDictionaryGenerationTestBaseWord() throws Exception {
        when(this.trainingConfiguration.getDirectlyValidWordCountThreshold()).thenReturn(4);
        when(this.trainingConfiguration.getUpperCaseReplacementThreshold()).thenReturn(0.35);

        when(this.trainingConfiguration.getConvertCaseFromDictionaryModelThreshold()).thenReturn(1E-8);
        when(this.baseWordDictionary.getFrequency("livre")).thenReturn(1E-6);

        // Configure
        when(this.baseWordDictionary.containsWord("j'")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("adore")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("aller")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("ajourd'hui")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("dans")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("ce")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("mange-test")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("pourtant")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("ce")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("n'")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("est")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("pas")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("toujours")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("facile")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("aussi")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("test")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("autre")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("enfin")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("ceci")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("fini")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("koala")).thenReturn(false);
        when(this.baseWordDictionary.containsWord("joie")).thenReturn(false);
        when(this.baseWordDictionary.containsWord("Fois")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("fois")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("Moi")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("moi")).thenReturn(true);
        when(this.baseWordDictionary.containsWord("livre")).thenReturn(true);

        // Integration test : generating word dictionary
        WordDictionary loadedDictionary = createAndLoadDictionary("it_worddic_2");

        assertEquals("j'", loadedDictionary.getWord("j'").getWord());
        assertTrue(loadedDictionary.getWord("j'").getClass().equals(SimpleWord.class));

        assertEquals("adore", loadedDictionary.getWord("adore").getWord());
        assertTrue(loadedDictionary.getWord("adore").getClass().equals(SimpleWord.class));

        assertEquals("mange-test", loadedDictionary.getWord("mange-test").getWord());
        assertTrue(loadedDictionary.getWord("mange-test").getClass().equals(SimpleWord.class));

        assertEquals("pourtant", loadedDictionary.getWord("pourtant").getWord());
        assertTrue(loadedDictionary.getWord("pourtant").getClass().equals(SimpleWord.class));

        assertEquals("aussi", loadedDictionary.getWord("aussi").getWord());
        assertTrue(loadedDictionary.getWord("aussi").getClass().equals(SimpleWord.class));

        assertEquals("enfin", loadedDictionary.getWord("enfin").getWord());
        assertTrue(loadedDictionary.getWord("enfin").getClass().equals(SimpleWord.class));

        assertEquals("koala", loadedDictionary.getWord("koala").getWord());
        assertTrue(loadedDictionary.getWord("koala").getClass().equals(SimpleWord.class));

        assertEquals(Tag.UNKNOWN, loadedDictionary.getWord("joie").getNGramTag());

        assertEquals("fois", loadedDictionary.getWord("fois").getWord());
        assertTrue(loadedDictionary.getWord("fois").getClass().equals(SimpleWord.class));
        assertEquals(Tag.UNKNOWN, loadedDictionary.getWord("Fois").getNGramTag());

        assertEquals("Moi", loadedDictionary.getWord("Moi").getWord());
        assertTrue(loadedDictionary.getWord("Moi").getClass().equals(SimpleWord.class));

        assertEquals("moi", loadedDictionary.getWord("moi").getWord());
        assertTrue(loadedDictionary.getWord("moi").getClass().equals(SimpleWord.class));

        assertEquals("livre", loadedDictionary.getWord("livre").getWord());
        assertTrue(loadedDictionary.getWord("livre").getClass().equals(SimpleWord.class));
        assertEquals(Tag.UNKNOWN, loadedDictionary.getWord("lIvre").getNGramTag());
        assertEquals(Tag.UNKNOWN, loadedDictionary.getWord("liVRe").getNGramTag());
    }

    private WordDictionary createAndLoadDictionary(String fileName)
            throws IOException, Exception, FileNotFoundException, WordDictionaryMatchingException {
        TrainingCorpus corpus = new TrainingCorpus(1, new File("./src/test/resources/" + fileName), new File("./build/tmp/" + fileName + "/output"),
                "utf-8");
        Tokenizer tokenizer = new Tokenizer(this.languageModel);
        TokenConverter tokenConverter = new TokenConverter(languageModel.getTokenMatchersForNGram());
        DUMB_TASK_EXECUTOR.accept(tokenizer.tokenize(corpus));
        DUMB_TASK_EXECUTOR.accept(tokenConverter.executeTokenPatternMatching(corpus));
        File dictionaryFile = new File("./build/tmp/" + fileName + "/output/dict.bin");
        wordDictionaryGenerator.createWordDictionary(corpus, DUMB_TASK_EXECUTOR, dictionaryFile);

        // Load result dictionary
        WordDictionary loadedDictionary = WordDictionary.loadDictionary(languageModel, dictionaryFile);
        return loadedDictionary;
    }

    private static final Consumer<List<TrainerTask>> DUMB_TASK_EXECUTOR = (tasks) -> tasks.forEach(t -> {
        try {
            t.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

}
