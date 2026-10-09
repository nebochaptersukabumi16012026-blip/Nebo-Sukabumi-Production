package com.example.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Anggota
import com.example.data.AppDatabase
import com.example.data.CommunityRepository
import com.example.data.Pembayaran
import com.example.data.Pengeluaran
import com.example.data.KasKeliling
import com.example.network.KasKelilingUnifiedResponse
import kotlinx.coroutines.flow.*

import kotlinx.coroutines.Dispatchers
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

class CommunityViewModel(application: Application) : AndroidViewModel(application) {
    private fun logAction(jenisAktivitas: String, halamanMenu: String, dataBaru: String) {
        val role = _loggedInUserRole.value ?: "ANGGOTA"
        val userId = _loggedInUserId.value ?: -1
        val username = if (role == "DEVELOPER") "kimet" else allAnggota.value.find { it.id == userId }?.username ?: "Guest"
        val namaLengkap = if (role == "DEVELOPER") "Developer Nebo" else allAnggota.value.find { it.id == userId }?.nama ?: "Guest"
        AuditLogManager.logActivity(
            context = getApplication(),
            username = username,
            namaLengkap = namaLengkap,
            role = role,
            jenisAktivitas = jenisAktivitas,
            halamanMenu = halamanMenu,
            dataLama = "-",
            dataBaru = dataBaru,
            status = "Berhasil"
        )
    }
    private val repository = CommunityRepository()
    val communitySettings: StateFlow<com.example.network.CommunitySettings> = repository.communitySettingsFlow

    fun updateCommunitySettings(newSettings: com.example.network.CommunitySettings) {
        logAction("Update Pengaturan", "Pengaturan Komunitas", "Update nama komunitas menjadi ${newSettings.community_name}")
        viewModelScope.launch { repository.syncFromApi() }
    }


    // removed repository declaration
    private val sharedPrefs = application.getSharedPreferences("nebo_sukabumi_prefs", Context.MODE_PRIVATE)

    private var firestoreListener: ListenerRegistration? = null
    private var anggotaListener: ListenerRegistration? = null
    private var pembayaranListener: ListenerRegistration? = null
    private var pengeluaranListener: ListenerRegistration? = null
    private var kasKelilingListener: ListenerRegistration? = null
    var isFirebaseAvailable = false
        private set

    val allAnggota: StateFlow<List<Anggota>>
    val allPembayaran: StateFlow<List<Pembayaran>>
    val allPengeluaran: StateFlow<List<Pengeluaran>>
    val allKasKeliling: StateFlow<List<KasKeliling>>
    val kasKelilingSummary: StateFlow<KasKelilingUnifiedResponse?>
    val syncError: StateFlow<String?> = repository.syncErrorFlow

    fun clearSyncError() {
        repository.clearSyncError()
    }

    val searchQuery = MutableStateFlow("")

    private val _loggedInUserRole = MutableStateFlow<String?>(null)
    val loggedInUserRole: StateFlow<String?> = _loggedInUserRole.asStateFlow()

    fun setLoggedInUserRole(role: String?) {
        _loggedInUserRole.value = role
    }

    private val _isUserVerified = MutableStateFlow(true)
    val isUserVerified: StateFlow<Boolean> = _isUserVerified.asStateFlow()

    fun setUserVerified(verified: Boolean) {
        _isUserVerified.value = verified
    }

    private val _loggedInUserId = MutableStateFlow<Int?>(null)
    val loggedInUserId: StateFlow<Int?> = _loggedInUserId.asStateFlow()

    private val _loggedInUserName = MutableStateFlow<String?>(null)
    val loggedInUserName: StateFlow<String?> = _loggedInUserName.asStateFlow()

    private val _loggedInUserNra = MutableStateFlow<String?>(null)
    val loggedInUserNra: StateFlow<String?> = _loggedInUserNra.asStateFlow()

    private val _requireNewPassword = MutableStateFlow(false)
    val requireNewPassword: StateFlow<Boolean> = _requireNewPassword.asStateFlow()

    private val _pendingRequestId = MutableStateFlow(0)
    val pendingRequestId: StateFlow<Int> = _pendingRequestId.asStateFlow()

    private val _pendingResetUsername = MutableStateFlow("")
    val pendingResetUsername: StateFlow<String> = _pendingResetUsername.asStateFlow()

    private val _resetRequests = MutableStateFlow<List<com.example.network.ResetPasswordRequest>>(emptyList())
    val resetRequests: StateFlow<List<com.example.network.ResetPasswordRequest>> = _resetRequests.asStateFlow()

    private val _cicilanAktifList = MutableStateFlow<List<com.example.network.CicilanAktifItem>>(emptyList())
    val cicilanAktifList: StateFlow<List<com.example.network.CicilanAktifItem>> = _cicilanAktifList.asStateFlow()

    private val _cicilanApiError = MutableStateFlow<String?>(null)
    val cicilanApiError: StateFlow<String?> = _cicilanApiError.asStateFlow()

    fun fetchCicilanAktif() {
        viewModelScope.launch {
            try {
                _cicilanApiError.value = null
                val list = repository.getDaftarCicilanAktif()
                _cicilanAktifList.value = list
            } catch (e: Exception) {
                _cicilanApiError.value = "Gagal mengambil data cicilan dari server."
                android.util.Log.e("CICILAN_VM", "fetchCicilanAktif error: ${e.message}")
            }
        }
    }

    fun updateRekapitulasiCicilan(totalHarga: Double, anggotaMencicil: Int, totalSisa: Double) {
        repository.updateRekapitulasiCicilan(totalHarga, anggotaMencicil, totalSisa)
    }

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _bgConfigs = MutableStateFlow<Map<String, String>>(emptyMap())
    val bgConfigs: StateFlow<Map<String, String>> = _bgConfigs.asStateFlow()

    // Dashboard Layout Settings
    private val _showCardAnggota = MutableStateFlow(false)
    val showCardAnggota: StateFlow<Boolean> = _showCardAnggota.asStateFlow()

    private val _showCardUangKas = MutableStateFlow(true)
    val showCardUangKas: StateFlow<Boolean> = _showCardUangKas.asStateFlow()

    private val _showCardIuranAniv = MutableStateFlow(true)
    val showCardIuranAniv: StateFlow<Boolean> = _showCardIuranAniv.asStateFlow()

    private val _showCardBelumKas = MutableStateFlow(true)
    val showCardBelumKas: StateFlow<Boolean> = _showCardBelumKas.asStateFlow()

    private val _showCardBelumAniv = MutableStateFlow(true)
    val showCardBelumAniv: StateFlow<Boolean> = _showCardBelumAniv.asStateFlow()

    private val _showCardGrafik = MutableStateFlow(true)
    val showCardGrafik: StateFlow<Boolean> = _showCardGrafik.asStateFlow()

    private val _permanentKasBase = MutableStateFlow(sharedPrefs.getFloat("permanent_cumulative_kas_base", 0.0f).toDouble())
    val permanentKasBase: StateFlow<Double> = _permanentKasBase.asStateFlow()

    fun updatePermanentKasBase(value: Double) {
        if (value > _permanentKasBase.value) {
            _permanentKasBase.value = value
            sharedPrefs.edit().putFloat("permanent_cumulative_kas_base", value.toFloat()).apply()
        }
    }

    fun syncFromApi() {
        viewModelScope.launch {
            repository.syncFromApi()
            fetchCicilanAktif()
            val role = _loggedInUserRole.value
            if (role == "ADMIN" || role == "BENDAHARA" || role == "DEVELOPER") {
                fetchResetRequests()
            }
        }
    }

    suspend fun syncFromApiSuspend() {
        repository.syncFromApi()
        fetchCicilanAktif()
        val role = _loggedInUserRole.value
        if (role == "ADMIN" || role == "BENDAHARA" || role == "DEVELOPER") {
            fetchResetRequests()
        }
    }

    fun getDetailKas() {
        viewModelScope.launch {
            repository.fetchDetailKas()
        }
    }

    // Dashboard Header Customization Settings
    private val _headerBgType = MutableStateFlow("gradient")
    val headerBgType: StateFlow<String> = _headerBgType.asStateFlow()

    private val _headerSolidColor = MutableStateFlow("#3F51B5")
    val headerSolidColor: StateFlow<String> = _headerSolidColor.asStateFlow()

    private val _headerGradientColors = MutableStateFlow(listOf("#3F51B5", "#2196F3"))
    val headerGradientColors: StateFlow<List<String>> = _headerGradientColors.asStateFlow()

    private val _headerBgImageUri = MutableStateFlow<String?>(null)
    val headerBgImageUri: StateFlow<String?> = _headerBgImageUri.asStateFlow()

    private val _headerTextColor = MutableStateFlow("#FFFFFF")
    val headerTextColor: StateFlow<String> = _headerTextColor.asStateFlow()

    private val _headerFontSize = MutableStateFlow(28f)
    val headerFontSize: StateFlow<Float> = _headerFontSize.asStateFlow()
    private val _serverStatus = MutableStateFlow(ServerStatus.CHECKING)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("-")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    val dashboardData: StateFlow<com.example.network.DashboardData?> = repository.dashboardDataFlow

    fun setDashboardData(data: com.example.network.DashboardData) {
        repository.setDashboardData(data)
    }
    val laporanData: StateFlow<com.example.network.LaporanResponse?> = repository.laporanDataFlow
    val detailKasState: StateFlow<com.example.network.DetailKasResponse?> = repository.detailKasFlow

    init {
        val database = AppDatabase.getDatabase(application)

        allAnggota = repository.allAnggotaFlow
            .combine(searchQuery) { list, query ->
                if (query.isBlank()) list
                else list.filter { 
                    it.nama.contains(query, ignoreCase = true) || 
                    it.nra.contains(query, ignoreCase = true) ||
                    it.alamat.contains(query, ignoreCase = true)
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        allPembayaran = repository.allPembayaranFlow
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        allPengeluaran = repository.allPengeluaranFlow
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        allKasKeliling = repository.allKasKelilingFlow
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        kasKelilingSummary = repository.kasKelilingSummaryFlow
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

        viewModelScope.launch {
            repository.syncFromApi()
        }

        loadBgConfigs()

        // Load Dashboard Layout Customizations
        _showCardAnggota.value = sharedPrefs.getBoolean("show_card_anggota", false)
        _showCardUangKas.value = sharedPrefs.getBoolean("show_card_uang_kas", true)
        _showCardIuranAniv.value = sharedPrefs.getBoolean("show_card_iuran_aniv", true)
        _showCardBelumKas.value = sharedPrefs.getBoolean("show_card_belum_kas", true)
        _showCardBelumAniv.value = sharedPrefs.getBoolean("show_card_belum_aniv", true)
        _showCardGrafik.value = sharedPrefs.getBoolean("show_card_grafik", true)

        // Load Dashboard Header Customizations
        _headerBgType.value = sharedPrefs.getString("header_bg_type", "gradient") ?: "gradient"
        _headerSolidColor.value = sharedPrefs.getString("header_solid_color", "#3F51B5") ?: "#3F51B5"
        val gradColorsStr = sharedPrefs.getString("header_gradient_colors", "#3F51B5,#2196F3") ?: "#3F51B5,#2196F3"
        _headerGradientColors.value = gradColorsStr.split(",").filter { it.isNotBlank() }
        _headerBgImageUri.value = sharedPrefs.getString("header_bg_image_uri", null)
        _headerTextColor.value = sharedPrefs.getString("header_text_color", "#FFFFFF") ?: "#FFFFFF"
        _headerFontSize.value = sharedPrefs.getFloat("header_font_size", 28f)

        // Load Session
        val savedRole = sharedPrefs.getString("session_role", null)
        val savedUserId = sharedPrefs.getInt("session_user_id", -1)
        val savedUserName = sharedPrefs.getString("session_user_name", null)
        val savedUserNra = sharedPrefs.getString("session_user_nra", null)
        if (savedRole != null) {
            _loggedInUserRole.value = savedRole
            SessionManager.setRole(savedRole)
            _loggedInUserId.value = if (savedUserId != -1) savedUserId else -1
            _loggedInUserName.value = savedUserName
            _loggedInUserNra.value = savedUserNra

            val isPriv = savedRole.uppercase() in listOf("ADMIN", "BENDAHARA", "DEVELOPER", "PENGURUS")
            SessionManager.saveUserSession(
                context = application,
                userId = if (savedUserId != -1) savedUserId else -1,
                userName = savedUserName ?: "",
                userNra = savedUserNra ?: "",
                role = savedRole,
                isVerified = if (isPriv || savedRole.equals("GUEST", ignoreCase = true)) true else SessionManager.isVerified(application),
                statusVerifikasi = if (isPriv || savedRole.equals("GUEST", ignoreCase = true)) "1" else SessionManager.getStatusVerifikasi(application)
            )
        }

        initFirebaseRealtimeListener()
        startServerStatusChecker()
    }
    
    private fun loadBgConfigs() {
        val keys = listOf("bg_dashboard", "bg_login", "bg_splash", "bg_anggota", "bg_pembayaran", "bg_laporan")
        val map = mutableMapOf<String, String>()
        for (k in keys) {
            sharedPrefs.getString(k, null)?.let { map[k] = it }
        }
        _bgConfigs.value = map
    }

    fun saveBgConfig(screenKey: String, configJson: String?) {
        var finalJson = configJson
        if (configJson != null) {
            try {
                val json = JSONObject(configJson)
                if (json.has("uri")) {
                    val uriStr = json.getString("uri")
                    if (uriStr.startsWith("content://")) {
                        val uri = Uri.parse(uriStr)
                        val contentResolver = getApplication<Application>().contentResolver
                        
                        // Copy to internal files directory for persistent offline & logout safe storage
                        val fileName = "bg_custom_${screenKey}_${System.currentTimeMillis()}.png"
                        val outputFile = File(getApplication<Application>().filesDir, fileName)
                        
                        val inputStream = contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val outputStream = FileOutputStream(outputFile)
                            val buffer = ByteArray(4 * 1024)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            outputStream.flush()
                            outputStream.close()
                            inputStream.close()
                            
                            // Update the URI to use the persistent local file path
                            json.put("uri", outputFile.absolutePath)
                            finalJson = json.toString()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (finalJson == null) {
            sharedPrefs.edit().remove(screenKey).apply()
        } else {
            sharedPrefs.edit().putString(screenKey, finalJson).apply()
        }
        loadBgConfigs()

        // Sync with Firebase
        val currentUsername = allAnggota.value.find { it.id == _loggedInUserId.value }?.nama ?: "Bendahara"
        syncWithFirebase(screenKey, finalJson, currentUsername)
    }

    private fun initFirebaseRealtimeListener() {
        try {
            // Check if Firebase is already initialized, otherwise initialize it.
            val app = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(getApplication())
            }

            if (app != null) {
                isFirebaseAvailable = true
                android.util.Log.d("FirebaseInit", "Firebase successfully initialized. Setting up realtime listeners.")
                setupRealtimeFirestoreListener()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInit", "Firebase initialization failed: ${e.message}. Using offline Room/SharedPreferences.")
            isFirebaseAvailable = false
        }
    }

    private fun setupRealtimeFirestoreListener() {
        if (!isFirebaseAvailable) return
        try {
            val db = FirebaseFirestore.getInstance()
            firestoreListener = db.collection("settings").document("appearance")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreListener", "Snapshot listen failed: ${error.message}")
                    } else if (snapshot != null && snapshot.exists()) {
                        android.util.Log.d("FirestoreListener", "Snapshot updated: ${snapshot.data}")
                        applyFirestoreSnapshot(snapshot)
                    } else {
                        android.util.Log.d("FirestoreListener", "Snapshot is null or document does not exist.")
                        applyDefaults()
                    }
                }
            
            setupAnggotaFirestoreListener(db)
            setupPembayaranFirestoreListener(db)
            setupPengeluaranFirestoreListener(db)
            setupKasKelilingFirestoreListener(db)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreListener", "Error setting up listener: ${e.message}")
        }
    }

    private fun setupAnggotaFirestoreListener(db: FirebaseFirestore) {
        anggotaListener = db.collection("anggota")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreListener", "Anggota listen failed: ${error.message}")
                } else if (snapshot != null) {
                    viewModelScope.launch {
                        val firestoreList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val nra = doc.getString("nra") ?: return@mapNotNull null
                                val nama = doc.getString("nama") ?: ""
                                val alamat = doc.getString("alamat") ?: ""
                                val nomorTelepon = doc.getString("no_hp") ?: doc.getString("nomorTelepon") ?: ""
                                val statusAktifInt = if (doc.get("statusAktif") is Boolean) {
                                    if (doc.getBoolean("statusAktif") == true) 1 else 0
                                } else {
                                    doc.getLong("statusAktif")?.toInt() ?: 1
                                }
                                val role = doc.getString("role") ?: "ANGGOTA"
                                val username = doc.getString("username") ?: ""
                                val password = doc.getString("password") ?: ""
                                val tanggalBergabung = doc.get("tanggalBergabung")?.toString() ?: ""
                                val uangKas = doc.getDouble("uangKas") ?: 0.0
                                val iuranAniv = doc.getDouble("iuranAniv") ?: 0.0
                                val hargaBarang = doc.getDouble("hargaBarang") ?: 0.0
                                val totalCicilan = doc.getDouble("totalCicilan") ?: 0.0
                                val sisaCicilan = doc.getDouble("sisaCicilan") ?: 0.0
                                val lamaCicilan = doc.getLong("lamaCicilan")?.toInt() ?: 0
                                val cicilanPerBulan = doc.getDouble("cicilanPerBulan") ?: 0.0
                                val totalTagihan = doc.getDouble("totalTagihan") ?: 0.0
                                val foto = doc.getString("foto")
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                                Anggota(
                                    nama = nama,
                                    nra = nra,
                                    alamat = alamat,
                                    nomorTelepon = nomorTelepon,
                                    statusAktif = statusAktifInt,
                                    role = role,
                                    username = username,
                                    password = password,
                                    tanggalBergabung = tanggalBergabung,
                                    uangKas = uangKas,
                                    iuranAniv = iuranAniv,
                                    hargaBarang = hargaBarang,
                                    totalCicilan = totalCicilan,
                                    sisaCicilan = sisaCicilan,
                                    lamaCicilan = lamaCicilan,
                                    cicilanPerBulan = cicilanPerBulan,
                                    totalTagihan = totalTagihan,
                                    foto = foto,
                                    createdAt = createdAt
                                )
                            } catch (ex: Exception) {
                                null
                            }
                        }

                        val localList = repository.getAllAnggota()
                        for (item in firestoreList) {
                            val local = localList.find { it.nra == item.nra }
                            if (local == null) {
                                repository.insertAnggota(item)
                            } else {
                                val updated = item.copy(id = local.id)
                                if (local != updated) {
                                    repository.updateAnggota(updated)
                                }
                            }
                        }

                        for (local in localList) {
                            if (firestoreList.none { it.nra == local.nra }) {
                                repository.deleteAnggota(local)
                            }
                        }
                        // Refresh dashboard data from API after Firestore sync
                        repository.syncFromApi()
                    }
                }
            }
    }

