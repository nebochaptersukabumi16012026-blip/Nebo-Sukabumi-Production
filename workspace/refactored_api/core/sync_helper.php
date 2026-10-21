<?php
// workspace/refactored_api/core/sync_helper.php
// Modul Rekalkulasi Terpadu & Single Source of Truth untuk Anggota

/**
 * Hitung ulang seluruh saldo anggota (Kas, Anniversary, Cicilan)
 */
function recalculateAllFields($conn, $anggota_id) {
    if (!$conn || empty($anggota_id)) return;
    recalculateAnggotaAniv($conn, $anggota_id);
    recalculateAnggotaCicilan($conn, $anggota_id);
    recalculateAnggotaKas($conn, $anggota_id);
}

/**
 * Rekalkulasi Iuran Anniversary untuk Anggota
 * Menggabungkan tabel iuran_anniversary & riwayat_aniv secara konsisten
 */
function recalculateAnggotaAniv($conn, $anggota_id) {
    if (!$conn || empty($anggota_id)) return 0.0;
    try {
        // 1. Prioritaskan tabel iuran_anniversary
        $total_aniv = 0.0;
        try {
            $stmt = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM iuran_anniversary WHERE anggota_id = ?");
            $stmt->execute([$anggota_id]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            $total_aniv = floatval($row['total'] ?? 0);
        } catch (Exception $e) {
            // Fallback ke riwayat_aniv jika tabel iuran_anniversary tidak ditemukan
            $stmt = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_aniv WHERE id_anggota = ?");
            $stmt->execute([$anggota_id]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            $total_aniv = floatval($row['total'] ?? 0);
        }

        // 2. Update field iuran_aniv & iuranAniv di tabel anggota
        try {
            $upd = $conn->prepare("UPDATE anggota SET iuran_aniv = ?, iuranAniv = ? WHERE id = ?");
            $upd->execute([$total_aniv, $total_aniv, $anggota_id]);
        } catch (Exception $e) {
            $upd = $conn->prepare("UPDATE anggota SET iuran_aniv = ? WHERE id = ?");
            $upd->execute([$total_aniv, $anggota_id]);
        }

        return $total_aniv;
    } catch (Exception $e) {
        error_log("ERROR_RECALCULATE_ANIV (id=$anggota_id): " . $e->getMessage());
        return 0.0;
    }
}

/**
 * Rekalkulasi Cicilan Anggota
 * Mengambil total dibayar dari tabel cicilan dan menghitung sisa tagihan
 */
function recalculateAnggotaCicilan($conn, $anggota_id) {
    if (!$conn || empty($anggota_id)) return [0.0, 0.0];
    try {
        // 1. Total cicilan terbayar
        $stmt = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM cicilan WHERE anggota_id = ?");
        $stmt->execute([$anggota_id]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        $total_dibayar = floatval($row['total'] ?? 0);

        // 2. Ambil harga barang / tagihan
        $stmt_m = $conn->prepare("SELECT COALESCE(harga_barang, hargaBarang, 0) as harga_barang FROM anggota WHERE id = ?");
        $stmt_m->execute([$anggota_id]);
        $m_row = $stmt_m->fetch(PDO::FETCH_ASSOC);
        $harga_barang = floatval($m_row['harga_barang'] ?? 0);

        // 3. Sisa cicilan (clamp minimal 0)
        $sisa_cicilan = max(0.0, $harga_barang - $total_dibayar);

        // 4. Update tabel anggota
        try {
            $upd = $conn->prepare("UPDATE anggota SET total_cicilan = ?, sisa_cicilan = ?, sisaCicilan = ? WHERE id = ?");
            $upd->execute([$total_dibayar, $sisa_cicilan, $sisa_cicilan, $anggota_id]);
        } catch (Exception $e) {
            $upd = $conn->prepare("UPDATE anggota SET total_cicilan = ?, sisa_cicilan = ? WHERE id = ?");
            $upd->execute([$total_dibayar, $sisa_cicilan, $anggota_id]);
        }

        return [$total_dibayar, $sisa_cicilan];
    } catch (Exception $e) {
        error_log("ERROR_RECALCULATE_CICILAN (id=$anggota_id): " . $e->getMessage());
        return [0.0, 0.0];
    }
}

/**
 * Rekalkulasi Kas Anggota
 * Mengambil total dari tabel riwayat_kas dan memperbarui uang_kas pada tabel anggota
 */
function recalculateAnggotaKas($conn, $anggota_id) {
    if (!$conn || empty($anggota_id)) return 0.0;
    try {
        $stmt = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_kas WHERE id_anggota = ?");
        $stmt->execute([$anggota_id]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        $total_kas = floatval($row['total'] ?? 0);

        try {
            $upd = $conn->prepare("UPDATE anggota SET uang_kas = ?, uangKas = ? WHERE id = ?");
            $upd->execute([$total_kas, $total_kas, $anggota_id]);
        } catch (Exception $e) {
            $upd = $conn->prepare("UPDATE anggota SET uang_kas = ? WHERE id = ?");
            $upd->execute([$total_kas, $anggota_id]);
        }

        return $total_kas;
    } catch (Exception $e) {
        error_log("ERROR_RECALCULATE_KAS (id=$anggota_id): " . $e->getMessage());
        return 0.0;
    }
}

/**
 * Sinkronisasi Master Ledger saldo_akumulasi dengan Single Source of Truth
 */
function syncMasterLedger($conn, $jenis = null) {
    if (!$conn) return;
    try {
        if ($jenis === null || $jenis === 'kas_utama') {
            $st_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total FROM riwayat_kas");
            $in_val = floatval($st_in ? $st_in->fetch(PDO::FETCH_ASSOC)['total'] : 0);
            $st_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total FROM pengeluaran WHERE LOWER(jenis_kas) IN ('kas_utama', 'kas utama', 'kas', 'saldo kas', 'uang kas') OR (LOWER(jenis_kas) NOT LIKE '%keliling%' AND LOWER(jenis_kas) NOT LIKE '%aniv%')");
            $out_val = floatval($st_out ? $st_out->fetch(PDO::FETCH_ASSOC)['total'] : 0);
            $upd = $conn->prepare("INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) VALUES ('kas_utama', ?, ?) ON DUPLICATE KEY UPDATE total_akumulasi_masuk = ?, total_akumulasi_keluar = ?");
            $upd->execute([$in_val, $out_val, $in_val, $out_val]);
        }
        if ($jenis === null || $jenis === 'kas_aniv') {
            $st_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total FROM iuran_anniversary");
            $in_val = floatval($st_in ? $st_in->fetch(PDO::FETCH_ASSOC)['total'] : 0);
            $st_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total FROM pengeluaran WHERE LOWER(jenis_kas) LIKE '%aniv%' OR LOWER(jenis_kas) LIKE '%anniversary%'");
            $out_val = floatval($st_out ? $st_out->fetch(PDO::FETCH_ASSOC)['total'] : 0);
            $upd = $conn->prepare("INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) VALUES ('kas_aniv', ?, ?) ON DUPLICATE KEY UPDATE total_akumulasi_masuk = ?, total_akumulasi_keluar = ?");
            $upd->execute([$in_val, $out_val, $in_val, $out_val]);
        }
    } catch (Exception $e) {
        // Abaikan jika tabel saldo_akumulasi tidak tersedia di hosting
    }
}
