package com.ignatov.indexer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Implementation of IndexService that indexes text files by words.
 */
class FileIndexer : IndexService {
    private val log: Logger = LoggerFactory.getLogger(FileIndexer::class.java)
    private val wordIndex = WordIndex()
    private val isTextFile = AtomicBoolean(true) // Default to true for simplicity

    /**
     * Extracts words from a text file.
     * @param filePath Path to the file
     * @return List of words in the file
     */
    private suspend fun extractWords(filePath: Path): List<String> = withContext(Dispatchers.IO) {
        try {
            val content = Files.readString(filePath)
            // Split by non-word characters and filter out empty strings
            return@withContext content.split(Regex("\\W+"))
                .filter { it.isNotEmpty() }
                .map { it.lowercase() }
        } catch (e: IOException) {
            log.trace("Error reading file $filePath: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Checks if a file is a text file based on its extension.
     * @param filePath Path to the file
     * @return true if the file is a text file, false otherwise
     */
    private fun isTextFile(filePath: Path): Boolean {
        // Expanded list of text file extensions
        val textExtensions = setOf(
            "txt", "md", "java", "kt", "kts", "gradle", "xml", "json", "properties", "yaml", "yml",
            "js", "ts", "html", "css", "scss", "sass", "less", "py", "rb", "sh", "bash", "zsh",
            "c", "cpp", "h", "hpp", "cs", "go", "rs", "php", "pl", "pm", "t", "sql", "conf",
            "ini", "cfg", "config", "toml", "lock", "gitignore", "dockerignore", "editorconfig",
            "log", "csv", "tsv", "svg", "graphql", "gql", "proto", "plist", "swift", "m", "mm"
        )
        val extension = filePath.name.substringAfterLast('.', "")

        // If no extension but file is small enough, consider it a text file
        if (extension.isEmpty()) {
            try {
                val fileSize = Files.size(filePath)
                // Consider files smaller than 1MB without extension as potential text files
                if (fileSize < 1024 * 1024) {
                    log.trace("Treating file without extension as text file: $filePath (size: $fileSize bytes)")
                    return true
                }
            } catch (e: IOException) {
                log.trace("Error checking file size for $filePath: ${e.message}")
            }
        }

        val isText = extension in textExtensions
        if (!isText && extension.isNotEmpty()) {
            log.trace("Skipping non-text file: $filePath (extension: $extension)")
        }
        return isText
    }

    override suspend fun addFile(filePath: Path): Boolean {
        if (!Files.exists(filePath) || !filePath.isRegularFile()) {
            log.trace("Skipping non-existent or non-regular file: $filePath")
            return false
        }

        if (!isTextFile(filePath)) {
            // Debug message already printed in isTextFile method
            return false
        }

        val words = extractWords(filePath)
        if (words.isEmpty()) {
            log.trace("Skipping file with no extractable words: $filePath")
            return false
        }

        wordIndex.addWords(words, filePath)
        log.trace("Added file to index: $filePath (${words.size} words)")
        return true
    }

    override suspend fun addDirectory(directoryPath: Path, recursive: Boolean): Int {
        if (!Files.exists(directoryPath) || !directoryPath.isDirectory()) {
            log.trace("Directory does not exist or is not a directory: $directoryPath")
            return 0
        }
        log.trace("Scanning directory: $directoryPath (recursive: $recursive)")

        var totalFiles = 0
        val filePathFlow = flow<Path> {
            Files.walk(directoryPath).use { stream ->
                for (path in stream) {
                    if (path.isRegularFile()) {
                        val inScope = if (recursive) true else (path.parent == directoryPath)
                        if (!inScope) {
                            log.trace("Skipping file outside scope: $path")
                        }
                        if (inScope && !path.iterator().asSequence().any { it.toString() == ".git" }) {
                            totalFiles++
                            emit(path)
                        }
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

        val workerCount = Runtime.getRuntime().availableProcessors()
        val successCount = filePathFlow
            .flatMapMerge(concurrency = workerCount) { path ->
                // For each file path, emit the result of addFile concurrently
                flow { emit(addFile(path)) }
            }
            .filter { it }  // Only count files that were successfully indexed
            .count()

        log.trace("Successfully indexed $successCount out of $totalFiles files in directory: $directoryPath")
        return successCount
    }

    override suspend fun findFilesWithWord(word: String): List<Path> {
        return wordIndex.findFilesWithWord(word)
    }

    override fun getIndexedFiles(): Flow<Path> = flow {
        wordIndex.getAllFiles().forEach { emit(it) }
    }

    override suspend fun removeFile(filePath: Path): Boolean {
        return wordIndex.removeFile(filePath)
    }

    override suspend fun removeDirectory(directoryPath: Path): Int {
        if (!Files.exists(directoryPath) || !directoryPath.isDirectory()) {
            return 0
        }

        val filesToRemove = wordIndex.getAllFiles().filter {
            it.startsWith(directoryPath)
        }

        // Process files in parallel
        return coroutineScope {
            val successCounter = AtomicInteger(0)

            // Create a list of deferred results
            val deferredResults = filesToRemove.map { filePath ->
                async(Dispatchers.IO) {
                    val success = removeFile(filePath)
                    if (success) {
                        successCounter.incrementAndGet()
                    }
                }
            }

            // Wait for all async operations to complete
            deferredResults.awaitAll()

            // Return the final count
            successCounter.get()
        }
    }

    override suspend fun clearIndex() {
        wordIndex.clear()
    }
}
