<?php
// ==============================================================================
// NEBO SUKABUMI - DEFAULT API ROUTER & STATUS ENDPOINT
// ==============================================================================
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
header('Content-Type: application/json; charset=UTF-8');
header('Cache-Control: no-cache, no-store, must-revalidate');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

http_response_code(200);
echo json_encode([
    "status" => "success",
    "message" => "NEBO Sukabumi API Engine Running",
    "version" => "1.0.0",
    "server_time" => date('Y-m-d H:i:s')
]);
?>
