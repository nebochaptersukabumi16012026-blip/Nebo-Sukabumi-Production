package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GantiPasswordScreen(
    idAnggotaAktif: Int,
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit
) {
    var passwordLama by remember { mutableStateOf("") }
    var passwordBaru by remember { mutableStateOf("") }
    var confirmPasswordBaru by remember { mutableStateOf("") }
    
    var passwordLamaVisible by remember { mutableStateOf(false) }
    var passwordBaruVisible by remember { mutableStateOf(false) }
    var confirmPasswordBaruVisible by remember { mutableStateOf(false) }
    
    var statusMessage by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color.Gray) }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ganti Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color.White
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color.Black)
                    )
                )
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEB1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF00BFA5)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Ganti Password Profil",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                    
                    Text(
                        text = "Masukkan password lama dan password baru Anda",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // OutlinedTextField 1: Password Lama
                    OutlinedTextField(
                        value = passwordLama,
                        onValueChange = { passwordLama = it },
                        label = { Text("Password Lama", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordLamaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordLamaVisible = !passwordLamaVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.LightGray
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordLamaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("password_lama_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                            focusedLabelColor = Color(0xFF00BFA5),
                            unfocusedLabelColor = Color.LightGray.copy(alpha = 0.7f),
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLeadingIconColor = Color(0xFF00BFA5),
                            unfocusedLeadingIconColor = Color.LightGray.copy(alpha = 0.7f),
                            focusedContainerColor = Color(0x221E293B),
                            unfocusedContainerColor = Color(0x111E293B)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // OutlinedTextField 2: Password Baru
                    OutlinedTextField(
                        value = passwordBaru,
                        onValueChange = { passwordBaru = it },
                        label = { Text("Password Baru", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordBaruVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordBaruVisible = !passwordBaruVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.LightGray
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordBaruVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("password_baru_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                            focusedLabelColor = Color(0xFF00BFA5),
                            unfocusedLabelColor = Color.LightGray.copy(alpha = 0.7f),
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLeadingIconColor = Color(0xFF00BFA5),
                            unfocusedLeadingIconColor = Color.LightGray.copy(alpha = 0.7f),
                            focusedContainerColor = Color(0x221E293B),
                            unfocusedContainerColor = Color(0x111E293B)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // OutlinedTextField 3: Konfirmasi Password Baru
                    OutlinedTextField(
                        value = confirmPasswordBaru,
                        onValueChange = { confirmPasswordBaru = it },
                        label = { Text("Konfirmasi Password Baru", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            val image = if (confirmPasswordBaruVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { confirmPasswordBaruVisible = !confirmPasswordBaruVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.LightGray
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (confirmPasswordBaruVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_password_baru_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                            focusedLabelColor = Color(0xFF00BFA5),
                            unfocusedLabelColor = Color.LightGray.copy(alpha = 0.7f),
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLeadingIconColor = Color(0xFF00BFA5),
                            unfocusedLeadingIconColor = Color.LightGray.copy(alpha = 0.7f),
                            focusedContainerColor = Color(0x221E293B),
                            unfocusedContainerColor = Color(0x111E293B)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Tombol Perbarui Password
                    Button(
                        onClick = {
                            if (passwordLama.isBlank() || passwordBaru.isBlank() || confirmPasswordBaru.isBlank()) {
                                statusMessage = "Harap isi semua kolom password!"
                                statusColor = Color(0xFFEF4444)
                                return@Button
                            }
                            if (passwordBaru != confirmPasswordBaru) {
                                statusMessage = "Password baru dan konfirmasi tidak cocok!"
                                statusColor = Color(0xFFEF4444)
                                return@Button
                            }
                            if (passwordBaru.length < 4) {
                                statusMessage = "Password baru minimal 4 karakter!"
                                statusColor = Color(0xFFEF4444)
                                return@Button
                            }
                            
                            isProcessing = true
                            statusMessage = "Sedang memproses..."
                            statusColor = Color.LightGray
                            
                            viewModel.prosesGantiPassword(
                                idAnggota = idAnggotaAktif,
                                passLama = passwordLama,
                                passBaru = passwordBaru
                            ) { result ->
                                isProcessing = false
                                statusMessage = result
                                if (result.contains("Sukses", ignoreCase = true) || result.contains("berhasil", ignoreCase = true)) {
                                    statusColor = Color(0xFF10B981)
                                    passwordLama = ""
                                    passwordBaru = ""
                                    confirmPasswordBaru = ""
                                } else {
                                    statusColor = Color(0xFFEF4444)
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_ganti_password_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BFA5),
                            disabledContainerColor = Color(0xFF00BFA5).copy(alpha = 0.5f)
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Simpan Password Baru",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                    
                    // Status Notification Text
                    if (statusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusMessage,
                            color = statusColor,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
