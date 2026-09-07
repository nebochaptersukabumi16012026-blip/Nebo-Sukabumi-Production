<?php
// hapus_kas_anggota.php - Hapus riwayat kas anggota secara presisi dan permanen
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

$rawInput = file_get_contents("php://input");
$input = json_decode($rawInput, true);

$user_role = '';
if (isset($input['role'])) {
    $user_role = trim($input['role']);
} elseif (isset($input['user_role'])) {
    $user_role = trim($input['user_role']);
} elseif (isset($_POST['role'])) {
    $user_role = trim($_POST['role']);
} elseif (isset($_POST['user_role'])) {
    $user_role = trim($_POST['user_role']);
} elseif (isset($_GET['role'])) {
    $user_role = trim($_GET['role']);
} elseif (isset($_GET['user_role'])) {
    $user_role = trim($_GET['user_role']);
} else {
    $dataObj = json_decode($rawInput);
    if (isset($dataObj->role)) {
        $user_role = trim($dataObj->role);
    } elseif (isset($dataObj->user_role)) {
        $user_role = trim($dataObj->user_role);
    }
}

$user_role_upper = strtoupper($user_role);
if ($user_role_upper !== 'ADMIN' && $user_role_upper !== 'BENDAHARA' && $user_role_upper !== 'DEVELOPER') {
    http_response_code(403);
    echo json_encode(array(
        'status' => 'error',
        'message' => 'Akses ditolak: Hanya ADMIN dan BENDAHARA yang memiliki hak akses.'
    ));
    exit();
}

$id = null;
if (isset($input['id'])) {
    $id = intval($input['id']);
} elseif (isset($_POST['id'])) {
    $id = intval($_POST['id']);
} elseif (isset($_GET['id'])) {
    $id = intval($_GET['id']);
} else {
    $dataObj = json_decode($rawInput);
    if (isset($dataObj->id)) {
        $id = intval($dataObj->id);
    }
}

if ($id === null || $id <= 0) {
    http_response_code(400);
    echo json_encode(array(
        'status' => 'error',
        'message' => 'ID Transaksi tidak terdeteksi'
    ));
    exit();
}

try {
    if (method_exists($pdo, 'beginTransaction')) {
        $pdo->beginTransaction();
    }

    $rowCount = 0;

    // Eksekusi hapus tegas pada riwayat_kas
    $stmt = $pdo->prepare("DELETE FROM riwayat_kas WHERE id = :id");
    $stmt->execute(array(':id' => $id));
    $rowCount = $stmt->rowCount();

    // Jika tidak ditemukan di riwayat_kas, cek tabel pembayaran
    if ($rowCount <= 0) {
        $stmt_p = $pdo->prepare("DELETE FROM pembayaran WHERE id = :id");
        $stmt_p->execute(array(':id' => $id));
        $rowCount = $stmt_p->rowCount();
    }

    if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
        $pdo->commit();
    }

    if ($rowCount > 0) {
        // SYARAT MUTLAK: DILARANG mengurangi atau mengubah Total Saldo Utama Dashboard (tabel saldo_akumulasi)
        http_response_code(200);
        echo json_encode(array(
            'status' => 'success',
            'message' => 'Riwayat berhasil dihapus dari MySQL'
        ));
    } else {
        http_response_code(404);
        echo json_encode(array(
            'status' => 'error',
            'message' => 'Gagal hapus: ID transaksi tidak ditemukan di database'
        ));
    }

} catch (Throwable $e) {
    if (isset($pdo) && method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        'status' => 'error',
        'message' => 'Gagal hapus: ' . $e->getMessage()
    ));
}
?>
