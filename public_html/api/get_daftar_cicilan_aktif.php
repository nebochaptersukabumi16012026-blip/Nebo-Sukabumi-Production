<?php
// get_daftar_cicilan_aktif.php
require_once 'config.php';

header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

try {
    // Periksa apakah kolom 'harga_barang' dan 'sudah_dibayar' ada di tabel cicilan
    $queryFromCicilanTable = false;
    try {
        $checkCol = $conn->query("SHOW COLUMNS FROM cicilan LIKE 'harga_barang'");
        if ($checkCol && $checkCol->rowCount() > 0) {
            $queryFromCicilanTable = true;
        }
    } catch (Exception $ignored) {
        $queryFromCicilanTable = false;
    }

    $total_harga_barang = 0.0;
    $total_sudah_dibayar = 0.0;
    $total_sisa_cicilan = 0.0;
    $anggota_mencicil = 0;

    if ($queryFromCicilanTable) {
        // Query persis sesuai spesifikasi jika tabel cicilan menyimpan data master cicilan
        $query = "SELECT id, nama, nra, harga_barang, sudah_dibayar, (harga_barang - sudah_dibayar) AS sisa_cicilan, cicilan_per_bulan 
                  FROM cicilan 
                  WHERE (harga_barang - sudah_dibayar) > 0 
                  ORDER BY sisa_cicilan DESC";
        $stmt = $conn->query($query);
        $rows = $stmt ? $stmt->fetchAll(PDO::FETCH_ASSOC) : array();

        // Query rekapitulasi sesuai instruksi
        $queryRekap = "SELECT 
                          COALESCE(SUM(harga_barang), 0) AS total_harga_barang,
                          COALESCE(SUM(sudah_dibayar), 0) AS total_sudah_dibayar,
                          COALESCE(SUM(harga_barang - sudah_dibayar), 0) AS total_sisa_cicilan,
                          COUNT(DISTINCT nra) AS anggota_mencicil
                       FROM cicilan 
                       WHERE (harga_barang - sudah_dibayar) > 0";
        $stmtRekap = $conn->query($queryRekap);
        if ($stmtRekap && $rowR = $stmtRekap->fetch(PDO::FETCH_ASSOC)) {
            $anggota_mencicil = intval($rowR['anggota_mencicil'] ?? 0);
            if ($anggota_mencicil > 0) {
                $total_harga_barang = floatval($rowR['total_harga_barang'] ?? 0);
                $total_sudah_dibayar = floatval($rowR['total_sudah_dibayar'] ?? 0);
                $total_sisa_cicilan = floatval($rowR['total_sisa_cicilan'] ?? 0);
            }
        }
    } else {
        // Query dari tabel anggota (master anggota & tagihan cicilan)
        // total_cicilan di tabel anggota merepresentasikan nominal yang 'sudah_dibayar'
        $query = "SELECT id, nama, nra, 
                         COALESCE(harga_barang, 0) AS harga_barang, 
                         COALESCE(total_cicilan, 0) AS sudah_dibayar, 
                         CASE 
                             WHEN sisa_cicilan > 0 THEN sisa_cicilan 
                             ELSE (COALESCE(harga_barang, 0) - COALESCE(total_cicilan, 0)) 
                         END AS sisa_cicilan, 
                         COALESCE(cicilan_per_bulan, 0) AS cicilan_per_bulan 
                  FROM anggota 
                  WHERE (COALESCE(harga_barang, 0) - COALESCE(total_cicilan, 0) > 0 OR COALESCE(sisa_cicilan, 0) > 0)
                  ORDER BY sisa_cicilan DESC";
        $stmt = $conn->query($query);
        $rows = $stmt ? $stmt->fetchAll(PDO::FETCH_ASSOC) : array();

        // Rekapitulasi dari tabel anggota HANYA untuk anggota yang MASIH MEMILIKI SISA CICILAN (sisa > 0)
        $queryRekap = "SELECT 
                          COALESCE(SUM(harga_barang), 0) AS total_harga_barang,
                          COALESCE(SUM(total_cicilan), 0) AS total_sudah_dibayar,
                          COALESCE(SUM(CASE WHEN sisa_cicilan > 0 THEN sisa_cicilan ELSE (COALESCE(harga_barang, 0) - COALESCE(total_cicilan, 0)) END), 0) AS total_sisa_cicilan,
                          COUNT(DISTINCT nra) AS anggota_mencicil
                       FROM anggota 
                       WHERE (COALESCE(harga_barang, 0) - COALESCE(total_cicilan, 0) > 0 OR COALESCE(sisa_cicilan, 0) > 0)";
        $stmtRekap = $conn->query($queryRekap);
        if ($stmtRekap && $rowR = $stmtRekap->fetch(PDO::FETCH_ASSOC)) {
            $anggota_mencicil = intval($rowR['anggota_mencicil'] ?? 0);
            if ($anggota_mencicil > 0) {
                $total_harga_barang = floatval($rowR['total_harga_barang'] ?? 0);
                $total_sudah_dibayar = floatval($rowR['total_sudah_dibayar'] ?? 0);
                $total_sisa_cicilan = floatval($rowR['total_sisa_cicilan'] ?? 0);
            }
        }
    }

    $data = array();
    foreach ($rows as $row) {
        $hargaBarang = floatval(isset($row['harga_barang']) ? $row['harga_barang'] : 0);
        $sudahDibayar = floatval(isset($row['sudah_dibayar']) ? $row['sudah_dibayar'] : 0);
        $sisaCicilan = floatval(isset($row['sisa_cicilan']) ? $row['sisa_cicilan'] : ($hargaBarang - $sudahDibayar));
        $cicilanPerBulan = floatval(isset($row['cicilan_per_bulan']) ? $row['cicilan_per_bulan'] : 0);

        if ($sisaCicilan > 0) {
            $data[] = array(
                "id" => intval($row['id']),
                "nama" => isset($row['nama']) ? $row['nama'] : '',
                "nra" => (!empty($row['nra'])) ? $row['nra'] : '-',
                "harga_barang" => $hargaBarang,
                "sudah_dibayar" => $sudahDibayar,
                "sisa_cicilan" => $sisaCicilan,
                "cicilan_per_bulan" => $cicilanPerBulan
            );
        }
    }

    // Jika tidak ada anggota yang mencicil (0 anggota), pastikan semua total otomatis bernilai 0
    if ($anggota_mencicil <= 0 || count($data) === 0) {
        $total_harga_barang = 0.0;
        $total_sudah_dibayar = 0.0;
        $total_sisa_cicilan = 0.0;
        $anggota_mencicil = 0;
    } else {
        // Sinkronkan data list dengan rekap
        $anggota_mencicil = count($data);
        $total_harga_barang = array_sum(array_column($data, 'harga_barang'));
        $total_sudah_dibayar = array_sum(array_column($data, 'sudah_dibayar'));
        $total_sisa_cicilan = array_sum(array_column($data, 'sisa_cicilan'));
    }

    echo json_encode(array(
        "status" => "success",
        "message" => "Berhasil mengambil daftar cicilan aktif",
        "total_harga_barang" => $total_harga_barang,
        "total_sudah_dibayar" => $total_sudah_dibayar,
        "total_sisa_cicilan" => $total_sisa_cicilan,
        "anggota_mencicil" => $anggota_mencicil,
        "rekapitulasi" => array(
            "total_harga_barang" => $total_harga_barang,
            "total_sudah_dibayar" => $total_sudah_dibayar,
            "total_sisa_cicilan" => $total_sisa_cicilan,
            "anggota_mencicil" => $anggota_mencicil
        ),
        "data" => $data,
        "total" => count($data)
    ));
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(array(
        "status" => "error",
        "message" => "Gagal mengambil daftar cicilan aktif: " . $e->getMessage(),
        "data" => array()
    ));
}
?>
