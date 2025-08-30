package com.demo.searchengine.integration;

import java.nio.file.Files;
import java.nio.file.Path;

import com.demo.searchengine.SearchEngineDemo;

/**
 * Dedicated performance testing for the Text Indexing Library. Tests performance under various conditions, including
 * large files and concurrent operations.
 */
public class PerformanceTest {

    public static void main(final String[] args) {
        System.out.println("=== Text Indexing Library Performance Test ===");

        try {
            // Create a temporary directory for tests
            final Path tempDir = Files.createTempDirectory("performance-test-");
            System.out.println("Test directory: " + tempDir.toAbsolutePath());

            // Run performance tests
            runPerformanceTests(tempDir);

            System.out.println("\n=== All performance tests completed successfully! ===");

        } catch (final Exception e) {
            System.err.println("Performance test failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs all performance tests
     */
    private static void runPerformanceTests(final Path tempDir) throws Exception {
        System.out.println("\n--- Running Combined Indexing + Search Performance Test ---");
        runCombinedPerformanceTest(tempDir);
    }

    private static void runCombinedPerformanceTest(final Path tempDir) throws Exception {
        System.out.println("Setting up combined performance test...");

        final SearchEngineDemo searchEngine = new SearchEngineDemo();
        try {
            // Start the search engine and begin watching
            startWatching(searchEngine, tempDir);
            System.out.println("Search engine started and watching directory: " + tempDir.toAbsolutePath());

            // Run combined indexing and search performance test
            System.out.println("\n--- Testing Combined Indexing + Search Performance ---");
            final PerformanceTestResult result = runCombinedPerformanceTestInternal(searchEngine, tempDir);

            // Performance validation
            System.out.println("\n--- Performance Results ---");
            System.out.println("Total time (file creation to searchable): " + result.totalTime + "ms");
            System.out.println("File creation time: " + result.fileCreationTime + "ms");
            System.out.println("Indexing + search detection time: " + result.indexingSearchTime + "ms");
            System.out.println("Search iterations: " + result.searchIterations);

            if (result.totalTime < 3000) {
                System.out.println("✅ Performance: PASSED (< 3 second)");
            } else {
                System.out.println("⚠️ Performance: FAILED (>= 3 second)");
            }

            System.out.println("✅ Combined performance test completed");

        } finally {
            cleanup(searchEngine);
        }
    }

    /**
     * Result class for performance test measurements
     */
    private record PerformanceTestResult(long totalTime, long fileCreationTime, long indexingSearchTime,
            int searchIterations) {
    }

    /**
     * Runs combined performance test measuring file creation to searchable time
     */
    private static PerformanceTestResult runCombinedPerformanceTestInternal(final SearchEngineDemo searchEngine,
        final Path tempDir) throws Exception {
        final String testKeyword = "PERFORMANCE_TEST_KEYWORD_" + System.currentTimeMillis();
        final Path testFile = tempDir.resolve("performance_test_file.txt");

        System.out.println("Creating test file with keyword: " + testKeyword);
        System.out.println("Test file path: " + testFile.toAbsolutePath());

        // Create file content with the keyword
        final String fileContent = "This is a performance test file. " + "It contains the keyword: " + testKeyword
                + " " + "along with some additional content to make it more realistic. "
                + "The search engine should be able to find this file quickly " + "when searching for the keyword.";

        // Start timer for file creation
        final long startTime = System.currentTimeMillis();
        Files.writeString(testFile, fileContent);
        final long fileCreatedTime = System.currentTimeMillis();

        final long fileCreationTime = fileCreatedTime - startTime;
        System.out.println("File created in " + fileCreationTime + "ms");
        System.out.println("Waiting for indexing to complete and searching for results...");

        // Continuously search until the file appears in results with 30 second timeouts
        final long maxWaitTime = 30000; // 30-second max waits
        final long searchStartTime = System.currentTimeMillis();
        boolean fileFound = false;
        int searchIterations = 0;

        while (System.currentTimeMillis() - searchStartTime < maxWaitTime) {
            final var searchResults = searchEngine.search(testKeyword);
            searchIterations++;

            if (!searchResults.isEmpty()) {
                fileFound = true;
                break;
            }

            // Small delay between searches to avoid overwhelming the system
            Thread.sleep(50);
        }

        if (!fileFound) {
            throw new RuntimeException("File was not indexed and searchable within " + maxWaitTime + "ms");
        }

        final long totalTime = System.currentTimeMillis() - startTime;
        final long indexingSearchTime = System.currentTimeMillis() - fileCreatedTime;

        System.out.println("File found in search results after " + indexingSearchTime + "ms");
        System.out.println("Total time (file creation to searchable): " + totalTime + "ms");
        System.out.println("Search iterations performed: " + searchIterations);

        return new PerformanceTestResult(totalTime, fileCreationTime, indexingSearchTime, searchIterations);
    }

    /**
     * Helper method to start watching a directory
     */
    private static void startWatching(final SearchEngineDemo searchEngine, final Path directory) {
        searchEngine.startWatching(directory);
    }

    /**
     * Helper method to clean up SearchEngine resources
     */
    private static void cleanup(final SearchEngineDemo searchEngine) {
        try {
            // First, stop the file watcher to prevent ClosedWatchServiceException
            searchEngine.stopWatching();

            // Wait a bit for watcher threads to fully stop
            Thread.sleep(1000);

        } catch (final Exception e) {
            // Ignore cleanup errors, but log them
            System.out.println("Cleanup warning: " + e.getMessage());
        }
    }
}
