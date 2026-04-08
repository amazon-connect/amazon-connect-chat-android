// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

import com.amazon.connect.chat.sdk.utils.Constants
import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface MessageContent {
}

// Plain Text
@Serializable
data class PlainTextContent(val text: String) : MessageContent {
    companion object{
        fun decode(text: String): MessageContent = PlainTextContent(text)
    }
}

// Generic Interactive Template
@Serializable
data class GenericInteractiveTemplate(
    val templateType: String
    // Other common properties
)

interface InteractiveContent : MessageContent {
}

// Quick Reply Content
@Serializable
data class QuickReplyElement(
    val title: String
)
@Serializable
data class QuickReplyContentData(
    val title: String,
    val subtitle: String? = null,
    val elements: List<QuickReplyElement>
)
@Serializable
data class QuickReplyReplyMessage(
    val title: String,
    val subtitle: String? = null
)
@Serializable
data class QuickReplyData(
    val content: QuickReplyContentData,
    val replyMessage: QuickReplyReplyMessage? = null,
)
@Serializable
data class QuickReplyTemplate(
    val templateType: String,
    val version: String,
    val data: QuickReplyData
)
@Serializable
data class QuickReplyContent(
    val title: String,
    val subtitle: String? = null,
    val options: List<String>
) : InteractiveContent {
    companion object {
        const val TEMPLATE_TYPE = Constants.QUICK_REPLY
        fun decode(text: String): InteractiveContent? {
            return try {
                val quickReply = Json.decodeFromString<QuickReplyTemplate>(text)
                val options = quickReply.data.content.elements.map { it.title }
                val title = quickReply.data.content.title
                val subtitle = quickReply.data.content.subtitle ?: "" // Fallback to empty string if null
                QuickReplyContent(title, subtitle, options)
            } catch (e: Exception) {
                SDKLogger.logger.logError{"MessageContent: Error decoding QuickReplyContent: ${e.message}"}
                null
            }
        }
    }
}

// List Picker
@Serializable
data class ListPickerElement(
    val title: String,
    val subtitle: String? = null,
    val imageType: String? = null,
    val imageData: String? = null
)
@Serializable
data class ListPickerContentData(
    val title: String,
    val subtitle: String? = null,
    val imageType: String? = null,
    val imageData: String? = null,
    val elements: List<ListPickerElement>
)
@Serializable
data class ListPickerReplyMessage(
    val title: String,
    val subtitle: String? = null
)
@Serializable
data class ListPickerData(
    val content: ListPickerContentData,
    val replyMessage: ListPickerReplyMessage? = null,
)
@Serializable
data class ListPickerTemplate(
    val templateType: String,
    val templateIdentifier: String,
    val version: String,
    val data: ListPickerData
)
@Serializable
data class ListPickerContent(
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val options: List<ListPickerElement>
) : InteractiveContent {
    companion object {
        const val TEMPLATE_TYPE = Constants.LIST_PICKER
        fun decode(text: String): InteractiveContent? {
            return try {
                val listPicker = Json.decodeFromString<ListPickerTemplate>(text)
                val title = listPicker.data.content.title
                val subtitle = listPicker.data.content.subtitle ?: ""
                val options = listPicker.data.content.elements
                val imageUrl = listPicker.data.content.imageData ?: ""
                ListPickerContent(title, subtitle, imageUrl, options)
            } catch (e: Exception) {
                SDKLogger.logger.logError{"MessageContent: Error decoding ListPickerContent: ${e.message}"}
                null
            }
        }
    }
}

// Time Picker
@Serializable
data class TimeSlot(
    val date: String,
    val duration: Int
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val radius: Int? = null
)

@Serializable
data class TimePickerContentData(
    val title: String,
    val subtitle: String? = null,
    val timeZoneOffset: Int? = null,
    val location: Location? = null,
    val timeslots: List<TimeSlot>
)

@Serializable
data class TimePickerReplyMessage(
    val title: String? = null,
    val subtitle: String? = null
)

@Serializable
data class TimePickerData(
    val replyMessage: TimePickerReplyMessage? = null,
    val content: TimePickerContentData
)

@Serializable
data class TimePickerTemplate(
    val templateType: String,
    val version: String,
    val data: TimePickerData
)

