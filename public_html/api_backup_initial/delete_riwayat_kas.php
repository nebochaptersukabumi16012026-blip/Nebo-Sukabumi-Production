<?php
// delete_riwayat_kas.php - Hapus riwayat kas anggota secara presisi dan permanen dengan validasi siklus DB
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
$input = json_decode($rawInput, true) ?: array();

$id = 0;
if (isset($input['id_kas']) && intval($input['id_kas']) > 0) $id = intval($input['id_kas']);
elseif (isset($input['id']) && intval($input['id']) > 0) $id = intval($input['id']);
elseif (isset($_POST['id_kas']) && intval($_POST['id_kas']) > 0) $id = intval($_POST['id_kas']);
elseif (isset($_POST['id']) && intval($_POST['id']) > 0) $id = intval($_POST['id']);
elseif (isset($_GET['id_kas']) && intval($_GET['id_kas']) > 0) $id = intval($_GET['id_kas']);
elseif (isset($_GET['id']) && intval($_GET['id']) > 0) $id = intval($_GET['id']);

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array(
        'status' => false,
        'message' => 'ID Transaksi Kas tidak terdeteksi atau tidak valid'
    ));
    exit();
}

try {
    if (method_exists($pdo, 'beginTransaction')) {
        $pdo->beginTransaction();
    }

    $rowCount = 0;

    $stmt = $pdo->prepare("DELETE FROM riwayat_kas WHERE id = :id");
    $stmt->execute(array(':id' => $id));
    $rowCount += $stmt->rowCount();

    if ($rowCount <= 0) {
        $stmt_p = $pdo->prepare("DELETE FROM pembayaran WHERE id = :id");
        $stmt_p->execute(array(':id' => $id));
        $rowCount += $stmt_p->rowCount();
    }

    // VALIDASI SIKLUS DATABASE
    if ($rowCount > 0) {
        if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
            $pdo->commit();
        }
        http_response_code(200);
        echo json_encode(array(
            'status' => true,
            'message' => 'Data berhasil dihapus dari database'
        ));
    } else {
        if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
        echo json_encode(array(
            'status' => false,
            'message' => 'Gagal hapus: ID transaksi tidak ditemukan atau 0 baris terhapus'
        ));
    }

} catch (Throwable $e) {
    if (isset($pdo) && method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        'status' => false,
        'message' => 'Gagal hapus: ' . $e->getMessage()
    ));
}
?>
