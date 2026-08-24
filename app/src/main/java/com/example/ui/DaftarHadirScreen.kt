package com.example.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.network.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaftarHadirScreen(navController: NavController, viewModel: CommunityViewModel) {
    val anggotaList by viewModel.allAnggota.collectAsState()
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val isGuest = userRole == "GUEST"
    val context = LocalContext.current

    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    val currentDate = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    // State for attendance and notes
    // Map of anggotaId to Pair(isPresent, note)
    var attendanceState by remember { mutableStateOf(mapOf<Int, Pair<Boolean, String>>()) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Load from SharedPreferences
    LaunchedEffect(currentMonthYear) {
        val prefs = context.getSharedPreferences("DaftarHadirPrefs", android.content.Context.MODE_PRIVATE)
        val savedJson = prefs.getString("hadir_$currentMonthYear", "{}")
        try {
            val jsonObject = org.json.JSONObject(savedJson ?: "{}")
            val newMap = mutableMapOf<Int, Pair<Boolean, String>>()
            for (key in jsonObject.keys()) {
                val obj = jsonObject.getJSONObject(key)
                newMap[key.toInt()] = Pair(obj.getBoolean("hadir"), obj.getString("catatan"))
            }
            attendanceState = newMap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        coroutineScope.launch(Dispatchers.IO) {
            
        }
    }

    // Save to SharedPreferences function
    val saveState = { newState: Map<Int, Pair<Boolean, String>> ->
        attendanceState = newState
        val prefs = context.getSharedPreferences("DaftarHadirPrefs", android.content.Context.MODE_PRIVATE)
        val jsonObject = org.json.JSONObject()
        for ((key, value) in newState) {
            val obj = org.json.JSONObject()
            obj.put("hadir", value.first)
            obj.put("catatan", value.second)
            jsonObject.put(key.toString(), obj)
        }
        prefs.edit().putString("hadir_$currentMonthYear", jsonObject.toString()).apply()
    }

    val totalHadir = anggotaList.count { attendanceState[it.id]?.first == true }
    val totalTidakHadir = anggotaList.count { attendanceState[it.id]?.first == false }

    val handleShare = {
        val sb = StringBuilder()
        sb.append("NEBO SUKABUMI\n\n")
        sb.append("Daftar Hadir Bulan $currentMonthYear\n\n")
        sb.append("Tanggal $currentDate\n\n")
        sb.append("✔ Hadir : $totalHadir Orang\n\n")
        sb.append("✖ Tidak Hadir : $totalTidakHadir Orang\n\n")
        sb.append("======================\n\n")

        for (anggota in anggotaList) {
            val state = attendanceState[anggota.id]
            val mark = if (state?.first == true) "✔" else if (state?.first == false) "✖" else "☐"
            sb.append("$mark ${anggota.nama}\n\n")
            if (!state?.second.isNullOrBlank()) {
                sb.append("Catatan: ${state!!.second}\n\n")
            }
        }
        sb.append("======================")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Daftar Hadir"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Hadir Bulanan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { handleShare() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bulan: $currentMonthYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total Anggota : ${anggotaList.size}")
                    Text("✔ Hadir : $totalHadir", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    Text("✖ Tidak Hadir : $totalTidakHadir", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(anggotaList) { anggota ->
                    val currentState = attendanceState[anggota.id]
                    val isHadir = currentState?.first
                    val catatan = currentState?.second ?: ""

                    var editCatatanMode by remember { mutableStateOf(false) }
                    var tempCatatan by remember { mutableStateOf(catatan) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (!isGuest) {
                                        editCatatanMode = true
                                        tempCatatan = catatan
                                    }
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Buttons for Hadir / Tidak Hadir
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Button Check
                                    IconButton(
                                        onClick = {
                                            if (!isGuest) {
                                                val newMap = attendanceState.toMutableMap()
                                                newMap[anggota.id] = Pair(true, catatan)
                                                saveState(newMap)
                                            }
                                        },
                                        modifier = Modifier.background(
                                            color = if (isHadir == true) Color(0xFF4CAF50) else Color.LightGray.copy(alpha = 0.3f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ).size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Hadir", tint = if (isHadir == true) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                                    }

                                    // Button Cross
                                    IconButton(
                                        onClick = {
                                            if (!isGuest) {
                                                val newMap = attendanceState.toMutableMap()
                                                newMap[anggota.id] = Pair(false, catatan)
                                                saveState(newMap)
                                            }
                                        },
                                        modifier = Modifier.background(
                                            color = if (isHadir == false) Color(0xFFE53935) else Color.LightGray.copy(alpha = 0.3f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ).size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Tidak Hadir", tint = if (isHadir == false) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = anggota.nama,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    if (catatan.isNotBlank() && !editCatatanMode) {
                                        Text(
                                            text = catatan,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (editCatatanMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = tempCatatan,
                                    onValueChange = { tempCatatan = it },
                                    label = { Text("Catatan (Opsional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val newMap = attendanceState.toMutableMap()
                                            newMap[anggota.id] = Pair(isHadir ?: false, tempCatatan)
                                            saveState(newMap)
                                            editCatatanMode = false
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Simpan Catatan", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
