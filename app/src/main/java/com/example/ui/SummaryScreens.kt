package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Print
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversarySummaryScreen(navController: NavController, viewModel: CommunityViewModel) {
    val anggotaList by viewModel.allAnggota.collectAsState()
    val communitySettings by viewModel.communitySettings.collectAsState()
    val targetAniv = communitySettings.target_aniv
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val isGuest = userRole == "GUEST"

    var selectedStatus by remember { mutableStateOf("SUDAH") } // "SUDAH" or "BELUM"
    var searchQuery by remember { mutableStateOf("") }

    val sudahBayarList = anggotaList.filter { it.iuranAniv > 0.0 }
    val belumBayarList = if (targetAniv > 0.0) anggotaList.filter { it.iuranAniv < targetAniv } else emptyList()

    val activeList = if (selectedStatus == "SUDAH") sudahBayarList else belumBayarList
    val filteredList = activeList.filter {
        it.nama.contains(searchQuery, ignoreCase = true) || it.nra.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Anniversary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Sudah Bayar", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50))
                        Text("${sudahBayarList.size} Orang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Belum Bayar", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF44336))
                        Text("${belumBayarList.size} Orang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                    }
                }
            }

            if (isGuest) {
                // Info Card for Guest
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Akses Terbatas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sebagai Guest, Anda hanya diperkenankan melihat rangkuman grafik iuran Anniversary. Rincian nama anggota yang sudah/belum membayar disembunyikan untuk melindungi privasi data pribadi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Elegant tabs for Sudah Bayar / Belum Bayar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedStatus = "SUDAH" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedStatus == "SUDAH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedStatus == "SUDAH") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Sudah Bayar (${sudahBayarList.size})")
                    }

                    Button(
                        onClick = { selectedStatus = "BELUM" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedStatus == "BELUM") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedStatus == "BELUM") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Belum Bayar (${belumBayarList.size})")
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari Anggota") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada anggota ditemukan.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList) { member ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("anggota_detail?id=${member.id}") },
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.nama, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text("NRA: ${member.nra.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (member.iuranAniv > 0.0) {
                                        Text(
                                            formatRupiah(member.iuranAniv),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text("Sudah Lunas", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                                    } else {
                                        Text(
                                            "Belum Bayar",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE53935)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CicilanSummaryScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    val anggotaList by viewModel.allAnggota.collectAsState()
    var selectedFilter by remember { mutableStateOf("SEMUA") } // "SEMUA", "BELUM", "LUNAS"
    var searchQuery by remember { mutableStateOf("") }

    // Members with actual installments (hargaBarang > 0)
    val membersWithCicilan = anggotaList.filter { it.hargaBarang > 0.0 }
    val totalAnggota = membersWithCicilan.size
    val belumLunasList = membersWithCicilan.filter { it.sisaCicilan > 0.0 }
    val sudahLunasList = membersWithCicilan.filter { it.sisaCicilan <= 0.0 }

    val activeList = when (selectedFilter) {
        "BELUM" -> belumLunasList
        "LUNAS" -> sudahLunasList
        else -> membersWithCicilan
    }

    val filteredList = activeList.filter {
        it.nama.contains(searchQuery, ignoreCase = true) || it.nra.contains(searchQuery, ignoreCase = true)
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Ekspor Laporan PDF", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    // Option 1: PDF LAPORAN CICILAN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "1. Laporan Data Cicilan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Berisi logo, nama komunitas, tabel daftar cicilan anggota, status lunas/belum lunas, serta total sisa cicilan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Save
                                Button(
                                    onClick = {
                                        viewModel.saveLaporanCicilanPdf(context) { path ->
                                            if (path != null) {
                                                Toast.makeText(context, "PDF Berhasil disimpan ke Download:\n$path", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simpan", style = MaterialTheme.typography.labelSmall)
                                }
                                
                                // Share
                                Button(
                                    onClick = { viewModel.shareLaporanCicilanPdf(context) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bagikan", style = MaterialTheme.typography.labelSmall)
                                }
                                
                                // Print
                                Button(
                                    onClick = { viewModel.printLaporanCicilanPdf(context) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cetak", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Option 2: PDF RIWAYAT PEMBAYARAN CICILAN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "2. Laporan Riwayat Pembayaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Berisi tabel riwayat pembayaran cicilan lengkap dengan tanggal, nominal pembayaran, sisa cicilan setelah dibayar, serta total akumulasi pembayaran.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Save
                                Button(
                                    onClick = {
                                        viewModel.saveRiwayatPembayaranPdf(context) { path ->
                                            if (path != null) {
                                                Toast.makeText(context, "PDF Berhasil disimpan ke Download:\n$path", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simpan", style = MaterialTheme.typography.labelSmall)
                                }
                                
                                // Share
                                Button(
                                    onClick = { viewModel.shareRiwayatPembayaranPdf(context) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bagikan", style = MaterialTheme.typography.labelSmall)
                                }
                                
                                // Print
                                Button(
                                    onClick = { viewModel.printRiwayatPembayaranPdf(context) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cetak", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Cicilan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Ekspor PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        val totalHargaBarang = membersWithCicilan.sumOf { it.hargaBarang }
        val totalSisaCicilan = membersWithCicilan.sumOf { it.sisaCicilan }
        val totalSudahDibayar = totalHargaBarang - totalSisaCicilan

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 2. Summary Grid Cards section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Anggota Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilter = "SEMUA" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFilter == "SEMUA") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Anggota", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalAnggota", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Belum Lunas Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilter = "BELUM" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFilter == "BELUM") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Belum Lunas", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${belumLunasList.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                        }
                    }

                    // Sudah Lunas Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilter = "LUNAS" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFilter == "LUNAS") MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Sudah Lunas", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${sudahLunasList.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            // 3. Search Bar section
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari Anggota Cicilan") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // 4. Header text
            item {
                Text(
                    text = when (selectedFilter) {
                        "BELUM" -> "Daftar Anggota Belum Lunas"
                        "LUNAS" -> "Daftar Anggota Sudah Lunas"
                        else -> "Semua Anggota yang Memiliki Cicilan"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp).padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 5. List items
            if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada data cicilan ditemukan.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(filteredList) { member ->
                    val sudahDibayar = member.hargaBarang - member.sisaCicilan
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("anggota_detail?id=${member.id}") },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("NRA: ${member.nra.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text(
                                    text = if (member.sisaCicilan <= 0.0) "LUNAS" else "BELUM LUNAS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (member.sisaCicilan <= 0.0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Cicilan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(formatRupiah(member.hargaBarang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Sudah Dibayar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(formatRupiah(sudahDibayar), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text("Sisa Cicilan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(formatRupiah(member.sisaCicilan), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (member.sisaCicilan > 0.0) Color(0xFFE53935) else Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SisaCicilanOnlyScreen(navController: NavController, viewModel: CommunityViewModel) {
    val anggotaList by viewModel.allAnggota.collectAsState()
    
    // Only show members that have a remaining cicilan > 0
    val activeList = anggotaList.filter { it.sisaCicilan > 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Sisa Cicilan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (activeList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada anggota dengan sisa cicilan.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(activeList) { member ->
                        val sudahDibayar = member.hargaBarang - member.sisaCicilan
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("anggota_detail?id=${member.id}") },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(member.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Total Cicilan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(formatRupiah(member.hargaBarang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Total Dibayar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(formatRupiah(sudahDibayar), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("Sisa Cicilan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(formatRupiah(member.sisaCicilan), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
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
