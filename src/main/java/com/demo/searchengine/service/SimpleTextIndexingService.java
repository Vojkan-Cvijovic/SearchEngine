package com.demo.searchengine.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.demo.searchengine.core.FileLocation;
import com.demo.searchengine.core.FileMetadata;
import com.demo.searchengine.core.Index;
import com.demo.searchengine.core.SearchResult;
import com.demo.searchengine.core.impl.ThreadSafeIndex;
import com.demo.searchengine.performance.PerformanceMetrics;
import com.demo.searchengine.performance.PerformanceMonitor;
import com.demo.searchengine.tokenizer.Tokenizer;
import com.demo.searchengine.tokenizer.model.Token;
import com.demo.searchengine.util.FileFilter;
import com.demo.searchengine.util.RetryUtils;

/**
 * Simple implementation of TextIndexingService. Provides file indexing and search capabilities with performance
 * monitoring and caching.
 */
public class SimpleTextIndexingService implements TextIndexingService {

    private static final Logger logger = LogManager.getLogger(SimpleTextIndexingService.class);

    private Index index;
    private final Tokenizer tokenizer;
    private PerformanceMonitor performanceMonitor;
    Set<Path> indexedFiles; // package-protected for testing
    private FileFilter fileFilter;

    /**
     * Creates service with specified tokenizer.
     * @param tokenizer the tokenizer to use
     */
    public SimpleTextIndexingService(final Tokenizer tokenizer) {
        this.index = new ThreadSafeIndex();
        this.tokenizer = Objects.requireNonNull(tokenizer, "Tokenizer cannot be null");
        this.performanceMonitor = new PerformanceMonitor();
        this.indexedFiles = ConcurrentHashMap.newKeySet();
        this.fileFilter = new FileFilter(); // Use default settings

        logger.info("SimpleTextIndexingService initialized with tokenizer: {}", tokenizer.getName());
    }

