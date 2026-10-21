<?php
// workspace/refactored_api/modules/catatan.php
// Modul Terpadu Catatan Komunitas

require_once __DIR__ . '/../core/config.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        $id = intval($input['id'] ?? 0);
        if ($id > 0) {
            $stmt = $pdo->prepare("SELECT * FROM catatan WHERE id = ?");
            $stmt->execute([$id]);
            $res = $stmt->fetch(PDO::FETCH_ASSOC);
            sendResponse(true, "Catatan ditemukan", $res);
        } else {
            $stmt = $pdo->query("SELECT * FROM catatan ORDER BY id DESC");
            $data = $stmt->fetchAll(PDO::FETCH_ASSOC);
            sendResponse(true, "Daftar catatan berhasil dimuat", $data);
        }
        break;

    case 'POST':
        $action = $input['action'] ?? '';
        if ($action === 'delete' || isset($input['hapus'])) {
            handleDeleteCatatan($pdo, $input);
            break;
        }

        $judul   = trim($input['judul'] ?? ($input['title'] ?? ''));
        $isi     = trim($input['isi'] ?? ($input['content'] ?? ''));
        $tanggal = $input['tanggal'] ?? date('Y-m-d H:i:s');

        if (empty($judul)) {
            sendResponse(false, "Judul catatan tidak boleh kosong", null, [], 400);
        }

        try {
            $stmt = $pdo->prepare("INSERT INTO catatan (judul, isi, tanggal) VALUES (?, ?, ?)");
            $stmt->execute([$judul, $isi, $tanggal]);
            sendResponse(true, "Catatan berhasil ditambahkan", ["id" => (int)$pdo->lastInsertId()]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal menambah catatan: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'PUT':
        $id    = intval($input['id'] ?? 0);
        $judul = trim($input['judul'] ?? ($input['title'] ?? ''));
        $isi   = trim($input['isi'] ?? ($input['content'] ?? ''));

        if ($id <= 0 || empty($judul)) {
            sendResponse(false, "ID dan judul valid wajib disertakan", null, [], 400);
        }

        try {
            $stmt = $pdo->prepare("UPDATE catatan SET judul = ?, isi = ? WHERE id = ?");
            $stmt->execute([$judul, $isi, $id]);
            sendResponse(true, "Catatan berhasil diperbarui", ["id" => $id]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal update catatan: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeleteCatatan($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

function handleDeleteCatatan($pdo, $input) {
    $id = intval($input['id'] ?? 0);
    if ($id <= 0) sendResponse(false, "ID catatan tidak valid", null, [], 400);

    try {
        $stmt = $pdo->prepare("DELETE FROM catatan WHERE id = ?");
        $stmt->execute([$id]);
        sendResponse(true, "Catatan berhasil dihapus", ["id" => $id]);
    } catch (Exception $e) {
        sendResponse(false, "Gagal menghapus catatan: " . $e->getMessage(), null, [], 500);
    }
}
