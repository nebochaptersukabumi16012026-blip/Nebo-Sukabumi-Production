<?php
// reset_data.php
include_once 'config.php';

header("Content-Type: application/json; charset=UTF-8");

try {
    // List of tables to truncate / reset
    $tablesToReset = [
        'catatan',
        'kas_keliling',
        'iuran_anniversary',
        'pengeluaran',
        'cicilan',
        'absensi',
        'reset_password_requests'
    ];

    foreach ($tablesToReset as $table) {
        try {
            $pdo->exec("TRUNCATE TABLE $table");
        } catch (PDOException $e) {
            // Fallback if TRUNCATE fails due to FK
            $pdo->exec("DELETE FROM $table");
        }
    }

    // Reset anggota table if requested (or clear transactional fields)
    try {
        $pdo->exec("DELETE FROM anggota");
    } catch (PDOException $e) {
        // Ignore if table doesn't exist
    }

    echo json_encode([
        "status" => "success",
        "message" => "Seluruh data transaksi, catatan, dan anggota dalam database berhasil dihapus bersih."
    ]);
} catch (PDOException $e) {
        echo json_encode([
        "status" => "error",
        "message" => "Gagal mereset database: " . $e->getMessage()
    ]);
}
?>
