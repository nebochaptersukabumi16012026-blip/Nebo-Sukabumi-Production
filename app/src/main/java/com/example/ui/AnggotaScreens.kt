package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.data.Anggota
import com.example.data.Pembayaran
import com.example.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnggotaListScreen(navController: NavController, viewModel: CommunityViewModel) {
    val anggotaList by viewModel.allAnggota.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pembayaranList by viewModel.allPembayaran.collectAsState()
    val kasKelilingList by viewModel.allKasKeliling.collectAsState()
    val communitySettings by viewModel.communitySettings.collectAsState()
    val targetAniv = communitySettings.target_aniv

    val userRole by viewModel.loggedInUserRole.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.syncFromApi() }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.syncFromApi()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var filterStatus by remember { mutableStateOf("SEMUA") } // SEMUA, BELUM_KAS, BELUM_ANIV
    
    val filteredList = anggotaList.filter {
        when (filterStatus) {
            "SEMUA" -> true
            else -> true
        }
    }

    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_anggota"]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Anggota") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncFromApi() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Perbarui Data")
                    }
                    IconButton(onClick = { navController.navigate("daftar_hadir") }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Daftar Hadir Bulanan")
                    }
                }
            )
        },
        floatingActionButton = {
            if (userRole == "BENDAHARA" || userRole == "ADMIN" || userRole == "DEVELOPER") {
                FloatingActionButton(onClick = { navController.navigate("anggota_form?id=-1") }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Anggota")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            DynamicBackground(configStr = bgConfigStr)
            
            Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Cari (Nama, NRA, Alamat)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            
            Text(
                text = "Total Anggota Ditemukan: ${filteredList.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterStatus == "SEMUA",
                    onClick = { filterStatus = "SEMUA" },
                    label = { Text("Semua") }
                )
            }

            val isGuest = userRole == "GUEST"

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (anggotaList.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                items(filteredList) { anggota ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            navController.navigate("anggota_detail?id=${anggota.id}")
                        },
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (anggota.foto != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(anggota.foto),
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(anggota.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("NRA: ${anggota.nra}", style = MaterialTheme.typography.bodyMedium)
                                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (anggota.statusAktif) "Aktif" else "Nonaktif",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.background(
                                            color = if (anggota.statusAktif) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                            shape = RoundedCornerShape(24.dp)
                                        ).padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Kas: ${formatRupiah(anggota.uangKas)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text("Aniv: ${formatRupiah(anggota.iuranAniv)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

private fun Double.formatRupiahOrDash(): String = if (this == 0.0) "Rp 0" else formatRupiah(this)
private fun Int.formatIntOrDash(): String = if (this == 0) "-" else "$this bulan"

private fun formatRupiahNoDecimal(number: Double): String {
    val localeID = java.util.Locale("id", "ID")
    val formatRupiah = java.text.NumberFormat.getCurrencyInstance(localeID)
    formatRupiah.maximumFractionDigits = 0
    return formatRupiah.format(number)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnggotaDetailScreen(navController: NavController, viewModel: CommunityViewModel, memberId: Int) {
    val anggotaFlow = viewModel.getAnggotaById(memberId).collectAsState(initial = null)
    val anggota = anggotaFlow.value

    val allAnggota by viewModel.allAnggota.collectAsState(initial = emptyList())
    val allPembayaran by viewModel.allPembayaran.collectAsState()
    val kasKelilingList by viewModel.allKasKeliling.collectAsState()
    val communitySettings by viewModel.communitySettings.collectAsState()
    val targetAniv = communitySettings.target_aniv
    val targetKas = communitySettings.target_kas

    val userRole by viewModel.loggedInUserRole.collectAsState()
    val loggedInUserId by viewModel.loggedInUserId.collectAsState()
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<String?>(null) }
    var paymentToDelete by remember { mutableStateOf<Pembayaran?>(null) }
    var showDeleteAnggotaDialog by remember { mutableStateOf(false) }

    // Dynamic sync on opening any member
    androidx.compose.runtime.LaunchedEffect(memberId) {
        viewModel.syncFromApi()
    }
    
    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_anggota"]

    if (anggota == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Role-based permission check:
    val isDeveloper = userRole?.equals("DEVELOPER", ignoreCase = true) == true
    val isBendahara = userRole?.uppercase() in listOf("BENDAHARA", "ADMIN", "DEVELOPER")
    // HANYA DEVELOPER yang diizinkan input kas/aniv dan menghapus riwayat pembayaran
    val canInputKasAniv = isDeveloper
    val canDeleteTransaction = isDeveloper
    val isOwnProfile = loggedInUserId == anggota.id
    val isGuest = !isBendahara && !isOwnProfile

    // Dynamic extraction of all payments belonging to this specific member (by ID or Name)
    val memberPayments = remember(allPembayaran, anggota) {
        val direct = allPembayaran.filter { p ->
            p.anggotaId == anggota.id || 
            (anggota.nama.isNotBlank() && p.anggotaNama.trim().equals(anggota.nama.trim(), ignoreCase = true))
        }
        val list = direct.toMutableList()
        if (list.none { it.jenisPembayaran.equals("KAS", ignoreCase = true) } && anggota.uangKas > 0.0) {
            list.add(
                Pembayaran(
                    id = if (anggota.id > 0) -anggota.id else -1,
                    anggotaId = anggota.id,
                    anggotaNama = anggota.nama,
                    jenisPembayaran = "KAS",
                    nominal = anggota.uangKas,
                    tanggalBayar = System.currentTimeMillis(),
                    keterangan = "Pembayaran Uang Kas"
                )
            )
        }
        if (list.none { it.jenisPembayaran.equals("ANIV", ignoreCase = true) } && anggota.iuranAniv > 0.0) {
            list.add(
                Pembayaran(
                    id = if (anggota.id > 0) -(anggota.id + 100000) else -2,
                    anggotaId = anggota.id,
                    anggotaNama = anggota.nama,
                    jenisPembayaran = "ANIV",
                    nominal = anggota.iuranAniv,
                    tanggalBayar = System.currentTimeMillis(),
                    keterangan = "Iuran Anniversary"
                )
            )
        }
        list
    }

    val kasList = memberPayments.filter { it.jenisPembayaran.equals("KAS", ignoreCase = true) }
    val anivList = memberPayments.filter { it.jenisPembayaran.equals("ANIV", ignoreCase = true) }
    val cicilanList = memberPayments.filter { it.jenisPembayaran.equals("CICILAN", ignoreCase = true) }

    // Dynamic calculation of values
    val totalKasMember = if (kasList.isNotEmpty()) kasList.sumOf { it.nominal } else anggota.uangKas
    val totalAnivMember = if (anivList.isNotEmpty()) anivList.sumOf { it.nominal } else anggota.iuranAniv
    val totalCicilanPaid = cicilanList.sumOf { it.nominal }
    val sisaCicilanDynamic = if (anggota.hargaBarang > 0.0) maxOf(0.0, anggota.hargaBarang - totalCicilanPaid) else anggota.sisaCicilan

    val nomorUrut = (allAnggota.indexOfFirst { it.id == anggota.id } + 1).let { if (it > 0) it.toString() else "-" }

    if (isGuest) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Akses Terbatas") },
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
                        "Akses Dibatasi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Anda tidak diperbolehkan melihat detail data anggota lain. Halaman ini hanya tersedia untuk profil pribadi atau pengelola (Admin/Bendahara).",
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

    // Confirmation dialog for deleting payment history (Developer only)
    paymentToDelete?.let { p ->
        val isKas = p.jenisPembayaran.equals("KAS", ignoreCase = true)
        val isAniv = p.jenisPembayaran.equals("ANIV", ignoreCase = true)
        val labelTrx = if (isKas) "Kas" else if (isAniv) "Iuran Anniversary" else p.jenisPembayaran
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Hapus Transaksi $labelTrx", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Hapus transaksi $labelTrx sebesar ${formatRupiah(p.nominal)} milik \"${anggota.nama}\"?\n\n" +
                    "• Nominal ${if (isKas) "Total Uang Kas" else if (isAniv) "Iuran Anniversary" else "Cicilan"} anggota akan otomatis berkurang sebesar ${formatRupiah(p.nominal)}.\n" +
                    "• Total Saldo Kas di Dashboard akan otomatis dihitung ulang secara real-time."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = p
                        paymentToDelete = null
                        viewModel.deletePembayaran(toDelete)
                        Toast.makeText(context, "Transaksi $labelTrx sebesar ${formatRupiah(toDelete.nominal)} berhasil dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Check if installment data exists
    val hasCicilanData = anggota.hargaBarang > 0.0 || totalCicilanPaid > 0.0 || sisaCicilanDynamic > 0.0 || anggota.lamaCicilan > 0 || anggota.cicilanPerBulan > 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Anggota") },
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
                },
                actions = {
                    if (isBendahara || isOwnProfile) {
                        IconButton(onClick = { navController.navigate("anggota_form?id=${anggota.id}") }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            DynamicBackground(configStr = bgConfigStr)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. HERO HEADER BANNER (Community Photo in High Quality)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val bannerModel = BrandingResolver.getBannerModel(communitySettings.profile_banner)
                            coil.compose.AsyncImage(
                                model = bannerModel,
                                placeholder = painterResource(id = BrandingResolver.PROFILE_BANNER_RES),
                                error = painterResource(id = BrandingResolver.PROFILE_BANNER_RES),
                                fallback = painterResource(id = BrandingResolver.PROFILE_BANNER_RES),
                                contentDescription = "Header Komunitas Nebo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                            )
                            Text(
                                text = "KAS KOMUNITAS NEBO SUKABUMI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // 1. DATA PRIBADI CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "DATA PRIBADI",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isGuest) {
                                    if (anggota.foto != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(anggota.foto),
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                
                                Column {
                                    Text(anggota.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("NRA: ${anggota.nra}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Nomor Urut: $nomorUrut", style = MaterialTheme.typography.bodyMedium)
                                    
                                    if (!isGuest) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Alamat: ${anggota.alamat}", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("No HP: ${anggota.nomorTelepon}", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val sdf = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))
                                        Text("Tanggal Bergabung: ${sdf.format(java.util.Date(anggota.tanggalBergabung))}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. DATA KAS CARD
                if (!isGuest) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "DATA KAS",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Status Kas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (anggota.statusAktif) "Aktif" else "Tidak Aktif",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (anggota.statusAktif) Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Iuran Anniversary", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        formatRupiah(anggota.iuranAniv),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (anggota.iuranAniv > 0.0) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Uang Kas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        formatRupiah(anggota.uangKas),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (anggota.uangKas > 0.0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 3. RIWAYAT KAS (for Bendahara & Own Profile)
                    if (isBendahara || isOwnProfile) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Riwayat Kas (${kasList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (canInputKasAniv) {
                                    Button(
                                        onClick = { navController.navigate("pembayaran_form?anggotaId=${anggota.id}&jenis=KAS") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Bayar Kas", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        if (kasList.isEmpty()) {
                            item {
                                Text("Belum ada riwayat pembayaran kas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(kasList) { trx ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(), 
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(), 
                                        horizontalArrangement = Arrangement.SpaceBetween, 
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = formatRupiah(trx.nominal), 
                                                style = MaterialTheme.typography.titleMedium, 
                                                fontWeight = FontWeight.Bold, 
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = formatDate(trx.tanggalBayar), 
                                                style = MaterialTheme.typography.bodySmall, 
                                                color = Color(0xFF94A3B8)
                                            )
                                            if (!trx.keterangan.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(trx.keterangan, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (trx.buktiPembayaran != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = { selectedImage = trx.buktiPembayaran }, 
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), 
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Lihat Bukti", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (canDeleteTransaction) {
                                            IconButton(
                                                onClick = { paymentToDelete = trx },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete, 
                                                    contentDescription = "Hapus Transaksi Kas", 
                                                    tint = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Riwayat Iuran Anniversary (${anivList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (canInputKasAniv) {
                                    Button(
                                        onClick = { navController.navigate("pembayaran_form?anggotaId=${anggota.id}&jenis=ANIV") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Bayar Aniv", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        if (anivList.isEmpty()) {
                            item {
                                Text("Belum ada riwayat iuran anniversary.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(anivList) { trx ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(), 
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(), 
                                        horizontalArrangement = Arrangement.SpaceBetween, 
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = formatRupiah(trx.nominal), 
                                                style = MaterialTheme.typography.titleMedium, 
                                                fontWeight = FontWeight.Bold, 
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = formatDate(trx.tanggalBayar), 
                                                style = MaterialTheme.typography.bodySmall, 
                                                color = Color(0xFF94A3B8)
                                            )
                                            if (!trx.keterangan.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(trx.keterangan, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (trx.buktiPembayaran != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = { selectedImage = trx.buktiPembayaran }, 
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), 
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Lihat Bukti", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (canDeleteTransaction) {
                                            IconButton(
                                                onClick = { paymentToDelete = trx },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete, 
                                                    contentDescription = "Hapus Iuran Anniversary", 
                                                    tint = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. DATA CICILAN CARD & RIWAYAT
                if (!isGuest) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    if (isOwnProfile) "CICILAN SAYA" else "DATA CICILAN",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (isBendahara || hasCicilanData) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Harga Barang", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(anggota.hargaBarang.formatRupiahOrDash(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val sudahDibayar = if (anggota.hargaBarang > 0.0) totalCicilanPaid else (anggota.hargaBarang - anggota.sisaCicilan)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Sudah Dibayar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(sudahDibayar.formatRupiahOrDash(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Sisa Cicilan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(sisaCicilanDynamic.formatRupiahOrDash(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (sisaCicilanDynamic > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cicilan per Bulan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(anggota.cicilanPerBulan.formatRupiahOrDash(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Lama Cicilan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(anggota.lamaCicilan.formatIntOrDash(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Belum ada data cicilan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (isBendahara || (isOwnProfile && hasCicilanData)) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Riwayat Pembayaran Cicilan (${cicilanList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (isBendahara) {
                                    if (anggota.hargaBarang > 0.0 && sisaCicilanDynamic <= 0.0) {
                                        Text("LUNAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    } else {
                                        Button(
                                            onClick = { navController.navigate("pembayaran_form?anggotaId=${anggota.id}&jenis=CICILAN") },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp),
                                            enabled = anggota.hargaBarang > 0.0
                                        ) {
                                            Text("Bayar Cicilan", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }

                        if (cicilanList.isEmpty()) {
                            item {
                                Text("Belum ada riwayat pembayaran cicilan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(cicilanList) { trx ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(), 
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(), 
                                        horizontalArrangement = Arrangement.SpaceBetween, 
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = formatRupiah(trx.nominal), 
                                                style = MaterialTheme.typography.titleMedium, 
                                                fontWeight = FontWeight.Bold, 
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = formatDate(trx.tanggalBayar), 
                                                style = MaterialTheme.typography.bodySmall, 
                                                color = Color(0xFF94A3B8)
                                            )
                                            if (!trx.keterangan.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(trx.keterangan, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (trx.buktiPembayaran != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = { selectedImage = trx.buktiPembayaran }, 
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), 
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Lihat Bukti", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (canDeleteTransaction) {
                                            IconButton(
                                                onClick = { paymentToDelete = trx },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete, 
                                                    contentDescription = "Hapus Riwayat Cicilan", 
                                                    tint = Color(0xFFEF4444)
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
fun AnggotaFormScreen(navController: NavController, viewModel: CommunityViewModel, memberId: Int) {
    val anggota = if (memberId != -1) viewModel.getAnggotaById(memberId).collectAsState(initial = null).value else null

    var nama by remember { mutableStateOf("") }
    var nra by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var nomorTelepon by remember { mutableStateOf("") }
    var statusAktif by remember { mutableStateOf(true) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var hargaBarangStr by remember { mutableStateOf("") }
    var lamaCicilanStr by remember { mutableStateOf("") }
    
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    var hasInitialized by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        fotoUri = uri
    }

    LaunchedEffect(anggota) {
        if (anggota != null && !hasInitialized) {
            hasInitialized = true
            nama = anggota.nama
            nra = anggota.nra
            alamat = anggota.alamat
            nomorTelepon = anggota.nomorTelepon
            statusAktif = anggota.statusAktif
            hargaBarangStr = if (anggota.hargaBarang == 0.0) "" else anggota.hargaBarang.toInt().toString()
            lamaCicilanStr = if (anggota.lamaCicilan == 0) "" else anggota.lamaCicilan.toString()
            if (anggota.foto != null) {
                fotoUri = Uri.parse(anggota.foto)
            }
        }
    }

    val hargaBarangVal = hargaBarangStr.toDoubleOrNull() ?: 0.0
    val lamaCicilanVal = lamaCicilanStr.toIntOrNull() ?: 0

    val cicilanPerBulanVal = if (lamaCicilanVal > 0) hargaBarangVal / lamaCicilanVal else 0.0

    val paymentsState = if (memberId != -1) {
        viewModel.getPembayaranByAnggota(memberId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<com.example.data.Pembayaran>()) }
    }
    val payments = paymentsState.value
    val totalPaid = payments.filter { it.jenisPembayaran == "CICILAN" }.sumOf { it.nominal }
    val sisaCicilanVal = maxOf(0.0, hargaBarangVal - totalPaid)

    val cicilanPerBulanDisplay = if (hargaBarangStr.isEmpty() || lamaCicilanStr.isEmpty() || lamaCicilanVal <= 0) "" else formatRupiahNoDecimal(cicilanPerBulanVal)
    val sisaCicilanDisplay = if (hargaBarangStr.isEmpty()) "" else formatRupiahNoDecimal(sisaCicilanVal)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memberId == -1) "Tambah Anggota" else "Edit Anggota") },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (fotoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(fotoUri),
                        contentDescription = "Foto Anggota",
                        modifier = Modifier.size(120.dp).clip(CircleShape).clickable { galleryLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Pilih Foto")
                    }
                }
            }

            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = nra,
                onValueChange = { nra = it },
                label = { Text("NRA (Nomor Registrasi Anggota)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = alamat,
                onValueChange = { alamat = it },
                label = { Text("Alamat") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = nomorTelepon,
                onValueChange = { nomorTelepon = it },
                label = { Text("Nomor HP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = hargaBarangStr,
                onValueChange = { hargaBarangStr = it },
                label = { Text("Harga Barang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = lamaCicilanStr,
                onValueChange = { lamaCicilanStr = it },
                label = { Text("Lama Cicilan (bulan)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = cicilanPerBulanDisplay,
                onValueChange = {},
                readOnly = true,
                label = { Text("Cicilan per Bulan") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = sisaCicilanDisplay,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sisa Cicilan") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status Anggota Aktif")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = statusAktif, onCheckedChange = { statusAktif = it })
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (nama.isBlank() || nra.isBlank() || alamat.isBlank() || nomorTelepon.isBlank()) {
                        Toast.makeText(context, "Semua kolom wajib diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (hargaBarangVal > 0.0 && lamaCicilanVal < 1) {
                        Toast.makeText(context, "Lama Cicilan minimal 1 bulan jika Harga Barang diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    viewModel.saveAnggota(
                        id = memberId,
                        nama = nama,
                        nra = nra,
                        alamat = alamat,
                        nomorTelepon = nomorTelepon,
                        statusAktif = statusAktif,
                        foto = fotoUri?.toString(),
                        hargaBarang = hargaBarangVal,
                        totalCicilan = hargaBarangVal,
                        sisaCicilan = sisaCicilanVal,
                        lamaCicilan = lamaCicilanVal,
                        cicilanPerBulan = cicilanPerBulanVal,
                        totalTagihan = hargaBarangVal
                    ) { success, message ->
                        isSaving = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
