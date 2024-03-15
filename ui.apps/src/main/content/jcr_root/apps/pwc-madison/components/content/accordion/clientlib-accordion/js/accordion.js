/**
 * Accordion Component
 */
$(document).ready(function() {
    //Add Inactive Class To All Accordion Headers
    //MD-2624 related content on mobile

    var $accordionContainer = $(),
        $accordionItems, paths = [],
        getAllRelatedItems, processRcl,
        initDigital, populateDigitalDataOnLoad, getQuestion, getPanelContentTitle, getPanelBodyTitle, addDigitalData, getData, addBaseTopic, runmode, checkPanelStatus, rclItemsSize;

    // if desktop
    if(window.matchMedia("(min-width: 1088px)").matches){
        $accordionContainer = $(".main-body-content .accordion-container");
    }
    // else mobile
    else{
        $accordionContainer = $("#animatedToolesModalNew .accordion-container");
    }
    
    rclItemsSize = $accordionContainer.find("[data-lazy-load]").length;
	
	//This is For Disabling the enitre Right Panel
    checkPanelStatus = function() {
        if ($('.panel-remove').length !== 6 && !($('.main-body-content').hasClass('show_right_rail'))) {
            $('.main-body-content').addClass('show_right_rail');
            $('.toc-mobile-bar').addClass('show-rcl');
            
        }
        if ($('.main-body-content').hasClass('show_right_rail') && window.sessionStorage.getItem("_RCLwidthInPX") !== null) {
            var RCLxPercent = window.sessionStorage.getItem('_RCLdragWidthFullScreen'),
            RCLwidth =  window.sessionStorage.getItem('_RCLdragWidth'),accordianItem = $('.accordion-item'),
            RCLwidthInPX =  window.sessionStorage.getItem('_RCLwidthInPX');
    
            $(".spacer-line").css("left", RCLwidth + "%");  //For spacer line left position in %
            window.RCLside1.css("width", RCLwidthInPX + "px");  //RCL searchfilter width in PX
            window.RCLside2.css("width","100%");  //Content page width
            $('.sticky-right-content, .slider-items').css("width", (RCLwidthInPX -20) + "px");  //RCL sticky-right-content width in PX
             //To refresh RCL slickslider
            window.slickRefresh();
            $('.content-page-nav').css('width', RCLxPercent + "%");   //To change Prev & Next btn width when dragging TOC btn               
        }
        if(window.rclStateCheck){
            if ($('.main-body-content').hasClass('show_right_rail') && window.sessionStorage.getItem("RCL_open") === 'false' && !$('.right_rail_toggle .cross_image').hasClass('plus_sign')){
                // code to hide RCL
                $('.right_rail_toggle .cross_image').addClass('plus_sign');
                $('.right_rail_toggle').addClass('hide_arrow');
                $('.content_page .search-filters').hide();
              } 
        }
    
    };

    runmode = undefined === $("#run-mode").val() ? "publish" : $("#run-mode").val();

    $accordionItems = $accordionContainer.find(".accordion-item");

    $accordionItems.addClass("panel");

    $(".search-filters .accordion-container").find(".accordion-item").first().addClass("related-content-dd");
    $(".search-filters .accordion-container").find(".accordion-item").eq(1).addClass("example-dd");
    $(".search-filters .accordion-container").find(".accordion-item").eq(2).addClass("template").addClass("template-dd");
    $(".search-filters .accordion-container").find(".accordion-item").eq(3).addClass("faq-dd");
    $(".search-filters .accordion-container").find(".accordion-item").eq(4).addClass("industry-insight").addClass("industry-dd");
    $(".mobile-table-content .accordion-container").find(".accordion-item").first().addClass("mobile-related-content-dd");
    $(".mobile-table-content .accordion-container").find(".accordion-item").eq(1).addClass("mobile-example-dd");
    $(".mobile-table-content .accordion-container").find(".accordion-item").eq(2).addClass("template").addClass("mobile-template-dd");
    $(".mobile-table-content .accordion-container").find(".accordion-item").eq(3).addClass("mobile-faq-dd");
    $(".mobile-table-content .accordion-container").find(".accordion-item").eq(4).addClass("industry-insight").addClass("mobile-industry-dd");
    
    /**
     * initilaize data layer
     */
    initDigital = function() {
        window.digitalData = window.digitalData || {};
        window.digitalData.page = window.digitalData.page || {};
        window.digitalData.rightNav = window.digitalData.rightNav || {};
    };

    getPanelContentTitle = function(element, clz) {
        if (element !== undefined) {
            var panelContent = element.find(".panel-content"),
                panelTitle;
            if (panelContent !== undefined && panelContent.length > 0) {
                panelTitle = panelContent.find(clz);
                try {
                    return $.trim(panelTitle !== undefined && panelTitle.length > 0 ? panelTitle.text() : "");
                } catch (error) {

                }
            }
        }
    };

    getPanelBodyTitle = function(element) {
        if (element !== undefined) {
            var panelContent = element.find(".panel-body"),
                panelTitle, contentType;
            if (panelContent !== undefined && panelContent.length > 0) {
                panelTitle = panelContent.find("span.panel-title");
                if (panelTitle !== undefined) {
                    contentType = "";
                    if (contentType !== undefined) {
                        contentType = panelTitle.contents().filter(function() {
                            return this.nodeType === 3;
                        });
                    }
                    return $.trim((contentType !== undefined && contentType.length > 0 && contentType[0].textContent !== undefined) ? contentType[0].textContent : "");
                }
            }
        }
    };

    getQuestion = function(element, clz) {
        if (element !== undefined) {
            var panelContent = element.find(".panel-content"),
                panelTitle, item, children;
            if (panelContent !== undefined && panelContent.length > 0) {
                panelTitle = panelContent.find(clz);
                try {
                    if (panelTitle !== undefined && panelTitle.length > 1) {
                        item = panelTitle[1];
                        if (item !== undefined) {
                            children = $(item).children(".bold");
                            if (children !== undefined && children.length > 1) {
                                return $.trim($(children[1]).text() !== undefined ? $(children[1]).text() : "");
                            }
                        }
                    }
                } catch (error) {

                }
            }
        }
    };

    getData = function(activeItems, key, tclz, fnc) {
        if (activeItems !== undefined && activeItems.length > 0) {
            window.digitalData.rightNav = {};
            window.digitalData.rightNav[key] = fnc(activeItems, tclz);
        }
    };

    addDigitalData = function(dclz, mclz, key, tclz, fnc) {
        if ($accordionContainer !== undefined) {
            var $relatedItems = $accordionContainer.find(dclz),
                activeItems;
            if ($relatedItems !== undefined && $relatedItems.hasClass("expanded")) {
                activeItems = $relatedItems.find(".slick-current.slick-active");
                getData(activeItems, key, tclz, fnc);
            }
            //mobile
            else {
                $relatedItems = $accordionContainer.find(mclz);
                if ($relatedItems !== undefined && $relatedItems.hasClass("expanded")) {
                    activeItems = $relatedItems.find(".slick-current.slick-active");
                    getData(activeItems, key, tclz, fnc);
                }
            }
        }
    };

    addBaseTopic = function() {
        if (window.digitalData.page && window.digitalData.page.content && window.digitalData.page.content.topic && !$.isEmptyObject(window.digitalData.rightNav)) {
            window.digitalData.rightNav.topic = window.digitalData.page.content.topic;
        }
    };

    /**
     * Capture right nav data on load
     */
    populateDigitalDataOnLoad = function() {
        var $relatedItems = $accordionContainer.find(".accordion-item.related-content-dd"),
            activeItems;
        if ($relatedItems !== undefined && $relatedItems.hasClass("expanded")) {
            activeItems = $relatedItems.find(".slick-current.slick-active");
            if (activeItems !== undefined && activeItems.length > 0) {
                window.digitalData.rightNav = {};
                window.digitalData.rightNav.relatedContentTopic = getPanelContentTitle(activeItems, ".panel-title");
                window.digitalData.rightNav.relatedContentCategory = getPanelBodyTitle(activeItems);
            }
        }
        //mobile
        else {
            $relatedItems = $accordionContainer.find(".accordion-item.mobile-related-content-dd");
            if ($relatedItems !== undefined && $relatedItems.hasClass("expanded")) {
                activeItems = $relatedItems.find(".slick-current.slick-active");
                if (activeItems !== undefined && activeItems.length > 0) {
                    window.digitalData.rightNav = {};
                    window.digitalData.rightNav.relatedContentTopic = getPanelContentTitle(activeItems, ".panel-title");
                    window.digitalData.rightNav.relatedContentCategory = getPanelBodyTitle(activeItems);
                }
            }
        }
        addDigitalData(".accordion-item.example-dd", ".accordion-item.mobile-example-dd", "examples", ".panel-title", getPanelContentTitle);
        addDigitalData(".accordion-item.template-dd", ".accordion-item.mobile-template-dd", "templateTools", ".panel-title", getPanelContentTitle);
        addDigitalData(".accordion-item.industry-dd", ".accordion-item.mobile-industry-dd", "industryInsight", ".at-a-glance", getPanelContentTitle);
        addDigitalData(".accordion-item.faq-dd", ".accordion-item.mobile-faq-dd", "faq", ".question-item", getQuestion);
        addBaseTopic();
    };

    getAllRelatedItems = function() {
        var deferred = $.Deferred(),
            refCallAjax = $.post("/bin/pwc-madison/validRelatedLinks.json", {
                path: paths.join(",")
            }, null, 'json');
        $.when(refCallAjax)
            .done(function(refCallAjaxRes) {
                if (refCallAjaxRes) {
                    $.each(refCallAjaxRes, function(index, path) {
                        if ($accordionContainer.find(".slider-items") && $accordionContainer.find(".slider-items").find("[data-lazy-load='" + path + "']").length > 0) {
                            try {
                                $accordionContainer.find(".slider-items").find("[data-lazy-load='" + path + "']").closest(".slider-items").remove();
                            } catch (error) {

                            }
                        }
                    });
                }

                deferred.resolve();
            }).fail(function(xhr, status, err) {
                deferred.reject(err);
            });
        return deferred.promise();
    };

    initDigital();

    processRcl = function() {
        $accordionItems.each(function() {
            var $accordionItem = $(this),
                $sliderItems = $accordionItem.find(".slider-items"),
                $slider, current, lazyLoadData, targetDD, initRightNavData,
                sliderSettings = {
                    focusOnSelect: false,
                    arrows: false,
                    dots: false,
                    adaptiveHeight: true
                };

            $accordionItem.find(".total-count").html($sliderItems.length);
            $accordionItem.find(".panel-title-width span").html("(" + $sliderItems.length + ")");

            $slider = $accordionItem.find('.slider-content');
            if ($sliderItems.length > 0) {
                $slider.on('init reInit afterChange beforeChange', function(event, slick, currentSlide, nextSlide) {
                    $('.sticky-right-content .accordion-content .panel-body').animate({ scrollTop: 0 }, "fast");
                    var $this = $(this),
                        nextElement;
                    if (event.type !== "afterChange") {
                        if (typeof nextSlide === "undefined") {
                            nextSlide = 0;
                        }
                        nextElement = $this.find('.slick-slide[data-slick-index=' + nextSlide + ']').not('.slick-cloned').find('[data-lazy-load]');
                        if (nextElement.children().length === 0) {
                            lazyLoadData(nextElement).done(function(data) {
                                nextElement.html(window.DOMPurify.sanitize(data));
                                setTimeout(function() {
                                    slick.setPosition();
                                }, 1000);
                            });
                        }
                    } else if (event.type === "afterChange") {
                        targetDD = $(event.currentTarget);
                        initRightNavData(targetDD);
                    }

                    current = (currentSlide ? currentSlide : 0) + 1;
                    $accordionItem.find(".dynamic-count").text(current);

                });
            }
            $('div[id*=r-content-loader]').each(function(index,element){
                $(element).hide();
            });
            $(".sticky-right-content").removeClass("r-op");

            /**
             * Capture right nav data
             * @param {*} target 
             */
            initRightNavData = function(target) {
                if (target !== undefined) {
                    var item = target.closest(".accordion-item"),
                        activeItems;
                    if (item !== undefined) {
                        if ((item.hasClass("related-content-dd") || item.hasClass("mobile-related-content-dd")) && item.hasClass("expanded")) {
                            activeItems = target.find(".slick-current.slick-active");
                            if (activeItems !== undefined && activeItems.length > 0) {
                                window.digitalData.rightNav = {};
                                window.digitalData.rightNav.relatedContentTopic = getPanelContentTitle(activeItems, ".panel-title");
                                window.digitalData.rightNav.relatedContentCategory = getPanelBodyTitle(activeItems);
                            }
                        } else if ((item.hasClass("example-dd") || item.hasClass("mobile-example-dd")) && item.hasClass("expanded")) {
                            activeItems = target.find(".slick-current.slick-active");
                            getData(activeItems, "examples", ".panel-title", getPanelContentTitle);
                        } else if ((item.hasClass("template-dd") || item.hasClass("mobile-template-dd")) && item.hasClass("expanded")) {
                            activeItems = target.find(".slick-current.slick-active");
                            getData(activeItems, "templateTools", ".panel-title", getPanelContentTitle);
                        } else if ((item.hasClass("industry-dd") || item.hasClass("mobile-industry-dd")) && item.hasClass("expanded")) {
                            activeItems = target.find(".slick-current.slick-active");
                            getData(activeItems, "industryInsight", ".at-a-glance", getPanelContentTitle);
                        } else if ((item.hasClass("faq-dd") || item.hasClass("mobile-faq-dd")) && item.hasClass("expanded")) {
                            activeItems = target.find(".slick-current.slick-active");
                            getData(activeItems, "faq", ".question-item", getQuestion);
                        }
                    }
                    addBaseTopic();
                }
            };

            lazyLoadData = function($element) {
                return $.ajax({
                    url: $element.data('lazyLoad'),
                    async: false
                });
            };

            if ($sliderItems.length === 0) {
                $accordionItem.addClass('panel-remove');
				
            }
            else{
                $accordionItem.find(".panel-title").css('display', 'flex');
                $accordionItem.find(".panel-title").parent().css('display', 'flex');
                checkPanelStatus();
            }


            if ($sliderItems.length < 1) {
                $accordionItem.find(".panel-title").removeClass("has-pagination");
            }

            if ($sliderItems.length === 1) {
                $accordionItem.find(".panel-paginate.is-pulled-left").hide();
            }

            $slider.slick(sliderSettings);
            $accordionItem.addClass("sliderInitialized");
            $('.panel-paginate.is-pulled-left').css("display", "none");

            $accordionItem.find(".accordion-header").click(function(e) {
				e.preventDefault();
                $('.main-body-content ul').parent('li').addClass('no-bullet');
                $('.main-body-content ol').parent('li').addClass('no-bullet');
                if ($(this).parents(".accordion-item").hasClass("expanded")) {
                    $(this).find(".item-count-heading").css("display", "inline-block");
					$(this).find(".panel-paginate.is-pulled-left").css("display", "none");
					$(this).find(".icon > span").removeClass("icon-caret-up");
					$(this).find(".icon > span").addClass("icon-caret-down");
                } else {
                    $(this).find(".item-count-heading").css("display", "none");
					$(this).find(".panel-paginate.is-pulled-left").css("display", "flex");
					$(this).find(".icon > span").removeClass("icon-caret-down");
					$(this).find(".icon > span").addClass("icon-caret-up");
                    if ($sliderItems.length === 1) {
                        $(this).find(".panel-paginate.is-pulled-left").css("display", "none");
                    }
                }
                var $elm = $(e.target);
                $accordionItems.not($elm.parents(".accordion-item")).removeClass("expanded");
                // manage arrow icon MD-16704
                $accordionItems.not($elm.parents(".accordion-item")).find(".icon-caret-up").removeClass("icon-caret-up").addClass("icon-caret-down");
                // end
                $accordionItems.not($elm.parents(".accordion-item")).find(".item-count-heading").css("display", "inline-block");
                window.digitalData.rightNav = {};
                $(this).parents(".accordion-item").toggleClass("expanded");
                //$slider.slick(sliderSettings);

                $slider.slick('setPosition');
                populateDigitalDataOnLoad();
            });

            $accordionItem.find(".panel-paginate a.arrow").click(function(e) {
                e.stopPropagation();
                e.preventDefault();
                var $elm = $(e.target);
                if ($elm.hasClass("relatedCarouselPrevious")) {
                    $slider.slick("slickPrev");
                } else if ($elm.hasClass("relatedCarouselNext")) {
                    $slider.slick("slickNext");
                }
                $('.main-body-content ul').parent('li').addClass('no-bullet');
                $('.main-body-content ol').parent('li').addClass('no-bullet');
            });

        });

    };

    (function() {
        if ("publish" === runmode && rclItemsSize) {
            $accordionItems.each(function() {
                var $allSliderItems = $(this).find(".slider-items");
                if ($allSliderItems) {
                    $allSliderItems.each(function() {
                        var $item = $(this);
                        if ($item && undefined !== $item.find('[data-lazy-load]')) {
                            paths.push($item.find('[data-lazy-load]').data('lazy-load'));
                        }
                    });
                }

            });
            getAllRelatedItems().then(function() {
                    processRcl();
                },
                function(error) {
                    processRcl();
                });
        } else {
            processRcl();
        }
    }());
	
});
