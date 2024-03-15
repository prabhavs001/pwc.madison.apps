$(document).ready(function () {
    var mostPopular, mostPopular_contentid, mostPopular_description,
        mostPopular_title, contentIdClamp, titleClamp, descriptionClamp, isMobileView;
    mostPopular = $('.popular-section .madison-card-block');
    isMobileView = $(window).width() < 769;
    mostPopular.each(function (index, eachfeature) {
        mostPopular_description = $(eachfeature).find(".popular-tile-abstract");
        mostPopular_title = $(eachfeature).find(".module-heading");
        mostPopular_contentid = $(eachfeature).find(".reference");
        contentIdClamp = 2;
        titleClamp = isMobileView ? 4 : 3;
        descriptionClamp = 4;
        if (mostPopular_contentid.length > 0 && mostPopular_title.length > 0 && mostPopular_description.length === 0) {
            titleClamp = 4;
        } else if (mostPopular_title.length > 0 && mostPopular_description.length === 0) {
            titleClamp = 5;
        }
        if (isMobileView && mostPopular_contentid.length === 0 && mostPopular_title.length > 0 && mostPopular_description.length > 0) {
            descriptionClamp = 3;
        }
        mostPopular_description.each(function (index, value) {
            $clamp(value, { clamp: descriptionClamp });
        });
        mostPopular_title.each(function (index, value) {
            $clamp(value, { clamp: titleClamp });
        });
        mostPopular_contentid.each(function (index, value) {
            $clamp(value, { clamp: contentIdClamp });
        });
    });
});
