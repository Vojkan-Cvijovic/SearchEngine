package com.demo.searchengine.util;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class providing retry mechanisms with exponential backoff for various operations. Supports operations that
 * may throw IOException and other exceptions.
 */
public final class RetryUtils {

    private static final Logger logger = LogManager.getLogger(RetryUtils.class);

    // Default retry configuration
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long DEFAULT_MAX_TIMEOUT_MS = 1000; // 1 second
    private static final long DEFAULT_BASE_DELAY_MS = 100;

    private RetryUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Functional interface for operations that may throw IOException.
     */
    @FunctionalInterface
    public interface SupplierWithIOException<T> {
        T get() throws IOException;
    }

    /**
     * Functional interface for operations that may throw Exception.
     */
    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }

    /**
     * Retry operation with exponential backoff for operations that throw IOException.
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging
     * @param filePath File path for logging context
     * @param <T> Return type of the operation
     * @return Result of the operation or null if all retries fail
     */
    public static <T> T retryWithExponentialBackoff(final SupplierWithIOException<T> operation,
        final String operationName, final Path filePath) {
        return retryFileOperation(operation, operationName, filePath);
    }

    /**
     * Retry operation with exponential backoff for operations that throw Exception.
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging
     * @param context Context for logging (e.g., file path, directory name)
     * @param <T> Return type of the operation
     * @return Result of the operation or null if all retries fail
     */
    public static <T> T retryWithExponentialBackoff(final SupplierWithException<T> operation,
        final String operationName, final String context, final boolean failOnMaxRetries) {
        return retryWithExponentialBackoff(operation, operationName, context, DEFAULT_MAX_RETRIES,
            DEFAULT_MAX_TIMEOUT_MS, DEFAULT_BASE_DELAY_MS, failOnMaxRetries);
    }

    /**
     * Retry operation with exponential backoff for operations that throw IOException.
     * @param <T> Return type of the operation
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging
     * @param filePath File path for logging context
     * @return Result of the operation or null if all retries fail
     */
    static <T> T retryFileOperation(final SupplierWithIOException<T> operation, final String operationName,
        final Path filePath) {

        int attempts = 0;
        long currentDelay = RetryUtils.DEFAULT_BASE_DELAY_MS;

        while (true) {
            try {
                return operation.get();
            } catch (final IOException e) {
                attempts++;
                if (attempts >= RetryUtils.DEFAULT_MAX_RETRIES) {
                    logger.error("Failed to {} after {} attempts: {}", operationName, RetryUtils.DEFAULT_MAX_RETRIES,
                        filePath, e);
                    return null;
                }

                // Calculate delay with exponential backoff, capped at maxTimeoutMs
                final long delay = currentDelay;
                currentDelay = Math.min(currentDelay * 2, RetryUtils.DEFAULT_MAX_TIMEOUT_MS);

                logger.warn("Failed to {} (attempt {}/{}), retrying in {}ms: {}", operationName, attempts,
                    RetryUtils.DEFAULT_MAX_RETRIES, delay, filePath);

                try {
                    Thread.sleep(delay);
                } catch (final InterruptedException ie) {
                    Thread.currentThread()
                            .interrupt();
                    logger.error("Interrupted during {} retry: {}", operationName, filePath, ie);
                    return null;
                }
            }
        }
    }

    /**
     * Retry operation with exponential backoff for operations that throw Exception.
     * @param operation The operation to retry
     * @param operationName Name of the operation for logging
     * @param context Context for logging (e.g., file path, directory name)
     * @param maxRetries Maximum number of retry attempts
     * @param maxTimeoutMs Maximum total timeout in milliseconds
     * @param baseDelayMs Base delay in milliseconds
     * @param <T> Return type of the operation
     * @return Result of the operation or null if all retries fail
     */
    public static <T> T retryWithExponentialBackoff(final SupplierWithException<T> operation,
        final String operationName, final String context, final int maxRetries, final long maxTimeoutMs,
        final long baseDelayMs, final boolean failOnMaxRetries) {

        int attempts = 0;
        long currentDelay = baseDelayMs;

        while (true) {
            try {
                return operation.get();
            } catch (final Exception e) {
                attempts++;
                if (attempts >= maxRetries) {
                    logger.error("Failed to {} after {} attempts: {}", operationName, maxRetries, context, e);
                    if (failOnMaxRetries) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }

                // Calculate delay with exponential backoff, capped at maxTimeoutMs
                final long delay = Math.min(currentDelay, maxTimeoutMs);
                currentDelay = Math.min(currentDelay * 2, maxTimeoutMs);

                logger.warn("Failed to {} (attempt {}/{}), retrying in {}ms: {}", operationName, attempts, maxRetries,
                    delay, context);

                try {
                    Thread.sleep(delay);
                } catch (final InterruptedException ie) {
                    Thread.currentThread()
                            .interrupt();
                    logger.error("Interrupted during {} retry: {}", operationName, context, ie);
                    return null;
                }
            }
        }
    }

}
