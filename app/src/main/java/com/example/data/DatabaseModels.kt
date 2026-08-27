package com.example.data

import androidx.room.*
import com.squareup.moshi.Json

@Entity(
    tableName = "anggota",
    indices = [Index(value = ["nra"], unique = true)]
)
data class Anggota(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String = "",
    val nra: String = "",
    val alamat: String = "",
    @Json(name = "no_wa") @ColumnInfo(name = "no_hp") val nomorTelepon: String = "",
    val statusAktif: Int = 1,
    val role: String = "ANGGOTA", // "BENDAHARA", "ANGGOTA"
    val username: String = "",
    val password: String = "",
    @Json(name = "tgl_gabung") @ColumnInfo(name = "tanggal_bergabung") val tanggalBergabung: String = "",
    @Json(name = "uang_kas") @ColumnInfo(name = "uang_kas") val uangKas: Double = 0.0,
    @Json(name = "iuran_aniv") @ColumnInfo(name = "iuran_aniv") val iuranAniv: Double = 0.0,
    @Json(name = "harga_barang") @ColumnInfo(name = "harga_barang") val hargaBarang: Double = 0.0,
    @Json(name = "totalTagihan") @ColumnInfo(name = "total_tagihan") val totalTagihan: Double = 0.0,
    @Json(name = "total_cicilan") @ColumnInfo(name = "total_cicilan") val totalCicilan: Double = 0.0,
    @Json(name = "sisa_cicilan") @ColumnInfo(name = "sisa_cicilan") val sisaCicilan: Double = 0.0,
    @Json(name = "lamaCicilan") @ColumnInfo(name = "lama_cicilan") val lamaCicilan: Int = 0,
    @Json(name = "cicilan_per_bulan") @ColumnInfo(name = "cicilan_per_bulan") val cicilanPerBulan: Double = 0.0,
    val foto: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pembayaran")
data class Pembayaran(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "firestore_id") val firestoreId: String = "",
    @ColumnInfo(name = "anggota_id") val anggotaId: Int = 0,
    @ColumnInfo(name = "anggota_nama") val anggotaNama: String = "",
    @ColumnInfo(name = "jenis_pembayaran") val jenisPembayaran: String = "", // "KAS" or "ANIV" or "CICILAN"
    val nominal: Double = 0.0,
    @ColumnInfo(name = "tanggal_bayar") val tanggalBayar: Long = System.currentTimeMillis(),
    val status: String = "LUNAS",
    @ColumnInfo(name = "bukti_pembayaran") val buktiPembayaran: String? = null,
    val keterangan: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pengeluaran")
data class Pengeluaran(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "firestore_id") val firestoreId: String = "",
    @Json(name = "jenis_kas") @ColumnInfo(name = "jenis_kas") val jenisKas: String = "", 
    val nominal: Double = 0.0,
    val keterangan: String = "",
    val tanggal: Long = System.currentTimeMillis(),
    val bukti: String? = null,
    @Json(name = "created_by") @ColumnInfo(name = "created_by") val createdBy: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "kas_keliling")
data class KasKeliling(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "firestore_id") val firestoreId: String = "",
    @ColumnInfo(name = "jenis_transaksi") val jenisTransaksi: String = "Pemasukan",
    val nominal: Double = 0.0,
    val tanggal: Long = System.currentTimeMillis(),
    val keterangan: String = "",
    val bulan: String = "",
    val tahun: String = "",
    @Json(name = "total_pemasukan") @ColumnInfo(name = "total_pemasukan") val totalPemasukan: Double = 0.0,
    @Json(name = "total_pengeluaran") @ColumnInfo(name = "total_pengeluaran") val totalPengeluaran: Double = 0.0,
    @Json(name = "saldo") @ColumnInfo(name = "saldo_bulan") val saldoBulan: Double = 0.0,
    val catatan: String = "",
    @ColumnInfo(name = "created_by") val createdBy: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface AnggotaDao { }
@Dao
interface PembayaranDao { }
@Dao
interface PengeluaranDao { }
@Dao
interface KasKelilingDao { }

@Database(entities = [Anggota::class, Pembayaran::class, Pengeluaran::class, KasKeliling::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun anggotaDao(): AnggotaDao
    abstract fun pembayaranDao(): PembayaranDao
    abstract fun pengeluaranDao(): PengeluaranDao
    abstract fun kasKelilingDao(): KasKelilingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nebo_sukabumi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
