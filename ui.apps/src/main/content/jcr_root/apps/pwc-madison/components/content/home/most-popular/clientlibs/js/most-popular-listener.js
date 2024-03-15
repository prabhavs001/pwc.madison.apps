$(document).on("dialog-ready", function() {
	$(".cq-dialog-submit").click(function() {
		var ui, noOfFallBacks = 0,componentName,$form,componentPath,areFallBackNumeric = true,checkFallback;
		checkFallback = function(){
			$("coral-numberinput.coral3-NumberInput").each(function() {
                if ($(this).attr('name').includes("./noOfViews")) {
                    if(typeof(parseInt($(this).val(),10)) !== "number" || $(this).val() === ""){
					areFallBackNumeric = false;
				}
                }
			});
		};
		$form = $(this).closest("form.foundation-form");
		componentPath = $form.find("[name='./sling:resourceType']").val();
		$('coral-checkbox').each(function() {
			if ($(this).attr('name').includes("./fallbackForMostPopular")) {
				checkFallback();
				componentName = "MostPopular";
				if ($(this).prop('checked') === true) {
					noOfFallBacks = noOfFallBacks + 1;
				}
			}
		});
		if ((noOfFallBacks < 4 && componentName === "MostPopular") || (noOfFallBacks < 4 && componentPath === "pwc-madison/components/content/home/most-popular")) {
			ui = $(window).adaptTo("foundation-ui");
			ui.alert("Number of fallbacks selected should be Four");
			return false;
		}
		else if(!areFallBackNumeric){
			ui = $(window).adaptTo("foundation-ui");
			ui.alert("Please enter a numeric value for Views");
			return false;
		}
	});
});