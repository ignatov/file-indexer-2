package com.ignatov.indexer

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe data structure for storing and retrieving word-to-file mappings.
 * Allows efficient lookup of files containing specific words.
 */
class WordIndex {
    // Map of words to the set of files containing them
    private val wordToFiles = ConcurrentHashMap<String, MutableSet<Path>>()
    
    // Map of files to the set of words they contain
    private val fileToWords = ConcurrentHashMap<Path, MutableSet<String>>()
    
    /**
     * Adds a word-file mapping to the index.
     * @param word The word to index
     * @param filePath The file containing the word
     */
    fun addWord(word: String, filePath: Path) {
        // Add to word -> files mapping
        wordToFiles.computeIfAbsent(word.lowercase()) { ConcurrentHashMap.newKeySet() }.add(filePath)
        
        // Add to file -> words mapping
        fileToWords.computeIfAbsent(filePath) { ConcurrentHashMap.newKeySet() }.add(word.lowercase())
    }
    
    /**
     * Adds multiple words for a file to the index.
     * @param words The words to index
     * @param filePath The file containing the words
     */
    fun addWords(words: Collection<String>, filePath: Path) {
        words.forEach { word -> addWord(word, filePath) }
    }
    
    /**
     * Finds all files containing the specified word.
     * @param word The word to search for
     * @return A list of file paths containing the word
     */
    fun findFilesWithWord(word: String): List<Path> {
        return wordToFiles[word.lowercase()]?.toList() ?: emptyList()
    }
    
    /**
     * Gets all indexed files.
     * @return A set of all indexed file paths
     */
    fun getAllFiles(): Set<Path> {
        return fileToWords.keys
    }
    
    /**
     * Removes a file from the index.
     * @param filePath The file to remove
     * @return true if the file was in the index and was removed, false otherwise
     */
    fun removeFile(filePath: Path): Boolean {
        val words = fileToWords.remove(filePath) ?: return false
        
        // Remove the file from all word entries
        words.forEach { word ->
            wordToFiles[word]?.remove(filePath)
            // If no files contain this word anymore, remove the word entry
            if (wordToFiles[word]?.isEmpty() == true) {
                wordToFiles.remove(word)
            }
        }
        
        return true
    }
    
    /**
     * Clears the entire index.
     */
    fun clear() {
        wordToFiles.clear()
        fileToWords.clear()
    }
    
    /**
     * Checks if a file is already indexed.
     * @param filePath The file to check
     * @return true if the file is indexed, false otherwise
     */
    fun containsFile(filePath: Path): Boolean {
        return fileToWords.containsKey(filePath)
    }
}