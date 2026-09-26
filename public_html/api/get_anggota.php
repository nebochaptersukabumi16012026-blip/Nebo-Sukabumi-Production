<?php
// =============================================================================
// API: get_anggota.php
// Nebo Sukabumi - Endpoint Data Anggota Terverifikasi (cPanel)
// =============================================================================

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

try {
    $data = [];
    
    // 1. Ambil data lengkap dari tabel anggota (master keuangan, kas, & cicilan)
    // dan padukan dengan status verifikasi terbaru dari users jika ada
    $query = "SELECT a.id, a.nama, a.nra, 
                     COALESCE(u.role, a.role, 'Anggota') AS role,
                     COALESCE(u.status, IF(a.statusAktif = 1, 'VERIFIED', 'PENDING')) AS status,
                     COALESCE(NULLIF(a.uang_kas, 0), NULLIF(a.uangKas, 0), 0) AS uang_kas,
                     COALESCE(NULLIF(a.iuran_aniv, 0), NULLIF(a.iuranAniv, 0), 0) AS iuran_aniv,
                     COALESCE(NULLIF(a.harga_barang, 0), NULLIF(a.hargaBarang, 0), 0) AS harga_barang,
                     COALESCE(NULLIF(a.total_cicilan, 0), NULLIF(a.totalCicilan, 0), 0) AS total_cicilan,
                     COALESCE(NULLIF(a.sisa_cicilan, 0), NULLIF(a.sisaCicilan, 0), 0) AS sisa_cicilan,
                     COALESCE(a.cicilan_per_bulan, 0) AS cicilan_per_bulan,
                     COALESCE(a.lamaCicilan, 0) AS lamaCicilan,
                     COALESCE(NULLIF(a.nama_barang, ''), a.namaBarang, '') AS nama_barang,
                     COALESCE(NULLIF(a.totalTagihan, 0), a.harga_barang, 0) AS totalTagihan
              FROM anggota a
              LEFT JOIN users u ON (u.username = a.nra OR u.id = a.id)
              ORDER BY a.nama ASC";

    try {
        $stmt = $conn->query($query);
        $data = $stmt ? $stmt->fetchAll(PDO::FETCH_ASSOC) : [];
    } catch (Exception $e) {
        // Fallback jika query join gagal
        try {
            $stmt = $conn->query("SELECT * FROM anggota ORDER BY nama ASC");
            $data = $stmt ? $stmt->fetchAll(PDO::FETCH_ASSOC) : [];
        } catch (Exception $e2) {
            $data = [];
        }
    }

    // 2. Format respon JSON secara presisi dengan semua kolom cicilan & status verifikasi
    $formattedList = [];
    foreach ($data as $row) {
        $hargaBarang = floatval(isset($row['harga_barang']) ? $row['harga_barang'] : (isset($row['hargaBarang']) ? $row['hargaBarang'] : 0));
        $totalCicilan = floatval(isset($row['total_cicilan']) ? $row['total_cicilan'] : (isset($row['totalCicilan']) ? $row['totalCicilan'] : 0));
        $rawSisa = isset($row['sisa_cicilan']) ? $row['sisa_cicilan'] : (isset($row['sisaCicilan']) ? $row['sisaCicilan'] : 0);
        $sisaCicilan = floatval($rawSisa > 0 ? $rawSisa : max(0.0, $hargaBarang - $totalCicilan));
        $cicilanPerBulan = floatval(isset($row['cicilan_per_bulan']) ? $row['cicilan_per_bulan'] : 0);
        $lamaCicilan = intval(isset($row['lamaCicilan']) ? $row['lamaCicilan'] : (isset($row['lama_cicilan']) ? $row['lama_cicilan'] : 0));
        $namaBarang = isset($row['nama_barang']) ? (string)$row['nama_barang'] : (isset($row['namaBarang']) ? (string)$row['namaBarang'] : '');
        $totalTagihan = floatval(isset($row['totalTagihan']) ? $row['totalTagihan'] : $hargaBarang);

        $statusStr = isset($row['status']) ? (string)$row['status'] : 'VERIFIED';
        $isVerified = ($statusStr === 'VERIFIED' || $statusStr === '1' || $statusStr === 'Aktif' || $statusStr === 'approved');

        $formattedList[] = [
            'id'                => (int)$row['id'],
            'nama'              => isset($row['nama']) ? (string)$row['nama'] : '',
            'nra'               => isset($row['nra']) ? (string)$row['nra'] : '',
            'role'              => isset($row['role']) ? (string)$row['role'] : 'Anggota',
            'status'            => $statusStr,
            'status_verifikasi' => $isVerified ? '1' : '0',
            'is_verified'       => $isVerified,
            'uang_kas'          => floatval(isset($row['uang_kas']) ? $row['uang_kas'] : (isset($row['uangKas']) ? $row['uangKas'] : 0)),
            'iuran_aniv'        => floatval(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : (isset($row['iuranAniv']) ? $row['iuranAniv'] : 0)),
            'harga_barang'      => $hargaBarang,
            'hargaBarang'       => $hargaBarang,
            'total_cicilan'     => $totalCicilan,
            'totalCicilan'      => $totalCicilan,
            'sisa_cicilan'      => $sisaCicilan,
            'sisaCicilan'       => $sisaCicilan,
            'cicilan_per_bulan' => $cicilanPerBulan,
            'cicilanPerBulan'   => $cicilanPerBulan,
            'lamaCicilan'       => $lamaCicilan,
            'lama_cicilan'      => $lamaCicilan,
            'nama_barang'       => $namaBarang,
            'namaBarang'        => $namaBarang,
            'totalTagihan'      => $totalTagihan
        ];
    }

    echo json_encode([
        'status'  => 'success',
        'success' => true,
        'message' => 'Data anggota berhasil diambil',
        'total'   => count($formattedList),
        'data'    => $formattedList
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status'  => 'error',
        'success' => false,
        'message' => 'Gagal mengambil data: ' . $e->getMessage(),
        'total'   => 0,
        'data'    => []
    ], JSON_UNESCAPED_UNICODE);
}
?>
