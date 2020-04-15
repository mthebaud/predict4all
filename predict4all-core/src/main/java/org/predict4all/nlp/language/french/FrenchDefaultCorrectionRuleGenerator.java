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

import org.predict4all.nlp.prediction.PredictionParameter;
import org.predict4all.nlp.words.correction.CorrectionRule;
import org.predict4all.nlp.words.correction.CorrectionRuleNode;
import org.predict4all.nlp.words.correction.CorrectionRuleNodeType;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.predict4all.nlp.words.correction.CorrectionRule.ruleBuilder;

/**
 * Generate base correction rule for french language.<br>
 * Keep every possible rule in {@link CorrectionRuleType} with a translated name, description and example.
 *
 * @author Mathieu THEBAUD
 */
public class FrenchDefaultCorrectionRuleGenerator {

    // EXISTING TYPES
    //========================================================================
    public static enum CorrectionRuleType {
        ACCENTS("predict4all.default.rule.forget.accent", FrenchDefaultCorrectionRuleGenerator::generateAccents), //
        WORD_SPACE_APOSTROPHE("predict4all.default.rule.word.split", FrenchDefaultCorrectionRuleGenerator::generateSpaceApostrophe), //
        PHONEM_CONFUSION_SET("predict4all.default.rule.phonem.confusion", FrenchDefaultCorrectionRuleGenerator::generatePhonemConfusionSets), //
        DOUBLE_LETTER("predict4all.default.rule.double.letter", FrenchDefaultCorrectionRuleGenerator::generateDoubleLetterConfusion), //
        M_FRONT_MBP("predict4all.default.rule.mbp", FrenchDefaultCorrectionRuleGenerator::generateMFrontOfMBP), //
        WORD_ENDINGS("predict4all.default.rule.word.ends", FrenchDefaultCorrectionRuleGenerator::generateWordEndRules), //
        VISUAL_CONFUSION("predict4all.default.rule.visual.confusion", FrenchDefaultCorrectionRuleGenerator::generateVisualConfusions), //
        HEARING_CONFUSION("predict4all.default.rule.hearing.confusion", FrenchDefaultCorrectionRuleGenerator::generateHearingConfusions), //
        GE_GU_SOUND("predict4all.default.rule.ge.gu.sound", FrenchDefaultCorrectionRuleGenerator::generateGEGUSound), //
        SEQUENCES("predict4all.default.rule.char.sequences", FrenchDefaultCorrectionRuleGenerator::generateSequences), //
        ADD_LETTER("predict4all.default.rule.missing.letter", FrenchDefaultCorrectionRuleGenerator::generateAddLetterRule), //
        REMOVE_LETTER("predict4all.default.rule.added.letter", FrenchDefaultCorrectionRuleGenerator::generateRemoveLetterRule), //
        HOMOPHONE("predict4all.default.rule.homophone", FrenchDefaultCorrectionRuleGenerator::generateHomophone), //
        AZERTY_KEYBOARD("predict4all.default.rule.azerty", FrenchDefaultCorrectionRuleGenerator::generateAzerty), //
        ;

        private final String translationRootId;
        private final Function<PredictionParameter, CorrectionRuleNode> generator;

        private CorrectionRuleType(String labelId, Function<PredictionParameter, CorrectionRuleNode> generator) {
            this.translationRootId = labelId;
            this.generator = generator;
        }

        public String getNameId() {
            return translationRootId + ".name";
        }

        public String getDescriptionId() {
            return translationRootId + ".description";
        }

        public String getExampleId() {
            return translationRootId + ".example";
        }

        public CorrectionRuleNode generateNodeFor(PredictionParameter predictionParamer) {
            return generator.apply(predictionParamer);
        }
    }
    //========================================================================

    // CONSTANT
    //========================================================================
    private static final double DEFAULT_AZERTY_COST = 0.5;
    private static final double DEFAULT_ACCENT_COST = 0.5;
    private static final double DEFAULT_SPACE_SPLIT_COST = 1.5;
    private static final double DEFAULT_APOSTROPHE_SPLIT_COST = 1.0;
    private static final double DEFAULT_HOMOPHONE_COST = 0.4;
    private static final double DEFAULT_ORTHO_RULE_M = 0.4;
    private static final double DEFAULT_SEQUENCE_COST = 1.2;

    private static final String[][] AZERTY_KEYBOARD = { //
            {"a", "z", "e", "r", "t", "y", "u", "i", "o", "p"}, //
            {"q", "s", "d", "f", "g", "g", "h", "j", "k", "l", "m"}, //
            {null, "w", "x", "c", "v", "b", "n"},//
    };

    private static final String[] ALPHABET = IntStream.range(97, 123).mapToObj(c -> Character.toString((char) c)).toArray(String[]::new);
    private static final String[] VOWEL = {"a", "e", "i", "o", "u", "y"};
    private static final String[] CONSONANT = {"b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v", "w", "x", "z"};
    //========================================================================

