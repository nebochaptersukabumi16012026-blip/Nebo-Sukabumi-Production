<?php
// config.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

$host = "localhost";
$db_name = "nebk6483_nebo_db";
$username = "nebk6483_nebo_user";
$password = "nfGy7V!RpQkSKY3";

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
    $tables = [
        "CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(255) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            role VARCHAR(50) DEFAULT 'USER'
        )",
        "CREATE TABLE IF NOT EXISTS anggota (
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
            cicilan_per_bulan DOUBLE DEFAULT 0
        )",
        "CREATE TABLE IF NOT EXISTS kas_keliling (
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
        )",
        "CREATE TABLE IF NOT EXISTS riwayat_kas (
            id INT AUTO_INCREMENT PRIMARY KEY,
            id_anggota INT NOT NULL,
            nominal DOUBLE NOT NULL,
            tanggal VARCHAR(50) NOT NULL,
            keterangan TEXT,
            created_at BIGINT DEFAULT 0
        )",
        "CREATE TABLE IF NOT EXISTS riwayat_aniv (
            id INT AUTO_INCREMENT PRIMARY KEY,
            id_anggota INT NOT NULL,
            nominal DOUBLE NOT NULL,
            tanggal VARCHAR(50) NOT NULL,
            keterangan TEXT,
            created_at BIGINT DEFAULT 0
        )",
        "CREATE TABLE IF NOT EXISTS iuran_anniversary (
            id INT AUTO_INCREMENT PRIMARY KEY,
            anggota_id INT NOT NULL,
            nominal DOUBLE NOT NULL,
            tanggal VARCHAR(50) NOT NULL,
            keterangan TEXT
        )",
        "CREATE TABLE IF NOT EXISTS pengeluaran (
            id INT AUTO_INCREMENT PRIMARY KEY,
            keterangan TEXT NOT NULL,
            nominal DOUBLE NOT NULL,
            tanggal VARCHAR(50) NOT NULL,
            jenis_kas VARCHAR(50) NOT NULL,
            created_by VARCHAR(255)
        )",
        "CREATE TABLE IF NOT EXISTS cicilan (
            id INT AUTO_INCREMENT PRIMARY KEY,
            anggota_id INT NOT NULL,
            nominal DOUBLE NOT NULL,
            tanggal VARCHAR(50) NOT NULL,
            keterangan TEXT
        )",
        "CREATE TABLE IF NOT EXISTS absensi (
            id INT AUTO_INCREMENT PRIMARY KEY,
            anggota_id INT NOT NULL,
            tanggal VARCHAR(50) NOT NULL,
            status VARCHAR(50) NOT NULL,
            keterangan TEXT
        )",
        "CREATE TABLE IF NOT EXISTS catatan (
            id INT AUTO_INCREMENT PRIMARY KEY,
            judul VARCHAR(255) NOT NULL,
            isi TEXT,
            tanggal VARCHAR(50) NOT NULL
        )",
        "CREATE TABLE IF NOT EXISTS pembayaran (
            id INT AUTO_INCREMENT PRIMARY KEY,
            firestoreId VARCHAR(100) DEFAULT NULL,
            anggotaId INT NOT NULL,
            anggotaNama VARCHAR(100) NOT NULL,
            jenisPembayaran VARCHAR(50) NOT NULL,
            nominal DOUBLE NOT NULL,
            tanggal BIGINT NOT NULL,
            keterangan TEXT DEFAULT NULL,
            buktiPembayaran TEXT DEFAULT NULL
        )",
        "CREATE TABLE IF NOT EXISTS community_settings (
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
            updated_at BIGINT DEFAULT 0,
            updated_by VARCHAR(255) DEFAULT ''
        )",
        "CREATE TABLE IF NOT EXISTS reset_password_requests (
            id INT AUTO_INCREMENT PRIMARY KEY,
            nama_anggota VARCHAR(255) NOT NULL,
            nra VARCHAR(255) NOT NULL,
            tanggal VARCHAR(50) NOT NULL,
            jam VARCHAR(50) NOT NULL,
            role VARCHAR(50) NOT NULL,
            status VARCHAR(50) DEFAULT 'Menunggu Persetujuan Admin',
            password_sementara VARCHAR(255) DEFAULT NULL
        )"
    ];

    foreach ($tables as $sql) {
        $pdo->exec($sql);
    }
    
    try {
        $pdo->exec("ALTER TABLE community_settings ADD COLUMN target_aniv DOUBLE DEFAULT 0");
    } catch (PDOException $e) {
        // Ignore if already exists
    }
    
    try {
        $pdo->exec("ALTER TABLE community_settings ADD COLUMN target_kas DOUBLE DEFAULT 0");
    } catch (PDOException $e) {
        // Ignore if already exists
    }

    // Ensure columns exist in 'community_settings' table
    $settings_alterations = [
        "ALTER TABLE community_settings ADD COLUMN login_background TEXT DEFAULT NULL",
        "ALTER TABLE community_settings ADD COLUMN profile_banner TEXT DEFAULT NULL",
        "ALTER TABLE community_settings ADD COLUMN cumulative_kas_archive DOUBLE DEFAULT 0"
    ];
    foreach ($settings_alterations as $sql) {
        try {
            $pdo->exec($sql);
        } catch (PDOException $e) {
            // Ignore if already exists
        }
    }

    // Ensure all columns exist in 'anggota' table
    $alterations = [
        "ALTER TABLE anggota ADD COLUMN nra VARCHAR(255) DEFAULT ''",
        "ALTER TABLE anggota ADD COLUMN statusAktif TINYINT(1) DEFAULT 1",
        "ALTER TABLE anggota ADD COLUMN username VARCHAR(255) DEFAULT ''",
        "ALTER TABLE anggota ADD COLUMN password VARCHAR(255) DEFAULT ''",
        "ALTER TABLE anggota ADD COLUMN foto TEXT DEFAULT NULL",
        "ALTER TABLE anggota ADD COLUMN totalTagihan DOUBLE DEFAULT 0",
        "ALTER TABLE anggota ADD COLUMN lamaCicilan INT DEFAULT 0"
    ];
    foreach ($alterations as $sql) {
        try {
            $pdo->exec($sql);
        } catch (PDOException $e) {
            // Ignore if already exists
        }
    }

    // Pastikan tabel users memiliki akun: username = bendahara
    $stmt = $pdo->prepare("SELECT id, password FROM users WHERE username = 'bendahara'");
    $stmt->execute();
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    $pwd = "est2024";
    if (!$row) {
        $pdo->prepare("INSERT INTO users (username, password, role) VALUES ('bendahara', :pwd, 'BENDAHARA')")->execute([':pwd' => $pwd]);
    } else {
        if ($row['password'] !== $pwd) {
            $pdo->prepare("UPDATE users SET password = :pwd, role = 'BENDAHARA' WHERE username = 'bendahara'")->execute([':pwd' => $pwd]);
        }
    }

    // --- MIGRATION: Copy existing KAS transactions from pembayaran to kas_keliling if empty ---
    try {
        $stmt_check = $pdo->query("SELECT COUNT(*) as total FROM kas_keliling WHERE nra != '' OR nominal > 0");
        $count_res = $stmt_check->fetch(PDO::FETCH_ASSOC);
        $count = $count_res ? intval($count_res['total']) : 0;
        
        if ($count == 0) {
            $stmt_source = $pdo->query("SELECT p.*, a.nra FROM pembayaran p LEFT JOIN anggota a ON a.id = p.anggotaId WHERE p.jenisPembayaran = 'KAS'");
            $rows = $stmt_source->fetchAll(PDO::FETCH_ASSOC);
            
            if (count($rows) > 0) {
                $stmt_ins = $pdo->prepare("INSERT INTO kas_keliling (nra, nominal, tanggal, keterangan, jenis_transaksi, bulan, tahun, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                foreach ($rows as $row) {
                    $ts = (isset($row['tanggal']) && $row['tanggal'] > 0) ? $row['tanggal'] : (time() * 1000);
                    $bulan = date("F", $ts / 1000);
                    $tahun = date("Y", $ts / 1000);
                    $stmt_ins->execute([
                        $row['nra'] ?? '',
                        $row['nominal'],
                        $ts,
                        $row['keterangan'] ?: 'Migrated Payment',
                        'Pemasukan',
                        $bulan,
                        $tahun,
                        $row['anggotaNama'],
                        $ts
                    ]);
                }
            }
        }
    } catch (Exception $e) {
        error_log("Migration Error: " . $e->getMessage());
    }

} catch(PDOException $exception) {
    echo json_encode(array("status" => "error", "message" => "Koneksi database gagal: " . $exception->getMessage()));
    exit();
}
?>
