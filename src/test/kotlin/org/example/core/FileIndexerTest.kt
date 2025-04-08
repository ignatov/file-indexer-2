package org.example.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileIndexerTest {
    
    private lateinit var fileIndexer: FileIndexer
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var textFile1: Path
    private lateinit var textFile2: Path
    private lateinit var nonTextFile: Path
    private lateinit var subDir: Path
    private lateinit var textFileInSubDir: Path
    
    @BeforeEach
    fun setUp() {
        fileIndexer = FileIndexer()
        
        // Create test files with content
        textFile1 = tempDir.resolve("file1.txt")
        Files.writeString(textFile1, "This is a test file with some words")
        
        textFile2 = tempDir.resolve("file2.txt")
        Files.writeString(textFile2, "This file has different content with some common words")
        
        nonTextFile = tempDir.resolve("image.png")
        Files.writeString(nonTextFile, "This is not a text file based on extension")
        
        // Create subdirectory with a file
        subDir = tempDir.resolve("subdir")
        Files.createDirectory(subDir)
        
        textFileInSubDir = subDir.resolve("subfile.txt")
        Files.writeString(textFileInSubDir, "This is a file in a subdirectory")
    }
    
    @Test
    fun `test addFile adds text file successfully`() = runTest {
        // When
        val result = fileIndexer.addFile(textFile1)
        
        // Then
        assertTrue(result)
        
        val files = fileIndexer.findFilesWithWord("test")
        assertEquals(1, files.size)
        assertEquals(textFile1, files[0])
    }
    
    @Test
    fun `test addFile ignores non-text files`() = runTest {
        // When
        val result = fileIndexer.addFile(nonTextFile)
        
        // Then
        assertFalse(result)
        
        val allFiles = fileIndexer.getIndexedFiles().toList()
        assertTrue(allFiles.isEmpty())
    }
    
    @Test
    fun `test addDirectory adds all text files in directory`() = runTest {
        // When
        val count = fileIndexer.addDirectory(tempDir, recursive = false)
        
        // Then
        assertEquals(2, count) // Only the 2 text files in the root directory
        
        val testFiles = fileIndexer.findFilesWithWord("test")
        assertEquals(1, testFiles.size)
        
        val commonFiles = fileIndexer.findFilesWithWord("common")
        assertEquals(1, commonFiles.size)
        
        val allFiles = fileIndexer.getIndexedFiles().toList()
        assertEquals(2, allFiles.size)
        assertTrue(allFiles.contains(textFile1))
        assertTrue(allFiles.contains(textFile2))
        assertFalse(allFiles.contains(textFileInSubDir))
    }
    
    @Test
    fun `test addDirectory with recursive option adds files in subdirectories`() = runTest {
        // When
        val count = fileIndexer.addDirectory(tempDir, recursive = true)
        
        // Then
        assertEquals(3, count) // 2 text files in root + 1 in subdirectory
        
        val subDirFiles = fileIndexer.findFilesWithWord("subdirectory")
        assertEquals(1, subDirFiles.size)
        assertEquals(textFileInSubDir, subDirFiles[0])
        
        val allFiles = fileIndexer.getIndexedFiles().toList()
        assertEquals(3, allFiles.size)
        assertTrue(allFiles.contains(textFileInSubDir))
    }
    
    @Test
    fun `test findFilesWithWord returns correct files`() = runTest {
        // Given
        fileIndexer.addFile(textFile1)
        fileIndexer.addFile(textFile2)
        
        // When
        val testFiles = fileIndexer.findFilesWithWord("test")
        val commonFiles = fileIndexer.findFilesWithWord("common")
        val differentFiles = fileIndexer.findFilesWithWord("different")
        val nonExistentFiles = fileIndexer.findFilesWithWord("nonexistent")
        
        // Then
        assertEquals(1, testFiles.size)
        assertEquals(textFile1, testFiles[0])
        
        assertEquals(1, commonFiles.size)
        assertEquals(textFile2, commonFiles[0])
        
        assertEquals(1, differentFiles.size)
        assertEquals(textFile2, differentFiles[0])
        
        assertTrue(nonExistentFiles.isEmpty())
    }
    
    @Test
    fun `test removeFile removes file from index`() = runTest {
        // Given
        fileIndexer.addFile(textFile1)
        fileIndexer.addFile(textFile2)
        
        // When
        val result = fileIndexer.removeFile(textFile1)
        
        // Then
        assertTrue(result)
        
        val testFiles = fileIndexer.findFilesWithWord("test")
        assertTrue(testFiles.isEmpty())
        
        val allFiles = fileIndexer.getIndexedFiles().toList()
        assertEquals(1, allFiles.size)
        assertEquals(textFile2, allFiles[0])
    }
    
    @Test
    fun `test removeDirectory removes all files in directory from index`() = runTest {
        // Given
        fileIndexer.addDirectory(tempDir, recursive = true)
        
        // When
        val count = fileIndexer.removeDirectory(subDir)
        
        // Then
        assertEquals(1, count)
        
        val subDirFiles = fileIndexer.findFilesWithWord("subdirectory")
        assertTrue(subDirFiles.isEmpty())
        
        val allFiles = fileIndexer.getIndexedFiles().toList()
        assertEquals(2, allFiles.size)
        assertTrue(allFiles.contains(textFile1))
        assertTrue(allFiles.contains(textFile2))
        assertFalse(allFiles.contains(textFileInSubDir))
    }
    
    @Test
    fun `test clearIndex removes all files from index`() = runTest {
        // Given
        fileIndexer.addDirectory(tempDir, recursive = true)
        
        // When
        fileIndexer.clearIndex()
        
        // Then
        val allFiles = fileIndexer.getIndexedFiles().toList()
        assertTrue(allFiles.isEmpty())
        
        val anyWordFiles = fileIndexer.findFilesWithWord("test")
        assertTrue(anyWordFiles.isEmpty())
    }
}