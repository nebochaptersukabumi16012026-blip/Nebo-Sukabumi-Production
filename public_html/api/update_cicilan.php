<?php
// update_cicilan.php - Endpoint untuk mengupdate data cicilan dengan proteksi RBAC
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, PUT, OPTIONS");
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
        'message' => 'Akses ditolak: Anggota/Guest tidak diizinkan mengubah data cicilan'
    ));
    exit();
}

$id = 0;
if (isset($data['id'])) $id = intval($data['id']);
elseif (isset($_POST['id'])) $id = intval($_POST['id']);
elseif (isset($_GET['id'])) $id = intval($_GET['id']);

$nominal = isset($data['nominal']) ? floatval($data['nominal']) : (isset($data['nominal_baru']) ? floatval($data['nominal_baru']) : null);
$tanggal = isset($data['tanggal']) ? $data['tanggal'] : date('Y-m-d');
$keterangan = isset($data['keterangan']) ? $data['keterangan'] : '';

if ($id <= 0 || $nominal === null) {
    http_response_code(400);
    echo json_encode(array("status" => "error", "message" => "Parameter id dan nominal wajib diisi"));
    exit();
}

try {
    $conn->beginTransaction();

    $stmt_get = $conn->prepare("SELECT anggota_id FROM cicilan WHERE id = ?");
    $stmt_get->execute(array($id));
    $row = $stmt_get->fetch(PDO::FETCH_ASSOC);

    if ($row) {
        $anggota_id = $row['anggota_id'];
        $stmt_upd = $conn->prepare("UPDATE cicilan SET nominal = ?, tanggal = ?, keterangan = ? WHERE id = ?");
        $stmt_upd->execute(array($nominal, $tanggal, $keterangan, $id));

        // Update juga di pembayaran jika ada
        $stmt_upd_p = $conn->prepare("UPDATE pembayaran SET nominal = ?, keterangan = ? WHERE id = ?");
        $stmt_upd_p->execute(array($nominal, $keterangan, $id));

        if (function_exists('recalculateAnggotaCicilan')) {
            recalculateAnggotaCicilan($conn, $anggota_id);
        }

        $conn->commit();
        echo json_encode(array("status" => "success", "message" => "Data cicilan berhasil diupdate"));
    } else {
        // Coba cari di pembayaran
        $stmt_p = $conn->prepare("SELECT anggotaId FROM pembayaran WHERE id = ? AND jenisPembayaran = 'CICILAN'");
        $stmt_p->execute(array($id));
        $row_p = $stmt_p->fetch(PDO::FETCH_ASSOC);

        if ($row_p) {
            $anggota_id = $row_p['anggotaId'];
            $stmt_upd_p = $conn->prepare("UPDATE pembayaran SET nominal = ?, keterangan = ? WHERE id = ?");
            $stmt_upd_p->execute(array($nominal, $keterangan, $id));

            if (function_exists('recalculateAnggotaCicilan')) {
                recalculateAnggotaCicilan($conn, $anggota_id);
            }

            $conn->commit();
            echo json_encode(array("status" => "success", "message" => "Data pembayaran cicilan berhasil diupdate"));
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
    echo json_encode(array("status" => "error", "message" => "Gagal update cicilan: " . $e->getMessage()));
}
?>
