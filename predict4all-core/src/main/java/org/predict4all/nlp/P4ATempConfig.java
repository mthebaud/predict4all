package org.predict4all.nlp;

import java.io.File;

public class P4ATempConfig {
	public static final File ROOT = new File(System.getProperty("user.home") + "/dev/predict4all/");

	public static final File LANGUAGE_MODEL_ROOT = new File(ROOT.getPath() + "/data/language-model/current/");
	public static final File DATA_WORD_DICTIONARY = new File(LANGUAGE_MODEL_ROOT.getPath() + "/words.bin");
	public static final File DATA_USER_WORD_DICTIONARY = new File(LANGUAGE_MODEL_ROOT.getPath() + "/user-words.bin");
	public static final File DATA_NGRAMS = new File(LANGUAGE_MODEL_ROOT.getPath() + "/ngrams.bin");
	public static final File DATA_USER_NGRAMS = new File(LANGUAGE_MODEL_ROOT.getPath() + "/user_ngrams.bin");
	public static final File DATA_SEMANTIC = new File(LANGUAGE_MODEL_ROOT.getPath() + "/semantic.bin");

	// TRAINING
	private static final File TRAINING_ROOT = new File(ROOT.getPath() + "/data/corpus/training/");
	public static final File TRAINING_FINAL_1 = new File(TRAINING_ROOT.getPath() + "/prepared/mixed1/");
	public static final File TRAINING_CUSTOM_LEMONDE = new File(TRAINING_ROOT.getPath() + "/custom/custom1/");

	// SOURCES
	private static final File TRAINING_SOURCE_ROOT = new File(ROOT.getPath() + "/data/corpus/training/sources/");
	public static final File TRAINING_SOURCE_SERIES_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/sub-title-movies-series/result/");
	public static final File TRAINING_SOURCE_SERIES_DOWNLOAD = new File(TRAINING_SOURCE_ROOT.getPath() + "/sub-title-movies-series/download/");
	public static final File TRAINING_SOURCE_WIKI_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/wikipedia/");
	public static final File TRAINING_SOURCE_VIKI_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/Vikidia/");
	public static final File TRAINING_SOURCE_WIKISOURCE_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/wikisource/");
	public static final File TRAINING_SOURCE_EGRATUITS_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/ebooksgratuits/");
	public static final File TRAINING_SOURCE_VOUS_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/vousnousils/");
	public static final File TRAINING_SOURCE_LIVRESJ_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/Livres/");
	public static final File TRAINING_SOURCE_CARTABLE_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/Cartablefantastique/");
	public static final File TRAINING_SOURCE_JIMINI_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/jimini/");

	public static final File TRAINING_SOURCE_EBOOKIDS_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/ebookids/");
	public static final File TRAINING_SOURCE_ACTUJ_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/actuJ/");
	public static final File TRAINING_SOURCE_LUMNI_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/lumni/");
	public static final File TRAINING_SOURCE_TV5MONDE_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/TV5Monde/");
	public static final File TRAINING_SOURCE_LPC_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/lesPetitsCitoyens/");
	public static final File TRAINING_SOURCE_GULLI_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/gulli/");
	public static final File TRAINING_SOURCE_JDE_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/jde/");
	public static final File TRAINING_SOURCE_HISTOIRE_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/histoire/");
	public static final File TRAINING_SOURCE_TREMPLIN_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/tremplin/");
	public static final File TRAINING_SOURCE_JSE_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/jeunesseSE/");
	public static final File TRAINING_SOURCE_MAGIC_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/magicMaman/");
	public static final File TRAINING_SOURCE_GOUPILI_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/goupili/");
	public static final File TRAINING_SOURCE_CONTES_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/contesDeFees/");
	public static final File TRAINING_SOURCE_HISTOIRES_RESULT =  new File(TRAINING_SOURCE_ROOT.getPath() + "/histoireChaqueJour/");
	public static final File TRAINING_SOURCE_ISA_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/isa&amis/");
	public static final File TRAINING_SOURCE_MONDEADOS_RESULT = new File(TRAINING_SOURCE_ROOT.getPath() + "/leMondeDesAdos/");

	// TESTING
	public static final File RESULT_ROOT = new File(ROOT.getPath() + "/data/result/");

	// EVALUATION
	private static final File EVALUATION_ROOT = new File(ROOT.getPath() + "/data/corpus/evaluation/");
	public static final File EVALUATION_NEWS = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_news_lemonde_2022.txt");
	public static final File EVALUATION_NEWS_1 = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_news_lemonde.txt");
	public static final File EVALUATION_NEWS_2 = new File(EVALUATION_ROOT.getPath() + "/evaluation2/fr_news_lemonde.txt");
	public static final File EVALUATION_KIDS_ = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_kids_2022_.txt");
	public static final File EVALUATION_LIFESTYLE = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_lifestyle.txt");

