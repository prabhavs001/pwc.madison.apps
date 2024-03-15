(function($, undefined) {
    "use strict";

    var fmdita = window.fmdita = window.fmdita || {};
    var PAYLOAD_KEY = {
        "review": "fmdita.review_topics_data",
        "approval": "fmdita.approval_topics_data"
    }

    function getParameterByName (name, loc) {
        if (loc == null) loc = window.location.search
        name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]')
        var regex = new RegExp('[\\?&]' + name + '=([^&#]*)')
        var results = regex.exec(loc)
        return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '))
    }

    var breakPath = function(path) {
        var idx = path.lastIndexOf("/");
        if(idx != -1)
            return {base : path.substring(0, idx), asset : [ path ]};
        else
            return {base : path, asset : []};
    };

    function startCollaboration(topics, type, redirectUrl) {
        var collabType = Granite.I18n.get(type);
        var targetUrl = "/bin/publishlistener";
        var ajaxOptions = {
            type: "get",
            data: {
                topics: JSON.stringify(topics),
                operation: type == "review" ? "REVIEWSTATUS" : "APPROVALSTATUS"
            },
            url: targetUrl
        };

        var jqxhr = $.ajax(ajaxOptions);
        jqxhr.done(function(resultJson) {

            var msgTopics = "";
            var isUnderCollab = false;
            for(var i = 0; i < topics.length; i++) {
                if(resultJson[i] == 1) {
                    msgTopics += topics[i] + "<br/>";
                    isUnderCollab = true;
                }
            }
            if(isUnderCollab) {
                var ui = $(window).adaptTo("foundation-ui");
                if(topics.length == 1) {
                    var lpath = topics[0].toLowerCase();
                    if (lpath.endsWith(".ditamap")) {
                        ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("The DITA map is already under ") + collabType + ":<br/><br/>" + msgTopics, "error");
                    } else {
                        ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Following topic is already under ") + collabType + ":<br/><br/>" + msgTopics, "error");
                    }
                } else {
                    ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Following topic(s) is/are already under ") + collabType + ":<br/><br/>" + msgTopics, "error");
                }
            } else {
                redirectWithPayLoad(redirectUrl, topics, type);
            }
        });
        jqxhr.fail(function(xhr, error, errorThrown) {
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Failed to create review."), "error");
        });
    }

    //Checks doc status for fullcycle/simple workflows. Shows error if the map is already in review.
    function checkDocStatus(topics,collabType,redirectUrl){
        $.get("/bin/pwc/getDocStatus", {
			ditamap: topics[0],
		}, null).then(function(result){
            var msgTopics = "";
            var isUnderCollab = false;
            for(var i = 0; i < topics.length; i++) {
                if(result == "reviewProgess") {
                    msgTopics += topics[i] + "<br/>";
                    isUnderCollab = true;
                }
                if(result == "collaborationProgess") {
                    msgTopics += topics[i] + "<br/>";
                    isUnderCollab = true;
                    collabType = "collaboration"
                }
            }
            if(isUnderCollab) {
                var ui = $(window).adaptTo("foundation-ui");
                if(topics.length == 1) {
                    var lpath = topics[0].toLowerCase();
                    if (lpath.endsWith(".ditamap")) {
                        ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("The DITA map is already under ") + collabType + ":<br/><br/>" + msgTopics, "error");
                    } else {
                        ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Following topic is already under ") + collabType + ":<br/><br/>" + msgTopics, "error");
                    }
                } else {
                    ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Following topic(s) is/are already under ") + collabType + ":<br/><br/>" + msgTopics, "error");
                }
            } else {
                redirectWithPayLoad(redirectUrl, topics, "review");
            }
        },function(error){
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Failed to create " + type + "."), "error");
        });

    }

    window.fmdita.createReview = function() {
        var topics = getFilesForCollab();
        var collabType = "review";
        var redirectUrl = "/libs/fmdita/review/createreview.html";
        startCollaboration(topics, collabType, redirectUrl);
    }

    window.fmdita.createFullCycleReview = function() {
        var topics = getFilesForCollab();
        var collabType = "review";
        var redirectUrl = "/apps/fmdita/review/createfullcyclereview.html";
        checkDocStatus(topics, collabType, redirectUrl);
    }

    window.fmdita.createSimpleContentReviewTask = function() {
        var topics = getFilesForCollab();
        var collabType = "review";
        var redirectUrl = "/apps/fmdita/review/createsimplereviewworkflow.html";
        checkDocStatus(topics, collabType, redirectUrl);
    }

    window.fmdita.createCollaborationTask = function() {
        var topics = getFilesForCollab();
        var collabType = "review";
        var redirectUrl = "/apps/fmdita/review/createcollaboration.html";
        checkDocStatus(topics, collabType, redirectUrl);
    }

    window.fmdita.createApproval = function() {
        var topics = getFilesForCollab();
        var collabType = "approval";
        var redirectUrl = "/libs/fmdita/approval/createapproval.html";
        startCollaboration(topics, collabType, redirectUrl);
    }

    function getFilesForCollab() {
        var topics = [];
        var selectedAsset =  $(".foundation-collection-item.foundation-selections-item");

        if (selectedAsset != null) {
            if(selectedAsset.length) {

                for(var i = 0; i < selectedAsset.length; i++) {
                    var path = $(selectedAsset[i]).attr("data-foundation-collection-item-id");;
                    var lpath = path.toLowerCase();
                    if (lpath.endsWith(".ditamap") || lpath.endsWith(".xml") || lpath.endsWith(".dita")) {
                        topics.push(path);
                    }
                }
            }
            /*else {
                var path = selectedAsset.data("path");
                if(path != null) {
                    var lpath = path.toLowerCase();
                    if (lpath.endsWith(".ditamap") || lpath.endsWith(".xml") || lpath.endsWith(".dita"))
                        topics.push(path);
                }
            }*/
        }
        if(topics.length == 0) {
            var payload = rh.model.get('.d.payload');
            if (payload.endsWith(".ditamap"))
                topics.push(payload);                
        }
        return topics;
    }

    function redirectWithPayLoad(url, topics, collabType) {
        var respjson = null;
        topics.forEach(function(topic, index){
            if(!respjson)
                respjson = breakPath(topic);
            else
            {
                var tmpjson = breakPath(topic);
                if(tmpjson.asset.length)
                    respjson.asset[respjson.asset.length] = tmpjson.asset[0];
            }
        });
        respjson.referrer = document.URL;
        if (respjson && PAYLOAD_KEY[collabType]) {
            sessionStorage.setItem(PAYLOAD_KEY[collabType], window.encodeURIComponent(JSON.stringify(respjson)));
            window.location.href = url;
        }
    }
})(Granite.$);
