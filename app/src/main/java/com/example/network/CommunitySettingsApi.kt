package com.example.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part

data class CommunitySettings(
    val id: Int = 1,
    val community_name: String = "NEBO SUKABUMI",
    val community_slogan: String = "",
    val community_motto: String = "",
    val community_logo: String = "",
    val community_banner: String = "",
    val community_splash: String = "",
    val community_address: String = "",
    val community_phone: String = "",
    val community_email: String = "",
    val community_website: String = "",
    val community_facebook: String = "",
    val community_instagram: String = "",
    val community_youtube: String = "",
    val target_aniv: Double = 0.0,
    val target_kas: Double = 0.0,
    val login_background: String = "",
    val profile_banner: String = "",
    val updated_at: Long = System.currentTimeMillis(),
    val updated_by: String = ""
)

interface CommunitySettingsApi {
    @GET("api.php?action=get_settings")
    suspend fun getSettings(): Response<CommunitySettings>

    @POST("api.php?action=save_settings")
    suspend fun saveSettings(@Body settings: CommunitySettings): Response<SimpleResponse>

    @Multipart
    @POST("upload_logo.php")
    suspend fun uploadLogo(@Part file: MultipartBody.Part): Response<SimpleResponse>

    @Multipart
    @POST("upload_banner.php")
    suspend fun uploadBanner(@Part file: MultipartBody.Part): Response<SimpleResponse>

    @Multipart
    @POST("upload_splash.php")
    suspend fun uploadSplash(@Part file: MultipartBody.Part): Response<SimpleResponse>

    @Multipart
    @POST("upload_login_bg.php")
    suspend fun uploadLoginBg(@Part file: MultipartBody.Part): Response<SimpleResponse>

    @Multipart
    @POST("upload_profile_banner.php")
    suspend fun uploadProfileBanner(@Part file: MultipartBody.Part): Response<SimpleResponse>
}
