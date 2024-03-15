$(document).on("dialog-ready", function() {
	$(".cq-dialog-submit").click(function() {
		var ui, noOfFallBacks = 0,componentName,$form,componentPath;
		$form = $(this).closest("form.foundation-form");
		componentPath = $form.find("[name='./sling:resourceType']").val();
		$('coral-checkbox').each(function() {
			if ($(this).attr('name').includes("./fallbackForInsights")) {
				componentName = "Insights";
				if ($(this).prop('checked') === true) {
					noOfFallBacks = noOfFallBacks + 1;
				}
			}
		});
		if ((noOfFallBacks < 1 && componentName === "Insights") || (noOfFallBacks < 1 && componentPath === "pwc-madison/components/content/home/insights")) {
			ui = $(window).adaptTo("foundation-ui");
			ui.alert("Number of fallbacks selected should be One");
			return false;
		}
	});
});