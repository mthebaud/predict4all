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

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * The way to represent correction rule used in {@link org.predict4all.nlp.prediction.WordPredictor} via {@link org.predict4all.nlp.words.correction.WordCorrectionGenerator}<br>
 * Correction rule are represented as a tree where you can enable/disable whole part of it (e.g. disabling a parent node also disable its children).<br>
 * Node are typed with {@link #getType()} so they can be {@link CorrectionRuleNodeType#NODE} or {@link CorrectionRuleNodeType#LEAF}.<br>
 * Every node can technically contains {@link #getCorrectionRule()} but be aware that only {@link CorrectionRuleNodeType#LEAF} are taken into account by {@link org.predict4all.nlp.words.correction.WordCorrectionGenerator}
 *
 * @author Mathieu THEBAUD
 */
public class CorrectionRuleNode {

    @Expose
    private String name;

    @Expose
    private boolean enabled = true;

    @Expose
    private CorrectionRule correctionRule;

    @Expose
    private final CorrectionRuleNodeType type;


    @Expose
    private final List<CorrectionRuleNode> children;

    public CorrectionRuleNode(CorrectionRuleNodeType type) {
        this.type = type;
        this.children = new ArrayList<>();
    }

    /**
     * The name for this node (just informative)
     *
     * @return this node name
     */
    public String getName() {
        return name;
    }

    /**
     * The name for this node (just informative)
     *
     * @param name this node name
     * @return this node
     */
    public CorrectionRuleNode setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * This node children, only useful if {@link #getType()} is {@link CorrectionRuleNodeType#NODE}
     *
     * @return the children
     */
    public List<CorrectionRuleNode> getChildren() {
        return children;
    }

    /**
     * Convenient method to add a child to this node.<br>
     * This method is NOOP if {@link #getType()} is {@link CorrectionRuleNodeType#LEAF}
     *
     * @param node the child node to add
     * @return this node
     */
    public CorrectionRuleNode addChild(CorrectionRuleNode node) {
        if (this.type == CorrectionRuleNodeType.NODE) {
            this.children.add(node);
        }
        return this;
    }

    /**
     * To know if this node should be taken into account in {@link org.predict4all.nlp.words.correction.WordCorrectionGenerator}<br>
     * Note that individually node can be enabled but then ignored. This is because the corrector goes into the tree from the parent and ignore disabled nodes.
     *
     * @return if this node is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * To know if this node should be taken into account in {@link org.predict4all.nlp.words.correction.WordCorrectionGenerator}<br>
     * Note that individually node can be enabled but then ignored. This is because the corrector goes into the tree from the parent and ignore disabled nodes.
     *
     * @param enabled if this node is enabled
     * @return this node
     */
    public CorrectionRuleNode setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * The correction rule associated on this node.<br>
     * Will be taken into account only if {@link #getType()} is {@link CorrectionRuleNodeType#LEAF}
     *
     * @return correction rule associated on this node
     */
    public CorrectionRule getCorrectionRule() {
        return correctionRule;
    }

    /**
     * The correction rule associated on this node.<br>
     * Will be taken into account only if {@link #getType()} is {@link CorrectionRuleNodeType#LEAF}
     *
     * @param correctionRule correction rule associated on this node
     * @return this node
     */
    public CorrectionRuleNode setCorrectionRule(CorrectionRule correctionRule) {
        this.correctionRule = correctionRule;
        return this;
    }

    /**
     * Type of this node
     *
     * @return type of this node
     */
    public CorrectionRuleNodeType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "CorrectionRuleNode [name=" + name + ", correctionRule=" + correctionRule + "]";
    }

}
