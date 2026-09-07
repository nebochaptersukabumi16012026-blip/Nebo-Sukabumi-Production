<?php
// hapus_cicilan.php - Endpoint untuk menghapus data cicilan dengan proteksi RBAC
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'config.php';
require_once 'sync_helper.php';

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput, true);

// Ekstraksi Role Pengguna
$role = '';
if (isset($data['role'])) $role = trim($data['role']);
elseif (isset($data['user_role'])) $role = trim($data['user_role']);
elseif (isset($_POST['role'])) $role = trim($_POST['role']);
elseif (isset($_POST['user_role'])) $role = trim($_POST['user_role']);
elseif (isset($_GET['role'])) $role = trim($_GET['role']);
elseif (isset($_GET['user_role'])) $role = trim($_GET['user_role']);

// Validasi RBAC server-side untuk ANGGOTA dan GUEST
$user_role = strtoupper($role);
if ($user_role === 'ANGGOTA' || $user_role === 'GUEST') {
    http_response_code(403);
    echo json_encode(array(
        'status' => 'error', 
        'message' => 'Akses ditolak: Anggota/Guest tidak diizinkan menghapus data'
    ));
    exit();
}

$id = 0;
if (isset($data['id'])) $id = intval($data['id']);
elseif (isset($_POST['id'])) $id = intval($_POST['id']);
elseif (isset($_GET['id'])) $id = intval($_GET['id']);

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array("status" => "error", "message" => "ID cicilan tidak valid"));
    exit();
}

try {
    $conn->beginTransaction();

    // Dapatkan data anggota_id sebelum dihapus
    $stmt_get = $conn->prepare("SELECT anggota_id, nominal FROM cicilan WHERE id = ?");
    $stmt_get->execute(array($id));
    $row = $stmt_get->fetch(PDO::FETCH_ASSOC);

    if ($row) {
        $anggota_id = $row['anggota_id'];
        
        $stmt_del = $conn->prepare("DELETE FROM cicilan WHERE id = ?");
        $stmt_del->execute(array($id));

        // Hapus juga dari tabel pembayaran jika tercatat
        $stmt_p = $conn->prepare("DELETE FROM pembayaran WHERE id = ? OR (anggotaId = ? AND nominal = ? AND jenisPembayaran = 'CICILAN') LIMIT 1");
        $stmt_p->execute(array($id, $anggota_id, $row['nominal']));

        if (function_exists('recalculateAnggotaCicilan')) {
            recalculateAnggotaCicilan($conn, $anggota_id);
        }

        $conn->commit();
        echo json_encode(array("status" => "success", "message" => "Data cicilan berhasil dihapus"));
    } else {
        // Coba cek di tabel pembayaran jika tersimpan sebagai jenisPembayaran CICILAN
        $stmt_p = $conn->prepare("SELECT anggotaId, nominal FROM pembayaran WHERE id = ? AND jenisPembayaran = 'CICILAN'");
        $stmt_p->execute(array($id));
        $row_p = $stmt_p->fetch(PDO::FETCH_ASSOC);

        if ($row_p) {
            $anggota_id = $row_p['anggotaId'];
            $stmt_del_p = $conn->prepare("DELETE FROM pembayaran WHERE id = ?");
            $stmt_del_p->execute(array($id));

            if (function_exists('recalculateAnggotaCicilan')) {
                recalculateAnggotaCicilan($conn, $anggota_id);
            }

            $conn->commit();
            echo json_encode(array("status" => "success", "message" => "Data pembayaran cicilan berhasil dihapus"));
        } else {
            $conn->rollBack();
            echo json_encode(array("status" => "error", "message" => "Data cicilan tidak ditemukan"));
        }
    }
} catch (Throwable $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array("status" => "error", "message" => "Gagal menghapus cicilan: " . $e->getMessage()));
}
?>
