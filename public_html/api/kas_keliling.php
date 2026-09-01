<?php
// kas_keliling.php - Khusus Kas Keliling Bulanan
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM kas_keliling WHERE id = ?");
            $stmt->execute(array($_GET['id']));
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
            echo json_encode(array("status" => "success", "data" => $result));
        } else {
            $stmt = $conn->query("SELECT * FROM kas_keliling ORDER BY tahun DESC, bulan DESC");
            $transaksi = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            // SQL Aggregate SUM calculation KHUSUS tabel kas_keliling
            $stmt_in = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) as total FROM kas_keliling");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE total_pengeluaran END), 0) as total FROM kas_keliling");
            $total_pengeluaran = floatval($stmt_out->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $saldo = max(0, $total_pemasukan - $total_pengeluaran);
            
            echo json_encode(array(
                "status" => "success", 
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => $saldo,
                "data" => $transaksi
            ));
        }
        break;
    case 'POST':
        if (!empty($data->bulan) && !empty($data->tahun) && isset($data->total_pemasukan)) {
            $pengeluaran = isset($data->total_pengeluaran) ? $data->total_pengeluaran : 0;
            $saldo = $data->total_pemasukan - $pengeluaran;
            $query = "INSERT INTO kas_keliling (bulan, tahun, total_pemasukan, total_pengeluaran, saldo, catatan) 
                      VALUES (:bulan, :tahun, :pemasukan, :pengeluaran, :saldo, :catatan)";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':bulan' => $data->bulan,
                ':tahun' => $data->tahun,
                ':pemasukan' => $data->total_pemasukan,
                ':pengeluaran' => $pengeluaran,
                ':saldo' => $saldo,
                ':catatan' => isset($data->catatan) ? $data->catatan : ''
            ));

            $stmt_in = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) as total FROM kas_keliling");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE total_pengeluaran END), 0) as total FROM kas_keliling");
            $total_pengeluaran = floatval($stmt_out->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $saldo = max(0, $total_pemasukan - $total_pengeluaran);

            echo json_encode(array(
                "status" => "success", 
                "message" => "Data kas keliling berhasil ditambahkan", 
                "id" => $conn->lastInsertId(),
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => $saldo
            ));
        } else {
            echo json_encode(array("status" => "error", "message" => "Data bulan, tahun, dan pemasukan wajib diisi"));
        }
        break;
    case 'PUT':
        if (!empty($data->id)) {
            $pengeluaran = isset($data->total_pengeluaran) ? $data->total_pengeluaran : 0;
            $saldo = $data->total_pemasukan - $pengeluaran;
            $query = "UPDATE kas_keliling SET bulan=:bulan, tahun=:tahun, total_pemasukan=:pemasukan, 
                      total_pengeluaran=:pengeluaran, saldo=:saldo, catatan=:catatan WHERE id=:id";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':bulan' => $data->bulan,
                ':tahun' => $data->tahun,
                ':pemasukan' => $data->total_pemasukan,
                ':pengeluaran' => $pengeluaran,
                ':saldo' => $saldo,
                ':catatan' => isset($data->catatan) ? $data->catatan : '',
                ':id' => $data->id
            ));

            $stmt_in = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) as total FROM kas_keliling");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE total_pengeluaran END), 0) as total FROM kas_keliling");
            $total_pengeluaran = floatval($stmt_out->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $saldo = max(0, $total_pemasukan - $total_pengeluaran);

            echo json_encode(array(
                "status" => "success", 
                "message" => "Data kas keliling berhasil diupdate",
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => $saldo
            ));
        } else {
            echo json_encode(array("status" => "error", "message" => "ID tidak ditemukan"));
        }
        break;
    case 'DELETE':
        $deleteId = 0;
        if (!empty($data->id)) {
            $deleteId = intval($data->id);
        } elseif (isset($_GET['id'])) {
            $deleteId = intval($_GET['id']);
        }

        if ($deleteId > 0) {
            $stmt = $conn->prepare("DELETE FROM kas_keliling WHERE id = ?");
            $stmt->execute(array($deleteId));

            $stmt_in = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pemasukan' THEN nominal ELSE total_pemasukan END), 0) as total FROM kas_keliling");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(CASE WHEN jenis_transaksi = 'Pengeluaran' THEN nominal ELSE total_pengeluaran END), 0) as total FROM kas_keliling");
            $total_pengeluaran = floatval($stmt_out->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $saldo = max(0, $total_pemasukan - $total_pengeluaran);

            echo json_encode(array(
                "status" => "success", 
                "message" => "Data kas keliling berhasil dihapus",
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => $saldo
            ));
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
