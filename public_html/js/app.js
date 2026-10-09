/**
 * NEBO Sukabumi Web Application - UI Controller & Menu Router
 */

let currentRoute = 'dashboard';
let cachedAnggotaList = [];

document.addEventListener('DOMContentLoaded', () => {
    NeboApi.init();
    setupNavigation();
    setupUserInterface();
    navigate('dashboard');

    // Polling sinkronisasi dashboard tiap 30 detik
    setInterval(() => {
        if (currentRoute === 'dashboard') {
            loadDashboard(false);
        }
    }, 30000);
});

function setupUserInterface() {
    const user = NeboApi.getUser();
    const userNameEl = document.getElementById('sidebarUserName');
    const userRoleEl = document.getElementById('sidebarUserRole');
    const userAvatarEl = document.getElementById('sidebarUserAvatar');
    const authBtnText = document.getElementById('sidebarAuthText');
    const authBtnIcon = document.getElementById('sidebarAuthIcon');

    if (user) {
        if (userNameEl) userNameEl.innerText = user.nama || user.username || 'Anggota NEBO';
        if (userRoleEl) userRoleEl.innerText = (user.role || 'ANGGOTA').toUpperCase();
        if (userAvatarEl) {
            const initial = (user.nama || user.username || 'N').charAt(0).toUpperCase();
            userAvatarEl.innerText = initial;
        }
        if (authBtnText) authBtnText.innerText = 'Keluar (Logout)';
        if (authBtnIcon) authBtnIcon.className = 'fa-solid fa-right-from-bracket';
    } else {
        if (userNameEl) userNameEl.innerText = 'Guest (Tamu)';
        if (userRoleEl) userRoleEl.innerText = 'BACA SAJA';
        if (userAvatarEl) userAvatarEl.innerText = 'G';
        if (authBtnText) authBtnText.innerText = 'Masuk (Login)';
        if (authBtnIcon) authBtnIcon.className = 'fa-solid fa-right-to-bracket';
    }
}

function handleAuthAction() {
    if (NeboApi.getUser()) {
        if (confirm('Apakah Anda yakin ingin keluar (logout)?')) {
            NeboApi.logout();
        }
    } else {
        openModal('modalLogin');
    }
}

function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item[data-route]');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const route = item.getAttribute('data-route');
            navigate(route);

            // Close sidebar on mobile
            const sidebar = document.getElementById('appSidebar');
            if (sidebar && window.innerWidth <= 992) {
                sidebar.classList.remove('open');
            }
        });
    });

    const menuToggle = document.getElementById('menuToggleBtn');
    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            const sidebar = document.getElementById('appSidebar');
            if (sidebar) sidebar.classList.toggle('open');
        });
    }
}

function navigate(route) {
    currentRoute = route;
    
    // Update active nav item
    document.querySelectorAll('.nav-item[data-route]').forEach(el => {
        if (el.getAttribute('data-route') === route) {
            el.classList.add('active');
        } else {
            el.classList.remove('active');
        }
    });

    // Update Topbar Title
    const titleMap = {
        'dashboard': { title: 'Dashboard Utama', desc: 'Rangkuman Keuangan & Statistik Komunitas' },
        'anggota': { title: 'Data Anggota', desc: 'Daftar Anggota Terverifikasi NEBO Sukabumi' },
        'kas_utama': { title: 'Kas Utama', desc: 'Rekap Iuran Pokok & Transaksi Kas' },
        'kas_keliling': { title: 'Kas Keliling', desc: 'Kas Operasional & Rekap Bulanan' },
        'pembayaran': { title: 'Riwayat Pembayaran', desc: 'Semua Transaksi Masuk Anggota' },
        'pengeluaran': { title: 'Pengeluaran Kas', desc: 'Catatan Pengeluaran Biaya Komunitas' },
        'cicilan': { title: 'Cicilan & Arisan', desc: 'Daftar Tagihan & Status Pelunasan' },
        'anniversary': { title: 'Iuran Anniversary', desc: 'Dana & Target Acara Anniversary' },
        'laporan': { title: 'Laporan Keuangan', desc: 'Neraca Komprehensif Kas & Operasional' },
        'absensi': { title: 'Absensi Kegiatan', desc: 'Daftar Hadir Anggota & Pertemuan' },
        'catatan': { title: 'Catatan Bebas', desc: 'Agenda, Memo & Pengumuman Komunitas' },
        'pengaturan': { title: 'Pengaturan', desc: 'Profil Komunitas & Target Iuran' }
    };

    const header = titleMap[route] || { title: 'NEBO Sukabumi', desc: 'Sistem Terpadu Komunitas' };
    const pageTitle = document.getElementById('pageTitle');
    const pageDesc = document.getElementById('pageDesc');
    if (pageTitle) pageTitle.innerText = header.title;
    if (pageDesc) pageDesc.innerText = header.desc;

    // Load content
    renderRouteContent(route);
}

function renderRouteContent(route) {
    const container = document.getElementById('mainContentArea');
    if (!container) return;

    container.innerHTML = `
        <div class="state-box">
            <i class="fa-solid fa-spinner fa-spin state-icon" style="color: var(--primary);"></i>
            <div class="state-title">Memuat data server...</div>
            <div class="state-desc">Menghubungkan ke API /api/${route === 'dashboard' ? 'get_dashboard.php' : route + '.php'}</div>
        </div>
    `;

    switch (route) {
        case 'dashboard': loadDashboard(true); break;
        case 'anggota': loadAnggota(); break;
        case 'kas_utama': loadKasUtama(); break;
        case 'kas_keliling': loadKasKeliling(); break;
        case 'pembayaran': loadPembayaran(); break;
        case 'pengeluaran': loadPengeluaran(); break;
        case 'cicilan': loadCicilan(); break;
        case 'anniversary': loadAnniversary(); break;
        case 'laporan': loadLaporan(); break;
        case 'absensi': loadAbsensi(); break;
        case 'catatan': loadCatatan(); break;
        case 'pengaturan': loadPengaturan(); break;
        default: loadDashboard(true); break;
    }
}

