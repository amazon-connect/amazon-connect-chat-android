// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.network.api.APIClient
import com.amazon.connect.chat.sdk.network.api.AttachmentsInterface
import com.amazon.connect.chat.sdk.network.api.MetricsInterface
import com.amazon.connect.chat.sdk.repository.AttachmentsManager
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.GetAttachmentResult
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.UploadMetadata
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import retrofit2.Response as RetrofitResponse

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AttachmentsManagerTest {

    @Mock
    private lateinit var mockClient: AmazonConnectParticipantClient

    @Mock
    private lateinit var attachmentsInterface: AttachmentsInterface

    @Mock
    private lateinit var metricsInterface: MetricsInterface

    @Mock
    private lateinit var mockAwsClient: AWSClient

    private lateinit var apiClient: APIClient
    private lateinit var attachmentsManager: AttachmentsManager

    private val context = mock(Context::class.java)
    private val contentResolver = mock(ContentResolver::class.java)
    private val mockStartAttachmentUploadRequest = StartAttachmentUploadRequest()
    private val mockStartAttachmentUploadResult = StartAttachmentUploadResult()
    private val mockGetAttachmentResult = GetAttachmentResult()

    private val mockUri: Uri = Uri.parse("https://example.com/dummy")
    private val mockAttachmentId = "12345"
    private val mockUploadMetadata = UploadMetadata()
    private val mockUrl = "https://example.com/"
    private val mockHeadersToInclude = mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3")
    private val mockConnectionToken = "connectionToken"
    private val mockFilename = "example.txt"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        apiClient = spy(APIClient(metricsInterface, attachmentsInterface))
        `when`(context.contentResolver).thenReturn(contentResolver)
        mockStartAttachmentUploadRequest.apply {
            this.connectionToken = mockConnectionToken
            this.attachmentName = "example.txt"
            this.contentType = "text/plain"
            this.attachmentSizeInBytes = 1000
        }
        mockUploadMetadata.apply {
            this.url = mockUrl
            this.headersToInclude = mockHeadersToInclude
        }
        mockStartAttachmentUploadResult.apply {
            this.attachmentId = mockAttachmentId
            this.uploadMetadata = mockUploadMetadata
        }
        // Create AttachmentsManager with mock AWSClient
        attachmentsManager = spy(AttachmentsManager(context, mockAwsClient, apiClient))
        
        // Configure with GlobalConfig that uses our mock client
        val config = GlobalConfig(region = Regions.US_WEST_2, customAWSClient = mockAwsClient)
        attachmentsManager.configure(config)
    }

    @Test
    fun test_sendAttachment_success() = runTest {
        val latch = CountDownLatch(1)

        doReturn(mockStartAttachmentUploadRequest)
            .`when`(attachmentsManager)
            .createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        doReturn(Result.success(mockStartAttachmentUploadResult))
            .`when`(mockAwsClient)
            .startAttachmentUpload(mockConnectionToken, mockStartAttachmentUploadRequest)

        doAnswer {
            latch.countDown()
        }.`when`(attachmentsManager).completeAttachmentUpload(mockConnectionToken, mockAttachmentId)

        doAnswer { invocation ->
            val callback = invocation.getArgument<(RetrofitResponse<*>?) -> Unit>(3)
            val mockResponse = mock(RetrofitResponse::class.java)
            `when`(mockResponse.isSuccessful).thenReturn(true)
            callback(mockResponse)
            null
        }.`when`(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())

        attachmentsManager.sendAttachment(mockConnectionToken, mockUri)

        latch.await(1, TimeUnit.SECONDS)

        verify(mockAwsClient).startAttachmentUpload(anyString(), anyOrNull())
        verify(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())
        verify(attachmentsManager).completeAttachmentUpload(mockConnectionToken, mockAttachmentId)
    }

    @Test
    fun test_sendAttachment_uploadAttachmentFailure() = runTest {
        doReturn(mockStartAttachmentUploadRequest)
            .`when`(attachmentsManager)
            .createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        doReturn(Result.success(mockStartAttachmentUploadResult))
            .`when`(mockAwsClient)
            .startAttachmentUpload(mockConnectionToken, mockStartAttachmentUploadRequest)

        doAnswer { invocation ->
            val callback = invocation.getArgument<((Response?) -> Unit)>(3)
            callback(null)
            null
        }.`when`(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())

        attachmentsManager.sendAttachment(mockConnectionToken, mockUri)
        verify(mockAwsClient).startAttachmentUpload(anyString(), anyOrNull())
        verify(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())
        verify(attachmentsManager, never()).completeAttachmentUpload(anyString(), anyString())
    }

    @Test
    fun test_sendAttachment_startAttachmentUploadFailure() = runTest {
        doReturn(mockStartAttachmentUploadRequest)
            .`when`(attachmentsManager)
            .createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        doReturn(Result.failure<StartAttachmentUploadResult>(Exception("Upload failed")))
            .`when`(mockAwsClient)
            .startAttachmentUpload(mockConnectionToken, mockStartAttachmentUploadRequest)

        attachmentsManager.sendAttachment(mockConnectionToken, mockUri)
        verify(apiClient, never()).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())
    }

    @Test
    fun test_sendAttachment_invalidType() = runTest {
        mockStartAttachmentUploadRequest.apply {
            this.attachmentName = "example.asdf"
        }
        doReturn(mockStartAttachmentUploadRequest)
            .`when`(attachmentsManager)
            .createStartAttachmentUploadRequest(mockConnectionToken, mockUri)
        attachmentsManager.sendAttachment(mockConnectionToken, mockUri)
        verify(mockAwsClient, never()).startAttachmentUpload(anyString(), anyOrNull())
    }

    @Test
    fun test_fileFromContentUri() = runTest {
        val mockCacheDir = File("test/cache")
        mockCacheDir.mkdirs()
        val tempFile = File(mockCacheDir, "example.txt")
        tempFile.createNewFile()
        tempFile.writeText("Write test file content")
        val testInputStream: InputStream = FileInputStream(tempFile)
        `when`(context.cacheDir).thenReturn(mockCacheDir)
        `when`(contentResolver.openInputStream(mockUri)).thenReturn(testInputStream)
        val copiedFile = attachmentsManager.fileFromContentUri(mockUri, "txt")
        assertTrue(copiedFile.isFile)
        assertTrue(copiedFile.path == "test/cache/temp_file.txt")
        assertTrue(copiedFile.length() == tempFile.length())
        File("test").deleteRecursively()
    }

    @Test
    fun test_downloadAttachment() = runTest {
        mockGetAttachmentResult.url = mockUrl
        doReturn(Result.success(mockGetAttachmentResult))
            .`when`(mockAwsClient)
            .getAttachment(mockConnectionToken, mockAttachmentId)

        doReturn(Result.success(URL(mockUrl)))
            .`when`(attachmentsManager)
            .downloadFile(URL(mockUrl), mockFilename)

        attachmentsManager.downloadAttachment(mockConnectionToken, mockAttachmentId, mockFilename)
        
        verify(mockAwsClient).getAttachment(mockConnectionToken, mockAttachmentId)
        verify(attachmentsManager).getAttachmentDownloadUrl(mockAttachmentId, mockConnectionToken)
        verify(attachmentsManager).downloadFile(URL(mockUrl), mockFilename)
    }

    @Test
    fun test_getFileExtension() = runTest {
        for (extension in Constants.attachmentTypeMap.keys) {
            val fileExtension = attachmentsManager.getFileExtension("example.$extension")
            assertTrue(fileExtension == extension)
        }
    }

    @Test
    fun test_completeAttachmentUpload() = runTest {
        val mockCompleteAttachmentUploadRequest = CompleteAttachmentUploadRequest()
        mockCompleteAttachmentUploadRequest.setAttachmentIds(listOf(mockAttachmentId))
        mockCompleteAttachmentUploadRequest.apply {
            this.connectionToken = mockConnectionToken
        }

        val mockCompleteAttachmentUploadResult = CompleteAttachmentUploadResult()

        doReturn(Result.success(mockCompleteAttachmentUploadResult))
            .`when`(mockAwsClient)
            .completeAttachmentUpload(mockConnectionToken, mockCompleteAttachmentUploadRequest)

        attachmentsManager.completeAttachmentUpload(mockConnectionToken, mockAttachmentId)

        verify(mockAwsClient).completeAttachmentUpload(mockConnectionToken, mockCompleteAttachmentUploadRequest)
    }
}
