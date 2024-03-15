(function ($, Granite) {
    "use strict";

    var registry = $(window).adaptTo("foundation-registry");

    //validation for Expiry Date
    registry.register("foundation.validation.validator", {
        selector: "[name='./jcr:content/metadata/pwc-expirationDate']",
        validate: function (el) {
            var expiryDate,
                publicationDate,
                expiryDateMs,
                publicationDateMs;
            expiryDate = el.value;
            publicationDate = $('[name="./jcr:content/metadata/pwc-publicationDate"]').val();
            if (expiryDate && publicationDate) {
                expiryDateMs = Date.parse(expiryDate);
                publicationDateMs = Date.parse(publicationDate);
                if (expiryDateMs <= publicationDateMs) {
                    return Granite.I18n.get('Expiry Date should be later than Publication Date');
                }
            }
        }
    });

    //validation for Publication Date
    registry.register("foundation.validation.validator", {
        selector: "[name='./jcr:content/metadata/pwc-publicationDate']",
        validate: function (el) {
            var expiryDate,
                publicationDate,
                expiryDateMs,
                publicationDateMs;
            publicationDate = el.value;
            expiryDate = $('[name="./jcr:content/metadata/pwc-expirationDate"]').val();
            if (expiryDate && publicationDate) {
                expiryDateMs = Date.parse(expiryDate);
                publicationDateMs = Date.parse(publicationDate);
                if (expiryDateMs <= publicationDateMs) {
                    return Granite.I18n.get('Publication Date should be earlier than Expiry Date');
                }
            }
        }
    });

}($, Granite));