package org.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Implementation of IndexService that indexes text files by words.
 */
class FileIndexer : IndexService {
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
            println("Error reading file $filePath: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Checks if a file is a text file based on its extension.
     * @param filePath Path to the file
     * @return true if the file is a text file, false otherwise
     */
    private fun isTextFile(filePath: Path): Boolean {
        val textExtensions = setOf("txt", "md", "java", "kt", "kts", "gradle", "xml", "json", "properties", "yaml", "yml")
        val extension = filePath.name.substringAfterLast('.', "")
        return extension in textExtensions
    }

    override suspend fun addFile(filePath: Path): Boolean {
        if (!Files.exists(filePath) || !filePath.isRegularFile()) {
            return false
        }

        if (!isTextFile(filePath)) {
            return false
        }

        val words = extractWords(filePath)
        if (words.isEmpty()) {
            return false
        }

        wordIndex.addWords(words, filePath)
        return true
    }

    override suspend fun addDirectory(directoryPath: Path, recursive: Boolean): Int {
        if (!Files.exists(directoryPath) || !directoryPath.isDirectory()) {
            return 0
        }

        var count = 0

        // Collect all file paths first
        val filePaths = withContext(Dispatchers.IO) {
            Files.walk(directoryPath).use { paths ->
                paths.filter { path ->
                    path.isRegularFile() && (!recursive && path.parent == directoryPath || recursive)
                }.toList()
            }
        }

        // Process each file
        for (path in filePaths) {
            if (addFile(path)) {
                count++
            }
        }

        return count
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

        var count = 0

        val filesToRemove = wordIndex.getAllFiles().filter { 
            it.startsWith(directoryPath)
        }

        // Process each file
        for (filePath in filesToRemove) {
            if (removeFile(filePath)) {
                count++
            }
        }

        return count
    }

    override suspend fun clearIndex() {
        wordIndex.clear()
    }
}
