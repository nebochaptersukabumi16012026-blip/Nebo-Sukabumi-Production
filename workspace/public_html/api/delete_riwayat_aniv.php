<?php
// delete_riwayat_aniv.php - Endpoint hapus riwayat anniversary dengan validasi DB, sinkronisasi anggota, dan diagnostic logging
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, DELETE, GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

$debugLogFile = __DIR__ . '/aniv_delete_debug.log';

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

// Log received request
$receivedDataLog = "[" . date('Y-m-d H:i:s') . "] REQ_RAW: " . $rawInput . " | POST: " . json_encode($_POST) . " | GET: " . json_encode($_GET) . "\n";
file_put_contents($debugLogFile, $receivedDataLog, FILE_APPEND);

// 1. OTORISASI: ADMIN, BENDAHARA, DEVELOPER, PENGURUS
$role = '';
if (!empty($data['user_role'])) $role = trim($data['user_role']);
elseif (!empty($data['role'])) $role = trim($data['role']);
elseif (!empty($_POST['user_role'])) $role = trim($_POST['user_role']);
elseif (!empty($_POST['role'])) $role = trim($_POST['role']);
elseif (!empty($_GET['user_role'])) $role = trim($_GET['user_role']);
elseif (!empty($_GET['role'])) $role = trim($_GET['role']);

$role_upper = strtoupper($role);
if (!empty($role) && $role_upper !== 'DEVELOPER' && $role_upper !== 'ADMIN' && $role_upper !== 'BENDAHARA' && $role_upper !== 'PENGURUS') {
    file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] AUTH_FAIL: Role=$role\n", FILE_APPEND);
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

file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] PARSED_ID: $id\n", FILE_APPEND);

if ($id <= 0) {
    file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] ERROR: Invalid ID ($id)\n", FILE_APPEND);
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

    // Ambil semua anggota_id terkait sebelum menghapus untuk sinkronisasi re-kalkulasi
    $anggota_ids_to_update = array();
    $stmt_find = $pdo->prepare("SELECT anggota_id FROM iuran_anniversary WHERE id = ? UNION SELECT id_anggota FROM riwayat_aniv WHERE id = ? UNION SELECT anggotaId FROM pembayaran WHERE id = ?");
    $stmt_find->execute(array($id, $id, $id));
    while ($row_f = $stmt_find->fetch(PDO::FETCH_ASSOC)) {
        $aid = intval($row_f['anggota_id'] ?? $row_f['id_anggota'] ?? $row_f['anggotaId'] ?? 0);
        if ($aid > 0) {
            $anggota_ids_to_update[$aid] = true;
        }
    }

    file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] FOUND_ANGGOTA_IDS: " . json_encode(array_keys($anggota_ids_to_update)) . "\n", FILE_APPEND);

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

    file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] DELETE_ROW_COUNT: $rowCount\n", FILE_APPEND);

    if ($rowCount > 0) {
        // Rekalkulasi iuran aniv anggota agar summary di tabel anggota ikut terupdate jadi 0 / berkurang
        foreach (array_keys($anggota_ids_to_update) as $aid) {
            if (function_exists('recalculateAnggotaAniv')) {
                recalculateAnggotaAniv($pdo, $aid);
            } else {
                $stmt_sum = $pdo->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM iuran_anniversary WHERE anggota_id = ?");
                $stmt_sum->execute(array($aid));
                $sum_row = $stmt_sum->fetch(PDO::FETCH_ASSOC);
                $tot = floatval($sum_row['total'] ?? 0);
                $stmt_upd = $pdo->prepare("UPDATE anggota SET iuran_aniv = ? WHERE id = ?");
                $stmt_upd->execute(array($tot, $aid));
            }
        }

        if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
            $pdo->commit();
        }
        file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] SUCCESS: ID=$id deleted successfully.\n", FILE_APPEND);

        http_response_code(200);
        echo json_encode(array(
            "status" => true,
            "message" => "Data berhasil dihapus dari database"
        ));
    } else {
        if (method_exists($pdo, 'inTransaction') && $pdo->inTransaction()) {
            $pdo->rollBack();
        }
        file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] FAIL: ID=$id not found or 0 rows deleted.\n", FILE_APPEND);

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
    file_put_contents($debugLogFile, "[" . date('Y-m-d H:i:s') . "] EXCEPTION: " . $e->getMessage() . "\n", FILE_APPEND);

    http_response_code(500);
    echo json_encode(array(
        "status" => false,
        "message" => "Gagal menghapus riwayat dari database: " . $e->getMessage()
    ));
}
?>
