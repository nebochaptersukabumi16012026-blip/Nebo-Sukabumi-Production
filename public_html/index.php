<?php
// ==============================================================================
// 1. KONEKSI DATABASE & KONFIGURASI HEADER
// ==============================================================================
error_reporting(0);
ini_set('display_errors', 0);

// Header Keamanan & Anti-Cache (Supaya data di APK & Web selalu Real-Time)
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Sertakan file koneksi database cPanel
if (file_exists('api/config.php')) {
    include_once 'api/config.php';
} elseif (file_exists('config.php')) {
    include_once 'config.php';
} elseif (file_exists('koneksi.php')) {
    include_once 'koneksi.php';
} elseif (file_exists('../koneksi.php')) {
    include_once '../koneksi.php';
} else {
    // Fallback jika file koneksi.php belum ada
    $host = "localhost";
    $user = "root"; // Sesuaikan dengan DB User cPanel
    $pass = "";     // Sesuaikan dengan DB Password cPanel
    $db   = "nebosuka_db"; // Sesuaikan dengan Nama DB cPanel
    $koneksi = @mysqli_connect($host, $user, $pass, $db);
}

// ==============================================================================
// 2. QUERY REKAPITULASI DATA SINKRON (TANPA BENTROK)
// ==============================================================================
$kas_utama       = 0.0;
$kas_keliling    = 0.0;
$pemasukan_kel   = 0.0;
$pengeluaran_kel = 0.0;
$kas_anniversary = 0.0;
$saldo_cicilan   = 0.0;
$total_anggota   = 0;

