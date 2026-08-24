package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanBulananScreen(
    navController: NavController,
    viewModel: CommunityViewModel
) {
    val context = LocalContext.current

    val allPembayaran by viewModel.allPembayaran.collectAsState()
    val allPengeluaran by viewModel.allPengeluaran.collectAsState()
    val allKasKeliling by viewModel.allKasKeliling.collectAsState()

    val userRole by viewModel.loggedInUserRole.collectAsState()
    val currentRole = userRole?.uppercase() ?: "GUEST"
    val userId by viewModel.loggedInUserId.collectAsState()
    val allAnggota by viewModel.allAnggota.collectAsState()
    val currentUsername = remember(userId, allAnggota, userRole) {
        val member = allAnggota.find { it.id == userId }
        member?.nama ?: userRole ?: "Admin"
    }

    val currentCal = remember { Calendar.getInstance() }
    val currentYearNow = currentCal.get(Calendar.YEAR)
    val currentMonthNow = currentCal.get(Calendar.MONTH) + 1

    var selectedYear by remember { mutableStateOf(currentYearNow) }
    var selectedMonth by remember { mutableStateOf(currentMonthNow) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("SEMUA") }

    var showConfirmTutupBulan by remember { mutableStateOf(false) }

    var archivesTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.syncFromApi()
        MonthlyArchiveManager.loadArchives(context)
    }

    val availableYears = remember {
        val yearsList = (2024..currentYearNow + 2).toList().reversed()
        yearsList
    }

    val selectedMonthName = MonthlyArchiveManager.getMonthName(selectedMonth)
    val isClosed = remember(selectedYear, selectedMonth, archivesTrigger) {
        MonthlyArchiveManager.isMonthClosed(context, selectedYear, selectedMonth)
    }
    val closedArchive = remember(selectedYear, selectedMonth, archivesTrigger) {
        MonthlyArchiveManager.getClosedArchive(context, selectedYear, selectedMonth)
    }

    val saldoAwal = remember(selectedYear, selectedMonth, allPembayaran, allPengeluaran, allKasKeliling, archivesTrigger) {
        MonthlyArchiveManager.getSaldoAwalForMonth(context, selectedYear, selectedMonth, allPembayaran, allPengeluaran, allKasKeliling)
    }

    val transactions = remember(selectedYear, selectedMonth, allPembayaran, allPengeluaran, allKasKeliling) {
        MonthlyArchiveManager.filterTransactionsForMonth(selectedYear, selectedMonth, allPembayaran, allPengeluaran, allKasKeliling)
    }

    val totalKasKeliling = transactions.filter { it.jenisKas == "Kas Keliling" && it.tipe == "PEMASUKAN" }.sumOf { it.nominal }
    val totalKasAniv = transactions.filter { it.jenisKas == "Kas Anniversary" }.sumOf { it.nominal }
    val totalCicilan = transactions.filter { it.jenisKas == "Cicilan" }.sumOf { it.nominal }
    val totalPengeluaran = transactions.filter { it.tipe == "PENGELUARAN" }.sumOf { it.nominal }
    val totalPemasukanBulan = totalKasKeliling + totalKasAniv + totalCicilan
    val saldoAkhir = saldoAwal + totalPemasukanBulan - totalPengeluaran

    val canSeeCicilan = currentRole in listOf("DEVELOPER", "ADMIN", "BENDAHARA")

    val filteredTransactions = transactions.filter { tx ->
        if (!canSeeCicilan && tx.jenisKas == "Cicilan") return@filter false
        val matchesSearch = searchQuery.isBlank() ||
                tx.namaAtauKeterangan.contains(searchQuery, ignoreCase = true) ||
                tx.jenisKas.contains(searchQuery, ignoreCase = true)
        val matchesCat = when (selectedFilterCategory) {
            "Kas Keliling" -> tx.jenisKas == "Kas Keliling"
            "Kas Anniversary" -> tx.jenisKas == "Kas Anniversary"
            "Cicilan" -> tx.jenisKas == "Cicilan"
            "Pengeluaran" -> tx.tipe == "PENGELUARAN"
            else -> true
        }
        matchesSearch && matchesCat
    }

    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_laporan"]

    if (showConfirmTutupBulan) {
        AlertDialog(
            onDismissRequest = { showConfirmTutupBulan = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Tutup Bulan $selectedMonthName $selectedYear?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tutup Bulan akan mengunci seluruh data keuangan bulan $selectedMonthName $selectedYear secara permanen.")
                    Text("• Backup otomatis akan disimpan ke penyimpanan lokal.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Data bulan ini menjadi Read Only (tidak dapat diubah/dihapus).", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Saldo Akhir (${formatRupiah(saldoAkhir)}) otomatis menjadi Saldo Awal bulan berikutnya.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmTutupBulan = false
                        val (success, msg) = MonthlyArchiveManager.closeMonth(
                            context = context,
                            year = selectedYear,
                            month = selectedMonth,
                            currentUser = currentUsername,
                            userRole = currentRole,
                            allPembayaran = allPembayaran,
                            allPengeluaran = allPengeluaran,
                            allKasKeliling = allKasKeliling
                        )
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        archivesTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Ya, Tutup Bulan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmTutupBulan = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Laporan Bulanan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("$selectedMonthName $selectedYear", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.shareLaporanBulananPdf(context, selectedYear, selectedMonth)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export PDF")
                    }
                    IconButton(onClick = {
                        viewModel.saveLaporanBulananPdf(context, selectedYear, selectedMonth) { path ->
                            if (path != null) {
                                Toast.makeText(context, "PDF tersimpan di Downloads:\n$path", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Download PDF")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            DynamicBackground(configStr = bgConfigStr)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Filter Tahun & Bulan
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("🗓️ Pilih Periode Laporan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filter Tahun
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tahun: ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(availableYears) { yr ->
                                        FilterChip(
                                            selected = selectedYear == yr,
                                            onClick = { selectedYear = yr },
                                            label = { Text(yr.toString()) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filter Bulan
                            Text("Bulan: ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(12) { idx ->
                                    val mNum = idx + 1
                                    val mName = MonthlyArchiveManager.getMonthName(mNum)
                                    FilterChip(
                                        selected = selectedMonth == mNum,
                                        onClick = { selectedMonth = mNum },
                                        label = { Text(mName) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Status Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isClosed) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isClosed) Icons.Default.Lock else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isClosed) Color(0xFF2E7D32) else Color(0xFF1976D2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isClosed) "STATUS: DITUTUP (READ ONLY)" else "STATUS: BULAN AKTIF (BERJALAN)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isClosed) Color(0xFF2E7D32) else Color(0xFF1976D2)
                                    )
                                }
                                if (isClosed && closedArchive != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Diarsipkan oleh ${closedArchive.closedBy}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF388E3C)
                                    )
                                }
                            }

                            if (!isClosed && currentRole in listOf("DEVELOPER", "ADMIN", "BENDAHARA")) {
                                Button(
                                    onClick = { showConfirmTutupBulan = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tutup Bulan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Summary Cards
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "📊 Ringkasan Financial $selectedMonthName $selectedYear",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Saldo Awal
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📌 Saldo Awal Bulan", style = MaterialTheme.typography.bodyMedium)
                                Text(formatRupiah(saldoAwal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))

                            // Kas Keliling
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("💰 Total Kas Keliling", style = MaterialTheme.typography.bodySmall)
                                Text(formatRupiah(totalKasKeliling), style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // Kas Anniversary
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎉 Total Kas Anniversary", style = MaterialTheme.typography.bodySmall)
                                Text(formatRupiah(totalKasAniv), style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // Total Cicilan
                            if (canSeeCicilan) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📦 Total Cicilan", style = MaterialTheme.typography.bodySmall)
                                    Text(formatRupiah(totalCicilan), style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Total Pengeluaran
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("💸 Total Pengeluaran", style = MaterialTheme.typography.bodySmall)
                                Text(formatRupiah(totalPengeluaran), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(thickness = 2.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Saldo Akhir
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("💵 Saldo Akhir Bulan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        formatRupiah(saldoAkhir),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (saldoAkhir >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                                Surface(
                                    color = if (saldoAkhir >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (saldoAkhir >= 0) "Surplus" else "Defisit",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = if (saldoAkhir >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter & Search Transaksi
                item {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Cari Transaksi...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { FilterChip(selected = selectedFilterCategory == "SEMUA", onClick = { selectedFilterCategory = "SEMUA" }, label = { Text("Semua (${transactions.size})") }) }
                            item { FilterChip(selected = selectedFilterCategory == "Kas Keliling", onClick = { selectedFilterCategory = "Kas Keliling" }, label = { Text("Kas Keliling") }) }
                            item { FilterChip(selected = selectedFilterCategory == "Kas Anniversary", onClick = { selectedFilterCategory = "Kas Anniversary" }, label = { Text("Kas Aniv") }) }
                            if (canSeeCicilan) {
                                item { FilterChip(selected = selectedFilterCategory == "Cicilan", onClick = { selectedFilterCategory = "Cicilan" }, label = { Text("Cicilan") }) }
                            }
                            item { FilterChip(selected = selectedFilterCategory == "Pengeluaran", onClick = { selectedFilterCategory = "Pengeluaran" }, label = { Text("Pengeluaran") }) }
                        }
                    }
                }

                // Header List
                item {
                    Text(
                        "📋 Daftar Transaksi ($selectedMonthName $selectedYear)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tidak ada transaksi pada periode ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = when (tx.jenisKas) {
                                                "Kas Anniversary" -> Color(0xFFEDE7F6)
                                                "Cicilan" -> Color(0xFFE0F2F1)
                                                "Kas Keliling" -> Color(0xFFE8F5E9)
                                                else -> Color(0xFFFFEBEE)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tx.jenisKas,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = when (tx.jenisKas) {
                                                    "Kas Anniversary" -> Color(0xFF512DA8)
                                                    "Cicilan" -> Color(0xFF00796B)
                                                    "Kas Keliling" -> Color(0xFF2E7D32)
                                                    else -> MaterialTheme.colorScheme.error
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tx.tanggalStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tx.namaAtauKeterangan, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (tx.tipe == "PEMASUKAN") "+ ${formatRupiah(tx.nominal)}" else "- ${formatRupiah(tx.nominal)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.tipe == "PEMASUKAN") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
