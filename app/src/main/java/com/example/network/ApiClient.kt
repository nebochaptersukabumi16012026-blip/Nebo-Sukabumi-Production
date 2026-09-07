package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.io.IOException

object NullToEmptyStringAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): String {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Nothing?>()
            return ""
        }
        return reader.nextString()
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: String?) {
        writer.value(value ?: "")
    }
}

object ApiClient {
    private const val BASE_URL = "https://nebosukabumi.net/api/"

    private val moshi = Moshi.Builder()
        .add(NullToEmptyStringAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val customLoggingInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        
        var requestBodyString = ""
        request.body?.let { body ->
            try {
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                requestBodyString = buffer.readUtf8()
            } catch (e: Exception) {
                requestBodyString = "(error reading body)"
            }
        }
        
        android.util.Log.d("LOGIN_API", "Request URL: $url")
        android.util.Log.d("LOGIN_API", "HTTP Method: $method")
        android.util.Log.d("LOGIN_API", "Request Body: $requestBodyString")
        
        val response = chain.proceed(request)
        
        val responseCode = response.code
        android.util.Log.d("LOGIN_API", "Response Code: $responseCode")
        
        val responseBody = response.body
        if (responseBody != null) {
            try {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer
                val responseBodyString = buffer.clone().readUtf8()
                if (response.isSuccessful) {
                    android.util.Log.d("LOGIN_API", "Response Body: $responseBodyString")
                } else {
                    android.util.Log.d("LOGIN_API", "Error Body: $responseBodyString")
                }
            } catch (e: Exception) {
                android.util.Log.d("LOGIN_API", "Error reading response body: ${e.message}")
            }
        }
        
        response
    }

    // Interceptor to prevent HTML pages, cPanel suspended redirects, or non-JSON payloads from crashing Moshi
    private val jsonValidationInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        // Block 3xx redirects (e.g. 302 to cgi-sys/suspendedpage.cgi)
        if (response.isRedirect || response.code in 300..399) {
            val location = response.header("Location") ?: ""
            android.util.Log.w("API_CLIENT", "Redirect detected (${response.code}) to: $location. Blocking redirect to non-API target.")
            val errorJson = """{"status":"error","message":"Layanan server dialihkan ke: $location (Hosting Suspended/Redirect)","data":null}"""
            return@Interceptor response.newBuilder()
                .code(503)
                .message("Service Unavailable (Server Redirected)")
                .body(errorJson.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // If response is 2xx, verify it is truly valid JSON and not an HTML frameset / error landing page
        if (response.isSuccessful) {
            val responseBody = response.body
            if (responseBody != null) {
                val contentType = responseBody.contentType()
                val isHtmlHeader = contentType != null && (
                    contentType.subtype.contains("html", ignoreCase = true) ||
                    contentType.subtype.contains("xml", ignoreCase = true) ||
                    (contentType.type.contains("text", ignoreCase = true) && !contentType.subtype.contains("json", ignoreCase = true))
                )

                if (isHtmlHeader) {
                    android.util.Log.w("API_CLIENT", "HTML Content-Type detected from ${request.url}: $contentType")
                    val errorJson = """{"status":"error","message":"Server mengembalikan respon HTML (Pemeliharaan/Ditangguhkan)","data":null}"""
                    return@Interceptor response.newBuilder()
                        .code(503)
                        .message("Service Unavailable (HTML Response Received)")
                        .body(errorJson.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                        .build()
                }

                try {
                    val source = responseBody.source()
                    source.request(512)
                    val buffer = source.buffer
                    val prefix = buffer.clone().readUtf8().trim()

                    val isNonJson = prefix.startsWith("<") ||
                            prefix.contains("<html", ignoreCase = true) ||
                            prefix.contains("<frameset", ignoreCase = true) ||
                            prefix.contains("suspended", ignoreCase = true) ||
                            (!prefix.startsWith("{") && !prefix.startsWith("["))

                    if (isNonJson) {
                        android.util.Log.w("API_CLIENT", "Non-JSON response detected from ${request.url}: ${prefix.take(120)}")
                        val errorJson = """{"status":"error","message":"Server sedang dalam pemeliharaan atau akun hosting ditangguhkan","data":null}"""
                        return@Interceptor response.newBuilder()
                            .code(503)
                            .message("Service Unavailable (HTML / Non-JSON Response)")
                            .body(errorJson.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                            .build()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("API_CLIENT", "Error verifying response body format: ${e.message}")
                }
            }
        }

        response
    }

    private class RetryInterceptor(private val maxRetries: Int = 1) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            var response: okhttp3.Response? = null
            var lastException: IOException? = null
            
            for (i in 0..maxRetries) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful) return response
                    
                    // Don't retry redirects or client errors
                    if (response.code !in 500..599 && response.code != 408) return response
                    
                    if (i < maxRetries) {
                        response.close()
                    }
                } catch (e: IOException) {
                    lastException = e
                    if (i >= maxRetries) throw e
                }
                
                if (i < maxRetries) {
                    try {
                        Thread.sleep(500L * (i + 1))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw lastException ?: IOException("Retry interrupted", ie)
                    }
                }
            }
            return response ?: throw lastException ?: IOException("Network request failed after retries")
        }
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor(jsonValidationInterceptor)
        .addInterceptor(RetryInterceptor(1))
        .addInterceptor(customLoggingInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ApiService::class.java)
    }
}
