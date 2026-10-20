<?php
// get_detail_anggota.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

$nraParam = isset($_GET['nra']) ? trim($_GET['nra']) : '';
$id = isset($_GET['id']) ? intval($_GET['id']) : (isset($_GET['anggota_id']) ? intval($_GET['anggota_id']) : 0);
$roleLogin = isset($_GET['role_login']) ? strtoupper(trim($_GET['role_login'])) : (isset($_GET['role']) ? strtoupper(trim($_GET['role'])) : '');
$nraLogin = isset($_GET['nra_login']) ? trim($_GET['nra_login']) : (isset($_GET['login_nra']) ? trim($_GET['login_nra']) : '');

if ($id <= 0 && empty($nraParam)) {
    $data = json_decode(file_get_contents("php://input"));
    if (isset($data->id)) {
        $id = intval($data->id);
    } elseif (isset($data->anggota_id)) {
        $id = intval($data->anggota_id);
    } elseif (isset($data->nra)) {
        $nraParam = trim($data->nra);
    }
    if (empty($roleLogin) && isset($data->role_login)) {
        $roleLogin = strtoupper(trim($data->role_login));
    }
    if (empty($nraLogin) && isset($data->nra_login)) {
        $nraLogin = trim($data->nra_login);
    }
}

if ($id <= 0 && empty($nraParam)) {
    http_response_code(400);
    echo json_encode(array("status" => "error", "message" => "ID atau NRA Anggota tidak valid atau kosong"));
    exit();
}

