<?php
// get_dashboard.php - Role-Based Access Control (RBAC) & Verification Status
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

try {
    $db = isset($conn) ? $conn : (isset($pdo) ? $pdo : null);
    if (!$db) {
        throw new PDOException("Koneksi database tidak tersedia.");
    }

    // Ambil identifier user dari Query Param / POST Body
    $rawInput = file_get_contents("php://input");
    $bodyData = json_decode($rawInput, true) ?? [];

    $userId = $_GET['user_id'] ?? $_GET['id'] ?? $bodyData['user_id'] ?? $bodyData['id'] ?? 0;
    $username = $_GET['username'] ?? $bodyData['username'] ?? '';

    $userRow = null;
    if (!empty($userId)) {
        $stmt = $db->prepare("SELECT id, username, nama, role, status FROM users WHERE id = :id LIMIT 1");
        $stmt->execute([':id' => $userId]);
        $userRow = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$userRow) {
            $stmtAng = $db->prepare("SELECT id, username, nama, role, status FROM anggota WHERE id = :id LIMIT 1");
            $stmtAng->execute([':id' => $userId]);
            $userRow = $stmtAng->fetch(PDO::FETCH_ASSOC);
        }
    } elseif (!empty($username)) {
        $stmt = $db->prepare("SELECT id, username, nama, role, status FROM users WHERE username = :usr LIMIT 1");
        $stmt->execute([':usr' => $username]);
        $userRow = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$userRow) {
            $stmtAng = $db->prepare("SELECT id, username, nra as username, nama, role, status FROM anggota WHERE nra = :usr OR username = :usr LIMIT 1");
            $stmtAng->execute([':usr' => $username]);
            $userRow = $stmtAng->fetch(PDO::FETCH_ASSOC);
        }
    }

    // Nilai default jika data user tidak ditemukan
    $idUser = intval($userRow['id'] ?? $userId ?? 0);
    $name = $userRow['nama'] ?? $userRow['username'] ?? "Pengguna";
    $rawRole = strtolower(trim($userRow['role'] ?? 'member'));
    $rawStatus = strtolower(trim($userRow['status'] ?? 'verified'));

    // Status Verifikasi Akun
    $isVerified = ($rawStatus === 'verified' || $rawStatus === 'active' || $rawStatus === 'approved');

    // Penentuan Hak Akses (RBAC Permissions)
    $isAdminOrPengurus = in_array($rawRole, ['admin', 'pengurus', 'bendahara', 'developer']);

    $permissions = [
        "can_view_laporan" => true,
        "can_edit_kas" => $isAdminOrPengurus && $isVerified,
        "can_manage_users" => ($rawRole === 'admin' || $rawRole === 'developer') && $isVerified
    ];

    $response = [
        "status" => "success",
        "data" => [
            "id_user" => $idUser,
            "name" => $name,
            "role" => $rawRole,
            "is_verified" => $isVerified,
            "permissions" => $permissions
        ]
    ];

    echo json_encode($response, JSON_UNESCAPED_UNICODE);

} catch (PDOException $e) {
    error_log("Database Error in get_dashboard.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Terjadi kesalahan pada basis data server."
    ], JSON_UNESCAPED_UNICODE);
} catch (Throwable $e) {
    error_log("General Error in get_dashboard.php: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Terjadi kesalahan sistem internal."
    ], JSON_UNESCAPED_UNICODE);
}
?>
