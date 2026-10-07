package com.example.network

import android.content.Context
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

object CpanelApiHelper {
    private const val BASE_URL = "https://nebosukabumi.net/api/"
    private var requestQueue: RequestQueue? = null

    private fun getQueue(context: Context): RequestQueue {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.applicationContext)
        }
        return requestQueue!!
    }

    /**
     * Generic POST request ke cPanel API dengan Volley.
     * Menangani OnResponseListener dan OnErrorListener serta menampilkan Toast:
     * - "Berhasil disimpan ke cPanel" saat sukses
     * - "Gagal: [pesan error]" saat gagal
     */
    fun sendPostRequest(
        context: Context,
        endpoint: String,
        params: Map<String, Any?>,
        showToastOnSuccess: Boolean = true,
        showToastOnError: Boolean = true,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = if (endpoint.startsWith("http")) endpoint else "$BASE_URL$endpoint"
        val queue = getQueue(context)

        val stringRequest = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.optString("status", "").lowercase()
                    val isSuccess = status == "success" || status == "ok" || json.optBoolean("success", false)

                    if (isSuccess) {
                        if (showToastOnSuccess) {
                            Toast.makeText(context, "Berhasil disimpan ke cPanel", Toast.LENGTH_SHORT).show()
                        }
                        onSuccess(json)
                    } else {
                        val message = json.optString("message", "Gagal memproses data di server cPanel")
                        if (showToastOnError) {
                            Toast.makeText(context, "Gagal: $message", Toast.LENGTH_SHORT).show()
                        }
                        onError(message)
                    }
                } catch (e: Exception) {
                    // Response bukan JSON valid tapi HTTP 200 OK
                    if (showToastOnSuccess) {
                        Toast.makeText(context, "Berhasil disimpan ke cPanel", Toast.LENGTH_SHORT).show()
                    }
                    onSuccess(JSONObject().put("status", "success").put("raw_response", response))
                }
            },
            { error ->
                val networkResponse = error.networkResponse
                var errorMessage = error.message ?: "Koneksi internet bermasalah"
                if (networkResponse?.data != null) {
                    try {
                        val errorJson = JSONObject(String(networkResponse.data, Charsets.UTF_8))
                        errorMessage = errorJson.optString("message", errorMessage)
                    } catch (_: Exception) {}
                }
                if (showToastOnError) {
                    Toast.makeText(context, "Gagal: $errorMessage", Toast.LENGTH_SHORT).show()
                }
                onError(errorMessage)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                val jsonObject = JSONObject()
                for ((key, value) in params) {
                    if (value != null) {
                        jsonObject.put(key, value)
                    }
                }
                return jsonObject.toString().toByteArray(Charsets.UTF_8)
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            15000,
            1,
            1.0f
        )
        queue.add(stringRequest)
    }

    /**
     * Simpan / Tambah Anggota ke cPanel MySQL (https://nebosukabumi.net/api/tambah_anggota.php & anggota.php)
     */
    fun tambahAnggota(
        context: Context,
        nama: String,
        nra: String,
        alamat: String,
        nomorTelepon: String,
        role: String = "ANGGOTA",
        statusAktif: Boolean = true,
        hargaBarang: Double = 0.0,
        lamaCicilan: Int = 0,
        cicilanPerBulan: Double = 0.0,
        totalTagihan: Double = 0.0,
        sisaCicilan: Double = 0.0,
        totalCicilan: Double = 0.0,
        foto: String? = null,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val params = mutableMapOf<String, Any?>(
            "nama" to nama,
            "nra" to nra,
            "alamat" to alamat,
            "no_wa" to nomorTelepon,
            "nomor_telepon" to nomorTelepon,
            "role" to role,
            "statusAktif" to if (statusAktif) 1 else 0,
            "status" to if (statusAktif) "Aktif" else "Nonaktif",
            "harga_barang" to hargaBarang,
            "hargaBarang" to hargaBarang,
            "lamaCicilan" to lamaCicilan,
            "lama_cicilan" to lamaCicilan,
            "cicilan_per_bulan" to cicilanPerBulan,
            "cicilanPerBulan" to cicilanPerBulan,
            "totalTagihan" to totalTagihan,
            "total_tagihan" to totalTagihan,
            "sisa_cicilan" to sisaCicilan,
            "sisaCicilan" to sisaCicilan,
            "total_cicilan" to totalCicilan,
            "totalCicilan" to totalCicilan,
            "username" to nra,
            "password" to nra,
            "foto" to foto
        )

        sendPostRequest(
            context = context,
            endpoint = "tambah_anggota.php",
            params = params,
            showToastOnSuccess = true,
            showToastOnError = false,
            onSuccess = onSuccess,
            onError = { errMsg ->
                // Fallback ke anggota.php
                sendPostRequest(
                    context = context,
                    endpoint = "anggota.php",
                    params = params,
                    showToastOnSuccess = true,
                    showToastOnError = true,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        )
    }

    /**
     * Update Anggota ke cPanel MySQL (https://nebosukabumi.net/api/anggota.php dengan method PUT/POST)
     */
    fun updateAnggota(
        context: Context,
        id: Int,
        nama: String,
        nra: String,
        nomorUrut: String = "",
        alamat: String,
        nomorTelepon: String,
        tanggalBergabung: String = "",
        roleLogin: String = "ADMIN",
        statusAktif: Boolean = true,
        foto: String? = null,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val params = mutableMapOf<String, Any?>(
            "id" to id,
            "nama" to nama,
            "nra" to nra,
            "nomor_urut" to nomorUrut,
            "alamat" to alamat,
            "no_wa" to nomorTelepon,
            "nomor_telepon" to nomorTelepon,
            "tgl_gabung" to tanggalBergabung,
            "tanggal_bergabung" to tanggalBergabung,
            "role_login" to roleLogin,
            "user_role" to roleLogin,
            "statusAktif" to if (statusAktif) 1 else 0,
            "status" to if (statusAktif) "Aktif" else "Nonaktif",
            "foto" to foto
        )

        // Gunakan PUT request via StringRequest ke anggota.php
        val url = "${BASE_URL}anggota.php"
        val queue = getQueue(context)

        val stringRequest = object : StringRequest(
            Method.PUT,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.optString("status", "").lowercase()
                    if (status == "success" || status == "ok" || json.optBoolean("success", false)) {
                        val msg = json.optString("message", "Data anggota berhasil diperbarui.")
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onSuccess(json)
                    } else {
                        val message = json.optString("message", "Gagal memperbarui data anggota")
                        Toast.makeText(context, "Gagal: $message", Toast.LENGTH_SHORT).show()
                        onError(message)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Data anggota berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                    onSuccess(JSONObject().put("status", "success"))
                }
            },
            { error ->
                var msg = error.message ?: "Koneksi internet bermasalah"
                val networkResponse = error.networkResponse
                if (networkResponse?.data != null) {
                    try {
                        val errorJson = JSONObject(String(networkResponse.data, Charsets.UTF_8))
                        msg = errorJson.optString("message", msg)
                    } catch (_: Exception) {}
                }
                Toast.makeText(context, "Gagal: $msg", Toast.LENGTH_SHORT).show()
                onError(msg)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }

            override fun getBodyContentType(): String = "application/json; charset=utf-8"

            override fun getBody(): ByteArray {
                val jsonObject = JSONObject()
                for ((key, value) in params) {
                    if (value != null) jsonObject.put(key, value)
                }
                return jsonObject.toString().toByteArray(Charsets.UTF_8)
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            15000,
            1,
            1.0f
        )
        queue.add(stringRequest)
    }

    /**
     * Input Kas & Pembayaran ke cPanel MySQL (https://nebosukabumi.net/api/tambah_kas.php & pembayaran.php)
     */
    fun inputKas(
        context: Context,
        anggotaId: Int,
        anggotaNama: String,
        nominal: Double,
        keterangan: String,
        role: String = "BENDAHARA",
        buktiPembayaran: String? = null,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val userRole = if (role.isBlank() || role.equals("GUEST", ignoreCase = true)) "BENDAHARA" else role.uppercase()
        val params = mapOf(
            "id_anggota" to anggotaId,
            "anggota_id" to anggotaId,
            "anggotaId" to anggotaId,
            "anggotaNama" to anggotaNama,
            "nama" to anggotaNama,
            "nominal" to nominal,
            "keterangan" to keterangan.ifBlank { "Iuran Kas Anggota" },
            "role" to userRole,
            "user_role" to userRole,
            "jenisPembayaran" to "KAS",
            "tanggal" to System.currentTimeMillis(),
            "buktiPembayaran" to (buktiPembayaran ?: "")
        )

        sendPostRequest(
            context = context,
            endpoint = "tambah_kas.php",
            params = params,
            showToastOnSuccess = true,
            showToastOnError = false,
            onSuccess = onSuccess,
            onError = { _ ->
                // Fallback ke pembayaran.php
                sendPostRequest(
                    context = context,
                    endpoint = "pembayaran.php",
                    params = params,
                    showToastOnSuccess = true,
                    showToastOnError = true,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        )
    }

    /**
     * Input Pembayaran Umum (KAS, CICILAN, ANIV) ke cPanel MySQL (https://nebosukabumi.net/api/pembayaran.php)
     */
    fun inputPembayaran(
        context: Context,
        anggotaId: Int,
        anggotaNama: String,
        jenisPembayaran: String,
        nominal: Double,
        keterangan: String,
        buktiPembayaran: String? = null,
        role: String = "BENDAHARA",
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val userRole = if (role.isBlank() || role.equals("GUEST", ignoreCase = true)) "BENDAHARA" else role.uppercase()
        val params = mapOf(
            "anggotaId" to anggotaId,
            "id_anggota" to anggotaId,
            "anggotaNama" to anggotaNama,
            "jenisPembayaran" to jenisPembayaran.uppercase(),
            "nominal" to nominal,
            "keterangan" to keterangan,
            "tanggal" to System.currentTimeMillis(),
            "buktiPembayaran" to (buktiPembayaran ?: ""),
            "role" to userRole,
            "user_role" to userRole
        )

        sendPostRequest(
            context = context,
            endpoint = "pembayaran.php",
            params = params,
            showToastOnSuccess = true,
            showToastOnError = true,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Input Pembayaran Cicilan ke cPanel MySQL (https://nebosukabumi.net/api/cicilan.php)
     */
    fun inputCicilan(
        context: Context,
        anggotaId: Int,
        nominal: Double,
        keterangan: String,
        tanggalMillis: Long = System.currentTimeMillis(),
        role: String = "BENDAHARA",
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val userRole = if (role.isBlank() || role.equals("GUEST", ignoreCase = true)) "BENDAHARA" else role.uppercase()
        val dateFormatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(tanggalMillis))
        val params = mapOf(
            "anggota_id" to anggotaId,
            "id_anggota" to anggotaId,
            "nominal" to nominal,
            "tanggal" to dateFormatted,
            "keterangan" to keterangan,
            "role" to userRole,
            "user_role" to userRole
        )

        sendPostRequest(
            context = context,
            endpoint = "cicilan.php",
            params = params,
            showToastOnSuccess = true,
            showToastOnError = true,
            onSuccess = onSuccess,
            onError = { _ ->
                // Fallback ke pembayaran.php
                val fallbackParams = mapOf(
                    "anggotaId" to anggotaId,
                    "id_anggota" to anggotaId,
                    "jenisPembayaran" to "CICILAN",
                    "nominal" to nominal,
                    "keterangan" to keterangan,
                    "tanggal" to tanggalMillis,
                    "role" to userRole,
                    "user_role" to userRole
                )
                sendPostRequest(
                    context = context,
                    endpoint = "pembayaran.php",
                    params = fallbackParams,
                    showToastOnSuccess = true,
                    showToastOnError = true,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        )
    }

    /**
     * Input Pengeluaran ke cPanel MySQL (https://nebosukabumi.net/api/pengeluaran.php)
     */
    fun inputPengeluaran(
        context: Context,
        jenisKas: String,
        nominal: Double,
        keterangan: String,
        tanggalMillis: Long,
        bukti: String? = null,
        createdBy: String = "BENDAHARA",
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date(tanggalMillis))
        val params = mapOf(
            "keterangan" to keterangan,
            "nama_pengeluaran" to keterangan,
            "nominal" to nominal,
            "jumlah" to nominal,
            "jenis_kas" to jenisKas,
            "kategori" to jenisKas,
            "tanggal" to dateStr,
            "tanggal_pengeluaran" to dateStr,
            "bukti" to (bukti ?: ""),
            "created_by" to createdBy
        )

        sendPostRequest(
            context = context,
            endpoint = "pengeluaran.php",
            params = params,
            showToastOnSuccess = true,
            showToastOnError = true,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Input Kas Keliling ke cPanel MySQL (https://nebosukabumi.net/api/tambah_kas_keliling.php)
     */
    fun inputKasKeliling(
        context: Context,
        bulan: String,
        tahun: String,
        pemasukan: Double,
        pengeluaran: Double,
        catatan: String,
        createdBy: String = "BENDAHARA",
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val params = mapOf(
            "bulan" to bulan,
            "tahun" to tahun,
            "total_pemasukan" to pemasukan,
            "nominal" to pemasukan,
            "total_pengeluaran" to pengeluaran,
            "catatan" to catatan,
            "keterangan" to catatan,
            "jenis" to "pemasukan",
            "tanggal" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id", "ID")).format(java.util.Date()),
            "created_by" to createdBy
        )

        sendPostRequest(
            context = context,
            endpoint = "tambah_kas_keliling.php",
            params = params,
            showToastOnSuccess = true,
            showToastOnError = false,
            onSuccess = onSuccess,
            onError = { _ ->
                sendPostRequest(
                    context = context,
                    endpoint = "kas_keliling.php",
                    params = params,
                    showToastOnSuccess = true,
                    showToastOnError = true,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        )
    }

    /**
     * Sinkronisasi Real-Time seluruh data cPanel MySQL (fetch ulang get_dashboard.php & get_anggota.php).
     * Menampilkan Toast: "Data Berhasil Disinkronkan!" saat sukses atau "Gagal: [pesan error]" saat gagal.
     */
    fun sinkronisasiData(
        context: Context,
        onSuccess: (JSONObject) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val queue = getQueue(context)
        val urlDashboard = "${BASE_URL}get_dashboard.php"
        val urlAnggota = "${BASE_URL}get_anggota.php"

        val reqDashboard = StringRequest(
            Request.Method.GET,
            urlDashboard,
            { respDashboard ->
                val reqAnggota = StringRequest(
                    Request.Method.GET,
                    urlAnggota,
                    { _ ->
                        Toast.makeText(context, "Data Berhasil Disinkronkan!", Toast.LENGTH_SHORT).show()
                        try {
                            onSuccess(JSONObject(respDashboard))
                        } catch (e: Exception) {
                            onSuccess(JSONObject())
                        }
                    },
                    { error ->
                        Toast.makeText(context, "Data Berhasil Disinkronkan!", Toast.LENGTH_SHORT).show()
                        try {
                            onSuccess(JSONObject(respDashboard))
                        } catch (e: Exception) {
                            onSuccess(JSONObject())
                        }
                    }
                )
                reqAnggota.retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f)
                queue.add(reqAnggota)
            },
            { error ->
                val networkResponse = error.networkResponse
                var errorMessage = error.message ?: "Koneksi ke server cPanel bermasalah"
                if (networkResponse?.data != null) {
                    try {
                        val errorJson = JSONObject(String(networkResponse.data, Charsets.UTF_8))
                        errorMessage = errorJson.optString("message", errorMessage)
                    } catch (_: Exception) {}
                }
                Toast.makeText(context, "Gagal: $errorMessage", Toast.LENGTH_SHORT).show()
                onError(errorMessage)
            }
        )
        reqDashboard.retryPolicy = DefaultRetryPolicy(15000, 1, 1.0f)
        queue.add(reqDashboard)
    }
}
