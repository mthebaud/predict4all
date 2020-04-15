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

package org.predict4all.nlp.parser.matcher;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.language.french.FrenchLanguageModel;
import org.predict4all.nlp.parser.Tokenizer;
import org.predict4all.nlp.parser.TokenizerUtils;
import org.predict4all.nlp.parser.token.EquivalenceClassToken;

import java.io.IOException;

import static org.predict4all.nlp.Separator.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class TokenConverterFrTest {

    //TODO : test for semantic matcher

    private TokenConverter tokenConverterNgram;
    //private TokenConverter tokenConverterSemantic;

    @Before
    public void setUp() {
        this.tokenConverterNgram = new TokenConverter(FrenchLanguageModel.MATCHERS_NGRAM_FR);
        //this.tokenConverterSemantic = new TokenConverter(new FrenchLanguageModel().getTokenMatchersForSemanticAnalysis());
    }

    @Test
    public void testPercent() throws IOException {
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, "50", PERCENT)), "à",
                SPACE, EquivalenceClassToken.create("50%", EquivalenceClass.PERCENT));
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, "50", SPACE, PERCENT)),
                "à", SPACE, EquivalenceClassToken.create("50%", EquivalenceClass.PERCENT));
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, "50", COMMA, "872", SPACE, PERCENT)), "à",
                SPACE, EquivalenceClassToken.create("50,872%", EquivalenceClass.PERCENT));
    }

    @Ignore
    @Test
    public void testPercentNegative() throws IOException {
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, HYPHEN, "50", PERCENT)),
                "à", SPACE, EquivalenceClassToken.create("-50%", EquivalenceClass.PERCENT));
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram
                        .executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, HYPHEN, "50", COMMA, "872", SPACE, PERCENT)),
                "à", SPACE, EquivalenceClassToken.create("-50,872%", EquivalenceClass.PERCENT));
    }

    @Test
    public void testSpecialWord() throws IOException {
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("car", SPACE, "aujourd", APOSTROPHE, "hui")), "car",
                SPACE, "aujourd'hui");
    }

    @Test
    public void testNumberDecimal() throws IOException {
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, "982", COMMA, "762")),
                "à", SPACE, EquivalenceClassToken.create("982,762", EquivalenceClass.DECIMAL));
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, "982", POINT, "762")),
                "à", SPACE, EquivalenceClassToken.create("982,762", EquivalenceClass.DECIMAL));
    }

    @Ignore
    @Test
    public void testNumberNegativeDecimal() throws IOException {
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, HYPHEN, "982", COMMA, "762")), "à", SPACE,
                EquivalenceClassToken.create("-982,762", EquivalenceClass.DECIMAL));
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, HYPHEN, "982", POINT, "762")), "à", SPACE,
                EquivalenceClassToken.create("-982,762", EquivalenceClass.DECIMAL));
    }

    @Test
    public void testNumberInt() throws IOException {
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, "98")), "à", SPACE,
                EquivalenceClassToken.create("98", EquivalenceClass.INTEGER));
    }

    @Ignore
    @Test
    public void testNegativeNumberInt() throws IOException {
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("à", SPACE, HYPHEN, "98")), "à",
                SPACE, EquivalenceClassToken.create("-98", EquivalenceClass.INTEGER));
    }

    @Test
    public void testApostrophe() throws IOException {
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("l", APOSTROPHE, "avantage")), "l'",
                "avantage");
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("boys", APOSTROPHE, "names")),
                "boys'", "names");
    }

    @Test
    public void testAcronym() throws IOException {
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("U", POINT, "S", POINT, "A")),
                EquivalenceClassToken.create("USA", EquivalenceClass.ACRONYM));
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("U", POINT, "S", POINT, "A", POINT)),
                EquivalenceClassToken.create("USA", EquivalenceClass.ACRONYM));
        this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("U", POINT, "S", POINT, "A", POINT, POINT));
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("U", POINT, "S", POINT, "A", POINT, POINT)),
                EquivalenceClassToken.create("USA", EquivalenceClass.ACRONYM), POINT);

    }

    @Test
    public void testHyphen() throws IOException {
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("combien", SPACE, "a", HYPHEN, "t", HYPHEN, "il")),
                "combien", SPACE, "a-t-il");
        TokenizerUtils.assertEquals(
                this.tokenConverterNgram.executeTermDetection(TokenizerUtils.createTokensList("combien", SPACE, "a", HYPHEN, "t", HYPHEN)), "combien",
                SPACE, "a", HYPHEN, "t", HYPHEN);
    }

    @Test
    public void testCombineTokensParsing() throws IOException {
        Tokenizer tokenizer = new Tokenizer(new FrenchLanguageModel());
        TokenizerUtils.assertEquals(this.tokenConverterNgram.executeTermDetection(tokenizer.tokenize(
                "bonjour, combien y-a-t-il de jours ? 35.25 n'est pas une réponse correcte aujourd'hui ! 25,54874% des réponses sont aux U.S.A.")),
                "bonjour", COMMA, SPACE, "combien", SPACE, "y-a-t-il", SPACE, "de", SPACE, "jours", SPACE, QUESTION, SPACE,
                EquivalenceClassToken.create("35,25", EquivalenceClass.DECIMAL), SPACE, "n'", "est", SPACE, "pas", SPACE, "une", SPACE, "réponse",
                SPACE, "correcte", SPACE, "aujourd'hui", SPACE, EXCLAMATION, SPACE,
                EquivalenceClassToken.create("25,54874%", EquivalenceClass.PERCENT), SPACE, "des", SPACE, "réponses", SPACE, "sont", SPACE, "aux",
                SPACE, EquivalenceClassToken.create("USA", EquivalenceClass.ACRONYM));
    }
}
