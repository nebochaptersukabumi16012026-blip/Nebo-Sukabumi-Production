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
        if (!empty($data->anggota_id) && isset($data->nominal)) {
            try {
                $conn->beginTransaction();
                $query = "INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)";
                $stmt = $conn->prepare($query);
                $stmt->execute(array(
                    $data->anggota_id,
                    $data->nominal,
                    isset($data->tanggal) ? $data->tanggal : date('Y-m-d'),
                    isset($data->keterangan) ? $data->keterangan : ''
                ));
                
                recalculateAnggotaCicilan($conn, $data->anggota_id);
                $conn->commit();
                
                echo json_encode(array("status" => "success", "message" => "Cicilan berhasil ditambahkan"));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal tambah cicilan: " . $e->getMessage()));
            }
        }
        break;

    case 'PUT':
        if (!empty($data->id) && !empty($data->anggota_id) && isset($data->nominal)) {
            try {
                $conn->beginTransaction();
                $query = "UPDATE cicilan SET anggota_id=?, nominal=?, tanggal=?, keterangan=? WHERE id=?";
                $stmt = $conn->prepare($query);
                $stmt->execute(array(
                    $data->anggota_id,
                    $data->nominal,
                    $data->tanggal,
                    $data->keterangan,
                    $data->id
                ));
                
                recalculateAnggotaCicilan($conn, $data->anggota_id);
                $conn->commit();
                
                echo json_encode(array("status" => "success", "message" => "Cicilan berhasil diupdate"));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal update cicilan: " . $e->getMessage()));
            }
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
                    
                    // JANGAN sentuh saldo_akumulasi / dashboard utama
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
