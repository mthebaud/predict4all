<p align="center">
  <img src="https://github.com/mthebaud/predict4all/raw/master/res/logos/predict4all.png">
</p>
<p align="center">
  <img src="https://github.com/mthebaud/predict4all/raw/master/res/logos/partners.png">
</p>

 PREDICT4ALL
======

[![mvn](https://img.shields.io/maven-central/v/io.github.mthebaud/predict4all)](http://mvnrepository.com/artifact/io.github.mthebaud/predict4all)
[![javadoc](https://javadoc.io/badge2/io.github.mthebaud/predict4all/javadoc.svg)](https://javadoc.io/doc/io.github.mthebaud/predict4all)
[![licence](https://img.shields.io/github/license/mthebaud/predict4all)](https://github.com/mthebaud/predict4all/blob/master/LICENCE)
[![Build Status](https://travis-ci.com/mthebaud/predict4all.svg?branch=master)](https://travis-ci.com/mthebaud/predict4all)

<p align="center">
  <img src="https://github.com/mthebaud/predict4all/raw/master/res/logos/predict4all_demo_gif.gif">
</p>

Predict4All is an **accurate, fast, lightweight, multilingual, free and open-source next word prediction library**.

It aims to be integrated in applications to display possible next words and help user input : virtual keyboards, text editors, AAC systems...

## Key features

- Next word prediction
- Current word completion
- Live accurate and customizable word correction while typing
- Dynamic models : automatically learn new words and sentence to integrate user's language and style
- Lightweight prediction and training : low dependency and fully integrated algorithms
- Easy integration : load precomputed models and start predicting !
- Low memory foot print : dynamically loaded language models allow memory saves - it only uses 25 MB of heap space ! 

**Predict4All originality stands in its correction model** : it works thanks to a set of correction rules (general or specific : accents, grammar, missing space, etc).
This correction model allows the correction to happen earlier in prediction compared to string distance techniques.
This also allows the correction to be similar to existing corrector (e.g. GBoard) but to be enhanced with custom rule based on user errors (dysorthography, dyslexia, etc)

**Predict4All was co-designed with speech therapists and occupational therapists from [CMRRF Kerpape](http://www.kerpape.mutualite56.fr/fr) and [Hopital Raymond Poincaré](http://raymondpoincare.aphp.fr/)
to ensure that it match needs and requirements for user with speech and text writing troubles.**
A particular attention was given to determine common and particular mistakes made by people with dysorthography and dyslexia.

Currently, Predict4All supports french language (provided rules and pre-trained language model).

## Project

This library was developed in the collaborative project Predict4All involving [CMRRF Kerpape](http://www.kerpape.mutualite56.fr/fr), [Hopital Raymond Poincaré](http://raymondpoincare.aphp.fr/) and [BdTln Team, LIFAT, Université de Tours](https://lifat.univ-tours.fr/teams/bdtin/)

Predict4All is supported by [Fondation Paul Bennetot](https://www.fondationpaulbennetot.org/), Fondation du Groupe Matmut under Fondation de l'Avenir, Paris, France (*project AP-FPB 16-001*)

This project has been integrated in the following AAC software : LifeCompanion, Sibylle, CiviKey

## Usage

### Installation

Train (see "Training your own language model") or download a language model. Pre-computed french language model is available :

- **[fr_ngrams.bin](https://drive.google.com/file/d/1OHdlfmOPHl0SHti4GZwWFVjHCQ_fmwJm/view?usp=sharing)** : contains pre trained word sequences
- **[fr_words.bin](https://drive.google.com/file/d/1HtluRHDp0xU-weJOClg-bRkm3gzS_CPC/view?usp=sharing)** : contains the associated vocabulary

The french language model have been trained on more than +20 millions words from Wikipedia and subtitles corpus. The vocabulary contains ~112 000 unique words.

Get the library through your favorite dependency manager :

**Maven**
```xml
<dependency>
    <groupId>io.github.mthebaud</groupId>
    <artifactId>predict4all</artifactId>
    <version>1.1.0</version>
</dependency>
```

**Gradle**
```
implementation 'io.github.mthebaud:predict4all:1.1.0'
```

**In the following examples, we assume that you initialized language model, predictor, etc.**

```java
final File FILE_NGRAMS = new File("fr_ngrams.bin");
final File FILE_WORDS = new File("fr_words.bin");

LanguageModel languageModel = new FrenchLanguageModel();
PredictionParameter predictionParameter = new PredictionParameter(languageModel);

WordDictionary dictionary = WordDictionary.loadDictionary(languageModel, FILE_WORDS);
try (StaticNGramTrieDictionary ngramDictionary = StaticNGramTrieDictionary.open(FILE_NGRAMS)) {
    WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary);
    // EXAMPLE CODE SHOULD RUN HERE
}
```

You can find complete working code for these examples and more complex ones in [predict4all-example](https://github.com/mthebaud/predict4all/blob/master/predict4all-example/src/main/java/org/predict4all/example/)

Please read the [Javadoc](https://javadoc.io/doc/io.github.mthebaud/predict4all) (public classes are well documented)

### Predict next words

```java
WordPredictionResult predictionResult = wordPredictor.predict("j'aime manger des ");
for (WordPrediction prediction : predictionResult.getPredictions()) {
    System.out.println(prediction);
}
```

Result (french language model)
```
trucs = 0.16105959785338766 (insert = trucs, remove = 0, space = true)
fruits = 0.16093509126844632 (insert = fruits, remove = 0, space = true)
bonbons = 0.11072838908013616 (insert = bonbons, remove = 0, space = true)
gâteaux = 0.1107102433239866 (insert = gâteaux, remove = 0, space = true)
frites = 0.1107077522148962 (insert = frites, remove = 0, space = true)
```

### Predict word endings

```java
WordPredictionResult predictionResult = wordPredictor.predict("je te r");
for (WordPrediction prediction : predictionResult.getPredictions()) {
    System.out.println(prediction);
}
```

Result (french language model)
```
rappelle = 0.25714609184509885 (insert = appelle, remove = 0, space = true)
remercie = 0.12539880967030353 (insert = emercie, remove = 0, space = true)
ramène = 0.09357117922321868 (insert = amène, remove = 0, space = true)
retrouve = 0.07317575867400958 (insert = etrouve, remove = 0, space = true)
rejoins = 0.06404375655722373 (insert = ejoins, remove = 0, space = true)
```

To tune the WordPredictor, you can explore [PredictionParameter](https://javadoc.io/doc/io.github.mthebaud/predict4all/latest/org/predict4all/nlp/prediction/PredictionParameter.html) javadoc

### Using correction rules

```java
CorrectionRuleNode root = new CorrectionRuleNode(CorrectionRuleNodeType.NODE);
root.addChild(FrenchDefaultCorrectionRuleGenerator.CorrectionRuleType.ACCENTS.generateNodeFor(predictionParameter));
predictionParameter.setCorrectionRulesRoot(root);
predictionParameter.setEnableWordCorrection(true);

WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary);
WordPredictionResult predictionResult = wordPredictor.predict("il eta");
for (WordPrediction prediction : predictionResult.getPredictions()) {
    System.out.println(prediction);
}
```

Result (french language model)
```
était = 0.9485814446960688 (insert = était, remove = 3, space = true)
établit = 0.05138460933797299 (insert = établit, remove = 3, space = true)
étale = 7.544080911878824E-6 (insert = étale, remove = 3, space = true)
établissait = 4.03283914323952E-6 (insert = établissait, remove = 3, space = true)
étaye = 4.025324786425216E-6 (insert = étaye, remove = 3, space = true)
```
In this example, remove become positive as the first letter in the word is incorrect : previous typed text should be removed before insert. 

### Using dynamic model

```java
DynamicNGramDictionary dynamicNGramDictionary = new DynamicNGramDictionary(4);
predictionParameter.setDynamicModelEnabled(true);
WordPredictor wordPredictor = new WordPredictor(predictionParameter, dictionary, ngramDictionary, dynamicNGramDictionary);
WordPredictionResult predictionResult = wordPredictor.predict("je vais à la ");
for (WordPrediction prediction : predictionResult.getPredictions()) {
    System.out.println(prediction);
}
wordPredictor.trainDynamicModel("je vais à la gare");
predictionResult = wordPredictor.predict("je vais à la ");
for (WordPrediction prediction : predictionResult.getPredictions()) {
    System.out.println(prediction);
}
```

Result (french language model)
```
fête = 0.3670450710570904 (insert = fête, remove = 0, space = true)
bibliothèque = 0.22412342109445696 (insert = bibliothèque, remove = 0, space = true)
salle = 0.22398910838330122 (insert = salle, remove = 0, space = true)
fin = 0.014600071765987328 (insert = fin, remove = 0, space = true)
suite = 0.014315510457449597 (insert = suite, remove = 0, space = true)
- After training
fête = 0.35000112941797795 (insert = fête, remove = 0, space = true)
bibliothèque = 0.2137161256141207 (insert = bibliothèque, remove = 0, space = true)
salle = 0.213588049788271 (insert = salle, remove = 0, space = true)
gare = 0.045754860284824 (insert = gare, remove = 0, space = true)
fin = 0.013922109328323544 (insert = fin, remove = 0, space = true)
```
In this example, the word "gare" appears after training the model with "je vais à la gare".

**Be careful**, training a model with wrong sentence will corrupt your data.

### Saving the dynamic model

When using a dynamic model, you should take care of saving/loading two different files : user ngrams and user word dictionary.

The original files won't be modified, to be shared across different users : the good implementation pattern.

Once your model is trained, you may want to save it :
```java
dynamicNGramDictionary.saveDictionary(new File("fr_user_ngrams.bin"));
```
and later load it again (and pass it to WordPredictor constructor)
```java
DynamicNGramDictionary dynamicNGramDictionary = DynamicNGramDictionary.load(new File("fr_user_ngrams.bin"));
```

You can also save the word dictionary if new words have been added :
```java
dictionary.saveUserDictionary(new File("fr_user_words.bin"));
```
and later load it again (on an existing WordDictionary instance)
```java
dictionary.loadUserDictionary(new File("fr_user_words.bin"));
```

### Modify vocabulary

It is sometimes useful to modify the available vocabulary to better adapt predictions to user.

This can be done working with WordDictionary, for example, you can disable a word :

```java
Word maisonWord = dictionary.getWord("maison");
maisonWord.setForceInvalid(true, true);
```

Or you can show to user every custom words added to the dictionary :
```java
dictionary.getAllWords().stream()
        .filter(w -> w.isValidToBePredicted(predictionParameter)) // don't want to display to the user the word that would never appears in prediction
        .filter(Word::isUserWord) // get only the user added words
        .forEach(w -> System.out.println(w.getWord()));
```

When you modify words (original or added by users), don't forget to save the user dictionary : it will save user words but also original words modifications.

You can find further information looking at [Word](https://javadoc.io/doc/io.github.mthebaud/predict4all/latest/org/predict4all/nlp/words/model/Word.html) javadoc

### Tech notes

When using Predict4All, you should take note that :

- The library is not designed to be thread safe : you should synchronize your calls to **WordPredictor**
- The library relies on disk reads : the ngram file is opened with a **[FileChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/FileChannel.html)** : this means that your ngram data file will be opened by the process as long as you're using the library
- Dynamic model files are dependent from the original data files : if the original data changes, you may get a **WordDictionaryMatchingException** when loading your previous user files 

## Training your own language model

To train your own language model, you will first need to prepare :

- The runtime environment for Predict4All (JRE 1.8+) with enough RAM (the more you get, the more you will be able to create big models)
- The training data : a directory containing .txt files encoded in UTF-8 (to improve computing performance, it's better to have multiple txt files than a single big txt file)
- Lexique : a base dictionary for the French Language that you should extract somewhere on your system ([download](http://www.lexique.org/databases/Lexique383/Lexique383.zip))
- A training configuration file : you can use **res/default/fr_default_training_configuration.json** - make sure to change **PATH_TO_LEXIQUE**

*A good CPU is also a key point : Predict4All strongly use multi threaded algorithms, so the more core you get, the faster the training will be*

Then, you can run the executable jar ([precompiled version available](https://drive.google.com/file/d/1wNB3eDdvkpI0386qX896IwkxPlHcza46/view?usp=sharing)) with a command line :

```
java -Xmx16G -jar predict4all-model-trainer-cmd-1.1.0-all.jar -config fr_training_configuration.json -language fr -ngram-dictionary fr_ngrams.bin -word-dictionary fr_words.bin path/to/corpus
```

This command will launch a training, allowing the JVM to get 16GB memory, and giving an input and output configuration.

Generated data files will be **fr_ngrams.bin** and **fr_words.bin**

Alternatively, you can check **LanguageDataModelTrainer** in **predict4all-model-trainer-cmd** to launch your training programmatically.

## Getting help

Please let us know if you use Predict4All !

Feel free to fill an [issue](https://github.com/mthebaud/predict4all/issues) if you need assistance or if you find a bug.

If you want, you can tag you question with "predict4all" on [StackOverflow](https://stackoverflow.com/questions/tagged/predict4all)

## Licence

This software is distributed under the **Apache License 2.0** (see file LICENCE) 

## References

This project was developed various NLP techniques (mainly ngram based)

### Directly related articles

- **Sibylle AAC system** : Tonio Wandmacher, Jean-Yves Antoine, Jean-Paul Departe, Franck Poirier. SIBYLLE, an assistive communication system adapting to the context and its user. *ACM Transactions on Accessible Computing* , ACM New York, NY, USA 2008, 1 (1), pp.1-30. [hal-01021174](https://hal.archives-ouvertes.fr/hal-01021174)
- **Correction needs (Dysorthography/Dyslexia users)** : Antoine J.Y., Crochetet M., Arbizu C., Lopez E., Pouplin S., Besnier A., Thebaud M. (2019) Ma copie adore le vélo: analyse des besoins réels en correction orthographique sur un corpus de dictées d’enfants. *TALN’2019*. [hal-02375246v1](https://hal.archives-ouvertes.fr/hal-02375246v1)

### Inspiring papers

- Techniques for automatically correcting words in text, Karen Kukich, *ACM Computing Surveys*, December 1992 [https://doi.org/10.1145/146370.146380](https://doi.org/10.1145/146370.146380)
- On structuring probabilistic dependences in stochastic language modelling, Hermann Ney, Ute Essen, Reinhard Kneser, *Computer Speech & Language*, Volume 8, Issue 1, 1994, Pages 1-38, ISSN 0885-2308, [https://doi.org/10.1006/csla.1994.1001](https://doi.org/10.1006/csla.1994.1001)
- *More to come...*