package com.tester.kai.assistant.data.serialisation

import com.tester.kai.assistant.data.model.CompletionsResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object CompletionsResponseSerializer : JsonContentPolymorphicSerializer<CompletionsResponse>(CompletionsResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<CompletionsResponse> {
        return if ("error" in element.jsonObject) {
            CompletionsResponse.Error.serializer()
        } else {
            CompletionsResponse.Success.serializer()
        }
    }
}