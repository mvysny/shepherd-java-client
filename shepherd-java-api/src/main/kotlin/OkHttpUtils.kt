@file:OptIn(ExperimentalSerializationApi::class)

package com.github.mvysny.shepherd.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.FileNotFoundException
import java.io.IOException


/**
 * Documents a HTTP failure.
 * @property statusCode the HTTP status code, one of [javax.servlet.http.HttpServletResponse] `SC_*` constants.
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
public fun Response.checkOk(): Response {
    if (!isSuccessful) {
        val response = body!!.string()
        if (code == 404) throw FileNotFoundException("$code: $response (${request.method} ${request.url})")
        throw HttpResponseException(code, request.method, request.url.toString(), response)
    }
    return this
}

/**
 * Destroys the [OkHttpClient] including the dispatcher, connection pool, everything. WARNING: THIS MAY AFFECT
 * OTHER http clients if they share e.g. dispatcher executor service.
 */
public fun OkHttpClient.destroy() {
    dispatcher.executorService.shutdown()
    connectionPool.evictAll()
    cache?.close()
}

/**
 * Parses the response as a JSON and converts it to a Java object.
 */
public inline fun <reified T> ResponseBody.json(json: Json = Json): T = json.decodeFromStream(byteStream().buffered())

/**
 * Runs given [request] synchronously and then runs [responseBlock] with the response body.
 * Everything including the [Response] and [ResponseBody] is properly closed afterwards.
 *
 * The [responseBlock] is only called on HTTP 200..299 SUCCESS. [checkOk] is used, to check for
 * possible failure reported as HTTP status code, prior calling the block.
 * @param responseBlock runs on success. Takes a [ResponseBody] and produces the object of type [T].
 * You can use [json] or other utility methods to convert JSON to a Java object.
 * @return whatever has been returned by [responseBlock]
 */
public fun <T> OkHttpClient.exec(request: Request, responseBlock: (ResponseBody) -> T): T =
    newCall(request).execute().use {
        val body: ResponseBody = it.checkOk().body!!
        body.use {
            responseBlock(body)
        }
    }

/**
 * Parses this string as a `http://` or `https://` URL. You can configure the URL
 * (e.g. add further query parameters) in [block]. For example:
 * ```
 * val url: HttpUrl = baseUrl.buildUrl {
 *   if (range != 0..Long.MAX_VALUE) {
 *     addQueryParameter("offset", range.first.toString())
 *     addQueryParameter("limit", range.length.toString())
 *   }
 * }
 * ```
 * @throws IllegalArgumentException if the URL is unparseable
 */
public inline fun String.buildUrl(block: HttpUrl.Builder.()->Unit = {}): HttpUrl = toHttpUrl().newBuilder().apply {
    block()
}.build()

/**
 * Builds a new OkHttp [Request] using given URL. You can optionally configure the request in [block]. Use [exec] to
 * execute the request with given OkHttp client and obtain a response. By default the `GET` request gets built.
 */
public inline fun HttpUrl.buildRequest(block: Request.Builder.()->Unit = {}): Request = Request.Builder().url(this).apply {
    block()
}.build()

internal class BasicAuthInterceptor(user: String, password: String) : Interceptor {
    // https://stackoverflow.com/a/36056355/377320
    private val credentials: String = Credentials.basic(user, password)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val authenticatedRequest = request.newBuilder().header("Authorization", credentials).build()
        return chain.proceed(authenticatedRequest)
    }
}

internal class BasicCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, Cookie>()
    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.values.toList()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { this.cookies[it.name] = it }
    }
}
