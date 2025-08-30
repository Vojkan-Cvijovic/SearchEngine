package com.demo.searchengine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.demo.searchengine.config.SearchEngineConfig;
import com.demo.searchengine.core.SearchResult;
import com.demo.searchengine.service.SimpleTextIndexingService;
import com.demo.searchengine.service.TextIndexingService;
import com.demo.searchengine.tokenizer.impl.SimpleWordTokenizer;
import com.demo.searchengine.watcher.FileSystemWatcher;
import com.demo.searchengine.watcher.FileSystemWatcherConfig;

/**
 * Interactive demo application for the Search Engine text indexing library.
 * <p>
 * Automatically indexes configured directory, watches for changes, and provides search interface.
 * </p>
 * <p>
 * Usage: java -jar SearchEngine.jar ./searchengine.properties
 * </p>
 * @author Vojkan Cvijovic
 * @version 1.0
 * @since 1.0
 */
public class SearchEngineDemo {

    private static final Logger logger = LogManager.getLogger(SearchEngineDemo.class);

    // The Configuration will be loaded from a properties file

    private TextIndexingService searchService;
    private FileSystemWatcher fileWatcher;
    private Scanner scanner;
    private SearchEngineConfig config;

    /**
     * Creates demo instance with default configuration.
     * @throws Exception if initialization fails
     */
    public SearchEngineDemo() throws Exception {
        // Initialize the search service
        this.searchService = new SimpleTextIndexingService(new SimpleWordTokenizer());

        // Create filesystem watcher with simple configuration
        final FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();
        this.fileWatcher = new FileSystemWatcher(searchService, config);
        this.scanner = new Scanner(System.in);

        logger.info("Search Engine initialized");
    }

    /**
     * Creates demo instance with configuration from a properties file.
     * @param configPath Path to the configuration properties file
     * @throws IOException if the configuration file cannot be read or is invalid
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public SearchEngineDemo(final String configPath) throws IOException {
        // Initialize the search service
        this.searchService = new SimpleTextIndexingService(new SimpleWordTokenizer());

        // Load configuration
        this.config = new SearchEngineConfig(configPath);
        this.config.ensureDirectoriesExist();
        this.config.validateDirectories();

        // Create filesystem watcher with configuration including watch directory
        final FileSystemWatcherConfig watcherConfig =
            FileSystemWatcherConfig.createWithWatchDirectory(config.getWatchDirectory());
        this.fileWatcher = new FileSystemWatcher(searchService, watcherConfig);
        this.scanner = new Scanner(System.in);

        logger.info("Search Engine initialized with configuration from: {}", configPath);
    }

    /**
     * Main entry point. Requires a configuration file path as argument.
     * @param args command line arguments (configuration file path)
     * @throws RuntimeException if the application fails to initialize
     */
    public static void main(final String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar searchengine.jar <config-file-path>");
            System.err.println("Example: java -jar searchengine.jar ./searchengine.properties");
            System.exit(1);
        }

