$(document).on("click",".coral3-Button", function (event) {
    var field = $(this).parent(), size = field.attr("data-fieldlimit"), label = field.attr("data-fieldlabel"), ui, totalLinkCount;
    if (size) {
        ui = $(window).adaptTo("foundation-ui");
        totalLinkCount = field.find(".coral3-Multifield-item").length;
        if (totalLinkCount > size) {
            if(totalLinkCount === parseInt(size, 10) + 1) {
                field.find(".coral3-Multifield-item")[size].remove();
            }
            ui.alert("Error", "Maximum " + size + " " + label + " are allowed!", "error");
            event.preventDefault();
            return false;
        }
    }
});