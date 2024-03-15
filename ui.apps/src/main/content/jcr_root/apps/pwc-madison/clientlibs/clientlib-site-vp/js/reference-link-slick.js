$(document).ready(function () {
    var helpfulLinksMobile, helpfulLinks, elements, elementsToHide, showMoreLink, showMoreLinkText, showLessLinkText, helpfulLinksDesktop, menuItems, readLessText, readMoreText, elem, menuItem, i;
    helpfulLinksMobile = {
        match: function() {
            $('.container .helpful_links').each(function(index, component) {
                helpfulLinks = $(component).find('.helpful-link-items > div');
                elements = helpfulLinks.length;
                if (elements > 4) {
                    elementsToHide = helpfulLinks.slice(4 - elements);
                    setTimeout(function () {
                        elementsToHide.hide();
                    }, 300);
                    showMoreLink = $(component).find('.show-more-link .show-more-action');
                    showLessLinkText = $(showMoreLink).data('readless-text');
                    showMoreLinkText = $(showMoreLink).data('readmore-text');
                    showMoreLink.on('click', function () {
                        if (elementsToHide.is(":visible")) {
                            elementsToHide.hide();
                            showMoreLink.find('.text').text(showMoreLinkText);
                            showMoreLink.find('.icon').html("<span class='icon-caret-down'>");
                        } else {
                            helpfulLinks.show();
                            showMoreLink.find('.text').text(showLessLinkText);
                            showMoreLink.find('.icon').html("<span class='icon-caret-up'>");
                        }
                    });
                }
                else {
                    $(component).find('.show-more-link .show-more-action').hide();
                }
            });
        },
        unmatch: function() {
            helpfulLinks = $('.helpful-link-items div');
            helpfulLinks.show();
        }
    };

    helpfulLinksDesktop = {
        match: function() {
            $('.helpful_links.carousel .helpful-link-items').slick({
                arrows: true,
                speed: 500,
                autoplay: false,
                autoplaySpeed: 100,
                cssEase: 'linear',
                slidesToShow: 4,
                slidesToScroll: 3,
                variableWidth: true,
                prevArrow: "<span class='icon-caret-left' />",
                nextArrow: "<span class='icon-caret-right' />",
                responsive: [
                    {
                        breakpoint: 769,
                        settings: "unslick"
                    }
                ]
            });
        }
    };
    enquire.register("(min-width: 320px) and (max-width: 768px)",[helpfulLinksMobile]).register("(min-width: 769px) and (max-width: 3840px)",[helpfulLinksDesktop]);
    function showhide(total,type) {

        if(type === "hide"){
            for(i=4;i<menuItems.length;i++){
                menuItem = menuItems[i];
                $(menuItem).addClass("is-hidden");
            }

        }
        else{
            for(i=4;i<menuItems.length;i++){
                menuItem = menuItems[i];
                $(menuItem).removeClass("is-hidden");
            }
        }

    }
    if(window.innerWidth < 767) {
        menuItems = $(".menu-items .menu-item:not(.search-menu)");
        if (menuItems.length > 4) {
            $(".menu-show-more-items").removeClass("is-hidden");
            showhide(menuItems.length, "hide");
        }
        $(".menu-show-more-items a.read-more").click(function () {
            readLessText = $(this).find("i").data("readless-text");
            readMoreText = $(this).find("i").data("readmore-text");
            elem = $(this).attr('data-action');
            if (elem === readMoreText) {
                $(this).find('i').text(readLessText);
                $(this).attr('data-action', readLessText);
                $(this).find(".icon  span").removeClass("icon-caret-down");
                $(this).find(".icon  span").addClass("icon-caret-up");
                showhide(menuItems.length, "show");
            } else {
                $(this).find('i').text(readMoreText);
                $(this).attr('data-action', readMoreText);
                $(this).find(".icon  span").removeClass("icon-caret-up");
                $(this).find(".icon  span").addClass("icon-caret-down");
                showhide(menuItems.length, "hide");
            }
        });
    }
});