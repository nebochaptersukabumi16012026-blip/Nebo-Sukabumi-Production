package com.example.ui

import android.content.Context
import com.example.data.KasKeliling
import com.example.data.Pembayaran
import com.example.data.Pengeluaran
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthlyArchive(
    val year: Int,
    val month: Int, // 1..12
    val monthName: String,
    val saldoAwal: Double,
    val totalKasKeliling: Double,
    val totalKasAniv: Double,
    val totalCicilan: Double,
    val totalPengeluaran: Double,
    val saldoAkhir: Double,
    val isClosed: Boolean,
    val closedAt: Long,
    val closedBy: String,
    val backupFileName: String
)

data class MonthlyTransactionItem(
    val id: String,
    val tanggalMillis: Long,
    val tanggalStr: String,
    val jenisKas: String, // "Kas Keliling", "Kas Anniversary", "Cicilan", "Pengeluaran"
    val tipe: String, // "PEMASUKAN" or "PENGELUARAN"
    val namaAtauKeterangan: String,
    val nominal: Double
)

object MonthlyArchiveManager {
    val MONTH_NAMES = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val _archivesFlow = MutableStateFlow<List<MonthlyArchive>>(emptyList())
    val archivesFlow: StateFlow<List<MonthlyArchive>> = _archivesFlow.asStateFlow()

    fun getMonthName(month: Int): String {
        return if (month in 1..12) MONTH_NAMES[month - 1] else "Bulan $month"
    }

