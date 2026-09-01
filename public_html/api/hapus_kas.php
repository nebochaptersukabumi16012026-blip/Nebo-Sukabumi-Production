<?php
// hapus_kas.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);

$id = 0;
if (isset($data->id)) {
    $id = intval($data->id);
} elseif (isset($_POST['id'])) {
    $id = intval($_POST['id']);
} elseif (isset($_GET['id'])) {
    $id = intval($_GET['id']);
}

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "ID transaksi tidak valid"
    ));
    exit();
}

try {
    $conn->beginTransaction();

    // 1. Eksekusi DELETE FROM riwayat_kas WHERE id = :id
    $stmt_del = $conn->prepare("DELETE FROM riwayat_kas WHERE id = :id");
    $stmt_del->execute(array(':id' => $id));

    $stmt_del_pem = $conn->prepare("DELETE FROM pembayaran WHERE id = :id");
    $stmt_del_pem->execute(array(':id' => $id));

    $stmt_del_kk = $conn->prepare("DELETE FROM kas_keliling WHERE id = :id");
    $stmt_del_kk->execute(array(':id' => $id));

    // 2. Hitung ulang Rekapitulasi Kas menggunakan SQL Aggregate SUM
    $stmt_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_kas");
    $row_in = $stmt_in->fetch(PDO::FETCH_ASSOC);
    $total_pemasukan = floatval($row_in['total'] ?? 0);

    $stmt_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM pengeluaran");
    $row_out = $stmt_out->fetch(PDO::FETCH_ASSOC);
    $total_pengeluaran = floatval($row_out['total'] ?? 0);

    $saldo_terbaru = max(0, $total_pemasukan - $total_pengeluaran);

    // Sinkronisasi ke master ledger
    try {
        $stmt_upd = $conn->prepare("
            INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) 
            VALUES ('kas_utama', :in, :out) 
            ON DUPLICATE KEY UPDATE 
                total_akumulasi_masuk = :in,
                total_akumulasi_keluar = :out
        ");
        $stmt_upd->execute(array(':in' => $total_pemasukan, ':out' => $total_pengeluaran));
    } catch (Exception $e_master) {}

    $conn->commit();

    http_response_code(200);
    echo json_encode(array(
        "status" => "success",
        "message" => "Transaksi berhasil dihapus dan rekapitulasi diperbarui",
        "total_pemasukan" => $total_pemasukan,
        "total_pengeluaran" => $total_pengeluaran,
        "saldo_terbaru" => $saldo_terbaru
    ));

} catch (Throwable $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal menghapus transaksi: " . $e->getMessage()
    ));
}
?>
