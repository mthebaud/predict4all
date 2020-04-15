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

package org.predict4all.nlp.utils;

public class BiIntegerKey {
    private static final int DEFAULT_VALUE = -1;
    private final int v1, v2;

    private BiIntegerKey(int v1, int v2) {
        super();
        this.v1 = v1;
        this.v2 = v2;
    }

    public static BiIntegerKey of(int v1) {
        return new BiIntegerKey(v1, DEFAULT_VALUE);
    }

    public static BiIntegerKey of(int v1, int v2) {
        return new BiIntegerKey(v1, v2);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + v1;
        result = prime * result + v2;
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
        BiIntegerKey other = (BiIntegerKey) obj;
        if (v1 != other.v1)
            return false;
        if (v2 != other.v2)
            return false;
        return true;
    }
}
