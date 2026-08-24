package com.example.network

import android.util.Log
import com.example.data.Pengeluaran
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PengeluaranParser {

    fun parsePengeluaranJson(jsonStr: String): List<Pengeluaran> {
        val results = mutableListOf<Pengeluaran>()
        if (jsonStr.isBlank()) return results

        try {
            val trimmed = jsonStr.trim()
            var jsonArray: JSONArray? = null

            if (trimmed.startsWith("[")) {
                jsonArray = JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val rootObj = JSONObject(trimmed)
                when {
                    rootObj.has("data") && !rootObj.isNull("data") -> {
                        val dataVal = rootObj.get("data")
                        if (dataVal is JSONArray) {
                            jsonArray = dataVal
                        } else if (dataVal is JSONObject) {
                            jsonArray = JSONArray().apply { put(dataVal) }
                        }
                    }
                    rootObj.has("pengeluaran") && !rootObj.isNull("pengeluaran") -> {
                        val pVal = rootObj.get("pengeluaran")
                        if (pVal is JSONArray) jsonArray = pVal
                        else if (pVal is JSONObject) jsonArray = JSONArray().apply { put(pVal) }
                    }
                    rootObj.has("riwayat") && !rootObj.isNull("riwayat") -> {
                        val rVal = rootObj.get("riwayat")
                        if (rVal is JSONArray) jsonArray = rVal
                    }
                    rootObj.has("list") && !rootObj.isNull("list") -> {
                        val lVal = rootObj.get("list")
                        if (lVal is JSONArray) jsonArray = lVal
                    }
                    rootObj.has("result") && !rootObj.isNull("result") -> {
                        val resVal = rootObj.get("result")
                        if (resVal is JSONArray) jsonArray = resVal
                        else if (resVal is JSONObject) jsonArray = JSONArray().apply { put(resVal) }
                    }
                }
            }

            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val id = item.optInt("id", item.optString("id", "0").toIntOrNull() ?: 0)
                    val firestoreId = item.optString("firestore_id", "")

                    // Extract nominal / jumlah / total / biaya
                    var nominal = 0.0
                    if (item.has("nominal") && !item.isNull("nominal")) {
                        nominal = parseDoubleSafe(item.opt("nominal"))
                    } else if (item.has("jumlah") && !item.isNull("jumlah")) {
                        nominal = parseDoubleSafe(item.opt("jumlah"))
                    } else if (item.has("total") && !item.isNull("total")) {
                        nominal = parseDoubleSafe(item.opt("total"))
                    } else if (item.has("biaya") && !item.isNull("biaya")) {
                        nominal = parseDoubleSafe(item.opt("biaya"))
                    } else if (item.has("amount") && !item.isNull("amount")) {
                        nominal = parseDoubleSafe(item.opt("amount"))
                    }

                    // Extract nama_pengeluaran / keterangan / deskripsi / judul
                    val keterangan = when {
                        item.has("nama_pengeluaran") && !item.isNull("nama_pengeluaran") -> item.optString("nama_pengeluaran")
                        item.has("keterangan") && !item.isNull("keterangan") -> item.optString("keterangan")
                        item.has("nama") && !item.isNull("nama") -> item.optString("nama")
                        item.has("deskripsi") && !item.isNull("deskripsi") -> item.optString("deskripsi")
                        item.has("judul") && !item.isNull("judul") -> item.optString("judul")
                        item.has("uraian") && !item.isNull("uraian") -> item.optString("uraian")
                        else -> "Pengeluaran"
                    }.trim()

                    // Extract kategori / jenis_kas / jenis / tipe
                    val jenisKas = when {
                        item.has("kategori") && !item.isNull("kategori") -> item.optString("kategori")
                        item.has("jenis_kas") && !item.isNull("jenis_kas") -> item.optString("jenis_kas")
                        item.has("jenis") && !item.isNull("jenis") -> item.optString("jenis")
                        item.has("tipe") && !item.isNull("tipe") -> item.optString("tipe")
                        else -> "Pengeluaran"
                    }.trim()

                    // Extract tanggal / tgl / created_at
                    val rawDate = when {
                        item.has("tanggal") && !item.isNull("tanggal") -> item.opt("tanggal")
                        item.has("tanggal_pengeluaran") && !item.isNull("tanggal_pengeluaran") -> item.opt("tanggal_pengeluaran")
                        item.has("tgl") && !item.isNull("tgl") -> item.opt("tgl")
                        item.has("created_at") && !item.isNull("created_at") -> item.opt("created_at")
                        else -> null
                    }
                    val tanggalLong = parseDateValue(rawDate)

                    val bukti = when {
                        item.has("bukti") && !item.isNull("bukti") -> item.optString("bukti")
                        item.has("bukti_pengeluaran") && !item.isNull("bukti_pengeluaran") -> item.optString("bukti_pengeluaran")
                        item.has("foto") && !item.isNull("foto") -> item.optString("foto")
                        else -> ""
                    }.ifEmpty { null }

                    val createdBy = when {
                        item.has("created_by") && !item.isNull("created_by") -> item.optString("created_by")
                        item.has("admin") && !item.isNull("admin") -> item.optString("admin")
                        else -> ""
                    }

                    results.add(
                        Pengeluaran(
                            id = id,
                            firestoreId = firestoreId,
                            jenisKas = if (jenisKas.isNotEmpty()) jenisKas else "Pengeluaran",
                            nominal = nominal,
                            keterangan = if (keterangan.isNotEmpty()) keterangan else "Pengeluaran",
                            tanggal = tanggalLong,
                            bukti = bukti,
                            createdBy = createdBy,
                            createdAt = tanggalLong,
                            updatedAt = tanggalLong
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PENGELUARAN_PARSER", "Failed parsing pengeluaran JSON: ${e.message}", e)
        }
        return results
    }

    fun parseDoubleSafe(value: Any?): Double {
        if (value == null) return 0.0
        return when (value) {
            is Number -> value.toDouble()
            is String -> {
                val clean = value.replace("Rp", "", ignoreCase = true)
                    .replace(".", "")
                    .replace(",", ".")
                    .trim()
                clean.toDoubleOrNull() ?: 0.0
            }
            else -> 0.0
        }
    }

    fun parseDateValue(value: Any?): Long {
        if (value == null) return System.currentTimeMillis()
        when (value) {
            is Number -> {
                val num = value.toLong()
                return if (num < 10_000_000_000L) num * 1000L else num
            }
            is String -> {
                val str = value.trim()
                val formats = listOf(
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm",
                    "yyyy-MM-dd",
                    "dd-MM-yyyy HH:mm:ss",
                    "dd-MM-yyyy",
                    "dd/MM/yyyy HH:mm:ss",
                    "dd/MM/yyyy",
                    "yyyy/MM/dd",
                    "dd MMMM yyyy",
                    "d MMMM yyyy"
                )
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale("id", "ID"))
                        sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                        val d = sdf.parse(str)
                        if (d != null) return d.time
                    } catch (_: Exception) {}
                }
                val num = str.toLongOrNull()
                if (num != null) {
                    return if (num < 10_000_000_000L) num * 1000L else num
                }
            }
        }
        return System.currentTimeMillis()
    }

    fun formatDateDisplay(millis: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            sdf.format(Date(millis))
        } catch (e: Exception) {
            "-"
        }
    }
}
