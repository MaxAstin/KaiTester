package com.tester.kai.assistant.data

import com.intellij.execution.processTools.mapFlat
import com.tester.kai.assistant.data.model.CompletionsResponse
import com.tester.kai.assistant.data.model.Message
import com.tester.kai.assistant.data.model.ResponseFormat
import com.tester.kai.assistant.data.model.TestFunctionRequest
import com.tester.kai.core.network.HttpClientProvider
import com.tester.kai.core.network.sendPost
import com.tester.kai.core.network.toResult
import com.tester.kai.model.RawTest
import com.tester.kai.model.TestableTarget
import io.ktor.client.*

object AssistantRepositoryProvider {

    private const val OPENAI_API_KEY = "OPENAI_API_KEY"
    private val apiKey = System.getenv(OPENAI_API_KEY)

    val assistantRepository: AssistantRepository by lazy {
        AssistantRepository(
            client = HttpClientProvider.provide(
                host = "api.openai.com/v1/chat",
                token = apiKey
            )
        )
    }
}

class AssistantRepository(private val client: HttpClient) {

    suspend fun getRawTest(testableTarget: TestableTarget): Result<RawTest> {
        val systemMessage = Message(
            role = ROLE_SYSTEM,
            content = listOf(
                Message.Content(
                    type = DEFAULT_TYPE,
                    text = SYSTEM_MESSAGE
                )
            )
        )
        val userMessage = Message(
            role = ROLE_USER,
            content = listOf(
                Message.Content(
                    type = DEFAULT_TYPE,
                    text = "Write unit tests for Kotlin class ${testableTarget.classConstructor} for function ${testableTarget.functionCode}"
                )
            )
        )
        val request = TestFunctionRequest(
            model = DEFAULT_MODEL,
            messages = listOf(
                systemMessage,
                userMessage,
            ),
            temperature = DEFAULT_TEMPERATURE,
            maxTokens = DEFAULT_MAX_TOKENS,
            topP = DEFAULT_TOP_P,
            frequencyPenalty = DEFAULT_FREQUENCY_PENALTY,
            presencePenalty = DEFAULT_PRESENCE_PENALTY,
            responseFormat = ResponseFormat(
                type = DEFAULT_TYPE
            )
        )

        return client.sendPost<CompletionsResponse>(
            path = "completions",
            body = request
        ).toResult()
            .mapFlat(CompletionsResponse::toRawTestResult)
    }

    companion object {

        private const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val DEFAULT_TEMPERATURE = 1
        private const val DEFAULT_MAX_TOKENS = 2048
        private const val DEFAULT_TOP_P = 1
        private const val DEFAULT_FREQUENCY_PENALTY = 0
        private const val DEFAULT_PRESENCE_PENALTY = 0
        private const val DEFAULT_TYPE = "text"

        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"

        private val SYSTEM_MESSAGE = """
            Write Kotlin code only without explanation without other extra note.
            Write only imports and top level Kotlin functions without class.
            Test functions must be named according to the pattern `WHEN {condition} THEN {expected result}`, 
            replace {condition} and {expected result}).
            The test functions must be annotated with `@Test` annotation.
            Words in the function's name must be separated by spaces and enclosed in backticks `.
            The functions must have only one check, for example `assertEquals(expectedValue, actualValue)`.
            The tests should follow the Arrange-Act-Assert pattern.
            
            Example Unit Tests for Reference:
            package com.example

            import io.mockk.*
            import org.junit.jupiter.api.Assertions.assertEquals
            import org.junit.jupiter.api.Assertions.assertNull
            import org.junit.Test

            class GetMenuProductTest {

                @Test
                fun `WHEN current company is null THEN returns null`() {
                    // Arrange
                    every { companyRepo.getCurrentCompany() } returns null
                    val menuProductId = "product123"

                    // Act
                    val result = getMenuProduct.invoke(menuProductId)

                    // Assert
                    assertNull(result)
                }

                @Test
                fun `WHEN menu product is null THEN returns null`() {
                    // Arrange
                    val company = Company(id = "company123")
                    every { companyRepo.getCurrentCompany() } returns company
                    every { menuProductRepo.getMenuProduct("product123", "company123") } returns null

                    // Act
                    val result = getMenuProduct.invoke("product123")

                    // Assert
                    assertNull(result)
                }

                @Test
                fun `WHEN it is visible THEN returns menu product`() {
                    // Arrange
                    val company = Company(id = "company123")
                    val menuProduct = MenuProduct(isVisible = true)
                    every { companyRepo.getCurrentCompany() } returns company
                    every { menuProductRepo.getMenuProduct("product123", "company123") } returns menuProduct

                    // Act
                    val result = getMenuProduct.invoke("product123")

                    // Assert
                    assertEquals(menuProduct, result)
                }

                @Test
                fun `WHEN menu product is not visible THEN returns null`() {
                    // Arrange
                    val company = Company(id = "company123")
                    val menuProduct = MenuProduct(isVisible = false)
                    every { companyRepo.getCurrentCompany() } returns company
                    every { menuProductRepo.getMenuProduct("product123", "company123") } returns menuProduct

                    // Act
                    val result = getMenuProduct.invoke("product123")

                    // Assert
                    assertNull(result)
                }
            }
        """.trimIndent()
    }

}