/* global jQuery, Coral */
(function($, Coral) {
    "use strict";

    var registry = $(window).adaptTo("foundation-registry");

    // Validator for required for multifield max and min items
    registry.register("foundation.validation.validator", {
        selector: "[data-validation=min-max-items-validation]",
        validate: function(element) {
            var el = $(element),max,min,zero,maxRequired,items,domitems;
            max=el.data("max-items");
            min=el.data("min-items");
            zero=el.data("zero-items");
            maxRequired=el.data("max-required") ? el.data("max-required") : false;
            items=el.children("coral-multifield-item").length;
            domitems=el.children("coral-multifield-item");
            if(items>max){
                domitems.last().remove();
                return "You can add maximum "+max+" items. You are trying to add "+items+"th item.";
            }
            if(items<min){
                return "You must add minimum "+min+" items.";
            }
            if(maxRequired && items>zero && items<max){
                return "You must add "+max+" items.";
            }
        }
    });

}(jQuery, Coral));