try {
    // 1. Query 1: Ambil data Profil Anggota dengan COALESCE total_uang_kas agar selalu terbaca meskipun riwayat kosong
    if ($id > 0) {
        $stmt = $conn->prepare("SELECT a.*, COALESCE((SELECT SUM(nominal) FROM riwayat_kas WHERE id_anggota = a.id), 0) AS total_uang_kas FROM anggota a WHERE a.id = ? OR a.nra = ?");
        $stmt->execute(array($id, strval($id)));
    } else {
        $stmt = $conn->prepare("SELECT a.*, COALESCE((SELECT SUM(nominal) FROM riwayat_kas WHERE id_anggota = a.id), 0) AS total_uang_kas FROM anggota a WHERE a.nra = ?");
        $stmt->execute(array($nraParam));
    }
    $row = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($row) {
        $rowUangKas = floatval(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
        $totalUangKas = floatval(isset($row['total_uang_kas']) ? $row['total_uang_kas'] : 0);
        
        // Prioritaskan kolom saldo tabel anggota (uang_kas) jika ada, atau max dengan total riwayat yang tersisa
        $uangKas = ($rowUangKas > 0) ? $rowUangKas : max($rowUangKas, $totalUangKas);
        if ($uangKas <= 0 && $rowUangKas > 0) {
            $uangKas = $rowUangKas;
        }

        $iuranAniv = floatval(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : 0);
        $totalCicilan = floatval(isset($row['total_cicilan']) ? $row['total_cicilan'] : 0);
        $hargaBarang = floatval(isset($row['harga_barang']) ? $row['harga_barang'] : 0);
        $sisaCicilan = floatval(isset($row['sisa_cicilan']) ? $row['sisa_cicilan'] : 0);
        $cicilanPerBulan = floatval(isset($row['cicilan_per_bulan']) ? $row['cicilan_per_bulan'] : 0);
        $nraVal = isset($row['nra']) ? $row['nra'] : '';

        // Hitung total_dibayar secara akurat dari tabel cicilan (Single Source of Truth)
        $stmt_cic_sum = $conn->prepare("SELECT COALESCE(SUM(nominal), 0) as total FROM cicilan WHERE anggota_id = ?");
        $stmt_cic_sum->execute(array($row['id']));
        $row_cic_sum = $stmt_cic_sum->fetch(PDO::FETCH_ASSOC);
        $sudahDibayarRiil = floatval($row_cic_sum['total'] ?? 0);
        if ($sudahDibayarRiil <= 0 && $totalCicilan > 0) {
            $sudahDibayarRiil = $totalCicilan;
        }

        if ($hargaBarang > 0) {
            $sisaCicilan = max(0.0, $hargaBarang - $sudahDibayarRiil);
        }
        $totalDibayar = $sudahDibayarRiil;

        // Ambil riwayat cicilan langsung dari tabel cicilan (Single Source of Truth)
        $stmt_cic_list = $conn->prepare("SELECT id, anggota_id, nominal, tanggal, keterangan FROM cicilan WHERE anggota_id = ? ORDER BY tanggal DESC, id DESC");
        $stmt_cic_list->execute(array($row['id']));
        $riwayatCicilanDb = $stmt_cic_list ? $stmt_cic_list->fetchAll(PDO::FETCH_ASSOC) : array();

        $riwayat_cicilan = array();
        foreach ($riwayatCicilanDb as $rc) {
            $riwayat_cicilan[] = array(
                "id" => intval($rc['id']),
                "id_transaksi" => "cicilan_" . $rc['id'],
                "nominal" => floatval($rc['nominal']),
                "tanggal" => $rc['tanggal'],
                "keterangan" => !empty($rc['keterangan']) ? $rc['keterangan'] : 'Pembayaran Cicilan'
            );
        }

        // Hak Akses Cicilan:
        // ADMIN, BENDAHARA, DEVELOPER, PENGURUS selalu dapat melihat data cicilan.
        // Pemilik akun sendiri (nra_login == nra target) juga dapat melihat data cicilan.
        // Anggota/Member lain tidak dapat melihat cicilan anggota lain (privasi terlindungi).
        $isPrivileged = in_array($roleLogin, array('ADMIN', 'BENDAHARA', 'DEVELOPER', 'PENGURUS'));
        $isSelf = (!empty($nraLogin) && !empty($nraVal) && ($nraLogin === $nraVal));
        $canSeeCicilan = ($isPrivileged || $isSelf || empty($roleLogin)); // default true if internal/unspecified or privileged/self

        // 2. Query 2: Ambil riwayat kas dari tabel riwayat_kas / kas_komunitas WHERE nra = :nra / id_anggota = :id ORDER BY tanggal DESC
        $stmt_rk = $conn->prepare("SELECT * FROM riwayat_kas WHERE id_anggota = ? OR id_anggota IN (SELECT id FROM anggota WHERE nra = ?) ORDER BY id DESC");
        $stmt_rk->execute(array($id, $nraVal));
        $riwayatKasDb = $stmt_rk->fetchAll(PDO::FETCH_ASSOC);

        $stmt_kk = $conn->prepare("SELECT * FROM kas_komunitas WHERE nra = ? ORDER BY tanggal DESC");
        $stmt_kk->execute(array($nraVal));
        $kasKomunitasDb = $stmt_kk->fetchAll(PDO::FETCH_ASSOC);

        // Fetch payment history for this member if exists
        $stmt_pay = $conn->prepare("SELECT * FROM pembayaran WHERE anggotaId = ? OR anggotaId IN (SELECT id FROM anggota WHERE nra = ?) ORDER BY tanggal DESC");
        $stmt_pay->execute(array($id, $nraVal));
        $riwayatPembayaran = $stmt_pay->fetchAll(PDO::FETCH_ASSOC);

        // Fetch from riwayat_aniv
        $stmt_ra = $conn->prepare("SELECT * FROM riwayat_aniv WHERE id_anggota = ? ORDER BY id DESC");
        $stmt_ra->execute(array($id));
        $riwayatAnivDb = $stmt_ra->fetchAll(PDO::FETCH_ASSOC);

        // Build riwayat_kas list
        $riwayat_kas = array();
        foreach ($riwayatKasDb as $rk) {
            $riwayat_kas[] = array(
                "id" => intval($rk['id']),
                "id_transaksi" => "kas_" . $rk['id'],
                "nominal" => floatval($rk['nominal']),
                "tanggal" => $rk['tanggal'],
                "keterangan" => isset($rk['keterangan']) ? $rk['keterangan'] : 'Pembayaran Uang Kas'
            );
        }
        foreach ($riwayatPembayaran as $p) {
            if (in_array(strtolower(isset($p['jenisPembayaran']) ? $p['jenisPembayaran'] : ''), array('kas', 'uang_kas'))) {
                $exists = false;
                foreach ($riwayat_kas as $rkItem) {
                    if ($rkItem['id'] == intval($p['id'])) {
                        $exists = true;
                        break;
                    }
                }
                if (!$exists) {
                    $riwayat_kas[] = array(
                        "id" => intval($p['id']),
                        "id_transaksi" => "kas_" . $p['id'],
                        "nominal" => floatval($p['nominal']),
                        "tanggal" => is_numeric($p['tanggal']) ? date('d M Y', intval($p['tanggal'] / 1000)) : $p['tanggal'],
                        "keterangan" => isset($p['keterangan']) ? $p['keterangan'] : 'Pembayaran Uang Kas',
                        "buktiPembayaran" => isset($p['buktiPembayaran']) ? $p['buktiPembayaran'] : null
                    );
                }
            }
        }

        // Build riwayat_aniv list
        $riwayat_aniv = array();
        foreach ($riwayatAnivDb as $ra) {
            $riwayat_aniv[] = array(
                "id" => intval($ra['id']),
                "id_transaksi" => "aniv_" . $ra['id'],
                "nominal" => floatval($ra['nominal']),
                "tanggal" => $ra['tanggal'],
                "keterangan" => isset($ra['keterangan']) ? $ra['keterangan'] : 'Iuran Anniversary'
            );
        }
        foreach ($riwayatPembayaran as $p) {
            if (in_array(strtolower(isset($p['jenisPembayaran']) ? $p['jenisPembayaran'] : ''), array('aniv', 'iuran_aniv', 'anniversary'))) {
                $exists = false;
                foreach ($riwayat_aniv as $raItem) {
                    if ($raItem['id'] == intval($p['id'])) {
                        $exists = true;
                        break;
                    }
                }
                if (!$exists) {
                    $riwayat_aniv[] = array(
                        "id" => intval($p['id']),
                        "id_transaksi" => "aniv_" . $p['id'],
                        "nominal" => floatval($p['nominal']),
                        "tanggal" => is_numeric($p['tanggal']) ? date('d M Y', intval($p['tanggal'] / 1000)) : $p['tanggal'],
                        "keterangan" => isset($p['keterangan']) ? $p['keterangan'] : 'Iuran Anniversary',
                        "buktiPembayaran" => isset($p['buktiPembayaran']) ? $p['buktiPembayaran'] : null
                    );
                }
            }
        }

        $noWa = isset($row['no_wa']) && $row['no_wa'] !== '' ? $row['no_wa'] : (isset($row['nomor_telepon']) ? $row['nomor_telepon'] : (isset($row['no_hp']) ? $row['no_hp'] : ''));
        $alamat = isset($row['alamat']) ? $row['alamat'] : '';
        $tglGabung = isset($row['tgl_gabung']) && $row['tgl_gabung'] !== '' ? $row['tgl_gabung'] : (isset($row['tanggal_bergabung']) ? $row['tanggal_bergabung'] : '');
        $nomorUrut = isset($row['nomor_urut']) && $row['nomor_urut'] !== '' ? $row['nomor_urut'] : (isset($row['nomorUrut']) ? $row['nomorUrut'] : (isset($row['no_urut']) ? $row['no_urut'] : ''));

        $response = array(
            "id" => intval($row['id']),
            "nama" => isset($row['nama']) ? $row['nama'] : '',
            "role" => isset($row['role']) ? $row['role'] : 'Anggota',
            "no_wa" => $noWa,
            "nomor_telepon" => $noWa,
            "no_hp" => $noWa,
            "alamat" => $alamat,
            "tgl_gabung" => $tglGabung,
            "tanggal_bergabung" => $tglGabung,
            "nomor_urut" => $nomorUrut,
            "nomorUrut" => $nomorUrut,
            "uang_kas" => $uangKas,
            "iuran_aniv" => $iuranAniv,
            "kas" => $uangKas,
            "total_kas" => $uangKas,
            "total_aniv" => $iuranAniv,
            "total_cicilan" => $totalCicilan,
            "total_dibayar" => $totalDibayar,
            "sudah_dibayar" => $totalDibayar,
            "harga_barang" => $hargaBarang,
            "sisa_cicilan" => $sisaCicilan,
            "can_see_cicilan" => (bool)$canSeeCicilan,
            "cicilan_per_bulan" => $cicilanPerBulan,
            "nra" => isset($row['nra']) ? $row['nra'] : '',
            "statusAktif" => isset($row['statusAktif']) ? (bool)$row['statusAktif'] : true,
            "status" => isset($row['status']) ? $row['status'] : ((isset($row['statusAktif']) && !$row['statusAktif']) ? 'Nonaktif' : 'Aktif'),
            "foto" => isset($row['foto']) ? $row['foto'] : '',
            "totalTagihan" => floatval(isset($row['totalTagihan']) ? $row['totalTagihan'] : 0),
            "lamaCicilan" => intval(isset($row['lamaCicilan']) ? $row['lamaCicilan'] : 0),
            "riwayat_kas" => $riwayat_kas,
            "riwayat_aniv" => $riwayat_aniv,
            "riwayat_cicilan" => $riwayat_cicilan,
            "riwayat_pembayaran" => $riwayatPembayaran
        );

        echo json_encode(array(
            "status" => "success",
            "data" => $response,
            "anggota" => $response
        ));
    } else {
        http_response_code(404);
        echo json_encode(array("status" => "error", "message" => "Anggota tidak ditemukan"));
    }
} catch (Exception $e) {
    echo json_encode(array("status" => "error", "message" => "Database error: " . $e->getMessage()));
}
?>
