package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperPanelScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val apiStatus by DeveloperManager.apiStatusMap.collectAsState()
    val dbStatus by DeveloperManager.dbConnectionStatus.collectAsState()
    val systemLogs by DeveloperManager.developerLogs.collectAsState()
    
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var deleteType by remember { mutableStateOf("") }
    var deleteTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        DeveloperManager.checkAllApiEndpoints(coroutineScope)
        DeveloperManager.checkDbConnection(coroutineScope)
        DeveloperManager.loadDeveloperLogs(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("dev_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // SECTION: PENGATURAN KOMUNITAS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = "Pengaturan Komunitas", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pengaturan Komunitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { navController.navigate("identitas_komunitas") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ganti Logo Komunitas")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate("identitas_komunitas") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ganti Header Dashboard")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate("identitas_komunitas") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ganti Splash Screen")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate("identitas_komunitas") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ganti Background Login")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate("identitas_komunitas") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ganti Banner Profil")
                        }
                    }
                }
            }

            // SECTION: SISTEM
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = "Sistem", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sistem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Backup
                        Button(
                            onClick = {
                                DeveloperManager.backupDatabase(context, viewModel)
                                Toast.makeText(context, "Mengekspor Backup Database...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Backup Database")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Restore
                        Button(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Restore Database")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Sync
                        Button(
                            onClick = {
                                viewModel.syncData()
                                DeveloperManager.logDeveloperAction(context, "Sinkronisasi Ulang Database")
                                Toast.makeText(context, "Sinkronisasi ulang database berhasil dipicu!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Sinkronisasi Database")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { 
                                DeveloperManager.checkDbConnection(coroutineScope)
                                Toast.makeText(context, "Status Server: $dbStatus", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Status Server")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { 
                                DeveloperManager.checkAllApiEndpoints(coroutineScope)
                                Toast.makeText(context, "Mengecek Status API...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Status API")
                        }
                    }
                }
            }

            // SECTION: MANAJEMEN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = "Manajemen", tint = Color(0xFFC62828))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manajemen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        val dangerButtons = listOf(
                            Triple("transaksi", "Reset Data Transaksi", "transaksi"),
                            Triple("kas", "Reset Kas", "kas"),
                            Triple("cicilan", "Reset Cicilan", "cicilan"),
                            Triple("pengeluaran", "Reset Pengeluaran", "pengeluaran")
                        )

                        dangerButtons.forEach { (type, label, tag) ->
                            OutlinedButton(
                                onClick = {
                                    deleteType = type
                                    deleteTitle = label
                                    showConfirmDeleteDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFC62828)))
                            ) {
                                Text(label, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // SECTION: AUDIT // SECTION: TENTANG APLIKASI LOGIN LOGS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = "Log Aktivitas", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Aktivitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate("audit_log") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Audit Log")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate("login_log") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Login Log")
                        }
                    }
                }
            }

            // SECTION: TENTANG APLIKASI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Tentang Aplikasi", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tentang Aplikasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Versi APK", style = MaterialTheme.typography.bodyMedium)
                            Text("1.0.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Versi Database", style = MaterialTheme.typography.bodyMedium)
                            Text("v1.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Versi API", style = MaterialTheme.typography.bodyMedium)
                            Text("v1.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Restore dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Database") },
            text = {
                Column {
                    Text("Tempelkan data JSON hasil backup di bawah ini:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp).testTag("restore_json_input"),
                        placeholder = { Text("Paste JSON here...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = DeveloperManager.restoreDatabase(restoreJsonText, context, viewModel)
                        if (success) {
                            Toast.makeText(context, "Database Berhasil Direstore!", Toast.LENGTH_SHORT).show()
                            showRestoreDialog = false
                            restoreJsonText = ""
                        } else {
                            Toast.makeText(context, "Gagal merestore database. Periksa format JSON!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirm Deletion Dialog
    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Konfirmasi Penghapusan") },
            text = {
                Text("Apakah Anda benar-benar yakin ingin melakukan: \"$deleteTitle\"? Tindakan ini tidak dapat dibatalkan!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        val onCompleted: (Boolean) -> Unit = { success ->
                            if (success) {
                                Toast.makeText(context, "Berhasil: $deleteTitle", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal menghapus data dari server!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        when (deleteType) {
                            "transaksi" -> DeveloperManager.deleteDevAllTransaksi(context, viewModel, onCompleted)
                            "pembayaran" -> DeveloperManager.deleteDevAllPembayaran(context, viewModel, onCompleted)
                            "pengeluaran" -> DeveloperManager.deleteDevAllPengeluaran(context, viewModel, onCompleted)
                            "cicilan" -> DeveloperManager.deleteDevAllCicilan(context, viewModel, onCompleted)
                            "kas" -> DeveloperManager.deleteDevAllKas(context, viewModel, onCompleted)
                            "kas_anniversary" -> DeveloperManager.deleteDevAllKasAniv(context, viewModel, onCompleted)
                            "kas_keliling" -> DeveloperManager.deleteDevAllKasKeliling(context, viewModel, onCompleted)
                            "absensi" -> DeveloperManager.deleteDevAllAbsensi(context, onCompleted)
                            "catatan" -> DeveloperManager.deleteDevAllCatatan(context, viewModel, onCompleted)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}
