<?php
// workspace/refactored_api/modules/absensi.php
// Modul Terpadu Absensi & Kehadiran Anggota

require_once __DIR__ . '/../core/config.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        try {
            $id = intval($input['id'] ?? 0);
            if ($id > 0) {
                $stmt = $pdo->prepare("SELECT * FROM absensi WHERE id = ?");
                $stmt->execute([$id]);
                $res = $stmt->fetch(PDO::FETCH_ASSOC);
                sendResponse(true, "Data absensi ditemukan", $res);
            } else {
                $stmt = $pdo->query("
                    SELECT ab.id, ab.anggota_id, a.nama as anggota_nama, a.nra, ab.tanggal, ab.status, ab.keterangan 
                    FROM absensi ab
                    LEFT JOIN anggota a ON ab.anggota_id = a.id
                    ORDER BY ab.id DESC
                ");
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);
                sendResponse(true, "Daftar absensi berhasil dimuat", $data);
            }
        } catch (Exception $e) {
            sendResponse(false, "Gagal memuat absensi: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'POST':
        $action = $input['action'] ?? ($_GET['action'] ?? ($_POST['action'] ?? ''));
        if ($action === 'delete' || isset($input['hapus']) || isset($_GET['delete'])) {
            handleDeleteAbsensi($pdo, $input);
            break;
        }

        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $anggota_id = intval($input['anggota_id'] ?? 0);
        $tanggal    = $input['tanggal'] ?? date('Y-m-d');
        $status     = trim($input['status'] ?? 'Hadir');
        $keterangan = trim($input['keterangan'] ?? '');

        if ($anggota_id <= 0) {
            sendResponse(false, "ID anggota wajib disertakan", null, [], 400);
        }

        try {
            $stmt = $pdo->prepare("
                INSERT INTO absensi (anggota_id, tanggal, status, keterangan) 
                VALUES (?, ?, ?, ?)
            ");
            $stmt->execute([$anggota_id, $tanggal, $status, $keterangan]);
            sendResponse(true, "Absensi berhasil dicatat", ["id" => (int)$pdo->lastInsertId()]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal mencatat absensi: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'PUT':
        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $id         = intval($input['id'] ?? 0);
        $status     = trim($input['status'] ?? 'Hadir');
        $keterangan = trim($input['keterangan'] ?? '');

        if ($id <= 0) sendResponse(false, "ID absensi tidak valid", null, [], 400);

        try {
            $stmt = $pdo->prepare("UPDATE absensi SET status = ?, keterangan = ? WHERE id = ?");
            $stmt->execute([$status, $keterangan, $id]);
            sendResponse(true, "Absensi berhasil diperbarui", ["id" => $id]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal update absensi: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeleteAbsensi($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

function handleDeleteAbsensi($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id = intval($input['id'] ?? 0);
    if ($id <= 0) sendResponse(false, "ID absensi tidak valid", null, [], 400);

    try {
        $stmt = $pdo->prepare("DELETE FROM absensi WHERE id = ?");
        $stmt->execute([$id]);
        sendResponse(true, "Absensi berhasil dihapus", ["id" => $id]);
    } catch (Exception $e) {
        sendResponse(false, "Gagal menghapus absensi: " . $e->getMessage(), null, [], 500);
    }
}
