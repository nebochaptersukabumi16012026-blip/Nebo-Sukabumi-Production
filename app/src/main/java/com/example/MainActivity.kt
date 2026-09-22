package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.UserData
import com.example.network.UserPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF3B82F6),
                    surface = Color(0xFF0F172A),
                    background = Color(0xFF020617)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Demo RBAC State
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

                    MainAppContent(
                        userData = currentUserData,
                        onRoleChanged = { newRole, isVerif ->
                            currentUserData = UserData(
                                idUser = 12,
                                name = "Dikha Resnanda",
                                role = newRole,
                                isVerified = isVerif,
                                permissions = UserPermissions(
                                    canViewLaporan = true,
                                    canEditKas = isVerif && (newRole == "admin" || newRole == "pengurus"),
                                    canManageUsers = isVerif && (newRole == "admin")
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    userData: UserData,
    onRoleChanged: (String, Boolean) -> Unit
) {
    var showGuestAlert by remember { mutableStateOf(false) }

    // Synchronize Dialog state with is_verified
    LaunchedEffect(userData.isVerified) {
        if (!userData.isVerified) {
            showGuestAlert = true
        } else {
            showGuestAlert = false
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nama: ${userData.name}", color = Color.White)
                Text("Role: ${userData.role.uppercase()}", color = Color.Cyan)
                Text(
                    "Status Verifikasi: ${if (userData.isVerified) "VERIFIED" else "PENDING"}",
                    color = if (userData.isVerified) Color.Green else Color.Red
                )
            }
        }

        Text("Simulasi Perubahan Role/Status:", fontWeight = FontWeight.SemiBold, color = Color.LightGray)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { onRoleChanged("member", true) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Member", fontSize = 12.sp)
            }
            Button(
                onClick = { onRoleChanged("pengurus", true) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Pengurus", fontSize = 12.sp)
            }
            Button(
                onClick = { onRoleChanged("admin", true) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Admin", fontSize = 12.sp)
            }
        }

        Button(
            onClick = { onRoleChanged("member", false) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Belum Verified (Pending)")
        }

        Divider(color = Color.Gray, thickness = 1.dp)

        Text("Hak Akses Fitur (Dinamis RBAC):", fontWeight = FontWeight.Bold, color = Color.White)

        if (userData.permissions.canViewLaporan) {
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Laporan Keuangan & Rekapitulasi Kas (Semua Role)")
            }
        }

        if (userData.permissions.canEditKas) {
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Input & Edit Kas (Admin & Pengurus)")
            }
        }

        if (userData.permissions.canManageUsers) {
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text("Kelola Anggota & Verifikasi (Admin Only)")
            }
        }
    }
}
