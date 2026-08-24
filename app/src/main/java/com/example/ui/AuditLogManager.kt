package com.example.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object AuditLogManager {
    private val _auditLogs = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val auditLogs: StateFlow<List<Map<String, String>>> = _auditLogs.asStateFlow()

    private val _loginLogs = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val loginLogs: StateFlow<List<Map<String, String>>> = _loginLogs.asStateFlow()

    fun logActivity(
        context: Context,
        username: String,
        namaLengkap: String,
        role: String,
        jenisAktivitas: String,
        halamanMenu: String,
        dataLama: String = "-",
        dataBaru: String = "-",
        status: String = "Berhasil"
    ) {
        val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = System.currentTimeMillis()
        val dateStr = sdfDate.format(now)
        val timeStr = sdfTime.format(now)
        val id = UUID.randomUUID().toString()

        val sharedPrefs = context.getSharedPreferences("NeboAuditLogs", Context.MODE_PRIVATE)
        val logsJson = sharedPrefs.getString("audit_logs", "[]") ?: "[]"
        try {
            val array = JSONArray(logsJson)
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("tanggal", dateStr)
            obj.put("jam", timeStr)
            obj.put("username", username)
            obj.put("nama_lengkap", namaLengkap)
            obj.put("role", role)
            obj.put("jenis_aktivitas", jenisAktivitas)
            obj.put("halaman_menu", halamanMenu)
            obj.put("data_lama", dataLama)
            obj.put("data_baru", dataBaru)
            obj.put("ip_address", "127.0.0.1")
            obj.put("device_android", android.os.Build.MODEL)
            obj.put("versi_apk", "1.0.0")
            obj.put("status", status)

            val tempArray = JSONArray()
            tempArray.put(obj)
            for (i in 0 until array.length()) {
                tempArray.put(array.get(i))
            }
            sharedPrefs.edit().putString("audit_logs", tempArray.toString()).apply()
            loadAuditLogs(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAuditLogs(context: Context) {
        val sharedPrefs = context.getSharedPreferences("NeboAuditLogs", Context.MODE_PRIVATE)
        val logsJson = sharedPrefs.getString("audit_logs", "[]") ?: "[]"
        try {
            val array = JSONArray(logsJson)
            val list = mutableListOf<Map<String, String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val map = mutableMapOf<String, String>()
                obj.keys().forEach { key ->
                    map[key] = obj.optString(key, "")
                }
                list.add(map)
            }
            _auditLogs.value = list
        } catch (e: Exception) {
            _auditLogs.value = emptyList()
        }
    }

    fun logLogin(
        context: Context,
        username: String,
        namaLengkap: String,
        role: String,
        status: String = "Berhasil"
    ) {
        val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = System.currentTimeMillis()
        val dateStr = sdfDate.format(now)
        val timeStr = sdfTime.format(now)
        val id = UUID.randomUUID().toString()

        val sharedPrefs = context.getSharedPreferences("NeboLoginLogs", Context.MODE_PRIVATE)
        val logsJson = sharedPrefs.getString("login_logs", "[]") ?: "[]"
        try {
            val array = JSONArray(logsJson)
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("tanggal", dateStr)
            obj.put("jam", timeStr)
            obj.put("username", username)
            obj.put("nama_lengkap", namaLengkap)
            obj.put("role", role)
            obj.put("ip_address", "127.0.0.1")
            obj.put("device_android", android.os.Build.MODEL)
            obj.put("versi_apk", "1.0.0")
            obj.put("status", status)

            val tempArray = JSONArray()
            tempArray.put(obj)
            for (i in 0 until array.length()) {
                tempArray.put(array.get(i))
            }
            sharedPrefs.edit().putString("login_logs", tempArray.toString()).apply()
            loadLoginLogs(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadLoginLogs(context: Context) {
        val sharedPrefs = context.getSharedPreferences("NeboLoginLogs", Context.MODE_PRIVATE)
        val logsJson = sharedPrefs.getString("login_logs", "[]") ?: "[]"
        try {
            val array = JSONArray(logsJson)
            val list = mutableListOf<Map<String, String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val map = mutableMapOf<String, String>()
                obj.keys().forEach { key ->
                    map[key] = obj.optString(key, "")
                }
                list.add(map)
            }
            _loginLogs.value = list
        } catch (e: Exception) {
            _loginLogs.value = emptyList()
        }
    }
}
