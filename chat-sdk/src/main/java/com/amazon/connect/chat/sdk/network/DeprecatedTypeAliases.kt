// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadResponse
import aws.sdk.kotlin.services.connectparticipant.model.DescribeViewResponse
import aws.sdk.kotlin.services.connectparticipant.model.DisconnectParticipantResponse
import aws.sdk.kotlin.services.connectparticipant.model.GetAttachmentResponse
import aws.sdk.kotlin.services.connectparticipant.model.GetTranscriptResponse
import aws.sdk.kotlin.services.connectparticipant.model.SendEventResponse
import aws.sdk.kotlin.services.connectparticipant.model.SendMessageResponse
import aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadResponse

// Aliases for the AWS Mobile SDK for Android result names used by 2.x AWSClient subclasses. They
// keep subclass signatures compiling after switching imports, but the AWS SDK for Kotlin models are
// immutable and built with builders, so code that constructs or mutates them still needs updating.

private const val ALIAS_MESSAGE = "AWS Mobile SDK for Android result types were replaced by AWS SDK for Kotlin response types."

@Deprecated(ALIAS_MESSAGE, ReplaceWith("CompleteAttachmentUploadResponse", "aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadResponse"))
typealias CompleteAttachmentUploadResult = CompleteAttachmentUploadResponse

@Deprecated(ALIAS_MESSAGE, ReplaceWith("DescribeViewResponse", "aws.sdk.kotlin.services.connectparticipant.model.DescribeViewResponse"))
typealias DescribeViewResult = DescribeViewResponse

@Deprecated(ALIAS_MESSAGE, ReplaceWith("DisconnectParticipantResponse", "aws.sdk.kotlin.services.connectparticipant.model.DisconnectParticipantResponse"))
typealias DisconnectParticipantResult = DisconnectParticipantResponse

@Deprecated(ALIAS_MESSAGE, ReplaceWith("GetAttachmentResponse", "aws.sdk.kotlin.services.connectparticipant.model.GetAttachmentResponse"))
typealias GetAttachmentResult = GetAttachmentResponse

@Deprecated(ALIAS_MESSAGE, ReplaceWith("GetTranscriptResponse", "aws.sdk.kotlin.services.connectparticipant.model.GetTranscriptResponse"))
typealias GetTranscriptResult = GetTranscriptResponse

@Deprecated(ALIAS_MESSAGE, ReplaceWith("SendEventResponse", "aws.sdk.kotlin.services.connectparticipant.model.SendEventResponse"))
typealias SendEventResult = SendEventResponse

@Deprecated(ALIAS_MESSAGE, ReplaceWith("SendMessageResponse", "aws.sdk.kotlin.services.connectparticipant.model.SendMessageResponse"))
typealias SendMessageResult = SendMessageResponse

@Deprecated(ALIAS_MESSAGE, ReplaceWith("StartAttachmentUploadResponse", "aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadResponse"))
typealias StartAttachmentUploadResult = StartAttachmentUploadResponse
