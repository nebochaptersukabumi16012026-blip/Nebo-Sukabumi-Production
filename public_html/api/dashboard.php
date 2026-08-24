<?php
// dashboard.php & get_dashboard.php
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

$method = $_SERVER['REQUEST_METHOD'];

if ($method == 'GET') {
    try {
        // 1. Total Anggota
        $stmt_total_m = $conn->query("SELECT COUNT(*) as total FROM anggota");
        $total_anggota = intval($stmt_total_m->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);
        
        // 2. KAS UTAMA
        // TOTAL KAS UTAMA = SUM(uang_kas) dari tabel 'anggota' - SUM(nominal) dari tabel 'pengeluaran' kategori 'kas_utama'
        $stmt_kas_in = $conn->query("SELECT COALESCE(SUM(uang_kas), 0) AS total_in FROM anggota");
        $total_pemasukan_kas = floatval($stmt_kas_in->fetch(PDO::FETCH_ASSOC)['total_in'] ?? 0);

        $stmt_kas_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_out FROM pengeluaran 
            WHERE LOWER(jenis_kas) IN ('kas_utama', 'saldo kas', 'kas', 'kas utama', 'uang kas', 'uang_kas', '') 
            OR (LOWER(jenis_kas) NOT IN ('kas_keliling', 'kas keliling', 'kas_aniv', 'kas aniv', 'kas_anniversary', 'kas anniversary', 'dana cicilan', 'cicilan'))");
        $kas_utama_out = floatval($stmt_kas_out->fetch(PDO::FETCH_ASSOC)['total_out'] ?? 0);
        $saldo_kas_utama = max(0, $total_pemasukan_kas - $kas_utama_out);

        // 3. KAS KELILING
        // TOTAL KAS KELILING = SUM(nominal / total_pemasukan) dari tabel 'kas_keliling' - SUM(nominal) dari tabel 'pengeluaran' kategori 'kas_keliling'
        $stmt_kk = $conn->query("SELECT 
            COALESCE(SUM(CASE WHEN total_pemasukan > 0 THEN total_pemasukan WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE 0 END), 0) AS total_in,
            COALESCE(SUM(CASE WHEN total_pengeluaran > 0 THEN total_pengeluaran WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE 0 END), 0) AS total_out_kk
            FROM kas_keliling");
        $row_kk = $stmt_kk->fetch(PDO::FETCH_ASSOC);
        $kas_keliling_in = floatval($row_kk['total_in'] ?? 0);
        $kas_keliling_out_table = floatval($row_kk['total_out_kk'] ?? 0);

        $stmt_kk_pengeluaran = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_out FROM pengeluaran 
            WHERE LOWER(jenis_kas) IN ('kas_keliling', 'kas keliling')");
        $kas_keliling_out_pengeluaran = floatval($stmt_kk_pengeluaran->fetch(PDO::FETCH_ASSOC)['total_out'] ?? 0);
        $kas_keliling_out = $kas_keliling_out_pengeluaran > 0 ? $kas_keliling_out_pengeluaran : $kas_keliling_out_table;
        $saldo_kas_keliling = max(0, $kas_keliling_in - $kas_keliling_out);

        // 4. KAS ANNIVERSARY
        // TOTAL KAS ANIV = SUM(iuran_aniv) dari tabel 'anggota' - SUM(nominal) dari tabel 'pengeluaran' kategori 'kas_anniversary'
        $stmt_aniv_in = $conn->query("SELECT COALESCE(SUM(iuran_aniv), 0) as total FROM anggota");
        $raw_total_aniv = floatval($stmt_aniv_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

        $stmt_aniv_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_out FROM pengeluaran 
            WHERE LOWER(jenis_kas) IN ('kas_anniversary', 'kas anniversary', 'kas_aniv', 'kas aniv')");
        $aniv_out = floatval($stmt_aniv_out->fetch(PDO::FETCH_ASSOC)['total_out'] ?? 0);
        $saldo_kas_aniv = max(0, $raw_total_aniv - $aniv_out);

        // 5. Total Pengeluaran All
        $stmt_pengeluaran_all = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_pengeluaran FROM pengeluaran");
        $total_pengeluaran_all = floatval($stmt_pengeluaran_all->fetch(PDO::FETCH_ASSOC)['total_pengeluaran'] ?? 0);

        // 6. Cicilan
        $stmt_sisa_cicilan = $conn->query("SELECT COALESCE(SUM(sisa_cicilan), 0) as total FROM anggota");
        $total_sisa_cicilan = floatval($stmt_sisa_cicilan->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

        // Kas Keliling Bulan Berjalan
        function getIndonesianMonth($monthNum) {
            $months = [
                1 => "Januari", 2 => "Februari", 3 => "Maret", 4 => "April",
                5 => "Mei", 6 => "Juni", 7 => "Juli", 8 => "Agustus",
                9 => "September", 10 => "Oktober", 11 => "November", 12 => "Desember"
            ];
            return $months[intval($monthNum)];
        }
        $currentMonthYearStr = getIndonesianMonth(date("n")) . " " . date("Y");
        $stmt_kk_month = $conn->prepare("SELECT COALESCE(SUM(total_pemasukan), 0) as total FROM kas_keliling WHERE bulan = ?");
        $stmt_kk_month->execute([$currentMonthYearStr]);
        $kas_keliling_bulan_ini = floatval($stmt_kk_month->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

        // Belum bayar kas / aniv
        $target_aniv = 0;
        $target_kas = 0;
        $stmt_set = $conn->query("SELECT target_aniv, target_kas FROM community_settings LIMIT 1");
        if($row_set = $stmt_set->fetch(PDO::FETCH_ASSOC)) {
            $target_aniv = floatval($row_set['target_aniv']);
            $target_kas = floatval($row_set['target_kas']);
        }
        
        $belum_bayar_kas = 0;
        if ($target_kas > 0) {
            $stmt = $conn->query("SELECT COUNT(*) as belum FROM anggota WHERE uang_kas < $target_kas");
            $belum_bayar_kas = intval($stmt->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
        } else {
            $stmt = $conn->query("SELECT COUNT(*) as belum FROM anggota WHERE uang_kas = 0");
            $belum_bayar_kas = intval($stmt->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
        }
        
        $belum_bayar_aniv = 0;
        if ($target_aniv > 0) {
            $stmt = $conn->query("SELECT COUNT(*) as belum FROM anggota WHERE iuran_aniv < $target_aniv");
            $belum_bayar_aniv = intval($stmt->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
        } else {
            $stmt = $conn->query("SELECT COUNT(*) as belum FROM anggota WHERE iuran_aniv = 0");
            $belum_bayar_aniv = intval($stmt->fetch(PDO::FETCH_ASSOC)['belum'] ?? 0);
        }

        echo json_encode([
            "status" => "success",
            "kas_utama" => [
                "total_pemasukan" => $total_pemasukan_kas,
                "total_pengeluaran" => $kas_utama_out,
                "saldo_kas" => $saldo_kas_utama,
                "saldo" => $saldo_kas_utama
            ],
            "kas_keliling" => [
                "total_pemasukan" => $kas_keliling_in,
                "total_pengeluaran" => $kas_keliling_out,
                "saldo_keliling" => $saldo_kas_keliling,
                "saldo" => $saldo_kas_keliling
            ],
            "kas_anniversary" => [
                "total_pemasukan" => $raw_total_aniv,
                "total_pengeluaran" => $aniv_out,
                "saldo_aniv" => $saldo_kas_aniv,
                "saldo" => $saldo_kas_aniv
            ],
            "data" => [
                "total_anggota" => $total_anggota,
                "total_kas" => $total_pemasukan_kas, 
                "total_aniv" => $raw_total_aniv, 
                "total_pengeluaran" => $total_pengeluaran_all,
                "total_sisa_cicilan" => $total_sisa_cicilan,
                "total_saldo" => $saldo_kas_keliling, 
                "kas_keliling_bulan_ini" => $kas_keliling_bulan_ini, 
                "saldo_kas" => $saldo_kas_utama,
                "belum_bayar_kas" => $belum_bayar_kas,
                "belum_bayar_aniv" => $belum_bayar_aniv,
                
                // Detailed separated objects
                "kas_utama" => [
                    "total_pemasukan" => $total_pemasukan_kas,
                    "total_pengeluaran" => $kas_utama_out,
                    "saldo_kas" => $saldo_kas_utama,
                    "saldo" => $saldo_kas_utama
                ],
                "kas_keliling_data" => [
                    "total_pemasukan" => $kas_keliling_in,
                    "total_pengeluaran" => $kas_keliling_out,
                    "saldo_keliling" => $saldo_kas_keliling,
                    "saldo" => $saldo_kas_keliling
                ],
                "kas_anniversary_data" => [
                    "total_pemasukan" => $raw_total_aniv,
                    "total_pengeluaran" => $aniv_out,
                    "saldo_aniv" => $saldo_kas_aniv,
                    "saldo" => $saldo_kas_aniv
                ],
                "kas_keliling" => $saldo_kas_keliling,
                "iuran_anniversary" => $raw_total_aniv,
                "belum_kas" => $belum_bayar_kas,
                "belum_anniversary" => $belum_bayar_aniv
            ]
        ]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
    }
} else {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
}
?>
