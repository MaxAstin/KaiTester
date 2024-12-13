package com.tester.kai.assistant.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TestFunctionRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<Message>,
    @SerialName("temperature") val temperature: Int,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("top_p") val topP: Int,
    @SerialName("frequency_penalty") val frequencyPenalty: Int,
    @SerialName("presence_penalty") val presencePenalty: Int,
    @SerialName("response_format") val responseFormat: ResponseFormat
)

@Serializable
class Message(
    val role: String,
    val content: List<Content>
) {
    @Serializable
    class Content(
        val type: String,
        val text: String,
    )
}

@Serializable
class ResponseFormat(
    val type: String
)