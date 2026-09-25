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
    
    // 1. Prioritaskan query dari tabel 'users' sesuai spesifikasi:
    // "SELECT id, nama, nra, role, status FROM users WHERE status = 'VERIFIED'"
    try {
        $stmtUsers = $conn->prepare("SELECT id, nama, nra, role, status FROM users WHERE status = 'VERIFIED' ORDER BY nama ASC");
        $stmtUsers->execute();
        $data = $stmtUsers->fetchAll(PDO::FETCH_ASSOC);
    } catch (Exception $e) {
        $data = [];
    }

    // 2. Fallback / Sinkronisasi jika data anggota ada di tabel 'anggota'
    if (empty($data)) {
        try {
            $stmtAnggota = $conn->prepare("SELECT id, nama, nra, role, IF(statusAktif = 1, 'VERIFIED', 'PENDING') AS status, uang_kas, iuran_aniv, sisa_cicilan FROM anggota WHERE statusAktif = 1 ORDER BY nama ASC");
            $stmtAnggota->execute();
            $data = $stmtAnggota->fetchAll(PDO::FETCH_ASSOC);
        } catch (Exception $e) {
            $data = [];
        }
    }

    // 3. Format respon JSON secara presisi
    $formattedList = [];
    foreach ($data as $row) {
        $formattedList[] = [
            'id'     => (int)$row['id'],
            'nama'   => isset($row['nama']) ? (string)$row['nama'] : '',
            'nra'    => isset($row['nra']) ? (string)$row['nra'] : '',
            'role'   => isset($row['role']) ? (string)$row['role'] : 'Anggota',
            'status' => isset($row['status']) ? (string)$row['status'] : 'VERIFIED',
            'uang_kas' => isset($row['uang_kas']) ? (float)$row['uang_kas'] : 0.0,
            'iuran_aniv' => isset($row['iuran_aniv']) ? (float)$row['iuran_aniv'] : 0.0,
            'sisa_cicilan' => isset($row['sisa_cicilan']) ? (float)$row['sisa_cicilan'] : 0.0
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
