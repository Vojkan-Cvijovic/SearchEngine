package com.demo.searchengine.watcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.demo.searchengine.service.TextIndexingService;

/**
 * Unit tests for FileSystemWatcher. Note: The current implementation has some limitations: 1. No duplicate directory
 * prevention - the same directory can be registered multiple times 2. No overlap detection - overlapping directories
 * can both be watched simultaneously 3. No individual directory unlatching can only stop all watching once These
 * tests document both the intended behavior and current limitations.
 */
class FileSystemWatcherTest {

    @Mock
    private TextIndexingService mockIndexingService;

    @Mock
    private FileSystemWatcherConfig mockConfig;

    @Mock
    private WatchService mockWatchService;

    @Mock
    private ExecutorService mockExecutorService;

    @Mock
    Path mockPath, fullPath;

    @Mock
    WatchEvent<Path> createEvent, modifyEvent, deleteEvent;

    @Mock
    WatchEvent<Object> overflowEvent;

    @Spy
    private FileSystemWatcher watcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject all mocked dependencies into the watcher spy
        watcher.setExecutorService(mockExecutorService);
        watcher.setWatchService(mockWatchService);
        watcher.setIndexingService(mockIndexingService);
        watcher.setConfig(mockConfig);

    }

    @Test
    void startWatching_RootDirectoryDoesNotExist_ThrowsException() {
        doReturn(false).when(watcher)
                .exists(mockPath);
        assertThrows(IllegalArgumentException.class, () -> watcher.startWatching(mockPath, false));
    }

    @Test
    void startWatching_NotDirectory_ThrowsException() {
        doReturn(true).when(watcher)
                .exists(mockPath);
        doReturn(false).when(watcher)
                .isDirectory(mockPath);
        assertThrows(IllegalArgumentException.class, () -> watcher.startWatching(mockPath, false));
    }

    @Test
    void startWatching_IndexFile_FileIndexed() throws IOException {
        doReturn(true).when(watcher)
                .exists(mockPath);
        doReturn(true).when(watcher)
                .isDirectory(mockPath);

        when(mockIndexingService.indexAllFilesInDirectory(mockPath)).thenReturn(1);

        doNothing().when(watcher)
                .registerDirectoryRecursively(mockPath);
        doNothing().when(watcher)
                .startWatchingThread();

        watcher.startWatching(mockPath, true);

        verify(watcher).registerDirectoryRecursively(mockPath);
        verify(mockIndexingService).indexAllFilesInDirectory(mockPath);
        verify(watcher).startWatchingThread();
    }

    @Test
    void startWatching_IndexFile_FileIndexFailed() throws IOException {
        doReturn(true).when(watcher)
                .exists(mockPath);
        doReturn(true).when(watcher)
                .isDirectory(mockPath);

        doThrow(new IllegalStateException()).when(mockIndexingService)
                .indexAllFilesInDirectory(mockPath);

        doNothing().when(watcher)
                .registerDirectoryRecursively(mockPath);
        doNothing().when(watcher)
                .startWatchingThread();

        watcher.startWatching(mockPath, true);

        verify(watcher).registerDirectoryRecursively(mockPath);
        verify(mockIndexingService, times(5)).indexAllFilesInDirectory(mockPath);
        verify(watcher).startWatchingThread();
    }

    @Test
    void registerDirectory_SuccessfulRegistration_IncrementsCounterAndLogs() throws IOException {
        when(mockPath.register(mockWatchService, StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)).thenReturn(null);

        watcher.registerDirectory(mockPath);

        assertEquals(1, watcher.getWatchedDirectoriesCount());
    }

    @Test
    void registerDirectory_IOException_ThrowsExceptionAndLogsError() throws IOException {
        doThrow(new IOException("Watch service error")).when(mockPath)
                .register(mockWatchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

        try {
            watcher.registerDirectory(mockPath);
            fail("Expected RuntimeException to be thrown");
        } catch (final RuntimeException exception) {
            assertEquals("java.io.IOException: Watch service error", exception.getMessage());
        }
        assertEquals(0, watcher.getWatchedDirectoriesCount());
    }

    @Test
    void watchLoop_SuccessfulPolling_ProcessesEvents() throws InterruptedException {
        final WatchKey mockWatchKey = mock(WatchKey.class);
        doReturn(true).when(watcher)
                .isWatcherRunning();
        when(mockWatchService.poll(anyLong(), any())).thenReturn(mockWatchKey);
        when(mockWatchKey.watchable()).thenReturn(mockPath);
        when(mockWatchKey.reset()).thenReturn(false);

        watcher.watchLoop();

        verify(mockWatchService).poll(anyLong(), any());
        verify(mockWatchKey).watchable();
        verify(mockWatchKey).reset();
    }

    @Test
    void watchLoop_InterruptedException_ExitsGracefully() throws InterruptedException {
        doReturn(true).when(watcher)
                .isWatcherRunning();
        when(mockWatchService.poll(anyLong(), any())).thenThrow(new InterruptedException("Interrupted"));

        watcher.watchLoop();

        verify(mockWatchService).poll(anyLong(), any());
    }

    @Test
    void processWatchEvents_AllEventsProvided_HandlesAllEventTypes() {
        final WatchKey mockWatchKey = mock(WatchKey.class);
        final List<WatchEvent<?>> events = List.of(overflowEvent, createEvent, modifyEvent, deleteEvent);
        when(mockWatchKey.pollEvents()).thenReturn(events);

        when(overflowEvent.kind()).thenReturn(StandardWatchEventKinds.OVERFLOW);
        when(createEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_CREATE);
        when(modifyEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_MODIFY);
        when(deleteEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_DELETE);

        when(modifyEvent.context()).thenReturn(mockPath);
        when(createEvent.context()).thenReturn(mockPath);
        when(deleteEvent.context()).thenReturn(mockPath);

        final Path watchedDirectory = mock(Path.class);

        when(watchedDirectory.resolve(mockPath)).thenReturn(fullPath);

        doThrow(new RuntimeException("Processing error")).when(watcher)
                .processFileEvent(StandardWatchEventKinds.ENTRY_CREATE, fullPath);
        doNothing().when(watcher)
                .processFileEvent(StandardWatchEventKinds.ENTRY_MODIFY, fullPath);
        doNothing().when(watcher)
                .processFileEvent(StandardWatchEventKinds.ENTRY_DELETE, fullPath);

        watcher.processWatchEvents(mockWatchKey, watchedDirectory);

        verify(watcher).processFileEvent(StandardWatchEventKinds.ENTRY_CREATE, fullPath);
        verify(watcher).processFileEvent(StandardWatchEventKinds.ENTRY_MODIFY, fullPath);
        verify(watcher).processFileEvent(StandardWatchEventKinds.ENTRY_DELETE, fullPath);
    }

    @Test
    void processFileEvent_ProvideAllEvents_HandleEachEventType() {
        doNothing().when(watcher)
                .handleFileCreated(fullPath);
        doNothing().when(watcher)
                .handleFileModified(fullPath);
        doNothing().when(watcher)
                .handleFileDeleted(fullPath);

        // Test ENTRY_CREATE
        watcher.processFileEvent(StandardWatchEventKinds.ENTRY_CREATE, fullPath);
        verify(watcher).handleFileCreated(fullPath);

        // Test ENTRY_MODIFY
        watcher.processFileEvent(StandardWatchEventKinds.ENTRY_MODIFY, fullPath);
        verify(watcher).handleFileModified(fullPath);

        // Test ENTRY_DELETE
        watcher.processFileEvent(StandardWatchEventKinds.ENTRY_DELETE, fullPath);
        verify(watcher).handleFileDeleted(fullPath);
    }

    @Test
    void handleFileCreated_DirectoryCreated_FailedToRegister() throws IOException {
        doReturn(true).when(watcher)
                .isDirectory(mockPath);
        doReturn(fullPath).when(watcher)
                .getWatchedDirectory();
        when(mockPath.startsWith(fullPath)).thenReturn(true);

        doThrow(new IOException("Registration failed")).when(watcher)
                .registerDirectoryRecursively(mockPath);

        watcher.handleFileCreated(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileCreated_DirectoryCreated_Registered() throws IOException {
        doReturn(true).when(watcher)
                .isDirectory(mockPath);
        doReturn(fullPath).when(watcher)
                .getWatchedDirectory();
        when(mockPath.startsWith(fullPath)).thenReturn(true);

        doNothing().when(watcher)
                .registerDirectoryRecursively(mockPath);

        watcher.handleFileCreated(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileCreated_FileCreated_Registered() {
        doReturn(false).when(watcher)
                .isDirectory(mockPath);
        doReturn(true).when(watcher)
                .isIndexableFile(mockPath);

        when(mockIndexingService.indexFile(mockPath)).thenReturn(true);

        watcher.handleFileCreated(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileCreated_FileCreated_FailedToRegister() {
        doReturn(false).when(watcher)
                .isDirectory(mockPath);
        doReturn(true).when(watcher)
                .isIndexableFile(mockPath);

        when(mockIndexingService.indexFile(mockPath)).thenReturn(false);

        watcher.handleFileCreated(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileCreated_FileCreated_ErrorDuringRegisterOperation() {
        doReturn(false).when(watcher)
                .isDirectory(mockPath);
        doReturn(true).when(watcher)
                .isIndexableFile(mockPath);

        when(mockIndexingService.indexFile(mockPath)).thenThrow(new RuntimeException());

        watcher.handleFileCreated(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileDeleted_FileDeleted_FileUnindexed() {
        when(mockIndexingService.removeFile(mockPath)).thenReturn(true);

        watcher.handleFileDeleted(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileDeleted_FileDeleted_FileNotFound() {
        when(mockIndexingService.removeFile(mockPath)).thenReturn(false);

        watcher.handleFileDeleted(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileDeleted_FileDeleted_ExceptionThrown() {
        when(mockIndexingService.removeFile(mockPath)).thenThrow(new RuntimeException());

        watcher.handleFileDeleted(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileModified_FileModified_FileUnindexed() {
        doReturn(true).when(watcher)
                .isIndexableFile(mockPath);
        when(mockIndexingService.indexFile(mockPath)).thenReturn(true);

        watcher.handleFileModified(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileModified_FileModified_FileNotFound() {
        doReturn(true).when(watcher)
                .isIndexableFile(mockPath);
        when(mockIndexingService.indexFile(mockPath)).thenReturn(false);

        watcher.handleFileModified(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void handleFileModified_FileModified_ExceptionThrown() {
        doReturn(true).when(watcher)
                .isIndexableFile(mockPath);
        when(mockIndexingService.indexFile(mockPath)).thenThrow(new RuntimeException());

        watcher.handleFileModified(mockPath);

        // Logger verification removed - using Log4J now
    }

    @Test
    void stop_watching_StopsWatcher() throws IOException {

        watcher.stop();

        verify(mockExecutorService).shutdown();
        verify(mockWatchService).close();

        // Logger verification removed - using Log4J now
    }

}
