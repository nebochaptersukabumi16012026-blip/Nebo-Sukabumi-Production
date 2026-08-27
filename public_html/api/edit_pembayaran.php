<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

include_once 'config.php';

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

        // 3. Update data di tabel riwayat spesifik (jika ada)
        if ($jenis == "KAS") {
            $stmt_rk = $pdo->prepare("UPDATE riwayat_kas SET nominal = ? WHERE id_anggota = ? AND nominal = ? AND created_at = ? LIMIT 1");
            // Catatan: Pencocokan riwayat_kas mungkin butuh ID unik jika ada, tapi di sini kita pakai kombinasi data yang ada
            // Namun cara paling aman adalah merefleksikan selisih ke akumulasi saja.
        }

        // 4. UPDATE AKUMULASI ANGGOTA (Koreksi Presisi)
        if ($jenis == "KAS") {
            $stmt_agg = $pdo->prepare("UPDATE anggota SET uang_kas = uang_kas + ? WHERE id = ?");
            $stmt_agg->execute(array($selisih, $id_anggota));
            
            // Update Dashboard Utama
            $stmt_dash = $pdo->prepare("UPDATE saldo_akumulasi SET total_akumulasi_masuk = total_akumulasi_masuk + ? WHERE jenis_kas = 'kas_utama'");
            $stmt_dash->execute(array($selisih));
        } 
        else if ($jenis == "ANIV") {
            $stmt_agg = $pdo->prepare("UPDATE anggota SET iuran_aniv = iuran_aniv + ? WHERE id = ?");
            $stmt_agg->execute(array($selisih, $id_anggota));
            
            // Update Dashboard Utama
            $stmt_dash = $pdo->prepare("UPDATE saldo_akumulasi SET total_akumulasi_masuk = total_akumulasi_masuk + ? WHERE jenis_kas = 'kas_aniv'");
            $stmt_dash->execute(array($selisih));
        }
        else if ($jenis == "CICILAN") {
            $stmt_agg = $pdo->prepare("UPDATE anggota SET total_cicilan = total_cicilan + ?, sisa_cicilan = sisa_cicilan - ? WHERE id = ?");
            $stmt_agg->execute(array($selisih, $selisih, $id_anggota));
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
