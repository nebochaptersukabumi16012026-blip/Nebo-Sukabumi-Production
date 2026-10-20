package com.example

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("get_dashboard.php")
    suspend fun getDashboard(): Response<ResponseBody>

    @GET("get_anggota.php")
    suspend fun getAnggota(): Response<ResponseBody>

    @GET("get_laporan.php")
    suspend fun getLaporan(): Response<ResponseBody>

    @GET("kas.php")
    suspend fun getKas(): Response<ResponseBody>

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

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}
