package org.example.core

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import io.methvin.watcher.DirectoryWatcher as JDirectoryWatcher

/**
 * Watches directories for file changes and updates the index accordingly.
 */
class DirectoryWatcher(private val indexService: IndexService) {
    private val logger = LoggerFactory.getLogger(DirectoryWatcher::class.java)
    private val watchers = ConcurrentHashMap<Path, JDirectoryWatcher>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Starts watching a directory for changes.
     * @param directoryPath The directory to watch
     * @return true if watching started successfully, false otherwise
     */
    fun watchDirectory(directoryPath: Path): Boolean {
        if (watchers.containsKey(directoryPath)) {
            logger.info("Already watching directory: $directoryPath")
            return true
        }
        
        try {
            val watcher = JDirectoryWatcher.builder()
                .path(directoryPath)
                .listener(createDirectoryChangeListener())
                .fileHashing(false)
                .build()
            
            watchers[directoryPath] = watcher
            watcher.watchAsync()
            logger.info("Started watching directory: $directoryPath")
            return true
        } catch (e: Exception) {
            logger.error("Failed to watch directory: $directoryPath", e)
            return false
        }
    }
    
    /**
     * Stops watching a directory.
     * @param directoryPath The directory to stop watching
     * @return true if watching stopped successfully, false otherwise
     */
    fun stopWatching(directoryPath: Path): Boolean {
        val watcher = watchers.remove(directoryPath)
        if (watcher != null) {
            try {
                watcher.close()
                logger.info("Stopped watching directory: $directoryPath")
                return true
            } catch (e: Exception) {
                logger.error("Failed to stop watching directory: $directoryPath", e)
                return false
            }
        }
        return false
    }
    
    /**
     * Stops watching all directories.
     */
    fun stopAll() {
        watchers.keys.forEach { stopWatching(it) }
    }
    
    /**
     * Creates a listener for directory changes.
     */
    private fun createDirectoryChangeListener(): DirectoryChangeListener {
        return DirectoryChangeListener { event ->
            val path = event.path()
            
            coroutineScope.launch {
                when (event.eventType()) {
                    DirectoryChangeEvent.EventType.CREATE -> {
                        logger.info("File created: $path")
                        indexService.addFile(path)
                    }
                    DirectoryChangeEvent.EventType.MODIFY -> {
                        logger.info("File modified: $path")
                        indexService.removeFile(path)
                        indexService.addFile(path)
                    }
                    DirectoryChangeEvent.EventType.DELETE -> {
                        logger.info("File deleted: $path")
                        indexService.removeFile(path)
                    }
                    else -> {
                        logger.info("Unknown event for file: $path")
                    }
                }
            }
        }
    }
    
    /**
     * Gets the list of directories being watched.
     * @return Set of watched directory paths
     */
    fun getWatchedDirectories(): Set<Path> {
        return watchers.keys
    }
}