    fun loadArchives(context: Context): List<MonthlyArchive> {
        val sp = context.getSharedPreferences("NeboMonthlyArchives", Context.MODE_PRIVATE)
        val jsonStr = sp.getString("monthly_archives", "[]") ?: "[]"
        val result = mutableListOf<MonthlyArchive>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val year = obj.optInt("year")
                val month = obj.optInt("month")
                val monthName = obj.optString("monthName", getMonthName(month))
                val saldoAwal = obj.optDouble("saldoAwal", 0.0)
                val totalKasKeliling = obj.optDouble("totalKasKeliling", 0.0)
                val totalKasAniv = obj.optDouble("totalKasAniv", 0.0)
                val totalCicilan = obj.optDouble("totalCicilan", 0.0)
                val totalPengeluaran = obj.optDouble("totalPengeluaran", 0.0)
                val saldoAkhir = obj.optDouble("saldoAkhir", 0.0)
                val isClosed = obj.optBoolean("isClosed", false)
                val closedAt = obj.optLong("closedAt", 0L)
                val closedBy = obj.optString("closedBy", "")
                val backupFileName = obj.optString("backupFileName", "")

                result.add(
                    MonthlyArchive(
                        year = year,
                        month = month,
                        monthName = monthName,
                        saldoAwal = saldoAwal,
                        totalKasKeliling = totalKasKeliling,
                        totalKasAniv = totalKasAniv,
                        totalCicilan = totalCicilan,
                        totalPengeluaran = totalPengeluaran,
                        saldoAkhir = saldoAkhir,
                        isClosed = isClosed,
                        closedAt = closedAt,
                        closedBy = closedBy,
                        backupFileName = backupFileName
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _archivesFlow.value = result
        return result
    }

    fun saveArchives(context: Context, archives: List<MonthlyArchive>) {
        val sp = context.getSharedPreferences("NeboMonthlyArchives", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (arc in archives) {
            val obj = JSONObject()
            obj.put("year", arc.year)
            obj.put("month", arc.month)
            obj.put("monthName", arc.monthName)
            obj.put("saldoAwal", arc.saldoAwal)
            obj.put("totalKasKeliling", arc.totalKasKeliling)
            obj.put("totalKasAniv", arc.totalKasAniv)
            obj.put("totalCicilan", arc.totalCicilan)
            obj.put("totalPengeluaran", arc.totalPengeluaran)
            obj.put("saldoAkhir", arc.saldoAkhir)
            obj.put("isClosed", arc.isClosed)
            obj.put("closedAt", arc.closedAt)
            obj.put("closedBy", arc.closedBy)
            obj.put("backupFileName", arc.backupFileName)
            array.put(obj)
        }
        sp.edit().putString("monthly_archives", array.toString()).apply()
        _archivesFlow.value = archives
    }

    fun isMonthClosed(context: Context, year: Int, month: Int): Boolean {
        val archives = loadArchives(context)
        return archives.any { it.year == year && it.month == month && it.isClosed }
    }

    fun getClosedArchive(context: Context, year: Int, month: Int): MonthlyArchive? {
        val archives = loadArchives(context)
        return archives.firstOrNull { it.year == year && it.month == month }
    }

    fun getMonthRangeMillis(year: Int, month: Int): Pair<Long, Long> {
        val calStart = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, calStart.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Pair(calStart.timeInMillis, calEnd.timeInMillis)
    }

    fun getMonthYearFromMillis(timestamp: Long): Pair<Int, Int> {
        if (timestamp <= 0L) return Pair(2026, 1)
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return Pair(year, month)
    }

    fun isDateInClosedMonth(context: Context, timestamp: Long): Boolean {
        val (y, m) = getMonthYearFromMillis(timestamp)
        return isMonthClosed(context, y, m)
    }

    fun getSaldoAwalForMonth(
        context: Context,
        year: Int,
        month: Int,
        allPembayaran: List<Pembayaran>,
        allPengeluaran: List<Pengeluaran>,
        allKasKeliling: List<KasKeliling>
    ): Double {
        var prevYear = year
        var prevMonth = month - 1
        if (prevMonth < 1) {
            prevMonth = 12
            prevYear = year - 1
        }

        val prevArchive = getClosedArchive(context, prevYear, prevMonth)
        if (prevArchive != null && prevArchive.isClosed) {
            return prevArchive.saldoAkhir
        }

        val (startMillis, _) = getMonthRangeMillis(year, month)

        val pastPemasukanKas = allPembayaran.filter { it.jenisPembayaran == "KAS" && it.tanggalBayar < startMillis }.sumOf { it.nominal } +
                allKasKeliling.filter { it.jenisTransaksi == "Pemasukan" && parseDateToMillis(it.tanggal) < startMillis }.sumOf { it.nominal }
        val pastPemasukanAniv = allPembayaran.filter { it.jenisPembayaran == "ANIV" && it.tanggalBayar < startMillis }.sumOf { it.nominal }
        val pastPemasukanCicilan = allPembayaran.filter { it.jenisPembayaran == "CICILAN" && it.tanggalBayar < startMillis }.sumOf { it.nominal }

        val pastTotalPemasukan = pastPemasukanKas + pastPemasukanAniv + pastPemasukanCicilan

        val pastPengeluaranKas = allPengeluaran.filter { it.tanggal < startMillis }.sumOf { it.nominal } +
                allKasKeliling.filter { it.jenisTransaksi == "Pengeluaran" && parseDateToMillis(it.tanggal) < startMillis }.sumOf { it.nominal }

        return pastTotalPemasukan - pastPengeluaranKas
    }

    fun filterTransactionsForMonth(
        year: Int,
        month: Int,
        allPembayaran: List<Pembayaran>,
        allPengeluaran: List<Pengeluaran>,
        allKasKeliling: List<KasKeliling>
    ): List<MonthlyTransactionItem> {
        val (startMillis, endMillis) = getMonthRangeMillis(year, month)
        val result = mutableListOf<MonthlyTransactionItem>()

        // 1. Pembayaran (Kas, Aniv, Cicilan)
        allPembayaran.forEach { p ->
            val time = p.tanggalBayar
            if (time in startMillis..endMillis) {
                val jenis = when (p.jenisPembayaran) {
                    "ANIV" -> "Kas Anniversary"
                    "CICILAN" -> "Cicilan"
                    else -> "Kas Keliling"
                }
                val desc = if (p.keterangan.isNotBlank()) "${p.anggotaNama} (${p.keterangan})" else p.anggotaNama
                result.add(
                    MonthlyTransactionItem(
                        id = "pembayaran_${p.id}",
                        tanggalMillis = time,
                        tanggalStr = formatDate(time),
                        jenisKas = jenis,
                        tipe = "PEMASUKAN",
                        namaAtauKeterangan = desc,
                        nominal = p.nominal
                    )
                )
            }
        }

        // 2. Kas Keliling (Pemasukan & Pengeluaran)
        allKasKeliling.forEach { k ->
            val time = parseDateToMillis(k.tanggal)
            if (time in startMillis..endMillis) {
                val isPemasukan = k.jenisTransaksi.equals("Pemasukan", ignoreCase = true)
                result.add(
                    MonthlyTransactionItem(
                        id = "kas_keliling_${k.id}",
                        tanggalMillis = time,
                        tanggalStr = formatDate(time),
                        jenisKas = "Kas Keliling",
                        tipe = if (isPemasukan) "PEMASUKAN" else "PENGELUARAN",
                        namaAtauKeterangan = if (k.catatan.isNotBlank()) "${k.keterangan} - ${k.catatan}" else k.keterangan.ifEmpty { "Kas Keliling" },
                        nominal = k.nominal
                    )
                )
            }
        }

        // 3. Pengeluaran
        allPengeluaran.forEach { e ->
            val time = e.tanggal
            if (time in startMillis..endMillis) {
                val jenis = if (e.jenisKas.isNotBlank()) e.jenisKas else "Pengeluaran General"
                result.add(
                    MonthlyTransactionItem(
                        id = "pengeluaran_${e.id}",
                        tanggalMillis = time,
                        tanggalStr = formatDate(time),
                        jenisKas = jenis,
                        tipe = "PENGELUARAN",
                        namaAtauKeterangan = e.keterangan.ifEmpty { "Pengeluaran" },
                        nominal = e.nominal
                    )
                )
            }
        }

        return result.sortedByDescending { it.tanggalMillis }
    }

    fun performAutomaticBackup(
        context: Context,
        year: Int,
        month: Int,
        monthName: String,
        saldoAwal: Double,
        totalKasKeliling: Double,
        totalKasAniv: Double,
        totalCicilan: Double,
        totalPengeluaran: Double,
        saldoAkhir: Double,
        transactions: List<MonthlyTransactionItem>
    ): String {
        val timestamp = System.currentTimeMillis()
        val backupFileName = "Backup_Bulanan_${year}_${month}_$timestamp.json"
        try {
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val file = File(backupDir, backupFileName)

            val rootObj = JSONObject()
            rootObj.put("app_name", "Nebo Sukabumi")
            rootObj.put("backup_type", "TUTUP_BULAN_AUTOMATIC")
            rootObj.put("created_at", timestamp)
            rootObj.put("formatted_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))

            val summaryObj = JSONObject()
            summaryObj.put("year", year)
            summaryObj.put("month", month)
            summaryObj.put("month_name", monthName)
            summaryObj.put("saldo_awal", saldoAwal)
            summaryObj.put("total_kas_keliling", totalKasKeliling)
            summaryObj.put("total_kas_aniv", totalKasAniv)
            summaryObj.put("total_cicilan", totalCicilan)
            summaryObj.put("total_pengeluaran", totalPengeluaran)
            summaryObj.put("saldo_akhir", saldoAkhir)
            rootObj.put("summary", summaryObj)

            val txArray = JSONArray()
            transactions.forEach { tx ->
                val txObj = JSONObject()
                txObj.put("id", tx.id)
                txObj.put("tanggal_str", tx.tanggalStr)
                txObj.put("tanggal_millis", tx.tanggalMillis)
                txObj.put("jenis_kas", tx.jenisKas)
                txObj.put("tipe", tx.tipe)
                txObj.put("keterangan", tx.namaAtauKeterangan)
                txObj.put("nominal", tx.nominal)
                txArray.put(txObj)
            }
            rootObj.put("transactions", txArray)

            FileOutputStream(file).use { out ->
                out.write(rootObj.toString(2).toByteArray())
            }
            return backupFileName
        } catch (e: Exception) {
            e.printStackTrace()
            return "error_${e.message}"
        }
    }

    fun closeMonth(
        context: Context,
        year: Int,
        month: Int,
        currentUser: String,
        userRole: String,
        allPembayaran: List<Pembayaran>,
        allPengeluaran: List<Pengeluaran>,
        allKasKeliling: List<KasKeliling>
    ): Pair<Boolean, String> {
        val archives = loadArchives(context).toMutableList()
        val existingIndex = archives.indexOfFirst { it.year == year && it.month == month }
        val monthName = getMonthName(month)

        if (existingIndex >= 0 && archives[existingIndex].isClosed) {
            return Pair(false, "Bulan $monthName $year sudah dalam status Tutup Bulan.")
        }

        val transactions = filterTransactionsForMonth(year, month, allPembayaran, allPengeluaran, allKasKeliling)

        val (startMillis, endMillis) = getMonthRangeMillis(year, month)

        val totalKasKeliling = allPembayaran.filter { it.jenisPembayaran == "KAS" && it.tanggalBayar in startMillis..endMillis }.sumOf { it.nominal } +
                allKasKeliling.filter { it.jenisTransaksi == "Pemasukan" && parseDateToMillis(it.tanggal) in startMillis..endMillis }.sumOf { it.nominal }

        val totalKasAniv = allPembayaran.filter { it.jenisPembayaran == "ANIV" && it.tanggalBayar in startMillis..endMillis }.sumOf { it.nominal }

        val totalCicilan = allPembayaran.filter { it.jenisPembayaran == "CICILAN" && it.tanggalBayar in startMillis..endMillis }.sumOf { it.nominal }

        val totalPengeluaran = allPengeluaran.filter { it.tanggal in startMillis..endMillis }.sumOf { it.nominal } +
                allKasKeliling.filter { it.jenisTransaksi == "Pengeluaran" && parseDateToMillis(it.tanggal) in startMillis..endMillis }.sumOf { it.nominal }

        val saldoAwal = getSaldoAwalForMonth(context, year, month, allPembayaran, allPengeluaran, allKasKeliling)
        val saldoAkhir = saldoAwal + (totalKasKeliling + totalKasAniv + totalCicilan) - totalPengeluaran

        val backupFileName = performAutomaticBackup(
            context = context,
            year = year,
            month = month,
            monthName = monthName,
            saldoAwal = saldoAwal,
            totalKasKeliling = totalKasKeliling,
            totalKasAniv = totalKasAniv,
            totalCicilan = totalCicilan,
            totalPengeluaran = totalPengeluaran,
            saldoAkhir = saldoAkhir,
            transactions = transactions
        )

        val newArchive = MonthlyArchive(
            year = year,
            month = month,
            monthName = monthName,
            saldoAwal = saldoAwal,
            totalKasKeliling = totalKasKeliling,
            totalKasAniv = totalKasAniv,
            totalCicilan = totalCicilan,
            totalPengeluaran = totalPengeluaran,
            saldoAkhir = saldoAkhir,
            isClosed = true,
            closedAt = System.currentTimeMillis(),
            closedBy = "$currentUser ($userRole)",
            backupFileName = backupFileName
        )

        if (existingIndex >= 0) {
            archives[existingIndex] = newArchive
        } else {
            archives.add(newArchive)
        }

        saveArchives(context, archives)

        AuditLogManager.logActivity(
            context = context,
            username = currentUser,
            namaLengkap = currentUser,
            role = userRole,
            jenisAktivitas = "Tutup Bulan Otomatis",
            halamanMenu = "Laporan Bulanan",
            dataLama = "Bulan $monthName $year (Aktif)",
            dataBaru = "Status: DITUTUP (Read Only), Saldo Akhir: Rp ${saldoAkhir.toLong()}, Backup: $backupFileName",
            status = "Berhasil"
        )

        return Pair(true, "Bulan $monthName $year berhasil ditutup. Data telah diarsipkan (Read Only) & backup dibuat.")
    }
}
