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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class FifoSetTest {
    private FifoSet<Integer> set;

    @Before
    public void setUp() {
        set = new FifoSet<>(4);
    }

    @Test
    public void testClassic() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
        assertEquals(4, set.size());
    }

    @Test
    public void testClassicPlus() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        assertFalse(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
        assertTrue(set.contains(5));
        assertEquals(4, set.size());
    }

    @Test
    public void testClassicDoubleInsert() {
        set.add(2);
        set.add(1);
        set.add(3);
        set.add(1);
        set.add(4);
        set.add(5);
        assertTrue(set.contains(1));
        assertFalse(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
        assertTrue(set.contains(5));
        assertEquals(4, set.size());
        set.add(6);
        assertTrue(set.contains(1));
        assertFalse(set.contains(2));
        assertFalse(set.contains(3));
        assertTrue(set.contains(4));
        assertTrue(set.contains(5));
        assertTrue(set.contains(6));
        assertEquals(4, set.size());
    }
}
