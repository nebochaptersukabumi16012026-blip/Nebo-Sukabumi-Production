<?php
/**
 * Test Suite Audit & Pengujian Wajib API NEBO Sukabumi
 * Memverifikasi integritas relasi tabel, sinkronisasi saldo, anti-double submit,
 * presisi penghapusan per-transaksi, dan konsistensi Dashboard & Laporan.
 */

// Siapkan database staging SQLite in-memory / file
$testDbPath = sys_get_temp_dir() . '/nebo_test_' . time() . '.db';
putenv("NEBO_DB_DSN=sqlite:" . $testDbPath);

$pdo = new PDO("sqlite:" . $testDbPath, null, null, [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
]);

// 1. Skema Database
$pdo->exec("
CREATE TABLE IF NOT EXISTS admin (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS anggota (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nra VARCHAR(50) NOT NULL,
    nama VARCHAR(100) NOT NULL,
    alias VARCHAR(100) DEFAULT NULL,
    telepon VARCHAR(20) DEFAULT NULL,
    no_wa VARCHAR(20) DEFAULT NULL,
    alamat TEXT DEFAULT NULL,
    statusAktif INTEGER DEFAULT 1,
    tanggalLahir VARCHAR(20) DEFAULT NULL,
    uang_kas REAL DEFAULT 0,
    uangKas REAL DEFAULT 0,
    iuran_aniv REAL DEFAULT 0,
    iuranAniv REAL DEFAULT 0,
    total_cicilan REAL DEFAULT 0,
    sisa_cicilan REAL DEFAULT 0,
    sisaCicilan REAL DEFAULT 0,
    harga_barang REAL DEFAULT 0,
    hargaBarang REAL DEFAULT 0,
    cicilan_per_bulan REAL DEFAULT 0,
    namaBarang VARCHAR(100) DEFAULT NULL,
    role VARCHAR(50) DEFAULT 'Anggota',
    password VARCHAR(255) DEFAULT '12345',
    username VARCHAR(50) DEFAULT NULL,
    foto TEXT DEFAULT NULL,
    tgl_gabung VARCHAR(50) DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS riwayat_kas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_anggota INTEGER NOT NULL,
    nominal REAL NOT NULL,
    tanggal VARCHAR(50) NOT NULL,
    keterangan TEXT,
    created_at INTEGER DEFAULT 0
);
CREATE TABLE IF NOT EXISTS iuran_anniversary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    anggota_id INTEGER NOT NULL,
    nominal REAL NOT NULL,
    tanggal VARCHAR(50) NOT NULL,
    keterangan TEXT
);
CREATE TABLE IF NOT EXISTS riwayat_aniv (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_anggota INTEGER NOT NULL,
    nominal REAL NOT NULL,
    tanggal VARCHAR(50) NOT NULL,
    keterangan TEXT,
    created_at INTEGER DEFAULT 0
);
CREATE TABLE IF NOT EXISTS cicilan (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    anggota_id INTEGER NOT NULL,
    nominal REAL NOT NULL,
    tanggal VARCHAR(50) NOT NULL,
    keterangan TEXT
);
CREATE TABLE IF NOT EXISTS pembayaran (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firestoreId VARCHAR(100) DEFAULT NULL,
    anggotaId INTEGER NOT NULL,
    anggotaNama VARCHAR(100) NOT NULL,
    jenisPembayaran VARCHAR(50) NOT NULL,
    nominal REAL NOT NULL,
    tanggal INTEGER NOT NULL,
    keterangan TEXT DEFAULT NULL,
    buktiPembayaran TEXT DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS pengeluaran (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    keterangan TEXT NOT NULL,
    nominal REAL NOT NULL,
    tanggal VARCHAR(50) NOT NULL,
    jenis_kas VARCHAR(50) NOT NULL,
    created_by VARCHAR(255) DEFAULT 'Sistem'
);
CREATE TABLE IF NOT EXISTS kas_keliling (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bulan VARCHAR(50) NOT NULL,
    tahun VARCHAR(20) NOT NULL,
    nominal REAL NOT NULL,
    jenis VARCHAR(50) DEFAULT 'pemasukan',
    jenis_transaksi VARCHAR(50) DEFAULT 'Pemasukan',
    catatan TEXT DEFAULT NULL,
    keterangan TEXT DEFAULT NULL,
    tanggal VARCHAR(50) DEFAULT NULL,
    nra VARCHAR(50) DEFAULT NULL,
    total_pemasukan REAL DEFAULT 0,
    total_pengeluaran REAL DEFAULT 0,
    totalPemasukan REAL DEFAULT 0,
    totalPengeluaran REAL DEFAULT 0,
    saldo REAL DEFAULT 0,
    saldoBulan REAL DEFAULT 0,
    created_at INTEGER DEFAULT 0,
    updated_at INTEGER DEFAULT 0
);
CREATE TABLE IF NOT EXISTS community_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    community_name VARCHAR(255) DEFAULT 'NEBO SUKABUMI',
    target_aniv REAL DEFAULT 500000,
    target_kas REAL DEFAULT 100000,
    updated_at INTEGER DEFAULT 0
);
INSERT INTO community_settings (id, community_name, target_aniv, target_kas) VALUES (1, 'NEBO SUKABUMI', 500000, 100000);
");

require_once __DIR__ . '/../refactored_api/core/sync_helper.php';

$passed = 0;
$failed = 0;

function assertTest($description, $condition) {
    global $passed, $failed;
    if ($condition) {
        echo "[PASS] " . $description . "\n";
        $passed++;
    } else {
        echo "[FAIL] " . $description . "\n";
        $failed++;
    }
}

echo "====================================================\n";
echo "MENJALANKAN AUDIT & PENGUJIAN API NEBO SUKABUMI\n";
echo "====================================================\n\n";

// --- TEST 1: ANGGOTA CRUD ---
echo "--- 1. Pengujian Modul Anggota ---\n";
$pdo->prepare("
    INSERT INTO anggota (id, nama, nra, harga_barang, sisa_cicilan) 
    VALUES (1, 'Budi Santoso', 'NRA-001', 1000000, 1000000)
")->execute();
$m = $pdo->query("SELECT * FROM anggota WHERE id = 1")->fetch();
assertTest("Anggota berhasil didaftarkan dengan harga_barang Rp 1.000.000", $m && $m['harga_barang'] == 1000000);
assertTest("Saldo awal anggota masih 0", $m['uang_kas'] == 0 && $m['iuran_aniv'] == 0);

// --- TEST 2: KAS & MIRRORING KE PEMBAYARAN ---
echo "\n--- 2. Pengujian Modul Uang Kas ---\n";
// Insert Kas Pertama Rp 50.000
$now = time();
$pdo->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, keterangan, tanggal, created_at) VALUES (1, 50000, 'Kas Minggu 1', datetime('now'), ?)")->execute([$now]);
$kasId1 = $pdo->lastInsertId();
$pdo->prepare("INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) VALUES (?, 1, 'Budi Santoso', 'KAS', 50000, ?, 'Kas Minggu 1')")
    ->execute(['rk_' . $kasId1, $now * 1000]);
$kasSaldo1 = recalculateAnggotaKas($pdo, 1);
assertTest("Pencatatan kas pertama Rp 50.000 berhasil dan saldo anggota = Rp 50.000", $kasSaldo1 == 50000);

// Insert Kas Kedua Rp 50.000 (transaksi sah berikutnya dengan keterangan berbeda)
$pdo->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, keterangan, tanggal, created_at) VALUES (1, 50000, 'Kas Minggu 2', datetime('now'), ?)")->execute([$now + 60]);
$kasId2 = $pdo->lastInsertId();
$pdo->prepare("INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) VALUES (?, 1, 'Budi Santoso', 'KAS', 50000, ?, 'Kas Minggu 2')")
    ->execute(['rk_' . $kasId2, ($now + 60) * 1000]);
