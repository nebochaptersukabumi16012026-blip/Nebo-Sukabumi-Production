package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CatatanItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatanScreen(navController: NavController, viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("CatatanPrefs", Context.MODE_PRIVATE) }
    
    val userRole by viewModel.loggedInUserRole.collectAsState()
    val canManage = userRole?.uppercase() in listOf("DEVELOPER", "ADMIN", "BENDAHARA")
    val isGuest = !canManage

    var catatanList by remember { mutableStateOf(emptyList<CatatanItem>()) }
    val coroutineScope = rememberCoroutineScope()

    // Load Data
    LaunchedEffect(Unit) {
        val savedJson = prefs.getString("catatan_list", "[]")
        try {
            val array = JSONArray(savedJson)
            val list = mutableListOf<CatatanItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CatatanItem(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            catatanList = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        coroutineScope.launch(Dispatchers.IO) {
            try { val res = ApiClient.apiService.getCatatan()
            if (res.isSuccessful) {
                res.body()?.data?.let { list ->
                    catatanList = list.map { CatatanItem(it.id.toLong(), it.judul, it.isi, try { it.tanggal.toLong() } catch(e:Exception){0L}) }
                }
            } } catch (e: Exception) {}
        }
    }

    val saveList = { newList: List<CatatanItem> ->
        catatanList = newList.sortedByDescending { it.timestamp }
        val array = JSONArray()
        for (item in newList) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("content", item.content)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("catatan_list", array.toString()).apply()
    }

    var showForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<CatatanItem?>(null) }
    
    var formTitle by remember { mutableStateOf("") }
    var formContent by remember { mutableStateOf("") }

    val handleSave = {
        val newItem = CatatanItem(
            id = editingItem?.id ?: System.currentTimeMillis(),
            title = formTitle,
            content = formContent,
            timestamp = editingItem?.timestamp ?: System.currentTimeMillis() // Keep original timestamp if editing
        )
        val mutableList = catatanList.toMutableList()
        if (editingItem != null) {
            val idx = mutableList.indexOfFirst { it.id == editingItem!!.id }
            if (idx != -1) mutableList[idx] = newItem
        } else {
            mutableList.add(newItem)
        }
        saveList(mutableList)
        coroutineScope.launch(Dispatchers.IO) {
            try { ApiClient.apiService.addCatatan(com.example.network.CatatanDto(judul = newItem.title, isi = newItem.content, tanggal = newItem.timestamp.toString())) } catch (e: Exception) {}
        }
        showForm = false
        editingItem = null
    }

    val handleDelete = { item: CatatanItem ->
        val mutableList = catatanList.toMutableList()
        mutableList.remove(item)
        saveList(mutableList)
        coroutineScope.launch(Dispatchers.IO) {
            try { ApiClient.apiService.deleteCatatan(mapOf("id" to item.id.toInt())) } catch (e: Exception) {}
        }
    }

    val handleShare = { item: CatatanItem ->
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            val shareText = if (item.title.isNotBlank()) "${item.title}\n\n${item.content}" else item.content
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Catatan"))
    }
    
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy '•' HH:mm", Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showForm) "Edit Catatan" else "Catatan Bebas") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (showForm) {
                            showForm = false
                            editingItem = null
                        } else {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showForm && !isGuest) {
                FloatingActionButton(
                    onClick = {
                        formTitle = ""
                        formContent = ""
                        editingItem = null
                        showForm = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Catatan")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showForm) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Judul Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = formContent,
                        onValueChange = { formContent = it },
                        label = { Text("Isi Catatan") },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Button(
                        onClick = handleSave,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Catatan", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                if (catatanList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada catatan.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(catatanList) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📄")
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (item.title.isNotBlank()) item.title else "Tanpa Judul",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = dateFormatter.format(Date(item.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                            )
                                        }
                                        
                                        // Menu
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (!isGuest) {
                                                IconButton(
                                                    onClick = {
                                                        formTitle = item.title
                                                        formContent = item.content
                                                        editingItem = item
                                                        showForm = true
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(
                                                    onClick = { handleDelete(item) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            IconButton(
                                                onClick = { handleShare(item) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    Text(
                                        text = item.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
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
