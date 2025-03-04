package com.amazon.connect.chat.sdk.utils

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test
import org.junit.Assert.assertNotNull
import com.amazonaws.services.connectparticipant.model.Item


@RunWith(RobolectricTestRunner::class)
class TranscriptItemUtilsTest {
    private val INTERACTIVE_MESSAGE_CONTENTS = arrayOf(
        // List Picker
        "{\"templateIdentifier\":\"uuid\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"\$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"\$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"\$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}}",
        // Time Picker
        "{\"templateType\":\"TimePicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"Schedule appointment\",\"subtitle\":\"Tap to select option\",\"timeslots\":[{\"date\":\"2026-01-02T00:00+00:00\",\"duration\":60},{\"date\":\"2026-01-03T00:00+00:00\",\"duration\":60},{\"date\":\"2026-01-04T00:00+00:00\",\"duration\":60}],\"location\":{\"title\":\"Oscar\",\"latitude\":47.616299,\"longitude\":-122.333031,\"radius\":1},\"timeZoneOffset\":-420}}}",
        // Panel
        "{\"templateIdentifier\":\"uuid\",\"templateType\":\"Panel\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\"},{\"title\":\"Orange\"},{\"title\":\"Banana\"}]}}}",
        // Quick Reply
        "{\"templateType\":\"QuickReply\",\"version\":\"1.0\",\"data\":{\"replyMessage\":{\"title\":\"Thanks for selecting!\"},\"content\":{\"title\":\"How was your experience?\",\"elements\":[{\"title\":\"Great\"},{\"title\":\"Good\"},{\"title\":\"Ok\"},{\"title\":\"Poor\"},{\"title\":\"Terrible\"}]}}}",
        // Carousel
        "{\"templateType\":\"Carousel\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"Select from produce carousel\",\"omitTitleFromCarouselResponse\":false,\"elements\":[{\"templateIdentifier\":\"interactiveCarousel001\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"\$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"\$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"\$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel002\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"\$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"\$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"\$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel003\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"\$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"\$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"\$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel004\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"\$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"\$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"\$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel005\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"\$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"\$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"\$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}}]}}}"
    )

    @Test
    fun test_serializeTranscriptItem(){
        INTERACTIVE_MESSAGE_CONTENTS.forEach { content ->
            val item = Item()
                .withId("e1d3bc93-fc72-421b-95f6-13b37626668b")
                .withContent(content)
                .withContentType("application/vnd.amazonaws.connect.message.interactive")
                .withParticipantId("c8528570-62e6-47d9-ab5d-b6e152a07935")
                .withParticipantRole("SYSTEM")
                .withType("MESSAGE")
            val result = TranscriptItemUtils.serializeTranscriptItem(item)
            assertNotNull(result)
        }
    }
}