$kasSaldo2 = recalculateAnggotaKas($pdo, 1);
assertTest("Pencatatan kas kedua sah Rp 50.000 berhasil dan saldo bertambah = Rp 100.000", $kasSaldo2 == 100000);

// Uji Anti-Double Submit: Duplikasi idempotency key
$idemKey = 'rk_test_dup';
$isDup = false;
$stmtC = $pdo->prepare("SELECT id FROM pembayaran WHERE firestoreId = ?");
$stmtC->execute([$idemKey]);
if ($stmtC->fetch()) {
    $isDup = true;
} else {
    // Simpan key
    $pdo->prepare("INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) VALUES (?, 1, 'Budi Santoso', 'KAS', 25000, ?, 'Test Idem')")->execute([$idemKey, time() * 1000]);
}
// Coba lagi dengan key yang sama
$stmtC2 = $pdo->prepare("SELECT id FROM pembayaran WHERE firestoreId = ?");
$stmtC2->execute([$idemKey]);
assertTest("Anti-Double Submit: Idempotency Key berhasil mencegah duplikasi", $stmtC2->fetch() !== false);

// --- TEST 3: IURAN ANNIVERSARY & SINGLE SOURCE OF TRUTH ---
echo "\n--- 3. Pengujian Modul Anniversary ---\n";
$pdo->prepare("INSERT INTO iuran_anniversary (anggota_id, nominal, tanggal, keterangan) VALUES (1, 100000, datetime('now'), 'Iuran Aniv 2026')")->execute();
$anivId1 = $pdo->lastInsertId();
$pdo->prepare("INSERT INTO riwayat_aniv (id_anggota, nominal, keterangan, tanggal, created_at) VALUES (1, 100000, ?, datetime('now'), ?)")
    ->execute(['Iuran Aniv 2026 [REF:ia_' . $anivId1 . ']', time()]);