data class TimePickerContent(
    val title: String,
    val subtitle: String? = null,
    val timeZoneOffset: Int? = null,
    val location: Location? = null,
    val timeslots: List<TimeSlot>
): InteractiveContent {
    companion object {
        const val TEMPLATE_TYPE = Constants.TIME_PICKER
        fun decode(text: String): InteractiveContent? {
            return try {
                val timePicker = Json.decodeFromString<TimePickerTemplate>(text)
                val contentData = timePicker.data.content
                TimePickerContent(
                    title = contentData.title,
                    subtitle = contentData.subtitle,
                    timeZoneOffset = contentData.timeZoneOffset,
                    location = contentData.location,
                    timeslots = contentData.timeslots
                )
            } catch (e: SerializationException) {
                SDKLogger.logger.logError{"MessageContent: Error decoding TimePickerContent: ${e.localizedMessage}"}
                null
            }
        }
    }
}

// Carousel
@Serializable
data class CarouselElement(
    val templateIdentifier: String,
    val templateType: String,
    val version: String,
    val data: PanelData
)

@Serializable
data class CarouselContentData(
    val title: String,
    val elements: List<CarouselElement>
)

@Serializable
data class CarouselData(
    val content: CarouselContentData
)

@Serializable
data class CarouselTemplate(
    val templateType: String,
    val version: String,
    val data: CarouselData
)

data class CarouselContent(
    val title: String,
    val elements: List<CarouselElement>
): InteractiveContent {
    companion object {
        const val TEMPLATE_TYPE = Constants.CAROUSEL
        fun decode(text: String): InteractiveContent? {
            return try {
                val carousel = Json.decodeFromString<CarouselTemplate>(text)
                val contentData = carousel.data.content
                CarouselContent(
                    title = contentData.title,
                    elements = contentData.elements
                )
            } catch (e: SerializationException) {
                SDKLogger.logger.logError{"MessageContent: Error decoding CarouselContent: ${e.localizedMessage}"}
                null
            }
        }
    }
}

// Panel
@Serializable
data class PanelElement(
    val title: String
)

@Serializable
data class PanelContentData(
    val title: String,
    val subtitle: String? = null,
    val imageType: String? = null,
    val imageData: String? = null,
    val imageDescription: String? = null,
    val elements: List<PanelElement>
)

@Serializable
data class PanelReplyMessage(
    val title: String,
    val subtitle: String? = null
)

@Serializable
data class PanelData(
    val replyMessage: PanelReplyMessage? = null,
    val content: PanelContentData
)

@Serializable
data class PanelTemplate(
    val templateType: String,
    val version: String,
    val data: PanelData
)

data class PanelContent(
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val imageDescription: String? = null,
    val options: List<PanelElement>
): InteractiveContent {
    companion object {
        const val TEMPLATE_TYPE = Constants.PANEL
        fun decode(text: String): InteractiveContent? {
            return try {
                val panel = Json.decodeFromString<PanelTemplate>(text)
                val contentData = panel.data.content
                PanelContent(
                    title = contentData.title,
                    subtitle = contentData.subtitle,
                    imageUrl = contentData.imageData,
                    imageDescription = contentData.imageDescription,
                    options = contentData.elements
                )
            } catch (e: SerializationException) {
                SDKLogger.logger.logError{"MessageContent: Error decoding PanelContent: ${e.localizedMessage}"}
                null
            }
        }
    }
}

// ViewResource (ShowView block)
@Serializable
data class ViewResourceDataContent(
    val viewId: String? = null,
    val viewToken: String? = null
)

@Serializable
data class ViewResourceData(
    val content: ViewResourceDataContent? = null
)

@Serializable
data class ViewResourceTemplate(
    val templateType: String,
    val version: String,
    val data: ViewResourceData
)

/**
 * Content for interactive messages using the ShowView block.
 * Use ChatSession.describeView(viewToken) to fetch the full view content.
 */
data class ViewResourceContent(
    val viewId: String?,
    val viewToken: String?
): InteractiveContent {
    companion object {
        const val TEMPLATE_TYPE = Constants.VIEW_RESOURCE
        fun decode(text: String): InteractiveContent? {
            return try {
                val viewResource = Json { ignoreUnknownKeys = true }.decodeFromString<ViewResourceTemplate>(text)
                val viewId = viewResource.data.content?.viewId
                val viewToken = viewResource.data.content?.viewToken
                ViewResourceContent(viewId = viewId, viewToken = viewToken)
            } catch (e: SerializationException) {
                SDKLogger.logger.logError{"MessageContent: Error decoding ViewResourceContent: ${e.localizedMessage}"}
                null
            }
        }
    }
}
