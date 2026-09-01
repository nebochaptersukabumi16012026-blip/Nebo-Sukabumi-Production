<?php
// tambah_kas_keliling.php - Khusus Kas Keliling Bulanan
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);

$bulan = '';
if (isset($data->bulan)) $bulan = trim($data->bulan);
elseif (isset($_POST['bulan'])) $bulan = trim($_POST['bulan']);

$tahun = '';
if (isset($data->tahun)) $tahun = trim($data->tahun);
elseif (isset($_POST['tahun'])) $tahun = trim($_POST['tahun']);

$nominal = 0.0;
if (isset($data->nominal)) $nominal = floatval($data->nominal);
elseif (isset($_POST['nominal'])) $nominal = floatval($_POST['nominal']);

$jenis = 'pemasukan';
if (isset($data->jenis)) $jenis = strtolower(trim($data->jenis));
elseif (isset($data->jenis_transaksi)) $jenis = strtolower(trim($data->jenis_transaksi));
elseif (isset($_POST['jenis'])) $jenis = strtolower(trim($_POST['jenis']));

$catatan = '';
if (isset($data->catatan)) $catatan = trim($data->catatan);
elseif (isset($data->keterangan)) $catatan = trim($data->keterangan);
elseif (isset($_POST['catatan'])) $catatan = trim($_POST['catatan']);
elseif (isset($_POST['keterangan'])) $catatan = trim($_POST['keterangan']);

$tanggal = '';
if (isset($data->tanggal)) $tanggal = trim($data->tanggal);
elseif (isset($_POST['tanggal'])) $tanggal = trim($_POST['tanggal']);
if (empty($tanggal)) {
    $tanggal = date('Y-m-d');
}

if (empty($bulan) || empty($tahun) || $nominal <= 0) {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "Bulan, tahun, dan nominal wajib diisi dengan benar"
    ));
    exit();
}

try {
    // INSERT query HANYA memasukkan data transaksi tunggal tanpa menyentuh kolom akumulasi/nra
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

    // Hitung rekapitulasi terbaru via SUM aggregate dari nominal
    $stmt_in = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pemasukan' OR LOWER(jenis_transaksi) = 'pemasukan'");
    $total_pemasukan = floatval($stmt_in->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

    $stmt_out = $conn->query("SELECT COALESCE(SUM(nominal), 0) as total FROM kas_keliling WHERE LOWER(jenis) = 'pengeluaran' OR LOWER(jenis_transaksi) = 'pengeluaran'");
    $total_pengeluaran = floatval($stmt_out->fetch(PDO::FETCH_ASSOC)['total'] ?? 0);

    $saldo = max(0, $total_pemasukan - $total_pengeluaran);

    http_response_code(200);
    echo json_encode(array(
        "status" => "success",
        "message" => "Transaksi kas keliling berhasil ditambahkan",
        "id" => $insertedId,
        "total_pemasukan" => $total_pemasukan,
        "total_pengeluaran" => $total_pengeluaran,
        "saldo" => $saldo
    ));

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal menambah kas keliling: " . $e->getMessage()
    ));
}
?>
