package com.example

import com.example.data.Anggota
import com.example.network.NullToEmptyStringAdapter
import com.example.network.PrimitiveAdapters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test

class MoshiFromJsonTest {
    @Test
    fun testDeserialization() {
        val moshi = Moshi.Builder()
            .add(NullToEmptyStringAdapter)
            .add(PrimitiveAdapters)
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(Anggota::class.java)
        val json = """{"id":1,"nama":"Kimet","nra":"038"}"""
        val anggota = adapter.fromJson(json)
        println("MOSHI_FROM_JSON_TEST_OUTPUT: ${anggota?.nra}")
    }
}
