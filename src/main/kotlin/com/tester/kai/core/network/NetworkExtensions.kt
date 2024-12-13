package com.tester.kai.core.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend inline fun <reified R> HttpClient.sendPost(
    path: String,
    body: Any,
): ApiResult<R> {
    return safeCall {
        post {
            setBody(body)
            url { path(path) }
        }
    }
}

suspend inline fun <reified R> safeCall(
    crossinline networkCall: suspend () -> HttpResponse
): ApiResult<R> {
    return try {
        val response = withContext(Dispatchers.IO) {
            networkCall()
        }
        ApiResult.Success(data = response.body())
    } catch (exception: ClientRequestException) {
        ApiResult.Error(throwable = exception)
    } catch (exception: Exception) {
        ApiResult.Error(throwable = exception)
    }
}