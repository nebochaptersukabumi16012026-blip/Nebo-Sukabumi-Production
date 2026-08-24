<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

// Database Config
$host = "localhost";
$user = "nebk6483_nebo_user";
$pass = "nfGy7V!RpQkSKY3";
$db   = "nebk6483_nebo_db";

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Database connection failed"]));
}

// Simple Token Authentication (Hardcoded for simplicity, you can improve this)
$headers = apache_request_headers();
$auth = isset($headers['Authorization']) ? $headers['Authorization'] : '';
$token = str_replace('Bearer ', '', $auth);
$secret_token = "secure_token_12345"; // Change this!

// Optionally verify token here
// if ($token !== $secret_token && !isset($_GET['action']) || $_GET['action'] !== 'login') {
//     echo json_encode(["status" => "error", "message" => "Unauthorized"]);
//     exit;
// }

$action = isset($_GET['action']) ? $_GET['action'] : '';

// Handle JSON input
$inputJSON = file_get_contents('php://input');
$input = json_decode($inputJSON, TRUE); //convert JSON into array

switch ($action) {
    case 'login':
        $username = $input['username'] ?? '';
        $password = $input['password'] ?? '';
        $hash = hash('sha256', $password);
        
        $stmt = $conn->prepare("SELECT * FROM admin WHERE username=?");
        $stmt->bind_param("s", $username);
        $stmt->execute();
        $result = $stmt->get_result();
        if ($admin = $result->fetch_assoc()) {
            if ($admin['password_hash'] === $hash || $admin['password_hash'] === $password) {
                echo json_encode(["status" => "success", "role" => $admin['role'], "token" => $secret_token]);
            } else {
                echo json_encode(["status" => "error", "message" => "Password salah"]);
            }
        } else {
            // Check Anggota
            $stmt = $conn->prepare("SELECT * FROM anggota WHERE (nra=? OR alias=?) AND password=? AND statusAktif=1");
            $stmt->bind_param("sss", $username, $username, $password);
            $stmt->execute();
            $result = $stmt->get_result();
            if ($anggota = $result->fetch_assoc()) {
                echo json_encode(["status" => "success", "role" => "USER", "id" => $anggota['id'], "token" => $secret_token]);
            } else {
                echo json_encode(["status" => "error", "message" => "Username atau password salah"]);
            }
        }
        break;

    case 'get_anggota':
        $result = $conn->query("SELECT * FROM anggota");
        $data = [];
        while ($row = $result->fetch_assoc()) {
            $data[] = $row;
        }
        echo json_encode($data);
        break;

    case 'add_anggota':
        $stmt = $conn->prepare("INSERT INTO anggota (nra, nama, alias, telepon, tanggalLahir, jabatan, password) VALUES (?, ?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("sssssss", $input['nra'], $input['nama'], $input['alias'], $input['telepon'], $input['tanggalLahir'], $input['jabatan'], $input['password']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "id" => $stmt->insert_id]);
        } else {
            echo json_encode(["status" => "error", "message" => $stmt->error]);
        }
        break;

    case 'update_anggota':
        $stmt = $conn->prepare("UPDATE anggota SET nra=?, nama=?, alias=?, telepon=?, statusAktif=?, tanggalLahir=?, uangKas=?, iuranAniv=?, sisaCicilan=?, hargaBarang=?, namaBarang=?, jabatan=?, password=? WHERE id=?");
        $stmt->bind_param("ssssisddddsssi", $input['nra'], $input['nama'], $input['alias'], $input['telepon'], $input['statusAktif'], $input['tanggalLahir'], $input['uangKas'], $input['iuranAniv'], $input['sisaCicilan'], $input['hargaBarang'], $input['namaBarang'], $input['jabatan'], $input['password'], $input['id']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success"]);
        } else {
            echo json_encode(["status" => "error"]);
        }
        break;

    case 'delete_anggota':
        $stmt = $conn->prepare("DELETE FROM anggota WHERE id=?");
        $stmt->bind_param("i", $_GET['id']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success"]);
        } else {
            echo json_encode(["status" => "error"]);
        }
        break;

    case 'get_pembayaran':
        $result = $conn->query("SELECT * FROM pembayaran");
        $data = [];
        while ($row = $result->fetch_assoc()) {
            $data[] = $row;
        }
        echo json_encode($data);
        break;
        
    case 'add_pembayaran':
        $stmt = $conn->prepare("INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) VALUES (?, ?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("issdiss", $input['anggotaId'], $input['anggotaNama'], $input['jenisPembayaran'], $input['nominal'], $input['tanggal'], $input['keterangan'], $input['buktiPembayaran']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "id" => $stmt->insert_id]);
        } else {
            echo json_encode(["status" => "error"]);
        }
        break;
        
    case 'get_pengeluaran':
        $result = $conn->query("SELECT * FROM pengeluaran");
        $data = [];
        while ($row = $result->fetch_assoc()) {
            $data[] = $row;
        }
        echo json_encode($data);
        break;
        
    case 'add_pengeluaran':
        $stmt = $conn->prepare("INSERT INTO pengeluaran (keterangan, nominal, tanggal, jenisKas) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("sdis", $input['keterangan'], $input['nominal'], $input['tanggal'], $input['jenisKas']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "id" => $stmt->insert_id]);
        } else {
            echo json_encode(["status" => "error"]);
        }
        break;
        
    case 'get_kas_keliling':
        $result = $conn->query("SELECT * FROM kas_keliling");
        $data = [];
        while ($row = $result->fetch_assoc()) {
            $data[] = $row;
        }
        echo json_encode($data);
        break;
        
    case 'add_kas_keliling':
        $stmt = $conn->prepare("INSERT INTO kas_keliling (nominal, tanggal, keterangan, jenisTransaksi, bulan, tahun, totalPemasukan, totalPengeluaran, saldoBulan, catatan) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("dissssddds", $input['nominal'], $input['tanggal'], $input['keterangan'], $input['jenisTransaksi'], $input['bulan'], $input['tahun'], $input['totalPemasukan'], $input['totalPengeluaran'], $input['saldoBulan'], $input['catatan']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "id" => $stmt->insert_id]);
        } else {
            echo json_encode(["status" => "error"]);
        }
        break;

    case 'get_catatan':
        $result = $conn->query("SELECT * FROM catatan");
        $data = [];
        while ($row = $result->fetch_assoc()) {
            $data[] = $row;
        }
        echo json_encode($data);
        break;
        
    case 'save_catatan':
        // using REPLACE INTO or UPDATE
        $stmt = $conn->prepare("REPLACE INTO catatan (id, title, content, timestamp) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("issi", $input['id'], $input['title'], $input['content'], $input['timestamp']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success"]);
        } else {
            echo json_encode(["status" => "error"]);
        }
        break;
        
    case 'delete_catatan':
        $stmt = $conn->prepare("DELETE FROM catatan WHERE id=?");
        $stmt->bind_param("i", $_GET['id']);
        if ($stmt->execute()) {
            echo json_encode(["status" => "success"]);
        } else {
            echo json_encode(["status" => "error"]);
        }
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}

$conn->close();
?>
<?php
// ... this is just mock for documentation. The app uses retrofit.
