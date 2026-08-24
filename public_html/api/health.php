<?php
include_once 'config.php';
header("Content-Type: application/json; charset=UTF-8");
try {
    $stmt = $conn->query("SELECT 1");
    echo json_encode(["status" => "success", "message" => "Server and database are healthy"]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Health check failed: " . $e->getMessage()]);
}
?>
