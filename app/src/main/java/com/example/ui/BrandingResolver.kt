package com.example.ui

import com.example.R

object BrandingResolver {
    // ------------------------------------------------------------------------
    // PANDUAN MENGGANTI GAMBAR LOKAL:
    // Jika server offline, gambar akan secara otomatis menggunakan cadangan lokal:
    // - R.drawable.logo_komunitas
    // - R.drawable.dashboard_banner
    // ------------------------------------------------------------------------

    // Fallback logo lokal
    val LOGO_RES = R.drawable.nebo_dashboard_banner
    
    // Fallback splash background lokal
    val SPLASH_RES = R.drawable.nebo_dashboard_banner

    // Fallback login background lokal
    val LOGIN_BG_RES = R.drawable.nebo_dashboard_banner

    // Fallback banner dashboard lokal
    val HEADER_DASHBOARD_RES = R.drawable.nebo_dashboard_banner

    // Fallback banner profil anggota lokal
    val PROFILE_BANNER_RES = R.drawable.nebo_dashboard_banner

    // URL Gambar di hosting sebagai sumber utama dinamis
    private const val DEFAULT_LOGO_URL = "https://nebosukabumi.net/images/logo_komunitas.png"
    private const val DEFAULT_BANNER_URL = "https://nebosukabumi.net/images/dashboard_banner.jpg"

    /**
     * Mengambil model logo secara dinamis dari database/API hosting.
     * Jika tidak ada, menggunakan default URL hosting.
     */
    fun getLogoModel(apiValue: String?): Any {
        return if (!apiValue.isNullOrBlank()) apiValue else DEFAULT_LOGO_URL
    }

    /**
     * Mengambil model banner secara dinamis dari database/API hosting.
     * Jika tidak ada, menggunakan default URL hosting.
     */
    fun getBannerModel(apiValue: String?): Any {
        return if (!apiValue.isNullOrBlank()) apiValue else DEFAULT_BANNER_URL
    }
}

