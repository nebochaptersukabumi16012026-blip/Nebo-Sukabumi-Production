<?php
// get_dashboard.php - Dashboard Metrics & RBAC User Session (cPanel)
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

try {
    $db = isset($conn) ? $conn : (isset($pdo) ? $pdo : null);
    if (!$db) {
        throw new PDOException("Koneksi database tidak tersedia.");
    }

    // 1. Ambil data User Session (jika ada param role, nra, username, atau user_id)
    $rawInput = file_get_contents("php://input");
    $bodyData = json_decode($rawInput, true) ?? [];

    $userId = $_GET['user_id'] ?? $_GET['id'] ?? $bodyData['user_id'] ?? $bodyData['id'] ?? 0;
    $username = $_GET['username'] ?? $_GET['nra'] ?? $bodyData['username'] ?? $bodyData['nra'] ?? '';
    $userRoleParam = $_GET['role'] ?? $_GET['role_login'] ?? $bodyData['role'] ?? '';

    $userRow = null;
    if (!empty($userId)) {
        $stmt = $db->prepare("SELECT id, username, nama, role, status FROM users WHERE id = :id LIMIT 1");
        $stmt->execute([':id' => $userId]);
        $userRow = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$userRow) {
            $stmtAng = $db->prepare("SELECT id, username, nama, role, status FROM anggota WHERE id = :id LIMIT 1");
            $stmtAng->execute([':id' => $userId]);
            $userRow = $stmtAng->fetch(PDO::FETCH_ASSOC);
        }
    } elseif (!empty($username)) {
        $stmt = $db->prepare("SELECT id, username, nama, role, status FROM users WHERE username = :usr LIMIT 1");
        $stmt->execute([':usr' => $username]);
        $userRow = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$userRow) {
            $stmtAng = $db->prepare("SELECT id, username, nra as username, nama, role, status FROM anggota WHERE nra = :usr OR username = :usr LIMIT 1");
            $stmtAng->execute([':usr' => $username]);
            $userRow = $stmtAng->fetch(PDO::FETCH_ASSOC);
        }
    }

    $idUser = intval($userRow['id'] ?? $userId ?? 0);
    $name = $userRow['nama'] ?? $userRow['username'] ?? "Pengguna";
    $rawRole = !empty($userRoleParam) ? strtoupper(trim($userRoleParam)) : strtoupper(trim($userRow['role'] ?? 'MEMBER'));
    $rawStatus = strtoupper(trim($userRow['status'] ?? 'VERIFIED'));
    $isVerified = ($rawStatus === 'VERIFIED' || $rawStatus === 'ACTIVE' || $rawStatus === 'APPROVED' || $rawStatus === '1' || in_array($rawRole, ['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']));

    // 2. Metrik Keuangan Komunitas (Single Source of Truth)
    // A. Total Anggota
    $total_anggota = 0;
    $stmt_m = $db->query("SELECT COUNT(*) as total FROM anggota");
    if ($stmt_m && $row_m = $stmt_m->fetch(PDO::FETCH_ASSOC)) {
        $total_anggota = intval($row_m['total'] ?? 0);
    }

    // B. Kas Utama
    $total_pemasukan_kas = 0.0;
    $kas_utama_out = 0.0;
    $stmt_master = $db->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_utama'");
    if ($stmt_master && $row_master = $stmt_master->fetch(PDO::FETCH_ASSOC)) {
        $total_pemasukan_kas = floatval($row_master['total_akumulasi_masuk'] ?? 0.0);
        $kas_utama_out = floatval($row_master['total_akumulasi_keluar'] ?? 0.0);
    }
    if ($total_pemasukan_kas <= 0) {
        $stmt_fb = $db->query("SELECT COALESCE(SUM(uang_kas), 0) as total FROM anggota");
        if ($stmt_fb && $row_fb = $stmt_fb->fetch(PDO::FETCH_ASSOC)) {
            $total_pemasukan_kas = floatval($row_fb['total'] ?? 0.0);
        }
    }
    $saldo_kas_utama = max(0.0, $total_pemasukan_kas - $kas_utama_out);

    // C. Kas Keliling
    $kas_keliling_in = 0.0;
    $kas_keliling_out = 0.0;
    $stmt_kk = $db->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_keliling'");
    if ($stmt_kk && $row_kk = $stmt_kk->fetch(PDO::FETCH_ASSOC)) {
        $kas_keliling_in = floatval($row_kk['total_akumulasi_masuk'] ?? 0.0);
        $kas_keliling_out = floatval($row_kk['total_akumulasi_keluar'] ?? 0.0);
    }
    if ($kas_keliling_in <= 0) {
        $stmt_kk_fb = $db->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) as total FROM kas_keliling");
        if ($stmt_kk_fb && $row_kk_fb = $stmt_kk_fb->fetch(PDO::FETCH_ASSOC)) {
            $kas_keliling_in = floatval($row_kk_fb['total'] ?? 0.0);
        }
    }
    $saldo_kas_keliling = max(0.0, $kas_keliling_in - $kas_keliling_out);

    // D. Kas Anniversary
    $raw_total_aniv = 0.0;
    $aniv_out = 0.0;
    $stmt_aniv = $db->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_aniv'");
    if ($stmt_aniv && $row_aniv = $stmt_aniv->fetch(PDO::FETCH_ASSOC)) {
        $raw_total_aniv = floatval($row_aniv['total_akumulasi_masuk'] ?? 0.0);
        $aniv_out = floatval($row_aniv['total_akumulasi_keluar'] ?? 0.0);
    }
    if ($raw_total_aniv <= 0) {
        $stmt_aniv_fb = $db->query("SELECT COALESCE(SUM(iuran_aniv), 0) as total FROM anggota");
        if ($stmt_aniv_fb && $row_aniv_fb = $stmt_aniv_fb->fetch(PDO::FETCH_ASSOC)) {
            $raw_total_aniv = floatval($row_aniv_fb['total'] ?? 0.0);
        }
    }
    $saldo_kas_aniv = max(0.0, $raw_total_aniv - $aniv_out);

    // E. Target settings & belum bayar
    $target_aniv = 0.0;
    $target_kas = 0.0;
    $stmt_set = $db->query("SELECT target_aniv, target_kas FROM community_settings LIMIT 1");
    if ($stmt_set && $row_set = $stmt_set->fetch(PDO::FETCH_ASSOC)) {
        $target_aniv = floatval($row_set['target_aniv'] ?? 0);
        $target_kas = floatval($row_set['target_kas'] ?? 0);
    }

    $belum_bayar_aniv = 0;
    if ($target_aniv > 0) {
        $stmt_ba = $db->prepare("SELECT COUNT(*) as belum FROM anggota WHERE iuran_aniv < ?");
        $stmt_ba->execute([$target_aniv]);
        $belum_bayar_aniv = intval($stmt_ba->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    } else {
        $stmt_ba = $db->query("SELECT COUNT(*) as belum FROM anggota WHERE iuran_aniv = 0");
        $belum_bayar_aniv = intval($stmt_ba->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    }

    $belum_bayar_kas = 0;
    if ($target_kas > 0) {
        $stmt_bk = $db->prepare("SELECT COUNT(*) as belum FROM anggota WHERE uang_kas < ?");
        $stmt_bk->execute([$target_kas]);
        $belum_bayar_kas = intval($stmt_bk->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    } else {
        $stmt_bk = $db->query("SELECT COUNT(*) as belum FROM anggota WHERE uang_kas = 0");
        $belum_bayar_kas = intval($stmt_bk->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    }

    $total_pengeluaran_all = $kas_utama_out + $kas_keliling_out + $aniv_out;

    $responseData = [
        "total_anggota"        => $total_anggota,
        "saldo_kas"            => $saldo_kas_keliling,
        "kas_keliling"         => $saldo_kas_keliling,
        "total_anniversary"    => $raw_total_aniv,
        "total_aniv"           => $raw_total_aniv,
        "iuran_anniversary"    => $raw_total_aniv,
        "iuran_aniv"           => $raw_total_aniv,
        "target_per_anggota"   => $target_aniv,
        "target_aniv"          => $target_aniv,
        "anggota_belum_bayar"  => $belum_bayar_aniv,
        "belum_anniversary"    => $belum_bayar_aniv,
        "belum_bayar_aniv"     => $belum_bayar_aniv,
        "total_kas"            => $total_pemasukan_kas,
        "belum_kas"            => $belum_bayar_kas,
        "belum_bayar_kas"      => $belum_bayar_kas,
        "total_pengeluaran"    => $total_pengeluaran_all,
        "id_user"              => $idUser,
        "name"                 => $name,
        "role"                 => $rawRole,
        "is_verified"          => $isVerified,
        "status_verifikasi"    => $isVerified ? "1" : "0",
        "status"               => $isVerified ? "VERIFIED" : "PENDING"
    ];

    echo json_encode([
        "status"  => "success",
        "success" => true,
        "data"    => $responseData,
        "user"    => [
            "id"                => $idUser,
            "nama"              => $name,
            "role"              => $rawRole,
            "is_verified"       => $isVerified,
            "status_verifikasi" => $isVerified ? "1" : "0",
            "status"            => $isVerified ? "VERIFIED" : "PENDING"
        ]
    ], JSON_UNESCAPED_UNICODE);

} catch (PDOException $e) {
    error_log("Database Error in get_dashboard.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Terjadi kesalahan pada basis data server: " . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
} catch (Throwable $e) {
    error_log("General Error in get_dashboard.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        "status"  => "error",
        "message" => "Terjadi kesalahan sistem internal: " . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}
?>
