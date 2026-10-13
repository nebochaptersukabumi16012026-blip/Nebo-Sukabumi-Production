<?php
// debug_anniversary_delete.php - Secure diagnostic script for Anniversary deletion tracing
// Runs in read-only / safe dry-run mode by default or logs execution details securely.
// Requirements: Log path outside web root (/tmp/debug_delete.log), sensitive data redacted.

header("Content-Type: application/json; charset=UTF-8");
include_once 'config.php';
include_once 'sync_helper.php';

$logFile = '/tmp/debug_delete.log';

// 1. Read input safely and redact sensitive keys
$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput, true) ?: $_REQUEST;

$safeData = $data;
$sensitiveKeys = array('password', 'pass', 'token', 'auth', 'authorization', 'cookie', 'secret');
foreach ($sensitiveKeys as $key) {
    if (isset($safeData[$key])) {
        $safeData[$key] = '[REDACTED]';
    }
}

$id = intval($safeData['id'] ?? $safeData['id_aniv'] ?? $safeData['id_transaksi'] ?? 0);

$logEntry = "[" . date('Y-m-d H:i:s') . "] TRACE: Received ID = " . $id . " | Safe Payload = " . json_encode($safeData) . "\n";
@file_put_contents($logFile, $logEntry, FILE_APPEND);

if ($id <= 0) {
    $msg = "[" . date('Y-m-d H:i:s') . "] ERROR: Invalid ID provided ($id)\n";
    @file_put_contents($logFile, $msg, FILE_APPEND);
    echo json_encode(array("status" => false, "message" => "Invalid ID provided"));
    exit();
}

// 2. Read-only diagnostic check (Dry Run): check existence across tables without modifying live data
try {
    $existenceReport = array();

    $stmt1 = $conn->prepare("SELECT COUNT(*) as cnt, anggota_id, nominal FROM iuran_anniversary WHERE id = ? GROUP BY anggota_id, nominal");
    $stmt1->execute(array($id));
    $existenceReport['iuran_anniversary'] = $stmt1->fetchAll(PDO::FETCH_ASSOC);

    $stmt2 = $conn->prepare("SELECT COUNT(*) as cnt, id_anggota, nominal FROM riwayat_aniv WHERE id = ? GROUP BY id_anggota, nominal");
    $stmt2->execute(array($id));
    $existenceReport['riwayat_aniv'] = $stmt2->fetchAll(PDO::FETCH_ASSOC);

    $stmt3 = $conn->prepare("SELECT COUNT(*) as cnt, anggotaId, nominal FROM pembayaran WHERE id = ? AND jenisPembayaran = 'ANIV' GROUP BY anggotaId, nominal");
    $stmt3->execute(array($id));
    $existenceReport['pembayaran'] = $stmt3->fetchAll(PDO::FETCH_ASSOC);

    $logReport = "[" . date('Y-m-d H:i:s') . "] DRY_RUN_CHECK: ID=$id Existence Report = " . json_encode($existenceReport) . "\n\n";
    @file_put_contents($logFile, $logReport, FILE_APPEND);

    echo json_encode(array(
        "status" => true,
        "message" => "Diagnostic trace completed successfully (Safe Read-Only Mode)",
        "id_checked" => $id,
        "existence" => $existenceReport
    ));

} catch (Throwable $e) {
    $errMsg = "[" . date('Y-m-d H:i:s') . "] DATABASE ERROR: " . $e->getMessage() . "\n\n";
    @file_put_contents($logFile, $errMsg, FILE_APPEND);
    echo json_encode(array("status" => false, "message" => "Database error: " . $e->getMessage()));
}
?>
