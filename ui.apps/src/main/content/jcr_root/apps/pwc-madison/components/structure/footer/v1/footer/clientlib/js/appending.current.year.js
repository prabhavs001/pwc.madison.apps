$(document).on("dialog-ready", function() {
    var date, currentYear, publishingYear;
	date = new Date();
	currentYear = date.getFullYear();
	publishingYear = $('input[name="./publishingYear"]');

	if (publishingYear.val() === "") {
		publishingYear.val(currentYear);
	}
	$('input[name="./publishingYear"]').parent().attr('max',currentYear);
});