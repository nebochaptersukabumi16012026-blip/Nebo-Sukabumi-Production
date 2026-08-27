<?php
// delete_riwayat_aniv.php
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

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);

// 1. OTORISASI KHUSUS DEVELOPER
$role = '';
if (isset($data->user_role)) {
    $role = trim($data->user_role);
} elseif (isset($data->role)) {
    $role = trim($data->role);
} elseif (isset($data->token_role)) {
    $role = trim($data->token_role);
} elseif (isset($_POST['user_role'])) {
    $role = trim($_POST['user_role']);
} elseif (isset($_POST['role'])) {
    $role = trim($_POST['role']);
} elseif (isset($_GET['user_role'])) {
    $role = trim($_GET['user_role']);
} elseif (isset($_GET['role'])) {
    $role = trim($_GET['role']);
}

if (empty($role) || strtolower($role) !== 'developer') {
    http_response_code(403);
    echo json_encode(array(
        "status" => "error",
        "message" => "Akses Ditolak!",
        "db_error" => "Unauthorized: Only developer role can delete riwayat records."
    ));
    exit();
}

// 2. PARSE ID TRANSAKSI & ID ANGGOTA
$raw_id = '';
if (isset($data->id_transaksi)) {
    $raw_id = strval($data->id_transaksi);
} elseif (isset($_GET['id_transaksi'])) {
    $raw_id = strval($_GET['id_transaksi']);
} elseif (isset($_POST['id_transaksi'])) {
    $raw_id = strval($_POST['id_transaksi']);
} elseif (isset($data->id)) {
    $raw_id = strval($data->id);
} elseif (isset($_GET['id'])) {
    $raw_id = strval($_GET['id']);
} elseif (isset($_POST['id'])) {
    $raw_id = strval($_POST['id']);
}

$id = 0;
$extractedMemberId = 0;
if (strpos($raw_id, 'aniv_') === 0) {
    $extractedNum = intval(substr($raw_id, 5));
    $id = $extractedNum;
    $extractedMemberId = $extractedNum;
} else {
    $id = intval($raw_id);
}

$id_anggota = 0;
if (isset($data->id_anggota)) {
    $id_anggota = intval($data->id_anggota);
} elseif (isset($data->anggota_id)) {
    $id_anggota = intval($data->anggota_id);
} elseif (isset($data->anggotaId)) {
    $id_anggota = intval($data->anggotaId);
} elseif (isset($_POST['id_anggota'])) {
    $id_anggota = intval($_POST['id_anggota']);
} elseif (isset($_POST['anggota_id'])) {
    $id_anggota = intval($_POST['anggota_id']);
} elseif (isset($_GET['id_anggota'])) {
    $id_anggota = intval($_GET['id_anggota']);
} elseif (isset($_GET['anggota_id'])) {
    $id_anggota = intval($_GET['anggota_id']);
}

if ($id_anggota <= 0 && $extractedMemberId > 0) {
    $id_anggota = $extractedMemberId;
}

if ($id <= 0 && $id_anggota <= 0) {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "ID transaksi atau ID Anggota tidak valid"
    ));
    exit();
}

try {
    $conn->beginTransaction();

    // Temukan ID Anggota jika belum ada
    if ($id > 0 && $id_anggota <= 0) {
        $stmt_find = $conn->prepare("SELECT id_anggota FROM riwayat_aniv WHERE id = ?");
        $stmt_find->execute(array($id));
        $row_ra = $stmt_find->fetch(PDO::FETCH_ASSOC);
        if ($row_ra) {
            $id_anggota = intval($row_ra['id_anggota']);
        } else {
            $stmt_find_pem = $conn->prepare("SELECT anggotaId FROM pembayaran WHERE id = ?");
            $stmt_find_pem->execute(array($id));
            $row_pem = $stmt_find_pem->fetch(PDO::FETCH_ASSOC);
            if ($row_pem) {
                $id_anggota = intval($row_pem['anggotaId']);
            }
        }
    }

    // A. HAPUS BARIS TRANSAKSI SPESIFIK DARI TABEL riwayat_aniv, pembayaran, dan iuran_anniversary
    if ($id > 0) {
        $stmt_del_ra = $conn->prepare("DELETE FROM riwayat_aniv WHERE id = ?");
        $stmt_del_ra->execute(array($id));

        $stmt_del_pem = $conn->prepare("DELETE FROM pembayaran WHERE id = ?");
        $stmt_del_pem->execute(array($id));

        $stmt_del_ia = $conn->prepare("DELETE FROM iuran_anniversary WHERE id = ?");
        $stmt_del_ia->execute(array($id));
    }

    // B. JIKA ID <= 0 DAN ID ANGGOTA VALID (HAPUS SEMUA RIWAYAT ANIV ANGGOTA TERSEBUT)
    if ($id <= 0 && $id_anggota > 0) {
        $stmt_del_all_ra = $conn->prepare("DELETE FROM riwayat_aniv WHERE id_anggota = ?");
        $stmt_del_all_ra->execute(array($id_anggota));

        $stmt_del_all_ia = $conn->prepare("DELETE FROM iuran_anniversary WHERE anggota_id = ?");
        $stmt_del_all_ia->execute(array($id_anggota));

        $stmt_del_all_pem = $conn->prepare("DELETE FROM pembayaran WHERE anggotaId = ? AND UPPER(jenisPembayaran) = 'ANIV'");
        $stmt_del_all_pem->execute(array($id_anggota));
    }

    // C. AKUMULASI ANGGOTA DAN SALDO UTAMA TETAP UTUH (NON-DECREASING)
    // Nominal iuran_aniv anggota di profil dan saldo akumulasi di dashboard utama tidak dikurangi saat riwayat dihapus.

    $conn->commit();

    // D. KEMBALIKAN RESPONSE JSON SUKSES
    http_response_code(200);
    echo json_encode(array(
        "status" => "success",
        "message" => "Riwayat anggota berhasil dihapus tanpa mengubah saldo utama",
        "deleted_id" => $id,
        "id_anggota" => $id_anggota
    ));

} catch (PDOException $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal menghapus riwayat dari database",
        "db_error" => $e->getMessage()
    ));
} catch (Exception $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal menghapus riwayat dari database",
        "db_error" => $e->getMessage()
    ));
}
?>
