(function($) {
	"use strict";
	var current_path = window.location.pathname;
	function showHideTagCreateButton() {
		var targetUrl = "/bin/validateUserPermission", ajaxOptions = {
			type : "get",
			url : targetUrl
		}, jqxhr = $.ajax(ajaxOptions);
		jqxhr
				.done(function(result) {
					var respFromJson = "", hasPermission = "";
					if (result && typeof result === "object") {
						respFromJson = result;
						hasPermission = respFromJson.hasPermission;
						if (hasPermission !== null
								&& hasPermission !== 'undefined') {
							if (hasPermission !== true) {
								if (jQuery('.granite-collection-create').length > 0) {
									jQuery('.granite-collection-create').css(
											"display", "none");
								}
							}
						}
					} else {
						if (jQuery('.granite-collection-create').length > 0) {
							jQuery('.granite-collection-create').css("display",
									"none");
						}
					}
				});
		jqxhr.fail(function(error) {
		});
	}
	if (current_path === '/aem/tags' || current_path === '/content/cq:tags') {
		showHideTagCreateButton();
	}
}(jQuery));