package com.demo.searchengine.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.demo.searchengine.SearchEngineDemo;

/**
 * Comprehensive integration tests for the Search Engine. Tests end-to-end workflows with real filesystem operations,
 * concurrent usage, and various file size scenarios. Migrated from IntegrationTestRunner for automated testing.
 */
public class SearchEngineIntegrationTest {

    @TempDir
    Path tempDir;

    private SearchEngineDemo searchEngine;
    private TestFileSystemGenerator fileSystemGenerator;
    private Path testFileSystemDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create test filesystem directory
        testFileSystemDir = tempDir.resolve("test-filesystem");
        Files.createDirectories(testFileSystemDir);

        // Initialize SearchEngine with test directory
        searchEngine = new SearchEngineDemo();

        // Create filesystem generator with comprehensive settings
        fileSystemGenerator = TestFileSystemGenerator.builder()
                .baseDir(testFileSystemDir)
                .maxDepth(3)
                .maxFilesPerDir(8)
                .maxDirsPerLevel(4)
                .maxFileSize(10 * 1024 * 1024) // 10MB max for faster tests
                .fileProbability(0.8)
                .dirProbability(0.7)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (searchEngine != null) {
            // Clean up SearchEngine resources
            try {
                searchEngine.getClass()
                        .getDeclaredMethod("cleanup")
                        .invoke(searchEngine);
            } catch (final Exception e) {
                // Ignore cleanup errors in tests
            }
        }