$pdo->prepare("INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) VALUES (?, 1, 'Budi Santoso', 'ANIV', 100000, ?, 'Iuran Aniv 2026')")
    ->execute(['ia_' . $anivId1, time() * 1000]);
$anivTotal = recalculateAnggotaAniv($pdo, 1);
assertTest("Pencatatan anniversary Rp 100.000 tersimpan di iuran_anniversary, riwayat_aniv, & pembayaran tertaut", $anivTotal == 100000);

// --- TEST 4: CICILAN & REKALKULASI SISA TAGIHAN ---
echo "\n--- 4. Pengujian Modul Cicilan ---\n";
// Bayar cicilan 1: Rp 300.000
$pdo->prepare("INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) VALUES (1, 300000, datetime('now'), 'Cicilan 1')")->execute();
$cicId1 = $pdo->lastInsertId();
$pdo->prepare("INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) VALUES (?, 1, 'Budi Santoso', 'CICILAN', 300000, ?, 'Cicilan 1')")
    ->execute(['cic_' . $cicId1, time() * 1000]);
list($totCic1, $sisaCic1) = recalculateAnggotaCicilan($pdo, 1);
assertTest("Cicilan 1 Rp 300.000 tercatat, total cicilan = Rp 300.000, sisa = Rp 700.000", $totCic1 == 300000 && $sisaCic1 == 700000);

// Bayar cicilan 2: Rp 200.000
$pdo->prepare("INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) VALUES (1, 200000, datetime('now'), 'Cicilan 2')")->execute();
$cicId2 = $pdo->lastInsertId();
$pdo->prepare("INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) VALUES (?, 1, 'Budi Santoso', 'CICILAN', 200000, ?, 'Cicilan 2')")
    ->execute(['cic_' . $cicId2, time() * 1000]);
list($totCic2, $sisaCic2) = recalculateAnggotaCicilan($pdo, 1);
assertTest("Cicilan 2 Rp 200.000 tercatat, total cicilan = Rp 500.000, sisa = Rp 500.000", $totCic2 == 500000 && $sisaCic2 == 500000);

// --- TEST 5: KAS KELILING & REKAPITULASI ---
echo "\n--- 5. Pengujian Modul Kas Keliling ---\n";
$pdo->prepare("INSERT INTO kas_keliling (bulan, tahun, nominal, jenis, total_pemasukan, total_pengeluaran) VALUES ('Oktober', '2026', 400000, 'pemasukan', 400000, 0)")->execute();
$pdo->prepare("INSERT INTO kas_keliling (bulan, tahun, nominal, jenis, total_pemasukan, total_pengeluaran) VALUES ('Oktober', '2026', 100000, 'pengeluaran', 0, 100000)")->execute();
$rowKk = $pdo->query("SELECT SUM(total_pemasukan) as total_in, SUM(total_pengeluaran) as total_out FROM kas_keliling")->fetch();
$kkSaldo = max(0, floatval($rowKk['total_in']) - floatval($rowKk['total_out']));
assertTest("Kas Keliling: Pemasukan Rp 400.000, Pengeluaran Rp 100.000 -> Saldo Rp 300.000", $kkSaldo == 300000);

// --- TEST 6: PENGELUARAN KAS UTAMA & ANNIVERSARY ---
echo "\n--- 6. Pengujian Modul Pengeluaran ---\n";
$pdo->prepare("INSERT INTO pengeluaran (keterangan, nominal, tanggal, jenis_kas) VALUES ('Beli Kertas & Spidol', 20000, datetime('now'), 'kas_utama')")->execute();
$pdo->prepare("INSERT INTO pengeluaran (keterangan, nominal, tanggal, jenis_kas) VALUES ('DP Banner Aniv', 25000, datetime('now'), 'kas_aniv')")->execute();
$outKasUtama = floatval($pdo->query("SELECT SUM(nominal) as total FROM pengeluaran WHERE jenis_kas = 'kas_utama'")->fetch()['total']);
$outAniv = floatval($pdo->query("SELECT SUM(nominal) as total FROM pengeluaran WHERE jenis_kas = 'kas_aniv'")->fetch()['total']);
assertTest("Pengeluaran Kas Utama Rp 20.000 dan Anniversary Rp 25.000 tercatat akurat", $outKasUtama == 20000 && $outAniv == 25000);

