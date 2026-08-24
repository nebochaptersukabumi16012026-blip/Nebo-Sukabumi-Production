<?php
// absensi.php
include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents("php://input"));

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM absensi WHERE id = ?");
            $stmt->execute([$_GET['id']]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM absensi ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(["status" => "success", "data" => $result]);
        break;
    case 'POST':
        if (!empty($data->anggota_id) && !empty($data->tanggal) && !empty($data->status)) {
            $query = "INSERT INTO absensi (anggota_id, tanggal, status, keterangan) 
                      VALUES (:anggota_id, :tanggal, :status, :keterangan)";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                ':anggota_id' => $data->anggota_id,
                ':tanggal' => $data->tanggal,
                ':status' => $data->status,
                ':keterangan' => $data->keterangan ?? ''
            ]);
            echo json_encode(["status" => "success", "message" => "Absensi berhasil dicatat", "id" => $conn->lastInsertId()]);
        } else {
            echo json_encode(["status" => "error", "message" => "Data tidak lengkap"]);
        }
        break;
    case 'PUT':
        if (!empty($data->id)) {
            $query = "UPDATE absensi SET anggota_id=:anggota_id, tanggal=:tanggal, status=:status, 
                      keterangan=:keterangan WHERE id=:id";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                ':anggota_id' => $data->anggota_id,
                ':tanggal' => $data->tanggal,
                ':status' => $data->status,
                ':keterangan' => $data->keterangan,
                ':id' => $data->id
            ]);
            echo json_encode(["status" => "success", "message" => "Absensi berhasil diupdate"]);
        } else {
            echo json_encode(["status" => "error", "message" => "ID tidak ditemukan"]);
        }
        break;
    case 'DELETE':
        if (!empty($data->id)) {
            $stmt = $conn->prepare("DELETE FROM absensi WHERE id = ?");
            $stmt->execute([$data->id]);
            echo json_encode(["status" => "success", "message" => "Absensi berhasil dihapus"]);
        } else {
            echo json_encode(["status" => "error", "message" => "ID tidak ditemukan"]);
        }
        break;
    default:
        http_response_code(405);
        echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
        break;
}
?>
