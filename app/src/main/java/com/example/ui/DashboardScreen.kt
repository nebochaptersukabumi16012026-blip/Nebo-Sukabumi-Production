package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import com.example.network.KasKelilingUnifiedResponse
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val settings by viewModel.communitySettings.collectAsState()
    val anggotaList by viewModel.allAnggota.collectAsState()
    val pembayaranList by viewModel.allPembayaran.collectAsState()
    val pengeluaranList by viewModel.allPengeluaran.collectAsState()
    val kasKelilingList by viewModel.allKasKeliling.collectAsState()
    val kasSummary by viewModel.kasKelilingSummary.collectAsState()

    val userRole by viewModel.loggedInUserRole.collectAsState()
    val userId by viewModel.loggedInUserId.collectAsState()
    val loggedInUserName by viewModel.loggedInUserName.collectAsState()
    val loggedInUserNra by viewModel.loggedInUserNra.collectAsState()

    val dashboardData by viewModel.dashboardData.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    LaunchedEffect(syncError) {
        syncError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSyncError()
        }
    }
    
    val currentMonthYear = remember {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
    }

    LaunchedEffect(Unit) {
        viewModel.syncFromApi()
    }

    // Prioritize values from API dashboardData for consistency as requested
    
    
    
    
    
    // THE UNIFIED SALDO KAS
    

    // CALCULATE KAS UTAMA - Dinamis Real-Time dari Tabel Anggota & API Dashboard
    val totalPemasukanKas: Double = dashboardData?.kas_utama?.total_pemasukan ?: dashboardData?.total_kas ?: anggotaList.sumOf { it.uangKas }
    val kasUtamaPengeluaranList = pengeluaranList.filter {
        val jk = it.jenisKas.trim().lowercase()
        (jk in listOf("kas_utama", "kas utama", "kas", "saldo kas", "uang kas", "uang_kas")) ||
        (!jk.contains("keliling") && !jk.contains("aniv") && !jk.contains("anniversary") && !jk.contains("cicilan") && jk.isNotEmpty())
    }
    val totalPengeluaranSum: Double = kasUtamaPengeluaranList.sumOf { it.nominal }
    val totalPengeluaranKas: Double = if (totalPengeluaranSum > 0.0) totalPengeluaranSum else (dashboardData?.kas_utama?.total_pengeluaran ?: dashboardData?.totalPengeluaran ?: 0.0)
    val saldoKasAkhirUtama: Double = dashboardData?.kas_utama?.saldo_kas?.let { maxOf(0.0, it) } ?: dashboardData?.saldo_kas?.let { maxOf(0.0, it) } ?: maxOf(0.0, totalPemasukanKas - totalPengeluaranKas)
    val saldoKasColor = Color(0xFF22C55E)
    
    // CALCULATE KAS KELILING (Source of Truth from API Summary or List)
    val grandTotalPemasukanKK = kasSummary?.total_pemasukan ?: kasKelilingList.sumOf { it.totalPemasukan }
    val grandTotalPengeluaranKK = kasSummary?.total_pengeluaran ?: kasKelilingList.sumOf { it.totalPengeluaran }
    val saldoKasKelilingFinal = kasSummary?.saldo_kas_keliling ?: (grandTotalPemasukanKK - grandTotalPengeluaranKK)

    val targetKas = settings.target_kas
    val targetAniv = settings.target_aniv
    val belumBayarKas = dashboardData?.belum_kas ?: dashboardData?.belum_bayar_kas ?: 0
    val belumBayarAniv = dashboardData?.belum_anniversary ?: dashboardData?.belum_bayar_aniv ?: 0
    val totalAnggota = dashboardData?.total_anggota ?: 0
    val totalSisaCicilan = anggotaList.sumOf { it.sisaCicilan }
    val totalHargaBarang = anggotaList.sumOf { it.hargaBarang }
    
    val totalPengeluaranAllStr = formatRupiah(totalPengeluaranKas)
    val saldoKasUnifiedStr = formatRupiah(saldoKasAkhirUtama)
    val totalKasInStr = formatRupiah(totalPemasukanKas)
    
    val actualAniv = dashboardData?.iuran_anniversary ?: dashboardData?.iuran_aniv ?: dashboardData?.total_aniv ?: 0.0
    val totalAnivStr = if (dashboardData != null) formatRupiah(actualAniv) else "Memuat..."
    
    val totalKasKelilingSaldoStr = formatRupiah(saldoKasKelilingFinal)
    
    val actualSisaCicilan = dashboardData?.total_sisa_cicilan ?: 0.0
    val totalSisaCicilanStr = if (dashboardData != null) formatRupiah(actualSisaCicilan) else "Memuat..."


    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_dashboard"]

    val showAnggota by viewModel.showCardAnggota.collectAsState()
    val showUangKas by viewModel.showCardUangKas.collectAsState()
    val showIuranAniv by viewModel.showCardIuranAniv.collectAsState()
    val showBelumKas by viewModel.showCardBelumKas.collectAsState()
    val showBelumAniv by viewModel.showCardBelumAniv.collectAsState()
    val showGrafik by viewModel.showCardGrafik.collectAsState()

    val headerBgType by viewModel.headerBgType.collectAsState()
    val headerSolidColor by viewModel.headerSolidColor.collectAsState()
    val headerGradientColors by viewModel.headerGradientColors.collectAsState()
    val headerBgImageUri by viewModel.headerBgImageUri.collectAsState()
    val headerTextColor by viewModel.headerTextColor.collectAsState()
    val headerFontSize by viewModel.headerFontSize.collectAsState()

    val customTextColor = headerTextColor
    val customFontSize = headerFontSize

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Permanent Community Photo as banner background

                val bannerModel = BrandingResolver.getBannerModel(settings.community_banner)
                AsyncImage(
                    model = bannerModel,
                    placeholder = painterResource(id = BrandingResolver.HEADER_DASHBOARD_RES),
                    error = painterResource(id = BrandingResolver.HEADER_DASHBOARD_RES),
                    fallback = painterResource(id = BrandingResolver.HEADER_DASHBOARD_RES),
                    contentDescription = "Banner Komunitas",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark premium gradient overlay for high readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Header content overlay
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Small premium logo next to welcome text
                        Card(
                            modifier = Modifier
                                .size(70.dp)
                                .padding(2.dp)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFE53935), Color(0xFF2196F3))
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {

                            val logoModel = BrandingResolver.getLogoModel(settings.community_logo)
                            AsyncImage(
                                model = logoModel,
                                placeholder = painterResource(id = BrandingResolver.LOGO_RES),
                                error = painterResource(id = BrandingResolver.LOGO_RES),
                                fallback = painterResource(id = BrandingResolver.LOGO_RES),
                                contentDescription = "Logo Mini",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                "Selamat Datang,", 
                                style = MaterialTheme.typography.titleSmall, 
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            val currentUser = anggotaList.find { it.id == userId } 
                                ?: anggotaList.find { !loggedInUserNra.isNullOrBlank() && it.nra.equals(loggedInUserNra, ignoreCase = true) }
                            
                            val displayName = currentUser?.nama 
                                ?: loggedInUserName 
                                ?: when (userRole) {
                                    "ANGGOTA" -> "Anggota"
                                    "BENDAHARA" -> "Admin Bendahara"
                                    "ADMIN" -> "Admin"
                                    "DEVELOPER" -> "Developer"
                                    else -> "Anggota Nebo"
                                }
                            
                            Text(
                                displayName, 
                                style = MaterialTheme.typography.titleLarge, 
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (userRole != "GUEST") {
                                val displayNra = currentUser?.nra 
                                    ?: loggedInUserNra 
                                    ?: when (userRole) {
                                        "DEVELOPER" -> "DEV-001"
                                        "ADMIN" -> "ADM-001"
                                        "BENDAHARA" -> "BDH-001"
                                        else -> "-"
                                    }
                                Text(
                                    "NRA: $displayNra",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (userRole == "ADMIN" || userRole == "DEVELOPER") {
                            var showEditHeaderDialog by remember { mutableStateOf(false) }

                            IconButton(
                                onClick = { showEditHeaderDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Header",
                                    tint = Color.White
                                )
                            }

                            if (showEditHeaderDialog) {
                                EditHeaderDialog(
                                    viewModel = viewModel,
                                    onDismiss = { showEditHeaderDialog = false }
                                )
                            }
                        }
                        
                        var showLogoutDialog by remember { mutableStateOf(false) }
                        
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Keluar",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logout", style = MaterialTheme.typography.labelMedium)
                        }
                        
                        if (showLogoutDialog) {
                            AlertDialog(
                                onDismissRequest = { showLogoutDialog = false },
                                title = { Text("Konfirmasi Keluar") },
                                text = { Text("Apakah Anda yakin ingin keluar dari aplikasi?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showLogoutDialog = false
                                        viewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = true
                                            }
                                        }
                                    }) {
                                        Text("Ya, Keluar", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showLogoutDialog = false }) {
                                        Text("Batal")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            val serverStatus by viewModel.serverStatus.collectAsState()
            val lastSyncTime by viewModel.lastSyncTime.collectAsState()
            val resetRequests by viewModel.resetRequests.collectAsState()
            val pendingRequestsCount = resetRequests.count { it.status == "Menunggu Persetujuan Admin" }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

            if (pendingRequestsCount > 0 && (userRole == "ADMIN" || userRole == "BENDAHARA" || userRole == "DEVELOPER")) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable {
                                navController.navigate("reset_password_requests")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Peringatan",
                                tint = Color(0xFFFF1744),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Permintaan Reset Password",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Ada $pendingRequestsCount permintaan baru yang menunggu persetujuan Anda.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
            
            item {
                ServerStatusCard(status = serverStatus, lastSyncTime = lastSyncTime)
            }
            
            item {
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                var isSyncing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSyncing = true
                            try {
                                viewModel.syncFromApiSuspend()
                                Toast.makeText(context, "Sinkronisasi data berhasil", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal sinkronisasi: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSyncing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mensinkronkan...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SINKRONISASI DATA (REAL-TIME)")
                    }
                }
            }

            // ========================================================
            // 2. CARD LAPORAN KEUANGAN (REKAPITULASI KAS)
            // ========================================================
            item {
                Text(
                    text = "📊 Laporan Rekapitulasi Kas",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Card 1: REKAPITULASI KAS KELILING (kas_keliling.php)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { navController.navigate("kas_keliling") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF059669).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "REKAPITULASI KAS KELILING",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Sumber: kas_keliling.php",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.LightGray.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Pemasukan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRupiah(grandTotalPemasukanKK),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Pengeluaran",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRupiah(grandTotalPengeluaranKK),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF87171)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Saldo Akhir Kas Keliling Highlight Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF065F46), Color(0xFF047857))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saldo Kas Keliling",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    text = totalKasKelilingSaldoStr,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: REKAPITULASI KAS ANNIVERSARY (iuran_anniversary.php)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { navController.navigate("detail_iuran_aniv") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF7C3AED).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "REKAPITULASI KAS ANNIVERSARY",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Sumber: iuran_anniversary.php",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.LightGray.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target / Anggota",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRupiah(targetAniv),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFBBF24)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Belum Bayar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$belumBayarAniv Anggota",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF87171)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Saldo Terkumpul Anniversary Highlight Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF5B21B6), Color(0xFF6D28D9))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Iuran Terkumpul",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    text = totalAnivStr,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color(0xFFDDD6FE)
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 3. KARTU GRID INDIKATOR UTAMA (2-Column Grid)
            // ========================================================
            item {
                Text(
                    text = "📌 Indikator & Menu Utama",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val gridCards = listOf<@Composable () -> Unit>(
                // 1. Belum Bayar Kas (Tunggakan Kas)
                {
                    StatCard(
                        title = "Belum Bayar Kas",
                        value = "$belumBayarKas Orang",
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("detail_belum_kas") },
                        valueColor = Color(0xFFEF4444),
                        subtitle = "Tunggakan Kas"
                    )
                },
                // 2. Belum Bayar Aniv (Tunggakan Aniv)
                {
                    StatCard(
                        title = "Belum Bayar Aniv",
                        value = "$belumBayarAniv Orang",
                        icon = Icons.Default.PendingActions,
                        iconTint = Color(0xFFF59E0B),
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("detail_belum_aniv") },
                        valueColor = Color(0xFFF59E0B),
                        subtitle = "Tunggakan Aniv"
                    )
                },
                // 3. Total Pengeluaran (pengeluaran.php murni)
                {
                    StatCard(
                        title = "Total Pengeluaran",
                        value = totalPengeluaranAllStr,
                        icon = Icons.Default.ShoppingCart,
                        iconTint = Color(0xFFF43F5E),
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("detail_total_pengeluaran") },
                        valueColor = Color(0xFFF43F5E),
                        subtitle = "Pengeluaran Murni"
                    )
                },
                // 4. Saldo Kas
                {
                    StatCard(
                        title = "Saldo Kas",
                        value = saldoKasUnifiedStr,
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = saldoKasColor,
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("detail_uang_kas") },
                        valueColor = saldoKasColor,
                        subtitle = "Sisa Saldo Kas"
                    )
                }
            )

            val chunkedGrid = gridCards.chunked(2)
            chunkedGrid.forEach { rowCards ->
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        rowCards.forEach { card ->
                            Box(modifier = Modifier.weight(1f)) {
                                card()
                            }
                        }
                    }
                }
            }

            // Catatan Komunitas (Full-Width Action Card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .clickable { navController.navigate("catatan_bebas") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF06B6D4).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Catatan Komunitas",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF06B6D4)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Catatan Komunitas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tulis & buka catatan bebas kegiatan komunitas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (userRole == "BENDAHARA" || userRole == "ADMIN" || userRole == "DEVELOPER") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable { navController.navigate("pengeluaran_kas") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Pengeluaran Kas",
                                modifier = Modifier.size(28.dp),
                                tint = Color(0xFFF43F5E)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Kelola Pengeluaran Kas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Tambah dan kelola daftar transaksi pengeluaran (pengeluaran.php)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
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

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    subtitle: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200)
    )

    Card(
        modifier = Modifier
            .height(145.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF1E293B)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = iconTint
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        ),
                        color = Color.LightGray.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
            )
        }
    }
}

