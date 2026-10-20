<?php
// workspace/refactored_api/modules/settings.php
// Modul Terpadu Pengaturan Komunitas & Upload Aset (Logo, Banner, Splash, Background)

require_once __DIR__ . '/../core/config.php';

$method = $_SERVER['REQUEST_METHOD'];
$input  = getRequestBody();
$action = $input['action'] ?? '';

if (!empty($action) && strpos($action, 'upload_') === 0) {
    handleUploadAsset($pdo, substr($action, 7));
    exit();
}

switch ($method) {
    case 'GET':
        try {
            $stmt = $pdo->query("SELECT * FROM community_settings LIMIT 1");
            $result = $stmt->fetch(PDO::FETCH_ASSOC);

            if (!$result) {
                $pdo->exec("INSERT INTO community_settings (community_name) VALUES ('NEBO SUKABUMI')");
                $stmt = $pdo->query("SELECT * FROM community_settings LIMIT 1");
                $result = $stmt->fetch(PDO::FETCH_ASSOC);
            }

            if ($result) {
                $result['id'] = intval($result['id']);
                $result['updated_at'] = intval($result['updated_at']);
            }

            sendResponse(true, "Pengaturan komunitas berhasil dimuat", $result, [
                "community_name"   => $result['community_name'] ?? 'NEBO SUKABUMI',
                "community_logo"   => $result['community_logo'] ?? 'https://nebosukabumi.net/images/logo_komunitas.png',
                "community_banner" => $result['community_banner'] ?? 'https://nebosukabumi.net/images/dashboard_banner.jpg'
            ]);
        } catch (Exception $e) {
            sendResponse(false, "Gagal memuat pengaturan: " . $e->getMessage(), null, [], 500);
        }
        break;

    case 'POST':
    case 'PUT':
        verifyRole(['ADMIN', 'DEVELOPER']);

        $name = trim($input['community_name'] ?? '');
        if (empty($name)) {
            sendResponse(false, "Nama komunitas tidak boleh kosong", null, [], 400);
        }

        try {
            $stmt = $pdo->prepare("
                UPDATE community_settings SET 
                    community_name = :name,
                    community_slogan = :slogan,
                    community_motto = :motto,
                    community_logo = :logo,
                    community_banner = :banner,
                    community_splash = :splash,
                    community_address = :address,
                    community_phone = :phone,
                    community_email = :email,
                    community_website = :website,
                    community_facebook = :fb,
                    community_instagram = :ig,
                    community_youtube = :yt,
                    target_aniv = :target_aniv,
                    target_kas = :target_kas,
                    login_background = :login_bg,
                    profile_banner = :prof_banner,
                    updated_at = :updated_at,
                    updated_by = :updated_by
                WHERE id = 1
            ");
            $stmt->execute([
                ':name'        => $name,
                ':slogan'      => $input['community_slogan'] ?? '',
                ':motto'       => $input['community_motto'] ?? '',
                ':logo'        => $input['community_logo'] ?? '',
                ':banner'      => $input['community_banner'] ?? '',
                ':splash'      => $input['community_splash'] ?? '',
                ':address'     => $input['community_address'] ?? '',
                ':phone'       => $input['community_phone'] ?? '',
                ':email'       => $input['community_email'] ?? '',
                ':website'     => $input['community_website'] ?? '',
                ':fb'          => $input['community_facebook'] ?? '',
                ':ig'          => $input['community_instagram'] ?? '',
                ':yt'          => $input['community_youtube'] ?? '',
                ':target_aniv' => floatval($input['target_aniv'] ?? 0),
                ':target_kas'  => floatval($input['target_kas'] ?? 0),
                ':login_bg'    => $input['login_background'] ?? '',
                ':prof_banner' => $input['profile_banner'] ?? '',
                ':updated_at'  => time() * 1000,
                ':updated_by'  => $input['updated_by'] ?? 'Admin'
            ]);

            sendResponse(true, "Pengaturan komunitas berhasil diperbarui");
        } catch (Exception $e) {
            sendResponse(false, "Gagal menyimpan pengaturan: " . $e->getMessage(), null, [], 500);
        }
        break;

    default:
        sendResponse(false, "Method not allowed", null, [], 405);
        break;
}

function handleUploadAsset($pdo, $type) {
    verifyRole(['ADMIN', 'DEVELOPER']);

    if (!isset($_FILES['file'])) {
        sendResponse(false, "Tidak ada file yang diunggah", null, [], 400);
    }

    $uploadMap = [
        'logo'           => ['file' => 'community_logo.png',          'col' => 'community_logo'],
        'banner'         => ['file' => 'dashboard_banner.jpg',        'col' => 'community_banner'],
        'splash'         => ['file' => 'community_splash.png',        'col' => 'community_splash'],
        'login_bg'       => ['file' => 'community_login_bg.jpg',      'col' => 'login_background'],
        'profile_banner' => ['file' => 'community_profile_banner.jpg','col' => 'profile_banner']
    ];

    if (!isset($uploadMap[$type])) {
        sendResponse(false, "Tipe aset upload tidak dikenal", null, [], 400);
    }

    $targetDir = __DIR__ . "/../../uploads/";
    if (!file_exists($targetDir)) {
        @mkdir($targetDir, 0755, true);
    }

    $fileName   = $uploadMap[$type]['file'];
    $targetFile = $targetDir . $fileName;

    if (move_uploaded_file($_FILES["file"]["tmp_name"], $targetFile)) {
        $url = "https://nebosukabumi.net/uploads/" . $fileName;
        $col = $uploadMap[$type]['col'];

        try {
            $stmt = $pdo->prepare("UPDATE community_settings SET {$col} = ?, updated_at = ? WHERE id = 1");
            $stmt->execute([$url, time() * 1000]);
            sendResponse(true, "Upload $type berhasil", ["url" => $url]);
        } catch (Exception $e) {
            sendResponse(false, "Database error: " . $e->getMessage(), null, [], 500);
        }
    } else {
        sendResponse(false, "Gagal mengunggah file ke server", null, [], 500);
    }
}
