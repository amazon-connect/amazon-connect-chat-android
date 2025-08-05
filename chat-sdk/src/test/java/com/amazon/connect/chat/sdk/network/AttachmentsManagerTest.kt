package com.amazon.connect.chat.sdk.network

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.amazon.connect.chat.sdk.network.api.APIClient
import com.amazon.connect.chat.sdk.network.api.AttachmentsInterface
import com.amazon.connect.chat.sdk.network.api.MetricsInterface
import com.amazon.connect.chat.sdk.repository.AttachmentsManager
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.GetAttachmentResult
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.UploadMetadata
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.eq
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

    private lateinit var awsClient: AWSClientImpl
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
        awsClient = spy(AWSClientImpl(mockClient))
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
        attachmentsManager = spy(AttachmentsManager(context, awsClient, apiClient))
    }

    @Test
    fun test_sendAttachment_success() = runTest {
        // Create a latch that will be counted down when completeAttachmentUpload is invoked.
        val latch = CountDownLatch(1)

        // Stub the methods that sendAttachment uses.
        doReturn(mockStartAttachmentUploadRequest)
            .`when`(attachmentsManager)
            .createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        doReturn(mockStartAttachmentUploadResult)
            .`when`(awsClient)
            .startAttachmentUpload(mockConnectionToken, mockStartAttachmentUploadRequest)

        // Stub completeAttachmentUpload so that when it is eventually called, we count down the latch.
        doAnswer {
            latch.countDown()
        }.`when`(attachmentsManager).completeAttachmentUpload(mockConnectionToken, mockAttachmentId)

        // Stub apiClient.uploadAttachment to trigger its callback with a "successful" response.
        doAnswer { invocation ->
            // The lambda is the 4th parameter.
            val callback = invocation.getArgument<(RetrofitResponse<*>?) -> Unit>(3)
            val mockResponse = mock(RetrofitResponse::class.java)
            // Simulate a successful response.
            `when`(mockResponse.isSuccessful).thenReturn(true)
            callback(mockResponse)
            null
        }.`when`(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())

        // When: call sendAttachment. (Make sure attachmentsManager is a spy so that we can verify internal calls.)
        attachmentsManager.sendAttachment(mockConnectionToken, mockUri)

        // Wait for up to 1 second for completeAttachmentUpload to be invoked.
        latch.await(1, TimeUnit.SECONDS)

        // Then: verify the expected interactions.
        verify(awsClient).startAttachmentUpload(anyString(), anyOrNull())
        verify(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())
        verify(attachmentsManager).completeAttachmentUpload(mockConnectionToken, mockAttachmentId)
    }

    @Test
    fun test_sendAttachment_uploadAttachmentFailure() = runTest {
        doReturn(mockStartAttachmentUploadRequest)
            .`when`(attachmentsManager)
            .createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        doReturn(mockStartAttachmentUploadResult)
            .`when`(awsClient)
            .startAttachmentUpload(mockConnectionToken, mockStartAttachmentUploadRequest)

        doAnswer { invocation ->
            val callback = invocation.getArgument<((Response?) -> Unit)>(3)
            callback(null)
            null
        }.`when`(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())

        attachmentsManager.sendAttachment(mockConnectionToken, mockUri)
        verify(awsClient).startAttachmentUpload(anyString(), anyOrNull())
        verify(apiClient).uploadAttachment(anyString(), anyMap(), anyOrNull(), anyOrNull())
        verify(attachmentsManager, never()).completeAttachmentUpload(anyString(), anyString())
    }

    @Test
    fun test_sendAttachment_startAttachmentUploadFailure() = runTest {
        doReturn(mockStartAttachmentUploadRequest)
            .`when`(attachmentsManager)
            .createStartAttachmentUploadRequest(mockConnectionToken, mockUri)

        doReturn(null)
            .`when`(awsClient)
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
        verify(awsClient, never()).startAttachmentUpload(anyString(), anyOrNull())
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
        doReturn(mockGetAttachmentResult)
            .`when`(awsClient)
            .getAttachment(mockConnectionToken, mockAttachmentId)

        // Stub downloadFile to avoid actual file I/O and return a successful result
        doReturn(Result.success(URL(mockUrl)))
            .`when`(attachmentsManager)
            .downloadFile(URL(mockUrl), mockFilename)

        // Await the completion of downloadAttachment
        attachmentsManager.downloadAttachment(mockConnectionToken, mockAttachmentId, mockFilename)
        
        verify(awsClient).getAttachment(mockConnectionToken, mockAttachmentId)
        verify(attachmentsManager).getAttachmentDownloadUrl(mockConnectionToken, mockAttachmentId)
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

        doReturn(mockCompleteAttachmentUploadResult)
            .`when`(awsClient)
            .completeAttachmentUpload(mockConnectionToken, mockCompleteAttachmentUploadRequest)

        attachmentsManager.completeAttachmentUpload(mockConnectionToken, mockAttachmentId)

        verify(awsClient).completeAttachmentUpload(mockConnectionToken, mockCompleteAttachmentUploadRequest)
    }
}
