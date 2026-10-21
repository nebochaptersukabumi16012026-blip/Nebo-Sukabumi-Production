<?php
// iuran_anniversary.php
require_once 'config.php';
require_once 'sync_helper.php';

$data = json_decode(file_get_contents("php://input"));
$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM iuran_anniversary WHERE id = ?");
            $stmt->execute(array($_GET['id']));
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM iuran_anniversary ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(array("status" => "success", "data" => $result));
        break;

    case 'POST':
        if (!empty($data->anggota_id) && isset($data->nominal)) {
            try {
                $conn->beginTransaction();
                $query = "INSERT INTO iuran_anniversary (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)";
                $stmt = $conn->prepare($query);
                $stmt->execute(array(
                    $data->anggota_id,
                    $data->nominal,
                    isset($data->tanggal) ? $data->tanggal : date('Y-m-d'),
                    isset($data->keterangan) ? $data->keterangan : ''
                ));
                
                recalculateAnggotaAniv($conn, $data->anggota_id);
                $conn->commit();
                
                echo json_encode(array("status" => "success", "message" => "Iuran anniversary berhasil ditambahkan"));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal tambah iuran anniversary: " . $e->getMessage()));
            }
        }
        break;

    case 'PUT':
        if (!empty($data->id) && !empty($data->anggota_id) && isset($data->nominal)) {
            try {
                $conn->beginTransaction();
                $query = "UPDATE iuran_anniversary SET anggota_id=?, nominal=?, tanggal=?, keterangan=? WHERE id=?";
                $stmt = $conn->prepare($query);
                $stmt->execute(array(
                    $data->anggota_id,
                    $data->nominal,
                    $data->tanggal,
                    $data->keterangan,
                    $data->id
                ));
                
                recalculateAnggotaAniv($conn, $data->anggota_id);
                $conn->commit();
                
                echo json_encode(array("status" => "success", "message" => "Iuran anniversary berhasil diupdate"));
            } catch (Throwable $e) {
                if ($conn->inTransaction()) {
                    $conn->rollBack();
                }
                http_response_code(500);
                echo json_encode(array("status" => "error", "message" => "Gagal update iuran anniversary: " . $e->getMessage()));
            }
        }
        break;

    case 'DELETE':
        if (!empty($data->id)) {
            try {
                $conn->beginTransaction();
                $stmt_get = $conn->prepare("SELECT anggota_id FROM iuran_anniversary WHERE id = ?");
                $stmt_get->execute(array($data->id));
                $row = $stmt_get->fetch(PDO::FETCH_ASSOC);
                
                if ($row) {
                    $anggota_id = $row['anggota_id'];
                    $stmt = $conn->prepare("DELETE FROM iuran_anniversary WHERE id = ?");
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
