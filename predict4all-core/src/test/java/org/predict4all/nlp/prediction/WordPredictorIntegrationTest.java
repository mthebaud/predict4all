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

package org.predict4all.nlp.prediction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class WordPredictorIntegrationTest extends AbstractWordPredictionIntegrationTest {

    // WORD COUNT FOR - it_predictor_1

    // (6) de, (4) que, (4) prédiction, (4) est, (3) la, (2) mots, (2) plus, (1) ainsi, (1) vérifier, (1) tester, (1) tout,
    // (1) ceci, (1) un, (1) essai, (1) but, (1) le, (1) fonctionne, (1) bonjour, (1) régulière, (1) revanche, (1) j',
    // (1) ai, (1) aimé, (1) bien, (1) en, (1) pense, (1) mais, (1) correctement, (1) choses, (1) variée, (1) on, (1) l, (1) ce

    @Test
    public void testSimplePredictionNGram() throws Exception {
        this.wordPredictor = trainFullPredictionModel("it_predictor_1");

        assertPredictionEquals("", 5, IGNORE_ORDER, "Bonjour", "Le", "Mais", "En", "J'", IGNORE_ORDER);
        assertPredictionEquals("bonjour ", 5, "ceci", "de", IGNORE_ORDER, "que", "prédiction", "est", IGNORE_ORDER);
        assertPredictionEquals("car ainsi ", 7, "que", IGNORE_ORDER, "de", "prédiction", "est", "la", "mots", "plus", IGNORE_ORDER);
        assertPredictionEquals("prédiction de ", 6, "mots", "choses", IGNORE_ORDER, "prédiction", "vérifier", "tester", "de", IGNORE_ORDER);
        assertPredictionEquals("plus variée que ", 4, "ce", IGNORE_ORDER, "de", "l'", "tout", IGNORE_ORDER);
        assertPredictionEquals("prédiction de c", 5, "choses", IGNORE_ORDER, "ceci", "correctement", "ce", IGNORE_ORDER);
    }

    // Pour ma part je relativise bien.
    // nous avons réaménagé nos bureaux ça me donne déjà beaucoup plus la pêche mais ce qui m'aide davantage c'est que j'ai enfin obtenu ma journée de télétravail.
    // désormais, je travaille de chez moi toute la journée du vendredi.
    // c'est pourquoi je relativise.
    // Avec un nouveau boulot, je n'aurais peut-être plus cette possibilité.
    // c'est pourquoi je reste.

    //	(5) je,	(3) c,	(3) est,	(2) pourquoi,	(2) journée,	(2) de,	(2) plus,	(2) la,	(2) ma,	(2) relativise,	(1) m
    //	(1) beaucoup,	(1) qui,	(1) ce,	(1) mais,	(1) pêche,	(1) déjà,	(1) donne,	(1) aide,	(1) nous,	(1) bien
    //	(1) reste,	(1) part,	(1) avons,	(1) réaménagé,	(1) me,	(1) ça,	(1) bureaux,	(1) nos,	(1) possibilité,	(1) davantage
    //	(1) un,	(1) avec,	(1) vendredi,	(1) du,	(1) nouveau,	(1) boulot,	(1) être,	(1) peut,	(1) aurais,	(1) n,	(1) toute
    //	(1) moi,	(1) j,	(1) que,	(1) pour,	(1) cette,	(1) ai,	(1) enfin,	(1) chez,	(1) travaille,	(1) désormais,	(1) obtenu,	(1) télétravail

    @Test
    public void testSimplePredictionNGram2() throws Exception {
        this.wordPredictor = trainFullPredictionModel("it_predictor_2");

        // Commentaire : Predict4All prend en compte les débuts de phrase, et donc, ici le résultat inclus d'abord les éléments en début de phrase (NGram) puis ensuite va compléter avec les unigram les plus fréquents
        assertPredictionEquals("", 14, "c'", IGNORE_ORDER, "pour", "nous", "désormais", "avec", IGNORE_ORDER, "je", "est", IGNORE_ORDER, "pourquoi",
                "journée", "de", "plus", "la", "ma", "relativise", IGNORE_ORDER);

        // Commentaire : Predict4All propose les suites probables directement (quand présence d'un apostrophe dans le mot)
        assertPredictionEquals("c", 5, "c'", "c'est", IGNORE_ORDER, "ce", "chez", "cette", IGNORE_ORDER);

        assertPredictionEquals("ce ", 4, "qui", "je", IGNORE_ORDER, "c'", "est", IGNORE_ORDER);

        assertPredictionEquals("ce que j'aimerais ", 10, "je", IGNORE_ORDER, "c'", "est", IGNORE_ORDER, IGNORE_ORDER, "ma", "relativise", "plus",
                "la", "journée", "de", "pourquoi", IGNORE_ORDER);

        assertPredictionEquals("ce que j'aimerais l", 1, "la");
        assertPredictionEquals("ce que j'aimerais le", 1);

        assertPredictionEquals("ce que j'aimerais le ", 10, "je", IGNORE_ORDER, "c'", "est", IGNORE_ORDER, IGNORE_ORDER, "ma", "relativise", "plus",
                "la", "journée", "de", "pourquoi", IGNORE_ORDER);

        assertPredictionEquals("ce que j'aimerais le p", 7, IGNORE_ORDER, "plus", "pourquoi", IGNORE_ORDER, IGNORE_ORDER, "pour", "part", "pêche",
                "possibilité", "peut-être", IGNORE_ORDER);// ajout de "peut-être" également présent une fois

        assertPredictionEquals("ce q", 2, "qui", "que");

        // Ici erreur mis en évidence, le résultat est "c'" puis "c'est", on devrait proposer "est" ou "c'est" mais sans doute pas "c'"
        // assertPredictionEquals("ce que j'aimerais le plus c'", 4, "est", "je", IGNORE_ORDER, "c'", "est", IGNORE_ORDER);

        // Correction erreur de frappe > sans "est"
        assertPredictionEquals("ce que j'aimerais le plus c'est ", 2, "pourquoi", "que");

        assertPredictionEquals("ce que j'aimerais le plus c'est q", 2, "que", "qui");
        assertPredictionEquals("ce que Pierre et m", 5, "ma", IGNORE_ORDER, "me", "mais", "m'", "moi", IGNORE_ORDER);
        assertPredictionEquals("ce que Pierre et mo", 1, "moi");

        // Ici, pas travaille, télétravail et toute apparaissent une fois, pourquoi mettre "toute" après ?
        assertPredictionEquals("on t", 3, IGNORE_ORDER, "travaille", "télétravail", "toute", IGNORE_ORDER);

        assertPredictionEquals("on tr", 1, "travaille");
    }
}
