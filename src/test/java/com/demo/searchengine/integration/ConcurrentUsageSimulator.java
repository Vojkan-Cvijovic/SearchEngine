package com.demo.searchengine.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.demo.searchengine.SearchEngineDemo;
import com.demo.searchengine.core.SearchResult;

/**
 * Simulates concurrent usage of the SearchEngine across multiple threads. Performs file operations (create, update,
 * delete) and searches in parallel with deterministic validation.
 */
public class ConcurrentUsageSimulator {

    private final SearchEngineDemo searchEngine;
    private final Path watchedDir;
    private final int threadCount;
    private final int operationsPerThread;
    private final long operationDelayMs;

    // Statistics
    private final AtomicInteger filesCreated = new AtomicInteger(0);
    private final AtomicInteger filesUpdated = new AtomicInteger(0);
    private final AtomicInteger filesDeleted = new AtomicInteger(0);
    private final AtomicInteger searchesPerformed = new AtomicInteger(0);
    private final AtomicLong totalSearchTime = new AtomicLong(0);
    private final AtomicInteger validationFailures = new AtomicInteger(0);

    ConcurrentUsageSimulator(final SearchEngineDemo searchEngine, final Path watchedDir, final int threadCount,
        final int operationsPerThread, final long operationDelayMs) {
        this.searchEngine = searchEngine;
        this.watchedDir = watchedDir;
        this.threadCount = threadCount;
        this.operationsPerThread = operationsPerThread;
        this.operationDelayMs = operationDelayMs;
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * Runs the concurrent simulation
     */
    SimulationResults run() throws InterruptedException {
        System.out.println("Starting concurrent usage simulation with " + threadCount + " threads...");
        final long startTime = System.currentTimeMillis();
        final long endTime;
        try (final ExecutorService executor = Executors.newFixedThreadPool(10)) {

            final CountDownLatch latch = new CountDownLatch(threadCount);

            // Start worker threads
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        runWorkerThread(threadId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            latch.await();
            endTime = System.currentTimeMillis();

            executor.shutdown();
        }

        final SimulationResults results =
            new SimulationResults(endTime - startTime, filesCreated.get(), filesUpdated.get(), filesDeleted.get(),
                searchesPerformed.get(), totalSearchTime.get(), validationFailures.get());

        // Throw exception if any validation failed
        if (validationFailures.get() > 0) {
            throw new RuntimeException("Concurrent usage simulation failed with " + validationFailures.get()
                    + " validation failures. Check the output above for details.");
        }

        return results;
    }

    /**
     * Worker thread that performs operations with deterministic validation
     */
    private void runWorkerThread(final int threadId) {
        for (int op = 0; op < operationsPerThread; op++) {
            try {
                performDeterministicOperation(threadId, op % 4);

                // Wait between operations
                if (operationDelayMs > 0) {
                    Thread.sleep(operationDelayMs);
                }

            } catch (final Exception e) {
                System.err.println("❌ Thread " + threadId + " operation " + op + " failed: " + e.getMessage());
                validationFailures.incrementAndGet();
            }
        }
    }

    /**
     * Performs a deterministic operation sequence for each thread
     */
    private void performDeterministicOperation(final int threadId, final int operation) throws IOException {
        // Each thread performs operations 0-3 in sequence
        // 0: create, 1: update, 2: remove keyword, 3: delete file

        switch (operation) {
            case 0:
                createDeterministicFile(threadId);
                break;
            case 1:
                updateDeterministicFile(threadId);
                break;
            case 2:
                removeKeywordFromFile(threadId);
                break;
            case 3:
                deleteDeterministicFile(threadId);
                break;
        }
    }

    /**
     * Creates a deterministic file with a specific keyword for the thread
     */
    private void createDeterministicFile(final int threadId) throws IOException {
        final String keyword = String.format("thread-%d-a", threadId); // Operation 0 uses -a suffix
        final String fileName = String.format("thread_%d_file.txt", threadId); // Fixed filename for the thread
        final Path filePath = watchedDir.resolve(fileName);

        final String content = generateDeterministicFileContent(keyword);
        Files.write(filePath, content.getBytes(), StandardOpenOption.CREATE_NEW);

        filesCreated.incrementAndGet();

        // Wait for indexing
        waitForIndexing(threadId);

        // Validate that the file is now searchable
        validateFileSearchable(keyword, fileName, true, "Thread " + threadId + " CREATE");
    }

    /**
     * Updates a deterministic file with a new keyword
     */
    private void updateDeterministicFile(final int threadId) throws IOException {
        final String fileName = String.format("thread_%d_file.txt", threadId);
        final Path filePath = watchedDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            return;
        }

        final String oldKeyword = String.format("thread-%d-a", threadId); // Operation 0 keyword (-a)
        final String newKeyword = String.format("thread-%d-b", threadId); // Operation 1 keyword (-b)

        // Read current content and replace old keyword with new keyword
        final String oldContent = Files.readString(filePath);
        final String newContent = oldContent.replace(oldKeyword, newKeyword);

        Files.write(filePath, newContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        filesUpdated.incrementAndGet();

        // Wait for re-indexing
        waitForIndexing(threadId);

        // Validate that old keyword is no longer searchable and new keyword is searchable
        validateFileSearchable(oldKeyword, fileName, false, "Thread " + threadId + " UPDATE (old)");
        validateFileSearchable(newKeyword, fileName, true, "Thread " + threadId + " UPDATE (new)");
    }

    /**
     * Removes the new keyword from the file (operation 2)
     */
    private void removeKeywordFromFile(final int threadId) throws IOException {
        final String fileName = String.format("thread_%d_file.txt", threadId);
        final Path filePath = watchedDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            return;
        }

        final String newKeyword = String.format("thread-%d-b", threadId); // The keyword that was added in operation 1
                                                                          // (-b)

        // Read current content and remove the new keyword
        final String currentContent = Files.readString(filePath);
        final String updatedContent = currentContent.replace(newKeyword, "[REMOVED]");

        Files.write(filePath, updatedContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        // Wait for re-indexing
        waitForIndexing(threadId);

        // Validate that the new keyword is no longer searchable
        validateFileSearchable(newKeyword, fileName, false, "Thread " + threadId + " REMOVE KEYWORD");
    }

    /**
     * Deletes a deterministic file
     */
    private void deleteDeterministicFile(final int threadId) throws IOException {
        final String fileName = String.format("thread_%d_file.txt", threadId);
        final Path filePath = watchedDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            return;
        }

        final String keyword = String.format("thread-%d-a", threadId); // Original keyword (-a)

        Files.delete(filePath);

        filesDeleted.incrementAndGet();

        // Wait for removal from index
        waitForIndexing(threadId);

        // Validate that the file is no longer searchable
        validateFileSearchable(keyword, fileName, false, "Thread " + threadId + " DELETE");
    }

    /**
     * Generates deterministic file content with the specified keyword
     */
    private String generateDeterministicFileContent(final String keyword) {

        return "This is a test file generated by thread " + Thread.currentThread()
                .getName() + "\n" + "It contains the keyword: " + keyword + "\n" + "Generated at: "
                + System.currentTimeMillis() + "\n" + "File content for testing search functionality.\n"
                + "The keyword " + keyword + " should be searchable.\n";
    }

    /**
     * Waits for indexing to complete with random delay based on thread ID
     */
    private void waitForIndexing(final int threadId) {
        try {
            // More randomness: 2-8 seconds with thread variation
            final double randomFactor = Math.random() * 3.0 + Math.random() * 2.0; // 0.0-5.0 with double randomness
            final int waitTimeMs = 2000 + (int) ((randomFactor + threadId * 0.15) * 1000); // 2000-8000ms, more random
            Thread.sleep(waitTimeMs);
        } catch (final InterruptedException e) {
            Thread.currentThread()
                    .interrupt();
        }
    }

    /**
     * Validates that a file is searchable or not searchable for a specific keyword
     */
    private void validateFileSearchable(final String keyword, final String fileName, final boolean shouldBeSearchable,
        final String operation) {
        final List<SearchResult> results = searchEngine.search(keyword);
        final boolean found = results.stream()
                .anyMatch(r -> r.getFileName()
                        .equals(fileName));

        if (shouldBeSearchable && !found) {
            final String error = "   ❌ VALIDATION FAILED: " + operation + " - File " + fileName
                    + " should contain keyword '" + keyword + "' but was not found in search results";
            System.err.println(error);
            throw new RuntimeException(error);
        } else if (!shouldBeSearchable && found) {
            final String error = "   ❌ VALIDATION FAILED: " + operation + " - File " + fileName
                    + " should NOT contain keyword '" + keyword + "' but was found in search results";
            System.err.println(error);
            throw new RuntimeException(error);
        } else {
            System.out.println("   ✅ VALIDATION PASSED: " + operation + " - File " + fileName + " "
                    + (shouldBeSearchable ? "correctly contains" : "correctly does not contain") + " keyword '"
                    + keyword + "'");
        }
    }

    /**
     * Results of the simulation
     */
    public record SimulationResults(long totalTimeMs, int filesCreated, int filesUpdated, int filesDeleted,
            int searchesPerformed, long totalSearchTime, int validationFailures) {

        public void printSummary() {
            System.out.println("\n=== SIMULATION RESULTS ===");
            System.out.println("Total time: " + totalTimeMs + "ms");
            System.out.println("Files created: " + filesCreated);
            System.out.println("Files updated: " + filesUpdated);
            System.out.println("Files deleted: " + filesDeleted);
            System.out.println("Searches performed: " + searchesPerformed);
            System.out.println("Validation failures: " + validationFailures);
            if (searchesPerformed > 0) {
                System.out.println("Average search time: " + (totalSearchTime / searchesPerformed) + "ms");
            }
            System.out.println("Operations per second: " + String.format("%.2f",
                (double) (filesCreated + filesUpdated + filesDeleted + searchesPerformed) / (totalTimeMs / 1000.0)));

            if (validationFailures > 0) {
                System.err.println("❌ SIMULATION FAILED: " + validationFailures + " validation failures detected!");
            } else {
                System.out.println("✅ SIMULATION SUCCESS: All validations passed!");
            }
        }
    }

    /**
     * Builder for ConcurrentUsageSimulator
     */
    public static class Builder {
        private SearchEngineDemo searchEngine;
        private Path watchedDir;
        private int threadCount = 4;
        private int operationsPerThread = 10;
        private long operationDelayMs = 500;

        public Builder searchEngine(final SearchEngineDemo searchEngine) {
            this.searchEngine = searchEngine;
            return this;
        }

        public Builder watchedDir(final Path watchedDir) {
            this.watchedDir = watchedDir;
            return this;
        }

        public Builder threadCount(final int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public Builder operationsPerThread(final int operationsPerThread) {
            this.operationsPerThread = operationsPerThread;
            return this;
        }

        public Builder operationDelayMs(final long operationDelayMs) {
            this.operationDelayMs = operationDelayMs;
            return this;
        }

        public ConcurrentUsageSimulator build() {
            if (searchEngine == null) {
                throw new IllegalStateException("searchEngine must be set");
            }
            if (watchedDir == null) {
                throw new IllegalStateException("watchedDir must be set");
            }
            return new ConcurrentUsageSimulator(searchEngine, watchedDir, threadCount, operationsPerThread,
                operationDelayMs);
        }
    }
}
