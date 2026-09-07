package com.example.data

import android.util.Log
import com.example.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class CommunityRepository(
    // We don't use DAOs anymore, everything is online
) {
    private val _allAnggota = MutableStateFlow<List<Anggota>>(emptyList())
    val allAnggotaFlow = _allAnggota.asStateFlow()

    private val _allPembayaran = MutableStateFlow<List<Pembayaran>>(emptyList())
    val allPembayaranFlow = _allPembayaran.asStateFlow()

    private val _allPengeluaran = MutableStateFlow<List<Pengeluaran>>(emptyList())
    val allPengeluaranFlow = _allPengeluaran.asStateFlow()

    private val _allKasKeliling = MutableStateFlow<List<KasKeliling>>(emptyList())
    val allKasKelilingFlow = _allKasKeliling.asStateFlow()

    private val _kasKelilingSummary = MutableStateFlow<KasKelilingUnifiedResponse?>(null)
    val kasKelilingSummaryFlow = _kasKelilingSummary.asStateFlow()

    private val _dashboardData = MutableStateFlow<DashboardData?>(null)
    val dashboardDataFlow = _dashboardData.asStateFlow()

    private val _laporanData = MutableStateFlow<com.example.network.LaporanResponse?>(null)
    val laporanDataFlow = _laporanData.asStateFlow()

    private val _communitySettings = MutableStateFlow<CommunitySettings>(CommunitySettings())
    val communitySettingsFlow = _communitySettings.asStateFlow()

    private val _detailKas = MutableStateFlow<DetailKasResponse?>(null)
    val detailKasFlow = _detailKas.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncErrorFlow = _syncError.asStateFlow()

    fun clearSyncError() {
        _syncError.value = null
    }

    // --- SYNC API METHODS ---
    suspend fun syncFromApi() {
        _syncError.value = null
        try {
            // Detail Kas
            fetchDetailKas()

            // Settings
            try {
                val settingsRes = ApiClient.apiService.getSettings()
                if (settingsRes.isSuccessful) {
                    settingsRes.body()?.data?.let { _communitySettings.value = it }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }

            // Dashboard
            try {
                val dashRes = ApiClient.apiService.getDashboard()
                if (dashRes.isSuccessful && dashRes.body()?.status == "success") {
                    val data = dashRes.body()?.data
                    _dashboardData.value = data ?: com.example.network.DashboardData()
                    Log.d("DASHBOARD_API", "HTTP CODE: ${dashRes.code()} - JSON RESPONSE: $data")
                    Log.d("DASHBOARD_API", "total_anggota: ${data?.total_anggota}, kas_keliling: ${data?.kas_keliling ?: data?.total_saldo}, iuran_anniversary: ${data?.iuran_anniversary ?: data?.iuran_aniv}, saldo_kas: ${data?.saldo_kas}, belum_kas: ${data?.belum_kas ?: data?.belum_bayar_kas}, belum_anniversary: ${data?.belum_anniversary ?: data?.belum_bayar_aniv}, total_sisa_cicilan: ${data?.total_sisa_cicilan}, total_pengeluaran: ${data?.totalPengeluaran}")
                } else {
                    _dashboardData.value = com.example.network.DashboardData()
                    Log.w("DASHBOARD_API", "API Unavailable: HTTP ${dashRes.code()}")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _dashboardData.value = com.example.network.DashboardData()
                Log.w("DASHBOARD_API", "Dashboard sync fallback: ${e.message}")
            }

            // Laporan Keuangan API
            try {
                val lapRes = ApiClient.apiService.getLaporan()
                if (lapRes.isSuccessful && lapRes.body()?.status == "success") {
                    _laporanData.value = lapRes.body()?.data
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("LAPORAN_API", "Laporan sync fallback: ${e.message}")
            }

            // Anggota
            try {
                val anggotaRes = ApiClient.apiService.getAnggota()
                if (anggotaRes.isSuccessful) {
                    anggotaRes.body()?.data?.let { list ->
                        _allAnggota.value = list
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("ANGGOTA_API", "Anggota sync fallback: ${e.message}")
            }
            
            // Pengeluaran (pengeluaran.php)
            try {
                val pengeluaranRes = ApiClient.apiService.getPengeluaran()
                if (pengeluaranRes.isSuccessful) {
                    val rawBody = pengeluaranRes.body()?.string() ?: ""
                    Log.d("PENGELUARAN_API", "pengeluaran.php raw response: $rawBody")
                    val list = com.example.network.PengeluaranParser.parsePengeluaranJson(rawBody)
                    _allPengeluaran.value = list
                    Log.d("PENGELUARAN_API", "Loaded ${list.size} pengeluaran items, Total: ${list.sumOf { it.nominal }}")
                } else {
                    Log.w("PENGELUARAN_API", "API Unavailable: HTTP ${pengeluaranRes.code()}")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("PENGELUARAN_API", "Pengeluaran sync fallback: ${e.message}")
            }

            // Kas Keliling
            try {
                val kasRes = ApiClient.apiService.getKasKeliling()
                if (kasRes.isSuccessful) {
                    val rawBody = kasRes.body()?.string() ?: ""
                    Log.d("KAS_KELILING_API", "kas_keliling.php raw response: $rawBody")
                    val result = com.example.network.KasKelilingParser.parseKasKelilingJson(rawBody)
                    _kasKelilingSummary.value = result.summary
                    _allKasKeliling.value = result.transaksi
                } else {
                    Log.w("KAS_KELILING_API", "API Unavailable: HTTP ${kasRes.code()}")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("KAS_KELILING_API", "Kas keliling sync fallback: ${e.message}")
            }

            // Merge Aniv & Cicilan to Pembayaran
            val newPembayaran = mutableListOf<Pembayaran>()
            
            // 1. Fetch from pembayaran table (KAS, etc)
            try {
                val pRes = ApiClient.apiService.getPembayaran()
                if (pRes.isSuccessful) {
                    pRes.body()?.data?.let { newPembayaran.addAll(it) }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("PEMBAYARAN_API", "Pembayaran sync fallback: ${e.message}")
            }

            // 2. Fetch from iuran_anniversary table
            try {
                val anivRes = ApiClient.apiService.getIuranAniv()
                if (anivRes.isSuccessful) {
                    anivRes.body()?.data?.let { list ->
                        newPembayaran.addAll(list.map {
                            val anggotaNama = _allAnggota.value.find { a -> a.id == it.anggota_id }?.nama ?: ""
                            Pembayaran(
                                id = it.id,
                                anggotaId = it.anggota_id,
                                anggotaNama = anggotaNama,
                                jenisPembayaran = "ANIV",
                                nominal = it.nominal,
                                tanggalBayar = try { it.tanggal.toLong() } catch(e:Exception){0L},
                                keterangan = it.keterangan
                            )
                        })
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("ANIV_API", "Aniv sync fallback: ${e.message}")
            }

            try {
                val cicilanRes = ApiClient.apiService.getCicilan()
                if (cicilanRes.isSuccessful) {
                    cicilanRes.body()?.data?.let { list ->
                        newPembayaran.addAll(list.map {
                            val anggotaNama = _allAnggota.value.find { a -> a.id == it.anggota_id }?.nama ?: ""
                            Pembayaran(
                                id = it.id,
                                anggotaId = it.anggota_id,
                                anggotaNama = anggotaNama,
                                jenisPembayaran = "CICILAN",
                                nominal = it.nominal,
                                tanggalBayar = try { it.tanggal.toLong() } catch(e:Exception){0L},
                                keterangan = it.keterangan
                            )
                        })
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w("CICILAN_API", "Cicilan sync fallback: ${e.message}")
            }
            
            _allPembayaran.value = newPembayaran.sortedByDescending { it.tanggalBayar }
            
            // Detail Kas
            try {
                val kasDetailRes = ApiClient.apiService.getDetailKas()
                if (kasDetailRes.isSuccessful && kasDetailRes.body()?.status == "success") {
                    _detailKas.value = kasDetailRes.body()?.data
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("API_SYNC", "Sync info: ${e.message}")
            _syncError.value = "Gagal terhubung ke server (Offline atau Pemeliharaan)"
        }
    }

    suspend fun fetchDetailKas() {
        try {
            val response = ApiClient.apiService.getDetailKas()
            if (response.isSuccessful && response.body()?.status == "success") {
                _detailKas.value = response.body()?.data
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("API_DETAIL_KAS", "Detail Kas sync fallback: ${e.message}")
            _syncError.value = "Gagal memuat Detail Kas: ${e.message}"
        }
    }

    suspend fun resetMemberKas(memberId: Int): Boolean {
        return try {
            val response = ApiClient.apiService.deleteKas(mapOf("id" to memberId, "action" to "reset_member", "role" to "DEVELOPER", "user_role" to "DEVELOPER"))
            if (response.isSuccessful) {
                _allAnggota.value = _allAnggota.value.map {
                    if (it.id == memberId) it.copy(uangKas = 0.0) else it
                }
                _allPembayaran.value = _allPembayaran.value.filterNot {
                    it.anggotaId == memberId && it.jenisPembayaran.equals("KAS", ignoreCase = true)
                }
                syncFromApi()
                fetchDetailKas()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            try {
                val fallback = ApiClient.apiService.actionKas(mapOf("action" to "reset_member", "id" to memberId, "role" to "DEVELOPER", "user_role" to "DEVELOPER"))
                if (fallback.isSuccessful) {
                    _allAnggota.value = _allAnggota.value.map {
                        if (it.id == memberId) it.copy(uangKas = 0.0) else it
                    }
                    _allPembayaran.value = _allPembayaran.value.filterNot {
                        it.anggotaId == memberId && it.jenisPembayaran.equals("KAS", ignoreCase = true)
                    }
                    syncFromApi()
                    fetchDetailKas()
                    true
                } else {
                    false
                }
            } catch (e2: Exception) {
                false
            }
        }
    }

    suspend fun resetAllKas(): Boolean {
        return try {
            val response = ApiClient.apiService.actionKas(mapOf("action" to "reset_all"))
            syncFromApi()
            fetchDetailKas()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllKasKeliling(): List<KasKeliling> = _allKasKeliling.value

    suspend fun insertKasKeliling(kasKeliling: KasKeliling): Long {
        try { ApiClient.apiService.addKasKeliling(kasKeliling) } catch (e: Exception) {}
        syncFromApi()
        return 0L
    }

    suspend fun updateKasKeliling(kasKeliling: KasKeliling) {
        try { ApiClient.apiService.updateKasKeliling(kasKeliling) } catch (e: Exception) {}
        syncFromApi()
    }

    suspend fun deleteKasKeliling(kasKeliling: KasKeliling) {
        try { ApiClient.apiService.deleteKasKeliling(mapOf("id" to kasKeliling.id)) } catch (e: Exception) {}
        syncFromApi()
    }

    suspend fun getAllPengeluaran(): List<Pengeluaran> = _allPengeluaran.value

    suspend fun insertPengeluaran(pengeluaran: Pengeluaran): Long {
        try {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date(pengeluaran.tanggal))
            val payload = mapOf(
                "nama_pengeluaran" to pengeluaran.keterangan,
                "keterangan" to pengeluaran.keterangan,
                "nominal" to pengeluaran.nominal,
                "jumlah" to pengeluaran.nominal,
                "kategori" to pengeluaran.jenisKas,
                "jenis_kas" to pengeluaran.jenisKas,
                "tanggal" to dateStr,
                "tanggal_pengeluaran" to dateStr,
                "bukti" to (pengeluaran.bukti ?: ""),
                "created_by" to pengeluaran.createdBy
            )
            ApiClient.apiService.addPengeluaran(payload)
        } catch (e: Exception) {
            Log.e("PENGELUARAN_API", "Error inserting pengeluaran: ${e.message}", e)
        }
        syncFromApi()
        return 0L
    }

    suspend fun updatePengeluaran(pengeluaran: Pengeluaran) {
        try {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date(pengeluaran.tanggal))
            val payload = mapOf(
                "id" to pengeluaran.id,
                "nama_pengeluaran" to pengeluaran.keterangan,
                "keterangan" to pengeluaran.keterangan,
                "nominal" to pengeluaran.nominal,
                "jumlah" to pengeluaran.nominal,
                "kategori" to pengeluaran.jenisKas,
                "jenis_kas" to pengeluaran.jenisKas,
                "tanggal" to dateStr,
                "tanggal_pengeluaran" to dateStr,
                "bukti" to (pengeluaran.bukti ?: ""),
                "created_by" to pengeluaran.createdBy
            )
            ApiClient.apiService.updatePengeluaran(payload)
        } catch (e: Exception) {
            Log.e("PENGELUARAN_API", "Error updating pengeluaran: ${e.message}", e)
        }
        syncFromApi()
    }

    suspend fun deletePengeluaran(pengeluaran: Pengeluaran): Boolean {
        return try {
            val response = ApiClient.apiService.deletePengeluaran(mapOf("id" to pengeluaran.id))
            if (response.isSuccessful) {
                _allPengeluaran.value = _allPengeluaran.value.filter { it.id != pengeluaran.id }
                syncFromApi()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllAnggota(): List<Anggota> = _allAnggota.value

    suspend fun getAllPembayaran(): List<Pembayaran> = _allPembayaran.value

    suspend fun getAnggotaById(id: Int): Anggota? = _allAnggota.value.find { it.id == id }

    suspend fun getAnggotaByNra(nra: String): Anggota? = _allAnggota.value.find { it.nra == nra }

    fun getPembayaranByAnggotaFlow(anggotaId: Int): Flow<List<Pembayaran>> = 
        allPembayaranFlow.map { list -> list.filter { it.anggotaId == anggotaId } }

    suspend fun insertAnggota(anggota: Anggota): Long {
        try { ApiClient.apiService.addAnggota(anggota) } catch (e: Exception) {}
        syncFromApi()
        return 0L
    }

    suspend fun updateAnggota(anggota: Anggota) {
        try { ApiClient.apiService.updateAnggota(anggota) } catch (e: Exception) {}
        syncFromApi()
    }

    suspend fun deleteAnggota(anggota: Anggota) {
        try { ApiClient.apiService.deleteAnggota(mapOf("id" to anggota.id)) } catch (e: Exception) {}
        syncFromApi()
    }

    suspend fun insertPembayaran(pembayaran: Pembayaran, userRole: String = "developer") {
        val anggota = getAnggotaById(pembayaran.anggotaId)
        if (pembayaran.jenisPembayaran.equals("ANIV", ignoreCase = true)) {
            try { 
                ApiClient.apiService.inputAniv(mapOf(
                    "id_anggota" to pembayaran.anggotaId,
                    "nominal" to pembayaran.nominal,
                    "keterangan" to (pembayaran.keterangan ?: "Iuran Anniversary"),
                    "tanggal" to pembayaran.tanggalBayar,
                    "bukti_pembayaran" to pembayaran.buktiPembayaran,
                    "role" to userRole,
                    "user_role" to userRole
                ))
            } catch (e: Exception) {
                try {
                    ApiClient.apiService.addIuranAniv(IuranAnivDto(
                        anggota_id = pembayaran.anggotaId,
                        nominal = pembayaran.nominal,
                        tanggal = pembayaran.tanggalBayar.toString(),
                        keterangan = pembayaran.keterangan
                    )) 
                } catch (e2: Exception) {}
            }
        } else if (pembayaran.jenisPembayaran.equals("CICILAN", ignoreCase = true)) {
            try { 
                ApiClient.apiService.addCicilan(CicilanDto(
                    anggota_id = pembayaran.anggotaId,
                    nominal = pembayaran.nominal,
                    tanggal = pembayaran.tanggalBayar.toString(),
                    keterangan = pembayaran.keterangan
                )) 
            } catch (e: Exception) {}
        } else if (pembayaran.jenisPembayaran.equals("KAS", ignoreCase = true)) {
            try {
                ApiClient.apiService.inputKas(mapOf(
                    "id_anggota" to pembayaran.anggotaId,
                    "nominal" to pembayaran.nominal,
                    "keterangan" to (pembayaran.keterangan ?: "Iuran Kas"),
                    "tanggal" to pembayaran.tanggalBayar,
                    "bukti_pembayaran" to pembayaran.buktiPembayaran,
                    "role" to userRole,
                    "user_role" to userRole
                ))
            } catch (e: Exception) {
                try {
                    ApiClient.apiService.addPembayaran(pembayaran)
                } catch (e2: Exception) {}
            }
        }
        syncFromApi()
    }

    suspend fun addPembayaran(
        anggotaId: Int,
        anggotaNama: String,
        jenisPembayaran: String,
        nominal: Double,
        buktiPembayaran: String?,
        keterangan: String,
        firestoreId: String = "",
        userRole: String = "developer"
    ) {
        val transaksi = Pembayaran(
            firestoreId = firestoreId,
            anggotaId = anggotaId,
            anggotaNama = anggotaNama,
            jenisPembayaran = jenisPembayaran,
            nominal = nominal,
            buktiPembayaran = buktiPembayaran,
            keterangan = keterangan
        )
        insertPembayaran(transaksi, userRole)
    }

    suspend fun updatePembayaran(pembayaran: Pembayaran, oldNominal: Double) {
        // No update logic for Pembayaran in the PHP backend, except maybe deleting and re-adding?
        // Let's ignore it for now or just sync
        syncFromApi()
    }

    suspend fun deletePembayaran(pembayaran: Pembayaran, userRole: String = "developer"): Boolean {
        var isSuccess = false
        if (pembayaran.jenisPembayaran.equals("ANIV", ignoreCase = true)) {
            try { 
                val resp = ApiClient.apiService.deleteRiwayatAniv(mapOf(
                    "id" to pembayaran.id, 
                    "anggota_id" to pembayaran.anggotaId,
                    "user_role" to userRole,
                    "role" to userRole
                )) 
                isSuccess = resp.isSuccessful
            } catch (e: Exception) {
                try { 
                    val resp2 = ApiClient.apiService.deleteIuranAniv(mapOf("id" to pembayaran.id)) 
                    isSuccess = resp2.isSuccessful
                } catch (e2: Exception) {}
            }
        } else if (pembayaran.jenisPembayaran.equals("CICILAN", ignoreCase = true)) {
            try { 
                val resp = ApiClient.apiService.deleteCicilan(mapOf("id" to pembayaran.id)) 
                isSuccess = resp.isSuccessful
            } catch (e: Exception) {}
        } else {
            // Default KAS
            try { 
                val resp = ApiClient.apiService.hapusKasAnggota(mapOf(
                    "id" to pembayaran.id, 
                    "anggota_id" to pembayaran.anggotaId,
                    "user_role" to userRole,
                    "role" to userRole
                )) 
                isSuccess = resp.isSuccessful
            } catch (e: Exception) {
                try {
                    val resp2 = ApiClient.apiService.deleteRiwayatKas(mapOf(
                        "id" to pembayaran.id, 
                        "anggota_id" to pembayaran.anggotaId,
                        "user_role" to userRole,
                        "role" to userRole
                    )) 
                    isSuccess = resp2.isSuccessful
                } catch (e2: Exception) {
                    try { 
                        val resp3 = ApiClient.apiService.deletePembayaran(mapOf("id" to pembayaran.id, "user_role" to userRole)) 
                        isSuccess = resp3.isSuccessful
                    } catch (e3: Exception) {}
                }
            }
        }

        // HANYA update UI / State jika backend berhasil (HTTP 200 OK)
        if (isSuccess) {
            // HANYA hapus baris di tabel riwayat dan DILARANG KERAS mengurangi saldo akumulasi utama
            _allPembayaran.value = _allPembayaran.value.filter { !(it.id == pembayaran.id && it.jenisPembayaran == pembayaran.jenisPembayaran) }
            syncFromApi()
        }
        return isSuccess
    }

    suspend fun editPembayaran(id: Int, nominalBaru: Double, keterangan: String): BaseResponse<Any> {
        return try {
            val response = ApiClient.apiService.editPembayaran(mapOf(
                "id" to id,
                "nominal_baru" to nominalBaru,
                "keterangan" to keterangan
            ))
            if (response.isSuccessful) {
                val body = response.body() ?: BaseResponse("error", "Empty body")
                syncFromApi()
                body
            } else {
                BaseResponse("error", "HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            BaseResponse("error", e.message)
        }
    }

    suspend fun getDaftarCicilanAktif(): List<CicilanAktifItem> {
        return try {
            val response = ApiClient.apiService.getDaftarCicilanAktif()
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!
                if (list.isNotEmpty()) list else getDaftarCicilanAktifFallback()
            } else {
                getDaftarCicilanAktifFallback()
            }
        } catch (e: Exception) {
            getDaftarCicilanAktifFallback()
        }
    }

    private fun getDaftarCicilanAktifFallback(): List<CicilanAktifItem> {
        return _allAnggota.value.filter {
            val sisa = if (it.sisaCicilan > 0) it.sisaCicilan else (it.hargaBarang - it.totalCicilan)
            sisa > 0.0
        }.map {
            val sudahBayar = it.totalCicilan
            val sisa = if (it.sisaCicilan > 0) it.sisaCicilan else (it.hargaBarang - sudahBayar)
            CicilanAktifItem(
                id = it.id,
                nama = it.nama,
                nra = it.nra.ifBlank { "-" },
                harga_barang = it.hargaBarang,
                sudah_dibayar = sudahBayar,
                sisa_cicilan = sisa,
                cicilan_per_bulan = it.cicilanPerBulan
            )
        }.sortedByDescending { it.sisa_cicilan }
    }

    suspend fun clearAllData() {
        _allAnggota.value = emptyList()
        _allPembayaran.value = emptyList()
        _allPengeluaran.value = emptyList()
        _allKasKeliling.value = emptyList()
    }

    suspend fun restoreDatabase(anggotaList: List<Anggota>, pembayaranList: List<Pembayaran>) {
        _allAnggota.value = anggotaList
        _allPembayaran.value = pembayaranList
    }

    fun setAllAnggota(list: List<Anggota>) { _allAnggota.value = list }
    fun setAllPembayaran(list: List<Pembayaran>) { _allPembayaran.value = list }
    fun setAllPengeluaran(list: List<Pengeluaran>) { _allPengeluaran.value = list }
    fun setAllKasKeliling(list: List<KasKeliling>) { _allKasKeliling.value = list }
}
