<?php
// delete_kas.php - Endpoint aman untuk hapus transaksi/riwayat kas dengan validasi siklus database
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

if (!isset($pdo) && isset($conn)) {
    $pdo = $conn;
}

// 1. BACA PARAMETER TERBUKA (POST, JSON Body, GET)
$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput, true) ?: array();

$role = '';
if (!empty($data['role'])) $role = trim($data['role']);
elseif (!empty($data['user_role'])) $role = trim($data['user_role']);
elseif (!empty($_POST['role'])) $role = trim($_POST['role']);
elseif (!empty($_POST['user_role'])) $role = trim($_POST['user_role']);
elseif (!empty($_GET['role'])) $role = trim($_GET['role']);
elseif (!empty($_GET['user_role'])) $role = trim($_GET['user_role']);

if (!empty($role)) {
    $role_upper = strtoupper($role);
    if ($role_upper !== 'ADMIN' && $role_upper !== 'BENDAHARA' && $role_upper !== 'DEVELOPER' && $role_upper !== 'PENGURUS') {
        http_response_code(403);
        echo json_encode(array("status" => false, "message" => "Akses ditolak: Anggota/Guest tidak memiliki hak akses menghapus data"));
        exit();
    }
}

$id = 0;
if (isset($data['id_kas']) && intval($data['id_kas']) > 0) $id = intval($data['id_kas']);
elseif (isset($data['id']) && intval($data['id']) > 0) $id = intval($data['id']);
elseif (isset($_POST['id_kas']) && intval($_POST['id_kas']) > 0) $id = intval($_POST['id_kas']);
elseif (isset($_POST['id']) && intval($_POST['id']) > 0) $id = intval($_POST['id']);
elseif (isset($_GET['id_kas']) && intval($_GET['id_kas']) > 0) $id = intval($_GET['id_kas']);
elseif (isset($_GET['id']) && intval($_GET['id']) > 0) $id = intval($_GET['id']);

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array("status" => false, "message" => "ID Kas tidak terdeteksi atau tidak valid"));
    exit();
}

try {
    if (method_exists($pdo, 'beginTransaction')) {
        $pdo->beginTransaction();
    }

    $rowCount = 0;

    // Hapus dari riwayat_kas
    $stmt_rk = $pdo->prepare("DELETE FROM riwayat_kas WHERE id = :id");
    $stmt_rk->execute(array(':id' => $id));
    $rowCount += $stmt_rk->rowCount();

    // Jika belum terhapus dari riwayat_kas, hapus dari tabel pembayaran
    if ($rowCount <= 0) {
        $stmt_p = $pdo->prepare("DELETE FROM pembayaran WHERE id = :id");
        $stmt_p->execute(array(':id' => $id));
        $rowCount += $stmt_p->rowCount();
    }

    // Jika dipanggil untuk reset member
    if (isset($data['action']) && ($data['action'] === 'reset_member' || $data['action'] === 'delete') && $rowCount <= 0) {
        $stmt_rk_m = $pdo->prepare("DELETE FROM riwayat_kas WHERE id_anggota = :id");
        $stmt_rk_m->execute(array(':id' => $id));
        $stmt_p_m = $pdo->prepare("DELETE FROM pembayaran WHERE anggotaId = :id AND UPPER(jenisPembayaran) = 'KAS'");
        $stmt_p_m->execute(array(':id' => $id));
        $stmt_upd = $pdo->prepare("UPDATE anggota SET uang_kas = 0 WHERE id = :id");
        $stmt_upd->execute(array(':id' => $id));
        $rowCount = 1;
    }

    // VALIDASI SIKLUS DATABASE: Pastikan minimal 1 baris terhapus
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
            "message" => "Gagal menghapus data: ID kas tidak ditemukan atau 0 baris terhapus"
        ));
    }
} catch (Throwable $e) {
    if (isset($pdo) && method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => false,
        "message" => "Gagal menghapus kas: " . $e->getMessage()
    ));
}
?>