    // GENERATE
    //========================================================================
    private static CorrectionRuleNode generateAzerty(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.azerty.name");

        // Create a map by inversion cost (factor)
        Map<Double, List<CorrectionRule>> rules = new HashMap<>();
        for (int r = 0; r < AZERTY_KEYBOARD.length; r++) {
            String[] row = AZERTY_KEYBOARD[r];
            for (int c = 0; c < row.length; c++) {
                String value = row[c];
                for (int rb = r; rb < AZERTY_KEYBOARD.length; rb++) {
                    String[] rowb = AZERTY_KEYBOARD[rb];
                    for (int cb = c; cb < rowb.length; cb++) {
                        if ((rb != r || cb != c) && value != null && rowb[cb] != null) {
                            double dist = (Math.sqrt((cb - c) * (cb - c) + (rb - r) * (rb - r)));
                            if (dist < predictionParamater.getCorrectionMaxCost()) {
                                ruleBuilder().withCost(dist).withFactor(DEFAULT_AZERTY_COST).withError(value).withReplacement(rowb[cb])
                                        .withBidirectional(true).addTo(rules.computeIfAbsent(dist, _k -> new ArrayList<>()));
                            }
                        }
                    }
                }
            }
        }

        // Create each rule node
        DecimalFormat formatDist = new DecimalFormat("##.000");
        rules.forEach((dist, ruleBuilders) -> {
            CorrectionRuleNode node = generateNode("predict4all.default.rule.azerty.rule.node.name", formatDist.format(dist));
            root.addChild(node);
            for (CorrectionRule ruleBuilder : ruleBuilders) {
                addNode(node, "[" + Arrays.stream(ruleBuilder.getErrors()).collect(Collectors.joining(", ")) + "] > ["
                        + Arrays.stream(ruleBuilder.getReplacements()).collect(Collectors.joining(", ")) + "]", ruleBuilder);
            }
        });

        return root;
    }

    private static CorrectionRuleNode generateAccents(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.forget.accent.name");
        addNode(root, "predict4all.default.rule.accent.e.char",
                ruleBuilder().withError("e").withReplacement("é", "è", "ê", "ë").withCost(DEFAULT_ACCENT_COST));
        addNode(root, "predict4all.default.rule.accent.o.char", ruleBuilder().withError("o").withReplacement("ô", "ö").withCost(DEFAULT_ACCENT_COST));
        addNode(root, "predict4all.default.rule.accent.a.char", ruleBuilder().withError("a").withReplacement("à", "â").withCost(DEFAULT_ACCENT_COST));
        addNode(root, "predict4all.default.rule.accent.i.char", ruleBuilder().withError("i").withReplacement("î", "ï").withCost(DEFAULT_ACCENT_COST));
        addNode(root, "predict4all.default.rule.accent.c.char", ruleBuilder().withError("c").withReplacement("ç").withCost(DEFAULT_ACCENT_COST));
        return root;
    }

    private static CorrectionRuleNode generateSpaceApostrophe(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.word.split.name");
        addNode(root, "predict4all.default.rule.apostrophe.missing", ruleBuilder().withError("").withReplacement("'")
                .withCost(DEFAULT_APOSTROPHE_SPLIT_COST).withMaxIndexFromStart(1).withMaxIndexFromStart(4));
        addNode(root, "predict4all.default.rule.space.missing",
                ruleBuilder().withError("").withReplacement(" ").withCost(DEFAULT_SPACE_SPLIT_COST).withMinIndexFromStart(2).withMaxIndexFromEnd(1));
        return root;
    }

