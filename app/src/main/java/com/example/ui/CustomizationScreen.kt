package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.draw.blur
import org.json.JSONObject

data class BackgroundConfigData(
    val uri: String? = null,
    val opacity: Float = 1f,
    val blur: Float = 0f,
    val scaleType: String = "Crop"
)

fun parseConfig(jsonStr: String?): BackgroundConfigData {
    if (jsonStr == null) return BackgroundConfigData()
    return try {
        val json = JSONObject(jsonStr)
        BackgroundConfigData(
            uri = if (json.has("uri")) json.getString("uri") else null,
            opacity = if (json.has("opacity")) json.getDouble("opacity").toFloat() else 1f,
            blur = if (json.has("blur")) json.getDouble("blur").toFloat() else 0f,
            scaleType = if (json.has("scaleType")) json.getString("scaleType") else "Crop"
        )
    } catch (e: Exception) {
        BackgroundConfigData()
    }
}

fun encodeConfig(config: BackgroundConfigData): String {
    val json = JSONObject()
    config.uri?.let { json.put("uri", it) }
    json.put("opacity", config.opacity.toDouble())
    json.put("blur", config.blur.toDouble())
    json.put("scaleType", config.scaleType)
    return json.toString()
}

@Composable
fun DynamicBackground(
    configStr: String?,
    modifier: Modifier = Modifier
) {
    val config = remember(configStr) { parseConfig(configStr) }
    
    if (config.uri != null) {
        if (config.uri.startsWith("color:")) {
            val colorVal = config.uri.substringAfter("color:").toULongOrNull(16)
            if (colorVal != null) {
                Box(modifier = modifier.fillMaxSize().background(Color(colorVal).copy(alpha = config.opacity)))
            }
        } else {
            val blurModifier = if (config.blur > 0f) Modifier.blur(config.blur.dp) else Modifier
            Image(
                painter = rememberAsyncImagePainter(config.uri),
                contentDescription = null,
                contentScale = when (config.scaleType) {
                    "Fit" -> ContentScale.Fit
                    "Fill" -> ContentScale.FillBounds
                    "Center" -> ContentScale.Inside
                    else -> ContentScale.Crop
                },
                modifier = modifier.fillMaxSize().then(blurModifier),
                alpha = config.opacity
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(navController: NavController, viewModel: CommunityViewModel) {
    val bgConfigs by viewModel.bgConfigs.collectAsState()
    val userRole by viewModel.loggedInUserRole.collectAsState()

    val showAnggotaInitial by viewModel.showCardAnggota.collectAsState()
    val showUangKasInitial by viewModel.showCardUangKas.collectAsState()
    val showIuranAnivInitial by viewModel.showCardIuranAniv.collectAsState()
    val showBelumKasInitial by viewModel.showCardBelumKas.collectAsState()
    val showBelumAnivInitial by viewModel.showCardBelumAniv.collectAsState()
    val showGrafikInitial by viewModel.showCardGrafik.collectAsState()
    
    val screens = listOf(
        "bg_login" to "Login",
        "bg_splash" to "Splash Screen",
        "bg_dashboard" to "Dashboard",
        "bg_anggota" to "Anggota",
        "bg_pembayaran" to "Pembayaran",
        "bg_laporan" to "Laporan"
    )
    
    var selectedScreenKey by remember { mutableStateOf(screens[0].first) }
    val currentConfigStr = bgConfigs[selectedScreenKey]
    var currentConfig by remember(currentConfigStr, selectedScreenKey) { 
        mutableStateOf(parseConfig(currentConfigStr)) 
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            currentConfig = currentConfig.copy(uri = uri.toString())
        }
    }
    
    val presetColors = listOf(
        Color(0xFF1E88E5) to "Biru Elegan",
        Color(0xFF43A047) to "Hijau Elegan",
        Color(0xFF37474F) to "Tema Gelap Premium",
        Color(0xFF8E24AA) to "Abstrak Modern"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kustomisasi Tampilan") },
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
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Pilih Halaman", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(screens) { (key, title) ->
                        FilterChip(
                            selected = selectedScreenKey == key,
                            onClick = { selectedScreenKey = key },
                            label = { Text(title) }
                        )
                    }
                }
            }
            
            item {
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Background Preview", style = MaterialTheme.typography.titleMedium)
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentConfig.uri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(currentConfig.uri),
                            contentDescription = null,
                            contentScale = when (currentConfig.scaleType) {
                                "Fit" -> ContentScale.Fit
                                "Fill" -> ContentScale.FillBounds
                                "Center" -> ContentScale.Inside
                                else -> ContentScale.Crop
                            },
                            modifier = Modifier.fillMaxSize(),
                            alpha = currentConfig.opacity
                        )
                    } else {
                        Text("Belum ada background")
                    }
                }
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("Pilih Gambar")
                    }
                    OutlinedButton(onClick = { currentConfig = BackgroundConfigData() }) {
                        Text("Reset Default")
                    }
                }
            }
            
            item {
                Text("Preset Tema Bawaan", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(presetColors) { (color, title) ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(color)
                                .clickable {
                                    // For simplicity, preset just resets to solid color via URI or we handle color.
                                    // But since URI is string, we might just store a color string and handle it later.
                                    // For now, let's keep it simple: we can map these to drawable resources if we had them.
                                    // We will skip actual image assets for presets if we don't have them, or use a solid color representation.
                                    currentConfig = currentConfig.copy(uri = "color:${color.value.toULong().toString(16)}")
                                }
                        )
                    }
                }
            }
            
            item {
                Text("Opacity: ${(currentConfig.opacity * 100).toInt()}%")
                Slider(
                    value = currentConfig.opacity,
                    onValueChange = { currentConfig = currentConfig.copy(opacity = it) },
                    valueRange = 0f..1f
                )
            }
            
            item {
                Text("Blur: ${currentConfig.blur.toInt()}")
                Slider(
                    value = currentConfig.blur,
                    onValueChange = { currentConfig = currentConfig.copy(blur = it) },
                    valueRange = 0f..100f
                )
            }
            
            item {
                Text("Posisi / Scale", style = MaterialTheme.typography.titleMedium)
                val scaleTypes = listOf("Crop", "Fit", "Fill", "Center")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scaleTypes) { scale ->
                        FilterChip(
                            selected = currentConfig.scaleType == scale,
                            onClick = { currentConfig = currentConfig.copy(scaleType = scale) },
                            label = { Text(scale) }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.saveBgConfig(selectedScreenKey, encodeConfig(currentConfig))
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Simpan Pengaturan", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }

            if (userRole == "ADMIN" || userRole == "DEVELOPER") {
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        "Kustomisasi Layout Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    var showAnggota by remember(showAnggotaInitial) { mutableStateOf(showAnggotaInitial) }
                    var showUangKas by remember(showUangKasInitial) { mutableStateOf(showUangKasInitial) }
                    var showIuranAniv by remember(showIuranAnivInitial) { mutableStateOf(showIuranAnivInitial) }
                    var showBelumKas by remember(showBelumKasInitial) { mutableStateOf(showBelumKasInitial) }
                    var showBelumAniv by remember(showBelumAnivInitial) { mutableStateOf(showBelumAnivInitial) }
                    var showGrafik by remember(showGrafikInitial) { mutableStateOf(showGrafikInitial) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showAnggota = !showAnggota }
                            ) {
                                Checkbox(checked = showAnggota, onCheckedChange = { showAnggota = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tampilkan Statistik Anggota", style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showUangKas = !showUangKas }
                            ) {
                                Checkbox(checked = showUangKas, onCheckedChange = { showUangKas = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tampilkan Statistik Uang Kas", style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showIuranAniv = !showIuranAniv }
                            ) {
                                Checkbox(checked = showIuranAniv, onCheckedChange = { showIuranAniv = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tampilkan Statistik Iuran Anniversary", style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showBelumKas = !showBelumKas }
                            ) {
                                Checkbox(checked = showBelumKas, onCheckedChange = { showBelumKas = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tampilkan Statistik Belum Bayar Kas", style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showBelumAniv = !showBelumAniv }
                            ) {
                                Checkbox(checked = showBelumAniv, onCheckedChange = { showBelumAniv = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tampilkan Statistik Belum Bayar Iuran Aniv", style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { showGrafik = !showGrafik }
                            ) {
                                Checkbox(checked = showGrafik, onCheckedChange = { showGrafik = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tampilkan Grafik Pemasukan", style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.updateDashboardLayout(
                                        showAnggota = showAnggota,
                                        showUangKas = showUangKas,
                                        showIuranAniv = showIuranAniv,
                                        showBelumKas = showBelumKas,
                                        showBelumAniv = showBelumAniv,
                                        showGrafik = showGrafik
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Simpan Layout Dashboard", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
