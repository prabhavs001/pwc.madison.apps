/**
 * Maximum items validation
 */
$(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
    // check elements that has attribute data-maxitemsallowed
    selector: "[data-maxitemsallowed]",
    validate: function(el) {
        var max = $(el).data("maxitemsallowed");
        // el is a coral-multifield element
        if (el.items.length > max){
            // items added are more than allowed, return error
            return "Cannot add more than " + max + " tiles. Please delete existing tile to proceed.";
        }
    }
});

/**
 * Minimum items validation
 */
$(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
    // check elements that has attribute data-minitemsallowed
    selector: "[data-minitemsallowed]",
    validate: function(el) {
        var min = $(el).data("minitemsallowed");
        // el is a coral-multifield element
        if (el.items.length < min){
            // items added are not sufficient, return error
            return "Add at least " + min +" items";
        }
    }
});