package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.example.core.DirectoryWatcher
import org.example.core.FileIndexer
import org.example.core.IndexService
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.system.exitProcess

/**
 * Main class for the File Indexer application.
 * Provides a REPL interface for interacting with the indexer.
 */
class FileIndexerRepl(private val indexService: IndexService) {
    private val directoryWatcher = DirectoryWatcher(indexService)

    /**
     * Expands a path that starts with "~" to the user's home directory.
     * @param path The path that may start with "~"
     * @return The expanded path
     */
    private fun expandTilde(path: String): String {
        if (path.startsWith("~")) {
            val userHome = System.getProperty("user.home")
            return path.replaceFirst("~", userHome)
        }
        return path
    }

    /**
     * Starts the REPL loop.
     */
    fun start() {
        println("Welcome to File Indexer!")
        println("Type 'help' for available commands.")

        while (true) {
            print("> ")
            val input = readlnOrNull() ?: break

            if (input.isBlank()) continue

            processCommand(input)
        }
    }

    /**
     * Processes a command from the user.
     * @param input The command input
     */
    private fun processCommand(input: String) {
        val parts = input.trim().split("\\s+".toRegex(), limit = 2)
        val command = parts[0].lowercase()
        val args = if (parts.size > 1) parts[1] else ""

        runBlocking {
            when (command) {
                "add" -> handleAddCommand(args)
                "search" -> handleSearchCommand(args)
                "list" -> handleListCommand()
                "watch" -> handleWatchCommand(args)
                "unwatch" -> handleUnwatchCommand(args)
                "clear" -> handleClearCommand()
                "help" -> handleHelpCommand()
                "exit", "quit" -> {
                    println("Goodbye!")
                    directoryWatcher.stopAll()
                    exitProcess(0)
                }
                else -> println("Unknown command. Type 'help' for available commands.")
            }
        }
    }

    /**
     * Handles the 'add' command.
     * @param args Command arguments
     */
    private suspend fun handleAddCommand(args: String) {
        if (args.isBlank()) {
            println("Usage: add <file_path> or add -r <directory_path>")
            return
        }

        val recursive = args.startsWith("-r ")
        val path = if (recursive) args.substring(3).trim() else args.trim()

        if (path.isBlank()) {
            println("Please provide a valid path.")
            return
        }

        val expandedPath = expandTilde(path)
        val filePath = Paths.get(expandedPath)

        if (filePath.toFile().isDirectory) {
            val count = indexService.addDirectory(filePath, recursive)
            println("Added $count files from directory: $path")
        } else {
            val success = indexService.addFile(filePath)
            if (success) {
                println("Added file: $path")
            } else {
                println("Failed to add file: $path")
            }
        }
    }

    /**
     * Handles the 'search' command.
     * @param args Command arguments
     */
    private suspend fun handleSearchCommand(args: String) {
        if (args.isBlank()) {
            println("Usage: search <word>")
            return
        }

        val word = args.trim()
        val files = indexService.findFilesWithWord(word)

        if (files.isEmpty()) {
            println("No files found containing word: $word")
        } else {
            println("Files containing word '$word':")
            files.forEach { println("  $it") }
        }
    }

    /**
     * Handles the 'list' command.
     */
    private suspend fun handleListCommand() {
        val files = indexService.getIndexedFiles().toList()

        if (files.isEmpty()) {
            println("No files are currently indexed.")
        } else {
            println("Indexed files:")
            files.forEach { println("  $it") }
        }
    }

    /**
     * Handles the 'watch' command.
     * @param args Command arguments
     */
    private suspend fun handleWatchCommand(args: String) {
        if (args.isBlank()) {
            println("Usage: watch <directory_path>")
            return
        }

        val path = args.trim()
        val expandedPath = expandTilde(path)
        val directoryPath = Paths.get(expandedPath)

        if (!directoryPath.toFile().isDirectory) {
            println("Not a directory: $path")
            return
        }

        println("Starting to watch directory: $path (expanded to: $expandedPath)")
        val start = System.currentTimeMillis()
        val success = directoryWatcher.watchDirectory(directoryPath)
        if (success) {
            println("Successfully set up directory watcher for: $path")

            // Count files in the directory before indexing
            val fileCount = withContext(Dispatchers.IO) {
                Files.walk(directoryPath).use { paths ->
                    paths.filter { it.isRegularFile() }.count()
                }
            }
            println("Found $fileCount files in directory: $path")

            // Index existing files in the directory
            println("Starting to index existing files...")
            val count = indexService.addDirectory(directoryPath, true)
            val duration = System.currentTimeMillis() - start

            println("Now watching directory: $path")
            println("Indexed $count existing files in the directory, took $duration ms.")

            if (count == 0 && fileCount > 0) {
                println("Note: No files were indexed despite finding $fileCount files in the directory.")
                println("This may be because none of the files are recognized as text files or they don't contain extractable text.")
                println("Check the logs above for more details on why files were skipped.")
            }
        } else {
            println("Failed to watch directory: $path")
        }
    }

    /**
     * Handles the 'unwatch' command.
     * @param args Command arguments
     */
    private fun handleUnwatchCommand(args: String) {
        if (args.isBlank()) {
            println("Usage: unwatch <directory_path>")
            return
        }

        val path = args.trim()
        val expandedPath = expandTilde(path)
        val directoryPath = Paths.get(expandedPath)

        val success = directoryWatcher.stopWatching(directoryPath)
        if (success) {
            println("Stopped watching directory: $path")
        } else {
            println("Not watching directory: $path")
        }
    }

    /**
     * Handles the 'clear' command.
     */
    private suspend fun handleClearCommand() {
        indexService.clearIndex()
        println("Index cleared.")
    }

    /**
     * Handles the 'help' command.
     */
    private fun handleHelpCommand() {
        println("""
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
        """.trimIndent())
    }
}

/**
 * Main entry point for the application.
 */
fun main() {
    val indexService = FileIndexer()
    val repl = FileIndexerRepl(indexService)
    repl.start()
}
