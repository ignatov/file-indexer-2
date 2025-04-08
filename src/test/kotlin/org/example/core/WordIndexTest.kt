package org.example.core

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WordIndexTest {
    
    private lateinit var wordIndex: WordIndex
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var file1: Path
    private lateinit var file2: Path
    private lateinit var file3: Path
    
    @BeforeEach
    fun setUp() {
        wordIndex = WordIndex()
        
        // Create test file paths
        file1 = tempDir.resolve("file1.txt")
        file2 = tempDir.resolve("file2.txt")
        file3 = tempDir.resolve("file3.txt")
    }
    
    @Test
    fun `test addWord adds word-file mapping correctly`() {
        // When
        wordIndex.addWord("test", file1)
        
        // Then
        val files = wordIndex.findFilesWithWord("test")
        assertEquals(1, files.size)
        assertEquals(file1, files[0])
        assertTrue(wordIndex.containsFile(file1))
    }
    
    @Test
    fun `test addWord is case insensitive`() {
        // When
        wordIndex.addWord("Test", file1)
        
        // Then
        val files = wordIndex.findFilesWithWord("test")
        assertEquals(1, files.size)
        assertEquals(file1, files[0])
        
        val filesUpperCase = wordIndex.findFilesWithWord("TEST")
        assertEquals(1, filesUpperCase.size)
        assertEquals(file1, filesUpperCase[0])
    }
    
    @Test
    fun `test addWords adds multiple words correctly`() {
        // When
        val words = listOf("test", "example", "kotlin")
        wordIndex.addWords(words, file1)
        
        // Then
        val testFiles = wordIndex.findFilesWithWord("test")
        assertEquals(1, testFiles.size)
        assertEquals(file1, testFiles[0])
        
        val exampleFiles = wordIndex.findFilesWithWord("example")
        assertEquals(1, exampleFiles.size)
        assertEquals(file1, exampleFiles[0])
        
        val kotlinFiles = wordIndex.findFilesWithWord("kotlin")
        assertEquals(1, kotlinFiles.size)
        assertEquals(file1, kotlinFiles[0])
    }
    
    @Test
    fun `test findFilesWithWord returns correct files`() {
        // Given
        wordIndex.addWord("common", file1)
        wordIndex.addWord("common", file2)
        wordIndex.addWord("unique", file3)
        
        // When
        val commonFiles = wordIndex.findFilesWithWord("common")
        val uniqueFiles = wordIndex.findFilesWithWord("unique")
        val nonExistentFiles = wordIndex.findFilesWithWord("nonexistent")
        
        // Then
        assertEquals(2, commonFiles.size)
        assertTrue(commonFiles.contains(file1))
        assertTrue(commonFiles.contains(file2))
        
        assertEquals(1, uniqueFiles.size)
        assertEquals(file3, uniqueFiles[0])
        
        assertTrue(nonExistentFiles.isEmpty())
    }
    
    @Test
    fun `test getAllFiles returns all indexed files`() {
        // Given
        wordIndex.addWord("word1", file1)
        wordIndex.addWord("word2", file2)
        wordIndex.addWord("word3", file3)
        
        // When
        val allFiles = wordIndex.getAllFiles()
        
        // Then
        assertEquals(3, allFiles.size)
        assertTrue(allFiles.contains(file1))
        assertTrue(allFiles.contains(file2))
        assertTrue(allFiles.contains(file3))
    }
    
    @Test
    fun `test removeFile removes file and its word mappings`() {
        // Given
        wordIndex.addWord("common", file1)
        wordIndex.addWord("common", file2)
        wordIndex.addWord("unique", file1)
        
        // When
        val result = wordIndex.removeFile(file1)
        
        // Then
        assertTrue(result)
        assertFalse(wordIndex.containsFile(file1))
        assertTrue(wordIndex.containsFile(file2))
        
        val commonFiles = wordIndex.findFilesWithWord("common")
        assertEquals(1, commonFiles.size)
        assertEquals(file2, commonFiles[0])
        
        val uniqueFiles = wordIndex.findFilesWithWord("unique")
        assertTrue(uniqueFiles.isEmpty())
    }
    
    @Test
    fun `test removeFile returns false for non-indexed file`() {
        // When
        val result = wordIndex.removeFile(file1)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `test clear removes all entries`() {
        // Given
        wordIndex.addWord("word1", file1)
        wordIndex.addWord("word2", file2)
        
        // When
        wordIndex.clear()
        
        // Then
        assertTrue(wordIndex.getAllFiles().isEmpty())
        assertTrue(wordIndex.findFilesWithWord("word1").isEmpty())
        assertTrue(wordIndex.findFilesWithWord("word2").isEmpty())
    }
    
    @Test
    fun `test containsFile returns correct result`() {
        // Given
        wordIndex.addWord("word", file1)
        
        // Then
        assertTrue(wordIndex.containsFile(file1))
        assertFalse(wordIndex.containsFile(file2))
    }
}