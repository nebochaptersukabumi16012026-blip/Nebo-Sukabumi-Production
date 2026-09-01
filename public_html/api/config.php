<?php
// config.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

date_default_timezone_set('Asia/Jakarta');

// Global error handler for debugging 500 errors
error_reporting(E_ALL);
ini_set('display_errors', '0');

function global_error_handler($errno, $errstr, $errfile, $errline) {
    if (!(error_reporting() & $errno)) return;
    echo json_encode(array(
        "status" => "error",
        "message" => "PHP Error ($errno): $errstr",
        "file" => $errfile,
        "line" => $errline
    ));
    exit();
}
set_error_handler("global_error_handler");

function global_shutdown_handler() {
    $error = error_get_last();
    if ($error !== NULL && in_array($error['type'], array(E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR))) {
        if (ob_get_length()) ob_clean();
        echo json_encode(array(
            "status" => "error",
            "message" => "Fatal PHP Error: " . $error['message'],
            "file" => $error['file'],
            "line" => $error['line']
        ));
    }
}
register_shutdown_function("global_shutdown_handler");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

$host = "localhost";
$db_name = "nebk6483_nebo_db";
$username = "nebk6483_nebo_user";
$password = "nfGy7V!RpQkSKY3";


if (!function_exists('getIndonesianMonth')) {
    function getIndonesianMonth($monthNum) {
        $months = array(
            1 => "Januari", 2 => "Februari", 3 => "Maret", 4 => "April",
            5 => "Mei", 6 => "Juni", 7 => "Juli", 8 => "Agustus",
            9 => "September", 10 => "Oktober", 11 => "November", 12 => "Desember"
        );
        return isset($months[intval($monthNum)]) ? $months[intval($monthNum)] : "Unknown";
    }
}

