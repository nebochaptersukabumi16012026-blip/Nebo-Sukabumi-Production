<?php
// pembayaran.php
require_once 'config.php';
require_once 'sync_helper.php';

$data = json_decode(file_get_contents("php://input"));
$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM pembayaran WHERE id = ?");
            $stmt->execute([$_GET['id']]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } elseif (isset($_GET['anggota_id'])) {
            $stmt = $conn->prepare("SELECT * FROM pembayaran WHERE anggotaId = ? ORDER BY tanggal DESC");
            $stmt->execute([$_GET['anggota_id']]);
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM pembayaran ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(["status" => "success", "data" => $result]);
        break;

    case 'POST':
        if (!empty($data->anggotaId) && isset($data->nominal) && !empty($data->jenisPembayaran)) {
            $tanggalTs = $data->tanggal ?? (time() * 1000);
            $formattedDate = date('Y-m-d H:i:s', intval($tanggalTs / 1000));
            $keteranganStr = $data->keterangan ?? '';

            $query = "INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) VALUES (?, ?, ?, ?, ?, ?, ?)";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                $data->anggotaId,
                $data->anggotaNama ?? '',
                $data->jenisPembayaran,
                $data->nominal,
                $tanggalTs,
                $keteranganStr,
                $data->buktiPembayaran ?? null
            ]);
            $insertedId = $conn->lastInsertId();
            
            if (strtoupper($data->jenisPembayaran) == 'KAS') {
                try {
                    $stmt_rk = $conn->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, tanggal, keterangan, created_at) VALUES (?, ?, ?, ?, ?)");
                    $stmt_rk->execute([
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Iuran Kas',
                        $tanggalTs
                    ]);
                } catch (Exception $e) {}

                recalculateAnggotaKas($conn, $data->anggotaId);
                
                // Also insert into kas_keliling for transaction history as requested
                try {
                    $stmt_nra = $conn->prepare("SELECT nra FROM anggota WHERE id = ?");
                    $stmt_nra->execute([$data->anggotaId]);
                    $nra = $stmt_nra->fetch(PDO::FETCH_ASSOC)['nra'] ?? '';
                    
                    $bulan = date("F", $tanggalTs / 1000);
                    $tahun = date("Y", $tanggalTs / 1000);
                    
                    $stmt_kk = $conn->prepare("INSERT INTO kas_keliling (nra, nominal, tanggal, keterangan, jenis_transaksi, bulan, tahun, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    $stmt_kk->execute([
                        $nra,
                        $data->nominal,
                        $tanggalTs,
                        $keteranganStr ?: 'Iuran Kas',
                        'Pemasukan',
                        $bulan,
                        $tahun,
                        $data->anggotaNama ?? 'System',
                        time() * 1000
                    ]);
                } catch (Exception $e) {
                    error_log("Error mirroring to kas_keliling: " . $e->getMessage());
                }
            } elseif (strtoupper($data->jenisPembayaran) == 'ANIV') {
                try {
                    $stmt_ra = $conn->prepare("INSERT INTO riwayat_aniv (id_anggota, nominal, tanggal, keterangan, created_at) VALUES (?, ?, ?, ?, ?)");
                    $stmt_ra->execute([
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Iuran Anniversary',
                        $tanggalTs
                    ]);
                } catch (Exception $e) {}

                try {
                    $stmt_ia = $conn->prepare("INSERT INTO iuran_anniversary (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)");
                    $stmt_ia->execute([
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Iuran Anniversary'
                    ]);
                } catch (Exception $e) {}

                recalculateAnggotaAniv($conn, $data->anggotaId);
            } elseif (strtoupper($data->jenisPembayaran) == 'CICILAN') {
                try {
                    $stmt_cic = $conn->prepare("INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)");
                    $stmt_cic->execute([
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Cicilan'
                    ]);
                } catch (Exception $e) {}

                recalculateAnggotaCicilan($conn, $data->anggotaId);
            }
            
            echo json_encode(["status" => "success", "message" => "Pembayaran berhasil ditambahkan", "id" => $insertedId]);
        }
        break;

    case 'DELETE':
        if (!empty($data->id)) {
            $stmt_get = $conn->prepare("SELECT anggotaId, jenisPembayaran FROM pembayaran WHERE id = ?");
            $stmt_get->execute([$data->id]);
            $row = $stmt_get->fetch(PDO::FETCH_ASSOC);
            
            if ($row) {
                $anggota_id = $row['anggotaId'];
                $jenis = $row['jenisPembayaran'];
                $stmt = $conn->prepare("DELETE FROM pembayaran WHERE id = ?");
                $stmt->execute([$data->id]);
                
                if ($jenis == 'KAS') {
                    recalculateAnggotaKas($conn, $anggota_id);
                }
                
                echo json_encode(["status" => "success", "message" => "Pembayaran berhasil dihapus"]);
            }
        }
        break;

    default:
        http_response_code(405);
        echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
        break;
}
?>
