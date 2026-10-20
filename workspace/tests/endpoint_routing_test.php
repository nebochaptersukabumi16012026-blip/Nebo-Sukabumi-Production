<?php
/**
 * Test Suite Pengujian Endpoint Wrapper & REST API NEBO Sukabumi
 */

$testDbPath = sys_get_temp_dir() . '/nebo_endpoint_test_' . time() . '.db';
putenv("NEBO_DB_DSN=sqlite:" . $testDbPath);

$pdo = new PDO("sqlite:" . $testDbPath, null, null, [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
]);

// Skema dasar
$pdo->exec("
CREATE TABLE IF NOT EXISTS admin (id INTEGER PRIMARY KEY AUTOINCREMENT, username VARCHAR(50), password_hash VARCHAR(255), role VARCHAR(50));
CREATE TABLE IF NOT EXISTS anggota (id INTEGER PRIMARY KEY AUTOINCREMENT, nra VARCHAR(50), nama VARCHAR(100), uang_kas REAL DEFAULT 0, uangKas REAL DEFAULT 0, iuran_aniv REAL DEFAULT 0, iuranAniv REAL DEFAULT 0, total_cicilan REAL DEFAULT 0, sisa_cicilan REAL DEFAULT 0, sisaCicilan REAL DEFAULT 0, harga_barang REAL DEFAULT 0, hargaBarang REAL DEFAULT 0, cicilan_per_bulan REAL DEFAULT 0, role VARCHAR(50) DEFAULT 'Anggota', password VARCHAR(255) DEFAULT '12345', statusAktif INTEGER DEFAULT 1);
CREATE TABLE IF NOT EXISTS riwayat_kas (id INTEGER PRIMARY KEY AUTOINCREMENT, id_anggota INTEGER, nominal REAL, tanggal VARCHAR(50), keterangan TEXT, created_at INTEGER DEFAULT 0);
CREATE TABLE IF NOT EXISTS iuran_anniversary (id INTEGER PRIMARY KEY AUTOINCREMENT, anggota_id INTEGER, nominal REAL, tanggal VARCHAR(50), keterangan TEXT);
CREATE TABLE IF NOT EXISTS riwayat_aniv (id INTEGER PRIMARY KEY AUTOINCREMENT, id_anggota INTEGER, nominal REAL, tanggal VARCHAR(50), keterangan TEXT, created_at INTEGER DEFAULT 0);
CREATE TABLE IF NOT EXISTS cicilan (id INTEGER PRIMARY KEY AUTOINCREMENT, anggota_id INTEGER, nominal REAL, tanggal VARCHAR(50), keterangan TEXT);
CREATE TABLE IF NOT EXISTS pembayaran (id INTEGER PRIMARY KEY AUTOINCREMENT, firestoreId VARCHAR(100), anggotaId INTEGER, anggotaNama VARCHAR(100), jenisPembayaran VARCHAR(50), nominal REAL, tanggal INTEGER, keterangan TEXT, buktiPembayaran TEXT);
CREATE TABLE IF NOT EXISTS pengeluaran (id INTEGER PRIMARY KEY AUTOINCREMENT, keterangan TEXT, nominal REAL, tanggal VARCHAR(50), jenis_kas VARCHAR(50), created_by VARCHAR(255) DEFAULT 'Sistem');
CREATE TABLE IF NOT EXISTS kas_keliling (id INTEGER PRIMARY KEY AUTOINCREMENT, bulan VARCHAR(50), tahun VARCHAR(20), nominal REAL, jenis VARCHAR(50), total_pemasukan REAL DEFAULT 0, total_pengeluaran REAL DEFAULT 0, saldo REAL DEFAULT 0);
CREATE TABLE IF NOT EXISTS community_settings (id INTEGER PRIMARY KEY AUTOINCREMENT, community_name VARCHAR(255), target_aniv REAL DEFAULT 500000, target_kas REAL DEFAULT 100000);
INSERT INTO community_settings (id, community_name, target_aniv, target_kas) VALUES (1, 'NEBO SUKABUMI', 500000, 100000);
INSERT INTO admin (username, password_hash, role) VALUES ('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'ADMIN');
INSERT INTO anggota (id, nama, nra, harga_barang, sisa_cicilan) VALUES (1, 'Budi Santoso', 'NRA-001', 500000, 500000);
");

$passed = 0;
$failed = 0;

function assertEndpoint($desc, $cond) {
    global $passed, $failed;
    if ($cond) {
        echo "[PASS] $desc\n";
        $passed++;
    } else {
        echo "[FAIL] $desc\n";
        $failed++;
    }
}

echo "====================================================\n";
echo "PENGUJIAN ENDPOINT WRAPPER & ROUTING\n";
echo "====================================================\n\n";

// Helper memanggil script API refactored
function callApiEndpoint($scriptPath, $method = 'GET', $body = [], $query = []) {
    global $testDbPath;
    $dsn = "sqlite:" . $testDbPath;
    $fullScript = __DIR__ . '/../refactored_api/' . $scriptPath;
    $cmd = sprintf(
        "NEBO_DB_DSN=%s php -d display_errors=0 -r %s 2>&1",
        escapeshellarg($dsn),
        escapeshellarg("
            \$_SERVER['REQUEST_METHOD'] = '$method';
            \$_GET = " . var_export($query, true) . ";
            \$_POST = " . var_export($body, true) . ";
            require '$fullScript';
        ")
    );
    $output = shell_exec($cmd);
    return json_decode($output, true) ?: $output;
}

// 1. Uji Tambah Kas via tambah_kas.php
$resKas = callApiEndpoint('tambah_kas.php', 'POST', [
    'id_anggota' => 1,
    'nominal' => 25000,
    'keterangan' => 'Kas Iuran 1',
    'role' => 'ADMIN'
]);
assertEndpoint("tambah_kas.php: Berhasil mencatat kas", is_array($resKas) && ($resKas['success'] ?? false) === true);

// 2. Uji Tambah Anniversary via iuran_anniversary.php
$resAniv = callApiEndpoint('iuran_anniversary.php', 'POST', [
    'id_anggota' => 1,
    'nominal' => 50000,
    'keterangan' => 'Aniv 2026 Part 1',
    'role' => 'ADMIN'
]);
assertEndpoint("iuran_anniversary.php: Berhasil mencatat anniversary", is_array($resAniv) && ($resAniv['success'] ?? false) === true);

// 3. Uji Tambah Cicilan via cicilan.php
$resCic = callApiEndpoint('cicilan.php', 'POST', [
    'anggota_id' => 1,
    'nominal' => 100000,
    'keterangan' => 'Cicilan Rompi 1',
    'role' => 'ADMIN'
]);
assertEndpoint("cicilan.php: Berhasil mencatat cicilan", is_array($resCic) && ($resCic['success'] ?? false) === true);

// 4. Uji Pengeluaran via pengeluaran.php
$resOut = callApiEndpoint('pengeluaran.php', 'POST', [
    'nominal' => 10000,
    'keterangan' => 'Konsumsi Pertemuan',
    'jenis_kas' => 'kas_utama',
    'role' => 'ADMIN'
]);
assertEndpoint("pengeluaran.php: Berhasil mencatat pengeluaran", is_array($resOut) && ($resOut['success'] ?? false) === true);

// 5. Uji get_dashboard.php
$resDash = callApiEndpoint('get_dashboard.php', 'GET');
assertEndpoint("get_dashboard.php: Data saldo kas utama, aniv, cicilan sinkron", 
    is_array($resDash) && ($resDash['success'] ?? false) === true && 
    ($resDash['data']['saldo_kas_utama'] ?? 0) == 15000 && // 25.000 - 10.000
    ($resDash['data']['kas_anniversary'] ?? 0) == 50000 &&
    ($resDash['data']['saldo_cicilan'] ?? 0) == 400000 // 500.000 - 100.000
);

// 6. Uji get_laporan.php
$resLap = callApiEndpoint('get_laporan.php', 'GET');
assertEndpoint("get_laporan.php: Laporan neraca konsisten 100% dengan transaksi sumber",
    is_array($resLap) && ($resLap['success'] ?? false) === true &&
    ($resLap['data']['kas_utama']['saldo_kas'] ?? 0) == 15000 &&
    ($resLap['data']['kas_anniversary']['saldo_anniversary'] ?? 0) == 50000 &&
    ($resLap['data']['cicilan']['total_sisa_cicilan'] ?? 0) == 400000
);

// 7. Uji Hapus Kas via delete_kas.php
$kasRow = $pdo->query("SELECT id FROM riwayat_kas WHERE id_anggota = 1 LIMIT 1")->fetch();
$resDelKas = callApiEndpoint('delete_kas.php', 'POST', [
    'id' => $kasRow['id'],
    'role' => 'ADMIN'
]);
assertEndpoint("delete_kas.php: Berhasil menghapus kas melalui wrapper legacy", 
    is_array($resDelKas) && ($resDelKas['success'] ?? false) === true);

// Verifikasi saldo kas kembali 0
$checkKas = $pdo->query("SELECT uang_kas FROM anggota WHERE id = 1")->fetch();
assertEndpoint("Verifikasi: Saldo kas anggota berkurang menjadi 0 setelah dihapus", ($checkKas['uang_kas'] ?? -1) == 0);

// 8. Uji Hapus Cicilan via hapus_cicilan.php
$cicRow = $pdo->query("SELECT id FROM cicilan WHERE anggota_id = 1 LIMIT 1")->fetch();
$resDelCic = callApiEndpoint('hapus_cicilan.php', 'POST', [
    'id' => $cicRow['id'],
    'role' => 'ADMIN'
]);
assertEndpoint("hapus_cicilan.php: Berhasil menghapus cicilan melalui wrapper legacy", 
    is_array($resDelCic) && ($resDelCic['success'] ?? false) === true);

// Verifikasi sisa cicilan kembali 500.000
$checkCic = $pdo->query("SELECT sisa_cicilan, total_cicilan FROM anggota WHERE id = 1")->fetch();
assertEndpoint("Verifikasi: Sisa cicilan kembali ke nilai penuh (Rp 500.000)", ($checkCic['sisa_cicilan'] ?? 0) == 500000);

echo "\n====================================================\n";
echo "HASIL PENGUJIAN ENDPOINT:\n";
echo "Total Lulus: $passed\n";
echo "Total Gagal: $failed\n";
echo "====================================================\n";
exit($failed === 0 ? 0 : 1);
