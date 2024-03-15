$(document).ready(function () {

    var readLessText, readMoreText, elem, headRows, checkHeadRows, maxHeight = 73;

    function templateInfoInit(maxTextHeight) {

        var contentParagraph = $('.template-info-text p'),
            readMore = $('.read-more-action'), contentHeight, checkIsExpanded;

        if (contentParagraph.length > 0) {
            contentParagraph.css('max-height', '');
            contentHeight = contentParagraph.css('height', 'auto').innerHeight();
            checkIsExpanded = readMore.hasClass('is-expanded') === false ? contentParagraph.css('max-height', maxTextHeight + 'px') : '';
            if (window.parseInt(maxTextHeight) < window.parseInt(contentHeight)) {
                readMore.removeClass("is-hidden");
            } else {
                readMore.addClass("is-hidden");
            }
        }
    }

    $(window).on('resize orientationchange', function () {
        $('.overview-section').css('height', "");
        templateInfoInit(maxHeight);
    });

    //-----------Read More Expand/Shrink----------
    $(".read-more-action").on('click', function () {
        $('.overview-section').css('height', "");
        readLessText = $(this).find("i").data("readless-text");
        readMoreText = $(this).find("i").data("readmore-text");
        elem = $(this).attr("data-action");
        var contentPar = $('.template-info-text p');
        contentPar.css('max-height', '');
        if (elem === readMoreText) {
            //Stuff to do when btn is in the read more state
            $('.read-more-action').addClass("is-expanded");
            $(this).find("i").text(readLessText);
            $(this).parent().find("p").css("height", "auto");
            $(this).attr("data-action", readLessText);
            $(this).find(".icon  span").removeClass("icon-caret-down");
            $(this).find(".icon  span").addClass("icon-caret-up");
        } else {
            //Stuff to do when btn is in the read less state
            $('.read-more-action').removeClass("is-expanded");
            contentPar.css('max-height', maxHeight + 'px');
            $(this).find("i").text(readMoreText);
            $(this).attr("data-action", readMoreText);
            $(this).find(".icon  span").removeClass("icon-caret-up");
            $(this).find(".icon  span").addClass("icon-caret-down");

        }
    });
    
    // get window width
    function windowWidth() {
        var winWidth = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
        return winWidth;
    }
    
    if(windowWidth() > 768) {
	    headRows = $($('.template_info h1')[0]).text().length;
	    if(windowWidth() > 768) {
            if(headRows > 30 && headRows < 39) {
                $('.template-info-text').css('max-width', '55%');
                maxHeight = 70;
            }
            if(headRows > 40 && headRows < 49) {
                $('.template-info-text').css('max-width', '40%');
                maxHeight = 70;
            }
            if(headRows > 50 && headRows < 59) {
                $('.template-info-text').css('max-width', '45%');
                maxHeight = 162;
            }
            if(headRows > 60 && headRows < 69) {
                $('.template-info-text').css('max-width', '40%');
                maxHeight = 208;
            }
            if(headRows > 73) {
                $('.template-info-text').css('max-width', '35%');
                maxHeight = 232;
            }}
    }

    templateInfoInit(maxHeight);

    setTimeout(function() {
        $('.overview-section').removeClass('template_info--initializing');
        $('.overview-section').css('height', ($('section.overview-section > .container').outerHeight() + 48) + 'px');
    }, 50);

    setTimeout(function() {
       $('.overview-section').addClass('template_info--show');
    }, 200);

});
