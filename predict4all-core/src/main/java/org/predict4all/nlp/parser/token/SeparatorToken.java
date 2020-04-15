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

import org.predict4all.nlp.Separator;

public class SeparatorToken extends AbstractToken {
    private Separator separator;

    protected SeparatorToken(Separator separator) {
        this.separator = separator;
    }

    @Override
    public boolean isSeparator() {
        return true;
    }

    @Override
    public String getText() {
        return this.separator.getOfficialCharString();
    }

    @Override
    public Separator getSeparator() {
        return this.separator;
    }

    public static SeparatorToken create(Separator separator) {
        return new SeparatorToken(separator);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((separator == null) ? 0 : separator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SeparatorToken other = (SeparatorToken) obj;
        if (separator != other.separator)
            return false;
        return true;
    }
}
