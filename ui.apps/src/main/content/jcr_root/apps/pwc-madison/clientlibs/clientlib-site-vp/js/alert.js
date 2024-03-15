$(document).ready(function () {

    $('.dark-subnav-slick-slider').on('init', function() {
        var darknavParent = $(this).parents('.dark-subnav.alertbox');
        darknavParent.addClass('dark-subnav--showing');
        darknavParent.removeClass('dark-subnav--hidden');
        setTimeout(function() {
            darknavParent.css('height', darknavParent.find('.navbar-start').outerHeight() + 'px');
            darknavParent.removeClass('dark-subnav--showing');
            $('.dark-subnav-slick-slider').addClass('slider-initialized');
        }, 5);

        setTimeout(function() {
            darknavParent.css('height', '');
        }, 255);
  });

    var itemsCount, activeItem, activeItemDiv, currentHtmlElement, totalHtmlElement,
        prev, next, controls, expiresVal, description, mobileDescription, mobileAlertCarousel, slideCount,
        darkSubnavSlider = $('.dark-subnav-slick-slider').slick({
            infinite: true,
            fade: true,
            slidesToShow: 1,
            slidesToScroll: 1,
            dots: false,
            prevArrow: null,
            nextArrow: null,
            autoplay: true,
            autoplaySpeed: 7000,
            rows: 0,
            responsive: [
                {
                    breakpoint: 1024,
                    settings: "unslick"
                }
            ]
        });

    function toggleButtonDisable(activeItem) {
        if (activeItem === 1 && itemsCount !== 1) {
            $('.navbar-menu .icon-caret-left').addClass('disabled ptrEventNone');
            $('.navbar-menu .icon-caret-right').removeClass('disabled ptrEventNone');
        } else if (activeItem === itemsCount && activeItem !== 1 && itemsCount !== 1) {
            $('.navbar-menu .icon-caret-left').removeClass('disabled ptrEventNone');
            $('.navbar-menu .icon-caret-right').addClass('disabled ptrEventNone');
        } else if (activeItem === 1 && itemsCount === 1) {
            $('.navbar-menu .icon-caret-left').addClass('disabled ptrptrEventNone');
            $('.navbar-menu .icon-caret-right').addClass('disabled ptrEventNone');
        } else {
            $('.navbar-menu .icon-caret-left').removeClass('disabled ptrEventNone');
            $('.navbar-menu .icon-caret-right').removeClass('disabled ptrEventNone');
        }
    }

    $('.dark-subnav-slick-next').on('click', function () {
        darkSubnavSlider.slick('slickNext');
        activeItem = $('.navbar-item').find('.dark-subnav-slick-slider .slick-track > .slick-active').index() + 1;
        currentHtmlElement = $('.navbar-item').prev('.steps-section').find('span.current');
        currentHtmlElement.text(activeItem);
        toggleButtonDisable(activeItem);
    });

    $('.dark-subnav-slick-prev').on('click', function () {
        darkSubnavSlider.slick('slickPrev');
        activeItem = $('.navbar-item').find('.dark-subnav-slick-slider .slick-track > .slick-active').index() + 1;
        currentHtmlElement = $('.navbar-item').prev('.steps-section').find('span.current');
        if (activeItem > '0') {
            currentHtmlElement.text(activeItem);
        }
        toggleButtonDisable(activeItem);
    });

    function carouselPagination(carousel, itemsCount) {
        currentHtmlElement = $('.navbar-menu .steps-section').find('span.current');
        totalHtmlElement = $('.navbar-menu .steps-section').find('span.total');
        activeItem = $(carousel).find('.dark-subnav-slick-slider .slick-track > .slick-active').index() + 1;
        activeItemDiv = $(carousel).find('.dark-subnav-slick-slider .slick-track > .slick-active');
        prev = $('.navbar-menu .steps-section').find('a.dark-subnav-slick-prev');
        next = $('.navbar-menu .steps-section').find('a.dark-subnav-slick-next');
        totalHtmlElement.text(itemsCount);
        currentHtmlElement.text(activeItem);
        // disabled previous arrow if item is first
        if (activeItem === 1) {
            prev.find('.icon-caret-left').addClass('disabled ptrEventNone');
        } else {
            prev.find('.icon-caret-left').removeClass('disabled ptrEventNone');
        }
        // disabled next arrow if item is last
        if (activeItem === itemsCount) {
            next.find('.icon-caret-right').addClass('disabled ptrEventNone');
            if ($(activeItemDiv).hasClass('slick-current')) {
                $(activeItemDiv).addClass('disabled ptrEventNone');
            }
        } else {
            next.find('.icon-caret-right').removeClass('disabled ptrEventNone');
            if ($(activeItemDiv).hasClass('slick-current')) {
                $(activeItemDiv).removeClass('disabled ptrEventNone');
            }
        }
    }

    $(".navbar-item.text-head-content").each(function () {
        itemsCount = $(this).find('.dark-subnav-slick-slider .slick-track > .slick-slide').length;
        if (itemsCount) {
            carouselPagination($(this), itemsCount);
        }
    });

    $('.dark-subnav-slick-slider').on('afterChange', function (event, slick, currentSlide, nextSlide) {
        $('.steps-text .current').text(currentSlide + 1);
    });

    mobileAlertCarousel = $('.register-mobile-nav');
    mobileAlertCarousel.not('.slick-initialized');

    function setSlideCount(slideCount) {
        var $el = $('.sliding-content').find('.m-total-count');
        $el.text(slideCount);
    }

    mobileAlertCarousel.on('init', function (event, slick) {
        $('.register-nav-sizecontrol').removeClass('register-nav-sizecontrol--initializing');
        setTimeout(function() {
            $('.register-mobile-nav').addClass('slick-initialized');
            $('.register-nav-sizecontrol').css('height', $('.register-mobile-nav').outerHeight() + 'px');
        }, 200);
        slideCount = slick.slideCount;
        setSlideCount(slideCount);
    });
    
    mobileAlertCarousel.slick({
        arrows: false,
        initialSlide: 0,
        speed: 500,
        autoplaySpeed: 0,
        cssEase: "linear",
        slidesToShow: 1,
        slidesToScroll: 1,
        centerMode: false,
        infinite: false,
        rows: 0
    });    

    description = $('.slick-slide.item > div > p');
    description.each(function (index, value) {
        $clamp(value, {clamp: 2});
    });

    mobileDescription = $('.register-mobile-nav .sliding-content .clamp-text');
    mobileDescription.each(function (index, value) {
        $clamp(value, {clamp: 1});
    });
});
