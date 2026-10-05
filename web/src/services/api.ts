import {
  ApiResponse,
  User,
  Anggota,
  DashboardData,
  CicilanAktifItem,
  CicilanTransaction,
  RiwayatKasItem,
  KasKelilingItem,
  PengeluaranItem,
  LaporanResponse
} from '../types';

// Determine Base URL: Use relative /api/ when running on domain or fallback to full domain
const BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
  ? 'https://nebosukabumi.net/api/'
  : '/api/';

async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const url = `${BASE_URL}${endpoint}`;
  
  const defaultHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    'Cache-Control': 'no-cache',
  };

  const config: RequestInit = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...(options.headers || {}),
    },
  };

  try {
    const res = await fetch(url, config);
    if (!res.ok) {
      let errMsg = `HTTP Error ${res.status}`;
      try {
        const errJson = await res.json();
        if (errJson && errJson.message) errMsg = errJson.message;
      } catch (_) {}
      throw new Error(errMsg);
    }
    const json = await res.json();
    return json;
  } catch (err: any) {
    console.error(`API Error (${endpoint}):`, err);
    throw err;
  }
}

export const apiService = {
  // Auth
  async login(username: string, password: string): Promise<ApiResponse<User>> {
    return fetchApi<User>('login.php', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
  },

  // Dashboard
  async getDashboard(): Promise<ApiResponse<DashboardData>> {
    return fetchApi<DashboardData>('get_dashboard.php');
  },

  // Anggota
  async getAnggotaList(): Promise<ApiResponse<Anggota[]>> {
    return fetchApi<Anggota[]>('get_anggota.php');
  },

  async getDetailAnggota(id: number): Promise<ApiResponse<Anggota>> {
    return fetchApi<Anggota>(`get_detail_anggota.php?id=${id}`);
  },

  async addAnggota(data: Partial<Anggota>): Promise<ApiResponse<any>> {
    return fetchApi<any>('anggota.php', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async updateAnggota(data: Partial<Anggota>): Promise<ApiResponse<any>> {
    return fetchApi<any>('anggota.php', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async deleteAnggota(id: number, role: string): Promise<ApiResponse<any>> {
    return fetchApi<any>('anggota.php', {
      method: 'DELETE',
      body: JSON.stringify({ id, role }),
    });
  },

  // Cicilan
  async getDaftarCicilanAktif(): Promise<ApiResponse<CicilanAktifItem[]>> {
    return fetchApi<CicilanAktifItem[]>('get_daftar_cicilan_aktif.php');
  },

  async getCicilanHistory(): Promise<ApiResponse<CicilanTransaction[]>> {
    return fetchApi<CicilanTransaction[]>('cicilan.php');
  },

  async addCicilan(anggota_id: number, nominal: number, tanggal: string, keterangan: string, role: string): Promise<ApiResponse<any>> {
    return fetchApi<any>('cicilan.php', {
      method: 'POST',
      body: JSON.stringify({ anggota_id, nominal, tanggal, keterangan, role }),
    });
  },

  async updateCicilan(id: number, anggota_id: number, nominal: number, tanggal: string, keterangan: string, role: string): Promise<ApiResponse<any>> {
    return fetchApi<any>('cicilan.php', {
      method: 'PUT',
      body: JSON.stringify({ id, anggota_id, nominal, tanggal, keterangan, role }),
    });
  },

  async deleteCicilan(id: number, role: string): Promise<ApiResponse<any>> {
    return fetchApi<any>('cicilan.php', {
      method: 'DELETE',
      body: JSON.stringify({ id, role }),
    });
  },

  // Kas
  async getDetailKas(): Promise<ApiResponse<any>> {
    return fetchApi<any>('detail_kas.php');
  },

  async getPembayaranList(): Promise<ApiResponse<any[]>> {
    return fetchApi<any[]>('pembayaran.php');
  },

  async addKas(id_anggota: number, nominal: number, keterangan: string, role: string): Promise<ApiResponse<any>> {
    return fetchApi<any>('tambah_kas.php', {
      method: 'POST',
      body: JSON.stringify({ id_anggota, nominal, keterangan, role }),
    });
  },

  async deleteKas(id: number, role: string): Promise<ApiResponse<any>> {
    return fetchApi<any>('delete_kas.php', {
      method: 'POST',
      body: JSON.stringify({ id, role }),
    });
  },

  // Kas Keliling
  async getKasKeliling(): Promise<any> {
    return fetchApi<any>('kas_keliling.php');
  },

  async addKasKeliling(data: Partial<KasKelilingItem>): Promise<ApiResponse<any>> {
    return fetchApi<any>('kas_keliling.php', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async updateKasKeliling(data: Partial<KasKelilingItem>): Promise<ApiResponse<any>> {
    return fetchApi<any>('kas_keliling.php', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async deleteKasKeliling(id: number): Promise<ApiResponse<any>> {
    return fetchApi<any>('kas_keliling.php', {
      method: 'DELETE',
      body: JSON.stringify({ id }),
    });
  },

  // Pengeluaran
  async getPengeluaran(): Promise<any> {
    return fetchApi<any>('pengeluaran.php');
  },

  async addPengeluaran(data: Partial<PengeluaranItem>): Promise<ApiResponse<any>> {
    return fetchApi<any>('pengeluaran.php', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async updatePengeluaran(data: Partial<PengeluaranItem>): Promise<ApiResponse<any>> {
    return fetchApi<any>('pengeluaran.php', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async deletePengeluaran(id: number): Promise<ApiResponse<any>> {
    return fetchApi<any>('pengeluaran.php', {
      method: 'DELETE',
      body: JSON.stringify({ id }),
    });
  },

  // Laporan
  async getLaporan(): Promise<ApiResponse<LaporanResponse>> {
    return fetchApi<LaporanResponse>('get_laporan.php');
  },
};
