<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/PositionService.php';
    create();
}

function create() {
    $latitude = $_POST['latitude'];
    $longitude = $_POST['longitude'];
    $datePosition = $_POST['date_position'];
    $imei = $_POST['imei'];

    $service = new PositionService();
    $position = new Position(null, $latitude, $longitude, $datePosition, $imei);
    $service->create($position);

    echo "Position enregistrée avec succès";
}
?>