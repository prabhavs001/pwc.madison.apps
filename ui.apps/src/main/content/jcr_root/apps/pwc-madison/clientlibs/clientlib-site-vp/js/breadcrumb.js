(function(document, $, tippy) {

	var breadcrumb = window.breadcrumbItems, items = [], tooltip_items = [], i;
	if(breadcrumb && breadcrumb.items){
		if (breadcrumb.items.length <= 4) {
			for (i = 0; i < breadcrumb.items.length; i++) {
				if(i === 0){
					items.push("<a href='" + breadcrumb.items[i].href
							+ "' class='link'>" + breadcrumb.items[i].title + "</a>");
				}else {
					items.push("<a href='" + breadcrumb.items[i].href
							+ "' class='link breadcrumb-clamp-text'>" + breadcrumb.items[i].title + "</a>");
				}				
				if (i < breadcrumb.items.length - 1) {
					items.push("<span class='icon-caret-right'></span>");
				}
			}
			$("<div/>", {
				"class" : "content-breadcrumb",
				html : items.join("")
			}).appendTo(".breadcrumb_container");
		} else {
			items.push("<a href='" + breadcrumb.items[0].href + "' class='link'>"
					+ breadcrumb.items[0].title + "</a>");
			items.push("<span class='icon-caret-right'></span>");
			items.push("<a href='javascript:;'' class='link breadcrumb-tooltip-popout dots-icon'><span class='icon-dots'></span></a>");
			for (i = breadcrumb.items.length - 3; i >= 1; i--) {
				tooltip_items.push("<div class='inner-lins breadcrumb-clamp-text'><a href='"
						+ breadcrumb.items[i].href + "' >"
						+ breadcrumb.items[i].title + "</a> </div>");
			}
			items.push("<span class='icon-caret-right'></span>");
			items.push("<a href='"
					+ breadcrumb.items[breadcrumb.items.length - 2].href
					+ "' class='link breadcrumb-clamp-text'>"
					+ breadcrumb.items[breadcrumb.items.length - 2].title + "</a>");
			items.push("<span class='icon-caret-right'></span>");
			items.push("<a href='"
					+ breadcrumb.items[breadcrumb.items.length - 1].href
					+ "' class='link breadcrumb-clamp-text'>"
					+ breadcrumb.items[breadcrumb.items.length - 1].title + "</a>");
			$("<div/>", {
				"class" : "breadcrumb-tooltip-content",
				html : tooltip_items.join("")
			}).appendTo(".breadcrumb_tooltip_content");
			$("<div/>", {
				"class" : "content-breadcrumb",
				html : items.join("")
			}).appendTo(".breadcrumb_container");
		}
	}
	
	tippy(".breadcrumb-tooltip-popout", {
		content : $("#breadcrumb_tooltip").html(),
		placement : "bottom",
		trigger : 'click',
		theme : "breadcrumb",
		arrow : true,
		arrowType : "sharp",
		animation : "fade",
		zIndex : 9999,
		interactive : true,
		distance : 20,
		boundary : "window",
		onShown : function() {
			if ($("body .tippy-arrow").length < 2) {
				$("body .tippy-arrow").clone().insertAfter(
						"div.tippy-arrow:last");
			}
		}
	});
	
}(document, $, window.tippy));