try {
    $pdo = new PDO("mysql:host=" . $host . ";dbname=" . $db_name, $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $conn = $pdo; // Alias for compatibility with other files

    // Recreate community_settings if it's the old schema
    try {
        $stmt_check = $pdo->prepare("SHOW COLUMNS FROM community_settings LIKE 'community_name'");
        $stmt_check->execute();
        if ($stmt_check->rowCount() == 0) {
            $pdo->exec("DROP TABLE IF EXISTS community_settings");
        }
    } catch (PDOException $e) {
        // Table probably doesn't exist yet
    }

    // Create Tables if not exists
    $pdo->exec("CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        role VARCHAR(50) DEFAULT 'USER'
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS anggota (
        id INT AUTO_INCREMENT PRIMARY KEY,
        nama VARCHAR(255) NOT NULL,
        role VARCHAR(50) DEFAULT 'Anggota',
        no_wa VARCHAR(50),
        alamat TEXT,
        tgl_gabung VARCHAR(50),
        uang_kas DOUBLE DEFAULT 0,
        iuran_aniv DOUBLE DEFAULT 0,
        total_cicilan DOUBLE DEFAULT 0,
        harga_barang DOUBLE DEFAULT 0,
        sisa_cicilan DOUBLE DEFAULT 0,
        cicilan_per_bulan DOUBLE DEFAULT 0,
        nra VARCHAR(255) DEFAULT '',
        statusAktif TINYINT(1) DEFAULT 1,
        username VARCHAR(255) DEFAULT '',
        password VARCHAR(255) DEFAULT '',
        foto TEXT DEFAULT NULL,
        totalTagihan DOUBLE DEFAULT 0,
        lamaCicilan INT DEFAULT 0
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS kas_keliling (
        id INT AUTO_INCREMENT PRIMARY KEY,
        nra VARCHAR(255) DEFAULT '',
        nominal DOUBLE DEFAULT 0,
        tanggal BIGINT DEFAULT 0,
        keterangan TEXT,
        jenis_transaksi VARCHAR(50) DEFAULT 'Pemasukan',
        bulan VARCHAR(50) NOT NULL,
        tahun VARCHAR(20) NOT NULL,
        total_pemasukan DOUBLE DEFAULT 0,
        total_pengeluaran DOUBLE DEFAULT 0,
        saldo DOUBLE DEFAULT 0,
        catatan TEXT,
        created_by VARCHAR(255) DEFAULT '',
        created_at BIGINT DEFAULT 0,
        updated_at BIGINT DEFAULT 0
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS riwayat_kas (
        id INT AUTO_INCREMENT PRIMARY KEY,
        id_anggota INT NOT NULL,
        nominal DOUBLE NOT NULL,
        tanggal VARCHAR(50) NOT NULL,
        keterangan TEXT,
        created_at BIGINT DEFAULT 0
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS riwayat_aniv (
        id INT AUTO_INCREMENT PRIMARY KEY,
        id_anggota INT NOT NULL,
        nominal DOUBLE NOT NULL,
        tanggal VARCHAR(50) NOT NULL,
        keterangan TEXT,
        created_at BIGINT DEFAULT 0
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS iuran_anniversary (
        id INT AUTO_INCREMENT PRIMARY KEY,
        anggota_id INT NOT NULL,
        nominal DOUBLE NOT NULL,
        tanggal VARCHAR(50) NOT NULL,
        keterangan TEXT
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS pengeluaran (
        id INT AUTO_INCREMENT PRIMARY KEY,
        keterangan TEXT NOT NULL,
        nominal DOUBLE NOT NULL,
        tanggal VARCHAR(50) NOT NULL,
        jenis_kas VARCHAR(50) NOT NULL,
        created_by VARCHAR(255)
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS cicilan (
        id INT AUTO_INCREMENT PRIMARY KEY,
        anggota_id INT NOT NULL,
        nominal DOUBLE NOT NULL,
        tanggal VARCHAR(50) NOT NULL,
        keterangan TEXT
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS absensi (
        id INT AUTO_INCREMENT PRIMARY KEY,
        anggota_id INT NOT NULL,
        tanggal VARCHAR(50) NOT NULL,
        status VARCHAR(50) NOT NULL,
        keterangan TEXT
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS catatan (
        id INT AUTO_INCREMENT PRIMARY KEY,
        judul VARCHAR(255) NOT NULL,
        isi TEXT,
        tanggal VARCHAR(50) NOT NULL
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS pembayaran (
        id INT AUTO_INCREMENT PRIMARY KEY,
        firestoreId VARCHAR(100) DEFAULT NULL,
        anggotaId INT NOT NULL,
        anggotaNama VARCHAR(100) NOT NULL,
        jenisPembayaran VARCHAR(50) NOT NULL,
        nominal DOUBLE NOT NULL,
        tanggal BIGINT NOT NULL,
        keterangan TEXT DEFAULT NULL,
        buktiPembayaran TEXT DEFAULT NULL
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS community_settings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        community_name VARCHAR(255) DEFAULT 'NEBO SUKABUMI',
        community_slogan VARCHAR(255) DEFAULT '',
        community_motto TEXT,
        community_logo TEXT,
        community_banner TEXT,
        community_splash TEXT,
        community_address TEXT,
        community_phone VARCHAR(50) DEFAULT '',
        community_email VARCHAR(255) DEFAULT '',
        community_website VARCHAR(255) DEFAULT '',
        community_facebook VARCHAR(255) DEFAULT '',
        community_instagram VARCHAR(255) DEFAULT '',
        community_youtube VARCHAR(255) DEFAULT '',
        target_aniv DOUBLE DEFAULT 0,
        target_kas DOUBLE DEFAULT 0,
        login_background TEXT DEFAULT NULL,
        profile_banner TEXT DEFAULT NULL,
        cumulative_kas_archive DOUBLE DEFAULT 0,
        updated_at BIGINT DEFAULT 0,
        updated_by VARCHAR(255) DEFAULT ''
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS reset_password_requests (
        id INT AUTO_INCREMENT PRIMARY KEY,
        nama_anggota VARCHAR(255) NOT NULL,
        nra VARCHAR(255) NOT NULL,
        tanggal VARCHAR(50) NOT NULL,
        jam VARCHAR(50) NOT NULL,
        role VARCHAR(50) NOT NULL,
        status VARCHAR(50) DEFAULT 'Menunggu Persetujuan Admin',
        password_sementara VARCHAR(255) DEFAULT NULL
    )");
    $pdo->exec("CREATE TABLE IF NOT EXISTS saldo_akumulasi (
        id INT AUTO_INCREMENT PRIMARY KEY,
        jenis_kas VARCHAR(50) UNIQUE NOT NULL,
        total_akumulasi_masuk DOUBLE DEFAULT 0,
        total_akumulasi_keluar DOUBLE DEFAULT 0,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )");

    // Ensure all columns exist in 'community_settings' table
    $settings_cols = array(
        "target_aniv" => "DOUBLE DEFAULT 0",
        "target_kas" => "DOUBLE DEFAULT 0",
        "login_background" => "TEXT DEFAULT NULL",
        "profile_banner" => "TEXT DEFAULT NULL",
        "cumulative_kas_archive" => "DOUBLE DEFAULT 0"
    );
    foreach ($settings_cols as $col => $type) {
        try {
            $pdo->exec("ALTER TABLE community_settings ADD COLUMN $col $type");
        } catch (PDOException $e) {}
    }

    // Ensure total_akumulasi_keluar exists in saldo_akumulasi
    try {
        $pdo->exec("ALTER TABLE saldo_akumulasi ADD COLUMN total_akumulasi_keluar DOUBLE DEFAULT 0");
    } catch (PDOException $e) {}

    // Ensure columns in 'anggota'
    $anggota_cols = array(
        "nra" => "VARCHAR(255) DEFAULT ''",
        "statusAktif" => "TINYINT(1) DEFAULT 1",
        "username" => "VARCHAR(255) DEFAULT ''",
        "password" => "VARCHAR(255) DEFAULT ''",
        "foto" => "TEXT DEFAULT NULL",
        "totalTagihan" => "DOUBLE DEFAULT 0",
        "lamaCicilan" => "INT DEFAULT 0",
        "uang_kas" => "DOUBLE DEFAULT 0",
        "iuran_aniv" => "DOUBLE DEFAULT 0",
        "total_cicilan" => "DOUBLE DEFAULT 0",
        "harga_barang" => "DOUBLE DEFAULT 0",
        "sisa_cicilan" => "DOUBLE DEFAULT 0",
        "cicilan_per_bulan" => "DOUBLE DEFAULT 0"
    );
    foreach ($anggota_cols as $col => $type) {
        try {
            $pdo->exec("ALTER TABLE anggota ADD COLUMN $col $type");
        } catch (PDOException $e) {}
    }

    // Ensure columns in 'kas_keliling'
    $kk_cols = array(
        "nra" => "VARCHAR(255) DEFAULT ''",
        "nominal" => "DOUBLE DEFAULT 0",
        "tanggal" => "VARCHAR(50) DEFAULT ''",
        "keterangan" => "TEXT",
        "jenis" => "VARCHAR(50) DEFAULT 'pemasukan'",
        "catatan" => "TEXT",
        "jenis_transaksi" => "VARCHAR(50) DEFAULT 'Pemasukan'",
        "bulan" => "VARCHAR(50) DEFAULT ''",
        "tahun" => "VARCHAR(20) DEFAULT ''",
        "total_pemasukan" => "DOUBLE DEFAULT 0",
        "total_pengeluaran" => "DOUBLE DEFAULT 0",
        "saldo" => "DOUBLE DEFAULT 0",
        "created_by" => "VARCHAR(255) DEFAULT ''",
        "created_at" => "BIGINT DEFAULT 0"
    );
    foreach ($kk_cols as $col => $type) {
        try {
            $pdo->exec("ALTER TABLE kas_keliling ADD COLUMN $col $type");
        } catch (PDOException $e) {}
    }
    
    // Check for camelCase columns from database.sql and sync if necessary
    try {
        $pdo->exec("UPDATE kas_keliling SET jenis_transaksi = jenisTransaksi WHERE jenis_transaksi = 'Pemasukan' AND jenisTransaksi IS NOT NULL");
    } catch (Exception $e) {}

    // Ensure columns in 'pengeluaran'
    $pengeluaran_cols = array(
        "jenis_kas" => "VARCHAR(50) DEFAULT 'kas_utama'",
        "created_by" => "VARCHAR(255) DEFAULT ''"
    );
    foreach ($pengeluaran_cols as $col => $type) {
        try {
            $pdo->exec("ALTER TABLE pengeluaran ADD COLUMN $col $type");
        } catch (PDOException $e) {}
    }
    
    // Sync jenis_kas from jenisKas if necessary
    try {
        $pdo->exec("UPDATE pengeluaran SET jenis_kas = jenisKas WHERE (jenis_kas IS NULL OR jenis_kas = '' OR jenis_kas = 'kas_utama') AND jenisKas IS NOT NULL AND jenisKas != ''");
    } catch (Exception $e) {}

    // Check for camelCase columns from database.sql and sync if necessary for anggota
    try {
        $pdo->exec("UPDATE anggota SET uang_kas = uangKas WHERE uang_kas = 0 AND uangKas > 0");
        $pdo->exec("UPDATE anggota SET iuran_aniv = iuranAniv WHERE iuran_aniv = 0 AND iuranAniv > 0");
        $pdo->exec("UPDATE anggota SET sisa_cicilan = sisaCicilan WHERE sisa_cicilan = 0 AND sisaCicilan > 0");
    } catch (Exception $e) {}

    // Initial Master Ledger Seeding if empty
    try {
        $checkMaster = $pdo->query("SELECT COUNT(*) as cnt FROM saldo_akumulasi");
        $row_master = $checkMaster ? $checkMaster->fetch(PDO::FETCH_ASSOC) : null;
        if (!$row_master || intval($row_master['cnt']) == 0) {
            $pdo->exec("
                INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) 
                VALUES 
                    ('kas_utama', 
                        (SELECT COALESCE(SUM(uang_kas), 0) FROM anggota), 
                        (SELECT COALESCE(SUM(nominal), 0) FROM pengeluaran WHERE LOWER(COALESCE(jenis_kas, '')) IN ('kas_utama', 'saldo kas', 'kas', 'kas utama', 'uang kas', 'uang_kas', ''))),
                    ('kas_aniv', 
                        (SELECT COALESCE(SUM(iuran_aniv), 0) FROM anggota), 
                        (SELECT COALESCE(SUM(nominal), 0) FROM pengeluaran WHERE LOWER(COALESCE(jenis_kas, '')) IN ('kas_anniversary', 'kas anniversary', 'kas_aniv', 'kas aniv'))),
                    ('kas_keliling', 
                        (SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) FROM kas_keliling), 
                        (SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE total_pengeluaran END), 0) FROM kas_keliling))
                ON DUPLICATE KEY UPDATE total_akumulasi_masuk = total_akumulasi_masuk
            ");
        }
    } catch (Throwable $e) {
        error_log("Seeding Error: " . $e->getMessage());
    }

    // Ensure bendahara account exists
    $stmt = $pdo->prepare("SELECT id FROM users WHERE username = 'bendahara'");
    $stmt->execute();
    if (!$stmt->fetch()) {
        $pdo->prepare("INSERT INTO users (username, password, role) VALUES ('bendahara', 'est2024', 'BENDAHARA')")->execute();
    }

    // --- MIGRATION: Copy existing KAS transactions from pembayaran to kas_keliling if empty ---
    try {
        $stmt_check = $pdo->query("SELECT COUNT(*) as total FROM kas_keliling WHERE nra != '' OR nominal > 0");
        $count_res = $stmt_check->fetch(PDO::FETCH_ASSOC);
        $count = $count_res ? intval($count_res['total']) : 0;
        
        if ($count == 0) {
            // Check if pembayaran table exists before migrating
            $checkTable = $pdo->query("SHOW TABLES LIKE 'pembayaran'");
            if ($checkTable->rowCount() > 0) {
                $stmt_source = $pdo->query("SELECT p.*, a.nra FROM pembayaran p LEFT JOIN anggota a ON a.id = p.anggotaId WHERE p.jenisPembayaran = 'KAS'");
                $rows = $stmt_source->fetchAll(PDO::FETCH_ASSOC);
                
                if (count($rows) > 0) {
                    $stmt_ins = $pdo->prepare("INSERT INTO kas_keliling (nra, nominal, tanggal, keterangan, jenis_transaksi, bulan, tahun, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    $stmt_rk = $pdo->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, tanggal, keterangan, created_at) VALUES (?, ?, ?, ?, ?)");
                    foreach ($rows as $row) {
                        $ts = (isset($row['tanggal']) && $row['tanggal'] > 0) ? $row['tanggal'] : (time() * 1000);
                        $formattedDate = date('Y-m-d H:i:s', intval($ts / 1000));
                        $bulan = getIndonesianMonth(date("n", $ts / 1000));
                        $tahun = date("Y", $ts / 1000);
                        
                        // Migrate to kas_keliling
                        $stmt_ins->execute(array(
                            isset($row['nra']) ? $row['nra'] : '',
                            $row['nominal'],
                            $ts,
                            $row['keterangan'] ? $row['keterangan'] : 'Migrated Payment',
                            'Pemasukan',
                            $bulan,
                            $tahun,
                            $row['anggotaNama'],
                            $ts
                        ));

                        // Migrate to riwayat_kas
                        $stmt_rk->execute(array(
                            $row['anggotaId'],
                            $row['nominal'],
                            $formattedDate,
                            $row['keterangan'] ? $row['keterangan'] : 'Migrated Payment',
                            $ts
                        ));
                    }
                }
            }
        }
    } catch (Exception $e) {
        error_log("Migration Error: " . $e->getMessage());
    }

} catch(Throwable $exception) {
    if (ob_get_length()) ob_clean();
    echo json_encode(array("status" => "error", "message" => "Database initialization failed: " . $exception->getMessage(), "file" => $exception->getFile(), "line" => $exception->getLine()));
    exit();
}
