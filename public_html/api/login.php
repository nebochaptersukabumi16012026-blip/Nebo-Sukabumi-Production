<?php
// login.php
header('Content-Type: application/json; charset=UTF-8');
include_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method !== 'POST') {
    http_response_code(405);
    echo json_encode([
        'status' => 'error',
        'success' => false,
        'message' => 'Method Not Allowed'
    ]);
    exit();
}

try {
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput);

    $usernameInput = isset($data->username) ? trim($data->username) : '';
    $password = isset($data->password) ? $data->password : '';

    if (empty($usernameInput) || empty($password)) {
        echo json_encode([
            'status' => 'error',
            'success' => false,
            'message' => 'INPUT_EMPTY',
            'message_detail' => 'Username/NRA dan password tidak boleh kosong.'
        ]);
        exit();
    }

    $user = null;

    // 1. Check in users table (e.g. admin, bendahara)
    $stmt = $conn->prepare("SELECT id, username, role, password, NULL as nama, NULL as nra FROM users WHERE username = :input LIMIT 1");
    $stmt->execute([':input' => $usernameInput]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    // 2. If not found in users table, check in anggota table using nra OR username without filtering role
    if (!$user) {
        $sql = "SELECT * FROM anggota WHERE nra = :input OR username = :input LIMIT 1";
        $stmt_ang = $conn->prepare($sql);
        $stmt_ang->execute([':input' => $usernameInput]);
        $user = $stmt_ang->fetch(PDO::FETCH_ASSOC);
    }

    if ($user) {
        $db_password = isset($user['password']) ? $user['password'] : '';
        
        // 3. Dukungan verifikasi password ganda (plain text & password_verify)
        $is_password_valid = password_verify($password, $db_password) || ($password === $db_password);

        if ($is_password_valid) {
            $require_new_password = false;
            $request_id = 0;
            $nra_to_check = !empty($user['nra']) ? $user['nra'] : (isset($user['username']) ? $user['username'] : $usernameInput);

            try {
                $stmt_req = $conn->prepare("SELECT id, password_sementara FROM reset_password_requests WHERE nra = :nra AND status = 'Disetujui' LIMIT 1");
                $stmt_req->execute([':nra' => $nra_to_check]);
                $req_row = $stmt_req->fetch(PDO::FETCH_ASSOC);
                if ($req_row && $password === $req_row['password_sementara']) {
                    $require_new_password = true;
                    $request_id = intval($req_row['id']);
                }
            } catch (Exception $ex) {
                // Ignore if table doesn't exist
            }

            $userId = isset($user['id']) ? intval($user['id']) : 0;
            $userName = isset($user['nama']) ? $user['nama'] : (isset($user['username']) ? $user['username'] : $usernameInput);
            $userNra = isset($user['nra']) ? $user['nra'] : $usernameInput;

            // 1. Sanitasi dan normalisasi role
            $role = isset($user['role']) && trim($user['role']) !== '' ? strtoupper(trim($user['role'])) : 'USER';

            // 4. Response JSON yang konsisten
            echo json_encode([
                'status' => 'success',
                'success' => true,
                'message' => 'Login berhasil',
                'data' => [
                    'id' => $userId,
                    'username' => $userNra ?: $userName,
                    'nama' => $userName,
                    'nra' => $userNra,
                    'role' => $role,
                    'require_new_password' => $require_new_password,
                    'request_id' => $request_id
                ],
                'user' => [
                    'id' => $userId,
                    'nama' => $userName,
                    'nra' => $userNra,
                    'role' => $role
                ]
            ]);
        } else {
            echo json_encode([
                'status' => 'error',
                'success' => false,
                'message' => 'WRONG_CREDENTIALS',
                'message_detail' => 'Password salah'
            ]);
        }
    } else {
        echo json_encode([
            'status' => 'error',
            'success' => false,
            'message' => 'USER_NOT_FOUND',
            'message_detail' => 'User tidak ditemukan'
        ]);
    }
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
