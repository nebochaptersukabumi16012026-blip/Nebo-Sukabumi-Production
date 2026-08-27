<?php
// input_aniv.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
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

// 1. AUTO-CREATE / FIX TABEL DATABASE (PADA BACKEND PHP) removed, handled in config.php

// 2. PARSE INPUT DATA (JSON & POST Form Support)
$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);

// Ekstraksi Role Pengirim
$role = '';
if (isset($data->role)) {
    $role = trim($data->role);
} elseif (isset($data->user_role)) {
    $role = trim($data->user_role);
} elseif (isset($data->token_role)) {
    $role = trim($data->token_role);
} elseif (isset($_POST['role'])) {
    $role = trim($_POST['role']);
} elseif (isset($_POST['user_role'])) {
    $role = trim($_POST['user_role']);
} elseif (isset($_POST['token_role'])) {
    $role = trim($_POST['token_role']);
}

// 3. PROTEKSI STRICT BACKEND: HANYA ROLE DEVELOPER YANG DIIZINKAN
if (empty($role) || strtolower($role) !== 'developer') {
    http_response_code(403);
    echo json_encode(array(
        "status" => "error",
        "message" => "Akses Ditolak: Hanya akun Developer yang diizinkan menginput data Kas/Aniv!",
        "db_error" => "Unauthorized access: role is '" . htmlspecialchars($role) . "', required 'developer'"
    ));
    exit();
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
} elseif (isset($_POST['anggotaId'])) {
    $id_anggota = intval($_POST['anggotaId']);
}

$nominal = 0.0;
if (isset($data->nominal)) {
    $nominal = floatval($data->nominal);
} elseif (isset($_POST['nominal'])) {
    $nominal = floatval($_POST['nominal']);
}

$keterangan = 'Iuran Anniversary';
if (isset($data->keterangan) && trim($data->keterangan) !== '') {
    $keterangan = trim($data->keterangan);
} elseif (isset($_POST['keterangan']) && trim($_POST['keterangan']) !== '') {
    $keterangan = trim($_POST['keterangan']);
}

$buktiPembayaran = null;
if (isset($data->bukti_pembayaran)) {
    $buktiPembayaran = $data->bukti_pembayaran;
} elseif (isset($data->buktiPembayaran)) {
    $buktiPembayaran = $data->buktiPembayaran;
} elseif (isset($_POST['bukti_pembayaran'])) {
    $buktiPembayaran = $_POST['bukti_pembayaran'];
} elseif (isset($_POST['buktiPembayaran'])) {
    $buktiPembayaran = $_POST['buktiPembayaran'];
}

// 4. VALIDASI INPUT
if ($id_anggota <= 0 || $nominal <= 0) {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal simpan ke database: id_anggota dan nominal wajib diisi dan harus lebih dari 0",
        "db_error" => "Invalid parameters: id_anggota=$id_anggota, nominal=$nominal"
    ));
    exit();
}

