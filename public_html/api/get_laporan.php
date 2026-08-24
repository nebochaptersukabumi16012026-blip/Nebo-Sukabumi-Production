<?php
// get_laporan.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method == 'GET') {
    try {
        // 1. KAS UTAMA
        // TOTAL KAS UTAMA = SUM(uang_kas) dari tabel 'anggota' - SUM(nominal) dari tabel 'pengeluaran' kategori 'kas_utama'
        $stmt_kas_in = $conn->query("SELECT COALESCE(SUM(uang_kas), 0) AS total_in FROM anggota");
        $kas_utama_in = floatval($stmt_kas_in->fetch(PDO::FETCH_ASSOC)['total_in'] ?? 0);

        $stmt_kas_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_out FROM pengeluaran 
            WHERE LOWER(jenis_kas) IN ('kas_utama', 'saldo kas', 'kas', 'kas utama', 'uang kas', 'uang_kas', '') 
            OR (LOWER(jenis_kas) NOT IN ('kas_keliling', 'kas keliling', 'kas_aniv', 'kas aniv', 'kas_anniversary', 'kas anniversary', 'dana cicilan', 'cicilan'))");
        $kas_utama_out = floatval($stmt_kas_out->fetch(PDO::FETCH_ASSOC)['total_out'] ?? 0);
        $kas_utama_saldo = max(0, $kas_utama_in - $kas_utama_out);

        // 2. KAS KELILING
        // TOTAL KAS KELILING = SUM(nominal / total_pemasukan) dari tabel 'kas_keliling' - SUM(nominal) dari tabel 'pengeluaran' kategori 'kas_keliling'
        // Let's compute total pemasukan from kas_keliling table
        $stmt_kk_in = $conn->query("SELECT 
            COALESCE(SUM(CASE WHEN total_pemasukan > 0 THEN total_pemasukan WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE 0 END), 0) AS total_in,
            COALESCE(SUM(CASE WHEN total_pengeluaran > 0 THEN total_pengeluaran WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE 0 END), 0) AS total_out_kk
            FROM kas_keliling");
        $row_kk = $stmt_kk_in->fetch(PDO::FETCH_ASSOC);
        $kk_in = floatval($row_kk['total_in'] ?? 0);
        $kk_out_table = floatval($row_kk['total_out_kk'] ?? 0);

        $stmt_kk_pengeluaran = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_out FROM pengeluaran 
            WHERE LOWER(jenis_kas) IN ('kas_keliling', 'kas keliling')");
        $kk_out_pengeluaran = floatval($stmt_kk_pengeluaran->fetch(PDO::FETCH_ASSOC)['total_out'] ?? 0);
        $kk_out = $kk_out_pengeluaran > 0 ? $kk_out_pengeluaran : $kk_out_table;
        $kk_saldo = max(0, $kk_in - $kk_out);

        // 3. KAS ANNIVERSARY
        // TOTAL KAS ANIV = SUM(iuran_aniv) dari tabel 'anggota' - SUM(nominal) dari tabel 'pengeluaran' kategori 'kas_anniversary'
        $stmt_aniv_in = $conn->query("SELECT COALESCE(SUM(iuran_aniv), 0) AS total_in FROM anggota");
        $aniv_in = floatval($stmt_aniv_in->fetch(PDO::FETCH_ASSOC)['total_in'] ?? 0);

        $stmt_aniv_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_out FROM pengeluaran 
            WHERE LOWER(jenis_kas) IN ('kas_anniversary', 'kas anniversary', 'kas_aniv', 'kas aniv')");
        $aniv_out = floatval($stmt_aniv_out->fetch(PDO::FETCH_ASSOC)['total_out'] ?? 0);
        $aniv_saldo = max(0, $aniv_in - $aniv_out);

        // 4. CICILAN (Informasi Pelengkap)
        $stmt_cicilan = $conn->query("SELECT COALESCE(SUM(harga_barang), 0) AS total_harga, COALESCE(SUM(sisa_cicilan), 0) AS total_sisa FROM anggota WHERE harga_barang > 0");
        $row_cicilan = $stmt_cicilan->fetch(PDO::FETCH_ASSOC);
        $cicilan_total_harga = floatval($row_cicilan['total_harga'] ?? 0);
        $cicilan_sisa = floatval($row_cicilan['total_sisa'] ?? 0);
        $cicilan_sudah_bayar = $cicilan_total_harga - $cicilan_sisa;

        echo json_encode([
            "status" => "success",
            "kas_utama" => [
                "total_pemasukan" => $kas_utama_in,
                "total_pengeluaran" => $kas_utama_out,
                "saldo_kas" => $kas_utama_saldo,
                "saldo" => $kas_utama_saldo
            ],
            "kas_keliling" => [
                "total_pemasukan" => $kk_in,
                "total_pengeluaran" => $kk_out,
                "saldo_keliling" => $kk_saldo,
                "saldo" => $kk_saldo
            ],
            "kas_anniversary" => [
                "total_pemasukan" => $aniv_in,
                "total_pengeluaran" => $aniv_out,
                "saldo_aniv" => $aniv_saldo,
                "saldo" => $aniv_saldo
            ],
            "cicilan" => [
                "total_harga_barang" => $cicilan_total_harga,
                "total_sudah_dibayar" => $cicilan_sudah_bayar,
                "total_sisa_cicilan" => $cicilan_sisa
            ],
            "data" => [
                "kas_utama" => [
                    "total_pemasukan" => $kas_utama_in,
                    "total_pengeluaran" => $kas_utama_out,
                    "saldo_kas" => $kas_utama_saldo,
                    "saldo" => $kas_utama_saldo
                ],
                "kas_keliling" => [
                    "total_pemasukan" => $kk_in,
                    "total_pengeluaran" => $kk_out,
                    "saldo_keliling" => $kk_saldo,
                    "saldo" => $kk_saldo
                ],
                "kas_anniversary" => [
                    "total_pemasukan" => $aniv_in,
                    "total_pengeluaran" => $aniv_out,
                    "saldo_aniv" => $aniv_saldo,
                    "saldo" => $aniv_saldo
                ]
            ]
        ]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            "status" => "error",
            "message" => "Database error: " . $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
}
?>
