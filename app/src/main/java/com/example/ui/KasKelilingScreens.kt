package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.network.KasKelilingUnifiedResponse
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.KasKeliling
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasKelilingScreen(navController: NavController, viewModel: CommunityViewModel) {
    val kasKelilingList by viewModel.allKasKeliling.collectAsState()
    val kasSummary by viewModel.kasKelilingSummary.collectAsState()
    val loggedInRole by viewModel.loggedInUserRole.collectAsState()
    val isBendahara = loggedInRole == "BENDAHARA" || loggedInRole == "ADMIN" || loggedInRole == "DEVELOPER"

    // Use summary from API if available, otherwise sum manually
    val grandTotalPemasukan = kasSummary?.total_pemasukan ?: kasKelilingList.sumOf { it.totalPemasukan }
    val grandTotalPengeluaran = kasSummary?.total_pengeluaran ?: kasKelilingList.sumOf { it.totalPengeluaran }
    val grandSaldo = kasSummary?.saldo_kas_keliling ?: (grandTotalPemasukan - grandTotalPengeluaran)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kas Keliling Bulanan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isBendahara) {
                FloatingActionButton(
                    onClick = { navController.navigate("kas_keliling_form?id=-1") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Elegant overall card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "REKAPITULASI KAS KELILING",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Pemasukan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(formatRupiah(grandTotalPemasukan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Pengeluaran", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(formatRupiah(grandTotalPengeluaran), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saldo Kas Keliling", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            formatRupiah(grandSaldo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (grandSaldo >= 0.0) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }
                }
            }

            if (kasKelilingList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data kas keliling bulanan.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(kasKelilingList.sortedByDescending { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isBendahara) {
                                        navController.navigate("kas_keliling_form?id=${item.id}")
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${item.bulan} ${item.tahun}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (isBendahara) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pemasukan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(formatRupiah(item.totalPemasukan), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pengeluaran", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(formatRupiah(item.totalPengeluaran), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                                    }
                                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                        Text("Saldo Bulan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(
                                            formatRupiah(item.saldoBulan),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (item.saldoBulan >= 0.0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                        )
                                    }
                                }
                                if (item.catatan.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        "Catatan: ${item.catatan}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        lineHeight = 16.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasKelilingFormScreen(navController: NavController, viewModel: CommunityViewModel, kasKelilingId: Int) {
    val kasKelilingList by viewModel.allKasKeliling.collectAsState()
    val existingItem = kasKelilingList.find { it.id == kasKelilingId }
    val context = LocalContext.current

    val months = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    var selectedBulan by remember { mutableStateOf(existingItem?.bulan ?: months[Calendar.getInstance().get(Calendar.MONTH)]) }
    var tahun by remember { mutableStateOf(existingItem?.tahun ?: Calendar.getInstance().get(Calendar.YEAR).toString()) }
    var totalPemasukan by remember { mutableStateOf(existingItem?.totalPemasukan?.toInt()?.toString() ?: "0") }
    var totalPengeluaran by remember { mutableStateOf(existingItem?.totalPengeluaran?.toInt()?.toString() ?: "0") }
    var catatan by remember { mutableStateOf(existingItem?.catatan ?: "") }

    var monthDropdownExpanded by remember { mutableStateOf(false) }

    // Auto calculate saldo
    val pemasukanVal = totalPemasukan.toDoubleOrNull() ?: 0.0
    val pengeluaranVal = totalPengeluaran.toDoubleOrNull() ?: 0.0
    val saldoBulan = pemasukanVal - pengeluaranVal

    val loggedInUserId by viewModel.loggedInUserId.collectAsState()
    val allAnggota by viewModel.allAnggota.collectAsState()
    val currentUsername = allAnggota.find { it.id == loggedInUserId }?.nama ?: "Bendahara"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (kasKelilingId == -1) "Tambah Kas Keliling" else "Edit Kas Keliling") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dropdown Bulan
            ExposedDropdownMenuBox(
                expanded = monthDropdownExpanded,
                onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBulan,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bulan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(24.dp)
                )
                ExposedDropdownMenu(
                    expanded = monthDropdownExpanded,
                    onDismissRequest = { monthDropdownExpanded = false }
                ) {
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                selectedBulan = month
                                monthDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Tahun Input
            OutlinedTextField(
                value = tahun,
                onValueChange = { tahun = it },
                label = { Text("Tahun") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )

            // Total Pemasukan Input
            OutlinedTextField(
                value = totalPemasukan,
                onValueChange = { totalPemasukan = it },
                label = { Text("Total Pemasukan (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )

            // Total Pengeluaran Input
            OutlinedTextField(
                value = totalPengeluaran,
                onValueChange = { totalPengeluaran = it },
                label = { Text("Total Pengeluaran (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )

            // Auto-Calculated Saldo Bulan Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saldo Bulan (Otomatis)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatRupiah(saldoBulan),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (saldoBulan >= 0.0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                }
            }

            // Catatan
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                label = { Text("Catatan") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (tahun.isBlank() || totalPemasukan.isBlank()) {
                        Toast.makeText(context, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (kasKelilingId == -1) {
                        viewModel.addMonthlyKasKeliling(
                            bulan = selectedBulan,
                            tahun = tahun,
                            totalPemasukan = pemasukanVal,
                            totalPengeluaran = pengeluaranVal,
                            catatan = catatan,
                            createdBy = currentUsername
                        )
                    } else {
                        viewModel.updateMonthlyKasKeliling(
                            id = kasKelilingId,
                            firestoreId = existingItem?.firestoreId ?: "",
                            bulan = selectedBulan,
                            tahun = tahun,
                            totalPemasukan = pemasukanVal,
                            totalPengeluaran = pengeluaranVal,
                            catatan = catatan,
                            createdBy = existingItem?.createdBy ?: currentUsername
                        )
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Simpan")
            }

            if (kasKelilingId != -1) {
                Button(
                    onClick = {
                        existingItem?.let { viewModel.deleteKasKeliling(it) }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Hapus")
                }
            }
        }
    }
}
