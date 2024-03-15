$(document).on("dialog-ready", function() {
	var ui, noOfFallBacks = 0,componentName,$form,componentPath,checkLengthOfTextFields,responseForCheckingTextFields = "Valid";
	checkLengthOfTextFields = function(){
		if($("[name='./text1']").val().length > 12){
			return "Character limit exceeded for line 1";
		}
		else if($("[name='./text2']").val().length > 12){
			return "Character limit exceeded for line 2";
		}
		else if($("[name='./text3']").val().length > 90){
			return "Character limit exceeded for line 3";
		}
        else{
			return "Valid";
		}
	};
	$(".cq-dialog-submit").click(function() {
		$form = $(this).closest("form.foundation-form");
		componentPath = $form.find("[name='./sling:resourceType']").val();
		$('coral-checkbox').each(function() {
			if ($(this).attr('name').includes("./fallbackForHeroTile")) {
				componentName = "HeroTile";
				responseForCheckingTextFields = checkLengthOfTextFields();
				if ($(this).prop('checked') === true) {
					noOfFallBacks = noOfFallBacks + 1;
				}
			}
		});
		if ((noOfFallBacks < 1 && componentName === "HeroTile") || (noOfFallBacks < 1 && componentPath === "pwc-madison/components/content/home/hero-tile")) {
			ui = $(window).adaptTo("foundation-ui");
			ui.alert("Number of fallbacks selected should be One");
			return false;
		}
		if(responseForCheckingTextFields !== "Valid" && componentName === "HeroTile"){
			ui = $(window).adaptTo("foundation-ui");
			ui.alert(responseForCheckingTextFields);
			return false;
		}
	});
});