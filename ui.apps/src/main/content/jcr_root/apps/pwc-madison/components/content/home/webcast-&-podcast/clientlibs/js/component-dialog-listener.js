$(document).on("dialog-ready", function() {

    var EAEM_MAX_ITEMS = 10,
        EAEM_MIN_ITEMS = 2,
        DATA_MF_NAME = "data-granite-coral-multifield-name",
        mfName = "./sourceItems",
        minMsg = "Number of configured items added are lesser than "+ EAEM_MIN_ITEMS,
        maxMsg = "Number of configured items added are more than " + EAEM_MAX_ITEMS,
        $multifield = $("[" + DATA_MF_NAME + "='" + mfName + "']");

    $(".cq-dialog-submit").click(function() {

        var $form = $(this).closest("form.foundation-form"),
			componentPath = $form.find("[name='./sling:resourceType']").val(),
			count,
			ui = $(window).adaptTo("foundation-ui"),
			noOfFallBacksForWebcasts = 0, componentNameForWebcasts;

        if(componentPath === "pwc-madison/components/content/home/webcast-&-podcast") {

            count = parseInt($multifield[0]._items.length,10);

            if (count > EAEM_MAX_ITEMS) {
                ui.alert(maxMsg);
                return false;
            }

            if (count < EAEM_MIN_ITEMS) {
                ui.alert(minMsg);
                return false;
            }

            $('coral-checkbox').each(function() {
                if ($(this).attr('name').includes("./fallbackForWebcasts")) {
                    componentNameForWebcasts = "WebcastsComponent";
                    if ($(this).prop('checked') === true) {
                        noOfFallBacksForWebcasts = noOfFallBacksForWebcasts + 1;
                    }
                }
            });

            if (noOfFallBacksForWebcasts < 2) {
                ui.alert("Number of fallbacks selected should be two");
                return false;
            }

            return true;
        }

    });

});
