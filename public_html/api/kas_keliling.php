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

/**
 * Rekapitulasi Kas Keliling:
 * - Jika total_pemasukan > 0, gunakan total_pemasukan.
 * - Jika total_pemasukan = 0 dan transaksi adalah pemasukan, gunakan nominal.
 * - Jika total_pengeluaran > 0, gunakan total_pengeluaran.
 * - Jika total_pengeluaran = 0 dan transaksi adalah pengeluaran, gunakan nominal.
 * Tidak ada double-count pada record yang sama.
 */
function hitungRekapKasKeliling($conn) {
    $stmt = $conn->query("SELECT 
        COALESCE(SUM(CASE 
            WHEN total_pemasukan > 0 THEN total_pemasukan
            WHEN (total_pemasukan = 0 OR total_pemasukan IS NULL) AND (LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pemasukan' OR (COALESCE(jenis, '') = '' AND COALESCE(jenis_transaksi, '') = '')) THEN nominal
            ELSE 0 
        END), 0) AS total_in,
        COALESCE(SUM(CASE 
            WHEN total_pengeluaran > 0 THEN total_pengeluaran
            WHEN (total_pengeluaran = 0 OR total_pengeluaran IS NULL) AND LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pengeluaran' THEN nominal
            ELSE 0 
        END), 0) AS total_out
    FROM kas_keliling");
    $row = $stmt ? $stmt->fetch(PDO::FETCH_ASSOC) : null;
    $total_in = floatval($row['total_in'] ?? 0);
    $total_out = floatval($row['total_out'] ?? 0);
    $saldo = max(0, $total_in - $total_out);
    return array(
        'total_pemasukan' => $total_in,
        'total_pengeluaran' => $total_out,
        'saldo' => $saldo
    );
}

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
            
            $rekap = hitungRekapKasKeliling($conn);
            $total_pemasukan = $rekap['total_pemasukan'];
            $total_pengeluaran = $rekap['total_pengeluaran'];
            $saldo = $rekap['saldo'];
            
            echo json_encode(array(
                "status" => "success", 
                "total_pemasukan" => $total_pemasukan,
                "total_pengeluaran" => $total_pengeluaran,
                "saldo" => $saldo,
                "saldo_kas_keliling" => $saldo,
                "saldo_akhir" => $saldo,
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

            $rekap = hitungRekapKasKeliling($conn);

            echo json_encode(array(
                "status" => "success", 
                "message" => "Data kas keliling berhasil ditambahkan", 
                "id" => $insertedId,
                "total_pemasukan" => $rekap['total_pemasukan'],
                "total_pengeluaran" => $rekap['total_pengeluaran'],
                "saldo" => $rekap['saldo']
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

            $rekap = hitungRekapKasKeliling($conn);

            echo json_encode(array(
                "status" => "success", 
                "message" => "Data kas keliling berhasil diupdate",
                "total_pemasukan" => $rekap['total_pemasukan'],
                "total_pengeluaran" => $rekap['total_pengeluaran'],
                "saldo" => $rekap['saldo']
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

            $rekap = hitungRekapKasKeliling($conn);

            echo json_encode(array(
                "status" => "success", 
                "message" => "Data kas keliling berhasil dihapus",
                "total_pemasukan" => $rekap['total_pemasukan'],
                "total_pengeluaran" => $rekap['total_pengeluaran'],
                "saldo" => $rekap['saldo']
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