    private static CorrectionRuleNode generatePhonemConfusionSets(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.phonem.confusion");

        // PHONETIQUE - https://www.forum.exionnaire.com/phonemes-alphabet-phonetique-international-api-4099
        // Voyelle ouverte antérieure non arrondie [a] - ignoré
        // Voyelle nasale ouverte postérieure arrondie [ã]
        addNode(root, "predict4all.default.rule.phonem.confusion.an.am.em.en", ruleBuilder().withConfusionSet("an", "am", "em", "en"));
        // Voyelle nasale antérieure non arrondie [ɛ]
        addNode(root, "predict4all.default.rule.phonem.confusion.in.ain",
                ruleBuilder().withConfusionSet("aim", "ain", "ein", "em", "en", "im", "in", "um", "un", "ym", "yn"));
        // Voyelle mi-ouverte postérieure arrondie [ɔ]
        addNode(root, "predict4all.default.rule.phonem.confusion.o", ruleBuilder().withConfusionSet("o", "oa", "oo"));
        // Voyelle mi-ouverte antérieure non arrondie [ɛ]
        addNode(root, "predict4all.default.rule.phonem.confusion.a.ai",
                ruleBuilder().withConfusionSet("a", "ai", "ay", "è", "ê", "ë", "ea", "ei", "ès", "ez", "êt", "es"));
        // Voyelle mi-ouverte antérieure arrondie [œ]
        addNode(root, "predict4all.default.rule.phonem.confusion.eu.oe", ruleBuilder().withConfusionSet("eu", "oe", "œ"));
        // Voyelle intermédiaire postérieure arrondie [ō] + Voyelle mi-fermée postérieure arrondie [o]
        addNode(root, "predict4all.default.rule.phonem.confusion.au.o", ruleBuilder().withConfusionSet("au", "eau", "o", "oa", "ô"));
        // Voyelle intermédiaire antérieure non arrondie [ē] - ignoré
        // Voyelle intermédiaire antérieure arrondie [œ]
        addNode(root, "predict4all.default.rule.phonem.confusion.e.eu", ruleBuilder().withConfusionSet("e", "eu", "eû", "oe"));
        // Voyelle nasale postérieure arrondie [ɔ]
        addNode(root, "predict4all.default.rule.phonem.confusion.om.on", ruleBuilder().withConfusionSet("om", "on"));
        // Voyelle mi-fermée antérieure non arrondie [e]
        addNode(root, "predict4all.default.rule.phonem.confusion.ez.ef", ruleBuilder().withConfusionSet("ez", "ef", "et", "ed", "é"));
        // Voyelle mi-fermée antérieure arrondie [ø]
        addNode(root, "predict4all.default.rule.phonem.confusion.eu.oe", ruleBuilder().withConfusionSet("eu", "eû", "oe"));
        // Voyelle fermée postérieure arrondie [u]
        addNode(root, "predict4all.default.rule.phonem.confusion.aou", ruleBuilder().withConfusionSet("aou", "aoû", "oo", "ou", "où", "oû"));
        // Voyelle fermée antérieure non arrondie [i]
        addNode(root, "predict4all.default.rule.phonem.confusion.i.y", ruleBuilder().withConfusionSet("i", "y", "ee"));
        // Voyelle fermée antérieure arrondie [y]
        addNode(root, "predict4all.default.rule.phonem.confusion.u.ul", ruleBuilder().withConfusionSet("u", "ul", "û"));
        // Consonne spirante labio-vélaire sonore [w] - ignoré
        // Consonne spirante labio-palatale sonore [ɥ]
        addNode(root, "predict4all.default.rule.phonem.confusion.u.yu", ruleBuilder().withConfusionSet("u", "yu"));
        // Consonne spirante palatale sonore [j]
        addNode(root, "predict4all.default.rule.phonem.confusion.il.ill", ruleBuilder().withConfusionSet("il", "ill", "ll", "g", "gh", "gui"));
        // Consonne fricative alvéolaire sourde [s]
        addNode(root, "predict4all.default.rule.phonem.confusion.c.ss.s", ruleBuilder().withConfusionSet("s", "ç", "c", "cc", "ls", "sc", "ss", "t"));
        // Consonne fricative alvéolaire sonore [z]
        addNode(root, "predict4all.default.rule.phonem.confusion.s.z.x", ruleBuilder().withConfusionSet("s", "z", "x", "si"));
        // Consonne fricative post-alvéolaire sourde [∫]
        addNode(root, "predict4all.default.rule.phonem.confusion.ch.sh", ruleBuilder().withConfusionSet("ch", "sh", "sc"));
        // Consonne fricative post-alvéolaire sonore [ʒ]
        addNode(root, "predict4all.default.rule.phonem.confusion.j.g", ruleBuilder().withConfusionSet("j", "g"));
        // Consonne fricative vélaire sourde [x] - ignoré
        // Consonne fricative labio-dentale sourde [f]
        addNode(root, "predict4all.default.rule.phonem.confusion.f.ph", ruleBuilder().withConfusionSet("f", "ff", "ph"));
        // Consonne fricative labio-dentale sonore [v]
        addNode(root, "predict4all.default.rule.phonem.confusion.w.v", ruleBuilder().withConfusionSet("w", "v"));
        // Consonne fricative uvulaire voisée [ʁ]
        addNode(root, "predict4all.default.rule.phonem.confusion.r.rr.rc", ruleBuilder().withConfusionSet("r", "rr", "rc"));
        // Consonne occlusive nasale alvéolaire sonore [n]
        addNode(root, "predict4all.default.rule.phonem.confusion.n.nn.mn", ruleBuilder().withConfusionSet("n", "nn", "mn"));
        // Consonne occlusive nasale bilabiale sonore [m]
        addNode(root, "predict4all.default.rule.phonem.confusion.n.mm", ruleBuilder().withConfusionSet("m", "mm"));
        // Consonne occlusive nasale vélaire sonore [ŋ] - ignoré
        // Consonne occlusive alvéolaire sourde [t]
        addNode(root, "predict4all.default.rule.phonem.confusion.t.tt.th", ruleBuilder().withConfusionSet("t", "tt", "th", "pt"));
        // Consonne occlusive alvéolaire sonore [d]
        addNode(root, "predict4all.default.rule.phonem.confusion.d.dd", ruleBuilder().withConfusionSet("dd", "d"));
        // Consonne occlusive bilabiale sourde [p]
        addNode(root, "predict4all.default.rule.phonem.confusion.b.p.pp", ruleBuilder().withConfusionSet("b", "p", "pp"));
        // Consonne occlusive bilabiale sonore [b]
        addNode(root, "predict4all.default.rule.phonem.confusion.b.bb", ruleBuilder().withConfusionSet("b", "bb"));
        // Consonne occlusive vélaire sourde [k]
        addNode(root, "predict4all.default.rule.phonem.confusion.cc.c.qu.k", ruleBuilder().withConfusionSet("c", "cc", "cu", "k", "q", "qu", "x"));
        // Consonne occlusive vélaire sonore [g]
        addNode(root, "predict4all.default.rule.phonem.confusion.c.g.gg", ruleBuilder().withConfusionSet("c", "g", "gg", "gu"));
        return root;
    }

