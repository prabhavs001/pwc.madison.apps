(function (document, $, GatedContent) {

    var ExternalRedirect = {}, redirectGuide;

    function initRedirectLanding(i18nMessage) {
        if ($('.redirect-guide-target').length > 0) {
            $(window).scrollTop(0);
            redirectGuide = $.guide({
                actions: [{
                    element: $('.redirect-guide-target'),
                    content: '<p class="redirect-guide">' + i18nMessage + '</p>',
                    offsetX: -315,
                    offsetY: -4
                }]
            });

            if($(window).height() < 700 || $(window).width() < 850) {
                $('html, body').animate({
                    scrollTop: $(".navbar.subnav").offset().top - 80
                }, 1000);
            } else {
                $("html").addClass("page-overflow-hidden");
            }

            setTimeout(function () {
                redirectGuide.draw();
                $(".redirect-modal").addClass("modal-active");
                $(".redirect-modal").css('opacity', '');
            }, 600);

            return redirectGuide;
        }
        return false;
    }

    function closeNeModal() {
        $(".ne-modal-container").removeClass("modal-active");
        $("html").removeClass("page-overflow-hidden");
        if($('#gatedModal').length) {
			GatedContent.checkGatedContent();
        }
    }

    if ($(".ne-modal-container").length > 0) {
        $(".close-neModal").on("click", function () {
            closeNeModal();
            if (typeof redirectGuide !== 'undefined' && redirectGuide !== false) {
                redirectGuide.exit();
            }
        });
    }

    $(window).on('resize', function () {
        if ($(window).height() < 700) {
            $(".redirect-modal .ne-modal-body").addClass("modal-mobile");
        } else {
            $(".redirect-modal .ne-modal-body").removeClass("modal-mobile");
        }
    });

    if ($(window).height() < 700) {
        $(".redirect-modal .ne-modal-body").addClass("modal-mobile");
    }

    ExternalRedirect.checkExternalRedirect = function () {

        var dom, informDomain, territories, i18nMessage, currentTerritory, referrerDomain;

        dom = document.getElementById('extDom');
        informDomain = dom.getAttribute('data-informDomain');
        territories = dom.getAttribute('data-territories').split(',');
        i18nMessage = dom.getAttribute('data-i18n-message');
        currentTerritory = $("input[name=pageTerritoryCode]").val();
        referrerDomain = document.referrer;

        if (referrerDomain && referrerDomain.indexOf(informDomain) !== -1 && territories.indexOf(currentTerritory) !== -1) {
            redirectGuide = initRedirectLanding(i18nMessage);
        }
        else {
            GatedContent.checkGatedContent();
        }

    };

    window.ExternalRedirect = ExternalRedirect;


}(document, $, window.GatedContent));