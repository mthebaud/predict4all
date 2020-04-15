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

package org.predict4all.nlp.parser.matcher;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import org.predict4all.nlp.EquivalenceClass;
import org.predict4all.nlp.io.TokenFileInputStream;
import org.predict4all.nlp.io.TokenFileOutputStream;
import org.predict4all.nlp.parser.TokenAppender;
import org.predict4all.nlp.parser.TokenProvider;
import org.predict4all.nlp.parser.token.EquivalenceClassToken;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.parser.token.WordToken;
import org.predict4all.nlp.trainer.TrainerTask;
import org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.parser.TokenListAppender;
import org.predict4all.nlp.parser.TokenListProvider;
import org.predict4all.nlp.utils.progressindicator.LoggingProgressIndicator;
import org.predict4all.nlp.utils.progressindicator.NoOpProgressIndicator;
import org.predict4all.nlp.utils.progressindicator.ProgressIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This token converter will convert input token list to another token list, with matched {@link TokenMatcher} pattern.
 * It is useful to convert a specific raw input to whole token matching specific cases.
 * See {@link TokenMatcher} implementations.
 *
 * @author Mathieu THEBAUD
 */
public class TokenConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenConverter.class);

    private final TokenMatcher[] tokenMatchers;

    public TokenConverter(TokenMatcher[] termMatchers) {
        this.tokenMatchers = termMatchers;
    }

    // PUBLIC API
    //========================================================================
    public List<Token> executeTermDetection(List<Token> inputTokenList) throws IOException {
        List<Token> outputTokenList = new ArrayList<>(inputTokenList.size());
        executeTermPatternMatching(new TokenListProvider(inputTokenList), new TokenListAppender(outputTokenList), NoOpProgressIndicator.INSTANCE);
        return outputTokenList;
    }

    public List<TrainerTask> executeTokenPatternMatching(TrainingCorpus corpus) throws IOException {
        corpus.initStep(TrainingStep.TOKEN_CONVERT);
        LoggingProgressIndicator progressIndicator = new LoggingProgressIndicator("Token conversion",
                corpus.getTotalCountFor(TrainingStep.TOKEN_CONVERT));
        return corpus.getDocuments(TrainingStep.TOKEN_CONVERT).stream().map(d -> new TokenPatternMatchingTask(progressIndicator, d))
                .collect(Collectors.toList());
    }
    //========================================================================

    // TASK
    //========================================================================
    private class TokenPatternMatchingTask extends TrainerTask {

        public TokenPatternMatchingTask(ProgressIndicator progressIndicator, AbstractTrainingDocument document) {
            super(progressIndicator, document);
        }

        @Override
        public void run() throws Exception {
            try (TokenFileInputStream tokenFis = new TokenFileInputStream(document.getInputFile())) {
                try (TokenFileOutputStream tokenFos = new TokenFileOutputStream(document.getOutputFile())) {
                    int tokenCount = executeTermPatternMatching(tokenFis, tokenFos, progressIndicator);
                    document.writeInformations(tokenCount);
                }
            }
        }
    }
    //========================================================================

    // PRIVATE API
    //========================================================================
    private int executeTermPatternMatching(TokenProvider tokenProvider, TokenAppender tokenAppender, ProgressIndicator progressIndicator)
            throws IOException {
        LOGGER.debug("Start token conversion");
        long start = System.currentTimeMillis();
        int transformCount = 0;

        TObjectIntHashMap<EquivalenceClass> ecCounts = new TObjectIntHashMap<>(EquivalenceClass.values().length);
        int tokenWritten = 0;
        // For each token in the input stream
        Token current = tokenProvider.getNext();
        while (current != null) {
            boolean matchFound = false;

            progressIndicator.increment();

            // Check if there is a match : the current token and eventually next tokens will
            // be replaced and written in output
            for (TokenMatcher matcher : tokenMatchers) {
                PatternMatched match = matcher.match(current, tokenProvider);
                if (matchFound = match != null) {
                    transformCount++;
                    if (LOGGER.isDebugEnabled())
                        ecCounts.adjustOrPutValue(match.getType(), 1, 1);
                    // Match can be a simple token change or an equivalence class
                    if (match.getType() != null) {
                        tokenAppender.append(EquivalenceClassToken.create(match.getFormattedText(), match.getType()));
                    } else {
                        tokenAppender.append(WordToken.create(match.getFormattedText()));
                    }
                    tokenWritten++;
                    current = match.getPreviousEndToken() != null ? match.getPreviousEndToken().getNext(tokenProvider)
                            : current.getNext(tokenProvider);
                    break;
                }
            }

            // If there is no match, the current token is written to the output
            if (!matchFound) {
                tokenAppender.append(current);
                tokenWritten++;
                current = current.getNext(tokenProvider);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("Token detection result for each entry : ");
            ecCounts.forEachEntry(new TObjectIntProcedure<EquivalenceClass>() {
                @Override
                public boolean execute(EquivalenceClass type, int count) {
                    LOGGER.debug("\t{} = {}", type, count);
                    return true;
                }
            });
            LOGGER.debug("{} total term found in {} s", transformCount, (System.currentTimeMillis() - start) / 1000.0);
        }
        return tokenWritten;
    }
    //========================================================================

}
