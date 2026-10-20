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
            $stmt->execute(array($_GET['id']));
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } elseif (isset($_GET['anggota_id'])) {
            $stmt = $conn->prepare("SELECT * FROM pembayaran WHERE anggotaId = ? ORDER BY tanggal DESC");
            $stmt->execute(array($_GET['anggota_id']));
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM pembayaran ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(array("status" => "success", "data" => $result));
        break;

    case 'POST':
        if (!empty($data->anggotaId) && isset($data->nominal) && !empty($data->jenisPembayaran)) {
            $user_role = '';
            if (isset($data->role)) $user_role = strtoupper(trim($data->role));
            elseif (isset($data->user_role)) $user_role = strtoupper(trim($data->user_role));

            if (strtoupper($data->jenisPembayaran) === 'CICILAN' && ($user_role === 'ANGGOTA' || $user_role === 'GUEST')) {
                http_response_code(403);
                echo json_encode(array('status' => 'error', 'message' => 'Akses ditolak: Anggota/Guest tidak diizinkan membayar cicilan'));
                exit();
            }

            try {
                $conn->beginTransaction();
                $tanggalTs = isset($data->tanggal) ? $data->tanggal : (time() * 1000);
                $formattedDate = date('Y-m-d H:i:s', intval($tanggalTs / 1000));
                $keteranganStr = isset($data->keterangan) ? $data->keterangan : '';

                $query = "INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) VALUES (?, ?, ?, ?, ?, ?, ?)";
                $stmt = $conn->prepare($query);
                $stmt->execute(array(
                    $data->anggotaId,
                    isset($data->anggotaNama) ? $data->anggotaNama : '',
                    $data->jenisPembayaran,
                    $data->nominal,
                    $tanggalTs,
                    $keteranganStr,
                    isset($data->buktiPembayaran) ? $data->buktiPembayaran : null
                ));
                $insertedId = $conn->lastInsertId();
                
                if (strtoupper($data->jenisPembayaran) == 'KAS') {
                    $conn->prepare("
                        INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk) 
                        VALUES ('kas_utama', ?) 
                        ON DUPLICATE KEY UPDATE total_akumulasi_masuk = total_akumulasi_masuk + ?
                    ")->execute(array($data->nominal, $data->nominal));

                    $conn->prepare("UPDATE anggota SET uang_kas = uang_kas + ? WHERE id = ?")->execute(array($data->nominal, $data->anggotaId));

                    $stmt_rk = $conn->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)");
                    $stmt_rk->execute(array(
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Iuran Kas'
                    ));
                } elseif (strtoupper($data->jenisPembayaran) == 'ANIV') {
                    $conn->prepare("
                        INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk) 
                        VALUES ('kas_aniv', ?) 
                        ON DUPLICATE KEY UPDATE total_akumulasi_masuk = total_akumulasi_masuk + ?
                    ")->execute(array($data->nominal, $data->nominal));

                    $conn->prepare("UPDATE anggota SET iuran_aniv = iuran_aniv + ? WHERE id = ?")->execute(array($data->nominal, $data->anggotaId));

                    $stmt_ra = $conn->prepare("INSERT INTO riwayat_aniv (id_anggota, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)");
                    $stmt_ra->execute(array(
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Iuran Anniversary'
                    ));

                    $stmt_ia = $conn->prepare("INSERT INTO iuran_anniversary (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)");
                    $stmt_ia->execute(array(
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Iuran Anniversary'
                    ));
                } elseif (strtoupper($data->jenisPembayaran) == 'CICILAN') {
                    $stmt_cic = $conn->prepare("INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)");
                    $stmt_cic->execute(array(
                        $data->anggotaId,
                        $data->nominal,
                        $formattedDate,
                        $keteranganStr ?: 'Cicilan'
                    ));

                    recalculateAnggotaCicilan($conn, $data->anggotaId);
                }
                
                $conn->commit();
                echo json_encode(array("status" => "success", "message" => "Pembayaran berhasil ditambahkan", "id" => $insertedId));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal tambah pembayaran: " . $e->getMessage()));
            }
        }
        break;

    case 'DELETE':
        if (!empty($data->id)) {
            try {
                $conn->beginTransaction();
                $stmt_get = $conn->prepare("SELECT anggotaId, jenisPembayaran, nominal FROM pembayaran WHERE id = ?");
                $stmt_get->execute(array($data->id));
                $row_p = $stmt_get->fetch(PDO::FETCH_ASSOC);

                $stmt = $conn->prepare("DELETE FROM pembayaran WHERE id = ?");
                $stmt->execute(array($data->id));

                if ($row_p && !empty($row_p['anggotaId'])) {
                    $aid = $row_p['anggotaId'];
                    $jp = strtoupper(trim($row_p['jenisPembayaran'] ?? ''));
                    if ($jp === 'CICILAN') {
                        $stmt_del_c = $conn->prepare("DELETE FROM cicilan WHERE anggota_id = ? AND nominal = ? LIMIT 1");
                        $stmt_del_c->execute(array($aid, $row_p['nominal']));
                        recalculateAnggotaCicilan($conn, $aid);
                    } elseif ($jp === 'KAS') {
                        $stmt_del_rk = $conn->prepare("DELETE FROM riwayat_kas WHERE id_anggota = ? AND nominal = ? LIMIT 1");
                        $stmt_del_rk->execute(array($aid, $row_p['nominal']));
                        recalculateAnggotaKas($conn, $aid);
                    } elseif ($jp === 'ANIV') {
                        $stmt_del_ia = $conn->prepare("DELETE FROM iuran_anniversary WHERE anggota_id = ? AND nominal = ? LIMIT 1");
                        $stmt_del_ia->execute(array($aid, $row_p['nominal']));
                        recalculateAnggotaAniv($conn, $aid);
                    }
                }

                $conn->commit();
                echo json_encode(array("status" => "success", "message" => "Pembayaran berhasil dihapus"));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal hapus pembayaran: " . $e->getMessage()));
            }
        }
        break;

    default:
        http_response_code(405);
        echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
        break;
}
?>
