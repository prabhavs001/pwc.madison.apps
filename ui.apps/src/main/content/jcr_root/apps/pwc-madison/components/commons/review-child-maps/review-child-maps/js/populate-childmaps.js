$(function () {
    var getReferencedMaps,
        REVIEW_PAYLOAD_KEY = "fmdita.review_topics_data",
        payload;

    //Fetches the referenced ditamaps to be populated on the workflow form console
    getReferencedMaps = function () {
        payload = window.fmdita.payload = window.fmdita.payload || JSON.parse(decodeURIComponent(sessionStorage.getItem(REVIEW_PAYLOAD_KEY)));
        sessionStorage.removeItem(REVIEW_PAYLOAD_KEY);
        $.get("/bin/pwc/referencedMaps", {
            ditamap: payload.asset[0]
        }, null, 'json').then(function (referencedMaps) {
            var referencedMapsContainer = $(".referenced-maps-container"),
                referencedMapsTemplateElement = $("#referenced-maps-template").html(),
                referencedMapsTemplate = Handlebars.compile(referencedMapsTemplateElement),
                referencedMapsHtml = referencedMapsTemplate(referencedMaps);
            referencedMapsContainer.append(referencedMapsHtml);
        });
    };


    $(".referenced-maps-container").on('change', 'input[type="checkbox"]', function () {
        populateCheckedCount();
    });
    $("#ref-ditamaps-checkAll").click(function () {
        $('.referenced-maps-container input:checkbox').not(this).prop('checked', this.checked);
        populateCheckedCount();
    });
    populateCheckedCount = function () {
        var countCheckedCheckboxes = $('.referenced-maps-container input[type="checkbox').filter(':checked').length;
        $('.selected-ref-maps-count').text(countCheckedCheckboxes);
    }


    getReferencedMaps();
});