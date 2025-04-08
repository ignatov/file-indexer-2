package org.example

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FileIndexerReplTest {

    @Test
    fun `test expandTilde expands tilde to home directory`() {
        // Create a test instance of FileIndexerRepl
        val repl = FileIndexerRepl::class.java
        
        // Get the expandTilde method using reflection
        val expandTildeMethod = repl.getDeclaredMethod("expandTilde", String::class.java)
        expandTildeMethod.isAccessible = true
        
        // Create an instance of FileIndexerRepl
        val indexService = org.example.core.FileIndexer()
        val replInstance = FileIndexerRepl(indexService)
        
        // Test with a path starting with ~
        val testPath = "~/Documents/test.txt"
        val userHome = System.getProperty("user.home")
        val expected = "$userHome/Documents/test.txt"
        
        val result = expandTildeMethod.invoke(replInstance, testPath) as String
        assertEquals(expected, result)
        
        // Test with a path not starting with ~
        val normalPath = "/usr/local/bin"
        val normalResult = expandTildeMethod.invoke(replInstance, normalPath) as String
        assertEquals(normalPath, normalResult)
    }
}