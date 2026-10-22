// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import aws.sdk.kotlin.services.connectparticipant.ConnectParticipantClient
import aws.sdk.kotlin.services.connectparticipant.endpoints.ConnectParticipantEndpointParameters
import aws.sdk.kotlin.services.connectparticipant.endpoints.DefaultConnectParticipantEndpointProvider
import aws.sdk.kotlin.services.connectparticipant.model.AccessDeniedException
import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadRequest
import aws.sdk.kotlin.services.connectparticipant.model.GetTranscriptRequest
import aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadRequest
import aws.sdk.kotlin.services.connectparticipant.model.StartPosition
import aws.sdk.kotlin.services.connectparticipant.withConfig
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.net.url.Url
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Region
import com.amazon.connect.chat.sdk.model.ScanDirection
import com.amazon.connect.chat.sdk.model.SortKey
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazon.connect.chat.sdk.utils.TranscriptItemUtils
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

/**
 * Exercises the real AWS SDK for Kotlin client configured by [DefaultAWSClient] against a local HTTP
 * server, to verify what goes over the wire: operation paths, X-Amz-Bearer auth, the absence of SigV4
 * signing, User-Agent attribution, request bodies and response parsing.
 */
@RunWith(RobolectricTestRunner::class)
class ConnectParticipantWireTest {

    private lateinit var server: MockWebServer
    private lateinit var client: ConnectParticipantClient

