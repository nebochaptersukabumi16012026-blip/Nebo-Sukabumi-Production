<?php
// workspace/refactored_api/modules/pengeluaran.php
// Modul Terpadu Pengeluaran Kas (Kas Utama, Anniversary, Kas Keliling)

require_once __DIR__ . '/../core/config.php';
require_once __DIR__ . '/../core/sync_helper.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        try {
            $id = intval($input['id'] ?? 0);
            $jenis_kas = trim($input['jenis_kas'] ?? '');

            if ($id > 0) {
                $stmt = $pdo->prepare("SELECT * FROM pengeluaran WHERE id = ?");
                $stmt->execute([$id]);
                $res = $stmt->fetch(PDO::FETCH_ASSOC);
                sendResponse(true, "Data pengeluaran ditemukan", $res);
            } elseif (!empty($jenis_kas)) {
                $stmt = $pdo->prepare("SELECT * FROM pengeluaran WHERE jenis_kas = ? ORDER BY id DESC");
                $stmt->execute([$jenis_kas]);
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);
                sendResponse(true, "Data pengeluaran berhasil dimuat", $data);
            } else {
                $stmt = $pdo->query("SELECT * FROM pengeluaran ORDER BY id DESC");
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);
                sendResponse(true, "Data pengeluaran berhasil dimuat", $data);
            }
        } catch (Exception $e) {
            sendResponse(false, "Gagal memuat pengeluaran: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'POST':
        $action = $input['action'] ?? '';
        if ($action === 'delete' || isset($input['hapus'])) {
            handleDeletePengeluaran($pdo, $input);
            break;
        }

        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $keterangan = trim($input['keterangan'] ?? '');
        $nominal    = floatval($input['nominal'] ?? 0);
        $tanggal    = $input['tanggal'] ?? date('Y-m-d H:i:s');
        $jenis_kas  = trim($input['jenis_kas'] ?? 'kas_utama');
        $created_by = trim($input['created_by'] ?? 'Sistem');

        if (empty($keterangan) || $nominal <= 0) {
            sendResponse(false, "Keterangan dan nominal (harus > 0) wajib diisi", null, [], 400);
        }

        try {
            $pdo->beginTransaction();

            $stmt = $pdo->prepare("
                INSERT INTO pengeluaran (keterangan, nominal, tanggal, jenis_kas, created_by) 
                VALUES (?, ?, ?, ?, ?)
            ");
            $stmt->execute([$keterangan, $nominal, $tanggal, $jenis_kas, $created_by]);
            $insertedId = $pdo->lastInsertId();

            // Pembaruan saldo akumulasi (jika tabel digunakan)
            try {
                $jenis_mapped = 'kas_utama';
                $jk_lower = strtolower($jenis_kas);
                if (strpos($jk_lower, 'aniv') !== false) {
                    $jenis_mapped = 'kas_aniv';
                } elseif (strpos($jk_lower, 'keliling') !== false) {
                    $jenis_mapped = 'kas_keliling';
                }
                $stmt_master = $pdo->prepare("
                    INSERT INTO saldo_akumulasi (jenis_kas, total_akumulasi_keluar) 
                    VALUES (?, ?) 
                    ON DUPLICATE KEY UPDATE total_akumulasi_keluar = total_akumulasi_keluar + ?
                ");
                $stmt_master->execute([$jenis_mapped, $nominal, $nominal]);
            } catch (Exception $e_master) {}

            $pdo->commit();

            sendResponse(true, "Pengeluaran berhasil dicatat", [
                "id" => (int)$insertedId,
                "nominal" => $nominal,
                "jenis_kas" => $jenis_kas
            ]);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            sendResponse(false, "Gagal mencatat pengeluaran: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'PUT':
        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $id         = intval($input['id'] ?? 0);
        $keterangan = trim($input['keterangan'] ?? '');
        $nominal    = floatval($input['nominal'] ?? 0);
        $tanggal    = $input['tanggal'] ?? date('Y-m-d H:i:s');
        $jenis_kas  = trim($input['jenis_kas'] ?? 'kas_utama');
        $created_by = trim($input['created_by'] ?? 'Sistem');

        if ($id <= 0 || empty($keterangan) || $nominal <= 0) {
            sendResponse(false, "ID, keterangan, dan nominal valid wajib diisi", null, [], 400);
        }

        try {
            $pdo->beginTransaction();
            $stmt = $pdo->prepare("
                UPDATE pengeluaran 
                SET keterangan = ?, nominal = ?, tanggal = ?, jenis_kas = ?, created_by = ? 
                WHERE id = ?
            ");
            $stmt->execute([$keterangan, $nominal, $tanggal, $jenis_kas, $created_by, $id]);
            syncMasterLedger($pdo);
            $pdo->commit();

            sendResponse(true, "Pengeluaran berhasil diupdate", [
                "id" => $id
            ]);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            sendResponse(false, "Gagal update pengeluaran: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeletePengeluaran($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

function handleDeletePengeluaran($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id = intval($input['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, "ID pengeluaran tidak valid", null, [], 400);
    }

    try {
        $pdo->beginTransaction();
        $stmt = $pdo->prepare("DELETE FROM pengeluaran WHERE id = ?");
        $stmt->execute([$id]);
        syncMasterLedger($pdo);
        $pdo->commit();

        sendResponse(true, "Pengeluaran berhasil dihapus", [
            "id" => $id,
            "rows_affected" => $stmt->rowCount()
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendResponse(false, "Gagal menghapus pengeluaran: " . $e->getMessage(), null, [], 500);
    }
}
