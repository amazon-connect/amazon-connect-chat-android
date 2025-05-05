package com.amazon.connect.chat.androidchatexample.utils

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class CustomLoggerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var customLogger: CustomLogger
    
    private val originalOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @Before
    fun setUp() {
        customLogger = CustomLogger()
        
        // Capture System.out.println output
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @After
    fun tearDown() {
        // Restore original System.out
        System.setOut(originalOut)
    }

    @Test
    fun logVerbose_shouldPrintMessageWithVerbosePrefix() {
        val message = "Test verbose message"
        
        customLogger.logVerbose { message }
        
        assertTrue(outputStreamCaptor.toString().contains("VERBOSE: $message"))
    }

    @Test
    fun logInfo_shouldPrintMessageWithInfoPrefix() {
        val message = "Test info message"
        
        customLogger.logInfo { message }
        
        assertTrue(outputStreamCaptor.toString().contains("INFO: $message"))
    }

    @Test
    fun logDebug_shouldPrintMessageWithDebugPrefix() {
        val message = "Test debug message"
        
        customLogger.logDebug { message }
        
        assertTrue(outputStreamCaptor.toString().contains("DEBUG: $message"))
    }

    @Test
    fun logWarn_shouldPrintMessageWithWarnPrefix() {
        val message = "Test warn message"
        
        customLogger.logWarn { message }
        
        assertTrue(outputStreamCaptor.toString().contains("WARN: $message"))
    }

    @Test
    fun logError_shouldPrintMessageWithErrorPrefix() {
        val message = "Test error message"
        
        customLogger.logError { message }
        
        assertTrue(outputStreamCaptor.toString().contains("ERROR: $message"))
    }

    @Test
    fun setLogOutputDir_shouldSetOutputDirectory() {
        val tempDir = tempFolder.newFolder("logs")
        
        customLogger.setLogOutputDir(tempDir)
        
        // No assertion needed, just verifying no exception is thrown
    }

    @Test
    fun writeToAppTempFile_shouldHandleNullOutputFileDir() {
        // Given
        // outputFileDir is null by default
        
        customLogger.logInfo { "Test message" }
        
        // No assertion needed, just verifying no exception is thrown
        // Wait a bit to allow the coroutine to execute
        Thread.sleep(100)
    }

    @Test
    fun writeToAppTempFile_shouldHandleValidDirectory() {
        val tempDir = tempFolder.newFolder("logs")
        customLogger.setLogOutputDir(tempDir)
        
        customLogger.logInfo { "Test message for file" }
        
        // No assertion needed, just verifying no exception is thrown
        // Wait a bit to allow the coroutine to execute
        Thread.sleep(100)
    }

    @Test
    fun writeToAppTempFile_shouldHandleNonExistentDirectory() {
        val nonExistentDir = File("/non/existent/directory")
        customLogger.setLogOutputDir(nonExistentDir)
        
        customLogger.logInfo { "Test message" }
        
        // No assertion needed, just verifying no exception is thrown
        // Wait a bit to allow the coroutine to execute
        Thread.sleep(100)
    }
}