package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.network.ApiClient
import com.example.network.CommunitySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentitasKomunitasScreen(navController: NavController, viewModel: CommunityViewModel) {
    val settings by viewModel.communitySettings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(settings.community_name) }
    var slogan by remember { mutableStateOf(settings.community_slogan) }
    var motto by remember { mutableStateOf(settings.community_motto) }
    var targetAniv by remember { mutableStateOf(settings.target_aniv.toString()) }
    var targetKas by remember { mutableStateOf(settings.target_kas.toString()) }
    var address by remember { mutableStateOf(settings.community_address) }
    var phone by remember { mutableStateOf(settings.community_phone) }
    var email by remember { mutableStateOf(settings.community_email) }
    var website by remember { mutableStateOf(settings.community_website) }
    var facebook by remember { mutableStateOf(settings.community_facebook) }
    var instagram by remember { mutableStateOf(settings.community_instagram) }
    var youtube by remember { mutableStateOf(settings.community_youtube) }

    // Fake Uris for local preview before upload
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var splashUri by remember { mutableStateOf<Uri?>(null) }
    var loginBgUri by remember { mutableStateOf<Uri?>(null) }
    var profileBannerUri by remember { mutableStateOf<Uri?>(null) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        logoUri = uri
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        bannerUri = uri
    }
    val splashPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        splashUri = uri
    }
    val loginBgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        loginBgUri = uri
    }
    val profileBannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profileBannerUri = uri
    }

    // Helper to upload Uri
    fun uriToMultipartBodyPart(context: android.content.Context, uri: Uri, partName: String): okhttp3.MultipartBody.Part? {
        val contentResolver = context.contentResolver
        val tempFile = java.io.File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}_${partName}.jpg")
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val mediaTypeString = contentResolver.getType(uri) ?: "image/jpeg"
            val mediaType = mediaTypeString.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()!!
            val requestFile = okhttp3.RequestBody.create(mediaType, tempFile)
            okhttp3.MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val handleSave = {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                var updatedLogo = settings.community_logo
                logoUri?.let { uri ->
                    val part = uriToMultipartBodyPart(context, uri, "logo")
                    if (part != null) {
                        val res = ApiClient.apiService.uploadLogo(part)
                        if (res.isSuccessful && res.body()?.status == "success") {
                            updatedLogo = "https://nebosukabumi.net/uploads/community_logo.png"
                        } else {
                            throw Exception("Gagal mengupload logo")
                        }
                    }
                }

                var updatedBanner = settings.community_banner
                bannerUri?.let { uri ->
                    val part = uriToMultipartBodyPart(context, uri, "banner")
                    if (part != null) {
                        val res = ApiClient.apiService.uploadBanner(part)
                        if (res.isSuccessful && res.body()?.status == "success") {
                            updatedBanner = "https://nebosukabumi.net/uploads/community_banner.jpg"
                        } else {
                            throw Exception("Gagal mengupload banner")
                        }
                    }
                }

                var updatedSplash = settings.community_splash
                splashUri?.let { uri ->
                    val part = uriToMultipartBodyPart(context, uri, "splash")
                    if (part != null) {
                        val res = ApiClient.apiService.uploadSplash(part)
                        if (res.isSuccessful && res.body()?.status == "success") {
                            updatedSplash = "https://nebosukabumi.net/uploads/splash.jpg"
                        } else {
                            throw Exception("Gagal mengupload splash")
                        }
                    }
                }

                var updatedLoginBg = settings.login_background
                loginBgUri?.let { uri ->
                    val part = uriToMultipartBodyPart(context, uri, "login_bg")
                    if (part != null) {
                        val res = ApiClient.apiService.uploadLoginBg(part)
                        if (res.isSuccessful && res.body()?.status == "success") {
                            updatedLoginBg = "https://nebosukabumi.net/uploads/login_bg.jpg"
                        } else {
                            throw Exception("Gagal mengupload background login")
                        }
                    }
                }

                var updatedProfileBanner = settings.profile_banner
                profileBannerUri?.let { uri ->
                    val part = uriToMultipartBodyPart(context, uri, "profile_banner")
                    if (part != null) {
                        val res = ApiClient.apiService.uploadProfileBanner(part)
                        if (res.isSuccessful && res.body()?.status == "success") {
                            updatedProfileBanner = "https://nebosukabumi.net/uploads/profile_banner.jpg"
                        } else {
                            throw Exception("Gagal mengupload banner profil")
                        }
                    }
                }

                val newSettings = settings.copy(
                    community_name = name,
                    community_slogan = slogan,
                    community_motto = motto,
                    target_aniv = targetAniv.toDoubleOrNull() ?: 0.0,
                    target_kas = targetKas.toDoubleOrNull() ?: 0.0,
                    community_address = address,
                    community_phone = phone,
                    community_email = email,
                    community_website = website,
                    community_facebook = facebook,
                    community_instagram = instagram,
                    community_youtube = youtube,
                    community_logo = updatedLogo,
                    community_banner = updatedBanner,
                    community_splash = updatedSplash,
                    login_background = updatedLoginBg,
                    profile_banner = updatedProfileBanner,
                    updated_at = System.currentTimeMillis(),
                    updated_by = "Super Admin"
                )

                val res = ApiClient.apiService.saveSettings(newSettings)
                if (res.isSuccessful) {
                    viewModel.updateCommunitySettings(newSettings)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Upload berhasil", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Upload gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identitas Komunitas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isLoading) handleSave() },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Simpan", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pengaturan Dasar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Komunitas") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = slogan,
                onValueChange = { slogan = it },
                label = { Text("Slogan") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = motto,
                onValueChange = { motto = it },
                label = { Text("Motto") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = targetAniv,
                onValueChange = { targetAniv = it },
                label = { Text("Target Iuran Anniversary (Rp)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
            OutlinedTextField(
                value = targetKas,
                onValueChange = { targetKas = it },
                label = { Text("Target Uang Kas (Rp)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Gambar & Aset (Hosting)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Logo
            Text("Logo Komunitas", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = logoUri ?: BrandingResolver.getLogoModel(settings.community_logo),
                    placeholder = painterResource(id = BrandingResolver.LOGO_RES),
                    error = painterResource(id = BrandingResolver.LOGO_RES),
                    fallback = painterResource(id = BrandingResolver.LOGO_RES),
                    contentDescription = "Logo Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Button(
                    onClick = { logoPicker.launch("image/*") },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih Logo")
                }
            }

            // Banner
            Text("Banner Dashboard", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = bannerUri ?: BrandingResolver.getBannerModel(settings.community_banner),
                    placeholder = painterResource(id = BrandingResolver.HEADER_DASHBOARD_RES),
                    error = painterResource(id = BrandingResolver.HEADER_DASHBOARD_RES),
                    fallback = painterResource(id = BrandingResolver.HEADER_DASHBOARD_RES),
                    contentDescription = "Banner Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Button(
                    onClick = { bannerPicker.launch("image/*") },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih Banner")
                }
            }

            // Splash
            Text("Splash Screen", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = splashUri ?: BrandingResolver.SPLASH_RES,
                    placeholder = painterResource(id = BrandingResolver.SPLASH_RES),
                    error = painterResource(id = BrandingResolver.SPLASH_RES),
                    fallback = painterResource(id = BrandingResolver.SPLASH_RES),
                    contentDescription = "Splash Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Button(
                    onClick = { splashPicker.launch("image/*") },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih Splash")
                }
            }

            // Background Login
            Text("Background Login", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = loginBgUri ?: BrandingResolver.LOGIN_BG_RES,
                    placeholder = painterResource(id = BrandingResolver.LOGIN_BG_RES),
                    error = painterResource(id = BrandingResolver.LOGIN_BG_RES),
                    fallback = painterResource(id = BrandingResolver.LOGIN_BG_RES),
                    contentDescription = "Background Login Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Button(
                    onClick = { loginBgPicker.launch("image/*") },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih Background Login")
                }
            }

            // Banner Profil
            Text("Banner Profil", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = profileBannerUri ?: BrandingResolver.PROFILE_BANNER_RES,
                    placeholder = painterResource(id = BrandingResolver.PROFILE_BANNER_RES),
                    error = painterResource(id = BrandingResolver.PROFILE_BANNER_RES),
                    fallback = painterResource(id = BrandingResolver.PROFILE_BANNER_RES),
                    contentDescription = "Banner Profil Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Button(
                    onClick = { profileBannerPicker.launch("image/*") },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih Banner Profil")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Informasi Kontak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat Sekretariat") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Nomor WhatsApp") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Komunitas") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Sosial Media", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = facebook,
                onValueChange = { facebook = it },
                label = { Text("Facebook URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instagram,
                onValueChange = { instagram = it },
                label = { Text("Instagram URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = youtube,
                onValueChange = { youtube = it },
                label = { Text("YouTube URL") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
