package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.network.UserData
import com.example.network.UserPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RbacSimulatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activeRole = remember { SessionManager.getRole(context).trim().uppercase() }
    val allowedRoles = remember { listOf("ADMIN", "BENDAHARA", "PENGURUS") }

    LaunchedEffect(Unit) {
        if (activeRole !in allowedRoles) {
            Toast.makeText(
                context,
                "Akses Terbatas untuk Pengurus & Admin!",
                Toast.LENGTH_SHORT
            ).show()
            onBackClick()
        }
    }

    // Enable system back and emulator back handler
    BackHandler {
        onBackClick()
    }

    var currentUserData by remember {
        mutableStateOf(
            UserData(
                idUser = 12,
                name = "Dikha Resnanda",
                role = "member",
                isVerified = true,
                permissions = UserPermissions(
                    canViewLaporan = true,
                    canEditKas = false,
                    canManageUsers = false
                )
            )
        )
    }

    var showGuestAlert by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserData.isVerified) {
        showGuestAlert = !currentUserData.isVerified
    }

    if (showGuestAlert) {
        AlertDialog(
            onDismissRequest = { showGuestAlert = false },
            title = { Text("Akses Terbatas", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Akun Anda belum diverifikasi oleh pengurus", color = Color.LightGray) },
            confirmButton = {
                Button(onClick = { showGuestAlert = false }) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Simulasi RBAC", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nebo Sukabumi - RBAC Dashboard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nama: ${currentUserData.name}", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Role: ${currentUserData.role.uppercase()}", color = Color.Cyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Status Verifikasi: ${if (currentUserData.isVerified) "VERIFIED" else "PENDING"}",
                        color = if (currentUserData.isVerified) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Simulasi Perubahan Role/Status:",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        currentUserData = currentUserData.copy(
                            role = "member",
                            isVerified = true,
                            permissions = UserPermissions(canViewLaporan = true, canEditKas = false, canManageUsers = false)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Member", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        currentUserData = currentUserData.copy(
                            role = "pengurus",
                            isVerified = true,
                            permissions = UserPermissions(canViewLaporan = true, canEditKas = true, canManageUsers = false)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pengurus", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        currentUserData = currentUserData.copy(
                            role = "admin",
                            isVerified = true,
                            permissions = UserPermissions(canViewLaporan = true, canEditKas = true, canManageUsers = true)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Admin", fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    currentUserData = currentUserData.copy(
                        isVerified = !currentUserData.isVerified
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (currentUserData.isVerified) "Set Belum Verified (Pending)" else "Set Sudah Verified")
            }

            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

            Text(
                text = "Hak Akses Fitur (Dinamis RBAC):",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )

            if (currentUserData.permissions.canViewLaporan) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Laporan Keuangan & Rekapitulasi Kas (Semua Role)")
                }
            }

            if (currentUserData.permissions.canEditKas) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Input & Edit Kas (Admin & Pengurus)")
                }
            }

            if (currentUserData.permissions.canManageUsers) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Kelola Anggota & Verifikasi (Admin Only)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol "← KEMBALI KE DASHBOARD" di bagian paling bawah
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "← KEMBALI KE DASHBOARD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}
