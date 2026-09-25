package com.example.java;

import com.google.gson.annotations.SerializedName;

public class CicilanAnggota {
    @SerializedName("id")
    private int id = 0;

    @SerializedName("nama")
    private String nama = "";

    @SerializedName("nra")
    private String nra = "";

    @SerializedName("harga_barang")
    private double harga_barang = 0.0;

    @SerializedName("total_dibayar")
    private double total_dibayar = 0.0;

    @SerializedName("sudah_dibayar")
    private double sudah_dibayar = 0.0;

    @SerializedName("sisa_cicilan")
    private double sisa_cicilan = 0.0;

    @SerializedName("cicilan_per_bulan")
    private double cicilan_per_bulan = 0.0;

    public CicilanAnggota() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNama() {
        return nama != null ? nama : "";
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getNra() {
        return nra != null ? nra : "-";
    }

    public void setNra(String nra) {
        this.nra = nra;
    }

    public double getHarga_barang() {
        return harga_barang;
    }

    public void setHarga_barang(double harga_barang) {
        this.harga_barang = harga_barang;
    }

    public double getTotal_dibayar() {
        return total_dibayar;
    }

    public void setTotal_dibayar(double total_dibayar) {
        this.total_dibayar = total_dibayar;
    }

    public double getSudah_dibayar() {
        return sudah_dibayar;
    }

    public void setSudah_dibayar(double sudah_dibayar) {
        this.sudah_dibayar = sudah_dibayar;
    }

    public double getSisa_cicilan() {
        return sisa_cicilan;
    }

    public void setSisa_cicilan(double sisa_cicilan) {
        this.sisa_cicilan = sisa_cicilan;
    }

    public double getCicilan_per_bulan() {
        return cicilan_per_bulan;
    }

    public void setCicilan_per_bulan(double cicilan_per_bulan) {
        this.cicilan_per_bulan = cicilan_per_bulan;
    }
}
