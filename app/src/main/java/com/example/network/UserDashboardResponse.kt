package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserPermissions(
    @Json(name = "can_view_laporan") val canViewLaporan: Boolean = true,
    @Json(name = "can_edit_kas") val canEditKas: Boolean = false,
    @Json(name = "can_manage_users") val canManageUsers: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UserData(
    @Json(name = "id_user") val idUser: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "role") val role: String = "member",
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "permissions") val permissions: UserPermissions = UserPermissions()
)

@JsonClass(generateAdapter = true)
data class UserDashboardResponse(
    @Json(name = "status") val status: String = "success",
    @Json(name = "data") val data: UserData? = null
)
