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
        
        echo json_encode(array(
            "status" => "success",
            "community_name" => isset($result['community_name']) ? $result['community_name'] : 'NEBO Sukabumi',
            "community_logo" => "https://nebosukabumi.net/images/logo_komunitas.png",
            "community_banner" => "https://nebosukabumi.net/images/dashboard_banner.jpg",
            "data" => $result
        ));
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
            $stmt->execute(array(
                ':community_name' => $data->community_name,
                ':community_slogan' => isset($data->community_slogan) ? $data->community_slogan : '',
                ':community_motto' => isset($data->community_motto) ? $data->community_motto : '',
                ':community_logo' => isset($data->community_logo) ? $data->community_logo : '',
                ':community_banner' => isset($data->community_banner) ? $data->community_banner : '',
                ':community_splash' => isset($data->community_splash) ? $data->community_splash : '',
                ':community_address' => isset($data->community_address) ? $data->community_address : '',
                ':community_phone' => isset($data->community_phone) ? $data->community_phone : '',
                ':community_email' => isset($data->community_email) ? $data->community_email : '',
                ':community_website' => isset($data->community_website) ? $data->community_website : '',
                ':community_facebook' => isset($data->community_facebook) ? $data->community_facebook : '',
                ':community_instagram' => isset($data->community_instagram) ? $data->community_instagram : '',
                ':community_youtube' => isset($data->community_youtube) ? $data->community_youtube : '',
                ':target_aniv' => isset($data->target_aniv) ? $data->target_aniv : 0,
                ':target_kas' => isset($data->target_kas) ? $data->target_kas : 0,
                ':login_background' => isset($data->login_background) ? $data->login_background : '',
                ':profile_banner' => isset($data->profile_banner) ? $data->profile_banner : '',
                ':updated_at' => isset($data->updated_at) ? $data->updated_at : 0,
                ':updated_by' => isset($data->updated_by) ? $data->updated_by : ''
            ));
            echo json_encode(array("status" => "success", "message" => "Pengaturan berhasil diupdate"));
        } else {
            echo json_encode(array("status" => "error", "message" => "Nama komunitas tidak boleh kosong"));
        }
        break;
    default:
        http_response_code(405);
        echo json_encode(array("status" => "error", "message" => "Method Not Allowed"));
        break;
}
?>
