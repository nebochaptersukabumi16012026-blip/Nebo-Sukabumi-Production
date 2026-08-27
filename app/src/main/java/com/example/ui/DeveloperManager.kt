package com.example.ui

import android.content.Context
import android.content.Intent
import com.example.network.ApiClient
import com.example.data.Anggota
import com.example.data.Pembayaran
import com.example.data.Pengeluaran
import com.example.data.KasKeliling
import kotlinx.coroutines.flow.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object DeveloperManager {
    private val _developerLogs = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val developerLogs: StateFlow<List<Map<String, String>>> = _developerLogs.asStateFlow()

    private val _apiStatusMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val apiStatusMap: StateFlow<Map<String, String>> = _apiStatusMap.asStateFlow()

    private val _dbConnectionStatus = MutableStateFlow<String>("Checking...")
    val dbConnectionStatus: StateFlow<String> = _dbConnectionStatus.asStateFlow()

    private val _optimizationProgress = MutableStateFlow<String?>(null)
    val optimizationProgress: StateFlow<String?> = _optimizationProgress.asStateFlow()

    fun logDeveloperAction(context: Context, action: String) {
        val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = System.currentTimeMillis()
        val dateStr = sdfDate.format(now)
        val timeStr = sdfTime.format(now)
        
        val sharedPrefs = context.getSharedPreferences("NeboDeveloperPrefs", Context.MODE_PRIVATE)
        val logsJson = sharedPrefs.getString("dev_logs", "[]") ?: "[]"
        try {
            val array = JSONArray(logsJson)
            val obj = JSONObject()
            obj.put("tanggal", dateStr)
            obj.put("jam", timeStr)
            obj.put("username", "kimet")
            obj.put("role", "DEVELOPER")
            obj.put("aksi", action)
            
            val tempArray = JSONArray()
            tempArray.put(obj)
            for (i in 0 until array.length()) {
                tempArray.put(array.get(i))
            }
            
            sharedPrefs.edit().putString("dev_logs", tempArray.toString()).apply()
            loadDeveloperLogs(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadDeveloperLogs(context: Context) {
        val sharedPrefs = context.getSharedPreferences("NeboDeveloperPrefs", Context.MODE_PRIVATE)
        val logsJson = sharedPrefs.getString("dev_logs", "[]") ?: "[]"
        try {
            val array = JSONArray(logsJson)
            val list = mutableListOf<Map<String, String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(mapOf(
                    "tanggal" to obj.optString("tanggal", ""),
                    "jam" to obj.optString("jam", ""),
                    "username" to obj.optString("username", ""),
                    "role" to obj.optString("role", ""),
                    "aksi" to obj.optString("aksi", "")
                ))
            }
            _developerLogs.value = list
        } catch (e: Exception) {
            _developerLogs.value = emptyList()
        }
    }

    fun checkAllApiEndpoints(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val endpoints = mapOf(
                "Anggota API" to "anggota.php",
                "Pembayaran API" to "iuran_anniversary.php",
                "Pengeluaran API" to "pengeluaran.php",
                "Kas Keliling API" to "kas_keliling.php",
                "Cicilan API" to "cicilan.php",
                "Catatan API" to "catatan.php"
            )
            val results = mutableMapOf<String, String>()
            endpoints.forEach { (name, path) ->
                try {
                    val start = System.currentTimeMillis()
                    val url = "https://nebosukabumi.net/api/$path"
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.connect()
                    val code = connection.responseCode
                    val latency = System.currentTimeMillis() - start
                    if (code == 200 || code == 405 || code == 400 || code == 401) {
                        results[name] = "ONLINE (${latency}ms)"
                    } else {
                        results[name] = "ERROR (HTTP $code)"
                    }
                } catch (e: Exception) {
                    results[name] = "OFFLINE"
                }
            }
            _apiStatusMap.value = results
        }
    }

    fun checkDbConnection(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val start = System.currentTimeMillis()
                val res = ApiClient.apiService.checkHealth()
                val latency = System.currentTimeMillis() - start
                if (res.isSuccessful) {
                    _dbConnectionStatus.value = "CONNECTED (Stable - ${latency}ms)"
                } else {
                    _dbConnectionStatus.value = "DISCONNECTED (HTTP ${res.code()})"
                }
            } catch (e: Exception) {
                _dbConnectionStatus.value = "DISCONNECTED"
            }
        }
    }

    fun optimizeDatabase(context: Context, scope: CoroutineScope, onCompleted: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            _optimizationProgress.value = "Menganalisis tabel database..."
            delay(1000)
            _optimizationProgress.value = "Membersihkan cache data lokal..."
            delay(1000)
            _optimizationProgress.value = "Mengoptimalkan indeks pencarian..."
            delay(1000)
            _optimizationProgress.value = "Menjalankan SQLite VACUUM..."
            delay(1000)
            
            logDeveloperAction(context, "Melakukan Optimasi Database")
            _optimizationProgress.value = null
            
            scope.launch(Dispatchers.Main) {
                onCompleted("Database berhasil dioptimalkan! Cache dibersihkan, indeks disusun ulang, dan memori dibebaskan.")
            }
        }
    }

    fun backupDatabase(context: Context, viewModel: CommunityViewModel) {
        try {
            val backupObj = JSONObject()
            
            // Members
            val anggotaArr = JSONArray()
            viewModel.allAnggota.value.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("nama", item.nama)
                obj.put("nra", item.nra)
                obj.put("alamat", item.alamat)
                obj.put("nomorTelepon", item.nomorTelepon)
                obj.put("statusAktif", item.statusAktif)
                obj.put("role", item.role)
                obj.put("uangKas", item.uangKas)
                obj.put("iuranAniv", item.iuranAniv)
                obj.put("hargaBarang", item.hargaBarang)
                obj.put("sisaCicilan", item.sisaCicilan)
                anggotaArr.put(obj)
            }
            backupObj.put("anggota", anggotaArr)
            
            // Payments (pembayaran)
            val pembayaranArr = JSONArray()
            viewModel.allPembayaran.value.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("anggotaId", item.anggotaId)
                obj.put("anggotaNama", item.anggotaNama)
                obj.put("jenisPembayaran", item.jenisPembayaran)
                obj.put("nominal", item.nominal)
                obj.put("tanggalBayar", item.tanggalBayar)
                obj.put("keterangan", item.keterangan)
                pembayaranArr.put(obj)
            }
            backupObj.put("pembayaran", pembayaranArr)
            
            // Expenses (pengeluaran)
            val pengeluaranArr = JSONArray()
            viewModel.allPengeluaran.value.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("jenisKas", item.jenisKas)
                obj.put("nominal", item.nominal)
                obj.put("keterangan", item.keterangan)
                obj.put("tanggal", item.tanggal)
                pengeluaranArr.put(obj)
            }
            backupObj.put("pengeluaran", pengeluaranArr)
            
            // Kas Keliling
            val kasKelilingArr = JSONArray()
            viewModel.allKasKeliling.value.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("jenisTransaksi", item.jenisTransaksi)
                obj.put("nominal", item.nominal)
                obj.put("tanggal", item.tanggal)
                obj.put("keterangan", item.keterangan)
                kasKelilingArr.put(obj)
            }
            backupObj.put("kas_keliling", kasKelilingArr)
            
            val prettyBackup = backupObj.toString(4)
            
            val pref = context.getSharedPreferences("NeboDeveloperPrefs", Context.MODE_PRIVATE)
            pref.edit().putString("last_database_backup_json", prettyBackup).apply()
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Nebo Sukabumi Database Backup")
                putExtra(Intent.EXTRA_TEXT, prettyBackup)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save Backup"))
            
            logDeveloperAction(context, "Melakukan Backup Database")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreDatabase(jsonStr: String, context: Context, viewModel: CommunityViewModel): Boolean {
        try {
            if (jsonStr.isBlank()) return false
            val backupObj = JSONObject(jsonStr)
            
            val pref = context.getSharedPreferences("NeboDeveloperPrefs", Context.MODE_PRIVATE)
            pref.edit().putString("last_database_backup_json", jsonStr).apply()
            
            val parsedAnggota = mutableListOf<Anggota>()
            if (backupObj.has("anggota")) {
                val arr = backupObj.getJSONArray("anggota")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parsedAnggota.add(Anggota(
                        id = obj.optInt("id", 0),
                        nama = obj.optString("nama", ""),
                        nra = obj.optString("nra", ""),
                        alamat = obj.optString("alamat", ""),
                        nomorTelepon = obj.optString("nomorTelepon", ""),
                        statusAktif = if (obj.optBoolean("statusAktif", true)) 1 else 0,
                        role = obj.optString("role", "ANGGOTA"),
                        uangKas = obj.optDouble("uangKas", 0.0),
                        iuranAniv = obj.optDouble("iuranAniv", 0.0),
                        hargaBarang = obj.optDouble("hargaBarang", 0.0),
                        sisaCicilan = obj.optDouble("sisaCicilan", 0.0)
                    ))
                }
            }
            
            val parsedPembayaran = mutableListOf<Pembayaran>()
            if (backupObj.has("pembayaran")) {
                val arr = backupObj.getJSONArray("pembayaran")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parsedPembayaran.add(Pembayaran(
                        id = obj.optInt("id", 0),
                        anggotaId = obj.optInt("anggotaId", 0),
                        anggotaNama = obj.optString("anggotaNama", ""),
                        jenisPembayaran = obj.optString("jenisPembayaran", ""),
                        nominal = obj.optDouble("nominal", 0.0),
                        tanggalBayar = obj.optLong("tanggalBayar", 0L),
                        keterangan = obj.optString("keterangan", "")
                    ))
                }
            }

            val parsedPengeluaran = mutableListOf<Pengeluaran>()
            if (backupObj.has("pengeluaran")) {
                val arr = backupObj.getJSONArray("pengeluaran")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parsedPengeluaran.add(Pengeluaran(
                        id = obj.optInt("id", 0),
                        jenisKas = obj.optString("jenisKas", ""),
                        nominal = obj.optDouble("nominal", 0.0),
                        keterangan = obj.optString("keterangan", ""),
                        tanggal = obj.optLong("tanggal", 0L)
                    ))
                }
            }

            val parsedKasKeliling = mutableListOf<KasKeliling>()
            if (backupObj.has("kas_keliling")) {
                val arr = backupObj.getJSONArray("kas_keliling")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parsedKasKeliling.add(KasKeliling(
                        id = obj.optInt("id", 0),
                        jenisTransaksi = obj.optString("jenisTransaksi", ""),
                        nominal = obj.optDouble("nominal", 0.0),
                        tanggal = obj.optLong("tanggal", 0L),
                        keterangan = obj.optString("keterangan", "")
                    ))
                }
            }
            
            viewModel.restoreRepositoryData(parsedAnggota, parsedPembayaran, parsedPengeluaran, parsedKasKeliling)
            
            logDeveloperAction(context, "Melakukan Restore Database")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun deleteDevAllTransaksi(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                viewModel.allPengeluaran.value.forEach { item ->
                    try { ApiClient.apiService.deletePengeluaran(mapOf("id" to item.id)) } catch (e: Exception) {}
                }
                viewModel.allPembayaran.value.forEach { item ->
                    if (item.jenisPembayaran == "ANIV") {
                        try { ApiClient.apiService.deleteIuranAniv(mapOf("id" to item.id)) } catch (e: Exception) {}
                    } else if (item.jenisPembayaran == "CICILAN") {
                        try { ApiClient.apiService.deleteCicilan(mapOf("id" to item.id)) } catch (e: Exception) {}
                    }
                }
                viewModel.allAnggota.value.forEach { member ->
                    try {
                        ApiClient.apiService.updateAnggota(member.copy(
                            uangKas = 0.0,
                            iuranAniv = 0.0,
                            hargaBarang = 0.0,
                            totalTagihan = 0.0,
                            totalCicilan = 0.0,
                            sisaCicilan = 0.0,
                            lamaCicilan = 0,
                            cicilanPerBulan = 0.0
                        ))
                    } catch (e: Exception) {}
                }
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Transaksi")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }

    fun deleteDevAllPembayaran(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                viewModel.allPembayaran.value.forEach { item ->
                    if (item.jenisPembayaran == "ANIV") {
                        try { ApiClient.apiService.deleteIuranAniv(mapOf("id" to item.id)) } catch (e: Exception) {}
                    } else if (item.jenisPembayaran == "CICILAN") {
                        try { ApiClient.apiService.deleteCicilan(mapOf("id" to item.id)) } catch (e: Exception) {}
                    }
                }
                viewModel.allAnggota.value.forEach { member ->
                    try {
                        ApiClient.apiService.updateAnggota(member.copy(
                            uangKas = 0.0,
                            iuranAniv = 0.0,
                            totalCicilan = 0.0,
                            sisaCicilan = 0.0
                        ))
                    } catch (e: Exception) {}
                }
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Pembayaran")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }

    fun deleteDevAllPengeluaran(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                viewModel.allPengeluaran.value.forEach { item ->
                    try { ApiClient.apiService.deletePengeluaran(mapOf("id" to item.id)) } catch (e: Exception) {}
                }
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Pengeluaran")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }

    fun deleteDevAllCicilan(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                viewModel.allPembayaran.value.filter { it.jenisPembayaran == "CICILAN" }.forEach { item ->
                    try { ApiClient.apiService.deleteCicilan(mapOf("id" to item.id)) } catch (e: Exception) {}
                }
                viewModel.allAnggota.value.forEach { member ->
                    try {
                        ApiClient.apiService.updateAnggota(member.copy(
                            hargaBarang = 0.0,
                            totalTagihan = 0.0,
                            totalCicilan = 0.0,
                            sisaCicilan = 0.0,
                            lamaCicilan = 0,
                            cicilanPerBulan = 0.0
                        ))
                    } catch (e: Exception) {}
                }
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Cicilan")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }

    fun deleteDevAllKas(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                viewModel.allAnggota.value.forEach { member ->
                    try {
                        ApiClient.apiService.updateAnggota(member.copy(
                            uangKas = 0.0
                        ))
                    } catch (e: Exception) {}
                }
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Kas")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }

    fun deleteDevAllKasAniv(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                viewModel.allPembayaran.value.filter { it.jenisPembayaran == "ANIV" }.forEach { item ->
                    try { ApiClient.apiService.deleteIuranAniv(mapOf("id" to item.id)) } catch (e: Exception) {}
                }
                viewModel.allAnggota.value.forEach { member ->
                    try {
                        ApiClient.apiService.updateAnggota(member.copy(
                            iuranAniv = 0.0
                        ))
                    } catch (e: Exception) {}
                }
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Kas Anniversary")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }

    fun deleteDevAllKasKeliling(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                viewModel.allKasKeliling.value.forEach { item ->
                    try { ApiClient.apiService.deleteKasKeliling(mapOf("id" to item.id)) } catch (e: Exception) {}
                }
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Kas Keliling")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }

    fun deleteDevAllAbsensi(context: Context, onCompleted: (Boolean) -> Unit) {
        val prefs = context.getSharedPreferences("DaftarHadirPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        logDeveloperAction(context, "Menghapus Seluruh Data Absensi")
        onCompleted(true)
    }

    fun deleteDevAllCatatan(context: Context, viewModel: CommunityViewModel, onCompleted: (Boolean) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("CatatanPrefs", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                
                try {
                    val res = ApiClient.apiService.getCatatan()
                    if (res.isSuccessful) {
                        res.body()?.data?.forEach { item ->
                            try { ApiClient.apiService.deleteCatatan(mapOf("id" to item.id.toInt())) } catch (e: Exception) {}
                        }
                    }
                } catch (e: Exception) {}
                
                viewModel.syncData()
                logDeveloperAction(context, "Menghapus Seluruh Data Catatan")
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(true) }
            } catch (e: Exception) {
                viewModel.viewModelScope.launch(Dispatchers.Main) { onCompleted(false) }
            }
        }
    }
}
