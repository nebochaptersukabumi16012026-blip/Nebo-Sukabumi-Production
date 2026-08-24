package com.example.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class DaftarHadir(
    val id: String,
    val judul: String,
    val tanggal: Long,
    val tipe: String, // "KOPDAR" or "TOURING"
    val lokasi: String,
    val catatan: String,
    val pesertaHadir: List<Int> // list of Anggota IDs
)

interface DaftarHadirApi {
    @GET("api.php?action=get_daftar_hadir")
    suspend fun getDaftarHadir(): Response<List<DaftarHadir>>

    @POST("api.php?action=save_daftar_hadir")
    suspend fun saveDaftarHadir(@Body daftarHadir: DaftarHadir): Response<SimpleResponse>

    @GET("api.php?action=delete_daftar_hadir")
    suspend fun deleteDaftarHadir(@Query("id") id: String): Response<SimpleResponse>
}
