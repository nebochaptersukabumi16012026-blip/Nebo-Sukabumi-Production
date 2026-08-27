<?php
include 'config.php';
echo json_encode(array("status" => "connected", "db" => $db_name));
?>
