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

package org.predict4all.nlp.trainer;

import org.predict4all.nlp.utils.Pair;

import java.util.Collections;
import java.util.Map;

public class DataTrainerResult {
    private Map<Integer, Pair<Integer, Integer>> ngramCounts;

    public Map<Integer, Pair<Integer, Integer>> getNgramCounts() {
        return ngramCounts;
    }

    private DataTrainerResult(Builder builder) {
        this.ngramCounts = builder.ngramCounts;
    }

    /**
     * Creates builder to build {@link DataTrainerResult}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link DataTrainerResult}.
     */
    public static final class Builder {
        private Map<Integer, Pair<Integer, Integer>> ngramCounts = Collections.emptyMap();

        private Builder() {
        }

        public Builder withNgramCounts(Map<Integer, Pair<Integer, Integer>> ngramCounts) {
            this.ngramCounts = ngramCounts;
            return this;
        }

        public DataTrainerResult build() {
            return new DataTrainerResult(this);
        }
    }

}
