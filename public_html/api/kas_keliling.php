<?php
// kas_keliling.php
include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents("php://input"));

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM kas_keliling WHERE id = ?");
            $stmt->execute([$_GET['id']]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
            echo json_encode(["status" => "success", "data" => $result]);
        } else {
            // Get all transactions
            $stmt = $conn->query("SELECT * FROM kas_keliling ORDER BY tahun DESC, bulan DESC");
            $transaksi = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            // Get summary
            $stmt_sum = $conn->query("SELECT 
                COALESCE(SUM(total_pemasukan), 0) as total_in, 
                COALESCE(SUM(total_pengeluaran), 0) as total_out 
                FROM kas_keliling");
            $summary = $stmt_sum->fetch(PDO::FETCH_ASSOC);
            
            $total_in = floatval($summary['total_in']);
            $total_out = floatval($summary['total_out']);
            $saldo = $total_in - $total_out;
            
            echo json_encode([
                "status" => "success", 
                "data" => [
                    "transaksi" => $transaksi,
                    "total_pemasukan" => $total_in,
                    "total_pengeluaran" => $total_out,
                    "saldo_kas_keliling" => $saldo
                ]
            ]);
        }
        break;
    case 'POST':
        if (!empty($data->bulan) && !empty($data->tahun) && isset($data->total_pemasukan)) {
            $pengeluaran = $data->total_pengeluaran ?? 0;
            $saldo = $data->total_pemasukan - $pengeluaran;
            $query = "INSERT INTO kas_keliling (bulan, tahun, total_pemasukan, total_pengeluaran, saldo, catatan) 
                      VALUES (:bulan, :tahun, :pemasukan, :pengeluaran, :saldo, :catatan)";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                ':bulan' => $data->bulan,
                ':tahun' => $data->tahun,
                ':pemasukan' => $data->total_pemasukan,
                ':pengeluaran' => $pengeluaran,
                ':saldo' => $saldo,
                ':catatan' => $data->catatan ?? ''
            ]);
            echo json_encode(["status" => "success", "message" => "Data kas keliling berhasil ditambahkan", "id" => $conn->lastInsertId()]);
        } else {
            echo json_encode(["status" => "error", "message" => "Data bulan, tahun, dan pemasukan wajib diisi"]);
        }
        break;
    case 'PUT':
        if (!empty($data->id)) {
            $pengeluaran = $data->total_pengeluaran ?? 0;
            $saldo = $data->total_pemasukan - $pengeluaran;
            $query = "UPDATE kas_keliling SET bulan=:bulan, tahun=:tahun, total_pemasukan=:pemasukan, 
                      total_pengeluaran=:pengeluaran, saldo=:saldo, catatan=:catatan WHERE id=:id";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                ':bulan' => $data->bulan,
                ':tahun' => $data->tahun,
                ':pemasukan' => $data->total_pemasukan,
                ':pengeluaran' => $pengeluaran,
                ':saldo' => $saldo,
                ':catatan' => $data->catatan ?? '',
                ':id' => $data->id
            ]);
            echo json_encode(["status" => "success", "message" => "Data kas keliling berhasil diupdate"]);
        } else {
            echo json_encode(["status" => "error", "message" => "ID tidak ditemukan"]);
        }
        break;
    case 'DELETE':
        if (!empty($data->id)) {
            $stmt = $conn->prepare("DELETE FROM kas_keliling WHERE id = ?");
            $stmt->execute([$data->id]);
            echo json_encode(["status" => "success", "message" => "Data kas keliling berhasil dihapus"]);
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
