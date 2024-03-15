$(function () {
    var GENERATE_BUTTON = $(".dam-admin-reports-generate"),
        DOWNLOAD_BUTTON = $(".dam-admin-reports-download"),
        report = {},
        generateReport,
        downloadReport;

    $(".vital-stats-container").on("click", ".report-row", function (e) {
        var $elm = $(e.target);
        if (!$elm.hasClass("report-row")) {
            $elm = $elm.parents(".report-row");
        }
        $elm.toggleClass("expanded");
    });

    $(".vital-stats-container").on("click", ".accrodion__tabs li", function (e) {
        var $elm = $(this),
            idx = $elm.index(),
            $accrodion = $elm.parents(".accrodion__tabs"),
            $menus = $accrodion.find("li"),
            $panel = $accrodion.nextAll().eq(1).find("li");
        $menus.removeClass("active");
        $elm.addClass("active");
        $panel.removeClass("show").eq(idx).addClass("show");
    });

    generateReport = function (pathString) {
        $.post("/bin/pwc/vitalstats.json", {
            path: pathString
        }, null, 'json').then(function (report) {
                var vitalStatsTemplateElement,
                    vitalStatsTemplate,
                    vitalStatsHTML;

                    $(".no-content-message").hide();
                    vitalStatsTemplateElement = $("#vital-stats-template").html();
                    vitalStatsTemplate = Handlebars.compile(vitalStatsTemplateElement);
                    vitalStatsHTML = vitalStatsTemplate(report);
                    $(".vital-stats-container").append(vitalStatsHTML);
        });
    };

    downloadReport = function (pathString) {
      window.location.href="/bin/pwc/vitalstats.csv?path="+pathString;
    };

    GENERATE_BUTTON.click(function () {
        $(".vital-stats-container").empty();
        var folderPath = $('[name="folderPath"]').val();
        if(!$('[name="folderPath"]').checkValidity()){
            $('[name="folderPath"]').updateErrorUI();
            return;
        }
		generateReport(folderPath);
    });

    DOWNLOAD_BUTTON.click(function () {
        var folderPath = $('[name="folderPath"]').val();
        if(!$('[name="folderPath"]').checkValidity()){
            $('[name="folderPath"]').updateErrorUI();
            return;
        }
		downloadReport(folderPath);
    });
});