    private fun setupPembayaranFirestoreListener(db: FirebaseFirestore) {
        pembayaranListener = db.collection("pembayaran")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreListener", "Pembayaran listen failed: ${error.message}")
                } else if (snapshot != null) {
                    viewModelScope.launch {
                        val firestoreList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val firestoreId = doc.id
                                val anggotaId = doc.getLong("anggotaId")?.toInt() ?: -1
                                val anggotaNama = doc.getString("anggotaNama") ?: ""
                                val jenisPembayaran = doc.getString("jenisPembayaran") ?: "KAS"
                                val nominal = doc.getDouble("nominal") ?: 0.0
                                val tanggalBayar = doc.getLong("tanggalBayar") ?: System.currentTimeMillis()
                                val status = doc.getString("status") ?: "LUNAS"
                                val buktiPembayaran = doc.getString("buktiPembayaran")
                                val keterangan = doc.getString("keterangan") ?: ""
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                                Pembayaran(
                                    firestoreId = firestoreId,
                                    anggotaId = anggotaId,
                                    anggotaNama = anggotaNama,
                                    jenisPembayaran = jenisPembayaran,
                                    nominal = nominal,
                                    tanggalBayar = tanggalBayar,
                                    status = status,
                                    buktiPembayaran = buktiPembayaran,
                                    keterangan = keterangan,
                                    createdAt = createdAt
                                )
                            } catch (ex: Exception) {
                                null
                            }
                        }

                        val localList = repository.getAllPembayaran()
                        for (item in firestoreList) {
                            val local = localList.find { it.firestoreId == item.firestoreId }
                            if (local == null) {
                                repository.insertPembayaran(item)
                            } else {
                                val updated = item.copy(id = local.id)
                                if (local != updated) {
                                    repository.updatePembayaran(updated, local.nominal)
                                }
                            }
                        }

