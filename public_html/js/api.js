/**
 * NEBO Sukabumi Web Application - API Client & Core Logic
 * Menggunakan Endpoint API PHP yang sama dengan aplikasi Android
 */

const API_BASE = (window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1'))
    ? 'https://nebosukabumi.net/api'
    : (window.location.pathname.includes('/public_html') ? '/public_html/api' : '/api');

// Fallback endpoints jika diakses direct file atau cross-domain
const API_URLS = [
    API_BASE,
    'api',
    '/api',
    'https://nebosukabumi.net/api'
];

const NeboApi = {
    // Current session
    currentUser: null,

    init() {
        try {
            const saved = localStorage.getItem('nebo_user_session');
            if (saved) {
                this.currentUser = JSON.parse(saved);
            }
        } catch (e) {
            console.warn('Gagal membaca sesi lokal', e);
        }
    },

    getUser() {
        return this.currentUser;
    },

    setUser(user) {
        this.currentUser = user;
        if (user) {
            localStorage.setItem('nebo_user_session', JSON.stringify(user));
        } else {
            localStorage.removeItem('nebo_user_session');
        }
    },

    logout() {
        this.setUser(null);
        window.location.reload();
    },

    isAdmin() {
        if (!this.currentUser) return false;
        const r = (this.currentUser.role || '').toUpperCase();
        return ['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS'].includes(r);
    },

    /**
     * Universal API Request
     */
    async request(endpoint, options = {}) {
        let lastError = null;
        const cleanEndpoint = endpoint.startsWith('/') ? endpoint.substring(1) : endpoint;

        for (const base of API_URLS) {
            try {
                const url = `${base.replace(/\/$/, '')}/${cleanEndpoint}`;
                const fetchOptions = {
                    ...options,
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                        'Cache-Control': 'no-cache, no-store, must-revalidate',
                        ...(options.headers || {})
                    }
                };

                const response = await fetch(url + (url.includes('?') ? '&' : '?') + 't=' + Date.now(), fetchOptions);
                const text = await response.text();
                
                try {
                    const json = JSON.parse(text);
                    if (response.ok || json.status === 'success' || json.status === true || json.data) {
                        return json;
                    } else {
                        throw new Error(json.message || json.message_detail || `HTTP ${response.status}`);
                    }
                } catch (pe) {
                    if (response.ok) {
                        throw new Error('Format respon API bukan JSON valid');
                    }
                    throw new Error(`HTTP ${response.status}: ${text.substring(0, 100)}`);
                }
            } catch (err) {
                lastError = err;
                // coba endpoint berikutnya
            }
        }

        throw lastError || new Error('Gagal menghubungkan ke server API NEBO.');
    },

    // 1. Dashboard
    async getDashboard() {
        return await this.request('get_dashboard.php');
    },

    // 2. Anggota
    async getAnggota() {
        return await this.request('get_anggota.php');
    },

    async tambahAnggota(data) {
        return await this.request('tambah_anggota.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async updateAnggota(data) {
        return await this.request('anggota.php', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async deleteAnggota(id) {
        return await this.request('anggota.php', {
            method: 'DELETE',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 3. Kas Utama / Detail Kas
    async getDetailKas() {
        return await this.request('detail_kas.php');
    },

    async inputKas(data) {
        return await this.request('input_kas.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async deleteRiwayatKas(id) {
        return await this.request('delete_riwayat_kas.php', {
            method: 'POST',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 4. Kas Keliling
    async getKasKeliling() {
        return await this.request('kas_keliling.php');
    },

    async tambahKasKeliling(data) {
        return await this.request('tambah_kas_keliling.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async deleteKasKeliling(id) {
        return await this.request('kas_keliling.php', {
            method: 'DELETE',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 5. Pembayaran
    async getPembayaran() {
        return await this.request('pembayaran.php');
    },

    async addPembayaran(data) {
        return await this.request('pembayaran.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    // 6. Pengeluaran
    async getPengeluaran() {
        return await this.request('pengeluaran.php');
    },

    async addPengeluaran(data) {
        return await this.request('pengeluaran.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async deletePengeluaran(id) {
        return await this.request('pengeluaran.php', {
            method: 'DELETE',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 7. Cicilan
    async getDaftarCicilanAktif() {
        return await this.request('get_daftar_cicilan_aktif.php');
    },

    async getCicilan() {
        return await this.request('cicilan.php');
    },

    async addCicilan(data) {
        return await this.request('cicilan.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async deleteCicilan(id) {
        return await this.request('hapus_cicilan.php', {
            method: 'POST',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 8. Anniversary
    async getIuranAniv() {
        return await this.request('iuran_anniversary.php');
    },

    async addIuranAniv(data) {
        return await this.request('input_aniv.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async deleteIuranAniv(id) {
        return await this.request('delete_riwayat_aniv.php', {
            method: 'POST',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 9. Laporan
    async getLaporan() {
        return await this.request('get_laporan.php');
    },

    // 10. Absensi
    async getAbsensi() {
        return await this.request('absensi.php');
    },

    async addAbsensi(data) {
        return await this.request('absensi.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async deleteAbsensi(id) {
        return await this.request('absensi.php', {
            method: 'DELETE',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 11. Catatan
    async getCatatan() {
        return await this.request('catatan.php');
    },

    async addCatatan(data) {
        return await this.request('catatan.php', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    async deleteCatatan(id) {
        return await this.request('catatan.php', {
            method: 'DELETE',
            body: JSON.stringify({ id: Number(id) })
        });
    },

    // 12. Pengaturan
    async getSettings() {
        return await this.request('community_settings.php');
    },

    async saveSettings(data) {
        return await this.request('community_settings.php', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    // 13. Login
    async login(username, password) {
        const res = await this.request('login.php', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
        if (res.data || res.user) {
            const userData = res.data || res.user;
            this.setUser(userData);
        }
        return res;
    }
};

// Utilities
function formatRupiah(amount) {
    const num = Number(amount) || 0;
    return 'Rp ' + num.toLocaleString('id-ID');
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    if (!isNaN(dateStr) && String(dateStr).length >= 10) {
        const d = new Date(Number(dateStr) > 9999999999 ? Number(dateStr) : Number(dateStr) * 1000);
        return d.toLocaleDateString('id-ID', { year: 'numeric', month: 'short', day: 'numeric' });
    }
    const d = new Date(dateStr);
    if (!isNaN(d.getTime())) {
        return d.toLocaleDateString('id-ID', { year: 'numeric', month: 'short', day: 'numeric' });
    }
    return dateStr;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'toast';
    
    let icon = 'fa-info-circle';
    let color = '#38bdf8';
    if (type === 'success') {
        icon = 'fa-check-circle';
        color = '#34d399';
    } else if (type === 'error') {
        icon = 'fa-triangle-exclamation';
        color = '#fb7185';
    }

    toast.innerHTML = `<i class="fa-solid ${icon}" style="color: ${color}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}
