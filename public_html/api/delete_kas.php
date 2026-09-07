<?php
// delete_kas.php - Endpoint aman untuk hapus transaksi/riwayat kas
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
$data = json_decode($rawInput, true);

$role = '';
if (isset($data['role'])) $role = trim($data['role']);
elseif (isset($data['user_role'])) $role = trim($data['user_role']);
elseif (isset($_POST['role'])) $role = trim($_POST['role']);
elseif (isset($_POST['user_role'])) $role = trim($_POST['user_role']);
elseif (isset($_GET['role'])) $role = trim($_GET['role']);
elseif (isset($_GET['user_role'])) $role = trim($_GET['user_role']);

if (!empty($role)) {
    $role_upper = strtoupper($role);
    if ($role_upper !== 'ADMIN' && $role_upper !== 'BENDAHARA' && $role_upper !== 'DEVELOPER') {
        http_response_code(403);
        echo json_encode(array("status" => "error", "message" => "Akses ditolak: Hanya ADMIN dan BENDAHARA yang memiliki hak akses."));
        exit();
    }
}

$id = 0;
if (isset($data['id'])) $id = intval($data['id']);
elseif (isset($_POST['id'])) $id = intval($_POST['id']);
elseif (isset($_GET['id'])) $id = intval($_GET['id']);

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array("status" => "error", "message" => "ID tidak valid"));
    exit();
}

try {
    $conn->beginTransaction();

    // Hapus hanya dari riwayat_kas dan pembayaran
    $stmt_rk = $conn->prepare("DELETE FROM riwayat_kas WHERE id = :id");
    $stmt_rk->execute(array(':id' => $id));
    $rowCount = $stmt_rk->rowCount();

    if ($rowCount <= 0) {
        $stmt_p = $conn->prepare("DELETE FROM pembayaran WHERE id = :id");
        $stmt_p->execute(array(':id' => $id));
        $rowCount = $stmt_p->rowCount();
    }

    // Jika dipanggil dengan action reset_member dan id adalah id_anggota
    if (isset($data['action']) && ($data['action'] === 'reset_member' || $data['action'] === 'delete') && $rowCount <= 0) {
        $stmt_rk_m = $conn->prepare("DELETE FROM riwayat_kas WHERE id_anggota = :id");
        $stmt_rk_m->execute(array(':id' => $id));
        $stmt_p_m = $conn->prepare("DELETE FROM pembayaran WHERE anggotaId = :id AND UPPER(jenisPembayaran) = 'KAS'");
        $stmt_p_m->execute(array(':id' => $id));
        $stmt_upd = $conn->prepare("UPDATE anggota SET uang_kas = 0 WHERE id = :id");
        $stmt_upd->execute(array(':id' => $id));
        $rowCount = 1;
    }

    // PENTING: DILARANG KERAS mengurangi atau mengubah angka di tabel saldo_akumulasi (Total Saldo Utama Dashboard)
    $conn->commit();

    http_response_code(200);
    echo json_encode(array(
        "status" => "success",
        "message" => "Data kas berhasil dihapus dari database"
    ));
} catch (Throwable $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal menghapus kas: " . $e->getMessage()
    ));
}
?>
