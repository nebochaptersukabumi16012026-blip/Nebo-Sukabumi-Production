package com.example.java;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class CicilanResponse {
    @SerializedName("status")
    private String status = "";

    @SerializedName("total_sisa_cicilan")
    private double total_sisa_cicilan = 0.0;

    @SerializedName("total_harga_barang")
    private double total_harga_barang = 0.0;

    @SerializedName("total_dibayar")
    private double total_dibayar = 0.0;

    @SerializedName("total_anggota_mencicil")
    private int total_anggota_mencicil = 0;

    @SerializedName("data")
    private List<CicilanAnggota> data = new ArrayList<>();

    public CicilanResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotal_sisa_cicilan() {
        return total_sisa_cicilan;
    }

    public void setTotal_sisa_cicilan(double total_sisa_cicilan) {
        this.total_sisa_cicilan = total_sisa_cicilan;
    }

    public double getTotal_harga_barang() {
        return total_harga_barang;
    }

    public void setTotal_harga_barang(double total_harga_barang) {
        this.total_harga_barang = total_harga_barang;
    }

    public double getTotal_dibayar() {
        return total_dibayar;
    }

    public void setTotal_dibayar(double total_dibayar) {
        this.total_dibayar = total_dibayar;
    }

    public int getTotal_anggota_mencicil() {
        return total_anggota_mencicil;
    }

    public void setTotal_anggota_mencicil(int total_anggota_mencicil) {
        this.total_anggota_mencicil = total_anggota_mencicil;
    }

    public List<CicilanAnggota> getData() {
        return data != null ? data : new ArrayList<>();
    }

    public void setData(List<CicilanAnggota> data) {
        this.data = data;
    }
}
