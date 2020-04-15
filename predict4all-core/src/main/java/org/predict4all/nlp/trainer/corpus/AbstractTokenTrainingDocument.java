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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public abstract class AbstractTokenTrainingDocument extends AbstractTrainingDocument {

    public AbstractTokenTrainingDocument(TrainingStep step, File inputFile, File outputDirectory) {
        super(step, null, inputFile, outputDirectory);
    }

    @Override
    protected void initializeInformation() throws IOException {
        File infoFile = new File(inputFile.getPath() + INFO_EXTENSION);
        try (Scanner scanner = new Scanner(new FileInputStream(infoFile), StandardCharsets.UTF_8.name())) {
            this.count = scanner.nextInt();
        }
    }
}
