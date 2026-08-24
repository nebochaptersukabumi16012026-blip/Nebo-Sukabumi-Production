package com.example.network

import com.example.ui.CatatanItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CatatanApi {
    @GET("api.php?action=get_catatan")
    suspend fun getCatatan(): Response<List<CatatanItem>>

    @POST("api.php?action=save_catatan")
    suspend fun saveCatatan(@Body catatan: CatatanItem): Response<SimpleResponse>

    @GET("api.php?action=delete_catatan")
    suspend fun deleteCatatan(@Query("id") id: Long): Response<SimpleResponse>
}
