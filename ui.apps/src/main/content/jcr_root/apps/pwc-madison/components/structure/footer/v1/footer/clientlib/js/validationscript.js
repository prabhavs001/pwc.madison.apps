$(document)
    .on("dialog-ready",
        function () {
            $(".coral3-Icon")
                .click(
                    function () {
                        var checkIcon, field,
                            attrDV, size,
                            ui, totalLinkCount;
                        checkIcon = $(this).attr(
                            'aria-label');
                        if (checkIcon === 'check') {
                            field = $('.coral3-Multifield');
                            attrDV = field.attr("data-validation");
                            if (typeof attrDV !== "undefined"
                                && attrDV !== false) {
                                size = attrDV;
                                if (size) {
                                    ui = $(window).adaptTo(
                                        "foundation-ui");
                                    totalLinkCount = $(
                                        '.coral3-Multifield')
                                        .find(
                                            '.coral3-Multifield-item').length;
                                    if (totalLinkCount < size) {
                                        ui
                                            .alert(
                                                "Warning",
                                                "Minimum "
                                                + size
                                                + " link is necessary!",
                                                "notice");
                                        return false;
                                    }
                                }
                            }
                        }
                    });
        });