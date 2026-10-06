package com.example.network

import android.util.Log
import com.example.data.KasKeliling
import org.json.JSONArray
import org.json.JSONObject

object KasKelilingParser {

    data class ParseResult(
        val transaksi: List<KasKeliling>,
        val summary: KasKelilingUnifiedResponse
    )

    fun parseKasKelilingJson(jsonStr: String): ParseResult {
        val items = mutableListOf<KasKeliling>()
        var totalPemasukan: Double? = null
        var totalPengeluaran: Double? = null
        var saldoKasKeliling: Double? = null

        if (jsonStr.isBlank()) {
            return ParseResult(
                emptyList(),
                KasKelilingUnifiedResponse(emptyList(), 0.0, 0.0, 0.0)
            )
        }

        try {
            val trimmed = jsonStr.trim()
            var arrayToParse: JSONArray? = null

            if (trimmed.startsWith("[")) {
                arrayToParse = JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val rootObj = JSONObject(trimmed)

                // 1. Ekstrak langsung dari root object jika ada
                if (rootObj.has("total_pemasukan") && !rootObj.isNull("total_pemasukan")) {
                    totalPemasukan = parseDoubleSafe(rootObj.opt("total_pemasukan"))
                }
                if (rootObj.has("total_pengeluaran") && !rootObj.isNull("total_pengeluaran")) {
                    totalPengeluaran = parseDoubleSafe(rootObj.opt("total_pengeluaran"))
                }
                if (rootObj.has("saldo_kas_keliling") && !rootObj.isNull("saldo_kas_keliling")) {
                    saldoKasKeliling = parseDoubleSafe(rootObj.opt("saldo_kas_keliling"))
                } else if (rootObj.has("saldo") && !rootObj.isNull("saldo")) {
                    saldoKasKeliling = parseDoubleSafe(rootObj.opt("saldo"))
                } else if (rootObj.has("saldo_akhir") && !rootObj.isNull("saldo_akhir")) {
                    saldoKasKeliling = parseDoubleSafe(rootObj.opt("saldo_akhir"))
                }

                if (rootObj.has("data") && !rootObj.isNull("data")) {
                    val dataVal = rootObj.get("data")
                    if (dataVal is JSONArray) {
                        arrayToParse = dataVal
                    } else if (dataVal is JSONObject) {
                        // Check if unified response object with summary + transaksi array
                        if (totalPemasukan == null && dataVal.has("total_pemasukan")) {
                            totalPemasukan = parseDoubleSafe(dataVal.opt("total_pemasukan"))
                        }
                        if (totalPengeluaran == null && dataVal.has("total_pengeluaran")) {
                            totalPengeluaran = parseDoubleSafe(dataVal.opt("total_pengeluaran"))
                        }
                        if (saldoKasKeliling == null) {
                            if (dataVal.has("saldo_kas_keliling")) {
                                saldoKasKeliling = parseDoubleSafe(dataVal.opt("saldo_kas_keliling"))
                            } else if (dataVal.has("saldo")) {
                                saldoKasKeliling = parseDoubleSafe(dataVal.opt("saldo"))
                            } else if (dataVal.has("saldo_akhir")) {
                                saldoKasKeliling = parseDoubleSafe(dataVal.opt("saldo_akhir"))
                            }
                        }

                        if (dataVal.has("transaksi") && !dataVal.isNull("transaksi")) {
                            val tVal = dataVal.get("transaksi")
                            if (tVal is JSONArray) {
                                arrayToParse = tVal
                            }
                        } else if (dataVal.has("list") && !dataVal.isNull("list")) {
                            val lVal = dataVal.get("list")
                            if (lVal is JSONArray) arrayToParse = lVal
                        } else {
                            arrayToParse = JSONArray().apply { put(dataVal) }
                        }
                    }
                } else if (rootObj.has("transaksi") && !rootObj.isNull("transaksi")) {
                    val tVal = rootObj.get("transaksi")
                    if (tVal is JSONArray) arrayToParse = tVal
                }
            }

            if (arrayToParse != null) {
                for (i in 0 until arrayToParse.length()) {
                    val item = arrayToParse.optJSONObject(i) ?: continue
                    val id = item.optInt("id", item.optString("id", "0").toIntOrNull() ?: 0)
                    val firestoreId = item.optString("firestore_id", "")
                    val jenisTransaksi = item.optString("jenis_transaksi", "Pemasukan")
                    val nominal = parseDoubleSafe(item.opt("nominal"))
                    val tanggal = PengeluaranParser.parseDateValue(item.opt("tanggal"))
                    val nama = when {
                        item.has("nama") && !item.isNull("nama") -> item.optString("nama")
                        item.has("nama_anggota") && !item.isNull("nama_anggota") -> item.optString("nama_anggota")
                        item.has("anggota_nama") && !item.isNull("anggota_nama") -> item.optString("anggota_nama")
                        else -> ""
                    }
                    val rawKeterangan = item.optString("keterangan", "")
                    val keterangan = when {
                        nama.isNotBlank() && rawKeterangan.isNotBlank() && !rawKeterangan.contains(nama, ignoreCase = true) -> "$nama - $rawKeterangan"
                        nama.isNotBlank() -> nama
                        else -> rawKeterangan
                    }
                    val bulan = item.optString("bulan", "")
                    val tahun = item.optString("tahun", "")

                    var itemTotalPemasukan = parseDoubleSafe(item.opt("total_pemasukan"))
                    var itemTotalPengeluaran = parseDoubleSafe(item.opt("total_pengeluaran"))
                    var itemSaldo = parseDoubleSafe(item.opt("saldo"))
                    if (itemSaldo == 0.0 && item.has("saldo_bulan")) {
                        itemSaldo = parseDoubleSafe(item.opt("saldo_bulan"))
                    }
                    val rawCatatan = when {
                        item.has("catatan") && !item.isNull("catatan") -> item.optString("catatan")
                        else -> ""
                    }
                    val catatan = when {
                        nama.isNotBlank() && rawCatatan.isNotBlank() && !rawCatatan.contains(nama, ignoreCase = true) -> "$nama - $rawCatatan"
                        nama.isNotBlank() && rawCatatan.isBlank() -> nama
                        else -> rawCatatan
                    }
                    val createdBy = item.optString("created_by", nama)

                    // Normalization if total_pemasukan / total_pengeluaran are 0 but nominal is set
                    if (itemTotalPemasukan == 0.0 && itemTotalPengeluaran == 0.0 && nominal > 0.0) {
                        if (jenisTransaksi.equals("Pengeluaran", ignoreCase = true)) {
                            itemTotalPengeluaran = nominal
                        } else {
                            itemTotalPemasukan = nominal
                        }
                    }

                    if (itemSaldo == 0.0 && (itemTotalPemasukan > 0.0 || itemTotalPengeluaran > 0.0)) {
                        itemSaldo = itemTotalPemasukan - itemTotalPengeluaran
                    }

                    items.add(
                        KasKeliling(
                            id = id,
                            firestoreId = firestoreId,
                            jenisTransaksi = jenisTransaksi,
                            nominal = nominal,
                            tanggal = tanggal,
                            keterangan = keterangan,
                            bulan = bulan,
                            tahun = tahun,
                            totalPemasukan = itemTotalPemasukan,
                            totalPengeluaran = itemTotalPengeluaran,
                            saldoBulan = itemSaldo,
                            catatan = catatan,
                            createdBy = createdBy,
                            createdAt = tanggal,
                            updatedAt = tanggal
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("KAS_PARSER", "Error parsing kas keliling json: ${e.message}", e)
        }

        val calculatedIn = totalPemasukan ?: items.sumOf { it.totalPemasukan }
        val calculatedOut = totalPengeluaran ?: items.sumOf { it.totalPengeluaran }
        val calculatedSaldo = saldoKasKeliling ?: (calculatedIn - calculatedOut)

        val summary = KasKelilingUnifiedResponse(
            transaksi = items,
            total_pemasukan = calculatedIn,
            total_pengeluaran = calculatedOut,
            saldo_kas_keliling = calculatedSaldo
        )

        return ParseResult(items, summary)
    }

    private fun parseDoubleSafe(value: Any?): Double {
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
}
