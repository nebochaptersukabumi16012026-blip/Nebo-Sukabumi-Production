<?php
// debug_aniv_schema.php - Diagnostic script for Anniversary JSON structure and schema validation
header("Content-Type: application/json; charset=UTF-8");
include_once 'config.php';

$debugLog = __DIR__ . '/aniv_schema_debug.log';

try {
    $stmt = $conn->query("SELECT * FROM iuran_anniversary ORDER BY id DESC LIMIT 5");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $expectedFields = array('id', 'anggota_id', 'nominal', 'tanggal', 'keterangan');
    $issues = array();
    $processedRows = array();

    foreach ($rows as $index => $row) {
        $rowKeys = array_keys($row);
        $missing = array_diff($expectedFields, $rowKeys);
        $extra = array_diff($rowKeys, $expectedFields);

        if (!empty($missing) || !empty($extra)) {
            $issues[] = array(
                "row_index" => $index,
                "id" => $row['id'] ?? null,
                "missing_fields" => $missing,
                "extra_fields" => $extra
            );
        }

        $processedRows[] = array(
            "id" => intval($row['id'] ?? 0),
            "anggota_id" => intval($row['anggota_id'] ?? 0),
            "nominal" => floatval($row['nominal'] ?? 0),
            "tanggal" => strval($row['tanggal'] ?? ''),
            "keterangan" => strval($row['keterangan'] ?? '')
        );
    }

    $ids = array_column($processedRows, 'id');
    $duplicateCounts = array_count_values($ids);
    $duplicateIds = array();
    foreach ($duplicateCounts as $idVal => $count) {
        if ($count > 1) {
            $duplicateIds[] = $idVal;
        }
    }

    $diagnosticResult = array(
        "status" => "success",
        "timestamp" => date("Y-m-d H:i:s"),
        "total_records_checked" => count($rows),
        "expected_schema" => $expectedFields,
        "schema_discrepancies" => $issues,
        "duplicate_ids_found" => $duplicateIds,
        "sample_data" => $processedRows
    );

    file_put_contents($debugLog, "[" . date('Y-m-d H:i:s') . "] SCHEMA_CHECK: " . json_encode($diagnosticResult, JSON_PRETTY_PRINT) . "\n", FILE_APPEND);

    echo json_encode($diagnosticResult, JSON_PRETTY_PRINT);

} catch (Throwable $e) {
    $err = array("status" => "error", "message" => $e->getMessage());
    file_put_contents($debugLog, "[" . date('Y-m-d H:i:s') . "] ERROR: " . $e->getMessage() . "\n", FILE_APPEND);
    echo json_encode($err);
}
?>
