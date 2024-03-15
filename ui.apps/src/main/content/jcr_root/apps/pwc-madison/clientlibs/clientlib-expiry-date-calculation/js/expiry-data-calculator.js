$(document).ready(function () {
    var EDIT_EXPIRY_FIELD = $('[name="./jcr:content/metadata/pwc-editExpiryDateField"]'),
        EXPIRY_DATE_FIELD = $('[name="./jcr:content/metadata/pwc-expirationDate"]'),
        PUBLICATION_DATE = $('[name="./jcr:content/metadata/pwc-publicationDate"]'),
        EFFECTIVE_DATE = $('[name="./jcr:content/metadata/pwc-effective-date"]'),
        populateExpiryDate,
        inputDate = "",
        editVal;

    EDIT_EXPIRY_FIELD.change(function () {
        var $checked = EDIT_EXPIRY_FIELD.filter(function () {
            return $(this).prop('checked');
        });
        editVal = $checked.val();
        if (editVal === "yes") {
            EXPIRY_DATE_FIELD.attr('readonly', false);
        } else {
            EXPIRY_DATE_FIELD.attr('readonly', true);
        }
    });

    populateExpiryDate = function () {
        var pubDateString = PUBLICATION_DATE.val(),
            effDateString = EFFECTIVE_DATE.val(),
            pubDate = new Date(pubDateString),
            contentType = $('[name="./jcr:content/metadata/pwc-contentType"]').val(),
            effDate = new Date(effDateString);
        if (pubDateString && effDateString) {
            if (pubDate >= effDate) {
                inputDate = pubDate;
            } else {
                inputDate = effDate;
            }
        } else if (pubDateString) {
            inputDate = pubDate;
        } else if (effDateString) {
            inputDate = effDate;
        }
        if(!contentType){
            contentType = "default";
        }
        if (inputDate && editVal!=="yes") {
            $.get("/bin/pwc/getExpirationDate", {
                contentType: contentType,
                inputDate: inputDate.getTime()
            }).then(function (expDate) {
                EXPIRY_DATE_FIELD.val(expDate);
            });
        }
    };

    $('[name="./jcr:content/metadata/pwc-publicationDate"], [name="./jcr:content/metadata/pwc-effective-date"],[name="./jcr:content/metadata/pwc-contentType"]').on('change', function () {
        populateExpiryDate();
    });

    setTimeout(function () {
        var $checked = EDIT_EXPIRY_FIELD.filter(function () {
            return $(this).prop('checked');
        });
        editVal = $checked.val();
        if (editVal && editVal === "yes") {
            EXPIRY_DATE_FIELD.attr("readonly", false);
        } else {
            EXPIRY_DATE_FIELD.attr('readonly', true);
        }
    }, 500);

});