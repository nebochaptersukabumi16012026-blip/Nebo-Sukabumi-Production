package com.example.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.R
import com.example.data.CicilanAnggota
import com.example.data.CicilanResponse
import com.google.gson.Gson
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

class DaftarCicilanActivity : AppCompatActivity() {

    private lateinit var tvSubtitleAnggota: TextView
    private lateinit var tvTotalSisaCicilan: TextView
    private lateinit var tvTotalHargaBarang: TextView
    private lateinit var tvTotalDibayar: TextView
    private lateinit var etSearch: EditText
    private lateinit var rvCicilan: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnRefresh: ImageButton

    private lateinit var adapter: CicilanAdapter
    private val masterList = mutableListOf<CicilanAnggota>()
    private val filteredList = mutableListOf<CicilanAnggota>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar_cicilan)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadCicilanData()
    }

    private fun initViews() {
        tvSubtitleAnggota = findViewById(R.id.tvSubtitleAnggota)
        tvTotalSisaCicilan = findViewById(R.id.tvTotalSisaCicilan)
        tvTotalHargaBarang = findViewById(R.id.tvTotalHargaBarang)
        tvTotalDibayar = findViewById(R.id.tvTotalDibayar)
        etSearch = findViewById(R.id.etSearch)
        rvCicilan = findViewById(R.id.rvCicilan)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnRefresh = findViewById(R.id.btnRefresh)
    }

    private fun setupRecyclerView() {
        adapter = CicilanAdapter(filteredList) { anggota ->
            Toast.makeText(this, "Anggota: ${anggota.nama} (NRA: ${anggota.nra})", Toast.LENGTH_SHORT).show()
        }
        rvCicilan.layoutManager = LinearLayoutManager(this)
        rvCicilan.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnRefresh.setOnClickListener { loadCicilanData() }

        // TextWatcher pencarian real-time Nama & NRA
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterData(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Memanggil API cPanel get_daftar_cicilan_aktif.php menggunakan Volley & Gson
     * dengan query parameter role dan nra dari user yang sedang login.
     */
    fun loadData() {
        // 1. Ambil data user yang sedang login dari SessionManager
        val rawRole = SessionManager.getRole(this).ifBlank { "MEMBER" }
        val userRole = rawRole.uppercase(Locale.ROOT)
        val userNra = SessionManager.getUserNra(this).ifBlank { SessionManager.getNra(this).ifBlank { "0001" } }

        // 2. Format URL API dengan query parameter
        val encodedRole = URLEncoder.encode(userRole, "UTF-8")
        val encodedNra = URLEncoder.encode(userNra, "UTF-8")
        val url = "https://nebosukabumi.net/api/get_daftar_cicilan_aktif.php?role=$encodedRole&nra=$encodedNra"

        progressBar.visibility = View.VISIBLE

        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET,
            url,
            { responseString ->
                progressBar.visibility = View.GONE
                try {
                    val gson = Gson()
                    val cicilanResponse = gson.fromJson(responseString, CicilanResponse::class.java)

                    updateUI(cicilanResponse)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Gagal memproses data cicilan: ${e.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                error.printStackTrace()
                Toast.makeText(this, "Gagal terhubung ke server: ${error.message}", Toast.LENGTH_SHORT).show()
                showEmptyState(true)
            }
        )

        queue.add(stringRequest)
    }

    private fun loadCicilanData() = loadData()

    /**
     * Mengupdate UI dengan data respons CPanel
     */
    private fun updateUI(response: CicilanResponse?) {
        if (response == null) {
            showEmptyState(true)
            return
        }

        // 1. Ekstraksi Data Rekapitulasi dengan Null Safety
        val totalSisa = response.total_sisa_cicilan
        val totalHarga = response.total_harga_barang
        val totalDibayar = response.total_dibayar

        // 2. Set Nilai Rekapitulasi ke TextView
        tvTotalSisaCicilan.text = formatRupiah(totalSisa)
        tvTotalHargaBarang.text = "Barang: " + formatRupiah(totalHarga)
        tvTotalDibayar.text = "Dibayar: " + formatRupiah(totalDibayar)

        masterList.clear()
        val listAnggota = response.data

        if (!listAnggota.isNullOrEmpty()) {
            masterList.addAll(listAnggota)
            val count = if (response.total_anggota_mencicil > 0) response.total_anggota_mencicil else listAnggota.size
            tvSubtitleAnggota.text = "Anggota Memiliki Cicilan ($count)"

            showEmptyState(false)
            filterData(etSearch.text.toString())
        } else {
            val count = response.total_anggota_mencicil
            tvSubtitleAnggota.text = "Anggota Memiliki Cicilan ($count)"
            showEmptyState(true)
        }
    }

    /**
     * Filter daftar cicilan anggota berdasarkan Nama atau NRA
     */
    private fun filterData(query: String) {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        filteredList.clear()

        if (trimmed.isEmpty()) {
            filteredList.addAll(masterList)
        } else {
            for (item in masterList) {
                val nama = item.nama?.lowercase(Locale.ROOT) ?: ""
                val nra = item.nra?.lowercase(Locale.ROOT) ?: ""
                if (nama.contains(trimmed) || nra.contains(trimmed)) {
                    filteredList.add(item)
                }
            }
        }

        adapter.notifyDataSetChanged()
        showEmptyState(filteredList.isEmpty())
    }

    /**
     * Mengatur visibilitas RecyclerView dan Empty State
     */
    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            rvCicilan.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvCicilan.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

    /**
     * Helper pemformatan mata uang Rupiah tanpa desimal (Locale in, ID)
     */
    private fun formatRupiah(amount: Double): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                maximumFractionDigits = 0
            }
            formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
        } catch (e: Exception) {
            "Rp " + String.format(Locale("in", "ID"), "%,.0f", amount)
        }
    }
}