try {
    // Pastikan anggota ada di database
    $stmt_mem = $conn->prepare("SELECT id, nama, nra, iuran_aniv FROM anggota WHERE id = ?");
    $stmt_mem->execute(array($id_anggota));
    $anggota = $stmt_mem->fetch(PDO::FETCH_ASSOC);

    if (!$anggota) {
        http_response_code(404);
        echo json_encode(array(
            "status" => "error",
            "message" => "Anggota dengan ID $id_anggota tidak ditemukan",
            "db_error" => "Member not found in anggota table"
        ));
        exit();
    }

    $namaAnggota = isset($anggota['nama']) ? $anggota['nama'] : '';
    $nowTs = time() * 1000;

    // Mulai Database Transaction
    $conn->beginTransaction();

    // A. JALANKAN QUERY INSERT KE TABEL riwayat_aniv
    $stmt_ra = $conn->prepare("INSERT INTO riwayat_aniv (id_anggota, nominal, keterangan, tanggal) VALUES (:id_anggota, :nominal, :keterangan, NOW())");
    $stmt_ra->execute(array(
        ':id_anggota' => $id_anggota,
        ':nominal' => $nominal,
        ':keterangan' => $keterangan
    ));
    $insertedRaId = $conn->lastInsertId();

    // B. Mirroring ke tabel pembayaran agar sinkronisasi multi-tabel 100% konsisten
    try {
        $stmt_pem = $conn->prepare("INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) VALUES (?, ?, 'ANIV', ?, ?, ?, ?)");
        $stmt_pem->execute(array(
            $id_anggota,
            $namaAnggota,
            $nominal,
            $nowTs,
            $keterangan,
            $buktiPembayaran
        ));
    } catch (Exception $e_pem) {}

    // C. Mirroring ke tabel iuran_anniversary
    try {
        $stmt_ia = $conn->prepare("INSERT INTO iuran_anniversary (anggota_id, nominal, keterangan, tanggal) VALUES (?, ?, ?, NOW())");
        $stmt_ia->execute(array(
            $id_anggota,
            $nominal,
            $keterangan
        ));
    } catch (Exception $e_ia) {}

    // D. JALANKAN QUERY UPDATE AKUMULASI KE TABEL anggota (Hanya bertambah / Akumulatif murni)
    $stmt_upd_aniv = $conn->prepare("
        UPDATE anggota 
        SET iuran_aniv = iuran_aniv + :nominal
        WHERE id = :id_anggota
    ");
    $stmt_upd_aniv->execute(array(':id_anggota' => $id_anggota, ':nominal' => $nominal));

    // E. UPDATE / INCREMENT SALDO AKUMULASI MASTER ANIV (Hanya bertambah / Akumulatif murni)
    try {
        $stmt_master_aniv = $conn->prepare("
            INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk) 
            VALUES ('kas_aniv', :nominal) 
            ON DUPLICATE KEY UPDATE total_akumulasi_masuk = total_akumulasi_masuk + :nominal
        ");
        $stmt_master_aniv->execute(array(':nominal' => $nominal));
    } catch (Exception $e_master) {}

    // Ambil nilai iuran_aniv terbaru
    $stmt_check = $conn->prepare("SELECT iuran_aniv FROM anggota WHERE id = ?");
    $stmt_check->execute(array($id_anggota));
    $row_check = $stmt_check->fetch(PDO::FETCH_ASSOC);
    $iuranAnivBaru = floatval(isset($row_check['iuran_aniv']) ? $row_check['iuran_aniv'] : 0);

    // Commit Transaction
    $conn->commit();

    // Hitung Saldo Kas Aniv dari Master Akumulasi
    $stmt_master_check = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_aniv'");
    $row_master_aniv = $stmt_master_check ? $stmt_master_check->fetch(PDO::FETCH_ASSOC) : null;
    $raw_total_aniv = $row_master_aniv ? floatval($row_master_aniv['total_akumulasi_masuk']) : 0.0;
    $aniv_out = $row_master_aniv ? floatval($row_master_aniv['total_akumulasi_keluar']) : 0.0;

    $saldo_kas_aniv = max(0, $raw_total_aniv - $aniv_out);

    // Kirim Response Sukses
    http_response_code(200);
    echo json_encode(array(
        "status" => "success",
        "message" => "Berhasil menyimpan iuran anniversary",
        "id_anggota" => $id_anggota,
        "riwayat_id" => intval($insertedRaId),
        "nominal" => $nominal,
        "iuran_aniv" => $iuranAnivBaru,
        "total_aniv" => $raw_total_aniv,
        "saldo_kas_aniv" => $saldo_kas_aniv
    ));

} catch (PDOException $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal simpan ke database",
        "db_error" => $e->getMessage()
    ));
} catch (Exception $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal simpan ke database",
        "db_error" => $e->getMessage()
    ));
}
?>
