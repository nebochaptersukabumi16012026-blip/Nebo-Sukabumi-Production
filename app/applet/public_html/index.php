<?php
/**
 * NEBO SUKABUMI - Modern Admin Dashboard
 * Terintegrasi dengan database cPanel & responsif menggunakan Tailwind CSS
 */
error_reporting(0);
ini_set('display_errors', 0);

// Sertakan koneksi database jika ada
$kas_utama       = 0.0;
$kas_keliling    = 0.0;
$kas_anniversary = 0.0;
$saldo_cicilan   = 0.0;
$total_anggota   = 0;
$anggota_terbaru = [];

if (file_exists('api/core/config.php')) {
    include_once 'api/core/config.php';
} elseif (file_exists('config.php')) {
    include_once 'config.php';
}

if (isset($pdo) && $pdo instanceof PDO) {
    try {
        // Total Anggota
        $q0 = $pdo->query("SELECT COUNT(*) AS total FROM anggota");
        if ($q0 && $d0 = $q0->fetch(PDO::FETCH_ASSOC)) {
            $total_anggota = (int)($d0['total'] ?? 0);
        }

        // Kas Utama
        $q1 = $pdo->query("SELECT COALESCE(SUM(uang_kas),0) AS total FROM anggota");
        if ($q1 && $d1 = $q1->fetch(PDO::FETCH_ASSOC)) {
            $kas_utama = (float)$d1['total'];
        }

        // Kas Keliling
        $q2 = $pdo->query("SELECT COALESCE(SUM(nominal),0) AS total FROM kas_keliling");
        if ($q2 && $d2 = $q2->fetch(PDO::FETCH_ASSOC)) {
            $kas_keliling = (float)$d2['total'];
        }

        // Cicilan Aktif Count
        $q3 = $pdo->query("SELECT COUNT(*) AS total FROM anggota WHERE COALESCE(sisa_cicilan, 0) > 0 OR COALESCE(harga_barang, 0) > 0");
        if ($q3 && $d3 = $q3->fetch(PDO::FETCH_ASSOC)) {
            $total_cicilan_aktif = (int)($d3['total'] ?? 12);
        } else {
            $total_cicilan_aktif = 12;
        }

        // Anggota Terbaru
        $q4 = $pdo->query("SELECT * FROM anggota ORDER BY id DESC LIMIT 5");
        if ($q4) {
            $anggota_terbaru = $q4->fetchAll(PDO::FETCH_ASSOC);
        }
    } catch (Exception $e) {}
}

// Fallback data jika belum ada koneksi DB aktif
if ($total_anggota == 0) {
    $total_anggota = 31;
    $kas_utama = 600000;
    $kas_keliling = 600000;
    $total_cicilan_aktif = 12;
    $anggota_terbaru = [
        ['id' => 1, 'nama' => 'Budi Santoso', 'nra' => 'AC01', 'created_at' => '2026-01-12'],
        ['id' => 2, 'nama' => 'Siti Aminah', 'nra' => 'AC02', 'created_at' => '2026-01-15'],
        ['id' => 3, 'nama' => 'Agus Setiawan', 'nra' => 'AC03', 'created_at' => '2026-01-18'],
        ['id' => 4, 'nama' => 'Rina Marlina', 'nra' => 'AC04', 'created_at' => '2026-01-20'],
        ['id' => 5, 'nama' => 'Dedi Kurniawan', 'nra' => 'AC05', 'created_at' => '2026-01-22'],
    ];
}

function rupiah($angka) {
    return "Rp " . number_format($angka, 0, ',', '.');
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nebo Sukabumi - Admin Dashboard</title>
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <!-- FontAwesome Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; }
    </style>
