<?php
// dashboard.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

try {
    // Ensure saldo_akumulasi table exists
    $conn->exec("CREATE TABLE IF NOT EXISTS saldo_akumulasi (
        id INT AUTO_INCREMENT PRIMARY KEY,
        jenis_kas VARCHAR(50) UNIQUE NOT NULL,
        total_akumulasi_masuk DOUBLE DEFAULT 0,
        total_akumulasi_keluar DOUBLE DEFAULT 0,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )");

    // 1. Total Anggota
    $total_anggota = 0;
    $stmt_m = $conn->query("SELECT COUNT(*) as total FROM anggota");
    if ($stmt_m) {
        $row = $stmt_m->fetch(PDO::FETCH_ASSOC);
        $total_anggota = intval($row['total'] ?? 0);
    }

    // 2. KAS UTAMA: Ambil total_pemasukan_kas dari saldo_akumulasi. Jika belum ada atau 0, seed dari riwayat_kas / anggota sekali.
    $total_pemasukan_kas = 0.0;
    $kas_utama_out = 0.0;

    $stmt_master = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_utama'");
    $row_master = $stmt_master ? $stmt_master->fetch(PDO::FETCH_ASSOC) : null;

    if ($row_master) {
        $total_pemasukan_kas = floatval($row_master['total_akumulasi_masuk'] ?? 0.0);
        $kas_utama_out = floatval($row_master['total_akumulasi_keluar'] ?? 0.0);
    }

    if ($total_pemasukan_kas <= 0) {
        // Fallback initial seed from anggota uang_kas or riwayat_kas
        $stmt_fb = $conn->query("SELECT COALESCE(SUM(uang_kas), 0) as total FROM anggota");
        $row_fb = $stmt_fb ? $stmt_fb->fetch(PDO::FETCH_ASSOC) : null;
        $total_pemasukan_kas = floatval($row_fb['total'] ?? 0.0);

        if ($total_pemasukan_kas <= 0) {
            $stmt_fb2 = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_kas");
            $row_fb2 = $stmt_fb2 ? $stmt_fb2->fetch(PDO::FETCH_ASSOC) : null;
            $total_pemasukan_kas = floatval($row_fb2['total'] ?? 0.0);
        }

        $stmt_ins = $conn->prepare("INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) VALUES ('kas_utama', ?, 0) ON DUPLICATE KEY UPDATE total_akumulasi_masuk = GREATEST(total_akumulasi_masuk, ?)");
        $stmt_ins->execute([$total_pemasukan_kas, $total_pemasukan_kas]);
    }

    // Hitung Saldo Kas Saat Ini = master_ledger.total_pemasukan_kas - total_pengeluaran
    $saldo_kas_utama = max(0.0, $total_pemasukan_kas - $kas_utama_out);

    // 3. KAS KELILING
    $kas_keliling_in = 0.0;
    $kas_keliling_out = 0.0;
    $stmt_kk = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_keliling'");
    $row_kk = $stmt_kk ? $stmt_kk->fetch(PDO::FETCH_ASSOC) : null;
    if ($row_kk) {
        $kas_keliling_in = floatval($row_kk['total_akumulasi_masuk'] ?? 0.0);
        $kas_keliling_out = floatval($row_kk['total_akumulasi_keluar'] ?? 0.0);
    }
    if ($kas_keliling_in <= 0) {
        $stmt_kk_fb = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) as total FROM kas_keliling");
        $row_kk_fb = $stmt_kk_fb ? $stmt_kk_fb->fetch(PDO::FETCH_ASSOC) : null;
        $kas_keliling_in = floatval($row_kk_fb['total'] ?? 0.0);
        
        $stmt_ins_kk = $conn->prepare("INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) VALUES ('kas_keliling', ?, 0) ON DUPLICATE KEY UPDATE total_akumulasi_masuk = GREATEST(total_akumulasi_masuk, ?)");
        $stmt_ins_kk->execute([$kas_keliling_in, $kas_keliling_in]);
    }
    $saldo_kas_keliling = max(0.0, $kas_keliling_in - $kas_keliling_out);

    // 4. KAS ANNIVERSARY
    $raw_total_aniv = 0.0;
    $aniv_out = 0.0;
    $stmt_aniv = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_aniv'");
    $row_aniv = $stmt_aniv ? $stmt_aniv->fetch(PDO::FETCH_ASSOC) : null;
    if ($row_aniv) {
        $raw_total_aniv = floatval($row_aniv['total_akumulasi_masuk'] ?? 0.0);
        $aniv_out = floatval($row_aniv['total_akumulasi_keluar'] ?? 0.0);
    }
    if ($raw_total_aniv <= 0) {
        $stmt_aniv_fb = $conn->query("SELECT COALESCE(SUM(iuran_aniv), 0) as total FROM anggota");
        $row_aniv_fb = $stmt_aniv_fb ? $stmt_aniv_fb->fetch(PDO::FETCH_ASSOC) : null;
        $raw_total_aniv = floatval($row_aniv_fb['total'] ?? 0.0);

        $stmt_ins_aniv = $conn->prepare("INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) VALUES ('kas_aniv', ?, 0) ON DUPLICATE KEY UPDATE total_akumulasi_masuk = GREATEST(total_akumulasi_masuk, ?)");
        $stmt_ins_aniv->execute([$raw_total_aniv, $raw_total_aniv]);
    }
    $saldo_kas_aniv = max(0.0, $raw_total_aniv - $aniv_out);

    // 5. Total Pengeluaran All & Sisa Cicilan
    $total_pengeluaran_all = $kas_utama_out + $kas_keliling_out + $aniv_out;
    $total_sisa_cicilan = 0.0;
    $stmt_cicilan = $conn->query("SELECT COALESCE(SUM(sisa_cicilan), 0) as total FROM anggota");
    if ($stmt_cicilan && $row_c = $stmt_cicilan->fetch(PDO::FETCH_ASSOC)) {
        $total_sisa_cicilan = floatval($row_c['total'] ?? 0.0);
    }

    // Belum bayar kas / aniv
    $target_aniv = 0.0;
    $target_kas = 0.0;
    $stmt_set = $conn->query("SELECT target_aniv, target_kas FROM community_settings LIMIT 1");
    if ($stmt_set && $row_set = $stmt_set->fetch(PDO::FETCH_ASSOC)) {
        $target_aniv = floatval($row_set['target_aniv'] ?? 0);
        $target_kas = floatval($row_set['target_kas'] ?? 0);
    }

    $belum_bayar_kas = 0;
    if ($target_kas > 0) {
        $stmt_b = $conn->prepare("SELECT COUNT(*) as belum FROM anggota WHERE uang_kas < ?");
        $stmt_b->execute([$target_kas]);
        $belum_bayar_kas = intval($stmt_b->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    } else {
        $stmt_b = $conn->query("SELECT COUNT(*) as belum FROM anggota WHERE uang_kas = 0");
        $belum_bayar_kas = intval($stmt_b->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    }

    $belum_bayar_aniv = 0;
    if ($target_aniv > 0) {
        $stmt_ba = $conn->prepare("SELECT COUNT(*) as belum FROM anggota WHERE iuran_aniv < ?");
        $stmt_ba->execute([$target_aniv]);
        $belum_bayar_aniv = intval($stmt_ba->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    } else {
        $stmt_ba = $conn->query("SELECT COUNT(*) as belum FROM anggota WHERE iuran_aniv = 0");
        $belum_bayar_aniv = intval($stmt_ba->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
    }

    echo json_encode(array(
        "status" => "success",
        "kas_utama" => array(
            "total_pemasukan" => $total_pemasukan_kas,
            "total_pengeluaran" => $kas_utama_out,
            "saldo_kas" => $saldo_kas_utama,
            "saldo" => $saldo_kas_utama
        ),
        "kas_keliling" => array(
            "total_pemasukan" => $kas_keliling_in,
            "total_pengeluaran" => $kas_keliling_out,
            "saldo_keliling" => $saldo_kas_keliling,
            "saldo" => $saldo_kas_keliling
        ),
        "kas_anniversary" => array(
            "total_pemasukan" => $raw_total_aniv,
            "total_pengeluaran" => $aniv_out,
            "saldo_aniv" => $saldo_kas_aniv,
            "saldo" => $saldo_kas_aniv
        ),
        "data" => array(
            "total_anggota" => $total_anggota,
            "total_kas" => $total_pemasukan_kas,
            "total_aniv" => $raw_total_aniv,
            "total_pengeluaran" => $total_pengeluaran_all,
            "total_sisa_cicilan" => $total_sisa_cicilan,
            "total_saldo" => $saldo_kas_keliling,
            "saldo_kas" => $saldo_kas_utama,
            "belum_bayar_kas" => $belum_bayar_kas,
            "belum_bayar_aniv" => $belum_bayar_aniv,
            "kas_utama" => array(
                "total_pemasukan" => $total_pemasukan_kas,
                "total_pengeluaran" => $kas_utama_out,
                "saldo_kas" => $saldo_kas_utama,
                "saldo" => $saldo_kas_utama
            ),
            "kas_keliling_data" => array(
                "total_pemasukan" => $kas_keliling_in,
                "total_pengeluaran" => $kas_keliling_out,
                "saldo_keliling" => $saldo_kas_keliling,
                "saldo" => $saldo_kas_keliling
            ),
            "kas_anniversary_data" => array(
                "total_pemasukan" => $raw_total_aniv,
                "total_pengeluaran" => $aniv_out,
                "saldo_aniv" => $saldo_kas_aniv,
                "saldo" => $saldo_kas_aniv
            ),
            "kas_keliling" => $saldo_kas_keliling,
            "iuran_anniversary" => $raw_total_aniv,
            "belum_kas" => $belum_bayar_kas,
            "belum_anniversary" => $belum_bayar_aniv
        )
    ));

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Dashboard Error: " . $e->getMessage()
    ));
}
?>
