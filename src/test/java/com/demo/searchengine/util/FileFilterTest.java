package com.demo.searchengine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for FileFilter class. Tests file size limits, extension filtering, and error handling.
 */
class FileFilterTest {

    @TempDir
    Path tempDir;

    private FileFilter fileFilter;
    private FileFilter customFileFilter;

    @BeforeEach
    void setUp() {
        fileFilter = new FileFilter(); // Default settings
        customFileFilter = new FileFilter.Builder().maxFileSize(5 * 1024 * 1024) // 5MB
                .indexableExtensions(Set.of(".txt", ".java", ".md"))
                .caseSensitive(true)
                .build();
    }

    @Test
    void shouldIndex_WithFileUnderSizeLimit_ReturnsTrue() throws IOException {
        // Create a 5MB file (under 10MB default limit)
        final Path testFile = tempDir.resolve("test.txt");
        final String content = "x".repeat(5 * 1024 * 1024); // 5MB
        Files.write(testFile, content.getBytes());

        final boolean result = fileFilter.shouldIndex(testFile);

        assertTrue(result, "File under size limit should be indexed");
    }

    @Test
    void shouldIndex_WithFileOverSizeLimit_ReturnsFalse() throws IOException {
        // Create a 15MB file (over 10MB default limit)
        final Path testFile = tempDir.resolve("large.txt");
        final String content = "x".repeat(15 * 1024 * 1024); // 15MB
        Files.write(testFile, content.getBytes());

        final boolean result = fileFilter.shouldIndex(testFile);

        assertFalse(result, "File over size limit should not be indexed");
    }

    @Test
    void shouldIndex_WithExactly10MBFile_ReturnsTrue() throws IOException {
        // Create exactly 10MB file (at the boundary)
        final Path testFile = tempDir.resolve("exact.txt");
        final String content = "x".repeat(10 * 1024 * 1024); // Exactly 10MB
        Files.write(testFile, content.getBytes());

        final boolean result = fileFilter.shouldIndex(testFile);

        assertTrue(result, "File exactly at size limit should be indexed");
    }

    @Test
    void shouldIndex_WithSupportedExtension_ReturnsTrue() throws IOException {
        // Create files with supported extensions
        final Path[] supportedFiles =
            {tempDir.resolve("test.txt"), tempDir.resolve("Main.java"), tempDir.resolve("document.md"),
                tempDir.resolve("config.xml"), tempDir.resolve("data.json"), tempDir.resolve("settings.yaml")};

        for (final Path file : supportedFiles) {
            Files.write(file, "content".getBytes());
            final boolean result = fileFilter.shouldIndex(file);
            assertTrue(result, "File with supported extension should be indexed: " + file.getFileName());
        }
    }

    @Test
    void shouldIndex_WithUnsupportedExtension_ReturnsFalse() throws IOException {
        // Create files with unsupported extensions
        final Path[] unsupportedFiles =
            {tempDir.resolve("image.png"), tempDir.resolve("document.pdf"), tempDir.resolve("archive.zip"),
                tempDir.resolve("video.mp4"), tempDir.resolve("audio.mp3"), tempDir.resolve("binary.bin")};

        for (final Path file : unsupportedFiles) {
            Files.write(file, "content".getBytes());
            final boolean result = fileFilter.shouldIndex(file);
            assertFalse(result, "File with unsupported extension should not be indexed: " + file.getFileName());
        }
    }

    @Test
    void shouldIndex_WithCaseInsensitiveExtensions_ReturnsTrue() throws IOException {
        // Test case-insensitive extension matching (default behavior)
        final Path[] caseVariations = {tempDir.resolve("test.TXT"), tempDir.resolve("document.MD"),
            tempDir.resolve("Main.JAVA"), tempDir.resolve("config.XML"), tempDir.resolve("data.JSON")};

        for (final Path file : caseVariations) {
            Files.write(file, "content".getBytes());
            final boolean result = fileFilter.shouldIndex(file);
            assertTrue(result, "File with case variation should be indexed: " + file.getFileName());
        }
    }

    @Test
    void shouldIndex_WithCaseSensitiveExtensions_RespectsCase() throws IOException {
        // Test case-sensitive extension matching with custom filter
        final Path testFile = tempDir.resolve("test.TXT"); // Uppercase extension
        Files.write(testFile, "content".getBytes());

        // Custom filter is case-sensitive, so .TXT should not match.txt
        final boolean result = customFileFilter.shouldIndex(testFile);
        assertFalse(result, "Case-sensitive filter should not match .TXT to .txt");

        // Lowercase extension should work
        final Path lowerCaseFile = tempDir.resolve("test.txt");
        Files.write(lowerCaseFile, "content".getBytes());
        final boolean lowerCaseResult = customFileFilter.shouldIndex(lowerCaseFile);
        assertTrue(lowerCaseResult, "Case-sensitive filter should match .txt");
    }

    @Test
    void shouldIndex_WithNoExtension_ReturnsFalse() throws IOException {
        // File with no extension
        final Path noExtensionFile = tempDir.resolve("README");
        Files.write(noExtensionFile, "content".getBytes());

        final boolean result = fileFilter.shouldIndex(noExtensionFile);

        assertFalse(result, "File with no extension should not be indexed");
    }

