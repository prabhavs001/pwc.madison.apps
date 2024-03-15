$(document).on("dialog-ready", function() {
    $(".cq-dialog-submit").click(function() {

        var ui, noOfFallBacks = 0,
            componentName, renditionStyle, imagePath;

        $('coral-checkbox').each(function() {
            if ($(this).attr('name').includes("./fallbackForFeatured")) {
                componentName = "FeaturedComponent";
                if ($(this).prop('checked') === true) {
                    noOfFallBacks = noOfFallBacks + 1;
                }
            }
        });

        if (noOfFallBacks < 1 && componentName === "FeaturedComponent") {
            ui = $(window).adaptTo("foundation-ui");
            ui.alert("Number of fallbacks selected should be one");
            return false;
        }



    });
});

(function(document, $) {
    "use strict";
    
    function showHideCustom(component, element) {
        // get the selector to find the target elements. Its stored as data-.. attribute
        var target = $(element).data("cq-dialog-rendition-dropdown-showhide-target"),
        $target = $(target),
        elementIndex = $(element).closest('coral-multifield-item').index(),
        value;

        if (target) {
            if (component.value) {
                value = component.value;
            } else {
                value = component.getValue();
            }
            $(element).closest("coral-multifield-item").find(target).each(function(index) {
                var tarIndex = $(this).closest('coral-multifield-item').index();
                if (elementIndex === tarIndex) {
                    $(this).not(".hide").parent().addClass("hide");
                    if (!$(this).filter("[data-showhidetargetvalue='" + value + "']").parent().hasClass("hide")) {
                        $(this).find('input').val('');
                    }
                    $(this).filter("[data-showhidetargetvalue='" + value + "']").parent().removeClass("hide");
                }
            });
        }
    }
    
    function showHideHandler(el) {
        el.each(function(i, element) {
            // handle Coral3 base drop-down
            Coral.commons.ready(element, function(component) {
                showHideCustom(component, element);
                component.on("change", function() {
                    showHideCustom(component, element);
                });
            });
        });
    }

    // when dialog gets injected
    $(document).on("foundation-contentloaded", function(e) {
        // if there is already an inital value make sure the according target element becomes visible
        showHideHandler($(".cq-dialog-rendition-dropdown-showhide", e.target));
    });

    $(document).on("change", ".cq-dialog-rendition-dropdown-showhide", function(e) {
        showHideHandler($(this));
    });
    

}(document, Granite.$));