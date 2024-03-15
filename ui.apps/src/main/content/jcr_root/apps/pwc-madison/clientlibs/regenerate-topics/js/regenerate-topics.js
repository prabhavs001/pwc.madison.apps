$(document).ready(function () {
    var startpolling,
        currentUrl = window.location.href,
        showErrorBox = function () {
            $(".regenerate-failure-modal").removeClass("hidden");
            setTimeout(function () {
                $(document).mouseup(function (e) {
                    var errorMsgBox = $(".regenerate-failure-modal");
                    //hiding the failure message on clicking outside the modal
                    if (!errorMsgBox.is(e.target) && errorMsgBox.has(e.target).length === 0) {
                        errorMsgBox.hide();
                    }
                });
            }, 1000);
        },
        statusPolling = function () {
            var rootDitaMap = $(".root-ditamap").val(),
                ajaxOptions = {
                    type: "get",
                    data: {
                        operation: "PUBLISHBEACON",
                        source: rootDitaMap
                    },
                    url: "/bin/publishlistener"
                },
                jqxhr = $.ajax(ajaxOptions);
            jqxhr.done(function (response) {
                if (response.queuedOutputs && response.queuedOutputs.length < 1) {
                    setTimeout(function () {
                        clearInterval(startpolling);
                        $(".success-button").removeClass("hidden");
                        $(".coral-Modal coral-wait").attr("hidden", true);
                    }, 1000);
                }
            });
            jqxhr.fail(function (error) {
                showErrorBox();
            });
        };

    if (currentUrl.indexOf("aempages=true") > 0) {
        $("a[data-foundation-wizard-control-action='cancel']").find("coral-anchorbutton-label").text("Close");
        $(".regenerate-topics-button").hide();
    }
    $(".regenerate-topics-button").click(function () {
        var ajaxOptions, jqxhr, topicsList = $(".topic-content-path"),
            rootDitaMap = $(".root-ditamap").val(),
            jsonString = {
                "ditamap": rootDitaMap,
                "topics": [],
                "outputs": ["workflowtopicregeneration"]
            };
        if (topicsList.length > 0) {
            topicsList.each(function (index, element) {
                var topicPath = $(this).val();
                jsonString.topics.push(topicPath);
            });
            ajaxOptions = {
                type: "get",
                data: {
                    operation: "INCREMENTALPUBLISH",
                    contentPath: JSON.stringify(jsonString)
                },
                url: "/bin/publishlistener"
            };
            jqxhr = $.ajax(ajaxOptions);
            jqxhr.done(function () {
                $(".regenerate-success-modal").removeClass("hidden");
                $(".regenerate-modal-background").addClass("coral-Modal-backdrop");
                startpolling = setInterval(statusPolling, 2000);
            });
            jqxhr.fail(function (error) {
                showErrorBox();
            });
        }
    });

    function sanitizeString(string) {
		var tempDiv = document.createElement('div');
		tempDiv.textContent = string;
		return tempDiv.innerHTML;
    }

    $(".success-button").click(function (event) {
        event.preventDefault();
        var url = window.location.href + "&aempages=true";
        window.location.href = sanitizeString(url);
    });
});
