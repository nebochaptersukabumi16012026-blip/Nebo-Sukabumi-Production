package com.example.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.R
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity untuk menampilkan Detail Iuran & Kas Anniversary
 * Mengambil data dari endpoint: https://nebosukabumi.net/api/get_detail_anniversary.php
 */
class DetailIuranAnniversaryActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var btnRefresh: ImageButton
    private lateinit var tvTotalPemasukan: TextView
    private lateinit var tvTotalPengeluaran: TextView
    private lateinit var tvSisaKas: TextView
    private lateinit var tvHeaderList: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var rvIuranAnniversary: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val adapter = IuranAnniversaryAdapter()
    private val apiUrl = "https://nebosukabumi.net/api/get_detail_anniversary.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_iuran_anniversary)

        initViews()
        setupToolbar()
        setupRecyclerView()
        loadDataAnniversary()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnRefresh = findViewById(R.id.btnRefresh)
        tvTotalPemasukan = findViewById(R.id.tvTotalPemasukan)
        tvTotalPengeluaran = findViewById(R.id.tvTotalPengeluaran)
        tvSisaKas = findViewById(R.id.tvSisaKas)
        tvHeaderList = findViewById(R.id.tvHeaderList)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        rvIuranAnniversary = findViewById(R.id.rvIuranAnniversary)
        progressBar = findViewById(R.id.progressBar)

        btnRefresh.setOnClickListener {
            loadDataAnniversary(showToast = true)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        rvIuranAnniversary.layoutManager = LinearLayoutManager(this)
        rvIuranAnniversary.adapter = adapter
    }

    /**
     * Memuat data detail kas anniversary dari API dengan no-cache
     */
    fun loadDataAnniversary(showToast: Boolean = false) {
        progressBar.visibility = View.VISIBLE

        val request = object : StringRequest(
            Request.Method.GET,
            apiUrl,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val rootJson = JSONObject(response)

                    // 1. Pemasukan Kas Anniversary (Kiri Atas)
                    val totalPemasukan = rootJson.optDouble("total_pemasukan", 0.0)
                    tvTotalPemasukan.text = formatRupiah(totalPemasukan)

                    // 2. Pengeluaran Kas Anniversary (Kanan Atas)
                    val totalPengeluaran = rootJson.optDouble("total_pengeluaran", 0.0)
                    tvTotalPengeluaran.text = formatRupiah(totalPengeluaran)

                    // 3. Sisa Kas Aniv (Nominal Besar)
                    val sisaKas = rootJson.optDouble("sisa_kas", maxOf(0.0, totalPemasukan - totalPengeluaran))
                    tvSisaKas.text = formatRupiah(sisaKas)

                    // 5. RecyclerView / List Item dari array "data"
                    val dataArray = rootJson.optJSONArray("data")
                    val items = mutableListOf<IuranAnniversaryItem>()

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val itemObj = dataArray.optJSONObject(i) ?: continue
                            val nama = itemObj.optString("nama", "-")
                            val jumlahBayar = itemObj.optDouble("jumlah_bayar", itemObj.optDouble("nominal", 0.0))
                            val nra = itemObj.optString("nra", "-")
                            val keterangan = itemObj.optString("keterangan", "Iuran Anniversary")
                            val id = itemObj.optInt("id", i + 1)

                            items.add(
                                IuranAnniversaryItem(
                                    id = id,
                                    nama = nama,
                                    nra = nra,
                                    jumlahBayar = jumlahBayar,
                                    keterangan = keterangan
                                )
                            )
                        }
                    }

                    // 4. Header List: jumlah item dari size array / key "total_transaksi"
                    val totalTransaksi = rootJson.optInt("total_transaksi", items.size)
                    val countDisplay = if (totalTransaksi > 0) totalTransaksi else items.size
                    tvHeaderList.text = "Daftar Iuran Masuk ($countDisplay)"

                    // Update Adapter
                    adapter.submitList(items)

                    if (items.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        rvIuranAnniversary.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        rvIuranAnniversary.visibility = View.VISIBLE
                    }

                    if (showToast) {
                        Toast.makeText(this@DetailIuranAnniversaryActivity, "Data iuran anniversary diperbarui", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@DetailIuranAnniversaryActivity, "Format data server tidak sesuai", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                error.printStackTrace()
                Toast.makeText(this@DetailIuranAnniversaryActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        ) {
            // 6. Anti-Crash & Cache: Tambahkan Header Cache-Control: no-cache
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache"
                headers["Pragma"] = "no-cache"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Format angka ke format Rupiah (contoh: "Rp 150.000", "Rp 0")
     */
    private fun formatRupiah(nominal: Double): String {
        val localeID = Locale("in", "ID")
        val formatter = NumberFormat.getNumberInstance(localeID)
        return "Rp " + formatter.format(nominal.toLong())
    }
}

/**
 * Model data transaksi Iuran Anniversary
 */
data class IuranAnniversaryItem(
    val id: Int,
    val nama: String,
    val nra: String,
    val jumlahBayar: Double,
    val keterangan: String
)

/**
 * Adapter RecyclerView untuk menampilkan daftar Iuran Anniversary
 */
class IuranAnniversaryAdapter : RecyclerView.Adapter<IuranAnniversaryAdapter.ViewHolder>() {

    private val dataList = mutableListOf<IuranAnniversaryItem>()

    fun submitList(newList: List<IuranAnniversaryItem>) {
        dataList.clear()
        dataList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_iuran_anniversary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemCount(): Int = dataList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        private val tvKeterangan: TextView = itemView.findViewById(R.id.tvKeterangan)
        private val tvJumlahBayar: TextView = itemView.findViewById(R.id.tvJumlahBayar)

        fun bind(item: IuranAnniversaryItem) {
            // Nama di sebelah kiri
            tvNama.text = item.nama
            tvKeterangan.text = if (item.keterangan.isNotBlank()) item.keterangan else "Iuran Anniversary"

            // Jumlah bayar dengan format Rupiah di sebelah kanan
            tvJumlahBayar.text = formatRupiah(item.jumlahBayar)
        }

        private fun formatRupiah(nominal: Double): String {
            val localeID = Locale("in", "ID")
            val formatter = NumberFormat.getNumberInstance(localeID)
            return "Rp " + formatter.format(nominal.toLong())
        }
    }
}
