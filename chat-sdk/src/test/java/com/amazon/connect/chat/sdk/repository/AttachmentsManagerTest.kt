// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.api.APIClient
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.GetAttachmentResult
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.UploadMetadata
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import retrofit2.Response as RetrofitResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AttachmentsManagerTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var awsClient: AWSClient

    @Mock
    private lateinit var apiClient: APIClient

    @Mock
    private lateinit var mockUri: Uri

    @Mock
    private lateinit var mockCursor: Cursor

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var attachmentsManager: AttachmentsManager

    private val mockConnectionToken = "test-connection-token"
    private val mockAttachmentId = "test-attachment-id"
    private val mockFileName = "test-file.txt"
    private val mockContentType = "text/plain"
    private val mockFileSize = 1024L
    private val mockUrl = "https://example.com/upload"
    private val mockHeaders = mapOf("Content-Type" to "text/plain", "Authorization" to "Bearer token")

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(mockUri)).thenReturn(mockContentType)

        // Setup cursor mock
        `when`(contentResolver.query(mockUri, null, null, null, null)).thenReturn(mockCursor)
        `when`(mockCursor.moveToFirst()).thenReturn(true)
        `when`(mockCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)).thenReturn(0)
        `when`(mockCursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(1)
        `when`(mockCursor.getString(0)).thenReturn(mockFileName)
        `when`(mockCursor.getLong(1)).thenReturn(mockFileSize)

        // Setup cache directory
        val cacheDir = tempFolder.newFolder("cache")
        `when`(context.cacheDir).thenReturn(cacheDir)

        attachmentsManager = spy(AttachmentsManager(context, awsClient, apiClient))
    }

    @Test
    fun `test createStartAttachmentUploadRequest creates request with correct values`() {
        val request = attachmentsManager.createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        assertEquals(mockConnectionToken, request.connectionToken)
        assertEquals(mockFileName, request.attachmentName)
        assertEquals(mockContentType, request.contentType)
        assertEquals(mockFileSize, request.attachmentSizeInBytes)
    }

    @Test
    fun `test getFileExtension returns correct extension`() {
        // Test various file extensions
        assertEquals("txt", attachmentsManager.getFileExtension("file.txt"))
        assertEquals("pdf", attachmentsManager.getFileExtension("document.pdf"))
        assertEquals("jpg", attachmentsManager.getFileExtension("image.jpg"))
        assertEquals("xlsx", attachmentsManager.getFileExtension("spreadsheet.xlsx"))
        assertEquals("txt", attachmentsManager.getFileExtension("file.name.with.multiple.dots.txt"))
        
        // For a file without extension, the regex will match the entire filename
        // This is the actual behavior of the implementation
        assertEquals("file_without_extension", attachmentsManager.getFileExtension("file_without_extension"))
    }

    @Test
    fun `test fileFromContentUri creates temporary file with correct extension`() {
        val testContent = "Test file content"
        val inputStream = ByteArrayInputStream(testContent.toByteArray())
        `when`(contentResolver.openInputStream(mockUri)).thenReturn(inputStream)

        val result = attachmentsManager.fileFromContentUri(mockUri, "txt")

        assertTrue(result.exists())
        assertEquals("temp_file.txt", result.name)
        assertTrue(result.readText().contains(testContent))
    }

    @Test
    fun `test getAttachmentDownloadUrl success`() = runTest {
        val getAttachmentResult = GetAttachmentResult()
        getAttachmentResult.url = mockUrl
        
        val mockResult = Result.success(getAttachmentResult)
        `when`(awsClient.getAttachment(mockConnectionToken, mockAttachmentId)).thenReturn(mockResult)

        val result = attachmentsManager.getAttachmentDownloadUrl(mockAttachmentId, mockConnectionToken)

        assertTrue(result.isSuccess)
        assertEquals(URL(mockUrl), result.getOrNull())
    }

    @Test
    fun `test getAttachmentDownloadUrl failure`() = runTest {
        val exception = IOException("Invalid URL")
        val mockResult = Result.failure<GetAttachmentResult>(exception)
        `when`(awsClient.getAttachment(mockConnectionToken, mockAttachmentId)).thenReturn(mockResult)

        val result = attachmentsManager.getAttachmentDownloadUrl(mockAttachmentId, mockConnectionToken)

        assertTrue(result.isFailure)
        assertEquals("Invalid URL", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test completeAttachmentUpload success`() = runTest {
        val completeResult = CompleteAttachmentUploadResult()
        val mockResult = Result.success(completeResult)
        
        // Create a concrete request that we'll use for verification
        val expectedRequest = CompleteAttachmentUploadRequest().apply {
            connectionToken = mockConnectionToken
            setAttachmentIds(listOf(mockAttachmentId))
        }
        
        // Setup the mock to return success for our specific request
        `when`(awsClient.completeAttachmentUpload(mockConnectionToken, expectedRequest))
            .thenReturn(mockResult)

        attachmentsManager.completeAttachmentUpload(mockConnectionToken, mockAttachmentId)

        // Then - verify the method was called with our specific parameters
        verify(awsClient).completeAttachmentUpload(mockConnectionToken, expectedRequest)
    }

    @Test
    fun `test sendAttachment with unsupported file type fails`() = runTest {
        val startRequest = StartAttachmentUploadRequest().apply {
            connectionToken = mockConnectionToken
            attachmentName = "test.unsupported"
            contentType = "application/octet-stream"
        }

        // Mock the getFileExtension method to return "unsupported"
        Mockito.doReturn("unsupported").`when`(attachmentsManager).getFileExtension("test.unsupported")
        Mockito.doReturn(startRequest).`when`(attachmentsManager).createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        val result = attachmentsManager.sendAttachment(mockConnectionToken, mockUri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported file type") == true)
    }
}