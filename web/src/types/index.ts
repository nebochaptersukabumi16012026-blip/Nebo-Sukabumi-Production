export interface User {
  id: number;
  username: string;
  role: string; // ADMIN, BENDAHARA, DEVELOPER, MEMBER, GUEST, ANGGOTA
  nama: string;
  nra?: string;
  require_new_password?: boolean;
  request_id?: number;
  status_verifikasi?: string;
  is_verified?: boolean;
}

export interface Anggota {
  id: number;
  nama: string;
  nra: string;
  alamat?: string;
  no_hp?: string;
  statusAktif?: number | string;
  role: string;
  username: string;
  uang_kas?: number;
  iuran_aniv?: number;
  harga_barang?: number;
  total_cicilan?: number;
  sisa_cicilan?: number;
  cicilan_per_bulan?: number;
  foto?: string;
  status_verifikasi?: string;
}

export interface DashboardData {
  total_anggota?: number;
  total_kas?: number;
  total_anniversary?: number;
  kas_anniversary?: number;
  kas_keliling?: number;
  saldo_kas_keliling?: number;
  saldo_cicilan?: number;
  total_sisa_cicilan?: number;
  total_harga_barang?: number;
  total_sudah_dibayar?: number;
  anggota_mencicil?: number;
  pemasukan_kas_keliling?: number;
  pengeluaran_kas_keliling?: number;
  pemasukan_kas?: number;
  pengeluaran_kas?: number;
  total_saldo?: number;
  total_pengeluaran?: number;
  belum_bayar_kas?: number;
  belum_kas?: number;
  kas_utama?: {
    total_pemasukan: number;
    total_pengeluaran: number;
    saldo_kas: number;
  };
  kas_keliling_data?: {
    total_pemasukan: number;
    total_pengeluaran: number;
    saldo_keliling: number;
  };
  kas_anniversary_data?: {
    total_pemasukan: number;
    total_pengeluaran: number;
    saldo_aniv: number;
  };
  cicilan?: {
    total_harga_barang: number;
    total_sudah_dibayar: number;
    total_sisa_cicilan: number;
    anggota_mencicil: number;
  };
  user?: User;
}

export interface CicilanAktifItem {
  id: number;
  nama: string;
  nra?: string;
  harga_barang: number;
  sudah_dibayar: number;
  sisa_cicilan: number;
  cicilan_per_bulan: number;
}

export interface CicilanTransaction {
  id: number;
  anggota_id: number;
  nominal: number;
  tanggal: string;
  keterangan: string;
}

export interface RiwayatKasItem {
  id: number;
  nra?: string;
  nama?: string;
  id_anggota?: number;
  nominal: number;
  tanggal: string;
  keterangan: string;
  status?: string;
  jenis?: string; // MASUK / KELUAR
}

export interface KasKelilingItem {
  id: number;
  jenis_transaksi: string; // PEMASUKAN / PENGELUARAN
  nominal: number;
  tanggal: string;
  keterangan: string;
  bulan?: string;
  tahun?: string;
  created_by?: string;
}

export interface PengeluaranItem {
  id: number;
  keterangan: string;
  nominal: number;
  tanggal: string;
  jenis_kas: string; // kas_utama, kas_keliling, kas_aniv
  created_by?: string;
}

export interface LaporanResponse {
  kas_utama?: {
    total_pemasukan: number;
    total_pengeluaran: number;
    saldo_kas: number;
  };
  kas_keliling?: {
    total_pemasukan: number;
    total_pengeluaran: number;
    saldo_keliling: number;
  };
  kas_anniversary?: {
    total_pemasukan: number;
    total_pengeluaran: number;
    saldo_aniv: number;
  };
  cicilan?: {
    total_harga_barang: number;
    total_sudah_dibayar: number;
    total_sisa_cicilan: number;
  };
}

export interface ApiResponse<T> {
  status: string;
  success?: boolean;
  message?: string;
  data?: T;
  total?: number;
  id?: number;
}
