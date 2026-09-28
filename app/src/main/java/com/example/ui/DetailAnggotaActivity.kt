package com.example.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.R
import org.json.JSONObject
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

class DetailAnggotaActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvNamaAnggota: TextView
    private lateinit var tvNraAnggota: TextView
    private lateinit var tvStatusAnggota: TextView

    private lateinit var cardDataKas: CardView
    private lateinit var tvTotalKas: TextView
    private lateinit var tvIuranAnniversary: TextView

    private lateinit var cardDataCicilan: CardView
    private lateinit var tvHargaBarang: TextView
    private lateinit var tvTotalDibayar: TextView
    private lateinit var tvSisaCicilan: TextView

    private lateinit var progressBar: ProgressBar

    private var targetNra: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_anggota)

        initViews()
        setupToolbar()
        extractIntentData()
        loadDetailAnggota()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvNamaAnggota = findViewById(R.id.tvNamaAnggota)
        tvNraAnggota = findViewById(R.id.tvNraAnggota)
        tvStatusAnggota = findViewById(R.id.tvStatusAnggota)

        cardDataKas = findViewById(R.id.cardDataKas)
        tvTotalKas = findViewById(R.id.tvTotalKas)
        tvIuranAnniversary = findViewById(R.id.tvIuranAnniversary)

        cardDataCicilan = findViewById(R.id.cardDataCicilan)
        tvHargaBarang = findViewById(R.id.tvHargaBarang)
        tvTotalDibayar = findViewById(R.id.tvTotalDibayar)
        tvSisaCicilan = findViewById(R.id.tvSisaCicilan)

        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun extractIntentData() {
        // Ambil NRA dari intent, jika tidak ada ambil dari user yang sedang login di SessionManager
        val nraIntent = intent?.getStringExtra("nra")
            ?: intent?.getStringExtra("NRA")
            ?: ""
        targetNra = if (nraIntent.isNotBlank()) {
            nraIntent.trim()
        } else {
            SessionManager.getUserNra(this).ifBlank {
                SessionManager.getNra(this).ifBlank { "0001" }
            }.trim()
        }

        val namaIntent = intent?.getStringExtra("nama")
            ?: intent?.getStringExtra("NAMA")
            ?: ""
        if (namaIntent.isNotBlank()) {
            tvNamaAnggota.text = "Nama: $namaIntent"
        }
        tvNraAnggota.text = "NRA: $targetNra"
    }

    /**
     * Memanggil API: https://nebosukabumi.net/api/get_detail_anggota.php?nra={NRA_TARGET}&role_login={USER_ROLE}&nra_login={USER_NRA}
     * Logika Hak Akses:
     * - Role "ADMIN", "BENDAHARA", "DEVELOPER" -> Full Akses (cardDataCicilan SELALU VISIBLE)
     * - Pemilik akun sendiri (nra_login == nra_target) -> VISIBLE
     * - can_see_cicilan == true dari response -> VISIBLE
     * - Member biasa membuka profil orang lain -> GONE
     */
    fun loadDetailAnggota() {
        if (targetNra.isBlank()) {
            Toast.makeText(this, "NRA Anggota tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Ambil Role dan NRA dari akun yang sedang login di SessionManager
        val loggedInRole = SessionManager.getRole(this).trim().uppercase(Locale.ROOT).ifBlank { "MEMBER" }
        val loggedInNra = SessionManager.getUserNra(this).trim()

        val encodedNra = try { URLEncoder.encode(targetNra, "UTF-8") } catch (e: Exception) { targetNra }
        val encodedRole = try { URLEncoder.encode(loggedInRole, "UTF-8") } catch (e: Exception) { loggedInRole }
        val encodedNraLogin = try { URLEncoder.encode(loggedInNra, "UTF-8") } catch (e: Exception) { loggedInNra }

        val url = "https://nebosukabumi.net/api/get_detail_anggota.php?nra=$encodedNra&role_login=$encodedRole&nra_login=$encodedNraLogin"

        val isPrivileged = loggedInRole in listOf("ADMIN", "BENDAHARA", "DEVELOPER", "PENGURUS")
        val isSelf = loggedInNra.isNotBlank() && loggedInNra.equals(targetNra, ignoreCase = true)

        val request = StringRequest(
            Request.Method.GET,
            url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val rootJson = JSONObject(response)
                    val dataObj = rootJson.optJSONObject("data") ?: rootJson

                    // Parsing Response JSON Object `data` dengan .optDouble()
                    val totalKas = dataObj.optDouble("total_kas", dataObj.optDouble("uang_kas", 0.0))
                    val totalAniv = dataObj.optDouble("total_aniv", dataObj.optDouble("iuran_aniv", 0.0))
                    val hargaBarang = dataObj.optDouble("harga_barang", 0.0)
                    val sisaCicilan = dataObj.optDouble("sisa_cicilan", 0.0)
                    var totalDibayar = dataObj.optDouble("total_dibayar", dataObj.optDouble("total_cicilan", 0.0))
                    if (totalDibayar <= 0.0 && hargaBarang > 0.0 && sisaCicilan < hargaBarang) {
                        totalDibayar = maxOf(0.0, hargaBarang - sisaCicilan)
                    }

                    // Ambil hak akses boolean `can_see_cicilan` dari response JSON
                    val canSeeCicilanFromApi = dataObj.optBoolean("can_see_cicilan", false)
                    val allowViewCicilan = isPrivileged || isSelf || canSeeCicilanFromApi

                    // Render Nominal Kas ke UI Format Rupiah
                    tvTotalKas.text = formatRupiah(totalKas)
                    tvIuranAnniversary.text = formatRupiah(totalAniv)

                    // Logika Tampilan Visibility (cardDataCicilan)
                    if (allowViewCicilan) {
                        cardDataCicilan.visibility = View.VISIBLE
                        tvHargaBarang.text = formatRupiah(hargaBarang)
                        tvTotalDibayar.text = formatRupiah(totalDibayar)
                        tvSisaCicilan.text = formatRupiah(sisaCicilan)
                    } else {
                        // Jika Member biasa membuka profil anggota lain: Sembunyikan untuk privasi
                        cardDataCicilan.visibility = View.GONE
                    }

                    // Sinkronisasi data identitas diri jika disediakan API
                    val nama = dataObj.optString("nama", "")
                    val nra = dataObj.optString("nra", "")
                    val status = dataObj.optString("status", "")
                    if (nama.isNotBlank()) {
                        tvNamaAnggota.text = "Nama: $nama"
                    }
                    if (nra.isNotBlank()) {
                        tvNraAnggota.text = "NRA: $nra"
                    }
                    if (status.isNotBlank()) {
                        val statusText = if (status == "1" || status.equals("VERIFIED", ignoreCase = true) || status.equals("Aktif", ignoreCase = true)) {
                            "Status: VERIFIED"
                        } else {
                            "Status: $status"
                        }
                        tvStatusAnggota.text = statusText
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Format data server tidak sesuai", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                error.printStackTrace()
                Toast.makeText(this, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Format angka ke format Rupiah Indonesia (contoh: "Rp 660.000", "Rp 0")
     */
    private fun formatRupiah(nominal: Double): String {
        val localeID = Locale("in", "ID")
        val formatter = NumberFormat.getNumberInstance(localeID)
        return "Rp " + formatter.format(nominal.toLong())
    }

    /**
     * Helper method publik untuk mengatur visibilitas Card data cicilan
     * sesuai aturan RBAC dan Privasi Data (jika dibutuhkan pemanggilan eksternal)
     */
    fun setupCicilanPrivacy(
        cardDataCicilan: View?,
        cardRiwayatCicilan: View?,
        loggedInNra: String,
        targetNra: String,
        userRole: String
    ) {
        val roleUpper = userRole.trim().uppercase(Locale.ROOT)
        if (loggedInNra == targetNra || roleUpper in listOf("ADMIN", "BENDAHARA", "PENGURUS", "DEVELOPER")) {
            cardDataCicilan?.visibility = View.VISIBLE
            cardRiwayatCicilan?.visibility = View.VISIBLE
        } else {
            cardDataCicilan?.visibility = View.GONE
            cardRiwayatCicilan?.visibility = View.GONE
        }
    }
}
