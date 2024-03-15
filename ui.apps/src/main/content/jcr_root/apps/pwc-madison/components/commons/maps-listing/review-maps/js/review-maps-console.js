$(function () {
    var mapListItem = $(".review-map-item"),
        url = window.location.href,
        getVersions,
        getVersionDetails,
        populateVersionSpecificRefs,
        ditamapPath,
        mapList,
        populateXmlStructure,
        formatXml,
        sanitizeString;

	sanitizeString = function (string) {
		var tempDiv = document.createElement('div');
		tempDiv.textContent = string;
		return tempDiv.innerHTML;
	};

    mapListItem.click(function (event) {
        event.preventDefault();
        if (url.indexOf("&ditamap")) {
            url = url.split("&ditamap")[0];
        }
        url = url + "&ditamap=" + $(this).find("a").text();
        window.location.href = sanitizeString(url);
    });

    populateVersionSpecificRefs = function (versionInfo, version) {
        var topicRefTemplateElement = $("#older-version-reftopics").html(),
            topicRefTemplate = Handlebars.compile(topicRefTemplateElement),
            frozenNodePath,
            versionSpecificList = versionInfo.map(function (ref) {
                if (ref.version === version) {
                    frozenNodePath = ref.frozenNodePath;
                    return ref.topicRefs;
                }
            }).filter(function (val) {
                return val !== undefined;
            })[0] || [],
            topicRefTemplateHtml = topicRefTemplate(versionSpecificList);
        $(".topicrefs-container").append(topicRefTemplateHtml);

        populateXmlStructure("older", frozenNodePath);
    };

    formatXml = function (xml) {
        var formatted = '',
            reg = /(>)(<)(\/)/g,
            pad = 0;
        xml = xml.replace(reg, '$1\r\n$2$3');
        jQuery.each(xml.split('\r\n'), function (index, node) {
            var indent = 0,
                padding = '';
            if (node.match(/[\w\W]+<\/\w[^>]*>$/)) {
                indent = 0;
            } else if (node.match(/^<\/\w/)) {
                if (pad != 0) {
                    pad -= 1;
                }
            } else if (node.match(/^<\w[^>]*[^\/]>[\w\W]*$/)) {
                indent = 1;
            } else {
                indent = 0;
            }
            for (var i = 0; i < pad; i++) {
                padding += '  ';
            }

            formatted += padding + node + '\r\n';
            pad += indent;
        });

        return formatted;
    };

    populateXmlStructure = function (type, frozenNodePath) {
        $.get("/bin/referencelistener", {
            operation: "getversion",
            path: ditamapPath,
            version: "",
            versionpath: frozenNodePath,
            cache: false
        }, null).then(function (respString) {
            if (type === "latest") {
                $(".latest-version-xml").val(formatXml(respString));
            } else {
                $(".selected-version-xml").val(formatXml(respString));
            }

        });
    }

    getVersionDetails = function (versionPaths) {
        $.post("/bin/pwc/versiondetails", {
            versionData: versionPaths
        }, null, 'json').then(function (versionInfo) {
            var latestVersion = versionInfo[versionInfo.length - 1],
                olderVersions = versionInfo.slice(0, versionInfo.length - 1),
                secondLastVersion,
                latestVersionTemplateElement = $("#review-map-latest-version").html(),
                olderVersionTemplateElement = $("#review-map-older-versions").html(),
                latestVersionTemplate = Handlebars.compile(latestVersionTemplateElement),
                olderVersionTemplate = Handlebars.compile(olderVersionTemplateElement),
                latestVersionHtml = latestVersionTemplate(latestVersion),
                olderVersionHtml = olderVersionTemplate(olderVersions);

            $(".older-versions-container").append(olderVersionHtml);
            $(".latest-version-container").append(latestVersionHtml);

            populateXmlStructure("latest", latestVersion.frozenNodePath);

            setTimeout(function () {
                if (versionInfo.length > 1) {
                    $(".no-older-versions").addClass("hidden");
                    $(".older-versions-container").removeClass("hidden");
                    $(".older-versions-text").removeClass("hidden");
                    secondLastVersion = versionInfo[versionInfo.length - 3];
                    $('.older-versions-container [value="' + secondLastVersion.version + '"]').attr("selected", "");
                    populateVersionSpecificRefs(versionInfo, secondLastVersion.version);
                } else {
                    $(".no-older-versions").removeClass("hidden");
                    $(".older-versions-container").addClass("hidden");
                    $(".older-versions-text").addClass("hidden");
                }
                $(".older-versions-container").change(function () {
                     $(".topicrefs-container").find('.topic-ref-item').remove();
                     var version = $(this).val();
                     populateVersionSpecificRefs(versionInfo, version);
                 });
            }, 100);


        });
    };

    getVersions = function (ditamapPath) {
        $.get("/bin/referencelistener", {
            operation: "getVersionHistory",
            path: ditamapPath,
            "_charset_": "UTF-8"
        }, null, 'json').then(function (resp) {
            var versionPaths = "";
            resp.versionHistoryData.forEach(function (version, index) {
                var rendition = version.rendition;
                rendition = rendition.split("/jcr:frozenNode/jcr:content")[0];
                if (versionPaths) {
                    versionPaths += "," + rendition;
                } else {
                    versionPaths += rendition;
                }
            });
            getVersionDetails(versionPaths);
        });
    };

    if (url.indexOf("&ditamap=")>0) {
        $(".diff-console").first().show();
        ditamapPath = url.split("&ditamap=")[1];
        mapList = $(".review-map-item");
        mapList.each(function (index, element) {
            var mapPath = $(this).find("a").text();
            if (mapPath === ditamapPath) {
                $(this).attr("selected", "");
            }
        });
        getVersions(ditamapPath);
    } else{
        $(".diff-console").first().hide();
    }

    Handlebars.registerHelper('formatLink', function (refMapPath) {
        var assetLink = refMapPath;
        if (refMapPath.includes(",")) {
            assetLink = refMapPath.replace(",", "");
        }
        return new Handlebars.SafeString(assetLink);
    });
});
