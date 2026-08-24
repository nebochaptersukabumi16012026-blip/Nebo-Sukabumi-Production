<?php
// dev_users.php
include_once 'config.php';

$req_role = isset($_SERVER['HTTP_X_USER_ROLE']) ? strtoupper($_SERVER['HTTP_X_USER_ROLE']) : 'GUEST';
if ($req_role !== 'DEVELOPER') {
    echo json_encode(["status" => "error", "message" => "Akses ditolak: Hanya DEVELOPER"]);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method == 'GET') {
    try {
        error_log("DEBUG_GET_ANGGOTA: Fetching master data from anggota");
        
        // Fetch all members from the Master table: anggota
        $query = "SELECT id, nama, nra, username, password, role FROM anggota ORDER BY nama ASC";
        $stmt = $conn->query($query);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        $result = [];
        foreach ($rows as $row) {
            $username = isset($row['username']) ? trim($row['username']) : '';
            $has_account = (!empty($username) && $username !== '');
            $result[] = [
                "id" => intval($row['id']),
                "nama" => $row['nama'],
                "nra" => !empty($row['nra']) ? $row['nra'] : '',
                "username" => $username,
                "role" => !empty($row['role']) ? $row['role'] : 'USER',
                "password" => !empty($row['password']) ? $row['password'] : '',
                "status_akun" => $has_account ? "Sudah memiliki akun" : "Belum memiliki akun"
            ];
        }
        
        error_log("DEBUG_GET_ANGGOTA: Successfully loaded " . count($result) . " members.");
        echo json_encode(["status" => "success", "data" => $result]);
    } catch (PDOException $e) {
        error_log("DEBUG_GET_ANGGOTA error: " . $e->getMessage());
        echo json_encode(["status" => "error", "message" => $e->getMessage()]);
    }
    exit();
}

if ($method == 'POST') {
    $data = json_decode(file_get_contents("php://input"));
    if (!$data) {
        echo json_encode(["status" => "error", "message" => "Invalid JSON payload."]);
        exit();
    }
    
    $action = $data->action ?? '';
    
    if ($action === 'create') {
        $anggota_id = isset($data->anggota_id) ? intval($data->anggota_id) : 0;
        $username = $data->username ?? '';
        $password = $data->password ?? '';
        $role = $data->role ?? 'USER';
        
        if ($anggota_id <= 0 || empty($username) || empty($password)) {
            echo json_encode(["status" => "error", "message" => "Parameter tidak lengkap untuk membuat akun."]);
            exit();
        }
        
        try {
            // Check if username already exists in users table
            $stmt_check = $conn->prepare("SELECT id FROM users WHERE username = ?");
            $stmt_check->execute([$username]);
            if ($stmt_check->fetch()) {
                echo json_encode(["status" => "error", "message" => "Username sudah digunakan oleh akun lain."]);
                exit();
            }
            
            // 1. UPDATE anggota
            error_log("DEBUG_UPDATE_USERNAME: Updating anggota with id $anggota_id to username $username");
            $stmt_update = $conn->prepare("UPDATE anggota SET username = ?, password = ?, role = ? WHERE id = ?");
            $stmt_update->execute([$username, $password, $role, $anggota_id]);
            
            // 2. INSERT users
            error_log("DEBUG_INSERT_USER: Inserting login credential for username $username with role $role");
            $stmt_insert = $conn->prepare("INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
            $stmt_insert->execute([$username, $password, $role]);
            
            error_log("DEBUG_CREATE_USER: Account successfully created for user $username");
            echo json_encode(["status" => "success", "message" => "Akun berhasil dibuat."]);
        } catch (PDOException $e) {
            error_log("DEBUG_CREATE_USER error: " . $e->getMessage());
            echo json_encode(["status" => "error", "message" => $e->getMessage()]);
        }
        exit();
    } elseif ($action === 'edit') {
        $username = $data->username ?? '';
        $password = $data->password ?? '';
        
        if (empty($username) || empty($password)) {
            echo json_encode(["status" => "error", "message" => "Username dan password baru wajib diisi."]);
            exit();
        }
        
        try {
            // UPDATE users.password only as requested: "UPDATE users.password. Jangan mengubah data anggota."
            error_log("DEBUG_UPDATE_PASSWORD: Changing password for username $username in users table");
            $stmt_update = $conn->prepare("UPDATE users SET password = ? WHERE username = ?");
            $stmt_update->execute([$password, $username]);
            
            echo json_encode(["status" => "success", "message" => "Password berhasil diperbarui."]);
        } catch (PDOException $e) {
            error_log("DEBUG_UPDATE_PASSWORD error: " . $e->getMessage());
            echo json_encode(["status" => "error", "message" => $e->getMessage()]);
        }
        exit();
    } else {
        echo json_encode(["status" => "error", "message" => "Action tidak dikenal."]);
        exit();
    }
}

http_response_code(405);
echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
?>
