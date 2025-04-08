# File Indexer

A high-performance file indexing and search tool built with Kotlin.

## Overview

File Indexer is a tool that indexes text files by words, allowing for fast searching of files containing specific words. It can watch directories for changes and automatically update the index when files are created, modified, or deleted.

## Features

- **Text File Indexing**: Indexes words in text files for fast searching
- **Directory Watching**: Monitors directories for file changes and updates the index automatically
- **Fast Search**: Quickly find files containing specific words
- **Interactive CLI**: User-friendly command-line interface for interacting with the indexer
- **Concurrent Processing**: Utilizes Kotlin coroutines for efficient parallel processing
- **Memory Efficient**: Optimized for handling large numbers of files with minimal memory footprint

## Architecture

The project is organized into several key components:

- **IndexService**: Interface defining the core indexing operations
- **FileIndexer**: Implementation of IndexService that indexes text files by words
- **WordIndex**: Thread-safe data structure for storing and retrieving word-to-file mappings
- **DirectoryWatcher**: Watches directories for file changes and updates the index accordingly
- **FileIndexerRepl**: Command-line interface for interacting with the indexer

## Getting Started

### Prerequisites

- JDK 22 or later
- Gradle 8.10 or later

### Building the Project

```bash
./gradlew build
```

### Running the Application

```bash
./gradlew run
```

## Usage

Once the application is running, you can use the following commands:

```
Available commands:
add <file_path>           - Add a file to the index
add -r <directory_path>   - Add a directory to the index recursively
search <word>             - Search for files containing a word
list                      - List all indexed files
watch <directory_path>    - Watch a directory for changes
unwatch <directory_path>  - Stop watching a directory
clear                     - Clear the index
help                      - Show this help message
exit, quit                - Exit the application
```

### Examples

```
> add ~/documents/sample.txt
Added file: ~/documents/sample.txt

> add -r ~/documents
Added 42 files from directory: ~/documents

> search kotlin
Files containing word 'kotlin':
  /home/user/documents/project.kt
  /home/user/documents/notes.txt

> watch ~/downloads
Now watching directory: ~/downloads
```

## Performance

The project includes JMH benchmarks to measure performance:

- **Word Indexing**: Benchmarks for adding words to the index and finding files
- **File Indexing**: Benchmarks for indexing small and large files
- **Concurrent Operations**: Benchmarks for parallel indexing operations
- **Memory Usage**: Benchmarks for measuring memory consumption

To run the benchmarks:

```bash
./gradlew jmh
```

## Testing

The project includes comprehensive unit and integration tests:

```bash
./gradlew test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.