	public static final File EVALUATION_TEENS_ = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_teens_2022_.txt");
	public static final File EVALUATION_REDDIT = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_reddit.txt");
	public static final File EVALUATION_ARTICLE = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_article.txt");
	public static final File EVALUATION_CONTE = new File(EVALUATION_ROOT.getPath() + "/evaluation/Les_Malheurs_de_Sophie.txt");
	public static final File EVALUATION_FABLE = new File(EVALUATION_ROOT.getPath() + "/evaluation/la_fontaine_fables_livre1.txt");

	public static final File EVALUATION_FAUTES = new File(EVALUATION_ROOT.getPath() + "/evaluation/fautes.txt");


	public static final File EVALUATION_NEWS_3 = new File(EVALUATION_ROOT.getPath() + "/evaluation3/fr_news_lemonde.txt");
	public static final File EVALUATION_LIT_1 = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_literature_germinal.txt");
	public static final File EVALUATION_LIT_2 = new File(EVALUATION_ROOT.getPath() + "/evaluation2/fr_literature_germinal.txt");
	public static final File EVALUATION_LIT_3 = new File(EVALUATION_ROOT.getPath() + "/evaluation3/fr_literature_germinal.txt");
	public static final File EVALUATION_SPEECH_1 = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_speech_otg.txt");
	public static final File EVALUATION_SPEECH_2 = new File(EVALUATION_ROOT.getPath() + "/evaluation2/fr_speech_otg.txt");
	public static final File EVALUATION_SPEECH_3 = new File(EVALUATION_ROOT.getPath() + "/evaluation3/fr_speech_otg.txt");
	public static final File EVALUATION_WIKI_1 = new File(EVALUATION_ROOT.getPath() + "/evaluation/fr_wiki.txt");
	public static final File EVALUATION_WIKI_2 = new File(EVALUATION_ROOT.getPath() + "/evaluation2/fr_wiki.txt");
	public static final File EVALUATION_ERROR_1 = new File(EVALUATION_ROOT.getPath() + "/errors/Marion_1_sans_prédiction_texte_1_J0.txt");
	public static final File EVALUATION_ERROR_2 = new File(EVALUATION_ROOT.getPath() + "/errors/Spagnulo_1_sans_prédiction_texte_1_J0.txt");
	public static final File EVALUATION_ERROR_CUSTOM_1 = new File(EVALUATION_ROOT.getPath() + "/errors/custom1.txt");
	public static final File EVALUATION_SEMANTIC_CUSTOM_1 = new File(EVALUATION_ROOT.getPath() + "/semantic/montagne_article.txt");
	public static final File EVALUATION_SEMANTIC_CUSTOM_2 = new File(EVALUATION_ROOT.getPath() + "/semantic/chat_ebook.txt");
	public static final File EVALUATION_SEMANTIC_CUSTOM_3 = new File(EVALUATION_ROOT.getPath() + "/semantic/sport_pilates_ebook.txt");
	public static final File EVALUATION_SEMANTIC_CUSTOM_4 = new File(EVALUATION_ROOT.getPath() + "/semantic/montagne_test_custom.txt");

	public static final File EVALUATION_NEWS_ADULTS= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_adults.txt");
	public static final File EVALUATION_NEWS_ADULTS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_adults_1.txt");
	public static final File EVALUATION_NEWS_ADULTS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_adults_2.txt");

	public static final File EVALUATION_NEWS_ADOS= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_ados.txt");
	public static final File EVALUATION_NEWS_ADOS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_ados_1.txt");

	public static final File EVALUATION_NEWS_ADOS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_ados_2.txt");

	public static final File EVALUATION_NEWS_KIDS= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_kids.txt");
	public static final File EVALUATION_NEWS_KIDS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_kids_1.txt");
	public static final File EVALUATION_NEWS_KIDS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/news_kids_2.txt");

	public static final File EVALUATION_COM_ADULTS= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_adults.txt");
	public static final File EVALUATION_COM_ADOS= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_ados.txt");
	public static final File EVALUATION_COM_KIDS= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_kids.txt");
	public static final File EVALUATION_HISTOIRE_ADULTS= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_adults.txt");
	public static final File EVALUATION_HISTOIRE_ADOS= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_ados.txt");
	public static final File EVALUATION_HISTOIRE_KIDS= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_kids.txt");
	public static final File EVALUATION_ROMANS_ADULTS= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_adults.txt");
	public static final File EVALUATION_ROMANS_ADOS= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_ados.txt");
	public static final File EVALUATION_ROMANS_KIDS= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_kids.txt");