        try {
            final String configPath = args[0];
            final SearchEngineDemo demo = new SearchEngineDemo(configPath);
            demo.run();
        } catch (final Exception e) {
            System.err.println("Failed to initialize Search Engine: " + e.getMessage());
            logger.error("Failed to initialize Search Engine: ", e);
            System.exit(1);
        }
    }

    /**
     * Main application loop that displays header, indexes directory starts watching, and enters search loop.
     */
    private void run() {
        System.out.println("=== Search Engine ===");

        if (config != null) {
            System.out.println("Indexing directory: " + config.getIndexDirectory());
            System.out.println("Watching directory: " + config.getWatchDirectory());
            System.out.println();

            // Index the configured directory
            indexDirectory(config.getIndexDirectory());

            // Start watching the configured directory
            startWatching(config.getWatchDirectory());
        } else {
            System.err.println("No configuration loaded. Cannot start.");
            return;
        }

        printHelp();

        // Interactive search loop
        while (true) {
            System.out.print("Search for words (or 'quit' to exit): ");
            final String input = scanner.nextLine()
                    .trim();

            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            // Search for the input words
            searchForWords(input);
        }

        cleanup();
    }

    /**
     * Indexes directory and displays count of indexed files.
     * @param directory the directory to index
     */
    void indexDirectory(final Path directory) {
        try {
            System.out.println("Indexing directory: " + directory);
            final int count = searchService.indexAllFilesInDirectory(directory);
            System.out.println("✅ Indexed " + count + " files");
        } catch (final Exception e) {
            System.err.println("⚠️ Failed to index directory: " + e.getMessage());
            logger.error("Failed to index directory: {}", directory, e);
        }
    }

    /**
     * Searches for query and returns results.
     * @param query the search query string
     * @return list of search results, or empty list if no matches found
     */
    public List<SearchResult> search(final String query) {
        try {
            return searchService.searchAll(List.of(query));
        } catch (final Exception e) {
            logger.error("Search failed for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * Starts filesystem watching for the specified directory.
     */
    public void startWatching(final Path directory) {
        try {
            System.out.println("Starting filesystem watching for: " + directory);
            fileWatcher.startWatching(directory, true);
            System.out.println("✅  Filesystem watching started");
            System.out.println("The index will automatically update when files change.");
        } catch (final Exception e) {
            System.err.println("✗ Failed to start watching: " + e.getMessage());
            logger.error("Failed to start watching directory: {}", directory, e);
        }
    }

    /**
     * Stops filesystem watching.
     */
    public void stopWatching() {
        if (fileWatcher != null) {
            fileWatcher.stop();
        }
    }

    /**
     * Handles single word or multiple word search, displays results.
     */
    void searchForWords(final String input) {
        try {
            // Split input into words
            final List<String> words = Arrays.asList(input.split("\\s+"));
            final List<SearchResult> results = searchService.searchAll(words);
            if (words.size() == 1) {
                // Single word search
                displayResults(words.getFirst(), results);
            } else {
                // Multiple words
                displayResults(String.join(" AND ", words), results);
            }

        } catch (final Exception e) {
            System.err.println("✗ Search failed: " + e.getMessage());
            logger.error("Search failed for input: {}", input, e);
        }
    }

    /**
     * Displays search results, limited to 10 with count of additional results.
     */
    void displayResults(final String searchTerm, final List<SearchResult> results) {
        if (results.isEmpty()) {
            System.out.println("No results found for: " + searchTerm);
        } else {
            System.out.println("Found " + results.size() + " results for: " + searchTerm);
            for (int i = 0; i < Math.min(results.size(), 10); i++) {
                final SearchResult result = results.get(i);
                System.out.printf("  %d. %s (line %d)%n", i + 1, result.getFileName(), result.getLineNumber());
            }

            if (results.size() > 10) {
                System.out.println("  ... and " + (results.size() - 10) + " more results");
            }
        }
        System.out.println();
    }

    /**
     * Prints usage instructions.
     */
    void printHelp() {
        System.out.println("=== How to use ===");
        System.out.println("• Type one or more words to search");
        System.out.println("• Multiple words will be searched as AND (all must be present on the same line)");
        System.out.println("• Files are automatically indexed and watched for changes");
        System.out.println("• Type 'quit' to exit");
        System.out.println();
    }

    /**
     * Closes file watcher and scanner.
     */
    void cleanup() {
        System.out.println("Cleaning up...");

        try {
            if (fileWatcher != null) {
                fileWatcher.close();
            }
            if (scanner != null) {
                scanner.close();
            }
        } catch (final Exception e) {
            logger.error("Error during cleanup", e);
        }

        System.out.println("Goodbye!");
    }

    void setConfig(final SearchEngineConfig config) {
        this.config = config;
    }

    void setSearchService(final TextIndexingService searchService) {
        this.searchService = searchService;
    }

    void setFileWatcher(final FileSystemWatcher fileWatcher) {
        this.fileWatcher = fileWatcher;
    }

    void setScanner(final Scanner scanner) {
        this.scanner = scanner;
    }
}
