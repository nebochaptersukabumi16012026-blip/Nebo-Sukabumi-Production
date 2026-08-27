<?php
// login.php
include_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method == 'POST') {
    $data = json_decode(file_get_contents("php://input"));
    
    if(!empty($data->username) && !empty($data->password)) {
        try {
            $query = "SELECT id, username, role, password, NULL as nra FROM users WHERE username = :username LIMIT 1";
            $stmt = $conn->prepare($query);
            $stmt->bindParam(':username', $data->username);
            
            if ($stmt->execute()) {
                $row = $stmt->fetch(PDO::FETCH_ASSOC);
                if (!$row) {
                    // Fallback to check the anggota table!
                    $query_ang = "SELECT id, username, role, password, nra FROM anggota WHERE (username = :username OR nra = :username) LIMIT 1";
                    $stmt_ang = $conn->prepare($query_ang);
                    $stmt_ang->bindParam(':username', $data->username);
                    if ($stmt_ang->execute()) {
                        $row = $stmt_ang->fetch(PDO::FETCH_ASSOC);
                        if ($row) {
                            if (empty($row['username'])) {
                                $row['username'] = $row['nra'];
                            }
                            $role = strtoupper($row['role']);
                            if ($role != 'BENDAHARA' && $role != 'ADMIN' && $role != 'DEVELOPER') {
                                $row['role'] = 'USER';
                            } else {
                                $row['role'] = $role;
                            }
                        }
                    }
                }

                if ($row) {
                    $db_password = $row['password'];
                    $input_password = $data->password;
                    
                    // Plaintext or bcrypt comparison
                    $is_valid = false;
                    if ($input_password === $db_password) {
                        $is_valid = true;
                    } elseif (function_exists('password_verify']) {
                        if (password_verify($input_password, $db_password)) {
                            $is_valid = true;
                        }
                    }
                    
                    if ($is_valid) {
                        // Check if they have an approved reset request with matching password_sementara
                        $require_new_password = false;
                        $request_id = 0;
                        $username_to_check = !empty($row['nra']) ? $row['nra'] : $row['username'];
                        
                        $stmt_req = $conn->prepare("SELECT id, password_sementara FROM reset_password_requests WHERE nra = :nra AND status = 'Disetujui' LIMIT 1");
                        $stmt_req->execute(array(':nra' => $username_to_check));
                        $req_row = $stmt_req->fetch(PDO::FETCH_ASSOC);
                        if ($req_row && $input_password === $req_row['password_sementara']) {
                            $require_new_password = true;
                            $request_id = intval($req_row['id']);
                        }

                        error_log("Login sukses untuk user: " . $data->username);
                        echo json_encode(array(
                            "status" => "success",
                            "success" => true,
                            "message" => "Login berhasil",
                            "data" => array(
                                "id" => intval($row['id']),
                                "username" => $row['username'],
                                "role" => $row['role'],
                                "require_new_password" => $require_new_password,
                                "request_id" => $request_id
                            )
                        ));
                    } else {
                        error_log("Login gagal: Password salah untuk user " . $data->username);
                        echo json_encode(array(
                            "status" => "error",
                            "success" => false,
                            "message" => "Password salah"
                        ));
                    }
                } else {
                    error_log("Login gagal: User tidak ditemukan (" . $data->username . ")");
                    echo json_encode(array(
                        "status" => "error",
                        "success" => false,
                        "message" => "User tidak ditemukan"
                    ));
                }
            } else {
                error_log("Login gagal: Query gagal dieksekusi");
                echo json_encode(array(
                    "status" => "error",
                    "success" => false,
                    "message" => "Query gagal dieksekusi"
                ));
            }
        } catch (PDOException $e) {
            error_log("Login gagal: Database error - " . $e->getMessage());
            echo json_encode(array(
                "status" => "error",
                "success" => false,
                "message" => "Database gagal: " . $e->getMessage()
            ));
        }
    } else {
        echo json_encode(array(
            "status" => "error",
            "success" => false,
            "message" => "Username atau password tidak boleh kosong."
        ));
    }
} else {
    http_response_code(405);
    echo json_encode(array(
        "status" => "error",
        "success" => false,
        "message" => "Method Not Allowed"
    ));
}
?>