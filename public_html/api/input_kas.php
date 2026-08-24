<?php
// input_kas.php
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

// 1. AUTO-CREATE / FIX TABEL DATABASE (PADA BACKEND PHP)
try {
    $conn->exec("
        CREATE TABLE IF NOT EXISTS riwayat_kas (
            id INT AUTO_INCREMENT PRIMARY KEY,
            id_anggota INT NOT NULL,
            nominal INT NOT NULL,
            keterangan VARCHAR(255) DEFAULT 'Iuran Kas Anggota',
            tanggal DATETIME DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS pembayaran (
            id INT AUTO_INCREMENT PRIMARY KEY,
            anggotaId INT NOT NULL,
            anggotaNama VARCHAR(255) DEFAULT '',
            jenisPembayaran VARCHAR(50) NOT NULL,
            nominal DOUBLE NOT NULL,
            tanggal BIGINT NOT NULL,
            keterangan TEXT,
            buktiPembayaran LONGTEXT
        );
    ");
} catch (PDOException $e) {
    // Abaikan jika tabel sudah ada
}

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
    echo json_encode([
        "status" => "error",
        "message" => "Akses Ditolak: Hanya akun Developer yang diizinkan menginput data Kas/Aniv!",
        "db_error" => "Unauthorized access: role is '" . htmlspecialchars($role) . "', required 'developer'"
    ]);
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

$keterangan = 'Iuran Kas Anggota';
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
    echo json_encode([
        "status" => "error",
        "message" => "Gagal simpan ke database: id_anggota dan nominal wajib diisi dan harus lebih dari 0",
        "db_error" => "Invalid parameters: id_anggota=$id_anggota, nominal=$nominal"
    ]);
    exit();
}

try {
    // Pastikan anggota ada di database
    $stmt_mem = $conn->prepare("SELECT id, nama, nra, uang_kas FROM anggota WHERE id = ?");
    $stmt_mem->execute([$id_anggota]);
    $anggota = $stmt_mem->fetch(PDO::FETCH_ASSOC);

    if (!$anggota) {
        http_response_code(404);
        echo json_encode([
            "status" => "error",
            "message" => "Anggota dengan ID $id_anggota tidak ditemukan",
            "db_error" => "Member not found in anggota table"
        ]);
        exit();
    }

    $namaAnggota = $anggota['nama'] ?? '';
    $nra = $anggota['nra'] ?? '';
    $nowTs = time() * 1000;

    // Mulai Database Transaction
    $conn->beginTransaction();

    // A. JALANKAN QUERY INSERT KE TABEL riwayat_kas
    $stmt_rk = $conn->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, keterangan, tanggal) VALUES (:id_anggota, :nominal, :keterangan, NOW())");
    $stmt_rk->execute([
        ':id_anggota' => $id_anggota,
        ':nominal' => $nominal,
        ':keterangan' => $keterangan
    ]);
    $insertedRkId = $conn->lastInsertId();

    // B. Mirroring ke tabel pembayaran agar sinkronisasi multi-tabel 100% konsisten
    try {
        $stmt_pem = $conn->prepare("INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) VALUES (?, ?, 'KAS', ?, ?, ?, ?)");
        $stmt_pem->execute([
            $id_anggota,
            $namaAnggota,
            $nominal,
            $nowTs,
            $keterangan,
            $buktiPembayaran
        ]);
    } catch (Exception $e_pem) {}

    // C. Mirroring ke tabel kas_keliling
    try {
        $bulan = date("F");
        $tahun = date("Y");
        $stmt_kk = $conn->prepare("INSERT INTO kas_keliling (nra, nominal, tanggal, keterangan, jenis_transaksi, bulan, tahun, created_by, created_at) VALUES (?, ?, ?, ?, 'Pemasukan', ?, ?, ?, ?)");
        $stmt_kk->execute([
            $nra,
            $nominal,
            $nowTs,
            $keterangan,
            $bulan,
            $tahun,
            $namaAnggota ?: 'Bendahara',
            $nowTs
        ]);
    } catch (Exception $e_kk) {}

    // D. JALANKAN QUERY UPDATE AKUMULASI KE TABEL anggota
    $stmt_upd_kas = $conn->prepare("
        UPDATE anggota 
        SET uang_kas = (SELECT COALESCE(SUM(nominal), 0) FROM riwayat_kas WHERE id_anggota = :id_anggota) 
        WHERE id = :id_anggota
    ");
    $stmt_upd_kas->execute([':id_anggota' => $id_anggota]);

    // Ambil nilai uang_kas terbaru
    $stmt_check = $conn->prepare("SELECT uang_kas FROM anggota WHERE id = ?");
    $stmt_check->execute([$id_anggota]);
    $uangKasBaru = floatval($stmt_check->fetch(PDO::FETCH_ASSOC)['uang_kas'] ?? 0);

    // Commit Transaction
    $conn->commit();

    // Hitung Saldo Kas Utama Komunitas (Prevent Minus)
    $stmt_kas_in = $conn->query("SELECT COALESCE(SUM(uang_kas), 0) AS total_in FROM anggota");
    $kas_utama_in = floatval($stmt_kas_in->fetch(PDO::FETCH_ASSOC)['total_in'] ?? 0);

    $stmt_kas_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_out FROM pengeluaran 
        WHERE (LOWER(COALESCE(jenis_kas, '')) IN ('kas_utama', 'saldo kas', 'kas', 'kas utama', 'uang kas', 'uang_kas') 
           OR (LOWER(COALESCE(jenis_kas, '')) NOT IN ('kas_keliling', 'kas keliling', 'kas_aniv', 'kas aniv', 'kas_anniversary', 'kas anniversary', 'dana cicilan', 'cicilan') AND LOWER(COALESCE(jenis_kas, '')) != ''))");
    $kas_utama_out = floatval($stmt_kas_out->fetch(PDO::FETCH_ASSOC)['total_out'] ?? 0);
    $kas_utama_saldo = max(0, $kas_utama_in - $kas_utama_out);

    // Kirim Response Sukses
    http_response_code(200);
    echo json_encode([
        "status" => "success",
        "message" => "Berhasil menyimpan kas",
        "id_anggota" => $id_anggota,
        "riwayat_id" => intval($insertedRkId),
        "nominal" => $nominal,
        "uang_kas" => $uangKasBaru,
        "total_pemasukan" => $kas_utama_in,
        "total_pengeluaran" => $kas_utama_out,
        "saldo_kas" => $kas_utama_saldo
    ]);

} catch (PDOException $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Gagal simpan ke database",
        "db_error" => $e->getMessage()
    ]);
} catch (Exception $e) {
    if (isset($conn) && $conn->inTransaction()) {
        $conn->rollBack();
    }
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Gagal simpan ke database",
        "db_error" => $e->getMessage()
    ]);
}
?>
