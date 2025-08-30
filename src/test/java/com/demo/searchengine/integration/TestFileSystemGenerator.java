package com.demo.searchengine.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates a realistic test filesystem for integration testing. Creates directories with varying depths, files with
 * different sizes, and separates watched vs. unwatched sections.
 */
public class TestFileSystemGenerator {

    private final Path baseDir;
    private final Path watchedDir;
    private final Path unwatchedDir;
    private final Random random;

    // Configuration
    private final int maxDepth;
    private final int maxFilesPerDir;
    private final int maxDirsPerLevel;
    private final long maxFileSize;
    private final double fileProbability;
    private final double dirProbability;
    private final boolean deterministic;

    TestFileSystemGenerator(final Path baseDir, final int maxDepth, final int maxFilesPerDir,
        final int maxDirsPerLevel, final long maxFileSize, final double fileProbability, final double dirProbability,
        final boolean deterministic) {
        this.baseDir = baseDir;
        this.watchedDir = baseDir.resolve("watched");
        this.unwatchedDir = baseDir.resolve("unwatched");
        this.maxDepth = maxDepth;
        this.maxFilesPerDir = maxFilesPerDir;
        this.maxDirsPerLevel = maxDirsPerLevel;
        this.maxFileSize = maxFileSize;
        this.fileProbability = fileProbability;
        this.dirProbability = dirProbability;
        this.deterministic = deterministic;
        this.random = deterministic ? new Random(42) : ThreadLocalRandom.current(); // Fixed seed for deterministic
                                                                                    // mode
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * Generates the complete test filesystem
     */
    void generate() throws IOException {
        // Create base directories
        Files.createDirectories(watchedDir);
        Files.createDirectories(unwatchedDir);

        // Generate a watched section (what SearchEngine will monitor)
        generateDirectoryStructure(watchedDir, 0);

        // Generate an unwatched section (for comparison)
        generateDirectoryStructure(unwatchedDir, 0);

        System.out.println("Test filesystem generated:");
        System.out.println("  Base: " + baseDir.toAbsolutePath());
        System.out.println("  Watched: " + watchedDir.toAbsolutePath());
        System.out.println("  Unwatched: " + unwatchedDir.toAbsolutePath());
    }

    /**
     * Recursively generates directory structure with files
     */
    private void generateDirectoryStructure(final Path currentDir, final int currentDepth) throws IOException {
        if (currentDepth >= maxDepth) {
            return;
        }

        // Generate files in the current directory
        final int fileCount = deterministic ? maxFilesPerDir : random.nextInt(maxFilesPerDir + 1);
        for (int i = 0; i < fileCount; i++) {
            if (deterministic || random.nextDouble() < fileProbability) {
                generateRandomFile(currentDir, "file_" + i);
            }
        }

        // Generate subdirectories
        final int dirCount = deterministic ? maxDirsPerLevel : random.nextInt(maxDirsPerLevel + 1);
        for (int i = 0; i < dirCount; i++) {
            if (deterministic || random.nextDouble() < dirProbability) {
                final Path subDir = currentDir.resolve("dir_" + i);
                Files.createDirectories(subDir);
                generateDirectoryStructure(subDir, currentDepth + 1);
            }
        }
    }

    /**
     * Generates a random file with realistic content
     */
    private void generateRandomFile(final Path dir, final String baseName) throws IOException {
        // Random file size (some small, some large)
        final long fileSize;
        if (random.nextDouble() < 0.1) { // 10% chance of a large file
            fileSize = random.nextLong(maxFileSize / 2, maxFileSize);
        } else if (random.nextDouble() < 0.3) { // 30% chance of a medium file
            fileSize = random.nextLong(1024, 1024 * 1024); // 1KB to 1MB
        } else { // 60% chance of a small file
            fileSize = random.nextLong(100, 1024); // 100B to 1KB
        }

        // Choose file extension
        final String extension = getRandomExtension();
        final Path filePath = dir.resolve(baseName + extension);

        // Generate content based on a file type
        final String content = generateFileContent(fileSize);

        // Write a file
        Files.write(filePath, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("  Generated: " + filePath + " (" + fileSize + " bytes)");
    }

    /**
     * Generates realistic file content
     */
    private String generateFileContent(final long targetSize) {
        final StringBuilder content = new StringBuilder();

        // Add some keywords for testing
        final String[] keywords = {"java", "programming", "search", "index", "file", "system", "test", "integration",
            "concurrent", "thread", "performance"};

        // In deterministic mode, always include keywords at the beginning
        if (deterministic) {
            content.append("This is a test file for integration testing. ");
            content.append(
                "Keywords: java programming search index file system test integration concurrent thread performance. ");
        }

        // Generate content with keywords sprinkled throughout
        while (content.length() < targetSize) {
            if (deterministic || random.nextDouble() < 0.1) { // Higher change in deterministic mode
                content.append(keywords[random.nextInt(keywords.length)]);
                content.append(" ");
            } else {
                // Generate random text
                content.append(generateRandomText(10 + random.nextInt(50)));
                content.append(" ");
            }
        }

        // Trim to target size
        if (content.length() > targetSize) {
            content.setLength((int) targetSize);
        }

        return content.toString();
    }

    /**
     * Generates random text for file content
     */
    private String generateRandomText(final int length) {
        final String chars = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789";
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(chars.charAt(random.nextInt(chars.length())));
        }
        return text.toString();
    }

    /**
     * Returns a random file extension
     */
    private String getRandomExtension() {
        final String[] extensions = {".txt", ".java", ".md", ".xml", ".json", ".properties", ".log"};
        return extensions[random.nextInt(extensions.length)];
    }

    /**
     * Gets the watched directory path
     */
    public Path getWatchedDir() {
        return watchedDir;
    }

    /**
     * Builder for TestFileSystemGenerator
     */
    public static class Builder {
        private Path baseDir;
        private int maxDepth = 3;
        private int maxFilesPerDir = 5;
        private int maxDirsPerLevel = 3;
        private long maxFileSize = 50 * 1024 * 1024; // 50MB
        private double fileProbability = 0.7;
        private double dirProbability = 0.6;
        private boolean deterministic = false;

        public Builder baseDir(final Path baseDir) {
            this.baseDir = baseDir;
            return this;
        }

        public Builder maxDepth(final int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder maxFilesPerDir(final int maxFilesPerDir) {
            this.maxFilesPerDir = maxFilesPerDir;
            return this;
        }

        public Builder maxDirsPerLevel(final int maxDirsPerLevel) {
            this.maxDirsPerLevel = maxDirsPerLevel;
            return this;
        }

        public Builder maxFileSize(final long maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        public Builder fileProbability(final double fileProbability) {
            this.fileProbability = fileProbability;
            return this;
        }

        public Builder dirProbability(final double dirProbability) {
            this.dirProbability = dirProbability;
            return this;
        }

        public Builder deterministic(final boolean deterministic) {
            this.deterministic = deterministic;
            return this;
        }

        public TestFileSystemGenerator build() {
            if (baseDir == null) {
                throw new IllegalStateException("baseDir must be set");
            }
            return new TestFileSystemGenerator(baseDir, maxDepth, maxFilesPerDir, maxDirsPerLevel, maxFileSize,
                fileProbability, dirProbability, deterministic);
        }
    }
}
