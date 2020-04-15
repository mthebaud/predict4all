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

import org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument;
import org.predict4all.nlp.utils.progressindicator.ProgressIndicator;

import java.util.concurrent.Callable;

public abstract class TrainerTask implements Callable<Void> {
    protected final ProgressIndicator progressIndicator;
    protected final AbstractTrainingDocument document;

    public TrainerTask(ProgressIndicator progressIndicator, AbstractTrainingDocument document) {
        super();
        this.progressIndicator = progressIndicator;
        this.document = document;
    }

    @Override
    public Void call() throws Exception {
        run();
        return null;
    }

    public abstract void run() throws Exception;
}
