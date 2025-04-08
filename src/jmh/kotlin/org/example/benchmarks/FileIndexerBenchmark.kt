package org.example.benchmarks

import org.example.core.FileIndexer
import org.example.core.WordIndex
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Performance benchmarks for the File Indexer project.
 * 
 * This class contains JMH benchmarks to measure the performance of:
 * - WordIndex operations (adding words, finding files)
 * - FileIndexer operations (indexing files, searching)
 * - Concurrent operations
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
open class FileIndexerBenchmark {

    private lateinit var tempDir: Path
    private lateinit var wordIndex: WordIndex
    private lateinit var fileIndexer: FileIndexer
    private lateinit var smallFiles: List<Path>
    private lateinit var largeFile: Path
    private lateinit var wordsToSearch: List<String>

    @Setup
    fun setup() {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("benchmark")

        // Initialize components
        wordIndex = WordIndex()
        fileIndexer = FileIndexer()

        // Create test data
        createTestFiles()

        // Words to search for in benchmarks
        wordsToSearch = listOf("benchmark", "performance", "indexer", "file", "test", "random")
    }

    @TearDown
    fun tearDown() {
        // Clean up temporary files
        tempDir.toFile().deleteRecursively()
    }

    /**
     * Creates test files for benchmarking:
     * - 100 small files with random content
     * - 1 large file with lots of content
     */
    private fun createTestFiles() {
        // Create 100 small files (1-5KB each)
        smallFiles = (1..100).map { i ->
            val file = tempDir.resolve("small_file_$i.txt")
            file.createFile()

            // Generate random content
            val content = buildString {
                repeat(1000 + (Math.random() * 4000).toInt()) {
                    append(SAMPLE_WORDS.random())
                    append(" ")
                    if (it % 20 == 0) append("\n")
                }
            }
            file.writeText(content)
            file
        }

        // Create 1 large file (10MB)
        largeFile = tempDir.resolve("large_file.txt")
        largeFile.createFile()

        // Generate large content
        val largeContent = buildString {
            repeat(2_000_000) { // Approximately 10MB of text
                append(SAMPLE_WORDS.random())
                append(" ")
                if (it % 20 == 0) append("\n")
            }
        }
        largeFile.writeText(largeContent)
    }

    // ===== WordIndex Benchmarks =====

    @Benchmark
    fun benchmarkWordIndexAddWord(blackhole: Blackhole) {
        val testIndex = WordIndex()
        val testFile = tempDir.resolve("test.txt")

        // Add 1000 words to the index
        for (word in SAMPLE_WORDS.take(1000)) {
            testIndex.addWord(word, testFile)
        }

        // Ensure the operation isn't optimized away
        blackhole.consume(testIndex)
    }

    @Benchmark
    fun benchmarkWordIndexFindFiles(blackhole: Blackhole) {
        // Pre-populate the index
        val testIndex = WordIndex()
        val testFiles = (1..10).map { tempDir.resolve("test_$it.txt") }

        // Add words to the index
        for (i in 0 until 1000) {
            val word = SAMPLE_WORDS[i % SAMPLE_WORDS.size]
            val file = testFiles[i % testFiles.size]
            testIndex.addWord(word, file)
        }

        // Benchmark finding files
        for (word in wordsToSearch) {
            blackhole.consume(testIndex.findFilesWithWord(word))
        }
    }

    // ===== FileIndexer Benchmarks =====

    @Benchmark
    fun benchmarkIndexSmallFiles(blackhole: Blackhole): Unit = runBlocking {
        val testIndexer = FileIndexer()

        // Index all small files
        for (file in smallFiles) {
            blackhole.consume(testIndexer.addFile(file))
        }
    }

    @Benchmark
    fun benchmarkIndexLargeFile(blackhole: Blackhole): Unit = runBlocking {
        val testIndexer = FileIndexer()

        // Index the large file
        blackhole.consume(testIndexer.addFile(largeFile))
    }

    @Benchmark
    fun benchmarkSearchIndexedFiles(blackhole: Blackhole): Unit = runBlocking {
        // Pre-populate the index
        val testIndexer = FileIndexer()
        for (file in smallFiles.take(20)) {
            testIndexer.addFile(file)
        }

        // Benchmark searching
        for (word in wordsToSearch) {
            blackhole.consume(testIndexer.findFilesWithWord(word))
        }
    }

    // ===== Concurrent Operations Benchmarks =====

    @Benchmark
    fun benchmarkConcurrentIndexing(blackhole: Blackhole): Unit = runBlocking {
        val testIndexer = FileIndexer()

        // Launch concurrent indexing operations
        val jobs = (1..10).map { 
            launch {
                for (file in smallFiles.subList((it-1) * 10, it * 10)) {
                    testIndexer.addFile(file)
                }
            }
        }

        // Wait for all jobs to complete
        jobs.forEach { it.join() }

        // Ensure the operation isn't optimized away
        blackhole.consume(testIndexer.getIndexedFiles().toList())
    }

    companion object {
        // Sample words for generating test content
        private val SAMPLE_WORDS = listOf(
            "file", "index", "search", "word", "text", "document", "content", "data",
            "benchmark", "performance", "test", "kotlin", "coroutine", "concurrent",
            "directory", "watcher", "service", "implementation", "interface", "class",
            "function", "method", "variable", "constant", "parameter", "return", "value",
            "string", "integer", "boolean", "list", "map", "set", "collection", "array",
            "thread", "process", "memory", "cpu", "disk", "network", "io", "input", "output",
            "read", "write", "open", "close", "create", "delete", "modify", "update", "get",
            "set", "add", "remove", "find", "search", "sort", "filter", "map", "reduce",
            "indexer", "analyzer", "parser", "tokenizer", "scanner", "reader", "writer",
            "buffer", "stream", "channel", "path", "file", "folder", "directory", "system",
            "random", "sequential", "parallel", "asynchronous", "synchronous", "blocking",
            "non-blocking", "callback", "future", "promise", "reactive", "event", "listener",
            "publisher", "subscriber", "observer", "observable", "subject", "message", "queue"
        )
    }
}
