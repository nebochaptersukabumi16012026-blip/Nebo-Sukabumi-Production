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
                $item['uang_kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
                $item['iuran_aniv'] = (int)(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : 0);
                $item['kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0); // Aliasing untuk kompatibilitas UI
                $item['total_kas'] = (int)(isset($row['uang_kas']) ? $row['uang_kas'] : 0);
                $item['total_aniv'] = (int)(isset($row['iuran_aniv']) ? $row['iuran_aniv'] : 0);
                $result[] = $item;
            }
        }
        echo json_encode(array("status" => "success", "data" => $result));
        break;
    case 'POST':
        if (!empty($data->nama)) {
            $query = "INSERT INTO anggota (nama, role, no_wa, alamat, tgl_gabung, uang_kas, iuran_aniv, total_cicilan, harga_barang, sisa_cicilan, cicilan_per_bulan, nra, statusAktif, username, password, foto, totalTagihan, lamaCicilan) 
                      VALUES (:nama, :role, :no_wa, :alamat, :tgl_gabung, :uang_kas, :iuran_aniv, :total_cicilan, :harga_barang, :sisa_cicilan, :cicilan_per_bulan, :nra, :statusAktif, :username, :password, :foto, :totalTagihan, :lamaCicilan)";
            $stmt = $conn->prepare($query);
            $stmt->execute(array(
                ':nama' => $data->nama,
                ':role' => isset($data->role) ? $data->role : 'Anggota',
                ':no_wa' => isset($data->no_wa) ? $data->no_wa : '',
                ':alamat' => isset($data->alamat) ? $data->alamat : '',
                ':tgl_gabung' => isset($data->tgl_gabung) ? $data->tgl_gabung : '',
                ':uang_kas' => isset($data->uang_kas) ? $data->uang_kas : 0,
                ':iuran_aniv' => isset($data->iuran_aniv) ? $data->iuran_aniv : 0,
                ':total_cicilan' => isset($data->total_cicilan) ? $data->total_cicilan : 0,
                ':harga_barang' => isset($data->harga_barang) ? $data->harga_barang : 0,
                ':sisa_cicilan' => isset($data->sisa_cicilan) ? $data->sisa_cicilan : 0,
                ':cicilan_per_bulan' => isset($data->cicilan_per_bulan) ? $data->cicilan_per_bulan : 0,
                ':nra' => isset($data->nra) ? $data->nra : '',
                ':statusAktif' => isset($data->statusAktif) ? ($data->statusAktif ? 1 : 0) : 1,
                ':username' => isset($data->username) ? $data->username : '',
                ':password' => isset($data->password) ? $data->password : '',
                ':foto' => isset($data->foto) ? $data->foto : null,
                ':totalTagihan' => isset($data->totalTagihan) ? $data->totalTagihan : 0,
                ':lamaCicilan' => isset($data->lamaCicilan) ? $data->lamaCicilan : 0
            ));
            echo json_encode(array("status" => "success", "message" => "Anggota berhasil ditambahkan", "id" => $conn->lastInsertId()));
        } else {
            echo json_encode(array("status" => "error", "message" => "Data nama tidak boleh kosong"));
        }
        break;
    case 'PUT':
        if (!empty($data->id)) {
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
                ':uang_kas' => $data->uang_kas,
                ':iuran_aniv' => $data->iuran_aniv,
                ':total_cicilan' => $data->total_cicilan,
                ':harga_barang' => $data->harga_barang,
                ':sisa_cicilan' => $data->sisa_cicilan,
                ':cicilan_per_bulan' => $data->cicilan_per_bulan,
                ':nra' => isset($data->nra) ? $data->nra : '',
                ':statusAktif' => isset($data->statusAktif) ? ($data->statusAktif ? 1 : 0) : 1,
                ':username' => isset($data->username) ? $data->username : '',
                ':password' => isset($data->password) ? $data->password : '',
                ':foto' => isset($data->foto) ? $data->foto : null,
                ':totalTagihan' => isset($data->totalTagihan) ? $data->totalTagihan : 0,
                ':lamaCicilan' => isset($data->lamaCicilan) ? $data->lamaCicilan : 0,
                ':id' => $data->id
            ));
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
