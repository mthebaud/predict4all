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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(BlockJUnit4ClassRunner.class)
public class Predict4AllUtilsTest {
    @Test
    public void testGetWordStart() {
        assertEquals("ai", Predict4AllUtils.getStartUntilNextSeparator("je n'ai"));
        assertEquals("", Predict4AllUtils.getStartUntilNextSeparator("je "));
        assertEquals("je", Predict4AllUtils.getStartUntilNextSeparator("je"));
        assertEquals("qu", Predict4AllUtils.getStartUntilNextSeparator("alors qu"));
    }

    @Test
    public void testGetWordEnd() {
        assertEquals("grès", Predict4AllUtils.getEndUntilNextSeparator("grès que"));
        assertEquals("", Predict4AllUtils.getEndUntilNextSeparator(" alors"));
    }

    @Test
    public void testNPE() {
        assertEquals("", Predict4AllUtils.getEndUntilNextSeparator(null));
        assertEquals("", Predict4AllUtils.getEndUntilNextSeparator(""));
        assertEquals("", Predict4AllUtils.getEndUntilNextSeparator("     "));
    }

    @Test
    public void testCountGetWordStart() {
        assertEquals(2, Predict4AllUtils.countStartUntilNextSeparator("je n'ai"));
        assertEquals(0, Predict4AllUtils.countStartUntilNextSeparator("je "));
        assertEquals(2, Predict4AllUtils.countStartUntilNextSeparator("je"));
        assertEquals(2, Predict4AllUtils.countStartUntilNextSeparator("alors qu"));
    }

    @Test
    public void testCountWordEnd() {
        assertEquals(4, Predict4AllUtils.countEndUntilNextSeparator("grès que"));
        assertEquals(0, Predict4AllUtils.countEndUntilNextSeparator(" alors"));
    }

    @Test
    public void countNPE() {
        assertEquals(0, Predict4AllUtils.countEndUntilNextSeparator(null));
        assertEquals(0, Predict4AllUtils.countEndUntilNextSeparator(""));
        assertEquals(0, Predict4AllUtils.countEndUntilNextSeparator("     "));
    }
}
