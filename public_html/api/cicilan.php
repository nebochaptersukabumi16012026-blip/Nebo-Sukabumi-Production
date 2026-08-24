<?php
// cicilan.php
require_once 'config.php';
require_once 'sync_helper.php';

$data = json_decode(file_get_contents("php://input"));
$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM cicilan WHERE id = ?");
            $stmt->execute([$_GET['id']]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM cicilan ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(["status" => "success", "data" => $result]);
        break;

    case 'POST':
        if (!empty($data->anggota_id) && isset($data->nominal)) {
            $query = "INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) VALUES (?, ?, ?, ?)";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                $data->anggota_id,
                $data->nominal,
                $data->tanggal ?? date('Y-m-d'),
                $data->keterangan ?? ''
            ]);
            
            recalculateAnggotaCicilan($conn, $data->anggota_id);
            
            echo json_encode(["status" => "success", "message" => "Cicilan berhasil ditambahkan"]);
        }
        break;

    case 'PUT':
        if (!empty($data->id) && !empty($data->anggota_id) && isset($data->nominal)) {
            $query = "UPDATE cicilan SET anggota_id=?, nominal=?, tanggal=?, keterangan=? WHERE id=?";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                $data->anggota_id,
                $data->nominal,
                $data->tanggal,
                $data->keterangan,
                $data->id
            ]);
            
            recalculateAnggotaCicilan($conn, $data->anggota_id);
            
            echo json_encode(["status" => "success", "message" => "Cicilan berhasil diupdate"]);
        }
        break;

    case 'DELETE':
        if (!empty($data->id)) {
            $stmt_get = $conn->prepare("SELECT anggota_id FROM cicilan WHERE id = ?");
            $stmt_get->execute([$data->id]);
            $row = $stmt_get->fetch(PDO::FETCH_ASSOC);
            
            if ($row) {
                $anggota_id = $row['anggota_id'];
                $stmt = $conn->prepare("DELETE FROM cicilan WHERE id = ?");
                $stmt->execute([$data->id]);
                
                recalculateAnggotaCicilan($conn, $anggota_id);
                
                echo json_encode(["status" => "success", "message" => "Cicilan berhasil dihapus"]);
            }
        }
        break;

    default:
        http_response_code(405);
        echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
        break;
}
?>
