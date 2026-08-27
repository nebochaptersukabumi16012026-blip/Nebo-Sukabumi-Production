<?php
// tambah_kas.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
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
        "message" => "Akses Ditolak: Hanya akun Developer yang diizinkan menginput data Kas!"
    ));
    exit();
}

$id_anggota = 0;
if (isset($data->id_anggota)) {
    $id_anggota = intval($data->id_anggota);
} elseif (isset($data->anggota_id)) {
    $id_anggota = intval($data->anggota_id);
} elseif (isset($data->anggotaId)) {
    $id_anggota = intval($data->anggotaId);
} elseif (isset($_POST['id_anggota'])) {
    $id_anggota = intval($_POST['id_anggota']);
}

$nominal = 0.0;
if (isset($data->nominal)) {
    $nominal = floatval($data->nominal);
} elseif (isset($_POST['nominal'])) {
    $nominal = floatval($_POST['nominal']);
}

$keterangan = 'Iuran Kas Anggota';
if (isset($data->keterangan) && trim($data->keterangan) !== '') {
    $keterangan = trim($data->keterangan);
} elseif (isset($_POST['keterangan']) && trim($_POST['keterangan']) !== '') {
    $keterangan = trim($_POST['keterangan']);
}

if ($id_anggota <= 0 || $nominal <= 0) {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "id_anggota dan nominal wajib diisi dan harus lebih dari 0"
    ));
    exit();
}

try {
    $conn->beginTransaction();

    // 1. Simpan detail ke tabel riwayat_kas
    $stmt_rk = $conn->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, keterangan, tanggal) VALUES (:id_anggota, :nominal, :keterangan, NOW())");
    $stmt_rk->execute(array(
        ':id_anggota' => $id_anggota,
        ':nominal' => $nominal,
        ':keterangan' => $keterangan
    ));
    $insertedId = $conn->lastInsertId();

    // Mirror to pembayaran
    try {
        $stmt_pem = $conn->prepare("INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) SELECT ?, nama, 'KAS', ?, ?, ? FROM anggota WHERE id = ?");
        $stmt_pem->execute(array($id_anggota, $nominal, time() * 1000, $keterangan, $id_anggota));
    } catch (Exception $e) {}

    // 2. Jalankan perintah UPDATE master_ledger (saldo_akumulasi) total_akumulasi_masuk = total_akumulasi_masuk + nominal_baru
    $stmt_master = $conn->prepare("
        INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk) 
        VALUES ('kas_utama', :nominal) 
        ON DUPLICATE KEY UPDATE total_akumulasi_masuk = total_akumulasi_masuk + :nominal
    ");
    $stmt_master->execute(array(':nominal' => $nominal));

    // Update profil anggota uang_kas
    $stmt_anggota = $conn->prepare("UPDATE anggota SET uang_kas = uang_kas + :nominal WHERE id = :id_anggota");
    $stmt_anggota->execute(array(':nominal' => $nominal, ':id_anggota' => $id_anggota));

    $conn->commit();

    http_response_code(200);
    echo json_encode(array(
        "status" => "success",
        "message" => "Kas berhasil ditambahkan",
        "inserted_id" => $insertedId
    ));

} catch (Throwable $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal menambah kas: " . $e->getMessage()
    ));
}
?>
