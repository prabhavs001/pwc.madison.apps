$(document).on("click",".coral3-Button", function (event) {
    var field = $(this).parent(), maxSize = field.attr("data-maxNewsItemsLimit"), label = field.attr("data-newsFieldLabel"), ui, totalLinkCount;
    if (maxSize) {
        ui = $(window).adaptTo("foundation-ui");
        totalLinkCount = field.find(".coral3-Multifield-item").length;
        if (totalLinkCount > maxSize) {
            if(totalLinkCount === parseInt(maxSize, 10) + 1) {
                field.find(".coral3-Multifield-item")[maxSize].remove();
            }
            ui.alert("Error", "Maximum " + maxSize + " " + label + " are allowed!", "error");
            event.preventDefault();
            return false;
        }
    }
});