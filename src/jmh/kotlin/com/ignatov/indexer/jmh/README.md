# File Indexer Performance Tests

This directory contains JMH (Java Microbenchmark Harness) benchmarks for measuring the performance of the File Indexer project.

## Benchmarks Overview

The benchmarks are organized into two main classes:

### 1. FileIndexerBenchmark

This class contains benchmarks for measuring the execution time of various operations:

- **WordIndex Operations**
  - `benchmarkWordIndexAddWord`: Measures the performance of adding words to the index
  - `benchmarkWordIndexFindFiles`: Measures the performance of finding files by word

- **FileIndexer Operations**
  - `benchmarkIndexSmallFiles`: Measures the performance of indexing many small files
  - `benchmarkIndexLargeFile`: Measures the performance of indexing one large file
  - `benchmarkSearchIndexedFiles`: Measures the performance of searching in indexed files

- **Concurrent Operations**
  - `benchmarkConcurrentIndexing`: Measures the performance of concurrent indexing operations

### 2. MemoryUsageBenchmark

This class contains benchmarks for measuring memory consumption:

- `benchmarkWordIndexMemoryUsage`: Measures memory usage when adding a large number of words to the index
- `benchmarkFileIndexerMemoryUsage`: Measures memory usage when indexing files
- `benchmarkMemoryRetentionAfterClear`: Measures memory retention after clearing the index (to check for memory leaks)
- `benchmarkHeapUsageOverTime`: Measures heap usage over time during batch indexing operations

## Running the Benchmarks

To run the benchmarks, use the following Gradle command:

```bash
./gradlew jmh
```

This will run all benchmarks and generate a report in the `build/reports/jmh/results.json` file.

To run a specific benchmark, use:

```bash
./gradlew jmh -PjmhInclude=com.ignatov.indexer.jmh.FileIndexerBenchmark.benchmarkIndexSmallFiles
```

## Interpreting Results

The benchmark results include:

- **Score**: The average execution time (for time benchmarks) or value (for memory benchmarks)
- **Error**: The error margin of the score
- **Units**: The units of the score (milliseconds for time, MB for memory)

For memory benchmarks, the score represents:
- The amount of memory consumed (in MB) for `benchmarkWordIndexMemoryUsage` and `benchmarkFileIndexerMemoryUsage`
- The amount of memory retained after clearing (in MB) for `benchmarkMemoryRetentionAfterClear`
- A list of memory readings over time for `benchmarkHeapUsageOverTime`

## Customizing Benchmarks

You can customize the benchmarks by modifying the JMH parameters in the `build.gradle.kts` file:

```kotlin
jmh {
    iterations = 5 // Number of measurement iterations
    warmupIterations = 3 // Number of warmup iterations
    fork = 2 // How many times to fork a single benchmark
    // ...
}
```

Or by modifying the JMH annotations in the benchmark classes:

```kotlin
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
```

## Best Practices

When running performance tests:

1. Close other applications to minimize interference
2. Run the tests multiple times to ensure consistent results
3. Be aware that JVM warmup can affect the first few runs
4. For memory tests, be aware that garbage collection timing can affect results