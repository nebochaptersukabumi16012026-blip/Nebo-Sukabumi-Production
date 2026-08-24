package com.example.ui

import coil.compose.AsyncImage

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, viewModel: CommunityViewModel) {
    val settings by viewModel.communitySettings.collectAsState()
    val userRole by viewModel.loggedInUserRole.collectAsState()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        delay(2500)
        if (userRole != null) {
            navController.navigate("main") { popUpTo("splash") { inclusive = true } }
        } else {
            navController.navigate("login") { popUpTo("splash") { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        val splashBgModel = BrandingResolver.SPLASH_RES
        AsyncImage(
            model = splashBgModel,
            placeholder = painterResource(id = BrandingResolver.SPLASH_RES),
            error = painterResource(id = BrandingResolver.SPLASH_RES),
            fallback = painterResource(id = BrandingResolver.SPLASH_RES),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.45f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(1200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(4.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF1744), Color(0xFFE53935))
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        elevation = CardDefaults.cardElevation(12.dp)
                    ) {
                        val logoModel = BrandingResolver.getLogoModel(settings.community_logo)
                        AsyncImage(
                            model = logoModel,
                            placeholder = painterResource(id = BrandingResolver.LOGO_RES),
                            error = painterResource(id = BrandingResolver.LOGO_RES),
                            fallback = painterResource(id = BrandingResolver.LOGO_RES),
                            contentDescription = "Logo Nebo Sukabumi",
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = settings.community_name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Aplikasi Keuangan Komunitas",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Light,
                            color = Color.LightGray,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: CommunityViewModel) {
    val settings by viewModel.communitySettings.collectAsState()
    var username by remember { mutableStateOf(viewModel.getSavedUsername()) }
    var password by remember { mutableStateOf(viewModel.getSavedPassword()) }
    var rememberMe by remember { mutableStateOf(viewModel.isRememberMeChecked()) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val bgConfigStr = bgConfigs["bg_login"]

    val loginBgModel = BrandingResolver.LOGIN_BG_RES

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Background Image with blur and ContentScale.Crop
        AsyncImage(
            model = loginBgModel,
            placeholder = painterResource(id = BrandingResolver.LOGIN_BG_RES),
            error = painterResource(id = BrandingResolver.LOGIN_BG_RES),
            fallback = painterResource(id = BrandingResolver.LOGIN_BG_RES),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(6.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.7f
        )

        // Overlay hitam transparan sekitar 50%
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Compact Elegant Login Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .testTag("login_card")
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEB0A0F1D)) // Translucent deep navy
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 28.dp, horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shrunken Lock Icon (by 25% from 64dp -> 48dp)
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF38BDF8)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Login Nebo Sukabumi",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Elegant Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("NRA (Nomor Registrasi Anggota)", fontSize = 13.sp) },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Default.Person, 
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color.LightGray.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLeadingIconColor = Color(0xFF38BDF8),
                        unfocusedLeadingIconColor = Color.LightGray.copy(alpha = 0.7f),
                        focusedContainerColor = Color(0x221E293B),
                        unfocusedContainerColor = Color(0x111E293B)
                    )
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Elegant Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", fontSize = 13.sp) },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Default.Lock, 
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image, 
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.LightGray
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color.LightGray.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLeadingIconColor = Color(0xFF38BDF8),
                        unfocusedLeadingIconColor = Color.LightGray.copy(alpha = 0.7f),
                        focusedContainerColor = Color(0x221E293B),
                        unfocusedContainerColor = Color(0x111E293B)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Remember Me Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF38BDF8),
                            uncheckedColor = Color.LightGray.copy(alpha = 0.6f),
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ingat Saya", 
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Gradient Login Button
                val gradientBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2563EB), Color(0xFF10B981))
                )
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "NRA dan password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.login(username, password, rememberMe) { success, msg ->
                            if (success) {
                                navController.navigate("main") { popUpTo("login") { inclusive = true } }
                            } else {
                                val errorMsg = when (msg) {
                                    "INPUT_EMPTY" -> "NRA dan password wajib diisi!"
                                    "WRONG_CREDENTIALS" -> "NRA atau password salah!"
                                    else -> if (msg.startsWith("ERROR:")) "Gagal masuk: ${msg.substringAfter("ERROR:")}" else msg
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Masuk", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController, viewModel: CommunityViewModel) {
    var identifier by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lupa Password", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F172A), Color.Black)))
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState())
                    .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = Color(0xFFFF1744),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Reset Password Anggota",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kirim permintaan reset password Anda langsung ke Admin.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.LightGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (successMessage.isNotEmpty()) {
                        Text(
                            text = successMessage,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Kembali ke Halaman Login", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("NRA / Username") },
                            placeholder = { Text("Masukkan NRA atau Username Anda") },
                            modifier = Modifier.fillMaxWidth().testTag("nra_reset_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedLabelColor = Color(0xFF38BDF8),
                                unfocusedLabelColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (isLoading) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        } else {
                            Button(
                                onClick = {
                                    if (identifier.isBlank()) {
                                        Toast.makeText(context, "NRA / Username wajib diisi!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.submitResetRequest(identifier) { success, msg ->
                                        isLoading = false
                                        if (success) {
                                            successMessage = msg
                                        } else {
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("submit_reset_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                            ) {
                                Text("Kirim Permintaan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
fun SettingsScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    val userRole by viewModel.loggedInUserRole.collectAsState()
    val isBendahara = userRole == "BENDAHARA" || userRole == "ADMIN" || userRole == "DEVELOPER"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val canCustomize = userRole == "ADMIN" || userRole == "DEVELOPER"
            if (canCustomize) {
                Button(
                    onClick = { navController.navigate("customization") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Kustomisasi Tampilan")
                }
            }
            
            if (userRole == "DEVELOPER") {
                Button(
                    onClick = { navController.navigate("developer_panel") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("developer_panel_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Panel Maintenance DEVELOPER")
                }
            }
            
            if (isBendahara) {
                Button(
                    onClick = { navController.navigate("reset_password_requests") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("reset_requests_menu_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Permintaan Reset Password", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White)
                }
            }
            
            if (isBendahara) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text("Keamanan", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    label = { Text("Password Lama") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("Password Baru") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.changePassword(oldPin, newPin) { success ->
                            if (success) {
                                Toast.makeText(context, "Password Berhasil Diubah", Toast.LENGTH_SHORT).show()
                                oldPin = ""
                                newPin = ""
                            } else {
                                Toast.makeText(context, "Password Lama Salah", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ubah Password")
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate("login") { popUpTo(0) }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Keluar")
            }
        }
    }
}
