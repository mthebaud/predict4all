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

public class Triple<K, T, V> {
    private final K left;
    private final T middle;
    private final V right;

    private Triple(K left, T middle, V right) {
        super();
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public K getLeft() {
        return left;
    }

    public T getMiddle() {
        return middle;
    }

    public V getRight() {
        return right;
    }

    public static <K, T, V> Triple<K, T, V> of(K left, T middle, V right) {
        return new Triple<>(left, middle, right);
    }

    @Override
    public String toString() {
        return "(" + left + ", " + middle + ", " + right + ")";
    }
}
