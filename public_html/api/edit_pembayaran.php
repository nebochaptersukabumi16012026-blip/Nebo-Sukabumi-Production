<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

include_once 'config.php';
include_once 'sync_helper.php';

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->id) && isset($data->nominal_baru)) {
    try {
        $pdo->beginTransaction();

        // 1. Ambil data lama untuk hitung selisih
        $stmt_old = $pdo->prepare("SELECT * FROM pembayaran WHERE id = ?");
        $stmt_old->execute(array($data->id));
        $old_data = $stmt_old->fetch(PDO::FETCH_ASSOC);

        if (!$old_data) {
            echo json_encode(array("status" => "error", "message" => "Data transaksi tidak ditemukan"));
            exit();
        }

        $id_anggota = $old_data['anggotaId'];
        $jenis = strtoupper($old_data['jenisPembayaran']);
        $nominal_lama = floatval($old_data['nominal']);
        $nominal_baru = floatval($data->nominal_baru);
        $selisih = $nominal_baru - $nominal_lama;

        // 2. Update data di tabel utama pembayaran
        $stmt_upd = $pdo->prepare("UPDATE pembayaran SET nominal = ?, keterangan = ? WHERE id = ?");
        $keterangan = !empty($data->keterangan) ? $data->keterangan : ($old_data['keterangan'] . " (Koreksi)");
        $stmt_upd->execute(array($nominal_baru, $keterangan, $data->id));

        // 3. Update data di tabel sumber transaksi spesifik dan sinkronkan profil anggota
        if ($jenis == "KAS") {
            try {
                $stmt_rk = $pdo->prepare("UPDATE riwayat_kas SET nominal = ? WHERE id_anggota = ? AND nominal = ? LIMIT 1");
                $stmt_rk->execute(array($nominal_baru, $id_anggota, $nominal_lama));
            } catch (Exception $e) {}
            recalculateAnggotaKas($pdo, $id_anggota);
            
            // Update Dashboard Utama
            $stmt_dash = $pdo->prepare("UPDATE saldo_akumulasi SET total_akumulasi_masuk = total_akumulasi_masuk + ? WHERE jenis_kas = 'kas_utama'");
            $stmt_dash->execute(array($selisih));
        } 
        else if ($jenis == "ANIV") {
            try {
                $stmt_ia = $pdo->prepare("UPDATE iuran_anniversary SET nominal = ? WHERE anggota_id = ? AND nominal = ? LIMIT 1");
                $stmt_ia->execute(array($nominal_baru, $id_anggota, $nominal_lama));
            } catch (Exception $e) {}
            recalculateAnggotaAniv($pdo, $id_anggota);
            
            // Update Dashboard Utama
            $stmt_dash = $pdo->prepare("UPDATE saldo_akumulasi SET total_akumulasi_masuk = total_akumulasi_masuk + ? WHERE jenis_kas = 'kas_aniv'");
            $stmt_dash->execute(array($selisih));
        }
        else if ($jenis == "CICILAN") {
            try {
                $stmt_cic = $pdo->prepare("UPDATE cicilan SET nominal = ? WHERE anggota_id = ? AND nominal = ? LIMIT 1");
                $stmt_cic->execute(array($nominal_baru, $id_anggota, $nominal_lama));
            } catch (Exception $e) {}
            recalculateAnggotaCicilan($pdo, $id_anggota);
        }

        $pdo->commit();
        echo json_encode(array("status" => "success", "message" => "Transaksi berhasil dikoreksi. Selisih " . $selisih . " telah disesuaikan."));

    } catch (Exception $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo json_encode(array("status" => "error", "message" => "Gagal koreksi: " . $e->getMessage()));
    }
} else {
    echo json_encode(array("status" => "error", "message" => "Data tidak lengkap"));
}
