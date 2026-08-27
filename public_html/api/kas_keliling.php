<?php
// kas_keliling.php
include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents("php://input"));

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM kas_keliling WHERE id = ?");
            $stmt->execute(array($_GET['id']));
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
            echo json_encode(array("status" => "success", "data" => $result));
        } else {
            // Get all transactions
            $stmt = $conn->query("SELECT * FROM kas_keliling ORDER BY tahun DESC, bulan DESC");
            $transaksi = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            // Get summary from Master Ledger (saldo_akumulasi) for non-decreasing totals
            $stmt_sum = $conn->query("SELECT total_akumulasi_masuk as total_in, total_akumulasi_keluar as total_out FROM saldo_akumulasi WHERE jenis_kas = 'kas_keliling'");
            $summary = $stmt_sum ? $stmt_sum->fetch(PDO::FETCH_ASSOC) : null;
            
            $total_in = $summary ? floatval($summary['total_in']) : 0.0;
            $total_out = $summary ? floatval($summary['total_out']) : 0.0;
            $saldo = max(0, $total_in - $total_out);
            
            echo json_encode(array(
                "status" => "success", 
                "data" => [
                    "transaksi" => $transaksi,
                    "total_pemasukan" => $total_in,
                    "total_pengeluaran" => $total_out,
                    "saldo_kas_keliling" => $saldo
                ]
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

            // UPDATE SALDO AKUMULASI (Locked total)
            try {
                $stmt_master = $conn->prepare("
                    INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_masuk, total_akumulasi_keluar) 
                    VALUES ('kas_keliling', ?, ?) 
                    ON DUPLICATE KEY UPDATE 
                        total_akumulasi_masuk = total_akumulasi_masuk + ?,
                        total_akumulasi_keluar = total_akumulasi_keluar + ?
                ");
                $stmt_master->execute(array($data->total_pemasukan, $pengeluaran, $data->total_pemasukan, $pengeluaran));
            } catch (Exception $e_master) {}

            echo json_encode(array("status" => "success", "message" => "Data kas keliling berhasil ditambahkan", "id" => $conn->lastInsertId()));
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
            echo json_encode(array("status" => "success", "message" => "Data kas keliling berhasil diupdate"));
        } else {
            echo json_encode(array("status" => "error", "message" => "ID tidak ditemukan"));
        }
        break;
    case 'DELETE':
        if (!empty($data->id)) {
            $stmt = $conn->prepare("DELETE FROM kas_keliling WHERE id = ?");
            $stmt->execute(array($data->id));
            echo json_encode(array("status" => "success", "message" => "Data kas keliling berhasil dihapus"));
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
