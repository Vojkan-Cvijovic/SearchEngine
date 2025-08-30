package com.demo.searchengine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.demo.searchengine.config.SearchEngineConfig;
import com.demo.searchengine.core.SearchResult;
import com.demo.searchengine.service.TextIndexingService;
import com.demo.searchengine.watcher.FileSystemWatcher;

/**
 * Unit tests for SearchEngineDemo class. Tests actual class instantiation and method invocation with proper mocking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchEngineDemoTest {

    @Mock
    private TextIndexingService mockSearchService;

    @Mock
    private FileSystemWatcher mockFileWatcher;

    @Mock
    private Scanner scanner;

    @Mock
    private SearchEngineConfig config;

    @Mock
    SearchResult mockSearchResult;

    private SearchEngineDemo demo;

    @BeforeEach
    void setUp() throws Exception {
        // Create a real SearchEngineDemo instance
        demo = new SearchEngineDemo();

        // Use reflection to inject mocked dependencies
        injectMockedDependencies();
    }

    @AfterEach
    void tearDown() {
        if (demo != null) {
            demo.stopWatching();
        }
    }

    @Test
    void testStartWatching_WithValidDirectory() throws Exception {

        final Path watchDir = Path.of("/test/directory");
        doNothing().when(mockFileWatcher)
                .startWatching(eq(watchDir), eq(true));

        demo.startWatching(watchDir);

        verify(mockFileWatcher).startWatching(watchDir, true);
    }

    @Test
    void testStartWatching_WithException() throws Exception {

        final Path watchDir = Path.of("/test/directory");
        doThrow(new RuntimeException("Watch failed")).when(mockFileWatcher)
                .startWatching(eq(watchDir), eq(true));

        demo.startWatching(watchDir);

        verify(mockFileWatcher).startWatching(watchDir, true);
    }

    @Test
    void testStopWatching() {
        doNothing().when(mockFileWatcher)
                .stop();

        demo.stopWatching();

        verify(mockFileWatcher).stop();
    }

    @Test
    void testStopWatching_WithNullWatcher() throws Exception {
        final SearchEngineDemo demoWithNullWatcher = createDemoWithNullWatcher();

        assertDoesNotThrow(demoWithNullWatcher::stopWatching);
    }

    @Test
    void indexDirectory_withValidDirectory_IndexedSuccessfully() {

        final Path indexDir = Path.of("/test/indexDir");
        when(mockSearchService.indexAllFilesInDirectory(eq(indexDir))).thenReturn(5);

        demo.indexDirectory(indexDir);

        verify(mockSearchService).indexAllFilesInDirectory(indexDir);
    }

    @Test
    void indexDirectory_withValidDirectory_FailedToIndex() {

        final Path indexDir = Path.of("/test/indexDir");
        doThrow(new IllegalStateException("dummy")).when(mockSearchService)
                .indexAllFilesInDirectory(eq(indexDir));

        demo.indexDirectory(indexDir);

        verify(mockSearchService).indexAllFilesInDirectory(indexDir);
    }

    @Test
    void cleanup_callsStopWatching() {

        doNothing().when(mockFileWatcher)
                .close();
        doNothing().when(scanner)
                .close();

        demo.cleanup();

        verify(mockFileWatcher).close();
        verify(scanner).close();
    }

    @Test
    void searchForWords_SingleWordInput_CallsSearchService() {
        final String query = "test";
        final String fileName = "testfile.txt";
        final List<SearchResult> results = List.of(mockSearchResult);
        when(mockSearchResult.getFileName()).thenReturn(fileName);
        when(mockSearchResult.getLineNumber()).thenReturn(1);

        when(mockSearchService.searchAll(any())).thenReturn(results);

        demo.searchForWords(query);

        verify(mockSearchService).searchAll(any());
    }

    @Test
    void searchForWords_MultipleWordsInput_CallsSearchService() {
        final String query = "test test";
        final String fileName = "testfile.txt";
        final List<SearchResult> results = List.of(mockSearchResult);
        when(mockSearchResult.getFileName()).thenReturn(fileName);
        when(mockSearchResult.getLineNumber()).thenReturn(2);

        when(mockSearchService.searchAll(any())).thenReturn(results);

        demo.searchForWords(query);

        verify(mockSearchService).searchAll(any());
    }

    // Helper methods
    private void injectMockedDependencies() {
        demo.setConfig(config);
        demo.setFileWatcher(mockFileWatcher);
        demo.setSearchService(mockSearchService);
        demo.setScanner(scanner);
    }

    private SearchEngineDemo createDemoWithNullWatcher() throws Exception {
        final SearchEngineDemo demo = new SearchEngineDemo();

        // Use reflection to set null watcher
        final java.lang.reflect.Field fileWatcherField = SearchEngineDemo.class.getDeclaredField("fileWatcher");
        fileWatcherField.setAccessible(true);
        fileWatcherField.set(demo, null);

        return demo;
    }

}
