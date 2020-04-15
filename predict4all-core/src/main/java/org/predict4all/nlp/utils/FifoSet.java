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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

/**
 * A set maintaining exactly {@link #maxSize} or less but keeping there insertion order to always delete the first inserted element when set is full.
 *
 * @param <T> type of elements in the set
 * @author Mathieu THEBAUD
 */
public class FifoSet<T> {

    private final int maxSize;
    private final HashMap<T, LongAdder> countMap;
    private final ArrayList<T> list;

    public FifoSet(int maxSize) {
        super();
        this.maxSize = maxSize;
        this.list = new ArrayList<>(maxSize + 3);
        this.countMap = new HashMap<>(maxSize + 10);
    }

    public void add(T element) {
        list.add(element);
        countMap.computeIfAbsent(element, e -> new LongAdder()).increment();
        // If an element should be removed
        if (list.size() > maxSize) {
            T first = list.remove(0);
            // First inserted is present once : delete
            LongAdder firstCount = countMap.get(first);
            if (firstCount.sum() <= 1) {
                countMap.remove(first);
            }
            // First is present more than once : decrease count but keep the element
            else {
                firstCount.decrement();
            }
        }
    }

    public Set<T> getSet() {
        return this.countMap.keySet();
    }

    public int size() {
        return this.countMap.size();
    }

    public boolean contains(T element) {
        return this.countMap.containsKey(element);
    }

    public void clear() {
        this.list.clear();
        this.countMap.clear();
    }

    public Map<T, LongAdder> getCountMap() {
        return this.countMap;
    }
}
