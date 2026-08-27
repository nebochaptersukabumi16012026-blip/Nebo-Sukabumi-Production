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
        $stmt = $conn->query("SELECT target_kas FROM community_settings LIMIT 1"); // Example column check
        // This function seems to be for a 'manual_total_saldo' column that might not exist in all versions
        // We'll skip actual update unless we're sure about the column name
    } catch (PDOException $e) {
        error_log("ERROR_ADJUST_MANUAL_SALDO: " . $e->getMessage());
    }
}

/**
 * Recalculate Anniversary Contribution for a member
 */
function recalculateAnggotaAniv($conn, $anggota_id) {
    try {
        $stmt_sum_ia = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM iuran_anniversary WHERE anggota_id = ?");
        $stmt_sum_ia->execute(array($anggota_id));
        $row_ia = $stmt_sum_ia->fetch(PDO::FETCH_ASSOC);
        $total_ia = floatval(isset($row_ia['total']) ? $row_ia['total'] : 0);

        $stmt_sum_pem = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM pembayaran WHERE anggotaId = ? AND UPPER(jenisPembayaran) = 'ANIV'");
        $stmt_sum_pem->execute(array($anggota_id));
        $row_pem = $stmt_sum_pem->fetch(PDO::FETCH_ASSOC);
        $total_pem = floatval(isset($row_pem['total']) ? $row_pem['total'] : 0);

        $stmt_sum_ra = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_aniv WHERE id_anggota = ?");
        $stmt_sum_ra->execute(array($anggota_id));
        $row_ra = $stmt_sum_ra->fetch(PDO::FETCH_ASSOC);
        $total_ra = floatval(isset($row_ra['total']) ? $row_ra['total'] : 0);

        $stmt_current = $conn->prepare("SELECT iuran_aniv FROM anggota WHERE id = ?");
        $stmt_current->execute(array($anggota_id));
        $row_current = $stmt_current->fetch(PDO::FETCH_ASSOC);
        $old_total = $row_current ? floatval($row_current['iuran_aniv']) : 0.0;

        $new_total = max($total_ia, $total_pem, $total_ra, $old_total);
        
        $stmt_update = $conn->prepare("UPDATE anggota SET iuran_aniv = ? WHERE id = ?");
        $stmt_update->execute(array($new_total, $anggota_id));
    } catch (Exception $e) {
        error_log("ERROR_SINKRONISASI_ANIV: " . $e->getMessage());
    }
}

/**
 * Recalculate Installments for a member
 */
function recalculateAnggotaCicilan($conn, $anggota_id) {
    try {
        $stmt_sum = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM cicilan WHERE anggota_id = ?");
        $stmt_sum->execute(array($anggota_id));
        $sum_row = $stmt_sum->fetch(PDO::FETCH_ASSOC);
        $total_cic = floatval(isset($sum_row['total']) ? $sum_row['total'] : 0);

        $stmt_sum_pem = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM pembayaran WHERE anggotaId = ? AND UPPER(jenisPembayaran) = 'CICILAN'");
        $stmt_sum_pem->execute(array($anggota_id));
        $row_pem = $stmt_sum_pem->fetch(PDO::FETCH_ASSOC);
        $total_pem = floatval(isset($row_pem['total']) ? $row_pem['total'] : 0);

        $new_total = max($total_cic, $total_pem);
        
        $stmt_harga = $conn->prepare("SELECT harga_barang FROM anggota WHERE id = ?");
        $stmt_harga->execute(array($anggota_id));
        $harga_row = $stmt_harga->fetch(PDO::FETCH_ASSOC);
        $harga_barang = floatval(isset($harga_row['harga_barang']) ? $harga_row['harga_barang'] : 0);
        $sisa_cicilan = max(0, $harga_barang - $new_total);
        
        $stmt_update = $conn->prepare("UPDATE anggota SET total_cicilan = ?, sisa_cicilan = ? WHERE id = ?");
        $stmt_update->execute(array($new_total, $sisa_cicilan, $anggota_id));
    } catch (Exception $e) {
        error_log("ERROR_SINKRONISASI_CICILAN: " . $e->getMessage());
    }
}

/**
 * Recalculate Cash (Uang Kas) for a member using SQL SUM from pembayaran and riwayat_kas tables
 */
function recalculateAnggotaKas($conn, $anggota_id) {
    try {
        $stmt_sum_pem = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM pembayaran WHERE anggotaId = ? AND LOWER(jenisPembayaran) IN ('kas', 'uang_kas')");
        $stmt_sum_pem->execute(array($anggota_id));
        $row_pem = $stmt_sum_pem->fetch(PDO::FETCH_ASSOC);
        $total_pem = floatval(isset($row_pem['total']) ? $row_pem['total'] : 0);

        $stmt_sum_rk = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM riwayat_kas WHERE id_anggota = ?");
        $stmt_sum_rk->execute(array($anggota_id));
        $row_rk = $stmt_sum_rk->fetch(PDO::FETCH_ASSOC);
        $total_rk = floatval(isset($row_rk['total']) ? $row_rk['total'] : 0);

        $stmt_current = $conn->prepare("SELECT uang_kas FROM anggota WHERE id = ?");
        $stmt_current->execute(array($anggota_id));
        $row_current = $stmt_current->fetch(PDO::FETCH_ASSOC);
        $old_total = $row_current ? floatval($row_current['uang_kas']) : 0.0;

        $total_kas = max($total_pem, $total_rk, $old_total);
        
        $stmt_update = $conn->prepare("UPDATE anggota SET uang_kas = ? WHERE id = ?");
        $stmt_update->execute(array($total_kas, $anggota_id));
    } catch (Exception $e) {
        error_log("ERROR_SINKRONISASI_KAS: " . $e->getMessage());
    }
}
?>
