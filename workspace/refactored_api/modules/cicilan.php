<?php
// workspace/refactored_api/modules/cicilan.php
// Modul Terpadu Pembayaran & Tagihan Cicilan Anggota

require_once __DIR__ . '/../core/config.php';
require_once __DIR__ . '/../core/sync_helper.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        $action = $input['action'] ?? '';
        if ($action === 'aktif' || isset($input['daftar_aktif'])) {
            // Mode: Daftar Cicilan Aktif
            handleDaftarCicilanAktif($pdo, $input);
            break;
        }

        // Mode standar: Daftar riwayat pembayaran cicilan
        try {
            $anggota_id = intval($input['anggota_id'] ?? ($input['id_anggota'] ?? 0));
            if ($anggota_id > 0) {
                $stmt = $pdo->prepare("
                    SELECT c.id, c.anggota_id, a.nama as anggota_nama, a.nra, c.nominal, c.tanggal, c.keterangan 
                    FROM cicilan c
                    LEFT JOIN anggota a ON c.anggota_id = a.id
                    WHERE c.anggota_id = ?
                    ORDER BY c.id DESC
                ");
                $stmt->execute([$anggota_id]);
            } else {
                $stmt = $pdo->prepare("
                    SELECT c.id, c.anggota_id, a.nama as anggota_nama, a.nra, c.nominal, c.tanggal, c.keterangan 
                    FROM cicilan c
                    LEFT JOIN anggota a ON c.anggota_id = a.id
                    ORDER BY c.id DESC
                ");
                $stmt->execute();
            }
            $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

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

            sendResponse(true, "Data cicilan berhasil dimuat", $formatted);
        } catch (Exception $e) {
            sendResponse(false, "Gagal memuat cicilan: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'POST':
        $action = $input['action'] ?? '';
        if ($action === 'delete' || isset($input['hapus'])) {
            handleDeleteCicilan($pdo, $input);
            break;
        }

        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $anggota_id = intval($input['anggota_id'] ?? ($input['id_anggota'] ?? ($input['anggotaId'] ?? 0)));
        $nominal    = floatval($input['nominal'] ?? 0);
        $keterangan = trim($input['keterangan'] ?? 'Pembayaran Cicilan');
        $tanggal    = $input['tanggal'] ?? date('Y-m-d H:i:s');
        $bukti      = $input['bukti_pembayaran'] ?? ($input['buktiPembayaran'] ?? null);

        if ($anggota_id <= 0 || $nominal <= 0) {
            sendResponse(false, "ID Anggota dan nominal (harus > 0) wajib diisi", null, [], 400);
        }

        try {
            $stmt_m = $pdo->prepare("SELECT id, nama, nra FROM anggota WHERE id = ?");
            $stmt_m->execute([$anggota_id]);
            $anggota = $stmt_m->fetch(PDO::FETCH_ASSOC);
            if (!$anggota) {
                sendResponse(false, "Anggota tidak ditemukan", null, [], 404);
            }

            // Anti-Double Submit / Idempotency Check (15 detik)
            $checkDup = $pdo->prepare("
                SELECT id, nominal, tanggal FROM cicilan 
                WHERE anggota_id = ? AND nominal = ? 
                ORDER BY id DESC LIMIT 1
            ");
            $checkDup->execute([$anggota_id, $nominal]);
            if ($existing = $checkDup->fetch(PDO::FETCH_ASSOC)) {
                $exTime = strtotime($existing['tanggal'] ?? '');
                if ($exTime > 0 && (time() - $exTime) < 15) {
                    list($curTotal, $curSisa) = recalculateAnggotaCicilan($pdo, $anggota_id);
                    sendResponse(true, "Pembayaran cicilan sudah tercatat sebelumnya (duplikasi dicegah)", [
                        "id" => (int)$existing['id'],
                        "anggota_id" => $anggota_id,
                        "nominal" => $nominal,
                        "total_cicilan" => $curTotal,
                        "sisa_cicilan" => $curSisa,
                        "is_duplicate" => true
                    ]);
                }
            }

            $pdo->beginTransaction();

            // 1. Simpan ke cicilan (Single Source of Truth)
            $stmt_c = $pdo->prepare("
                INSERT INTO cicilan (anggota_id, nominal, tanggal, keterangan) 
                VALUES (?, ?, ?, ?)
            ");
            $stmt_c->execute([$anggota_id, $nominal, $tanggal, $keterangan]);
            $cicilanId = $pdo->lastInsertId();

            // 2. Mirroring ke pembayaran dengan firestoreId sebagai referensi tertaut
            try {
                $nowTs = is_numeric($tanggal) ? intval($tanggal) : (time() * 1000);
                $linkRef = 'cic_' . $cicilanId;
                $stmt_pem = $pdo->prepare("
                    INSERT INTO pembayaran (firestoreId, anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan, buktiPembayaran) 
                    VALUES (?, ?, ?, 'CICILAN', ?, ?, ?, ?)
                ");
                $stmt_pem->execute([
                    $linkRef,
                    $anggota_id,
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
                        VALUES (?, ?, 'CICILAN', ?, ?, ?, ?)
                    ");
                    $stmt_pem2->execute([$anggota_id, $anggota['nama'], $nominal, $nowTs, $keterangan, $bukti]);
                } catch (Exception $e2) {}
            }

            // 3. Rekalkulasi cicilan anggota
            list($totalDibayar, $sisaCicilan) = recalculateAnggotaCicilan($pdo, $anggota_id);

            $pdo->commit();

            sendResponse(true, "Pembayaran cicilan berhasil dicatat", [
                "id" => (int)$cicilanId,
                "anggota_id" => $anggota_id,
                "nominal" => $nominal,
                "total_cicilan" => $totalDibayar,
                "sisa_cicilan" => $sisaCicilan
            ]);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            sendResponse(false, "Gagal mencatat cicilan: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'PUT':
        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);
        $id = intval($input['id'] ?? 0);
        $nominal = floatval($input['nominal'] ?? 0);
        $keterangan = trim($input['keterangan'] ?? '');

        if ($id <= 0 || $nominal <= 0) {
            sendResponse(false, "ID dan nominal valid wajib disertakan", null, [], 400);
        }

        try {
            $stmt_old = $pdo->prepare("SELECT anggota_id FROM cicilan WHERE id = ?");
            $stmt_old->execute([$id]);
            $oldRow = $stmt_old->fetch(PDO::FETCH_ASSOC);
            if (!$oldRow) {
                sendResponse(false, "Data cicilan tidak ditemukan", null, [], 404);
            }
            $targetAnggotaId = intval($oldRow['anggota_id']);

            $pdo->beginTransaction();
            $upd = $pdo->prepare("UPDATE cicilan SET nominal = ?, keterangan = ? WHERE id = ?");
            $upd->execute([$nominal, $keterangan, $id]);

            list($totalDibayar, $sisaCicilan) = recalculateAnggotaCicilan($pdo, $targetAnggotaId);
            $pdo->commit();

            sendResponse(true, "Cicilan berhasil diperbarui", [
                "id" => $id,
                "anggota_id" => $targetAnggotaId,
                "total_cicilan" => $totalDibayar,
                "sisa_cicilan" => $sisaCicilan
            ]);
        } catch (Exception $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            sendResponse(false, "Gagal mengubah cicilan: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeleteCicilan($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

/**
 * Handle Daftar Cicilan Aktif
 */
function handleDaftarCicilanAktif($pdo, $input) {
    try {
        $user_role = strtoupper(trim($input['role'] ?? ($input['user_role'] ?? '')));
        $user_nra  = trim($input['nra'] ?? '');
        $isMemberOnly = in_array($user_role, ['MEMBER', 'ANGGOTA', 'GUEST']) && !empty($user_nra);

        $params = [];
        $whereMember = "";
        if ($isMemberOnly) {
            $whereMember = " AND a.nra = ?";
            $params[] = $user_nra;
        }

        $query = "
            SELECT 
                a.id, 
                a.nama, 
                a.nra,
                COALESCE(a.harga_barang, a.hargaBarang, 0) AS harga_barang,
                COALESCE(c.total_bayar, a.total_cicilan, 0) AS sudah_dibayar,
                CASE 
                    WHEN COALESCE(a.harga_barang, a.hargaBarang, 0) > 0 
                        THEN GREATEST(0, COALESCE(a.harga_barang, a.hargaBarang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0))
                    ELSE GREATEST(0, COALESCE(a.sisa_cicilan, a.sisaCicilan, 0))
                END AS sisa_cicilan,
                COALESCE(a.cicilan_per_bulan, 0) AS cicilan_per_bulan
            FROM anggota a
            LEFT JOIN (
                SELECT anggota_id, COALESCE(SUM(nominal), 0) AS total_bayar
                FROM cicilan
                GROUP BY anggota_id
            ) c ON a.id = c.anggota_id
            WHERE ((COALESCE(a.harga_barang, a.hargaBarang, 0) - COALESCE(c.total_bayar, a.total_cicilan, 0) > 0) 
                   OR COALESCE(a.sisa_cicilan, a.sisaCicilan, 0) > 0)
            $whereMember
            ORDER BY sisa_cicilan DESC
        ";

        $stmt = $pdo->prepare($query);
        $stmt->execute($params);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $totalHargaBarang = 0.0;
        $totalSudahDibayar = 0.0;
        $totalSisaCicilan = 0.0;
        $anggotaMencicil = count($rows);

        foreach ($rows as $r) {
            $totalHargaBarang += floatval($r['harga_barang']);
            $totalSudahDibayar += floatval($r['sudah_dibayar']);
            $totalSisaCicilan += floatval($r['sisa_cicilan']);
        }

        sendResponse(true, "Daftar cicilan aktif berhasil dimuat", $rows, [
            "total_harga_barang"  => $totalHargaBarang,
            "total_sudah_dibayar" => $totalSudahDibayar,
            "total_sisa_cicilan"  => $totalSisaCicilan,
            "anggota_mencicil"    => $anggotaMencicil
        ]);
    } catch (Exception $e) {
        sendResponse(false, "Gagal memuat cicilan aktif: " . $e->getMessage(), null, [], 500);
    }
}

/**
 * Handle Hapus Cicilan
 */
function handleDeleteCicilan($pdo, $input) {
    verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

    $id = intval($input['id_cicilan'] ?? ($input['id'] ?? 0));
    if ($id <= 0) {
        sendResponse(false, "ID cicilan tidak valid", null, [], 400);
    }

    try {
        $pdo->beginTransaction();

        $targetAnggotaId = 0;
        $targetNominal   = 0;
        $isFromCicilan   = false;
        $linkedCicId     = 0;

        // Cek di tabel cicilan
        $check_c = $pdo->prepare("SELECT id, anggota_id, nominal FROM cicilan WHERE id = ?");
        $check_c->execute([$id]);
        if ($row = $check_c->fetch(PDO::FETCH_ASSOC)) {
            $isFromCicilan   = true;
            $targetAnggotaId = intval($row['anggota_id']);
            $targetNominal   = floatval($row['nominal']);
            $linkedCicId     = intval($row['id']);
        }

        // Cek di tabel pembayaran
        if (!$isFromCicilan) {
            $check_pem = $pdo->prepare("SELECT id, firestoreId, anggotaId, nominal FROM pembayaran WHERE id = ? AND UPPER(jenisPembayaran) = 'CICILAN'");
            $check_pem->execute([$id]);
            if ($row_p = $check_pem->fetch(PDO::FETCH_ASSOC)) {
                $targetAnggotaId = intval($row_p['anggotaId']);
                $targetNominal   = floatval($row_p['nominal']);
                $fId = $row_p['firestoreId'] ?? '';
                if (str_starts_with($fId, 'cic_')) {
                    $linkedCicId = intval(substr($fId, 4));
                }
            }
        }

        $rowsDeleted = 0;

        if ($isFromCicilan) {
            $del_c = $pdo->prepare("DELETE FROM cicilan WHERE id = ?");
            $del_c->execute([$id]);
            $rowsDeleted += $del_c->rowCount();

            $linkKey = 'cic_' . $id;
            $del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE firestoreId = ?");
            $del_pem->execute([$linkKey]);
            if ($del_pem->rowCount() === 0 && $targetAnggotaId > 0 && $targetNominal > 0) {
                $del_pem2 = $pdo->prepare("DELETE FROM pembayaran WHERE anggotaId = ? AND nominal = ? AND UPPER(jenisPembayaran) = 'CICILAN' ORDER BY id DESC LIMIT 1");
                $del_pem2->execute([$targetAnggotaId, $targetNominal]);
            }
        } else {
            $del_pem = $pdo->prepare("DELETE FROM pembayaran WHERE id = ?");
            $del_pem->execute([$id]);
            $rowsDeleted += $del_pem->rowCount();

            if ($linkedCicId > 0) {
                $del_c = $pdo->prepare("DELETE FROM cicilan WHERE id = ?");
                $del_c->execute([$linkedCicId]);
            } elseif ($targetAnggotaId > 0 && $targetNominal > 0) {
                $del_c2 = $pdo->prepare("DELETE FROM cicilan WHERE anggota_id = ? AND nominal = ? ORDER BY id DESC LIMIT 1");
                $del_c2->execute([$targetAnggotaId, $targetNominal]);
            }
        }

        $newTotal = 0.0;
        $newSisa  = 0.0;
        if ($targetAnggotaId > 0) {
            list($newTotal, $newSisa) = recalculateAnggotaCicilan($pdo, $targetAnggotaId);
        }

        $pdo->commit();

        sendResponse(true, "Data cicilan berhasil dihapus dan tagihan diperbarui", [
            "id" => $id,
            "anggota_id" => $targetAnggotaId,
            "total_cicilan" => $newTotal,
            "sisa_cicilan" => $newSisa,
            "rows_affected" => $rowsDeleted
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendResponse(false, "Gagal menghapus cicilan: " . $e->getMessage(), null, [], 500);
    }
}
