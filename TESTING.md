# Testing - Search Engine Library

A comprehensive guide to the testing strategy, coverage, and test types in the Search Engine library.

## 📊 Test Coverage Overview

**Current Test Coverage: 81%** ✅

The library maintains a solid test coverage with comprehensive unit tests and integration tests covering all major components and workflows.

## 🧪 Unit Tests

### Core Components Testing

The library includes unit tests for all major components:
- **Core classes**: Index, search results, service layer
- **Configuration**: Properties loading and validation  
- **Filesystem watching**: Directory monitoring and event handling
- **Utilities**: File filtering and performance monitoring
- **Tokenizer**: Custom tokenization implementations

### Unit Test Characteristics

- **Framework**: JUnit 5 (Jupiter)
- **Mocking**: Mockito for dependency isolation
- **Assertions**: Comprehensive validation of expected behavior
- **Coverage**: Tests cover happy path, edge cases, and error conditions
- **Isolation**: Each test is independent and doesn't affect others

## 🔗 Integration Tests

The integration test package (`src/test/java/com/demo/searchengine/integration/`) provides comprehensive end-to-end testing of the entire system.

### **SearchEngineIntegrationTest.java** - Main Integration Suite

Tests complete workflows with real filesystem operations:

#### **Basic Functionality Tests**
- **File Indexing and Search**: End-to-end indexing and search workflow
- **Two Keywords Search**: Multi-term search functionality
- **File System Watching**: Real-time directory monitoring
- **Large File Handling**: Performance with substantial file sizes

#### **Real Filesystem Testing**
- Uses `@TempDir` for isolated test environments
- Creates realistic test files with varied content
- Tests actual file operations (create, modify, delete)
- Validates filesystem watching integration

### **ConcurrentUsageSimulator.java** - Concurrency Testing

Simulates real-world concurrent usage scenarios:

#### **Multi-Threaded Operations**
- **File Operations**: Create, update, delete files across multiple threads
- **Search Operations**: Concurrent searches during file modifications
- **Validation**: Deterministic validation of concurrent operations
- **Stress Testing**: High-load scenarios with multiple worker threads

#### **Concurrency Validation**
- Ensures thread safety under load
- Validates data consistency during concurrent access
- Tests performance under concurrent usage
- Measures throughput and response times

### **PerformanceTest.java** - Performance Benchmarking

Dedicated performance testing under various conditions:

#### **Performance Metrics**
- **File Creation to Searchable Time**: End-to-end performance measurement
- **Large File Handling**: Performance with files up to 10MB
- **Concurrent Operations**: Performance under multi-threaded load
- **Response Time Validation**: Ensures performance meets requirements (< 3 seconds)

#### **Performance Validation**
- Measures indexing performance
- Tests search response times
- Validates performance under load
- Provides performance benchmarks

### **TestFileSystemGenerator.java** - Test Environment Setup

Creates realistic test filesystems for integration testing:

#### **Filesystem Generation**
- **Directory Structure**: Creates multi-level directory hierarchies
- **File Varieties**: Different file sizes and content types
- **Watched vs Unwatched**: Separates monitored and unmonitored areas
- **Deterministic Mode**: Reproducible test environments

#### **Realistic Testing**
- Generates files with realistic content
- Creates varying directory depths
- Provides configurable complexity levels
- Supports both random and deterministic generation



### **Test Execution**

#### **Unit Tests**
```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=SimpleTextIndexingServiceTest

# Run with coverage report
mvn test jacoco:report
```

#### **Integration Tests**
```bash
# Run integration tests
mvn test -Dtest="*IntegrationTest"

# Run performance tests
mvn test -Dtest="*PerformanceTest"
```

## 🎉 Summary

The Search Engine library maintains a robust testing strategy with:

- **81% test coverage** across all components
- **Comprehensive unit tests** for individual components
- **Real-world integration tests** with actual filesystem operations
- **Performance and concurrency testing** for production readiness
- **Deterministic test environments** for reliable results

The testing approach ensures code quality, validates functionality, and provides confidence in the library's reliability for production use.

For implementation details, see the test source code in:
- `src/test/java/com/demo/searchengine/` - Unit tests
- `src/test/java/com/demo/searchengine/integration/` - Integration tests
