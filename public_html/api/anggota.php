<?php
// anggota.php
include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents("php://input"));

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            $stmt = $conn->prepare("SELECT * FROM anggota WHERE id = ?");
            $stmt->execute(array($_GET['id']));
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($row) {
                $row['id'] = (int)$row['id'];
                $row['uang_kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
                $row['iuran_aniv'] = (int)(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : 0);
                $row['kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0); // Aliasing untuk kompatibilitas UI
                $row['total_kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
                $row['total_aniv'] = (int)(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : 0);
                $row['status'] = isset($row['status']) ? $row['status'] : ((isset($row['statusAktif']) && !$row['statusAktif']) ? 'Nonaktif' : 'Aktif');
                $result = $row;
            } else {
                $result = null;
            }
        } else {
            $stmt = $conn->query("SELECT * FROM anggota ORDER BY nama ASC");
            $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
            $result = array();
            foreach ($rows as $row) {
                $item = $row;
                $item['id'] = (int)$row['id'];
                $item['nama'] = isset($row['nama']) ? $row['nama'] : '';
                $item['nra'] = isset($row['nra']) ? $row['nra'] : '';
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

            $query = "INSERT INTO anggota (nama, role, no_wa, alamat, tgl_gabung, uang_kas, iuran_aniv, total_cicilan, harga_barang, sisa_cicilan, cicilan_per_bulan, nra, statusAktif, username, password, foto, totalTagihan, lamaCicilan) 
                      VALUES (:nama, :role, :no_wa, :alamat, :tgl_gabung, :uang_kas, :iuran_aniv, :total_cicilan, :harga_barang, :sisa_cicilan, :cicilan_per_bulan, :nra, :statusAktif, :username, :password, :foto, :totalTagihan, :lamaCicilan)";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':nama' => $data->nama,
                ':role' => isset($data->role) ? $data->role : 'Anggota',
                ':no_wa' => isset($data->no_wa) ? $data->no_wa : '',
                ':alamat' => isset($data->alamat) ? $data->alamat : '',
                ':tgl_gabung' => isset($data->tgl_gabung) ? $data->tgl_gabung : '',
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
        if (!empty($data->id)) {
            $hargaBarangVal = floatval($data->harga_barang ?? $data->hargaBarang ?? 0);
            $totalCicilanVal = floatval($data->total_cicilan ?? $data->totalCicilan ?? 0);
            $sisaCicilanVal = floatval($data->sisa_cicilan ?? $data->sisaCicilan ?? max(0.0, $hargaBarangVal - $totalCicilanVal));
            $cicilanPerBulanVal = floatval($data->cicilan_per_bulan ?? $data->cicilanPerBulan ?? 0);
            $lamaCicilanVal = intval($data->lamaCicilan ?? $data->lama_cicilan ?? 0);
            $totalTagihanVal = floatval($data->totalTagihan ?? $data->total_tagihan ?? $hargaBarangVal);
            $namaBarangVal = isset($data->namaBarang) ? $data->namaBarang : (isset($data->nama_barang) ? $data->nama_barang : '');

            $query = "UPDATE anggota SET nama=:nama, role=:role, no_wa=:no_wa, alamat=:alamat, tgl_gabung=:tgl_gabung, 
                      uang_kas=:uang_kas, iuran_aniv=:iuran_aniv, total_cicilan=:total_cicilan, harga_barang=:harga_barang, 
                      sisa_cicilan=:sisa_cicilan, cicilan_per_bulan=:cicilan_per_bulan, nra=:nra, statusAktif=:statusAktif,
                      username=:username, password=:password, foto=:foto, totalTagihan=:totalTagihan, lamaCicilan=:lamaCicilan 
                      WHERE id=:id";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':nama' => $data->nama,
                ':role' => $data->role,
                ':no_wa' => $data->no_wa,
                ':alamat' => $data->alamat,
                ':tgl_gabung' => $data->tgl_gabung,
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
                ':lamaCicilan' => $lamaCicilanVal,
                ':id' => $data->id
            ));

            try {
                $stmtCamel = $conn->prepare("UPDATE anggota SET hargaBarang = ?, sisaCicilan = ?, namaBarang = ?, lamaCicilan = ?, totalTagihan = ? WHERE id = ?");
                $stmtCamel->execute(array($hargaBarangVal, $sisaCicilanVal, $namaBarangVal, $lamaCicilanVal, $totalTagihanVal, $data->id));
            } catch (Exception $e) {}

            echo json_encode(array("status" => "success", "message" => "Data anggota berhasil diupdate"));
        } else {
            echo json_encode(array("status" => "error", "message" => "ID anggota tidak ditemukan"));
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
