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

package org.predict4all.nlp.semantic;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import org.predict4all.nlp.Tag;
import org.predict4all.nlp.io.TokenFileInputStream;
import org.predict4all.nlp.language.LanguageModel;
import org.predict4all.nlp.language.StopWordDictionary;
import org.predict4all.nlp.parser.token.Token;
import org.predict4all.nlp.trainer.TrainerTask;
import org.predict4all.nlp.trainer.configuration.TrainingConfiguration;
import org.predict4all.nlp.trainer.corpus.AbstractTrainingDocument;
import org.predict4all.nlp.trainer.corpus.TrainingCorpus;
import org.predict4all.nlp.trainer.step.TrainingStep;
import org.predict4all.nlp.utils.Pair;
import org.predict4all.nlp.utils.progressindicator.LoggingProgressIndicator;
import org.predict4all.nlp.utils.progressindicator.ProgressIndicator;
import org.predict4all.nlp.words.WordDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * To generate a {@link SemanticDictionary} from an input corpus.<br>
 * This creates a term x term matrix and then reduces it with SVD (via an optimized R script, "Rscript" should be available in path).
 * <strong>WARNING : THIS IS A WIP</strong>
 *
 * @author Mathieu THEBAUD
 */
public class SemanticDictionaryGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticDictionaryGenerator.class);

    private static final String FILE_SUFFIX = ".bin";
    private static final String FILE_PREFIX = "predict4all-r-computing";

    private static final int NO_VALUE_ENTRY = -1;
    private static final int DENSITY_BLOCK_SIZE = 1000;
    private static final String R_SCRIPT_NAME = "svd.r";

    private final LanguageModel languageModel;
    private final WordDictionary wordDictionary;
    private final TrainingConfiguration trainingConfiguration;
    private final StopWordDictionary stopWordDictionary;

    public SemanticDictionaryGenerator(LanguageModel languageModel, WordDictionary wordDictionary, final TrainingConfiguration trainingConfiguration)
            throws IOException {
        this.wordDictionary = wordDictionary;
        this.languageModel = languageModel;
        this.trainingConfiguration = trainingConfiguration;
        this.stopWordDictionary = this.languageModel.getStopWordDictionary(trainingConfiguration);
        if (!this.stopWordDictionary.isInitialized()) {
            this.stopWordDictionary.initialize(wordDictionary);
        }
    }

    public void executeLSATrainingForR(TrainingCorpus corpus, File lsaOutputFile, Consumer<List<? extends TrainerTask>> blockingTaskExecutor)
            throws IOException {
        corpus.initStep(TrainingStep.SEMANTIC_DICTIONARY);
        LoggingProgressIndicator progressIndicator = new LoggingProgressIndicator("LSA generation",
                corpus.getTotalCountFor(TrainingStep.SEMANTIC_DICTIONARY) * 2);

        final List<Pair<Integer, Integer>> wordOrdered = countWordAndGetSortedList(corpus, blockingTaskExecutor, progressIndicator);
        Pair<TIntIntHashMap, TIntIntHashMap> indexes = initializeOccurenceMatrix(wordOrdered);
        TIntIntHashMap rowIndexes = indexes.getLeft();
        TIntIntHashMap columnIndexes = indexes.getRight();
        final int rowCount = rowIndexes.size();

        ConcurrentHashMap<CoOccurrenceKey, LongAdder> countMap = new ConcurrentHashMap<>();

        // Fill matrix
        LOGGER.info("Count matrix initiliazed, will now start counting");
        int windowSize = trainingConfiguration.getLsaWindowSize() % 2 == 0 ? trainingConfiguration.getLsaWindowSize() + 1
                : trainingConfiguration.getLsaWindowSize();
        blockingTaskExecutor.accept(corpus.getDocuments(TrainingStep.SEMANTIC_DICTIONARY).stream()
                .map(d -> new FillCountMatrixTask(progressIndicator, d, windowSize, windowSize / 2, rowIndexes, columnIndexes, countMap))
                .collect(Collectors.toList()));
        LOGGER.info("Matrix filled, filling percentage is {}%", 100.0 * (1.0 * countMap.size()) / (1.0 * rowIndexes.size() * columnIndexes.size()));

        File sizeFile = File.createTempFile(FILE_PREFIX, FILE_SUFFIX);
        File rowIndexesFile = File.createTempFile(FILE_PREFIX, FILE_SUFFIX);
        File columnIndexesFile = File.createTempFile(FILE_PREFIX, FILE_SUFFIX);
        File valuesFile = File.createTempFile(FILE_PREFIX, FILE_SUFFIX);

        prepapreDataForR(sizeFile, rowIndexesFile, columnIndexesFile, valuesFile, rowIndexes, columnIndexes, countMap);

        // Launch R script
        System.gc();
        File ouputMatrixFile = launchRScript(sizeFile, rowIndexesFile, columnIndexesFile, valuesFile);

        // Save matrix info
        try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(lsaOutputFile)))) {
            // Matrix size
            dos.writeInt(rowCount);
            dos.writeInt(this.trainingConfiguration.getLsaTargetSvdSize());
            writeIndexes(dos, rowIndexes);

            double[][] semanticMatrix = readMatrixFromR(ouputMatrixFile, dos, rowIndexes);

            // Compute densities
            computeAndWriteDensities(blockingTaskExecutor, rowCount, dos, semanticMatrix);
        }
    }

    private void computeAndWriteDensities(Consumer<List<? extends TrainerTask>> blockingTaskExecutor, final int rowCount, DataOutputStream dos,
                                          double[][] semanticMatrix) throws IOException {
        List<ComputeDensitiesTask> tasks = new ArrayList<>();
        double[] densities = new double[rowCount];
        ProgressIndicator proDens = new LoggingProgressIndicator("Computing densities", rowCount);
        for (int i = 0; i < rowCount; i += DENSITY_BLOCK_SIZE) {
            tasks.add(new ComputeDensitiesTask(proDens, semanticMatrix, i, Math.min(rowCount, i + DENSITY_BLOCK_SIZE), densities));
        }
        blockingTaskExecutor.accept(tasks);
        //Write densities
        double minD = Double.MAX_VALUE, maxD = Double.MIN_VALUE;
        for (double d : densities) {
            minD = Math.min(minD, d);
            maxD = Math.max(maxD, d);
            dos.writeDouble(d);
        }
        LOGGER.info("Wrote density matrix in file, min value is {}, max value is {}", minD, maxD);
    }

    private double[][] readMatrixFromR(File ouputMatrixFile, DataOutputStream dos, TIntIntHashMap rowIndexes)
            throws IOException, FileNotFoundException {
        double[][] semanticMatrix;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(ouputMatrixFile)))) {
            int rowCountR = dis.readInt();
            int colCountR = dis.readInt();
            LOGGER.info("Will read data matrix from r result : {}x{}", rowCountR, colCountR);
            semanticMatrix = new double[rowCountR][colCountR];
            for (int r = 0; r < rowCountR; r++) {
                double[] values = new double[colCountR];
                for (int c = 0; c < colCountR; c++) {
                    values[c] = dis.readDouble();
                }
                SemanticDictionary.normalizeRow(values);
                semanticMatrix[r] = values;
                double sum = 0.0;
                boolean foundZero = false;
                for (double v : values) {
                    dos.writeDouble(v);
                    sum += v;
                    if (v == 0) {
                        foundZero = true;
                    }
                }
                if (sum == 0.0 || foundZero) {
                    LOGGER.warn("Found a line with only zeros, index {}", r);
                    final int rowIndexWithOnlyZeros = r;
                    rowIndexes.forEachEntry(new TIntIntProcedure() {
                        @Override
                        public boolean execute(int wordId, int rowIndex) {
                            if (rowIndex == rowIndexWithOnlyZeros) {
                                LOGGER.warn("Found the corresponding word for zero line : {}", wordDictionary.getWord(wordId));
                                return false;
                            }
                            return true;
                        }
                    });
                }
            }
        }
        return semanticMatrix;
    }

    private void writeIndexes(DataOutputStream dos, TIntIntHashMap rowIndexes) throws IOException {
        // Indexes
        final boolean writeIndexesSuccess = rowIndexes.forEachEntry(new TIntIntProcedure() {
            @Override
            public boolean execute(int wordId, int rowIndex) {
                try {
                    dos.writeInt(wordId);
                    dos.writeInt(rowIndex);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        if (!writeIndexesSuccess)
            throw new IOException("Couldn't write whole row indexes into semantic data file");
    }

    private File launchRScript(File sizeFile, File rowIndexesFile, File columnIndexesFile, File valuesFile) throws IOException {
        // Create R script
        File rDir = Files.createTempDirectory("predict4all-r").toFile();
        try (OutputStream os = new FileOutputStream(rDir.getPath() + File.separator + R_SCRIPT_NAME)) {
            try (InputStream is = getClass().getResourceAsStream("/r-script/" + R_SCRIPT_NAME)) {
                byte[] buffer = new byte[4096];
                int n;
                while (-1 != (n = is.read(buffer))) {
                    os.write(buffer, 0, n);
                }
            }
        }
        LOGGER.info("R script created to {}", rDir.getAbsolutePath());
        File ouputMatrixFile = File.createTempFile(FILE_PREFIX, FILE_SUFFIX);
        LOGGER.info("Will launch R process, expected output R file : {}", ouputMatrixFile.getAbsolutePath());
        Process rProcess = new ProcessBuilder(//
                "Rscript", "--vanilla", R_SCRIPT_NAME, //
                sizeFile.getAbsolutePath(), //
                rowIndexesFile.getAbsolutePath(), columnIndexesFile.getAbsolutePath(), valuesFile.getAbsolutePath(), //
                ouputMatrixFile.getAbsolutePath(), //
                "" + trainingConfiguration.getLsaTargetSvdSize())//
                .directory(rDir)//
                .start();
        createProcessLogger(rProcess, true);
        createProcessLogger(rProcess, false);
        try {
            int r = rProcess.waitFor();
            LOGGER.info("R script ended with result {}", r);
        } catch (InterruptedException e1) {
            throw new IOException("R script failed", e1);
        }
        return ouputMatrixFile;
    }

    private void prepapreDataForR(File sizeFile, File rowIndexesFile, File columnIndexesFile, File valuesFile, TIntIntHashMap rowIndexes,
                                  TIntIntHashMap columnIndexes, ConcurrentHashMap<CoOccurrenceKey, LongAdder> countMap) throws IOException, FileNotFoundException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(sizeFile))) {
            dos.writeInt(rowIndexes.size());
            dos.writeInt(columnIndexes.size());
            dos.writeInt(countMap.size());
        }
        LOGGER.info("Size matrix read");

        try (DataOutputStream dosRowIndex = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rowIndexesFile)))) {
            try (DataOutputStream dosColumIndex = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(columnIndexesFile)))) {
                try (DataOutputStream dosValue = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(valuesFile)))) {
                    countMap.forEach((index, v) -> {
                        try {
                            dosRowIndex.writeInt(index.rowIndex + 1);
                            dosColumIndex.writeInt(index.columnIndex + 1);
                            dosValue.writeInt(v.intValue());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
        countMap.clear();
        LOGGER.info("File prepared for R script, will now launch it\n\tSize = {}\n\tRow indexes = {}\n\tColumn indexes = {}\n\tValues = {}", sizeFile,
                rowIndexesFile, columnIndexesFile, valuesFile);
    }

    private Pair<TIntIntHashMap, TIntIntHashMap> initializeOccurenceMatrix(final List<Pair<Integer, Integer>> wordOrdered) {
        // Create base matrix : column are most frequent words, row are the most frequent part of the vocabulary
        // we don't need to keep the column indexes, but we need to keep the row indexes to be able to compare two words
        final int columnCount = this.trainingConfiguration.getLsaFrequentWordSize();
        final int rowCount = Math.min(wordOrdered.size(), trainingConfiguration.getLsaVocabularySize());
        TIntIntHashMap rowIndexes = new TIntIntHashMap(rowCount, 0.9f, NO_VALUE_ENTRY, NO_VALUE_ENTRY);
        TIntIntHashMap columnIndexes = new TIntIntHashMap(columnCount, 0.9f, NO_VALUE_ENTRY, NO_VALUE_ENTRY);
        LOGGER.info("{} words sorted by count, will initilize count matrix {} rows, {} columns", wordOrdered.size(), rowCount, columnCount);
        for (int i = 0; i < rowCount; i++) {
            rowIndexes.put(wordOrdered.get(i).getLeft(), i);
            if (i < columnCount) {
                columnIndexes.put(wordOrdered.get(i).getLeft(), i);
            }
        }
        // Allow GC
        rowIndexes.compact();
        columnIndexes.compact();
        wordOrdered.clear();

        return Pair.of(rowIndexes, columnIndexes);
    }

    private List<Pair<Integer, Integer>> countWordAndGetSortedList(TrainingCorpus corpus, Consumer<List<? extends TrainerTask>> blockingTaskExecutor,
                                                                   LoggingProgressIndicator progressIndicator) {
        // Count all words
        final ConcurrentHashMap<Integer, LongAdder> wordCounts = new ConcurrentHashMap<>(
                (int) (trainingConfiguration.getLsaWindowSize() * trainingConfiguration.getLsaFrequentWordSize() * 1.05), 0.95f,
                Runtime.getRuntime().availableProcessors());
        blockingTaskExecutor.accept(corpus.getDocuments(TrainingStep.SEMANTIC_DICTIONARY).stream()
                .map(d -> new CountWordTask(wordCounts, progressIndicator, d)).collect(Collectors.toList()));
        LOGGER.info("Word count created, found {} differents words, will now sort ", wordCounts.size());

        // Sort to get the most frequent words
        final List<Pair<Integer, Integer>> wordOrdered = new ArrayList<>();
        wordCounts.forEach((id, count) -> wordOrdered.add(Pair.of(id, count.intValue())));
        wordCounts.clear();
        Collections.sort(wordOrdered, (p1, p2) -> Integer.compare(p2.getRight(), p1.getRight()));
        return wordOrdered;
    }

    // R SCRIPTS
    //========================================================================
    private void createProcessLogger(final Process process, boolean error) {
        Thread logThread = new Thread(() -> {
            InputStream is = error ? process.getErrorStream() : process.getInputStream();
            try (Scanner scan = new Scanner(is)) {
                while (scan.hasNextLine()) {
                    final String line = scan.nextLine();
                    if (error) {
                        LOGGER.warn("RScript : {}", line);
                    } else {
                        LOGGER.info("RScript : {}", line);
                    }
                }
            }
        }, "Process-Logger-" + (error ? "Err" : "Out"));
        logThread.setDaemon(true);
        logThread.start();
    }
    //========================================================================

    // UTILS
    //========================================================================
    private void putLSADataInMatrix(final int windowSize, Token[] windowArray, final int middleIndex, TIntIntHashMap rowIndexes,
                                    TIntIntHashMap columnIndexes, ConcurrentHashMap<CoOccurrenceKey, LongAdder> countMap) throws IOException {
        Token srcToken = windowArray[middleIndex];
        if (srcToken != null) {
            final int srcId = this.getWordIdForLSA(srcToken);
            if (srcId >= 0) {
                for (int i = 0; i < windowArray.length; i++) {
                    Token dstToken = windowArray[i];
                    if (srcToken != dstToken && dstToken != null) {
                        int dstId = this.getWordIdForLSA(dstToken);
                        incrementCount(srcId, dstId, rowIndexes, columnIndexes, countMap);
                        incrementCount(dstId, srcId, rowIndexes, columnIndexes, countMap);
                    }
                }
            }
        }
    }

    private void incrementCount(final int srcId, int dstId, TIntIntHashMap rowIndexes, TIntIntHashMap columnIndexes,
                                ConcurrentHashMap<CoOccurrenceKey, LongAdder> countMap) {
        int currentWordRowIndex = rowIndexes.get(srcId);
        int wordInWindowColumnIndex = columnIndexes.get(dstId);
        if (currentWordRowIndex >= 0 && wordInWindowColumnIndex >= 0) {
            countMap.computeIfAbsent(new CoOccurrenceKey(currentWordRowIndex, wordInWindowColumnIndex), k -> new LongAdder()).increment();
        }
    }

    private int getWordIdForLSA(Token token) {
        if (!token.isSeparator()) {
            final int wordId = token.getWordId(wordDictionary);
            if (wordId != Tag.UNKNOWN.getId() && !stopWordDictionary.containsWord(wordId)) {
                return wordId;
            }
        }
        return -1;
    }
    //========================================================================

    // TASKS
    //========================================================================
    private class FillCountMatrixTask extends TrainerTask {
        private final int windowSize, middleIndex;
        private final TIntIntHashMap rowIndexes, columnIndexes;
        private final ConcurrentHashMap<CoOccurrenceKey, LongAdder> countMap;

        public FillCountMatrixTask(ProgressIndicator progressIndicator, AbstractTrainingDocument document, final int windowSize,
                                   final int middleIndex, TIntIntHashMap rowIndexes, TIntIntHashMap columnIndexes,
                                   ConcurrentHashMap<CoOccurrenceKey, LongAdder> countMap) {
            super(progressIndicator, document);
            this.windowSize = windowSize;
            this.middleIndex = middleIndex;
            this.rowIndexes = rowIndexes;
            this.columnIndexes = columnIndexes;
            this.countMap = countMap;
        }

        @Override
        public void run() throws Exception {
            Token[] windowArray = new Token[windowSize];
            try (TokenFileInputStream tokenFis = new TokenFileInputStream(document.getInputFile())) {
                Token token = tokenFis.getNext();
                while (token != null) {
                    // Shift array to left and fill with the new token
                    if (!token.isSeparator()) {
                        System.arraycopy(windowArray, 1, windowArray, 0, windowArray.length - 1);
                        windowArray[windowArray.length - 1] = token;
                    }
                    putLSADataInMatrix(trainingConfiguration.getLsaWindowSize(), windowArray, middleIndex, rowIndexes, columnIndexes, countMap);

                    token = token.getNext(tokenFis);
                    progressIndicator.increment();
                }
            }
        }
    }

    private class CountWordTask extends TrainerTask {
        private final ConcurrentHashMap<Integer, LongAdder> wordCounts;

        public CountWordTask(ConcurrentHashMap<Integer, LongAdder> wordCounts, ProgressIndicator progressIndicator,
                             AbstractTrainingDocument document) {
            super(progressIndicator, document);
            this.wordCounts = wordCounts;
        }

        @Override
        public void run() throws Exception {
            try (TokenFileInputStream tokenFis = new TokenFileInputStream(document.getInputFile())) {
                Token token = tokenFis.readToken();
                while (token != null) {
                    int wordId = getWordIdForLSA(token);
                    if (wordId >= 0) {
                        wordCounts.computeIfAbsent(wordId, k -> new LongAdder()).increment();
                    }
                    token = token.getNext(tokenFis);
                    progressIndicator.increment();
                }
            }
        }
    }

    private class ComputeDensitiesTask extends TrainerTask {
        private final double[][] matrix;
        private final int rowIndexStart, rowIndexEnd;
        private final double[] densities;

        public ComputeDensitiesTask(ProgressIndicator progressIndicator, double[][] matrix, int rowIndexStart, int rowIndexEnd,
                                    final double[] densities) {
            super(progressIndicator, null);
            this.matrix = matrix;
            this.rowIndexStart = rowIndexStart;
            this.rowIndexEnd = rowIndexEnd;
            this.densities = densities;
        }

        @Override
        public void run() throws Exception {
            for (int r = rowIndexStart; r < rowIndexEnd; r++) {
                double[] row = matrix[r];
                List<Double> cosineAngles = new ArrayList<>(matrix.length);
                for (int r2 = 0; r2 < matrix.length; r2++) {
                    if (r != r2) {
                        cosineAngles.add(SemanticDictionary.cosineAngle(row, matrix[r2]));
                    }
                }
                this.densities[r] = cosineAngles.stream()//
                        .sorted((a, b) -> Double.compare(b, a))//
                        .limit(trainingConfiguration.getLsaDensitySize())//
                        .mapToDouble(v -> v)//
                        .average()//
                        .orElseGet(() -> 0.0);
                progressIndicator.increment();
            }
        }
    }
    //========================================================================

}
