<?php
// workspace/refactored_api/core/config.php
// Konfigurasi Terpusat, Koneksi Database PDO, CORS, dan Helper Response

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With, Cache-Control");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

date_default_timezone_set('Asia/Jakarta');

// Error handling settings
error_reporting(0);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

// OPTIONS Preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database Credentials (Sesuai Konfigurasi Produksi / Eksisting)
$host     = "localhost";
$db_name  = "nebk6483_nebo_db";
$username = "nebk6483_nebo_user";
$password = "nfGy7V!RpQkSKY3";

try {
    $pdo = new PDO("mysql:host={$host};dbname={$db_name};charset=utf8mb4", $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false
    ]);
    $conn = $pdo; // Alias kompatibilitas
} catch (PDOException $e) {
    // Fallback jika database lokal belum running (misal lingkungan testing non-MySQL)
    error_log("Database connection failed: " . $e->getMessage());
    $pdo = null;
    $conn = null;
}

/**
 * Helper untuk membaca input request secara universal:
 * Mendukung JSON php://input, form-urlencoded $_POST, multipart $_POST, dan $_GET
 */
function getRequestBody() {
    $raw = file_get_contents("php://input");
    $data = [];
    if (!empty($raw)) {
        $json = json_decode($raw, true);
        if (is_array($json)) {
            $data = $json;
        }
    }
    // Merge dengan $_POST dan $_GET tanpa menimpa data body utama
    $merged = array_merge($_GET, $_POST, $data);
    return $merged;
}

/**
 * Helper untuk format response JSON standar
 * Kompatibel dengan Android Retrofit BaseResponse<T> dan Frontend Web
 */
function sendResponse($success, $message, $data = null, $extra = [], $http_code = 200) {
    http_response_code($http_code);
    $response = [
        "status" => $success ? (is_bool($success) ? true : $success) : false,
        "success" => (bool)$success,
        "message" => $message
    ];
    if ($data !== null) {
        $response["data"] = $data;
    }
    if (!empty($extra) && is_array($extra)) {
        foreach ($extra as $key => $val) {
            $response[$key] = $val;
        }
    }
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit();
}

/**
 * Validasi otorisasi peran pengguna (Role Verification)
 */
function verifyRole($allowedRoles = ['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']) {
    $input = getRequestBody();
    $role = '';
    if (!empty($input['role'])) $role = trim($input['role']);
    elseif (!empty($input['user_role'])) $role = trim($input['user_role']);
    elseif (!empty($input['role_login'])) $role = trim($input['role_login']);
    elseif (!empty($input['login_role'])) $role = trim($input['login_role']);
    elseif (!empty($_SERVER['HTTP_X_USER_ROLE'])) $role = trim($_SERVER['HTTP_X_USER_ROLE']);

    if (empty($role)) {
        // Jika tidak disertakan, periksa jika ini GET publik
        return true;
    }
    $role_upper = strtoupper($role);
    $allowed_upper = array_map('strtoupper', $allowedRoles);
    if (!in_array($role_upper, $allowed_upper)) {
        sendResponse(false, "Akses ditolak: Anda tidak memiliki hak akses yang mencukupi ($role_upper)", null, [], 403);
    }
    return true;
}

/**
 * Nama bulan dalam bahasa Indonesia
 */
function getIndonesianMonthName($monthNum) {
    $months = [
        1 => "Januari", 2 => "Februari", 3 => "Maret", 4 => "April",
        5 => "Mei", 6 => "Juni", 7 => "Juli", 8 => "Agustus",
        9 => "September", 10 => "Oktober", 11 => "November", 12 => "Desember"
    ];
    return $months[(int)$monthNum] ?? "Unknown";
}