    private val connectionToken = "test-connection-token"
    private val participantToken = "test-participant-token"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = DefaultAWSClient.createConnectParticipantClient(Region.US_WEST_2).withConfig {
            endpointUrl = Url.parse(server.url("/").toString())
        }
        DefaultAWSClient.setClient(client)
    }

    @After
    fun tearDown() {
        client.close()
        server.shutdown()
    }

    private fun enqueueJson(body: String, code: Int = 200, headers: Map<String, String> = emptyMap()) {
        val response = MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
        headers.forEach { (name, value) -> response.setHeader(name, value) }
        server.enqueue(response)
    }

    private fun takeRequest(): RecordedRequest =
        server.takeRequest(5, TimeUnit.SECONDS) ?: throw AssertionError("No request was sent")

    private fun assertParticipantRequest(request: RecordedRequest, method: String, path: String, bearer: String) {
        assertEquals(method, request.method)
        assertEquals(path, request.requestUrl?.encodedPath)
        assertEquals(bearer, request.getHeader("X-Amz-Bearer"))
        assertNull("Requests must not be SigV4 signed", request.getHeader("Authorization"))
        assertNull(request.getHeader("X-Amz-Security-Token"))

        val userAgent = request.getHeader("User-Agent").orEmpty()
        assertTrue("User-Agent was: $userAgent", userAgent.startsWith("aws-sdk-kotlin/"))
        assertTrue("User-Agent was: $userAgent", userAgent.contains(" app/${Constants.USER_AGENT_APP_ID}"))
        assertTrue("User-Agent was: $userAgent", userAgent.endsWith(" ${Constants.USER_AGENT_PRODUCT_TOKEN}"))
        assertEquals(1, userAgent.split(Constants.USER_AGENT_PRODUCT_TOKEN).size - 1)
    }

    @Test
    fun createParticipantConnection_sendsParticipantTokenAndParsesConnection() = runBlocking {
        enqueueJson(
            """{"Websocket":{"Url":"wss://websocket.example","ConnectionExpiry":"2026-10-20T20:00:00.000Z"},
               "ConnectionCredentials":{"ConnectionToken":"new-connection-token","Expiry":"2026-10-21T20:00:00.000Z"}}"""
        )

        val result = DefaultAWSClient.createParticipantConnection(participantToken)

        val details = result.getOrThrow()
        assertEquals("wss://websocket.example", details.websocketUrl)
        assertEquals("new-connection-token", details.connectionToken)
        assertEquals("2026-10-20T20:00:00.000Z", details.expiry)

        val request = takeRequest()
        assertParticipantRequest(request, "POST", "/participant/connection", participantToken)
        val body = JSONObject(request.body.readUtf8())
        assertEquals(JSONArray(listOf("WEBSOCKET", "CONNECTION_CREDENTIALS")).toString(), body.getJSONArray("Type").toString())
    }

    @Test
    fun sendMessage_sendsContentAndParsesMessageId() = runBlocking {
        enqueueJson("""{"Id":"message-id","AbsoluteTime":"2026-10-20T20:00:00.000Z"}""")

        val response = DefaultAWSClient.sendMessage(connectionToken, ContentType.PLAIN_TEXT, "hello").getOrThrow()

        assertEquals("message-id", response.id)
        val request = takeRequest()
        assertParticipantRequest(request, "POST", "/participant/message", connectionToken)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("text/plain", body.getString("ContentType"))
        assertEquals("hello", body.getString("Content"))
        assertTrue("An idempotency token is generated", body.optString("ClientToken").isNotEmpty())
    }

    @Test
    fun sendEvent_sendsEventContent() = runBlocking {
        enqueueJson("""{"Id":"event-id","AbsoluteTime":"2026-10-20T20:00:00.000Z"}""")

        DefaultAWSClient.sendEvent(connectionToken, ContentType.MESSAGE_READ, """{"messageId":"m1"}""").getOrThrow()

        val request = takeRequest()
        assertParticipantRequest(request, "POST", "/participant/event", connectionToken)
        val body = JSONObject(request.body.readUtf8())
        assertEquals(ContentType.MESSAGE_READ.type, body.getString("ContentType"))
        assertEquals("""{"messageId":"m1"}""", body.getString("Content"))
    }

    @Test
    fun sendEvent_omitsEmptyContent() = runBlocking {
        enqueueJson("""{"Id":"event-id","AbsoluteTime":"2026-10-20T20:00:00.000Z"}""")

        DefaultAWSClient.sendEvent(connectionToken, ContentType.TYPING, "").getOrThrow()

        val body = JSONObject(takeRequest().body.readUtf8())
        assertEquals(ContentType.TYPING.type, body.getString("ContentType"))
        assertFalse(body.has("Content"))
    }

    @Test
    fun disconnectParticipant_usesConnectionToken() = runBlocking {
        enqueueJson("{}")

        DefaultAWSClient.disconnectParticipantConnection(connectionToken).getOrThrow()

        assertParticipantRequest(takeRequest(), "POST", "/participant/disconnect", connectionToken)
    }

    @Test
    fun attachmentOperations_useExpectedPathsAndBodies() = runBlocking {
        enqueueJson(
            """{"AttachmentId":"attachment-id","UploadMetadata":{"Url":"https://upload.example",
               "UrlExpiry":"2026-10-20T20:00:00.000Z","HeadersToInclude":{"x-amz-meta-test":"value"}}}"""
        )
        enqueueJson("{}")
        enqueueJson("""{"Url":"https://download.example","UrlExpiry":"2026-10-20T20:00:00.000Z"}""")

        val start = DefaultAWSClient.startAttachmentUpload(
            connectionToken,
            StartAttachmentUploadRequest {
                this.connectionToken = this@ConnectParticipantWireTest.connectionToken
                attachmentName = "file.txt"
                attachmentSizeInBytes = 12L
                contentType = "text/plain"
            }
        ).getOrThrow()
        assertEquals("attachment-id", start.attachmentId)
        assertEquals("https://upload.example", start.uploadMetadata?.url)
        assertEquals(mapOf("x-amz-meta-test" to "value"), start.uploadMetadata?.headersToInclude)

        val startRequest = takeRequest()
        assertParticipantRequest(startRequest, "POST", "/participant/start-attachment-upload", connectionToken)
        val startBody = JSONObject(startRequest.body.readUtf8())
        assertEquals("file.txt", startBody.getString("AttachmentName"))
        assertEquals(12L, startBody.getLong("AttachmentSizeInBytes"))
        assertEquals("text/plain", startBody.getString("ContentType"))

        DefaultAWSClient.completeAttachmentUpload(
            connectionToken,
            CompleteAttachmentUploadRequest {
                this.connectionToken = this@ConnectParticipantWireTest.connectionToken
                attachmentIds = listOf("attachment-id")
            }
        ).getOrThrow()
        val completeRequest = takeRequest()
        assertParticipantRequest(completeRequest, "POST", "/participant/complete-attachment-upload", connectionToken)
        assertEquals("attachment-id", JSONObject(completeRequest.body.readUtf8()).getJSONArray("AttachmentIds").getString(0))

        val attachment = DefaultAWSClient.getAttachment(connectionToken, "attachment-id").getOrThrow()
        assertEquals("https://download.example", attachment.url)
        val getRequest = takeRequest()
        assertParticipantRequest(getRequest, "POST", "/participant/attachment", connectionToken)
        assertEquals("attachment-id", JSONObject(getRequest.body.readUtf8()).getString("AttachmentId"))
    }

    @Test
    fun describeView_usesViewTokenPath() = runBlocking {
        enqueueJson(
            """{"View":{"Arn":"view-arn","Id":"view-id","Name":"view-name","Version":2,
               "Content":{"Actions":["Submit"],"InputSchema":"{}","Template":"<t/>"}}}"""
        )

        val view = DefaultAWSClient.describeView(connectionToken, "view-token").getOrThrow()

        assertEquals("view-id", view.id)
        assertEquals(2, view.version)
        assertEquals(listOf("Submit"), view.content?.actions)
        assertParticipantRequest(takeRequest(), "GET", "/participant/views/view-token", connectionToken)
    }

    @Test
    fun getTranscript_sendsOptionsAndItemsSerializeLikeTheLegacyModels() = runBlocking {
        enqueueJson(
            """{"InitialContactId":"initial-contact","NextToken":"next-token","Transcript":[
                {"Id":"item-1","AbsoluteTime":"2026-10-20T20:00:00.000Z","Content":"hi","ContentType":"text/plain",
                 "Type":"MESSAGE","ParticipantId":"p1","DisplayName":"Agent","ParticipantRole":"AGENT",
                 "Attachments":[{"AttachmentId":"a1","AttachmentName":"file.txt","ContentType":"text/plain","Status":"APPROVED"}],
                 "MessageMetadata":{"MessageId":"item-1","Receipts":[{"DeliveredTimestamp":"2026-10-20T20:00:01.000Z",
                 "ReadTimestamp":"2026-10-20T20:00:02.000Z","RecipientParticipantId":"p2"}]}}]}"""
        )

        val response = DefaultAWSClient.getTranscript(
            GetTranscriptRequest {
                this.connectionToken = this@ConnectParticipantWireTest.connectionToken
                scanDirection = ScanDirection.FORWARD.toSdkType()
                sortOrder = SortKey.ASCENDING.toSdkType()
                maxResults = 100
                startPosition = StartPosition { id = "item-0" }
            }
        ).getOrThrow()

        val request = takeRequest()
        assertParticipantRequest(request, "POST", "/participant/transcript", connectionToken)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("FORWARD", body.getString("ScanDirection"))
        assertEquals("ASCENDING", body.getString("SortOrder"))
        assertEquals(100, body.getInt("MaxResults"))
        assertEquals("item-0", body.getJSONObject("StartPosition").getString("Id"))

        assertEquals("initial-contact", response.initialContactId)
        assertEquals("next-token", response.nextToken)
        val serialized = JSONObject(TranscriptItemUtils.serializeTranscriptItem(response.transcript!!.single())!!)
        assertEquals("AGENT", serialized.getString("ParticipantRole"))
        assertEquals("MESSAGE", serialized.getString("Type"))
        assertEquals("APPROVED", serialized.getJSONArray("Attachments").getJSONObject(0).getString("Status"))
        assertEquals("item-1", serialized.getJSONObject("MessageMetadata").getString("MessageId"))
        assertEquals(
            "p2",
            serialized.getJSONObject("MessageMetadata").getJSONArray("Receipts").getJSONObject(0).getString("RecipientParticipantId")
        )
    }

    @Test
    fun accessDenied_surfacesHttp403() = runBlocking {
        enqueueJson(
            """{"message":"You do not have sufficient access to perform this action."}""",
            code = 403,
            headers = mapOf("x-amzn-ErrorType" to "AccessDeniedException")
        )

        val error = DefaultAWSClient.createParticipantConnection(participantToken).exceptionOrNull()

        assertTrue("Unexpected error: $error", error is AccessDeniedException)
        val httpResponse = (error as AccessDeniedException).sdkErrorMetadata.protocolResponse as HttpResponse
        assertEquals(403, httpResponse.status.value)
        assertFalse("The AWS SDK for Kotlin message does not include the status code", error.message.orEmpty().contains("403"))
    }

    @Test
    fun defaultEndpoint_matchesTheLegacyParticipantServiceHost() = runBlocking {
        val provider = DefaultConnectParticipantEndpointProvider()

        suspend fun endpointFor(region: Region) =
            provider.resolveEndpoint(ConnectParticipantEndpointParameters { this.region = region.getName() }).uri.toString()

        assertEquals("https://participant.connect.us-west-2.amazonaws.com", endpointFor(Region.US_WEST_2))
        assertEquals("https://participant.connect.us-gov-west-1.amazonaws.com", endpointFor(Region.GovCloud))
        assertEquals("https://participant.connect.cn-north-1.amazonaws.com.cn", endpointFor(Region.CN_NORTH_1))
    }
}
