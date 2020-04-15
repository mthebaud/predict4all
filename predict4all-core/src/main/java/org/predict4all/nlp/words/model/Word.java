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

package org.predict4all.nlp.words.model;

import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.prediction.PredictionParameter;

import java.io.File;

/**
 * Represent a word stored in a {@link org.predict4all.nlp.words.WordDictionary} - word are stored with a int ID to optimize memory usage.
 * Word can be special ({@link EquivalenceClassWord} or simple. {@link SimpleWord} are firstly got from trained data, but then {@link UserWord} can be added from user vocabulary.
 * <p>
 * Word type is not specialized as it contains every type methods (e.g. {@link #getNGramTag()} even for {@link SimpleWord}) but it is done to optimized runtime performance (avoid instanceof)<br>
 * <p>
 * Word can be modified using {@link #setProbFactor(double, boolean)}, {@link #setForceInvalid(boolean, boolean)} etc...<br>
 * When a word is modified, the modifier should indicate if it is a user or system modification : this would change anything as every modified word are saved with {@link org.predict4all.nlp.words.WordDictionary#saveUserDictionary(File)}
 * but it is a convenient way for library users to know if the word was modified programmatically or by the user (e.g. to filter out)
 *
 * @author Mathieu THEBAUD
 */
public interface Word {
    byte TYPE_EQUIVALENCE_CLASS = 0, TYPE_NGRAM_TAG = 1, TYPE_SIMPLE = 3, TYPE_USER_WORD = 4;

    // BASE INFO
    //========================================================================

    /**
     * @return this word ID : use int as it's the lowest memory foot print primitive to store enough word
     */
    int getID();

    /**
     * @return this word "word" ! Can sometimes be null if the word represent a concept more than a real word (e.g. {@link EquivalenceClassWord} or {@link TagWord})
     */
    String getWord();
    //========================================================================

    // TYPE INFO

    //========================================================================

    /**
     * @return the byte to save this word type (using TYPE_EQUIVALENCE_CLASS, TYPE_NGRAM_TAG, etc...)<br>
     * Used in {@link org.predict4all.nlp.io.WordFileInputStream} and {@link org.predict4all.nlp.io.WordFileOutputStream}
     */
    byte getType();

    /**
     * @return true if this word is {@link TagWord} instance
     */
    boolean isNGramTag();

    /**
     * @return true if this word is {@link EquivalenceClassWord} instance
     */
    boolean isEquivalenceClass();

    /**
     * @return true if this word is {@link UserWord} instance
     */
    boolean isUserWord();

    //========================================================================
    // TYPE SPECIFIC INFO

    //========================================================================

    /**
     * @return the equivalence class represented by this word (only if {@link #isEquivalenceClass()})
     */
    EquivalenceClass getEquivalenceClass();

    /**
     * @return the equivalence class id represented by this word (only if {@link #isEquivalenceClass()})
     */
    byte getEquivalenceClassId();

    /**
     * @return the ngram tag id represented by this word (only if {@link #isNGramTag()})
     */
    byte getNGramTagId();

    /**
     * @return the ngram tag represented by this word (only if {@link #isNGramTag()})
     */
    Tag getNGramTag();

    //========================================================================

    // MODIFY DICTIONARY
    //========================================================================

    /**
     * @return true if this world should be saved (in both original and user dictionary)
     */
    boolean isValidForSaving();

    /**
     * To check if this word can be displayed as a prediction result.<br>
     * This typically return true for original words, but can be sensible to computation for user words.<br>
     * This can also return true/false regarding {@link #isForceInvalid()} or {@link #isForceValid()}
     * Also, user word are valid for prediction regarding {@link PredictionParameter#getMinUseCountToValidateNewWord()}
     *
     * @param predictionParameter the prediction parameter, could be use to validate the word
     * @return true if the word can be displayed in prediction result
     */
    boolean isValidToBePredicted(PredictionParameter predictionParameter);

    /**
     * This factor can be used to modify final probabilities of the predictions.<br>
     * It will be applied once probabilities are computed to influence result list.<br>
     * It is mainly used in a multiplication with the original probability (and then the result list is normalized).<br>
     * To only rely on probabilities, the value should be 1.0
     *
     * @return the prob factor
     */
    double getProbFactor();

    /**
     * This factor can be used to modify final probabilities of the predictions.<br>
     * It will be applied once probabilities are computed to influence result list.<br>
     * It is mainly used in a multiplication with the original probability (and then the result list is normalized).<br>
     * To only rely on probabilities, the value should be 1.0
     *
     * @param factor             the prob factor
     * @param modificationByUser true indicates that the modification was done by the user and not the system
     */
    void setProbFactor(double factor, boolean modificationByUser);

    /**
     * To force that this word become valid, mostly use on {@link UserWord} to ignore validation.
     *
     * @return force valid enabled
     */
    boolean isForceValid();

    /**
     * To force that this word become valid, mostly use on {@link UserWord} to ignore validation.
     *
     * @param forceValid         force valid enabled
     * @param modificationByUser true indicates that the modification was done by the user and not the system
     */
    void setForceValid(boolean forceValid, boolean modificationByUser);

    /**
     * To force that this word is invalid.<br>
     * In fact, this method allow removal of a word from prediction result :
     * words can't be removed from dictionary as they can be used in ngrams, but having forceInvalid true has the same effect than removing a word.
     *
     * @return force invalid enabled
     */
    boolean isForceInvalid();

    /**
     * To force that this word is invalid.<br>
     * In fact, this method allow removal of a word from prediction result :
     * words can't be removed from dictionary as they can be used in ngrams, but having forceInvalid true has the same effect than removing a word.
     *
     * @param forceInvalid       force invalid enabled
     * @param modificationByUser true indicates that the modification was done by the user and not the system
     */
    void setForceInvalid(boolean forceInvalid, boolean modificationByUser);

    //========================================================================
    // MODIFICATIONS DETECTION
    //========================================================================

    /**
     * To manually set modification by user flag
     *
     * @param modifiedByUser modification by user flag
     */
    void setModifiedByUser(boolean modifiedByUser);

    /**
     * To manually set modification by system flag
     *
     * @param modifiedBySystem modification by system flag
     */
    void setModifiedBySystem(boolean modifiedBySystem);

    /**
     * Indicate that this word was modified by the system (e.g. calling a modification method with <code>modificationByUser</code> parameter to false)
     *
     * @return modification by system flag
     */
    boolean isModifiedBySystem();

    /**
     * Indicate that this word was modified by the user (e.g. calling a modification method with <code>modificationByUser</code> parameter to true)
     *
     * @return modification by user flag
     */
    boolean isModifiedByUser();

    /**
     * @return true if {@link #isModifiedByUser()} or {@link #isModifiedBySystem()}
     */
    boolean isModifiedByUserOrSystem();
    //========================================================================


    // USER WORD
    //========================================================================

    /**
     * @return the number of times this word was seen "used" in user text.<br>
     * This count is update by {@link org.predict4all.nlp.prediction.WordPredictor} when training the dynamic model.
     */
    int getUsageCount();

    /**
     * To increase the "usage" count of this word
     */
    void incrementUsageCount();

    /**
     * @return the timestamp of the last usage (typically the last call to {@link #incrementUsageCount()})
     */
    long getLastUseDate();
    //========================================================================

    /**
     * Create a clone of this word.<br>
     * This allow duplication existing word, an new id should be provided.
     *
     * @param newId the word new id
     * @return a clone of this word, with the new id
     */
    Word clone(int newId);
}
