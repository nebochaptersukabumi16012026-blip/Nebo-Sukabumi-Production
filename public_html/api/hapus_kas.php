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
include_once 'sync_helper.php';

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);

$role = '';
if (isset($data->role)) {
    $role = trim($data->role);
} elseif (isset($data->user_role)) {
    $role = trim($data->user_role);
} elseif (isset($_POST['role'])) {
    $role = trim($_POST['role']);
} elseif (isset($_POST['user_role'])) {
    $role = trim($_POST['user_role']);
}

if (empty($role) || strtolower($role) !== 'developer') {
    http_response_code(403);
    echo json_encode(array(
        "status" => "error",
        "message" => "Akses Ditolak: Hanya akun Developer yang diizinkan menghapus transaksi!"
    ));
    exit();
}

$raw_id = '';
if (isset($data->id_transaksi)) {
    $raw_id = strval($data->id_transaksi);
} elseif (isset($data->id)) {
    $raw_id = strval($data->id);
} elseif (isset($_GET['id'])) {
    $raw_id = strval($_GET['id']);
} elseif (isset($_POST['id'])) {
    $raw_id = strval($_POST['id']);
}

$id = 0;
if (strpos($raw_id, 'kas_') === 0) {
    $id = intval(substr($raw_id, 4));
} else {
    $id = intval($raw_id);
}

$id_anggota = 0;
if (isset($data->id_anggota)) {
    $id_anggota = intval($data->id_anggota);
} elseif (isset($data->anggota_id)) {
    $id_anggota = intval($data->anggota_id);
}

if ($id <= 0 && $id_anggota <= 0) {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "ID transaksi atau ID Anggota tidak valid"
    ));
    exit();
}

try {
    $conn->beginTransaction();

    // 3. Alur Hapus Transaksi: Jalankan HANYA DELETE FROM riwayat_kas WHERE id = :id.
    // DILARANG HARAM menjalankan UPDATE pengurangan pada master_ledger atau menghitung ulang total pemasukan saat hapus.
    if ($id > 0) {
        $stmt_del = $conn->prepare("DELETE FROM riwayat_kas WHERE id = ?");
        $stmt_del->execute(array($id));

        $stmt_del_pem = $conn->prepare("DELETE FROM pembayaran WHERE id = ?");
        $stmt_del_pem->execute(array($id));
    }

    if ($id <= 0 && $id_anggota > 0) {
        $stmt_del_all = $conn->prepare("DELETE FROM riwayat_kas WHERE id_anggota = ?");
        $stmt_del_all->execute(array($id_anggota));

        $stmt_del_all_pem = $conn->prepare("DELETE FROM pembayaran WHERE anggotaId = ? AND LOWER(jenisPembayaran) IN ('kas', 'uang_kas')");
        $stmt_del_all_pem->execute(array($id_anggota));
    }

    $conn->commit();

    http_response_code(200);
    echo json_encode(array(
        "status" => "success",
        "message" => "Riwayat kas berhasil dihapus tanpa mengubah saldo total master"
    ));

} catch (Throwable $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal menghapus riwayat: " . $e->getMessage()
    ));
}
?>
