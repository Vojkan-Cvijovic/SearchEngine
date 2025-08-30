package com.demo.searchengine.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration loader for Search Engine application. Loads configuration from properties file with validation and
 * fallbacks.
 */
public class SearchEngineConfig {

    private static final Logger logger = LogManager.getLogger(SearchEngineConfig.class);

    // Configuration keys
    private static final String INDEX_DIR_KEY = "index.directory";
    private static final String WATCH_DIR_KEY = "watch.directory";

    private Properties properties;
    private Path indexDirectory;
    private Path watchDirectory;

    SearchEngineConfig() {
    }

    /**
     * Creates a new configuration instance, loading from the specified properties file.
     * @param configFilePath Path to the properties file
     * @throws IllegalArgumentException if configFilePath is null or empty
     * @throws IOException if the file cannot be read or is invalid
     */
    public SearchEngineConfig(final String configFilePath) throws IOException {
        if (configFilePath == null || configFilePath.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Configuration file path cannot be null or empty");
        }

        this.properties = loadProperties(Paths.get(configFilePath.trim()));
        this.indexDirectory = resolveIndexDirectory();
        this.watchDirectory = resolveWatchDirectory();

        logger.info("SearchEngine configuration loaded from: " + configFilePath + " - Index: " + indexDirectory
                + ", Watch: " + watchDirectory);
    }

    /**
     * Loads properties from the specified configuration file.
     * @param configFile Path to the configuration file
     * @throws IOException if the file cannot be read or is invalid
     */
    Properties loadProperties(final Path configFile) throws IOException {
        final Properties props = createProperties();

        if (!fileExists(configFile)) {
            throw new IOException("Configuration file not found: " + configFile.toAbsolutePath());
        }

        if (!isFileReadable(configFile)) {
            throw new IOException("Configuration file is not readable: " + configFile.toAbsolutePath());
        }

        try (final InputStream fileStream = readFile(configFile)) {
            props.load(fileStream);
            logger.info("Configuration loaded from: " + configFile.toAbsolutePath());
        } catch (final IOException e) {
            logger.error("Error reading configuration file: " + e.getMessage());
            throw new IOException("Failed to read configuration file: " + configFile, e);
        }

        return props;
    }

    Properties createProperties() {
        return new Properties();
    }

    InputStream readFile(final Path configFile) throws IOException {
        return Files.newInputStream(configFile);
    }

    boolean isFileReadable(final Path configFile) {
        return Files.isReadable(configFile);
    }

    boolean fileExists(final Path configFile) {
        return Files.exists(configFile);
    }

    /**
     * Resolves the index directory path with validation.
     */
    Path resolveIndexDirectory() {
        final String configValue = properties.getProperty(INDEX_DIR_KEY);
        if (configValue == null || configValue.trim()
                .isEmpty()) {
            throw new IllegalArgumentException(
                "Required property '" + INDEX_DIR_KEY + "' is missing or empty in configuration file");
        }

        final Path path = getPath(configValue.trim());
        if (!isValidDirectoryPath(path)) {
            throw new IllegalArgumentException("Invalid index directory path in configuration: " + configValue);
        }

        logger.info("Using configured index directory: {}", path);
        return path;
    }

    Path getPath(final String trim) {
        return Paths.get(trim);
    }

    /**
     * Resolves the watch directory path with validation.
     */
    Path resolveWatchDirectory() {
        final String configValue = properties.getProperty(WATCH_DIR_KEY);
        if (configValue == null || configValue.trim()
                .isEmpty()) {
            throw new IllegalArgumentException(
                "Required property '" + WATCH_DIR_KEY + "' is missing or empty in configuration file");
        }

        final Path path = getPath(configValue.trim());
        if (!isValidDirectoryPath(path)) {
            throw new IllegalArgumentException("Invalid watch directory path in configuration: " + configValue);
        }

        logger.info("Using configured watch directory: " + path);
        return path;
    }

    /**
     * Validates if a path string represents a valid directory path.
     */
    boolean isValidDirectoryPath(final Path path) {
        if (path == null) {
            return false;
        }

        final String pathString = path.toString();

        // Check for empty or whitespace-only paths
        if (pathString.trim()
                .isEmpty()) {
            return false;
        }

        // Check for relative paths that might be problematic
        if (!path.isAbsolute() && pathString.startsWith("..")) {
            return false;
        }

        // Check for paths with invalid characters (basic validation)
        if (pathString.contains("\0") || pathString.contains("\n") || pathString.contains("\r")) {
            return false;
        }

        return true;
    }

    /**
     * Gets the configured index directory path.
     */
    public Path getIndexDirectory() {
        return indexDirectory;
    }

    /**
     * Gets the configured watch directory path.
     */
    public Path getWatchDirectory() {
        return watchDirectory;
    }

    /**
     * Gets the raw properties for debugging purposes.
     */
    public Properties getProperties() {
        return new Properties(properties);
    }

    /**
     * Ensures that both index and watch directories exist and are accessible.
     * @throws IllegalArgumentException if directories do not exist
     */
    public void ensureDirectoriesExist() {
        if (!directoryExists(indexDirectory)) {
            logger.error("Provided directory does not exist: {}", indexDirectory);
            throw new IllegalArgumentException("Provided directory does not exist: " + indexDirectory);
        }

        if (!directoryExists(watchDirectory)) {
            logger.error("Provided watch directory does not exist: {}", indexDirectory);
            throw new IllegalArgumentException("Provided watch directory does not exist: " + indexDirectory);
        }
    }

    boolean directoryExists(final Path indexDirectory) {
        return Files.exists(indexDirectory);
    }

    /**
     * Validates that the configured directories are accessible and writable.
     * @throws IOException if directories are invalid or inaccessible
     */
    public void validateDirectories() throws IOException {
        try {
            if (!Files.isDirectory(indexDirectory)) {
                throw new IOException("Index directory is not a directory: " + indexDirectory);
            }

            if (!Files.isDirectory(watchDirectory)) {
                throw new IOException("Watch directory is not a directory: " + watchDirectory);
            }

            // Test write permissions
            if (!Files.isWritable(indexDirectory)) {
                throw new IOException("Index directory is not writable: " + indexDirectory);
            }

            if (!Files.isWritable(watchDirectory)) {
                throw new IOException("Watch directory is not writable: " + watchDirectory);
            }

            logger.info("Directory validation successful");

        } catch (final IOException e) {
            logger.error("Directory validation failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets properties for testing purposes.
     * @param properties the properties to set
     */
    void setProperties(final Properties properties) {
        this.properties = properties;
    }

    /**
     * Sets index directory for testing purposes.
     * @param indexDirectory the index directory to set
     */
    void setIndexDirectory(final Path indexDirectory) {
        this.indexDirectory = indexDirectory;
    }

    /**
     * Sets watch directory for testing purposes.
     * @param watchDirectory the watch directory to set
     */
    void setWatchDirectory(final Path watchDirectory) {
        this.watchDirectory = watchDirectory;
    }
}
