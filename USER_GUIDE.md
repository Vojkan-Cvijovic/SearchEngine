# Search Engine - User Guide

Complete guide for building, configuring, and using the Search Engine text indexing library.

## 📋 Table of Contents

1. [Requirements](#requirements)
2. [Building the Project](#building-the-project)
3. [Configuration](#configuration)
4. [Basic Usage](#basic-usage)
5. [Advanced Usage](#advanced-usage)
6. [Performance Tuning](#performance-tuning)
7. [Troubleshooting](#troubleshooting)
8. [Examples](#examples)

## 🔧 Requirements

### System Requirements
- **Java**: Version 21 or higher
- **Maven**: Version 3.6 or higher
- **Operating System**: Windows, macOS, or Linux
- **Memory**: Minimum 512MB RAM, recommended 2GB+
- **Disk Space**: At least 100MB free space

### Java Installation
```bash
# Check Java version
java -version

# Should show Java 21 or higher
# java version "21.0.8" 2025-07-15 LTS
```

### Maven Installation
```bash
# Check Maven version
mvn -version

# Should show Maven 3.6 or higher
# Apache Maven 3.9.1
```

## 🏗️ Building the Project

### 1. Setup
```bash
# Verify Java version
export JAVA_HOME=/path/to/java21
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Build Commands
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Build JAR file
mvn clean package

# Install to local repository
mvn clean install
```

### 3. Build Output
After a successful build, you'll find:
- **Compiled classes**: `target/classes/`
- **Test classes**: `target/test-classes/`
- **JAR file**: `target/SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar`
- **Test reports**: `target/surefire-reports/`

### 4. Troubleshooting Build Issues
```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Skip tests during build
mvn clean package -DskipTests

# Debug Maven issues
mvn clean package -X
```

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test categories
mvn test -Dtest=*Test

# Run integration tests
mvn test -Dtest=SearchEngineIntegrationTest

# Run performance tests
mvn test -Dtest=PerformanceTest

# Run specific test methods
mvn test -Dtest=SearchEngineIntegrationTest#testBasicFileIndexingAndSearch
```

### Test Categories
- **Unit Tests**: Individual component testing
- **Integration Tests**: End-to-end workflow testing with real filesystem operations
- **Performance Tests**: Load testing and scalability validation

### Test Reports
After running tests, view reports in:
- **Surefire reports**: `target/surefire-reports/`
- **Test results**: `target/test-classes/`

## ⚙️ Configuration

### Configuration File
Update `searchengine.properties` in your project directory:

```properties
# Directory Configuration, use absolute paths
index.directory=/path/to/your/index
watch.directory=/path/to/your/watch
```

### Directory Setup
```bash
# Create required directories
mkdir -p /path/to/your/index
mkdir -p /path/to/your/watch

# Set permissions (Unix/Linux/macOS)
chmod 755 /path/to/your/index
chmod 755 /path/to/your/watch
```

### Environment Variables
```bash
# Set Java options
export JAVA_OPTS="-Xmx2g -Xms512m"

# Set Maven options (if needed)
export MAVEN_OPTS="-Xmx1g"
```

## **Running the Application**

### **Prerequisites**
- Java 21 (JDK) installed and configured
- Maven 3.6+ for building the project
- A configuration properties file (recommended)

### **Build the Project**
```bash
# Navigate to project directory
cd SearchEngine

# Clean and build the project
mvn clean package
```

This will create two JAR files:
- `target/SearchEngine-1.0-SNAPSHOT.jar` - Basic JAR (without dependencies)
- `target/SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar` - **Recommended** (includes all dependencies)

### **Run the Application**
```bash
# Run with properties file (RECOMMENDED)
java -jar target/SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar ./searchengine.properties

# Run without properties file (uses default directories)
java -jar target/SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Important Notes**:
- Use the `-jar-with-dependencies` JAR file to avoid dependency issues
- The properties file is required for proper configuration
- Without properties, the application will use default directories in your home folder

## 🚀 Basic Usage

### 1. Running the Demo
```bash
# Run with custom configuration (required)
java -jar target/SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar ./searchengine.properties

# Note: Configuration file is required - the demo cannot run without it
```

### 2. Programmatic Usage

```java
import service.com.demo.searchengine.SimpleTextIndexingService;
import service.com.demo.searchengine.TextIndexingService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

// Create service
TextIndexingService service = new SimpleTextIndexingService();

        // Index a single file
        Path file = Paths.get("document.txt");
        boolean success = service.indexFile(file);

        // Index an entire directory
        Path directory = Paths.get("documents/");
service.

        indexDirectory(directory);

        // Search for terms
        List<SearchResult> results = service.searchAll(Arrays.asList("java programming"));

        // Search with multiple terms (AND search)
        List<SearchResult> results = service.searchAll(Arrays.asList("java", "programming"));

        // Get performance metrics
        PerformanceMetrics metrics = service.getPerformanceMetrics();
System.out.

        println("Health: "+metrics.getHealthSummary());
```

### 3. Interactive Demo Commands
When running the demo program:
```
Available commands:
- Type search terms to search the index
- Multiple words will be searched as AND (all must be present)
- Type 'quit' or 'exit' to exit the program

Example:
Search for words (or 'quit' to exit): java
Search for words (or 'quit' to exit): java programming
Search for words (or 'quit' to exit): quit
```

**Note**: The demo automatically indexes the configured directory and watches for file changes. It only provides a search interface, not manual indexing commands.

## 🔧 Advanced Usage

### 1. Custom Tokenizer

```java
import tokenizer.com.demo.searchengine.Tokenizer;
import impl.tokenizer.com.demo.searchengine.SimpleWordTokenizer;

// Create custom tokenizer
Tokenizer tokenizer = new SimpleWordTokenizer("CustomTokenizer", true, 3);

        // Use with service
        TextIndexingService service = new SimpleTextIndexingService(tokenizer);
```

### 2. File System Watching

```java
import watcher.com.demo.searchengine.FileSystemWatcher;
import watcher.com.demo.searchengine.FileSystemWatcherConfig;

// Configure watcher
FileSystemWatcherConfig config = FileSystemWatcherConfig.builder()
        .watchRecursively(true)
        .pollingInterval(1000)
        .build();

        // Create and start watcher
        FileSystemWatcher watcher = new FileSystemWatcher(service, config);
watcher.

        startWatching(Paths.get("/path/to/watch"));

// Stop watching
        watcher.

        stopWatching();
```

### 3. Performance Monitoring

```java
import performance.com.demo.searchengine.PerformanceMonitor;

// Get a performance monitor
PerformanceMonitor monitor = service.getPerformanceMonitor();

        // Track indexing operation
        PerformanceMonitor.IndexingOperation op = monitor.startIndexing();
// ... perform indexing ...
monitor.

        completeIndexing(op);

        // Track search operation
        PerformanceMonitor.SearchOperation searchOp = monitor.startSearch();
// ... perform search ...
monitor.

        completeSearch(searchOp);

// Log system status
monitor.

        logSystemStatus();
monitor.

        logMemoryStatus();
```

### 4. File Filtering

```java
import util.com.demo.searchengine.FileFilter;

// Create custom file filter
FileFilter filter = FileFilter.builder()
        .maxFileSize(5 * 1024 * 1024)  // 5MB
        .supportedExtensions(".txt", ".md", ".java")
        .excludePatterns("*.tmp", "*.bak")
        .build();

// Use with service (requires setter method)
// service.setFileFilter(filter);
```

## 📁 File Type Support

### Default Supported Extensions
The search engine automatically indexes these file types out of the box:

#### Text Files
- **`.txt`** - Plain text files, documents, logs
- **`.md`** - Markdown documents, README files, documentation

#### Source Code
- **`.java`** - Java source code files
- **`.py`** - Python scripts and modules
- **`.js`** - JavaScript files
- **`.go`** - Go source code files
- **`.sh`** - Shell scripts (bash, sh, etc.)

#### Data Files
- **`.xml`** - XML documents, configuration files
- **`.json`** - JSON data files, configuration files

### Customizing File Types
You can configure which file types to watch and index by creating a custom `FileSystemWatcherConfig`:

```java
import com.demo.searchengine.watcher.FileSystemWatcherConfig;

// Create custom configuration with specific file types
FileSystemWatcherConfig customConfig = FileSystemWatcherConfig.builder()
    .supportedExtensions(".txt", ".md", ".java", ".custom")
    .build();

// Use with FileSystemWatcher
FileSystemWatcher watcher = new FileSystemWatcher(service, customConfig);
```

### File Type Detection
The system determines file types by:
1. **File Extension**: Primary method - checks if filename ends with supported extension
2. **Case Insensitive**: `.TXT`, `.txt`, and `.Txt` all work
3. **Exact Match**: Only files with exact extension matches are indexed



### Adding New File Types
To support additional file types, you need to modify the code:

1. **Update FileSystemWatcherConfig**: Modify the supported extensions in the configuration
2. **Restart Watcher**: New configuration takes effect after restart
3. **Re-index**: Existing files of new types won't be automatically indexed

```java
// Example: Add support for more programming languages
// This requires code changes in FileSystemWatcherConfig.java
Set<String> extensions = new HashSet<>(Arrays.asList(
    ".txt", ".md", ".java", ".py", ".js", ".go", ".sh", ".xml", ".json",
    ".cpp", ".c", ".h", ".cs", ".php", ".rb", ".scala", ".kt"
));
```

**Note**: Currently, file type configuration is hardcoded and requires code changes. Future versions may support configuration via properties files.

## 📊 Performance Tuning

### 1. Indexing Performance
```java
// Batch processing for large directories
Path[] files = getFilesToIndex();
for (Path file : files) {
    service.indexFile(file);
}

// Monitor performance
PerformanceMetrics metrics = service.getPerformanceMetrics();
System.out.println("Files indexed: " + metrics.getTotalFilesIndexed());
System.out.println("Avg indexing time: " + metrics.getAverageIndexingTime());
```
### 2. Search Performance
```java
// Use specific search terms
List<SearchResult> results = service.search("specific term");

// Avoid very short search terms (less than 2 characters)
// The system automatically filters these out
```

### 3. File System Watching Performance
```java
// Adjust polling interval for large directories
FileSystemWatcherConfig config = FileSystemWatcherConfig.builder()
    .pollingInterval(2000)  // 2 seconds for large directories
    .watchRecursively(false)  // Disable for very deep structures
    .build();
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Java Version Problems
```bash
# Error: invalid target release: 21
# Solution: Ensure Java 21 is installed and JAVA_HOME is set
export JAVA_HOME=/path/to/java21
export PATH=$JAVA_HOME/bin:$PATH
java -version
```

#### 2. Maven Build Issues
```bash
# Error: Could not create the Java Virtual Machine
# Solution: Clear Maven options that conflict with Java 21
unset MAVEN_OPTS
mvn clean compile
```

#### 3. Permission Denied
```bash
# Error: Permission denied when accessing directories
# Solution: Check directory permissions
ls -la /path/to/directory
chmod 755 /path/to/directory
```

#### 4. Out of Memory
```bash
# Error: java.lang.OutOfMemoryError
# Solution: Increase JVM memory
export JAVA_OPTS="-Xmx4g -Xms1g"
```

#### 5. File Not Found
```bash
# Error: Configuration file not found
# Solution: Ensure searchengine.properties exists and path is correct
ls -la searchengine.properties
pwd
```

#### 6. File Not Indexed
```bash
# Error: File exists but is not being indexed
# Solution: Check if file extension is supported
# Supported: .txt, .md, .java, .py, .js, .go, .sh, .xml, .json
# Not supported: .pdf, .doc, .exe, .bin

# Check file extension
ls -la yourfile.*

# Verify file is in watched directory
ls -la /path/to/watched/directory/
```

#### 7. Unsupported File Type
```bash
# Error: File type not supported for indexing
# Solution: Add custom file type support or convert file format

# Option 1: Convert to supported format
# Example: Convert .doc to .txt
# Option 2: Add custom extension support via configuration
```

### Debug Mode
```bash
# Enable debug logging
export JAVA_OPTS="$JAVA_OPTS -Dlog4j.configurationFile=log4j2-debug.xml"

# Run with verbose output
mvn clean package -X
```

### Log Files
Check log files for detailed error information:
- **Application logs**: `logs/searchengine.log`
- **Maven logs**: `target/maven.log`
- **Test logs**: `target/surefire-reports/`

## 📝 Examples

### 1. Complete Working Example~~~~

```java
import service.com.demo.searchengine.SimpleTextIndexingService;
import service.com.demo.searchengine.TextIndexingService;
import watcher.com.demo.searchengine.FileSystemWatcher;
import watcher.com.demo.searchengine.FileSystemWatcherConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchEngineExample {
    public static void main(String[] args) throws Exception {
        // Create service
        TextIndexingService service = new SimpleTextIndexingService();

        // Index a directory
        Path docsDir = Paths.get("documents/");
        service.indexAllFilesInDirectory(docsDir);

        // Search
        List<SearchResult> results = service.searchAll(Arrays.asList("example"));
        results.forEach(result -> {
            System.out.println("Found in: " + result.getFilePath());
            System.out.println("Relevance: " + result.getRelevanceScore());
        });

        // Start file watching
        FileSystemWatcherConfig config = FileSystemWatcherConfig.createDefault();
        FileSystemWatcher watcher = new FileSystemWatcher(service, config);
        watcher.startWatching(docsDir);

        // Keep running
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

### 2. Configuration Example
```properties
# searchengine.properties
index.directory=/home/user/documents/index
watch.directory=/home/user/documents/watch
logging.level=INFO
logging.file=/home/user/logs/searchengine.log
max.file.size=10485760
supported.extensions=.txt,.md,.java,.py,.js
```

### 3. Shell Script Example
```bash
#!/bin/bash
# run-searchengine.sh

# Set Java environment
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Set memory options
export JAVA_OPTS="-Xmx2g -Xms512m"

# Run the application
java $JAVA_OPTS -jar target/SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar ./searchengine.properties
```

### 4. Windows Batch Example
```batch
@echo off
REM run-searchengine.bat

REM Set Java environment
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

REM Set memory options
set JAVA_OPTS=-Xmx2g -Xms512m

REM Run the application
java %JAVA_OPTS% -jar target\SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar .\searchengine.properties
```

## 📚 Additional Resources

- **API Reference**: See [API_REFERENCE.md](API_REFERENCE.md)
- **Configuration Guide**: See [CONFIGURATION.md](CONFIGURATION.md)
- **Project Status**: See [PROJECT_STATUS.md](PROJECT_STATUS.md)
- **Implementation Plan**: See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md)

## 🤝 Getting Help

If you encounter issues:

1. **Check the logs** for error messages
2. **Verify requirements** (Java 21, Maven 3.6+)
3. **Review configuration** files and paths
4. **Check permissions** on directories
5. **Review this guide** for common solutions

For additional support, check the project documentation or create an issue in the project repository.

