<?php
// get_kas_detail.php & detail_kas.php
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
    try {
        // 1. ISOLASI PEMASUKAN KHUSUS KAS UTAMA
        $stmt_in_anggota = $conn->query("SELECT COALESCE(SUM(uang_kas), 0) AS total FROM anggota");
        $pemasukan_anggota = floatval($stmt_in_anggota->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

        $stmt_in_pem = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total FROM pembayaran WHERE LOWER(jenisPembayaran) IN ('kas', 'uang_kas')");
        $pemasukan_pem = floatval($stmt_in_pem->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

        $stmt_in_rk = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total FROM riwayat_kas");
        $pemasukan_rk = floatval($stmt_in_rk->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

        $total_pemasukan = max($pemasukan_anggota, $pemasukan_pem, $pemasukan_rk);

        // 2. ISOLASI PENGELUARAN KHUSUS KAS UTAMA (JANGAN gabungkan kas keliling / kas anniversary / cicilan)
        $stmt_pengeluaran = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_pengeluaran FROM pengeluaran 
            WHERE (LOWER(COALESCE(jenis_kas, '')) IN ('kas_utama', 'saldo kas', 'kas', 'kas utama', 'uang kas', 'uang_kas') 
               OR (LOWER(COALESCE(jenis_kas, '')) NOT IN ('kas_keliling', 'kas keliling', 'kas_aniv', 'kas aniv', 'kas_anniversary', 'kas anniversary', 'dana cicilan', 'cicilan') AND LOWER(COALESCE(jenis_kas, '')) != ''))");
        $row_pengeluaran = $stmt_pengeluaran->fetch(PDO::FETCH_ASSOC);
        $total_pengeluaran = floatval($row_pengeluaran['total_pengeluaran'] ?? 0);

        // 3. LOGIKA GAIRAH SALDO (PREVENT MINUS LOGIC: Jika saldo < 0, set menjadi 0)
        $raw_saldo = $total_pemasukan - $total_pengeluaran;
        $saldo = max(0, $raw_saldo);

        // 4. Query daftar riwayat kas anggota
        $stmt_anggota = $conn->query("
            SELECT id, nama, nra, uang_kas, tgl_gabung 
            FROM anggota 
            WHERE uang_kas > 0 
            ORDER BY id DESC
        ");
        $rows_anggota = $stmt_anggota->fetchAll(PDO::FETCH_ASSOC);

        $riwayat = array();
        foreach ($rows_anggota as $row) {
            $tgl = !empty($row['tgl_gabung']) ? $row['tgl_gabung'] : "Hari Ini";
            $riwayat[] = array(
                "id" => (int)$row['id'],
                "nama" => $row['nama'] ?? '',
                "nra" => (string)($row['nra'] ?? '-'),
                "nominal" => (float)($row['uang_kas'] ?? 0),
                "tanggal" => $tgl,
                "keterangan" => "Iuran Kas Anggota",
                "status" => "TERKONFIRMASI"
            );
        }

        $response = array(
            "status" => "success",
            "total_pemasukan" => $total_pemasukan,
            "total_pengeluaran" => $total_pengeluaran,
            "saldo" => $saldo,
            "saldo_kas" => $saldo,
            "riwayat" => $riwayat,
            "kas_utama" => array(
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo_kas" => $saldo,
                "saldo" => $saldo
            ),
            "data" => array(
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => $saldo,
                "saldo_kas" => $saldo,
                "riwayat" => $riwayat
            )
        );

        echo json_encode($response);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
    }
} else {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
}
?>
