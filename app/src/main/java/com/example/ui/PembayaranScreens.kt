package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.util.Locale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UangKasScreen(navController: NavController, viewModel: CommunityViewModel) {
    val pembayaranList by viewModel.allPembayaran.collectAsState()
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val loggedInUserId by viewModel.loggedInUserId.collectAsState()
    
    var filterJenis by remember { mutableStateOf("SEMUA") } // SEMUA, KAS, ANIV, CICILAN

    val filteredList = pembayaranList.filter {
        if (userRole == "ANGGOTA" && it.anggotaId != loggedInUserId) return@filter false
        if (userRole == "GUEST" && it.jenisPembayaran == "CICILAN") return@filter false
        
        when (filterJenis) {
            "KAS" -> it.jenisPembayaran == "KAS"
            "ANIV" -> it.jenisPembayaran == "ANIV"
            "CICILAN" -> it.jenisPembayaran == "CICILAN"
            else -> true
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.syncFromApi() }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_pembayaran"]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Pembayaran") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            DynamicBackground(configStr = bgConfigStr)

            Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterJenis == "SEMUA",
                    onClick = { filterJenis = "SEMUA" },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = filterJenis == "KAS",
                    onClick = { filterJenis = "KAS" },
                    label = { Text("Kas") }
                )
                FilterChip(
                    selected = filterJenis == "ANIV",
                    onClick = { filterJenis = "ANIV" },
                    label = { Text("Aniv") }
                )
                if (userRole != "GUEST") {
                    FilterChip(
                        selected = filterJenis == "CICILAN",
                        onClick = { filterJenis = "CICILAN" },
                        label = { Text("Cicilan") }
                    )
                }
            }
            
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { trx ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(trx.anggotaNama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${trx.jenisPembayaran} - ${formatRupiah(trx.nominal)}", color = MaterialTheme.colorScheme.primary)
                                Text(formatDate(trx.tanggalBayar), style = MaterialTheme.typography.bodySmall)
                                if (trx.buktiPembayaran != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(onClick = { selectedImage = trx.buktiPembayaran }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(30.dp)) {
                                        Text("Lihat Bukti", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        selectedImage?.let { image ->
            FullscreenImageDialog(
                imagePath = image,
                onDismiss = { selectedImage = null },
                userRole = userRole,
                onDownload = {
                    Toast.makeText(context, "Mulai mengunduh...", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PembayaranFormScreen(
    navController: NavController,
    viewModel: CommunityViewModel,
    anggotaId: Int,
    jenisPembayaran: String
) {
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val isDeveloper = userRole?.equals("DEVELOPER", ignoreCase = true) == true
    val isKasOrAniv = jenisPembayaran.equals("KAS", ignoreCase = true) || jenisPembayaran.equals("ANIV", ignoreCase = true)

    if (isKasOrAniv && !isDeveloper) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Akses Ditolak") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Akses Ditolak",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hanya akun Developer yang diizinkan menginput data Kas dan Anniversary.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Kembali")
                    }
                }
            }
        }
        return
    }

    val anggota = if (anggotaId != -1) viewModel.getAnggotaById(anggotaId).collectAsState(initial = null).value else null
    var nominalStr by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }
    var buktiUri by remember { mutableStateOf<Uri?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        buktiUri = uri
    }

    val context = LocalContext.current
    var paymentSaved by remember { mutableStateOf(false) }
    var lastSavedNominal by remember { mutableStateOf(0.0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (paymentSaved) "Pembayaran Berhasil" else "Bayar $jenisPembayaran") },
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
            modifier = Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val sdf = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("id", "ID"))
            val tanggalStr = sdf.format(java.util.Date())

            if (anggota != null) {
                Text("Nama Anggota: ${anggota.nama}", style = MaterialTheme.typography.bodyLarge)
                Text("NRA: ${anggota.nra}", style = MaterialTheme.typography.bodyLarge)
                
                if (jenisPembayaran == "CICILAN") {
                    val sisaCicilanDisplay = formatRupiah(if (paymentSaved) anggota.sisaCicilan - lastSavedNominal else anggota.sisaCicilan)
                    val cicilanPerBulan = if (anggota.lamaCicilan > 0) anggota.hargaBarang / anggota.lamaCicilan else 0.0
                    val cicilanPerBulanDisplay = formatRupiah(cicilanPerBulan)
                    
                    Text("Sisa Cicilan Saat Ini: $sisaCicilanDisplay", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    Text("Cicilan Per Bulan: $cicilanPerBulanDisplay", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text("Anggota: Tidak diketahui", style = MaterialTheme.typography.titleMedium)
            }
            
            Text("Tanggal: $tanggalStr", style = MaterialTheme.typography.bodyLarge)

            if (!paymentSaved) {
                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it },
                    label = { Text("Nominal Pembayaran") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Bukti Pembayaran (Opsional)")
                if (buktiUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(buktiUri),
                        contentDescription = "Bukti Pembayaran",
                        modifier = Modifier.fillMaxWidth().height(200.dp).clickable { galleryLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.surfaceVariant).clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Pilih / Ambil Foto Bukti")
                    }
                }
            } else {
                // Payment Saved State
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Berhasil", tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
                }
                Text("Pembayaran sebesar ${formatRupiah(lastSavedNominal)} berhasil disimpan.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.weight(1f))
            
            if (!paymentSaved) {
                if (jenisPembayaran == "CICILAN" && (anggota?.sisaCicilan ?: 0.0) <= 0.0) {
                    Text(
                        text = "LUNAS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Button(
                        onClick = {
                            val nominal = nominalStr.toDoubleOrNull() ?: 0.0
                            if (nominal > 0 && anggota != null) {
                                if (jenisPembayaran == "CICILAN" && nominal > anggota.sisaCicilan) {
                                    Toast.makeText(context, "Nominal pembayaran melebihi sisa cicilan.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addPembayaran(
                                    anggotaId = anggota.id,
                                    anggotaNama = anggota.nama,
                                    jenisPembayaran = jenisPembayaran,
                                    nominal = nominal,
                                    buktiPembayaran = buktiUri?.toString(),
                                    keterangan = keterangan
                                )
                                lastSavedNominal = nominal
                                paymentSaved = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Simpan Pembayaran", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Selesai")
                    }
                    
                    Button(
                        onClick = {
                            val shareText = """
                                NEBO SUKABUMI
                                Bukti Pembayaran
                                Nama: ${anggota?.nama ?: "-"}
                                ${if (jenisPembayaran == "ANIV") "Anniversary: ${formatRupiah(lastSavedNominal)}" else ""}
                                ${if (jenisPembayaran == "CICILAN") "Cicilan: ${formatRupiah(lastSavedNominal)}" else ""}
                                ${if (jenisPembayaran == "KAS") "Uang Kas: ${formatRupiah(lastSavedNominal)}" else ""}
                                Nominal Dibayar: ${formatRupiah(lastSavedNominal)}
                                ${if (jenisPembayaran == "CICILAN") "Total Dibayar: ${formatRupiah((anggota?.hargaBarang ?: 0.0) - (anggota?.sisaCicilan ?: 0.0) + lastSavedNominal)}\nSisa Cicilan: ${formatRupiah((anggota?.sisaCicilan ?: 0.0) - lastSavedNominal)}" else ""}
                                Tanggal: $tanggalStr
                            """.trimIndent().lines().filter { it.isNotBlank() }.joinToString("\n")
                            
                            val sendIntent: android.content.Intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncFromApi()
    }
    
    val pembayaranList by viewModel.allPembayaran.collectAsState()
    val anggotaList by viewModel.allAnggota.collectAsState()
    val pengeluaranList by viewModel.allPengeluaran.collectAsState()
    val kasKelilingList by viewModel.allKasKeliling.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    
    // Password Form State
    var passwordLama by remember { mutableStateOf("") }
    var passwordBaru by remember { mutableStateOf("") }
    var konfirmasiPassword by remember { mutableStateOf("") }
    var passwordLamaVisible by remember { mutableStateOf(false) }
    var passwordBaruVisible by remember { mutableStateOf(false) }
    var konfirmasiVisible by remember { mutableStateOf(false) }
    var isSavingPassword by remember { mutableStateOf(false) }

    val dashboardData by viewModel.dashboardData.collectAsState()
    val laporanData by viewModel.laporanData.collectAsState()

    // 1. KARTU 1: 💰 UANG KAS (Kas Bulanan/Iuran Anggota Utama)
    val totalPemasukanKas = laporanData?.kas_utama?.total_pemasukan 
        ?: dashboardData?.kas_utama?.total_pemasukan 
        ?: (dashboardData?.total_kas ?: anggotaList.sumOf { it.uangKas })

    val totalPengeluaranKas = laporanData?.kas_utama?.total_pengeluaran 
        ?: dashboardData?.kas_utama?.total_pengeluaran 
        ?: pengeluaranList.filter { 
            it.jenisKas in listOf("Saldo Kas", "Kas", "Kas Utama", "Uang Kas", "") || 
            (!it.jenisKas.equals("Kas Keliling", ignoreCase = true) && 
             !it.jenisKas.equals("Kas Aniv", ignoreCase = true) && 
             !it.jenisKas.equals("Kas Anniversary", ignoreCase = true) && 
             !it.jenisKas.equals("Dana Cicilan", ignoreCase = true) && 
             !it.jenisKas.equals("Cicilan", ignoreCase = true))
        }.sumOf { it.nominal }

    val saldoKasUtama = laporanData?.kas_utama?.saldo_kas 
        ?: dashboardData?.kas_utama?.saldo_kas 
        ?: (totalPemasukanKas - totalPengeluaranKas)

    // 2. KARTU 2: 🛵 KAS KELILING (Uang Kopdar/Keliling)
    val totalPemasukanKeliling = laporanData?.kas_keliling?.total_pemasukan 
        ?: dashboardData?.kas_keliling_data?.total_pemasukan 
        ?: (dashboardData?.kas_keliling ?: kasKelilingList.sumOf { it.totalPemasukan })

    val totalPengeluaranKeliling = laporanData?.kas_keliling?.total_pengeluaran 
        ?: dashboardData?.kas_keliling_data?.total_pengeluaran 
        ?: (pengeluaranList.filter { it.jenisKas.equals("Kas Keliling", ignoreCase = true) }.sumOf { it.nominal }.takeIf { it > 0 } ?: kasKelilingList.sumOf { it.totalPengeluaran })

    val saldoKasKeliling = laporanData?.kas_keliling?.saldo_keliling 
        ?: dashboardData?.kas_keliling_data?.saldo_keliling 
        ?: (totalPemasukanKeliling - totalPengeluaranKeliling)
    
    // 3. KARTU 3: 🎉 KAS ANNIVERSARY
    val totalPemasukanAniv = laporanData?.kas_anniversary?.total_pemasukan 
        ?: dashboardData?.kas_anniversary_data?.total_pemasukan 
        ?: (dashboardData?.iuran_anniversary ?: dashboardData?.total_aniv ?: pembayaranList.filter { it.jenisPembayaran == "ANIV" }.sumOf { it.nominal })

    val totalPengeluaranAniv = laporanData?.kas_anniversary?.total_pengeluaran 
        ?: dashboardData?.kas_anniversary_data?.total_pengeluaran 
        ?: pengeluaranList.filter { it.jenisKas.equals("Kas Aniv", ignoreCase = true) || it.jenisKas.equals("Kas Anniversary", ignoreCase = true) }.sumOf { it.nominal }

    val saldoKasAniv = laporanData?.kas_anniversary?.saldo_aniv 
        ?: dashboardData?.kas_anniversary_data?.saldo_aniv 
        ?: (totalPemasukanAniv - totalPengeluaranAniv)
    
    // 4. CICILAN
    val totalSisaCicilan = dashboardData?.total_sisa_cicilan ?: anggotaList.filter { it.hargaBarang > 0.0 }.sumOf { it.sisaCicilan }
    val totalHargaBarang = anggotaList.filter { it.hargaBarang > 0.0 }.sumOf { it.hargaBarang }
    val totalSudahDibayar = totalHargaBarang - totalSisaCicilan
    val totalPengeluaranCicilan = pengeluaranList.filter { it.jenisKas.equals("Dana Cicilan", ignoreCase = true) || it.jenisKas.equals("Cicilan", ignoreCase = true) }.sumOf { it.nominal }
    val saldoDanaCicilan = totalSudahDibayar - totalPengeluaranCicilan
    
    val filteredAnggota = anggotaList.filter { 
        it.nama.contains(searchQuery, ignoreCase = true)
    }

    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_laporan"]
    
    val userRole by viewModel.loggedInUserRole.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.syncFromApi() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi") },
            text = { Text("Apakah Anda yakin ingin keluar?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Keuangan", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Keluar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().imePadding()) {
            DynamicBackground(configStr = bgConfigStr)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: 💰 UANG KAS (Kas Bulanan/Iuran Anggota Utama)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "💰 UANG KAS",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                if (userRole != "GUEST") {
                                    IconButton(
                                        onClick = { viewModel.shareLaporanKasPdf(context) },
                                        modifier = Modifier.size(32.dp).testTag("share_kas_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Pemasukan Kas", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text(formatRupiah(totalPemasukanKas), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Pengeluaran Kas", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text(formatRupiah(totalPengeluaranKas), style = MaterialTheme.typography.titleSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Saldo Akhir Uang Kas", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(
                                formatRupiah(saldoKasUtama),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (saldoKasUtama >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Card 2: 🛵 KAS KELILING (Uang Kopdar/Keliling)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "🛵 KAS KELILING",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                if (userRole != "GUEST") {
                                    IconButton(
                                        onClick = { viewModel.shareLaporanKelilingPdf(context) },
                                        modifier = Modifier.size(32.dp).testTag("share_keliling_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Pemasukan Kas Keliling", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text(formatRupiah(totalPemasukanKeliling), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Pengeluaran Kas Keliling", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text(formatRupiah(totalPengeluaranKeliling), style = MaterialTheme.typography.titleSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Saldo Akhir Kas Keliling", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(
                                formatRupiah(saldoKasKeliling),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (saldoKasKeliling >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Card 3: 🎉 KAS ANNIVERSARY
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "🎉 KAS ANNIVERSARY",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                if (userRole != "GUEST" && userRole != "ANGGOTA") {
                                    IconButton(
                                        onClick = { viewModel.shareLaporanAnivPdf(context) },
                                        modifier = Modifier.size(32.dp).testTag("share_aniv_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Total Pemasukan Kas Aniv", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text(formatRupiah(totalPemasukanAniv), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Pengeluaran Kas Aniv", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                    Text(formatRupiah(totalPengeluaranAniv), style = MaterialTheme.typography.titleSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Saldo Akhir Kas Anniversary", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(
                                formatRupiah(saldoKasAniv),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (saldoKasAniv >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (userRole == "DEVELOPER" || userRole == "ADMIN" || userRole == "BENDAHARA") {
                    // Card 3: CICILAN
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E293B)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "📦 CICILAN",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.shareLaporanCicilanPdf(context) },
                                        modifier = Modifier.size(32.dp).testTag("share_cicilan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Total Harga Barang", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                        Text(formatRupiah(totalHargaBarang), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Sudah Dibayar", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                        Text(formatRupiah(totalSudahDibayar), style = MaterialTheme.typography.titleSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Total Pengeluaran", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    formatRupiah(totalPengeluaranCicilan),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Saldo Dana Cicilan", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    formatRupiah(saldoDanaCicilan),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (saldoDanaCicilan >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Total Sisa Cicilan", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    formatRupiah(totalSisaCicilan),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (totalSisaCicilan > 0.0) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
    
                    // Search field
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Cari Anggota Berdasarkan Nama", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cari",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFF3B82F6),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
    
                    // Header for member list
                    item {
                        Text(
                            text = "Daftar Cicilan Anggota",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
    
                    // Member item list
                    itemsIndexed(filteredAnggota) { index, anggota ->
                        val totalPaid = pembayaranList.filter { it.anggotaId == anggota.id && it.jenisPembayaran == "CICILAN" }.sumOf { it.nominal }
                        val sisaTagihan = anggota.hargaBarang - totalPaid
                        val sisaTagihanStr = if (sisaTagihan <= 0.0) "Lunas" else formatRupiah(sisaTagihan)
                        val sisaColor = if (sisaTagihan <= 0.0) Color(0xFF10B981) else Color(0xFFEF4444)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
                            ),
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
                                    Text(
                                        text = anggota.nama,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Total: ${formatRupiah(anggota.hargaBarang)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "|",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.2f)
                                        )
                                        Text(
                                            text = "Dibayar: ${formatRupiah(totalPaid)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Sisa Cicilan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sisaTagihanStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = sisaColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                }
            }

            // Card: GANTI PASSWORD MANUAL
            item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "🔑 Ganti Password Manual",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = passwordLama,
                                onValueChange = { passwordLama = it },
                                label = { Text("Password Lama", color = Color.White.copy(alpha = 0.6f)) },
                                visualTransformation = if (passwordLamaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { passwordLamaVisible = !passwordLamaVisible }) {
                                        Icon(
                                            imageVector = if (passwordLamaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = Color(0xFF3B82F6),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = passwordBaru,
                                onValueChange = { passwordBaru = it },
                                label = { Text("Password Baru", color = Color.White.copy(alpha = 0.6f)) },
                                visualTransformation = if (passwordBaruVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { passwordBaruVisible = !passwordBaruVisible }) {
                                        Icon(
                                            imageVector = if (passwordBaruVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = Color(0xFF3B82F6),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = konfirmasiPassword,
                                onValueChange = { konfirmasiPassword = it },
                                label = { Text("Konfirmasi Password Baru", color = Color.White.copy(alpha = 0.6f)) },
                                visualTransformation = if (konfirmasiVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { konfirmasiVisible = !konfirmasiVisible }) {
                                        Icon(
                                            imageVector = if (konfirmasiVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = Color(0xFF3B82F6),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = {
                                    if (passwordLama.isBlank() || passwordBaru.isBlank() || konfirmasiPassword.isBlank()) {
                                        Toast.makeText(context, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (passwordBaru != konfirmasiPassword) {
                                        Toast.makeText(context, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isSavingPassword = true
                                    viewModel.changePassword(passwordLama, passwordBaru) { success ->
                                        isSavingPassword = false
                                        if (success) {
                                            Toast.makeText(context, "Password berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                            passwordLama = ""
                                            passwordBaru = ""
                                            konfirmasiPassword = ""
                                        } else {
                                            Toast.makeText(context, "Gagal memperbarui password. Pastikan password lama benar.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                enabled = !isSavingPassword
                            ) {
                                if (isSavingPassword) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                } else {
                                    Text("Simpan Password Baru", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
