<?php
// anggota.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once 'config.php';
include_once 'sync_helper.php';

// Pastikan kolom nomor_urut ada di tabel anggota
try {
    $conn->exec("ALTER TABLE anggota ADD COLUMN nomor_urut VARCHAR(50) DEFAULT ''");
} catch (Exception $e) {}

$method = $_SERVER['REQUEST_METHOD'];
$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);
if (!$data) {
    $data = (object)$_POST;
}

// Cek apakah ada override method di POST
if ($method === 'POST') {
    if (isset($data->_method) && strtoupper($data->_method) === 'PUT') {
        $method = 'PUT';
    } elseif (isset($data->action) && strtolower($data->action) === 'update') {
        $method = 'PUT';
    } elseif (isset($_POST['_method']) && strtoupper($_POST['_method']) === 'PUT') {
        $method = 'PUT';
    } elseif (isset($_POST['action']) && strtolower($_POST['action']) === 'update') {
        $method = 'PUT';
    }
}

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT id, nama, role, no_wa, alamat, tgl_gabung, uang_kas, iuran_aniv, total_cicilan, harga_barang, sisa_cicilan, cicilan_per_bulan, nra, nomor_urut, statusAktif, status, username, foto, totalTagihan, lamaCicilan, namaBarang, hargaBarang, totalCicilan, sisaCicilan, cicilanPerBulan, uangKas, iuranAniv, created_at, status_verifikasi FROM anggota WHERE id = ?");
            $stmt->execute(array($_GET['id']));
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if (!$row) {
                $stmt = $conn->prepare("SELECT * FROM anggota WHERE id = ?");
                $stmt->execute(array($_GET['id']));
                $row = $stmt->fetch(PDO::FETCH_ASSOC);
            }
            
            if ($row) {
                unset($row['password'], $row['password_hash'], $row['token'], $row['secret']);
                $row['id'] = (int)$row['id'];
                $row['uang_kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
                $row['iuran_aniv'] = (int)(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : 0);
                $row['kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
                $row['total_kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
                $row['total_aniv'] = (int)(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : 0);
                $row['nomor_urut'] = isset($row['nomor_urut']) ? $row['nomor_urut'] : '';
                $row['tanggal_bergabung'] = isset($row['tgl_gabung']) ? $row['tgl_gabung'] : '';
                $row['status'] = isset($row['status']) ? $row['status'] : ((isset($row['statusAktif']) && !$row['statusAktif']) ? 'Nonaktif' : 'Aktif');
                $result = $row;
            } else {
                $result = null;
            }
        } else {
            try {
                $stmt = $conn->query("SELECT id, nama, role, no_wa, alamat, tgl_gabung, uang_kas, iuran_aniv, total_cicilan, harga_barang, sisa_cicilan, cicilan_per_bulan, nra, nomor_urut, statusAktif, status, username, foto, totalTagihan, lamaCicilan, namaBarang, hargaBarang, totalCicilan, sisaCicilan, cicilanPerBulan, uangKas, iuranAniv, created_at, status_verifikasi FROM anggota ORDER BY nama ASC");
                $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
            } catch (Exception $e) {
                $stmt = $conn->query("SELECT * FROM anggota ORDER BY nama ASC");
                $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
            }
            $result = array();
            foreach ($rows as $row) {
                $item = $row;
                unset($item['password'], $item['password_hash'], $item['token'], $item['secret']);
                $item['id'] = (int)$row['id'];
                $item['nama'] = isset($row['nama']) ? $row['nama'] : '';
                $item['nra'] = isset($row['nra']) ? $row['nra'] : '';
                $item['nomor_urut'] = isset($row['nomor_urut']) ? $row['nomor_urut'] : '';
                $item['tanggal_bergabung'] = isset($row['tgl_gabung']) ? $row['tgl_gabung'] : '';
                $item['status'] = isset($row['status']) ? $row['status'] : ((isset($row['statusAktif']) && !$row['statusAktif']) ? 'Nonaktif' : 'Aktif');
                $item['no_wa'] = isset($row['no_wa']) ? $row['no_wa'] : '';
                $item['uang_kas'] = (float)(isset($row['uang_kas']) ? $row['uang_kas'] : (isset($row['uangKas']) ? $row['uangKas'] : 0));
                $item['iuran_aniv'] = (float)(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : (isset($row['iuranAniv']) ? $row['iuranAniv'] : 0));
                $item['kas'] = $item['uang_kas'];
                $item['total_kas'] = $item['uang_kas'];
                $item['total_aniv'] = $item['iuran_aniv'];
                
                $hBarang = floatval(isset($row['harga_barang']) ? $row['harga_barang'] : (isset($row['hargaBarang']) ? $row['hargaBarang'] : 0));
                $tCicilan = floatval(isset($row['total_cicilan']) ? $row['total_cicilan'] : (isset($row['totalCicilan']) ? $row['totalCicilan'] : 0));
                $rawSisa = isset($row['sisa_cicilan']) ? $row['sisa_cicilan'] : (isset($row['sisaCicilan']) ? $row['sisaCicilan'] : 0);
                $sCicilan = floatval($rawSisa > 0 ? $rawSisa : max(0.0, $hBarang - $tCicilan));
                
                $item['harga_barang'] = $hBarang;
                $item['hargaBarang'] = $hBarang;
                $item['total_cicilan'] = $tCicilan;
                $item['totalCicilan'] = $tCicilan;
                $item['sisa_cicilan'] = $sCicilan;
                $item['sisaCicilan'] = $sCicilan;
                $item['cicilan_per_bulan'] = floatval(isset($row['cicilan_per_bulan']) ? $row['cicilan_per_bulan'] : 0);
                $item['cicilanPerBulan'] = $item['cicilan_per_bulan'];
                $item['lamaCicilan'] = intval(isset($row['lamaCicilan']) ? $row['lamaCicilan'] : (isset($row['lama_cicilan']) ? $row['lama_cicilan'] : 0));
                $item['namaBarang'] = isset($row['namaBarang']) ? $row['namaBarang'] : (isset($row['nama_barang']) ? $row['nama_barang'] : '');
                $item['nama_barang'] = $item['namaBarang'];
                $item['totalTagihan'] = floatval(isset($row['totalTagihan']) ? $row['totalTagihan'] : $hBarang);
                $result[] = $item;
            }
        }
        echo json_encode(array("status" => "success", "data" => $result));
        break;

    case 'POST':
        if (!empty($data->nama)) {
            $hargaBarangVal = floatval($data->harga_barang ?? $data->hargaBarang ?? 0);
            $totalCicilanVal = floatval($data->total_cicilan ?? $data->totalCicilan ?? 0);
            $sisaCicilanVal = floatval($data->sisa_cicilan ?? $data->sisaCicilan ?? max(0.0, $hargaBarangVal - $totalCicilanVal));
            $cicilanPerBulanVal = floatval($data->cicilan_per_bulan ?? $data->cicilanPerBulan ?? 0);
            $lamaCicilanVal = intval($data->lamaCicilan ?? $data->lama_cicilan ?? 0);
            $totalTagihanVal = floatval($data->totalTagihan ?? $data->total_tagihan ?? $hargaBarangVal);
            $namaBarangVal = isset($data->namaBarang) ? $data->namaBarang : (isset($data->nama_barang) ? $data->nama_barang : '');

            $query = "INSERT INTO anggota (nama, role, no_wa, alamat, tgl_gabung, nomor_urut, uang_kas, iuran_aniv, total_cicilan, harga_barang, sisa_cicilan, cicilan_per_bulan, nra, statusAktif, username, password, foto, totalTagihan, lamaCicilan) 
                      VALUES (:nama, :role, :no_wa, :alamat, :tgl_gabung, :nomor_urut, :uang_kas, :iuran_aniv, :total_cicilan, :harga_barang, :sisa_cicilan, :cicilan_per_bulan, :nra, :statusAktif, :username, :password, :foto, :totalTagihan, :lamaCicilan)";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':nama' => $data->nama,
                ':role' => isset($data->role) ? $data->role : 'Anggota',
                ':no_wa' => isset($data->no_wa) ? $data->no_wa : (isset($data->nomor_telepon) ? $data->nomor_telepon : ''),
                ':alamat' => isset($data->alamat) ? $data->alamat : '',
                ':tgl_gabung' => isset($data->tgl_gabung) ? $data->tgl_gabung : (isset($data->tanggal_bergabung) ? $data->tanggal_bergabung : date('Y-m-d')),
                ':nomor_urut' => isset($data->nomor_urut) ? $data->nomor_urut : (isset($data->no_urut) ? $data->no_urut : ''),
                ':uang_kas' => isset($data->uang_kas) ? $data->uang_kas : (isset($data->uangKas) ? $data->uangKas : 0),
                ':iuran_aniv' => isset($data->iuran_aniv) ? $data->iuran_aniv : (isset($data->iuranAniv) ? $data->iuranAniv : 0),
                ':total_cicilan' => $totalCicilanVal,
                ':harga_barang' => $hargaBarangVal,
                ':sisa_cicilan' => $sisaCicilanVal,
                ':cicilan_per_bulan' => $cicilanPerBulanVal,
                ':nra' => isset($data->nra) ? $data->nra : '',
                ':statusAktif' => isset($data->statusAktif) ? ($data->statusAktif ? 1 : 0) : 1,
                ':username' => isset($data->username) ? $data->username : '',
                ':password' => isset($data->password) ? $data->password : '',
                ':foto' => isset($data->foto) ? $data->foto : null,
                ':totalTagihan' => $totalTagihanVal,
                ':lamaCicilan' => $lamaCicilanVal
            ));
            $insertedId = $conn->lastInsertId();

            try {
                $stmtCamel = $conn->prepare("UPDATE anggota SET hargaBarang = ?, sisaCicilan = ?, namaBarang = ?, lamaCicilan = ?, totalTagihan = ? WHERE id = ?");
                $stmtCamel->execute(array($hargaBarangVal, $sisaCicilanVal, $namaBarangVal, $lamaCicilanVal, $totalTagihanVal, $insertedId));
            } catch (Exception $e) {}

            echo json_encode(array("status" => "success", "message" => "Anggota berhasil ditambahkan", "id" => $insertedId));
        } else {
            echo json_encode(array("status" => "error", "message" => "Data nama tidak boleh kosong"));
        }
        break;

    case 'PUT':
        // 1. Validasi Hak Akses / Role Pemeriksaan Server-Side
        $user_role = '';
        if (isset($data->role_login)) $user_role = trim($data->role_login);
        elseif (isset($data->user_role)) $user_role = trim($data->user_role);
        elseif (isset($data->login_role)) $user_role = trim($data->login_role);
        elseif (isset($data->current_user_role)) $user_role = trim($data->current_user_role);
        elseif (isset($_POST['role_login'])) $user_role = trim($_POST['role_login']);
        elseif (isset($_POST['user_role'])) $user_role = trim($_POST['user_role']);
        elseif (isset($_GET['role_login'])) $user_role = trim($_GET['role_login']);
        elseif (isset($_GET['user_role'])) $user_role = trim($_GET['user_role']);
        elseif (isset($_GET['role'])) $user_role = trim($_GET['role']);

        $role_upper = strtoupper($user_role);
        if (!in_array($role_upper, array('ADMIN', 'BENDAHARA', 'DEVELOPER'))) {
            http_response_code(403);
            echo json_encode(array(
                "status" => "error",
                "success" => false,
                "message" => "Anda tidak memiliki izin untuk mengubah data anggota."
            ));
            exit();
        }

        // 2. Validasi ID Anggota (Single Source of Truth)
        $targetId = isset($data->id) ? intval($data->id) : (isset($_GET['id']) ? intval($_GET['id']) : 0);
        if ($targetId <= 0) {
            http_response_code(400);
            echo json_encode(array("status" => "error", "success" => false, "message" => "ID anggota tidak ditemukan atau tidak valid"));
            exit();
        }

        // 3. Ambil data lama anggota dari database berdasarkan ID
        $stmtExisting = $conn->prepare("SELECT * FROM anggota WHERE id = ? LIMIT 1");
        $stmtExisting->execute(array($targetId));
        $existingMember = $stmtExisting->fetch(PDO::FETCH_ASSOC);

        if (!$existingMember) {
            http_response_code(404);
            echo json_encode(array("status" => "error", "success" => false, "message" => "Data anggota tidak ditemukan di server"));
            exit();
        }

        // 4. Validasi Field yang Diizinkan Diedit
        $nama = isset($data->nama) && trim($data->nama) !== '' ? trim($data->nama) : $existingMember['nama'];
        if (empty($nama)) {
            http_response_code(400);
            echo json_encode(array("status" => "error", "success" => false, "message" => "Nama anggota tidak boleh kosong"));
            exit();
        }

        $nra = isset($data->nra) && trim($data->nra) !== '' ? trim($data->nra) : ($existingMember['nra'] ?? '');
        $alamat = isset($data->alamat) ? trim($data->alamat) : ($existingMember['alamat'] ?? '');
        $no_wa = isset($data->no_wa) ? trim($data->no_wa) : (isset($data->nomor_telepon) ? trim($data->nomor_telepon) : ($existingMember['no_wa'] ?? ''));
        $tgl_gabung = isset($data->tgl_gabung) && trim($data->tgl_gabung) !== '' ? trim($data->tgl_gabung) : (isset($data->tanggal_bergabung) && trim($data->tanggal_bergabung) !== '' ? trim($data->tanggal_bergabung) : ($existingMember['tgl_gabung'] ?? ''));
        $nomor_urut = isset($data->nomor_urut) ? trim($data->nomor_urut) : (isset($data->no_urut) ? trim($data->no_urut) : ($existingMember['nomor_urut'] ?? ''));
        $statusAktif = isset($data->statusAktif) ? ($data->statusAktif ? 1 : 0) : ($existingMember['statusAktif'] ?? 1);
        $foto = isset($data->foto) && !empty($data->foto) ? $data->foto : ($existingMember['foto'] ?? null);

        // 5. Eksekusi UPDATE HANYA PADA DATA PRIBADI (Password, Role, Kas, Cicilan, Aniv TIDAK BOLEH DIUBAH)
        $updateQuery = "UPDATE anggota SET 
                            nama = :nama, 
                            nra = :nra, 
                            alamat = :alamat, 
                            no_wa = :no_wa, 
                            tgl_gabung = :tgl_gabung, 
                            nomor_urut = :nomor_urut, 
                            statusAktif = :statusAktif, 
                            foto = :foto 
                        WHERE id = :id";
        
        $stmtUpdate = $conn->prepare($updateQuery);
        $execResult = $stmtUpdate->execute(array(
            ':nama'        => $nama,
            ':nra'         => $nra,
            ':alamat'      => $alamat,
            ':no_wa'       => $no_wa,
            ':tgl_gabung'  => $tgl_gabung,
            ':nomor_urut'  => $nomor_urut,
            ':statusAktif' => $statusAktif,
            ':foto'        => $foto,
            ':id'          => $targetId
        ));

        if ($execResult) {
            echo json_encode(array(
                "status"  => "success",
                "success" => true,
                "message" => "Data anggota berhasil diperbarui.",
                "data"    => array(
                    "id"                => (int)$targetId,
                    "nama"              => $nama,
                    "nra"               => $nra,
                    "alamat"            => $alamat,
                    "no_wa"             => $no_wa,
                    "nomor_urut"        => $nomor_urut,
                    "tgl_gabung"        => $tgl_gabung,
                    "tanggal_bergabung" => $tgl_gabung,
                    "statusAktif"       => $statusAktif
                )
            ));
        } else {
            http_response_code(500);
            echo json_encode(array("status" => "error", "success" => false, "message" => "Gagal memperbarui data anggota di database"));
        }
        break;

    case 'DELETE':
        if (!empty($data->id)) {
            $stmt = $conn->prepare("DELETE FROM anggota WHERE id = ?");
            $stmt->execute(array($data->id));
            echo json_encode(array("status" => "success", "message" => "Data anggota berhasil dihapus"));
        } else {
            echo json_encode(array("status" => "error", "message" => "ID anggota tidak ditemukan"));
        }
        break;

    default:
        http_response_code(405);
        echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
        break;
}
?>
