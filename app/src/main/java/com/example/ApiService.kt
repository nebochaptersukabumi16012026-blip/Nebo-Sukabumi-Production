package com.example

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("get_dashboard.php")
    suspend fun getDashboard(): Response<Map<String, Any?>?>

    @GET("get_anggota.php")
    suspend fun getAnggota(): Response<Any?>

    @GET("get_laporan.php")
    suspend fun getLaporan(): Response<Map<String, Any?>?>

    @GET("kas.php")
    suspend fun getKas(): Response<Any?>

    companion object {
        private const val BASE_URL = "https://nebosukabumi.net/api/"

        fun create(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}
