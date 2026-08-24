package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val responseBodyString = buffer.clone().readUtf8()
            if (response.isSuccessful) {
                android.util.Log.d("LOGIN_API", "Response Body: $responseBodyString")
            } else {
                android.util.Log.d("LOGIN_API", "Error Body: $responseBodyString")
            }
        }
        
        response
    }

    private class RetryInterceptor(private val maxRetries: Int = 3) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            var response: okhttp3.Response? = null
            var lastException: IOException? = null
            
            for (i in 0..maxRetries) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful) return response
                    
                    // Retry on 5xx errors or 408 Timeout
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
                        Thread.sleep(1000L * (i + 1))
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
        .addInterceptor(RetryInterceptor(3))
        .addInterceptor(customLoggingInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
