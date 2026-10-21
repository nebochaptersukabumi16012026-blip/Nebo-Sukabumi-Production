<?php
// catatan.php
include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents("php://input"));

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM catatan WHERE id = ?");
            $stmt->execute(array($_GET['id']));
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        } else {
            $stmt = $conn->query("SELECT * FROM catatan ORDER BY tanggal DESC");
            $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        }
        echo json_encode(array("status" => "success", "data" => $result));
        break;
    case 'POST':
        if (!empty($data->judul) && !empty($data->tanggal)) {
            $query = "INSERT INTO catatan (judul, isi, tanggal) VALUES (:judul, :isi, :tanggal)";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':judul' => $data->judul,
                ':isi' => isset($data->isi) ? $data->isi : '',
                ':tanggal' => $data->tanggal
            ));
            echo json_encode(array("status" => "success", "message" => "Catatan berhasil ditambahkan", "id" => $conn->lastInsertId()));
        } else {
            echo json_encode(array("status" => "error", "message" => "Judul dan tanggal wajib diisi"));
        }
        break;
    case 'PUT':
        if (!empty($data->id)) {
            $query = "UPDATE catatan SET judul=:judul, isi=:isi, tanggal=:tanggal WHERE id=:id";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':judul' => $data->judul,
                ':isi' => $data->isi,
                ':tanggal' => $data->tanggal,
                ':id' => $data->id
            ));
            echo json_encode(array("status" => "success", "message" => "Catatan berhasil diupdate"));
        } else {
            echo json_encode(array("status" => "error", "message" => "ID tidak ditemukan"));
        }
        break;
    case 'DELETE':
        if (!empty($data->id)) {
            $stmt = $conn->prepare("DELETE FROM catatan WHERE id = ?");
            $stmt->execute(array($data->id));
            echo json_encode(array("status" => "success", "message" => "Catatan berhasil dihapus"));
        } else {
            echo json_encode(array("status" => "error", "message" => "ID tidak ditemukan"));
        }
        break;
    default:
        http_response_code(405);
        echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
        break;
}
?>
