package com.example

import com.example.data.Anggota
import com.example.network.ApiClient
import com.example.network.NullToEmptyStringAdapter
import com.example.network.PrimitiveAdapters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.example.network.ApiService

class RetrofitTest2 {
    @Test
    fun testRetrofitRequest() = runBlocking {
        val moshi = Moshi.Builder()
            .add(NullToEmptyStringAdapter)
            .add(PrimitiveAdapters)
            .add(KotlinJsonAdapterFactory())
            .build()
        
        val apiService = Retrofit.Builder()
            .baseUrl("https://nebosukabumi.net/api/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)

        try {
            val response = apiService.addAnggota(Anggota(nama="Kimet API", nra="999"))
            println("API_TEST_OUTPUT: ${response.code()}")
        } catch (e: Exception) {
            println("API_TEST_OUTPUT_ERR: ${e.message}")
        }
    }
}
