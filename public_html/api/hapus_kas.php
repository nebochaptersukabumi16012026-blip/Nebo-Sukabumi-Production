<?php
// hapus_kas.php - Khusus Kas Keliling
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
        "message" => "ID transaksi kas keliling tidak valid"
    ));
    exit();
}

try {
    $conn->beginTransaction();

    // 1. HANYA hapus dari tabel kas_keliling (TERISOLASI: Jangan sentuh riwayat_kas atau kas komunitas)
    $stmt_del = $conn->prepare("DELETE FROM kas_keliling WHERE id = :id");
    $stmt_del->execute(array(':id' => $id));

    // 2. Hitung ulang rekapitulasi khusus tabel kas_keliling menggunakan SQL Aggregate SUM
    $stmt_in = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) as total FROM kas_keliling");
    $row_in = $stmt_in->fetch(PDO::FETCH_ASSOC);
    $total_pemasukan = floatval($row_in['total'] ?? 0);

    $stmt_out = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE total_pengeluaran END), 0) as total FROM kas_keliling");
    $row_out = $stmt_out->fetch(PDO::FETCH_ASSOC);
    $total_pengeluaran = floatval($row_out['total'] ?? 0);

    $saldo_terbaru = max(0, $total_pemasukan - $total_pengeluaran);

    // Sinkronisasi master ledger khusus kas_keliling
    try {
        $stmt_upd = $conn->prepare("
            INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) 
            VALUES ('kas_keliling', :in, :out) 
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
        "message" => "Data kas keliling berhasil dihapus dan rekapitulasi diperbarui",
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
        "message" => "Gagal menghapus kas keliling: " . $e->getMessage()
    ));
}
?>