// =========================================================================
// 1. DASHBOARD
// =========================================================================
async function loadDashboard(showLoading = true) {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getDashboard();
        const data = res.data || res;

        const saldoKasUtama = Number(data.saldo_kas_utama ?? data.saldo_kas ?? 0);
        const kasKeliling = Number(data.kas_keliling ?? data.saldo_kas_keliling ?? 0);
        const kasAniv = Number(data.kas_anniversary ?? data.total_anniversary ?? 0);
        const saldoCicilan = Number(data.saldo_cicilan ?? data.total_sisa_cicilan ?? 0);
        const totalSaldo = Number(data.total_saldo ?? (saldoKasUtama + kasKeliling + kasAniv + saldoCicilan));
        const totalAnggota = Number(data.total_anggota ?? 0);

        const pemasukanKas = Number(data.pemasukan_kas_utama ?? data.pemasukan_kas ?? saldoKasUtama);
        const pengeluaranKas = Number(data.pengeluaran_kas_utama ?? data.pengeluaran_kas ?? 0);

        const pemasukanKeliling = Number(data.pemasukan_kas_keliling ?? data.pemasukan_kas ?? kasKeliling);
        const pengeluaranKeliling = Number(data.pengeluaran_kas_keliling ?? data.pengeluaran_kas ?? 0);

        const belumKas = Number(data.belum_kas ?? data.belum_bayar_kas ?? 0);
        const belumAniv = Number(data.belum_anniversary ?? data.belum_bayar_aniv ?? 0);

        container.innerHTML = `
            <!-- Hero Grand Total -->
            <section class="glass-panel hero-grand-total">
                <div class="hero-label">Total Akumulasi Saldo Komunitas</div>
                <div class="hero-value">${formatRupiah(totalSaldo)}</div>
                <div class="hero-sub">
                    <span><i class="fa-solid fa-users"></i> <strong>${totalAnggota}</strong> Anggota Terdaftar</span>
                    <span><i class="fa-solid fa-circle-exclamation" style="color: #fb7185;"></i> <strong>${belumKas}</strong> Belum Kas</span>
                    <span><i class="fa-solid fa-clock-rotate-left"></i> Terakhir Sinkron: <strong>${new Date().toLocaleTimeString('id-ID')}</strong></span>
                </div>
            </section>

            <!-- 4 Metric Cards -->
            <section class="cards-grid-4">
                <div class="metric-card">
                    <div class="card-top">
                        <span class="card-tag">Kas Utama</span>
                        <div class="card-icon icon-kas"><i class="fa-solid fa-vault"></i></div>
                    </div>
                    <div class="card-amount val-kas">${formatRupiah(saldoKasUtama)}</div>
                    <div class="card-desc">Saldo Pokok Kas Anggota</div>
                </div>

                <div class="metric-card">
                    <div class="card-top">
                        <span class="card-tag">Kas Keliling</span>
                        <div class="card-icon icon-keliling"><i class="fa-solid fa-hand-holding-dollar"></i></div>
                    </div>
                    <div class="card-amount val-keliling">${formatRupiah(kasKeliling)}</div>
                    <div class="card-desc">Kas Operasional Bulanan</div>
                </div>

                <div class="metric-card">
                    <div class="card-top">
                        <span class="card-tag">Kas Anniversary</span>
                        <div class="card-icon icon-aniv"><i class="fa-solid fa-cake-candles"></i></div>
                    </div>
                    <div class="card-amount val-aniv">${formatRupiah(kasAniv)}</div>
                    <div class="card-desc">${belumAniv} Anggota Belum Bayar</div>
                </div>

                <div class="metric-card">
                    <div class="card-top">
                        <span class="card-tag">Saldo Cicilan</span>
                        <div class="card-icon icon-cicilan"><i class="fa-solid fa-receipt"></i></div>
                    </div>
                    <div class="card-amount val-cicilan">${formatRupiah(saldoCicilan)}</div>
                    <div class="card-desc">Sisa Tagihan / Arisan</div>
                </div>
            </section>

            <!-- Rincian Section -->
            <section style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px;">
                <div class="glass-panel" style="padding: 24px;">
                    <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
                        <i class="fa-solid fa-chart-pie" style="color: #34d399; font-size: 18px;"></i>
                        <h3 style="font-size: 16px; font-weight: 700;">Rincian Kas Utama</h3>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 12px;">
                        <div style="display: flex; justify-content: space-between; padding: 10px 14px; background: rgba(15, 23, 42, 0.4); border-radius: 12px;">
                            <span style="color: var(--text-muted); font-size: 13px;">Pemasukan Kas</span>
                            <span style="color: #34d399; font-weight: 700;">${formatRupiah(pemasukanKas)}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; padding: 10px 14px; background: rgba(15, 23, 42, 0.4); border-radius: 12px;">
                            <span style="color: var(--text-muted); font-size: 13px;">Pengeluaran Kas</span>
                            <span style="color: #f87171; font-weight: 700;">${formatRupiah(pengeluaranKas)}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; padding: 12px 14px; background: rgba(15, 23, 42, 0.6); border-radius: 12px; border-left: 3px solid #34d399;">
                            <span style="font-weight: 700;">Saldo Bersih</span>
                            <span class="val-kas" style="font-weight: 800; font-size: 15px;">${formatRupiah(saldoKasUtama)}</span>
                        </div>
                    </div>
                </div>

                <div class="glass-panel" style="padding: 24px;">
                    <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
                        <i class="fa-solid fa-chart-line" style="color: #38bdf8; font-size: 18px;"></i>
                        <h3 style="font-size: 16px; font-weight: 700;">Rincian Kas Keliling</h3>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 12px;">
                        <div style="display: flex; justify-content: space-between; padding: 10px 14px; background: rgba(15, 23, 42, 0.4); border-radius: 12px;">
                            <span style="color: var(--text-muted); font-size: 13px;">Pemasukan Keliling</span>
                            <span style="color: #38bdf8; font-weight: 700;">${formatRupiah(pemasukanKeliling)}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; padding: 10px 14px; background: rgba(15, 23, 42, 0.4); border-radius: 12px;">
                            <span style="color: var(--text-muted); font-size: 13px;">Pengeluaran Keliling</span>
                            <span style="color: #f87171; font-weight: 700;">${formatRupiah(pengeluaranKeliling)}</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; padding: 12px 14px; background: rgba(15, 23, 42, 0.6); border-radius: 12px; border-left: 3px solid #38bdf8;">
                            <span style="font-weight: 700;">Saldo Bersih</span>
                            <span class="val-keliling" style="font-weight: 800; font-size: 15px;">${formatRupiah(kasKeliling)}</span>
                        </div>
                    </div>
                </div>
            </section>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Dashboard', err.message, () => loadDashboard(true));
    }
}

// =========================================================================
// 2. ANGGOTA
// =========================================================================
async function loadAnggota() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getAnggota();
        const list = res.data || [];
        cachedAnggotaList = list;

        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <input type="text" id="searchAnggota" class="form-control" style="width: 250px;" placeholder="Cari nama atau NRA..." onkeyup="filterAnggotaTable()">
                    <span style="color: var(--text-muted); font-size: 13px;">Total: <strong>${list.length}</strong> anggota</span>
                </div>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalTambahAnggota()"><i class="fa-solid fa-user-plus"></i> Tambah Anggota</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table" id="tableAnggota">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Nama</th>
                            <th>NRA</th>
                            <th>No WA</th>
                            <th>Uang Kas</th>
                            <th>Iuran Aniv</th>
                            <th>Sisa Cicilan</th>
                            <th>Role / Status</th>
                            ${isAdmin ? '<th>Aksi</th>' : ''}
                        </tr>
                    </thead>
                    <tbody>
                        ${list.map((m, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td>
                                    <div style="display: flex; align-items: center; gap: 10px;">
                                        <div style="width: 32px; height: 32px; border-radius: 8px; background: rgba(56, 189, 248, 0.15); color: #38bdf8; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;">
                                            ${(m.nama || 'A').charAt(0).toUpperCase()}
                                        </div>
                                        <div>
                                            <div style="font-weight: 700;">${m.nama || '-'}</div>
                                            <div style="font-size: 11px; color: var(--text-dim);">${m.alamat || '-'}</div>
                                        </div>
                                    </div>
                                </td>
                                <td><code>${m.nra || '-'}</code></td>
                                <td>${m.no_wa ? `<a href="https://wa.me/${m.no_wa.replace(/[^0-9]/g, '')}" target="_blank" style="color: #34d399; text-decoration: none;"><i class="fa-brands fa-whatsapp"></i> ${m.no_wa}</a>` : '-'}</td>
                                <td style="color: #34d399; font-weight: 700;">${formatRupiah(m.uang_kas || m.uangKas || 0)}</td>
                                <td style="color: #facc15; font-weight: 700;">${formatRupiah(m.iuran_aniv || m.iuranAniv || 0)}</td>
                                <td style="color: #fb7185; font-weight: 700;">${formatRupiah(m.sisa_cicilan || m.sisaCicilan || 0)}</td>
                                <td>
                                    <span class="badge ${m.role === 'ADMIN' ? 'badge-danger' : (m.role === 'BENDAHARA' ? 'badge-warning' : 'badge-info')}">${m.role || 'ANGGOTA'}</span>
                                </td>
                                ${isAdmin ? `
                                    <td>
                                        <button class="btn btn-secondary" style="padding: 6px 10px; font-size: 12px;" onclick="openModalEditAnggota(${m.id})"><i class="fa-solid fa-pen-to-square"></i></button>
                                        <button class="btn btn-danger" style="padding: 6px 10px; font-size: 12px;" onclick="confirmDeleteAnggota(${m.id}, '${m.nama}')"><i class="fa-solid fa-trash"></i></button>
                                    </td>
                                ` : ''}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Anggota', err.message, loadAnggota);
    }
}

function filterAnggotaTable() {
    const input = document.getElementById('searchAnggota');
    const filter = input ? input.value.toLowerCase() : '';
    const table = document.getElementById('tableAnggota');
    if (!table) return;

    const tr = table.getElementsByTagName('tr');
    for (let i = 1; i < tr.length; i++) {
        const text = tr[i].textContent || tr[i].innerText;
        tr[i].style.display = text.toLowerCase().includes(filter) ? '' : 'none';
    }
}

// =========================================================================
// 3. KAS UTAMA
// =========================================================================
async function loadKasUtama() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getDetailKas();
        const data = res.data || res;
        const riwayat = data.riwayat || [];

        const totalIn = Number(data.total_pemasukan || 0);
        const totalOut = Number(data.total_pengeluaran || 0);
        const saldo = Number(data.saldo || data.saldo_kas || 0);

        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div class="cards-grid-4">
                <div class="metric-card">
                    <div class="card-tag">Pemasukan Kas</div>
                    <div class="card-amount val-kas">${formatRupiah(totalIn)}</div>
                    <div class="card-desc">Total Kas Terkumpul</div>
                </div>
                <div class="metric-card">
                    <div class="card-tag">Pengeluaran Kas</div>
                    <div class="card-amount" style="color: #f87171;">${formatRupiah(totalOut)}</div>
                    <div class="card-desc">Total Pengeluaran Kas</div>
                </div>
                <div class="metric-card">
                    <div class="card-tag">Saldo Bersih</div>
                    <div class="card-amount val-kas">${formatRupiah(saldo)}</div>
                    <div class="card-desc">Saldo Tersedia di Rekening</div>
                </div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px;">
                <h3 style="font-size: 16px; font-weight: 700;">Riwayat Iuran Kas Anggota</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalInputKas()"><i class="fa-solid fa-plus"></i> Input Kas Anggota</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Nama Anggota</th>
                            <th>NRA</th>
                            <th>Nominal</th>
                            <th>Tanggal</th>
                            <th>Keterangan</th>
                            ${isAdmin ? '<th>Aksi</th>' : ''}
                        </tr>
                    </thead>
                    <tbody>
                        ${riwayat.length === 0 ? `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 30px;">Belum ada riwayat iuran kas.</td></tr>` : ''}
                        ${riwayat.map((r, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td style="font-weight: 700;">${r.nama || '-'}</td>
                                <td><code>${r.nra || '-'}</code></td>
                                <td style="color: #34d399; font-weight: 700;">${formatRupiah(r.nominal || 0)}</td>
                                <td>${r.tanggal || '-'}</td>
                                <td>${r.keterangan || '-'}</td>
                                ${isAdmin ? `
                                    <td>
                                        <button class="btn btn-danger" style="padding: 6px 10px; font-size: 12px;" onclick="confirmDeleteRiwayatKas(${r.id}, '${r.nama}')"><i class="fa-solid fa-trash"></i></button>
                                    </td>
                                ` : ''}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Kas Utama', err.message, loadKasUtama);
    }
}

// =========================================================================
// 4. KAS KELILING
// =========================================================================
async function loadKasKeliling() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getKasKeliling();
        const data = res.data || [];
        const totalIn = Number(res.total_pemasukan || 0);
        const totalOut = Number(res.total_pengeluaran || 0);
        const saldo = Number(res.saldo || res.saldo_kas_keliling || 0);

        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div class="cards-grid-4">
                <div class="metric-card">
                    <div class="card-tag">Pemasukan Keliling</div>
                    <div class="card-amount val-keliling">${formatRupiah(totalIn)}</div>
                    <div class="card-desc">Operasional Masuk</div>
                </div>
                <div class="metric-card">
                    <div class="card-tag">Pengeluaran Keliling</div>
                    <div class="card-amount" style="color: #f87171;">${formatRupiah(totalOut)}</div>
                    <div class="card-desc">Operasional Keluar</div>
                </div>
                <div class="metric-card">
                    <div class="card-tag">Saldo Kas Keliling</div>
                    <div class="card-amount val-keliling">${formatRupiah(saldo)}</div>
                    <div class="card-desc">Saldo Operasional Bersih</div>
                </div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px;">
                <h3 style="font-size: 16px; font-weight: 700;">Daftar Transaksi Kas Keliling</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalTambahKeliling()"><i class="fa-solid fa-plus"></i> Tambah Transaksi</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Bulan & Tahun</th>
                            <th>Tanggal</th>
                            <th>Jenis</th>
                            <th>Nominal</th>
                            <th>Keterangan / Catatan</th>
                            ${isAdmin ? '<th>Aksi</th>' : ''}
                        </tr>
                    </thead>
                    <tbody>
                        ${data.length === 0 ? `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 30px;">Belum ada data kas keliling.</td></tr>` : ''}
                        ${data.map((item, idx) => {
                            const isPemasukan = (item.jenis || item.jenis_transaksi || 'pemasukan').toLowerCase() === 'pemasukan';
                            return `
                                <tr>
                                    <td>${idx + 1}</td>
                                    <td><strong>${item.bulan || '-'} ${item.tahun || ''}</strong></td>
                                    <td>${formatDate(item.tanggal)}</td>
                                    <td>
                                        <span class="badge ${isPemasukan ? 'badge-success' : 'badge-danger'}">
                                            ${isPemasukan ? 'PEMASUKAN' : 'PENGELUARAN'}
                                        </span>
                                    </td>
                                    <td style="font-weight: 700; color: ${isPemasukan ? '#38bdf8' : '#f87171'};">
                                        ${formatRupiah(item.nominal || item.total_pemasukan || item.total_pengeluaran || 0)}
                                    </td>
                                    <td>${item.catatan || item.keterangan || '-'}</td>
                                    ${isAdmin ? `
                                        <td>
                                            <button class="btn btn-danger" style="padding: 6px 10px; font-size: 12px;" onclick="confirmDeleteKeliling(${item.id})"><i class="fa-solid fa-trash"></i></button>
                                        </td>
                                    ` : ''}
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Kas Keliling', err.message, loadKasKeliling);
    }
}

// =========================================================================
// 5. PEMBAYARAN
// =========================================================================
async function loadPembayaran() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getPembayaran();
        const list = res.data || [];
        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <h3 style="font-size: 16px; font-weight: 700;">Seluruh Riwayat Pembayaran Masuk</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalTambahPembayaran()"><i class="fa-solid fa-plus"></i> Catat Pembayaran</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Nama Anggota</th>
                            <th>Jenis Pembayaran</th>
                            <th>Nominal</th>
                            <th>Tanggal</th>
                            <th>Keterangan</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 30px;">Belum ada riwayat pembayaran.</td></tr>` : ''}
                        ${list.map((p, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td style="font-weight: 700;">${p.anggotaNama || p.nama || '-'}</td>
                                <td><span class="badge badge-info">${p.jenisPembayaran || '-'}</span></td>
                                <td style="color: #34d399; font-weight: 700;">${formatRupiah(p.nominal || 0)}</td>
                                <td>${formatDate(p.tanggal)}</td>
                                <td>${p.keterangan || '-'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Pembayaran', err.message, loadPembayaran);
    }
}

// =========================================================================
// 6. PENGELUARAN
// =========================================================================
async function loadPengeluaran() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getPengeluaran();
        const list = res.data || [];
        const total = list.reduce((acc, curr) => acc + Number(curr.nominal || 0), 0);
        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div class="cards-grid-4">
                <div class="metric-card">
                    <div class="card-tag">Total Pengeluaran</div>
                    <div class="card-amount" style="color: #fb7185;">${formatRupiah(total)}</div>
                    <div class="card-desc">${list.length} Catatan Transaksi</div>
                </div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px;">
                <h3 style="font-size: 16px; font-weight: 700;">Daftar Pengeluaran Komunitas</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalTambahPengeluaran()"><i class="fa-solid fa-plus"></i> Catat Pengeluaran</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Tanggal</th>
                            <th>Jenis Kas</th>
                            <th>Keterangan</th>
                            <th>Nominal</th>
                            <th>Dicatat Oleh</th>
                            ${isAdmin ? '<th>Aksi</th>' : ''}
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 30px;">Belum ada pengeluaran tercatat.</td></tr>` : ''}
                        ${list.map((item, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td>${formatDate(item.tanggal)}</td>
                                <td><span class="badge badge-purple">${item.jenis_kas || 'Kas Utama'}</span></td>
                                <td style="font-weight: 600;">${item.keterangan || '-'}</td>
                                <td style="color: #fb7185; font-weight: 700;">${formatRupiah(item.nominal || 0)}</td>
                                <td>${item.created_by || 'Sistem'}</td>
                                ${isAdmin ? `
                                    <td>
                                        <button class="btn btn-danger" style="padding: 6px 10px; font-size: 12px;" onclick="confirmDeletePengeluaran(${item.id})"><i class="fa-solid fa-trash"></i></button>
                                    </td>
                                ` : ''}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Pengeluaran', err.message, loadPengeluaran);
    }
}

// =========================================================================
// 7. CICILAN & ARISAN
// =========================================================================
async function loadCicilan() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getDaftarCicilanAktif();
        const list = res.data || [];

        const totalHarga = Number(res.total_harga_barang || list.reduce((a, b) => a + Number(b.harga_barang || 0), 0));
        const totalBayar = Number(res.total_sudah_dibayar || list.reduce((a, b) => a + Number(b.sudah_dibayar || 0), 0));
        const totalSisa = Number(res.total_sisa_cicilan || list.reduce((a, b) => a + Number(b.sisa_cicilan || 0), 0));

        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div class="cards-grid-4">
                <div class="metric-card">
                    <div class="card-tag">Total Harga Barang</div>
                    <div class="card-amount val-keliling">${formatRupiah(totalHarga)}</div>
                    <div class="card-desc">Nilai Keseluruhan Barang</div>
                </div>
                <div class="metric-card">
                    <div class="card-tag">Sudah Dibayar</div>
                    <div class="card-amount val-kas">${formatRupiah(totalBayar)}</div>
                    <div class="card-desc">Total Terbayar Anggota</div>
                </div>
                <div class="metric-card">
                    <div class="card-tag">Sisa Cicilan Aktif</div>
                    <div class="card-amount val-cicilan">${formatRupiah(totalSisa)}</div>
                    <div class="card-desc">${list.length} Anggota Masih Mencicil</div>
                </div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px;">
                <h3 style="font-size: 16px; font-weight: 700;">Daftar Anggota Dengan Cicilan Aktif</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalBayarCicilan()"><i class="fa-solid fa-plus"></i> Input Pembayaran Cicilan</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Nama Anggota</th>
                            <th>NRA</th>
                            <th>Harga Barang</th>
                            <th>Sudah Dibayar</th>
                            <th>Sisa Tagihan</th>
                            <th>Cicilan / Bulan</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 30px;">Tidak ada anggota yang memiliki cicilan aktif (Semua Lunas).</td></tr>` : ''}
                        ${list.map((c, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td style="font-weight: 700;">${c.nama || '-'}</td>
                                <td><code>${c.nra || '-'}</code></td>
                                <td>${formatRupiah(c.harga_barang || 0)}</td>
                                <td style="color: #34d399; font-weight: 700;">${formatRupiah(c.sudah_dibayar || 0)}</td>
                                <td style="color: #fb7185; font-weight: 800;">${formatRupiah(c.sisa_cicilan || 0)}</td>
                                <td>${formatRupiah(c.cicilan_per_bulan || 0)}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Cicilan', err.message, loadCicilan);
    }
}

// =========================================================================
// 8. ANNIVERSARY
// =========================================================================
async function loadAnniversary() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getIuranAniv();
        const list = res.data || [];
        const total = list.reduce((a, b) => a + Number(b.nominal || 0), 0);
        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div class="cards-grid-4">
                <div class="metric-card">
                    <div class="card-tag">Total Iuran Terkumpul</div>
                    <div class="card-amount val-aniv">${formatRupiah(total)}</div>
                    <div class="card-desc">${list.length} Kali Pembayaran</div>
                </div>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px;">
                <h3 style="font-size: 16px; font-weight: 700;">Riwayat Iuran Anniversary</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalTambahAniv()"><i class="fa-solid fa-plus"></i> Input Iuran Anniversary</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>ID Anggota</th>
                            <th>Nominal</th>
                            <th>Tanggal</th>
                            <th>Keterangan</th>
                            ${isAdmin ? '<th>Aksi</th>' : ''}
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 30px;">Belum ada iuran anniversary.</td></tr>` : ''}
                        ${list.map((a, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td>Anggota #${a.anggota_id}</td>
                                <td style="color: #facc15; font-weight: 700;">${formatRupiah(a.nominal || 0)}</td>
                                <td>${formatDate(a.tanggal)}</td>
                                <td>${a.keterangan || '-'}</td>
                                ${isAdmin ? `
                                    <td>
                                        <button class="btn btn-danger" style="padding: 6px 10px; font-size: 12px;" onclick="confirmDeleteAniv(${a.id})"><i class="fa-solid fa-trash"></i></button>
                                    </td>
                                ` : ''}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Anniversary', err.message, loadAnniversary);
    }
}

// =========================================================================
// 9. LAPORAN
// =========================================================================
async function loadLaporan() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getLaporan();
        const ku = res.kas_utama || {};
        const kk = res.kas_keliling || {};
        const ka = res.kas_anniversary || {};
        const cc = res.cicilan || {};

        container.innerHTML = `
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px;">
                <!-- Kas Utama -->
                <div class="glass-panel" style="padding: 24px;">
                    <h3 style="color: #34d399; font-size: 16px; margin-bottom: 14px;"><i class="fa-solid fa-vault"></i> Kas Utama</h3>
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <div style="display: flex; justify-content: space-between;"><span>Total Pemasukan:</span> <strong style="color: #34d399;">${formatRupiah(ku.total_pemasukan || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between;"><span>Total Pengeluaran:</span> <strong style="color: #f87171;">${formatRupiah(ku.total_pengeluaran || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between; border-top: 1px solid var(--border-glass); padding-top: 8px;"><span>Saldo Bersih:</span> <strong class="val-kas">${formatRupiah(ku.saldo_kas || 0)}</strong></div>
                    </div>
                </div>

                <!-- Kas Keliling -->
                <div class="glass-panel" style="padding: 24px;">
                    <h3 style="color: #38bdf8; font-size: 16px; margin-bottom: 14px;"><i class="fa-solid fa-hand-holding-dollar"></i> Kas Keliling</h3>
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <div style="display: flex; justify-content: space-between;"><span>Total Pemasukan:</span> <strong style="color: #38bdf8;">${formatRupiah(kk.total_pemasukan || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between;"><span>Total Pengeluaran:</span> <strong style="color: #f87171;">${formatRupiah(kk.total_pengeluaran || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between; border-top: 1px solid var(--border-glass); padding-top: 8px;"><span>Saldo Bersih:</span> <strong class="val-keliling">${formatRupiah(kk.saldo_keliling || 0)}</strong></div>
                    </div>
                </div>

                <!-- Kas Anniversary -->
                <div class="glass-panel" style="padding: 24px;">
                    <h3 style="color: #facc15; font-size: 16px; margin-bottom: 14px;"><i class="fa-solid fa-cake-candles"></i> Kas Anniversary</h3>
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <div style="display: flex; justify-content: space-between;"><span>Total Pemasukan:</span> <strong style="color: #facc15;">${formatRupiah(ka.total_pemasukan || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between;"><span>Total Pengeluaran:</span> <strong style="color: #f87171;">${formatRupiah(ka.total_pengeluaran || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between; border-top: 1px solid var(--border-glass); padding-top: 8px;"><span>Saldo Bersih:</span> <strong class="val-aniv">${formatRupiah(ka.saldo_aniv || 0)}</strong></div>
                    </div>
                </div>

                <!-- Cicilan -->
                <div class="glass-panel" style="padding: 24px;">
                    <h3 style="color: #fb7185; font-size: 16px; margin-bottom: 14px;"><i class="fa-solid fa-receipt"></i> Cicilan & Arisan</h3>
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <div style="display: flex; justify-content: space-between;"><span>Total Nilai Barang:</span> <strong>${formatRupiah(cc.total_harga_barang || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between;"><span>Total Sudah Dibayar:</span> <strong style="color: #34d399;">${formatRupiah(cc.total_sudah_dibayar || 0)}</strong></div>
                        <div style="display: flex; justify-content: space-between; border-top: 1px solid var(--border-glass); padding-top: 8px;"><span>Sisa Piutang:</span> <strong class="val-cicilan">${formatRupiah(cc.total_sisa_cicilan || 0)}</strong></div>
                    </div>
                </div>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Laporan', err.message, loadLaporan);
    }
}

// =========================================================================
// 10. ABSENSI
// =========================================================================
async function loadAbsensi() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getAbsensi();
        const list = res.data || [];
        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <h3 style="font-size: 16px; font-weight: 700;">Daftar Hadir Anggota</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalTambahAbsensi()"><i class="fa-solid fa-plus"></i> Catat Absensi</button>` : ''}
            </div>

            <div class="glass-panel table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>ID Anggota</th>
                            <th>Tanggal</th>
                            <th>Status Hadir</th>
                            <th>Keterangan</th>
                            ${isAdmin ? '<th>Aksi</th>' : ''}
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 30px;">Belum ada data absensi.</td></tr>` : ''}
                        ${list.map((item, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td>Anggota #${item.anggota_id}</td>
                                <td>${item.tanggal}</td>
                                <td><span class="badge ${item.status === 'Hadir' ? 'badge-success' : 'badge-danger'}">${item.status}</span></td>
                                <td>${item.keterangan || '-'}</td>
                                ${isAdmin ? `
                                    <td>
                                        <button class="btn btn-danger" style="padding: 6px 10px; font-size: 12px;" onclick="confirmDeleteAbsensi(${item.id})"><i class="fa-solid fa-trash"></i></button>
                                    </td>
                                ` : ''}
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Absensi', err.message, loadAbsensi);
    }
}

// =========================================================================
// 11. CATATAN
// =========================================================================
async function loadCatatan() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getCatatan();
        const list = res.data || [];
        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <h3 style="font-size: 16px; font-weight: 700;">Agenda & Catatan Bebas Komunitas</h3>
                ${isAdmin ? `<button class="btn btn-primary" onclick="openModalTambahCatatan()"><i class="fa-solid fa-plus"></i> Buat Catatan Baru</button>` : ''}
            </div>

            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 18px; margin-top: 14px;">
                ${list.length === 0 ? `<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px;">Belum ada catatan yang tersimpan.</div>` : ''}
                ${list.map(c => `
                    <div class="glass-panel" style="padding: 20px; display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px;">
                                <h4 style="font-size: 15px; font-weight: 700; color: #ffffff;">${c.judul || '-'}</h4>
                                ${isAdmin ? `<button onclick="confirmDeleteCatatan(${c.id})" style="background: none; border: none; color: #fb7185; cursor: pointer;"><i class="fa-solid fa-trash"></i></button>` : ''}
                            </div>
                            <p style="font-size: 13px; color: var(--text-muted); line-height: 1.5; white-space: pre-wrap;">${c.isi || '-'}</p>
                        </div>
                        <div style="margin-top: 16px; font-size: 11px; color: var(--text-dim); border-top: 1px solid var(--border-glass); padding-top: 10px;">
                            <i class="fa-regular fa-calendar"></i> ${formatDate(c.tanggal)}
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Catatan', err.message, loadCatatan);
    }
}

// =========================================================================
// 12. PENGATURAN
// =========================================================================
async function loadPengaturan() {
    const container = document.getElementById('mainContentArea');
    try {
        const res = await NeboApi.getSettings();
        const settings = res.data || res;
        const isAdmin = NeboApi.isAdmin();

        container.innerHTML = `
            <div class="glass-panel" style="padding: 28px; max-width: 700px; margin: 0 auto; width: 100%;">
                <h3 style="font-size: 18px; font-weight: 700; margin-bottom: 20px; border-bottom: 1px solid var(--border-glass); padding-bottom: 12px;">
                    Pengaturan Identitas & Target Komunitas
                </h3>
                <form id="formSettings" onsubmit="handleSaveSettings(event)">
                    <div class="form-group">
                        <label class="form-label">Nama Komunitas</label>
                        <input type="text" name="community_name" class="form-control" value="${settings.community_name || 'NEBO SUKABUMI'}" ${!isAdmin ? 'disabled' : ''} required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Slogan / Motto</label>
                        <input type="text" name="community_slogan" class="form-control" value="${settings.community_slogan || ''}" ${!isAdmin ? 'disabled' : ''}>
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px;">
                        <div class="form-group">
                            <label class="form-label">Target Kas (Rp)</label>
                            <input type="number" name="target_kas" class="form-control" value="${settings.target_kas || 0}" ${!isAdmin ? 'disabled' : ''}>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Target Anniversary (Rp)</label>
                            <input type="number" name="target_aniv" class="form-control" value="${settings.target_aniv || 0}" ${!isAdmin ? 'disabled' : ''}>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">No Telepon / Kontak</label>
                        <input type="text" name="community_phone" class="form-control" value="${settings.community_phone || ''}" ${!isAdmin ? 'disabled' : ''}>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Alamat Sekretariat</label>
                        <textarea name="community_address" class="form-control" rows="3" ${!isAdmin ? 'disabled' : ''}>${settings.community_address || ''}</textarea>
                    </div>

                    ${isAdmin ? `
                        <div style="margin-top: 24px; text-align: right;">
                            <button type="submit" class="btn btn-primary"><i class="fa-solid fa-floppy-disk"></i> Simpan Pengaturan</button>
                        </div>
                    ` : `<p style="color: var(--text-muted); font-size: 12px; margin-top: 14px;">* Anda login sebagai Guest/Member (Baca Saja). Pengurus/Admin dapat mengubah pengaturan ini.</p>`}
                </form>
            </div>
        `;
    } catch (err) {
        showErrorView(container, 'Gagal Memuat Pengaturan', err.message, loadPengaturan);
    }
}

// =========================================================================
// ACTION HANDLERS & MODALS
// =========================================================================
async function handleSaveSettings(e) {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    const data = {
        community_name: formData.get('community_name'),
        community_slogan: formData.get('community_slogan'),
        target_kas: Number(formData.get('target_kas') || 0),
        target_aniv: Number(formData.get('target_aniv') || 0),
        community_phone: formData.get('community_phone'),
        community_address: formData.get('community_address')
    };

    try {
        await NeboApi.saveSettings(data);
        showToast('Pengaturan berhasil disimpan!', 'success');
    } catch (err) {
        showToast('Gagal simpan: ' + err.message, 'error');
    }
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('show');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('show');
}

// LOGIN MODAL HANDLER
async function handleLoginSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const username = form.username.value.trim();
    const password = form.password.value;

    const btn = form.querySelector('button[type="submit"]');
    if (btn) btn.disabled = true;

    try {
        const res = await NeboApi.login(username, password);
        showToast('Login berhasil! Selamat datang, ' + (res.data?.nama || res.user?.nama || username), 'success');
        closeModal('modalLogin');
        setupUserInterface();
        navigate(currentRoute);
    } catch (err) {
        showToast('Login gagal: ' + err.message, 'error');
    } finally {
        if (btn) btn.disabled = false;
    }
}

// DELETE HELPERS
async function confirmDeleteAnggota(id, nama) {
    if (confirm(`Hapus anggota "${nama}" dari database?`)) {
        try {
            await NeboApi.deleteAnggota(id);
            showToast('Anggota berhasil dihapus', 'success');
            loadAnggota();
        } catch (e) {
            showToast('Gagal hapus: ' + e.message, 'error');
        }
    }
}

async function confirmDeleteRiwayatKas(id, nama) {
    if (confirm(`Hapus riwayat kas anggota ini?`)) {
        try {
            await NeboApi.deleteRiwayatKas(id);
            showToast('Riwayat kas dihapus', 'success');
            loadKasUtama();
        } catch (e) {
            showToast('Gagal hapus: ' + e.message, 'error');
        }
    }
}

async function confirmDeleteKeliling(id) {
    if (confirm('Hapus transaksi kas keliling ini?')) {
        try {
            await NeboApi.deleteKasKeliling(id);
            showToast('Kas keliling dihapus', 'success');
            loadKasKeliling();
        } catch (e) {
            showToast('Gagal hapus: ' + e.message, 'error');
        }
    }
}

async function confirmDeletePengeluaran(id) {
    if (confirm('Hapus pengeluaran ini?')) {
        try {
            await NeboApi.deletePengeluaran(id);
            showToast('Pengeluaran berhasil dihapus', 'success');
            loadPengeluaran();
        } catch (e) {
            showToast('Gagal hapus: ' + e.message, 'error');
        }
    }
}

async function confirmDeleteAniv(id) {
    if (confirm('Hapus riwayat anniversary ini?')) {
        try {
            await NeboApi.deleteIuranAniv(id);
            showToast('Iuran anniversary dihapus', 'success');
            loadAnniversary();
        } catch (e) {
            showToast('Gagal hapus: ' + e.message, 'error');
        }
    }
}

async function confirmDeleteAbsensi(id) {
    if (confirm('Hapus catatan absensi ini?')) {
        try {
            await NeboApi.deleteAbsensi(id);
            showToast('Absensi dihapus', 'success');
            loadAbsensi();
        } catch (e) {
            showToast('Gagal hapus: ' + e.message, 'error');
        }
    }
}

async function confirmDeleteCatatan(id) {
    if (confirm('Hapus catatan ini?')) {
        try {
            await NeboApi.deleteCatatan(id);
            showToast('Catatan dihapus', 'success');
            loadCatatan();
        } catch (e) {
            showToast('Gagal hapus: ' + e.message, 'error');
        }
    }
}

// GENERIC FORM MODALS
function openModalTambahAnggota() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Tambah Anggota Baru';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormTambahAnggota(event)">
            <div class="form-group">
                <label class="form-label">Nama Lengkap</label>
                <input type="text" name="nama" class="form-control" required placeholder="Contoh: Budi Santoso">
            </div>
            <div class="form-group">
                <label class="form-label">Nomor Registrasi Anggota (NRA)</label>
                <input type="text" name="nra" class="form-control" placeholder="Contoh: NEBO-001">
            </div>
            <div class="form-group">
                <label class="form-label">No WhatsApp</label>
                <input type="text" name="no_wa" class="form-control" placeholder="Contoh: 08123456789">
            </div>
            <div class="form-group">
                <label class="form-label">Alamat</label>
                <input type="text" name="alamat" class="form-control" placeholder="Contoh: Sukabumi">
            </div>
            <div class="form-group">
                <label class="form-label">Role</label>
                <select name="role" class="form-control">
                    <option value="Anggota">Anggota</option>
                    <option value="Bendahara">Bendahara</option>
                    <option value="Admin">Admin</option>
                </select>
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Anggota</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormTambahAnggota(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        nama: fd.get('nama'),
        nra: fd.get('nra') || '',
        no_wa: fd.get('no_wa') || '',
        alamat: fd.get('alamat') || '',
        role: fd.get('role') || 'Anggota',
        tgl_gabung: new Date().toISOString().split('T')[0]
    };

    try {
        await NeboApi.tambahAnggota(data);
        showToast('Anggota berhasil ditambahkan!', 'success');
        closeModal('modalDynamic');
        loadAnggota();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalInputKas() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Input Iuran Kas';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormInputKas(event)">
            <div class="form-group">
                <label class="form-label">Pilih Anggota</label>
                <select name="id_anggota" class="form-control" required>
                    <option value="">-- Pilih Anggota --</option>
                    ${cachedAnggotaList.map(a => `<option value="${a.id}">${a.nama} (${a.nra || '-'})</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Nominal (Rp)</label>
                <input type="number" name="nominal" class="form-control" required placeholder="Contoh: 20000">
            </div>
            <div class="form-group">
                <label class="form-label">Keterangan</label>
                <input type="text" name="keterangan" class="form-control" value="Iuran Kas Pokok">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Kas</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormInputKas(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        id_anggota: Number(fd.get('id_anggota')),
        nominal: Number(fd.get('nominal')),
        keterangan: fd.get('keterangan') || 'Iuran Kas Anggota',
        tanggal: new Date().toISOString().split('T')[0]
    };

    try {
        await NeboApi.inputKas(data);
        showToast('Kas berhasil disimpan!', 'success');
        closeModal('modalDynamic');
        loadKasUtama();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalTambahKeliling() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    const months = ['Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni', 'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'];
    const currentMonth = months[new Date().getMonth()];
    const currentYear = new Date().getFullYear();

    modal.querySelector('.modal-header h3').innerText = 'Tambah Kas Keliling';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormTambahKeliling(event)">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                <div class="form-group">
                    <label class="form-label">Bulan</label>
                    <select name="bulan" class="form-control">
                        ${months.map(m => `<option value="${m}" ${m === currentMonth ? 'selected' : ''}>${m}</option>`).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Tahun</label>
                    <input type="text" name="tahun" class="form-control" value="${currentYear}">
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Jenis Transaksi</label>
                <select name="jenis" class="form-control">
                    <option value="pemasukan">Pemasukan (Uang Masuk)</option>
                    <option value="pengeluaran">Pengeluaran (Uang Keluar)</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Nominal (Rp)</label>
                <input type="number" name="nominal" class="form-control" required placeholder="50000">
            </div>
            <div class="form-group">
                <label class="form-label">Catatan / Keterangan</label>
                <input type="text" name="catatan" class="form-control" placeholder="Contoh: Operasional Kopdar">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Transaksi</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormTambahKeliling(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        bulan: fd.get('bulan'),
        tahun: fd.get('tahun'),
        jenis: fd.get('jenis'),
        nominal: Number(fd.get('nominal')),
        catatan: fd.get('catatan') || '',
        tanggal: new Date().toISOString().split('T')[0]
    };

    try {
        await NeboApi.tambahKasKeliling(data);
        showToast('Kas keliling berhasil dicatat!', 'success');
        closeModal('modalDynamic');
        loadKasKeliling();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalTambahPengeluaran() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Catat Pengeluaran';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormTambahPengeluaran(event)">
            <div class="form-group">
                <label class="form-label">Sumber Kas</label>
                <select name="jenis_kas" class="form-control">
                    <option value="Kas Utama">Kas Utama</option>
                    <option value="Kas Keliling">Kas Keliling</option>
                    <option value="Kas Anniversary">Kas Anniversary</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Keterangan Pengeluaran</label>
                <input type="text" name="keterangan" class="form-control" required placeholder="Contoh: Beli Banner & Konsumsi">
            </div>
            <div class="form-group">
                <label class="form-label">Nominal (Rp)</label>
                <input type="number" name="nominal" class="form-control" required placeholder="100000">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Pengeluaran</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormTambahPengeluaran(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        jenis_kas: fd.get('jenis_kas'),
        keterangan: fd.get('keterangan'),
        nominal: Number(fd.get('nominal')),
        tanggal: new Date().toISOString().split('T')[0],
        created_by: NeboApi.getUser()?.nama || 'Admin Web'
    };

    try {
        await NeboApi.addPengeluaran(data);
        showToast('Pengeluaran berhasil dicatat!', 'success');
        closeModal('modalDynamic');
        loadPengeluaran();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalTambahCatatan() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Buat Catatan Baru';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormTambahCatatan(event)">
            <div class="form-group">
                <label class="form-label">Judul Agenda / Catatan</label>
                <input type="text" name="judul" class="form-control" required placeholder="Contoh: Jadwal Kopdar Gabungan">
            </div>
            <div class="form-group">
                <label class="form-label">Isi Catatan</label>
                <textarea name="isi" class="form-control" rows="4" placeholder="Tuliskan catatan atau agenda di sini..."></textarea>
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Catatan</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormTambahCatatan(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        judul: fd.get('judul'),
        isi: fd.get('isi') || '',
        tanggal: new Date().toISOString().split('T')[0]
    };

    try {
        await NeboApi.addCatatan(data);
        showToast('Catatan berhasil ditambahkan!', 'success');
        closeModal('modalDynamic');
        loadCatatan();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalTambahAbsensi() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Catat Absensi Anggota';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormTambahAbsensi(event)">
            <div class="form-group">
                <label class="form-label">Pilih Anggota</label>
                <select name="anggota_id" class="form-control" required>
                    ${cachedAnggotaList.map(a => `<option value="${a.id}">${a.nama} (${a.nra || '-'})</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Status</label>
                <select name="status" class="form-control">
                    <option value="Hadir">Hadir</option>
                    <option value="Izin">Izin</option>
                    <option value="Sakit">Sakit</option>
                    <option value="Alpha">Alpha</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Keterangan</label>
                <input type="text" name="keterangan" class="form-control" placeholder="Contoh: Rapat Bulanan">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Absensi</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormTambahAbsensi(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        anggota_id: Number(fd.get('anggota_id')),
        status: fd.get('status'),
        keterangan: fd.get('keterangan') || '',
        tanggal: new Date().toISOString().split('T')[0]
    };

    try {
        await NeboApi.addAbsensi(data);
        showToast('Absensi berhasil dicatat!', 'success');
        closeModal('modalDynamic');
        loadAbsensi();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalBayarCicilan() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Input Pembayaran Cicilan';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormBayarCicilan(event)">
            <div class="form-group">
                <label class="form-label">Pilih Anggota</label>
                <select name="anggota_id" class="form-control" required>
                    ${cachedAnggotaList.map(a => `<option value="${a.id}">${a.nama} (${a.nra || '-'}) - Sisa: ${formatRupiah(a.sisa_cicilan || 0)}</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Nominal Pembayaran (Rp)</label>
                <input type="number" name="nominal" class="form-control" required placeholder="Contoh: 100000">
            </div>
            <div class="form-group">
                <label class="form-label">Keterangan</label>
                <input type="text" name="keterangan" class="form-control" value="Pembayaran Cicilan">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Pembayaran</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormBayarCicilan(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        anggota_id: Number(fd.get('anggota_id')),
        nominal: Number(fd.get('nominal')),
        keterangan: fd.get('keterangan') || 'Pembayaran Cicilan',
        tanggal: new Date().toISOString().split('T')[0]
    };

    try {
        await NeboApi.addCicilan(data);
        showToast('Pembayaran cicilan berhasil!', 'success');
        closeModal('modalDynamic');
        loadCicilan();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalTambahAniv() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Input Iuran Anniversary';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormTambahAniv(event)">
            <div class="form-group">
                <label class="form-label">Pilih Anggota</label>
                <select name="anggota_id" class="form-control" required>
                    ${cachedAnggotaList.map(a => `<option value="${a.id}">${a.nama} (${a.nra || '-'})</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Nominal (Rp)</label>
                <input type="number" name="nominal" class="form-control" required placeholder="50000">
            </div>
            <div class="form-group">
                <label class="form-label">Keterangan</label>
                <input type="text" name="keterangan" class="form-control" value="Iuran Acara Anniversary">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Iuran</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

async function handleFormTambahAniv(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        anggota_id: Number(fd.get('anggota_id')),
        nominal: Number(fd.get('nominal')),
        keterangan: fd.get('keterangan') || 'Iuran Anniversary',
        tanggal: new Date().toISOString().split('T')[0]
    };

    try {
        await NeboApi.addIuranAniv(data);
        showToast('Iuran anniversary disimpan!', 'success');
        closeModal('modalDynamic');
        loadAnniversary();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

function openModalTambahPembayaran() {
    const modal = document.getElementById('modalDynamic');
    if (!modal) return;

    modal.querySelector('.modal-header h3').innerText = 'Catat Pembayaran Masuk';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="formDynamic" onsubmit="handleFormTambahPembayaran(event)">
            <div class="form-group">
                <label class="form-label">Pilih Anggota</label>
                <select name="anggotaId" id="pembayaranAnggotaId" class="form-control" required onchange="updateAnggotaNamaField(this)">
                    <option value="">-- Pilih Anggota --</option>
                    ${cachedAnggotaList.map(a => `<option value="${a.id}" data-nama="${a.nama}">${a.nama} (${a.nra || '-'})</option>`).join('')}
                </select>
                <input type="hidden" name="anggotaNama" id="pembayaranAnggotaNama" value="">
            </div>
            <div class="form-group">
                <label class="form-label">Jenis Pembayaran</label>
                <select name="jenisPembayaran" class="form-control">
                    <option value="KAS">Iuran Kas Pokok</option>
                    <option value="ANNIVERSARY">Iuran Anniversary</option>
                    <option value="CICILAN">Pembayaran Cicilan</option>
                    <option value="LAINNYA">Lain-lain</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Nominal (Rp)</label>
                <input type="number" name="nominal" class="form-control" required placeholder="50000">
            </div>
            <div class="form-group">
                <label class="form-label">Keterangan</label>
                <input type="text" name="keterangan" class="form-control" placeholder="Contoh: Transfer BCA">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('modalDynamic')">Batal</button>
                <button type="submit" class="btn btn-primary">Simpan Pembayaran</button>
            </div>
        </form>
    `;
    openModal('modalDynamic');
}

function updateAnggotaNamaField(select) {
    const opt = select.options[select.selectedIndex];
    const nama = opt.getAttribute('data-nama') || '';
    const hidden = document.getElementById('pembayaranAnggotaNama');
    if (hidden) hidden.value = nama;
}

async function handleFormTambahPembayaran(e) {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        anggotaId: Number(fd.get('anggotaId')),
        anggotaNama: fd.get('anggotaNama') || 'Anggota',
        jenisPembayaran: fd.get('jenisPembayaran'),
        nominal: Number(fd.get('nominal')),
        keterangan: fd.get('keterangan') || '',
        tanggal: Date.now()
    };

    try {
        await NeboApi.addPembayaran(data);
        showToast('Pembayaran berhasil dicatat!', 'success');
        closeModal('modalDynamic');
        loadPembayaran();
    } catch (err) {
        showToast('Gagal: ' + err.message, 'error');
    }
}

// ERROR VIEW HELPER
function showErrorView(container, title, message, retryFn) {
    container.innerHTML = `
        <div class="state-box">
            <i class="fa-solid fa-triangle-exclamation state-icon" style="color: #fb7185;"></i>
            <div class="state-title" style="color: #fb7185;">${title}</div>
            <div class="state-desc">${message}</div>
            <button class="btn btn-primary" id="btnRetry"><i class="fa-solid fa-rotate-right"></i> Coba Lagi</button>
        </div>
    `;
    const btn = document.getElementById('btnRetry');
    if (btn && retryFn) {
        btn.addEventListener('click', retryFn);
    }
}
