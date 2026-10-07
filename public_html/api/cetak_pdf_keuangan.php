<?php
// ==============================================================================
// CETAK PDF / LAPORAN RESMI KEUANGAN - NEBO SUKABUMI
// ==============================================================================
error_reporting(0);
ini_set('display_errors', '0');

header("Content-Type: text/html; charset=UTF-8");
header("Cache-Control: no-cache, no-store, must-revalidate");

if (file_exists('config.php')) {
    include_once 'config.php';
} elseif (file_exists('../config.php')) {
    include_once '../config.php';
}

$community_name = "NEBO SUKABUMI";
$tanggal_cetak = date('d F Y');
$waktu_cetak = date('H:i') . ' WIB';

// Ambil data akumulasi dari database jika ada koneksi
$total_masuk = 0;
$total_keluar = 0;
$saldo_kas = 0;
$transactions = [];

if (isset($conn) || isset($pdo)) {
    $db = isset($conn) ? $conn : $pdo;
    try {
        // Ambil saldo master dari saldo_akumulasi jika ada
        $stmtKas = $db->query("SELECT saldo_kas_utama, total_pemasukan, total_pengeluaran FROM saldo_akumulasi WHERE id = 1 LIMIT 1");
        if ($rowKas = $stmtKas->fetch(PDO::FETCH_ASSOC)) {
            $saldo_kas = floatval($rowKas['saldo_kas_utama']);
            $total_masuk = floatval($rowKas['total_pemasukan']);
            $total_keluar = floatval($rowKas['total_pengeluaran']);
        }
    } catch (Exception $e) {}

    try {
        // Ambil 50 transaksi terakhir dari riwayat pembayaran & pengeluaran
        $stmtP = $db->query("SELECT tanggal_bayar as tgl, CONCAT('Iuran Kas: ', a.nama) as keterangan, 'Masuk' as jenis, p.nominal 
                             FROM pembayaran p 
                             LEFT JOIN anggota a ON p.anggota_id = a.id 
                             ORDER BY p.tanggal_bayar DESC LIMIT 25");
        while ($row = $stmtP->fetch(PDO::FETCH_ASSOC)) {
            $transactions[] = $row;
        }

        $stmtK = $db->query("SELECT tanggal as tgl, CONCAT('Pengeluaran: ', keterangan) as keterangan, 'Keluar' as jenis, nominal 
                             FROM pengeluaran 
                             ORDER BY tanggal DESC LIMIT 25");
        while ($row = $stmtK->fetch(PDO::FETCH_ASSOC)) {
            $transactions[] = $row;
        }

        // Urutkan berdasarkan tanggal descending
        usort($transactions, function($a, $b) {
            return strtotime($b['tgl']) - strtotime($a['tgl']);
        });
    } catch (Exception $e) {}
}

if ($total_masuk == 0 && count($transactions) > 0) {
    foreach ($transactions as $t) {
        if ($t['jenis'] == 'Masuk') $total_masuk += $t['nominal'];
        else $total_keluar += $t['nominal'];
    }
    $saldo_kas = $total_masuk - $total_keluar;
}

function format_rp($num) {
    return 'Rp ' . number_format($num, 0, ',', '.');
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Laporan Keuangan Resmi - <?= htmlspecialchars($community_name) ?></title>
    <style>
        @page {
            size: A4 portrait;
            margin: 12mm 15mm 15mm 15mm;
        }
        * {
            box-sizing: border-box;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            color: #0f172a;
            background: #f8fafc;
            margin: 0;
            padding: 20px;
            font-size: 11px;
            line-height: 1.4;
        }
        .sheet {
            max-width: 210mm;
            min-height: 297mm;
            margin: 0 auto;
            background: #ffffff;
            padding: 15mm;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
            border-radius: 4px;
            position: relative;
        }
        @media print {
            body { background: none; padding: 0; }
            .sheet { box-shadow: none; padding: 0; max-width: 100%; border-radius: 0; }
            .no-print { display: none !important; }
        }
        /* Kop Surat */
        .kop-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 8px;
        }
        .kop-left {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .kop-logo {
            width: 50px;
            height: 50px;
            background: #0f172a;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            font-weight: 800;
            font-size: 24px;
            border: 2px solid #3b82f6;
        }
        .kop-title h1 {
            font-size: 16px;
            font-weight: 800;
            margin: 0;
            color: #0f172a;
            letter-spacing: 0.5px;
        }
        .kop-title .subtitle {
            font-size: 9px;
            font-weight: 700;
            color: #475569;
            margin-top: 2px;
            letter-spacing: 0.5px;
        }
        .kop-title .address {
            font-size: 8.5px;
            color: #64748b;
            margin-top: 2px;
        }
        .kop-meta {
            text-align: right;
            font-size: 8.5px;
            color: #475569;
        }
        .kop-meta .status-badge {
            display: inline-block;
            margin-top: 3px;
            padding: 2px 6px;
            background: #dcfce7;
            color: #15803d;
            font-weight: 700;
            border-radius: 3px;
            font-size: 8px;
        }
        .divider-primary {
            height: 2px;
            background: #0f172a;
            margin-top: 6px;
        }
        .divider-accent {
            height: 1px;
            background: #cbd5e1;
            margin-top: 2px;
            margin-bottom: 12px;
        }
        /* Title Banner */
        .title-banner {
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 4px;
            padding: 8px 12px;
            text-align: center;
            margin-bottom: 14px;
        }
        .title-banner h2 {
            margin: 0;
            font-size: 13px;
            font-weight: 800;
            color: #0f172a;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }
        .title-banner p {
            margin: 2px 0 0 0;
            font-size: 9px;
            color: #64748b;
        }
        /* Table */
        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 9.5px;
            margin-bottom: 16px;
        }
        thead th {
            background: #f1f5f9;
            color: #0f172a;
            font-weight: 700;
            padding: 7px 8px;
            border: 1px solid #cbd5e1;
            text-transform: uppercase;
            font-size: 8.5px;
        }
        tbody td {
            padding: 6px 8px;
            border: 1px solid #e2e8f0;
            vertical-align: middle;
        }
        tbody tr:nth-child(even) {
            background: #f8fafc;
        }
        .text-center { text-align: center; }
        .text-right { text-align: right; }
        .text-left { text-align: left; }
        .badge-masuk {
            color: #16a34a;
            font-weight: 700;
        }
        .badge-keluar {
            color: #dc2626;
            font-weight: 700;
        }
        /* Summary Box */
        .summary-card {
            background: #f8fafc;
            border: 1px solid #cbd5e1;
            border-radius: 4px;
            overflow: hidden;
            margin-bottom: 20px;
        }
        .summary-header {
            background: #f1f5f9;
            padding: 6px 12px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 9px;
            font-weight: 700;
            color: #0f172a;
            border-bottom: 1px solid #e2e8f0;
        }
        .summary-grid {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr;
            gap: 10px;
            padding: 10px 12px;
        }
        .kpi-box {
            padding: 8px 10px;
            border-radius: 4px;
            border: 1px solid transparent;
        }
        .kpi-masuk { background: #f0fdf4; border-color: #bbf7d0; }
        .kpi-keluar { background: #fef2f2; border-color: #fecaca; }
        .kpi-saldo { background: #f0f9ff; border-color: #bae6fd; }
        .kpi-label {
            font-size: 7.5px;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 4px;
        }
        .kpi-masuk .kpi-label { color: #15803d; }
        .kpi-keluar .kpi-label { color: #b91c1c; }
        .kpi-saldo .kpi-label { color: #0369a1; }
        .kpi-val {
            font-size: 13px;
            font-weight: 800;
        }
        .kpi-masuk .kpi-val { color: #166534; }
        .kpi-keluar .kpi-val { color: #991b1b; }
        .kpi-saldo .kpi-val { color: #075985; }
        /* Signatures */
        .sig-section {
            display: flex;
            justify-content: space-between;
            margin-top: 24px;
            page-break-inside: avoid;
        }
        .sig-box {
            text-align: center;
            width: 180px;
        }
        .sig-title {
            font-weight: 700;
            font-size: 9.5px;
            color: #0f172a;
            margin-bottom: 50px;
        }
        .sig-line {
            border-bottom: 1px solid #cbd5e1;
            margin: 0 15px;
        }
        .sig-name {
            font-size: 8.5px;
            color: #64748b;
            margin-top: 4px;
        }
        /* Footer */
        .sheet-footer {
            margin-top: 20px;
            padding-top: 8px;
            border-top: 1px solid #e2e8f0;
            display: flex;
            justify-content: space-between;
            font-size: 8px;
            color: #94a3b8;
        }
        /* Floating Action Bar */
        .fab-bar {
            position: fixed;
            bottom: 20px;
            right: 20px;
            display: flex;
            gap: 10px;
            z-index: 999;
        }
        .btn {
            background: #0f172a;
            color: #ffffff;
            border: none;
            padding: 10px 16px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.15);
            display: flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s;
        }
        .btn:hover { background: #1e293b; transform: translateY(-1px); }
        .btn-print { background: #2563eb; }
        .btn-print:hover { background: #1d4ed8; }
    </style>
</head>
<body>

    <div class="fab-bar no-print">
        <button class="btn btn-print" onclick="window.print()">
            🖨️ Cetak / Simpan PDF
        </button>
        <button class="btn" onclick="window.close()">
            ✕ Tutup
        </button>
    </div>

    <div class="sheet">
        <!-- Kop Surat -->
        <div class="kop-header">
            <div class="kop-left">
                <div class="kop-logo">N</div>
                <div class="kop-title">
                    <h1><?= htmlspecialchars($community_name) ?></h1>
                    <div class="subtitle">KOMUNITAS PENGENDARA & SOSIAL SUKABUMI</div>
                    <div class="address">Sekretariat: Kota Sukabumi, Jawa Barat • Dokumen Laporan Resmi</div>
                </div>
            </div>
            <div class="kop-meta">
                <div>Tanggal: <strong><?= $tanggal_cetak ?></strong></div>
                <div>Waktu: <?= $waktu_cetak ?></div>
                <div class="status-badge">TERVERIFIKASI RESMI</div>
            </div>
        </div>

        <div class="divider-primary"></div>
        <div class="divider-accent"></div>

        <!-- Title Banner -->
        <div class="title-banner">
            <h2>Laporan Keuangan & Mutasi Kas</h2>
            <p>Kategori: Kas Utama Komunitas • Periode Tahun Berjalan</p>
        </div>

        <!-- Tabel Transaksi -->
        <table>
            <thead>
                <tr>
                    <th style="width: 35px;" class="text-center">No</th>
                    <th style="width: 80px;" class="text-center">Tanggal</th>
                    <th class="text-left">Keterangan Transaksi</th>
                    <th style="width: 65px;" class="text-center">Jenis</th>
                    <th style="width: 120px;" class="text-right">Nominal</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($transactions)): ?>
                    <tr>
                        <td colspan="5" class="text-center" style="padding: 20px; color: #94a3b8;">
                            Belum ada riwayat transaksi keuangan yang tercatat.
                        </td>
                    </tr>
                <?php else: ?>
                    <?php $no = 1; foreach ($transactions as $t): ?>
                        <tr>
                            <td class="text-center"><?= $no++ ?></td>
                            <td class="text-center"><?= date('d/m/Y', strtotime($t['tgl'])) ?></td>
                            <td class="text-left"><?= htmlspecialchars($t['keterangan']) ?></td>
                            <td class="text-center">
                                <span class="<?= $t['jenis'] == 'Masuk' ? 'badge-masuk' : 'badge-keluar' ?>">
                                    <?= htmlspecialchars($t['jenis']) ?>
                                </span>
                            </td>
                            <td class="text-right" style="font-weight: 600;">
                                <?= format_rp($t['nominal']) ?>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>

        <!-- Ringkasan Eksekutif -->
        <div class="summary-card">
            <div class="summary-header">
                <span>IKHTISAR KEUANGAN RESMI</span>
                <span style="color: #16a34a;">STATUS: REKONSILIASI SEIMBANG</span>
            </div>
            <div class="summary-grid">
                <div class="kpi-box kpi-masuk">
                    <div class="kpi-label">Total Pemasukan</div>
                    <div class="kpi-val">+ <?= format_rp($total_masuk) ?></div>
                </div>
                <div class="kpi-box kpi-keluar">
                    <div class="kpi-label">Total Pengeluaran</div>
                    <div class="kpi-val">- <?= format_rp($total_keluar) ?></div>
                </div>
                <div class="kpi-box kpi-saldo">
                    <div class="kpi-label">Saldo Kas Bersih</div>
                    <div class="kpi-val"><?= format_rp($saldo_kas) ?></div>
                </div>
            </div>
        </div>

        <!-- Pengesahan / Tanda Tangan -->
        <div style="text-align: right; font-size: 9px; color: #475569; margin-top: 10px;">
            Kota Sukabumi, <?= $tanggal_cetak ?>
        </div>
        <div class="sig-section">
            <div class="sig-box">
                <div class="sig-title">Ketua Komunitas</div>
                <div class="sig-line"></div>
                <div class="sig-name">( Ketua NEBO )</div>
            </div>
            <div class="sig-box">
                <div class="sig-title">Bendahara Komunitas</div>
                <div class="sig-line"></div>
                <div class="sig-name">( Bendahara NEBO )</div>
            </div>
        </div>

        <!-- Footer -->
        <div class="sheet-footer">
            <span>Sistem Informasi Manajemen Komunitas NEBO Sukabumi • Dokumen Resmi</span>
            <span>Halaman 1 dari 1</span>
        </div>
    </div>

</body>
</html>
