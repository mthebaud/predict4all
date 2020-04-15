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

package org.predict4all.nlp.trainer.corpus;

import org.predict4all.nlp.trainer.step.TrainingStep;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrainingCorpus {

    private final File outputDirectory;
    private final int concurrencyLevel;
    private final Map<TrainingStep, List<AbstractTrainingDocument>> documentsByStep;
    private final String encoding;
    private final File[] inputFiles;

    public TrainingCorpus(final int concurrencyLevel, File inputDirectory, File outputDirectory, String encoding) throws IOException {
        this.encoding = encoding;
        this.concurrencyLevel = concurrencyLevel;
        this.outputDirectory = outputDirectory;
        this.documentsByStep = new HashMap<>();
        this.inputFiles = Arrays.stream(inputDirectory.listFiles()).filter(f -> !f.getName().endsWith(".info.json")).toArray(File[]::new);
    }

    public void initStep(TrainingStep step) {
        this.documentsByStep.put(step, Arrays.stream(inputFiles).parallel().map(d -> step.getStep(d, this)).collect(Collectors.toList()));
    }

    public int getTotalCountFor(TrainingStep step) {
        return this.documentsByStep.get(step).stream().mapToInt(AbstractTrainingDocument::getCount).sum();
    }

    public List<AbstractTrainingDocument> getDocuments(TrainingStep step) {
        return this.documentsByStep.get(step);
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public String getEncoding() {
        return encoding;
    }

}
