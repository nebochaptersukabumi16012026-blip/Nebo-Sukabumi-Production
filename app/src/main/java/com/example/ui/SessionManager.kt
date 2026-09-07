package com.example.ui

import android.content.Context

object SessionManager {
    private const val PREF_NAME = "nebo_sukabumi_prefs"
    private const val KEY_ROLE = "session_role"
    private const val KEY_USER_ID = "session_user_id"
    private const val KEY_USER_NAME = "session_user_name"
    private const val KEY_USER_NRA = "session_user_nra"

    @Volatile
    private var cachedRole: String? = null

    fun setRole(role: String?) {
        cachedRole = role
    }

    fun getRole(context: Context? = null): String {
        cachedRole?.let { return it }
        if (context != null) {
            val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val role = sp.getString(KEY_ROLE, null)
            if (role != null) {
                cachedRole = role
                return role
            }
        }
        return cachedRole ?: ""
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
