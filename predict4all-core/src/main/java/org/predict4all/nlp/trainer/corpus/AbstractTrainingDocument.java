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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

// Init should be auto based on an input directory and document name (and encoding)
public abstract class AbstractTrainingDocument {
    public static final String INFO_EXTENSION = ".info";
    public static final String OUTPUT_EXTENSION = ".bin";

    private final TrainingStep step;
    protected final String encoding;
    protected final File inputFile;
    protected final File outputFile;
    protected int count;

    public AbstractTrainingDocument(TrainingStep step, String encoding, File inputFile, File outputDirectory) {
        this.step = step;
        this.encoding = encoding;
        TrainingStep previousStep = step.getPreviousStep();
        this.inputFile = previousStep != null
                ? new File(outputDirectory.getPath() + File.separator + previousStep.getOutputDirectoryName()
                + File.separator + inputFile.getName() + OUTPUT_EXTENSION)
                : inputFile;
        if (step.getOutputDirectoryName() != null) {
            File outputDir = new File(outputDirectory.getPath() + File.separator + step.getOutputDirectoryName());
            outputDir.mkdirs();
            this.outputFile = new File(outputDir.getPath() + File.separator + inputFile.getName() + OUTPUT_EXTENSION);
        } else {
            this.outputFile = null;
        }
        try {
            this.initializeInformation();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize informations for " + inputFile, e);
        }
    }

    protected abstract void initializeInformation() throws IOException;

    public TrainingStep getStep() {
        return step;
    }

    public File getInputFile() {
        return inputFile;
    }

    public int getCount() {
        return count;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void writeInformations(int outputCount) {
        try (PrintWriter pw = new PrintWriter(outputFile + INFO_EXTENSION, StandardCharsets.UTF_8.name())) {
            pw.println(outputCount);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't write information for output file " + outputFile, e);
        }
    }

}