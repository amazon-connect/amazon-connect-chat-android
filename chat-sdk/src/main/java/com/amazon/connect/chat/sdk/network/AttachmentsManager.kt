package com.amazon.connect.chat.sdk.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import javax.inject.Inject
import java.util.*
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class AttachmentsManager @Inject constructor(
    private val context: Context,
    private val awsClient: AWSClient,
    private var apiClient: APIClient
) {

    suspend fun sendAttachment(connectionToken: String, fileUri: Uri) : Result<String> {
        return runCatching {
            val startAttachmentUploadRequest =
                createStartAttachmentUploadRequest(connectionToken, fileUri)
            val fileExtension = getFileExtension(startAttachmentUploadRequest.attachmentName)
            if (!Constants.attachmentTypeMap.containsKey(fileExtension)) {
                Log.d("AttachmentsManager", "Unsupported file type: $fileExtension")
                throw Exception("Unsupported file type")
            }

            val startAttachmentResult =
                awsClient.startAttachmentUpload(connectionToken, startAttachmentUploadRequest)

            val startAttachmentResponse = startAttachmentResult.getOrNull()
                ?: throw startAttachmentResult.exceptionOrNull() ?: Exception("Error starting attachment upload")

            val attachmentId = startAttachmentResponse.attachmentId

            val file = fileFromContentUri(fileUri, fileExtension)

            apiClient.uploadAttachment(
                startAttachmentResponse.uploadMetadata.url,
                startAttachmentResponse.uploadMetadata.headersToInclude,
                file
            ) { response ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (response != null && response.isSuccessful) {
                        completeAttachmentUpload(connectionToken, attachmentId)
                    } else {
                        val exception = response?.message()
                        println("Error occurred: $exception")
                    }
                    file.deleteRecursively()
                }
            }

            attachmentId
        }
    }

    suspend fun completeAttachmentUpload(connectionToken: String, attachmentId: String) {
        val request = CompleteAttachmentUploadRequest().apply {
            this.connectionToken = connectionToken
            this.setAttachmentIds(listOf(attachmentId))
        }
        val completeAttachmentUploadResult = awsClient.completeAttachmentUpload(connectionToken, request)

        val completeAttachmentUploadResponse = completeAttachmentUploadResult.getOrNull()
        if (completeAttachmentUploadResponse == null) {
            val exception = completeAttachmentUploadResult.exceptionOrNull()
            println("Error occurred: ${exception?.message}")
            return
        }
    }

    fun createStartAttachmentUploadRequest(connectionToken: String, fileUri: Uri): StartAttachmentUploadRequest {
        var attachmentName: String? = null
        var attachmentSize: Long? = null
        val contentType = context.contentResolver.getType(fileUri)

        context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex != -1) {
                    attachmentName = cursor.getString(nameIndex)
                }
                if (sizeIndex != -1) {
                    attachmentSize = cursor.getLong(sizeIndex)
                }
            }
        }
        val request = StartAttachmentUploadRequest().apply {
            this.connectionToken = connectionToken
            this.attachmentName = attachmentName
            this.contentType = contentType
            this.attachmentSizeInBytes = attachmentSize
        }
        return request
    }

    suspend fun downloadAttachment(
        connectionToken: String,
        attachmentId: String,
        fileName: String,
    ): Result<URL> {
        return getAttachmentDownloadUrl(connectionToken, attachmentId).mapCatching { url ->
            downloadFile(url, fileName).getOrThrow()
        }.onFailure {
            Log.e("AttachmentsManager", "Error occurred during downloadAttachment: ${it.message}")
        }
    }

    suspend fun getAttachmentDownloadUrl(connectionToken: String, attachmentId: String): Result<URL> {
        return runCatching {
            val response = awsClient.getAttachment(connectionToken, attachmentId)
            URL(response.getOrNull()?.url ?: throw IOException("Invalid URL"))
        }.onFailure {
            Log.e("AttachmentsManager", "Error occurred during getAttachmentDownloadUrl: ${it.message}")
        }
    }

    suspend fun downloadFile(url: URL, fileName: String): Result<URL> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Failed to download file: HTTP response code ${connection.responseCode}")
            }

            val tempDirectory = File.createTempFile("temp", null).parentFile
            if (!tempDirectory.exists()) {
                tempDirectory.mkdirs()
            }

            val tempFilePath = File(tempDirectory, fileName)

            connection.inputStream.use { input ->
                tempFilePath.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFilePath.exists()) {
                throw IOException("File was not created: ${tempFilePath.absolutePath}")
            }

            tempFilePath.also {
                Log.d("AttachmentsManager", "File successfully downloaded to: ${it.absolutePath}")
            }.toURI().toURL()
        }.onFailure {
            Log.e("AttachmentsManager", "Error occurred during downloadFile: ${it.message}")
        }
    }

    fun fileFromContentUri(contentUri: Uri, fileExtension: String?): File {

        val fileName = "temp_file" + if (fileExtension != null) ".$fileExtension" else ""

        val tempFile = File(context.cacheDir, fileName)
        tempFile.createNewFile()
        try {
            val outputStream = FileOutputStream(tempFile)
            val inputStream = context.contentResolver.openInputStream(contentUri)

            inputStream?.let {
                copy(inputStream, outputStream)
            }

            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tempFile
    }

    fun getFileExtension(filename: String): String? {
        val fileExtensionRegex = """[^.]+$""".toRegex()
        return fileExtensionRegex.find(filename)?.value
    }

    @Throws(IOException::class)
    private fun copy(source: InputStream, target: OutputStream) {
        val buf = ByteArray(8192)
        var length: Int
        while (source.read(buf).also { length = it } > 0) {
            target.write(buf, 0, length)
        }
    }
}
