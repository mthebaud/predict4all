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

package org.predict4all.nlp.utils.progressindicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LoggingProgressIndicator implements ProgressIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingProgressIndicator.class);

    private final String name;
    private final long max;
    private AtomicLong count;
    private AtomicInteger lastPercent = new AtomicInteger(-1);

    public LoggingProgressIndicator(String name, long max) {
        this.max = max;
        this.name = name;
        count = new AtomicLong();
    }

    @Override
    public void increment() {
        if (this.max > 0) {
            count.incrementAndGet();
            printPercent();
        }
    }

    @Override
    public long getMax() {
        return max;
    }

    @Override
    public long getCount() {
        return count.get();
    }

    private void printPercent() {
        int percent = (int) ((1.0 * count.get()) / (1.0 * max) * 100);
        if (percent != lastPercent.getAndSet(percent)) {
            if (lastPercent.get() % 5 == 0) {
                LOGGER.info("{} progress : {}%", name, lastPercent.get());
            }
        }
    }
}
