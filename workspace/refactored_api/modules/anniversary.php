<?php
// workspace/refactored_api/modules/anniversary.php
// Modul Terpadu Iuran & Riwayat Anniversary

require_once __DIR__ . '/../core/config.php';
require_once __DIR__ . '/../core/sync_helper.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        // Menampilkan daftar iuran anniversary
        try {
            $anggota_id = isset($input['anggota_id']) ? intval($input['anggota_id']) : (isset($input['id_anggota']) ? intval($input['id_anggota']) : 0);
            
            if ($anggota_id > 0) {
                // Ambil untuk anggota tertentu
                $stmt = $pdo->prepare("
                    SELECT ia.id, ia.anggota_id, a.nama as anggota_nama, a.nra, ia.nominal, ia.tanggal, ia.keterangan 
                    FROM iuran_anniversary ia
                    LEFT JOIN anggota a ON ia.anggota_id = a.id
                    WHERE ia.anggota_id = ?
                    ORDER BY ia.id DESC
                ");
                $stmt->execute([$anggota_id]);
            } else {
                // Ambil seluruh data
                $stmt = $pdo->prepare("
                    SELECT ia.id, ia.anggota_id, a.nama as anggota_nama, a.nra, ia.nominal, ia.tanggal, ia.keterangan 
                    FROM iuran_anniversary ia
                    LEFT JOIN anggota a ON ia.anggota_id = a.id
                    ORDER BY ia.id DESC
                ");
                $stmt->execute();
            }
            $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

            // Format data agar kompatibel dengan Android DTO (IuranAnivDto)
            $formatted = array_map(function($row) {
                return [
                    'id'           => intval($row['id']),
                    'anggota_id'   => intval($row['anggota_id']),
                    'anggota_nama' => $row['anggota_nama'] ?? '',
                    'nra'          => $row['nra'] ?? '',
                    'nominal'      => floatval($row['nominal']),
                    'tanggal'      => $row['tanggal'] ?? '',
                    'keterangan'   => $row['keterangan'] ?? ''
                ];
            }, $data);

            sendResponse(true, "Data iuran anniversary berhasil dimuat", $formatted);
        } catch (Exception $e) {
            sendResponse(false, "Gagal memuat iuran anniversary: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'POST':
        // Periksa apakah ini permintaan delete via POST (tunneling)
        $action = $input['action'] ?? ($_GET['action'] ?? ($_POST['action'] ?? ''));
        if ($action === 'delete' || isset($input['hapus']) || isset($_GET['delete'])) {
            handleDeleteAniv($pdo, $input);
            break;
        }

        // Tambah Iuran Anniversary Baru
        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $id_anggota = intval($input['id_anggota'] ?? ($input['anggota_id'] ?? ($input['anggotaId'] ?? 0)));
        $nominal    = floatval($input['nominal'] ?? 0);
        $keterangan = trim($input['keterangan'] ?? 'Iuran Anniversary');
        $tanggal    = $input['tanggal'] ?? date('Y-m-d H:i:s');
        $bukti      = $input['bukti_pembayaran'] ?? ($input['buktiPembayaran'] ?? null);
        $idempotencyKey = trim($input['idempotency_key'] ?? ($input['request_id'] ?? ($input['firestoreId'] ?? '')));

        if ($id_anggota <= 0 || $nominal <= 0) {
            sendResponse(false, "ID Anggota dan nominal (harus > 0) wajib diisi", null, [], 400);
        }

        try {
            // Verifikasi anggota ada
            $stmt_m = $pdo->prepare("SELECT id, nama, nra, iuran_aniv FROM anggota WHERE id = ?");
            $stmt_m->execute([$id_anggota]);
            $anggota = $stmt_m->fetch(PDO::FETCH_ASSOC);
            if (!$anggota) {
                sendResponse(false, "Anggota dengan ID $id_anggota tidak ditemukan", null, [], 404);
            }

            // 1. Cek Idempotency Key jika disertakan client
            if (!empty($idempotencyKey)) {
                $stmtIdem = $pdo->prepare("SELECT id, nominal FROM pembayaran WHERE firestoreId = ? LIMIT 1");
                $stmtIdem->execute([$idempotencyKey]);
                if ($exIdem = $stmtIdem->fetch(PDO::FETCH_ASSOC)) {
                    $curAniv = recalculateAnggotaAniv($pdo, $id_anggota);
                    sendResponse(true, "Iuran anniversary sudah tercatat sebelumnya (idempotency key cocok)", [
                        "id" => (int)$exIdem['id'],
                        "anggota_id" => $id_anggota,
                        "nominal" => floatval($exIdem['nominal']),
                        "total_aniv_sekarang" => $curAniv,
                        "is_duplicate" => true
                    ]);
                }
            }

            // 2. Anti-Double Submit / Idempotency Window (10 detik dengan nominal & keterangan sama)
            $checkDup = $pdo->prepare("
                SELECT id, nominal, tanggal FROM iuran_anniversary 
                WHERE anggota_id = ? AND nominal = ? AND keterangan = ?
                ORDER BY id DESC LIMIT 1
            ");
            $checkDup->execute([$id_anggota, $nominal, $keterangan]);
            if ($existing = $checkDup->fetch(PDO::FETCH_ASSOC)) {
                $exDate = strtotime($existing['tanggal'] ?? '');
                if ($exDate > 0 && abs(time() - $exDate) < 10) {
                    $curAniv = recalculateAnggotaAniv($pdo, $id_anggota);
                    sendResponse(true, "Iuran anniversary sudah tercatat sebelumnya (duplikasi dicegah)", [
                        "id" => (int)$existing['id'],
                        "anggota_id" => $id_anggota,
                        "nominal" => $nominal,
                        "total_aniv_sekarang" => $curAniv,
                        "is_duplicate" => true
                    ]);
                }
            }

            $pdo->beginTransaction();

            // 1. Simpan ke iuran_anniversary (Single Source of Truth)
            $stmt_ia = $pdo->prepare("
                INSERT INTO iuran_anniversary (anggota_id, nominal, tanggal, keterangan) 
                VALUES (?, ?, ?, ?)
            ");
            $stmt_ia->execute([$id_anggota, $nominal, $tanggal, $keterangan]);
            $anivId = $pdo->lastInsertId();

            // 2. Simpan ke riwayat_aniv tertaut referensi unik
            try {
                $refTag = $keterangan . " [REF:ia_{$anivId}]";
                $stmt_ra = $pdo->prepare("
                    INSERT INTO riwayat_aniv (id_anggota, nominal, keterangan, tanggal, created_at) 
                    VALUES (?, ?, ?, ?, ?)
                ");
                $stmt_ra->execute([$id_anggota, $nominal, $refTag, $tanggal, time()]);
            } catch (Exception $e) {}

            // 3. Mirroring ke pembayaran dengan firestoreId sebagai referensi unik tertaut
            try {
                $nowTs = is_numeric($tanggal) ? intval($tanggal) : (time() * 1000);
                $linkRef = 'ia_' . $anivId;
                $stmt_pem = $pdo->prepare("
                    INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) 
                    VALUES (?, ?, ?, 'ANIV', ?, ?, ?, ?)
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
                try {
                    $stmt_pem2 = $pdo->prepare("
                        INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) 
                        VALUES (?, ?, 'ANIV', ?, ?, ?, ?)
                    ");
                    $stmt_pem2->execute([$id_anggota, $anggota['nama'], $nominal, $nowTs, $keterangan, $bukti]);
                } catch (Exception $e2) {}
            }

            // 4. Rekalkulasi otomatis saldo anniversary anggota & master ledger
            $newTotalAniv = recalculateAnggotaAniv($pdo, $id_anggota);
            syncMasterLedger($pdo, 'kas_aniv');

            $pdo->commit();

            sendResponse(true, "Pembayaran anniversary berhasil dicatat", [
                "id" => (int)$anivId,
                "anggota_id" => $id_anggota,
                "nominal" => $nominal,
                "total_aniv_sekarang" => $newTotalAniv
            ]);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            sendResponse(false, "Gagal mencatat anniversary: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeleteAniv($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

/**
 * Logika Penghapusan Anniversary Terpadu & Terhubung Presisi
 */
function handleDeleteAniv($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id = intval($input['id_aniv'] ?? ($input['id_transaksi'] ?? ($input['id'] ?? 0)));
    if ($id <= 0) {
        sendResponse(false, "ID transaksi anniversary tidak valid", null, [], 400);
    }

    try {
        $pdo->beginTransaction();

        $targetAnggotaId = 0;
        $targetNominal   = 0;
        $targetTanggal   = '';
        $originTable     = 'unknown';
        $linkedIaId      = 0;

        // 1. Cek di iuran_anniversary (tabel utama)
        $check_ia = $pdo->prepare("SELECT id, anggota_id, nominal, tanggal FROM iuran_anniversary WHERE id = ?");
        $check_ia->execute([$id]);
        if ($row = $check_ia->fetch(PDO::FETCH_ASSOC)) {
            $originTable     = 'iuran_anniversary';
            $targetAnggotaId = intval($row['anggota_id']);
            $targetNominal   = floatval($row['nominal']);
            $targetTanggal   = $row['tanggal'] ?? '';
            $linkedIaId      = intval($row['id']);
        }

        // 2. Jika tidak ada, cek di pembayaran
        if ($originTable === 'unknown') {
            $check_pem = $pdo->prepare("SELECT id, firestoreId, anggotaId, nominal FROM pembayaran WHERE id = ? AND UPPER(jenisPembayaran) = 'ANIV'");
            $check_pem->execute([$id]);
            if ($row_p = $check_pem->fetch(PDO::FETCH_ASSOC)) {
                $originTable     = 'pembayaran';
                $targetAnggotaId = intval($row_p['anggotaId']);
                $targetNominal   = floatval($row_p['nominal']);
                $fId = $row_p['firestoreId'] ?? '';
                if (str_starts_with($fId, 'ia_')) {
                    $linkedIaId = intval(substr($fId, 3));
                }
            }
        }

        // 3. Jika masih tidak ada, cek di riwayat_aniv
        if ($originTable === 'unknown') {
            $check_ra = $pdo->prepare("SELECT id, id_anggota, nominal, keterangan FROM riwayat_aniv WHERE id = ?");
            $check_ra->execute([$id]);
            if ($row_ra = $check_ra->fetch(PDO::FETCH_ASSOC)) {
                $originTable     = 'riwayat_aniv';
                $targetAnggotaId = intval($row_ra['id_anggota']);
                $targetNominal   = floatval($row_ra['nominal']);
                if (preg_match('/\[REF:ia_(\d+)\]/', $row_ra['keterangan'] ?? '', $m)) {
                    $linkedIaId = intval($m[1]);
                }
            }
        }

        $rowsDeleted = 0;

        if ($originTable === 'iuran_anniversary') {
            $del_ia = $pdo->prepare("DELETE FROM iuran_anniversary WHERE id = ?");
            $del_ia->execute([$id]);
            $rowsDeleted += $del_ia->rowCount();

            // Hapus di pembayaran yang tertaut
            $linkKey = 'ia_' . $id;
            $del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE firestoreId = ?");
            $del_pem->execute([$linkKey]);
            if ($del_pem->rowCount() === 0 && $targetAnggotaId > 0 && $targetNominal > 0) {
                $del_pem2 = $pdo->prepare("DELETE FROM pembayaran WHERE anggotaId = ? AND nominal = ? AND UPPER(jenisPembayaran) = 'ANIV' ORDER BY id DESC LIMIT 1");
                $del_pem2->execute([$targetAnggotaId, $targetNominal]);
            }

            // Hapus di riwayat_aniv
            $del_ra = $pdo->prepare("DELETE FROM riwayat_aniv WHERE keterangan LIKE ?");
            $del_ra->execute(["%[REF:ia_{$id}]%"]);
            if ($del_ra->rowCount() === 0 && $targetAnggotaId > 0 && $targetNominal > 0) {
                $del_ra2 = $pdo->prepare("DELETE FROM riwayat_aniv WHERE id_anggota = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_ra2->execute([$targetAnggotaId, $targetNominal]);
            }
        } elseif ($originTable === 'pembayaran') {
            $del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE id = ?");
            $del_pem->execute([$id]);
            $rowsDeleted += $del_pem->rowCount();

            if ($linkedIaId > 0) {
                $del_ia = $pdo->prepare("DELETE FROM iuran_anniversary WHERE id = ?");
                $del_ia->execute([$linkedIaId]);
                $del_ra = $pdo->prepare("DELETE FROM riwayat_aniv WHERE keterangan LIKE ?");
                $del_ra->execute(["%[REF:ia_{$linkedIaId}]%"]);
            } elseif ($targetAnggotaId > 0 && $targetNominal > 0) {
                $del_ia2 = $pdo->prepare("DELETE FROM iuran_anniversary WHERE anggota_id = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_ia2->execute([$targetAnggotaId, $targetNominal]);
                $del_ra2 = $pdo->prepare("DELETE FROM riwayat_aniv WHERE id_anggota = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_ra2->execute([$targetAnggotaId, $targetNominal]);
            }
        } elseif ($originTable === 'riwayat_aniv') {
            $del_ra = $pdo->prepare("DELETE FROM riwayat_aniv WHERE id = ?");
            $del_ra->execute([$id]);
            $rowsDeleted += $del_ra->rowCount();

            if ($linkedIaId > 0) {
                $del_ia = $pdo->prepare("DELETE FROM iuran_anniversary WHERE id = ?");
                $del_ia->execute([$linkedIaId]);
                $linkKey = 'ia_' . $linkedIaId;
                $del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE firestoreId = ?");
                $del_pem->execute([$linkKey]);
            } elseif ($targetAnggotaId > 0 && $targetNominal > 0) {
                $del_ia2 = $pdo->prepare("DELETE FROM iuran_anniversary WHERE anggota_id = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_ia2->execute([$targetAnggotaId, $targetNominal]);
                $del_pem2 = $pdo->prepare("DELETE FROM pembayaran WHERE anggotaId = ? AND nominal = ? AND UPPER(jenisPembayaran) = 'ANIV' ORDER BY id DESC LIMIT 1");
                $del_pem2->execute([$targetAnggotaId, $targetNominal]);
            }
        }

        // Rekalkulasi mutlak total saldo anniversary anggota & master ledger
        $newTotal = 0.0;
        if ($targetAnggotaId > 0) {
            $newTotal = recalculateAnggotaAniv($pdo, $targetAnggotaId);
        }
        syncMasterLedger($pdo, 'kas_aniv');

        $pdo->commit();

        sendResponse(true, "Data anniversary berhasil dihapus dan saldo anggota diperbarui", [
            "id" => $id,
            "anggota_id" => $targetAnggotaId,
            "total_aniv_sekarang" => $newTotal,
            "rows_affected" => $rowsDeleted
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendResponse(false, "Gagal menghapus anniversary: " . $e->getMessage(), null, [], 500);
    }
}
