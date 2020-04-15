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

package org.predict4all.nlp.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.predict4all.nlp.Separator;
import org.predict4all.nlp.language.LanguageModel;

import java.io.IOException;

import static org.mockito.Mockito.mock;

@RunWith(BlockJUnit4ClassRunner.class)
public class TokenizerTest {

    private Tokenizer tokenizer;
    private LanguageModel languageModel;

    @Before
    public void setUp() {
        languageModel = mock(LanguageModel.class);
        this.tokenizer = new Tokenizer(languageModel);
    }

    @Test
    public void testTokenizeSimple() throws IOException {
        TokenizerUtils.assertEquals(this.tokenizer.tokenize("ceci est un test."), "ceci", Separator.SPACE, "est", Separator.SPACE, "un",
                Separator.SPACE, "test", Separator.POINT);

    }

    @Test
    public void testTokenizeOther() throws IOException {
        TokenizerUtils.assertEquals(this.tokenizer.tokenize("ceci/.est...l. "), "ceci", Separator.SLASH, Separator.POINT, "est", Separator.POINT,
                Separator.POINT, Separator.POINT, "l", Separator.POINT, Separator.SPACE);

    }

    @Test
    public void testTokenizeWordCurrent() throws IOException {
        TokenizerUtils.assertEquals(this.tokenizer.tokenize("ceci-est"), "ceci", Separator.HYPHEN, "est");

    }
}
