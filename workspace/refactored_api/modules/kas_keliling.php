<?php
// workspace/refactored_api/modules/kas_keliling.php
// Modul Terpadu Kas Keliling (Bulanan, Pemasukan, Pengeluaran, Rekapitulasi)

require_once __DIR__ . '/../core/config.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

/**
 * Rekapitulasi Kas Keliling secara akurat tanpa double-counting
 */
function getKasKelilingSummary($pdo) {
    try {
        $stmt = $pdo->query("
            SELECT 
                COALESCE(SUM(CASE 
                    WHEN total_pemasukan > 0 THEN total_pemasukan
                    WHEN (total_pemasukan = 0 OR total_pemasukan IS NULL) AND (LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pemasukan' OR (COALESCE(jenis, '') = '' AND COALESCE(jenis_transaksi, '') = '')) THEN nominal
                    ELSE 0 
                END), 0) AS total_in,
                COALESCE(SUM(CASE 
                    WHEN total_pengeluaran > 0 THEN total_pengeluaran
                    WHEN (total_pengeluaran = 0 OR total_pengeluaran IS NULL) AND LOWER(COALESCE(jenis_transaksi, jenis, '')) = 'pengeluaran' THEN nominal
                    ELSE 0 
                END), 0) AS total_out
            FROM kas_keliling
        ");
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        $total_in  = floatval($row['total_in'] ?? 0);
        $total_out = floatval($row['total_out'] ?? 0);
        $saldo     = max(0.0, $total_in - $total_out);

        return [
            'total_pemasukan'    => $total_in,
            'total_pengeluaran'  => $total_out,
            'saldo'              => $saldo,
            'saldo_kas_keliling' => $saldo,
            'saldo_akhir'        => $saldo
        ];
    } catch (Exception $e) {
        return [
            'total_pemasukan'    => 0.0,
            'total_pengeluaran'  => 0.0,
            'saldo'              => 0.0,
            'saldo_kas_keliling' => 0.0,
            'saldo_akhir'        => 0.0
        ];
    }
}

switch ($method) {
    case 'GET':
        $id = intval($input['id'] ?? 0);
        if ($id > 0) {
            $stmt = $pdo->prepare("SELECT * FROM kas_keliling WHERE id = ?");
            $stmt->execute([$id]);
            $res = $stmt->fetch(PDO::FETCH_ASSOC);
            sendResponse(true, "Data kas keliling ditemukan", $res);
        } else {
            $stmt = $pdo->query("SELECT * FROM kas_keliling ORDER BY tahun DESC, bulan DESC, id DESC");
            $transaksi = $stmt->fetchAll(PDO::FETCH_ASSOC);
            $rekap = getKasKelilingSummary($pdo);

            sendResponse(true, "Data kas keliling berhasil dimuat", $transaksi, $rekap);
        }
        break;

    case 'POST':
        $action = $input['action'] ?? '';
        if ($action === 'delete' || isset($input['hapus'])) {
            handleDeleteKasKeliling($pdo, $input);
            break;
        }

        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $bulan   = trim($input['bulan'] ?? '');
        $tahun   = trim($input['tahun'] ?? date('Y'));
        $nominal = floatval($input['nominal'] ?? 0);
        $jenis   = strtolower(trim($input['jenis'] ?? ($input['jenis_transaksi'] ?? 'pemasukan')));
        $catatan = trim($input['catatan'] ?? ($input['keterangan'] ?? ''));
        $tanggal = $input['tanggal'] ?? date('Y-m-d');
        $nra     = trim($input['nra'] ?? '');

        if (empty($bulan) || empty($tahun) || $nominal <= 0) {
            sendResponse(false, "Bulan, tahun, dan nominal (harus > 0) wajib diisi", null, [], 400);
        }

        try {
            $stmt = $pdo->prepare("
                INSERT INTO kas_keliling (bulan, tahun, nominal, jenis, jenis_transaksi, catatan, tanggal, nra, created_at) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ");
            $stmt->execute([
                $bulan, 
                $tahun, 
                $nominal, 
                $jenis, 
                ucfirst($jenis), 
                $catatan, 
                $tanggal, 
                $nra, 
                time()
            ]);
            $newId = $pdo->lastInsertId();
            $rekap = getKasKelilingSummary($pdo);

            sendResponse(true, "Data kas keliling berhasil ditambahkan", [
                "id" => (int)$newId
            ], $rekap);
        } catch (Exception $e) {
            sendResponse(false, "Gagal menambahkan kas keliling: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'PUT':
        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $id      = intval($input['id'] ?? 0);
        $bulan   = trim($input['bulan'] ?? '');
        $tahun   = trim($input['tahun'] ?? date('Y'));
        $nominal = floatval($input['nominal'] ?? 0);
        $jenis   = strtolower(trim($input['jenis'] ?? ($input['jenis_transaksi'] ?? 'pemasukan')));
        $catatan = trim($input['catatan'] ?? '');
        $tanggal = $input['tanggal'] ?? date('Y-m-d');

        if ($id <= 0 || empty($bulan) || empty($tahun) || $nominal <= 0) {
            sendResponse(false, "ID, bulan, tahun, dan nominal valid wajib diisi", null, [], 400);
        }

        try {
            $stmt = $pdo->prepare("
                UPDATE kas_keliling 
                SET bulan = ?, tahun = ?, nominal = ?, jenis = ?, jenis_transaksi = ?, catatan = ?, tanggal = ?, updated_at = ?
                WHERE id = ?
            ");
            $stmt->execute([$bulan, $tahun, $nominal, $jenis, ucfirst($jenis), $catatan, $tanggal, time(), $id]);
            $rekap = getKasKelilingSummary($pdo);

            sendResponse(true, "Data kas keliling berhasil diperbarui", [
                "id" => $id
            ], $rekap);
        } catch (Exception $e) {
            sendResponse(false, "Gagal memperbarui kas keliling: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeleteKasKeliling($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

function handleDeleteKasKeliling($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id = intval($input['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, "ID kas keliling tidak valid", null, [], 400);
    }

    try {
        $stmt = $pdo->prepare("DELETE FROM kas_keliling WHERE id = ?");
        $stmt->execute([$id]);
        $rekap = getKasKelilingSummary($pdo);

        sendResponse(true, "Data kas keliling berhasil dihapus", [
            "id" => $id,
            "rows_affected" => $stmt->rowCount()
        ], $rekap);
    } catch (Exception $e) {
        sendResponse(false, "Gagal menghapus kas keliling: " . $e->getMessage(), null, [], 500);
    }
}
