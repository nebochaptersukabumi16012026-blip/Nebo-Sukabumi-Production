<?php
// workspace/refactored_api/modules/pembayaran.php
// Modul Terpadu Pembayaran Universal (Daftar, Tambah, Edit, Hapus dengan Sinkronisasi Otomatis)

require_once __DIR__ . '/../core/config.php';
require_once __DIR__ . '/../core/sync_helper.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        try {
            $anggota_id = intval($input['anggotaId'] ?? ($input['anggota_id'] ?? 0));
            $jenis      = trim($input['jenisPembayaran'] ?? ($input['jenis'] ?? ''));

            $whereParts = [];
            $params     = [];

            if ($anggota_id > 0) {
                $whereParts[] = "anggotaId = ?";
                $params[] = $anggota_id;
            }
            if (!empty($jenis)) {
                $whereParts[] = "jenisPembayaran = ?";
                $params[] = strtoupper($jenis);
            }

            $whereSql = !empty($whereParts) ? "WHERE " . implode(" AND ", $whereParts) : "";
            $stmt = $pdo->prepare("SELECT * FROM pembayaran $whereSql ORDER BY tanggal DESC, id DESC");
            $stmt->execute($params);
            $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

            $formatted = array_map(function($r) {
                return [
                    'id'              => intval($r['id']),
                    'firestoreId'     => $r['firestoreId'] ?? '',
                    'anggotaId'       => intval($r['anggotaId']),
                    'anggotaNama'     => $r['anggotaNama'] ?? '',
                    'jenisPembayaran' => strtoupper($r['jenisPembayaran'] ?? 'KAS'),
                    'nominal'         => floatval($r['nominal']),
                    'tanggal'         => is_numeric($r['tanggal']) ? intval($r['tanggal']) : strtotime($r['tanggal']) * 1000,
                    'keterangan'      => $r['keterangan'] ?? '',
                    'buktiPembayaran' => $r['buktiPembayaran'] ?? null
                ];
            }, $rows);

            sendResponse(true, "Data pembayaran berhasil dimuat", $formatted);
        } catch (Exception $e) {
            sendResponse(false, "Gagal memuat pembayaran: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'POST':
        $action = $input['action'] ?? ($_GET['action'] ?? ($_POST['action'] ?? ''));
        if ($action === 'edit' || isset($input['edit'])) {
            handleEditPembayaran($pdo, $input);
            break;
        }
        if ($action === 'delete' || isset($input['hapus']) || isset($_GET['delete'])) {
            handleDeletePembayaran($pdo, $input);
            break;
        }

        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $anggotaId = intval($input['anggotaId'] ?? ($input['anggota_id'] ?? 0));
        $nominal   = floatval($input['nominal'] ?? 0);
        $jenis     = strtoupper(trim($input['jenisPembayaran'] ?? ($input['jenis'] ?? 'KAS')));
        $keterangan= trim($input['keterangan'] ?? '');
        $tanggal   = $input['tanggal'] ?? (time() * 1000);
        $bukti     = $input['buktiPembayaran'] ?? ($input['bukti_pembayaran'] ?? null);
        $idempotencyKey = trim($input['idempotency_key'] ?? ($input['request_id'] ?? ($input['firestoreId'] ?? '')));

        if ($anggotaId <= 0 || $nominal <= 0) {
            sendResponse(false, "ID Anggota dan nominal (harus > 0) wajib diisi", null, [], 400);
        }

        try {
            $stmt_m = $pdo->prepare("SELECT id, nama FROM anggota WHERE id = ?");
            $stmt_m->execute([$anggotaId]);
            $member = $stmt_m->fetch(PDO::FETCH_ASSOC);
            if (!$member) {
                sendResponse(false, "Anggota tidak ditemukan", null, [], 404);
            }

            // 1. Cek Idempotency Key jika disertakan
            if (!empty($idempotencyKey)) {
                $stmtIdem = $pdo->prepare("SELECT id, nominal FROM pembayaran WHERE firestoreId = ? LIMIT 1");
                $stmtIdem->execute([$idempotencyKey]);
                if ($exIdem = $stmtIdem->fetch(PDO::FETCH_ASSOC)) {
                    sendResponse(true, "Pembayaran sudah tercatat sebelumnya (idempotency key cocok)", [
                        "id"              => (int)$exIdem['id'],
                        "anggotaId"       => $anggotaId,
                        "jenisPembayaran" => $jenis,
                        "nominal"         => floatval($exIdem['nominal']),
                        "is_duplicate"    => true
                    ]);
                }
            }

            // 2. Anti-Double Submit / Idempotency Window (10 detik)
            $checkDup = $pdo->prepare("
                SELECT id, nominal, tanggal FROM pembayaran 
                WHERE anggotaId = ? AND nominal = ? AND jenisPembayaran = ? AND keterangan = ?
                ORDER BY id DESC LIMIT 1
            ");
            $checkDup->execute([$anggotaId, $nominal, $jenis, $keterangan]);
            if ($existing = $checkDup->fetch(PDO::FETCH_ASSOC)) {
                $lastT = intval($existing['tanggal'] ?? 0);
                $diffSec = abs((time() * 1000) - $lastT) / 1000;
                if ($diffSec < 10) {
                    sendResponse(true, "Pembayaran sudah tercatat sebelumnya (duplikasi dicegah)", [
                        "id"              => (int)$existing['id'],
                        "anggotaId"       => $anggotaId,
                        "jenisPembayaran" => $jenis,
                        "nominal"         => $nominal,
                        "is_duplicate"    => true
                    ]);
                }
            }

            $pdo->beginTransaction();

            $tglStr = date('Y-m-d H:i:s');
            $nowTime = time();
            $nowTs = is_numeric($tanggal) ? intval($tanggal) : ($nowTime * 1000);
            $linkKey = null;

            // Simpan ke tabel spesifik terlebih dahulu untuk mendapatkan ID domain
            if ($jenis === 'KAS') {
                $stmt_sp = $pdo->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, keterangan, tanggal, created_at) VALUES (?, ?, ?, ?, ?)");
                $stmt_sp->execute([$anggotaId, $nominal, $keterangan ?: 'Uang Kas', $tglStr, $nowTime]);
                $rkId = $pdo->lastInsertId();
                $linkKey = 'rk_' . $rkId;
                recalculateAnggotaKas($pdo, $anggotaId);
                syncMasterLedger($pdo, 'kas_utama');
            } elseif ($jenis === 'ANIV') {
                $stmt_sp = $pdo->prepare("INSERT INTO iuran_anniversary (anggota_id, nominal, keterangan, tanggal) VALUES (?, ?, ?, ?)");
                $stmt_sp->execute([$anggotaId, $nominal, $keterangan ?: 'Iuran Anniversary', $tglStr]);
                $iaId = $pdo->lastInsertId();
                $linkKey = 'ia_' . $iaId;
                try {
                    $refTag = ($keterangan ?: 'Iuran Anniversary') . " [REF:ia_{$iaId}]";
                    $stmt_ra = $pdo->prepare("INSERT INTO riwayat_aniv (id_anggota, nominal, keterangan, tanggal, created_at) VALUES (?, ?, ?, ?, ?)");
                    $stmt_ra->execute([$anggotaId, $nominal, $refTag, $tglStr, $nowTime]);
                } catch (Exception $e) {}
                recalculateAnggotaAniv($pdo, $anggotaId);
                syncMasterLedger($pdo, 'kas_aniv');
            } elseif ($jenis === 'CICILAN') {
                $stmt_sp = $pdo->prepare("INSERT INTO cicilan (anggota_id, nominal, keterangan, tanggal) VALUES (?, ?, ?, ?)");
                $stmt_sp->execute([$anggotaId, $nominal, $keterangan ?: 'Cicilan', $tglStr]);
                $cicId = $pdo->lastInsertId();
                $linkKey = 'cic_' . $cicId;
                recalculateAnggotaCicilan($pdo, $anggotaId);
            }

            // Simpan ke tabel universal pembayaran dengan firestoreId sebagai penaut unik
            $stmt_pem = $pdo->prepare("
                INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ");
            $stmt_pem->execute([$linkKey, $anggotaId, $member['nama'], $jenis, $nominal, $nowTs, $keterangan, $bukti]);
            $pemId = $pdo->lastInsertId();

            $pdo->commit();

            sendResponse(true, "Pembayaran berhasil dicatat", [
                "id"              => (int)$pemId,
                "anggotaId"       => $anggotaId,
                "jenisPembayaran" => $jenis,
                "nominal"         => $nominal
            ]);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            sendResponse(false, "Gagal mencatat pembayaran: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeletePembayaran($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

function handleEditPembayaran($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id      = intval($input['id'] ?? 0);
    $nominal = floatval($input['nominal'] ?? 0);
    $ket     = trim($input['keterangan'] ?? '');

    if ($id <= 0 || $nominal <= 0) {
        sendResponse(false, "ID dan nominal baru valid wajib disertakan", null, [], 400);
    }

    try {
        $stmt_old = $pdo->prepare("SELECT * FROM pembayaran WHERE id = ?");
        $stmt_old->execute([$id]);
        $old = $stmt_old->fetch(PDO::FETCH_ASSOC);
        if (!$old) {
            sendResponse(false, "Data pembayaran tidak ditemukan", null, [], 404);
        }

        $anggotaId = intval($old['anggotaId']);
        $jenis     = strtoupper($old['jenisPembayaran']);
        $oldNominal= floatval($old['nominal']);
        $fId       = $old['firestoreId'] ?? '';

        $pdo->beginTransaction();

        $upd = $pdo->prepare("UPDATE pembayaran SET nominal = ?, keterangan = ? WHERE id = ?");
        $upd->execute([$nominal, $ket, $id]);

        // Sinkronkan ke tabel turunan secara tertaut presisi
        if ($jenis === 'KAS') {
            if (str_starts_with($fId, 'rk_')) {
                $targetId = intval(substr($fId, 3));
                $stmt_sync = $pdo->prepare("UPDATE riwayat_kas SET nominal = ?, keterangan = ? WHERE id = ?");
                $stmt_sync->execute([$nominal, $ket, $targetId]);
            } else {
                $stmt_sync = $pdo->prepare("UPDATE riwayat_kas SET nominal = ?, keterangan = ? WHERE id_anggota = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $stmt_sync->execute([$nominal, $ket, $anggotaId, $oldNominal]);
            }
            recalculateAnggotaKas($pdo, $anggotaId);
            syncMasterLedger($pdo, 'kas_utama');
        } elseif ($jenis === 'ANIV') {
            if (str_starts_with($fId, 'ia_')) {
                $targetId = intval(substr($fId, 3));
                $stmt_sync = $pdo->prepare("UPDATE iuran_anniversary SET nominal = ?, keterangan = ? WHERE id = ?");
                $stmt_sync->execute([$nominal, $ket, $targetId]);
                $stmt_sync_ra = $pdo->prepare("UPDATE riwayat_aniv SET nominal = ? WHERE keterangan LIKE ?");
                $stmt_sync_ra->execute([$nominal, "%[REF:ia_{$targetId}]%"]);
            } else {
                $stmt_sync = $pdo->prepare("UPDATE iuran_anniversary SET nominal = ?, keterangan = ? WHERE anggota_id = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $stmt_sync->execute([$nominal, $ket, $anggotaId, $oldNominal]);
            }
            recalculateAnggotaAniv($pdo, $anggotaId);
            syncMasterLedger($pdo, 'kas_aniv');
        } elseif ($jenis === 'CICILAN') {
            if (str_starts_with($fId, 'cic_')) {
                $targetId = intval(substr($fId, 4));
                $stmt_sync = $pdo->prepare("UPDATE cicilan SET nominal = ?, keterangan = ? WHERE id = ?");
                $stmt_sync->execute([$nominal, $ket, $targetId]);
            } else {
                $stmt_sync = $pdo->prepare("UPDATE cicilan SET nominal = ?, keterangan = ? WHERE anggota_id = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $stmt_sync->execute([$nominal, $ket, $anggotaId, $oldNominal]);
            }
            recalculateAnggotaCicilan($pdo, $anggotaId);
        }

        $pdo->commit();

        sendResponse(true, "Pembayaran berhasil diperbarui", [
            "id" => $id,
            "nominal" => $nominal
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendResponse(false, "Gagal mengedit pembayaran: " . $e->getMessage(), null, [], 500);
    }
}

function handleDeletePembayaran($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id = intval($input['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, "ID pembayaran tidak valid", null, [], 400);
    }

    try {
        $stmt_old = $pdo->prepare("SELECT * FROM pembayaran WHERE id = ?");
        $stmt_old->execute([$id]);
        $old = $stmt_old->fetch(PDO::FETCH_ASSOC);

        if (!$old) {
            sendResponse(false, "Data pembayaran tidak ditemukan", null, [], 404);
        }

        $anggotaId = intval($old['anggotaId']);
        $jenis     = strtoupper($old['jenisPembayaran']);
        $nominal   = floatval($old['nominal']);
        $fId       = $old['firestoreId'] ?? '';

        $pdo->beginTransaction();

        $del = $pdo->prepare("DELETE FROM pembayaran WHERE id = ?");
        $del->execute([$id]);

        if ($jenis === 'KAS') {
            if (str_starts_with($fId, 'rk_')) {
                $targetId = intval(substr($fId, 3));
                $del_sp = $pdo->prepare("DELETE FROM riwayat_kas WHERE id = ?");
                $del_sp->execute([$targetId]);
            } else {
                $del_sp = $pdo->prepare("DELETE FROM riwayat_kas WHERE id_anggota = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_sp->execute([$anggotaId, $nominal]);
            }
            recalculateAnggotaKas($pdo, $anggotaId);
            syncMasterLedger($pdo, 'kas_utama');
        } elseif ($jenis === 'ANIV') {
            if (str_starts_with($fId, 'ia_')) {
                $targetId = intval(substr($fId, 3));
                $del_sp = $pdo->prepare("DELETE FROM iuran_anniversary WHERE id = ?");
                $del_sp->execute([$targetId]);
                $del_ra = $pdo->prepare("DELETE FROM riwayat_aniv WHERE keterangan LIKE ?");
                $del_ra->execute(["%[REF:ia_{$targetId}]%"]);
            } else {
                $del_sp = $pdo->prepare("DELETE FROM iuran_anniversary WHERE anggota_id = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_sp->execute([$anggotaId, $nominal]);
                $del_ra = $pdo->prepare("DELETE FROM riwayat_aniv WHERE id_anggota = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_ra->execute([$anggotaId, $nominal]);
            }
            recalculateAnggotaAniv($pdo, $anggotaId);
            syncMasterLedger($pdo, 'kas_aniv');
        } elseif ($jenis === 'CICILAN') {
            if (str_starts_with($fId, 'cic_')) {
                $targetId = intval(substr($fId, 4));
                $del_sp = $pdo->prepare("DELETE FROM cicilan WHERE id = ?");
                $del_sp->execute([$targetId]);
            } else {
                $del_sp = $pdo->prepare("DELETE FROM cicilan WHERE anggota_id = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_sp->execute([$anggotaId, $nominal]);
            }
            recalculateAnggotaCicilan($pdo, $anggotaId);
        }

        $pdo->commit();

        sendResponse(true, "Pembayaran berhasil dihapus dan saldo anggota disinkronkan", [
            "id" => $id,
            "anggotaId" => $anggotaId,
            "jenisPembayaran" => $jenis
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendResponse(false, "Gagal menghapus pembayaran: " . $e->getMessage(), null, [], 500);
    }
}
