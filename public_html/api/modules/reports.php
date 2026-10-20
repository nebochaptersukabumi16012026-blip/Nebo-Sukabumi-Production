<?php
// workspace/refactored_api/modules/reports.php
// Modul Terpadu Laporan Keuangan & Cetak PDF

require_once __DIR__ . '/../core/config.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();
$action = $input['action'] ?? '';

if ($action === 'pdf_keuangan') {
    handlePdfKeuangan($pdo);
    exit();
} elseif ($action === 'pdf_cicilan') {
    handlePdfCicilan($pdo);
    exit();
}

// Default: Laporan JSON (get_laporan.php)
try {
    // 1. Kas Utama
    $stmt_rk = $pdo->query("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_kas");
    $kas_utama_in = floatval($stmt_rk ? $stmt_rk->fetch(PDO::FETCH_ASSOC)['total'] : 0);

    $stmt_out_ku = $pdo->query("
        SELECT COALESCE(SUM(nominal), 0) as total 
        FROM pengeluaran 
        WHERE LOWER(jenis_kas) IN ('kas_utama', 'kas utama', 'kas', 'saldo kas', 'uang kas', 'uang_kas') 
           OR (LOWER(jenis_kas) NOT LIKE '%keliling%' AND LOWER(jenis_kas) NOT LIKE '%aniv%' AND LOWER(jenis_kas) NOT LIKE '%anniversary%')
    ");
    $kas_utama_out = floatval($stmt_out_ku ? $stmt_out_ku->fetch(PDO::FETCH_ASSOC)['total'] : 0);
    $kas_utama_saldo = max(0.0, $kas_utama_in - $kas_utama_out);

    // 2. Kas Keliling
    require_once __DIR__ . '/../core/sync_helper.php';
    $rekap_kk = getKasKelilingSummary($pdo);
    $kk_in    = floatval($rekap_kk['total_pemasukan'] ?? 0);
    $kk_out   = floatval($rekap_kk['total_pengeluaran'] ?? 0);
    $kk_saldo = floatval($rekap_kk['saldo'] ?? 0);

    // 3. Kas Anniversary
    $stmt_ia = $pdo->query("SELECT COALESCE(SUM(nominal), 0) as total FROM iuran_anniversary");
    $aniv_in = floatval($stmt_ia ? $stmt_ia->fetch(PDO::FETCH_ASSOC)['total'] : 0);

    $stmt_out_aniv = $pdo->query("SELECT COALESCE(SUM(nominal), 0) as total FROM pengeluaran WHERE LOWER(jenis_kas) LIKE '%aniv%' OR LOWER(jenis_kas) LIKE '%anniversary%'");
    $aniv_out = floatval($stmt_out_aniv ? $stmt_out_aniv->fetch(PDO::FETCH_ASSOC)['total'] : 0);
    $aniv_saldo = max(0.0, $aniv_in - $aniv_out);

    // 4. Cicilan
    $stmt_cicilan = $pdo->query("
        SELECT 
            COALESCE(SUM(COALESCE(a.harga_barang, a.hargaBarang, 0)), 0) AS total_harga_barang,
            COALESCE(SUM(COALESCE(c.total_bayar, a.total_cicilan, 0)), 0) AS total_sudah_dibayar,
            COALESCE(SUM(CASE 
                WHEN COALESCE(a.harga_barang, a.hargaBarang, 0) > 0 
                    THEN (CASE WHEN (COALESCE(a.harga_barang, a.hargaBarang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0)) > 0 THEN (COALESCE(a.harga_barang, a.hargaBarang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0)) ELSE 0 END)
                ELSE (CASE WHEN COALESCE(a.sisa_cicilan, a.sisaCicilan, 0) > 0 THEN COALESCE(a.sisa_cicilan, a.sisaCicilan, 0) ELSE 0 END)
            END), 0) AS total_sisa_cicilan,
            COUNT(DISTINCT a.nra) AS anggota_mencicil
        FROM anggota a
        LEFT JOIN (
            SELECT anggota_id, COALESCE(SUM(nominal), 0) AS total_bayar
            FROM cicilan
            GROUP BY anggota_id
        ) c ON a.id = c.anggota_id
        WHERE ((COALESCE(a.harga_barang, a.hargaBarang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0) > 0) OR COALESCE(a.sisa_cicilan, a.sisaCicilan, 0) > 0)
    ");
    $row_c = $stmt_cicilan->fetch(PDO::FETCH_ASSOC);
    $cicilan_total_harga = floatval($row_c['total_harga_barang'] ?? 0);
    $cicilan_sudah_bayar = floatval($row_c['total_sudah_dibayar'] ?? 0);
    $cicilan_sisa        = floatval($row_c['total_sisa_cicilan'] ?? 0);
    $anggota_mencicil    = intval($row_c['anggota_mencicil'] ?? 0);

    $reportData = [
        "kas_utama" => [
            "total_pemasukan"   => $kas_utama_in,
            "total_pengeluaran" => $kas_utama_out,
            "saldo_kas"         => $kas_utama_saldo,
            "saldo"             => $kas_utama_saldo
        ],
        "kas_keliling" => [
            "total_pemasukan"   => $kk_in,
            "total_pengeluaran" => $kk_out,
            "saldo_kas_keliling"=> $kk_saldo,
            "saldo"             => $kk_saldo
        ],
        "kas_anniversary" => [
            "total_pemasukan"   => $aniv_in,
            "total_pengeluaran" => $aniv_out,
            "saldo_anniversary" => $aniv_saldo,
            "saldo"             => $aniv_saldo
        ],
        "cicilan" => [
            "total_harga_barang" => $cicilan_total_harga,
            "total_sudah_dibayar"=> $cicilan_sudah_bayar,
            "total_sisa_cicilan" => $cicilan_sisa,
            "anggota_mencicil"   => $anggota_mencicil
        ],
        "ringkasan" => [
            "total_saldo_komunitas" => $kas_utama_saldo + $kk_saldo + $aniv_saldo + $cicilan_sisa,
            "total_semua_pengeluaran" => $kas_utama_out + $kk_out + $aniv_out
        ]
    ];

    sendResponse(true, "Laporan keuangan berhasil dimuat", $reportData, $reportData);

} catch (Exception $e) {
    sendResponse(false, "Gagal memuat laporan: " . $e->getMessage(), null, [], 500);
}

function handlePdfKeuangan($pdo) {
    // Memanggil fpdf / cetak PDF jika library tersedia, atau menyajikan ringkasan printable HTML
    header("Content-Type: text/html; charset=UTF-8");
    echo "<h2>Laporan Keuangan Komunitas NEBO Sukabumi</h2>";
    echo "<p>Tanggal cetak: " . date('d F Y H:i:s') . "</p>";
    exit();
}

function handlePdfCicilan($pdo) {
    header("Content-Type: text/html; charset=UTF-8");
    echo "<h2>Laporan Cicilan Rahasia Komunitas NEBO Sukabumi</h2>";
    echo "<p>Tanggal cetak: " . date('d F Y H:i:s') . "</p>";
    exit();
}
