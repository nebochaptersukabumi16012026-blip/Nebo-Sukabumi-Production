<?php
// hapus_cicilan.php - Endpoint aman untuk menghapus data cicilan dengan proteksi RBAC dan validasi siklus DB
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

require_once 'config.php';
require_once 'sync_helper.php';

if (!isset($pdo) && isset($conn)) {
    $pdo = $conn;
}

// 1. BACA PARAMETER TERBUKA (POST, JSON Body, GET)
$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput, true) ?: array();

// Ekstraksi Role Pengguna
$role = '';
if (!empty($data['role'])) $role = trim($data['role']);
elseif (!empty($data['user_role'])) $role = trim($data['user_role']);
elseif (!empty($_POST['role'])) $role = trim($_POST['role']);
elseif (!empty($_POST['user_role'])) $role = trim($_POST['user_role']);
elseif (!empty($_GET['role'])) $role = trim($_GET['role']);
elseif (!empty($_GET['user_role'])) $role = trim($_GET['user_role']);

$user_role = strtoupper($role);
if ($user_role === 'ANGGOTA' || $user_role === 'GUEST') {
    http_response_code(403);
    echo json_encode(array(
        "status" => false, 
        "message" => "Akses ditolak: Anggota/Guest tidak diizinkan menghapus data cicilan"
    ));
    exit();
}

$id = 0;
if (isset($data['id_cicilan']) && intval($data['id_cicilan']) > 0) $id = intval($data['id_cicilan']);
elseif (isset($data['id']) && intval($data['id']) > 0) $id = intval($data['id']);
elseif (isset($_POST['id_cicilan']) && intval($_POST['id_cicilan']) > 0) $id = intval($_POST['id_cicilan']);
elseif (isset($_POST['id']) && intval($_POST['id']) > 0) $id = intval($_POST['id']);
elseif (isset($_GET['id_cicilan']) && intval($_GET['id_cicilan']) > 0) $id = intval($_GET['id_cicilan']);
elseif (isset($_GET['id']) && intval($_GET['id']) > 0) $id = intval($_GET['id']);

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array("status" => false, "message" => "ID cicilan tidak valid atau tidak terdeteksi"));
    exit();
}

try {
    if (method_exists($pdo, 'beginTransaction')) {
        $pdo->beginTransaction();
    }

    $rowCount = 0;

    // Dapatkan data anggota_id sebelum dihapus
    $stmt_get = $pdo->prepare("SELECT anggota_id, nominal FROM cicilan WHERE id = ?");
    $stmt_get->execute(array($id));
    $row = $stmt_get->fetch(PDO::FETCH_ASSOC);

    if ($row) {
        $anggota_id = $row['anggota_id'];
        
        $stmt_del = $pdo->prepare("DELETE FROM cicilan WHERE id = ?");
        $stmt_del->execute(array($id));
        $rowCount += $stmt_del->rowCount();

        // Hapus juga dari tabel pembayaran jika tercatat
        $stmt_p = $pdo->prepare("DELETE FROM pembayaran WHERE id = ? OR (anggotaId = ? AND nominal = ? AND UPPER(jenisPembayaran) = 'CICILAN')");
        $stmt_p->execute(array($id, $anggota_id, $row['nominal']));

        if (function_exists('recalculateAnggotaCicilan')) {
            recalculateAnggotaCicilan($pdo, $anggota_id);
        }
    } else {
        // Coba cek di tabel pembayaran jika tersimpan sebagai jenisPembayaran CICILAN
        $stmt_p = $pdo->prepare("SELECT anggotaId, nominal FROM pembayaran WHERE id = ? AND UPPER(jenisPembayaran) = 'CICILAN'");
        $stmt_p->execute(array($id));
        $row_p = $stmt_p->fetch(PDO::FETCH_ASSOC);

        if ($row_p) {
            $anggota_id = $row_p['anggotaId'];
            $stmt_del_p = $pdo->prepare("DELETE FROM pembayaran WHERE id = ?");
            $stmt_del_p->execute(array($id));
            $rowCount += $stmt_del_p->rowCount();

            if (function_exists('recalculateAnggotaCicilan')) {
                recalculateAnggotaCicilan($pdo, $anggota_id);
            }
        }
    }

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
            "message" => "Gagal menghapus cicilan: ID tidak ditemukan atau 0 baris terhapus"
        ));
    }
} catch (Throwable $e) {
    if (isset($pdo) && method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array("status" => false, "message" => "Gagal menghapus cicilan: " . $e->getMessage()));
}
?>
