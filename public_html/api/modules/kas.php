<?php
// workspace/refactored_api/modules/kas.php
// Modul Terpadu Transaksi Kas Anggota (Tambah, Detail, Hapus)

require_once __DIR__ . '/../core/config.php';
require_once __DIR__ . '/../core/sync_helper.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        // Mengambil riwayat kas
        try {
            $id_anggota = intval($input['id_anggota'] ?? ($input['anggota_id'] ?? 0));
            if ($id_anggota > 0) {
                $stmt = $pdo->prepare("
                    SELECT rk.id, rk.id_anggota as anggota_id, a.nama as anggota_nama, a.nra, rk.nominal, rk.tanggal, rk.keterangan
                    FROM riwayat_kas rk
                    LEFT JOIN anggota a ON rk.id_anggota = a.id
                    WHERE rk.id_anggota = ?
                    ORDER BY rk.id DESC
                ");
                $stmt->execute([$id_anggota]);
            } else {
                $stmt = $pdo->prepare("
                    SELECT rk.id, rk.id_anggota as anggota_id, a.nama as anggota_nama, a.nra, rk.nominal, rk.tanggal, rk.keterangan
                    FROM riwayat_kas rk
                    LEFT JOIN anggota a ON rk.id_anggota = a.id
                    ORDER BY rk.id DESC
                ");
                $stmt->execute();
            }
            $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

            // Perhitungan ringkasan
            $totalKas = 0.0;
            foreach ($data as $item) {
                $totalKas += floatval($item['nominal']);
            }

            sendResponse(true, "Data kas berhasil dimuat", $data, [
                "total_kas" => $totalKas,
                "count" => count($data)
            ]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal memuat data kas: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'POST':
        // Cek jika aksi adalah delete via POST tunneling
        $action = $input['action'] ?? ($_GET['action'] ?? ($_POST['action'] ?? ''));
        if ($action === 'delete' || isset($input['hapus']) || isset($_GET['delete'])) {
            handleDeleteKas($pdo, $input);
            break;
        }

        // Tambah Uang Kas
        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $id_anggota = intval($input['id_anggota'] ?? ($input['anggota_id'] ?? ($input['anggotaId'] ?? 0)));
        $nominal    = floatval($input['nominal'] ?? 0);
        $keterangan = trim($input['keterangan'] ?? 'Uang Kas');
        $tanggal    = $input['tanggal'] ?? date('Y-m-d H:i:s');
        $bukti      = $input['bukti_pembayaran'] ?? ($input['buktiPembayaran'] ?? null);
        $idempotencyKey = trim($input['idempotency_key'] ?? ($input['request_id'] ?? ($input['firestoreId'] ?? '')));

        if ($id_anggota <= 0 || $nominal <= 0) {
            sendResponse(false, "ID Anggota dan nominal (harus > 0) wajib diisi", null, [], 400);
        }

        try {
            $stmt_m = $pdo->prepare("SELECT id, nama, nra, uang_kas FROM anggota WHERE id = ?");
            $stmt_m->execute([$id_anggota]);
            $anggota = $stmt_m->fetch(PDO::FETCH_ASSOC);
            if (!$anggota) {
                sendResponse(false, "Anggota dengan ID $id_anggota tidak ditemukan", null, [], 404);
            }

            // 1. Cek Idempotency Key jika ada
            if (!empty($idempotencyKey)) {
                $stmtIdem = $pdo->prepare("SELECT id, nominal FROM pembayaran WHERE firestoreId = ? LIMIT 1");
                $stmtIdem->execute([$idempotencyKey]);
                if ($exIdem = $stmtIdem->fetch(PDO::FETCH_ASSOC)) {
                    $curKas = recalculateAnggotaKas($pdo, $id_anggota);
                    sendResponse(true, "Transaksi kas sudah tercatat sebelumnya (idempotency key cocok)", [
                        "id" => (int)$exIdem['id'],
                        "anggota_id" => $id_anggota,
                        "nominal" => floatval($exIdem['nominal']),
                        "uang_kas_sekarang" => $curKas,
                        "is_duplicate" => true
                    ]);
                }
            }

            // 2. Anti-Double Submit / Idempotency Window (10 detik dengan nominal & keterangan sama)
            $checkDup = $pdo->prepare("
                SELECT id, nominal, created_at FROM riwayat_kas 
                WHERE id_anggota = ? AND nominal = ? AND keterangan = ? 
                ORDER BY id DESC LIMIT 1
            ");
            $checkDup->execute([$id_anggota, $nominal, $keterangan]);
            if ($existing = $checkDup->fetch(PDO::FETCH_ASSOC)) {
                $lastTime = intval($existing['created_at'] ?? 0);
                if ($lastTime > 0 && abs(time() - $lastTime) < 10) {
                    $curKas = recalculateAnggotaKas($pdo, $id_anggota);
                    sendResponse(true, "Transaksi kas sudah tercatat sebelumnya (duplikasi dicegah)", [
                        "id" => (int)$existing['id'],
                        "anggota_id" => $id_anggota,
                        "nominal" => $nominal,
                        "uang_kas_sekarang" => $curKas,
                        "is_duplicate" => true
                    ]);
                }
            }

            $pdo->beginTransaction();

            // 1. Simpan ke riwayat_kas (Single Source of Truth)
            $nowTime = time();
            $stmt_rk = $pdo->prepare("
                INSERT INTO riwayat_kas (id_anggota, nominal, keterangan, tanggal, created_at) 
                VALUES (?, ?, ?, ?, ?)
            ");
            $stmt_rk->execute([$id_anggota, $nominal, $keterangan, $tanggal, $nowTime]);
            $kasId = $pdo->lastInsertId();

            // 2. Mirroring ke pembayaran dengan firestoreId sebagai referensi unik tertaut
            try {
                $nowTs = is_numeric($tanggal) ? intval($tanggal) : ($nowTime * 1000);
                $linkRef = 'rk_' . $kasId;
                $stmt_pem = $pdo->prepare("
                    INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) 
                    VALUES (?, ?, ?, 'KAS', ?, ?, ?, ?)
                ");
                $stmt_pem->execute([
                    $linkRef,
                    $id_anggota,
                    $anggota['nama'],
                    $nominal,
                    $nowTs,
                    $keterangan,
                    $bukti
                ]);
            } catch (Exception $e) {
                // Abaikan jika kolom firestoreId bermasalah
                try {
                    $stmt_pem2 = $pdo->prepare("
                        INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) 
                        VALUES (?, ?, 'KAS', ?, ?, ?, ?)
                    ");
                    $stmt_pem2->execute([$id_anggota, $anggota['nama'], $nominal, $nowTs, $keterangan, $bukti]);
                } catch (Exception $e2) {}
            }

            // 3. Rekalkulasi saldo kas anggota & master ledger
            $newTotalKas = recalculateAnggotaKas($pdo, $id_anggota);
            syncMasterLedger($pdo, 'kas_utama');

            $pdo->commit();

            sendResponse(true, "Pembayaran kas berhasil dicatat", [
                "id" => (int)$kasId,
                "anggota_id" => $id_anggota,
                "nominal" => $nominal,
                "uang_kas_sekarang" => $newTotalKas
            ]);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            sendResponse(false, "Gagal mencatat kas: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeleteKas($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

/**
 * Logika Hapus Kas Terpadu & Terhubung
 */
function handleDeleteKas($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id = intval($input['id_kas'] ?? ($input['id_transaksi'] ?? ($input['id'] ?? 0)));
    if ($id <= 0) {
        sendResponse(false, "ID transaksi kas tidak valid", null, [], 400);
    }

    try {
        $pdo->beginTransaction();

        $targetAnggotaId = 0;
        $targetNominal   = 0;
        $isFromRiwayatKas = false;
        $linkedRkId = 0;

        // Periksa apakah ID merujuk ke tabel riwayat_kas
        $check_rk = $pdo->prepare("SELECT id, id_anggota, nominal FROM riwayat_kas WHERE id = ?");
        $check_rk->execute([$id]);
        if ($row = $check_rk->fetch(PDO::FETCH_ASSOC)) {
            $isFromRiwayatKas = true;
            $targetAnggotaId = intval($row['id_anggota']);
            $targetNominal   = floatval($row['nominal']);
            $linkedRkId      = intval($row['id']);
        }

        // Jika tidak ditemukan di riwayat_kas, periksa di tabel pembayaran
        if (!$isFromRiwayatKas) {
            $check_pem = $pdo->prepare("SELECT id, firestoreId, anggotaId, nominal FROM pembayaran WHERE id = ? AND UPPER(jenisPembayaran) = 'KAS'");
            $check_pem->execute([$id]);
            if ($row_p = $check_pem->fetch(PDO::FETCH_ASSOC)) {
                $targetAnggotaId = intval($row_p['anggotaId']);
                $targetNominal   = floatval($row_p['nominal']);
                $fId = $row_p['firestoreId'] ?? '';
                if (str_starts_with($fId, 'rk_')) {
                    $linkedRkId = intval(substr($fId, 3));
                }
            }
        }

        $rowsDeleted = 0;

        if ($isFromRiwayatKas) {
            // Hapus dari riwayat_kas
            $del_rk = $pdo->prepare("DELETE FROM riwayat_kas WHERE id = ?");
            $del_rk->execute([$id]);
            $rowsDeleted += $del_rk->rowCount();

            // Hapus cerminannya dari pembayaran via link firestoreId atau fallback kriteria aman
            $linkKey = 'rk_' . $id;
            $del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE firestoreId = ?");
            $del_pem->execute([$linkKey]);
            if ($del_pem->rowCount() === 0 && $targetAnggotaId > 0 && $targetNominal > 0) {
                $del_pem2 = $pdo->prepare("DELETE FROM pembayaran WHERE anggotaId = ? AND nominal = ? AND UPPER(jenisPembayaran) = 'KAS' ORDER BY id DESC LIMIT 1");
                $del_pem2->execute([$targetAnggotaId, $targetNominal]);
            }
        } else {
            // ID berasal dari pembayaran
            $del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE id = ?");
            $del_pem->execute([$id]);
            $rowsDeleted += $del_pem->rowCount();

            // Hapus cerminannya dari riwayat_kas
            if ($linkedRkId > 0) {
                $del_rk = $pdo->prepare("DELETE FROM riwayat_kas WHERE id = ?");
                $del_rk->execute([$linkedRkId]);
            } elseif ($targetAnggotaId > 0 && $targetNominal > 0) {
                $del_rk2 = $pdo->prepare("DELETE FROM riwayat_kas WHERE id_anggota = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_rk2->execute([$targetAnggotaId, $targetNominal]);
            }
        }

        // Rekalkulasi uang kas anggota & master ledger
        $newTotalKas = 0.0;
        if ($targetAnggotaId > 0) {
            $newTotalKas = recalculateAnggotaKas($pdo, $targetAnggotaId);
        }
        syncMasterLedger($pdo, 'kas_utama');

        $pdo->commit();

        sendResponse(true, "Data kas berhasil dihapus dan saldo diperbarui", [
            "id" => $id,
            "anggota_id" => $targetAnggotaId,
            "uang_kas_sekarang" => $newTotalKas,
            "rows_affected" => $rowsDeleted
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendResponse(false, "Gagal menghapus kas: " . $e->getMessage(), null, [], 500);
    }
}
