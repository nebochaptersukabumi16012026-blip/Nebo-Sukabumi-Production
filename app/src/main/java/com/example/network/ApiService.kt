package com.example.network

import com.example.data.Anggota
import com.example.data.KasKeliling
import com.example.data.Pengeluaran
import com.example.data.Pembayaran
import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.*

data class BaseResponse<T>(
    val status: String,
    val message: String? = null,
    val data: T? = null,
    val id: Int? = null
)

data class LoginRequest(val username: String, val password: String)
data class LoginData(
    val id: Int, 
    val username: String, 
    val role: String,
    val nama: String? = null,
    val nra: String? = null,
    val require_new_password: Boolean? = false,
    val request_id: Int? = null
)

data class GantiPasswordRequest(
    val username: String? = null,
    val nra: String? = null,
    val id: Int? = null,
    val password_lama: String,
    val password_baru: String,
    val konfirmasi_password: String
)

data class ResetPasswordRequest(
    val id: Int,
    val nama_anggota: String,
    val nra: String,
    val tanggal: String,
    val jam: String,
    val role: String,
    val status: String,
    val password_sementara: String?
)
data class DashboardData(
    @Json(name = "total_anggota") val total_anggota: Int? = null,
    @Json(name = "total_kas") val total_kas: Double? = null,
    @Json(name = "total_aniv") val total_aniv: Double? = null,
    @Json(name = "iuran_aniv") val iuran_aniv: Double? = null,
    @Json(name = "iuran_anniversary") val iuran_anniversary: Double? = null,
    @Json(name = "total_pengeluaran") val totalPengeluaran: Double? = null,
    @Json(name = "total_sisa_cicilan") val total_sisa_cicilan: Double? = null,
    @Json(name = "total_saldo") val total_saldo: Double? = null,
    @Json(name = "kas_keliling") val kas_keliling: Double? = null,
    @Json(name = "kas_keliling_bulan_ini") val kas_keliling_bulan_ini: Double? = null,
    @Json(name = "saldo_kas") val saldo_kas: Double? = null,
    @Json(name = "belum_bayar_kas") val belum_bayar_kas: Int? = null,
    @Json(name = "belum_kas") val belum_kas: Int? = null,
    @Json(name = "belum_bayar_aniv") val belum_bayar_aniv: Int? = null,
    @Json(name = "belum_anniversary") val belum_anniversary: Int? = null,
    @Json(name = "kas_utama") val kas_utama: KasUtamaLaporan? = null,
    @Json(name = "kas_keliling_data") val kas_keliling_data: KasKelilingLaporan? = null,
    @Json(name = "kas_anniversary_data") val kas_anniversary_data: KasAnniversaryLaporan? = null
)

data class KasUtamaLaporan(
    @Json(name = "total_pemasukan") val total_pemasukan: Double = 0.0,
    @Json(name = "total_pengeluaran") val total_pengeluaran: Double = 0.0,
    @Json(name = "saldo_kas") val saldo_kas: Double = 0.0
)

data class KasKelilingLaporan(
    @Json(name = "total_pemasukan") val total_pemasukan: Double = 0.0,
    @Json(name = "total_pengeluaran") val total_pengeluaran: Double = 0.0,
    @Json(name = "saldo_keliling") val saldo_keliling: Double = 0.0
)

data class KasAnniversaryLaporan(
    @Json(name = "total_pemasukan") val total_pemasukan: Double = 0.0,
    @Json(name = "total_pengeluaran") val total_pengeluaran: Double = 0.0,
    @Json(name = "saldo_aniv") val saldo_aniv: Double = 0.0
)

data class CicilanLaporan(
    @Json(name = "total_harga_barang") val total_harga_barang: Double = 0.0,
    @Json(name = "total_sudah_dibayar") val total_sudah_dibayar: Double = 0.0,
    @Json(name = "total_sisa_cicilan") val total_sisa_cicilan: Double = 0.0
)

data class LaporanResponse(
    @Json(name = "kas_utama") val kas_utama: KasUtamaLaporan? = null,
    @Json(name = "kas_keliling") val kas_keliling: KasKelilingLaporan? = null,
    @Json(name = "kas_anniversary") val kas_anniversary: KasAnniversaryLaporan? = null,
    @Json(name = "cicilan") val cicilan: CicilanLaporan? = null
)

data class DetailKasResponse(
    @Json(name = "total_pemasukan") val total_pemasukan: Double? = null,
    @Json(name = "total_pengeluaran") val total_pengeluaran: Double? = null,
    @Json(name = "saldo") val saldo: Double? = null,
    @Json(name = "riwayat") val riwayat: List<RiwayatTransaksiKas>? = null
)

