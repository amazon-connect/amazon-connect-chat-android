# Amazon Connect Chat SDK for Android

## Table of Contents
* [About](#about)
* [Installation Steps](#installation-steps)
* [Getting Started](#getting-started)
  * [How to receive messages](#how-to-receive-messages)
* [API List](#api-list)
  * [GlobalConfig](#globalconfig)
    * [GlobalConfig.init](#globalconfiginit)
    * [Updating configuration](#updating-configuration)
  * [SDKLogger](#sdklogger)
  * [ChatSession APIs](#chatsession-apis)
  * [ChatSession Events](#chatsession-events)
  * [Classes and Structs](#classes-and-structs)
* [Security](#security)
* [License](#license)

## About
The Amazon Connect Chat SDK for Android is a Kotlin library that gives you the power to easily integrate Amazon Connect Chat directly into your native Android applications. The Amazon Connect Chat SDK helps handle client side chat logic and back-end communications similar to the [Amazon Connect ChatJS Library](https://github.com/amazon-connect/amazon-connect-chatjs). The SDK wraps the [Amazon Connect Participant Service](https://docs.aws.amazon.com/connect/latest/APIReference/API_Operations_Amazon_Connect_Participant_Service.html) APIs and abstracts away the management of the chat session and WebSocket.  This allows you to focus on the user interface and experience while relying on the Amazon Connect Chat SDK to interact with all the back-end services.  This approach still requires using your own chat back end to call the Amazon Connect [StartChatContact](https://docs.aws.amazon.com/connect/latest/APIReference/API_StartChatContact.html) API to initiate contact. You can read instructions on how to quickly set up a StartChatContact Lambda from our [startChatContactAPI](https://github.com/amazon-connect/amazon-connect-chat-ui-examples/tree/master/cloudformationTemplates/startChatContactAPI) example.

## Installation Steps
There are two options to install the Amazon Connect Chat SDK for Android to your project:

### Install via Maven

To obtain the dependencies from Maven, add the dependencies to your app's (module-level) build.gradle.

Update build.gradle in root/app and add the following under dependencies:

```
dependencies {
    implementation 'software.aws.connect:amazon-connect-chat-android:$SDK_VERSION'
}
```

The version numbers could be found under the [Releases](https://github.com/amazon-connect/amazon-connect-chat-android/releases) page.

### Install via Binaries Directly from GitHub Releases

1. Go to the [Amazon Connect Chat SDK for Android GitHub Releases](https://github.com/amazon-connect/amazon-connect-chat-android/releases) page.
2. Download the latest release of the `AmazonConnectChatAndroid.aar`.
3. Unzip the downloaded file, if necessary.
4. Copy the `AmazonConnectChatAndroid.aar` file into your Android project into the project's `lib` directory.
5. In your `build.gradle` file, add `AmazonConnectChatAndroid.aar` to your dependency list.

Example:
```
dependencies {
    implementation(name: 'AmazonConnectChatAndroid', ext: 'aar')
}
```

## Getting Started

The first step to leveraging the Amazon Connect Chat SDK after installation is to import the library into your file. Next, let's call the StartChatContact API and pass the response details into the SDK’s ChatSession object. For reference, you can visit the [AndroidChatExample demo](https://github.com/amazon-connect/amazon-connect-chat-ui-examples/tree/master/mobileChatExamples/androidChatExample) within the [Amazon Connect Chat UI Examples](https://github.com/amazon-connect/amazon-connect-chat-ui-examples/tree/master) GitHub repository.

The majority of the SDKs functionality will be accessed through the `ChatSession` object. In order to use this object in the file, you can inject it using `@HiltViewModel`:

```
class ChatViewModel @Inject constructor(
    private val chatSession: ChatSession, // Injected ChatSession
    private val chatRepository: ChatRepository,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {
```

If you are not using Hilt, then you can initialise `ChatSession` like this:

```
private val chatSession = ChatSessionProvider.getChatSession(context)
```
> Note: When not using Hilt/Dagger for dependency injection, you may need to manually import the necessary dependencies for [AWSConnectParticipant](https://github.com/aws-amplify/aws-sdk-android) and [AWSCore](https://github.com/aws-amplify/aws-sdk-android) in your app’s build.gradle file. With Hilt/Dagger, these dependencies are managed automatically, but for manual implementations, you must include them explicitly to ensure all required classes are available.

In this example, we are using a `ChatViewModel` class that helps bridge UI and SDK communication.  This class is responsible for managing interactions with the SDK's ChatSession object. From here, we can access the SDK's suite of APIs from the `chatSession` property. 

Before using the chatSession object, we need to set the config for it via the GlobalConfig object.  Most importantly, the GlobalConfig object will be used to set the AWS region that your Connect instance lives in.  Here is an example of how to configure the ChatSession object:

```
private suspend fun configureChatSession() {
      val globalConfig = GlobalConfig(region = chatConfiguration.region)
      chatSession.configure(globalConfig)
      ...
  }
```

From here, you are now ready to interact with the chat via the `ChatSession` object.

### How to receive messages

The Amazon Connect Chat SDK for Android provides two methods to receive messages.

1. Use [ChatSession.onTranscriptUpdated](#chatsessionontranscriptupdated)
  * This event will pass back the entire transcript every time the transcript is updated. This will return the transcript inside the [TranscriptData](#transcriptdata) object.

2. Use [ChatSession.onMessageReceived](#chatsessiononmessagereceived)
  * This event will pass back each message that is received by the WebSocket.  The event handler will be passed a single [TranscriptItem](#transcriptitem).

## API List

### GlobalConfig
The `GlobalConfig` object is used to configure both the AWS ConnectParticipant client as well as some of the chat behavior.

#### `GlobalConfig.init`
The initializer for the `GlobalConfig` class requires the `AWSRegionType` and the enabled `Features`

```
data class GlobalConfig(
    var region: Regions = defaultRegion,
    var features: Features = Features.defaultFeatures,
    var disableCsm: Boolean = false,
    var customAWSClient: AWSClient? = null,
    var customWebSocketURLProvider: ((String) -> String)? = null
)
```
* `region`
  * This property is used to set the region of the ConnectParticipant client.  This should be set to the region of your Connect instance (e.g. `.USEast1`)
  * Type: `AWSRegionType`
* `features: Features`
  * The features property dictates the enablement of certain features as well as their configurations. If no value is passed for this property, the chat will be configured with default values. See [Features](#features) for more details.
  * Type: [Features](#features)
* `disableCsm`
  * This property acts as a setting to enable or disable our client side metric service. Client side metrics can provide insights into the real performance and usability, it helps us to understand how customers are actually using the SDK and what UI experiences they prefer.
  * Default: `false`
* `customAWSClient`
  * Optional custom AWSClient implementation for routing API calls through a backend proxy with certificate pinning. See [Custom AWSClient](#custom-awsclient) for more details.
  * Type: `AWSClient?`
  * Default: `null`
* `customWebSocketURLProvider`
  * Optional function to transform WebSocket URLs (e.g., to route through a proxy).
  * Type: `((String) -> String)?`
  * Default: `null`
 
#### Updating configuration
If you have set the `GlobalConfig` object or want to update the configurations, you can call `ChatSession.configure` to update the config object.

```
val globalConfig = GlobalConfig(region = chatConfiguration.region)
chatSession.configure(globalConfig)
```

### Custom AWSClient

The SDK supports custom client implementations to enable advanced networking scenarios such as:

- Custom endpoint routing through proxy servers or API gateways
- Custom header injection for authentication tokens or security headers
- Certificate pinning for enhanced security in enterprise environments
- Request/response transformation for compliance or monitoring requirements

#### Overview

By default, the SDK uses the standard AWS SDK HTTP client to communicate directly with Amazon Connect Participant Service APIs. For organizations requiring custom networking behavior, the SDK provides an inheritance-based system that allows you to subclass `AWSClient` and selectively override only the methods you need.

#### Creating a Custom Client

Subclass `AWSClient` and override the methods you need:

**Option A: Partial Override**
```kotlin
// Only override the methods you need to customize
class MyCustomClient : AWSClient() {
    
    override suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult> {
        // Add custom logging/monitoring
        Log.d("CustomClient", "sendMessage called: $message")
        // Call parent implementation to use default AWS behavior
        return super.sendMessage(connectionToken, contentType, message)
    }
    
    // Other methods automatically use parent AWSClient implementation
}
```

**Option B: Complete Override (For full proxy routing)**
```kotlin
// Override ALL methods with completely custom implementations
class MyProxyClient(private val proxyUrl: String) : AWSClient() {
    
    override suspend fun createParticipantConnection(participantToken: String): Result<ConnectionDetails> {
        // Route through your proxy instead of AWS
        return makeProxyRequest("createParticipantConnection", participantToken)
    }
    
    override suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult> {
        // Custom message routing through proxy
        return makeProxyRequest("sendMessage", message)
    }
    
    // Override all other methods for complete control...
}
```

#### Configuring the Custom Client

```kotlin
// Partial override example
val customClient = MyCustomClient()

val globalConfig = GlobalConfig(
    region = Regions.US_WEST_2,
    customAWSClient = customClient
)

chatSession.configure(globalConfig)
```

```kotlin
// Complete override with WebSocket proxy
val proxyClient = MyProxyClient("https://your-proxy.example.com/api")

val globalConfig = GlobalConfig(
    region = Regions.US_WEST_2,
    customAWSClient = proxyClient,
    customWebSocketURLProvider = { originalUrl ->
        // Transform WebSocket URL to point to your proxy
        "wss://your-proxy.example.com/ws?original=$originalUrl"
    }
)

chatSession.configure(globalConfig)
```

#### Available Methods to Override

The custom client can override all ACPS API methods:

| Method | Description |
|--------|-------------|
| `createParticipantConnection` | Establish chat connection |
| `disconnectParticipantConnection` | End chat connection |
| `sendMessage` | Send text messages |
| `sendEvent` | Send events (typing, receipts) |
| `getTranscript` | Retrieve chat history |
| `startAttachmentUpload` | Initiate file upload |
| `completeAttachmentUpload` | Complete file upload |
| `getAttachment` | Download attachments |
| `describeView` | Retrieve view for interactive messages |

### SDKLogger
The `SDKLogger` class is responsible for logging relevant runtime information to the console which is useful for debugging purposes. The `SDKLogger` will log key events such as establishing a connection or failures such as failing to send a message.

#### `SDKLogger.configure`
This API will allow you to override the SDK's built-in logger with your own [ChatSDKLogger](#chatsdklogger) implementation. This is especially useful in cases where you would want to store logs for debugging purposes. Attaching these logs to issues filed in this project will greatly expedite the resolution process.

```
fun configureLogger(logger: ChatSDKLogger) {
    this.logger = logger
}
```

#### ChatSDKLogger
The ChatSDKLogger is an interface used for the `SDKLogger`.  Users can override the `SDKLogger` with any class that implements the ChatSDKLogger interface.

```
interface ChatSDKLogger {
    fun logVerbose(message: () -> String)
    fun logInfo(message: () -> String)
    fun logDebug(message: () -> String)
    fun logWarn(message: () -> String)
    fun logError(message: () -> String)
    fun setLoggingEnabled(enabled: Boolean)
}
```

#### `SDKLogger.setLoggingEnabled`
This API will allow you to enable and disable logging.

```
fun setLoggingEnabled(enabled: Boolean) {
   this.logger.setLoggingEnabled(enabled)
}
```

--------------------

### ChatSession APIs

#### `ChatSession.configure`
Configures the chat service with a `GlobalConfiguration` object.

```
fun configure(config: GlobalConfig)
```
* `config`
  * The global configuration to use
  * Type: [GlobalConfig](#globalconfig)

--------------------

#### `ChatSession.getConnectionDetailsProvider`
Returns a `ConnectionDetailsProvider` object that includes connection details.

```
fun getConnectionDetailsProvider(): ConnectionDetailsProvider
```
* Return type: [ConnectionDetailsProvider](#connectiondetailsprovider)

#### `ChatSession.connect`
Attempts to connect to a chat session with the given details.

```
suspend fun connect(chatDetails: ChatDetails): Result<Boolean>

data class ChatDetails(
    var contactId: String? = null,
    var participantId: String? = null,
    var participantToken: String
)
```

* `chatDetails`
  * The details of the chat session to connect to. `ChatDetails` data is extracted from `StartChatContact` response.
  * Type: `ChatDetails`

--------------------

#### `ChatSession.disconnect`
Disconnects the current chat session.

```
suspend fun disconnect(): Result<Boolean>
```

--------------------

#### `ChatSession.suspendWebSocketConnection`
Disconnects the websocket and suspends reconnection attempts.

```
suspend fun suspendWebSocketConnection(): Result<Boolean>
```

--------------------


#### `ChatSession.resumeWebSocketConnection`
Resumes a suspended websocket and attempts to reconnect.

```
suspend fun resumeWebSocketConnection(): Result<Boolean>
```

--------------------

#### `ChatSession.reset`
Resets the ChatSession object which will disconnect the webSocket and remove all session related data without disconnecting the participant from the chat contact.

```
suspend fun reset(): Result<Boolean>
```

--------------------

#### `ChatSession.sendMessage`
Sends a message within the chat session.

```
suspend fun sendMessage(contentType: ContentType, message: String): Result<Boolean>
```

* `contentType`
  * The type of the message content.
  * Type: [ContentType](#contenttype)
* `message`
  * The message to send.
  * Type: `String`

--------------------

#### `ChatSession.sendEvent`
Sends an event within the chat session.

```
suspend fun sendEvent(contentType: ContentType, event: String): Result<Boolean>
```

* `event`
  * The type of the event content.
  * Type: [ContentType](#contenttype)
* `content`
  * The event content to send.
  * Type: `String`

--------------------

#### `ChatSession.sendMessageReceipt`
Sends read receipt for a message.

```
suspend fun sendMessageReceipt(transcriptItem: TranscriptItem, receiptType: MessageReceiptType): Result<Boolean>
```
* `transcriptItem`
  * The transcript item associated with the message receipt.
  * Type: [TranscriptItem](#transcriptitem)
* `receiptType`
  * The type of read receipt to send (either `MessageReceiptType.MESSAGE_DELIVERED` or `MessageReceiptType.MESSAGE_READ`)
  * Type: [MessageReceiptType](#messagereceipttype)   


--------------------

#### `ChatSession.getTranscript`

Retrieves the chat transcript.

```
suspend fun getTranscript(
        scanDirection: ScanDirection?,
        sortKey: SortKey?,
        maxResults: Int?,
        nextToken: String?,
        startPosition: String?
    ): Result<TranscriptResponse>
```

* `scanDirection`
  * The direction to scan the transcript.
  * Type: `ScanDirection?` (String: `'FORWARD' | 'BACKWARD'`) 
  * Default: `BACKWARD`
* `sortOrder`
  * The order to sort the transcript.
  * Type: `SortKey?` (String: `'DESCENDING | 'ASCENDING'`)
  * Default: `ASCENDING`
* `maxResults`
  * The maximum number of results to retrieve.
  * Type: `Int?`
  * Default: `15`
* `nextToken`
  * Type: `String`
  * The token for the next set of results.
* `startPosition`
  * The start position for the transcript.
  * Type: `StartPosition?`. See [StartPosition](https://docs.aws.amazon.com/connect/latest/APIReference/API_connect-participant_StartPosition.html)
* Return result type: [TranscriptResponse](#transcriptresponse)

Upon establishing a connection, the SDK automatically calls `getTranscript` to retrieve messages
sent while the app was in the background, offline, or in similar disconnected states. Users can also
call `getTranscript` as they need, for example - pull to fetch previous message etc.

Here's how `getTranscript` works in SDK:

* **Automatic Transcript Management:** The SDK automatically manages the received transcript.
  New messages are delivered through the `onTranscriptUpdated` and `onMessageReceived` callbacks.
* **Pagination:** The response includes a `nextToken` that you can use to fetch additional messages
  if the transcript is large.

For more information on how getTranscript API works, please
visit [GetTranscript](https://docs.aws.amazon.com/connect/latest/APIReference/API_connect-participant_GetTranscript.html)

--------------------

#### `ChatSession.sendAttachment`
Sends an attachment within the chat session.

```
suspend fun sendAttachment(fileUri: Uri): Result<Boolean>
```

* `fileUri`
  * The Uri of the file to attach.
  * Type: `Uri`

--------------------

#### `ChatSession.downloadAttachment`
Downloads an attachment to the app's temporary directory given an attachment ID.

```
suspend fun downloadAttachment(attachmentId: String, filename: String): Result<URL>
```

* `attachmentId`
  * The ID of the attachment to download.
  * Type: `String`
* `filename`
  * The name of the file to save the attachment as.
  * Type: `String`
* Return result type: `URL`
  * The return URL points to the location of the downloaded attachment in the local temporary storage.
  
--------------------

#### `ChatSession.getAttachmentDownloadUrl`
Returns the download URL link for the given attachment ID.

```
suspend fun getAttachmentDownloadUrl(attachmentId: String): Result<URL>
```

* `attachmentId`
  * The ID of the attachment.
  * Type: `String`
* Return result type: `URL`
  * This return URL points to the S3 download URL of the attachment.
 
--------------------

#### `ChatSession.resendFailedMessage`
Retry a text message or attachment that failed to be sent.

```
suspend fun resendFailedMessage(messageId: String): Result<Boolean>
```

* `messageId`
  * messageId The Id of the message that failed to be sent.
  * Type: `String`

--------------------

#### `ChatSession.describeView`
Retrieves the view for the specified view token. This is used to get view content for interactive messages that use the ShowView block.

```
suspend fun describeView(viewToken: String): Result<View>
```

* `viewToken`
  * An encrypted token originating from the interactive message of a ShowView block operation.
  * Type: `String`
* Return result type: [View](#view)

For more information on how DescribeView API works, please visit [DescribeView](https://docs.aws.amazon.com/connect/latest/APIReference/API_connect-participant_DescribeView.html)

--------------------

### ChatSession Events

#### `ChatSession.onConnectionEstablished`
Callback for when the connection is established.

```
var onConnectionEstablished: (() -> Unit)?
```

--------------------

#### `ChatSession.onConnectionBroken`
Callback for when the connection is broken.

```
var onConnectionBroken: (() -> Unit)?
```

--------------------

#### `ChatSession.onMessageReceived`
Callback for when a WebSocket message is received. See [TranscriptItem](#transcriptitem).

```
var onMessageReceived: ((TranscriptItem) -> Unit)?
```
--------------------

#### `ChatSession.onTranscriptUpdated`
Callback for when the transcript is updated. See [TranscriptData](#transcriptdata)

```
var onTranscriptUpdated: ((TranscriptData) -> Unit)?
```

--------------------

#### `ChatSession.onChatEnded`
Callback for when the chat ends. See [Event](#event).

```
var onChatEnded: ((Event?) -> Unit)?
```

--------------------
#### `ChatSession.onConnectionReEstablished`
Callback for when the connection is re-established.

```
var onConnectionReEstablished: (() -> Unit)?
```
--------------------
#### `ChatSession.onDeepHeartBeatFailure`
Callback for when the deep heartbeat fails.

```
var onDeepHeartBeatFailure: (() -> Unit)?
```
--------------------
#### `ChatSession.onTyping`
Callback for when a typing event is received. See [Event](#event).

```
var onTyping: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onMessageDelivered`
Callback for when a message delivery receipt is received. See [Event](#event).

```
var onMessageDelivered: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onMessageRead`
Callback for when a message read receipt is received. See [Event](#event).

```
var onMessageRead: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onParticipantIdle`
Callback for when a participant becomes idle. See [Event](#event).

```
var onParticipantIdle: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onParticipantReturned`
Callback for when a participant returns from idle/inactive state. See [Event](#event).

```
var onParticipantReturned: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onParticipantInvited`
Callback for when a participant is invited to the chat. See [Event](#event).

```
var onParticipantInvited: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onAutoDisconnection`
Callback for when an auto-disconnection occurs. See [Event](#event).

```
var onAutoDisconnection: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onChatRehydrated`
Callback for when the chat is rehydrated after a reconnection. See [Event](#event).

```
var onChatRehydrated: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onParticipantJoined`
Callback for when a participant joins the chat. See [Event](#event).

```
var onParticipantJoined: ((Event?) -> Unit)?
```
--------------------
#### `ChatSession.onParticipantLeft`
Callback for when a participant leaves the chat. See [Event](#event).

```
var onParticipantLeft: ((Event?) -> Unit)?
```
--------------------

## Classes and Structs

### Features
Features are a list of optional chat functions that users may choose to enable, disable or reconfigure.

```
data class Features(
    var messageReceipts: MessageReceipts = MessageReceipts.defaultReceipts
) {
    companion object {
        val defaultFeatures: Features
            get() = Features(messageReceipts = MessageReceipts.defaultReceipts)
    }
}
```

the default value for `Features` will contain the default values for all containing features.

--------------------

### Message Receipts
This feature enables the use of `Read` and `Delivered` receipts for messages. This is used to indicate whether agents have read texts that the client has sent and vice versa.

```
data class MessageReceipts(
    var shouldSendMessageReceipts: Boolean = true,
    var throttleTime: Double = Constants.MESSAGE_RECEIPT_THROTTLE_TIME,
    var deliveredThrottleTime: Double = Constants.MESSAGE_RECEIPT_DELIVERED_THROTTLE_TIME
) {
    companion object {
        val defaultReceipts: MessageReceipts
            get() = MessageReceipts(
                shouldSendMessageReceipts = true,
                throttleTime = Constants.MESSAGE_RECEIPT_THROTTLE_TIME
            )
    }
}
```
* `shouldSendMessageReceipts`
  * Type: `Boolean`
  * This is the flag that dictates whether message receipts will be sent from the client side.  Note that this will not block message receipt events from being sent from the agent.
  * Default: `true`
* `throttleTime`
  * Type: `Double`
  * This is used to determine how long to throttle message receipt events before firing them. We recommend having at least some throttling time before each event to reduce unnecessary network requests
  * Default: `5.0`
* `deliveredThrottleTime`
  * Type: `Double`
  * When sending a delivered receipt, we generally want to wait a couple seconds to see if a read receipt comes for the same message to avoid unnecessary network requests.  This is used to determine the wait period after sending a delivered receipt.
  * Default: `3.0`

--------------------
### ChatDetails

```
data class ChatDetails(
    var contactId: String? = null,
    var participantId: String? = null,
    var participantToken: String
)
```
* `contactId`
  * Contact identifier received via [StartChatContact](https://docs.aws.amazon.com/connect/latest/APIReference/API_StartChatContact.html) response
  * Type: `String`
* `particpantId`
  * Participant identifier received via [StartChatContact](https://docs.aws.amazon.com/connect/latest/APIReference/API_StartChatContact.html) response
  * Type: `String`
* `participantToken`
  * Participant token received via [StartChatContact](https://docs.aws.amazon.com/connect/latest/APIReference/API_StartChatContact.html) response
  * Type: `String`
---------------------

### ConnectionDetailsProvider

```
interface ConnectionDetailsProvider {
    fun updateChatDetails(newDetails: ChatDetails)
    fun getConnectionDetails(): ConnectionDetails?
    fun updateConnectionDetails(newDetails: ConnectionDetails)
    fun getChatDetails(): ChatDetails?
    fun isChatSessionActive(): Boolean
    fun setChatSessionState(isActive: Boolean)
}
```
* `updateChatDetails`
  * Updates chat details
  * newDetails
    * Type: `ChatDetails`
* `getConnectionDetails`
  * Gets connection details received via [CreateParticipantConnection](https://docs.aws.amazon.com/connect/latest/APIReference/API_connect-participant_CreateParticipantConnection.html) response
  * Return type: [ConnectionDetails](#connectiondetails)
* `updateConnectionDetails`
  * Updates connection details 
  * newDetails
    * Type: [ConnectionDetails](#connectiondetails)
* `getChatDetails`
  * Gets chat details
  * Return type: `ChatDetails`
* `isChatSessionActive`
  * Gets chat session active state
  * Return type: Boolean
* `setChatSessionState`
  * Sets chat session state
  * isActive
    * Type: Boolean

---------------------

### ConnectionDetails

```
data class ConnectionDetails(
    val websocketUrl: String,
    val connectionToken: String,
    val expiry: String
)
```
* `websocketUrl`
  * URL of the websocket received via [CreateParticipantConnection](https://docs.aws.amazon.com/connect/latest/APIReference/API_connect-participant_CreateParticipantConnection.html) response
  * Type: `String`
* `connectionToken`
  * Connection token received via [CreateParticipantConnection](https://docs.aws.amazon.com/connect/latest/APIReference/API_connect-participant_CreateParticipantConnection.html) response
  * Type: `String`
* `expiry`
  * Expiration of the token received via [CreateParticipantConnection](https://docs.aws.amazon.com/connect/latest/APIReference/API_connect-participant_CreateParticipantConnection.html) response
  * Type: `String`
---------------------

### ContentType

`ContentType` describe the type of events and messages that come through the WebSocket.

```
enum class ContentType(val type: String){
    TYPING("application/vnd.amazonaws.connect.event.typing"),
    CONNECTION_ACKNOWLEDGED("application/vnd.amazonaws.connect.event.connection.acknowledged"),
    MESSAGE_DELIVERED("application/vnd.amazonaws.connect.event.message.delivered"),
    MESSAGE_READ("application/vnd.amazonaws.connect.event.message.read"),
    META_DATA("application/vnd.amazonaws.connect.event.message.metadata"),
    JOINED("application/vnd.amazonaws.connect.event.participant.joined"),
    LEFT("application/vnd.amazonaws.connect.event.participant.left"),
    ENDED("application/vnd.amazonaws.connect.event.chat.ended"),
    PLAIN_TEXT("text/plain"),
    RICH_TEXT("text/markdown"),
    INTERACTIVE_TEXT("application/vnd.amazonaws.connect.message.interactive");
}
```
--------------------
### MessageReceiptType

`MessageReceiptType` is a subset of [ContentType](#contenttype) for message receipt related events.

```
enum class MessageReceiptType(val type: String){
    MESSAGE_DELIVERED("application/vnd.amazonaws.connect.event.message.delivered"),
    MESSAGE_READ("application/vnd.amazonaws.connect.event.message.read");
}
```
--------------------
### TranscriptResponse

```
data class TranscriptResponse(
    val initialContactId: String = "",
    val nextToken: String? = null,
    val transcript: List<TranscriptItem> = emptyList()
)
```
* `initialContactId`
  * This is the id of the chat contact
  * Type: `String`
* `nextToken`
  * The `nextToken` is used to retrieve the next batch of messages from the server. This can be passed into [ChatSession.getTranscript](#chatsessiongettranscript)
  * Type: `String`
* `transcript`
  * This contains the messages that were loaded
  * Type: List of [TranscriptItem](#transcriptitem)
-------------------
### TranscriptItem
This is the base class for all renderable messages in the transcript.

```
open class TranscriptItem(
    id: String = "",
    timeStamp: String,
    override var contentType: String,
    override var serializedContent: String? = null,
    persistentId: String = ""
)
```
* `id`
  * Id for the message. Unique to each message in the transcript. Id can be used only for the ACPS APIs requests.
  * Type: `String`
* `timeStamp`
  * Time when the message or event was sent. Formatted in ISO 8601 (e.g. `yyyy-MM-ddThh:mm:ss.SSSZ` or ` 2019-11-08T02:41:28.172Z`)
  * Type: `String`
* `contentType`
  * The type of message
  * Type: `String` (See [ContentType](#contenttype))
* `serializedContent`
  * The raw JSON format of the received WebSocket message
  * Type: Map of `String?`
* `persistentId`
  * Id for the message, Unique to each message throughout chat session. This can be used only for tracking purposes by the client.
  * Type: `String`

--------
### TranscriptData
This is the object that is passed back to the registered [ChatSession.onTranscriptUpdated](#chatsessionontranscriptupdated) event handler

```
data class TranscriptData(
    val transcriptList: List<TranscriptItem>,
    val previousTranscriptNextToken: String?
)
```
* `transcriptList`
  * The current in-memory transcript list.
  * Type: Array of `TranscriptItem` (See [TranscriptItem](#transcriptitem))
* `previousTranscriptNextToken`
  * This is a next token that is used as a `getTranscript` argument to retrieve older messages.  This will be `null` if there are no more available messages to fetch from the top of the currently loaded transcript.
  * Type: `String`

--------
### Message (extends [TranscriptItem](#transcriptitem))

The Message type of TranscriptItem is reserved for all messages sent by bots, contact flow or other participants.  This includes interactive messages, attachments and plain text messages.

```
class Message(
    override var participant: String,
    override var text: String,
    contentType: String,
    override var displayName: String? = null,
    override var messageDirection: MessageDirection? = null,
    timeStamp: String,
    var attachmentId: String? = null,
    id: String,
    override var metadata: MessageMetadataProtocol? = null,
    serializedContent: String?? = null
)
```
* `participant`
  * This is the participant role of the message sender (e.g. `AGENT`)
  * Type: `String`
* `text`
  * This is the text content of the message
  * Type: `String`
* `contentType`
  * This is the type of message.
  * Type: `String` (See [ContentType](#contenttype)
* `displayName`
  * This is the display name of the message
  * Type: `String`
* `messageDirection`
  * This is the direction of the message
  * Type: `MessageDirection` (`OUTGOING | INCOMING | COMMON`)
* `attachmentId`
  * This is the id of the attachment.  Only defined if this message contains an attachment.
  * Type: `String`
* `id`
  * This is the id of the message
  * Type: `String`
* `metadata`
  * This is the metadata associated with the message.
  * Type: [Metadata](#metadata)
* `serializedContent`
  * This is the serialized message data that comes through the WebSocket which is used to create the message object.
  * Type: `String?`
---
### Event (extends [TranscriptItem](#transcriptitem))
The Event type of the TranscriptItem is for events that come through the WebSocket.  See [ContentType](#contenttype) for a list of possible events.

```
class Event(
    override var participant: String? = null,
    override var text: String? = null,
    override var displayName: String? = null,
    override var eventDirection: MessageDirection? = MessageDirection.COMMON,

    // TranscriptItem properties
    timeStamp: String,
    contentType: String,
    id: String,
    serializedContent: String? = null
)
```
* `participant`
  * This is the participant role of the message sender (e.g. `AGENT`)
  * Type: `String?`
* `text`
  * This is the text content of the event
  * Type: `String?`
* `displayName`
  * This is the display name of the event
  * Type: `String`
* `eventDirection`
  * This is the direction of the event
  * Type: `eventDirection` (`Outgoing | Incoming | Common`)

--------
### MessageMetadata (extends [TranscriptItem](#transcriptitem))
The MessageMetadata event is used to receive additional data on a given message such as message receipt status.

```
class MessageMetadata(
    override var status: MessageStatus? = null,
    override var eventDirection: MessageDirection? = MessageDirection.COMMON,

    // TranscriptItem properties
    timeStamp: String,
    contentType: String,
    id: String,
    serializedContent: String? = null
)
```
* `status`
  * This is the receipt status for the event.
  * Type: `MessageStatus` (`'Read' | 'Delivered'`)
* `eventDirection`
  * This is the direction of the metadata event.
  * Type: `eventDirection` (`Outgoing | Incoming | Common`)

--------
### View
A view resource object containing metadata and content necessary to render the view. This is returned by the [describeView](#chatsessiondescribeview) API.

```
data class View(
    val arn: String? = null,
    val content: ViewContent? = null,
    val id: String? = null,
    val name: String? = null,
    val version: Int? = null
)
```
* `arn`
  * The Amazon Resource Name (ARN) of the view.
  * Type: `String?`
* `content`
  * View content containing all content necessary to render a view except for runtime input data.
  * Type: [ViewContent](#viewcontent)
* `id`
  * The identifier of the view.
  * Type: `String?`
* `name`
  * The name of the view.
  * Type: `String?`
* `version`
  * The current version of the view.
  * Type: `Int?`

--------
### ViewContent
View content containing all content necessary to render a view except for runtime input data.

```
data class ViewContent(
    val actions: List<String>? = null,
    val inputSchema: String? = null,
    val template: String? = null
)
```
* `actions`
  * A list of actions possible from the view.
  * Type: `List<String>?`
* `inputSchema`
  * The schema representing the input data that the view template must be supplied to render.
  * Type: `String?`
* `template`
  * The view template representing the structure of the view.
  * Type: `String?`

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.

