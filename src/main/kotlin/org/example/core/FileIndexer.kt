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
                    println("Treating file without extension as text file: $filePath (size: $fileSize bytes)")
                    return true
                }
            } catch (e: IOException) {
                println("Error checking file size for $filePath: ${e.message}")
            }
        }

        val isText = extension in textExtensions
        if (!isText && extension.isNotEmpty()) {
//            println("Skipping non-text file: $filePath (extension: $extension)")
        }
        return isText
    }

    override suspend fun addFile(filePath: Path): Boolean {
        if (!Files.exists(filePath) || !filePath.isRegularFile()) {
            println("Skipping non-existent or non-regular file: $filePath")
            return false
        }

        if (!isTextFile(filePath)) {
            // Debug message already printed in isTextFile method
            return false
        }

        val words = extractWords(filePath)
        if (words.isEmpty()) {
            println("Skipping file with no extractable words: $filePath")
            return false
        }

        wordIndex.addWords(words, filePath)
//        println("Added file to index: $filePath (${words.size} words)")
        return true
    }

    override suspend fun addDirectory(directoryPath: Path, recursive: Boolean): Int {
        if (!Files.exists(directoryPath) || !directoryPath.isDirectory()) {
            println("Directory does not exist or is not a directory: $directoryPath")
            return 0
        }

        println("Scanning directory: $directoryPath (recursive: $recursive)")
        var count = 0
        var totalFiles = 0

        // Collect all file paths first
        val filePaths = withContext(Dispatchers.IO) {
            Files.walk(directoryPath).use { paths ->
                paths.filter { path ->
                    val isFile = path.isRegularFile()
                    val inScope = (!recursive && path.parent == directoryPath) || recursive

                    // Check if any part of the path is a .git directory
                    val containsGitDir = path.iterator().asSequence().any { it.toString() == ".git" }
                    if (containsGitDir) {
                        println("Skipping file in .git directory: $path")
                        return@filter false
                    }

                    if (isFile && !inScope) {
                        println("Skipping file outside scope: $path")
                    }
                    isFile && inScope
                }.toList()
            }
        }

        totalFiles = filePaths.size
        println("Found $totalFiles files in directory: $directoryPath")

        // Process each file
        for (path in filePaths) {
            if (addFile(path)) {
                count++
            }
        }

        println("Successfully indexed $count out of $totalFiles files in directory: $directoryPath")
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
