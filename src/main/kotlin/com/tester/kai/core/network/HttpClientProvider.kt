package com.tester.kai.core.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientProvider {

    fun provide(
        host: String,
        token: String,
    ): HttpClient {
        return HttpClient(OkHttp.create()) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(HttpRequestRetry) {
                retryOnException(
                    maxRetries = 3,
                    retryOnTimeout = true
                )
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            install(DefaultRequest) {
                this.host = host
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                bearerAuth(token)

                url {
                    protocol = URLProtocol.HTTPS
                }
            }
        }
    }
}