package org.example.core

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

class DirectoryWatcherTest {
    
    private lateinit var indexService: IndexService
    private lateinit var directoryWatcher: DirectoryWatcher
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeEach
    fun setUp() {
        // Create a mock IndexService
        indexService = mockk(relaxed = true)
        
        // Create the DirectoryWatcher with the mock IndexService
        directoryWatcher = DirectoryWatcher(indexService)
    }
    
    @AfterEach
    fun tearDown() {
        // Stop watching all directories to clean up resources
        directoryWatcher.stopAll()
    }
    
    @Test
    fun `test watchDirectory starts watching successfully`() {
        // When
        val result = directoryWatcher.watchDirectory(tempDir)
        
        // Then
        assertTrue(result)
        assertTrue(directoryWatcher.getWatchedDirectories().contains(tempDir))
    }
    
    @Test
    fun `test watchDirectory returns true when already watching`() {
        // Given
        directoryWatcher.watchDirectory(tempDir)
        
        // When
        val result = directoryWatcher.watchDirectory(tempDir)
        
        // Then
        assertTrue(result)
        assertEquals(1, directoryWatcher.getWatchedDirectories().size)
    }
    
    @Test
    fun `test stopWatching stops watching a directory`() {
        // Given
        directoryWatcher.watchDirectory(tempDir)
        
        // When
        val result = directoryWatcher.stopWatching(tempDir)
        
        // Then
        assertTrue(result)
        assertFalse(directoryWatcher.getWatchedDirectories().contains(tempDir))
    }
    
    @Test
    fun `test stopWatching returns false for non-watched directory`() {
        // When
        val result = directoryWatcher.stopWatching(tempDir)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `test stopAll stops watching all directories`() {
        // Given
        val tempDir2 = Files.createTempDirectory("test2")
        directoryWatcher.watchDirectory(tempDir)
        directoryWatcher.watchDirectory(tempDir2)
        
        // When
        directoryWatcher.stopAll()
        
        // Then
        assertTrue(directoryWatcher.getWatchedDirectories().isEmpty())
        
        // Clean up
        Files.deleteIfExists(tempDir2)
    }
    
    @Test
    fun `test getWatchedDirectories returns correct directories`() {
        // Given
        directoryWatcher.watchDirectory(tempDir)
        
        // When
        val watchedDirs = directoryWatcher.getWatchedDirectories()
        
        // Then
        assertEquals(1, watchedDirs.size)
        assertTrue(watchedDirs.contains(tempDir))
    }
    
    // Note: Testing the actual file system events is challenging in a unit test
    // as it requires simulating file system events. The following test is more
    // of an integration test and may be flaky depending on the environment.
    @Test
    fun `test file events trigger IndexService methods`() = runTest {
        // Set up mocks to return success
        coEvery { indexService.addFile(any()) } returns true
        coEvery { indexService.removeFile(any()) } returns true
        
        // Start watching the directory
        directoryWatcher.watchDirectory(tempDir)
        
        // Create a test file (this should trigger a CREATE event)
        val testFile = tempDir.resolve("test-event.txt")
        Files.writeString(testFile, "Test content")
        
        // Give some time for the event to be processed
        delay(500)
        
        // Verify that addFile was called
        coVerify(timeout = 2000) { indexService.addFile(testFile) }
        
        // Modify the file (this should trigger a MODIFY event)
        Files.writeString(testFile, "Modified content")
        
        // Give some time for the event to be processed
        delay(500)
        
        // Verify that removeFile and addFile were called
        coVerify(timeout = 2000) { indexService.removeFile(testFile) }
        coVerify(timeout = 2000, atLeast = 2) { indexService.addFile(testFile) }
        
        // Delete the file (this should trigger a DELETE event)
        Files.delete(testFile)
        
        // Give some time for the event to be processed
        delay(500)
        
        // Verify that removeFile was called again
        coVerify(timeout = 2000, atLeast = 2) { indexService.removeFile(testFile) }
    }
}