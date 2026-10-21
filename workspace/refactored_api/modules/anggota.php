<?php
// workspace/refactored_api/modules/anggota.php
// Modul Terpadu Manajemen Anggota (Daftar, Detail, Tambah, Edit, Hapus)

require_once __DIR__ . '/../core/config.php';
require_once __DIR__ . '/../core/sync_helper.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();

switch ($method) {
    case 'GET':
        $action = $input['action'] ?? '';
        $id     = intval($input['id'] ?? ($input['anggota_id'] ?? 0));
        $nra    = trim($input['nra'] ?? '');

        if ($action === 'detail' || $id > 0 || !empty($nra)) {
            // Mode: Detail Anggota Lengkap
            handleDetailAnggota($pdo, $id, $nra);
        } else {
            // Mode: List Semua Anggota
            handleListAnggota($pdo);
        }
        break;

    case 'POST':
        $action = $input['action'] ?? '';
        if ($action === 'delete' || isset($input['hapus'])) {
            handleDeleteAnggota($pdo, $input);
            break;
        }

        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $nama = trim($input['nama'] ?? '');
        if (empty($nama)) {
            sendResponse(false, "Nama anggota tidak boleh kosong", null, [], 400);
        }

        try {
            $nra               = trim($input['nra'] ?? '');
            $role              = trim($input['role'] ?? 'Anggota');
            $no_wa             = trim($input['no_wa'] ?? ($input['telepon'] ?? ($input['nomor_telepon'] ?? '')));
            $alamat            = trim($input['alamat'] ?? '');
            $tgl_gabung        = trim($input['tgl_gabung'] ?? ($input['tanggal_bergabung'] ?? date('Y-m-d')));
            $nomor_urut        = trim($input['nomor_urut'] ?? ($input['no_urut'] ?? ''));
            $uang_kas          = floatval($input['uang_kas'] ?? ($input['uangKas'] ?? 0));
            $iuran_aniv        = floatval($input['iuran_aniv'] ?? ($input['iuranAniv'] ?? 0));
            $harga_barang      = floatval($input['harga_barang'] ?? ($input['hargaBarang'] ?? 0));
            $total_cicilan     = floatval($input['total_cicilan'] ?? 0);
            $sisa_cicilan      = max(0.0, $harga_barang - $total_cicilan);
            $cicilan_per_bulan = floatval($input['cicilan_per_bulan'] ?? 0);
            $statusAktif       = isset($input['statusAktif']) ? ($input['statusAktif'] ? 1 : 0) : 1;
            $username          = trim($input['username'] ?? '');
            $password          = trim($input['password'] ?? '12345');
            $foto              = $input['foto'] ?? null;

            $stmt = $pdo->prepare("
                INSERT INTO anggota (
                    nama, role, no_wa, alamat, tgl_gabung, nomor_urut,
                    uang_kas, iuran_aniv, total_cicilan, harga_barang, sisa_cicilan,
                    cicilan_per_bulan, nra, statusAktif, username, password, foto
                ) VALUES (
                    ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?
                )
            ");
            $stmt->execute([
                $nama, $role, $no_wa, $alamat, $tgl_gabung, $nomor_urut,
                $uang_kas, $iuran_aniv, $total_cicilan, $harga_barang, $sisa_cicilan,
                $cicilan_per_bulan, $nra, $statusAktif, $username, $password, $foto
            ]);
            $newId = $pdo->lastInsertId();

            // Sync camelCase columns if they exist
            try {
                $stmtCamel = $pdo->prepare("UPDATE anggota SET hargaBarang = ?, sisaCicilan = ?, uangKas = ?, iuranAniv = ? WHERE id = ?");
                $stmtCamel->execute([$harga_barang, $sisa_cicilan, $uang_kas, $iuran_aniv, $newId]);
            } catch (Exception $e) {}

            sendResponse(true, "Anggota berhasil ditambahkan", [
                "id"   => (int)$newId,
                "nama" => $nama,
                "nra"  => $nra
            ]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal menambah anggota: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'PUT':
        verifyRole(['ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS']);

        $id = intval($input['id'] ?? 0);
        if ($id <= 0) {
            sendResponse(false, "ID anggota tidak ditemukan atau tidak valid", null, [], 400);
        }

        try {
            $stmt_old = $pdo->prepare("SELECT * FROM anggota WHERE id = ? LIMIT 1");
            $stmt_old->execute([$id]);
            $existing = $stmt_old->fetch(PDO::FETCH_ASSOC);
            if (!$existing) {
                sendResponse(false, "Data anggota tidak ditemukan", null, [], 404);
            }

            $nama        = !empty($input['nama']) ? trim($input['nama']) : $existing['nama'];
            $nra         = isset($input['nra']) ? trim($input['nra']) : ($existing['nra'] ?? '');
            $alamat      = isset($input['alamat']) ? trim($input['alamat']) : ($existing['alamat'] ?? '');
            $no_wa       = isset($input['no_wa']) ? trim($input['no_wa']) : (isset($input['telepon']) ? trim($input['telepon']) : ($existing['no_wa'] ?? ''));
            $tgl_gabung  = isset($input['tgl_gabung']) ? trim($input['tgl_gabung']) : ($existing['tgl_gabung'] ?? '');
            $statusAktif = isset($input['statusAktif']) ? ($input['statusAktif'] ? 1 : 0) : ($existing['statusAktif'] ?? 1);
            $foto        = !empty($input['foto']) ? $input['foto'] : ($existing['foto'] ?? null);

            $stmt = $pdo->prepare("
                UPDATE anggota SET 
                    nama = ?, nra = ?, alamat = ?, no_wa = ?, tgl_gabung = ?, statusAktif = ?, foto = ?
                WHERE id = ?
            ");
            $stmt->execute([$nama, $nra, $alamat, $no_wa, $tgl_gabung, $statusAktif, $foto, $id]);

            sendResponse(true, "Data anggota berhasil diperbarui", [
                "id"          => $id,
                "nama"        => $nama,
                "nra"         => $nra,
                "statusAktif" => $statusAktif
            ]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal memperbarui anggota: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'DELETE':
        handleDeleteAnggota($pdo, $input);
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

function handleListAnggota($pdo) {
    try {
        $stmt = $pdo->query("SELECT * FROM anggota ORDER BY nama ASC");
        $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($row) {
            return [
                'id'                => intval($row['id']),
                'nra'               => $row['nra'] ?? '',
                'nama'              => $row['nama'] ?? '',
                'alias'             => $row['alias'] ?? ($row['nama'] ?? ''),
                'telepon'           => $row['no_wa'] ?? ($row['telepon'] ?? ''),
                'statusAktif'       => (bool)($row['statusAktif'] ?? 1),
                'tanggalLahir'      => $row['tanggalLahir'] ?? '',
                'uangKas'           => floatval($row['uang_kas'] ?? ($row['uangKas'] ?? 0)),
                'iuranAniv'         => floatval($row['iuran_aniv'] ?? ($row['iuranAniv'] ?? 0)),
                'sisaCicilan'       => floatval($row['sisa_cicilan'] ?? ($row['sisaCicilan'] ?? 0)),
                'hargaBarang'       => floatval($row['harga_barang'] ?? ($row['hargaBarang'] ?? 0)),
                'namaBarang'        => $row['namaBarang'] ?? '',
                'jabatan'           => $row['role'] ?? ($row['jabatan'] ?? 'Anggota'),
                'role'              => $row['role'] ?? 'Anggota',
                'alamat'            => $row['alamat'] ?? '',
                'foto'              => $row['foto'] ?? null,
                'tgl_gabung'        => $row['tgl_gabung'] ?? ''
            ];
        }, $data);

        sendResponse(true, "Daftar anggota berhasil dimuat", $formatted);
    } catch (Exception $e) {
        sendResponse(false, "Gagal memuat daftar anggota: " . $e->getMessage(), null, [], 500);
    }
}

function handleDetailAnggota($pdo, $id, $nra) {
    try {
        if ($id > 0) {
            $stmt = $pdo->prepare("SELECT * FROM anggota WHERE id = ?");
            $stmt->execute([$id]);
        } else {
            $stmt = $pdo->prepare("SELECT * FROM anggota WHERE nra = ?");
            $stmt->execute([$nra]);
        }
        $member = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$member) {
            sendResponse(false, "Data anggota tidak ditemukan", null, [], 404);
        }

        $memberId = intval($member['id']);

        // 1. Riwayat Kas (Single Source of Truth)
        $stmt_kas = $pdo->prepare("SELECT id, id_anggota, nominal, keterangan, tanggal FROM riwayat_kas WHERE id_anggota = ? ORDER BY id DESC");
        $stmt_kas->execute([$memberId]);
        $rawKas = $stmt_kas->fetchAll(PDO::FETCH_ASSOC);
        $riwayatKas = array_map(function($rk) {
            return [
                "id"           => intval($rk['id']),
                "id_transaksi" => "kas_" . $rk['id'],
                "nominal"      => floatval($rk['nominal']),
                "tanggal"      => $rk['tanggal'] ?? '',
                "keterangan"   => $rk['keterangan'] ?? 'Uang Kas'
            ];
        }, $rawKas);

        // 2. Riwayat Anniversary (Single Source of Truth)
        $stmt_aniv = $pdo->prepare("SELECT id, anggota_id, nominal, keterangan, tanggal FROM iuran_anniversary WHERE anggota_id = ? ORDER BY id DESC");
        $stmt_aniv->execute([$memberId]);
        $rawAniv = $stmt_aniv->fetchAll(PDO::FETCH_ASSOC);
        $riwayatAniv = array_map(function($ra) {
            return [
                "id"           => intval($ra['id']),
                "id_transaksi" => "aniv_" . $ra['id'],
                "nominal"      => floatval($ra['nominal']),
                "tanggal"      => $ra['tanggal'] ?? '',
                "keterangan"   => $ra['keterangan'] ?? 'Iuran Anniversary'
            ];
        }, $rawAniv);

        // 3. Riwayat Cicilan (Single Source of Truth)
        $stmt_cicilan = $pdo->prepare("SELECT id, anggota_id, nominal, keterangan, tanggal FROM cicilan WHERE anggota_id = ? ORDER BY id DESC");
        $stmt_cicilan->execute([$memberId]);
        $rawCic = $stmt_cicilan->fetchAll(PDO::FETCH_ASSOC);
        $riwayatCicilan = array_map(function($rc) {
            return [
                "id"           => intval($rc['id']),
                "id_transaksi" => "cicilan_" . $rc['id'],
                "nominal"      => floatval($rc['nominal']),
                "tanggal"      => $rc['tanggal'] ?? '',
                "keterangan"   => $rc['keterangan'] ?? 'Pembayaran Cicilan'
            ];
        }, $rawCic);

        // 4. Riwayat Universal Pembayaran (Read-only log)
        $stmt_pem = $pdo->prepare("SELECT * FROM pembayaran WHERE anggotaId = ? ORDER BY tanggal DESC");
        $stmt_pem->execute([$memberId]);
        $riwayatPembayaran = $stmt_pem->fetchAll(PDO::FETCH_ASSOC);

        // Saldo dan metrik anggota
        $uangKas      = floatval($member['uang_kas'] ?? ($member['uangKas'] ?? 0));
        $iuranAniv    = floatval($member['iuran_aniv'] ?? ($member['iuranAniv'] ?? 0));
        $totalCicilan = floatval($member['total_cicilan'] ?? ($member['totalCicilan'] ?? 0));
        $sisaCicilan  = floatval($member['sisa_cicilan'] ?? ($member['sisaCicilan'] ?? 0));
        $hargaBarang  = floatval($member['harga_barang'] ?? ($member['hargaBarang'] ?? 0));
        $cicilanBulan = floatval($member['cicilan_per_bulan'] ?? ($member['cicilanPerBulan'] ?? 0));
        $noWa         = $member['no_wa'] ?? ($member['nomor_telepon'] ?? ($member['no_hp'] ?? ''));

        // Konstruksi objek respons komprehensif (rata/flat + nested profil)
        $result = array_merge($member, [
            "id"                => $memberId,
            "nama"              => $member['nama'] ?? '',
            "nra"               => $member['nra'] ?? '',
            "role"              => $member['role'] ?? 'Anggota',
            "no_wa"             => $noWa,
            "nomor_telepon"     => $noWa,
            "no_hp"             => $noWa,
            "alamat"            => $member['alamat'] ?? '',
            "tgl_gabung"        => $member['tgl_gabung'] ?? '',
            "uang_kas"          => $uangKas,
            "uangKas"           => $uangKas,
            "kas"               => $uangKas,
            "total_kas"         => $uangKas,
            "iuran_aniv"        => $iuranAniv,
            "iuranAniv"         => $iuranAniv,
            "total_aniv"        => $iuranAniv,
            "harga_barang"      => $hargaBarang,
            "hargaBarang"       => $hargaBarang,
            "total_cicilan"     => $totalCicilan,
            "totalCicilan"      => $totalCicilan,
            "total_dibayar"     => $totalCicilan,
            "sudah_dibayar"     => $totalCicilan,
            "sisa_cicilan"      => $sisaCicilan,
            "sisaCicilan"       => $sisaCicilan,
            "cicilan_per_bulan" => $cicilanBulan,
            "cicilanPerBulan"   => $cicilanBulan,
            "statusAktif"       => (bool)($member['statusAktif'] ?? 1),
            "status"            => $member['status'] ?? 'Aktif',
            "profil"            => $member,
            "riwayat_kas"       => $riwayatKas,
            "riwayat_aniv"      => $riwayatAniv,
            "riwayat_cicilan"   => $riwayatCicilan,
            "riwayat_pembayaran"=> $riwayatPembayaran
        ]);

        sendResponse(true, "Detail anggota berhasil dimuat", $result, ["anggota" => $result]);
    } catch (Exception $e) {
        sendResponse(false, "Gagal memuat detail anggota: " . $e->getMessage(), null, [], 500);
    }
}

function handleDeleteAnggota($pdo, $input) {
    verifyRole(['ADMIN', 'DEVELOPER']);

    $id = intval($input['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, "ID anggota tidak ditemukan", null, [], 400);
    }

    try {
        $pdo->beginTransaction();
        $stmt = $pdo->prepare("DELETE FROM anggota WHERE id = ?");
        $stmt->execute([$id]);
        $pdo->commit();

        sendResponse(true, "Data anggota berhasil dihapus", [
            "id" => $id,
            "rows_affected" => $stmt->rowCount()
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendResponse(false, "Gagal menghapus anggota: " . $e->getMessage(), null, [], 500);
    }
}
