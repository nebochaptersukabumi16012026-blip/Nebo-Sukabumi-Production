package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeboSukabumiApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeboSukabumiApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1E3A8A),
            secondary = Color(0xFF0D9488),
            background = Color(0xFFF8FAFC),
            surface = Color.White
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "NEBO SUKABUMI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                "Manajemen Kas & Komunitas",
                                fontSize = 12.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E3A8A)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Kas") },
                        label = { Text("Kas") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("nav_kas")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Anggota") },
                        label = { Text("Anggota") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("nav_anggota")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Laporan") },
                        label = { Text("Laporan") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.testTag("nav_laporan")
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8FAFC))
            ) {
                when (selectedTab) {
                    0 -> DashboardView()
                    1 -> KasView()
                    2 -> AnggotaView()
                    3 -> LaporanView()
                }
            }
        }
    }
}

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount).replace("Rp", "Rp ")
}

@Composable
fun DashboardView() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                modifier = Modifier.fillMaxWidth().testTag("card_total_saldo")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Total Saldo Komunitas",
                        color = Color(0xFF93C5FD),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatRupiah(35250000.0),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sinkronisasi Realtime Terverifikasi",
                        color = Color(0xFF86EFAC),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text(
                "Ringkasan Saldo",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Kas Utama",
                    amount = formatRupiah(18500000.0),
                    icon = Icons.Default.AccountBalance,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f).testTag("card_kas_utama")
                )
                MetricCard(
                    title = "Kas Keliling",
                    amount = formatRupiah(6250000.0),
                    icon = Icons.Default.DirectionsCar,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f).testTag("card_kas_keliling")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Anniversary",
                    amount = formatRupiah(8500000.0),
                    icon = Icons.Default.Celebration,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f).testTag("card_anniversary")
                )
                MetricCard(
                    title = "Sisa Cicilan",
                    amount = formatRupiah(2000000.0),
                    icon = Icons.Default.CreditCard,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f).testTag("card_cicilan")
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontSize = 12.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(amount, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
    }
}

@Composable
fun KasView() {
    val sampleKas = listOf(
        "Iuran Kas Rutin - Bulan Oktober" to 50000.0,
        "Kas Keliling Kopdar Sukabumi" to 75000.0,
        "Iuran Anniversary Ke-5" to 100000.0
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Transaksi Kas Terbaru", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(sampleKas) { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("item_kas")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.first, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Terverifikasi & Sinkron", fontSize = 12.sp, color = Color(0xFF10B981))
                    }
                    Text(
                        formatRupiah(item.second),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                }
            }
        }
    }
}

@Composable
fun AnggotaView() {
    val sampleAnggota = listOf(
        Triple("Ahmad Fauzi", "NRA-001", "Aktif"),
        Triple("Budi Santoso", "NRA-002", "Aktif"),
        Triple("Cecep Supriatna", "NRA-003", "Aktif")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Daftar Anggota Komunitas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(sampleAnggota) { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("item_anggota")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.first, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("NRA: ${item.second}", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(item.third) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFDCFCE7),
                            labelColor = Color(0xFF166534)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LaporanView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Laporan Keuangan Terpadu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("laporan_rekap")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ringkasan Neraca", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pemasukan:", color = Color(0xFF64748B))
                        Text(formatRupiah(42000000.0), fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pengeluaran:", color = Color(0xFF64748B))
                        Text(formatRupiah(6750000.0), fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saldo Akhir:", fontWeight = FontWeight.Bold)
                        Text(formatRupiah(35250000.0), fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    }
                }
            }
        }
    }
}
