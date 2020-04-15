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

package org.predict4all.nlp.words.correction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.predict4all.nlp.language.french.FrenchDefaultCorrectionRuleGenerator;
import org.predict4all.nlp.prediction.AbstractWordPredictionIntegrationTest;
import org.predict4all.nlp.language.french.FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType;

import static org.mockito.Mockito.when;

@RunWith(BlockJUnit4ClassRunner.class)
public class WordCorrectionIntegrationTest extends AbstractWordPredictionIntegrationTest {

    // http://compteur-de-mots.net/

    @Before
    public void setUp() {
        super.setUp();
        when(this.predictionParameter.isEnableWordCorrection()).thenReturn(true);
        when(this.predictionParameter.getCorrectionMaxCost()).thenReturn(3.5);
    }

    //(3) il
    //(1) jamais
    //(1) pu
    //(1) aller
    //(1) vers
    //(1) avait
    //(1) n
    //(1) était
    //(1) un
    //(1) grand
    //(1) homme
    //(1) là
    //(1) bas
    //(1) car
    //(1) ça
    //(1) lui
    //(1) allait
    //(1) intéressantes
    //(1) choses
    //(1) pourtant
    //(1) est
    //(1) allé
    //(1) des
    //(1) faire

    @Test
    public void testSimpleAccentCorrection() throws Exception {
        when(this.predictionParameter.getCorrectionRulesRoot())
                .thenReturn(generateNodeFor(FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType.ACCENTS));

        this.wordPredictor = trainFullPredictionModel("it_correction_1");

        assertPredictionEquals("il e", 5, IGNORE_ORDER, "était", "est", IGNORE_ORDER);
    }

    private CorrectionRuleNode generateNodeFor(CorrectionRuleType ruleType) {
        CorrectionRuleNode root = new CorrectionRuleNode(CorrectionRuleNodeType.NODE);
        root.addChild(ruleType.generateNodeFor(predictionParameter));
        return root;
    }
}
