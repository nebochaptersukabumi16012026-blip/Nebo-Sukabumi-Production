<?php
// sync_helper.php

/**
 * Recalculate everything for a specific member
 */
function recalculateAllFields($conn, $anggota_id) {
    recalculateAnggotaAniv($conn, $anggota_id);
    recalculateAnggotaCicilan($conn, $anggota_id);
    recalculateAnggotaKas($conn, $anggota_id);
}

/**
 * Adjust manual saldo in community_settings (if used)
 */
function adjustManualSaldo($conn, $delta) {
    if ($delta == 0) return;
    try {
        $stmt = $conn->query("SELECT target_kas FROM community_settings LIMIT 1");
    } catch (PDOException $e) {
        error_log("ERROR_ADJUST_MANUAL_SALDO: " . $e->getMessage());
    }
}

/**
 * Recalculate Anniversary Contribution for a member
 * Single Source of Truth: tabel `iuran_anniversary`
 */
function recalculateAnggotaAniv($conn, $anggota_id) {
    if (!$conn || empty($anggota_id)) {
        return;
    }
    try {
        $stmt_sum_ia = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM iuran_anniversary WHERE anggota_id = ?");
        $stmt_sum_ia->execute(array($anggota_id));
        $row_ia = $stmt_sum_ia->fetch(PDO::FETCH_ASSOC);
        if ($row_ia === false) {
            error_log("ERROR_SINKRONISASI_ANIV: Gagal membaca total iuran_anniversary untuk anggota_id=" . $anggota_id);
            return;
        }
        $total_aniv = floatval($row_ia['total'] ?? 0);

        $stmt_update = $conn->prepare("UPDATE anggota SET iuran_aniv = ? WHERE id = ?");
        $stmt_update->execute(array($total_aniv, $anggota_id));
    } catch (Exception $e) {
        error_log("ERROR_SINKRONISASI_ANIV (anggota_id=" . $anggota_id . "): " . $e->getMessage());
    }
}

/**
 * Recalculate Installments for a member
 * Single Source of Truth: tabel `cicilan`
 */
function recalculateAnggotaCicilan($conn, $anggota_id) {
    if (!$conn || empty($anggota_id)) {
        return;
    }
    try {
        // 1. Hitung total yang sudah dibayar dari tabel cicilan (Single Source of Truth)
        $stmt_sum = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM cicilan WHERE anggota_id = ?");
        $stmt_sum->execute(array($anggota_id));
        $sum_row = $stmt_sum->fetch(PDO::FETCH_ASSOC);
        if ($sum_row === false) {
            error_log("ERROR_SINKRONISASI_CICILAN: Gagal membaca total cicilan untuk anggota_id=" . $anggota_id);
            return;
        }
        $total_dibayar = floatval($sum_row['total'] ?? 0);

        // 2. Baca harga_barang dari tabel anggota
        $stmt_harga = $conn->prepare("SELECT COALESCE(harga_barang, 0) as harga_barang FROM anggota WHERE id = ?");
        $stmt_harga->execute(array($anggota_id));
        $harga_row = $stmt_harga->fetch(PDO::FETCH_ASSOC);
        if ($harga_row === false) {
            error_log("ERROR_SINKRONISASI_CICILAN: Anggota tidak ditemukan untuk id=" . $anggota_id);
            return;
        }
        $harga_barang = floatval($harga_row['harga_barang'] ?? 0);

        // 3. Hitung sisa_cicilan: clamp minimal 0
        $sisa_cicilan = max(0.0, $harga_barang - $total_dibayar);

        // 4. Update data cicilan di tabel anggota (hanya gunakan nama kolom database yang benar)
        $stmt_update = $conn->prepare("UPDATE anggota SET total_cicilan = ?, sisa_cicilan = ? WHERE id = ?");
        $stmt_update->execute(array($total_dibayar, $sisa_cicilan, $anggota_id));
    } catch (Exception $e) {
        error_log("ERROR_SINKRONISASI_CICILAN (anggota_id=" . $anggota_id . "): " . $e->getMessage());
    }
}

/**
 * Recalculate Cash (Uang Kas) for a member
 * Single Source of Truth: tabel `riwayat_kas`
 */
function recalculateAnggotaKas($conn, $anggota_id) {
    if (!$conn || empty($anggota_id)) {
        return;
    }
    try {
        $stmt_sum_rk = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_kas WHERE id_anggota = ?");
        $stmt_sum_rk->execute(array($anggota_id));
        $row_rk = $stmt_sum_rk->fetch(PDO::FETCH_ASSOC);
        if ($row_rk === false) {
            error_log("ERROR_SINKRONISASI_KAS: Gagal membaca total riwayat_kas untuk id_anggota=" . $anggota_id);
            return;
        }
        $total_kas = floatval($row_rk['total'] ?? 0);

        $stmt_update = $conn->prepare("UPDATE anggota SET uang_kas = ? WHERE id = ?");
        $stmt_update->execute(array($total_kas, $anggota_id));
    } catch (Exception $e) {
        error_log("ERROR_SINKRONISASI_KAS (anggota_id=" . $anggota_id . "): " . $e->getMessage());
    }
}
?>
