<?php
// get_laporan.php - Direct Kas Keliling Synchronization
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $kas_utama_in = 0.0;
    $kas_utama_out = 0.0;
    $kas_utama_saldo = 0.0;
    
    $kk_in = 0.0;
    $kk_out = 0.0;
    $kk_saldo = 0.0;
    
    $aniv_in = 0.0;
    $aniv_out = 0.0;
    $aniv_saldo = 0.0;
    
    $cicilan_total_harga = 0.0;
    $cicilan_sudah_bayar = 0.0;
    $cicilan_sisa = 0.0;
    $anggota_mencicil = 0;

    try {
        $db = isset($conn) ? $conn : (isset($pdo) ? $pdo : null);
        if (!$db) {
            throw new PDOException("Koneksi database tidak tersedia.");
        }

        // 1. KAS UTAMA (Master Akumulasi)
        $stmt_master_kas = $db->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_utama'");
        $row_master_kas = $stmt_master_kas ? $stmt_master_kas->fetch(PDO::FETCH_ASSOC) : null;
        $kas_utama_in = $row_master_kas ? floatval($row_master_kas['total_akumulasi_masuk']) : 0.0;
        $kas_utama_out = $row_master_kas ? floatval($row_master_kas['total_akumulasi_keluar']) : 0.0;
        $kas_utama_saldo = max(0.0, $kas_utama_in - $kas_utama_out);

        // 2. KAS KELILING (Langsung dari tabel kas_keliling)
        $col_amount_kk = "nominal";
        try {
            $checkColKk = $db->query("SHOW COLUMNS FROM kas_keliling LIKE 'jumlah'");
            if ($checkColKk && $checkColKk->rowCount() > 0) {
                $col_amount_kk = "jumlah";
            }
        } catch (Exception $e) {
            $col_amount_kk = "nominal";
        }

        // Pemasukan Kas Keliling
        $stmt_kk_in = $db->query("SELECT COALESCE(SUM({$col_amount_kk}), 0) AS total 
                                  FROM kas_keliling 
                                  WHERE LOWER(jenis) = 'pemasukan' 
                                     OR LOWER(jenis_transaksi) = 'pemasukan' 
                                     OR (COALESCE(jenis, '') = '' AND COALESCE(jenis_transaksi, '') = '')");
        $kk_in = floatval($stmt_kk_in ? $stmt_kk_in->fetch(PDO::FETCH_ASSOC)['total'] : 0.0);

        // Pengeluaran Kas Keliling
        $stmt_kk_out = $db->query("SELECT COALESCE(SUM({$col_amount_kk}), 0) AS total 
                                   FROM kas_keliling 
                                   WHERE LOWER(jenis) = 'pengeluaran' 
                                      OR LOWER(jenis_transaksi) = 'pengeluaran'");
        $kk_out = floatval($stmt_kk_out ? $stmt_kk_out->fetch(PDO::FETCH_ASSOC)['total'] : 0.0);

        // Saldo Kas Keliling
        $kk_saldo = max(0.0, $kk_in - $kk_out);

        // 3. KAS ANNIVERSARY (Master Akumulasi)
        $stmt_master_aniv = $db->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_aniv'");
        $row_master_aniv = $stmt_master_aniv ? $stmt_master_aniv->fetch(PDO::FETCH_ASSOC) : null;
        $aniv_in = $row_master_aniv ? floatval($row_master_aniv['total_akumulasi_masuk']) : 0.0;
        $aniv_out = $row_master_aniv ? floatval($row_master_aniv['total_akumulasi_keluar']) : 0.0;
        $aniv_saldo = max(0.0, $aniv_in - $aniv_out);

        // 4. REKAPITULASI CICILAN
        $queryFromCicilanTable = false;
        try {
            $checkCol = $db->query("SHOW COLUMNS FROM cicilan LIKE 'harga_barang'");
            if ($checkCol && $checkCol->rowCount() > 0) {
                $queryFromCicilanTable = true;
            }
        } catch (Exception $ignored) {
            $queryFromCicilanTable = false;
        }

        if ($queryFromCicilanTable) {
            $stmt_cicilan = $db->query("SELECT 
                COALESCE(SUM(harga_barang), 0) AS total_harga_barang,
                COALESCE(SUM(sudah_dibayar), 0) AS total_sudah_dibayar,
                COALESCE(SUM(harga_barang - sudah_dibayar), 0) AS total_sisa_cicilan,
                COUNT(DISTINCT nra) AS anggota_mencicil
            FROM cicilan 
            WHERE (harga_barang - sudah_dibayar) > 0");

            if ($stmt_cicilan && $row_c = $stmt_cicilan->fetch(PDO::FETCH_ASSOC)) {
                $anggota_mencicil = intval($row_c['anggota_mencicil'] ?? 0);
                if ($anggota_mencicil > 0) {
                    $cicilan_total_harga = floatval($row_c['total_harga_barang'] ?? 0);
                    $cicilan_sudah_bayar = floatval($row_c['total_sudah_dibayar'] ?? 0);
                    $cicilan_sisa = floatval($row_c['total_sisa_cicilan'] ?? 0);
                }
            }
        } else {
            $stmt_cicilan = $db->query("SELECT 
                COALESCE(SUM(harga_barang), 0) AS total_harga_barang,
                COALESCE(SUM(total_cicilan), 0) AS total_sudah_dibayar,
                COALESCE(SUM(CASE WHEN sisa_cicilan > 0 THEN sisa_cicilan ELSE (COALESCE(harga_barang, 0) - COALESCE(total_cicilan, 0)) END), 0) AS total_sisa_cicilan,
                COUNT(DISTINCT nra) AS anggota_mencicil
            FROM anggota 
            WHERE (COALESCE(harga_barang, 0) - COALESCE(total_cicilan, 0) > 0 OR COALESCE(sisa_cicilan, 0) > 0)");

            if ($stmt_cicilan && $row_c = $stmt_cicilan->fetch(PDO::FETCH_ASSOC)) {
                $anggota_mencicil = intval($row_c['anggota_mencicil'] ?? 0);
                if ($anggota_mencicil > 0) {
                    $cicilan_total_harga = floatval($row_c['total_harga_barang'] ?? 0);
                    $cicilan_sudah_bayar = floatval($row_c['total_sudah_dibayar'] ?? 0);
                    $cicilan_sisa = floatval($row_c['total_sisa_cicilan'] ?? 0);
                }
            }
        }

        if ($anggota_mencicil <= 0) {
            $cicilan_total_harga = 0.0;
            $cicilan_sudah_bayar = 0.0;
            $cicilan_sisa = 0.0;
            $anggota_mencicil = 0;
        }

        // Output JSON Terstruktur & Kompatibel
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
                "total_sisa_cicilan" => $cicilan_sisa,
                "anggota_mencicil" => $anggota_mencicil
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
        ], JSON_UNESCAPED_UNICODE);

    } catch (Throwable $e) {
        error_log("Error in get_laporan.php: " . $e->getMessage());
        http_response_code(500);
        echo json_encode([
            "status" => "error",
            "message" => "Terjadi kesalahan pada server saat memuat laporan keuangan."
        ], JSON_UNESCAPED_UNICODE);
    }
} else {
    http_response_code(405);
    echo json_encode([
        "status" => "error",
        "message" => "Method Not Allowed"
    ], JSON_UNESCAPED_UNICODE);
}
?>
