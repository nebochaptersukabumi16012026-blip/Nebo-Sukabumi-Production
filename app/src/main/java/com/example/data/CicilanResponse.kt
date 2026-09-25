package com.example.data

import com.google.gson.annotations.SerializedName

data class CicilanResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("total_sisa_cicilan")
    val total_sisa_cicilan: Double = 0.0,

    @SerializedName("total_harga_barang")
    val total_harga_barang: Double = 0.0,

    @SerializedName("total_dibayar")
    val total_dibayar: Double = 0.0,

    @SerializedName("total_anggota_mencicil")
    val total_anggota_mencicil: Int = 0,

    @SerializedName("data")
    val data: List<CicilanAnggota>? = emptyList()
)

data class CicilanAnggota(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("nama")
    val nama: String? = "",

    @SerializedName("nra")
    val nra: String? = "",

    @SerializedName("harga_barang")
    val harga_barang: Double = 0.0,

    @SerializedName("total_dibayar")
    val total_dibayar: Double = 0.0,

    @SerializedName("sudah_dibayar")
    val sudah_dibayar: Double = 0.0,

    @SerializedName("sisa_cicilan")
    val sisa_cicilan: Double = 0.0,

    @SerializedName("cicilan_per_bulan")
    val cicilan_per_bulan: Double = 0.0
)
