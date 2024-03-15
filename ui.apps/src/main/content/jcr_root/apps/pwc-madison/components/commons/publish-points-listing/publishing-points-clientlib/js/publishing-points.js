$(function () {
    var publishingPoint, getPublishingPoint, showErrorBox, statusPolling, startpolling, triggerAssetReplication,regenTopics;

    if ($(".no-publish-point").length > 0) {
        $(".generate-output-button").hide();
        $(".regenerate-topics-button").hide();
    }

    showErrorBox = function (classString) {
        document.querySelector('.' + classString).show();
    };

    triggerAssetReplication = function (generatedPage) {
        var ajaxOptions = {
            type: "get",
            data: {
                outputPath: generatedPage,
                regeneratedTopics: regenTopics
            },
            url: "/bin/pwc/triggerAssetReplication"
        },
        jqxhr = $.ajax(ajaxOptions);
    };

    statusPolling = function () {
        publishingPoint = getPublishingPoint().val();
        var ajaxOptions = {
                type: "get",
                data: {
                    operation: "PUBLISHBEACON",
                    source: publishingPoint
                },
                url: "/bin/publishlistener"
            },
            jqxhr = $.ajax(ajaxOptions);
        jqxhr.done(function (response) {
            if (response.queuedOutputs && response.queuedOutputs.length < 1) {
                setTimeout(function () {
                    clearInterval(startpolling);
                    $(".publish-console-wait").addClass("hidden");
                    var generatedPage = getPublishingPoint().attr("data-published-page");
                    if (regenTopics && generatedPage) {
                        triggerAssetReplication(generatedPage);
                        document.querySelector('.regenerate-success-modal').show();
                    } else if (generatedPage){
                        document.querySelector('.full-generation-success-modal').show();
                    }
                    else {
                        showErrorBox("regenerate-no-page-modal");
                    }
                }, 1000);
            }
        });
        jqxhr.fail(function (error) {
            showErrorBox();
        });
    };

    getPublishingPoint = function () {
        var $checked = $('[name="pub-points-item"]').filter(function () {
            return $(this).prop('checked');
        });
        return $checked;
    };

    $(".publishing-points-form").on('click', '.generate-output-button', function (event) {
        publishingPoint = getPublishingPoint().val();
        var jqxhr, ajaxOptions = {
            type: "get",
            data: {
                operation: "GENERATEOUTPUT",
                source: publishingPoint,
                outputName: "aemsite"
            },
            url: "/bin/publishlistener"
        };
        jqxhr = $.ajax(ajaxOptions);
        jqxhr.done(function () {
            $(".publish-console-wait").removeClass("hidden");
            startpolling = setInterval(statusPolling, 2000);
        });
        jqxhr.fail(function (error) {
        });
    });
    $(".success-button").click(function (event) {
        var generatedPage = getPublishingPoint().attr("data-published-page");
        if (generatedPage) {
            window.open(generatedPage);
        }
    });

    $(".regenerate-topics-button").click(function () {
        publishingPoint = getPublishingPoint().val();
        var ajaxOptions, jqxhr, topicsList = $('[name="visibleTopicList"]'), maps = getPublishingPoint().find('[name="mapList"]'),
            jsonString = {
                "ditamap": publishingPoint,
                "topics": [],
                "fullMaps": [],
                "maps": [],
                "outputs": ["workflowtopicregeneration"]
            };
        if (topicsList.length > 0) {
            topicsList.each(function (index, element) {
                var topicPath = $(this).attr("value");
                if(topicPath.endsWith('.ditamap')){
                    jsonString.fullMaps.push(topicPath);
                }else{
                    jsonString.topics.push(topicPath);
                }
            });
            if(maps.length > 0){
                maps.each(function(index, element){
                var mapPath = $(this).val();
                jsonString.maps.push(mapPath);
                });
            }
            regenTopics = JSON.stringify(jsonString.topics);
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
                $(".publish-console-wait").removeClass("hidden");
                startpolling = setInterval(statusPolling, 2000);
            });
            jqxhr.fail(function (error) {
                showErrorBox("regenerate-failure-modal");
            });
        }
    });
});
