use(function () {
	"use strict";
    if(wcmmode != 'EDIT') {
        var linkUrl = granite.resource.properties["linkUrl"];
        if(linkUrl) {
            response.setStatus(302);
            response.setHeader("Location", linkUrl);
        }
    }
});