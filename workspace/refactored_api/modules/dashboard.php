<?php
// workspace/refactored_api/modules/dashboard.php
// Modul Terpadu Dashboard & Ringkasan Metrik Keuangan Komunitas NEBO Sukabumi

require_once __DIR__ . '/../core/config.php';

$input = getRequestBody();

try {
    $userId        = intval($input['user_id'] ?? ($input['id'] ?? 0));
    $username      = trim($input['username'] ?? '');
    $userRoleParam = trim($input['role'] ?? ($input['user_role'] ?? ''));

    // 1. Data Pengguna
    $idUser     = $userId;
    $name       = "Pengguna";
    $rawRole    = !empty($userRoleParam) ? strtoupper($userRoleParam) : 'MEMBER';
    $isVerified = true;

    if ($userId > 0 || !empty($username)) {
        if ($userId > 0) {
            $stmtU = $pdo->prepare("SELECT id, username, nama, role FROM anggota WHERE id = ? LIMIT 1");
            $stmtU->execute([$userId]);
        } else {
            $stmtU = $pdo->prepare("SELECT id, username, nama, role FROM anggota WHERE username = ? OR nra = ? LIMIT 1");
            $stmtU->execute([$username, $username]);
        }
        $uRow = $stmtU->fetch(PDO::FETCH_ASSOC);
        if ($uRow) {
            $idUser  = intval($uRow['id']);
            $name    = $uRow['nama'] ?? $uRow['username'];
            $rawRole = !empty($userRoleParam) ? strtoupper($userRoleParam) : strtoupper($uRow['role'] ?? 'MEMBER');
        }
    }

    // 2. Total Anggota
    $stmt_ang = $pdo->query("SELECT COUNT(*) as total FROM anggota");
    $row_ang  = $stmt_ang->fetch(PDO::FETCH_ASSOC);
    $total_anggota = intval($row_ang['total'] ?? 0);

    // 3. Kas Utama (Pemasukan, Pengeluaran, Saldo Bersih)
    $pemasukan_kas_utama = 0.0;
    $stmt_rk = $pdo->query("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_kas");
    if ($stmt_rk && $r_rk = $stmt_rk->fetch(PDO::FETCH_ASSOC)) {
        $pemasukan_kas_utama = floatval($r_rk['total'] ?? 0);
    }
    if ($pemasukan_kas_utama <= 0.0) {
        $stmt_ang_k = $pdo->query("SELECT COALESCE(SUM(uang_kas), 0) as total FROM anggota WHERE uang_kas > 0");
        if ($stmt_ang_k && $r_ak = $stmt_ang_k->fetch(PDO::FETCH_ASSOC)) {
            $pemasukan_kas_utama = floatval($r_ak['total'] ?? 0);
        }
    }

    $pengeluaran_kas_utama = 0.0;
    $stmt_out_ku = $pdo->query("
        SELECT COALESCE(SUM(nominal), 0) as total 
        FROM pengeluaran 
        WHERE LOWER(jenis_kas) IN ('kas_utama', 'kas utama', 'kas', 'saldo kas', 'uang kas', 'uang_kas') 
           OR (LOWER(jenis_kas) NOT LIKE '%keliling%' AND LOWER(jenis_kas) NOT LIKE '%aniv%' AND LOWER(jenis_kas) NOT LIKE '%anniversary%')
    ");
    if ($stmt_out_ku && $r_oku = $stmt_out_ku->fetch(PDO::FETCH_ASSOC)) {
        $pengeluaran_kas_utama = floatval($r_oku['total'] ?? 0);
    }
    $saldo_kas_utama = max(0.0, $pemasukan_kas_utama - $pengeluaran_kas_utama);

    // 4. Kas Keliling (Pemasukan, Pengeluaran, Saldo Bersih)
    $stmt_kk = $pdo->query("
        SELECT 
            COALESCE(SUM(CASE 
                WHEN total_pemasukan > 0 THEN total_pemasukan
                WHEN (total_pemasukan = 0 OR total_pemasukan IS NULL) AND (LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pemasukan' OR (COALESCE(jenis, '') = '' AND COALESCE(jenis_transaksi, '') = '')) THEN nominal
                ELSE 0 
            END), 0) AS in_k,
            COALESCE(SUM(CASE 
                WHEN total_pengeluaran > 0 THEN total_pengeluaran
                WHEN (total_pengeluaran = 0 OR total_pengeluaran IS NULL) AND LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pengeluaran' THEN nominal
                ELSE 0 
            END), 0) AS out_k
        FROM kas_keliling
    ");
    $row_kk = $stmt_kk->fetch(PDO::FETCH_ASSOC);
    $pemasukan_kas_keliling   = floatval($row_kk['in_k'] ?? 0);
    $pengeluaran_kas_keliling = floatval($row_kk['out_k'] ?? 0);
    $kas_keliling             = max(0.0, $pemasukan_kas_keliling - $pengeluaran_kas_keliling);

    // 5. Kas Anniversary (Pemasukan, Pengeluaran, Saldo Bersih)
    $pemasukan_aniv = 0.0;
    $stmt_ia = $pdo->query("SELECT COALESCE(SUM(nominal), 0) as total FROM iuran_anniversary");
    if ($stmt_ia && $r_ia = $stmt_ia->fetch(PDO::FETCH_ASSOC)) {
        $pemasukan_aniv = floatval($r_ia['total'] ?? 0);
    }
    if ($pemasukan_aniv <= 0.0) {
        $stmt_aniv_ang = $pdo->query("SELECT COALESCE(SUM(iuran_aniv), 0) as total FROM anggota WHERE iuran_aniv > 0");
        if ($stmt_aniv_ang && $r_aa = $stmt_aniv_ang->fetch(PDO::FETCH_ASSOC)) {
            $pemasukan_aniv = floatval($r_aa['total'] ?? 0);
        }
    }

    $pengeluaran_aniv = 0.0;
    $stmt_out_aniv = $pdo->query("SELECT COALESCE(SUM(nominal), 0) as total FROM pengeluaran WHERE LOWER(jenis_kas) LIKE '%aniv%' OR LOWER(jenis_kas) LIKE '%anniversary%'");
    if ($stmt_out_aniv && $r_oa = $stmt_out_aniv->fetch(PDO::FETCH_ASSOC)) {
        $pengeluaran_aniv = floatval($r_oa['total'] ?? 0);
    }
    $kas_anniversary = max(0.0, $pemasukan_aniv - $pengeluaran_aniv);

    // 6. Cicilan Aktif & Sisa Cicilan
    $saldo_cicilan       = 0.0;
    $total_harga_barang  = 0.0;
    $total_sudah_dibayar = 0.0;
    $anggota_mencicil    = 0;

    $stmt_cic = $pdo->query("
        SELECT 
            COALESCE(SUM(COALESCE(a.harga_barang, a.hargaBarang, 0)), 0) AS total_harga,
            COALESCE(SUM(COALESCE(c.total_bayar, a.total_cicilan, 0)), 0) AS total_bayar,
            COALESCE(SUM(CASE 
                WHEN COALESCE(a.harga_barang, a.hargaBarang, 0) > 0 
                    THEN GREATEST(0, COALESCE(a.harga_barang, a.hargaBarang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0))
                ELSE GREATEST(0, COALESCE(a.sisa_cicilan, a.sisaCicilan, 0))
            END), 0) AS total_sisa,
            COUNT(CASE 
                WHEN (COALESCE(a.harga_barang, a.hargaBarang, 0) > 0 AND (COALESCE(a.harga_barang, a.hargaBarang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0)) > 0)
                  OR (COALESCE(a.harga_barang, a.hargaBarang, 0) = 0 AND COALESCE(a.sisa_cicilan, a.sisaCicilan, 0) > 0)
                THEN 1 
            END) AS count_mencicil
        FROM anggota a
        LEFT JOIN (
            SELECT anggota_id, COALESCE(SUM(nominal), 0) AS total_bayar
            FROM cicilan
            GROUP BY anggota_id
        ) c ON a.id = c.anggota_id
        WHERE COALESCE(a.harga_barang, a.hargaBarang, 0) > 0 OR COALESCE(a.sisa_cicilan, a.sisaCicilan, 0) > 0
    ");
    if ($stmt_cic && $row_cic = $stmt_cic->fetch(PDO::FETCH_ASSOC)) {
        $saldo_cicilan       = floatval($row_cic['total_sisa'] ?? 0);
        $total_harga_barang  = floatval($row_cic['total_harga'] ?? 0);
        $total_sudah_dibayar = floatval($row_cic['total_bayar'] ?? 0);
        $anggota_mencicil    = intval($row_cic['count_mencicil'] ?? 0);
    }

    // 7. Total Keseluruhan & Settings
    $total_saldo = $saldo_kas_utama + $kas_keliling + $kas_anniversary + $saldo_cicilan;

    $target_aniv = 0.0;
    $target_kas  = 0.0;
    $stmt_set = $pdo->query("SELECT target_aniv, target_kas FROM community_settings LIMIT 1");
    if ($stmt_set && $r_set = $stmt_set->fetch(PDO::FETCH_ASSOC)) {
        $target_aniv = floatval($r_set['target_aniv'] ?? 0);
        $target_kas  = floatval($r_set['target_kas'] ?? 0);
    }

    $belum_kas  = 0;
    $belum_aniv = 0;
    $q_bk = $pdo->query("SELECT COUNT(*) as total FROM anggota WHERE COALESCE(uang_kas, uangKas, 0) <= 0");
    if ($q_bk && $d_bk = $q_bk->fetch(PDO::FETCH_ASSOC)) {
        $belum_kas = intval($d_bk['total'] ?? 0);
    }
    $q_ba = $pdo->query("SELECT COUNT(*) as total FROM anggota WHERE COALESCE(iuran_aniv, iuranAniv, 0) <= 0");
    if ($q_ba && $d_ba = $q_ba->fetch(PDO::FETCH_ASSOC)) {
        $belum_aniv = intval($d_ba['total'] ?? 0);
    }

    $total_pengeluaran = $pengeluaran_kas_utama + $pengeluaran_kas_keliling + $pengeluaran_aniv;

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
        "total_pengeluaran"        => $total_pengeluaran,
        "id_user"                  => $idUser,
        "name"                     => $name,
        "role"                     => $rawRole,
        "is_verified"              => $isVerified,
        "status_verifikasi"        => "1",
        "status"                   => "VERIFIED",
        "timestamp"                => date("Y-m-d H:i:s")
    ];

    $response = array_merge([
        "status"  => true,
        "success" => true,
        "message" => "Dashboard synchronized"
    ], $responseData);
    $response["data"] = $responseData;

    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit();

} catch (Throwable $e) {
    error_log("Dashboard error: " . $e->getMessage());
    $fallback = [
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
        "pemasukan_aniv"           => 0.0,
        "pengeluaran_aniv"         => 0.0,
        "saldo_cicilan"            => 0.0,
        "total_sisa_cicilan"       => 0.0,
        "total_harga_barang"       => 0.0,
        "total_sudah_dibayar"      => 0.0,
        "anggota_mencicil"         => 0,
        "total_saldo"              => 0.0,
        "target_per_anggota"       => 0.0,
        "target_aniv"              => 0.0,
        "target_kas"               => 0.0,
        "anggota_belum_bayar"      => 0,
        "belum_anniversary"        => 0,
        "belum_bayar_aniv"         => 0,
        "belum_kas"                => 0,
        "belum_bayar_kas"          => 0,
        "total_pengeluaran"        => 0.0,
        "timestamp"                => date("Y-m-d H:i:s")
    ];
    $res = array_merge([
        "status"  => false,
        "success" => false,
        "message" => "Dashboard fallback: " . $e->getMessage()
    ], $fallback);
    $res["data"] = $fallback;
    echo json_encode($res, JSON_UNESCAPED_UNICODE);
    exit();
}
