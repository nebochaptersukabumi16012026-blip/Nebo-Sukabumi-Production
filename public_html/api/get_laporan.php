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
    // Initialize variables to avoid notices
    $kas_utama_in = 0;
    $kas_utama_out = 0;
    $kas_utama_saldo = 0;
    $kk_in = 0;
    $kk_out_table = 0;
    $kk_out_pengeluaran = 0;
    $kk_out = 0;
    $kk_saldo = 0;
    $aniv_in = 0;
    $aniv_out = 0;
    $aniv_saldo = 0;
    $cicilan_total_harga = 0;
    $cicilan_sisa = 0;
    $cicilan_sudah_bayar = 0;

    try {
        // 1. KAS UTAMA (Membaca Master Akumulasi agar kebal saat riwayat dihapus)
        $stmt_master_kas = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_utama'");
        $row_master_kas = $stmt_master_kas ? $stmt_master_kas->fetch(PDO::FETCH_ASSOC) : null;
        $kas_utama_in = $row_master_kas ? floatval($row_master_kas['total_akumulasi_masuk']) : 0.0;
        $kas_utama_out = $row_master_kas ? floatval($row_master_kas['total_akumulasi_keluar']) : 0.0;

        $kas_utama_saldo = max(0, $kas_utama_in - $kas_utama_out);

        // 2. KAS KELILING
        $stmt_master_kk = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_keliling'");
        $row_master_kk = $stmt_master_kk ? $stmt_master_kk->fetch(PDO::FETCH_ASSOC) : null;
        $kk_in = $row_master_kk ? floatval($row_master_kk['total_akumulasi_masuk']) : 0.0;
        $kk_out = $row_master_kk ? floatval($row_master_kk['total_akumulasi_keluar']) : 0.0;

        $kk_saldo = max(0, $kk_in - $kk_out);

        // 3. KAS ANNIVERSARY (Membaca Master Akumulasi agar kebal saat riwayat dihapus)
        $stmt_master_aniv = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_aniv'");
        $row_master_aniv = $stmt_master_aniv ? $stmt_master_aniv->fetch(PDO::FETCH_ASSOC) : null;
        $aniv_in = $row_master_aniv ? floatval($row_master_aniv['total_akumulasi_masuk']) : 0.0;
        $aniv_out = $row_master_aniv ? floatval($row_master_aniv['total_akumulasi_keluar']) : 0.0;

        $aniv_saldo = max(0, $aniv_in - $aniv_out);

        // 4. CICILAN (Informasi Pelengkap)
        $stmt_cicilan = $conn->query("SELECT COALESCE(SUM(harga_barang), 0) AS total_harga, COALESCE(SUM(sisa_cicilan), 0) AS total_sisa FROM anggota WHERE harga_barang > 0");
        if ($stmt_cicilan) {
            $row_cicilan = $stmt_cicilan->fetch(PDO::FETCH_ASSOC);
            $cicilan_total_harga = floatval(isset($row_cicilan['total_harga']) ? $row_cicilan['total_harga'] : 0);
            $cicilan_sisa = floatval(isset($row_cicilan['total_sisa']) ? $row_cicilan['total_sisa'] : 0);
        }
        $cicilan_sudah_bayar = $cicilan_total_harga - $cicilan_sisa;

        echo json_encode(array(
            "status" => "success",
            "kas_utama" => array(
                "total_pemasukan" => $kas_utama_in,
                "total_pengeluaran" => $kas_utama_out,
                "saldo_kas" => $kas_utama_saldo,
                "saldo" => $kas_utama_saldo
            ),
            "kas_keliling" => array(
                "total_pemasukan" => $kk_in,
                "total_pengeluaran" => $kk_out,
                "saldo_keliling" => $kk_saldo,
                "saldo" => $kk_saldo
            ),
            "kas_anniversary" => array(
                "total_pemasukan" => $aniv_in,
                "total_pengeluaran" => $aniv_out,
                "saldo_aniv" => $aniv_saldo,
                "saldo" => $aniv_saldo
            ),
            "cicilan" => array(
                "total_harga_barang" => $cicilan_total_harga,
                "total_sudah_dibayar" => $cicilan_sudah_bayar,
                "total_sisa_cicilan" => $cicilan_sisa
            ),
            "data" => array(
                "kas_utama" => array(
                    "total_pemasukan" => $kas_utama_in,
                    "total_pengeluaran" => $kas_utama_out,
                    "saldo_kas" => $kas_utama_saldo,
                    "saldo" => $kas_utama_saldo
                ),
                "kas_keliling" => array(
                    "total_pemasukan" => $kk_in,
                    "total_pengeluaran" => $kk_out,
                    "saldo_keliling" => $kk_saldo,
                    "saldo" => $kk_saldo
                ),
                "kas_anniversary" => array(
                    "total_pemasukan" => $aniv_in,
                    "total_pengeluaran" => $aniv_out,
                    "saldo_aniv" => $aniv_saldo,
                    "saldo" => $aniv_saldo
                )
            )
        ));
    } catch (Throwable $e) {
        // http_response_code(500);
        error_log("Laporan Error: " . $e->getMessage());
        echo json_encode(array(
            "status" => "error",
            "message" => "Server error: " . $e->getMessage(),
            "file" => $e->getFile(),
            "line" => $e->getLine()
        ));
    }
} else {
    http_response_code(405);
    echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
}
?>