fun safeParseColor(hexStr: String, default: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexStr))
    } catch (e: Exception) {
        default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHeaderDialog(
    viewModel: CommunityViewModel,
    onDismiss: () -> Unit
) {
    val currentType by viewModel.headerBgType.collectAsState()
    val currentSolidColor by viewModel.headerSolidColor.collectAsState()
    val currentGradientColors by viewModel.headerGradientColors.collectAsState()
    val currentBgImageUri by viewModel.headerBgImageUri.collectAsState()
    val currentTextColor by viewModel.headerTextColor.collectAsState()
    val currentFontSize by viewModel.headerFontSize.collectAsState()

    var selectedType by remember { mutableStateOf(currentType) }
    var selectedSolidColor by remember { mutableStateOf(currentSolidColor) }
    var selectedGradientColors by remember { mutableStateOf(currentGradientColors) }
    var selectedBgImageUri by remember { mutableStateOf(currentBgImageUri) }
    var selectedTextColor by remember { mutableStateOf(currentTextColor) }
    var selectedFontSize by remember { mutableStateOf(currentFontSize) }

    var customSolidInput by remember { mutableStateOf(selectedSolidColor) }
    var customStartInput by remember { mutableStateOf(selectedGradientColors.getOrNull(0) ?: "#3F51B5") }
    var customEndInput by remember { mutableStateOf(selectedGradientColors.getOrNull(1) ?: "#2196F3") }
    var customTextInput by remember { mutableStateOf(selectedTextColor) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedBgImageUri = uri.toString()
        }
    }

    val presetSolidColors = listOf(
        "#3F51B5" to "Royal Blue",
        "#1E88E5" to "Blue",
        "#009688" to "Teal",
        "#4CAF50" to "Green",
        "#FF9800" to "Orange",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple",
        "#212121" to "Slate Dark",
        "#D32F2F" to "Red"
    )

    val presetGradients = listOf(
        listOf("#3F51B5", "#2196F3") to "Ocean Blue",
        listOf("#FF4E50", "#F9D423") to "Sunset Glow",
        listOf("#11998E", "#38EF7D") to "Forest Mint",
        listOf("#141E30", "#243B55") to "Midnight Royal",
        listOf("#8E24AA", "#FF1744") to "Cosmic Berry",
        listOf("#0F2027", "#203A43", "#2C5364") to "Deep Charcoal"
    )

    val presetTextColors = listOf(
        "#FFFFFF" to "Putih",
        "#E0E0E0" to "Abu",
        "#FFE082" to "Kuning",
        "#80DEEA" to "Cyan",
        "#212121" to "Hitam"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Kustomisasi Header Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Background Type Selection
                Text("Tipe Background", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "solid" to "Warna Solid",
                        "gradient" to "Gradient",
                        "image" to "Gambar Galeri"
                    ).forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Button(
                            onClick = { selectedType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }

                Divider()

                // Section 2: Specific Customizations based on Type
                when (selectedType) {
                    "solid" -> {
                        Text("Pilih Warna Solid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(presetSolidColors) { (hex, name) ->
                                val color = safeParseColor(hex, Color.Gray)
                                val isSelected = selectedSolidColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedSolidColor = hex
                                            customSolidInput = hex
                                        }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customSolidInput,
                            onValueChange = {
                                customSolidInput = it
                                if (it.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
                                    selectedSolidColor = it
                                }
                            },
                            label = { Text("Custom Hex Warna Solid") },
                            placeholder = { Text("#3F51B5") },
                            isError = !customSolidInput.matches(Regex("^#[0-9a-fA-F]{6}$")),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    "gradient" -> {
                        Text("Pilih Preset Gradient", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            presetGradients.chunked(3).forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPresets.forEach { (colorsList, name) ->
                                        val isSelected = selectedGradientColors == colorsList
                                        val brush = Brush.horizontalGradient(colorsList.map { safeParseColor(it, Color.Gray) })
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(brush)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                                .clickable {
                                                    selectedGradientColors = colorsList
                                                    customStartInput = colorsList.getOrNull(0) ?: "#3F51B5"
                                                    customEndInput = colorsList.getOrNull(1) ?: "#2196F3"
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text("Custom Gradient", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customStartInput,
                                onValueChange = {
                                    customStartInput = it
                                    if (it.matches(Regex("^#[0-9a-fA-F]{6}$")) && customEndInput.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
                                        selectedGradientColors = listOf(it, customEndInput)
                                    }
                                },
                                label = { Text("Warna Mulai") },
                                isError = !customStartInput.matches(Regex("^#[0-9a-fA-F]{6}$")),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customEndInput,
                                onValueChange = {
                                    customEndInput = it
                                    if (it.matches(Regex("^#[0-9a-fA-F]{6}$")) && customStartInput.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
                                        selectedGradientColors = listOf(customStartInput, it)
                                    }
                                },
                                label = { Text("Warna Akhir") },
                                isError = !customEndInput.matches(Regex("^#[0-9a-fA-F]{6}$")),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                    "image" -> {
                        Text("Pilih Gambar Header", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedBgImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedBgImageUri),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("Belum memilih gambar")
                            }
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Pilih Gambar dari Galeri")
                        }
                    }
                }

                Divider()

                // Section 3: Text Customization
                Text("Pengaturan Teks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                // Color presets for text
                Text("Warna Teks", style = MaterialTheme.typography.bodyMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(presetTextColors) { (hex, name) ->
                        val color = safeParseColor(hex, Color.Gray)
                        val isSelected = selectedTextColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedTextColor = hex
                                    customTextInput = hex
                                }
                        )
                    }
                }

                OutlinedTextField(
                    value = customTextInput,
                    onValueChange = {
                        customTextInput = it
                        if (it.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
                            selectedTextColor = it
                        }
                    },
                    label = { Text("Custom Hex Warna Teks") },
                    placeholder = { Text("#FFFFFF") },
                    isError = !customTextInput.matches(Regex("^#[0-9a-fA-F]{6}$")),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Slider for text size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ukuran Teks", style = MaterialTheme.typography.bodyMedium)
                    Text("${selectedFontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = selectedFontSize,
                    onValueChange = { selectedFontSize = it },
                    valueRange = 18f..38f,
                    steps = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateHeaderConfig(
                        bgType = selectedType,
                        solidColor = selectedSolidColor,
                        gradientColors = selectedGradientColors,
                        bgImageUri = selectedBgImageUri,
                        textColor = selectedTextColor,
                        fontSize = selectedFontSize
                    )
                    onDismiss()
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ServerStatusCard(status: ServerStatus, lastSyncTime: String) {
    val bgColor = when (status) {
        ServerStatus.ONLINE -> Color(0xFFE8F5E9) // Light Green
        ServerStatus.OFFLINE -> Color(0xFFFFEBEE) // Light Pink
        ServerStatus.NO_INTERNET -> Color(0xFFFFFDE7) // Light Yellow
        ServerStatus.CHECKING -> Color(0xFFF5F5F5) // Light Gray
    }
    
    val iconColor = when (status) {
        ServerStatus.ONLINE -> Color(0xFF4CAF50)
        ServerStatus.OFFLINE -> Color(0xFFF44336)
        ServerStatus.NO_INTERNET -> Color(0xFFFFC107)
        ServerStatus.CHECKING -> Color.Gray
    }

    val title = when (status) {
        ServerStatus.ONLINE -> "🟢 Server Online"
        ServerStatus.OFFLINE -> "🔴 Server Offline"
        ServerStatus.NO_INTERNET -> "📶 Tidak Ada Internet"
        ServerStatus.CHECKING -> "⏳ Mengecek Status..."
    }

    val description = when (status) {
        ServerStatus.ONLINE -> "Terhubung ke nebosukabumi.net\nSinkron terakhir: $lastSyncTime"
        ServerStatus.OFFLINE -> "Tidak dapat terhubung ke server.\nPeriksa koneksi internet atau hubungi Administrator."
        ServerStatus.NO_INTERNET -> "Silakan sambungkan internet untuk sinkronisasi data."
        ServerStatus.CHECKING -> "Sedang memverifikasi koneksi..."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
