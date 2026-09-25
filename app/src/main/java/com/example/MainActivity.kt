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

        val activeRole = SessionManager.getRole(this).let { if (it.isBlank()) "BENDAHARA" else it }.uppercase()
        val activeNra = SessionManager.getUserNra(this).let { if (it.isBlank()) "0001" else it }
        if (activeRole in listOf("BENDAHARA", "ADMIN", "DEVELOPER")) {
            fetchRekapitulasiCicilanBarang(role = activeRole, nra = activeNra)
        }

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
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optInt("id", 0)
                        val nama = obj.optString("nama", "Anggota")
                        val nra = obj.optString("nra", "-")
                        val role = obj.optString("role", "MEMBER")
                        val statusStr = obj.optString("status", "VERIFIED")
                        val uangKas = obj.optDouble("uang_kas", 0.0)
                        val iuranAniv = obj.optDouble("iuran_aniv", 0.0)
                        val sisaCicilan = obj.optDouble("sisa_cicilan", 0.0)
                        val statusAktif = if (statusStr.equals("VERIFIED", ignoreCase = true) || statusStr.equals("Aktif", ignoreCase = true)) 1 else 0

                        memberList.add(
                            com.example.data.Anggota(
                                id = id,
                                nama = nama,
                                nra = nra,
                                role = role,
                                statusAktif = statusAktif,
                                uangKas = uangKas,
                                iuranAniv = iuranAniv,
                                sisaCicilan = sisaCicilan
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
     * Memanggil endpoint API get_daftar_cicilan_aktif.php?role=BENDAHARA&nra=0001
     * Khusus Role BENDAHARA, ADMIN, dan DEVELOPER untuk data Akumulasi CPanel.
     * Mengambil:
     * - total_harga_barang
     * - anggota_mencicil
     * - total_sisa_cicilan
     * Format Rupiah Indonesia dan Null Safety check ("Rp 0" & "0 Anggota").
     */
    fun fetchRekapitulasiCicilanBarang(
        role: String = "BENDAHARA",
        nra: String = "0001",
        tvTotalHargaBarang: android.widget.TextView? = null,
        tvAnggotaMencicil: android.widget.TextView? = null,
        tvTotalSisaCicilan: android.widget.TextView? = null,
        onResult: ((totalHargaBarang: Long, anggotaMencicil: Int, totalSisaCicilan: Long) -> Unit)? = null
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Panggil endpoint API get_daftar_cicilan_aktif.php?role=BENDAHARA&nra=0001
                val urlStr = "https://nebosukabumi.net/api/get_daftar_cicilan_aktif.php?role=" +
                        java.net.URLEncoder.encode(role, "UTF-8") +
                        "&nra=" + java.net.URLEncoder.encode(nra, "UTF-8")
                val url = URL(urlStr)
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

                    val jsonString = response.toString().trim()
                    var totalHargaBarang = 0L
                    var anggotaMencicil = 0
                    var totalSisaCicilan = 0L

                    // 2. Ambil nilai response JSON & 4. Null Safety check
                    if (jsonString.startsWith("{")) {
                        val jsonObject = JSONObject(jsonString)
                        val dataObj = jsonObject.optJSONObject("data")

                        if (dataObj != null) {
                            totalHargaBarang = dataObj.optLong("total_harga_barang", 0L)
                            anggotaMencicil = dataObj.optInt("anggota_mencicil", 0)
                            totalSisaCicilan = dataObj.optLong("total_sisa_cicilan", 0L)
                        } else {
                            totalHargaBarang = jsonObject.optLong("total_harga_barang", 0L)
                            anggotaMencicil = jsonObject.optInt("anggota_mencicil", 0)
                            totalSisaCicilan = jsonObject.optLong("total_sisa_cicilan", 0L)
                        }

                        // Fallback parsing jika 'data' berupa JSON Array list anggota cicilan
                        val dataArray = jsonObject.optJSONArray("data")
                        if (dataArray != null && totalHargaBarang == 0L && totalSisaCicilan == 0L && dataArray.length() > 0) {
                            var sumHarga = 0.0
                            var sumSisa = 0.0
                            var activeCount = 0
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.optJSONObject(i) ?: continue
                                val harga = item.optDouble("harga_barang", 0.0)
                                val sisa = item.optDouble("sisa_cicilan", 0.0)
                                sumHarga += harga
                                sumSisa += sisa
                                if (sisa > 0.0) activeCount++
                            }
                            totalHargaBarang = sumHarga.toLong()
                            totalSisaCicilan = sumSisa.toLong()
                            anggotaMencicil = activeCount
                        }
                    } else if (jsonString.startsWith("[")) {
                        val jsonArray = JSONArray(jsonString)
                        var sumHarga = 0.0
                        var sumSisa = 0.0
                        var activeCount = 0
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.optJSONObject(i) ?: continue
                            val harga = item.optDouble("harga_barang", 0.0)
                            val sisa = item.optDouble("sisa_cicilan", 0.0)
                            sumHarga += harga
                            sumSisa += sisa
                            if (sisa > 0.0) activeCount++
                        }
                        totalHargaBarang = sumHarga.toLong()
                        totalSisaCicilan = sumSisa.toLong()
                        anggotaMencicil = activeCount
                    }

                    // 3. Format angka mata uang ke Rupiah Indonesia dengan Null Safety:
                    // - tvTotalHargaBarang: "Rp " + String.format("%,d", total_harga_barang)
                    // - tvAnggotaMencicil: anggota_mencicil + " Anggota"
                    // - tvTotalSisaCicilan: "Rp " + String.format("%,d", total_sisa_cicilan)
                    val formattedHargaBarang = if (totalHargaBarang > 0) {
                        "Rp " + String.format(Locale("id", "ID"), "%,d", totalHargaBarang)
                    } else {
                        "Rp 0"
                    }

                    val formattedAnggotaMencicil = if (anggotaMencicil > 0) {
                        "$anggotaMencicil Anggota"
                    } else {
                        "0 Anggota"
                    }

                    val formattedSisaCicilan = if (totalSisaCicilan > 0) {
                        "Rp " + String.format(Locale("id", "ID"), "%,d", totalSisaCicilan)
                    } else {
                        "Rp 0"
                    }

                    withContext(Dispatchers.Main) {
                        tvTotalHargaBarang?.text = formattedHargaBarang
                        tvAnggotaMencicil?.text = formattedAnggotaMencicil
                        tvTotalSisaCicilan?.text = formattedSisaCicilan

                        // Sinkronisasi data ke ViewModel untuk Compose UI (DashboardScreen)
                        viewModel.updateRekapitulasiCicilan(
                            totalHarga = totalHargaBarang.toDouble(),
                            anggotaMencicil = anggotaMencicil,
                            totalSisa = totalSisaCicilan.toDouble()
                        )

                        onResult?.invoke(totalHargaBarang, anggotaMencicil, totalSisaCicilan)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvTotalHargaBarang?.text = "Rp 0"
                        tvAnggotaMencicil?.text = "0 Anggota"
                        tvTotalSisaCicilan?.text = "Rp 0"
                        onResult?.invoke(0L, 0, 0L)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvTotalHargaBarang?.text = "Rp 0"
                    tvAnggotaMencicil?.text = "0 Anggota"
                    tvTotalSisaCicilan?.text = "Rp 0"
                    onResult?.invoke(0L, 0, 0L)
                }
            }
        }
    }

    /**
     * Sinkronisasi Real-Time seluruh data CPanel saat tombol Refresh ditekan.
     */
    fun syncAllDataRealtime(onFinished: (() -> Unit)? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                viewModel.syncFromApiSuspend()
                fetchDaftarAnggota()

                // Cek role aktif user
                val currentRole = SessionManager.getRole(this@MainActivity).let { if (it.isBlank()) "BENDAHARA" else it }.uppercase()
                val currentNra = SessionManager.getUserNra(this@MainActivity).let { if (it.isBlank()) "0001" else it }

                // Jika Role adalah BENDAHARA, ADMIN, atau DEVELOPER, ambil data akumulasi cicilan
                val requestRole = if (currentRole in listOf("BENDAHARA", "ADMIN", "DEVELOPER")) currentRole else "BENDAHARA"
                fetchRekapitulasiCicilanBarang(role = requestRole, nra = currentNra)

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