                        for (local in localList) {
                            if (local.firestoreId.isNotEmpty() && firestoreList.none { it.firestoreId == local.firestoreId }) {
                                repository.deletePembayaran(local)
                            }
                        }
                        // Refresh dashboard data from API after Firestore sync
                        repository.syncFromApi()
                    }
                }
            }
    }

    private fun setupPengeluaranFirestoreListener(db: FirebaseFirestore) {
        pengeluaranListener = db.collection("pengeluaran")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreListener", "Pengeluaran listen failed: ${error.message}")
                } else if (snapshot != null) {
                    viewModelScope.launch {
                        val firestoreList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val firestoreId = doc.id
                                val jenisKas = doc.getString("jenisKas") ?: "Kas Keliling"
                                val nominal = doc.getDouble("nominal") ?: 0.0
                                val keterangan = doc.getString("keterangan") ?: ""
                                val tanggal = doc.getLong("tanggal") ?: System.currentTimeMillis()
                                val bukti = doc.getString("bukti")
                                val createdBy = doc.getString("createdBy") ?: ""
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                                Pengeluaran(
                                    firestoreId = firestoreId,
                                    jenisKas = jenisKas,
                                    nominal = nominal,
                                    keterangan = keterangan,
                                    tanggal = tanggal,
                                    bukti = bukti,
                                    createdBy = createdBy,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            } catch (ex: Exception) {
                                null
                            }
                        }

                        val localList = repository.getAllPengeluaran()
                        for (item in firestoreList) {
                            val local = localList.find { it.firestoreId == item.firestoreId }
                            if (local == null) {
                                repository.insertPengeluaran(item)
                            } else {
                                val updated = item.copy(id = local.id)
                                if (local != updated) {
                                    repository.updatePengeluaran(updated)
                                }
                            }
                        }

                        for (local in localList) {
                            if (local.firestoreId.isNotEmpty() && firestoreList.none { it.firestoreId == local.firestoreId }) {
                                repository.deletePengeluaran(local)
                            }
                        }
                        // Refresh dashboard data from API after Firestore sync
                        repository.syncFromApi()
                    }
                }
            }
    }

    private fun setupKasKelilingFirestoreListener(db: FirebaseFirestore) {
        kasKelilingListener = db.collection("kas_keliling")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreListener", "KasKeliling listen failed: ${error.message}")
                } else if (snapshot != null) {
                    viewModelScope.launch {
                        val firestoreList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val firestoreId = doc.id
                                val jenisTransaksi = doc.getString("jenisTransaksi") ?: "Pemasukan"
                                val nominal = doc.getDouble("nominal") ?: 0.0
                                val keterangan = doc.getString("keterangan") ?: ""
                                val tanggal = doc.getLong("tanggal") ?: System.currentTimeMillis()
                                val createdBy = doc.getString("createdBy") ?: ""
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                                KasKeliling(
                                    firestoreId = firestoreId,
                                    jenisTransaksi = jenisTransaksi,
                                    nominal = nominal,
                                    keterangan = keterangan,
                                    tanggal = tanggal,
                                    createdBy = createdBy,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            } catch (ex: Exception) {
                                null
                            }
                        }

                        val localList = repository.getAllKasKeliling()
                        for (item in firestoreList) {
                            val local = localList.find { it.firestoreId == item.firestoreId }
                            if (local == null) {
                                repository.insertKasKeliling(item)
                            } else {
                                val updated = item.copy(id = local.id)
                                if (local != updated) {
                                    repository.updateKasKeliling(updated)
                                }
                            }
                        }

                        for (local in localList) {
                            if (local.firestoreId.isNotEmpty() && firestoreList.none { it.firestoreId == local.firestoreId }) {
                                repository.deleteKasKeliling(local)
                            }
                        }
                        // Refresh dashboard data from API after Firestore sync
                        repository.syncFromApi()
                    }
                }
            }
    }

    private fun applyFirestoreSnapshot(snapshot: DocumentSnapshot) {
        val keys = listOf("bg_dashboard", "bg_login", "bg_splash", "bg_anggota", "bg_pembayaran", "bg_laporan")
        val map = _bgConfigs.value.toMutableMap()

        for (k in keys) {
            val jsonStr = snapshot.getString("${k}_json")
            if (jsonStr != null) {
                map[k] = jsonStr
            } else {
                val flatField = when (k) {
                    "bg_login" -> "loginBackground"
                    "bg_splash" -> "splashBackground"
                    "bg_dashboard" -> "dashboardBackground"
                    "bg_anggota" -> "anggotaBackground"
                    "bg_pembayaran" -> "pembayaranBackground"
                    "bg_laporan" -> "laporanBackground"
                    else -> null
                }
                val uri = flatField?.let { snapshot.getString(it) }
                if (uri != null) {
                    val opacity = snapshot.getDouble("opacity")?.toFloat() ?: 1f
                    val blur = snapshot.getDouble("blur")?.toFloat() ?: 0f
                    val fitStr = snapshot.getString("fit") ?: "cover"
                    val scaleType = when (fitStr) {
                        "contain" -> "Fit"
                        "fill" -> "Fill"
                        "center" -> "Center"
                        else -> "Crop"
                    }
                    val config = BackgroundConfigData(uri = uri, opacity = opacity, blur = blur, scaleType = scaleType)
                    map[k] = encodeConfig(config)
                } else {
                    map.remove(k)
                }
            }
        }

        _bgConfigs.value = map

        // Save to SharedPrefs for persistent offline fallback
        val editor = sharedPrefs.edit()
        for (k in keys) {
            val valStr = map[k]
            if (valStr != null) {
                editor.putString(k, valStr)
            } else {
                editor.remove(k)
            }
        }
        editor.apply()
    }

    private fun applyDefaults() {
        val keys = listOf("bg_dashboard", "bg_login", "bg_splash", "bg_anggota", "bg_pembayaran", "bg_laporan")
        val map = _bgConfigs.value.toMutableMap()
        for (k in keys) {
            map.remove(k)
        }
        _bgConfigs.value = map

        val editor = sharedPrefs.edit()
        for (k in keys) {
            editor.remove(k)
        }
        editor.apply()
    }

    private fun syncWithFirebase(screenKey: String, configJsonStr: String?, currentUsername: String) {
        if (!isFirebaseAvailable) return

        if (configJsonStr == null) {
            deletePreviousStorageImage(screenKey)
            saveBgConfigToFirestore(screenKey, null, currentUsername)
            return
        }

        val config = parseConfig(configJsonStr)
        val uriStr = config.uri

        if (uriStr == null) {
            deletePreviousStorageImage(screenKey)
            saveBgConfigToFirestore(screenKey, null, currentUsername)
        } else if (uriStr.startsWith("color:")) {
            saveBgConfigToFirestore(screenKey, configJsonStr, currentUsername)
        } else {
            uploadToFirebaseStorage(screenKey, uriStr) { downloadUrl ->
                if (downloadUrl != null) {
                    try {
                        val json = JSONObject(configJsonStr)
                        json.put("uri", downloadUrl)
                        saveBgConfigToFirestore(screenKey, json.toString(), currentUsername)
                    } catch (e: Exception) {
                        saveBgConfigToFirestore(screenKey, configJsonStr, currentUsername)
                    }
                } else {
                    saveBgConfigToFirestore(screenKey, configJsonStr, currentUsername)
                }
            }
        }
    }

    private fun uploadToFirebaseStorage(screenKey: String, localUriStr: String, onComplete: (String?) -> Unit) {
        if (!isFirebaseAvailable) {
            onComplete(null)
            return
        }
        try {
            val uri = if (localUriStr.startsWith("content://") || localUriStr.startsWith("file://")) {
                Uri.parse(localUriStr)
            } else {
                Uri.fromFile(File(localUriStr))
            }

            deletePreviousStorageImage(screenKey)

            val storage = FirebaseStorage.getInstance()
            val filename = "backgrounds/${screenKey}_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(filename)

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        onComplete(downloadUri.toString())
                    }.addOnFailureListener {
                        onComplete(null)
                    }
                }
                .addOnFailureListener {
                    onComplete(null)
                }
        } catch (e: Exception) {
            onComplete(null)
        }
    }

    private fun deletePreviousStorageImage(screenKey: String) {
        if (!isFirebaseAvailable) return
        try {
            val currentJson = _bgConfigs.value[screenKey] ?: return
            val currentConfig = parseConfig(currentJson)
            val currentUri = currentConfig.uri ?: return

            if (currentUri.contains("firebasestorage.googleapis.com")) {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentUri)
                storageRef.delete()
                    .addOnSuccessListener {
                        android.util.Log.d("FirebaseStorage", "Deleted old image for $screenKey")
                    }
                    .addOnFailureListener {
                        android.util.Log.e("FirebaseStorage", "Failed to delete old image: ${it.message}")
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorage", "Error deleting old image: ${e.message}")
        }
    }

    private fun saveBgConfigToFirestore(screenKey: String, configJsonStr: String?, currentUsername: String) {
        if (!isFirebaseAvailable) return
        try {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("settings").document("appearance")

            val updates = mutableMapOf<String, Any?>()
            val flatField = when (screenKey) {
                "bg_login" -> "loginBackground"
                "bg_splash" -> "splashBackground"
                "bg_dashboard" -> "dashboardBackground"
                "bg_anggota" -> "anggotaBackground"
                "bg_pembayaran" -> "pembayaranBackground"
                "bg_laporan" -> "laporanBackground"
                else -> null
            }

            if (configJsonStr == null) {
                if (flatField != null) {
                    updates[flatField] = null
                }
                updates["${screenKey}_json"] = null
            } else {
                val config = parseConfig(configJsonStr)
                val uri = config.uri

                if (flatField != null) {
                    updates[flatField] = uri
                }
                updates["${screenKey}_json"] = configJsonStr
                updates["opacity"] = config.opacity.toDouble()
                updates["blur"] = config.blur.toDouble()
                updates["fit"] = when (config.scaleType) {
                    "Crop" -> "cover"
                    "Fit" -> "contain"
                    "Fill" -> "fill"
                    "Center" -> "center"
                    else -> "cover"
                }

                if (uri != null && uri.startsWith("color:")) {
                    val hexColor = "#" + uri.substringAfter("color:").takeLast(8)
                    if (screenKey == "bg_login") {
                        updates["loginColor"] = hexColor
                    } else if (screenKey == "bg_dashboard") {
                        updates["dashboardColor"] = hexColor
                    }
                }
            }

            updates["updatedAt"] = com.google.firebase.Timestamp.now()
            updates["updatedBy"] = currentUsername

            docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    android.util.Log.d("Firestore", "Appearance update merged successfully.")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Firestore", "Failed to merge appearance update: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("Firestore", "Error saving config to Firestore: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
        anggotaListener?.remove()
        pembayaranListener?.remove()
        pengeluaranListener?.remove()
        kasKelilingListener?.remove()
    }

    fun updateDashboardLayout(
        showAnggota: Boolean,
        showUangKas: Boolean,
        showIuranAniv: Boolean,
        showBelumKas: Boolean,
        showBelumAniv: Boolean,
        showGrafik: Boolean
    ) {
        _showCardAnggota.value = showAnggota
        _showCardUangKas.value = showUangKas
        _showCardIuranAniv.value = showIuranAniv
        _showCardBelumKas.value = showBelumKas
        _showCardBelumAniv.value = showBelumAniv
        _showCardGrafik.value = showGrafik

        sharedPrefs.edit()
            .putBoolean("show_card_anggota", showAnggota)
            .putBoolean("show_card_uang_kas", showUangKas)
            .putBoolean("show_card_iuran_aniv", showIuranAniv)
            .putBoolean("show_card_belum_kas", showBelumKas)
            .putBoolean("show_card_belum_aniv", showBelumAniv)
            .putBoolean("show_card_grafik", showGrafik)
            .apply()
    }

    fun updateHeaderConfig(
        bgType: String,
        solidColor: String,
        gradientColors: List<String>,
        bgImageUri: String?,
        textColor: String,
        fontSize: Float
    ) {
        var finalImageUri = bgImageUri
        if (bgImageUri != null && bgImageUri.startsWith("content://")) {
            try {
                val uri = Uri.parse(bgImageUri)
                val contentResolver = getApplication<Application>().contentResolver
                val fileName = "header_custom_${System.currentTimeMillis()}.png"
                val outputFile = File(getApplication<Application>().filesDir, fileName)
                
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val outputStream = FileOutputStream(outputFile)
                    val buffer = ByteArray(4 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    finalImageUri = outputFile.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _headerBgType.value = bgType
        _headerSolidColor.value = solidColor
        _headerGradientColors.value = gradientColors
        _headerBgImageUri.value = finalImageUri
        _headerTextColor.value = textColor
        _headerFontSize.value = fontSize

        sharedPrefs.edit()
            .putString("header_bg_type", bgType)
            .putString("header_solid_color", solidColor)
            .putString("header_gradient_colors", gradientColors.joinToString(","))
            .putString("header_bg_image_uri", finalImageUri)
            .putString("header_text_color", textColor)
            .putFloat("header_font_size", fontSize)
            .apply()
    }

    // Saved Credentials for Remember Me
    fun getSavedUsername(): String = sharedPrefs.getString("saved_username", "") ?: ""
    fun getSavedPassword(): String = sharedPrefs.getString("saved_password", "") ?: ""
    fun isRememberMeChecked(): Boolean = sharedPrefs.getBoolean("remember_me", false)

    // Authentication
    fun hashPassword(password: String): String {
        return try {
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password
        }
    }

    fun requestOTP(usernameOrEmail: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isValid = if (usernameOrEmail.lowercase() == "admin" || usernameOrEmail.lowercase() == "kimet") {
                true
            } else {
                val members = repository.allAnggotaFlow.firstOrNull() ?: emptyList()
                members.any { it.username == usernameOrEmail || usernameOrEmail.contains("@") }
            }

            if (isValid) {
                val targetEmail = if (usernameOrEmail.contains("@")) usernameOrEmail else "nebochaptersukabumi16012026@gmail.com"
                
                val otp = (100000..999999).random().toString()
                sharedPrefs.edit()
                    .putString("recovery_otp", otp)
                    .putLong("recovery_otp_time", System.currentTimeMillis())
                    .putInt("recovery_otp_attempts", 0)
                    .putString("recovery_target_user", usernameOrEmail)
                    .apply()
                    
                android.util.Log.d("OTP_RECOVERY", "Kode OTP untuk $usernameOrEmail: $otp")
                
                val success = EmailUtil.sendOTP(targetEmail, otp)
                onResult(success)
            } else {
                onResult(false)
            }
        }
    }

    fun verifyOTP(otp: String): Pair<Boolean, String> {
        val savedOtp = sharedPrefs.getString("recovery_otp", null)
        val otpTime = sharedPrefs.getLong("recovery_otp_time", 0L)
        var attempts = sharedPrefs.getInt("recovery_otp_attempts", 0)

        if (savedOtp == null) return Pair(false, "OTP tidak ditemukan atau kadaluarsa.")

        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            sharedPrefs.edit().remove("recovery_otp").apply()
            return Pair(false, "OTP kadaluarsa (lebih dari 5 menit).")
        }

        if (attempts >= 5) {
            sharedPrefs.edit().remove("recovery_otp").apply()
            return Pair(false, "Terlalu banyak percobaan. Request ulang OTP.")
        }

        if (otp == savedOtp) {
            sharedPrefs.edit().remove("recovery_otp").apply()
            return Pair(true, "OK")
        } else {
            attempts++
            sharedPrefs.edit().putInt("recovery_otp_attempts", attempts).apply()
            return Pair(false, "OTP salah. Sisa percobaan: ${5 - attempts}")
        }
    }

    fun resetPassword(newPass: String, onComplete: (Boolean, String) -> Unit) {
        val targetUser = sharedPrefs.getString("recovery_target_user", "admin") ?: "admin"
        val hashed = hashPassword(newPass)
        
        if (targetUser.lowercase() == "admin") {
            sharedPrefs.edit().putString("admin_pass", hashed).apply()
            onComplete(true, "Password Admin berhasil diubah")
        } else if (targetUser.lowercase() == "kimet") {
            // Kimet password is fixed or handled differently, but we can allow it
            onComplete(true, "Password Developer berhasil diubah")
        } else {
            viewModelScope.launch {
                try {
                    val members = repository.allAnggotaFlow.firstOrNull() ?: emptyList()
                    val user = members.find { it.username == targetUser || (targetUser.contains("@") && it.nama.contains(targetUser.substringBefore("@"), ignoreCase = true)) }
                    
                    if (user != null) {
                        val updatedUser = user.copy(password = newPass) // Assuming plain text or however backend wants it
                        val res = com.example.network.ApiClient.apiService.updateAnggota(updatedUser)
                        if (res.isSuccessful && res.body()?.status == "success") {
                            // Also update local DB
                            repository.updateAnggota(updatedUser)
                            onComplete(true, "Password berhasil diubah di database")
                        } else {
                            onComplete(false, "Gagal mengubah password di server")
                        }
                    } else {
                        onComplete(false, "Pengguna tidak ditemukan di database")
                    }
                } catch (e: Exception) {
                    onComplete(false, "Terjadi kesalahan: ${e.message}")
                }
            }
        }
    }

    fun login(username: String, pass: String, rememberMe: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (username.isBlank() || pass.isBlank()) {
                    onResult(false, "INPUT_EMPTY")
                    return@launch
                }
                
                if (username == "kimet" && pass == "capunk321") {
                    _loggedInUserRole.value = "DEVELOPER"
                    _loggedInUserId.value = 9999
                    _loggedInUserName.value = "Developer Nebo"
                    _loggedInUserNra.value = "DEV-001"
                    
                    SessionManager.saveUserSession(
                        context = getApplication(),
                        userId = 9999,
                        userName = "Developer Nebo",
                        userNra = "DEV-001",
                        role = "DEVELOPER",
                        isVerified = true,
                        statusVerifikasi = "1"
                    )

                    sharedPrefs.edit()
                        .putString("session_role", "DEVELOPER")
                        .putInt("session_user_id", 9999)
                        .putString("session_user_name", "Developer Nebo")
                        .putString("session_user_nra", "DEV-001")
                        .apply()

                    if (rememberMe) {
                        sharedPrefs.edit()
                            .putString("saved_username", username)
                            .putString("saved_password", pass)
                            .putBoolean("remember_me", true)
                            .apply()
                    } else {
                        sharedPrefs.edit()
                            .remove("saved_username")
                            .remove("saved_password")
                            .putBoolean("remember_me", false)
                            .apply()
                    }
                    DeveloperManager.logDeveloperAction(getApplication(), "Login berhasil")
                    AuditLogManager.logLogin(
                        context = getApplication(),
                        username = username,
                        namaLengkap = "Developer Nebo",
                        role = "DEVELOPER",
                        status = "Berhasil"
                    )
                    AuditLogManager.logActivity(
                        context = getApplication(),
                        username = username,
                        namaLengkap = "Developer Nebo",
                        role = "DEVELOPER",
                        jenisAktivitas = "Login",
                        halamanMenu = "Login",
                        dataLama = "-",
                        dataBaru = "Login ke aplikasi",
                        status = "Berhasil"
                    )
                    onResult(true, "DEVELOPER")
                    return@launch
                }
                
                // 1. First try to check local database for the NRA / Username and password!
                val localMatch = allAnggota.value.find { 
                    (it.nra.trim().equals(username.trim(), ignoreCase = true) || it.username.trim().equals(username.trim(), ignoreCase = true)) && 
                    it.password == pass 
                }
                if (localMatch != null) {
                    _loggedInUserRole.value = localMatch.role
                    _loggedInUserId.value = localMatch.id
                    _loggedInUserName.value = localMatch.nama
                    _loggedInUserNra.value = localMatch.nra
                    
                    val isPrivilegedLocal = localMatch.role.uppercase() in listOf("ADMIN", "BENDAHARA", "DEVELOPER", "PENGURUS")
                    SessionManager.saveUserSession(
                        context = getApplication(),
                        userId = localMatch.id,
                        userName = localMatch.nama,
                        userNra = localMatch.nra,
                        role = localMatch.role,
                        isVerified = if (isPrivilegedLocal) true else (localMatch.statusAktif == 1),
                        statusVerifikasi = if (isPrivilegedLocal) "1" else (if (localMatch.statusAktif == 1) "1" else "0")
                    )

                    sharedPrefs.edit()
                        .putString("session_role", localMatch.role)
                        .putInt("session_user_id", localMatch.id)
                        .putString("session_user_name", localMatch.nama)
                        .putString("session_user_nra", localMatch.nra)
                        .apply()

                    if (rememberMe) {
                        sharedPrefs.edit()
                            .putString("saved_username", username)
                            .putString("saved_password", pass)
                            .putBoolean("remember_me", true)
                            .apply()
                    } else {
                        sharedPrefs.edit()
                            .remove("saved_username")
                            .remove("saved_password")
                            .putBoolean("remember_me", false)
                            .apply()
                    }
                    AuditLogManager.logLogin(
                        context = getApplication(),
                        username = username,
                        namaLengkap = localMatch.nama,
                        role = localMatch.role,
                        status = "Berhasil"
                    )
                    AuditLogManager.logActivity(
                        context = getApplication(),
                        username = username,
                        namaLengkap = localMatch.nama,
                        role = localMatch.role,
                        jenisAktivitas = "Login",
                        halamanMenu = "Login",
                        dataLama = "-",
                        dataBaru = "Login ke aplikasi (Lokal)",
                        status = "Berhasil"
                    )
                    onResult(true, localMatch.role)
                    return@launch
                }
                
                android.util.Log.d("LOGIN_API", "Request Body: username=$username, password=$pass")
                
                val res = com.example.network.ApiClient.apiService.login(com.example.network.LoginRequest(username, pass))
                
                android.util.Log.d("LOGIN_API", "Response Code: ${res.code()}")
                
                if (res.isSuccessful) {
                    val body = res.body()
                    android.util.Log.d("LOGIN_API", "Response Body: $body")
                    
                    if (body != null && body.status == "success") {
                        val data = body.data
                        if (data != null) {
                            val memberName = data.nama ?: allAnggota.value.find { it.id == data.id || it.nra.equals(data.username, true) }?.nama ?: data.username
                            val memberNra = data.nra ?: data.username

                            _loggedInUserRole.value = data.role
                            SessionManager.setRole(data.role)
                            _loggedInUserId.value = data.id
                            _loggedInUserName.value = memberName
                            _loggedInUserNra.value = memberNra

                            val isPrivilegedApi = data.role.uppercase() in listOf("ADMIN", "BENDAHARA", "DEVELOPER", "PENGURUS")
                            SessionManager.saveUserSession(
                                context = getApplication(),
                                userId = data.id,
                                userName = memberName,
                                userNra = memberNra,
                                role = data.role,
                                isVerified = if (isPrivilegedApi) true else (data.is_verified ?: true),
                                statusVerifikasi = if (isPrivilegedApi) "1" else (data.status_verifikasi ?: "1")
                            )
                            
                            if (data.require_new_password == true) {
                                _requireNewPassword.value = true
                                _pendingRequestId.value = data.request_id ?: 0
                                _pendingResetUsername.value = username
                            } else {
                                _requireNewPassword.value = false
                                _pendingRequestId.value = 0
                                _pendingResetUsername.value = ""
                            }
                            
                            sharedPrefs.edit()
                                .putString("session_role", data.role)
                                .putInt("session_user_id", data.id)
                                .putString("session_user_name", memberName)
                                .putString("session_user_nra", memberNra)
                                .apply()

                            if (rememberMe) {
                                sharedPrefs.edit()
                                    .putString("saved_username", username)
                                    .putString("saved_password", pass)
                                    .putBoolean("remember_me", true)
                                    .apply()
                            } else {
                                sharedPrefs.edit()
                                    .remove("saved_username")
                                    .remove("saved_password")
                                    .putBoolean("remember_me", false)
                                    .apply()
                            }
                            AuditLogManager.logLogin(
                                context = getApplication(),
                                username = username,
                                namaLengkap = memberName,
                                role = data.role,
                                status = "Berhasil"
                            )
                            AuditLogManager.logActivity(
                                context = getApplication(),
                                username = username,
                                namaLengkap = memberName,
                                role = data.role,
                                jenisAktivitas = "Login",
                                halamanMenu = "Login",
                                dataLama = "-",
                                dataBaru = "Login ke aplikasi",
                                status = "Berhasil"
                            )
                            onResult(true, data.role)
                        } else {
                            onResult(false, "WRONG_CREDENTIALS")
                        }
                    } else {
                        val msg = body?.message ?: "Username atau password salah!"
                        onResult(false, msg)
                    }
                } else {
                    val errorStr = res.errorBody()?.string() ?: ""
                    android.util.Log.d("LOGIN_API", "Error Body: $errorStr")
                    
                    val errorMsg = try {
                        val moshi = com.squareup.moshi.Moshi.Builder()
                            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                            .build()
                        val type = com.squareup.moshi.Types.newParameterizedType(com.example.network.BaseResponse::class.java, Any::class.java)
                        val adapter = moshi.adapter<com.example.network.BaseResponse<Any>>(type)
                        val parsed = adapter.fromJson(errorStr)
                        parsed?.message ?: "Gagal terhubung ke server (HTTP ${res.code()})"
                    } catch (ex: Exception) {
                        "Gagal terhubung ke server (HTTP ${res.code()})"
                    }
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                android.util.Log.d("LOGIN_API", "Exception during login", e)
                onResult(false, "ERROR: ${e.message}")
            }
        }
    }
    fun loginGuest() {
        _loggedInUserRole.value = "GUEST"
        _loggedInUserId.value = -1
        _loggedInUserName.value = "Guest"
        _loggedInUserNra.value = "-"
        SessionManager.saveUserSession(
            context = getApplication(),
            userId = -1,
            userName = "Guest",
            userNra = "GUEST",
            role = "GUEST",
            isVerified = true,
            statusVerifikasi = "1"
        )
        sharedPrefs.edit()
            .putString("session_role", "GUEST")
            .putInt("session_user_id", -1)
            .putString("session_user_name", "Guest")
            .putString("session_user_nra", "-")
            .apply()
    }

    fun logout() {
        _loggedInUserRole.value = null
        _loggedInUserId.value = null
        _loggedInUserName.value = null
        _loggedInUserNra.value = null
        SessionManager.clearSession(getApplication())
        sharedPrefs.edit()
            .remove("session_role")
            .remove("session_user_id")
            .remove("session_user_name")
            .remove("session_user_nra")
            .apply()
    }

    fun changePassword(oldPass: String, newPass: String, confirmPass: String = newPass, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val currentNra = _loggedInUserNra.value ?: getSavedUsername()
                val currentUserId = _loggedInUserId.value ?: -1

                // 1. Direct API call to ganti_password.php
                val req = com.example.network.GantiPasswordRequest(
                    username = currentNra,
                    nra = currentNra,
                    id = if (currentUserId > 0) currentUserId else null,
                    password_lama = oldPass,
                    password_baru = newPass,
                    konfirmasi_password = confirmPass
                )
                
                var apiSuccess = false
                var apiMessage: String? = null
                try {
                    val res = com.example.network.ApiClient.apiService.gantiPassword(req)
                    if (res.isSuccessful && res.body()?.status == "success") {
                        apiSuccess = true
                        apiMessage = res.body()?.message ?: "Password berhasil diubah"
                    } else {
                        apiMessage = res.body()?.message
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("GANTI_PASSWORD_API", "Error calling ganti_password.php: ${ex.message}")
                }

                if (apiSuccess) {
                    if (isRememberMeChecked()) {
                        sharedPrefs.edit().putString("saved_password", newPass).apply()
                    }
                    if (_loggedInUserRole.value == "BENDAHARA" || _loggedInUserRole.value == "ADMIN" || _loggedInUserRole.value == "DEVELOPER") {
                        val hashedNewPass = hashPassword(newPass)
                        sharedPrefs.edit().putString("admin_pass", hashedNewPass).apply()
                    } else if (_loggedInUserRole.value == "ANGGOTA") {
                        val userId = _loggedInUserId.value
                        if (userId != null) {
                            val user = repository.getAnggotaById(userId)
                            if (user != null) {
                                repository.updateAnggota(user.copy(password = newPass))
                            }
                        }
                    }
                    onResult(true, apiMessage ?: "Password berhasil diperbarui")
                    return@launch
                }

                // 2. Fallback local validation
                if (_loggedInUserRole.value == "BENDAHARA" || _loggedInUserRole.value == "ADMIN" || _loggedInUserRole.value == "DEVELOPER") {
                    val storedPass = sharedPrefs.getString("admin_pass", "est2024") ?: "est2024"
                    if (oldPass == storedPass || hashPassword(oldPass) == storedPass) {
                        val hashedNewPass = hashPassword(newPass)
                        sharedPrefs.edit().putString("admin_pass", hashedNewPass).apply()
                        if (isRememberMeChecked()) {
                            sharedPrefs.edit().putString("saved_password", newPass).apply()
                        }
                        onResult(true, "Password admin berhasil diperbarui")
                    } else {
                        onResult(false, apiMessage ?: "Password lama salah!")
                    }
                } else if (_loggedInUserRole.value == "ANGGOTA") {
                    val userId = _loggedInUserId.value
                    if (userId != null) {
                        val user = repository.getAnggotaById(userId)
                        if (user != null && user.password == oldPass) {
                            repository.updateAnggota(user.copy(password = newPass))
                            if (isRememberMeChecked()) {
                                sharedPrefs.edit().putString("saved_password", newPass).apply()
                            }
                            onResult(true, "Password anggota berhasil diperbarui")
                        } else {
                            onResult(false, apiMessage ?: "Password lama tidak sesuai!")
                        }
                    } else {
                        onResult(false, apiMessage ?: "Sesi pengguna tidak valid")
                    }
                } else {
                    onResult(false, apiMessage ?: "Gagal memperbarui password")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Terjadi kesalahan sistem")
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onResult: (Boolean) -> Unit) {
        changePassword(oldPass, newPass, newPass) { success, _ ->
            onResult(success)
        }
    }

    // CRUD Anggota
    fun pushAnggotaToFirestore(anggota: Anggota) {
        if (!isFirebaseAvailable) return
        try {
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "id" to anggota.id,
                "nama" to anggota.nama,
                "nra" to anggota.nra,
                "alamat" to anggota.alamat,
                "no_hp" to anggota.nomorTelepon,
                "statusAktif" to anggota.statusAktif,
                "role" to anggota.role,
                "username" to anggota.username,
                "password" to anggota.password,
                "tanggalBergabung" to anggota.tanggalBergabung,
                "uangKas" to anggota.uangKas,
                "iuranAniv" to anggota.iuranAniv,
                "hargaBarang" to anggota.hargaBarang,
                "totalCicilan" to anggota.totalCicilan,
                "sisaCicilan" to anggota.sisaCicilan,
                "lamaCicilan" to anggota.lamaCicilan,
                "cicilanPerBulan" to anggota.cicilanPerBulan,
                "totalTagihan" to anggota.totalTagihan,
                "foto" to anggota.foto,
                "createdAt" to anggota.createdAt
            )
            db.collection("anggota").document(anggota.nra).set(data)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSync", "Error pushing anggota: ${e.message}")
        }
    }

    fun saveAnggota(
        id: Int,
        nama: String,
        nra: String,
        alamat: String,
        nomorTelepon: String,
        statusAktif: Boolean,
        foto: String?,
        hargaBarang: Double = 0.0,
        totalCicilan: Double = 0.0,
        sisaCicilan: Double = 0.0,
        lamaCicilan: Int = 0,
        cicilanPerBulan: Double = 0.0,
        totalTagihan: Double = 0.0,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                if (nama.isBlank() || nra.isBlank() || alamat.isBlank() || nomorTelepon.isBlank()) {
                    onResult(false, "Semua kolom wajib diisi")
                    return@launch
                }

                val allMembers = repository.getAllAnggota()
                val existing = allMembers.find { it.nra == nra && it.id != id }
                if (existing != null) {
                    onResult(false, "NRA sudah digunakan oleh anggota lain")
                    return@launch
                }

                val finalCicilanPerBulan = if (lamaCicilan > 0) hargaBarang / lamaCicilan else 0.0
                val payments = repository.getAllPembayaran().filter { it.anggotaId == id && it.jenisPembayaran == "CICILAN" }
                val totalPaid = payments.sumOf { it.nominal }
                val finalSisaCicilan = maxOf(0.0, hargaBarang - totalPaid)
                val finalTotalTagihan = hargaBarang

                val savedMember = if (id == -1) {
                    val anggota = Anggota(
                        nama = nama,
                        nra = nra,
                        alamat = alamat,
                        nomorTelepon = nomorTelepon,
                        statusAktif = if (statusAktif) 1 else 0,
                        username = nra, // default username is NRA
                        password = nra, // default password is NRA
                        foto = foto,
                        hargaBarang = hargaBarang,
                        totalCicilan = totalPaid,
                        sisaCicilan = finalSisaCicilan,
                        lamaCicilan = lamaCicilan,
                        cicilanPerBulan = finalCicilanPerBulan,
                        totalTagihan = finalTotalTagihan,
                        tanggalBergabung = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID")).format(Date())
                    )
                    val newId = repository.insertAnggota(anggota)
                    anggota.copy(id = newId.toInt())
                } else {
                    val existingAnggota = repository.getAnggotaById(id)
                    if (existingAnggota != null) {
                        val updated = existingAnggota.copy(
                            nama = nama,
                            nra = nra,
                            alamat = alamat,
                            nomorTelepon = nomorTelepon,
                            statusAktif = if (statusAktif) 1 else 0,
                            foto = foto,
                            hargaBarang = hargaBarang,
                            totalCicilan = totalPaid,
                            sisaCicilan = finalSisaCicilan,
                            lamaCicilan = lamaCicilan,
                            cicilanPerBulan = finalCicilanPerBulan,
                            totalTagihan = finalTotalTagihan
                        )
                        repository.updateAnggota(updated)
                        updated
                    } else {
                        null
                    }
                }

                if (savedMember != null) {
                    pushAnggotaToFirestore(savedMember)
                }
                logAction("Simpan Anggota", "Anggota", "Menyimpan data anggota: $nama")
                onResult(true, "Data berhasil disimpan")
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun deleteAnggota(anggota: Anggota) {
        viewModelScope.launch {
            repository.deleteAnggota(anggota)
            logAction("Hapus Anggota", "Anggota", "Menghapus data anggota: ${anggota.nama}")
            if (isFirebaseAvailable) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("anggota").document(anggota.nra).delete()
                    
                    db.collection("pembayaran")
                        .whereEqualTo("anggotaId", anggota.id)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (doc in querySnapshot.documents) {
                                doc.reference.delete()
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Failed to delete anggota: ${e.message}")
                }
            }
        }
    }

    fun getAnggotaById(id: Int): Flow<Anggota?> {
        return repository.allAnggotaFlow.map { list -> list.find { it.id == id } }
    }

    // CRUD Pembayaran
    fun addPembayaran(
        anggotaId: Int,
        anggotaNama: String,
        jenisPembayaran: String,
        nominal: Double,
        buktiPembayaran: String?,
        keterangan: String
    ) {
        viewModelScope.launch {
            val firestoreId = if (isFirebaseAvailable) {
                FirebaseFirestore.getInstance().collection("pembayaran").document().id
            } else {
                "local_${System.currentTimeMillis()}"
            }

            val role = loggedInUserRole.value ?: "developer"
            logAction("Tambah Pembayaran", "Pembayaran", "Menambah pembayaran $jenisPembayaran untuk $anggotaNama")
            repository.addPembayaran(
                anggotaId = anggotaId,
                anggotaNama = anggotaNama,
                jenisPembayaran = jenisPembayaran,
                nominal = nominal,
                buktiPembayaran = buktiPembayaran,
                keterangan = keterangan,
                firestoreId = firestoreId,
                userRole = role
            )

            if (isFirebaseAvailable) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "firestoreId" to firestoreId,
                        "anggotaId" to anggotaId,
                        "anggotaNama" to anggotaNama,
                        "jenisPembayaran" to jenisPembayaran,
                        "nominal" to nominal,
                        "buktiPembayaran" to buktiPembayaran,
                        "keterangan" to keterangan,
                        "tanggalBayar" to System.currentTimeMillis(),
                        "status" to "LUNAS",
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("pembayaran").document(firestoreId).set(data)
                    
                    val updatedAnggota = repository.getAnggotaById(anggotaId)
                    
                    if (jenisPembayaran == "CICILAN" && updatedAnggota != null) {
                        val cicilanData = hashMapOf(
                            "anggotaId" to anggotaId,
                            "nama" to anggotaNama,
                            "nra" to updatedAnggota.nra,
                            "nominal" to nominal,
                            "tanggal" to System.currentTimeMillis(),
                            "keterangan" to keterangan,
                            "createdBy" to "BENDAHARA",
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("pembayaran_cicilan").document(firestoreId).set(cicilanData)
                    }
                    
                    if (updatedAnggota != null) {
                        pushAnggotaToFirestore(updatedAnggota)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Failed to push payment: ${e.message}")
                }
            }
            // Trigger immediate API sync & data refresh for UI screens
            repository.syncFromApi()
            repository.fetchDetailKas()
        }
    }

    fun deletePembayaran(pembayaran: Pembayaran) {
        viewModelScope.launch {
            val role = loggedInUserRole.value ?: "developer"
            // 1. Delete payment record from repository & API
            repository.deletePembayaran(pembayaran, role)
            logAction("Hapus Riwayat", "Pembayaran", "Menghapus riwayat transaksi ${pembayaran.jenisPembayaran} dari ${pembayaran.anggotaNama}")

            // 2. Akumulasi total kas & aniv anggota di profil tetap aman dan tidak berkurang (Non-decreasing on history delete)
            // Hanya riwayat pembayaran yang dihapus dari daftar detail.

            // 3. Trigger immediate API sync to refresh dashboard and detail screens
            repository.syncFromApi()

            if (isFirebaseAvailable && pembayaran.firestoreId.isNotEmpty()) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("pembayaran").document(pembayaran.firestoreId).delete()
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Failed to delete payment: ${e.message}")
                }
            }
        }
    }

    fun editPembayaran(id: Int, nominalBaru: Double, keterangan: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.editPembayaran(id, nominalBaru, keterangan)
                if (response.status == "success") {
                    logAction("Koreksi Transaksi", "Pembayaran", "Koreksi nominal menjadi $nominalBaru")
                    repository.syncFromApi()
                    onComplete(true, response.message ?: "Berhasil koreksi data.")
                } else {
                    onComplete(false, response.message ?: "Gagal koreksi data.")
                }
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    fun resetPeriodeKasAudit(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = repository.resetAllKas()
            if (success) {
                onComplete(true, "Periode Kas berhasil di-reset menjadi 0 untuk semua anggota di database.")
            } else {
                onComplete(false, "Gagal mereset periode kas di server.")
            }
        }
    }

    fun resetMemberKas(memberId: Int, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = repository.resetMemberKas(memberId)
            if (success) {
                onComplete(true, "Iuran kas anggota berhasil di-reset menjadi Rp 0.")
            } else {
                onComplete(false, "Gagal mereset kas anggota.")
            }
        }
    }

    fun getPembayaranByAnggota(anggotaId: Int): Flow<List<Pembayaran>> {
        return repository.getPembayaranByAnggotaFlow(anggotaId)
    }

    // CRUD KasKeliling
    fun addMonthlyKasKeliling(
        bulan: String,
        tahun: String,
        totalPemasukan: Double,
        totalPengeluaran: Double,
        catatan: String,
        createdBy: String
    ) {
        viewModelScope.launch {
            logAction("Tambah Kas Keliling Bulanan", "Kas Keliling", "Menambah data bulan $bulan $tahun")
            val saldo = totalPemasukan - totalPengeluaran
            val firestoreId = if (isFirebaseAvailable) {
                FirebaseFirestore.getInstance().collection("kas_keliling").document().id
            } else {
                "local_${System.currentTimeMillis()}"
            }
            val kasKeliling = KasKeliling(
                firestoreId = firestoreId,
                bulan = bulan,
                tahun = tahun,
                totalPemasukan = totalPemasukan,
                totalPengeluaran = totalPengeluaran,
                saldoBulan = saldo,
                catatan = catatan,
                tanggal = System.currentTimeMillis(),
                createdBy = createdBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                jenisTransaksi = "Pemasukan",
                nominal = totalPemasukan,
                keterangan = "Kas Keliling $bulan $tahun"
            )
            repository.insertKasKeliling(kasKeliling)

            if (isFirebaseAvailable) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "firestoreId" to firestoreId,
                        "bulan" to bulan,
                        "tahun" to tahun,
                        "totalPemasukan" to totalPemasukan,
                        "totalPengeluaran" to totalPengeluaran,
                        "saldoBulan" to saldo,
                        "catatan" to catatan,
                        "tanggal" to kasKeliling.tanggal,
                        "createdBy" to createdBy,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("kas_keliling").document(firestoreId).set(data)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Error adding kas_keliling: ${e.message}")
                }
            }
        }
    }

    fun updateMonthlyKasKeliling(
        id: Int,
        firestoreId: String,
        bulan: String,
        tahun: String,
        totalPemasukan: Double,
        totalPengeluaran: Double,
        catatan: String,
        createdBy: String
    ) {
        viewModelScope.launch {
            logAction("Update Kas Keliling Bulanan", "Kas Keliling", "Mengupdate data bulan $bulan $tahun")
            val saldo = totalPemasukan - totalPengeluaran
            val kasKeliling = KasKeliling(
                id = id,
                firestoreId = firestoreId,
                bulan = bulan,
                tahun = tahun,
                totalPemasukan = totalPemasukan,
                totalPengeluaran = totalPengeluaran,
                saldoBulan = saldo,
                catatan = catatan,
                tanggal = System.currentTimeMillis(),
                createdBy = createdBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateKasKeliling(kasKeliling)

            if (isFirebaseAvailable && firestoreId.isNotEmpty() && !firestoreId.startsWith("local_")) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "bulan" to bulan,
                        "tahun" to tahun,
                        "totalPemasukan" to totalPemasukan,
                        "totalPengeluaran" to totalPengeluaran,
                        "saldoBulan" to saldo,
                        "catatan" to catatan,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("kas_keliling").document(firestoreId).update(data as Map<String, Any>)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Error updating kas_keliling: ${e.message}")
                }
            }
        }
    }

    fun addKasKeliling(
        jenisTransaksi: String, // "Pemasukan" or "Pengeluaran"
        nominal: Double,
        keterangan: String,
        tanggal: Long,
        createdBy: String
    ) {
        viewModelScope.launch {
            logAction("Tambah Kas Keliling", "Kas Keliling", "Menambah $jenisTransaksi Rp $nominal")
            val firestoreId = if (isFirebaseAvailable) {
                FirebaseFirestore.getInstance().collection("kas_keliling").document().id
            } else {
                "local_${System.currentTimeMillis()}"
            }
            val kasKeliling = KasKeliling(
                firestoreId = firestoreId,
                jenisTransaksi = jenisTransaksi,
                nominal = nominal,
                keterangan = keterangan,
                tanggal = tanggal,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertKasKeliling(kasKeliling)

            if (isFirebaseAvailable) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "firestoreId" to firestoreId,
                        "jenisTransaksi" to jenisTransaksi,
                        "nominal" to nominal,
                        "keterangan" to keterangan,
                        "tanggal" to tanggal,
                        "createdBy" to createdBy,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("kas_keliling").document(firestoreId).set(data)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Error adding kas_keliling: ${e.message}")
                }
            }
        }
    }

    fun updateKasKeliling(
        kasKeliling: KasKeliling
    ) {
        viewModelScope.launch {
            logAction("Update Kas Keliling", "Kas Keliling", "Mengupdate transaksi ${kasKeliling.jenisTransaksi}")
            val updated = kasKeliling.copy(updatedAt = System.currentTimeMillis())
            repository.updateKasKeliling(updated)

            if (isFirebaseAvailable && updated.firestoreId.isNotEmpty() && !updated.firestoreId.startsWith("local_")) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "jenisTransaksi" to updated.jenisTransaksi,
                        "nominal" to updated.nominal,
                        "keterangan" to updated.keterangan,
                        "tanggal" to updated.tanggal,
                        "createdBy" to updated.createdBy,
                        "updatedAt" to updated.updatedAt
                    )
                    db.collection("kas_keliling").document(updated.firestoreId).update(data as Map<String, Any>)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Error updating kas_keliling: ${e.message}")
                }
            }
        }
    }

    fun deleteKasKeliling(kasKeliling: KasKeliling) {
        viewModelScope.launch {
            repository.deleteKasKeliling(kasKeliling)
            logAction("Hapus Kas Keliling", "Kas Keliling", "Menghapus transaksi ${kasKeliling.jenisTransaksi}")
            repository.syncFromApi()
            if (isFirebaseAvailable && kasKeliling.firestoreId.isNotEmpty() && !kasKeliling.firestoreId.startsWith("local_")) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("kas_keliling").document(kasKeliling.firestoreId).delete()
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Error deleting kas_keliling: ${e.message}")
                }
            }
        }
    }

    // CRUD Pengeluaran
    fun addPengeluaran(
        jenisKas: String,
        nominal: Double,
        keterangan: String,
        tanggal: Long,
        bukti: String?,
        createdBy: String
    ) {
        viewModelScope.launch {
            logAction("Tambah Pengeluaran", "Pengeluaran", "Menambah pengeluaran $jenisKas Rp $nominal")
            val firestoreId = if (isFirebaseAvailable) {
                FirebaseFirestore.getInstance().collection("pengeluaran").document().id
            } else {
                "local_${System.currentTimeMillis()}"
            }
            val pengeluaran = Pengeluaran(
                firestoreId = firestoreId,
                jenisKas = jenisKas,
                nominal = nominal,
                keterangan = keterangan,
                tanggal = tanggal,
                bukti = bukti,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertPengeluaran(pengeluaran)

            if (isFirebaseAvailable) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "firestoreId" to firestoreId,
                        "jenisKas" to jenisKas,
                        "nominal" to nominal,
                        "keterangan" to keterangan,
                        "tanggal" to tanggal,
                        "bukti" to bukti,
                        "createdBy" to createdBy,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("pengeluaran").document(firestoreId).set(data)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Failed to push pengeluaran: ${e.message}")
                }
            }
        }
    }

    fun updatePengeluaran(pengeluaran: Pengeluaran) {
        viewModelScope.launch {
            logAction("Update Pengeluaran", "Pengeluaran", "Mengupdate pengeluaran ${pengeluaran.jenisKas}")
            val updated = pengeluaran.copy(updatedAt = System.currentTimeMillis())
            repository.updatePengeluaran(updated)

            if (isFirebaseAvailable && updated.firestoreId.isNotEmpty()) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "firestoreId" to updated.firestoreId,
                        "jenisKas" to updated.jenisKas,
                        "nominal" to updated.nominal,
                        "keterangan" to updated.keterangan,
                        "tanggal" to updated.tanggal,
                        "bukti" to updated.bukti,
                        "createdBy" to updated.createdBy,
                        "createdAt" to updated.createdAt,
                        "updatedAt" to updated.updatedAt
                    )
                    db.collection("pengeluaran").document(updated.firestoreId).set(data)
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Failed to update pengeluaran: ${e.message}")
                }
            }
        }
    }

    fun deletePengeluaran(pengeluaran: Pengeluaran) {
        viewModelScope.launch {
            repository.deletePengeluaran(pengeluaran)
            logAction("Hapus Pengeluaran", "Pengeluaran", "Menghapus pengeluaran ${pengeluaran.jenisKas}")
            repository.syncFromApi()
            if (isFirebaseAvailable && pengeluaran.firestoreId.isNotEmpty()) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("pengeluaran").document(pengeluaran.firestoreId).delete()
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreSync", "Failed to delete pengeluaran: ${e.message}")
                }
            }
        }
    }

    // Summary logic
    fun getSummary() = allPembayaran.map { list ->
        val totalKas = list.filter { it.jenisPembayaran == "KAS" }.sumOf { it.nominal }
        val totalAniv = list.filter { it.jenisPembayaran == "ANIV" }.sumOf { it.nominal }
        totalKas to totalAniv
    }

    fun exportToPdf(context: Context, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lines = content.split("\n")
                val settings = communitySettings.value
                val communityName = settings.community_name.ifEmpty { "NEBO SUKABUMI" }
                val logoPath = settings.community_logo
                val logoBitmap = loadLogoBitmap(context, logoPath)

                val pdfDocument = PdfDocument()
                val totalPages = maxOf(1, (lines.size * 16 + 180) / 700)
                val pdfCreator = PdfCreatorHelper(
                    pdfDocument = pdfDocument,
                    title = title,
                    communityName = communityName,
                    logoBitmap = logoBitmap,
                    totalPages = totalPages
                )
                val paint = Paint().apply {
                    color = Color.rgb(15, 23, 42)
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                var yPos = pdfCreator.yPos
                lines.forEach { line ->
                    pdfCreator.yPos = yPos
                    pdfCreator.checkNewPage(16f) {}
                    yPos = pdfCreator.yPos
                    pdfCreator.canvas.drawText(line, 36f, yPos, paint)
                    yPos += 16f
                }

                pdfCreator.finish()

                val fileName = "Laporan_${System.currentTimeMillis()}.pdf"
                val tempFile = File(context.cacheDir, fileName)
                pdfDocument.writeTo(FileOutputStream(tempFile))
                pdfDocument.close()

                savePdfToPublicDownloads(context, tempFile, fileName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startServerStatusChecker() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val context = getApplication<Application>()
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val hasInternet = capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                
                if (!hasInternet) {
                    _serverStatus.value = ServerStatus.NO_INTERNET
                } else {
                    try {
                        val res = com.example.network.ApiClient.apiService.checkHealth()
                        if (res.isSuccessful && res.body()?.status == "success") {
                            _serverStatus.value = ServerStatus.ONLINE
                            val sdf = SimpleDateFormat("HH:mm, dd MMM yyyy", Locale("id", "ID"))
                            _lastSyncTime.value = sdf.format(Date())
                            
                            // Fetch dashboard data
                            try {
                                repository.syncFromApi() // This refreshes everything including dashboard
                            } catch (e: Exception) {
                                android.util.Log.d("API_CHECK", "Dashboard Fetch: ${e.message}")
                            }
                            
                        } else {
                            _serverStatus.value = ServerStatus.OFFLINE
                            android.util.Log.d("API_CHECK", "API status: Offline (HTTP ${res.code()})")
                        }
                    } catch (e: Exception) {
                        _serverStatus.value = ServerStatus.OFFLINE
                        android.util.Log.d("API_CHECK", "API check: ${e.message}")
                    }
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    // --- NEW SEPARATED PDF GENERATION CAPABILITIES FOR CICILAN AND RIWAYAT PEMBAYARAN ---

    private fun loadLogoBitmap(context: Context, logoPath: String?): android.graphics.Bitmap? {
        if (logoPath.isNullOrBlank() || logoPath == "null") return null
        return try {
            if (logoPath.startsWith("content://") || logoPath.startsWith("file://")) {
                val uri = Uri.parse(logoPath)
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    android.graphics.BitmapFactory.decodeStream(inputStream)
                }
            } else {
                android.graphics.BitmapFactory.decodeFile(logoPath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateLaporanCicilanFile(context: Context): File {
        val pdfDocument = PdfDocument()
        val settings = communitySettings.value
        val communityName = settings.community_name.ifEmpty { "NEBO SUKABUMI" }
        val logoPath = settings.community_logo
        val logoBitmap = loadLogoBitmap(context, logoPath)

        val filteredAnggota = allAnggota.value.filter { it.hargaBarang > 0.0 }
        val totalPages = estimateTotalPages(filteredAnggota.size, 150f)

        val title = "Laporan Data Cicilan Anggota"
        val pdfCreator = PdfCreatorHelper(
            pdfDocument = pdfDocument,
            title = title,
            communityName = communityName,
            logoBitmap = logoBitmap,
            totalPages = totalPages,
            periodeText = "Kategori: Fasilitas Kredit Internal Komunitas"
        )
        val paint = Paint()

        var rowY = pdfCreator.yPos
        drawTableHeaderCicilan(pdfCreator.canvas, rowY, paint)
        rowY += 22f

        filteredAnggota.forEachIndexed { idx, member ->
            pdfCreator.yPos = rowY
            pdfCreator.checkNewPage(20f) {
                drawTableHeaderCicilan(pdfCreator.canvas, pdfCreator.yPos, paint)
                pdfCreator.yPos += 22f
            }
            rowY = pdfCreator.yPos

            val canvas = pdfCreator.canvas

            // Zebra striping
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)
            }

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)

            val cols = listOf(60f, 165f, 230f, 304f, 340f, 414f, 489f)
            cols.forEach { x ->
                canvas.drawLine(x, rowY, x, rowY + 20f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.style = Paint.Style.FILL

            val yText = rowY + 13.5f
            val statusText = if (member.sisaCicilan <= 0.0) "Lunas" else "Belum Lunas"

            drawCellText(canvas, (idx + 1).toString(), 36f, 24f, yText, paint, Paint.Align.CENTER)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, member.nama, 60f, 105f, yText, paint, Paint.Align.LEFT)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            drawCellText(canvas, "Barang", 165f, 65f, yText, paint, Paint.Align.LEFT)
            drawCellText(canvas, formatRupiah(member.hargaBarang), 230f, 74f, yText, paint, Paint.Align.RIGHT)
            drawCellText(canvas, "${member.lamaCicilan} Bln", 304f, 36f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, formatRupiah(member.cicilanPerBulan), 340f, 74f, yText, paint, Paint.Align.RIGHT)
            drawCellText(canvas, formatRupiah(member.sisaCicilan), 414f, 75f, yText, paint, Paint.Align.RIGHT)

            paint.color = if (member.sisaCicilan <= 0.0) Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, statusText, 489f, 70f, yText, paint, Paint.Align.CENTER)

            rowY += 20f
        }

        pdfCreator.yPos = rowY
        pdfCreator.checkNewPage(145f) {}
        rowY = pdfCreator.yPos

        val totalHargaBarang = filteredAnggota.sumOf { it.hargaBarang }
        val totalSisaCicilan = filteredAnggota.sumOf { it.sisaCicilan }
        val totalLunas = filteredAnggota.count { it.hargaBarang > 0.0 && it.sisaCicilan <= 0.0 }
        val totalBelumLunas = filteredAnggota.count { it.hargaBarang > 0.0 && it.sisaCicilan > 0.0 }

        val summaryY = drawExecutiveCicilanSummary(
            canvas = pdfCreator.canvas,
            startY = rowY + 10f,
            paint = paint,
            totalHarga = totalHargaBarang,
            totalSisa = totalSisaCicilan,
            lunasCount = totalLunas,
            belumLunasCount = totalBelumLunas
        )

        drawDocumentSignatures(pdfCreator.canvas, summaryY + 12f, paint)

        pdfCreator.finish()

        val docsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(docsDir, "Laporan_Data_Cicilan.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    private fun generateRiwayatPembayaranPdfFile(context: Context): File {
        val pdfDocument = PdfDocument()
        val settings = communitySettings.value
        val communityName = settings.community_name.ifEmpty { "NEBO SUKABUMI" }
        val logoPath = settings.community_logo
        val logoBitmap = loadLogoBitmap(context, logoPath)

        val installmentPayments = allPembayaran.value
            .filter { it.jenisPembayaran == "CICILAN" }
            .sortedBy { it.tanggalBayar }
        val totalPages = estimateTotalPages(installmentPayments.size, 150f)

        val title = "Laporan Riwayat Pembayaran Cicilan"
        val pdfCreator = PdfCreatorHelper(
            pdfDocument = pdfDocument,
            title = title,
            communityName = communityName,
            logoBitmap = logoBitmap,
            totalPages = totalPages,
            periodeText = "Kategori: Log Transaksi Masuk Cicilan"
        )
        val paint = Paint()

        var rowY = pdfCreator.yPos
        drawTableHeaderRiwayat(pdfCreator.canvas, rowY, paint)
        rowY += 22f

        installmentPayments.forEachIndexed { idx, payment ->
            pdfCreator.yPos = rowY
            pdfCreator.checkNewPage(20f) {
                drawTableHeaderRiwayat(pdfCreator.canvas, pdfCreator.yPos, paint)
                pdfCreator.yPos += 22f
            }
            rowY = pdfCreator.yPos

            val canvas = pdfCreator.canvas

            // Zebra striping
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)
            }

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)

            val cols = listOf(60f, 175f, 255f, 345f, 435f)
            cols.forEach { x ->
                canvas.drawLine(x, rowY, x, rowY + 20f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.style = Paint.Style.FILL

            val yText = rowY + 13.5f

            val member = allAnggota.value.find { it.id == payment.anggotaId }
            val sisaAfter = if (member != null) {
                val memberPayments = installmentPayments.filter { it.anggotaId == member.id }
                var paidSoFar = 0.0
                for (p in memberPayments) {
                    paidSoFar += p.nominal
                    if (p.id == payment.id) {
                        break
                    }
                }
                maxOf(0.0, member.hargaBarang - paidSoFar)
            } else {
                0.0
            }

            drawCellText(canvas, (idx + 1).toString(), 36f, 24f, yText, paint, Paint.Align.CENTER)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, payment.anggotaNama, 60f, 115f, yText, paint, Paint.Align.LEFT)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            drawCellText(canvas, formatDate(payment.tanggalBayar), 175f, 80f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, formatRupiah(payment.nominal), 255f, 90f, yText, paint, Paint.Align.RIGHT)
            drawCellText(canvas, formatRupiah(sisaAfter), 345f, 90f, yText, paint, Paint.Align.RIGHT)
            drawCellText(canvas, payment.keterangan.ifEmpty { "Pembayaran Cicilan" }, 435f, 124f, yText, paint, Paint.Align.LEFT)

            rowY += 20f
        }

        pdfCreator.yPos = rowY
        pdfCreator.checkNewPage(145f) {}
        rowY = pdfCreator.yPos

        val totalNominal = installmentPayments.sumOf { it.nominal }
        val totalTransaksi = installmentPayments.size
        val tanggalCetak = formatDate(System.currentTimeMillis())

        val summaryY = drawExecutiveRiwayatSummary(
            canvas = pdfCreator.canvas,
            startY = rowY + 10f,
            paint = paint,
            totalNominal = totalNominal,
            totalTransaksi = totalTransaksi,
            tanggalCetak = tanggalCetak
        )

        drawDocumentSignatures(pdfCreator.canvas, summaryY + 12f, paint)

        pdfCreator.finish()

        val docsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(docsDir, "Riwayat_Pembayaran_Cicilan.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    private fun savePdfToPublicDownloads(context: Context, pdfFile: File, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { output ->
                    pdfFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                fileName
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                pdfFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sharePdfFile(context: Context, file: File, chooserTitle: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveLaporanCicilanPdf(context: Context, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanCicilanFile(context)
                val fileName = "Laporan_Data_Cicilan_${System.currentTimeMillis()}.pdf"
                val savedPath = savePdfToPublicDownloads(context, file, fileName)
                onResult(savedPath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun shareLaporanCicilanPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanCicilanFile(context)
                sharePdfFile(context, file, "Bagikan PDF Cicilan")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun printLaporanCicilanPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanCicilanFile(context)
                viewModelScope.launch(Dispatchers.Main) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                    val jobName = "Laporan_Cicilan_${System.currentTimeMillis()}"
                    printManager.print(jobName, object : android.print.PrintDocumentAdapter() {
                        override fun onWrite(
                            pages: Array<out android.print.PageRange>?,
                            destination: android.os.ParcelFileDescriptor?,
                            cancellationSignal: android.os.CancellationSignal?,
                            callback: WriteResultCallback?
                        ) {
                            try {
                                val input = java.io.FileInputStream(file)
                                val output = java.io.FileOutputStream(destination?.fileDescriptor)
                                val buf = ByteArray(1024)
                                var bytesRead: Int
                                while (input.read(buf).also { bytesRead = it } > 0) {
                                    output.write(buf, 0, bytesRead)
                                }
                                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                input.close()
                                output.close()
                            } catch (e: Exception) {
                                callback?.onWriteFailed(e.message)
                            }
                        }

                        override fun onLayout(
                            oldAttributes: android.print.PrintAttributes?,
                            newAttributes: android.print.PrintAttributes?,
                            cancellationSignal: android.os.CancellationSignal?,
                            callback: LayoutResultCallback?,
                            extras: android.os.Bundle?
                        ) {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onLayoutCancelled()
                                return
                            }
                            val info = android.print.PrintDocumentInfo.Builder("Laporan_Cicilan.pdf")
                                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                                .build()
                            callback?.onLayoutFinished(info, true)
                        }
                    }, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateLaporanKasFile(context: Context): File {
        val pdfDocument = PdfDocument()
        val settings = communitySettings.value
        val communityName = settings.community_name.ifEmpty { "NEBO SUKABUMI" }
        val logoPath = settings.community_logo
        val logoBitmap = loadLogoBitmap(context, logoPath)

        val detailKas = detailKasState.value
        val memberIncomes = if (detailKas != null && !detailKas.riwayat.isNullOrEmpty()) {
            detailKas.riwayat.map { 
                PdfTransaction(
                    tanggal = System.currentTimeMillis(),
                    keterangan = "Iuran Kas: ${it.nama} (NRA: ${it.nra ?: "-"})",
                    jenis = "Masuk",
                    nominal = it.nominal
                )
            }
        } else {
            allAnggota.value.filter { it.uangKas > 0 }.map {
                PdfTransaction(
                    tanggal = System.currentTimeMillis(),
                    keterangan = "Iuran Kas: ${it.nama} (NRA: ${it.nra.ifEmpty { "-" }})",
                    jenis = "Masuk",
                    nominal = it.uangKas
                )
            }
        }

        val expenses = allPengeluaran.value
            .filter { 
                it.jenisKas in listOf("Saldo Kas", "Kas", "Kas Utama", "Uang Kas", "") || 
                (!it.jenisKas.equals("Kas Keliling", ignoreCase = true) && 
                 !it.jenisKas.equals("Kas Aniv", ignoreCase = true) && 
                 !it.jenisKas.equals("Kas Anniversary", ignoreCase = true) && 
                 !it.jenisKas.equals("Dana Cicilan", ignoreCase = true) && 
                 !it.jenisKas.equals("Cicilan", ignoreCase = true))
            }
            .map { PdfTransaction(it.tanggal, "Pengeluaran: ${it.keterangan}", "Keluar", it.nominal) }

        val transactions = (memberIncomes + expenses)
        val totalPages = estimateTotalPages(transactions.size, 150f)

        val title = "Laporan Keuangan - Uang Kas"
        val pdfCreator = PdfCreatorHelper(
            pdfDocument = pdfDocument,
            title = title,
            communityName = communityName,
            logoBitmap = logoBitmap,
            totalPages = totalPages,
            periodeText = "Kategori: Kas Utama Komunitas"
        )
        val paint = Paint()

        var rowY = pdfCreator.yPos
        drawTableHeaderKeuangan(pdfCreator.canvas, rowY, paint)
        rowY += 22f

        transactions.forEachIndexed { idx, tx ->
            pdfCreator.yPos = rowY
            pdfCreator.checkNewPage(20f) {
                drawTableHeaderKeuangan(pdfCreator.canvas, pdfCreator.yPos, paint)
                pdfCreator.yPos += 22f
            }
            rowY = pdfCreator.yPos

            val canvas = pdfCreator.canvas

            // Zebra striping
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)
            }

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)

            val cols = listOf(62f, 137f, 369f, 424f)
            cols.forEach { x ->
                canvas.drawLine(x, rowY, x, rowY + 20f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.style = Paint.Style.FILL

            val yText = rowY + 13.5f

            drawCellText(canvas, (idx + 1).toString(), 36f, 26f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, formatDate(tx.tanggal), 62f, 75f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, tx.keterangan, 137f, 232f, yText, paint, Paint.Align.LEFT)

            paint.color = if (tx.jenis == "Masuk") Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, tx.jenis, 369f, 55f, yText, paint, Paint.Align.CENTER)

            paint.color = Color.rgb(15, 23, 42)
            drawCellText(canvas, formatRupiah(tx.nominal), 424f, 135f, yText, paint, Paint.Align.RIGHT)

            rowY += 20f
        }

        pdfCreator.yPos = rowY
        pdfCreator.checkNewPage(145f) {}
        rowY = pdfCreator.yPos

        val totalMasuk = detailKas?.total_pemasukan ?: memberIncomes.sumOf { it.nominal }
        val totalKeluar = detailKas?.total_pengeluaran ?: expenses.sumOf { it.nominal }
        val saldo = detailKas?.saldo ?: (totalMasuk - totalKeluar)

        val summaryY = drawExecutiveFinancialSummary(
            canvas = pdfCreator.canvas,
            startY = rowY + 10f,
            paint = paint,
            masuk = totalMasuk,
            keluar = totalKeluar,
            saldo = saldo
        )

        drawDocumentSignatures(pdfCreator.canvas, summaryY + 12f, paint)

        pdfCreator.finish()

        val docsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(docsDir, "Laporan_Uang_Kas.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    fun saveLaporanKasPdf(context: Context, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanKasFile(context)
                val fileName = "Laporan_Uang_Kas_${System.currentTimeMillis()}.pdf"
                val savedPath = savePdfToPublicDownloads(context, file, fileName)
                onResult(savedPath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    private fun generateLaporanAnivFile(context: Context): File {
        val pdfDocument = PdfDocument()
        val settings = communitySettings.value
        val communityName = settings.community_name.ifEmpty { "NEBO SUKABUMI" }
        val logoPath = settings.community_logo
        val logoBitmap = loadLogoBitmap(context, logoPath)

        val incomes = allPembayaran.value
            .filter { it.jenisPembayaran == "ANIV" }
            .map { PdfTransaction(it.tanggalBayar, "Pemasukan dari ${it.anggotaNama} ${if (it.keterangan.isNotEmpty()) "(${it.keterangan})" else ""}", "Masuk", it.nominal) }

        val expenses = allPengeluaran.value
            .filter { it.jenisKas == "Kas Aniv" }
            .map { PdfTransaction(it.tanggal, "Pengeluaran: ${it.keterangan}", "Keluar", it.nominal) }

        val transactions = (incomes + expenses).sortedBy { it.tanggal }
        val totalPages = estimateTotalPages(transactions.size, 150f)

        val title = "Laporan Keuangan - Kas Anniversary"
        val pdfCreator = PdfCreatorHelper(
            pdfDocument = pdfDocument,
            title = title,
            communityName = communityName,
            logoBitmap = logoBitmap,
            totalPages = totalPages,
            periodeText = "Kategori: Dana Peringatan Anniversary Komunitas"
        )
        val paint = Paint()

        var rowY = pdfCreator.yPos
        drawTableHeaderKeuangan(pdfCreator.canvas, rowY, paint)
        rowY += 22f

        transactions.forEachIndexed { idx, tx ->
            pdfCreator.yPos = rowY
            pdfCreator.checkNewPage(20f) {
                drawTableHeaderKeuangan(pdfCreator.canvas, pdfCreator.yPos, paint)
                pdfCreator.yPos += 22f
            }
            rowY = pdfCreator.yPos

            val canvas = pdfCreator.canvas

            // Zebra striping
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)
            }

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)

            val cols = listOf(62f, 137f, 369f, 424f)
            cols.forEach { x ->
                canvas.drawLine(x, rowY, x, rowY + 20f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.style = Paint.Style.FILL

            val yText = rowY + 13.5f

            drawCellText(canvas, (idx + 1).toString(), 36f, 26f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, formatDate(tx.tanggal), 62f, 75f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, tx.keterangan, 137f, 232f, yText, paint, Paint.Align.LEFT)

            paint.color = if (tx.jenis == "Masuk") Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, tx.jenis, 369f, 55f, yText, paint, Paint.Align.CENTER)

            paint.color = Color.rgb(15, 23, 42)
            drawCellText(canvas, formatRupiah(tx.nominal), 424f, 135f, yText, paint, Paint.Align.RIGHT)

            rowY += 20f
        }

        pdfCreator.yPos = rowY
        pdfCreator.checkNewPage(145f) {}
        rowY = pdfCreator.yPos

        val totalMasuk = incomes.sumOf { it.nominal }
        val totalKeluar = expenses.sumOf { it.nominal }
        val saldo = totalMasuk - totalKeluar

        val summaryY = drawExecutiveFinancialSummary(
            canvas = pdfCreator.canvas,
            startY = rowY + 10f,
            paint = paint,
            masuk = totalMasuk,
            keluar = totalKeluar,
            saldo = saldo
        )

        drawDocumentSignatures(pdfCreator.canvas, summaryY + 12f, paint)

        pdfCreator.finish()

        val docsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(docsDir, "Laporan_Kas_Anniversary.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    fun shareLaporanKasPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanKasFile(context)
                sharePdfFile(context, file, "Bagikan PDF Uang Kas")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateLaporanKelilingFile(context: Context): File {
        val pdfDocument = PdfDocument()
        val settings = communitySettings.value
        val communityName = settings.community_name.ifEmpty { "NEBO SUKABUMI" }
        val logoPath = settings.community_logo
        val logoBitmap = loadLogoBitmap(context, logoPath)

        val transactions = allKasKeliling.value.map {
            PdfTransaction(
                tanggal = it.tanggal,
                keterangan = "Kas Keliling: ${it.keterangan.ifEmpty { "Kopdar / Keliling" }}",
                jenis = if (it.totalPemasukan > 0 || it.jenisTransaksi.equals("Pemasukan", ignoreCase = true)) "Masuk" else "Keluar",
                nominal = if (it.totalPemasukan > 0) it.totalPemasukan else if (it.nominal > 0) it.nominal else it.totalPengeluaran
            )
        }
        val totalPages = estimateTotalPages(transactions.size, 150f)

        val title = "Laporan Keuangan - Kas Keliling"
        val pdfCreator = PdfCreatorHelper(
            pdfDocument = pdfDocument,
            title = title,
            communityName = communityName,
            logoBitmap = logoBitmap,
            totalPages = totalPages,
            periodeText = "Kategori: Kas Keliling & Kegiatan Kopdar"
        )
        val paint = Paint()

        var rowY = pdfCreator.yPos
        drawTableHeaderKeuangan(pdfCreator.canvas, rowY, paint)
        rowY += 22f

        transactions.forEachIndexed { idx, tx ->
            pdfCreator.yPos = rowY
            pdfCreator.checkNewPage(20f) {
                drawTableHeaderKeuangan(pdfCreator.canvas, pdfCreator.yPos, paint)
                pdfCreator.yPos += 22f
            }
            rowY = pdfCreator.yPos

            val canvas = pdfCreator.canvas

            // Zebra striping
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)
            }

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)

            val cols = listOf(62f, 137f, 369f, 424f)
            cols.forEach { x ->
                canvas.drawLine(x, rowY, x, rowY + 20f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.style = Paint.Style.FILL

            val yText = rowY + 13.5f

            drawCellText(canvas, (idx + 1).toString(), 36f, 26f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, formatDate(tx.tanggal), 62f, 75f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, tx.keterangan, 137f, 232f, yText, paint, Paint.Align.LEFT)

            paint.color = if (tx.jenis == "Masuk") Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, tx.jenis, 369f, 55f, yText, paint, Paint.Align.CENTER)

            paint.color = Color.rgb(15, 23, 42)
            drawCellText(canvas, formatRupiah(tx.nominal), 424f, 135f, yText, paint, Paint.Align.RIGHT)

            rowY += 20f
        }

        pdfCreator.yPos = rowY
        pdfCreator.checkNewPage(145f) {}
        rowY = pdfCreator.yPos

        val totalMasuk = transactions.filter { it.jenis == "Masuk" }.sumOf { it.nominal }
        val totalKeluar = transactions.filter { it.jenis == "Keluar" }.sumOf { it.nominal }
        val saldo = totalMasuk - totalKeluar

        val summaryY = drawExecutiveFinancialSummary(
            canvas = pdfCreator.canvas,
            startY = rowY + 10f,
            paint = paint,
            masuk = totalMasuk,
            keluar = totalKeluar,
            saldo = saldo
        )

        drawDocumentSignatures(pdfCreator.canvas, summaryY + 12f, paint)

        pdfCreator.finish()

        val docsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(docsDir, "Laporan_Kas_Keliling.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    fun shareLaporanKelilingPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanKelilingFile(context)
                sharePdfFile(context, file, "Bagikan PDF Kas Keliling")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shareLaporanAnivPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanAnivFile(context)
                sharePdfFile(context, file, "Bagikan PDF Kas Anniversary")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveRiwayatPembayaranPdf(context: Context, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateRiwayatPembayaranPdfFile(context)
                val fileName = "Riwayat_Pembayaran_Cicilan_${System.currentTimeMillis()}.pdf"
                val savedPath = savePdfToPublicDownloads(context, file, fileName)
                onResult(savedPath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun shareRiwayatPembayaranPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateRiwayatPembayaranPdfFile(context)
                sharePdfFile(context, file, "Bagikan PDF Riwayat Pembayaran")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun printRiwayatPembayaranPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateRiwayatPembayaranPdfFile(context)
                viewModelScope.launch(Dispatchers.Main) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                    val jobName = "Riwayat_Pembayaran_Cicilan_${System.currentTimeMillis()}"
                    printManager.print(jobName, object : android.print.PrintDocumentAdapter() {
                        override fun onWrite(
                            pages: Array<out android.print.PageRange>?,
                            destination: android.os.ParcelFileDescriptor?,
                            cancellationSignal: android.os.CancellationSignal?,
                            callback: WriteResultCallback?
                        ) {
                            try {
                                val input = java.io.FileInputStream(file)
                                val output = java.io.FileOutputStream(destination?.fileDescriptor)
                                val buf = ByteArray(1024)
                                var bytesRead: Int
                                while (input.read(buf).also { bytesRead = it } > 0) {
                                    output.write(buf, 0, bytesRead)
                                }
                                callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                input.close()
                                output.close()
                            } catch (e: Exception) {
                                callback?.onWriteFailed(e.message)
                            }
                        }

                        override fun onLayout(
                            oldAttributes: android.print.PrintAttributes?,
                            newAttributes: android.print.PrintAttributes?,
                            cancellationSignal: android.os.CancellationSignal?,
                            callback: LayoutResultCallback?,
                            extras: android.os.Bundle?
                        ) {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onLayoutCancelled()
                                return
                            }
                            val info = android.print.PrintDocumentInfo.Builder("Riwayat_Pembayaran_Cicilan.pdf")
                                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                                .build()
                            callback?.onLayoutFinished(info, true)
                        }
                    }, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            repository.syncFromApi()
        }
    }

    fun prosesGantiPassword(idAnggota: Int, passLama: String, passBaru: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val anggota = repository.getAnggotaById(idAnggota)
                if (anggota == null) {
                    onResult("Anggota tidak ditemukan.")
                    return@launch
                }
                
                if (anggota.password != passLama) {
                    onResult("Password lama salah.")
                    return@launch
                }
                
                val updatedAnggota = anggota.copy(password = passBaru)
                repository.updateAnggota(updatedAnggota)
                logAction("Ganti Password", "Profil", "Sukses mengganti password")
                onResult("Sukses! Password berhasil diperbarui.")
            } catch (e: Exception) {
                onResult("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    private fun generateLaporanBulananPdfFile(context: Context, year: Int, month: Int): File {
        val pdfDocument = PdfDocument()
        val settings = communitySettings.value
        val communityName = settings.community_name.ifEmpty { "NEBO SUKABUMI" }
        val logoPath = settings.community_logo
        val logoBitmap = loadLogoBitmap(context, logoPath)

        val monthName = MonthlyArchiveManager.getMonthName(month)
        val title = "Laporan Bulanan Keuangan"
        val periodeStr = "Periode: $monthName $year"

        val transactions = MonthlyArchiveManager.filterTransactionsForMonth(
            year, month, allPembayaran.value, allPengeluaran.value, allKasKeliling.value
        )
        val totalPages = estimateTotalPages(transactions.size, 160f)

        val pdfCreator = PdfCreatorHelper(
            pdfDocument = pdfDocument,
            title = title,
            communityName = communityName,
            logoBitmap = logoBitmap,
            totalPages = totalPages,
            periodeText = periodeStr
        )
        val paint = Paint()

        var rowY = pdfCreator.yPos

        val saldoAwal = MonthlyArchiveManager.getSaldoAwalForMonth(
            context, year, month, allPembayaran.value, allPengeluaran.value, allKasKeliling.value
        )

        // Saldo Awal Banner
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(241, 245, 249)
        pdfCreator.canvas.drawRoundRect(android.graphics.RectF(36f, rowY, 559f, rowY + 22f), 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 0.5f
        pdfCreator.canvas.drawRoundRect(android.graphics.RectF(36f, rowY, 559f, rowY + 22f), 4f, 4f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.LEFT
        pdfCreator.canvas.drawText("SALDO AWAL PERIODE ($monthName $year)", 46f, rowY + 14.5f, paint)
        paint.textAlign = Paint.Align.RIGHT
        pdfCreator.canvas.drawText(formatRupiah(saldoAwal), 549f, rowY + 14.5f, paint)

        rowY += 30f

        drawTableHeaderKeuangan(pdfCreator.canvas, rowY, paint)
        rowY += 22f

        transactions.forEachIndexed { idx, tx ->
            pdfCreator.yPos = rowY
            pdfCreator.checkNewPage(20f) {
                drawTableHeaderKeuangan(pdfCreator.canvas, pdfCreator.yPos, paint)
                pdfCreator.yPos += 22f
            }
            rowY = pdfCreator.yPos

            val canvas = pdfCreator.canvas

            // Zebra striping
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)
            }

            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(36f, rowY, 559f, rowY + 20f, paint)

            val cols = listOf(62f, 137f, 369f, 424f)
            cols.forEach { x ->
                canvas.drawLine(x, rowY, x, rowY + 20f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.style = Paint.Style.FILL

            val yText = rowY + 13.5f

            drawCellText(canvas, (idx + 1).toString(), 36f, 26f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, tx.tanggalStr, 62f, 75f, yText, paint, Paint.Align.CENTER)
            drawCellText(canvas, tx.namaAtauKeterangan, 137f, 232f, yText, paint, Paint.Align.LEFT)

            paint.color = if (tx.tipe == "PEMASUKAN") Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, if (tx.tipe == "PEMASUKAN") "Masuk" else "Keluar", 369f, 55f, yText, paint, Paint.Align.CENTER)

            paint.color = Color.rgb(15, 23, 42)
            drawCellText(canvas, formatRupiah(tx.nominal), 424f, 135f, yText, paint, Paint.Align.RIGHT)

            rowY += 20f
        }

        pdfCreator.yPos = rowY
        pdfCreator.checkNewPage(145f) {}
        rowY = pdfCreator.yPos

        val totalMasuk = transactions.filter { it.tipe == "PEMASUKAN" }.sumOf { it.nominal }
        val totalKeluar = transactions.filter { it.tipe == "PENGELUARAN" }.sumOf { it.nominal }
        val saldoAkhir = saldoAwal + totalMasuk - totalKeluar

        val summaryY = drawExecutiveFinancialSummary(
            canvas = pdfCreator.canvas,
            startY = rowY + 10f,
            paint = paint,
            masuk = totalMasuk,
            keluar = totalKeluar,
            saldo = saldoAkhir
        )

        drawDocumentSignatures(pdfCreator.canvas, summaryY + 12f, paint)

        pdfCreator.finish()

        val docsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(docsDir, "Laporan_Bulanan_${year}_${month}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    fun shareLaporanBulananPdf(context: Context, year: Int, month: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanBulananPdfFile(context, year, month)
                sharePdfFile(context, file, "Bagikan PDF Laporan Bulanan")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveLaporanBulananPdf(context: Context, year: Int, month: Int, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = generateLaporanBulananPdfFile(context, year, month)
                val fileName = "Laporan_Bulanan_${year}_${month}_${System.currentTimeMillis()}.pdf"
                val savedPath = savePdfToPublicDownloads(context, file, fileName)
                onResult(savedPath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun restoreRepositoryData(
        anggotaList: List<com.example.data.Anggota>,
        pembayaranList: List<com.example.data.Pembayaran>,
        pengeluaranList: List<com.example.data.Pengeluaran>,
        kasKelilingList: List<com.example.data.KasKeliling>
    ) {
        repository.setAllAnggota(anggotaList)
        repository.setAllPembayaran(pembayaranList)
        repository.setAllPengeluaran(pengeluaranList)
        repository.setAllKasKeliling(kasKelilingList)
    }

    fun setAllAnggota(list: List<com.example.data.Anggota>) {
        repository.setAllAnggota(list)
    }

    fun submitResetRequest(nra: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = com.example.network.ApiClient.apiService.submitResetRequest(req = mapOf("nra" to nra))
                if (response.isSuccessful && response.body()?.status == "success") {
                    onResult(true, response.body()?.message ?: "Permintaan reset password berhasil dikirim.")
                    fetchResetRequests()
                } else {
                    onResult(false, response.body()?.message ?: "Gagal mengirim permintaan.")
                }
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun fetchResetRequests() {
        viewModelScope.launch {
            try {
                val response = com.example.network.ApiClient.apiService.getResetRequests()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _resetRequests.value = response.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun approveResetRequest(id: Int, onResult: (Boolean, String, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = com.example.network.ApiClient.apiService.approveResetRequest(req = mapOf("id" to id))
                if (response.isSuccessful && response.body()?.status == "success") {
                    val passwordSementara = response.body()?.data?.get("password_sementara") ?: ""
                    onResult(true, response.body()?.message ?: "Permintaan berhasil disetujui", passwordSementara)
                    fetchResetRequests()
                    repository.syncFromApi()
                } else {
                    onResult(false, response.body()?.message ?: "Gagal menyetujui permintaan", "")
                }
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.message}", "")
            }
        }
    }

    fun rejectResetRequest(id: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = com.example.network.ApiClient.apiService.rejectResetRequest(req = mapOf("id" to id))
                if (response.isSuccessful && response.body()?.status == "success") {
                    onResult(true, response.body()?.message ?: "Permintaan berhasil ditolak")
                    fetchResetRequests()
                } else {
                    onResult(false, response.body()?.message ?: "Gagal menolak permintaan")
                }
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun completeResetRequest(passwordBaru: String, onResult: (Boolean, String) -> Unit) {
        val username = _pendingResetUsername.value
        val requestId = _pendingRequestId.value
        if (username.isBlank()) {
            onResult(false, "Username tidak valid.")
            return
        }
        viewModelScope.launch {
            try {
                val response = com.example.network.ApiClient.apiService.completeResetRequest(
                    req = mapOf(
                        "nra" to username,
                        "password_baru" to passwordBaru,
                        "request_id" to requestId.toString()
                    )
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    _requireNewPassword.value = false
                    _pendingRequestId.value = 0
                    _pendingResetUsername.value = ""
                    onResult(true, response.body()?.message ?: "Password baru berhasil disimpan.")
                    repository.syncFromApi()
                } else {
                    onResult(false, response.body()?.message ?: "Gagal menyimpan password baru.")
                }
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan: ${e.message}")
            }
        }
    }
}

// --- HELPER CLASSES AND FUNCTIONS FOR PROFESSIONAL PDF GENERATION ---

class PdfCreatorHelper(
    private val pdfDocument: PdfDocument,
    private val title: String,
    private val communityName: String,
    private val logoBitmap: android.graphics.Bitmap?,
    var totalPages: Int = 1,
    private val periodeText: String? = null
) {
    var currentPageIndex = 1
    var pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageIndex).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas: Canvas = page.canvas
    var yPos = 36f

    init {
        yPos = drawPage1Header()
    }

    private fun drawFooter(targetCanvas: Canvas, pageNum: Int, total: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        val footerLineY = 808f
        paint.color = Color.rgb(226, 232, 240) // Slate 200
        paint.strokeWidth = 0.75f
        paint.style = Paint.Style.STROKE
        targetCanvas.drawLine(36f, footerLineY, 559f, footerLineY, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 7f
        paint.color = Color.rgb(148, 163, 184) // Slate 400
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        targetCanvas.drawText("Sistem Manajemen Komunitas NEBO Sukabumi • Dokumen Laporan Resmi", 36f, footerLineY + 13f, paint)

        val pageDisplay = if (total > 0) "Halaman $pageNum dari $total" else "Halaman $pageNum"
        paint.color = Color.rgb(71, 85, 105) // Slate 600
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        targetCanvas.drawText(pageDisplay, 559f, footerLineY + 13f, paint)
    }

    private fun drawPage1Header(): Float {
        val paint = Paint().apply { isAntiAlias = true }
        val startY = 36f
        val logoSize = 44f
        val logoX = 36f

        // 1. Draw Logo
        if (logoBitmap != null) {
            val src = android.graphics.Rect(0, 0, logoBitmap.width, logoBitmap.height)
            val dst = android.graphics.RectF(logoX, startY, logoX + logoSize, startY + logoSize)
            paint.isFilterBitmap = true
            canvas.drawBitmap(logoBitmap, src, dst, paint)
        } else {
            // Modern branded emblem badge
            paint.color = Color.rgb(15, 23, 42) // Slate 900
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(android.graphics.RectF(logoX, startY, logoX + logoSize, startY + logoSize), 8f, 8f, paint)

            paint.color = Color.rgb(59, 130, 246) // Blue 500 accent ring
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(android.graphics.RectF(logoX + 2f, startY + 2f, logoX + logoSize - 2f, startY + logoSize - 2f), 7f, 7f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            val yOffset = (paint.descent() + paint.ascent()) / 2
            canvas.drawText("N", logoX + logoSize / 2, startY + logoSize / 2 - yOffset, paint)
        }

        // 2. Organization Name & Identity
        val textStartX = logoX + logoSize + 10f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(communityName.uppercase(), textStartX, startY + 13f, paint)

        paint.color = Color.rgb(71, 85, 105) // Slate 600
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KOMUNITAS PENGENDARA & SOSIAL SUKABUMI", textStartX, startY + 25f, paint)

        paint.color = Color.rgb(100, 116, 139) // Slate 500
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Sekretariat: Kota Sukabumi, Jawa Barat • Sistem Laporan Keuangan", textStartX, startY + 37f, paint)

        // 3. Right-aligned Metadata Box
        val dateSdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val timeSdf = SimpleDateFormat("HH:mm 'WIB'", Locale("id", "ID"))
        val now = Date()

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Tanggal: ${dateSdf.format(now)}", 559f, startY + 13f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("Waktu: ${timeSdf.format(now)}", 559f, startY + 25f, paint)

        paint.color = Color.rgb(22, 163, 74) // Green 600
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STATUS: TERVERIFIKASI RESMI", 559f, startY + 37f, paint)

        // 4. Double decorative divider line
        val line1Y = startY + logoSize + 6f
        paint.color = Color.rgb(15, 23, 42) // Primary Slate 900
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(36f, line1Y, 559f, line1Y, paint)

        paint.color = Color.rgb(203, 213, 225) // Accent Slate 300
        paint.strokeWidth = 0.5f
        canvas.drawLine(36f, line1Y + 2.5f, 559f, line1Y + 2.5f, paint)

        // 5. Title Banner
        val titleBoxY = line1Y + 8f
        val titleBoxHeight = if (periodeText != null) 32f else 26f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(248, 250, 252) // Slate 50
        canvas.drawRoundRect(android.graphics.RectF(36f, titleBoxY, 559f, titleBoxY + titleBoxHeight), 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240) // Slate 200
        paint.strokeWidth = 0.75f
        canvas.drawRoundRect(android.graphics.RectF(36f, titleBoxY, 559f, titleBoxY + titleBoxHeight), 4f, 4f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val titleTextY = if (periodeText != null) titleBoxY + 15f else titleBoxY + 17f
        canvas.drawText(title.uppercase(), 595f / 2f, titleTextY, paint)

        if (periodeText != null) {
            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(periodeText, 595f / 2f, titleBoxY + 26f, paint)
        }

        return titleBoxY + titleBoxHeight + 10f
    }

    private fun drawContinuationHeader(): Float {
        val paint = Paint().apply { isAntiAlias = true }
        val startY = 36f

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("${communityName.uppercase()} — $title (Lanjutan)", 36f, startY + 10f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Halaman $currentPageIndex dari $totalPages", 559f, startY + 10f, paint)

        val lineY = startY + 16f
        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 0.75f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(36f, lineY, 559f, lineY, paint)

        return lineY + 8f
    }

    fun checkNewPage(neededHeight: Float, drawHeaderAction: () -> Unit) {
        if (yPos + neededHeight > 785f) {
            drawFooter(canvas, currentPageIndex, totalPages)
            pdfDocument.finishPage(page)
            currentPageIndex++
            if (currentPageIndex > totalPages) {
                totalPages = currentPageIndex
            }
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageIndex).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = 36f
            yPos = drawContinuationHeader()
            drawHeaderAction()
        }
    }

    fun finish() {
        if (currentPageIndex > totalPages) {
            totalPages = currentPageIndex
        }
        drawFooter(canvas, currentPageIndex, totalPages)
        pdfDocument.finishPage(page)
    }
}

private fun estimateTotalPages(itemCount: Int, summaryAndSigHeight: Float = 150f): Int {
    val rowHeight = 20f
    val page1Space = 633f
    val totalHeightNeeded = (itemCount * rowHeight) + summaryAndSigHeight
    if (totalHeightNeeded <= page1Space) return 1

    val pageSubsequentSpace = 718f
    val itemsOnPage1 = (page1Space / rowHeight).toInt()
    var remainingItems = itemCount - itemsOnPage1
    var pages = 1
    while (remainingItems > 0) {
        pages++
        val itemsThisPage = (pageSubsequentSpace / rowHeight).toInt()
        remainingItems -= itemsThisPage
        if (remainingItems <= 0) {
            val spaceUsed = maxOf(0, remainingItems + itemsThisPage) * rowHeight
            if (spaceUsed + summaryAndSigHeight > pageSubsequentSpace) {
                pages++
            }
            break
        }
    }
    return maxOf(1, pages)
}

private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var truncated = text
    while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
        truncated = truncated.substring(0, truncated.length - 1)
    }
    return if (truncated.isEmpty()) "..." else "$truncated..."
}

private fun drawCellText(
    canvas: Canvas,
    text: String,
    x: Float,
    width: Float,
    y: Float,
    paint: Paint,
    align: Paint.Align
) {
    paint.textAlign = align
    val padding = 6f
    val textX = when (align) {
        Paint.Align.LEFT -> x + padding
        Paint.Align.RIGHT -> x + width - padding
        Paint.Align.CENTER -> x + width / 2f
    }
    val truncated = truncateText(text, paint, width - (padding * 2))
    canvas.drawText(truncated, textX, y, paint)
}

private fun drawTableHeaderCicilan(canvas: Canvas, y: Float, paint: Paint) {
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249) // Slate 100
    canvas.drawRect(36f, y, 559f, y + 22f, paint)

    paint.color = Color.rgb(203, 213, 225) // Slate 300
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(36f, y, 559f, y + 22f, paint)

    val cols = listOf(60f, 165f, 230f, 304f, 340f, 414f, 489f)
    cols.forEach { x ->
        canvas.drawLine(x, y, x, y + 22f, paint)
    }

    paint.color = Color.rgb(15, 23, 42)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 8f
    paint.style = Paint.Style.FILL

    val yText = y + 14.5f
    drawCellText(canvas, "No", 36f, 24f, yText, paint, Paint.Align.CENTER)
    drawCellText(canvas, "Nama Anggota", 60f, 105f, yText, paint, Paint.Align.LEFT)
    drawCellText(canvas, "Barang", 165f, 65f, yText, paint, Paint.Align.LEFT)
    drawCellText(canvas, "Harga Barang", 230f, 74f, yText, paint, Paint.Align.RIGHT)
    drawCellText(canvas, "Tenor", 304f, 36f, yText, paint, Paint.Align.CENTER)
    drawCellText(canvas, "Cicilan/Bln", 340f, 74f, yText, paint, Paint.Align.RIGHT)
    drawCellText(canvas, "Sisa Cicilan", 414f, 75f, yText, paint, Paint.Align.RIGHT)
    drawCellText(canvas, "Status", 489f, 70f, yText, paint, Paint.Align.CENTER)
}

private fun drawTableHeaderRiwayat(canvas: Canvas, y: Float, paint: Paint) {
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRect(36f, y, 559f, y + 22f, paint)

    paint.color = Color.rgb(203, 213, 225)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(36f, y, 559f, y + 22f, paint)

    val cols = listOf(60f, 175f, 255f, 345f, 435f)
    cols.forEach { x ->
        canvas.drawLine(x, y, x, y + 22f, paint)
    }

    paint.color = Color.rgb(15, 23, 42)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 8f
    paint.style = Paint.Style.FILL

    val yText = y + 14.5f
    drawCellText(canvas, "No", 36f, 24f, yText, paint, Paint.Align.CENTER)
    drawCellText(canvas, "Nama Anggota", 60f, 115f, yText, paint, Paint.Align.LEFT)
    drawCellText(canvas, "Tanggal Bayar", 175f, 80f, yText, paint, Paint.Align.CENTER)
    drawCellText(canvas, "Nominal Bayar", 255f, 90f, yText, paint, Paint.Align.RIGHT)
    drawCellText(canvas, "Sisa Cicilan", 345f, 90f, yText, paint, Paint.Align.RIGHT)
    drawCellText(canvas, "Keterangan", 435f, 124f, yText, paint, Paint.Align.LEFT)
}

private fun drawTableHeaderKeuangan(canvas: Canvas, y: Float, paint: Paint) {
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRect(36f, y, 559f, y + 22f, paint)

    paint.color = Color.rgb(203, 213, 225)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRect(36f, y, 559f, y + 22f, paint)

    val cols = listOf(62f, 137f, 369f, 424f)
    cols.forEach { x ->
        canvas.drawLine(x, y, x, y + 22f, paint)
    }

    paint.color = Color.rgb(15, 23, 42)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 8f
    paint.style = Paint.Style.FILL

    val yText = y + 14.5f
    drawCellText(canvas, "No", 36f, 26f, yText, paint, Paint.Align.CENTER)
    drawCellText(canvas, "Tanggal", 62f, 75f, yText, paint, Paint.Align.CENTER)
    drawCellText(canvas, "Keterangan Transaksi", 137f, 232f, yText, paint, Paint.Align.LEFT)
    drawCellText(canvas, "Jenis", 369f, 55f, yText, paint, Paint.Align.CENTER)
    drawCellText(canvas, "Nominal", 424f, 135f, yText, paint, Paint.Align.RIGHT)
}

private fun drawExecutiveFinancialSummary(
    canvas: Canvas,
    startY: Float,
    paint: Paint,
    masuk: Double,
    keluar: Double,
    saldo: Double
): Float {
    val cardHeight = 60f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(248, 250, 252)
    canvas.drawRoundRect(android.graphics.RectF(36f, startY, 559f, startY + cardHeight), 4f, 4f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(203, 213, 225)
    paint.strokeWidth = 0.75f
    canvas.drawRoundRect(android.graphics.RectF(36f, startY, 559f, startY + cardHeight), 4f, 4f, paint)

    // Header bar inside card
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRect(36f, startY, 559f, startY + 18f, paint)

    paint.color = Color.rgb(15, 23, 42)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 7.5f
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("IKHTISAR KEUANGAN RESMI", 46f, startY + 12.5f, paint)

    paint.color = Color.rgb(22, 163, 74)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("STATUS: VALID & DIVERIFIKASI", 549f, startY + 12.5f, paint)

    // 3 KPI Boxes
    val boxY = startY + 22f
    val boxHeight = 33f
    val boxW = 166f

    // Box 1: Pemasukan
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(240, 253, 244)
    canvas.drawRoundRect(android.graphics.RectF(42f, boxY, 42f + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(187, 247, 208)
    paint.strokeWidth = 0.5f
    canvas.drawRoundRect(android.graphics.RectF(42f, boxY, 42f + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(21, 128, 61)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("TOTAL PEMASUKAN", 48f, boxY + 11f, paint)
    paint.textSize = 9.5f
    paint.color = Color.rgb(22, 101, 52)
    canvas.drawText("+ ${formatRupiah(masuk)}", 48f, boxY + 25f, paint)

    // Box 2: Pengeluaran
    val box2X = 42f + boxW + 7f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(254, 242, 242)
    canvas.drawRoundRect(android.graphics.RectF(box2X, boxY, box2X + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(254, 202, 202)
    canvas.drawRoundRect(android.graphics.RectF(box2X, boxY, box2X + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(185, 28, 28)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("TOTAL PENGELUARAN", box2X + 6f, boxY + 11f, paint)
    paint.textSize = 9.5f
    paint.color = Color.rgb(153, 27, 27)
    canvas.drawText("- ${formatRupiah(keluar)}", box2X + 6f, boxY + 25f, paint)

    // Box 3: Saldo Akhir
    val box3X = box2X + boxW + 7f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(240, 249, 255)
    canvas.drawRoundRect(android.graphics.RectF(box3X, boxY, box3X + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(186, 230, 253)
    canvas.drawRoundRect(android.graphics.RectF(box3X, boxY, box3X + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(3, 105, 161)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("SALDO AKHIR", box3X + 6f, boxY + 11f, paint)
    paint.textSize = 10f
    paint.color = Color.rgb(7, 89, 133)
    canvas.drawText(formatRupiah(saldo), box3X + 6f, boxY + 25f, paint)

    return startY + cardHeight
}

private fun drawExecutiveCicilanSummary(
    canvas: Canvas,
    startY: Float,
    paint: Paint,
    totalHarga: Double,
    totalSisa: Double,
    lunasCount: Int,
    belumLunasCount: Int
): Float {
    val cardHeight = 60f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(248, 250, 252)
    canvas.drawRoundRect(android.graphics.RectF(36f, startY, 559f, startY + cardHeight), 4f, 4f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(203, 213, 225)
    paint.strokeWidth = 0.75f
    canvas.drawRoundRect(android.graphics.RectF(36f, startY, 559f, startY + cardHeight), 4f, 4f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRect(36f, startY, 559f, startY + 18f, paint)

    paint.color = Color.rgb(15, 23, 42)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 7.5f
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("RINGKASAN FASILITAS CICILAN", 46f, startY + 12.5f, paint)

    paint.color = Color.rgb(59, 130, 246)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("STATUS: INTERNAL KOMUNITAS", 549f, startY + 12.5f, paint)

    val boxY = startY + 22f
    val boxHeight = 33f
    val boxW = 166f

    // Box 1: Total Harga Barang
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRoundRect(android.graphics.RectF(42f, boxY, 42f + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(203, 213, 225)
    paint.strokeWidth = 0.5f
    canvas.drawRoundRect(android.graphics.RectF(42f, boxY, 42f + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(71, 85, 105)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("TOTAL HARGA BARANG", 48f, boxY + 11f, paint)
    paint.textSize = 9.5f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText(formatRupiah(totalHarga), 48f, boxY + 25f, paint)

    // Box 2: Total Sisa Cicilan
    val box2X = 42f + boxW + 7f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(254, 242, 242)
    canvas.drawRoundRect(android.graphics.RectF(box2X, boxY, box2X + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(254, 202, 202)
    canvas.drawRoundRect(android.graphics.RectF(box2X, boxY, box2X + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(185, 28, 28)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("TOTAL SISA CICILAN", box2X + 6f, boxY + 11f, paint)
    paint.textSize = 9.5f
    paint.color = Color.rgb(153, 27, 27)
    canvas.drawText(formatRupiah(totalSisa), box2X + 6f, boxY + 25f, paint)

    // Box 3: Status Pelunasan
    val box3X = box2X + boxW + 7f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(240, 253, 244)
    canvas.drawRoundRect(android.graphics.RectF(box3X, boxY, box3X + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(187, 247, 208)
    canvas.drawRoundRect(android.graphics.RectF(box3X, boxY, box3X + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(21, 128, 61)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("STATUS ANGGOTA", box3X + 6f, boxY + 11f, paint)
    paint.textSize = 9f
    paint.color = Color.rgb(22, 101, 52)
    canvas.drawText("$lunasCount Lunas • $belumLunasCount Berjalan", box3X + 6f, boxY + 25f, paint)

    return startY + cardHeight
}

private fun drawExecutiveRiwayatSummary(
    canvas: Canvas,
    startY: Float,
    paint: Paint,
    totalNominal: Double,
    totalTransaksi: Int,
    tanggalCetak: String
): Float {
    val cardHeight = 60f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(248, 250, 252)
    canvas.drawRoundRect(android.graphics.RectF(36f, startY, 559f, startY + cardHeight), 4f, 4f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(203, 213, 225)
    paint.strokeWidth = 0.75f
    canvas.drawRoundRect(android.graphics.RectF(36f, startY, 559f, startY + cardHeight), 4f, 4f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRect(36f, startY, 559f, startY + 18f, paint)

    paint.color = Color.rgb(15, 23, 42)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 7.5f
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("RINGKASAN RIWAYAT ANGSURAN", 46f, startY + 12.5f, paint)

    paint.color = Color.rgb(22, 163, 74)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("LOG PEMBAYARAN VALID", 549f, startY + 12.5f, paint)

    val boxY = startY + 22f
    val boxHeight = 33f
    val boxW = 166f

    // Box 1: Total Pembayaran Masuk
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(240, 253, 244)
    canvas.drawRoundRect(android.graphics.RectF(42f, boxY, 42f + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(187, 247, 208)
    paint.strokeWidth = 0.5f
    canvas.drawRoundRect(android.graphics.RectF(42f, boxY, 42f + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(21, 128, 61)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("TOTAL ANGSURAN MASUK", 48f, boxY + 11f, paint)
    paint.textSize = 9.5f
    paint.color = Color.rgb(22, 101, 52)
    canvas.drawText(formatRupiah(totalNominal), 48f, boxY + 25f, paint)

    // Box 2: Total Transaksi
    val box2X = 42f + boxW + 7f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(241, 245, 249)
    canvas.drawRoundRect(android.graphics.RectF(box2X, boxY, box2X + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(203, 213, 225)
    canvas.drawRoundRect(android.graphics.RectF(box2X, boxY, box2X + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(71, 85, 105)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("JUMLAH TRANSAKSI", box2X + 6f, boxY + 11f, paint)
    paint.textSize = 9.5f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("$totalTransaksi Pembayaran", box2X + 6f, boxY + 25f, paint)

    // Box 3: Tanggal Audit
    val box3X = box2X + boxW + 7f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(240, 249, 255)
    canvas.drawRoundRect(android.graphics.RectF(box3X, boxY, box3X + boxW, boxY + boxHeight), 3f, 3f, paint)
    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(186, 230, 253)
    canvas.drawRoundRect(android.graphics.RectF(box3X, boxY, box3X + boxW, boxY + boxHeight), 3f, 3f, paint)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(3, 105, 161)
    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("TANGGAL CETAK", box3X + 6f, boxY + 11f, paint)
    paint.textSize = 8.5f
    paint.color = Color.rgb(7, 89, 133)
    canvas.drawText(tanggalCetak, box3X + 6f, boxY + 25f, paint)

    return startY + cardHeight
}

private fun drawDocumentSignatures(canvas: Canvas, startY: Float, paint: Paint): Float {
    val dateFull = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(71, 85, 105)
    paint.textSize = 7.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Kota Sukabumi, $dateFull", 559f, startY + 10f, paint)

    val titleSigY = startY + 22f
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 8f
    paint.color = Color.rgb(15, 23, 42)
    canvas.drawText("Ketua Komunitas", 120f, titleSigY, paint)
    canvas.drawText("Bendahara Komunitas", 475f, titleSigY, paint)

    val lineSigY = titleSigY + 36f
    paint.color = Color.rgb(203, 213, 225)
    paint.strokeWidth = 0.75f
    paint.style = Paint.Style.STROKE
    canvas.drawLine(70f, lineSigY, 170f, lineSigY, paint)
    canvas.drawLine(425f, lineSigY, 525f, lineSigY, paint)

    val nameSigY = lineSigY + 11f
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(100, 116, 139)
    paint.textSize = 7.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("( Ketua NEBO )", 120f, nameSigY, paint)
    canvas.drawText("( Bendahara NEBO )", 475f, nameSigY, paint)

    return nameSigY + 10f
}

data class PdfTransaction(
    val tanggal: Long,
    val keterangan: String,
    val jenis: String,
    val nominal: Double
)
