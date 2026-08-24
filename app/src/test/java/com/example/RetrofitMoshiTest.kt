package com.example

import com.example.data.Anggota
import com.example.network.ApiClient
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RetrofitMoshiTest {
    @Test
    fun testRetrofitRequest() = runBlocking {
        try {
            val response = ApiClient.apiService.addAnggota(Anggota(nama="Kimet API", nra="999"))
            println("API_TEST_OUTPUT: ${response.code()}")
        } catch (e: Exception) {
            println("API_TEST_OUTPUT: ${e.message}")
        }
    }
}
