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
            $stmt = $conn->query("SELECT * FROM kas_keliling ORDER BY tahun DESC, bulan DESC, id DESC");
            $transaksi = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            // Rekapitulasi dinamis menggunakan SUM aggregate dari kolom nominal
            $stmt_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pemasukan' OR LOWER(jenis_transaksi) = 'pemasukan' OR (jenis = '' AND jenis_transaksi = '')");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pengeluaran' OR LOWER(jenis_transaksi) = 'pengeluaran'");
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
        // Redirect or handle POST as adding single transaction
        $bulan = isset($data->bulan) ? trim($data->bulan) : '';
        $tahun = isset($data->tahun) ? trim($data->tahun) : '';
        $nominal = isset($data->nominal) ? floatval($data->nominal) : 0;
        $jenis = isset($data->jenis) ? strtolower(trim($data->jenis)) : (isset($data->jenis_transaksi) ? strtolower(trim($data->jenis_transaksi)) : 'pemasukan');
        $catatan = isset($data->catatan) ? trim($data->catatan) : (isset($data->keterangan) ? trim($data->keterangan) : '');
        $tanggal = isset($data->tanggal) ? trim($data->tanggal) : date('Y-m-d');

        if (!empty($bulan) && !empty($tahun) && $nominal > 0) {
            $query = "INSERT INTO kas_keliling (bulan, tahun, nominal, jenis, catatan, tanggal) VALUES (:bulan, :tahun, :nominal, :jenis, :catatan, :tanggal)";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':bulan' => $bulan,
                ':tahun' => $tahun,
                ':nominal' => $nominal,
                ':jenis' => $jenis,
                ':catatan' => $catatan,
                ':tanggal' => $tanggal
            ));
            $insertedId = $conn->lastInsertId();

            $stmt_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pemasukan' OR LOWER(jenis_transaksi) = 'pemasukan' OR (jenis = '' AND jenis_transaksi = '')");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pengeluaran' OR LOWER(jenis_transaksi) = 'pengeluaran'");
            $total_pengeluaran = floatval($stmt_out->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $saldo = max(0, $total_pemasukan - $total_pengeluaran);

            echo json_encode(array(
                "status" => "success", 
                "message" => "Data kas keliling berhasil ditambahkan", 
                "id" => $insertedId,
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => $saldo
            ));
        } else {
            echo json_encode(array("status" => "error", "message" => "Bulan, tahun, dan nominal wajib diisi"));
        }
        break;
    case 'PUT':
        if (!empty($data->id)) {
            $bulan = isset($data->bulan) ? trim($data->bulan) : '';
            $tahun = isset($data->tahun) ? trim($data->tahun) : '';
            $nominal = isset($data->nominal) ? floatval($data->nominal) : 0;
            $jenis = isset($data->jenis) ? strtolower(trim($data->jenis)) : 'pemasukan';
            $catatan = isset($data->catatan) ? trim($data->catatan) : '';
            $tanggal = isset($data->tanggal) ? trim($data->tanggal) : date('Y-m-d');

            $query = "UPDATE kas_keliling SET bulan=:bulan, tahun=:tahun, nominal=:nominal, jenis=:jenis, catatan=:catatan, tanggal=:tanggal WHERE id=:id";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':bulan' => $bulan,
                ':tahun' => $tahun,
                ':nominal' => $nominal,
                ':jenis' => $jenis,
                ':catatan' => $catatan,
                ':tanggal' => $tanggal,
                ':id' => $data->id
            ));

            $stmt_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pemasukan' OR LOWER(jenis_transaksi) = 'pemasukan'");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pengeluaran' OR LOWER(jenis_transaksi) = 'pengeluaran'");
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

            $stmt_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pemasukan' OR LOWER(jenis_transaksi) = 'pemasukan'");
            $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

            $stmt_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pengeluaran' OR LOWER(jenis_transaksi) = 'pengeluaran'");
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
