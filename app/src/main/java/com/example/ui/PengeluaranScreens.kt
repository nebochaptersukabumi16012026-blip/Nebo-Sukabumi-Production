package com.example.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.data.Pengeluaran
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengeluaranScreen(navController: NavController, viewModel: CommunityViewModel) {
    val pengeluaranList by viewModel.allPengeluaran.collectAsState()
    val userRole by viewModel.loggedInUserRole.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.syncFromApi() }

    var filterJenis by remember { mutableStateOf("SEMUA") } // SEMUA, Kas Aniv, Saldo Kas, Dana Cicilan

    val filteredList = pengeluaranList.filter {
        when (filterJenis) {
            "Kas Aniv" -> it.jenisKas == "Kas Aniv"
            "Dana Cicilan" -> it.jenisKas == "Dana Cicilan"
            "Kas Keliling" -> it.jenisKas == "Kas Keliling"
            "Saldo Kas" -> it.jenisKas == "Saldo Kas"
            else -> true
        }
    }

    var pengeluaranToDelete by remember { mutableStateOf<Pengeluaran?>(null) }
    var selectedImageForPreview by remember { mutableStateOf<String?>(null) }

    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_pembayaran"]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengeluaran Kas") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (userRole == "BENDAHARA" || userRole == "ADMIN" || userRole == "DEVELOPER") {
                FloatingActionButton(
                    onClick = { navController.navigate("pengeluaran_form?id=-1") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Pengeluaran")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            DynamicBackground(configStr = bgConfigStr)

            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                    FilterChip(
                        selected = filterJenis == "SEMUA",
                        onClick = { filterJenis = "SEMUA" },
                        label = { Text("Semua") }
                    )
                    }
                    item {
                    FilterChip(
                        selected = filterJenis == "Kas Aniv",
                        onClick = { filterJenis = "Kas Aniv" },
                        label = { Text("Kas Aniv") }
                    )
                    }
                    item {
                    FilterChip(
                        selected = filterJenis == "Saldo Kas",
                        onClick = { filterJenis = "Saldo Kas" },
                        label = { Text("Saldo Kas") }
                    )
                    }
                    item {
                    FilterChip(
                        selected = filterJenis == "Dana Cicilan",
                        onClick = { filterJenis = "Dana Cicilan" },
                        label = { Text("Dana Cicilan") }
                    )
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Tidak ada data pengeluaran", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (item.jenisKas == "Kas Aniv") Icons.Default.CardGiftcard else Icons.Default.AccountBalanceWallet,
                                                contentDescription = item.jenisKas,
                                                tint = if (item.jenisKas == "Kas Aniv") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                item.jenisKas,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            formatDate(item.tanggal),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        formatRupiah(item.nominal),
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        item.keterangan,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (!item.bukti.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clickable { selectedImageForPreview = item.bukti }
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(item.bukti),
                                                contentDescription = "Bukti Pengeluaran",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    val canManage = userRole?.uppercase() in listOf("BENDAHARA", "ADMIN", "DEVELOPER")
                                    if (canManage) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextButton(
                                                onClick = { navController.navigate("pengeluaran_form?id=${item.id}") },
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Edit")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            TextButton(
                                                onClick = { pengeluaranToDelete = item },
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Hapus")
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

        if (pengeluaranToDelete != null) {
            AlertDialog(
                onDismissRequest = { pengeluaranToDelete = null },
                title = { Text("Konfirmasi Hapus") },
                text = { Text("Apakah Anda yakin ingin menghapus pengeluaran ini?") },
                confirmButton = {
                    Button(
                        onClick = {
                            pengeluaranToDelete?.let { viewModel.deletePengeluaran(it) }
                            pengeluaranToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pengeluaranToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (selectedImageForPreview != null) {
            AlertDialog(
                onDismissRequest = { selectedImageForPreview = null },
                title = { Text("Bukti Pengeluaran") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageForPreview),
                            contentDescription = "Bukti",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedImageForPreview = null }) {
                        Text("Tutup")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengeluaranFormScreen(
    navController: NavController,
    viewModel: CommunityViewModel,
    pengeluaranId: Int
) {
    val pengeluaranList by viewModel.allPengeluaran.collectAsState()
    val existingPengeluaran = pengeluaranList.find { it.id == pengeluaranId }

    val isEditMode = existingPengeluaran != null

    var jenisKas by remember { mutableStateOf(existingPengeluaran?.jenisKas ?: "Kas Aniv") }
    var nominalStr by remember { mutableStateOf(existingPengeluaran?.nominal?.toInt()?.toString() ?: "") }
    var keterangan by remember { mutableStateOf(existingPengeluaran?.keterangan ?: "") }
    var tanggal by remember { mutableStateOf(existingPengeluaran?.tanggal ?: System.currentTimeMillis()) }
    var buktiUri by remember { mutableStateOf<Uri?>(existingPengeluaran?.bukti?.let { Uri.parse(it) }) }

    var expandedDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        buktiUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Pengeluaran" else "Tambah Pengeluaran") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Jenis Kas Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = jenisKas,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jenis Kas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Kas Aniv") },
                            onClick = {
                                jenisKas = "Kas Aniv"
                                expandedDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Saldo Kas") },
                            onClick = {
                                jenisKas = "Saldo Kas"
                                expandedDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dana Cicilan") },
                            onClick = {
                                jenisKas = "Dana Cicilan"
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Tanggal Picker
            OutlinedTextField(
                value = formatDate(tanggal),
                onValueChange = {},
                readOnly = true,
                label = { Text("Tanggal") },
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = tanggal
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(year, month, dayOfMonth)
                                tanggal = selectedCalendar.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Nominal Input
            OutlinedTextField(
                value = nominalStr,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        nominalStr = input
                    }
                },
                label = { Text("Nominal (Rupiah)") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Keterangan Textarea
            OutlinedTextField(
                value = keterangan,
                onValueChange = { keterangan = it },
                label = { Text("Keterangan") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Upload Bukti (Optional)
            Text("Upload Bukti (Opsional)", fontWeight = FontWeight.Bold)
            if (buktiUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(buktiUri),
                            contentDescription = "Bukti Pengeluaran",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { buktiUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pilih Foto Nota/Struk", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simpan Button
            Button(
                onClick = {
                    val nominal = nominalStr.toDoubleOrNull() ?: 0.0
                    if (nominal <= 0) {
                        Toast.makeText(context, "Nominal wajib lebih dari 0", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (jenisKas.isBlank()) {
                        Toast.makeText(context, "Jenis Kas wajib dipilih", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (keterangan.isBlank()) {
                        Toast.makeText(context, "Keterangan wajib diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (isEditMode && existingPengeluaran != null) {
                        viewModel.updatePengeluaran(
                            existingPengeluaran.copy(
                                jenisKas = jenisKas,
                                nominal = nominal,
                                keterangan = keterangan,
                                tanggal = tanggal,
                                bukti = buktiUri?.toString(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(context, "Pengeluaran berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addPengeluaran(
                            jenisKas = jenisKas,
                            nominal = nominal,
                            keterangan = keterangan,
                            tanggal = tanggal,
                            bukti = buktiUri?.toString(),
                            createdBy = "BENDAHARA"
                        )
                        Toast.makeText(context, "Pengeluaran berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    }
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (isEditMode) "Simpan Perubahan" else "Tambah Pengeluaran",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
