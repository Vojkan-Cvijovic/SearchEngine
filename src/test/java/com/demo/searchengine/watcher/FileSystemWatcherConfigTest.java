package com.demo.searchengine.watcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class FileSystemWatcherConfigTest {

    @Test
    void createDefault_ReturnsConfigWithCommonExtensions() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();

        final Set<String> extensions = config.getSupportedExtensions();
        assertTrue(extensions.contains(".txt"));
        assertTrue(extensions.contains(".md"));
        assertTrue(extensions.contains(".java"));
        assertTrue(extensions.contains(".py"));
        assertTrue(extensions.contains(".js"));
        assertTrue(extensions.contains(".xml"));
        assertTrue(extensions.contains(".json"));
    }

    @Test
    void createMinimal_ReturnsConfigWithBasicExtensions() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createMinimal();

        final Set<String> extensions = config.getSupportedExtensions();
        assertTrue(extensions.contains(".txt"));
        assertTrue(extensions.contains(".md"));
        assertTrue(extensions.contains(".java"));
        assertEquals(3, extensions.size());
    }

    @Test
    void shouldWatchFile_WithSupportedExtension_ReturnsTrue() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();

        assertTrue(config.shouldWatchFile("document.txt"));
        assertTrue(config.shouldWatchFile("README.md"));
        assertTrue(config.shouldWatchFile("Main.java"));
        assertTrue(config.shouldWatchFile("script.py"));
        assertTrue(config.shouldWatchFile("app.js"));
        assertTrue(config.shouldWatchFile("config.xml"));
        assertTrue(config.shouldWatchFile("data.json"));
    }

    @Test
    void shouldWatchFile_WithUnsupportedExtension_ReturnsFalse() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();

        assertFalse(config.shouldWatchFile("document.pdf"));
        assertFalse(config.shouldWatchFile("image.png"));
        assertFalse(config.shouldWatchFile("archive.zip"));
        assertFalse(config.shouldWatchFile("executable.exe"));
    }

    @Test
    void shouldWatchFile_WithCaseInsensitiveExtension_ReturnsTrue() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();

        assertTrue(config.shouldWatchFile("document.TXT"));
        assertTrue(config.shouldWatchFile("README.MD"));
        assertTrue(config.shouldWatchFile("Main.JAVA"));
        assertTrue(config.shouldWatchFile("script.PY"));
    }

    @Test
    void shouldWatchFile_WithNoExtension_ReturnsFalse() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();

        assertFalse(config.shouldWatchFile("README"));
        assertFalse(config.shouldWatchFile("Makefile"));
        assertFalse(config.shouldWatchFile("Dockerfile"));
    }

    @Test
    void shouldWatchFile_WithMultipleDots_ReturnsTrue() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();

        assertTrue(config.shouldWatchFile("config.prod.json"));
        assertTrue(config.shouldWatchFile("file.backup.txt"));

        assertFalse(config.shouldWatchFile("build.gradle.kts"));
    }

    @Test
    void getSupportedExtensions_ReturnsCopyOfSet() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();

        final Set<String> extensions1 = config.getSupportedExtensions();
        final Set<String> extensions2 = config.getSupportedExtensions();

        assertNotSame(extensions1, extensions2);

        assertEquals(extensions1, extensions2);
    }

    @Test
    void getThreadPoolSize_ReturnsDefaultValue() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();
        assertEquals(4, config.getThreadPoolSize());
    }

    @Test
    void getWatchEventTimeoutMs_ReturnsDefaultValue() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();
        assertEquals(500, config.getWatchEventTimeoutMs());
    }

    @Test
    void toString_ContainsSupportedExtensions() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();
        final String result = config.toString();

        assertTrue(result.contains("FileSystemWatcherConfig"));
        assertTrue(result.contains("supportedExtensions="));
        assertTrue(result.contains(".txt"));
        assertTrue(result.contains(".java"));
    }

    @Test
    void createDefault_AndCreateMinimal_ReturnDifferentConfigs() {
        final FileSystemWatcherConfig defaultConfig = FileSystemWatcherConfig.createDefault();
        final FileSystemWatcherConfig minimalConfig = FileSystemWatcherConfig.createMinimal();

        final Set<String> defaultExtensions = defaultConfig.getSupportedExtensions();
        final Set<String> minimalExtensions = minimalConfig.getSupportedExtensions();

        assertTrue(defaultExtensions.size() > minimalExtensions.size());
        assertTrue(defaultExtensions.containsAll(minimalExtensions));
    }

    @Test
    void shouldWatchFile_WithEmptyFilename_ReturnsFalse() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();
        assertFalse(config.shouldWatchFile(""));
    }

    @Test
    void shouldWatchFile_WithNullFilename_ReturnsFalse() {
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();
        assertFalse(config.shouldWatchFile(null));
    }
}
