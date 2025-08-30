# Concurrency and Filesystem Watching - Search Engine Library

A simple guide explaining how the Search Engine library handles concurrent access and filesystem change detection.

## 🎯 Overview

The Search Engine library addresses two key requirements:

1. **Concurrent Access Support** - Multiple users can access the service simultaneously
2. **Filesystem Change Detection** - Automatically reacts to changes in watched directories

## 🔒 1. Concurrent Access Support

### Thread Safety Implementation

The library uses **ReadWriteLock** to protect critical data structures during updates.

#### Core Protection Strategy

```java
public class ThreadSafeIndex implements Index {
    // Read-write lock for protecting critical data structure updates
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Regular HashMap (not ConcurrentHashMap) - protected by locks
    private final Map<String, Set<FileLocation>> termToLocations;
    private final Map<Path, FileMetadata> fileMetadata;
}
```

#### How It Works

- **Read Operations** (Search): Use shared lock - multiple threads can search simultaneously
- **Write Operations** (Indexing): Use exclusive lock - only one thread can update the index at a time
- **Data Consistency**: All compound operations are atomic and protected by locks

#### Simple Example

```java
// Multiple threads can search at the same time
public Set<FileLocation> findTerm(String term) {
    lock.readLock().lock(); // Shared lock - multiple readers allowed
    try {
        return termToLocations.getOrDefault(term, Set.of());
    } finally {
        lock.readLock().unlock();
    }
}

// Only one thread can update the index at a time
public void addTermsWithLineInfo(List<Token> tokens, Path filePath) {
    lock.writeLock().lock(); // Exclusive lock - only one writer
    try {
        // Update data structures safely
        // ... indexing logic
    } finally {
        lock.writeLock().unlock();
    }
}
```

### Thread-Safe Components

- **ThreadSafeIndex**: Main index with ReadWriteLock protection
- **PerformanceMetrics**: Uses AtomicInteger for thread-safe counters
- **FileSystemWatcher**: Dedicated thread pool for watching operations

## 👀 2. Filesystem Change Detection

### How It Works

The library uses Java's `WatchService` API to monitor directory changes in real-time.

#### Basic Setup

```java
public class FileSystemWatcher {
    private final WatchService watchService;
    private final ExecutorService executorService;
    
    public FileSystemWatcher() {
        // Create WatchService for filesystem monitoring
        this.watchService = FileSystems.getDefault().newWatchService();
        // Dedicated thread pool for file watching
        this.executorService = Executors.newFixedThreadPool(4);
    }
}
```

#### Directory Registration

```java
public void startWatching(Path directory, boolean recursive) {
    // Register directory with WatchService
    WatchKey key = directory.register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,  // New files
        StandardWatchEventKinds.ENTRY_DELETE,  // Deleted files
        StandardWatchEventKinds.ENTRY_MODIFY   // Modified files
    );
    
    if (recursive) {
        // Also watch subdirectories
        registerSubdirectories(directory);
    }
}
```

#### Event Processing

```java
private void processWatchEvents() {
    while (running) {
        WatchKey key = watchService.take(); // Wait for events
        
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            Path fileName = ((WatchEvent<Path>) event).context();
            Path fullPath = dir.resolve(fileName);
            
            // Handle based on event type
            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                indexingService.indexFile(fullPath); // Index new file
            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                indexingService.removeFile(fullPath); // Remove from index
            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                indexingService.removeFile(fullPath); // Remove old version
                indexingService.indexFile(fullPath);  // Index new version
            }
        }
        
        key.reset(); // Continue watching
    }
}
```

## 🔄 3. How They Work Together

### Non-Blocking Operation

- **Filesystem watching** runs in background threads
- **Search operations** can continue while files are being indexed
- **Index updates** are protected by locks to prevent data corruption

### Simple Flow

1. **File changes** are detected by WatchService
2. **Background thread** processes the change (index/remove file)
3. **Index updates** are protected by ReadWriteLock
4. **Search operations** continue normally during updates

## 📊 4. Performance Characteristics

### Concurrent Access
- **Multiple searches**: Can run simultaneously (shared read lock)
- **Indexing**: One at a time (exclusive write lock)
- **Memory**: Efficient with regular HashMap + lock protection

### Filesystem Watching
- **Event latency**: Typically < 100ms
- **CPU usage**: Minimal overhead
- **Threads**: Dedicated pool (4 threads by default)

## 🎉 Summary

The library provides:

1. **Thread Safety**: ReadWriteLock protects data during updates
2. **Concurrent Search**: Multiple users can search simultaneously
3. **Real-Time Monitoring**: Automatic file change detection
4. **Non-Blocking**: Search continues during indexing operations

**Key Implementation Details:**
- Uses `ReadWriteLock` (not ConcurrentHashMap)
- Regular `HashMap` protected by locks
- `WatchService` for filesystem monitoring
- Dedicated thread pools for background operations

For implementation details, see:
- `src/main/java/com/demo/searchengine/core/impl/ThreadSafeIndex.java`
- `src/main/java/com/demo/searchengine/watcher/FileSystemWatcher.java`
- `src/main/java/com/demo/searchengine/service/SimpleTextIndexingService.java`
