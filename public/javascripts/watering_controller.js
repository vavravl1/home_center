function redrawWateringState() {
    $.ajax({
        url: $("#wateringBackendUrl").val(),
        success: renderWateringState,
        complete: function () {
            setTimeout(redrawWateringState, 1000);
        }
    });
}

function renderWateringState(wateringMessage) {
    if(wateringMessage !== null) {
        $('#watering_last_update').text(DateUtils.epochToString(wateringMessage.timestamp));
        $('#watering_actual_humidity').text(wateringMessage.telemetry.humidity.actual);
        $('#watering_base_line').text(wateringMessage.telemetry.humidity['base-line']);
    }
}

window.onload = function() {
    setTimeout(redrawWateringState, 1000);

    $("#ibisek-watering-button").click(function () {
        $.post( $("#wateringBackendUrl").val()+"/manual-watering", function() {});
    });
};