	public static final File EVALUATION_COM_ADULTS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_adults_1.txt");
	public static final File EVALUATION_COM_ADOS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_ados_1.txt");
	public static final File EVALUATION_COM_KIDS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_kids_1.txt");
	public static final File EVALUATION_HISTOIRE_ADULTS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_adults_1.txt");
	public static final File EVALUATION_HISTOIRE_ADOS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_ados_1.txt");
	public static final File EVALUATION_HISTOIRE_KIDS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_kids_1.txt");
	public static final File EVALUATION_ROMANS_ADULTS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_adults_1.txt");
	public static final File EVALUATION_ROMANS_ADOS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_ados_1.txt");
	public static final File EVALUATION_ROMANS_KIDS1= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_kids_1.txt");
	public static final File EVALUATION_COM_ADULTS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_adults_2.txt");
	public static final File EVALUATION_COM_ADOS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_ados_2.txt");
	public static final File EVALUATION_COM_KIDS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/comments_kids_2.txt");
	public static final File EVALUATION_HISTOIRE_ADULTS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_adults_2.txt");
	public static final File EVALUATION_HISTOIRE_ADOS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_ados_2.txt");
	public static final File EVALUATION_HISTOIRE_KIDS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/histoires_kids_2.txt");
	public static final File EVALUATION_ROMANS_ADULTS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_adults_2.txt");
	public static final File EVALUATION_ROMANS_ADOS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_ados_2.txt");
	public static final File EVALUATION_ROMANS_KIDS2= new File(EVALUATION_ROOT.getPath() + "/evaluation/romans_kids_2.txt");
	public static final File EVALUATION_PAW = new File(EVALUATION_ROOT.getPath() + "/evaluation/Paw_Patrol.txt");

	public static final File EVALUATION_DISCUSSION_PH6 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo6.txt");
	public static final File EVALUATION_DISCUSSION_PH10 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo10.txt");
	public static final File EVALUATION_DISCUSSION_PH12 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo12.txt");
	public static final File EVALUATION_DISCUSSION_PH13 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo13.txt");
	public static final File EVALUATION_ECRIT_TEEN = new File(EVALUATION_ROOT.getPath() + "/evaluation/teens.txt");
	public static final File EVALUATION_ECRIT_HFS = new File(EVALUATION_ROOT.getPath() + "/evaluation/le_rubis.txt");
	public static final File EVALUATION_ECRIT_CPM = new File(EVALUATION_ROOT.getPath() + "/evaluation/c_est_pas_moi.txt");

	public static final File EVALUATION_DISCUSSION_PH61 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo6_1.txt");
	public static final File EVALUATION_DISCUSSION_PH101 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo10_1.txt");
	public static final File EVALUATION_DISCUSSION_PH121 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo12_1.txt");
	public static final File EVALUATION_DISCUSSION_PH131 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo13_1.txt");
	public static final File EVALUATION_ECRIT_TEEN1 = new File(EVALUATION_ROOT.getPath() + "/evaluation/teens_1.txt");
	public static final File EVALUATION_ECRIT_HFS1 = new File(EVALUATION_ROOT.getPath() + "/evaluation/le_rubis_1.txt");
	public static final File EVALUATION_ECRIT_CPM1 = new File(EVALUATION_ROOT.getPath() + "/evaluation/c_est_pas_moi_1.txt");
	public static final File EVALUATION_DISCUSSION_PH62 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo6_2.txt");
	public static final File EVALUATION_DISCUSSION_PH102 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo10_2.txt");
	public static final File EVALUATION_DISCUSSION_PH122 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo12_2.txt");
	public static final File EVALUATION_DISCUSSION_PH132 = new File(EVALUATION_ROOT.getPath() + "/evaluation/philo13_2.txt");
	public static final File EVALUATION_ECRIT_TEEN2 = new File(EVALUATION_ROOT.getPath() + "/evaluation/teens_2.txt");
	public static final File EVALUATION_ECRIT_HFS2 = new File(EVALUATION_ROOT.getPath() + "/evaluation/le_rubis_2.txt");
	public static final File EVALUATION_ECRIT_CPM2 = new File(EVALUATION_ROOT.getPath() + "/evaluation/c_est_pas_moi_2.txt");
	// TEMP : WICOPACO
	public static final File TEST_WICAOPACO_CORPUS = new File(EVALUATION_ROOT.getPath() + "/wicopaco/TODO/wrhc-fr_070101-sch1-v2.xml");
	public static final File TEST_WICAOPACO_WIKI_EXCLUDE_CORPUS = new File(EVALUATION_ROOT.getPath() + "/wicopaco/TODO/mixed2-corpus.info.json");
	public static final File TEST_WICAOPACO_CONVERTED = new File(EVALUATION_ROOT.getPath() + "/wicopaco/TODO/wrhc-fr_070101-sch1-v2-converted.txt");

	// TEMP : LEXIQUE
	public static final File LEXIQUE_PATH = new File(ROOT.getPath() + "/data/lexique/Lexique383/Lexique383.tsv");
}