data class RiwayatTransaksiKas(
    val id: Int? = null,
    val nra: String? = null,
    val nama: String = "",
    val nominal: Double = 0.0,
    val tanggal: String? = "Hari Ini",
    val keterangan: String? = "Iuran Kas Anggota",
    val status: String? = "TERKONFIRMASI",
    val jenis: String? = null // MASUK / KELUAR
)

data class IuranAnivDto(
    val id: Int = 0,
    val anggota_id: Int,
    val nominal: Double,
    val tanggal: String,
    val keterangan: String
)

data class CicilanDto(
    val id: Int = 0,
    val anggota_id: Int,
    val nominal: Double,
    val tanggal: String,
    val keterangan: String
)

data class AbsensiDto(
    val id: Int = 0,
    val anggota_id: Int,
    val tanggal: String,
    val status: String,
    val keterangan: String
)

data class CatatanDto(
    val id: Int = 0,
    val judul: String,
    val isi: String,
    val tanggal: String
)

data class KasKelilingUnifiedResponse(
    @Json(name = "transaksi") val transaksi: List<KasKeliling>? = null,
    @Json(name = "total_pemasukan") val total_pemasukan: Double? = null,
    @Json(name = "total_pengeluaran") val total_pengeluaran: Double? = null,
    @Json(name = "saldo_kas_keliling") val saldo_kas_keliling: Double? = null
)

interface ApiService {
    @POST("login.php")
    suspend fun login(@Body req: LoginRequest): Response<BaseResponse<LoginData>>

    @GET("health.php")
    suspend fun checkHealth(): Response<BaseResponse<Any>>

    @Headers("Cache-Control: no-cache")
    @GET("detail_kas.php")
    suspend fun getDetailKas(): Response<BaseResponse<DetailKasResponse>>

    @POST("kas.php")
    suspend fun actionKas(@Body body: Map<String, Any>): Response<BaseResponse<Any>>

    @POST("delete_kas.php")
    suspend fun deleteKas(@Body body: Map<String, Any>): Response<BaseResponse<Any>>

    @Headers("Cache-Control: no-cache")
    @GET("dashboard.php")
    suspend fun getDashboard(): Response<BaseResponse<DashboardData>>

    @Headers("Cache-Control: no-cache")
    @GET("get_laporan.php")
    suspend fun getLaporan(): Response<BaseResponse<LaporanResponse>>

    @Headers("Cache-Control: no-cache")
    @GET("anggota.php")
    suspend fun getAnggota(): Response<BaseResponse<List<Anggota>>>

    @Headers("Cache-Control: no-cache")
    @POST("anggota.php")
    suspend fun addAnggota(@Body anggota: Anggota): Response<BaseResponse<Any>>