    /**
     * Indexes a single file and returns success status.
     * @param filePath the file to index
     * @return true if indexing succeeded
     */
    @Override
    public boolean indexFile(final Path filePath) {
        Objects.requireNonNull(filePath, "File path cannot be null");

        // Validate file before processing
        if (!validateFileForIndexing(filePath)) {
            return false;
        }

        try {
            // Start performance monitoring
            final PerformanceMonitor.IndexingOperation op = performanceMonitor.startIndexing();

            // Process the file content
            final boolean success = processFileContent(filePath);

            // Complete performance monitoring
            performanceMonitor.completeIndexing(op);

            if (success) {
                logger.info("Successfully indexed file: {}", filePath);
            } else {
                logger.error("Failed to index file: {}", filePath);
            }
            return success;

        } catch (final Exception e) {
            logger.error("Unexpected error indexing file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Validates if a file can be indexed.
     * @param filePath The file path to validate
     * @return true if the file can be indexed, false otherwise
     */
    boolean validateFileForIndexing(final Path filePath) {
        if (!Files.exists(filePath)) {
            logger.warn("File does not exist: {}", filePath);
            return false;
        }

        if (!Files.isRegularFile(filePath)) {
            logger.warn("Path is not a regular file: {}", filePath);
            return false;
        }

        if (!isSupportedFile(filePath)) {
            logger.info("Skipping unsupported file type: {}", filePath);
            return false;
        }

        return true;
    }

    /**
     * Processes the content of a file for indexing.
     * @param filePath The file path to process
     * @return true if processing was successful, false otherwise
     */
    boolean processFileContent(final Path filePath) {
        try {
            // Read and validate file content
            final String content = readAndValidateFileContent(filePath);
            if (content == null) {
                return false;
            }

            // Tokenize content
            final List<Token> tokenInfos = tokenizer.tokenize(content);
            if (tokenInfos.isEmpty()) {
                logger.info("No valid terms found in file: {}", filePath);
                return false;
            }

            // Process tokens and update index
            processTokensAndUpdateIndex(filePath, tokenInfos);

            // Create and add file metadata
            createAndAddFileMetadata(filePath, tokenInfos);

            // Track indexed file
            indexedFiles.add(filePath);

            logger.info("Successfully processed file: {} ({} terms)", filePath, tokenInfos.size());
            return true;

        } catch (final Exception e) {
            logger.error("Error processing file content: {}", filePath, e);
            return false;
        }
    }

    /**
     * Reads and validates file content.
     * @param filePath The file path to read
     * @return The file content, or null if validation fails
     */
    String readAndValidateFileContent(final Path filePath) {
        final String content = readFileWithRetry(filePath);
        if (content.trim()
                .isEmpty()) {
            logger.info("Skipping empty file: {}", filePath);
            return null;
        }
        return content;
    }



    String readFileWithRetry(final Path filePath) {
        return RetryUtils.retryWithExponentialBackoff(() -> Files.readString(filePath), "read file", filePath);
    }

    /**
     * Processes tokens and updates the index accordingly.
     * @param filePath The file path being processed
     * @param tokenInfos The token information to process
     */
    void processTokensAndUpdateIndex(final Path filePath, final List<Token> tokenInfos) {
        if (indexedFiles.contains(filePath)) {
            // File already indexed - update terms with line information
            index.updateTermsForFile(tokenInfos, filePath);
            logger.info("Updated existing file in index: {}", filePath);
        } else {
            // New file - add terms with line information
            index.addTermsWithLineInfo(tokenInfos, filePath);
            logger.info("Added new file to index: {}", filePath);
        }
    }

    /**
     * Creates and adds file metadata to the index.
     * @param filePath The file path
     * @param tokenInfos The token information for metadata calculation
     */
    void createAndAddFileMetadata(final Path filePath, final List<Token> tokenInfos) {
        final BasicFileAttributes attrs = readAttributesWithRetry(filePath);
        if (attrs == null) {
            logger.error("Failed to read file attributes for metadata: {}", filePath);
            return;
        }
        // Extract terms for metadata
        final List<String> terms = tokenInfos.stream()
                .map(Token::getValue)
                .toList();

        final FileMetadata metadata = new FileMetadata(filePath, attrs.size(), attrs.lastModifiedTime()
                .toInstant(),
            terms.size(), (int) terms.stream()
                    .distinct()
                    .count());

        index.addFileMetadata(metadata);

    }

    BasicFileAttributes readAttributesWithRetry(final Path filePath) {
        return RetryUtils.retryWithExponentialBackoff(() -> Files.readAttributes(filePath, BasicFileAttributes.class),
            "read file attributes", filePath);
    }

    /**
     * Indexes multiple files and returns count of successful operations.
     * @param filePaths list of files to index
     * @return number of successfully indexed files
     */
    int indexFiles(final List<Path> filePaths) {
        Objects.requireNonNull(filePaths, "File paths cannot be null");

        logger.info("Starting batch indexing of {} files", filePaths.size());

        int successCount = 0;
        for (final Path filePath : filePaths) {
            if (indexFile(filePath)) {
                successCount++;
            }
        }

        logger.info("Batch indexing completed: {}/{} files indexed successfully", successCount, filePaths.size());
        return successCount;
    }

    /**
     * Recursively indexes all supported files in directory.
     * @param directoryPath the directory to index
     * @return number of successfully indexed files
     */
    @Override
    public int indexAllFilesInDirectory(final Path directoryPath) {
        Objects.requireNonNull(directoryPath, "Directory path cannot be null");

        if (!Files.exists(directoryPath)) {
            logger.warn("Directory does not exist: {}", directoryPath);
            return 0;
        }

        if (!Files.isDirectory(directoryPath)) {
            logger.warn("Path is not a directory: {}", directoryPath);
            return 0;
        }

        logger.info("Starting recursive directory indexing: {}", directoryPath);

        try {
            final List<Path> filesToIndex = new ArrayList<>();
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    if (isSupportedFile(file)) {
                        filesToIndex.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                    logger.warn("Failed to visit file: {} - {}", file, exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });

            logger.info("Found {} files to index in directory: {}", filesToIndex.size(), directoryPath);
            return indexFiles(filesToIndex);

        } catch (final IOException e) {
            logger.error("Failed to walk directory: {}", directoryPath, e);
            return 0;
        }
    }

    /**
     * Removes a file from index and returns success status.
     * @param filePath the file to remove
     * @return true if removal succeeded
     */
    @Override
    public boolean removeFile(final Path filePath) {
        if (filePath == null) {
            return false;
        }

        if (indexedFiles.remove(filePath)) {
            index.removeFile(filePath);
            logger.info("Removed file from index: {}", filePath);
            return true;
        }

        logger.info("File not found in index: {}", filePath);
        return false;
    }

    /**
     * Searches for all terms (AND search) and returns results.
     * @param terms list of terms to search for
     * @return list of search results
     */
    @Override
    public List<SearchResult> searchAll(final List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }

        logger.info("Searching for ALL terms: {}", terms);

        final PerformanceMonitor.SearchOperation op = performanceMonitor.startSearch();

        try {
            final Set<FileLocation> locations = index.findAllTerms(terms);
            final List<SearchResult> results = convertToSearchResults(locations, String.join(" AND ", terms));

            performanceMonitor.completeSearch(op);

            logger.info("Found {} results for ALL terms: {}", results.size(), terms);
            return results;

        } catch (final Exception e) {
            logger.error("Error during AND search for terms: {}", terms, e);
            return List.of();
        }
    }

    FileMetadata getFileMetadata(final Path filePath) {
        return filePath != null ? index.getFileMetadata(filePath) : null;
    }

    /**
     * Returns current performance metrics.
     * @return performance metrics object
     */
    @Override
    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMonitor.getMetrics();
    }

    /**
     * Check if a file type is supported for indexing using the FileFilter.
     */
    boolean isSupportedFile(final Path filePath) {
        return fileFilter.shouldIndex(filePath);
    }

    /**
     * Set the FileFilter for testing purposes.
     * @param fileFilter The FileFilter to use
     */
    void setFileFilter(final FileFilter fileFilter) {
        this.fileFilter = Objects.requireNonNull(fileFilter, "FileFilter cannot be null");
    }

    /**
     * Set the Index for testing purposes.
     * @param index The Index to use
     */
    void setIndex(final Index index) {
        this.index = Objects.requireNonNull(index, "Index cannot be null");
    }

    /**
     * Set the PerformanceMonitor for testing purposes.
     * @param performanceMonitor The PerformanceMonitor to use
     */
    void setPerformanceMonitor(final PerformanceMonitor performanceMonitor) {
        this.performanceMonitor = Objects.requireNonNull(performanceMonitor, "PerformanceMonitor cannot be null");
    }

    /**
     * Convert file locations to search results with relevance scoring.
     */
    List<SearchResult> convertToSearchResults(final Set<FileLocation> locations, final String searchTerm) {
        if (locations.isEmpty()) {
            return List.of();
        }

        // Group locations by file for better result presentation
        final Map<Path, List<FileLocation>> fileGroups = locations.stream()
                .collect(Collectors.groupingBy(FileLocation::filePath));

        final List<SearchResult> results = new ArrayList<>();

        for (final Map.Entry<Path, List<FileLocation>> entry : fileGroups.entrySet()) {
            final Path filePath = entry.getKey();
            final List<FileLocation> fileLocations = entry.getValue();

            // Create a result for each file (could be enhanced with line previews)
            for (final FileLocation location : fileLocations) {
                final FileMetadata metadata = getFileMetadata(filePath);
                final long fileSize = metadata != null ? metadata.getFileSize() : 0;

                final SearchResult result = new SearchResult(filePath, location.lineNumber(), searchTerm, fileSize);
                results.add(result);
            }
        }

        // Sort by relevance (highest first)
        results.sort(Collections.reverseOrder());
        return results;
    }

}