if (isset($conn) && $conn instanceof PDO) {
    // Menggunakan PDO dari config.php
    try {
        // A. Total Anggota
        $q0 = $conn->query("SELECT COUNT(*) AS total FROM anggota");
        if ($q0 && $d0 = $q0->fetch(PDO::FETCH_ASSOC)) {
            $total_anggota = (int)($d0['total'] ?? 0);
        }

        // B. Kas Utama
        $stmt_ku = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas = 'kas_utama' LIMIT 1");
        $row_ku = $stmt_ku ? $stmt_ku->fetch(PDO::FETCH_ASSOC) : null;
        if ($row_ku) {
            $in_ku = floatval($row_ku['total_akumulasi_masuk'] ?? 0);
            $out_ku = floatval($row_ku['total_akumulasi_keluar'] ?? 0);
            $kas_utama = max(0.0, $in_ku - $out_ku);
        } else {
            $q1 = $conn->query("SELECT IFNULL(SUM(uang_kas),0) AS total FROM anggota");
            if ($q1 && $d1 = $q1->fetch(PDO::FETCH_ASSOC)) {
                $kas_utama = (float)$d1['total'];
            }
        }

        // C. Kas Keliling
        $q2 = $conn->query("SELECT 
            COALESCE(SUM(CASE WHEN total_pemasukan > 0 THEN total_pemasukan WHEN (total_pemasukan = 0 OR total_pemasukan IS NULL) AND LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pemasukan' THEN nominal ELSE 0 END), 0) AS masukan,
            COALESCE(SUM(CASE WHEN total_pengeluaran > 0 THEN total_pengeluaran WHEN (total_pengeluaran = 0 OR total_pengeluaran IS NULL) AND LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pengeluaran' THEN nominal ELSE 0 END), 0) AS keluaran 
            FROM kas_keliling");
        if ($q2 && $d2 = $q2->fetch(PDO::FETCH_ASSOC)) {
            $pemasukan_kel   = (float)$d2['masukan'];
            $pengeluaran_kel = (float)$d2['keluaran'];
            $kas_keliling    = max(0.0, $pemasukan_kel - $pengeluaran_kel);
        }

        // D. Kas Anniversary
        $q3 = $conn->query("SELECT IFNULL(SUM(nominal),0) AS total FROM iuran_anniversary");
        if ($q3 && $d3 = $q3->fetch(PDO::FETCH_ASSOC)) {
            $kas_anniversary = (float)$d3['total'];
        }
        if ($kas_anniversary == 0.0) {
            $q3b = $conn->query("SELECT IFNULL(SUM(iuran_aniv),0) AS total FROM anggota");
            if ($q3b && $d3b = $q3b->fetch(PDO::FETCH_ASSOC)) {
                $kas_anniversary = (float)$d3b['total'];
            }
        }

        // E. Saldo Cicilan
        $q4 = $conn->query("SELECT IFNULL(SUM(sisa_cicilan),0) AS total FROM anggota WHERE sisa_cicilan > 0");
        if ($q4 && $d4 = $q4->fetch(PDO::FETCH_ASSOC)) {
            $saldo_cicilan = (float)$d4['total'];
        }
    } catch (Exception $e) {}
} elseif (isset($koneksi) && $koneksi) {
    // Menggunakan MySQLi
    $q0 = mysqli_query($koneksi, "SELECT COUNT(*) AS total FROM users");
    if ($q0 && $d0 = mysqli_fetch_assoc($q0)) {
        $total_anggota = (int)($d0['total'] ?? 0);
    }

    $q1 = mysqli_query($koneksi, "SELECT IFNULL(SUM(pemasukan),0) - IFNULL(SUM(pengeluaran),0) AS total FROM kas");
    if ($q1 && $d1 = mysqli_fetch_assoc($q1)) {
        $kas_utama = (float)$d1['total'];
    }

    $q2 = mysqli_query($koneksi, "SELECT IFNULL(SUM(pemasukan),0) AS masukan, IFNULL(SUM(pengeluaran),0) AS keluaran FROM kas_keliling");
    if ($q2 && $d2 = mysqli_fetch_assoc($q2)) {
        $pemasukan_kel   = (float)$d2['masukan'];
        $pengeluaran_kel = (float)$d2['keluaran'];
        $kas_keliling    = max(0.0, $pemasukan_kel - $pengeluaran_kel);
    }

    $q3 = mysqli_query($koneksi, "SELECT IFNULL(SUM(jumlah_bayar),0) AS total FROM iuran_anniversary");
    if ($q3 && $d3 = mysqli_fetch_assoc($q3)) {
        $kas_anniversary = (float)$d3['total'];
    }

    $q4 = mysqli_query($koneksi, "SELECT IFNULL(SUM(jumlah_bayar),0) AS total FROM cicilan");
    if ($q4 && $d4 = mysqli_fetch_assoc($q4)) {
        $saldo_cicilan = (float)$d4['total'];
    }
}

// Total Akumulasi Keseluruhan
$total_saldo = $kas_utama + $kas_keliling + $kas_anniversary + $saldo_cicilan;

// Data Array Tunggal
$response_data = array(
    "status"                   => "success",
    "success"                  => true,
    "message"                  => "Data synchronized successfully",
    "total_anggota"            => $total_anggota,
    "saldo_kas_utama"          => $kas_utama,
    "saldo_kas"                => $kas_utama,
    "kas_keliling"             => $kas_keliling,
    "saldo_kas_keliling"       => $kas_keliling,
    "pemasukan_kas_keliling"   => $pemasukan_kel,
    "pengeluaran_kas_keliling" => $pengeluaran_kel,
    "pemasukan_kas"            => $pemasukan_kel,
    "pengeluaran_kas"          => $pengeluaran_kel,
    "kas_anniversary"          => $kas_anniversary,
    "total_anniversary"        => $kas_anniversary,
    "saldo_cicilan"            => $saldo_cicilan,
    "total_sisa_cicilan"       => $saldo_cicilan,
    "total_saldo"              => $total_saldo,
    "timestamp"                => date("Y-m-d H:i:s")
);
$response_data["data"] = $response_data;

// ==============================================================================
// 3. DETEKSI MODE: API (UNTUK APK ANDROID) VS WEB BROWSER
// ==============================================================================
$is_api_request = isset($_GET['action']) && $_GET['action'] === 'api';
$is_json_header  = isset($_SERVER['HTTP_ACCEPT']) && strpos($_SERVER['HTTP_ACCEPT'], 'application/json') !== false;
$is_android_req  = isset($_SERVER['HTTP_X_REQUESTED_WITH']) || (isset($_SERVER['HTTP_USER_AGENT']) && strpos($_SERVER['HTTP_USER_AGENT'], 'Android') !== false && !isset($_GET['web']));

if ($is_api_request || $is_json_header || (isset($_GET['format']) && $_GET['format'] === 'json')) {
    // --------------------------------------------------------------------------
    // MODE API (Output JSON ringkas untuk Kotlin/Java Android)
    // --------------------------------------------------------------------------
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($response_data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit();
}

// ------------------------------------------------------------------------------
// MODE WEB (Output Tampilan HTML Glassmorphism untuk Browser)
// ------------------------------------------------------------------------------
function rupiah($angka) {
    return "Rp " . number_format($angka, 0, ',', '.');
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEBO Sukabumi - Control Center & API Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Poppins', sans-serif; }
        body {
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
            color: #f8fafc;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .container {
            width: 100%;
            max-width: 950px;
            background: rgba(30, 41, 59, 0.75);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 24px;
            padding: 35px;
            box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5);
        }
        .header { text-align: center; margin-bottom: 30px; }
        .header h1 { font-size: 28px; font-weight: 700; color: #38bdf8; letter-spacing: 1px; }
        .header p { font-size: 14px; color: #94a3b8; margin-top: 5px; }
        .status-badge {
            display: inline-block;
            background: rgba(34, 197, 94, 0.15);
            color: #4ade80;
            border: 1px solid rgba(34, 197, 94, 0.3);
            padding: 6px 18px;
            border-radius: 50px;
            font-size: 13px;
            font-weight: 600;
            margin-top: 15px;
        }
        .grid-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-top: 25px;
        }
        .card {
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 18px;
            padding: 22px;
            transition: all 0.3s ease;
        }
        .card:hover { transform: translateY(-4px); border-color: rgba(56, 189, 248, 0.4); }
        .card-title { font-size: 12px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
        .card-value { font-size: 22px; font-weight: 700; margin-top: 10px; }
        
        .val-kas { color: #4ade80; }
        .val-aniv { color: #facc15; }
        .val-keliling { color: #38bdf8; }
        .val-cicilan { color: #f87171; }
        .val-total { color: #a78bfa; }

        .card-total {
            grid-column: 1 / -1;
            background: rgba(88, 28, 135, 0.25);
            border: 1px solid rgba(167, 139, 250, 0.3);
            text-align: center;
        }

        .actions { display: flex; gap: 15px; margin-top: 30px; }
        .btn {
            flex: 1;
            padding: 14px;
            border: none;
            border-radius: 12px;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            text-align: center;
            text-decoration: none;
            transition: background 0.2s ease;
        }
        .btn-refresh { background: #2563eb; color: #ffffff; }
        .btn-refresh:hover { background: #1d4ed8; }
        .btn-api { background: rgba(255, 255, 255, 0.08); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.3); }
        .btn-api:hover { background: rgba(56, 189, 248, 0.15); }

        .footer {
            text-align: center;
            margin-top: 25px;
            font-size: 12px;
            color: #64748b;
        }
        code { color: #38bdf8; background: rgba(0,0,0,0.3); padding: 3px 8px; border-radius: 6px; }
    </style>
</head>
<body>

    <div class="container">
        <div class="header">
            <h1>NEBO SUKABUMI</h1>
            <p>Unified Server API & Web Control Center</p>
            <div class="status-badge">● Server Online & Data Synchronized</div>
        </div>

        <div class="grid-cards">
            <div class="card">
                <div class="card-title">Saldo Kas Utama</div>
                <div class="card-value val-kas"><?= rupiah($kas_utama) ?></div>
            </div>
            <div class="card">
                <div class="card-title">Kas Anniversary</div>
                <div class="card-value val-aniv"><?= rupiah($kas_anniversary) ?></div>
            </div>
            <div class="card">
                <div class="card-title">Kas Keliling</div>
                <div class="card-value val-keliling"><?= rupiah($kas_keliling) ?></div>
            </div>
            <div class="card">
                <div class="card-title">Saldo Cicilan</div>
                <div class="card-value val-cicilan"><?= rupiah($saldo_cicilan) ?></div>
            </div>
            <div class="card card-total">
                <div class="card-title" style="color: #c084fc;">Total Akumulasi Saldo</div>
                <div class="card-value val-total" style="font-size: 28px;"><?= rupiah($total_saldo) ?></div>
            </div>
        </div>

        <div class="actions">
            <a href="index.php" class="btn btn-refresh">Refresh Data Server</a>
            <a href="index.php?action=api" target="_blank" class="btn btn-api">Cek Output API JSON</a>
        </div>

        <div class="footer">
            Endpoint APK Android: <code>nebosukabumi.net/index.php?action=api</code> atau <code>nebosukabumi.net/api/get_dashboard.php</code>
        </div>
    </div>

</body>
</html>
