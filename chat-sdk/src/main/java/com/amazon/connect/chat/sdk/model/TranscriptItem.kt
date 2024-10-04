package com.amazon.connect.chat.sdk.model

interface TranscriptItemProtocol {
    val id: String
    val timeStamp: String
    var contentType: String
    var serializedContent: String?
}

open class TranscriptItem(
    override var id: String,
    override var timeStamp: String,
    override var contentType: String,
    override var serializedContent: String? = null
) : TranscriptItemProtocol {

    internal fun updateId(newId: String) {
        this.id = newId
    }

    internal fun updateTimeStamp(newTimeStamp: String) {
        this.timeStamp = newTimeStamp
    }

    // Methods needed for comparison on item changes
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TranscriptItem) return false
        return id == other.id &&
                timeStamp == other.timeStamp &&
                contentType == other.contentType &&
                serializedContent == other.serializedContent
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timeStamp.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + (serializedContent?.hashCode() ?: 0)
        return result
    }
}

