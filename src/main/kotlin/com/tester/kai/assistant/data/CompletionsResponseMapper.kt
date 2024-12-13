package com.tester.kai.assistant.data

import com.tester.kai.assistant.data.model.CompletionsResponse
import com.tester.kai.model.RawTest

private const val ROLE_ASSISTANT = "assistant"

fun CompletionsResponse.toRawTestResult(): Result<RawTest> {
    return when (this) {
        is CompletionsResponse.Success -> {
            assistantMessage?.let { message ->
                Result.success(
                    value = RawTest(
                        code = message
                    )
                )
            } ?: Result.failure(
                exception = Exception("Assistant message not found")
            )
        }

        is CompletionsResponse.Error -> {
            Result.failure(
                exception = Exception(error.message)
            )
        }
    }
}

private val CompletionsResponse.Success.assistantMessage: String?
    get() {
        return choices.find { choice ->
            choice.message.role == ROLE_ASSISTANT
        }?.message?.content
    }