    private static CorrectionRuleNode generateDoubleLetterConfusion(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.double.letter.name");
        Arrays.asList("b", "g", "k", "z").stream()//
                .forEach(c -> addNode(root, c + " > " + c + c,
                        ruleBuilder().withError(c).withReplacement(c + c).withMinIndexFromStart(1).withMaxIndexFromEnd(1).withCost(1.5)//
                ));
        Arrays.asList("c", "d", "f", "l", "m", "n", "p", "r", "s", "t").stream()//
                .forEach(c -> addNode(root, c + " <> " + c + c,
                        ruleBuilder().withError(c).withReplacement(c + c).withMinIndexFromStart(1).withMaxIndexFromEnd(1).withBidirectional(true)//
                ));
        return root;
    }

    private static CorrectionRuleNode generateAddLetterRule(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.missing.letter.name");
        for (String c : ALPHABET) {
            addNode(root, c, ruleBuilder().withError("").withReplacement(c).withCost(1.5).withMinIndexFromStart(1).withMaxIndexFromEnd(1));
        }
        return root;
    }

    private static CorrectionRuleNode generateRemoveLetterRule(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.added.letter.name");
        for (String c : ALPHABET) {
            addNode(root, c, ruleBuilder().withError(c).withReplacement("").withCost(1.5).withMinIndexFromStart(1).withMaxIndexFromEnd(1));
        }
        return root;
    }

    private static CorrectionRuleNode generateWordEndRules(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.word.ends.name");
        addNode(root, "predict4all.default.rule.word.ends.ai.e.sound",
                ruleBuilder().withConfusionSet("é", "és", "ées", "ée", "er", "ai", "ais", "ait", "aient").withMinIndexFromEnd(2));
        return root;
    }

    private static CorrectionRuleNode generateMFrontOfMBP(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.mbp.name");
        addNode(root, "predict4all.default.rule.mbp.m", ruleBuilder().withError("nm").withReplacement("mm").withCost(DEFAULT_ORTHO_RULE_M));
        addNode(root, "predict4all.default.rule.mbp.b", ruleBuilder().withError("nb").withReplacement("mb").withCost(DEFAULT_ORTHO_RULE_M));
        addNode(root, "predict4all.default.rule.mbp.p", ruleBuilder().withError("np").withReplacement("mp").withCost(DEFAULT_ORTHO_RULE_M));
        return root;
    }

    private static CorrectionRuleNode generateHomophone(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.homophone.name");

        List<List<String>> values = new ArrayList<>();
        values.add(Arrays.asList("c'est", "ces"));
        values.add(Arrays.asList("poids", "pois"));
        values.add(Arrays.asList("pin", "pain"));
        values.add(Arrays.asList("thym", "teint", "tain"));
        values.add(Arrays.asList("verso", "verseau"));
        values.add(Arrays.asList("temps", "taon"));
        values.add(Arrays.asList("seau", "saut", "sot"));
        values.add(Arrays.asList("port", "porc", "pore"));
        values.add(Arrays.asList("tente", "tante"));
        values.add(Arrays.asList("encre", "ancre"));
        values.add(Arrays.asList("vert", "vers", "verre"));
        values.add(Arrays.asList("air", "aire", "ère"));
        values.add(Arrays.asList("balle", "bal"));
        values.add(Arrays.asList("chant", "champ"));
        values.add(Arrays.asList("phare", "fard", "far"));
        values.add(Arrays.asList("fil", "file"));
        values.add(Arrays.asList("cane", "canne"));
        values.add(Arrays.asList("python", "piton"));
        values.add(Arrays.asList("balade", "ballade"));
        values.add(Arrays.asList("ballet", "balai"));
        values.add(Arrays.asList("pause", "pose"));
        values.add(Arrays.asList("amande", "amende"));
        values.add(Arrays.asList("mer", "maire", "mère"));
        values.add(Arrays.asList("choeur", "coeur"));
        values.add(Arrays.asList("signe", "cygne"));
        values.add(Arrays.asList("point", "poings"));
        values.add(Arrays.asList("poignée", "poignet ?"));
        values.add(Arrays.asList("Saule", "sole", "sol"));
        values.add(Arrays.asList("compte", "comte", "conte"));
        values.add(Arrays.asList("canot", "cannaux"));
        values.add(Arrays.asList("bout", "boue"));
        values.add(Arrays.asList("sel", "selle"));
        values.add(Arrays.asList("plaine", "pleine"));
        values.add(Arrays.asList("thé", "taie"));
        values.add(Arrays.asList("sur", "sûre", "sûr", "penser", "panser", "pensée"));
        values.add(Arrays.asList("Épée", "épais"));
        values.add(Arrays.asList("hoquet", "hockey"));
        values.add(Arrays.asList("car", "quart"));
        values.add(Arrays.asList("datte", "date"));
        values.add(Arrays.asList("patte", "pâte"));
        values.add(Arrays.asList("mètre", "maître", "mettre"));
        values.add(Arrays.asList("maux", "mots"));
        values.add(Arrays.asList("filtre", "philtre"));
        values.add(Arrays.asList("hauteur", "auteur"));
        values.add(Arrays.asList("sale", "salle"));
        values.add(Arrays.asList("plainte", "plinthe"));
        values.add(Arrays.asList("but", "butte"));
        values.add(Arrays.asList("coup", "cou", "coût"));
        values.add(Arrays.asList("voix", "voie"));
        values.add(Arrays.asList("raisonne", "résonne"));
        values.add(Arrays.asList("délasser", "délacer"));
        values.add(Arrays.asList("marées", "marais"));
        values.add(Arrays.asList("repère", "repaire"));
        values.add(Arrays.asList("vaine", "veine"));
        values.add(Arrays.asList("laid", "laie", "lait", "les"));
        values.add(Arrays.asList("banc", "ban"));
        values.add(Arrays.asList("chaîne", "chêne"));
        values.add(Arrays.asList("cours", "courre", "court"));
        values.add(Arrays.asList("lisse", "lys"));
        values.add(Arrays.asList("box", "boxe"));
        values.add(Arrays.asList("goutte", "goûte"));
        values.add(Arrays.asList("haute", "hotte", "hôte"));
        values.add(Arrays.asList("serre", "cerf"));
        values.add(Arrays.asList("tard", "tare"));
        values.add(Arrays.asList("pieu", "pieux"));
        values.add(Arrays.asList("pot", "peau"));
        values.add(Arrays.asList("plan", "plant"));
        values.add(Arrays.asList("reine", "renne"));
        values.add(Arrays.asList("cap", "cape"));
        values.add(Arrays.asList("fois", "foie"));
        values.add(Arrays.asList("toit", "toi"));
        values.add(Arrays.asList("gaze", "gaz"));
        values.add(Arrays.asList("danse", "dense"));
        values.add(Arrays.asList("phoque", "foc"));
        values.add(Arrays.asList("geai", "jet"));
        values.add(Arrays.asList("grasse", "grâce"));
        values.add(Arrays.asList("guet", "gai"));
        values.add(Arrays.asList("flan", "flanc"));
        values.add(Arrays.asList("maie", "mai", "mais"));
        values.add(Arrays.asList("chat", "chas"));
        values.add(Arrays.asList("arrhes", "art"));
        values.add(Arrays.asList("poil", "poêle"));
        values.add(Arrays.asList("archer", "archet"));
        values.add(Arrays.asList("cher", "chaire", "chaire"));
        values.add(Arrays.asList("taux", "tôt"));

        values.forEach(set -> {
            CorrectionRuleNode leaf = generateLeaf("predict4all.default.homophone.of", set.get(0));
            leaf.setCorrectionRule(ruleBuilder().withConfusionSet(set.toArray(new String[0])).withCost(DEFAULT_HOMOPHONE_COST));
            root.addChild(leaf);
        });
        return root;
    }

