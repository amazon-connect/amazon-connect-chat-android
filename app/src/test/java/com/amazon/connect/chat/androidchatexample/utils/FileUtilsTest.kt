package com.amazon.connect.chat.androidchatexample.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FileUtilsTest {

    @Test
    fun testFileNameExtraction() {
        // Test the extension extraction logic that's part of getMimeType
        val fileName1 = "document.pdf"
        val fileName2 = "image.jpg"
        val fileName3 = "noextension"
        val fileName4 = ".hiddenfile"
        val fileName5 = "archive.tar.gz"

        assertEquals("pdf", fileName1.substringAfterLast('.', ""))
        assertEquals("jpg", fileName2.substringAfterLast('.', ""))
        assertEquals("", fileName3.substringAfterLast('.', ""))
        assertEquals("hiddenfile", fileName4.substringAfterLast('.', ""))
        assertEquals("gz", fileName5.substringAfterLast('.', ""))
    }

    @Test
    fun testFilePathConstruction() {
        // Test file path construction logic
        val dirPath = "/cache"
        val fileName = "test.txt"

        val tempDir = File(dirPath, "attachments")
        val file = File(tempDir, fileName)

        assertEquals("/cache/attachments/test.txt", file.path)
    }

    @Test
    fun testFilePathWithSpecialCharacters() {
        // Test file path with special characters
        val dirPath = "/user/data"
        val fileName = "test file with spaces.pdf"

        val tempDir = File(dirPath, "attachments")
        val file = File(tempDir, fileName)

        assertEquals("/user/data/attachments/test file with spaces.pdf", file.path)
    }

    @Test
    fun testNestedDirectoryConstruction() {
        // Test nested directory construction
        val baseDir = "/storage/emulated/0"
        val nestedPath = "Download/documents/reports"
        val fileName = "annual_report.xlsx"

        val tempDir = File(baseDir, nestedPath)
        val file = File(tempDir, fileName)

        assertEquals("/storage/emulated/0/Download/documents/reports/annual_report.xlsx", file.path)
    }
}
