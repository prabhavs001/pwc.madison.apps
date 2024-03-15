/*global URLSearchParams */
$(function() {
    var GENERATE_BUTTON = $(".dam-admin-reports-generate"),
        inputType = $('[name="expirationViewType"]'),
        expiringDateRange = $('[name="expiringDateRange"]'),
        expiredDateRange = $('[name="expiredDateRange"]'),
        folderPath = $('[name="folderPath"]'),
        userID = $('#pwc-userID').val(),
        searchParams = new URLSearchParams(window.location.search),
        viewType = searchParams.get('view'),
        date = searchParams.get('daterange'),
        path = searchParams.get('path');

        function updateDropdown(el, value) {
            el.each(function(i, element) {
                if ($(element).is("coral-select")) {
                    // handle Coral3 base drop-down
                    Coral.commons.ready(element, function(component) {
                        component.value = value;
                        });
                } 
            });
        }

    $('[name="expirationViewType"]').closest(".coral-Form-fieldwrapper").show();
    $('[name="expiredDateRange"]').closest(".coral-Form-fieldwrapper").hide();
    
    if(viewType !== null && date !== null){
        updateDropdown($('[name="expirationViewType"]'), viewType);
        if(viewType === 'expiringContent'){
           updateDropdown($('[name="expiringDateRange"]'), date);
           $('[name="expiredDateRange"]').closest(".coral-Form-fieldwrapper").hide();
           $('[name="expiringDateRange"]').closest(".coral-Form-fieldwrapper").show();
        }else if(viewType === 'expiredContent'){
            updateDropdown($('[name="expiredDateRange"]'), date);
            $('[name="expiredDateRange"]').closest(".coral-Form-fieldwrapper").show();
           $('[name="expiringDateRange"]').closest(".coral-Form-fieldwrapper").hide();
        }
    }
    if(path !== null){
        folderPath.val(path);
    }

    $(".report-row").on("click", function(e) {
        var $elm = $(e.target),
            mapList = ["path1", "path2", "path3"],
            ditaPath = $elm.parents("tr").next("tr").find(".ditapath").html(),
            refCallAjax;
        if (!$elm.hasClass("report-row")) {
            $elm = $elm.parents(".report-row");
        }
        if ($elm.attr('clicked') === 'true') {
            $elm.toggleClass("expanded");
            return;
        }
        refCallAjax = $.post("/bin/linkmanager", {
            "operation": "getbackwardrefs",
            "items": ditaPath
        }, null, 'json');
        $.when(refCallAjax)
            .done(function(refCallAjaxRes) {
                $elm.attr('clicked', 'true');
                $elm.next(".report-panel-row").find('.ditamappath').html('');
                if(refCallAjaxRes.backwardRefs.length > 0){
                    refCallAjaxRes.backwardRefs[0].refs.forEach(function(val, idx) {
                                        var $link = $('<a>');
                                        $link.attr('href', '/libs/fmdita/report/report.html'+val);
                                        $link.attr('target', '_blank');
                                        $link.html(val);
                                        $elm.next(".report-panel-row").find('.ditamappath').append($link);
                                    });
                }
            });
        $elm.toggleClass("expanded");
    });

    $(".btn-approve").on("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        var $elm = $(e.target),
            $actions = $elm.parents(".actions");
        $actions.hide();
        $actions.siblings(".confirm-approve").show();
    });

    $(".btn-reject").on("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        var $elm = $(e.target),
            $actions = $elm.parents(".actions");
        $actions.hide();
        $actions.siblings(".confirm-reject").show();
    });

    $(".btn-confirm-no").on("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        var $elm = $(e.target);
        $elm.parent().hide();
        $elm.parent().next().hide();
        $elm.parent().prev().show();
    });

    $(".btn-approve-yes").on("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        var $elm = $(e.target),
            $confirm = $elm.parents(".confirm-approve"),
            path=$elm.parents("tr").next("tr").find(".ditapath").html(),
            refCallAjax = $.post("/bin/pwc/expirationreport", {
                "confirmationstatus": "approved",
                "path": path,
                "userID" : userID
            }, null, 'json');
            $.when(refCallAjax)
            .done(function (refCallAjaxRes) {
                $confirm.hide();
                $confirm.siblings(".confirm-ack").html("Approved!").removeClass("error").addClass("success").show();
            });
    });

    $(".btn-reject-yes").on("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        var $elm = $(e.target),
            $confirm = $elm.parents(".confirm-reject"),
            path=$elm.parents("tr").next("tr").find(".ditapath").html(),
            refCallAjax = $.post("/bin/pwc/expirationreport", {
                "confirmationstatus": "rejected",
                "path": path,
                "userID" : userID
            }, null, 'json');
            $.when(refCallAjax)
            .done(function (refCallAjaxRes) {
                $confirm.hide();
                $confirm.siblings(".confirm-ack").html("Rejected!").removeClass("error").addClass("success").show();
            });
    });
    $(document).on('change', 'coral-select[name="expirationViewType"]', function(e) {
        var inputType = $(this).val();
        if (inputType === "expiringContent") {
            $('[name="expiredDateRange"]').closest(".coral-Form-fieldwrapper").hide();
            $('[name="expiringDateRange"]').closest(".coral-Form-fieldwrapper").show();
        } else {
            $('[name="expiredDateRange"]').closest(".coral-Form-fieldwrapper").show();
            $('[name="expiringDateRange"]').closest(".coral-Form-fieldwrapper").hide();
        }
    });



    GENERATE_BUTTON.click(function() {
        var dateRange = '';
        if (inputType.val() === 'expiringContent') {
            dateRange = expiringDateRange.val();
        } else if (inputType.val() === 'expiredContent') {
            dateRange = expiredDateRange.val();
        }
        if ('URLSearchParams' !== undefined) {
            searchParams.set("daterange", dateRange);
            searchParams.set("view", inputType.val());
            searchParams.set("path", folderPath.val());
            window.location.search = searchParams.toString();
        }
    });
});
