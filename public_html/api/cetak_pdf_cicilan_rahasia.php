<?php
// ==============================================================================
// CETAK PDF / LAPORAN RESMI CICILAN RAHASIA - NEBO SUKABUMI
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

$cicilan_list = [];
$total_harga_barang = 0;
$total_sisa_cicilan = 0;
$lunas_count = 0;
$belum_lunas_count = 0;

if (isset($conn) || isset($pdo)) {
    $db = isset($conn) ? $conn : $pdo;
    try {
        $stmt = $db->query("SELECT id, nama, harga_barang, sisa_cicilan, lama_cicilan, cicilan_per_bulan 
                            FROM anggota 
                            WHERE harga_barang > 0 
                            ORDER BY nama ASC");
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $hb = floatval($row['harga_barang']);
            $sc = floatval($row['sisa_cicilan']);
            $total_harga_barang += $hb;
            $total_sisa_cicilan += $sc;
            if ($sc <= 0) {
                $lunas_count++;
            } else {
                $belum_lunas_count++;
            }
            $cicilan_list[] = $row;
        }
    } catch (Exception $e) {}
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
    <title>Laporan Data Cicilan Anggota - <?= htmlspecialchars($community_name) ?></title>
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
            background: #fee2e2;
            color: #b91c1c;
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
        .badge-lunas {
            color: #16a34a;
            font-weight: 700;
        }
        .badge-berjalan {
            color: #dc2626;
            font-weight: 700;
        }
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
        .kpi-barang { background: #f1f5f9; border-color: #cbd5e1; }
        .kpi-sisa { background: #fef2f2; border-color: #fecaca; }
        .kpi-status { background: #f0fdf4; border-color: #bbf7d0; }
        .kpi-label {
            font-size: 7.5px;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 4px;
        }
        .kpi-barang .kpi-label { color: #475569; }
        .kpi-sisa .kpi-label { color: #b91c1c; }
        .kpi-status .kpi-label { color: #15803d; }
        .kpi-val {
            font-size: 13px;
            font-weight: 800;
        }
        .kpi-barang .kpi-val { color: #0f172a; }
        .kpi-sisa .kpi-val { color: #991b1b; }
        .kpi-status .kpi-val { color: #166534; font-size: 11px; }
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
        .sheet-footer {
            margin-top: 20px;
            padding-top: 8px;
            border-top: 1px solid #e2e8f0;
            display: flex;
            justify-content: space-between;
            font-size: 8px;
            color: #94a3b8;
        }
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
                    <div class="address">Sekretariat: Kota Sukabumi, Jawa Barat • Dokumen Internal Terbatas</div>
                </div>
            </div>
            <div class="kop-meta">
                <div>Tanggal: <strong><?= $tanggal_cetak ?></strong></div>
                <div>Waktu: <?= $waktu_cetak ?></div>
                <div class="status-badge">KHUSUS PENGURUS (INTERNAL)</div>
            </div>
        </div>

        <div class="divider-primary"></div>
        <div class="divider-accent"></div>

        <!-- Title Banner -->
        <div class="title-banner">
            <h2>Laporan Data Fasilitas Kredit Cicilan Anggota</h2>
            <p>Kategori: Fasilitas Internal Komunitas • Dokumen Rekapitulasi</p>
        </div>

        <!-- Tabel Cicilan -->
        <table>
            <thead>
                <tr>
                    <th style="width: 30px;" class="text-center">No</th>
                    <th class="text-left">Nama Anggota</th>
                    <th style="width: 60px;" class="text-left">Barang</th>
                    <th style="width: 80px;" class="text-right">Harga Barang</th>
                    <th style="width: 45px;" class="text-center">Tenor</th>
                    <th style="width: 80px;" class="text-right">Cicilan/Bln</th>
                    <th style="width: 85px;" class="text-right">Sisa Cicilan</th>
                    <th style="width: 70px;" class="text-center">Status</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($cicilan_list)): ?>
                    <tr>
                        <td colspan="8" class="text-center" style="padding: 20px; color: #94a3b8;">
                            Tidak ada anggota yang memiliki data cicilan aktif.
                        </td>
                    </tr>
                <?php else: ?>
                    <?php $no = 1; foreach ($cicilan_list as $c): ?>
                        <?php $isLunas = floatval($c['sisa_cicilan']) <= 0; ?>
                        <tr>
                            <td class="text-center"><?= $no++ ?></td>
                            <td class="text-left" style="font-weight: 600;"><?= htmlspecialchars($c['nama']) ?></td>
                            <td class="text-left">Barang</td>
                            <td class="text-right"><?= format_rp($c['harga_barang']) ?></td>
                            <td class="text-center"><?= intval($c['lama_cicilan']) ?> Bln</td>
                            <td class="text-right"><?= format_rp($c['cicilan_per_bulan']) ?></td>
                            <td class="text-right" style="font-weight: 600;"><?= format_rp($c['sisa_cicilan']) ?></td>
                            <td class="text-center">
                                <span class="<?= $isLunas ? 'badge-lunas' : 'badge-berjalan' ?>">
                                    <?= $isLunas ? 'Lunas' : 'Belum Lunas' ?>
                                </span>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>

        <!-- Ringkasan Eksekutif -->
        <div class="summary-card">
            <div class="summary-header">
                <span>RINGKASAN FASILITAS CICILAN</span>
                <span style="color: #2563eb;">AUDIT INTERNAL</span>
            </div>
            <div class="summary-grid">
                <div class="kpi-box kpi-barang">
                    <div class="kpi-label">Total Harga Barang</div>
                    <div class="kpi-val"><?= format_rp($total_harga_barang) ?></div>
                </div>
                <div class="kpi-box kpi-sisa">
                    <div class="kpi-label">Total Sisa Tagihan</div>
                    <div class="kpi-val"><?= format_rp($total_sisa_cicilan) ?></div>
                </div>
                <div class="kpi-box kpi-status">
                    <div class="kpi-label">Status Pelunasan</div>
                    <div class="kpi-val"><?= $lunas_count ?> Lunas • <?= $belum_lunas_count ?> Berjalan</div>
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
            <span>Sistem Informasi Manajemen Komunitas NEBO Sukabumi • Dokumen Internal Terbatas</span>
            <span>Halaman 1 dari 1</span>
        </div>
    </div>

</body>
</html>