    @Test
    void shouldIndex_WithHiddenFile_ReturnsFalse() throws IOException {
        // Hidden file (starts with dot)
        final Path hiddenFile = tempDir.resolve(".hidden");
        Files.write(hiddenFile, "content".getBytes());

        final boolean result = fileFilter.shouldIndex(hiddenFile);

        assertFalse(result, "Hidden file should not be indexed");
    }

    @Test
    void shouldIndex_WithDirectory_ReturnsFalse() throws IOException {
        // Directory
        final Path directory = tempDir.resolve("subdir");
        Files.createDirectory(directory);

        final boolean result = fileFilter.shouldIndex(directory);

        assertFalse(result, "Directory should not be indexed");
    }

    @Test
    void shouldIndex_WithNonExistentFile_ReturnsFalse() {
        // Non-existent file
        final Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        final boolean result = fileFilter.shouldIndex(nonExistentFile);

        assertFalse(result, "Non-existent file should not be indexed");
    }

    @Test
    void shouldIndex_WithCustomSizeLimit_RespectsLimit() throws IOException {
        // Custom filter with 5MB limit
        final Path smallFile = tempDir.resolve("small.txt");
        final String smallContent = "x".repeat(3 * 1024 * 1024); // 3MB
        Files.write(smallFile, smallContent.getBytes());

        final Path largeFile = tempDir.resolve("large.txt");
        final String largeContent = "x".repeat(8 * 1024 * 1024); // 8MB
        Files.write(largeFile, largeContent.getBytes());

        // Small file should be indexed
        final boolean smallResult = customFileFilter.shouldIndex(smallFile);
        assertTrue(smallResult, "File under custom size limit should be indexed");

        // Large file should not be indexed
        final boolean largeResult = customFileFilter.shouldIndex(largeFile);
        assertFalse(largeResult, "File over custom size limit should not be indexed");
    }

    @Test
    void shouldIndex_WithCustomExtensions_RespectsExtensions() throws IOException {
        // Custom filter only allows .txt, .java, .md
        final Path[] allowedFiles =
            {tempDir.resolve("test.txt"), tempDir.resolve("Main.java"), tempDir.resolve("document.md")};

        final Path[] disallowedFiles =
            {tempDir.resolve("config.xml"), tempDir.resolve("data.json"), tempDir.resolve("script.py")};

        // Test allowed extensions
        for (final Path file : allowedFiles) {
            Files.write(file, "content".getBytes());
            final boolean result = customFileFilter.shouldIndex(file);
            assertTrue(result, "File with custom allowed extension should be indexed: " + file.getFileName());
        }

        // Test disallowed extensions
        for (final Path file : disallowedFiles) {
            Files.write(file, "content".getBytes());
            final boolean result = customFileFilter.shouldIndex(file);
            assertFalse(result, "File with custom disallowed extension should not be indexed: " + file.getFileName());
        }
    }

    @Test
    void getMaxFileSize_ReturnsConfiguredValue() {
        assertEquals(10 * 1024 * 1024, fileFilter.getMaxFileSize(), "Default max file size should be 10MB");
        assertEquals(5 * 1024 * 1024, customFileFilter.getMaxFileSize(), "Custom max file size should be 5MB");
    }

    @Test
    void getIndexableExtensions_ReturnsConfiguredValue() {
        final Set<String> defaultExtensions = fileFilter.getIndexableExtensions();
        assertTrue(defaultExtensions.contains(".txt"), "Default extensions should include .txt");
        assertTrue(defaultExtensions.contains(".java"), "Default extensions should include .java");
        assertTrue(defaultExtensions.contains(".md"), "Default extensions should include .md");

        final Set<String> customExtensions = customFileFilter.getIndexableExtensions();
        assertEquals(3, customExtensions.size(), "Custom extensions should have exactly 3 items");
        assertTrue(customExtensions.contains(".txt"), "Custom extensions should include .txt");
        assertTrue(customExtensions.contains(".java"), "Custom extensions should include .java");
        assertTrue(customExtensions.contains(".md"), "Custom extensions should include .md");
    }

    @Test
    void isCaseSensitive_ReturnsConfiguredValue() {
        assertFalse(fileFilter.isCaseSensitive(), "Default filter should be case-insensitive");
        assertTrue(customFileFilter.isCaseSensitive(), "Custom filter should be case-sensitive");
    }

    @Test
    void builder_WithAllOptions_CreatesCorrectFilter() {
        final FileFilter builtFilter = new FileFilter.Builder().maxFileSize(20 * 1024 * 1024) // 20MB
                .indexableExtensions(Set.of(".custom", ".special"))
                .caseSensitive(false)
                .build();

        assertEquals(20 * 1024 * 1024, builtFilter.getMaxFileSize(), "Built filter should have 20MB limit");
        assertEquals(2, builtFilter.getIndexableExtensions()
                .size(),
            "Built filter should have 2 extensions");
        assertFalse(builtFilter.isCaseSensitive(), "Built filter should be case-insensitive");
    }
}
