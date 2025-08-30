package com.demo.searchengine.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchEngineConfigTest {

    @Mock
    private Path mockConfigFile;

    @Mock
    private Path mockIndexDir;

    @Mock
    private Path mockWatchDir;

    @Mock
    private Properties mockProperties;

    @Spy
    private SearchEngineConfig configSpy;

    @BeforeEach
    void setUp() {
        configSpy = spy(new SearchEngineConfig());

        configSpy.setProperties(mockProperties);
        configSpy.setIndexDirectory(mockIndexDir);
        configSpy.setWatchDirectory(mockWatchDir);
    }

    @Test
    void loadProperties_ConfigFileDoesNotExist_ThrowsIOException() {
        doReturn(false).when(configSpy)
                .fileExists(mockConfigFile);

        try {
            configSpy.loadProperties(mockConfigFile);
            fail("Expected IOException was not thrown");
        } catch (final IOException e) {
            verify(mockConfigFile).toAbsolutePath();
        }
    }

    @Test
    void loadProperties_ConfigFileIsNotReadable_ThrowsIOException() {
        doReturn(true).when(configSpy)
                .fileExists(mockConfigFile);
        doReturn(false).when(configSpy)
                .isFileReadable(mockConfigFile);

        try {
            configSpy.loadProperties(mockConfigFile);
            fail("Expected IOException was not thrown");
        } catch (final IOException e) {
            verify(mockConfigFile).toAbsolutePath();
        }
    }

    @Test
    void loadProperties_ConfigFileFailedToRead_ThrowsIOException() throws IOException {
        doReturn(true).when(configSpy)
                .fileExists(mockConfigFile);
        doReturn(false).when(configSpy)
                .isFileReadable(mockConfigFile);

        doThrow(new IOException()).when(configSpy)
                .readFile(mockConfigFile);

        try {

            configSpy.loadProperties(mockConfigFile);
            fail("Expected IOException was not thrown");
        } catch (final IOException e) {
            verify(mockConfigFile).toAbsolutePath();
        }
    }

    @Test
    void loadProperties_ConfigFileRead_ThrowsIOException() throws IOException {
        doReturn(true).when(configSpy)
                .fileExists(mockConfigFile);
        doReturn(true).when(configSpy)
                .isFileReadable(mockConfigFile);

        final Properties properties = mock(Properties.class);
        final InputStream inputStream = mock(InputStream.class);

        doReturn(inputStream).when(configSpy)
                .readFile(mockConfigFile);
        doReturn(properties).when(configSpy)
                .createProperties();
        doNothing().when(properties)
                .load(inputStream);

        configSpy.loadProperties(mockConfigFile);
        verify(mockConfigFile).toAbsolutePath();
    }

    @Test
    void resolveIndexDirectory_IndexDirPropertyMissing_ThrowsException() {
        doReturn(null).when(mockProperties)
                .getProperty("index.directory");

        try {
            configSpy.resolveIndexDirectory();
            fail("Expected IOException was not thrown");
        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    void resolveIndexDirectory_InvalidIndexDirectoryPath_ThrowsException() {
        final String invalidIndexDirectoryPath = "invalidIndexDirectoryPath";
        doReturn(invalidIndexDirectoryPath).when(mockProperties)
                .getProperty("index.directory");
        final Path path = mock(Path.class);
        doReturn(path).when(configSpy)
                .getPath(invalidIndexDirectoryPath);
        doReturn(false).when(configSpy)
                .isValidDirectoryPath(path);
        try {
            configSpy.resolveIndexDirectory();
            fail("Expected IOException was not thrown");
        } catch (final IllegalArgumentException ignored) {
        }
        verify(configSpy).isValidDirectoryPath(path);
    }

    @Test
    void resolveIndexDirectory_ValidIndexDirectoryPath_ReturnsPath() {
        final String invalidIndexDirectoryPath = "invalidIndexDirectoryPath";
        doReturn(invalidIndexDirectoryPath).when(mockProperties)
                .getProperty("index.directory");
        final Path path = mock(Path.class);
        doReturn(path).when(configSpy)
                .getPath(invalidIndexDirectoryPath);
        doReturn(true).when(configSpy)
                .isValidDirectoryPath(path);

        final Path actualPath = configSpy.resolveIndexDirectory();

        verify(configSpy).isValidDirectoryPath(path);

        assertSame(path, actualPath);
    }

    @Test
    void resolveWatchDirectory_WatchDirPropertyMissing_ThrowsException() {
        doReturn(null).when(mockProperties)
                .getProperty("watch.directory");

        try {
            configSpy.resolveWatchDirectory();
            fail("Expected IOException was not thrown");
        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    void resolveWatchDirectory_InvalidWatchDirectoryPath_ThrowsException() {
        final String invalidWatchDirectoryPath = "invalidWatchDirectoryPath";
        doReturn(invalidWatchDirectoryPath).when(mockProperties)
                .getProperty("watch.directory");
        final Path path = mock(Path.class);
        doReturn(path).when(configSpy)
                .getPath(invalidWatchDirectoryPath);
        doReturn(false).when(configSpy)
                .isValidDirectoryPath(path);
        try {
            configSpy.resolveWatchDirectory();
            fail("Expected IOException was not thrown");
        } catch (final IllegalArgumentException ignored) {
        }
        verify(configSpy).isValidDirectoryPath(path);
    }

    @Test
    void resolveWatchDirectory_ValidWatchedDirectoryPath_ReturnsPath() {
        final String invalidWatchDirectoryPath = "invalidWatchDirectoryPath";
        doReturn(invalidWatchDirectoryPath).when(mockProperties)
                .getProperty("watch.directory");
        final Path path = mock(Path.class);
        doReturn(path).when(configSpy)
                .getPath(invalidWatchDirectoryPath);
        doReturn(true).when(configSpy)
                .isValidDirectoryPath(path);

        final Path actualPath = configSpy.resolveWatchDirectory();

        verify(configSpy).isValidDirectoryPath(path);

        assertSame(path, actualPath);
    }

    @Test
    void isValidDirectoryPath_NullPath_ReturnsFalse() {
        final boolean result = configSpy.isValidDirectoryPath(null);
        assertSame(false, result);
    }

    @Test
    void isValidDirectoryPath_EmptyPath_ReturnsFalse() {
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("");
        final boolean result = configSpy.isValidDirectoryPath(path);
        assertSame(false, result);
    }

    @Test
    void isValidDirectoryPath_RelativePath_ReturnsFalse() {
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("../some/relative/path");
        final boolean result = configSpy.isValidDirectoryPath(path);
        assertSame(false, result);
    }

    @Test
    void isValidDirectoryPath_InvalidPath_ReturnsFalse() {
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("../some/relative/path\n");
        final boolean result = configSpy.isValidDirectoryPath(path);
        assertSame(false, result);
    }

    @Test
    void isValidDirectoryPath_ValidPath_ReturnsFalse() {
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("/root/some/absolute/path");
        final boolean result = configSpy.isValidDirectoryPath(path);
        assertSame(true, result);
    }

    @Test
    void ensureDirectoriesExist_IndexDirectoryDoesNotExist_ExceptionThrown() {
        doReturn(false).when(configSpy)
                .directoryExists(mockIndexDir);

        try {
            configSpy.ensureDirectoriesExist();
            fail("Expected IllegalArgumentException was not thrown");
        } catch (final IllegalArgumentException ignored) {
        }

        verify(configSpy).directoryExists(mockIndexDir);
    }

    @Test
    void ensureDirectoriesExist_WatchDirectoryDoesNotExist_ExceptionThrown() {
        doReturn(true).when(configSpy)
                .directoryExists(mockIndexDir);
        doReturn(false).when(configSpy)
                .directoryExists(mockWatchDir);

        try {
            configSpy.ensureDirectoriesExist();
            fail("Expected IllegalArgumentException was not thrown");
        } catch (final IllegalArgumentException ignored) {
        }

        verify(configSpy).directoryExists(mockIndexDir);
        verify(configSpy).directoryExists(mockWatchDir);
    }
}
