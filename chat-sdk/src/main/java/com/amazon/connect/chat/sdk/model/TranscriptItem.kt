package com.amazon.connect.chat.sdk.model

import java.util.UUID

interface TranscriptItemProtocol {
    val id: String
    val timeStamp: String
    var contentType: String
    var serializedContent: Map<String, Any>?
}

open class TranscriptItem(
    id: String = "",
    timeStamp: String,
    override var contentType: String,
    override var serializedContent: Map<String, Any>? = null
) : TranscriptItemProtocol {

    private var _id: String = id
    private var _timeStamp: String = timeStamp

    override val id: String
        get() = _id

    override val timeStamp: String
        get() = _timeStamp

    // Internal methods to update id and timeStamp if needed
    protected fun updateId(newId: String) {
        _id = newId
    }

    protected fun updateTimeStamp(newTimeStamp: String) {
        _timeStamp = newTimeStamp
    }
}
