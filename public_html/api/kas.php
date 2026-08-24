<?php
header('Content-Type: application/json');
include_once 'config.php';

$data = json_decode(file_get_contents("php://input"));
$method = $_SERVER['REQUEST_METHOD'];

if ($method == 'POST') {
    // Aksi Reset Kas Anggota (Sesuai Instruksi)
    if (isset($data->action) && $data->action == 'reset_all') {
        try {
            $conn->query("UPDATE anggota SET uang_kas = 0");
            $conn->query("DELETE FROM pembayaran WHERE jenisPembayaran = 'KAS'");
            $conn->query("DELETE FROM kas_keliling WHERE jenis_transaksi = 'Pemasukan'");
            
            $stmt_sum = $conn->query("SELECT COALESCE(SUM(uang_kas), 0) AS total_pemasukan FROM anggota");
            $total_pemasukan = (int)($stmt_sum->fetch(PDO::FETCH_ASSOC)['total_pemasukan'] ?? 0);
            
            $stmt_peng = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_pengeluaran FROM pengeluaran");
            $total_pengeluaran = (int)($stmt_peng->fetch(PDO::FETCH_ASSOC)['total_pengeluaran'] ?? 0);
            
            echo json_encode([
                "status" => "success",
                "message" => "Seluruh iuran kas anggota telah di-reset menjadi 0.",
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => ($total_pemasukan - $total_pengeluaran)
            ]);
        } catch (PDOException $e) {
            echo json_encode(["status" => "error", "message" => "Gagal reset: " . $e->getMessage()]);
        }
    } elseif (isset($data->action) && ($data->action == 'reset_member' || $data->action == 'delete') && isset($data->id)) {
        try {
            $targetId = (int)$data->id;
            
            $stmt_get = $conn->prepare("SELECT nama, nra, uang_kas FROM anggota WHERE id = ?");
            $stmt_get->execute([$targetId]);
            $member = $stmt_get->fetch(PDO::FETCH_ASSOC);
            $nama = $member['nama'] ?? '';
            $nra = $member['nra'] ?? '';
            $nominal = (int)($member['uang_kas'] ?? 0);
            
            $stmt = $conn->prepare("UPDATE anggota SET uang_kas = 0 WHERE id = ?");
            $stmt->execute([$targetId]);
            
            // Delete payments for this member
            $stmt_del = $conn->prepare("DELETE FROM pembayaran WHERE anggotaId = ? AND jenisPembayaran = 'KAS'");
            $stmt_del->execute([$targetId]);

            // Also delete from kas_keliling history for this member
            if (!empty($nra)) {
                $stmt_del_kk = $conn->prepare("DELETE FROM kas_keliling WHERE nra = ? AND jenis_transaksi = 'Pemasukan'");
                $stmt_del_kk->execute([$nra]);
            }
            
            $stmt_sum = $conn->query("SELECT COALESCE(SUM(uang_kas), 0) AS total_pemasukan FROM anggota");
            $total_pemasukan = (int)($stmt_sum->fetch(PDO::FETCH_ASSOC)['total_pemasukan'] ?? 0);
            
            $stmt_peng = $conn->query("SELECT COALESCE(SUM(nominal), 0) AS total_pengeluaran FROM pengeluaran");
            $total_pengeluaran = (int)($stmt_peng->fetch(PDO::FETCH_ASSOC)['total_pengeluaran'] ?? 0);
            
            echo json_encode([
                "status" => "success",
                "message" => "Iuran kas milik $nama sebesar Rp " . number_format($nominal, 0, ',', '.') . " berhasil di-reset menjadi 0.",
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => ($total_pemasukan - $total_pengeluaran)
            ]);
        } catch (PDOException $e) {
            echo json_encode(["status" => "error", "message" => "Gagal reset member: " . $e->getMessage()]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "Action tidak valid"]);
    }
} else {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
}
?>
