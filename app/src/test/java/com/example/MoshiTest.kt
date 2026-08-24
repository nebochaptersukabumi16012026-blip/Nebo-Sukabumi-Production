package com.example

import com.example.data.Anggota
import com.example.network.NullToEmptyStringAdapter
import com.example.network.PrimitiveAdapters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test

class MoshiTest {
    @Test
    fun testSerialization() {
        val moshi = Moshi.Builder()
            .add(NullToEmptyStringAdapter)
            .add(PrimitiveAdapters)
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(Anggota::class.java)
        val json = adapter.toJson(Anggota(id=1, nama="Kimet", nra="038"))
        println("MOSHI_TEST_OUTPUT: $json")
    }
}
