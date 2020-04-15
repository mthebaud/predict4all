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

package org.predict4all.nlp.parser.token;

import org.predict4all.nlp.EquivalenceClass;

//TODO : should not inherit from WordToken because the text doesn't matter... ?
public class EquivalenceClassToken extends WordToken {

    private EquivalenceClass equivalenceClass;

    private EquivalenceClassToken(String text, EquivalenceClass equivalenceClass) {
        super(text);
        this.equivalenceClass = equivalenceClass;
    }

    @Override
    public boolean isEquivalenceClass() {
        return true;
    }

    @Override
    public boolean isWord() {
        return false;
    }

    @Override
    public EquivalenceClass getEquivalenceClass() {
        return equivalenceClass;
    }

    public static EquivalenceClassToken create(String text, EquivalenceClass equivalenceClass) {
        return new EquivalenceClassToken(text, equivalenceClass);
    }
}
