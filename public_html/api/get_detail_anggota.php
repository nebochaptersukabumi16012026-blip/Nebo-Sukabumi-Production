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
    // Try to get from JSON body if POST
    $data = json_decode(file_get_contents("php://input"));
    if (isset($data->id)) {
        $id = intval($data->id);
    } elseif (isset($data->anggota_id)) {
        $id = intval($data->anggota_id);
    }
}

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "ID Anggota tidak valid atau kosong"]);
    exit();
}

try {
    $stmt = $conn->prepare("SELECT * FROM anggota WHERE id = ?");
    $stmt->execute([$id]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($row) {
        // Explicitly map values
        $uangKas = floatval($row['uang_kas'] ?? 0);
        $iuranAniv = floatval($row['iuran_aniv'] ?? 0);
        $totalCicilan = floatval($row['total_cicilan'] ?? 0);
        $hargaBarang = floatval($row['harga_barang'] ?? 0);
        $sisaCicilan = floatval($row['sisa_cicilan'] ?? 0);
        $cicilanPerBulan = floatval($row['cicilan_per_bulan'] ?? 0);

        // Fetch payment history for this member if exists
        $stmt_pay = $conn->prepare("SELECT * FROM pembayaran WHERE anggotaId = ? ORDER BY tanggal DESC");
        $stmt_pay->execute([$id]);
        $riwayatPembayaran = $stmt_pay->fetchAll(PDO::FETCH_ASSOC);

        // Fetch from riwayat_kas
        $stmt_rk = $conn->prepare("SELECT * FROM riwayat_kas WHERE id_anggota = ? ORDER BY id DESC");
        $stmt_rk->execute([$id]);
        $riwayatKasDb = $stmt_rk->fetchAll(PDO::FETCH_ASSOC);

        // Fetch from riwayat_aniv
        $stmt_ra = $conn->prepare("SELECT * FROM riwayat_aniv WHERE id_anggota = ? ORDER BY id DESC");
        $stmt_ra->execute([$id]);
        $riwayatAnivDb = $stmt_ra->fetchAll(PDO::FETCH_ASSOC);

        // Build riwayat_kas list
        $riwayat_kas = [];
        foreach ($riwayatKasDb as $rk) {
            $riwayat_kas[] = [
                "id" => intval($rk['id']),
                "id_transaksi" => "kas_" . $rk['id'],
                "nominal" => floatval($rk['nominal']),
                "tanggal" => $rk['tanggal'],
                "keterangan" => $rk['keterangan'] ?? 'Pembayaran Uang Kas'
            ];
        }
        foreach ($riwayatPembayaran as $p) {
            if (in_array(strtolower($p['jenisPembayaran'] ?? ''), ['kas', 'uang_kas'])) {
                $exists = false;
                foreach ($riwayat_kas as $rkItem) {
                    if ($rkItem['id'] == intval($p['id'])) {
                        $exists = true;
                        break;
                    }
                }
                if (!$exists) {
                    $riwayat_kas[] = [
                        "id" => intval($p['id']),
                        "id_transaksi" => "kas_" . $p['id'],
                        "nominal" => floatval($p['nominal']),
                        "tanggal" => is_numeric($p['tanggal']) ? date('d M Y', intval($p['tanggal'] / 1000)) : $p['tanggal'],
                        "keterangan" => $p['keterangan'] ?? 'Pembayaran Uang Kas',
                        "buktiPembayaran" => $p['buktiPembayaran'] ?? null
                    ];
                }
            }
        }

        // Auto-generate fallback for riwayat_kas if empty but uang_kas > 0
        if (empty($riwayat_kas) && $uangKas > 0) {
            $tglStr = !empty($row['tgl_gabung']) ? $row['tgl_gabung'] : date('d M Y');
            $riwayat_kas[] = [
                "id" => intval($row['id']),
                "id_transaksi" => "kas_" . $row['id'],
                "nominal" => $uangKas,
                "tanggal" => $tglStr,
                "keterangan" => "Pembayaran Uang Kas",
                "buktiPembayaran" => null
            ];

            // Auto-heal/insert into database so subsequent lists remain populated
            try {
                $nowTs = time() * 1000;
                $formattedNow = date('Y-m-d H:i:s');
                $stmt_heal = $conn->prepare("INSERT INTO pembayaran (anggotaId, anggotaNama, jenisPembayaran, nominal, tanggal, keterangan) VALUES (?, ?, 'KAS', ?, ?, 'Pembayaran Uang Kas')");
                $stmt_heal->execute([$id, $row['nama'] ?? '', $uangKas, $nowTs]);
                $newPemId = $conn->lastInsertId();

                $stmt_heal_rk = $conn->prepare("INSERT INTO riwayat_kas (id_anggota, nominal, tanggal, keterangan, created_at) VALUES (?, ?, ?, 'Pembayaran Uang Kas', ?)");
                $stmt_heal_rk->execute([$id, $uangKas, $formattedNow, $nowTs]);

                // Re-fetch to reflect generated ID
                $stmt_pay->execute([$id]);
                $riwayatPembayaran = $stmt_pay->fetchAll(PDO::FETCH_ASSOC);
            } catch (Exception $e) {}
        }

        // Build riwayat_aniv list
        $riwayat_aniv = [];
        foreach ($riwayatAnivDb as $ra) {
            $riwayat_aniv[] = [
                "id" => intval($ra['id']),
                "id_transaksi" => "aniv_" . $ra['id'],
                "nominal" => floatval($ra['nominal']),
                "tanggal" => $ra['tanggal'],
                "keterangan" => $ra['keterangan'] ?? 'Iuran Anniversary'
            ];
        }
        foreach ($riwayatPembayaran as $p) {
            if (in_array(strtolower($p['jenisPembayaran'] ?? ''), ['aniv', 'iuran_aniv', 'anniversary'])) {
                $exists = false;
                foreach ($riwayat_aniv as $raItem) {
                    if ($raItem['id'] == intval($p['id'])) {
                        $exists = true;
                        break;
                    }
                }
                if (!$exists) {
                    $riwayat_aniv[] = [
                        "id" => intval($p['id']),
                        "id_transaksi" => "aniv_" . $p['id'],
                        "nominal" => floatval($p['nominal']),
                        "tanggal" => is_numeric($p['tanggal']) ? date('d M Y', intval($p['tanggal'] / 1000)) : $p['tanggal'],
                        "keterangan" => $p['keterangan'] ?? 'Iuran Anniversary',
                        "buktiPembayaran" => $p['buktiPembayaran'] ?? null
                    ];
                }
            }
        }

        // Auto-generate fallback for riwayat_aniv if empty but iuran_aniv > 0
        if (empty($riwayat_aniv) && $iuranAniv > 0) {
            $tglStr = !empty($row['tgl_gabung']) ? $row['tgl_gabung'] : date('d M Y');
            $riwayat_aniv[] = [
                "id" => intval($row['id']),
                "id_transaksi" => "aniv_" . $row['id'],
                "nominal" => $iuranAniv,
                "tanggal" => $tglStr,
                "keterangan" => "Iuran Anniversary",
                "buktiPembayaran" => null
            ];
        }

        $response = [
            "id" => intval($row['id']),
            "nama" => $row['nama'] ?? '',
            "role" => $row['role'] ?? 'Anggota',
            "no_wa" => $row['no_wa'] ?? '',
            "alamat" => $row['alamat'] ?? '',
            "tgl_gabung" => $row['tgl_gabung'] ?? '',
            "uang_kas" => $uangKas,
            "iuran_aniv" => $iuranAniv,
            "kas" => $uangKas,
            "total_kas" => $uangKas,
            "total_aniv" => $iuranAniv,
            "total_cicilan" => $totalCicilan,
            "harga_barang" => $hargaBarang,
            "sisa_cicilan" => $sisaCicilan,
            "cicilan_per_bulan" => $cicilanPerBulan,
            "nra" => $row['nra'] ?? '',
            "statusAktif" => isset($row['statusAktif']) ? (bool)$row['statusAktif'] : true,
            "status" => $row['status'] ?? ((isset($row['statusAktif']) && !$row['statusAktif']) ? 'Nonaktif' : 'Aktif'),
            "foto" => $row['foto'] ?? '',
            "totalTagihan" => floatval($row['totalTagihan'] ?? 0),
            "lamaCicilan" => intval($row['lamaCicilan'] ?? 0),
            "riwayat_kas" => $riwayat_kas,
            "riwayat_aniv" => $riwayat_aniv,
            "riwayat_pembayaran" => $riwayatPembayaran
        ];

        echo json_encode([
            "status" => "success",
            "data" => $response,
            "anggota" => $response
        ]);
    } else {
        http_response_code(404);
        echo json_encode(["status" => "error", "message" => "Anggota tidak ditemukan"]);
    }
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
}
?>
