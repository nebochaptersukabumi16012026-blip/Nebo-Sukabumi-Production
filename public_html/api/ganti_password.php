<?php
// reset_password.php
include_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];
$action = isset($_GET['action']) ? $_GET['action'] : '';

if ($method == 'GET' && $action == 'list') {
    try {
        $stmt = $conn->query("SELECT * FROM reset_password_requests ORDER BY id DESC");
        $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        echo json_encode(array("status" => "success", "data" => $result));
    } catch (PDOException $e) {
        echo json_encode(array("status" => "error", "message" => "Database error: " . $e->getMessage()));
    }
} else if ($method == 'POST') {
    $data = json_decode(file_get_contents("php://input"));
    
    if ($action == 'request') {
        $input = isset($data->nra) ? trim($data->nra) : (isset($data->username) ? trim($data->username) : '');
        if (empty($input)) {
            echo json_encode(array("status" => "error", "message" => "NRA atau Username wajib diisi."));
            exit();
        }
        
        try {
            // Check if NRA/Username exists in anggota table
            $stmt = $conn->prepare("SELECT nama, nra, role, username FROM anggota WHERE nra = :input OR username = :input LIMIT 1");
            $stmt->execute(array(':input' => $input));
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if (!$row) {
                echo json_encode(array("status" => "error", "message" => "NRA tidak ditemukan."));
                exit();
            }
            
            $nama = $row['nama'];
            $nra = !empty($row['nra']) ? $row['nra'] : $row['username'];
            $role = !empty($row['role']) ? $row['role'] : 'Anggota';
            $tanggal = date('Y-m-d');
            $jam = date('H:i:s');
            
            // Insert request
            $stmt_insert = $conn->prepare("INSERT INTO reset_password_requests (nama_anggota, nra, tanggal, jam, role, status) VALUES (:nama, :nra, :tanggal, :jam, :role, 'Menunggu Persetujuan Admin')");
            $stmt_insert->execute(array(
                ':nama' => $nama,
                ':nra' => $nra,
                ':tanggal' => $tanggal,
                ':jam' => $jam,
                ':role' => $role
            ));
            
            // Send email
            $to = "nebochaptersukabumi16012026@gmail.com";
            $subject = "Permintaan Reset Password";
            $message = "Nama Anggota: " . $nama . "\r\n" .
                       "NRA: " . $nra . "\r\n" .
                       "Tanggal: " . $tanggal . "\r\n" .
                       "Jam: " . $jam . "\r\n" .
                       "Role: " . $role . "\r\n" .
                       "Status: Menunggu Persetujuan Admin\r\n";
                       
            $headers = "From: nebochaptersukabumi16012026@gmail.com\r\n" .
                       "Reply-To: nebochaptersukabumi16012026@gmail.com\r\n" .
                       "X-Mailer: PHP/" . phpversion();
                       
            @mail($to, $subject, $message, $headers);
            
            echo json_encode(array(
                "status" => "success",
                "message" => "Permintaan reset password berhasil dikirim ke email Admin."
            ));
            
        } catch (PDOException $e) {
            echo json_encode(array("status" => "error", "message" => "Database error: " . $e->getMessage()));
        }
    } else if ($action == 'approve') {
        $id = isset($data->id) ? intval($data->id) : 0;
        if ($id <= 0) {
            echo json_encode(array("status" => "error", "message" => "ID permintaan tidak valid."));
            exit();
        }
        
        try {
            // Find the request
            $stmt = $conn->prepare("SELECT * FROM reset_password_requests WHERE id = ? LIMIT 1");
            $stmt->execute(array($id));
            $request = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if (!$request) {
                echo json_encode(array("status" => "error", "message" => "Permintaan tidak ditemukan."));
                exit();
            }
            
            $nra = $request['nra'];
            
            // Generate temporary password
            $temp_password = "Nebo" . rand(10000, 99999);
            $hashed_password = password_hash($temp_password, PASSWORD_BCRYPT);
            
            // Update request
            $stmt_update = $conn->prepare("UPDATE reset_password_requests SET status = 'Disetujui', password_sementara = :temp WHERE id = :id");
            $stmt_update->execute(array(
                ':temp' => $temp_password,
                ':id' => $id
            ));
            
            // Update anggota table
            $stmt_ang = $conn->prepare("UPDATE anggota SET password = :hashed WHERE nra = :nra OR username = :nra");
            $stmt_ang->execute(array(
                ':hashed' => $hashed_password,
                ':nra' => $nra
            ));
            
            // Update users table (if exists)
            $stmt_usr = $conn->prepare("UPDATE users SET password = :hashed WHERE username = :nra");
            $stmt_usr->execute(array(
                ':hashed' => $hashed_password,
                ':nra' => $nra
            ));
            
            echo json_encode(array(
                "status" => "success",
                "message" => "Permintaan reset password disetujui.",
                "data" => [
                    "password_sementara" => $temp_password
                ]
            ));
            
        } catch (PDOException $e) {
            echo json_encode(array("status" => "error", "message" => "Database error: " . $e->getMessage()));
        }
    } else if ($action == 'reject') {
        $id = isset($data->id) ? intval($data->id) : 0;
        if ($id <= 0) {
            echo json_encode(array("status" => "error", "message" => "ID permintaan tidak valid."));
            exit();
        }
        
        try {
            $stmt_update = $conn->prepare("UPDATE reset_password_requests SET status = 'Ditolak' WHERE id = ?");
            $stmt_update->execute(array($id));
            
            echo json_encode(array(
                "status" => "success",
                "message" => "Permintaan reset password ditolak."
            ));
        } catch (PDOException $e) {
            echo json_encode(array("status" => "error", "message" => "Database error: " . $e->getMessage()));
        }
    } else if ($action == 'reset_complete') {
        $nra = isset($data->nra) ? trim($data->nra) : (isset($data->username) ? trim($data->username) : '');
        $password_baru = isset($data->password_baru) ? trim($data->password_baru) : '';
        $request_id = isset($data->request_id) ? intval($data->request_id) : 0;
        
        if (empty($nra) || empty($password_baru)) {
            echo json_encode(array("status" => "error", "message" => "Data tidak lengkap."));
            exit();
        }
        
        try {
            $hashed_password = password_hash($password_baru, PASSWORD_BCRYPT);
            
            // Update anggota table
            $stmt_ang = $conn->prepare("UPDATE anggota SET password = :hashed WHERE nra = :nra OR username = :nra");
            $stmt_ang->execute(array(
                ':hashed' => $hashed_password,
                ':nra' => $nra
            ));
            
            // Update users table (if exists)
            $stmt_usr = $conn->prepare("UPDATE users SET password = :hashed WHERE username = :nra");
            $stmt_usr->execute(array(
                ':hashed' => $hashed_password,
                ':nra' => $nra
            ));
            
            // Update request status to Selesai
            if ($request_id > 0) {
                $stmt_req = $conn->prepare("UPDATE reset_password_requests SET status = 'Selesai', password_sementara = NULL WHERE id = ?");
                $stmt_req->execute(array($request_id));
            } else {
                $stmt_req = $conn->prepare("UPDATE reset_password_requests SET status = 'Selesai', password_sementara = NULL WHERE nra = ? AND status = 'Disetujui'");
                $stmt_req->execute(array($nra));
            }
            
            echo json_encode(array(
                "status" => "success",
                "message" => "Password baru berhasil disimpan."
            ));
        } catch (PDOException $e) {
            echo json_encode(array("status" => "error", "message" => "Database error: " . $e->getMessage()));
        }
    } else if (empty($action)) {
        // Fallback for original Ganti Password feature
        if (isset($data->id) && !empty($data->password_lama) && !empty($data->password_baru)) {
            try {
                $id = intval($data->id);
                $password_lama = $data->password_lama;
                $password_baru = $data->password_baru;
                
                // Find in anggota first (since most user profiles are linked to anggota)
                $stmt = $conn->prepare("SELECT id, username, password FROM anggota WHERE id = :id LIMIT 1");
                $stmt->bindParam(':id', $id);
                $stmt->execute();
                $row = $stmt->fetch(PDO::FETCH_ASSOC);
                
                if ($row) {
                    $db_password = $row['password'];
                    $username = $row['username'];
                    
                    // Verify password_lama (support both plaintext and hashed)
                    $is_valid = ($password_lama === $db_password || password_verify($password_lama, $db_password));
                    
                    if ($is_valid) {
                        $hashed_password = password_hash($password_baru, PASSWORD_BCRYPT);
                        // Update in anggota table
                        $stmt_up_ang = $conn->prepare("UPDATE anggota SET password = :new_pass WHERE id = :id");
                        $stmt_up_ang->execute(array(':new_pass' => $hashed_password, ':id' => $id));
                        
                        // If username is set, also update in users table
                        if (!empty($username)) {
                            $stmt_up_usr = $conn->prepare("UPDATE users SET password = :new_pass WHERE username = :username");
                            $stmt_up_usr->execute(array(':new_pass' => $hashed_password, ':username' => $username));
                        }
                        
                        echo json_encode(array(
                            "status" => "success",
                            "message" => "Password berhasil diperbarui"
                        ));
                    } else {
                        echo json_encode(array(
                            "status" => "error",
                            "message" => "Password lama salah"
                        ));
                    }
                } else {
                    // If not found in anggota, check users table (e.g., standalone admin/bendahara)
                    $stmt_usr = $conn->prepare("SELECT id, username, password FROM users WHERE id = :id LIMIT 1");
                    $stmt_usr->bindParam(':id', $id);
                    $stmt_usr->execute();
                    $row_usr = $stmt_usr->fetch(PDO::FETCH_ASSOC);
                    
                    if ($row_usr) {
                        $db_password = $row_usr['password'];
                        $username = $row_usr['username'];
                        
                        $is_valid = ($password_lama === $db_password || password_verify($password_lama, $db_password));
                        
                        if ($is_valid) {
                            $hashed_password = password_hash($password_baru, PASSWORD_BCRYPT);
                            // Update in users table
                            $stmt_up_usr = $conn->prepare("UPDATE users SET password = :new_pass WHERE id = :id");
                            $stmt_up_usr->execute(array(':new_pass' => $hashed_password, ':id' => $id));
                            
                            // Also try updating in anggota table if there's any matching username
                            $stmt_up_ang = $conn->prepare("UPDATE anggota SET password = :new_pass WHERE username = :username");
                            $stmt_up_ang->execute(array(':new_pass' => $hashed_password, ':username' => $username));
                            
                            echo json_encode(array(
                                "status" => "success",
                                "message" => "Password berhasil diperbarui"
                            ));
                        } else {
                            echo json_encode(array(
                                "status" => "error",
                                "message" => "Password lama salah"
                            ));
                        }
                    } else {
                        echo json_encode(array(
                            "status" => "error",
                            "message" => "User tidak ditemukan"
                        ));
                    }
                }
            } catch (PDOException $e) {
                echo json_encode(array(
                    "status" => "error",
                    "message" => "Gagal memperbarui password: " . $e->getMessage()
                ));
            }
        } else {
            echo json_encode(array(
                "status" => "error",
                "message" => "Parameter tidak lengkap"
            ));
        }
    } else {
        http_response_code(400);
        echo json_encode(array("status" => "error", "message" => "Action tidak valid."));
    }
} else {
    http_response_code(405);
    echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
}
?>