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

$id = isset($_GET['id']) ? intval($_GET['id']) : (isset($_GET['anggota_id']) ? intval($_GET['anggota_id']) : 0);

if ($id <= 0) {
    $data = json_decode(file_get_contents("php://input"));
    if (isset($data->id)) {
        $id = intval($data->id);
    } elseif (isset($data->anggota_id)) {
        $id = intval($data->anggota_id);
    }
}

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(array("status" => "error", "message" => "ID Anggota tidak valid atau kosong"));
    exit();
}

try {
    // 1. Query 1: Ambil data Profil Anggota dengan COALESCE total_uang_kas agar selalu terbaca meskipun riwayat kosong
    $stmt = $conn->prepare("SELECT a.*, COALESCE((SELECT SUM(nominal) FROM riwayat_kas WHERE id_anggota = a.id), 0) AS total_uang_kas FROM anggota a WHERE a.id = ? OR a.nra = ?");
    $stmt->execute(array($id, $id));
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

        $response = array(
            "id" => intval($row['id']),
            "nama" => isset($row['nama']) ? $row['nama'] : '',
            "role" => isset($row['role']) ? $row['role'] : 'Anggota',
            "no_wa" => isset($row['no_wa']) ? $row['no_wa'] : '',
            "alamat" => isset($row['alamat']) ? $row['alamat'] : '',
            "tgl_gabung" => isset($row['tgl_gabung']) ? $row['tgl_gabung'] : '',
            "uang_kas" => $uangKas,
            "iuran_aniv" => $iuranAniv,
            "kas" => $uangKas,
            "total_kas" => $uangKas,
            "total_aniv" => $iuranAniv,
            "total_cicilan" => $totalCicilan,
            "harga_barang" => $hargaBarang,
            "sisa_cicilan" => $sisaCicilan,
            "cicilan_per_bulan" => $cicilanPerBulan,
            "nra" => isset($row['nra']) ? $row['nra'] : '',
            "statusAktif" => isset($row['statusAktif']) ? (bool)$row['statusAktif'] : true,
            "status" => isset($row['status']) ? $row['status'] : ((isset($row['statusAktif']) && !$row['statusAktif']) ? 'Nonaktif' : 'Aktif'),
            "foto" => isset($row['foto']) ? $row['foto'] : '',
            "totalTagihan" => floatval(isset($row['totalTagihan']) ? $row['totalTagihan'] : 0),
            "lamaCicilan" => intval(isset($row['lamaCicilan']) ? $row['lamaCicilan'] : 0),
            "riwayat_kas" => $riwayat_kas,
            "riwayat_aniv" => $riwayat_aniv,
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
