(function (document, $, window) {
    "use strict";
    var url = window.location.href, setColumnInclination, setLayoutChange;

    setLayoutChange = function () {

        var stackOrder = $("input[name='./stackOrderMobile']"), centerVertical = $("input[name='./verticallyCenterColumn']"), layout = $("coral-select[name='./layout']");

        setTimeout(function () {
            setColumnInclination(layout, stackOrder, centerVertical, false);
        }, 0);
        layout.change(function () {
            setColumnInclination(layout, stackOrder, centerVertical, true);
        });
    };

    if (url.indexOf("/mnt/override/apps/pwc-madison/components") !== -1) {
        $(window).on("load", function () {
            setLayoutChange();
        });
    } else {
        $(document).on("dialog-ready", function () {
            setLayoutChange();
        });
    }


    // Enables/disables the stack order and Column Vertically Centered based on column layout
    setColumnInclination = function (layout, stackOrder, centerVertical, isChange) {
        var selectedLayout = layout.find('coral-select-item[selected]').val(), layoutVal = selectedLayout.substring(0, selectedLayout.indexOf(';')), stackOrderCheck, centerVerticalCheck;
        if (layoutVal === '2') {
            stackOrder.removeAttr('disabled');
            centerVertical.removeAttr('disabled');
        } else {
            stackOrder.attr('disabled', 'disabled');
            centerVertical.attr('disabled', 'disabled');
        }
        if (isChange) {
            if (layoutVal !== '2') {
                stackOrder.filter("[value='column1']").prop("checked", true);
                centerVertical.filter("[value='none']").prop("checked", true);
            }
        } else {
            stackOrderCheck = stackOrder.filter(':checked');
            centerVerticalCheck = centerVertical.filter(':checked');
            if (stackOrderCheck.length === 0) {
                stackOrder.filter("[value='column1']").prop("checked", true);
            }
            if (centerVerticalCheck.length === 0) {
                centerVertical.filter("[value='none']").prop("checked", true);
            }
        }
    };

}(document, Granite.$, window));