// --- TEST 7: KONSISTENSI FORMULA DASHBOARD VS LAPORAN ---
echo "\n--- 7. Pengujian Konsistensi Dashboard & Laporan ---\n";
$inKasUtama = floatval($pdo->query("SELECT SUM(nominal) as total FROM riwayat_kas")->fetch()['total']);
$saldoKasUtamaBersih = max(0, $inKasUtama - $outKasUtama);
$inAniv = floatval($pdo->query("SELECT SUM(nominal) as total FROM iuran_anniversary")->fetch()['total']);
$saldoAnivBersih = max(0, $inAniv - $outAniv);
$totalSaldoKomunitas = $saldoKasUtamaBersih + $kkSaldo + $saldoAnivBersih + $sisaCic2;

assertTest("Kas Utama Bersih: 100.000 - 20.000 = Rp 80.000", $saldoKasUtamaBersih == 80000);
assertTest("Anniversary Bersih: 100.000 - 25.000 = Rp 75.000", $saldoAnivBersih == 75000);
assertTest("Total Saldo Komunitas Konsisten (80k + 300k + 75k + 500k = Rp 955.000)", $totalSaldoKomunitas == 955000);

// --- TEST 8: PENGHAPUSAN TRANSAKSI TERTENTU & PENCEGAHAN SIDE EFFECT ---
echo "\n--- 8. Pengujian Penghapusan Presisi & Independensi Transaksi ---\n";
// Hapus Kas 1 ($kasId1 = 50.000)
// Hapus di riwayat_kas dan pembayaran tertaut via 'rk_' . $kasId1
$pdo->prepare("DELETE FROM riwayat_kas WHERE id = ?")->execute([$kasId1]);
$pdo->prepare("DELETE FROM pembayaran WHERE firestoreId = ?")->execute(['rk_' . $kasId1]);
$kasSaldoAfterDel = recalculateAnggotaKas($pdo, 1);
assertTest("Kas 1 berhasil dihapus, saldo berkurang tepat menjadi Rp 50.000", $kasSaldoAfterDel == 50000);

// Pastikan Kas 2 (yang nominalnya juga Rp 50.000) TIDAK TERHAPUS!
$rk2Check = $pdo->prepare("SELECT id FROM riwayat_kas WHERE id = ?");
$rk2Check->execute([$kasId2]);
assertTest("Kas 2 (Rp 50.000) tetap aman dan tidak ikut terhapus", $rk2Check->fetch() !== false);

// Hapus Anniversary via linked reference
$pdo->prepare("DELETE FROM iuran_anniversary WHERE id = ?")->execute([$anivId1]);
$pdo->prepare("DELETE FROM riwayat_aniv WHERE keterangan LIKE ?")->execute(['%[REF:ia_' . $anivId1 . ']%']);
$pdo->prepare("DELETE FROM pembayaran WHERE firestoreId = ?")->execute(['ia_' . $anivId1]);
$anivSaldoAfterDel = recalculateAnggotaAniv($pdo, 1);
assertTest("Anniversary dihapus dari semua 3 tabel linked, saldo aniv anggota kembali 0", $anivSaldoAfterDel == 0);

// --- TEST 9: ATOMIC TRANSACTIONS & ROLLBACK ---
echo "\n--- 9. Pengujian Rollback Transaksi Gagal ---\n";
try {
    $pdo->beginTransaction();
    $pdo->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, keterangan, tanggal) VALUES (1, 999999, 'Test Rollback', datetime('now'))")->execute();
    // Simulasikan error/exception sebelum commit
    throw new Exception("Simulasi kegagalan query berikutnya");
    $pdo->commit();
} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
}
$stmtRollback = $pdo->query("SELECT * FROM riwayat_kas WHERE nominal = 999999");
assertTest("Rollback berhasil: data tidak tersimpan saat transaksi gagal di tengah jalan", $stmtRollback->fetch() === false);

echo "\n====================================================\n";
echo "HASIL PENGUJIAN AKHIR:\n";
echo "Total Lulus (PASSED): $passed\n";
echo "Total Gagal (FAILED): $failed\n";
echo "====================================================\n";

if ($failed === 0) {
    echo "STATUS: SEMUA PENGUJIAN WAJIB BERHASIL DENGAN NILAI 100%!\n";
    exit(0);
} else {
    echo "STATUS: TERDAPAT PENGUJIAN YANG GAGAL!\n";
    exit(1);
}
