package com.example

import com.example.data.Anggota
import com.example.network.NullToEmptyStringAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
import org.junit.Assert.assertNotNull

class MoshiToJsonTest {
    @Test
    fun testSerialization() {
        val moshi = Moshi.Builder()
            .add(NullToEmptyStringAdapter)
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(Anggota::class.java)
        val anggota = Anggota(id = 1, nama = "Kimet", nra = "038")
        val json = adapter.toJson(anggota)
        assertNotNull(json)
    }
}
