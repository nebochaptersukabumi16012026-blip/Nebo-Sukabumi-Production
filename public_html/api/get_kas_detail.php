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
        // 1. ISOLASI PEMASUKAN KHUSUS KAS UTAMA (Gunakan Master Akumulasi agar stabil)
        $stmt_master = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_utama'");
        $row_master = $stmt_master ? $stmt_master->fetch(PDO::FETCH_ASSOC) : null;
        $total_pemasukan = $row_master ? floatval($row_master['total_akumulasi_masuk']) : 0.0;
        $total_pengeluaran = $row_master ? floatval($row_master['total_akumulasi_keluar']) : 0.0;


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
                "nama" => isset($row['nama']) ? $row['nama'] : '',
                "nra" => (string)(isset($row['nra']) ? $row['nra'] : '-'),
                "nominal" => (float)(isset($row['uang_kas']) ? $row['uang_kas'] : 0),
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
    } catch (Throwable $e) {
        // http_response_code(500);
        echo json_encode(array("status" => "error", "message" => "Database error: " . $e->getMessage()));
    }
} else {
    http_response_code(405);
    echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
}
?>