    @Headers("Cache-Control: no-cache")
    @PUT("anggota.php")
    suspend fun updateAnggota(@Body anggota: Anggota): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "anggota.php", hasBody = true)
    suspend fun deleteAnggota(@Body req: Map<String, Int>): Response<BaseResponse<Any>>

    @Headers("Cache-Control: no-cache")
    @GET("kas_keliling.php")
    suspend fun getKasKeliling(): Response<okhttp3.ResponseBody>

    @POST("kas_keliling.php")
    suspend fun addKasKeliling(@Body kas: KasKeliling): Response<BaseResponse<Any>>

    @PUT("kas_keliling.php")
    suspend fun updateKasKeliling(@Body kas: KasKeliling): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "kas_keliling.php", hasBody = true)
    suspend fun deleteKasKeliling(@Body req: Map<String, Int>): Response<BaseResponse<Any>>

    @GET("pembayaran.php")
    suspend fun getPembayaran(): Response<BaseResponse<List<Pembayaran>>>

    @POST("pembayaran.php")
    suspend fun addPembayaran(@Body pembayaran: Pembayaran): Response<BaseResponse<Any>>

    @POST("input_kas.php")
    suspend fun inputKas(@Body req: Map<String, @JvmSuppressWildcards Any?>): Response<BaseResponse<Any>>

    @POST("input_aniv.php")
    suspend fun inputAniv(@Body req: Map<String, @JvmSuppressWildcards Any?>): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "pembayaran.php", hasBody = true)
    suspend fun deletePembayaran(@Body req: Map<String, @JvmSuppressWildcards Any?>): Response<BaseResponse<Any>>

    @POST("delete_riwayat_kas.php")
    suspend fun deleteRiwayatKas(@Body req: Map<String, @JvmSuppressWildcards Any?>): Response<BaseResponse<Any>>

    @POST("delete_riwayat_aniv.php")
    suspend fun deleteRiwayatAniv(@Body req: Map<String, @JvmSuppressWildcards Any?>): Response<BaseResponse<Any>>

    @Headers("Cache-Control: no-cache")
    @GET("pengeluaran.php")
    suspend fun getPengeluaran(): Response<okhttp3.ResponseBody>

    @POST("pengeluaran.php")
    suspend fun addPengeluaran(@Body pengeluaran: Map<String, Any?>): Response<BaseResponse<Any>>

    @PUT("pengeluaran.php")
    suspend fun updatePengeluaran(@Body pengeluaran: Map<String, Any?>): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "pengeluaran.php", hasBody = true)
    suspend fun deletePengeluaran(@Body req: Map<String, Int>): Response<BaseResponse<Any>>

    @GET("iuran_anniversary.php")
    suspend fun getIuranAniv(): Response<BaseResponse<List<IuranAnivDto>>>

    @POST("iuran_anniversary.php")
    suspend fun addIuranAniv(@Body dto: IuranAnivDto): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "iuran_anniversary.php", hasBody = true)
    suspend fun deleteIuranAniv(@Body req: Map<String, Int>): Response<BaseResponse<Any>>

    @GET("cicilan.php")
    suspend fun getCicilan(): Response<BaseResponse<List<CicilanDto>>>

    @POST("cicilan.php")
    suspend fun addCicilan(@Body dto: CicilanDto): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "cicilan.php", hasBody = true)
    suspend fun deleteCicilan(@Body req: Map<String, Int>): Response<BaseResponse<Any>>

    @GET("absensi.php")
    suspend fun getAbsensi(): Response<BaseResponse<List<AbsensiDto>>>

    @POST("absensi.php")
    suspend fun addAbsensi(@Body dto: AbsensiDto): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "absensi.php", hasBody = true)
    suspend fun deleteAbsensi(@Body req: Map<String, Int>): Response<BaseResponse<Any>>

    @GET("catatan.php")
    suspend fun getCatatan(): Response<BaseResponse<List<CatatanDto>>>

    @POST("catatan.php")
    suspend fun addCatatan(@Body dto: CatatanDto): Response<BaseResponse<Any>>
    
    @PUT("catatan.php")
    suspend fun updateCatatan(@Body dto: CatatanDto): Response<BaseResponse<Any>>

    @HTTP(method = "DELETE", path = "catatan.php", hasBody = true)
    suspend fun deleteCatatan(@Body req: Map<String, Int>): Response<BaseResponse<Any>>

    @GET("community_settings.php")
    suspend fun getSettings(): Response<BaseResponse<com.example.network.CommunitySettings>>

    @PUT("community_settings.php")
    suspend fun saveSettings(@Body settings: com.example.network.CommunitySettings): Response<BaseResponse<Any>>

    @retrofit2.http.Multipart
    @POST("upload_logo.php")
    suspend fun uploadLogo(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Response<BaseResponse<Any>>

    @retrofit2.http.Multipart
    @POST("upload_banner.php")
    suspend fun uploadBanner(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Response<BaseResponse<Any>>

    @retrofit2.http.Multipart
    @POST("upload_splash.php")
    suspend fun uploadSplash(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Response<BaseResponse<Any>>

    @retrofit2.http.Multipart
    @POST("upload_login_bg.php")
    suspend fun uploadLoginBg(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Response<BaseResponse<Any>>

    @retrofit2.http.Multipart
    @POST("upload_profile_banner.php")
    suspend fun uploadProfileBanner(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Response<BaseResponse<Any>>

    @GET("reset_password.php")
    suspend fun getResetRequests(
        @Query("action") action: String = "list"
    ): Response<BaseResponse<List<ResetPasswordRequest>>>

    @POST("reset_password.php")
    suspend fun submitResetRequest(
        @Query("action") action: String = "request",
        @Body req: Map<String, String>
    ): Response<BaseResponse<Any>>

    @POST("reset_password.php")
    suspend fun approveResetRequest(
        @Query("action") action: String = "approve",
        @Body req: Map<String, Int>
    ): Response<BaseResponse<Map<String, String>>>

    @POST("reset_password.php")
    suspend fun rejectResetRequest(
        @Query("action") action: String = "reject",
        @Body req: Map<String, Int>
    ): Response<BaseResponse<Any>>

    @POST("reset_password.php")
    suspend fun completeResetRequest(
        @Query("action") action: String = "reset_complete",
        @Body req: Map<String, String>
    ): Response<BaseResponse<Any>>

    @POST("ganti_password.php")
    suspend fun gantiPassword(@Body req: GantiPasswordRequest): Response<BaseResponse<Any>>

    @POST("ganti_password.php")
    suspend fun gantiPasswordMap(@Body req: Map<String, String>): Response<BaseResponse<Any>>
}
