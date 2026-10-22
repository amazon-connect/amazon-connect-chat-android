# Migrating from 2.x to 3.0

Version 3.0 replaces the deprecated AWS Mobile SDK for Android (`com.amazonaws:aws-android-sdk-*`)
with the AWS SDK for Kotlin (`aws.sdk.kotlin:connectparticipant`). This is a major release because
several AWS-owned types in the public API change.

## 1. Update your build

The AWS SDK for Kotlin requires:

* `minSdk` 24 or higher (unchanged from 2.x)
* Kotlin 2.2 or higher
* Core library desugaring

```kotlin
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation("software.aws.connect:amazon-connect-chat-android:3.0.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
```

Remove any explicit `com.amazonaws:aws-android-sdk-core` or
`com.amazonaws:aws-android-sdk-connectparticipant` dependencies you added for the Chat SDK. If your
application uses those artifacts for other AWS services, you can keep them; the Chat SDK no longer
depends on them.

The Chat SDK now depends on OkHttp 5.3.2, which is backwards compatible with OkHttp 4.x and does not
raise your `compileSdk` requirement. If your application declares OkHttp 4.x, Gradle resolves it to
5.3.2.

## 2. Update region names

`com.amazonaws.regions.Regions` is replaced by the Chat SDK's `Region` class. The constant names are
unchanged:

```kotlin
// 2.x
import com.amazonaws.regions.Regions
GlobalConfig(region = Regions.US_WEST_2)

// 3.0
import com.amazon.connect.chat.sdk.model.Region
GlobalConfig(region = Region.US_WEST_2)
```

You can also create a region from its identifier, including regions added after this release:

```kotlin
GlobalConfig(region = Region("ap-southeast-4"))
```

`Region.getName()` returns the identifier, as `Regions.getName()` did. A deprecated
`com.amazon.connect.chat.sdk.model.Regions` alias is included, so replacing only the import also
compiles. The default region remains `us-west-2`.

## 3. Update transcript option types

| 2.x | 3.0 |
| --- | --- |
| `com.amazonaws.services.connectparticipant.model.ScanDirection` | `com.amazon.connect.chat.sdk.model.ScanDirection` |
| `com.amazonaws.services.connectparticipant.model.SortKey` | `com.amazon.connect.chat.sdk.model.SortKey` |

The enum constants (`FORWARD`, `BACKWARD`, `ASCENDING`, `DESCENDING`) are unchanged, so only the
imports change. `ChatSession.getTranscript` still takes the start position as a `String` message ID.

## 4. Update custom `AWSClient` subclasses

The `AWSClient` extension point is unchanged, but its request and response types now come from
`aws.sdk.kotlin.services.connectparticipant.model`:

| 2.x | 3.0 |
| --- | --- |
| `CompleteAttachmentUploadRequest` | `CompleteAttachmentUploadRequest` |
| `CompleteAttachmentUploadResult` | `CompleteAttachmentUploadResponse` |
| `DescribeViewResult` | `DescribeViewResponse` |
| `DisconnectParticipantResult` | `DisconnectParticipantResponse` |
| `GetAttachmentResult` | `GetAttachmentResponse` |
| `GetTranscriptRequest` | `GetTranscriptRequest` |
| `GetTranscriptResult` | `GetTranscriptResponse` |
| `SendEventResult` | `SendEventResponse` |
| `SendMessageResult` | `SendMessageResponse` |
| `StartAttachmentUploadRequest` | `StartAttachmentUploadRequest` |
| `StartAttachmentUploadResult` | `StartAttachmentUploadResponse` |

For example:

```kotlin
import aws.sdk.kotlin.services.connectparticipant.model.SendMessageResponse
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.network.AWSClient

class MyCustomClient : AWSClient() {
    override suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResponse> {
        return super.sendMessage(connectionToken, contentType, message)
    }
}
```

Deprecated aliases for the 2.x `*Result` names are included in `com.amazon.connect.chat.sdk.network`.
They do not make the models behavior-compatible: AWS SDK for Kotlin models are immutable and are
built with builders rather than setters.

```kotlin
// 2.x
val request = CompleteAttachmentUploadRequest().apply {
    connectionToken = token
    setAttachmentIds(listOf(attachmentId))
}

// 3.0
val request = CompleteAttachmentUploadRequest {
    connectionToken = token
    attachmentIds = listOf(attachmentId)
}
```

Use `copy { }` to change a field on an existing model. Generated enums such as `ParticipantRole` and
`ChatItemType` are sealed classes; read the wire value with `.value`.

`CommonUtils.createConnectParticipantConfiguration()` returned the AWS Mobile SDK
`ClientConfiguration` and has been removed.

The same model change applies to these public helpers:

| Member | 3.0 type |
| --- | --- |
| `AttachmentsManager.createStartAttachmentUploadRequest` | returns `aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadRequest` |
| `TranscriptItemUtils.serializeTranscriptItem` | takes `aws.sdk.kotlin.services.connectparticipant.model.Item` |
| `ChatService.getTranscript` | `startPosition` is `aws.sdk.kotlin.services.connectparticipant.model.StartPosition` |

## 5. Update error handling

Service errors are now AWS SDK for Kotlin exceptions, such as
`aws.sdk.kotlin.services.connectparticipant.model.AccessDeniedException`, instead of
`com.amazonaws.AmazonServiceException`. Their messages no longer include the HTTP status code. Read
the status from the error metadata when you need it:

```kotlin
val status = ((error as? ServiceException)?.sdkErrorMetadata?.protocolResponse as? HttpResponse)?.status?.value
```

## 6. Hilt

The SDK's Hilt modules no longer bind `OkHttpClient`, `Retrofit.Builder` or `Context` without a
qualifier, so applications that provide their own bindings for those types no longer fail with
`DuplicateBindings` errors
([#88](https://github.com/amazon-connect/amazon-connect-chat-android/issues/88)). Injecting
`ChatSession` is unchanged.

## 7. Validate your application

Before releasing:

1. Build your application, including a release build with R8 if you use it.
2. Start a chat and connect to an agent.
3. Send and receive messages, typing events, and read/delivered receipts.
4. Retrieve the transcript, including after a network interruption.
5. Upload and download attachments.
6. Disconnect, and resume a persistent chat.

Participant and connection tokens continue to be sent in the `X-Amz-Bearer` header, and requests
remain unsigned, as with the AWS Mobile SDK. The SDK identifies itself in the `User-Agent` header
both through the AWS SDK for Kotlin application ID (`app/AmazonConnect-Mobile-Chat-Android-SDK-<version>`)
and with the exact 2.x product token (`AmazonConnect-Mobile Chat-Android SDK/<version>`).
