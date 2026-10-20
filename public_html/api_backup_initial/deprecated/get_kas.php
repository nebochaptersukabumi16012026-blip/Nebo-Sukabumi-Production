<?php
// get_kas.php - Sinkronisasi Rekapitulasi & Laporan Kas Keliling
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

try {
    $db = isset($conn) ? $conn : (isset($pdo) ? $pdo : null);
    if (!$db) {
        throw new PDOException("Koneksi database tidak tersedia.");
    }

    // Deteksi nama kolom nominal / jumlah secara fleksibel untuk mencegah SQL Error
    $col_amount = "nominal";
    try {
        $checkCol = $db->query("SHOW COLUMNS FROM kas_keliling LIKE 'jumlah'");
        if ($checkCol && $checkCol->rowCount() > 0) {
            $col_amount = "jumlah";
        }
    } catch (Exception $e) {
        $col_amount = "nominal";
    }

    // 1. QUERY PEMASUKAN KAS KELILING GLOBAL (Tanpa filter id_user)
    $queryIn = "SELECT COALESCE(SUM({$col_amount}), 0) AS total 
                FROM kas_keliling 
                WHERE LOWER(jenis) = 'pemasukan' 
                   OR LOWER(jenis_transaksi) = 'pemasukan' 
                   OR (COALESCE(jenis, '') = '' AND COALESCE(jenis_transaksi, '') = '')";
    $stmtIn = $db->prepare($queryIn);
    $stmtIn->execute();
    $rowIn = $stmtIn->fetch(PDO::FETCH_ASSOC);
    $pemasukan = floatval($rowIn['total'] ?? 0);

    // 2. QUERY PENGELUARAN KAS KELILING GLOBAL (Tanpa filter id_user)
    $queryOut = "SELECT COALESCE(SUM({$col_amount}), 0) AS total 
                 FROM kas_keliling 
                 WHERE LOWER(jenis) = 'pengeluaran' 
                    OR LOWER(jenis_transaksi) = 'pengeluaran'";
    $stmtOut = $db->prepare($queryOut);
    $stmtOut->execute();
    $rowOut = $stmtOut->fetch(PDO::FETCH_ASSOC);
    $pengeluaran = floatval($rowOut['total'] ?? 0);

    // 3. KALKULASI SALDO AKHIR
    $saldo_akhir = max(0, $pemasukan - $pengeluaran);

    // 4. RESPONS JSON BERSIH TERSTANDAR
    $response = [
        "status" => "success",
        "data" => [
            "pemasukan_kas_keliling" => $pemasukan,
            "pengeluaran_kas_keliling" => $pengeluaran,
            "saldo_kas_keliling" => $saldo_akhir
        ],
        // Field tambahan untuk kompatibilitas endpoint/aplikasi
        "pemasukan_kas_keliling" => $pemasukan,
        "pengeluaran_kas_keliling" => $pengeluaran,
        "saldo_kas_keliling" => $saldo_akhir,
        "total_pemasukan" => $pemasukan,
        "total_pengeluaran" => $pengeluaran,
        "saldo" => $saldo_akhir
    ];

    echo json_encode($response, JSON_UNESCAPED_UNICODE);

} catch (PDOException $e) {
    error_log("Database Error in get_kas.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Terjadi kesalahan pada basis data server."
    ], JSON_UNESCAPED_UNICODE);
} catch (Throwable $e) {
    error_log("General Error in get_kas.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Terjadi kesalahan sistem internal."
    ], JSON_UNESCAPED_UNICODE);
}
?>
