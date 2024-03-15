$(document).ready(function () {

    $('.line-background-image').slick({
        dots: true,
        infinite: false,
        speed: 300,
        slidesToShow: 1,
        slidesToScroll: 1,
        prevArrow:'<button class="slick-prev" aria-label="Previous" type="button"></button>',
        nextArrow:'<button class="slick-next" aria-label="Next" type="button"></button>',
        responsive: [{
            breakpoint: 1024,
            settings: {
                autoplay: true,
                autoplaySpeed: 5000
            }
        }]
    });

    setTimeout(function () {

        var isMobile = window.matchMedia("only screen and (max-width: 768px)").matches, clamp, blackSectionHeading, blackSectionBrief;

        blackSectionHeading = $(".black-section-heading");

        blackSectionHeading.each(function (index, value) {
            if ($(this).siblings().hasClass('no-abstract-desc')) {
                $clamp(value, { clamp: 5 });
            }
            else {
                clamp = isMobile ? 4 : 2;
                $clamp(value, { clamp: clamp });
            }
        });

        blackSectionBrief = $(".black-section-brief");

        blackSectionBrief.each(function (index, value) {
            clamp = isMobile ? 6 : 3;
            $clamp(value, { clamp: clamp });
        });

    }, 0);

});