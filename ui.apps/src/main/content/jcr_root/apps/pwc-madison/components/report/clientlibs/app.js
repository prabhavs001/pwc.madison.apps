
$(document).ready(function () {

    var $downloadButton, action, $buttonParent, $newDownloadButton, assetPath, arr = [], path;

    $("a").each(function () {
        if ($(this).attr("ng-click")) {
            if ($(this).attr("ng-click").indexOf("download") !== -1) {
                $downloadButton = $(this);
                return;
            }
        }
    });

    action = $downloadButton.attr("ng-click");
    action = action.replace("download", "downloadReport");
    $downloadButton.removeAttr("ng-click");
    $downloadButton.attr("onclick", action);
    $buttonParent = $downloadButton.parent();
    $newDownloadButton = $downloadButton.clone();
    $downloadButton.remove();
    $buttonParent.append($newDownloadButton);

    if (location.href.includes("unpublished-assets-report")) {
        $('<button class="coral-Button unpublish-all-btn coral-Button--primary">Unpublish All</button>').insertAfter($newDownloadButton);
        $(document).on("click", ".unpublish-all-btn", function (event) {
            var unpublishAllBtnElement = $(this);
            arr = [];
            $('tbody').children().each(function () {
                path = $(this).children().eq(1).val();
                arr.push(path);
            });
            if(arr.length > 0){
               unpublishAllBtnElement.attr('disabled', true);
               $.ajax({
                 url: "/bin/pwc-madison/unpublish-assets",
                 type: "POST",
                 data: {
                   "assetPaths": arr
                 },
                 success: function (success) {
                   unpublishAllBtnElement.attr('disabled', false);

                 },
                 error: function (err) {
                     unpublishAllBtnElement.attr('disabled', false);
                 }
               });
            }
        });
    }

});

function downloadReport(path) {
    var isInvalid = false,
        url;
    $("[aria-required=true]").each(function () {
        if (!$(this).checkValidity()) {
            $(this).updateErrorUI();
            isInvalid = true;
        }
    });
    if (!isInvalid) {
        url = path + '?' + $('#report--form').serialize();
        window.open(url, '_blank');
    }
}
