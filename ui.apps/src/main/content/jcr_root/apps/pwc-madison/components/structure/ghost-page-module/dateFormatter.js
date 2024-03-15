use(function () {
	"use strict";
	var formattedDate = new java.text.SimpleDateFormat(this.mask).format(this.date);
	return {
		formattedDate: formattedDate
    };
});