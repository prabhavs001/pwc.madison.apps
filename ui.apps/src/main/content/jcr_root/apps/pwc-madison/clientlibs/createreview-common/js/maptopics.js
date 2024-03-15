/*
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */

$(function() {

	window.fmdita = window.fmdita || {};
	var REVIEW_PAYLOAD_KEY = "fmdita.review_topics_data";
	var ui = $(window).adaptTo("foundation-ui");
	var waitTicker = null;
	var payload = window.fmdita.payload = window.fmdita.payload || JSON.parse(decodeURIComponent(sessionStorage.getItem(REVIEW_PAYLOAD_KEY)));
	var isDitamap = payload && Array.isArray(payload.asset) && (payload.asset.length > 0) && payload.asset[0].endsWith(".ditamap");
	if(isDitamap) {
		initReviewMapTopics();
	}

	function initReviewMapTopics() {
	    $("#review-maptopics").attr("hidden", false);
	    var data = {
	        ditamap: payload.asset[0]
	    }
	    var ajaxOptions = {
	        url: "/bin/pwc/fetchtopics",
	        data: data,
	        method: "get"
		}
	    $.ajax(ajaxOptions)
	    .done(function(data) {
			data = JSON.parse(data);
	        rh.model.publish(".d.topics", data);
	    })
	    .fail(function(){
	        console.error("Error in fetching topics");
		})
		.complete(function() {
			if(waitTicker) {
				waitTicker.clear();
			}
		});
		waitTicker = ui.waitTicker(Granite.I18n.get("Please wait"), Granite.I18n.get("Map topics loading"));

	    $("#topics-checkAll").on("change", function() {
	        var activeCheckBoxes = $(this).closest("table").find("tbody .coral-Checkbox-input").not(":disabled");
	        $(activeCheckBoxes).prop("checked", this.checked);
	    });

	    $("#review-maptopics").on("change", ".coral-Checkbox-input", updateSelectionCount);

	    updateSelectionCount();
	}
	
	function updateSelectionCount() {
        var selected = $("#review-maptopics").find("tbody .coral-Checkbox-input:checked");
        rh.model.publish(".d.selectionCount", selected.length);
    }
})