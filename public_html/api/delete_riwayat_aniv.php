<?php
// delete_riwayat_aniv.php - Endpoint hapus riwayat anniversary dengan validasi DB
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, DELETE, GET, OPTIONS");
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

if (!isset($pdo) && isset($conn)) {
    $pdo = $conn;
}

// 1. BACA PARAMETER TERBUKA (POST, JSON Body, GET)
$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput, true) ?: array();

// 1. OTORISASI: ADMIN, BENDAHARA, DEVELOPER
$role = '';
if (!empty($data['user_role'])) $role = trim($data['user_role']);
elseif (!empty($data['role'])) $role = trim($data['role']);
elseif (!empty($_POST['user_role'])) $role = trim($_POST['user_role']);
elseif (!empty($_POST['role'])) $role = trim($_POST['role']);
elseif (!empty($_GET['user_role'])) $role = trim($_GET['user_role']);
elseif (!empty($_GET['role'])) $role = trim($_GET['role']);

$role_upper = strtoupper($role);
if (!empty($role) && $role_upper !== 'DEVELOPER' && $role_upper !== 'ADMIN' && $role_upper !== 'BENDAHARA' && $role_upper !== 'PENGURUS') {
    http_response_code(403);
    echo json_encode(array(
        "status" => false,
        "message" => "Akses Ditolak: Hanya ADMIN dan BENDAHARA yang dapat menghapus riwayat anniversary."
    ));
    exit();
}

// 2. PARSE ID TRANSAKSI
$id = 0;
if (isset($data['id_aniv']) && intval($data['id_aniv']) > 0) $id = intval($data['id_aniv']);
elseif (isset($data['id_transaksi']) && intval($data['id_transaksi']) > 0) $id = intval($data['id_transaksi']);
elseif (isset($data['id']) && intval($data['id']) > 0) $id = intval($data['id']);
elseif (isset($_POST['id_aniv']) && intval($_POST['id_aniv']) > 0) $id = intval($_POST['id_aniv']);
elseif (isset($_POST['id_transaksi']) && intval($_POST['id_transaksi']) > 0) $id = intval($_POST['id_transaksi']);
elseif (isset($_POST['id']) && intval($_POST['id']) > 0) $id = intval($_POST['id']);
elseif (isset($_GET['id_aniv']) && intval($_GET['id_aniv']) > 0) $id = intval($_GET['id_aniv']);
elseif (isset($_GET['id_transaksi']) && intval($_GET['id_transaksi']) > 0) $id = intval($_GET['id_transaksi']);
elseif (isset($_GET['id']) && intval($_GET['id']) > 0) $id = intval($_GET['id']);

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array(
        "status" => false,
        "message" => "ID transaksi anniversary tidak valid atau tidak terdeteksi"
    ));
    exit();
}

try {
    if (method_exists($pdo, 'beginTransaction')) {
        $pdo->beginTransaction();
    }

    $rowCount = 0;

    // Hapus dari riwayat_aniv
    $stmt_del_ra = $pdo->prepare("DELETE FROM riwayat_aniv WHERE id = ?");
    $stmt_del_ra->execute(array($id));
    $rowCount += $stmt_del_ra->rowCount();

    // Hapus dari pembayaran
    $stmt_del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE id = ?");
    $stmt_del_pem->execute(array($id));
    $rowCount += $stmt_del_pem->rowCount();

    // Hapus dari iuran_anniversary
    $stmt_del_ia = $pdo->prepare("DELETE FROM iuran_anniversary WHERE id = ?");
    $stmt_del_ia->execute(array($id));
    $rowCount += $stmt_del_ia->rowCount();

    // VALIDASI SIKLUS DATABASE: Gunakan rowCount > 0
    if ($rowCount > 0) {
        if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
            $pdo->commit();
        }
        http_response_code(200);
        echo json_encode(array(
            "status" => true,
            "message" => "Data berhasil dihapus dari database"
        ));
    } else {
        if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
        echo json_encode(array(
            "status" => false,
            "message" => "Gagal menghapus riwayat anniversary: ID tidak ditemukan atau 0 baris terhapus"
        ));
    }

} catch (Throwable $e) {
    if (isset($pdo) && method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => false,
        "message" => "Gagal menghapus riwayat dari database: " . $e->getMessage()
    ));
}
?>
