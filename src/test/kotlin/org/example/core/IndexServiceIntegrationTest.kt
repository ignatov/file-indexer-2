package org.example.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for the IndexService implementation (FileIndexer)
 * and its interaction with DirectoryWatcher.
 */
class IndexServiceIntegrationTest {
    
    private lateinit var indexService: IndexService
    private lateinit var directoryWatcher: DirectoryWatcher
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var textFile1: Path
    private lateinit var textFile2: Path
    private lateinit var subDir: Path
    private lateinit var textFileInSubDir: Path
    
    @BeforeEach
    fun setUp() {
        // Create real implementations
        indexService = FileIndexer()
        directoryWatcher = DirectoryWatcher(indexService)
        
        // Create test files with content
        textFile1 = tempDir.resolve("file1.txt")
        Files.writeString(textFile1, "This is a test file with some words")
        
        textFile2 = tempDir.resolve("file2.txt")
        Files.writeString(textFile2, "This file has different content with some common words")
        
        // Create subdirectory with a file
        subDir = tempDir.resolve("subdir")
        Files.createDirectory(subDir)
        
        textFileInSubDir = subDir.resolve("subfile.txt")
        Files.writeString(textFileInSubDir, "This is a file in a subdirectory")
    }
    
    @AfterEach
    fun tearDown() {
        // Stop watching all directories to clean up resources
        directoryWatcher.stopAll()
    }
    
    @Test
    fun `test directory watching and indexing integration`() = runTest {
        // Start watching the directory
        val watchResult = directoryWatcher.watchDirectory(tempDir)
        assertTrue(watchResult)
        
        // Verify the directory is being watched
        val watchedDirs = directoryWatcher.getWatchedDirectories()
        assertTrue(watchedDirs.contains(tempDir))
        
        // Add the directory to the index
        val addCount = indexService.addDirectory(tempDir, recursive = true)
        assertEquals(3, addCount) // 2 text files in root + 1 in subdirectory
        
        // Verify files are indexed
        val allFiles = indexService.getIndexedFiles().toList()
        assertEquals(3, allFiles.size)
        assertTrue(allFiles.contains(textFile1))
        assertTrue(allFiles.contains(textFile2))
        assertTrue(allFiles.contains(textFileInSubDir))
        
        // Search for words in the indexed files
        val testFiles = indexService.findFilesWithWord("test")
        assertEquals(1, testFiles.size)
        assertEquals(textFile1, testFiles[0])
        
        val commonFiles = indexService.findFilesWithWord("common")
        assertEquals(1, commonFiles.size)
        assertEquals(textFile2, commonFiles[0])
        
        val subdirFiles = indexService.findFilesWithWord("subdirectory")
        assertEquals(1, subdirFiles.size)
        assertEquals(textFileInSubDir, subdirFiles[0])
        
        // Remove a file and verify it's no longer indexed
        val removeResult = indexService.removeFile(textFile1)
        assertTrue(removeResult)
        
        val updatedTestFiles = indexService.findFilesWithWord("test")
        assertTrue(updatedTestFiles.isEmpty())
        
        // Remove a directory and verify its files are no longer indexed
        val removeCount = indexService.removeDirectory(subDir)
        assertEquals(1, removeCount)
        
        val updatedSubdirFiles = indexService.findFilesWithWord("subdirectory")
        assertTrue(updatedSubdirFiles.isEmpty())
        
        // Clear the index and verify all files are removed
        indexService.clearIndex()
        val finalFiles = indexService.getIndexedFiles().toList()
        assertTrue(finalFiles.isEmpty())
    }
    
    @Test
    fun `test file creation is detected and indexed`() = runTest {
        // Start watching the directory
        directoryWatcher.watchDirectory(tempDir)
        
        // Create a new file after watching has started
        val newFile = tempDir.resolve("new-file.txt")
        Files.writeString(newFile, "This is a new file with unique content")
        
        // Wait for the file to be indexed (this may be flaky)
        Thread.sleep(1000)
        
        // Verify the file is indexed
        val uniqueFiles = indexService.findFilesWithWord("unique")
        assertEquals(1, uniqueFiles.size)
        assertEquals(newFile, uniqueFiles[0])
        
        // Clean up
        Files.deleteIfExists(newFile)
    }
    
    @Test
    fun `test file modification is detected and reindexed`() = runTest {
        // Start watching the directory
        directoryWatcher.watchDirectory(tempDir)
        
        // Add the file to the index
        indexService.addFile(textFile1)
        
        // Modify the file
        Files.writeString(textFile1, "This is a modified file with unique content")
        
        // Wait for the file to be reindexed (this may be flaky)
        Thread.sleep(1000)
        
        // Verify the file is reindexed with new content
        val uniqueFiles = indexService.findFilesWithWord("unique")
        assertEquals(1, uniqueFiles.size)
        assertEquals(textFile1, uniqueFiles[0])
        
        // The old word should no longer be associated with the file
        val testFiles = indexService.findFilesWithWord("test")
        assertTrue(testFiles.isEmpty())
    }
    
    @Test
    fun `test file deletion is detected and removed from index`() = runTest {
        // Start watching the directory
        directoryWatcher.watchDirectory(tempDir)
        
        // Add the file to the index
        indexService.addFile(textFile1)
        
        // Verify the file is indexed
        val testFiles = indexService.findFilesWithWord("test")
        assertEquals(1, testFiles.size)
        
        // Delete the file
        Files.delete(textFile1)
        
        // Wait for the file to be removed from the index (this may be flaky)
        Thread.sleep(1000)
        
        // Verify the file is no longer indexed
        val updatedTestFiles = indexService.findFilesWithWord("test")
        assertTrue(updatedTestFiles.isEmpty())
        
        val allFiles = indexService.getIndexedFiles().toList()
        assertFalse(allFiles.contains(textFile1))
    }
}