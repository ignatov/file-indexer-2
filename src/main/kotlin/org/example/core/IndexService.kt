package org.example.core

import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

/**
 * Interface for a service that indexes text files by words.
 * Provides functionality to add files/directories to the index and search for files containing specific words.
 */
interface IndexService {
    /**
     * Adds a file to the index.
     * @param filePath Path to the file to be indexed
     * @return true if the file was successfully added, false otherwise
     */
    suspend fun addFile(filePath: Path): Boolean
    
    /**
     * Adds a directory to the index. All text files in the directory will be indexed.
     * @param directoryPath Path to the directory to be indexed
     * @param recursive If true, subdirectories will also be indexed
     * @return Number of files successfully indexed
     */
    suspend fun addDirectory(directoryPath: Path, recursive: Boolean = true): Int
    
    /**
     * Searches for files containing the specified word.
     * @param word The word to search for
     * @return A list of file paths containing the word
     */
    suspend fun findFilesWithWord(word: String): List<Path>
    
    /**
     * Gets a flow of all indexed files.
     * @return A flow of all indexed file paths
     */
    fun getIndexedFiles(): Flow<Path>
    
    /**
     * Removes a file from the index.
     * @param filePath Path to the file to be removed
     * @return true if the file was successfully removed, false otherwise
     */
    suspend fun removeFile(filePath: Path): Boolean
    
    /**
     * Removes a directory from the index.
     * @param directoryPath Path to the directory to be removed
     * @return Number of files successfully removed from the index
     */
    suspend fun removeDirectory(directoryPath: Path): Int
    
    /**
     * Clears the entire index.
     */
    suspend fun clearIndex()
}