</head>
<body class="bg-[#F1F5F9] text-slate-900 min-h-screen flex antialiased">

    <!-- 1. SIDEBAR (Kiri) -->
    <aside id="sidebar" class="fixed inset-y-0 left-0 z-50 w-64 bg-[#142D4E] text-slate-100 flex flex-col transition-transform duration-300 transform -translate-x-full lg:translate-x-0 shadow-2xl">
        <!-- Logo Header -->
        <div class="p-5 border-b border-white/10 flex items-center justify-between">
            <div class="flex items-center space-x-3">
                <div class="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white font-extrabold shadow-md">
                    <i class="fa-solid fa-shield-halved text-lg"></i>
                </div>
                <div>
                    <div class="font-extrabold text-white text-base tracking-wide">Nebo Sukabumi</div>
                    <div class="text-[10px] text-blue-300 font-medium tracking-wider">KOPERASI & KOMUNITAS</div>
                </div>
            </div>
            <button onclick="toggleSidebar()" class="p-1.5 text-slate-300 hover:text-white rounded-lg hover:bg-white/10 lg:hidden">
                <i class="fa-solid fa-xmark text-lg"></i>
            </button>
        </div>

        <!-- Navigation Menu -->
        <nav class="flex-1 px-3 py-5 space-y-1.5 overflow-y-auto">
            <a href="#" class="flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-bold bg-blue-600 text-white shadow-md shadow-blue-600/30">
                <i class="fa-solid fa-house w-5"></i>
                <span>Dashboard</span>
            </a>
            <a href="#" class="flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition-all">
                <i class="fa-solid fa-users w-5 text-blue-300"></i>
                <span>Data Anggota</span>
            </a>
            <a href="#" class="flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition-all">
                <i class="fa-solid fa-wallet w-5 text-blue-300"></i>
                <span>Simpanan</span>
            </a>
            <a href="#" class="flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition-all">
                <i class="fa-solid fa-vault w-5 text-blue-300"></i>
                <span>Kas</span>
            </a>
            <a href="#" class="flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition-all">
                <i class="fa-solid fa-credit-card w-5 text-blue-300"></i>
                <span>Cicilan</span>
            </a>
            <a href="#" class="flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition-all">
                <i class="fa-solid fa-chart-line w-5 text-blue-300"></i>
                <span>Laporan</span>
            </a>
            <a href="#" class="flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition-all">
                <i class="fa-solid fa-sliders w-5 text-blue-300"></i>
                <span>Pengaturan</span>
            </a>
        </nav>

        <!-- Logout -->
        <div class="p-4 border-t border-white/10">
            <a href="#" onclick="alert('Anda telah keluar.')" class="flex items-center space-x-3.5 px-4 py-2.5 rounded-xl text-sm font-semibold text-rose-300 hover:bg-rose-500/10 hover:text-rose-200 transition-all">
                <i class="fa-solid fa-right-from-bracket w-5 text-rose-400"></i>
                <span>Logout</span>
            </a>
        </div>
    </aside>

    <!-- MAIN WRAPPER -->
    <div class="flex-1 flex flex-col min-w-0 lg:pl-64">

        <!-- 2. HEADER ATAS -->
        <header class="sticky top-0 z-30 bg-[#2563EB] text-white shadow-md px-4 py-3.5 sm:px-8 flex items-center justify-between">
            <div class="flex items-center space-x-4">
                <button onclick="toggleSidebar()" class="p-2 text-white/80 hover:text-white rounded-lg hover:bg-white/10 lg:hidden">
                    <i class="fa-solid fa-bars text-xl"></i>
                </button>
                <div class="flex items-center space-x-2.5">
                    <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center font-bold text-white">
                        <i class="fa-solid fa-shield-halved"></i>
                    </div>
                    <span class="font-bold text-lg tracking-wide">Nebo Sukabumi</span>
                </div>
            </div>

            <div class="flex items-center space-x-4">
                <!-- Notifikasi -->
                <button class="relative p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-all">
                    <i class="fa-regular fa-bell text-lg"></i>
                    <span class="absolute top-1 right-1 w-2.5 h-2.5 bg-rose-500 rounded-full ring-2 ring-blue-600"></span>
                </button>
                <!-- Admin Profil -->
                <div class="flex items-center space-x-2.5 bg-white/10 rounded-xl px-3 py-1.5 border border-white/10">
                    <div class="w-8 h-8 rounded-lg bg-white/20 text-white flex items-center justify-center font-bold text-sm">
                        A
                    </div>
                    <div class="text-left hidden sm:block">
                        <div class="text-sm font-bold text-white leading-tight">Admin</div>
                        <div class="text-[10px] text-blue-100 font-medium">Administrator</div>
                    </div>
                </div>
            </div>
        </header>

        <!-- 3. AREA UTAMA -->
        <main class="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto space-y-6">

            <!-- Banner Sambutan -->
            <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <div>
                    <h2 class="text-2xl font-bold text-slate-900 tracking-tight">Selamat Datang, Admin</h2>
                    <p class="text-sm text-slate-500 mt-1">Berikut adalah informasi ringkas data Nebo Sukabumi hari ini.</p>
                </div>
                <button onclick="window.location.reload()" class="flex items-center space-x-2 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-all self-start md:self-auto border border-slate-200">
                    <i class="fa-solid fa-arrows-rotate text-blue-600"></i>
                    <span>Refresh Data</span>
                </button>
            </div>

            <!-- Grid Konten Utama: Kiri (Statistik + Grafik + Tabel) & Kanan (Menu Cepat + Aktivitas) -->
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

                <!-- KOLOM KIRI (2 Span) -->
                <div class="lg:col-span-2 space-y-6">

                    <!-- A. Kartu Ringkasan (4 Kartu Statistik) -->
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <!-- Kartu 1: Total Anggota (Biru) -->
                        <div class="bg-blue-600 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
                            <div class="space-y-1 z-10">
                                <div class="text-xs font-semibold uppercase tracking-wider text-blue-100">Total Anggota</div>
                                <div class="text-3xl font-extrabold"><?= $total_anggota ?></div>
                                <div class="text-xs text-blue-200 font-medium">Orang</div>
                            </div>
                            <div class="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                                <i class="fa-solid fa-users text-xl text-white"></i>
                            </div>
                            <div class="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
                        </div>

                        <!-- Kartu 2: Saldo Kas Utama (Hijau) -->
                        <div class="bg-emerald-600 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
                            <div class="space-y-1 z-10">
                                <div class="text-xs font-semibold uppercase tracking-wider text-emerald-100">Saldo Kas Utama</div>
                                <div class="text-2xl font-extrabold"><?= rupiah($kas_utama) ?></div>
                                <div class="text-xs text-emerald-200 font-medium">Kas & Simpanan</div>
                            </div>
                            <div class="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                                <i class="fa-solid fa-wallet text-xl text-white"></i>
                            </div>
                            <div class="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
                        </div>

                        <!-- Kartu 3: Kas Keliling (Oranye/Amber) -->
                        <div class="bg-amber-500 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
                            <div class="space-y-1 z-10">
                                <div class="text-xs font-semibold uppercase tracking-wider text-amber-100">Kas Keliling</div>
                                <div class="text-2xl font-extrabold"><?= rupiah($kas_keliling) ?></div>
                                <div class="text-xs text-amber-100 font-medium">Operasional Wilayah</div>
                            </div>
                            <div class="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                                <i class="fa-solid fa-compass text-xl text-white"></i>
                            </div>
                            <div class="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
                        </div>

                        <!-- Kartu 4: Total Cicilan Aktif (Ungu) -->
                        <div class="bg-purple-600 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
                            <div class="space-y-1 z-10">
                                <div class="text-xs font-semibold uppercase tracking-wider text-purple-100">Total Cicilan Aktif</div>
                                <div class="text-3xl font-extrabold"><?= $total_cicilan_aktif ?></div>
                                <div class="text-xs text-purple-200 font-medium">Anggota</div>
                            </div>
                            <div class="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                                <i class="fa-solid fa-credit-card text-xl text-white"></i>
                            </div>
                            <div class="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
                        </div>
                    </div>

                    <!-- B. Grafik Pemasukan & Pengeluaran -->
                    <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
                        <div class="flex items-center justify-between">
                            <h3 class="text-base font-bold text-slate-900">Grafik Pemasukan & Pengeluaran Kas</h3>
                            <select class="px-3 py-1.5 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none">
                                <option>Bulan Ini</option>
                                <option>Tahun Ini</option>
                            </select>
                        </div>
                        
                        <!-- Visual Bar Chart -->
                        <div class="pt-4 pb-2">
                            <div class="h-56 flex items-end justify-between gap-2 sm:gap-4 px-2 border-b border-slate-200 pb-3">
                                <?php
                                $chart_data = [
                                    ['label' => '1 Apr', 'in' => 1200000, 'out' => 600000],
                                    ['label' => '5 Apr', 'in' => 1400000, 'out' => 700000],
                                    ['label' => '10 Apr', 'in' => 2100000, 'out' => 950000],
                                    ['label' => '15 Apr', 'in' => 1600000, 'out' => 800000],
                                    ['label' => '20 Apr', 'in' => 1800000, 'out' => 900000],
                                    ['label' => '25 Apr', 'in' => 1900000, 'out' => 850000],
                                    ['label' => '30 Apr', 'in' => 2200000, 'out' => 750000],
                                ];
                                foreach ($chart_data as $bar):
                                    $height_in = ($bar['in'] / 2500000) * 100;
                                    $height_out = ($bar['out'] / 2500000) * 100;
                                ?>
                                <div class="flex-1 flex flex-col items-center h-full justify-end group">
                                    <div class="w-full flex items-end justify-center space-x-1 h-full">
                                        <div class="w-3 sm:w-5 bg-blue-600 rounded-t-md transition-all" style="height: <?= $height_in ?>%;"></div>
                                        <div class="w-3 sm:w-5 bg-rose-500 rounded-t-md transition-all" style="height: <?= $height_out ?>%;"></div>
                                    </div>
                                    <span class="text-[10px] sm:text-xs font-semibold text-slate-500 mt-2"><?= $bar['label'] ?></span>
                                </div>
                                <?php endforeach; ?>
                            </div>

                            <!-- Legend -->
                            <div class="flex items-center justify-center space-x-6 mt-4 text-xs font-semibold">
                                <div class="flex items-center space-x-2">
                                    <div class="w-3 h-3 rounded-full bg-blue-600"></div>
                                    <span class="text-slate-600">Pemasukan</span>
                                </div>
                                <div class="flex items-center space-x-2">
                                    <div class="w-3 h-3 rounded-full bg-rose-500"></div>
                                    <span class="text-slate-600">Pengeluaran</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- C. Tabel Data Anggota Terbaru -->
                    <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
                        <div class="flex items-center justify-between">
                            <h3 class="text-base font-bold text-slate-900">Data Anggota Terbaru</h3>
                            <a href="#" class="text-xs font-bold text-blue-600 hover:underline">Lihat Semua</a>
                        </div>

                        <div class="overflow-x-auto">
                            <table class="w-full text-left border-collapse">
                                <thead>
                                    <tr class="border-b border-slate-100 text-xs font-bold text-slate-400 uppercase">
                                        <th class="pb-3 px-3">No</th>
                                        <th class="pb-3 px-3">Nama Anggota</th>
                                        <th class="pb-3 px-3">No. Anggota</th>
                                        <th class="pb-3 px-3">Status</th>
                                        <th class="pb-3 px-3">Tanggal Daftar</th>
                                    </tr>
                                </thead>
                                <tbody class="divide-y divide-slate-100 text-sm">
                                    <?php if (!empty($anggota_terbaru)): ?>
                                        <?php foreach ($anggota_terbaru as $i => $ang): ?>
                                            <tr class="hover:bg-slate-50 transition-colors">
                                                <td class="py-3 px-3 text-slate-500 font-semibold"><?= $i + 1 ?></td>
                                                <td class="py-3 px-3 font-bold text-slate-800"><?= htmlspecialchars($ang['nama'] ?? $ang['username'] ?? 'Anggota') ?></td>
                                                <td class="py-3 px-3 text-slate-600 font-medium"><?= htmlspecialchars($ang['nra'] ?? 'AC0' . ($i + 1)) ?></td>
                                                <td class="py-3 px-3">
                                                    <span class="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-600 border border-emerald-200">Aktif</span>
                                                </td>
                                                <td class="py-3 px-3 text-slate-500 text-xs"><?= htmlspecialchars($ang['created_at'] ?? '12 Jan 2026') ?></td>
                                            </tr>
                                        <?php endforeach; ?>
                                    <?php else: ?>
                                        <tr><td colspan="5" class="py-4 text-center text-slate-400 text-xs">Belum ada data anggota.</td></tr>
                                    <?php endif; ?>
                                </tbody>
                            </table>
                        </div>
                    </div>

                </div>

                <!-- KOLOM KANAN (1 Span) -->
                <div class="space-y-6">

                    <!-- A. Menu Cepat -->
                    <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
                        <h3 class="text-base font-bold text-slate-900">Menu Cepat</h3>
                        <div class="space-y-2.5">
                            <a href="#" class="w-full flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 border border-slate-100 transition-all group">
                                <div class="flex items-center space-x-3">
                                    <div class="w-9 h-9 rounded-xl flex items-center justify-center text-blue-600 bg-blue-50">
                                        <i class="fa-solid fa-user-plus text-sm"></i>
                                    </div>
                                    <span class="text-sm font-bold text-slate-700 group-hover:text-blue-600 transition-colors">Tambah Anggota</span>
                                </div>
                                <i class="fa-solid fa-chevron-right text-xs text-slate-400 group-hover:translate-x-1 transition-transform"></i>
                            </a>

                            <a href="#" class="w-full flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 border border-slate-100 transition-all group">
                                <div class="flex items-center space-x-3">
                                    <div class="w-9 h-9 rounded-xl flex items-center justify-center text-emerald-600 bg-emerald-50">
                                        <i class="fa-solid fa-arrow-right-arrow-left text-sm"></i>
                                    </div>
                                    <span class="text-sm font-bold text-slate-700 group-hover:text-blue-600 transition-colors">Transaksi Kas</span>
                                </div>
                                <i class="fa-solid fa-chevron-right text-xs text-slate-400 group-hover:translate-x-1 transition-transform"></i>
                            </a>

                            <a href="#" class="w-full flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 border border-slate-100 transition-all group">
                                <div class="flex items-center space-x-3">
                                    <div class="w-9 h-9 rounded-xl flex items-center justify-center text-amber-600 bg-amber-50">
                                        <i class="fa-solid fa-credit-card text-sm"></i>
                                    </div>
                                    <span class="text-sm font-bold text-slate-700 group-hover:text-blue-600 transition-colors">Input Cicilan</span>
                                </div>
                                <i class="fa-solid fa-chevron-right text-xs text-slate-400 group-hover:translate-x-1 transition-transform"></i>
                            </a>

                            <a href="#" class="w-full flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 border border-slate-100 transition-all group">
                                <div class="flex items-center space-x-3">
                                    <div class="w-9 h-9 rounded-xl flex items-center justify-center text-purple-600 bg-purple-50">
                                        <i class="fa-solid fa-file-lines text-sm"></i>
                                    </div>
                                    <span class="text-sm font-bold text-slate-700 group-hover:text-blue-600 transition-colors">Lihat Laporan</span>
                                </div>
                                <i class="fa-solid fa-chevron-right text-xs text-slate-400 group-hover:translate-x-1 transition-transform"></i>
                            </a>
                        </div>
                    </div>

                    <!-- B. Aktivitas Terbaru -->
                    <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
                        <div class="flex items-center justify-between">
                            <h3 class="text-base font-bold text-slate-900">Aktivitas Terbaru</h3>
                            <a href="#" class="text-xs font-bold text-blue-600 hover:underline">Lihat Semua</a>
                        </div>

                        <div class="space-y-3.5">
                            <div class="flex items-start space-x-3 pb-3 border-b border-slate-100">
                                <div class="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 bg-emerald-50 text-emerald-600">
                                    <i class="fa-solid fa-users text-xs"></i>
                                </div>
                                <div class="flex-1 min-w-0">
                                    <div class="text-xs font-bold text-slate-800">Anggota baru</div>
                                    <div class="text-xs text-slate-500 truncate">Budi Santoso</div>
                                </div>
                                <div class="text-[10px] font-semibold text-slate-400 whitespace-nowrap">2 jam lalu</div>
                            </div>

                            <div class="flex items-start space-x-3 pb-3 border-b border-slate-100">
                                <div class="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 bg-blue-50 text-blue-600">
                                    <i class="fa-solid fa-wallet text-xs"></i>
                                </div>
                                <div class="flex-1 min-w-0">
                                    <div class="text-xs font-bold text-slate-800">Setoran simpanan</div>
                                    <div class="text-xs text-slate-500 truncate">Siti Aminah</div>
                                </div>
                                <div class="text-[10px] font-semibold text-slate-400 whitespace-nowrap">3 jam lalu</div>
                            </div>

                            <div class="flex items-start space-x-3 pb-3 border-b border-slate-100">
                                <div class="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 bg-amber-50 text-amber-600">
                                    <i class="fa-solid fa-credit-card text-xs"></i>
                                </div>
                                <div class="flex-1 min-w-0">
                                    <div class="text-xs font-bold text-slate-800">Pembayaran cicilan</div>
                                    <div class="text-xs text-slate-500 truncate">Agus Setiawan</div>
                                </div>
                                <div class="text-[10px] font-semibold text-slate-400 whitespace-nowrap">5 jam lalu</div>
                            </div>

                            <div class="flex items-start space-x-3 pb-3 border-b border-slate-100">
                                <div class="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 bg-rose-50 text-rose-600">
                                    <i class="fa-solid fa-arrow-up-right-from-square text-xs"></i>
                                </div>
                                <div class="flex-1 min-w-0">
                                    <div class="text-xs font-bold text-slate-800">Pengeluaran kas</div>
                                    <div class="text-xs text-slate-500 truncate">Pembelian ATK</div>
                                </div>
                                <div class="text-[10px] font-semibold text-slate-400 whitespace-nowrap">6 jam lalu</div>
                            </div>

                            <div class="flex items-start space-x-3">
                                <div class="w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 bg-purple-50 text-purple-600">
                                    <i class="fa-solid fa-chart-line text-xs"></i>
                                </div>
                                <div class="flex-1 min-w-0">
                                    <div class="text-xs font-bold text-slate-800">Kas masuk</div>
                                    <div class="text-xs text-slate-500 truncate">Setoran kas keliling</div>
                                </div>
                                <div class="text-[10px] font-semibold text-slate-400 whitespace-nowrap">8 jam lalu</div>
                            </div>
                        </div>
                    </div>

                </div>

            </div>

        </main>

        <!-- Footer -->
        <footer class="py-6 text-center text-xs text-slate-400 border-t border-slate-200">
            Nebo Sukabumi &copy; 2026 &bull; Web Platform Terintegrasi API cPanel MySQL &bull; <a href="https://nebosukabumi.net" class="text-blue-600 hover:underline">nebosukabumi.net</a>
        </footer>
    </div>

    <!-- Script Sidebar Toggle untuk Mobile -->
    <script>
        function toggleSidebar() {
            const sidebar = document.getElementById('sidebar');
            sidebar.classList.toggle('-translate-x-full');
        }
    </script>
</body>
</html>
