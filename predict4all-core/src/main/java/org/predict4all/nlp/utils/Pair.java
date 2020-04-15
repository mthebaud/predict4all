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

public class Pair<K, T> {
    private final K left;
    private final T right;

    private Pair(K left, T right) {
        super();
        this.left = left;
        this.right = right;
    }

    public K getLeft() {
        return left;
    }

    public T getRight() {
        return right;
    }

    public static <K, T> Pair<K, T> of(K left, T right) {
        return new Pair<>(left, right);
    }

    @Override
    public String toString() {
        return "(" + left + ", " + right + ")";
    }


}
