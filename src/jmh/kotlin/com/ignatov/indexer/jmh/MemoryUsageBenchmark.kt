package com.ignatov.indexer.jmh

import com.ignatov.indexer.FileIndexer
import com.ignatov.indexer.WordIndex
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createFile
import kotlin.io.path.writeText

/**
 * Memory usage benchmarks for the File Indexer project.
 * 
 * This class contains JMH benchmarks to measure the memory consumption of:
 * - WordIndex when adding large numbers of words
 * - FileIndexer when indexing large numbers of files
 * - Memory retention after clearing the index
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Fork(1)
open class MemoryUsageBenchmark {

    private lateinit var tempDir: Path
    private lateinit var largeFiles: List<Path>

    @Setup
    fun setup() {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("memory_benchmark")

        // Create test data
        createLargeTestFiles()
    }

    @TearDown
    fun tearDown() {
        // Clean up temporary files
        tempDir.toFile().deleteRecursively()
    }

    /**
     * Creates large test files for memory benchmarking
     */
    private fun createLargeTestFiles() {
        // Create 50 medium-sized files (100KB each)
        largeFiles = (1..50).map { i ->
            val file = tempDir.resolve("large_file_$i.txt")
            file.createFile()

            // Generate content with many unique words to stress the index
            val content = buildString {
                repeat(20_000) { j -> // ~100KB of text
                    append("word${i}_${j} ")
                    if (j % 20 == 0) append("\n")
                }
            }
            file.writeText(content)
            file
        }
    }

    /**
     * Helper function to get current memory usage in MB
     */
    private fun getMemoryUsageMB(): Double {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024.0 * 1024.0)
    }

    /**
     * Helper function to force garbage collection
     */
    private fun forceGC() {
        System.gc()
        System.gc()
        Thread.sleep(100)
    }

    @Benchmark
    fun benchmarkWordIndexMemoryUsage(blackhole: Blackhole): Double {
        forceGC()
        val initialMemory = getMemoryUsageMB()

        // Create a large WordIndex with many entries
        val wordIndex = WordIndex()

        // Add 1 million word-file mappings
        val testFile = tempDir.resolve("test.txt")
        for (i in 1..1_000_000) {
            wordIndex.addWord("uniqueword$i", testFile)
        }

        blackhole.consume(wordIndex)

        forceGC()
        val finalMemory = getMemoryUsageMB()

        // Return memory usage difference in MB
        return finalMemory - initialMemory
    }

    @Benchmark
    fun benchmarkFileIndexerMemoryUsage(blackhole: Blackhole): Double = runBlocking {
        forceGC()
        val initialMemory = getMemoryUsageMB()

        val fileIndexer = FileIndexer()

        // Index all large files
        for (file in largeFiles) {
            fileIndexer.addFile(file)
        }

        blackhole.consume(fileIndexer.getIndexedFiles().toList())

        forceGC()
        val finalMemory = getMemoryUsageMB()

        // Memory usage difference in MB
        finalMemory - initialMemory
    }

    @Benchmark
    fun benchmarkMemoryRetentionAfterClear(blackhole: Blackhole): Double = runBlocking {
        val fileIndexer = FileIndexer()

        // First index all files
        for (file in largeFiles) {
            fileIndexer.addFile(file)
        }

        blackhole.consume(fileIndexer.getIndexedFiles().toList())

        // Measure memory before clearing
        forceGC()
        val memoryBeforeClear = getMemoryUsageMB()

        // Clear the index
        fileIndexer.clearIndex()

        // Force GC and measure memory after clearing
        forceGC()
        val memoryAfterClear = getMemoryUsageMB()

        // Memory retention (how much wasn't freed) in MB
        memoryAfterClear - memoryBeforeClear
    }

    @Benchmark
    fun benchmarkHeapUsageOverTime(blackhole: Blackhole): List<Double> = runBlocking {
        forceGC()

        val memoryReadings = mutableListOf<Double>()
        val fileIndexer = FileIndexer()

        // Take initial reading
        memoryReadings.add(getMemoryUsageMB())

        // Index files in batches and measure memory after each batch
        val batchSize = 10
        for (i in 0 until largeFiles.size step batchSize) {
            val endIndex = minOf(i + batchSize, largeFiles.size)
            for (j in i until endIndex) {
                fileIndexer.addFile(largeFiles[j])
            }

            blackhole.consume(fileIndexer.getIndexedFiles().toList())

            // Record memory usage after this batch
            memoryReadings.add(getMemoryUsageMB())
        }

        // Clear the index
        fileIndexer.clearIndex()
        forceGC()

        // Final memory reading after clearing
        memoryReadings.add(getMemoryUsageMB())

        // Return the list of memory readings
        memoryReadings
    }
}