    private static CorrectionRuleNode generateVisualConfusions(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.visual.confusion.name");
        addNode(root, "predict4all.default.rule.visual.confusion.of.value", ruleBuilder().withConfusionSet("b", "d"), "b <> d");
        addNode(root, "predict4all.default.rule.visual.confusion.of.value", ruleBuilder().withConfusionSet("p", "q"), "p <> q");
        addNode(root, "predict4all.default.rule.visual.confusion.of.value", ruleBuilder().withConfusionSet("m", "n"), "m <> n");
        addNode(root, "predict4all.default.rule.visual.confusion.of.value", ruleBuilder().withConfusionSet("f", "t"), "f <> t");
        addNode(root, "predict4all.default.rule.visual.confusion.of.value", ruleBuilder().withConfusionSet("an", "au"), "an <> au");
        addNode(root, "predict4all.default.rule.visual.confusion.of.value", ruleBuilder().withConfusionSet("ou", "on"), "ou <> on");
        return root;
    }

    private static CorrectionRuleNode generateGEGUSound(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.ge.gu.sound.name");
        addNode(root, "ge > gue", ruleBuilder().withError("ge").withReplacement("gue"));
        addNode(root, "gi > gui", ruleBuilder().withError("gi").withReplacement("gui"));
        addNode(root, "ga > gea", ruleBuilder().withError("ga").withReplacement("gea"));
        addNode(root, "go > geo", ruleBuilder().withError("go").withReplacement("geo"));
        return root;
    }

    private static CorrectionRuleNode generateHearingConfusions(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.hearing.confusion.name");
        addNode(root, "predict4all.default.rule.hearing.confusion.of.value", ruleBuilder().withConfusionSet("p", "b"), "p <> b");
        addNode(root, "predict4all.default.rule.hearing.confusion.of.value", ruleBuilder().withConfusionSet("t", "d"), "t <> d");
        addNode(root, "predict4all.default.rule.hearing.confusion.of.value",
                ruleBuilder().withError("q", "qu", "c", "k").withReplacement("g", "gu", "gg").withBidirectional(true), "c <> g");
        addNode(root, "predict4all.default.rule.hearing.confusion.of.value",
                ruleBuilder().withError("ph", "f").withReplacement("v").withBidirectional(true), "f <> v");
        addNode(root, "predict4all.default.rule.hearing.confusion.of.value",
                ruleBuilder().withError("ch").withReplacement("j", "g").withBidirectional(true), "ch <> j");
        return root;
    }

