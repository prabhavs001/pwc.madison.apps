var guide, tourPerc, total_step_perc, imgtutorial, prevEl, MADISON_TUTORIAL_COOKIE_NAME = "tutorial_visited", isTutorialLinkClicked = false;

function eachTutorial(element) {
    imgtutorial = ['#search_option', '#menu_option', '#signin_option', '#suggested_option', '#contentmouse_option', '#displaycontent_option', '#contentpage_option', '#relatedlink_option', '#prevandafter_option', '#trending_option', '#searchicon_option', '#search_option', '#feedback_option', '#end_option'];
    return {
        element: $(element),
        content: '',
        isBeforeFuncExec: true,
        beforeFunc: function(g) {
            var el = $(element),
                elOffset, elHeight, windowHeight, offset, gifimg, gifsrc;

            elOffset = el.offset().top;
            elHeight = el.innerHeight();
            windowHeight = $(window).innerHeight();

            if (imgtutorial.indexOf(element) !== -1) {
                    gifimg = $(el).attr('data-gif');
                    gifsrc = $(el).attr('data-src');
                if (gifimg) {
                    $(el).attr('src', gifimg);
                } else if(gifsrc) {
                  $(el).attr('src', gifsrc);
                }
                if(typeof prevEl !== 'undefined') {
                  prevEl.css('display', '').css('opacity', '');
                }
                el.attr('src', el.attr('src'));
                el.css('display', 'inline').css('opacity', 0);
                setTimeout(function () {
                    if (elHeight > (windowHeight - 80)) {
                        $(el).css('top', 0);
                    } else {
                        $(el).css('top', '');
                    }
                    el.css('opacity', 1);
                    g.execAction();
                }, 250);
            }
            
            if (elHeight < windowHeight) {
                offset = elOffset - ((windowHeight / 2) - (elHeight / 2));
            } else {
                offset = elOffset - 80;
            }
            
            prevEl = el;

            $('html, body').animate({
                scrollTop: offset
            }, 0);
        },
        successFunc: function (g) {
            setTimeout(function () {
                guide.draw();
            }, 800);
        }
    };
}

function tourPercentage() {
    total_step_perc = Math.round((guide.actionList.length) * 100);
    tourPerc = Math.round(((guide.step.current + 1) / (guide.actionList.length)) * total_step_perc);

    $(".sitetour_progress").val(tourPerc);
    $(".sitetour_progress").attr("max", total_step_perc);
    if (tourPerc === total_step_perc) {
        $("#nextGuideBtn").prop("disabled", true);
    } else {
        $("#nextGuideBtn").prop("disabled", false);
    }
    if (tourPerc === 100) {
        $("#prevGuideBtn").prop("disabled", true);
    } else {
        $("#prevGuideBtn").prop("disabled", false);
    }
}

function domoGuide() {

    guide = $.guide({
        actions: [
            eachTutorial('#signin_option'),
            eachTutorial('#menu_option'),
            eachTutorial('#suggested_option'),
            eachTutorial('#contentmouse_option'),
            eachTutorial('#displaycontent_option'),
            eachTutorial('#contentpage_option'),
            eachTutorial('#relatedlink_option'),
            eachTutorial('#prevandafter_option'),
            eachTutorial('#trending_option'),
            eachTutorial('#searchicon_option'),
            eachTutorial('#search_option'),
            eachTutorial('#feedback_option'),
            eachTutorial('#end_option')
        ]
    });

}

function next() {
    guide.next();
    tourPercentage();
}

function prev() {
    guide.back();
    tourPercentage();
}

function createCookie() {
    var date, d, secure;
    date = new Date();
    d = date.getDate();
    date.setMonth(date.getMonth() + 6);
    //takes care for edge case for rolling year and varying month lengths
    if (date.getDate() !== d) {
        date.setDate(0);
    }
    secure = location.protocol === "https:";
//    $.cookie(MADISON_TUTORIAL_COOKIE_NAME, true, { expires: date, path: '/', secure: secure });
}

function startTour() {
    createCookie();
    $("body").addClass("is-guide");
    $(".sitetour_pop").removeClass("is-active");
    $(".sitetour_arrows").removeClass("is-hidden");
    $(".sitetour_arrows").addClass("is-block");
    //$(".sitetour_arrows").toggleClass("is-block is-hidden");
    domoGuide();
    tourPercentage();
}

function showRedirect(ExternalRedirect) {
    ExternalRedirect.checkExternalRedirect();
}

function endTour() {
    createCookie();
    $("body").removeClass("is-guide");
    $(".hideslide").css('display', 'none');
    $(".sitetour_pop").removeClass("is-active");
    $('.video-container').removeClass('active');
    $('.sitetour_pop').removeClass('video-dimmer');
    $('.guide-start-2').removeClass('active');
    $('#search_option,.sitetour_pop').fadeOut();
    $(".sitetour_arrows").addClass("is-hidden");
    $(".sitetour_arrows").removeClass("is-block");
    if (guide !== undefined) {
        guide.exit();
    }
    if (!isTutorialLinkClicked) {
        showRedirect(window.ExternalRedirect);
    }

}

function initTutorial() {
    $('.sitetour_pop').addClass('is-active');
    $('.guide-start').addClass('active');
    $('#guideLanguageSelect').on('click', function () {
        $('.guide-start').removeClass('active');
        setTimeout(function () {
            $('.guide-start-2').addClass('active');
        }, 150);
    });

    //English Tutorial
    $('#startEnTutorial').on('click', function () {
        startTour();
    });

    //French Tutorial
    $('#startFrTutorial').on('click', function () {
        createCookie();
        var frenchLeaveLabel = $('.video-container button.end-tour').attr('data-fr-leave-label');
        $('.guide-start-2').removeClass('active');
        $('.video-container video source.mp4').attr('src', $('.video-container video').attr('data-fr-mp4'));
        $('.video-container > button').text(frenchLeaveLabel);
        setTimeout(function () {
            $('.video-container video')[0].load();
            $('.video-container').addClass('active');
            $('.sitetour_pop').addClass('video-dimmer');
        }, 150);
    });

    //Japanese Tutorial
    $('#startJpTutorial').on('click', function () {
        createCookie();
        var japaneseLeaveLabel = $('.video-container button.end-tour').attr('data-jp-leave-label');
        $('.guide-start-2').removeClass('active');
        $('.video-container video source.mp4').attr('src', $('.video-container video').attr('data-jp-mp4'));
        $('.video-container > button').text(japaneseLeaveLabel);
        setTimeout(function () {
            $('.video-container video')[0].load();
            $('.video-container').addClass('active');
            $('.sitetour_pop').addClass('video-dimmer');
        }, 150);
    });

    $('.end-tour').on('click', function (e) {
        e.preventDefault();
        endTour();
    });

    $('.close-tutModal').on('click', function () {
        endTour();
    });

}

//show tutorial modal only if the user has not visited the tutorial yet
(function () {
    showRedirect(window.ExternalRedirect);
    $('#tutorial-link').click(function () {
        isTutorialLinkClicked = true;
        initTutorial();
    });

}());