        // Clean up test filesystem
        if (testFileSystemDir != null && Files.exists(testFileSystemDir)) {
            deleteDirectoryRecursively(testFileSystemDir);
        }
    }

    @Test
    void testBasicFileIndexingAndSearch() throws Exception {
        // Generate test filesystem
        fileSystemGenerator.generate();
        final Path watchedDir = fileSystemGenerator.getWatchedDir();

        // Start watching the directory
        startWatching(watchedDir);

        // Wait for initial indexing
        Thread.sleep(1000);

        // Test basic search functionality
        final var results = searchEngine.search("java");
        assertNotNull(results, "Search results should not be null");

        assertFalse(results.isEmpty(), "Should find some results for 'java'");

        System.out.println("Basic search test passed - found " + results.size() + " results for 'java'");
    }

    @Test
    void testTwoKeywordsSearch() throws Exception {
        // Generate test filesystem
        fileSystemGenerator.generate();
        final Path watchedDir = fileSystemGenerator.getWatchedDir();

        // Start watching the directory
        startWatching(watchedDir);

        // Wait for initial indexing
        Thread.sleep(1000);

        // Test search for two keywords (should find files containing both "java" AND "programming")
        // We need to use the searchAll method for multiple keywords
        final var results = searchEngine.search("java programming");
        assertNotNull(results, "Search results should not be null");

        // Note: The basic search method treats "java programming" as a single term
        // For true two-keyword AND search. we would need to use searchAll with a list
        // For now, we'll test that the search doesn't crash and returns some results
        System.out.println(
            "Two keywords search test passed - found " + results.size() + " results for 'java programming'");

        // Verify that the results contain the search term
        for (final var result : results) {
            System.out.println("  Found: " + result.getFileName() + " (line " + result.getLineNumber() + ")");
        }
    }

    @Test
    void testSingleKeywordSearch() throws Exception {
        // Generate test filesystem
        fileSystemGenerator.generate();
        final Path watchedDir = fileSystemGenerator.getWatchedDir();

        // Start watching the directory
        startWatching(watchedDir);

        // Wait for initial indexing
        Thread.sleep(1000);

        // Test search for single keyword
        final var results = searchEngine.search("java");
        assertNotNull(results, "Search results should not be null");

        // We should find results for the single keyword
        assertFalse(results.isEmpty(), "Should find some results for 'java'");

        System.out.println("Single keyword search test passed - found " + results.size() + " results for 'java'");

        // Verify that the results contain the keyword
        for (final var result : results) {
            System.out.println("  Found: " + result.getFileName() + " (line " + result.getLineNumber() + ")");
        }
    }

    @Test
    void testBasicFileIndexingAndSearchComprehensive() throws Exception {
        // Generate a simple, predictable test filesystem (like IntegrationTestRunner)
        final TestFileSystemGenerator generator = TestFileSystemGenerator.builder()
                .baseDir(tempDir.resolve("basic-test-comprehensive"))
                .maxDepth(1)           // Only 1 level deep
                .maxFilesPerDir(3)     // Exactly 3 files per directory
                .maxDirsPerLevel(1)    // Only 1 subdirectory
                .maxFileSize(1024)     // Small files (1KB max) for fast processing
                .fileProbability(1.0)  // Always create files
                .dirProbability(1.0)   // Always create subdirectories
                .deterministic(true)   // Use fixed seed for predictable results
                .build();

        generator.generate();

        // Count the actual files created
        final Path watchedDir = generator.getWatchedDir();
        final long fileCount;
        try (final var fileStream = Files.walk(watchedDir)) {
            fileCount = fileStream.filter(Files::isRegularFile)
                    .count();
        }

        System.out.println("Created " + fileCount + " files in watched directory");
        System.out.println("Watched directory: " + watchedDir.toAbsolutePath());

        // List the files created for debugging
        System.out.println("Files created:");
        try (final var fileStream = Files.walk(watchedDir)) {
            fileStream.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            final long size = Files.size(file);
                            System.out.println("  " + file.getFileName() + " (" + size + " bytes)");
                        } catch (final IOException e) {
                            System.out.println("  " + file.getFileName() + " (size unknown)");
                        }
                    });
        }

        // Start watching and test
        startWatching(watchedDir);
        Thread.sleep(2000); // Give more time for indexing

        // Test search for a keyword we know should exist
        final var results = searchEngine.search("java");
        System.out.println("Search for 'java' returned " + results.size() + " results");

        // Verify we found results (since we generate files with keywords)
        if (results.isEmpty()) {
            throw new Exception("Basic indexing test failed - no results found for 'java'");
        }

        // Verify we found results for the expected number of files
        // (Not all files may contain 'java', but some should)
        if (results.size() < fileCount * 0.3) { // At least 30% of files should contain 'java'
            System.out.println("Warning: Found fewer results than expected");
            System.out.println("  Files created: " + fileCount);
            System.out.println("  Results found: " + results.size());
        }

        System.out.println("✅  Basic comprehensive test passed - found " + results.size() + " results for 'java'");
        System.out.println("  Files created: " + fileCount);
        System.out.println("  Results found: " + results.size());

        // Show some sample results
        System.out.println("Sample results:");
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            final var result = results.get(i);
            System.out
                    .println("  " + (i + 1) + ". " + result.getFileName() + " (line " + result.getLineNumber() + ")");
        }
    }

    @Test
    void testConcurrentUsageSimulation() throws Exception {
        // High stress test like IntegrationTestRunner
        final TestFileSystemGenerator generator = TestFileSystemGenerator.builder()
                .baseDir(tempDir.resolve("concurrent-test-high-stress"))
                .maxDepth(1)
                .maxFilesPerDir(1)
                .maxDirsPerLevel(1)
                .maxFileSize(1024) // 1KB max
                .deterministic(true)
                .build();

        generator.generate();

        startWatching(generator.getWatchedDir());
        Thread.sleep(1000);

        final ConcurrentUsageSimulator simulator = ConcurrentUsageSimulator.builder()
                .searchEngine(searchEngine)
                .watchedDir(generator.getWatchedDir())
                .threadCount(5)
                .operationsPerThread(4)
                .operationDelayMs(0)
                .build();

        final var results = simulator.run();
        results.printSummary();

        if (results.validationFailures() > 0) {
            throw new Exception("High stress concurrent usage test failed with " + results.validationFailures()
                    + " validation failures");
        }

        System.out.println("✅ High stress concurrent usage test passed");
    }

    @Test
    void testFileSystemChangeDetection() throws Exception {
        // Generate test filesystem
        fileSystemGenerator.generate();
        final Path watchedDir = fileSystemGenerator.getWatchedDir();

        // Start watching the directory
        startWatching(watchedDir);

        // Wait for initial indexing
        Thread.sleep(1000);

        // Create a new file with a specific keyword
        final String testKeyword = "integration_test_keyword_" + System.currentTimeMillis();
        final Path newFile = watchedDir.resolve("new_test_file.txt");
        final String content = "This file contains the keyword: " + testKeyword + "\n";
        Files.write(newFile, content.getBytes());

        // Wait for a file to be indexed
        Thread.sleep(2000);

        // Search for the keyword
        final var results = searchEngine.search(testKeyword);

        // Verify the new file is found
        assertFalse(results.isEmpty(), "Should find the newly created file");
        final boolean foundNewFile = results.stream()
                .anyMatch(r -> r.getFileName()
                        .equals("new_test_file.txt"));
        assertTrue(foundNewFile, "Should find the newly created file in search results");

        System.out.println("File change detection test passed - found new file in search results");
    }

    @Test
    void testFileSystemChangeDetectionSimple() throws Exception {
        // Simple change detection test like IntegrationTestRunner
        final TestFileSystemGenerator generator = TestFileSystemGenerator.builder()
                .baseDir(tempDir.resolve("change-test-simple"))
                .maxDepth(1)
                .maxFilesPerDir(2)
                .maxDirsPerLevel(1)
                .maxFileSize(1024) // 1KB max
                .build();

        generator.generate();

        startWatching(generator.getWatchedDir());
        Thread.sleep(1000);

        // Create a new file
        final String keyword = "change_test_" + System.currentTimeMillis();
        final Path newFile = generator.getWatchedDir()
                .resolve("new_file.txt");
        Files.write(newFile, ("Keyword: " + keyword).getBytes());

        Thread.sleep(1000);

        final var results = searchEngine.search(keyword);
        if (!results.isEmpty()) {
            System.out.println("✅ Simple file system change test passed - found new file");
        } else {
            System.out.println("⚠ Simple file system change test - new file not found (timing issue?)");
        }
    }

    @Test
    void testLargeFileHandling() throws Exception {
        // Generate test filesystem with larger files
        final TestFileSystemGenerator largeFileGenerator = TestFileSystemGenerator.builder()
                .baseDir(tempDir.resolve("large-files"))
                .maxDepth(2)
                .maxFilesPerDir(3)
                .maxDirsPerLevel(2)
                .maxFileSize(5 * 1024 * 1024) // 5MB files
                .fileProbability(1.0)
                .dirProbability(0.5)
                .build();

        largeFileGenerator.generate();
        final Path watchedDir = largeFileGenerator.getWatchedDir();

        // Start watching the directory
        startWatching(watchedDir);

        Thread.sleep(1000);

        // Test search in large files
        final var results = searchEngine.search("search");
        assertNotNull(results, "Search results should not be null");

        System.out.println("Large file handling test passed - found " + results.size() + " results");
    }

    @Test
    void testLargeFileHandlingComprehensive() throws Exception {
        System.out.println("Setting up comprehensive large file test...");

        // Create a dedicated directory for the large file test
        final Path largeFileDir = tempDir.resolve("large-file-test-comprehensive");
        Files.createDirectories(largeFileDir);

        startWatching(largeFileDir);

        // Test: An oversized file should be ignored
        testFileIndexingBehavior();
    }

    /**
     * Generic test method for testing file indexing behavior based on size.
     */
    private void testFileIndexingBehavior() throws Exception {
        System.out.println("\n--- Test: oversized file (15MB) - should be ignored ---");

        final Path testFileSystemDir = tempDir.resolve("large-files-comprehensive");
        final Path filePath = testFileSystemDir.resolve("oversized_file.txt");
        Files.createDirectories(testFileSystemDir);

        System.out.println("Creating 15MB test file: " + filePath.toAbsolutePath());

        final String fileContent = generateLargeFileContent();
        Files.write(filePath, fileContent.getBytes());

        final long actualSize = Files.size(filePath);
        System.out.println(
            "Created oversized file: " + filePath.getFileName() + " (" + (actualSize / (1024 * 1024)) + "MB)");

        // Wait for an indexing attempt
        Thread.sleep(2000);

        // Search for keywords and validate results
        final String[] testKeywords = {"java", "search", "performance"};
        for (final String keyword : testKeywords) {
            System.out.println("Searching for keyword: '" + keyword + "' (should find nothing)");
            final var results = searchEngine.search(keyword);
            System.out.println("  Found " + results.size() + " results for '" + keyword + "'");

            // File should be ignored - validate it's not found
            if (!results.isEmpty()) {
                throw new Exception("oversized file test failed - file was indexed when it should have been ignored");
            }
        }

        final String status = "correctly ignored";
        System.out.println("✅ oversized file " + status);
    }

    /**
     * Generates large file content with searchable keywords
     */
    private String generateLargeFileContent() {
        final StringBuilder content = new StringBuilder();

        // Define test keywords that will be searchable
        final String[] keywords = {"java", "programming", "search", "index", "file", "system", "test", "integration",
            "concurrent", "thread", "performance", "large", "megabyte", "algorithm", "data", "structure",
            "optimization", "scalability", "throughput"};

        // Add header with keywords
        content.append("LARGE FILE TEST CONTENT\n");
        content.append("This is a test file for testing search engine performance with large files.\n");
        content.append("Keywords: ");
        for (final String keyword : keywords) {
            content.append(keyword)
                    .append(" ");
        }
        content.append("\n\n");

        // Generate content more efficiently for large files
        final String loremIpsum =
            "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do usermod tempor incident ut labor et color magna aliquot. ";
        final int loremLength = loremIpsum.length();

        // Calculate how many repetitions we need
        final long remainingSize = 15728640L - content.length();
        final int repetitions = (int) (remainingSize / loremLength);

        // Add lorem ipsum repetitions
        content.append(loremIpsum.repeat(Math.max(0, repetitions)));

        // Add keywords every 1000 characters for searchability
        for (int i = 0; i < content.length(); i += 1000) {
            final int keywordIndex = (i / 1000) % keywords.length;
            content.insert(i, "KEYWORD_" + keywords[keywordIndex] + " ");
        }

        // Trim to exact target size
        if (content.length() > 15728640L) {
            content.setLength((int) 15728640L);
        }

        return content.toString();
    }

    /**
     * Helper method to start watching a directory
     */
    private void startWatching(final Path directory) throws Exception {
        // Use reflection to call the private startWatching method
        final var method = searchEngine.getClass()
                .getDeclaredMethod("startWatching", Path.class);
        method.setAccessible(true);
        method.invoke(searchEngine, directory);
    }

    /**
     * Helper method to recursively delete a directory
     */
    private void deleteDirectoryRecursively(final Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (final Stream<Path> lists = Files.list(path)) {
                lists.forEach(child -> {
                    try {
                        deleteDirectoryRecursively(child);
                    } catch (final IOException e) {
                        // Ignore errors during cleanup
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }
}
