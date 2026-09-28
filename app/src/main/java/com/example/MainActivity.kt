package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: CommunityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Auto-refresh session status verifikasi akun & role terbaru dari cPanel saat startup
        fetchDashboardData()
        autoRefreshSession()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF3B82F6),
                    surface = Color(0xFF0F172A),
                    background = Color(0xFF020617)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "main") {
                        composable("splash") {
                            SplashScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("login") {
                            LoginScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("main") {
                            DashboardScreen(navController = navController, viewModel = viewModel)
                        }

                        composable("reset_password_requests") {
                            ResetPasswordRequestsScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("customization") {
                            CustomizationScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("identitas_komunitas") {
                            IdentitasKomunitasScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("developer_panel") {
                            DeveloperPanelScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("catatan_bebas") {
                            CatatanScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("daftar_hadir") {
                            DaftarHadirScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("pengeluaran_kas") {
                            PengeluaranScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("laporan") {
                            LaporanScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("kas_keliling") {
                            KasKelilingScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_belum_aniv") {
                            DetailBelumAnivScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_belum_kas") {
                            DetailBelumKasScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_total_pengeluaran") {
                            DetailTotalPengeluaranScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_uang_kas") {
                            DetailUangKasScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_iuran_aniv") {
                            DetailIuranAnivScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("daftar_cicilan_anggota") {
                            DaftarCicilanAnggotaScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_sisa_cicilan") {
                            DetailSisaCicilanScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_saldo_kas") {
                            DetailSaldoKasScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("detail_anggota") {
                            DetailAnggotaScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("audit_log") {
                            AuditLogScreen(navController = navController)
                        }
                        composable("login_log") {
                            LoginLogScreen(navController = navController)
                        }
                        composable("anggota_list") {
                            AnggotaListScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("uang_kas") {
                            UangKasScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("anniversary_summary") {
                            AnniversarySummaryScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("cicilan_summary") {
                            CicilanSummaryScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("sisa_cicilan_only") {
                            SisaCicilanOnlyScreen(navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "anggota_detail?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            AnggotaDetailScreen(navController = navController, viewModel = viewModel, memberId = id)
                        }
                        composable(
                            route = "anggota_form?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            AnggotaFormScreen(navController = navController, viewModel = viewModel, memberId = id)
                        }
                        composable(
                            route = "kas_keliling_form?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            KasKelilingFormScreen(navController = navController, viewModel = viewModel, kasKelilingId = id)
                        }
                        composable(
                            route = "pembayaran_form?anggotaId={anggotaId}&jenis={jenis}",
                            arguments = listOf(
                                navArgument("anggotaId") { type = NavType.IntType; defaultValue = 0 },
                                navArgument("jenis") { type = NavType.StringType; defaultValue = "KAS" }
                            )
                        ) { backStackEntry ->
                            val anggotaId = backStackEntry.arguments?.getInt("anggotaId") ?: 0
                            val jenis = backStackEntry.arguments?.getString("jenis") ?: "KAS"
                            PembayaranFormScreen(navController = navController, viewModel = viewModel, anggotaId = anggotaId, jenisPembayaran = jenis)
                        }
                        composable(
                            route = "pengeluaran_form?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: -1
                            PengeluaranFormScreen(navController = navController, viewModel = viewModel, pengeluaranId = id)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchDashboardData()
        autoRefreshSession()
    }

    /**
     * Formatting Rupiah secara presisi dan terstandarisasi.
     */
    fun formatRupiah(amount: Double?): String {
        if (amount == null || amount.isNaN()) return "Rp 0"
        return try {
            val localeID = Locale("id", "ID")
            val formatter = NumberFormat.getCurrencyInstance(localeID).apply {
                maximumFractionDigits = 0
            }
            formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
        } catch (e: Exception) {
            "Rp " + String.format(Locale("id", "ID"), "%,.0f", amount)
        }
    }

    /**
     * Memanggil API backend CPanel https://nebosukabumi.net/api/get_anggota.php
     * Parsing data JSON (Nama, NRA, Status, Role) dengan Null Safety Check.
     */
    fun fetchDaftarAnggota(onComplete: ((List<com.example.data.Anggota>) -> Unit)? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://nebosukabumi.net/api/get_anggota.php")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonObject = JSONObject(response.toString())
                    val jsonArray = if (jsonObject.has("data")) {
                        jsonObject.getJSONArray("data")
                    } else {
                        JSONArray()
                    }

                    val memberList = mutableListOf<com.example.data.Anggota>()
                    val currentNra = SessionManager.getUserNra(this@MainActivity).trim()
                    val currentUserId = SessionManager.getUserId(this@MainActivity)
                    val currentUserName = SessionManager.getUserName(this@MainActivity).trim()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optInt("id", 0)
                        val nama = obj.optString("nama", "Anggota")
                        val nra = obj.optString("nra", "-")
                        val role = obj.optString("role", "MEMBER").trim()
                        val statusStr = obj.optString("status", "").trim()
                        val statusVerifStr = obj.optString("status_verifikasi", "").trim()
                        val uangKas = obj.optDouble("kas", obj.optDouble("uang_kas", obj.optDouble("uangKas", obj.optDouble("total_kas", 0.0))))
                        val iuranAniv = obj.optDouble("aniv", obj.optDouble("iuran_aniv", obj.optDouble("iuranAniv", obj.optDouble("total_aniv", 0.0))))
                        val hargaBarang = obj.optDouble("harga_barang", obj.optDouble("hargaBarang", 0.0))
                        val totalCicilan = obj.optDouble("total_cicilan", obj.optDouble("totalCicilan", 0.0))
                        val sisaCicilan = obj.optDouble("sisa_cicilan", obj.optDouble("sisaCicilan", 0.0))
                        val cicilanPerBulan = obj.optDouble("cicilan_per_bulan", obj.optDouble("cicilanPerBulan", 0.0))
                        val lamaCicilan = obj.optInt("lamaCicilan", obj.optInt("lama_cicilan", 0))
                        val totalTagihan = obj.optDouble("totalTagihan", obj.optDouble("total_tagihan", hargaBarang))
                        
                        // 1. Parsing status verifikasi (status_verifikasi / status == "1" atau VERIFIED) dan role
                        val roleUpper = role.uppercase()
                        val isItemVerified = statusVerifStr == "1" ||
                                statusStr == "1" ||
                                statusStr.equals("VERIFIED", ignoreCase = true) ||
                                statusStr.equals("Aktif", ignoreCase = true) ||
                                obj.optInt("status_verifikasi", -1) == 1 ||
                                obj.optInt("status", -1) == 1 ||
                                obj.optBoolean("is_verified", false) ||
                                roleUpper in listOf("ADMIN", "BENDAHARA", "PENGURUS", "DEVELOPER")

                        val statusVerifikasiStr = if (isItemVerified) "1" else "0"
                        val statusAktif = if (isItemVerified) 1 else 0

                        // Update SessionManager jika item ini adalah user yang sedang login
                        val isCurrentUser = (currentUserId != -1 && currentUserId == id) ||
                                (currentNra.isNotEmpty() && (currentNra.equals(nra.trim(), ignoreCase = true) || currentNra.toIntOrNull() == nra.trim().toIntOrNull())) ||
                                (currentUserName.isNotEmpty() && currentUserName.equals(nama.trim(), ignoreCase = true))

                        if (isCurrentUser) {
                            val latestRole = if (roleUpper.isNotBlank()) roleUpper else "MEMBER"
                            SessionManager.updateRoleAndVerification(
                                context = this@MainActivity,
                                role = latestRole,
                                isVerified = isItemVerified,
                                statusVerifikasi = statusVerifikasiStr
                            )
                            withContext(Dispatchers.Main) {
                                viewModel.setLoggedInUserRole(latestRole)
                                viewModel.setUserVerified(isItemVerified)
                            }
                        }

                        memberList.add(
                            com.example.data.Anggota(
                                id = id,
                                nama = nama,
                                nra = nra,
                                role = role,
                                statusAktif = statusAktif,
                                uangKas = uangKas,
                                iuranAniv = iuranAniv,
                                hargaBarang = hargaBarang,
                                totalCicilan = totalCicilan,
                                sisaCicilan = sisaCicilan,
                                cicilanPerBulan = cicilanPerBulan,
                                lamaCicilan = lamaCicilan,
                                totalTagihan = totalTagihan
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        viewModel.setAllAnggota(memberList)
                        onComplete?.invoke(memberList)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Gagal terhubung ke server CPanel (HTTP $responseCode)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal terhubung ke server CPanel", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Memanggil API backend cPanel get_dashboard.php / dashboard.php
     * untuk membaca status verifikasi (status_verifikasi / status == "1" atau "VERIFIED")
     * serta role user terbaru dan memperbarui SessionManager.
     */
    fun fetchDashboardUserSession(onComplete: ((isVerified: Boolean, role: String) -> Unit)? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentRole = SessionManager.getRole(this@MainActivity).ifBlank { "MEMBER" }
                val currentNra = SessionManager.getUserNra(this@MainActivity).ifBlank { "0001" }
                val urlString = "https://nebosukabumi.net/api/get_dashboard.php?role=" +
                        java.net.URLEncoder.encode(currentRole, "UTF-8") +
                        "&nra=" + java.net.URLEncoder.encode(currentNra, "UTF-8")

                var jsonString = ""
                try {
                    val url = URL(urlString)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("Accept", "application/json")
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                        reader.close()
                        jsonString = sb.toString().trim()
                    }
                } catch (e: Exception) {
                    // Fallback ke dashboard.php jika routing get_dashboard.php diarahkan ke dashboard.php
                    try {
                        val fallbackUrl = URL("https://nebosukabumi.net/api/dashboard.php?role=" +
                                java.net.URLEncoder.encode(currentRole, "UTF-8") +
                                "&nra=" + java.net.URLEncoder.encode(currentNra, "UTF-8"))
                        val conn = (fallbackUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 8000
                            readTimeout = 8000
                            setRequestProperty("Accept", "application/json")
                        }
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val reader = BufferedReader(InputStreamReader(conn.inputStream))
                            val sb = StringBuilder()
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                sb.append(line)
                            }
                            reader.close()
                            jsonString = sb.toString().trim()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                if (jsonString.startsWith("{")) {
                    val root = JSONObject(jsonString)
                    val userObj = root.optJSONObject("user")
                        ?: root.optJSONObject("data")?.optJSONObject("user")
                        ?: root.optJSONObject("data")?.optJSONObject("profile")
                        ?: root.optJSONObject("profile")
                        ?: root.optJSONObject("data")
                        ?: root

                    val statusVerifStr = when {
                        userObj.has("status_verifikasi") -> userObj.optString("status_verifikasi")
                        root.has("status_verifikasi") -> root.optString("status_verifikasi")
                        else -> ""
                    }.trim()

                    val statusStr = when {
                        userObj.has("status") -> userObj.optString("status")
                        root.has("status") -> root.optString("status")
                        else -> ""
                    }.trim()

                    val roleStr = when {
                        userObj.has("role") -> userObj.optString("role")
                        root.has("role") -> root.optString("role")
                        else -> currentRole
                    }.trim().uppercase()

                    // 1. Parsing status verifikasi: status_verifikasi == "1" ATAU status == "1" / "VERIFIED" / "Aktif"
                    val isVerified = statusVerifStr == "1" ||
                            statusStr == "1" ||
                            statusStr.equals("VERIFIED", ignoreCase = true) ||
                            statusStr.equals("Aktif", ignoreCase = true) ||
                            userObj.optInt("status_verifikasi", -1) == 1 ||
                            userObj.optInt("status", -1) == 1 ||
                            userObj.optBoolean("is_verified", false) ||
                            roleStr in listOf("ADMIN", "BENDAHARA", "PENGURUS", "DEVELOPER")

                    val statusVerifikasiStr = if (isVerified) "1" else "0"
                    val finalRole = if (roleStr.isNotBlank()) roleStr else currentRole.uppercase()

                    // Simpan status dan role terbaru tersebut ke SessionManager / SharedPreferences
                    SessionManager.updateRoleAndVerification(
                        context = this@MainActivity,
                        role = finalRole,
                        isVerified = isVerified,
                        statusVerifikasi = statusVerifikasiStr
                    )

                    withContext(Dispatchers.Main) {
                        viewModel.setLoggedInUserRole(finalRole)
                        viewModel.setUserVerified(isVerified)
                        onComplete?.invoke(isVerified, finalRole)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Memanggil API https://nebosukabumi.net/api/get_dashboard.php
     * Parsing JSON Object secara aman menggunakan optDouble dan optInt:
     * - total_anniversary -> Total Terkumpul (Format Rupiah)
     * - target_per_anggota -> Target / Anggota (Format Rupiah)
     * - anggota_belum_bayar -> Belum Bayar (Contoh: "X Anggota")
     * - saldo_kas -> Card Kas Keliling (Format Rupiah)
     * - total_anggota -> Banner Total Anggota
     */
    fun fetchDashboardData(onComplete: ((com.example.network.DashboardData) -> Unit)? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentRole = SessionManager.getRole(this@MainActivity).ifBlank { "MEMBER" }
                val currentNra = SessionManager.getUserNra(this@MainActivity).ifBlank { "0001" }
                val urlString = "https://nebosukabumi.net/api/get_dashboard.php?role=" +
                        java.net.URLEncoder.encode(currentRole, "UTF-8") +
                        "&nra=" + java.net.URLEncoder.encode(currentNra, "UTF-8")

                var jsonStr = ""
                try {
                    val url = URL(urlString)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("Accept", "application/json")
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                        reader.close()
                        jsonStr = sb.toString().trim()
                    }
                } catch (e: Exception) {
                    // Fallback URL jika ada perbedaan path
                    try {
                        val fallbackUrl = URL("https://nebosukabumi.net/api/dashboard.php?role=" +
                                java.net.URLEncoder.encode(currentRole, "UTF-8") +
                                "&nra=" + java.net.URLEncoder.encode(currentNra, "UTF-8"))
                        val conn = (fallbackUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 8000
                            readTimeout = 8000
                            setRequestProperty("Accept", "application/json")
                        }
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val reader = BufferedReader(InputStreamReader(conn.inputStream))
                            val sb = StringBuilder()
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                sb.append(line)
                            }
                            reader.close()
                            jsonStr = sb.toString().trim()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                if (jsonStr.startsWith("{")) {
                    val root = JSONObject(jsonStr)
                    val dataObj = if (root.has("data") && !root.isNull("data")) {
                        root.optJSONObject("data") ?: root
                    } else {
                        root
                    }

                    // Safe parsing dengan optDouble dan optInt
                    val totalAnggota = dataObj.optInt("total_anggota", 0)
                    val totalKas = dataObj.optDouble("total_kas", 0.0)
                    val totalAnniversary = dataObj.optDouble(
                        "total_anniversary",
                        dataObj.optDouble(
                            "total_aniv",
                            dataObj.optDouble(
                                "iuran_anniversary",
                                dataObj.optDouble("iuran_aniv", 0.0)
                            )
                        )
                    )
                    val targetPerAnggota = dataObj.optDouble("target_per_anggota", dataObj.optDouble("target_aniv", 0.0))
                    val anggotaBelumBayar = dataObj.optInt(
                        "anggota_belum_bayar",
                        dataObj.optInt(
                            "belum_anniversary",
                            dataObj.optInt("belum_bayar_aniv", 0)
                        )
                    )
                    val saldoKas = dataObj.optDouble("saldo_kas", dataObj.optDouble("kas_keliling", dataObj.optDouble("total_saldo", 0.0)))
                    val belumKas = dataObj.optInt("belum_kas", dataObj.optInt("belum_bayar_kas", 0))
                    val totalPengeluaran = dataObj.optDouble("total_pengeluaran", 0.0)
                    val totalSisaCicilan = dataObj.optDouble("total_sisa_cicilan", 0.0)
                    val totalHargaBarang = dataObj.optDouble("total_harga_barang", 0.0)
                    val totalSudahDibayar = dataObj.optDouble("total_sudah_dibayar", 0.0)
                    val anggotaMencicil = dataObj.optInt("anggota_mencicil", 0)

                    val parsedData = com.example.network.DashboardData(
                        total_anggota = totalAnggota,
                        total_kas = totalKas,
                        total_anniversary = totalAnniversary,
                        total_aniv = totalAnniversary,
                        iuran_anniversary = totalAnniversary,
                        iuran_aniv = totalAnniversary,
                        target_per_anggota = targetPerAnggota,
                        target_aniv = targetPerAnggota,
                        anggota_belum_bayar = anggotaBelumBayar,
                        saldo_kas = saldoKas,
                        kas_keliling = saldoKas,
                        belum_kas = belumKas,
                        belum_bayar_kas = belumKas,
                        belum_anniversary = anggotaBelumBayar,
                        belum_bayar_aniv = anggotaBelumBayar,
                        totalPengeluaran = totalPengeluaran,
                        total_sisa_cicilan = totalSisaCicilan,
                        total_harga_barang = totalHargaBarang,
                        total_sudah_dibayar = totalSudahDibayar,
                        anggota_mencicil = anggotaMencicil
                    )

                    withContext(Dispatchers.Main) {
                        viewModel.setDashboardData(parsedData)
                        onComplete?.invoke(parsedData)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Auto-refresh session saat tombol dialog OK ditekan atau saat pengguna melakukan Swipe Refresh pada Dashboard.
     * Mengambil status verifikasi dan role terbaru dari API cPanel (get_dashboard.php dan get_anggota.php).
     */
    fun autoRefreshSession(onComplete: (() -> Unit)? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Refresh dashboard data dari get_dashboard.php
                fetchDashboardData()
                // 2. Refresh session dari get_dashboard.php
                fetchDashboardUserSession()
                // 3. Refresh session & daftar anggota dari get_anggota.php
                fetchDaftarAnggota()
                // 4. Sinkronisasi data ke ViewModel
                viewModel.syncFromApiSuspend()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    /**
     * Pengecekan Akses (Akses Terbatas):
     * - Hanya batasi akses jika userRole == "GUEST" ATAU statusVerifikasi == "0".
     * - Jika userRole berisi "ADMIN", "BENDAHARA", "PENGURUS", atau "MEMBER" (dengan statusVerifikasi != "0"),
     *   izinkan akun membuka menu Laporan Kas, Cicilan, dan Daftar Anggota secara penuh.
     */
    fun hasAccessToFeatures(): Boolean {
        val userRole = SessionManager.getRole(this).trim().uppercase()
        val statusVerifikasi = SessionManager.getStatusVerifikasi(this).trim()

        // Role ADMIN, BENDAHARA, PENGURUS, DEVELOPER selalu memiliki akses penuh
        if (userRole in listOf("ADMIN", "BENDAHARA", "PENGURUS", "DEVELOPER")) {
            return true
        }

        // Hanya tampilkan dialog "Akses Terbatas" jika userRole == "GUEST" ATAU statusVerifikasi == "0"
        if (userRole == "GUEST" || statusVerifikasi == "0") {
            return false
        }

        // Jika userRole berisi "MEMBER" atau "ANGGOTA" dan statusVerifikasi != "0", izinkan akses penuh
        if (userRole in listOf("MEMBER", "ANGGOTA") && statusVerifikasi != "0") {
            return true
        }

        return SessionManager.isVerified(this)
    }

    /**
     * Menampilkan dialog "Akses Terbatas" jika akun belum diverifikasi.
     * Mengandung tombol "OK" yang memicu auto-refresh session ke backend cPanel.
     */
    fun showAksesTerbatasDialog(onOkPressed: (() -> Unit)? = null) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Akses Terbatas")
            .setMessage("Akun Anda belum diverifikasi oleh pengurus. Silakan hubungi pengurus atau tunggu hingga akun Anda aktif.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                autoRefreshSession {
                    onOkPressed?.invoke()
                }
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Helper untuk memeriksa hak akses sebelum membuka menu (Laporan Kas, Cicilan, Anggota).
     */
    fun checkAksesMenu(onAllowed: () -> Unit) {
        if (hasAccessToFeatures()) {
            onAllowed()
        } else {
            showAksesTerbatasDialog()
        }
    }

    /**
     * Sinkronisasi Real-Time seluruh data CPanel saat tombol Refresh ditekan.
     */
    fun syncAllDataRealtime(onFinished: (() -> Unit)? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                fetchDashboardData()
                viewModel.syncFromApiSuspend()
                fetchDashboardUserSession()
                fetchDaftarAnggota()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Data CPanel Berhasil Diperbarui", Toast.LENGTH_SHORT).show()
                    onFinished?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal terhubung ke server CPanel", Toast.LENGTH_SHORT).show()
                    onFinished?.invoke()
                }
            }
        }
    }
}
