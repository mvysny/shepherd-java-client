@file:OptIn(ExperimentalSerializationApi::class)

package com.github.mvysny.shepherd.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.util.Base64

/**
 * Documents a HTTP failure.
 * @property statusCode the HTTP status code.
 * @property method the request method, e.g. `"GET"`
 * @property requestUrl the URL requested from the server
 * @property response the response body received from the server, may provide further information to the nature of the failure.
 * May be blank.
 */
public class HttpResponseException(
    public val statusCode: Int,
    public val method: String,
    public val requestUrl: String,
    public val response: String,
    cause: Throwable? = null
) : IOException("$statusCode: $response", cause) {
    override fun toString(): String = "${javaClass.simpleName}: $message ($method $requestUrl)"
}

/**
 * Fails if the response is not in 200..299 range; otherwise returns [this].
 * @throws FileNotFoundException if the HTTP response was 404
 * @throws HttpResponseException if the response is not in 200..299 ([Response.isSuccessful] returns false)
 * @throws IOException on I/O error.
 */
public fun <T> HttpResponse<T>.checkOk(): HttpResponse<T> {
    if (!isSuccessful) {
        val response = bodyAsString()
        if (statusCode() == 404) throw FileNotFoundException("${statusCode()}: $response (${request().method()} ${request().uri()})")
        throw HttpResponseException(statusCode(), request().method(), request().uri().toString(), response)
    }
    return this
}

/**
 * True if [HttpResponse.statusCode] is 200..299
 */
public val HttpResponse<*>.isSuccessful: Boolean get() = statusCode() in 200..299

/**
 * Returns the [HttpResponse.body] as [String].
 */
public fun HttpResponse<*>.bodyAsString(): String {
    return when (val body = body()) {
        is String -> body
        is ByteArray -> body.toString(Charsets.UTF_8)
        is InputStream -> body.readAllBytes().toString(Charsets.UTF_8)
        is Reader -> body.readText()
        is CharArray -> body.concatToString()
        else -> body.toString()
    }
}

/**
 * Parses the response as a JSON and converts it to a Java object.
 */
public inline fun <reified T> HttpResponse<InputStream>.json(json: Json = Json): T = json.decodeFromStream(body().buffered())

/**
 * Runs given [request] synchronously and then runs [responseBlock] with the response body.
 *
 * The [responseBlock] is only called on HTTP 200..299 SUCCESS. [checkOk] is used, to check for
 * possible failure reported as HTTP status code, prior calling the block.
 * @param responseBlock runs on success. Takes a [HttpResponse] and produces the object of type [T].
 * You can use [json] or other utility methods to convert JSON to a Java object.
 * @return whatever has been returned by [responseBlock]
 * @throws FileNotFoundException on 404
 * @throws HttpResponseException or any other http error code
 * @throws IOException on i/o error
 */
public fun <T> HttpClient.exec(request: HttpRequest, responseBlock: (HttpResponse<InputStream>) -> T): T {
    val result = send(request, BodyHandlers.ofInputStream())
    return result.body().use {
        result.checkOk()
        responseBlock(result)
    }
}

/**
 * Builds a new [HttpRequest] using given URL. You can optionally configure the request in [block]. Use [exec] to
 * execute the request with given OkHttp client and obtain a response. By default, the `GET` request gets built.
 */
public inline fun URI.buildRequest(block: HttpRequest.Builder.()->Unit = {}): HttpRequest = HttpRequest.newBuilder(this).apply(block).build()

public fun HttpRequest.Builder.basicAuth(username: String, password: String) {
    val valueToEncode = "$username:$password"
    val h = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.toByteArray())
    header("Authorization", h)
}
