<?php
// get_detail_anniversary.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    try {
        // 1. Hitung Total Pemasukan Anniversary
        $total_pemasukan = 0.0;
        
        // Cek dari saldo_akumulasi jika ada
        $stmt_master = $conn->query("SELECT total_akumulasi_masuk, total_akumulasi_keluar FROM saldo_akumulasi WHERE jenis_kas IN ('kas_aniv', 'aniv', 'kas_anniversary', 'anniversary') LIMIT 1");
        $row_master = $stmt_master ? $stmt_master->fetch(PDO::FETCH_ASSOC) : null;
        
        // Cek sum dari tabel iuran_anniversary
        $stmt_sum_iuran = $conn->query("SELECT SUM(nominal) as total FROM iuran_anniversary");
        $row_sum_iuran = $stmt_sum_iuran ? $stmt_sum_iuran->fetch(PDO::FETCH_ASSOC) : null;
        $sum_iuran = ($row_sum_iuran && $row_sum_iuran['total'] !== null) ? floatval($row_sum_iuran['total']) : 0.0;

        // Cek sum dari tabel anggota kolom iuran_aniv
        $stmt_sum_anggota = $conn->query("SELECT SUM(iuran_aniv) as total FROM anggota WHERE iuran_aniv > 0");
        $row_sum_anggota = $stmt_sum_anggota ? $stmt_sum_anggota->fetch(PDO::FETCH_ASSOC) : null;
        $sum_anggota = ($row_sum_anggota && $row_sum_anggota['total'] !== null) ? floatval($row_sum_anggota['total']) : 0.0;

        if ($sum_iuran > 0) {
            $total_pemasukan = $sum_iuran;
        } elseif ($sum_anggota > 0) {
            $total_pemasukan = $sum_anggota;
        } elseif ($row_master) {
            $total_pemasukan = floatval($row_master['total_akumulasi_masuk']);
        }

        // 2. Hitung Total Pengeluaran Anniversary
        $total_pengeluaran = 0.0;
        $stmt_pengeluaran = $conn->query("SELECT SUM(nominal) as total FROM pengeluaran WHERE LOWER(jenis_kas) IN ('kas aniv', 'kas anniversary', 'aniv', 'anniversary')");
        $row_pengeluaran = $stmt_pengeluaran ? $stmt_pengeluaran->fetch(PDO::FETCH_ASSOC) : null;
        if ($row_pengeluaran && $row_pengeluaran['total'] !== null) {
            $total_pengeluaran = floatval($row_pengeluaran['total']);
        } elseif ($row_master) {
            $total_pengeluaran = floatval($row_master['total_akumulasi_keluar']);
        }

        // 3. Hitung Sisa Kas Aniv
        $raw_sisa = $total_pemasukan - $total_pengeluaran;
        $sisa_kas = max(0.0, $raw_sisa);

        // 4. Ambil Daftar Transaksi / Pembayar Anniversary
        // Coba dari tabel iuran_anniversary JOIN anggota
        $items = array();
        $stmt_list = $conn->query("
            SELECT ia.id, ia.anggota_id, ia.nominal, ia.tanggal, ia.keterangan, a.nama, a.nra 
            FROM iuran_anniversary ia
            LEFT JOIN anggota a ON ia.anggota_id = a.id
            ORDER BY ia.tanggal DESC, ia.id DESC
        ");
        $rows_list = $stmt_list ? $stmt_list->fetchAll(PDO::FETCH_ASSOC) : array();

        if (!empty($rows_list)) {
            foreach ($rows_list as $r) {
                $nama = !empty($r['nama']) ? $r['nama'] : "Anggota #" . $r['anggota_id'];
                $nominal = floatval($r['nominal']);
                $items[] = array(
                    "id" => (int)$r['id'],
                    "nama" => $nama,
                    "nra" => isset($r['nra']) ? (string)$r['nra'] : "-",
                    "jumlah_bayar" => $nominal,
                    "nominal" => $nominal,
                    "tanggal" => isset($r['tanggal']) ? (string)$r['tanggal'] : date('Y-m-d'),
                    "keterangan" => isset($r['keterangan']) ? (string)$r['keterangan'] : "Iuran Anniversary"
                );
            }
        } else {
            // Fallback dari anggota yang memiliki iuran_aniv > 0
            $stmt_ang = $conn->query("SELECT id, nama, nra, iuran_aniv, tgl_gabung FROM anggota WHERE iuran_aniv > 0 ORDER BY id DESC");
            $rows_ang = $stmt_ang ? $stmt_ang->fetchAll(PDO::FETCH_ASSOC) : array();
            foreach ($rows_ang as $r) {
                $nominal = floatval($r['iuran_aniv']);
                $items[] = array(
                    "id" => (int)$r['id'],
                    "nama" => (string)$r['nama'],
                    "nra" => isset($r['nra']) ? (string)$r['nra'] : "-",
                    "jumlah_bayar" => $nominal,
                    "nominal" => $nominal,
                    "tanggal" => isset($r['tgl_gabung']) ? (string)$r['tgl_gabung'] : date('Y-m-d'),
                    "keterangan" => "Iuran Anniversary"
                );
            }
        }

        $total_transaksi = count($items);

        $response = array(
            "status" => "success",
            "total_pemasukan" => $total_pemasukan,
            "total_pengeluaran" => $total_pengeluaran,
            "sisa_kas" => $sisa_kas,
            "total_transaksi" => $total_transaksi,
            "data" => $items
        );

        echo json_encode($response);
    } catch (Throwable $e) {
        echo json_encode(array(
            "status" => "error",
            "message" => "Database error: " . $e->getMessage(),
            "total_pemasukan" => 0.0,
            "total_pengeluaran" => 0.0,
            "sisa_kas" => 0.0,
            "total_transaksi" => 0,
            "data" => array()
        ));
    }
} else {
    http_response_code(405);
    echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
}
?>
