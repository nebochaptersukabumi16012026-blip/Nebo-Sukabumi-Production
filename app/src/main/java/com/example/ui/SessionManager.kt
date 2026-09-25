package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SessionManager {
    private const val PREF_NAME = "secure_nebo_sukabumi_prefs"
    private const val KEY_ROLE = "session_role"
    private const val KEY_USER_ID = "session_user_id"
    private const val KEY_USER_NAME = "session_user_name"
    private const val KEY_USER_NRA = "session_user_nra"
    private const val KEY_AUTH_TOKEN = "session_auth_token"
    private const val KEY_IS_VERIFIED = "session_is_verified"

    @Volatile
    private var cachedRole: String? = null

    private fun getSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback jika terjadi error hardware KeyStore
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveUserSession(
        context: Context,
        userId: Int,
        userName: String,
        userNra: String,
        role: String,
        token: String? = null,
        isVerified: Boolean = true
    ) {
        cachedRole = role
        getSecurePrefs(context).edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_NRA, userNra)
            .putString(KEY_ROLE, role)
            .putString(KEY_AUTH_TOKEN, token ?: "")
            .putBoolean(KEY_IS_VERIFIED, isVerified)
            .apply()
    }

    fun setRole(role: String?) {
        cachedRole = role
    }

    fun getRole(context: Context? = null): String {
        cachedRole?.let { return it }
        if (context != null) {
            val role = getSecurePrefs(context).getString(KEY_ROLE, null)
            if (role != null) {
                cachedRole = role
                return role
            }
        }
        return cachedRole ?: ""
    }

    fun getUserId(context: Context): Int {
        return getSecurePrefs(context).getInt(KEY_USER_ID, -1)
    }

    fun getUserNra(context: Context): String {
        return getSecurePrefs(context).getString(KEY_USER_NRA, "") ?: ""
    }

    fun getNra(context: Context): String = getUserNra(context)

    fun getUserName(context: Context): String {
        return getSecurePrefs(context).getString(KEY_USER_NAME, "") ?: ""
    }

    fun getAuthToken(context: Context): String {
        return getSecurePrefs(context).getString(KEY_AUTH_TOKEN, "") ?: ""
    }

    fun isVerified(context: Context): Boolean {
        return getSecurePrefs(context).getBoolean(KEY_IS_VERIFIED, true)
    }

    fun clearSession(context: Context) {
        cachedRole = null
        getSecurePrefs(context).edit().clear().apply()
    }

    fun isGuest(context: Context? = null): Boolean {
        return getRole(context).equals("GUEST", ignoreCase = true)
    }

    fun isAnggota(context: Context? = null): Boolean {
        return getRole(context).equals("ANGGOTA", ignoreCase = true)
    }

    fun isRestrictedRole(context: Context? = null): Boolean {
        val r = getRole(context).uppercase()
        return r == "ANGGOTA" || r == "GUEST"
    }
}
