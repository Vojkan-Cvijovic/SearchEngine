package com.demo.searchengine.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Performance monitors that track and logs performance metrics. Integrates with the logging system to provide
 * human-readable performance information.
 */
public class PerformanceMonitor {
    private static final Logger logger = LogManager.getLogger(PerformanceMonitor.class);

    private final PerformanceMetrics metrics;
    private final AtomicInteger activeOperations;

    /**
     * Creates new performance monitor.
     */
    public PerformanceMonitor() {
        this.metrics = new PerformanceMetrics();
        this.activeOperations = new AtomicInteger(0);
    }

    /**
     * Start monitoring an indexing operation.
     * @return Operation context for tracking
     */
    public IndexingOperation startIndexing() {
        activeOperations.incrementAndGet();
        return new IndexingOperation(Instant.now());
    }

    /**
     * Complete an indexing operation and record metrics.
     */
    public void completeIndexing(final IndexingOperation operation) {
        final long durationMs = Duration.between(operation.startTime, Instant.now())
                .toMillis();
        metrics.recordFileIndexed(durationMs);
        activeOperations.decrementAndGet();

        // Log performance information
        logger.info("PERF: File Indexing completed in {}ms - Total files: {}, Avg time: {}ms", durationMs,
            metrics.getTotalFilesIndexed(), metrics.getAverageIndexingTime()
                    .toMillis());

        // Log memory usage
        final long currentMemory = metrics.getCurrentMemoryUsage();
        final long maxMemory = metrics.getMaxMemory();
        logger.info("MEMORY: After Indexing - Used: {}MB, Total: {}MB", currentMemory / (1024 * 1024),
            maxMemory / (1024 * 1024));

        // Log warnings for slow operations
        if (durationMs > 1000) {
            logger.warn("Slow indexing operation: {}}ms", durationMs);
        }
    }

    /**
     * Start monitoring a search operation.
     * @return Operation context for tracking
     */
    public SearchOperation startSearch() {
        activeOperations.incrementAndGet();
        return new SearchOperation(Instant.now());
    }

    /**
     * Complete a search operation and record metrics.
     */
    public void completeSearch(final SearchOperation operation) {
        final long durationMs = Duration.between(operation.startTime, Instant.now())
                .toMillis();
        metrics.recordSearchQuery(durationMs);
        activeOperations.decrementAndGet();

        // Log performance information
        logger.info("PERF: Search Query completed in {}ms - Total queries: {}, Avg time: {}ms", durationMs,
            metrics.getTotalSearchQueries(), metrics.getAverageSearchTime()
                    .toMillis());

        // Log warnings for slow operations
        if (durationMs > 100) {
            logger.warn("Slow search operation: {}ms", durationMs);
        }
    }

    /**
     * Get current performance metrics.
     */
    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    // Operation context classes
    public static class IndexingOperation {
        final Instant startTime;

        IndexingOperation(final Instant startTime) {
            this.startTime = startTime;
        }
    }

    public static class SearchOperation {
        final Instant startTime;

        SearchOperation(final Instant startTime) {
            this.startTime = startTime;
        }
    }
}