    private static CorrectionRuleNode generateSequences(PredictionParameter predictionParamater) {
        CorrectionRuleNode root = generateNode("predict4all.default.rule.char.sequences.name");
        //Auto generate combination
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bla").withReplacement("bal"), "bla <> bal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bna").withReplacement("ban"), "bna <> ban");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bra").withReplacement("bar"), "bra <> bar");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bsa").withReplacement("bas"), "bsa <> bas");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bta").withReplacement("bat"), "bta <> bat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("ble").withReplacement("bel"), "ble <> bel");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bre").withReplacement("ber"), "bre <> ber");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bli").withReplacement("bil"), "bli <> bil");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bti").withReplacement("bit"), "bti <> bit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bno").withReplacement("bon"), "bno <> bon");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("bro").withReplacement("bor"), "bro <> bor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cla").withReplacement("cal"), "cla <> cal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cma").withReplacement("cam"), "cma <> cam");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cna").withReplacement("can"), "cna <> can");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cpa").withReplacement("cap"), "cpa <> cap");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cra").withReplacement("car"), "cra <> car");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("csa").withReplacement("cas"), "csa <> cas");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cta").withReplacement("cat"), "cta <> cat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cle").withReplacement("cel"), "cle <> cel");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cne").withReplacement("cen"), "cne <> cen");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cre").withReplacement("cer"), "cre <> cer");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cse").withReplacement("ces"), "cse <> ces");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cni").withReplacement("cin"), "cni <> cin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("csi").withReplacement("cis"), "csi <> cis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cti").withReplacement("cit"), "cti <> cit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("clo").withReplacement("col"), "clo <> col");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cmo").withReplacement("com"), "cmo <> com");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cno").withReplacement("con"), "cno <> con");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cpo").withReplacement("cop"), "cpo <> cop");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cro").withReplacement("cor"), "cro <> cor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("clu").withReplacement("cul"), "clu <> cul");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("cru").withReplacement("cur"), "cru <> cur");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dna").withReplacement("dan"), "dna <> dan");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dme").withReplacement("dem"), "dme <> dem");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dne").withReplacement("den"), "dne <> den");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dre").withReplacement("der"), "dre <> der");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dse").withReplacement("des"), "dse <> des");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dci").withReplacement("dic"), "dci <> dic");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dni").withReplacement("din"), "dni <> din");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dsi").withReplacement("dis"), "dsi <> dis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dti").withReplacement("dit"), "dti <> dit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dno").withReplacement("don"), "dno <> don");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("dro").withReplacement("dor"), "dro <> dor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("fre").withReplacement("fer"), "fre <> fer");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("fci").withReplacement("fic"), "fci <> fic");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("fli").withReplacement("fil"), "fli <> fil");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("fni").withReplacement("fin"), "fni <> fin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("fno").withReplacement("fon"), "fno <> fon");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("fro").withReplacement("for"), "fro <> for");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("fsu").withReplacement("fus"), "fsu <> fus");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gla").withReplacement("gal"), "gla <> gal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gna").withReplacement("gan"), "gna <> gan");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gra").withReplacement("gar"), "gra <> gar");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gne").withReplacement("gen"), "gne <> gen");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gre").withReplacement("ger"), "gre <> ger");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gse").withReplacement("ges"), "gse <> ges");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gsi").withReplacement("gis"), "gsi <> gis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("gno").withReplacement("gon"), "gno <> gon");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hba").withReplacement("hab"), "hba <> hab");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hla").withReplacement("hal"), "hla <> hal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hma").withReplacement("ham"), "hma <> ham");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hna").withReplacement("han"), "hna <> han");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hra").withReplacement("har"), "hra <> har");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hne").withReplacement("hen"), "hne <> hen");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hre").withReplacement("her"), "hre <> her");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hse").withReplacement("hes"), "hse <> hes");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hli").withReplacement("hil"), "hli <> hil");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hsi").withReplacement("his"), "hsi <> his");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hno").withReplacement("hon"), "hno <> hon");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("hro").withReplacement("hor"), "hro <> hor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lba").withReplacement("lab"), "lba <> lab");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lca").withReplacement("lac"), "lca <> lac");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lga").withReplacement("lag"), "lga <> lag");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lma").withReplacement("lam"), "lma <> lam");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lna").withReplacement("lan"), "lna <> lan");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lra").withReplacement("lar"), "lra <> lar");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lsa").withReplacement("las"), "lsa <> las");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lta").withReplacement("lat"), "lta <> lat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lce").withReplacement("lec"), "lce <> lec");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lme").withReplacement("lem"), "lme <> lem");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lne").withReplacement("len"), "lne <> len");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lre").withReplacement("ler"), "lre <> ler");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lse").withReplacement("les"), "lse <> les");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lte").withReplacement("let"), "lte <> let");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lbi").withReplacement("lib"), "lbi <> lib");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lci").withReplacement("lic"), "lci <> lic");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lgi").withReplacement("lig"), "lgi <> lig");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lmi").withReplacement("lim"), "lmi <> lim");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lni").withReplacement("lin"), "lni <> lin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lqi").withReplacement("liq"), "lqi <> liq");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lsi").withReplacement("lis"), "lsi <> lis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lti").withReplacement("lit"), "lti <> lit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lgo").withReplacement("log"), "lgo <> log");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lno").withReplacement("lon"), "lno <> lon");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lro").withReplacement("lor"), "lro <> lor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("lto").withReplacement("lot"), "lto <> lot");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mga").withReplacement("mag"), "mga <> mag");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mla").withReplacement("mal"), "mla <> mal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mna").withReplacement("man"), "mna <> man");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mra").withReplacement("mar"), "mra <> mar");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("msa").withReplacement("mas"), "msa <> mas");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mta").withReplacement("mat"), "mta <> mat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mne").withReplacement("men"), "mne <> men");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mre").withReplacement("mer"), "mre <> mer");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mse").withReplacement("mes"), "mse <> mes");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mte").withReplacement("met"), "mte <> met");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mli").withReplacement("mil"), "mli <> mil");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mni").withReplacement("min"), "mni <> min");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("msi").withReplacement("mis"), "msi <> mis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mti").withReplacement("mit"), "mti <> mit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mdo").withReplacement("mod"), "mdo <> mod");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mno").withReplacement("mon"), "mno <> mon");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mro").withReplacement("mor"), "mro <> mor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("mlu").withReplacement("mul"), "mlu <> mul");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nga").withReplacement("nag"), "nga <> nag");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nla").withReplacement("nal"), "nla <> nal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nna").withReplacement("nan"), "nna <> nan");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nra").withReplacement("nar"), "nra <> nar");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nta").withReplacement("nat"), "nta <> nat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nle").withReplacement("nel"), "nle <> nel");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nme").withReplacement("nem"), "nme <> nem");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nne").withReplacement("nen"), "nne <> nen");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nre").withReplacement("ner"), "nre <> ner");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nse").withReplacement("nes"), "nse <> nes");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nte").withReplacement("net"), "nte <> net");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nci").withReplacement("nic"), "nci <> nic");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nqi").withReplacement("niq"), "nqi <> niq");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nsi").withReplacement("nis"), "nsi <> nis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nti").withReplacement("nit"), "nti <> nit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nmo").withReplacement("nom"), "nmo <> nom");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nno").withReplacement("non"), "nno <> non");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("nro").withReplacement("nor"), "nro <> nor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pla").withReplacement("pal"), "pla <> pal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pna").withReplacement("pan"), "pna <> pan");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pra").withReplacement("par"), "pra <> par");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("psa").withReplacement("pas"), "psa <> pas");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pta").withReplacement("pat"), "pta <> pat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("ple").withReplacement("pel"), "ple <> pel");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pne").withReplacement("pen"), "pne <> pen");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pre").withReplacement("per"), "pre <> per");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pse").withReplacement("pes"), "pse <> pes");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pli").withReplacement("pil"), "pli <> pil");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pni").withReplacement("pin"), "pni <> pin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pri").withReplacement("pir"), "pri <> pir");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pti").withReplacement("pit"), "pti <> pit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("plo").withReplacement("pol"), "plo <> pol");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pno").withReplacement("pon"), "pno <> pon");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pro").withReplacement("por"), "pro <> por");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("pso").withReplacement("pos"), "pso <> pos");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("plu").withReplacement("pul"), "plu <> pul");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rba").withReplacement("rab"), "rba <> rab");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rca").withReplacement("rac"), "rca <> rac");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rda").withReplacement("rad"), "rda <> rad");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rga").withReplacement("rag"), "rga <> rag");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rla").withReplacement("ral"), "rla <> ral");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rma").withReplacement("ram"), "rma <> ram");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rna").withReplacement("ran"), "rna <> ran");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rpa").withReplacement("rap"), "rpa <> rap");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rsa").withReplacement("ras"), "rsa <> ras");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rta").withReplacement("rat"), "rta <> rat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rva").withReplacement("rav"), "rva <> rav");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rce").withReplacement("rec"), "rce <> rec");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rde").withReplacement("red"), "rde <> red");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rfe").withReplacement("ref"), "rfe <> ref");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rge").withReplacement("reg"), "rge <> reg");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rle").withReplacement("rel"), "rle <> rel");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rme").withReplacement("rem"), "rme <> rem");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rne").withReplacement("ren"), "rne <> ren");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rpe").withReplacement("rep"), "rpe <> rep");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rre").withReplacement("rer"), "rre <> rer");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rse").withReplacement("res"), "rse <> res");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rte").withReplacement("ret"), "rte <> ret");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rve").withReplacement("rev"), "rve <> rev");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rze").withReplacement("rez"), "rze <> rez");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rci").withReplacement("ric"), "rci <> ric");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rfi").withReplacement("rif"), "rfi <> rif");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rgi").withReplacement("rig"), "rgi <> rig");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rli").withReplacement("ril"), "rli <> ril");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rmi").withReplacement("rim"), "rmi <> rim");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rni").withReplacement("rin"), "rni <> rin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rpi").withReplacement("rip"), "rpi <> rip");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rqi").withReplacement("riq"), "rqi <> riq");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rri").withReplacement("rir"), "rri <> rir");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rsi").withReplacement("ris"), "rsi <> ris");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rti").withReplacement("rit"), "rti <> rit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rvi").withReplacement("riv"), "rvi <> riv");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rco").withReplacement("roc"), "rco <> roc");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rdo").withReplacement("rod"), "rdo <> rod");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rgo").withReplacement("rog"), "rgo <> rog");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rlo").withReplacement("rol"), "rlo <> rol");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rmo").withReplacement("rom"), "rmo <> rom");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rno").withReplacement("ron"), "rno <> ron");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rpo").withReplacement("rop"), "rpo <> rop");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rso").withReplacement("ros"), "rso <> ros");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rto").withReplacement("rot"), "rto <> rot");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("rsu").withReplacement("rus"), "rsu <> rus");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sba").withReplacement("sab"), "sba <> sab");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sca").withReplacement("sac"), "sca <> sac");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sla").withReplacement("sal"), "sla <> sal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sna").withReplacement("san"), "sna <> san");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sra").withReplacement("sar"), "sra <> sar");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sta").withReplacement("sat"), "sta <> sat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sme").withReplacement("sem"), "sme <> sem");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sne").withReplacement("sen"), "sne <> sen");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sre").withReplacement("ser"), "sre <> ser");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sse").withReplacement("ses"), "sse <> ses");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sze").withReplacement("sez"), "sze <> sez");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sdi").withReplacement("sid"), "sdi <> sid");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sgi").withReplacement("sig"), "sgi <> sig");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sni").withReplacement("sin"), "sni <> sin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("ssi").withReplacement("sis"), "ssi <> sis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sti").withReplacement("sit"), "sti <> sit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("slo").withReplacement("sol"), "slo <> sol");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sno").withReplacement("son"), "sno <> son");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sro").withReplacement("sor"), "sro <> sor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sbu").withReplacement("sub"), "sbu <> sub");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("spu").withReplacement("sup"), "spu <> sup");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("sru").withReplacement("sur"), "sru <> sur");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tba").withReplacement("tab"), "tba <> tab");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tca").withReplacement("tac"), "tca <> tac");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tga").withReplacement("tag"), "tga <> tag");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tla").withReplacement("tal"), "tla <> tal");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tna").withReplacement("tan"), "tna <> tan");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tra").withReplacement("tar"), "tra <> tar");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tta").withReplacement("tat"), "tta <> tat");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tle").withReplacement("tel"), "tle <> tel");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tme").withReplacement("tem"), "tme <> tem");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tne").withReplacement("ten"), "tne <> ten");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tre").withReplacement("ter"), "tre <> ter");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tse").withReplacement("tes"), "tse <> tes");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tze").withReplacement("tez"), "tze <> tez");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tci").withReplacement("tic"), "tci <> tic");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tfi").withReplacement("tif"), "tfi <> tif");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tli").withReplacement("til"), "tli <> til");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tmi").withReplacement("tim"), "tmi <> tim");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tni").withReplacement("tin"), "tni <> tin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tqi").withReplacement("tiq"), "tqi <> tiq");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tri").withReplacement("tir"), "tri <> tir");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tsi").withReplacement("tis"), "tsi <> tis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tti").withReplacement("tit"), "tti <> tit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tvi").withReplacement("tiv"), "tvi <> tiv");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tmo").withReplacement("tom"), "tmo <> tom");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tno").withReplacement("ton"), "tno <> ton");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tro").withReplacement("tor"), "tro <> tor");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("tru").withReplacement("tur"), "tru <> tur");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vla").withReplacement("val"), "vla <> val");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vna").withReplacement("van"), "vna <> van");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vle").withReplacement("vel"), "vle <> vel");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vne").withReplacement("ven"), "vne <> ven");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vre").withReplacement("ver"), "vre <> ver");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vse").withReplacement("ves"), "vse <> ves");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vli").withReplacement("vil"), "vli <> vil");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vni").withReplacement("vin"), "vni <> vin");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vsi").withReplacement("vis"), "vsi <> vis");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vti").withReplacement("vit"), "vti <> vit");
        addNode(root, "predict4all.default.rule.sequence.of.value",
                ruleBuilder().withCost(DEFAULT_SEQUENCE_COST).withError("vlo").withReplacement("vol"), "vlo <> vol");
        //Auto-generated end
        return root;
    }

    //========================================================================

    // UTILS
    //========================================================================
    private static void addNode(CorrectionRuleNode parent, String nameId, CorrectionRule correctionBuilder, Object... args) {
        CorrectionRuleNode leaf = generateLeaf(nameId, args);
        leaf.setCorrectionRule(correctionBuilder);
        parent.addChild(leaf);
    }

    private static CorrectionRuleNode generateNode(String nameId, Object... args) {
        return generateNodeOrLeaf(CorrectionRuleNodeType.NODE, nameId, args);
    }

    private static CorrectionRuleNode generateLeaf(String nameId, Object... args) {
        return generateNodeOrLeaf(CorrectionRuleNodeType.LEAF, nameId, args);
    }

    private static CorrectionRuleNode generateNodeOrLeaf(CorrectionRuleNodeType type, String nameId, Object... args) {
        CorrectionRuleNode root = new CorrectionRuleNode(type);
        root.setName(translationProvider.translate(nameId, args));
        return root;
    }
    //========================================================================

    // TRANSLATION
    //========================================================================
    private static TranslationProvider translationProvider = (id, args) -> id;

    public static void setTranslationProvider(TranslationProvider provider) {
        translationProvider = provider;
    }

    @FunctionalInterface
    public static interface TranslationProvider {
        public String translate(String id, Object... args);
    }
    //========================================================================
}
