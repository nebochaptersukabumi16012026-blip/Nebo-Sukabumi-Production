package com.example.ui

import android.content.Context
import android.widget.Toast
import com.example.network.ApiClient
import com.example.network.DetailKasResponse
import com.example.network.RiwayatTransaksiKas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.Anggota
import com.example.data.Pembayaran
import com.example.data.Pengeluaran
import com.example.data.KasKeliling
import com.example.network.KasKelilingUnifiedResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Utility for uniform sharing across detail screens
fun shareReport(context: Context, title: String, content: String) {
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n$content")
        type = "text/plain"
    }
    context.startActivity(android.content.Intent.createChooser(sendIntent, "Bagikan Laporan"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .size(40.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            textAlign = TextAlign.Start
        )

        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .size(40.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Segarkan", tint = Color.White)
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onShare,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .size(40.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = Color.White)
        }
    }
}

@Composable
fun DarkGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E3B4E), Color(0xFF1F2937))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

// 👥 Screen 1: Detail Anggota Screen
@Composable
fun DetailAnggotaScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val anggotaList by viewModel.allAnggota.collectAsState()
    val allPembayaran by viewModel.allPembayaran.collectAsState()
    val filteredList = anggotaList.filter {
        it.nama.contains(searchQuery, ignoreCase = true) || it.nra.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Detail Anggota",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        isRefreshing = false
                        Toast.makeText(context, "Data anggota diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val content = filteredList.mapIndexed { index, item ->
                        "${index + 1}. ${item.nama} (NRA: ${item.nra.ifBlank { "-" }}) - Status: ${if (item.statusAktif == 1) "Aktif" else "Nonaktif"}"
                    }.joinToString("\n")
                    shareReport(context, "DAFTAR ANGGOTA KOMUNITAS\nTotal Anggota: ${filteredList.size} Orang", content)
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Anggota (Nama, NRA)", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.6f),
                    unfocusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.3f)
                )
            )

            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2196F3))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Anggota Terdaftar", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                Text("${anggotaList.size} Orang", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Daftar Anggota (${filteredList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Tidak ada anggota ditemukan", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(filteredList) { member ->
                        val memberPayments = remember(allPembayaran, member) {
                            allPembayaran.filter { p ->
                                p.anggotaId == member.id ||
                                (member.nama.isNotBlank() && p.anggotaNama.trim().equals(member.nama.trim(), ignoreCase = true))
                            }
                        }
                        val kasNominal = memberPayments.filter { it.jenisPembayaran.equals("KAS", ignoreCase = true) }.sumOf { it.nominal }
                        val anivNominal = memberPayments.filter { it.jenisPembayaran.equals("ANIV", ignoreCase = true) }.sumOf { it.nominal }

                        DarkGradientCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("anggota_detail?id=${member.id}") }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("NRA: ${member.nra.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Kas: ${formatRupiah(kasNominal)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64B5F6))
                                        Text("Aniv: ${formatRupiah(anivNominal)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFB74D))
                                    }
                                    if (userRole != "ANGGOTA") {
                                        Text("HP: ${member.nomorTelepon.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                
                                Text(
                                    text = if (member.statusAktif == 1) "Aktif" else "Nonaktif",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = if (member.statusAktif == 1) Color(0xFF4CAF50) else Color(0xFFF44336),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data class for uniform rendering of Kas items
data class KasItemUi(
    val id: Int,
    val anggotaId: Int,
    val nama: String,
    val nra: String,
    val nominal: Double,
    val tanggal: String,
    val keterangan: String,
    val originalPembayaran: Pembayaran? = null,
    val originalKasKeliling: KasKeliling? = null
)

// 💰 Screen 2: Detail Uang Kas Screen
@Composable
fun DetailUangKasScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<KasItemUi?>(null) }
    var locallyDeletedIds by remember { mutableStateOf(setOf<Int>()) }
    var showResetAuditDialog by remember { mutableStateOf(false) }
    
    val dashboardData by viewModel.dashboardData.collectAsState()
    val detailKas by viewModel.detailKasState.collectAsState()
    val allAnggota by viewModel.allAnggota.collectAsState()
    val allPengeluaran by viewModel.allPengeluaran.collectAsState()
    val userRole by viewModel.loggedInUserRole.collectAsState()
    
    // ATURAN HAK AKSES:
    // 1. Tombol Hapus (Delete / Reset Kas per item) HANYA TAMPIL & BISA DIGUNAKAN oleh 'developer'
    val isDeveloper = userRole?.equals("DEVELOPER", ignoreCase = true) == true
    // 2. Fitur Tutup Periode Audit & Export PDF bisa diakses oleh DEVELOPER, ADMIN, BENDAHARA
    val canManageAudit = userRole?.uppercase() in listOf("DEVELOPER", "ADMIN", "BENDAHARA")
    
    var showExportPdfDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.getDetailKas()
        viewModel.syncFromApi()
    }

    // 1. Build Transaction List from riwayat (Real Member Data) with fallback to allAnggota
    val allKasTransactions = remember(detailKas, allAnggota, locallyDeletedIds) {
        val list = if (detailKas != null && !detailKas?.riwayat.isNullOrEmpty()) {
            detailKas!!.riwayat!!.filter { !locallyDeletedIds.contains(it.id ?: -1) }.map { item ->
                KasItemUi(
                    id = item.id ?: 0,
                    anggotaId = item.id ?: 0,
                    nama = item.nama,
                    nra = item.nra ?: "-",
                    nominal = item.nominal,
                    tanggal = item.tanggal ?: "Hari Ini",
                    keterangan = item.keterangan ?: "Iuran Kas Anggota",
                    originalPembayaran = Pembayaran(id = item.id ?: 0, anggotaId = item.id ?: 0, anggotaNama = item.nama, jenisPembayaran = "KAS", nominal = item.nominal, tanggalBayar = 0)
                )
            }
        } else {
            allAnggota.filter { it.uangKas > 0 && !locallyDeletedIds.contains(it.id) }.map { member ->
                KasItemUi(
                    id = member.id,
                    anggotaId = member.id,
                    nama = member.nama,
                    nra = member.nra.ifEmpty { "-" },
                    nominal = member.uangKas,
                    tanggal = "Hari Ini",
                    keterangan = "Iuran Kas Anggota",
                    originalPembayaran = Pembayaran(id = member.id, anggotaId = member.id, anggotaNama = member.nama, jenisPembayaran = "KAS", nominal = member.uangKas, tanggalBayar = 0)
                )
            }
        }
        list.sortedByDescending { it.id }
    }

    // 2. Totals from Detail Kas (Pure Member Cash Payments) or fallback to allAnggota
    val totalPemasukan = detailKas?.total_pemasukan ?: allAnggota.sumOf { it.uangKas }
    val totalPengeluaran = detailKas?.total_pengeluaran ?: allPengeluaran.filter {
        val jk = it.jenisKas.trim().lowercase()
        (jk in listOf("kas_utama", "kas utama", "kas", "saldo kas", "uang kas", "uang_kas")) ||
        (!jk.contains("keliling") && !jk.contains("aniv") && !jk.contains("anniversary") && !jk.contains("cicilan") && jk.isNotEmpty())
    }.sumOf { it.nominal }
    val saldoSaatIni = detailKas?.saldo?.let { maxOf(0.0, it) } ?: maxOf(0.0, totalPemasukan - totalPengeluaran)
    val filteredTransactions = allKasTransactions.filter {
        it.nama.contains(searchQuery, ignoreCase = true) || 
        it.keterangan.contains(searchQuery, ignoreCase = true) ||
        it.nra.contains(searchQuery, ignoreCase = true)
    }

    // Confirmation Dialog for Safe Deletion (HANYA UNTUK ROLE DEVELOPER)
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Pembayaran Kas", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Apakah Anda yakin ingin menghapus iuran kas anggota \"${target.nama}\" sebesar ${formatRupiah(target.nominal)}?",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        "• Iuran kas anggota akan di-reset menjadi Rp 0.\n" +
                        "• Total Pemasukan dan Saldo Kas Komunitas akan berkurang otomatis senilai ${formatRupiah(target.nominal)}.\n" +
                        "• Data kas dan ringkasan akan langsung diperbarui secara real-time.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        locallyDeletedIds = locallyDeletedIds + target.id
                        // Sesuai Instruksi: Reset kas anggota menjadi 0 di tabel anggota & kurangi saldo
                        viewModel.resetMemberKas(target.id) { success, msg ->
                             Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                             viewModel.getDetailKas()
                             viewModel.syncFromApi()
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Hapus Data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Dialog Export PDF (BENDAHARA, ADMIN, DEVELOPER)
    if (showExportPdfDialog) {
        AlertDialog(
            onDismissRequest = { showExportPdfDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Simpan PDF Kas", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pilih metode untuk mencetak atau membagikan Laporan Keuangan Uang Kas lengkap dengan rincian pemasukan anggota dan saldo akhir.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showExportPdfDialog = false
                            viewModel.saveLaporanKasPdf(context) { path ->
                                if (path != null) {
                                    Toast.makeText(context, "PDF berhasil disimpan di folder Unduhan (Downloads)", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Gagal membuat file PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            showExportPdfDialog = false
                            viewModel.shareLaporanKasPdf(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPdfDialog = false }) {
                    Text("Tutup", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Confirmation Dialog for Close Audit / Reset Periode Kas
    if (showResetAuditDialog) {
        AlertDialog(
            onDismissRequest = { showResetAuditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tutup Periode & Audit Kas", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Apakah Anda yakin ingin menutup periode audit kas bulanan ini?",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "• Seluruh iuran kas anggota akan di-reset menjadi Rp 0 untuk periode buku baru.\n" +
                        "• Riwayat kas bulanan diarsipkan dan dikosongkan untuk menyambut iuran baru.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetAuditDialog = false
                        viewModel.resetPeriodeKasAudit { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            viewModel.getDetailKas()
                            viewModel.syncFromApi()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Tutup Periode Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAuditDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Detail Uang Kas",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        delay(400)
                        isRefreshing = false
                        Toast.makeText(context, "Data kas berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val daftarTeks = filteredTransactions.mapIndexed { index, item ->
                        "${index + 1}. ${item.nama}: ${formatRupiah(item.nominal)} (${item.tanggal})"
                    }.joinToString("\n")

                    val teksLaporan = "Ijin lapor pak uang yang terkumpul dan data yang masuk\n\nLAPORAN KEUANGAN UANG KAS\n\nRingkasan Kas:\n- Total Pemasukan: ${formatRupiah(totalPemasukan)}\n- Total Pengeluaran: ${formatRupiah(totalPengeluaran)}\n- Saldo Kas Saat Ini: ${formatRupiah(saldoSaatIni)}\n\nDaftar Pembayaran Kas Terakhir:\n$daftarTeks"

                    shareReport(context, "", teksLaporan)
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Riwayat Kas (Nama/NRA/Keterangan)", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.6f),
                    unfocusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.3f)
                )
            )

            if (isRefreshing && allKasTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
                ) {
                    item {
                        DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Ringkasan Kas Komunitas", style = MaterialTheme.typography.titleSmall, color = Color.LightGray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Pemasukan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(formatRupiah(totalPemasukan), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Pengeluaran", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(formatRupiah(totalPengeluaran), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Saldo Kas Saat Ini", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                    Text(
                                        text = formatRupiah(saldoSaatIni),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (saldoSaatIni >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (saldoSaatIni >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    if (canManageAudit) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showExportPdfDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EXPORT PDF",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = { showResetAuditDialog = true },
                                    modifier = Modifier.weight(1.3f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TUTUP AUDIT KAS",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Riwayat Transaksi Kas (${filteredTransactions.size} Anggota Bayar)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Tidak ada riwayat transaksi kas ditemukan", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        items(filteredTransactions) { trx ->
                            DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Nama:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(trx.nama, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Text("NRA:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(trx.nra, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "TERKONFIRMASI",
                                                    color = Color(0xFF4CAF50),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Text("Nominal:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(
                                                text = formatRupiah(trx.nominal),
                                                color = Color(0xFF4CAF50),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            
                                            // HANYA ROLE DEVELOPER YANG DAPAT MELIHAT DAN MENEKAN TOMBOL HAPUS
                                            if (isDeveloper) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                IconButton(
                                                    onClick = { itemToDelete = trx },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Riwayat",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.05f))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Keterangan:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(trx.keterangan, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Tanggal:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(trx.tanggal, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// 🎉 Screen 3: Detail Iuran Aniv Screen
@Composable
fun DetailIuranAnivScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val anggotaList by viewModel.allAnggota.collectAsState()
    val pengeluaranList by viewModel.allPengeluaran.collectAsState()
    
    val totalPengeluaranAniv = pengeluaranList.filter { it.jenisKas == "Kas Aniv" }.sumOf { it.nominal }
    
    val totalPemasukanAniv = dashboardData?.total_aniv ?: anggotaList.sumOf { it.iuranAniv }
    val totalAniv = totalPemasukanAniv - totalPengeluaranAniv
    
    val anivPayments = anggotaList.filter { it.iuranAniv > 0.0 }

    val filteredList = anivPayments.filter {
        it.nama.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.iuranAniv }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Detail Iuran Anniversary",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        isRefreshing = false
                        Toast.makeText(context, "Data iuran diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val contentStr = """
                        Ringkasan Anniversary:
                        - Total Pemasukan: ${formatRupiah(totalPemasukanAniv)}
                        - Total Pengeluaran: ${formatRupiah(totalPengeluaranAniv)}
                        - Sisa Kas Aniv: ${formatRupiah(totalAniv)}

                        Daftar Anggota yang Sudah Membayar:
                        ${filteredList.mapIndexed { idx, it -> "${idx+1}. ${it.nama}: ${formatRupiah(it.iuranAniv)}" }.joinToString("\n")}
                    """.trimIndent()
                    shareReport(context, "LAPORAN IURAN ANNIVERSARY", contentStr)
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Iuran (Nama Pembayar)", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE91E63),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.6f),
                    unfocusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.3f)
                )
            )

            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE91E63))
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Pemasukan Aniv", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(formatRupiah(totalPemasukanAniv), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Pengeluaran Aniv", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(formatRupiah(totalPengeluaranAniv), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sisa Kas Aniv", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                Text(
                                    text = formatRupiah(totalAniv),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalAniv >= 0) Color(0xFFE91E63) else Color(0xFFF44336)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = if (totalAniv >= 0) Color(0xFFE91E63) else Color(0xFFF44336),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Daftar Iuran Masuk (${filteredList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada yang membayar Anniversary", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(filteredList) { trx ->
                        DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trx.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Iuran Anniversary", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                }
                                
                                Text(
                                    text = formatRupiah(trx.iuranAniv),
                                    color = Color(0xFFE91E63),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



// 🧾 Screen 4: Detail Sisa Cicilan Screen
@Composable
fun DetailSisaCicilanScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val loggedInUserId by viewModel.loggedInUserId.collectAsState()
    val anggotaList by viewModel.allAnggota.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()

    val totalSisaCicilan = dashboardData?.total_sisa_cicilan ?: anggotaList.sumOf { it.sisaCicilan }
    
    val membersWithCicilan = anggotaList.filter { 
        it.sisaCicilan > 0.0 && (userRole != "ANGGOTA" || it.id == loggedInUserId) 
    }
    val filteredList = membersWithCicilan.filter {
        it.nama.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.sisaCicilan }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Detail Sisa Cicilan",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        isRefreshing = false
                        Toast.makeText(context, "Data cicilan diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val contentStr = """
                        Total Sisa Cicilan Keseluruhan: ${formatRupiah(totalSisaCicilan)}

                        Daftar Anggota:
                        ${filteredList.mapIndexed { idx, it -> "${idx+1}. ${it.nama}: ${formatRupiah(it.sisaCicilan)}" }.joinToString("\n")}
                    """.trimIndent()
                    shareReport(context, "LAPORAN SISA CICILAN", contentStr)
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Anggota", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.6f),
                    unfocusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.3f)
                )
            )
            
            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF9800))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Keseluruhan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(formatRupiah(totalSisaCicilan), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                    }
                }
                
                if (filteredList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Tidak ada sisa cicilan", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredList) { member ->
                        DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(member.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(formatRupiah(member.sisaCicilan), color = Color(0xFFFF9800), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ⚠️ Screen 5: Detail Anggota Belum Bayar Kas
@Composable
fun DetailBelumKasScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val anggotaList by viewModel.allAnggota.collectAsState()
    val settings by viewModel.communitySettings.collectAsState()
    
    val targetKas = settings.target_kas
    val unpaidMembers = if (targetKas > 0.0) anggotaList.filter { it.uangKas < targetKas } else anggotaList.filter { it.uangKas == 0.0 }
    
    val filteredList = unpaidMembers.filter {
        it.nama.contains(searchQuery, ignoreCase = true)
    }.sortedBy { it.nama }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Belum Bayar Kas",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        isRefreshing = false
                        Toast.makeText(context, "Data diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val contentStr = """
                        Daftar Anggota Belum Lunas Kas:
                        ${filteredList.mapIndexed { idx, it -> "${idx+1}. ${it.nama}" + (if (targetKas > 0) " (Kurang: ${formatRupiah(targetKas - it.uangKas)})" else "") }.joinToString("\n")}
                    """.trimIndent()
                    shareReport(context, "LAPORAN BELUM BAYAR KAS", contentStr)
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Anggota", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFF44336), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.6f), unfocusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.3f)
                )
            )

            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFF44336)) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Anggota Belum Bayar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${filteredList.size} Orang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                    }
                }
                
                if (filteredList.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Semua anggota sudah bayar Kas", color = Color.Gray) } }
                } else {
                    items(filteredList) { member ->
                        DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(member.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                val tunggakan = targetKas - member.uangKas
                                if (tunggakan > 0) {
                                    Text("Kekurangan: ${formatRupiah(tunggakan)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF44336), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ⚠️ Screen 6: Detail Anggota Belum Bayar Anniversary
@Composable
fun DetailBelumAnivScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val anggotaList by viewModel.allAnggota.collectAsState()
    val settings by viewModel.communitySettings.collectAsState()
    
    val targetAniv = settings.target_aniv
    val unpaidMembers = if (targetAniv > 0.0) anggotaList.filter { it.iuranAniv < targetAniv } else anggotaList.filter { it.iuranAniv == 0.0 }
    
    val filteredList = unpaidMembers.filter {
        it.nama.contains(searchQuery, ignoreCase = true)
    }.sortedBy { it.nama }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Belum Bayar Anniversary",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        isRefreshing = false
                        Toast.makeText(context, "Data diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val contentStr = """
                        Daftar Anggota Belum Lunas Anniversary:
                        ${filteredList.mapIndexed { idx, it -> "${idx+1}. ${it.nama}" + (if (targetAniv > 0) " (Kurang: ${formatRupiah(targetAniv - it.iuranAniv)})" else "") }.joinToString("\n")}
                    """.trimIndent()
                    shareReport(context, "LAPORAN BELUM BAYAR ANNIVERSARY", contentStr)
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Anggota", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE91E63), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.6f), unfocusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.3f)
                )
            )

            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFE91E63)) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Anggota Belum Bayar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${filteredList.size} Orang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                    }
                }
                
                if (filteredList.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Semua anggota sudah bayar Anniversary", color = Color.Gray) } }
                } else {
                    items(filteredList) { member ->
                        DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(member.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                val tunggakan = targetAniv - member.iuranAniv
                                if (tunggakan > 0) {
                                    Text("Kekurangan: ${formatRupiah(tunggakan)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF44336), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 📉 Screen 7: Detail Total Pengeluaran Screen
@Composable
fun DetailTotalPengeluaranScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val dashboardData by viewModel.dashboardData.collectAsState()
    val pengeluaranList by viewModel.allPengeluaran.collectAsState()
    
    // Pastikan mengambil seluruh riwayat pengeluaran dari database cPanel pengeluaran.php
    val baseList = pengeluaranList
    val calculatedSum = baseList.sumOf { it.nominal }
    val totalPengeluaranAll = if (calculatedSum > 0.0) calculatedSum else (dashboardData?.totalPengeluaran ?: 0.0)
    
    val filteredList = baseList.filter {
        it.keterangan.contains(searchQuery, ignoreCase = true) ||
        it.jenisKas.contains(searchQuery, ignoreCase = true) ||
        com.example.network.PengeluaranParser.formatDateDisplay(it.tanggal).contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.tanggal }

    LaunchedEffect(Unit) {
        viewModel.syncFromApi()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Total Pengeluaran",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        isRefreshing = false
                        Toast.makeText(context, "Data pengeluaran diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val contentStr = """
                        Total Pengeluaran: ${formatRupiah(totalPengeluaranAll)}

                        Riwayat Pengeluaran (pengeluaran.php):
                        ${filteredList.mapIndexed { idx, it -> "${idx+1}. ${it.keterangan} [${it.jenisKas}] - ${com.example.network.PengeluaranParser.formatDateDisplay(it.tanggal)}: ${formatRupiah(it.nominal)}" }.joinToString("\n")}
                    """.trimIndent()
                    shareReport(context, "LAPORAN PENGELUARAN", contentStr)
                }
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Pengeluaran / Kategori / Tanggal", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFF44336), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.6f), unfocusedContainerColor = Color(0xFF1F2937).copy(alpha = 0.3f)
                )
            )

            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFF44336)) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Keseluruhan Pengeluaran", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                Text(
                                    formatRupiah(totalPengeluaranAll),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFFF43F5E)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${filteredList.size} Transaksi",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF43F5E)
                                )
                            }
                        }
                    }
                }
                
                if (filteredList.isEmpty()) {
                    item { 
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Tidak ada riwayat pengeluaran", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            }
                        } 
                    }
                } else {
                    items(filteredList) { trx ->
                        DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trx.keterangan.ifEmpty { "Pengeluaran #${trx.id}" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = trx.jenisKas.ifEmpty { "Pengeluaran" },
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = com.example.network.PengeluaranParser.formatDateDisplay(trx.tanggal),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = Color.LightGray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formatRupiah(trx.nominal),
                                    color = Color(0xFFF43F5E),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 💵 Screen 8: Detail Saldo Kas Screen
@Composable
fun DetailSaldoKasScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val kasKelilingList by viewModel.allKasKeliling.collectAsState()
    val pengeluaranList by viewModel.allPengeluaran.collectAsState()
    
    val totalPemasukan = kasKelilingList.sumOf { it.totalPemasukan }
    val totalPengeluaranMonthly = kasKelilingList.sumOf { it.totalPengeluaran }
    val totalPengeluaranBaru = pengeluaranList.filter { it.jenisKas == "Kas Keliling" }.sumOf { it.nominal }
    val totalPengeluaran = totalPengeluaranMonthly + totalPengeluaranBaru
    val saldoAkhir = totalPemasukan - totalPengeluaran
    
    val filteredList = kasKelilingList.filter {
        it.keterangan.contains(searchQuery, ignoreCase = true) || 
        it.bulan.contains(searchQuery, ignoreCase = true) ||
        it.tahun.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.tanggal }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = "Detail Kas Keliling",
                onBack = { navController.popBackStack() },
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncFromApiSuspend()
                        isRefreshing = false
                        Toast.makeText(context, "Data diperbarui", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val contentStr = """
                        Total Kas Keliling: ${formatRupiah(saldoAkhir)}
                    """.trimIndent()
                    shareReport(context, "LAPORAN KAS KELILING", contentStr)
                }
            )

            if (isRefreshing) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF4CAF50)) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
            ) {
                item {
                    DarkGradientCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Saldo Kas Keliling", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(formatRupiah(saldoAkhir), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (saldoAkhir >= 0) Color(0xFF4CAF50) else Color(0xFFF44336))
                    }
                }
            }
        }
    }
}
