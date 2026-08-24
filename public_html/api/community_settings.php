<?php
// community_settings.php
include_once 'config.php';
$method = $_SERVER['REQUEST_METHOD'];
$data = json_decode(file_get_contents("php://input"));

switch ($method) {
    case 'GET':
        $stmt = $conn->query("SELECT * FROM community_settings LIMIT 1");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$result) {
            $conn->exec("INSERT INTO community_settings (community_name) VALUES ('NEBO SUKABUMI')");
            $stmt = $conn->query("SELECT * FROM community_settings LIMIT 1");
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        
        if ($result) {
            $result['id'] = intval($result['id']);
            $result['updated_at'] = intval($result['updated_at']);
        }
        
        echo json_encode([
            "status" => "success",
            "community_name" => $result['community_name'] ?? 'NEBO Sukabumi',
            "community_logo" => "https://nebosukabumi.net/images/logo_komunitas.png",
            "community_banner" => "https://nebosukabumi.net/images/dashboard_banner.jpg",
            "data" => $result
        ]);
        break;
    case 'POST':
    case 'PUT':
        if (!empty($data->community_name)) {
            $query = "UPDATE community_settings SET 
                community_name = :community_name,
                community_slogan = :community_slogan,
                community_motto = :community_motto,
                community_logo = :community_logo,
                community_banner = :community_banner,
                community_splash = :community_splash,
                community_address = :community_address,
                community_phone = :community_phone,
                community_email = :community_email,
                community_website = :community_website,
                community_facebook = :community_facebook,
                community_instagram = :community_instagram,
                community_youtube = :community_youtube,
                target_aniv = :target_aniv,
                target_kas = :target_kas,
                login_background = :login_background,
                profile_banner = :profile_banner,
                updated_at = :updated_at,
                updated_by = :updated_by
                WHERE id = 1";
            $stmt = $conn->prepare($query);
            $stmt->execute([
                ':community_name' => $data->community_name,
                ':community_slogan' => $data->community_slogan ?? '',
                ':community_motto' => $data->community_motto ?? '',
                ':community_logo' => $data->community_logo ?? '',
                ':community_banner' => $data->community_banner ?? '',
                ':community_splash' => $data->community_splash ?? '',
                ':community_address' => $data->community_address ?? '',
                ':community_phone' => $data->community_phone ?? '',
                ':community_email' => $data->community_email ?? '',
                ':community_website' => $data->community_website ?? '',
                ':community_facebook' => $data->community_facebook ?? '',
                ':community_instagram' => $data->community_instagram ?? '',
                ':community_youtube' => $data->community_youtube ?? '',
                ':target_aniv' => $data->target_aniv ?? 0,
                ':target_kas' => $data->target_kas ?? 0,
                ':login_background' => $data->login_background ?? '',
                ':profile_banner' => $data->profile_banner ?? '',
                ':updated_at' => $data->updated_at ?? 0,
                ':updated_by' => $data->updated_by ?? ''
            ]);
            echo json_encode(["status" => "success", "message" => "Pengaturan berhasil diupdate"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Nama komunitas tidak boleh kosong"]);
        }
        break;
    default:
        http_response_code(405);
        echo json_encode(["status" => "error", "message" => "Method Not Allowed"]);
        break;
}
?>
