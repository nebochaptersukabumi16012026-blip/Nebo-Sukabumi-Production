<?php
// cicilan.php
require_once 'config.php';
require_once 'sync_helper.php';

$data = json_decode(file_get_contents("php://input"));
$method = $_SERVER['REQUEST_METHOD'];

$user_role = '';
if (isset($data->role)) $user_role = strtoupper(trim($data->role));
elseif (isset($data->user_role)) $user_role = strtoupper(trim($data->user_role));
elseif (isset($_GET['role'])) $user_role = strtoupper(trim($_GET['role']));
elseif (isset($_GET['user_role'])) $user_role = strtoupper(trim($_GET['user_role']));

if ($method === 'POST' || $method === 'PUT' || $method === 'DELETE') {
    if ($user_role === 'ANGGOTA' || $user_role === 'GUEST') {
        http_response_code(403);
        echo json_encode(array(
            'status' => 'error',
            'message' => 'Akses ditolak: Anggota/Guest tidak diizinkan menghapus data'
        ));
        exit();
    }
}

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM cicilan WHERE id = ?");
            $stmt->execute(array($_GET['id']));
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM cicilan ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(array("status" => "success", "data" => $result));
        break;

    case 'POST':
        $anggota_id = !empty($data->anggota_id) ? intval($data->anggota_id) : (!empty($data->id_anggota) ? intval($data->id_anggota) : intval($data->anggotaId ?? 0));
        $nominal = isset($data->nominal) ? floatval($data->nominal) : 0.0;
        
        if ($anggota_id > 0 && $nominal > 0) {
            try {
                $conn->beginTransaction();
                
                $tgl = date('Y-m-d');
                if (!empty($data->tanggal)) {
                    if (is_numeric($data->tanggal)) {
                        $ts = intval($data->tanggal);
                        if ($ts > 9999999999) $ts = intval($ts / 1000);
                        $tgl = date('Y-m-d', $ts);
                    } else {
                        $tgl = date('Y-m-d', strtotime($data->tanggal));
                    }
                }
                $keterangan = !empty($data->keterangan) ? $data->keterangan : 'Pembayaran Cicilan';

                $query = "INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)";
                $stmt = $conn->prepare($query);
                $stmt->execute(array(
                    $anggota_id,
                    $nominal,
                    $tgl,
                    $keterangan
                ));
                $insertedId = $conn->lastInsertId();
                
                recalculateAnggotaCicilan($conn, $anggota_id);
                $conn->commit();
                
                echo json_encode(array(
                    "status" => "success", 
                    "message" => "Cicilan berhasil ditambahkan",
                    "id" => intval($insertedId),
                    "anggota_id" => $anggota_id,
                    "nominal" => $nominal
                ));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal tambah cicilan: " . $e->getMessage()));
            }
        } else {
            http_response_code(400);
            echo json_encode(array("status" => "error", "message" => "anggota_id dan nominal wajib diisi"));
        }
        break;

    case 'PUT':
        $cid = intval($data->id ?? 0);
        $aid = !empty($data->anggota_id) ? intval($data->anggota_id) : (!empty($data->id_anggota) ? intval($data->id_anggota) : intval($data->anggotaId ?? 0));
        $nominal = isset($data->nominal) ? floatval($data->nominal) : 0.0;

        if ($cid > 0 && $aid <= 0) {
            $stmt_find = $conn->prepare("SELECT anggota_id FROM cicilan WHERE id = ?");
            $stmt_find->execute(array($cid));
            $aid = intval($stmt_find->fetchColumn() ?: 0);
        }

        if ($cid > 0 && $aid > 0 && $nominal > 0) {
            try {
                $conn->beginTransaction();

                $tgl = date('Y-m-d');
                if (!empty($data->tanggal)) {
                    if (is_numeric($data->tanggal)) {
                        $ts = intval($data->tanggal);
                        if ($ts > 9999999999) $ts = intval($ts / 1000);
                        $tgl = date('Y-m-d', $ts);
                    } else {
                        $tgl = date('Y-m-d', strtotime($data->tanggal));
                    }
                }
                $keterangan = !empty($data->keterangan) ? $data->keterangan : 'Pembayaran Cicilan';

                $query = "UPDATE cicilan SET anggota_id=?, nominal=?, tanggal=?, keterangan=? WHERE id=?";
                $stmt = $conn->prepare($query);
                $stmt->execute(array(
                    $aid,
                    $nominal,
                    $tgl,
                    $keterangan,
                    $cid
                ));
                
                recalculateAnggotaCicilan($conn, $aid);
                $conn->commit();
                
                echo json_encode(array(
                    "status" => "success", 
                    "message" => "Cicilan berhasil diupdate",
                    "id" => $cid,
                    "anggota_id" => $aid,
                    "nominal" => $nominal
                ));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal update cicilan: " . $e->getMessage()));
            }
        } else {
            http_response_code(400);
            echo json_encode(array("status" => "error", "message" => "ID cicilan, anggota_id, dan nominal tidak valid"));
        }
        break;

    case 'DELETE':
        if (!empty($data->id)) {
            try {
                $conn->beginTransaction();
                $stmt_get = $conn->prepare("SELECT anggota_id FROM cicilan WHERE id = ?");
                $stmt_get->execute(array($data->id));
                $row = $stmt_get->fetch(PDO::FETCH_ASSOC);
                
                if ($row) {
                    $anggota_id = $row['anggota_id'];
                    $stmt = $conn->prepare("DELETE FROM cicilan WHERE id = ?");
                    $stmt->execute(array($data->id));
                    
                    recalculateAnggotaCicilan($conn, $anggota_id);
                    $conn->commit();
                    
                    echo json_encode(array("status" => "success", "message" => "Riwayat berhasil dihapus"));
                } else {
                    $conn->rollBack();
                    echo json_encode(array("status" => "error", "message" => "Data tidak ditemukan"));
                }
            } catch (Exception $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                echo json_encode(array("status" => "error", "message" => $e->getMessage()));
            }
        }
        break;

    default:
        http_response_code(405);
        echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
        break;
}
?>
