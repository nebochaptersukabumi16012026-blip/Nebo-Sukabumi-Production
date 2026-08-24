<?php
// upload_banner.php
include_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
    exit();
}

if (!isset($_FILES['file'])) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "No file uploaded"]);
    exit();
}

$target_dir = "../uploads/";
if (!file_exists($target_dir)) {
    mkdir($target_dir, 0755, true);
}

$file_name = "community_banner.jpg";
$target_file = $target_dir . $file_name;

if (move_uploaded_file($_FILES["file"]["tmp_name"], $target_file)) {
    $url = "https://nebosukabumi.net/uploads/" . $file_name;
    
    // Update the database
    try {
        $stmt = $conn->prepare("UPDATE community_settings SET community_banner = :url, updated_at = :updated_at WHERE id = 1");
        $stmt->execute([
            ':url' => $url,
            ':updated_at' => round(microtime(true) * 1000)
        ]);
        echo json_encode(["status" => "success", "message" => "Upload berhasil", "url" => $url]);
    } catch (PDOException $e) {
        echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Upload gagal"]);
}
?>