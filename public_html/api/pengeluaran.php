<?php
// pengeluaran.php
include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents("php://input"));

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM pengeluaran WHERE id = ?");
            $stmt->execute([$_GET['id']]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM pengeluaran ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(["status" => "success", "data" => $result]);
        break;
    case 'POST':
        if (!empty($data->keterangan) && isset($data->nominal) && !empty($data->tanggal) && !empty($data->jenis_kas)) {
            $query = "INSERT INTO pengeluaran (keterangan, nominal, tanggal, jenis_kas, created_by) 
                      VALUES (:keterangan, :nominal, :tanggal, :jenis_kas, :created_by)";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                ':keterangan' => $data->keterangan,
                ':nominal' => $data->nominal,
                ':tanggal' => $data->tanggal,
                ':jenis_kas' => $data->jenis_kas,
                ':created_by' => $data->created_by ?? 'Sistem'
            ]);
            echo json_encode(["status" => "success", "message" => "Pengeluaran berhasil dicatat", "id" => $conn->lastInsertId()]);
        } else {
            echo json_encode(["status" => "error", "message" => "Data tidak lengkap"]);
        }
        break;
    case 'PUT':
        if (!empty($data->id)) {
            $query = "UPDATE pengeluaran SET keterangan=:keterangan, nominal=:nominal, tanggal=:tanggal, 
                      jenis_kas=:jenis_kas, created_by=:created_by WHERE id=:id";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                ':keterangan' => $data->keterangan,
                ':nominal' => $data->nominal,
                ':tanggal' => $data->tanggal,
                ':jenis_kas' => $data->jenis_kas,
                ':created_by' => $data->created_by,
                ':id' => $data->id
            ]);
            echo json_encode(["status" => "success", "message" => "Data pengeluaran berhasil diupdate"]);
        } else {
            echo json_encode(["status" => "error", "message" => "ID tidak ditemukan"]);
        }
        break;
    case 'DELETE':
        if (!empty($data->id)) {
            $stmt = $conn->prepare("DELETE FROM pengeluaran WHERE id = ?");
            $stmt->execute([$data->id]);
            echo json_encode(["status" => "success", "message" => "Data pengeluaran berhasil dihapus"]);
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
