<?php
// hapus_riwayat_kas_anggota.php - Perbaikan dan debugging hapus riwayat kas anggota
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

// Pastikan $pdo tersedia (alias dari $conn jika config.php menggunakan $conn)
if (!isset($pdo) && isset($conn)) {
    $pdo = $conn;
}

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput, true); // associative array

$id = null;
if (isset($data['id'])) {
    $id = intval($data['id']);
} elseif (isset($_POST['id'])) {
    $id = intval($_POST['id']);
} elseif (isset($_GET['id'])) {
    $id = intval($_GET['id']);
} else {
    // Try object decode fallback
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

    // 1. Cek dan hapus dari tabel riwayat_kas (kolom id)
    $stmt = $pdo->prepare("DELETE FROM riwayat_kas WHERE id = :id");
    $stmt->execute(array(':id' => $id));
    $rowCount = $stmt->rowCount();

    // 2. Jika tidak ditemukan di riwayat_kas, cek tabel pembayaran (kolom id)
    if ($rowCount <= 0) {
        $stmt_p = $pdo->prepare("DELETE FROM pembayaran WHERE id = :id");
        $stmt_p->execute(array(':id' => $id));
        $rowCount = $stmt_p->rowCount();
    }

    if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
        $pdo->commit();
    }

    if ($rowCount > 0) {
        http_response_code(200);
        echo json_encode(array(
            'status' => 'success',
            'message' => 'Data terhapus dari MySQL'
        ));
    } else {
        http_response_code(404);
        echo json_encode(array(
            'status' => 'error',
            'message' => 'Gagal hapus: ID tidak ditemukan di MySQL'
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
