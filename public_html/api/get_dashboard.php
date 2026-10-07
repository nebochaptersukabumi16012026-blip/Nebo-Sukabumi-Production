<?php
// ==============================================================================
// NEBO SUKABUMI - UNIFIED DASHBOARD API (cPanel & Android Unified Endpoint)
// Endpoint: https://nebosukabumi.net/api/get_dashboard.php
// ==============================================================================
error_reporting(0);
ini_set('display_errors', 0);

header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

try {
    $db = isset($conn) && ($conn instanceof PDO) ? $conn : null;
    if (!$db) {
        throw new Exception("Koneksi database PDO tidak tersedia.");
    }

    // Ambil parameter user session jika disediakan oleh client
    $rawInput = file_get_contents("php://input");
    $bodyData = json_decode($rawInput, true) ?? [];
    $userId = $_GET['user_id'] ?? $_GET['id'] ?? $bodyData['user_id'] ?? $bodyData['id'] ?? 0;
    $username = $_GET['username'] ?? $_GET['nra'] ?? $bodyData['username'] ?? $bodyData['nra'] ?? '';
    $userRoleParam = $_GET['role'] ?? $_GET['role_login'] ?? $bodyData['role'] ?? '';

    $userRow = null;
    if (!empty($userId)) {
        $stmtU = $db->prepare("SELECT id, username, nama, role, status FROM users WHERE id = ? LIMIT 1");
        $stmtU->execute([$userId]);
        $userRow = $stmtU->fetch(PDO::FETCH_ASSOC);
        if (!$userRow) {
            $stmtU2 = $db->prepare("SELECT id, username, nama, role, status FROM anggota WHERE id = ? LIMIT 1");
            $stmtU2->execute([$userId]);
            $userRow = $stmtU2->fetch(PDO::FETCH_ASSOC);
        }
    } elseif (!empty($username)) {
        $stmtU = $db->prepare("SELECT id, username, nama, role, status FROM users WHERE username = ? LIMIT 1");
        $stmtU->execute([$username]);
        $userRow = $stmtU->fetch(PDO::FETCH_ASSOC);
        if (!$userRow) {
            $stmtU2 = $db->prepare("SELECT id, username, nra as username, nama, role, status FROM anggota WHERE nra = ? OR username = ? LIMIT 1");
            $stmtU2->execute([$username, $username]);
            $userRow = $stmtU2->fetch(PDO::FETCH_ASSOC);
        }
    }

    $idUser = intval($userRow['id'] ?? $userId ?? 0);
    $name = (string)($userRow['nama'] ?? $userRow['username'] ?? "Pengguna");
    $rawRole = !empty($userRoleParam) ? strtoupper(trim($userRoleParam)) : strtoupper(trim($userRow['role'] ?? 'MEMBER'));
    $rawStatus = strtoupper(trim($userRow['status'] ?? 'VERIFIED'));
    $isVerified = ($rawStatus === 'VERIFIED' || $rawStatus === 'ACTIVE' || $rawStatus === 'APPROVED' || $rawStatus === '1' || in_array($rawRole, ['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']));

    // ==============================================================================
    // 1. TOTAL ANGGOTA
    // ==============================================================================
    $total_anggota = 0;
    $q_ang = $db->query("SELECT COUNT(*) AS total FROM anggota");
    if ($q_ang && $d_ang = $q_ang->fetch(PDO::FETCH_ASSOC)) {
        $total_anggota = intval($d_ang['total'] ?? 0);
    }
    if ($total_anggota <= 0) {
        $q_usr = $db->query("SELECT COUNT(*) AS total FROM users");
        if ($q_usr && $d_usr = $q_usr->fetch(PDO::FETCH_ASSOC)) {
            $total_anggota = intval($d_usr['total'] ?? 0);
        }
    }

    // ==============================================================================
    // 2. KAS UTAMA (Pemasukan, Pengeluaran, Saldo Bersih)
    // ==============================================================================
    $pemasukan_kas_utama = 0.0;
    $pengeluaran_kas_utama = 0.0;

    // Cek tabel 'kas'
    $q_kas_table = $db->query("SELECT IFNULL(SUM(pemasukan), 0) AS total_in, IFNULL(SUM(pengeluaran), 0) AS total_out FROM kas");
    if ($q_kas_table && $d_kas_table = $q_kas_table->fetch(PDO::FETCH_ASSOC)) {
        $pemasukan_kas_utama = floatval($d_kas_table['total_in'] ?? 0.0);
        $pengeluaran_kas_utama = floatval($d_kas_table['total_out'] ?? 0.0);
    }

    // Cek saldo_akumulasi / anggota / riwayat_kas jika pemasukan masih 0
    if ($pemasukan_kas_utama <= 0.0) {
        $stmt_sa = $db->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_utama' LIMIT 1");
        if ($stmt_sa && $row_sa = $stmt_sa->fetch(PDO::FETCH_ASSOC)) {
            $pemasukan_kas_utama = floatval($row_sa['total_akumulasi_masuk'] ?? 0.0);
            $pengeluaran_kas_utama = max($pengeluaran_kas_utama, floatval($row_sa['total_akumulasi_keluar'] ?? 0.0));
        }

        $stmt_ang_kas = $db->query("SELECT IFNULL(SUM(uang_kas), 0) as total FROM anggota");
        if ($stmt_ang_kas && $row_ang_kas = $stmt_ang_kas->fetch(PDO::FETCH_ASSOC)) {
            $pemasukan_kas_utama = max($pemasukan_kas_utama, floatval($row_ang_kas['total'] ?? 0.0));
        }

        $stmt_rk = $db->query("SELECT IFNULL(SUM(nominal), 0) as total FROM riwayat_kas");
        if ($stmt_rk && $row_rk = $stmt_rk->fetch(PDO::FETCH_ASSOC)) {
            $pemasukan_kas_utama = max($pemasukan_kas_utama, floatval($row_rk['total'] ?? 0.0));
        }
    }

    if ($pengeluaran_kas_utama <= 0.0) {
        $stmt_out_ku = $db->query("SELECT IFNULL(SUM(nominal), 0) as total FROM pengeluaran WHERE LOWER(jenis_kas) IN ('kas_utama', 'kas utama', 'kas', 'saldo kas', 'uang kas', 'uang_kas') OR (LOWER(jenis_kas) NOT LIKE '%keliling%' AND LOWER(jenis_kas) NOT LIKE '%aniv%' AND LOWER(jenis_kas) NOT LIKE '%anniversary%')");
        if ($stmt_out_ku && $row_oku = $stmt_out_ku->fetch(PDO::FETCH_ASSOC)) {
            $pengeluaran_kas_utama = floatval($row_oku['total'] ?? 0.0);
        }
    }

    $saldo_kas_utama = max(0.0, $pemasukan_kas_utama - $pengeluaran_kas_utama);

    // ==============================================================================
    // 3. KAS KELILING (Pemasukan, Pengeluaran, Saldo Bersih)
    // ==============================================================================
    $pemasukan_kas_keliling = 0.0;
    $pengeluaran_kas_keliling = 0.0;

    $stmt_kk = $db->query("SELECT 
        IFNULL(SUM(CASE 
            WHEN total_pemasukan > 0 THEN total_pemasukan
            WHEN pemasukan > 0 THEN pemasukan
            WHEN (total_pemasukan = 0 OR total_pemasukan IS NULL) AND LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pemasukan' THEN nominal
            WHEN (COALESCE(jenis, '') = '' AND COALESCE(jenis_transaksi, '') = '') THEN nominal
            ELSE 0 
        END), 0) AS in_k,
        IFNULL(SUM(CASE 
            WHEN total_pengeluaran > 0 THEN total_pengeluaran
            WHEN pengeluaran > 0 THEN pengeluaran
            WHEN (total_pengeluaran = 0 OR total_pengeluaran IS NULL) AND LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pengeluaran' THEN nominal
            ELSE 0 
        END), 0) AS out_k
        FROM kas_keliling");

    if ($stmt_kk && $row_kk = $stmt_kk->fetch(PDO::FETCH_ASSOC)) {
        $pemasukan_kas_keliling = floatval($row_kk['in_k'] ?? 0.0);
        $pengeluaran_kas_keliling = floatval($row_kk['out_k'] ?? 0.0);
    }
    $kas_keliling = max(0.0, $pemasukan_kas_keliling - $pengeluaran_kas_keliling);

    // ==============================================================================
    // 4. KAS ANNIVERSARY (Hitung Murni)
    // ==============================================================================
    $pemasukan_aniv = 0.0;
    $pengeluaran_aniv = 0.0;

    $stmt_ia = $db->query("SELECT IFNULL(SUM(CASE WHEN jumlah_bayar > 0 THEN jumlah_bayar ELSE nominal END), 0) as total FROM iuran_anniversary");
    if ($stmt_ia && $row_ia = $stmt_ia->fetch(PDO::FETCH_ASSOC)) {
        $pemasukan_aniv = floatval($row_ia['total'] ?? 0.0);
    }

    if ($pemasukan_aniv <= 0.0) {
        $stmt_aniv_ang = $db->query("SELECT IFNULL(SUM(iuran_aniv), 0) as total FROM anggota WHERE iuran_aniv > 0");
        if ($stmt_aniv_ang && $row_aniv_ang = $stmt_aniv_ang->fetch(PDO::FETCH_ASSOC)) {
            $pemasukan_aniv = floatval($row_aniv_ang['total'] ?? 0.0);
        }
    }

    $stmt_out_aniv = $db->query("SELECT IFNULL(SUM(nominal), 0) as total FROM pengeluaran WHERE LOWER(jenis_kas) IN ('kas aniv', 'kas anniversary', 'aniv', 'anniversary')");
    if ($stmt_out_aniv && $row_out_aniv = $stmt_out_aniv->fetch(PDO::FETCH_ASSOC)) {
        $pengeluaran_aniv = floatval($row_out_aniv['total'] ?? 0.0);
    }

    $kas_anniversary = max(0.0, $pemasukan_aniv - $pengeluaran_aniv);

    // ==============================================================================
    // 5. SALDO CICILAN / ARISAN
    // ==============================================================================
    $saldo_cicilan = 0.0;
    $total_harga_barang = 0.0;
    $total_sudah_dibayar = 0.0;
    $anggota_mencicil = 0;

    $stmt_cic_ang = $db->query("SELECT 
        COALESCE(SUM(COALESCE(a.harga_barang, 0)), 0) AS total_harga,
        COALESCE(SUM(COALESCE(c.total_bayar, a.total_cicilan, 0)), 0) AS total_bayar,
        COALESCE(SUM(CASE 
            WHEN COALESCE(a.harga_barang, 0) > 0 
                THEN GREATEST(0, COALESCE(a.harga_barang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0)) 
            ELSE GREATEST(0, COALESCE(a.sisa_cicilan, 0)) 
        END), 0) AS total_sisa,
        COUNT(CASE 
            WHEN (COALESCE(a.harga_barang, 0) > 0 AND (COALESCE(a.harga_barang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0)) > 0)
              OR (COALESCE(a.harga_barang, 0) = 0 AND COALESCE(a.sisa_cicilan, 0) > 0)
            THEN 1 
        END) AS count_mencicil
        FROM anggota a
        LEFT JOIN (
            SELECT anggota_id, COALESCE(SUM(nominal), 0) AS total_bayar 
            FROM cicilan 
            GROUP BY anggota_id
        ) c ON a.id = c.anggota_id
        WHERE a.harga_barang > 0 OR a.sisa_cicilan > 0");

    if ($stmt_cic_ang && $row_cic = $stmt_cic_ang->fetch(PDO::FETCH_ASSOC)) {
        $saldo_cicilan = floatval($row_cic['total_sisa'] ?? 0.0);
        $total_harga_barang = floatval($row_cic['total_harga'] ?? 0.0);
        $total_sudah_dibayar = floatval($row_cic['total_bayar'] ?? 0.0);
        $anggota_mencicil = intval($row_cic['count_mencicil'] ?? 0);
    }

    // ==============================================================================
    // 6. TOTAL AKUMULASI KESELURUHAN & TARGET SETTINGS
    // ==============================================================================
    $total_saldo = $saldo_kas_utama + $kas_keliling + $kas_anniversary + $saldo_cicilan;

    $target_aniv = 0.0;
    $target_kas = 0.0;
    $stmt_set = $db->query("SELECT target_aniv, target_kas FROM community_settings LIMIT 1");
    if ($stmt_set && $row_set = $stmt_set->fetch(PDO::FETCH_ASSOC)) {
        $target_aniv = floatval($row_set['target_aniv'] ?? 0.0);
        $target_kas = floatval($row_set['target_kas'] ?? 0.0);
    }

    // Anggota belum bayar
    $belum_kas = 0;
    $belum_aniv = 0;
    $q_blm_kas = $db->query("SELECT COUNT(*) as total FROM anggota WHERE uang_kas <= 0");
    if ($q_blm_kas && $d_bk = $q_blm_kas->fetch(PDO::FETCH_ASSOC)) {
        $belum_kas = intval($d_bk['total'] ?? 0);
    }
    $q_blm_aniv = $db->query("SELECT COUNT(*) as total FROM anggota WHERE iuran_aniv <= 0");
    if ($q_blm_aniv && $d_ba = $q_blm_aniv->fetch(PDO::FETCH_ASSOC)) {
        $belum_aniv = intval($d_ba['total'] ?? 0);
    }

    // ==============================================================================
    // 7. PAYLOAD LENGKAP & KOMPATIBEL (ROOT & DATA OBJECT)
    // ==============================================================================
    $responseData = [
        "total_anggota"            => $total_anggota,
        "saldo_kas_utama"          => $saldo_kas_utama,
        "saldo_kas"                => $saldo_kas_utama,
        "pemasukan_kas_utama"      => $pemasukan_kas_utama,
        "pengeluaran_kas_utama"    => $pengeluaran_kas_utama,
        "kas_keliling"             => $kas_keliling,
        "saldo_kas_keliling"       => $kas_keliling,
        "pemasukan_kas_keliling"   => $pemasukan_kas_keliling,
        "pengeluaran_kas_keliling" => $pengeluaran_kas_keliling,
        "pemasukan_kas"            => $pemasukan_kas_keliling,
        "pengeluaran_kas"          => $pengeluaran_kas_keliling,
        "kas_anniversary"          => $kas_anniversary,
        "total_anniversary"        => $kas_anniversary,
        "pemasukan_aniv"           => $pemasukan_aniv,
        "pengeluaran_aniv"         => $pengeluaran_aniv,
        "saldo_cicilan"            => $saldo_cicilan,
        "total_sisa_cicilan"       => $saldo_cicilan,
        "total_harga_barang"       => $total_harga_barang,
        "total_sudah_dibayar"      => $total_sudah_dibayar,
        "anggota_mencicil"         => $anggota_mencicil,
        "total_saldo"              => $total_saldo,
        "target_per_anggota"       => $target_aniv,
        "target_aniv"              => $target_aniv,
        "target_kas"               => $target_kas,
        "anggota_belum_bayar"      => $belum_aniv,
        "belum_anniversary"        => $belum_aniv,
        "belum_bayar_aniv"         => $belum_aniv,
        "belum_kas"                => $belum_kas,
        "belum_bayar_kas"          => $belum_kas,
        "total_pengeluaran"        => $pengeluaran_kas_utama + $pengeluaran_kas_keliling + $pengeluaran_aniv,
        "id_user"                  => $idUser,
        "name"                     => $name,
        "role"                     => $rawRole,
        "is_verified"              => $isVerified,
        "status_verifikasi"        => $isVerified ? "1" : "0",
        "status"                   => $isVerified ? "VERIFIED" : "PENDING",
        "timestamp"                => date("Y-m-d H:i:s")
    ];

    $response = array_merge([
        "status"  => "success",
        "success" => true,
        "message" => "Dashboard metrics synchronized successfully"
    ], $responseData);

    $response["data"] = $responseData;
    $response["user"] = [
        "id"                => $idUser,
        "nama"              => $name,
        "role"              => $rawRole,
        "is_verified"       => $isVerified,
        "status_verifikasi" => $isVerified ? "1" : "0",
        "status"            => $isVerified ? "VERIFIED" : "PENDING"
    ];

    echo json_encode($response, JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    error_log("Error in get_dashboard.php: " . $e->getMessage());
    http_response_code(200); // Return 200 with safe JSON numbers to prevent APK crash
    $fallbackPayload = [
        "status"                   => "error",
        "success"                  => false,
        "message"                  => "Server fallback: " . $e->getMessage(),
        "total_anggota"            => 0,
        "saldo_kas_utama"          => 0.0,
        "saldo_kas"                => 0.0,
        "pemasukan_kas_utama"      => 0.0,
        "pengeluaran_kas_utama"    => 0.0,
        "kas_keliling"             => 0.0,
        "saldo_kas_keliling"       => 0.0,
        "pemasukan_kas_keliling"   => 0.0,
        "pengeluaran_kas_keliling" => 0.0,
        "pemasukan_kas"            => 0.0,
        "pengeluaran_kas"          => 0.0,
        "kas_anniversary"          => 0.0,
        "total_anniversary"        => 0.0,
        "saldo_cicilan"            => 0.0,
        "total_sisa_cicilan"       => 0.0,
        "total_saldo"              => 0.0,
        "target_per_anggota"       => 0.0,
        "anggota_belum_bayar"      => 0,
        "timestamp"                => date("Y-m-d H:i:s")
    ];
    $fallbackPayload["data"] = $fallbackPayload;
    echo json_encode($fallbackPayload, JSON_UNESCAPED_UNICODE);
}
?>
