//Global RCL declaration
var RCLdragging = false,RCLside1,RCLside2,RCLxPercent,RCLwidth,rclStateCheck=false,RCLwidthInPX;
RCLside1 = $(".search-filters");
RCLside2 = $(".content-page-container");


//To refresh the RCL slick
function slickRefresh() {
    var accordianItem = $('.accordion-item');
    accordianItem.each(function() {
        if ($(this).hasClass('expanded')) {
            if ($(this).find('.slider-content').hasClass("slick-initialized")) {
                $(".slider-content").addClass('is-faded');
                // setTimeout(function() {
                    $('.accordion-item.expanded .slider-content')[0].slick.setPosition();
                // }, 200);
            }
        }
    });
}

//Only to refresh RCL slick on expanding item
$('.icon.is-pulled-right').click(function () {
    slickRefresh();
});


/*global tippy */

// ---------------------------------------------------------------------
// --------- Polyfill for CustomEvent() - Required for IE---------------
// ---------------------------------------------------------------------
(function () {

  if ( typeof window.CustomEvent === "function" ) { return false; }

  function CustomEvent ( event, params ) {
    params = params || { bubbles: false, cancelable: false, detail: null };
    var evt = document.createEvent( 'CustomEvent' );
    evt.initCustomEvent( event, params.bubbles, params.cancelable, params.detail );
    return evt;
   }

  window.CustomEvent = CustomEvent;
}());

function clamp(selector, noOfLines) {
    var limitedword = $(selector);
    limitedword.each(function (index, value) {
        $clamp(value, {
            clamp: noOfLines
        });
    });
}

// ---------------------------------------------------------------------
// -------------------- Character Limit Function -----------------------
// ---------------------------------------------------------------------

function trimText(elem, desktopLim, mobileLim, ipadLim) {

    var isMob = window.matchMedia("only screen and (max-width: 767px)").matches,
        maxLength = false,
        elemText, trimmed;

    if ($(window).outerWidth() === 768) {
        maxLength = ipadLim;
    } else if (isMob === true) {
        maxLength = mobileLim;
    } else {
        maxLength = desktopLim;
    }

    $(elem).each(function(index, value) {
        if (typeof($(value).attr('data-text')) === 'undefined') {
            $(value).attr('data-text', $(value).text());
        }

        elemText = $(value).attr('data-text');

        if (elemText.length > maxLength && elem.parents(".cp-nav-item").length <= 0) {
            trimmed = elemText.substring(0, maxLength).concat("...");
            $(value).text(trimmed);
        }
        else if (elemText.length > maxLength && elem.parents(".cp-nav-item").length > 0) {
            clamp($(value), 1);
        }
        
    });

    return false;
}

function calcWidth(e) {
    setTimeout(function() {
  
        var fullPageContainer = $('#content-page-full-container')[0].getBoundingClientRect(),
           tocActualWidth = parseFloat( parseFloat($(".toc-content").css('width')) - fullPageContainer.left),
           tocActualWidthPercent = 100 - ( ( tocActualWidth / parseFloat($("#content-page-full-container").css('width')) ) * 100 );
        $(".content-page-container").css("width", tocActualWidthPercent + "%");
        if (window.location.href.indexOf('joined.html') !== -1 ) {
            $('#navbar-toc').css("width", tocActualWidthPercent + "%");
          $('#navbar-toc').css("margin-left", 'auto');
          $('#navbar-toc > .toc-action-navbar').css("width","auto");
          }
    }, 200);
  }

//----------------------------------------------
//------------ Addresses MD-11429 --------------
//----------------------------------------------

// For mobile view navbar & toolbar, when we scroll up & down 
function tocMenuFix(scrollDirec) {
	if($('.global-navbar.subnav').length > 0) {
		var rect = $('.navbar-madison.subnav')[0].getBoundingClientRect(),
			margin = rect.bottom,
            scrollTop=$(window).scrollTop(),
			fixHeight = $('.navbar-message--mobile').length > 0 ? $('.fix-menu-onscroll').outerHeight() + $('.navbar-message--mobile').outerHeight() : $('.fix-menu-onscroll').outerHeight();

        if(window.matchMedia("only screen and (max-width: 768px)").matches){
            if(margin > -4) {
                $('.toc-mobile-bar').css('position', 'absolute').css('margin-top', fixHeight + 'px');
                //--- Removing classname to the mobile menu when it is not sticky ---//
                $('.toc-mobile-bar').removeClass('stickymob');
            } else {
                $('.toc-mobile-bar').css('position', 'fixed').css('margin-top', '');
                 //--- Adding classname to the mobile menu when it is sticky ---//
                 $('.toc-mobile-bar').addClass('stickymob');
            }
        }else{
        if(margin > -4) {
                if(scrollDirec==='initial'){
                $('.toc-mobile-bar').css('top', fixHeight + 'px');
                }
	        //--- Removing classname to the mobile menu when it is not sticky ---//
	        $('.toc-mobile-bar').removeClass('stickymob');
		} else {
			$('.toc-mobile-bar').css('position', 'fixed').css('top', '');
			 //--- Adding classname to the mobile menu when it is sticky ---//
		     $('.toc-mobile-bar').addClass('stickymob');
		}
        if(scrollDirec==='up'){
            $('.toc-mobile-bar').css('top', fixHeight + 'px');
        }
        if(scrollDirec==='down'){
             if(fixHeight-scrollTop > 0){
                $('.toc-mobile-bar').css('top', fixHeight-scrollTop + 'px');
            }
        }
    }
	}

	return false;
}

// To make navbar & toolbar fix to top when we scroll up & down

var lastScrollTop = 0;
function adjustGlobalHeader(){
// To make navbar & toolbar fix to top when we scroll up & down
if(window.matchMedia("only screen and (min-width: 769px)").matches){
// if ($('#content-page-full-container').length > 0) {
    var scrollTop = $(this).scrollTop();

    if(scrollTop === 0){
        $("#navbar-toc").removeClass("sticky-toc");  //For toolbar
        $("#navbar-toc .toc-action-navbar").removeClass("container");  //For toolbar
        $(".stick-nav-title").hide(); $('.fix-menu-onscroll').removeClass('fix-top-onscroll');  //For navbar
            $('#navbar-toc').removeClass('scroll-up-toolbar');  //For toolbar
            $('.navbar-toc.aem-component-toolbar').addClass("scroll-up-toolbar");

            $(".spacer-line").removeClass("spacer-line-scrolled");
			$(".toc-spacer-line").removeClass("spacer-line-scrolled");
            $('.sticky-right-content').css('top','');
            $('.right_rail_toggle').css('top','');
            $('.navbar-toc .toc-content').css('top',  151);


        return false;

    }
        if ( (scrollTop > lastScrollTop ) ) {
            $('.fix-menu-onscroll').removeClass('fix-top-onscroll');  //For navbar
            $('#navbar-toc').removeClass('scroll-up-toolbar');  //For toolbar
            $('.navbar-toc.aem-component-toolbar').addClass("scroll-up-toolbar");


            $('.sticky-right-content').css('top','');
            $('.right_rail_toggle').css('top','');
            $(".toc-rail-toggle").css("top",'');
            $('#content-page-full-container .doc-body-content').removeClass('bottom-top');
            $('#content-page-full-container .doc-body-content').addClass('top-bottom');

            tocMenuFix('down'); //To make it working in Mobile
        } else {
            $('.fix-menu-onscroll').addClass('fix-top-onscroll');  //For navbar
            $("#navbar-toc").addClass("sticky-toc");  //For toolbar
            $('#navbar-toc').addClass('scroll-up-toolbar');  //For toolbar
            $("#navbar-toc .toc-action-navbar").addClass("container");  //For toolbar
            $(".spacer-line").addClass("spacer-line-scrolled");
			$(".toc-spacer-line").addClass("spacer-line-scrolled");
             // manage icons position on scroll up
            $('.sticky-right-content').css('top',170);
            $('.right_rail_toggle').css('top','215px');
            $(".loadedToc .toc-rail-toggle").css("top",'215px');
            $(".toc-rail-toggle").css("top",'215px');
            // end
            $('#content-page-full-container .doc-body-content').removeClass('top-bottom');
            $('#content-page-full-container .doc-body-content').addClass('bottom-top');
            tocMenuFix('up'); //To make it working in Mobile
            
        }




}
// }
}


var timer = null;
window.addEventListener('scroll', function() {


    if(timer !== null) {
        clearTimeout(timer);
    }
    timer = setTimeout(function() {
          // do something
        adjustGlobalHeader();
    lastScrollTop = $(window).scrollTop();
    }, 250);
}, false);



$.fn.isOnScreen = function() {

    var win = $(window), bounds, viewport;

    viewport = {
        top: win.scrollTop(),
        left: win.scrollLeft()
    };
    viewport.right = viewport.left + win.width();
    viewport.bottom = viewport.top + win.height();

    bounds = this.offset();
    bounds.right = bounds.left + this.outerWidth();
    bounds.bottom = bounds.top + this.outerHeight();

    return (!(viewport.right < bounds.left || viewport.left > bounds.right || viewport.bottom < bounds.top || viewport.top > bounds.bottom));

};

// ----------------------------------------------
// ------------ Addresses MD-11432 --------------
// ----------------------------------------------

function contentNavFooterFix() {
    if ($('.content-page-main').length > 0) {
        var contentNav = $('.content-page-nav'),nonStaticNav,rectToC,
        rectFoot = $('.footer')[0].getBoundingClientRect(),
        margin = (rectFoot.top - document.documentElement.clientHeight) * -1,
        rect = $('.content-page-main')[0].getBoundingClientRect(),
        newWidth = rect.right + parseFloat($('.content-page-main').css('margin-right'));

        if ($(window).width() < 1088) {
            contentNav.css('padding-right', 0).css('padding-left', 0);
            contentNav.css('width', '');
        } else {
            if ($('.main-body-content').hasClass('hide_right_rail') && !$('.content-page-main').hasClass('toc-open') && !$('#navbar-toc .toc-content').hasClass('non-static-toc-content')) {
                contentNav.css('padding-right', rect.left + 'px').css('padding-left', rect.left + 'px');
                contentNav.css('width', '');
            }else{
                if ($('.content-page-main').hasClass('toc-open')) {
                     contentNav.css({"right": "0px", 'padding-left': "28px", 'padding-right': rect.left + 'px'});
                      if ($('#navbar-toc .toc-content').hasClass('non-static-toc-content')) {
                            nonStaticNav = parseFloat( parseFloat($('.content_page').width()) - parseFloat($('#navbar-toc .toc-content.non-static-toc-content').width()) );   //For old TOC layout Footer Nav
                            rectToC = $('#navbar-toc .toc-content.non-static-toc-content')[0].getBoundingClientRect(); //For old TOC layout Footer Nav

                         contentNav.css('width', (nonStaticNav - rectToC.left) + "px");   //For old TOC layout Footer Nav
                      } else{
                          nonStaticNav = parseFloat( parseFloat($('.content_page').width()) - parseFloat($('#navbar-toc .toc-content').width()) );   //For old TOC layout Footer Nav
                          rectToC = $('#navbar-toc .toc-content')[0].getBoundingClientRect();   //For old TOC layout Footer Nav
                          contentNav.css('width', (nonStaticNav - rectToC.left) + "px");
                     }
                     if( ($(window).width() < 1088) || ($(window).width() < 1400) ) {
                           trimText($('.cp-nav-item a > p'), 0, 0, 0);
                     }
                }  else {
                      if ($(".main-body-content").find(".expand_more").length) {   //For when RCL is expanded the layout Footer Nav
                        contentNav.css({'width': "100%", 'padding-right': rect.left + 'px'});
                      } else {
                        if ($(".search-filters").css("display") === "none") {   //When RCL is closed, full width Footer Nav
                            contentNav.css({'width': "100%", 'padding-right': rect.left + 'px','padding-left': rect.left + 'px'});
                        } else {
                            contentNav.css({'padding-right': ($('.content-page-main').css('margin-right')), 'padding-left': rect.left + 'px',
                            'width': newWidth, "right": "auto"
                            });
                        }
                    }
                }
            }
            if(window.matchMedia("(min-width: 1088px)").matches){
                if (!$('.main-body-content').hasClass('hide_right_rail') && !$('.main-body-content').hasClass('show_right_rail') && !$('.content-page-main').hasClass('toc-open') && !$('#navbar-toc .toc-content').hasClass('non-static-toc-content')) {
                    contentNav.css('padding-right', rect.left + 'px').css('padding-left', rect.left + 'px');
                    contentNav.css('width', '');
                }
    
            }

        }

        if ($('.footer').isOnScreen() === true || margin > -50) {
            contentNav.css('position', 'absolute').css('bottom', $('.footer').outerHeight() + 'px');
            $('.main').css('position', 'relative');
        } else {
            contentNav.css('position', 'fixed').css('bottom', '');
            $('.main').css('position', '');
        }
    }

    return false;
}

function scrollDownToTargetPositionInDesktop(targethash) {
     setTimeout(function() {
       if ($('#navbar-toc').hasClass('sticky-toc')) {
          if ($('#navbar-toc').is(":visible")) {
            // MD-15004 Scroll issue wit join page
            if (window.location.href.indexOf('joined.html') !== -1) {
              if (!targethash.children('.title').length) {
                $('html, body').animate({
                  scrollTop: 0
                }, 650);
              }
              else{
                $('html, body').animate({
                  scrollTop: targethash.offset().top - ($('#navbar-toc').outerHeight()) -$('nav.navbar-madison').outerHeight() -150
                }, 650);
              }
            }
            else{
                if (!targethash.children('.title').length && !targethash.hasClass('anchor-id') && !targethash.children('.titlealts')) {
                        $('html, body').animate({
                          scrollTop: 0
                        }, 0);
                    }else{
                $('html, body').animate({
                   scrollTop: targethash.offset().top - ($('#navbar-toc').outerHeight()) - $('nav.navbar-madison').outerHeight() -150
                }, 0);
            }
              }
         // End
         }
       }
     }, 1010);
}

// get window width
    function windowWidth() {
      var winWidth = window.innerWidth
      || document.documentElement.clientWidth
      || document.body.clientWidth;

      return winWidth;
    }


$(document).ready(function(e) {

    // <<-------------------End------------------->>
    //To add PWC icon when we add page to home screen (Favicon)
    if($(window).width() < 767) {
        $("html > head").append('<link href="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/PwC_App_Logo.png" type="image/png" rel="apple-touch-icon" sizes="192x192">',
        '<link href="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/PwC_App_Logo.png" type="image/png" rel="apple-touch-icon" sizes="128x128">');
    }
  if($('.content-page-main').hasClass('toc-open')) {
    calcWidth(e);
  }
  function isWhiteListLink(url) {
    if ( url.href.includes('mailto:')|| location.hostname === url.hostname || !url.hostname.length
    || url.href.includes('viewpoint') || url.href.includes('madison-dev')  ) {
      return true;
    }
    
    return false;
  }
    //To add a icon on only external links
    Array.from( document.querySelectorAll( '#content-page-full-container .content-page-container a,.in-the-loop-template a' ) ).forEach(function(a) {
        a.classList.add( isWhiteListLink(a)? 'local' : 'external' );
    });
    // <<<----------------Sea-Also Truncate Logic For mobile------------>>>
    if(window.matchMedia("(max-width: 768px)").matches){
        // calculate no. of char in a line according to screen size
        var maxCharacters = Math.floor($('a.see-also_link').width() / 6.5); // 6.5 is the avg. width of character according to fontsize&fontfamily
        // truncate text if text length is greter then max char in 2.5 line  
        $('.see-also_link').each(function( index,ele ) {
            if(ele.text.length>maxCharacters*2.5){
                $('.see-also_link')[index].text=ele.text.slice(0,maxCharacters*2.5)+'... ';
            }
        });
    }
    //<<<----------End------------>>>
});

$( window ).resize(function(e) {
  if($('.content-page-main').hasClass('toc-open')) {
    calcWidth(e);
  }
});

function toggleToc(e) {
   var $showStaticToc = true,
    targethash = $(location.hash.replace(/\./,'\\.')),
    $TOCtop = $(".content-page-main").offset().top -2,
    $spacebarTocTop = $(".space-bar-content").offset().top+ parseInt($(".space-bar-content").css("padding-top"),10),
    $TOCtop1 = $spacebarTocTop-$(window).scrollTop()+5,
    $tocButtonPos = $(".content-page-main").offset().top-$(window).scrollTop()+5,dragWidth;

   if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false){
       $(".toc-content").fadeToggle("fast");
   }
   else{
       $(".toc-content").slideToggle("fast");
   }
   $('.content-page-main').toggleClass("toc-open");
   if($('.content-page-main').hasClass('toc-open')) {
       calcWidth(e);
       $('.content_page .search-filters').css('display', 'none');
       $('.content_page .spacer-line .cross_image').addClass("plus_sign");
       $('.right_rail_toggle').addClass('hide_arrow');
   } else {
       $('.content_page .search-filters').css('display', '');
       $('.content_page .spacer-line .cross_image').removeClass("plus_sign");
       $('.right_rail_toggle').removeClass('hide_arrow');

       //RCL drag width, when TOC toggled
        if ($('.main-body-content').hasClass('show_right_rail') && window.sessionStorage.getItem("RCL_open") === 'true' && window.sessionStorage.getItem("_RCLwidthInPX") !== null) {
            RCLxPercent = window.sessionStorage.getItem('_RCLdragWidthFullScreen');
            RCLwidth =  window.sessionStorage.getItem('_RCLdragWidth');
            RCLwidthInPX =  window.sessionStorage.getItem('_RCLwidthInPX');
    
            $(".spacer-line").css("left", RCLwidth + "%");  //For spacer line left position in %
            RCLside1.css("width", RCLwidthInPX + "px");  //RCL searchfilter width in PX
            RCLside2.css("width","100%");  //Content page width
            $('.sticky-right-content, .slider-items').css("width", (RCLwidthInPX -20) + "px");  //RCL sticky-right-content width in PX
            slickRefresh();  //To refresh RCL slickslider
            $('.content-page-nav').css('width', RCLxPercent + "%");   //To change Prev & Next btn width when dragging TOC btn               
        }
   }

    if($(targethash).length){
         scrollDownToTargetPositionInDesktop(targethash);
    }

    if ($('.content-page-main').hasClass('toc-open')) {
        if ($TOCtop1 < 0) {
             $('.navbar-toc .toc-content').css('top',  0);
        } else {
            $('.navbar-toc .toc-content').css('top',  $TOCtop1);
          }
        $('.toc-rail-toggle').removeClass('toc-close');
       // $('.content_page .search-filters').css('display', 'none');
        //$('.content_page .spacer-line').css('display', 'none');
        //$('.content_page .spacer-line .cross_image').addClass("plus_sign");
        //$('.right_rail_toggle').addClass('hide_arrow');
       // $('.content-page-nav').removeClass('content-page-nav--show');
        $(".onloadToc").css('visibility', 'hidden');
        //$(".main-body-content .doc-body-head").attr('style', 'padding-top: 6% !important');

         contentNavFooterFix();
         $(".toc-rail-toggle .divider_line").css("display","");   //To make TOC divider line visible
         $(".toc-resize").css("display","");     //To make TOC-resize visible
         $(".toc-rail-toggle").css({"display": "block", "width": "34px", 'height': '96px'});   //To adjust TOC-button content

         if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false){         //To re-position TOC according to Old/Non-static-toc
            if(windowWidth() >= 1088) {
              if (window.sessionStorage.getItem("_dragwidth")) {
                if (window.sessionStorage.getItem("_dragwidth") === "0") {
                  window.sessionStorage.setItem('_dragwidth', 34);
                } else {
                dragWidth = window.sessionStorage.getItem("_dragwidth");
                $(".toc-content").css("width", dragWidth+ "%");
                $(".content-page-container").css("width", 100-dragWidth+ "%");
                if (window.location.href.indexOf('joined.html') !== -1 ) {
                    $('#navbar-toc').css("width", 100-dragWidth+ "%");
                    }
                contentNavFooterFix();
                 $('.content-page-nav').css('width', 100-dragWidth + "%");   //To change Prev & Next btn width when dragging TOC btn
                }
              }
              /*else {
                    $('.content-page-nav').css('width', "59%");
              }*/
            }
         } else{
             contentNavFooterFix();
         }
        setTimeout(function(){
            $(".toc-open .toc-content .toc-container").addClass('hasScrolled');
            if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false){
               $(".toc-content .toc-container").addClass("non-scrollbar-ToC");  //To hide only scrollbar but it can be scrollable
            }
            if ($(".toc-open .toc-content .toc-container").hasClass('hasScrolled')) {
                 $(".toc-open .toc-content .toc-container").scrollTo(".toc-open .toc-content .toc-container .toc-scrollto", "800");
            }

        },500);
    } else {
        $(".toc-rail-toggle .divider_line").css("display","none");   //To hide divider line, when ToC list is closed
        $(".toc-content").css("width","34%");
        if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false){
            $(".onloadToc").css('visibility', 'visible');
            $(".toc-rail-toggle").css({"display": "flex", "width": "34px", 'height': 'auto'});   //To adjust TOC-button content
        }
        $(".toc-content .toc-rail-toggle").css({"display": "none"});
        $(".content-page-container").removeAttr("style");
        if (window.location.href.indexOf('joined.html') !== -1 ) {
        $('#navbar-toc').removeAttr("style");
        }
        setTimeout(function() {
            contentNavFooterFix();
            tocMenuFix();
			$('.content-page-nav').addClass('content-page-nav--show');
        }, 200);

        /*if ($('.content-page-main').hasClass('expand_full_size') === false) {
            $('.content_page .search-filters').css('display', '');
            $('.content_page .spacer-line').css('display', '');
        } else {
            $('.content_page .spacer-line').css('display', '');
        }*/
       // $(".toc-button").css({"position": "fixed", "left": "5px","right":"auto"});
         $(".toc-spacer-line").css({"left": 0});

        $(".main-body-content .doc-body-head").attr('style', 'padding-top: auto !important');
    }


}


// FOR MAKING THE TOC BUTTON LOAD AFTER PAGELOAD
// ATTRIBUTE BASED TOC SHOW
function tocOpen(){
    var customEvent = document.createEvent("CustomEvent"),
    $tocClamp = (".toc-open #navbar-toc .toc-content .toc-item a");
    customEvent.initCustomEvent('TOCOpened', false, false,{
    });
    document.dispatchEvent(customEvent);
    toggleToc();

    clamp($tocClamp,3);
}

$(document).ready(function() {
    //var $showStaticToc = $('.toc-button').data('showStaticToc'),targethash,$tocButntop ,container,
    var noTOCLength = $("section .container .no-toc").length, $showStaticToc = true, targethash, $tocButntop, container,
    $TocState = $('.toc-button').data('showStaticToc'),
    $ditaElement = $('.content-page-container');
    $('.toc-resize').click(false);       //To make TOC-resize button non-clickable
    $(".toc-rail-toggle .divider_line").css("display","none");   //To make TOC divider line hidden
    $(".toc-resize").css("display","none");   //To make TOC-resize icon hidden
    $tocButntop=$(".onloadToc .toc-rail-toggle").offset();


   //$(".loadedToc .toc-rail-toggle").css("top",$tocButntop.top);
   if($ditaElement.length){
       $(".loadedToc .toc-rail-toggle").css("top", $(".content-page-main").offset().top-$(window).scrollTop()+124);
    }
    if($('.content-page-main').hasClass('toc-open')) {
        $('.content_page .search-filters').css('display', 'none');
        $('.content_page .spacer-line .cross_image').addClass("plus_sign");
        $('.right_rail_toggle').addClass('hide_arrow');
    } else {
        $('.content_page .search-filters').css('display', '');
        $('.content_page .spacer-line .cross_image').removeClass("plus_sign");
        $('.right_rail_toggle').removeClass('hide_arrow');

        //RCL drag width, when page load
      
    }
     // show StaticToc layout  , in else show non static toc layout
  setTimeout(function(){
   // $(".toc-button").css("display","flex");
    if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false && noTOCLength!==1){
          $(".toc-spacer-line").show();
          $(".content-page-main .toc-content").removeClass("non-static-toc-content");
          $(".list-view-icon").hide();
         // $(".toc-content .toc-container").addClass("non-scrollbar-ToC");  //To hide only scrollbar but it can be scrollable
          //$('.content-page-nav').removeClass('content-page-nav--show');
        //  tocOpen();
        if((!$TocState && window.sessionStorage.getItem("_dragwidth")==="0") || (!$TocState && !window.sessionStorage.getItem("_dragwidth"))){
          $(".onloadToc").css('visibility', 'visible');
          $(".toc-rail-toggle").css({"display": "flex", "width": "34px"});
          rclStateCheck=true;
          if ($('.main-body-content').hasClass('show_right_rail') && window.sessionStorage.getItem("RCL_open") === 'false' && !$('.right_rail_toggle .cross_image').hasClass('plus_sign')){
            // code to hide RCL
            $('.right_rail_toggle .cross_image').addClass('plus_sign');
            $('.right_rail_toggle').addClass('hide_arrow');
            $('.content_page .search-filters').hide();
          } 
        } else if ($TocState && window.sessionStorage.getItem("_dragwidth")==="0"){
		  $(".onloadToc").css('visibility', 'visible');
          $(".toc-rail-toggle").css({"display": "flex", "width": "34px"}); 
          rclStateCheck=true;
          if ($('.main-body-content').hasClass('show_right_rail') && window.sessionStorage.getItem("RCL_open") === 'false' && !$('.right_rail_toggle .cross_image').hasClass('plus_sign')){
            // code to hide RCL
            $('.right_rail_toggle .cross_image').addClass('plus_sign');
            $('.right_rail_toggle').addClass('hide_arrow');
            $('.content_page .search-filters').hide();
          } 
        } else {
          tocOpen();
          if(window.sessionStorage.getItem("_dragwidth") <=20){
            $('#navbar-toc .toc-content').addClass("textwrapcover");
          }else{
            $('#navbar-toc .toc-content').removeClass("textwrapcover");
          }
        }


    }
    else{
        $(".content-page-main .toc-content").addClass("non-static-toc-content");
        //$(".toc-content").removeClass("non-scrollbar-ToC");   //To hide only scrollbar but it can be scrollable
        $(".list-view-icon").show();
        $(".toc-spacer-line").hide();
        $(".showtocFlag").remove();

         // To toggle ToC after clicking at outside of the ToC list
         // For only Old Static ToC Layout
        if($('.toc-content').length > 0) {
            document.addEventListener('click', function( event ) {
               container = document.getElementsByClassName("toc-content")[0].getElementsByClassName("toc-container")[0];
               if($(event.target).hasClass('icon-ToC') === false) {
                  if (container !== event.target && !container.contains(event.target)) {
                    if($('.toc-content').css('display') === 'block') {
                        toggleToc();
                    }
                  }
               }
            });
        }
         //Scroll down to the target position on desktop , reducing the height of sticky menu//
         targethash = $(location.hash.replace(/\./,'\\.'));
         if ($(targethash).length) {
           scrollDownToTargetPositionInDesktop(targethash);
         }
    }
  },200);
});

$(".toc-plus-image,.list-view-icon.toc-button").on("click", function(e) {
  e.preventDefault();
  $('.sub-topic-slick-slider').slick("refresh");
  tocOpen();
  if( $(e.target).hasClass('toc-cross-sign')){
    window.sessionStorage.setItem('_dragwidth', 0);
    if($('.main-body-content').hasClass('show_right_rail')){
        window.sessionStorage.setItem('RCL_open', true);
    } // need to add one more check when user play with toc rcl flag should change if rcl present 
  }
  else {
    window.sessionStorage.setItem('_dragwidth', 34);
    if($('.main-body-content').hasClass('show_right_rail')){
        window.sessionStorage.setItem('RCL_open', false);
    }
  }

});

$(".toc-close.toc-button").on("click", function(e) {
    e.preventDefault();
    $('.sub-topic-slick-slider').slick("refresh");
    tocOpen();
});

// Resizing TOC

// TOC Draggable Button
var dragging = false,side1,side2,xPercent;
$(document).ready(function() {
  side1 = $('#navbar-toc .toc-content');
  side2 = $(".content-page-container");
  $('.toc-resize').mousedown(function(e) {
  e.preventDefault();
  dragging = true;
  $(document).mousemove(function(ex) {
      var mouseMove = ex.pageX,
      mainBody = $('#content-page-full-container')[0].getBoundingClientRect(),
      sub = parseFloat(mouseMove - mainBody.left),
      tocWidth = parseFloat( ( sub / parseFloat($("#content-page-full-container").css('width')) ) * 100 );
      xPercent = mouseMove / $( document ).width() * 100;

	if ( dragging &&  (xPercent > 7 && xPercent < 50) && (tocWidth >= 0)) {
	if(xPercent <=20) {
      side1.addClass("textwrapcover");
    } else {
      side1.removeClass("textwrapcover");
    }
    $(".toc-spacer-line").css("left", xPercent+ "%");
    side1.css("width", xPercent + "%");
    side2.css("width", 100-tocWidth+ "%");
    if (window.location.href.indexOf('joined.html') !== -1) {
    $('#navbar-toc').css("width", 100-tocWidth+ "%");
    $('#navbar-toc').css("margin-left", 'auto');
    $('#navbar-toc > .toc-action-navbar').css("width","auto");
    }
    window.sessionStorage.setItem('_dragwidth', xPercent);
    $('.content-page-nav').css('width', 100-xPercent + "%");   //To change Prev & Next btn width when dragging TOC btn
  }
  $('.sub-topic-slick-slider').slick("refresh");
  });
});

$(document).mouseup(function(e) {
     if (dragging===true) {
      dragging = false;
      $(document).unbind('mousemove');
    }

});
});
// Resizing TOC End

$(".right_rail_toggle > .rcl-closed-tooltip").on("click", function(e) {
    window.sessionStorage.setItem('RCL_open', true);
    window.sessionStorage.setItem('_dragwidth', 0); 
});
$(".right_rail_toggle > .rcl-opened-tooltip").on("click", function(e) {
    window.sessionStorage.setItem('RCL_open', false);
});


// RCL Draggable Button
$(document).ready(function() {

    $('.RCL-resize').mousedown(function(e) {
        e.preventDefault();
        RCLdragging = true;

        $(document).mousemove(function(ex) {
            var mouseMove = ex.pageX, minValueRCL,
            mainBody = $('#content-page-full-container')[0].getBoundingClientRect(),
            sub = parseFloat(mouseMove - mainBody.left),
            RCLwidthInPX = parseFloat(mainBody.width - sub),
            RCLwidth = parseFloat( ( sub / parseFloat($("#content-page-full-container").css('width')) ) * 100 );
            RCLxPercent = mouseMove / $( document ).width() * 100;
            
            if ($(window).width() < 1280) {
                minValueRCL = 68;
            } else {
                minValueRCL = 74;
            }

            if ( RCLdragging &&  (RCLwidth > 50 && RCLwidth < minValueRCL)) {
                $(".spacer-line").css("left", RCLwidth + "%");
                RCLside1.css("width", RCLwidthInPX + "px");
                RCLside2.css({"width":"100%"});
                $('.sticky-right-content, .slider-items').css("width", (RCLwidthInPX -20) + "px");
                
                
                window.sessionStorage.setItem('_RCLdragWidthFullScreen', RCLxPercent);
                window.sessionStorage.setItem('_RCLdragWidth', RCLwidth);
                window.sessionStorage.setItem('_RCLwidthInPX', RCLwidthInPX);
                window.sessionStorage.setItem('RCL_open', true);

                slickRefresh();
                $('.content-page-nav').css('width', RCLxPercent + "%");   //To change Prev & Next btn width when draggingRCL TOC btn
            }
            $('.sub-topic-slick-slider').slick("refresh");
        });
    });

    $(document).mouseup(function(e) {
        if (RCLdragging===true) {
            RCLdragging = false;
            $(document).unbind('mousemove');
        }
    });
});
// Resizing RCL End

$(function() {
    setTimeout(function() {
        contentNavFooterFix();
        tocMenuFix('initial');
		if ($('.main-body-content').hasClass('hide_right_rail')) {
			trimText($('.cp-nav-item a > p'), 0, 0, 0);
		} else {
			trimText($('.cp-nav-item a > p'), 0, 0, 0);
		}
        $('.content-page-nav').addClass('content-page-nav--show');
		$('.content-page-nav').removeClass('mobile-fade');
		$('.toc-mobile-bar').css('opacity', '1');
    }, 1000);
	
	$(window).on("orientationchange", function() {
		setTimeout(function() {
			contentNavFooterFix();
		}, 500);
	});

    $(window).on('scroll resize', function() {
        contentNavFooterFix();
        tocMenuFix();
    });
	
	$(window).on('resize', function() {
		if ($('.main-body-content').hasClass('hide_right_rail') || $('.content-page-main').hasClass('expand_full_size')) {
			trimText($('.cp-nav-item a > p'), 0, 0, 0);
		} else {
			trimText($('.cp-nav-item a > p'), 0, 0, 0);
		}
	});
	
	$('.right_rail_toggle').on('click', function() {
		if ($(this).find('.arrow_left').hasClass('icon-caret-right') === false) {
			$('.search-filters').css('background-color', 'white').css('z-index', '5');

			setTimeout(function() {
				$('.search-filters').css('background-color', '').css('z-index', '');
			}, 500);
		}
		
		// ----------------------------------------------
		// ------------ Addresses MD-11423 --------------
		// ----------------------------------------------
		var interval = window.setInterval(function() {
			contentNavFooterFix();
		}, 5);

		setTimeout(function() {
			$('.content-page-nav').addClass('content-page-nav--show');
			clearInterval(interval);
			interval = 0;
		}, 600);

		if ($('.content-page-main').hasClass('expand_full_size') === true) {
			trimText($('.cp-nav-item a > p'), 0, 0, 0);
		} else {
			trimText($('.cp-nav-item a > p'), 0, 0, 0);
		}
	});

});

// ----------------------------------------------------
// ---------------Content Page Nav Butons END--------------
// ----------------------------------------------------

/**
 * Function to be called to show blur effect
 */
function showBlur() {
    $(".main > div, .main > section, .main > nav, .main > footer")
        .not(".modal")
        .addClass("blur-Search");
    $(".main").addClass("modal-is-open");
}


function fixedRightContent() {
    var scrollTop = $(window).scrollTop(),searchFilters = $('.main-body-content'),distance, elementOffset;
    if (searchFilters.length) {
        elementOffset = searchFilters.offset().top;
        distance = elementOffset - scrollTop - parseInt($('.main-body-content .search-filters').css('padding-top'), 10);
        if (distance > 0) {
            $(".sticky-right-content").removeClass("scroll-fixed-top");
        } else {
            $(".sticky-right-content").addClass("scroll-fixed-top");
        }
    }
}

/**
 * Function to be called to hide blur effect
 */
function hideBlur() {
    $(".main > div, .main > section, .main > nav, .main > footer")
        .not(".modal")
        .removeClass("blur-Search");
    $(".main").removeClass("modal-is-open");
}

// function to be called when modal is closed
function closeModal(element) {
    $(element).closest('.modal').removeClass("is-active");
    $('body').removeClass('modal-is-open');
    hideBlur();
}

function removeTitleHeader(mywindow) {
    var headerDivs = mywindow.document.getElementsByClassName('navbar-madison primary'),
        length = headerDivs.length,
        i;
    if (length > 1) {
        for (i = length - 1; i > 0; i--) {
            headerDivs[i].parentElement.removeChild(headerDivs[i]);
        }
    }
}

function printElem(elem,mywindow) {
   var siteTitle = $('#pdfTitle').val(),
        links = document.getElementsByTagName("link"),
        link,
        counter,
        promise;

    mywindow.document.write('<html><head><title>' + siteTitle + '</title>');

    for (counter = 0; counter < links.length; counter++) {
        link = mywindow.document.createElement('link');
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", links[counter].href);
        mywindow.document.getElementsByTagName("head")[0].appendChild(link);
    }
    mywindow.document.write('</head><body class="page-vp page basicpage content_page body content_page_print"><div class="root responsivegrid">');
    mywindow.document.write('<div class="aem-Grid aem-Grid--12 aem-Grid--default--12 "><div class="main"><div class="aem-Grid aem-Grid--12 aem-Grid--default--12 ">');
    mywindow.document.write('<section><div class="container"><div class="news main-body-content"><div class="columns"><div class="column content-page-main"><div class="content-page-container"><div class="aem-Grid aem-Grid--12 aem-Grid--default--12 "><div class="responsivegrid aem-GridColumn aem-GridColumn--default--12"><div class="aem-Grid aem-Grid--12 aem-Grid--default--12 ">');
    mywindow.document.write(elem);
    mywindow.document.write('</div></div></div></div></div></div></div></div></section></div></div></div></div></div></body></html>');

    promise = new Promise(function (resolve, reject) {
        if (!!mywindow.MSInputMethodContext && !!document.documentMode) {
			removeTitleHeader(mywindow);
            setTimeout(function () {
                mywindow.document.close();
                mywindow.focus();
                mywindow.print();
                mywindow.close();
            }, 2000);
		}
        else{
            link.onload = function () {
                resolve(); 
            };
        }
    });
    promise.then(function () {
        removeTitleHeader(mywindow);
        mywindow.setTimeout(function () {
           mywindow.print();
           mywindow.close();
        }, 1000);
    });
    return true;
}


function printPDF(element) {
    var mywindow = window.open('', 'PRINT'),
        pagePath = $('#pagePath').val() + ".dwnldpdf.html",
        downloadError = $('#pdf-error').val(),
        userProfile;
    element.className = "icon-loading";
    element.style.pointerEvents = "none";
    if ($('#run-mode').val() !== 'author') {
        pagePath = pagePath.replace("/content/pwc-madison/ditaroot", "/dt");
    }
    if (window.UserRegistration.isUserLoggedIn) {
        userProfile = window.UserRegistration.userInfo;
        if (userProfile.isInternalUser) {
            if (userProfile.contentAccessInfo.privateGroups !== undefined && userProfile.contentAccessInfo.privateGroups.length > 0) {
                pagePath = pagePath.replace(".dwnldpdf.html", ".dwnldpdf.i_p.html?uri=_y");
            } else {
                pagePath = pagePath.replace(".dwnldpdf", ".dwnldpdf.i_n");
            }
        } else if (userProfile.contentAccessInfo.licenses !== undefined && userProfile.contentAccessInfo.licenses.length > 0) {
            pagePath = pagePath.replace(".dwnldpdf.html", ".dwnldpdf.i_e_l.html?uri=_y");
        } else {
            pagePath = pagePath.replace(".dwnldpdf", ".dwnldpdf.i_e_p");
        }
    }
    $.ajax({
        url: pagePath,
        dataType: "html",
        success: function (response) {
            printElem(response,mywindow);
        },
        error: function () {
            mywindow.close();
            $(element).parent().append('<div id="custom-tooltip" class="custom-tooltip"><div class="bottom"><span>' + downloadError + '</span><i></i></div></div>');
            setTimeout(function () {
                $(element).parent().find('.custom-tooltip').remove();
            }, 3000);
        },
        complete: function () {
            element.className = "icon-download";
            element.style.pointerEvents = "all";
        }
    });
}

function printElementByClick() {
    $(".modal").removeClass("is-active");
    hideBlur();
    window.print();
}

function copyToClipboard() {
    var el = document.createElement('textarea');
    el.value = window.location.href;
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
}

window.onbeforeprint = function () {
};

window.onafterprint = function () {
};

function print_info_modal() {
    if (localStorage.getItem("_remember_print_event") === null) {
        $(".print-instruction-body").addClass("is-active");
    } else {
        printElementByClick();
    }
}
$(".print-instruction-modal").on("click", function (e) {
    e.preventDefault();
    print_info_modal();
});

$("#remember_print_event").click(function () {
    if ($(this).prop("checked") === true) {
        localStorage.setItem("_remember_print_event", "true");
    }
});

$(".modal-close").click(function () {
    var refreshNotRequired={
        editModal:$(this).hasClass("edit-modal-close"),
        imageModal:$(this).parent().hasClass("pwc-image-modal"),
        tableModal:$(this).parent().hasClass('pwc-table-modal')
    },refererHeaderURL;
    if(!refreshNotRequired.editModal && !refreshNotRequired.imageModal && !refreshNotRequired .tableModal){
        $(".modal").removeClass("is-active");
        if($("#gated-content-referer-header-url").val()) {
		    refererHeaderURL = $("#gated-content-referer-header-url").val().indexOf("/user/gated-content")<0 ? $("#gated-content-referer-header-url").val() : $("#gated-content-redirect-url").val();
		    window.location.href = window.UserRegistration.sanitizeString(refererHeaderURL || $("#gated-content-redirect-url").val() || window.location.href);
        }else{
            window.location.href = window.UserRegistration.sanitizeString($("#gated-content-redirect-url").val());
        }
    }
});

jQuery(document).bind("keyup keydown", function (e) {
    if ((e.ctrlKey && e.keyCode === 80) || (e.metaKey && e.keyCode === 80)) {
        print_info_modal();
        return false;
    }
});

/**
 * Clamp lines for the given selector.
 * Define number of lines to be clamped in data-lines-to-clamp attribute. Default value is 1.
 */
window.clampTextForSelector = function (selector) {
    $(selector).each(function (index, value) {
        var linesToClamp = $(value).data("linesToClamp");
        if (linesToClamp === undefined) {
            linesToClamp = 1;
        }
        $clamp(value, {
            clamp: linesToClamp
        });
    });
};

/**
 * Clamp lines for all elements having class clamp-text.
 * Define number of lines to be clamped in data-lines-to-clamp attribute. Default value is 1.
 */
window.clampText = function () {
    window.clampTextForSelector(".clamp-text");
};

// ------------------------------------------------------------------------------
// ------------------------ Buzzsprout Scrollbar ------------------------------
// ------------------------------------------------------------------------------

if($(window).width() > 768){
  $('.buzzsprout-podcast .menu-items').scrollbar({
    "autoScrollSize": true,
    "disableBodyScroll": true
  });
}

$(document).ready(function () {
    var isMobileDevice = window.matchMedia("only screen and (max-width: 768px)").matches,
        p1 = $('.truncate-link').get(0),
        getVisible, $title,alertTitle = $(".alert_title"),
        footerElement, isElementInView, featureComponent, mostPopular, highlightsAbstract, featureComponentHome, searchKeyword, options;
    
    trimText(alertTitle, 250, 130, 250);
    trimText($('.register-sliding-box .sliding-content a'), 80, 80, 80);
	
    if (p1) {
        $clamp(p1, {
            clamp: 1
        });
    }

    getVisible = function () {
        var $el = $('.footer'),
            scrollTop = $(this).scrollTop(),
            scrollBot = scrollTop + $(this).height(),
            elTop = $el.offset().top,
            elBottom = elTop + $el.outerHeight(),
            visibleTop = elTop < scrollTop ? scrollTop : elTop,
            visibleBottom = elBottom > scrollBot ? scrollBot : elBottom,
            newdHeight,
            visibleArea = visibleBottom - visibleTop;
        if (visibleArea > 0) {
            $(".sticky-right-content").addClass("right-rail-bottom-height");
            newdHeight = ($(window).height() - 140 - visibleArea + 10) + 'px';
            $(".sticky-right-content").css('height', newdHeight);
        } else {
            $(".sticky-right-content").removeClass("right-rail-bottom-height");
            $(".sticky-right-content").css("height", 'auto');
        }
    };

    $(".content-page-container").append($("#next-page-link"));

    if ($('.text-right.doc-next-link').length > 1) {
        $('.text-right.doc-next-link').not('.text-right.doc-next-link:last').remove();
    }

    $title = $('#madison-title-print').clone();
    $('#madison-title-web').html($title);
	$('.doc-body-content #madison-title-print').removeClass('print-hidden');

    $('.back-to-top').on('click', function () {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });

    $('.mobile-search-filters .search-filter-details-toggle .js-all-filters-toggle').click(function (e) {
        e.preventDefault();
        $('.mobile-search-filters .search-filter-details-toggle .js-applied-filters-toggle').removeClass('selected');
        $('.mobile-search-filters .js-all-filters').show();
        $('.mobile-search-filters .js-applied-filters').hide();
        $('.mobile-filter-apply-bar .apply-filters').css('display', 'inline-flex');
        $('.mobile-filter-apply-bar .update-filters').hide();
    });
    $('.mobile-search-filters .search-filter-details-toggle .js-applied-filters-toggle').click(function (e) {
        e.preventDefault();
        $('.mobile-search-filters .search-filter-details-toggle .js-all-filters-toggle').removeClass('selected');
        $('.mobile-search-filters .search-filter-details-toggle .js-applied-filters-toggle').addClass('selected');
        $('.mobile-search-filters .js-all-filters').hide();
        $('.mobile-search-filters .js-applied-filters').show();
        $('.mobile-filter-apply-bar .apply-filters').hide();
        $('.mobile-filter-apply-bar .update-filters').css('display', 'inline-flex');
    });

    $(".toc-item > a.expand-link").click(function (e) {
        if (location.pathname.replace(/^\//, '') === this.pathname.replace(/^\//, '') ||
            location.hostname === this.hostname) {
            var target = $(this.hash);
            if (target.length) {
                if ($('#navbar-toc').hasClass('sticky-toc')) {
                    $('html,body').animate({
                        scrollTop: target.offset().top - ($('#navbar-toc').outerHeight() + $('.toc-action-navbar').outerHeight())
                    }, 650);
                } else {
                    $('html,body').animate({
                        scrollTop: target.offset().top - ($('#navbar-toc').outerHeight() + $('.toc-action-navbar').outerHeight()) - 30
                    }, 650);
                }

                if ($('div.toc-content').is(":visible")) {
                    $("div.toc-content").slideUp('fast');
                    window.location.hash = this.hash;
                }
                return false;
            }
        }
    });

    $('#toc-mobile-content .toc-item .expand-link').click(function () {
        if (location.pathname.replace(/^\//, '') === this.pathname.replace(/^\//, '') ||
            location.hostname === this.hostname) {
            var target = $(this.hash);
            if (target.length) {
                $('html,body').animate({
                    scrollTop: target.offset().top
                }, 650);

                if ($('#animatedTableOfContentModal').is(":visible")) {
                    $('#animatedTableOfContentModal .icon-close-popup').trigger('click');
                }
                return false;
            }
        }
    });

	jQuery.fn.scrollTo = function(elem, speed) { 
		if($(elem).length > 0) {
		  $(this).animate({
			scrollTop:  $(this).scrollTop() - $(this).offset().top + $(elem).offset().top 
		  }, speed === undefined ? 1000 : speed); 
		  return this; 
		}

		return false;
	};


    if ($(".toc-mobile-bar #animatedTableOfContent").length) {
		$("#animatedTableOfContentModal").show();
		$(".toc-mobile-bar #animatedTableOfContent").animatedModal({
			color: "#f7f7f7",
			animatedIn: "slideInUp",
			animatedOut: "slideOutDown",
			beforeOpen: function() {
				$(".navbar.subnav").addClass("position-initial");
				$("html").addClass("hide-scroll-mobile");
			},
			afterOpen: function() {
				if ($(".mobile-table-content-data .toc-container").hasClass('hasScrolled') === false) {
					setTimeout(function() {
						$(".mobile-table-content-data .toc-container").scrollTo(".mobile-table-content-data .toc-scrollto", "800");
						$(".mobile-table-content-data .toc-container").addClass('hasScrolled');
					}, 500);
				}
			},
			afterClose: function() {
				$(".navbar.subnav").removeClass("position-initial");
				$("html").removeClass("hide-scroll-mobile");
			}
		});
	}

    function fixedSearchPosAdjust() {
        if(windowWidth() >= 1088) {
          if($('.navbar-toc.sticky-toc').length > 0) {
            var searchDocEl = $('.searchdoc-v2.searchdoc-v2--scroll'),
            desktopBtn = $('.toc-action-navbar .share_icons'),
            rightVal = desktopBtn.outerWidth() + parseInt($('.navbar-toc.sticky-toc .toc-action-navbar').css('margin-right'), 10) - 12;
            searchDocEl.css('right', rightVal);
          } else {
            $('.searchdoc-v2.searchdoc-v2--scroll').css('right', '');
          }
        } else {
          $('.searchdoc-v2.searchdoc-v2--mobile').removeClass('searchdoc-v2--scroll');
        }
    }
    //Sticky Navbar
    $(window).scroll(function() {
		var isMegaMenuOn = $(".show-mega-menu"), p3,
		$openTOC= $('.content-page-main.toc-open'),
		$ditaElement = $('.content-page-container'),
		$spacebarTocTop , $TOCtop1,
		//$TOCtop1 = $(".content-page-main").offset().top-$(window).scrollTop()-2,
        $topposToc= $(".space-bar-content").offset(),
        //$showStaticToc = $('.toc-button').data('showStaticToc'),
        $showStaticToc = true,
        scrollLimit=260,
        $tocButntop=$(".onloadToc .toc-rail-toggle").offset();
	    //$tocTop = $(".space-bar-content"),$tocTop1;
		if (!isMegaMenuOn.length) {
            if($('.toolbar-inloop').length){
                if(window.matchMedia("(max-width: 768px)").matches){
                    scrollLimit = 126;
                }else{
                    scrollLimit = 0;
                }
            }
			if ($(this).scrollTop() > scrollLimit) {
				if ($('.stick-nav-title').length > 0) {
					p3 = $('.stick-nav-title').get(0);
					$(function() {
						$clamp(p3, {
							clamp: 1
						});
					});
				}

				$("#navbar-toc").addClass("sticky-toc");
				$(".search-filters").addClass("search-filters-scrolled");
				$(".content-page-container").addClass("sticky-toc-sib");
				$(".spacer-line").addClass("spacer-line-scrolled");
				$(".toc-spacer-line").addClass("spacer-line-scrolled");
				$("#navbar-toc .toc-action-navbar").addClass("container");
				$("#navbar-toc .toc-content").addClass("container");
				$("#navbar-toc .toc-content").addClass("sticky-border");
				$(".stick-nav-title").show();
				//$(".nav-title").hide();
				$(".backto-link").hide();
				 if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false){
                    if($('.fix-menu-onscroll').hasClass('fix-top-onscroll')){
                        $(".loadedToc .toc-rail-toggle").css("top",215);
                    }else{
                     $(".loadedToc .toc-rail-toggle").css("top",122);
                    }
                 }
				if($ditaElement.length){
				   if($openTOC.length){
				       $('.navbar-toc .toc-content').css('top',0);
                      //$(".toc-button").css({"left":"41%","right":"auto","top":"11%"});
                       //$(".toc-spacer-line").css({"left":draggedTocBtn,"right":"auto","top":"auto"}); //to resizing TOC position
                       $(".toc-spacer-line").css({"right":"auto","top":"auto"}); //to resizing TOC position

                   }else{
                      //$(".toc-button").css({"top":"11%"});
                      $(".toc-spacer-line").css({"top":"auto"});
                   }
                }
				fixedSearchPosAdjust();
				if(windowWidth() >= 1088) {
                    if($('.searchdoc-v2.searchdoc-v2--desktop').length > 0) {
                        if($('.searchdoc-v2.searchdoc-v2--mobile').length > 0){
                            $('.searchdoc-v2.searchdoc-v2--mobile').addClass('searchdoc-v2--scroll');
                        }
                    }
                    if($('.searchdoc-v2.searchdoc-v2--desktop').hasClass('is-hidden') === false) {
                      $('.searchdoc-v2.searchdoc-v2--mobile').removeClass('is-hidden');
                    }
                }
			} else {
                $(".toolbar-inloop #navbar-toc").removeClass("sticky-toc");
				$(".search-filters").removeClass("search-filters-scrolled");
				$(".content-page-container").removeClass("sticky-toc-sib");
				$("#navbar-toc .toc-content").removeClass("container");
				$("#navbar-toc .toc-content").removeClass("sticky-border");
				//$(".nav-title").show();
                if ($('#content-page-full-container').length === 0) {
                    $(".spacer-line").removeClass("spacer-line-scrolled");
				    $(".toc-spacer-line").removeClass("spacer-line-scrolled");
                    $(".stick-nav-title").hide();
                }
				if ($(".backto-link").hasClass('show')) {
					$(".backto-link").show();
				}
		        if($ditaElement.length){
                   $spacebarTocTop = $(".space-bar-content").offset().top+ parseInt($(".space-bar-content").css("padding-top"),10);
                   $TOCtop1 = $spacebarTocTop-$(window).scrollTop()+5;
				    if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false){
                        if($TOCtop1>0){
                            if($('.fix-menu-onscroll').hasClass('fix-top-onscroll')){
                                $(".loadedToc .toc-rail-toggle").css("top",215);
                            }else{
                        $(".loadedToc .toc-rail-toggle").css("top",$TOCtop1+122);
                            }
                        }
                        else{
                            if($('.fix-menu-onscroll').hasClass('fix-top-onscroll')){
                                $(".loadedToc .toc-rail-toggle").css("top",215);
                            }else{
                            $(".loadedToc .toc-rail-toggle").css("top",122);
                            }
                        }
                    }
                    //$tocTop = $tocTop.offset().top+ parseInt($(".space-bar-content").css("padding-top"),10);
                    //$tocTop1 = $tocTop  -$(window).scrollTop();
				    //$(".toc-button").css({"top": $tocTop1+32});
				    if($openTOC.length){
                        //$(".toc-button").css({"left":"41%"});
                        //$('.navbar-toc .toc-content').css('top',  $tocTop1+2);
                        if(typeof $showStaticToc !== 'undefined' && $showStaticToc !== false){     //To re-position TOC according to Old/Non-static-toc
                            $('.navbar-toc .toc-content').css('top',  $TOCtop1);
                            //$(".loadedToc .toc-rail-toggle").css("top",$tocButntop.top);
                            //$(".toc-spacer-line").css({"left":draggedTocBtn});
                        }
                        else {
                          $(".toc-content").addClass("non-static-toc-content");
                          $('.navbar-toc .toc-content').css('top',  $TOCtop1 + 60);
                        }
                    }
                }
				if(windowWidth() >= 1088) {
                    if($('.searchdoc-v2.searchdoc-v2--mobile').length > 0){
                        $('.searchdoc-v2.searchdoc-v2--mobile').addClass('is-hidden').removeClass('searchdoc-v2--scroll');
                    }
                }
			}
		}

		/*if ($('.nav-title').is(":visible")) {
			$(".stick-nav-title").hide();
		} else {-item
			$(".stick-nav-title").show();
		}*/

		getVisible();
		fixedRightContent();

	});

    $('#toolsWrapper').click(function () {
        setTimeout(function(){
            $("#rclWrapper").removeClass('tools-dropdown-arrow');
            $("#toolsWrapper").toggleClass('tools-dropdown-arrow');
           // show tools wrapper
            if($('#animatedToolesModalNew').hasClass('show-tools')){
                $('#animatedToolesModalNew').addClass('hide-tools-modal');
                $('#animatedToolesModalNew').removeClass('show-tools');
            }
            else{
                $('#animatedToolesModalNew').removeClass('hide-tools-modal');
                $('#animatedToolesModalNew').addClass('show-tools');
                $('#animatedToolesModalNew').removeClass('show-rcl');
            }
            //Only works when in Mobile/Tablet has TOC & Tools bar only
            var $showStaticToc = true, noTOCLength = $("section .container .no-toc").length;

            //When only RCL option is hidden
            if ($('.mobile-rcl-wrapper').css('display') === 'none' && noTOCLength!==1) {
                if($(window).width() < 590) {
                    $(".toc-mobile-bar .show-tools").addClass('only-rcl-hidden-tools-mobile');
                } else if ($(window).width() < 1091) {
                    $(".toc-mobile-bar .show-tools").addClass('only-rcl-hidden-tools-tablet');
                }
            }
            //when only TOC option is hidden
            if ($('.mobile-rcl-wrapper').css('display') === 'flex' && noTOCLength!==0) {
                if($(window).width() < 590) {
                    $(".toc-mobile-bar .show-tools").addClass('only-toc-hidden-tools-mobile');
                } else if ($(window).width() < 1091) {
                    $(".toc-mobile-bar .show-tools").addClass('only-toc-hidden-tools-tablet');
                }
            }
        },200);

    });
    
    $('#rclWrapper').click(function () {
        $("#toolsWrapper").removeClass('tools-dropdown-arrow');
        var firstRCLEle=$('.mobile-toc-tools .accordion-header');
        if($('#animatedToolesModalNew').hasClass('show-rcl')){
            $('#animatedToolesModalNew').addClass('hide-tools-modal');
            $('#animatedToolesModalNew').removeClass('show-rcl');
            $("#rclWrapper").removeClass('tools-dropdown-arrow');
            $('.doc-body-content .table-responsive').show();
            $('body').css("position", "");
        }
        else{
            $('.doc-body-content .table-responsive').css('display',"none");
            $('body').css("position", "fixed");
            $("#rclWrapper").addClass('tools-dropdown-arrow');
            $('#animatedToolesModalNew').removeClass('hide-tools-modal');
            $('#animatedToolesModalNew').addClass('show-rcl');
            $('#animatedToolesModalNew').removeClass('show-tools');
            // to expand first element
            if(firstRCLEle.length && !$($(firstRCLEle).get(0)).parents(".accordion-item").hasClass("expanded")){
                firstRCLEle.get(0).click();
            }
        }
        $('.slider-content.slick-initialized').slick('refresh');
    });
    
    $(document).mouseup(function(e) {
        var container = $("#animatedToolesModalNew"), rclWrapper = $("#rclWrapper");
        if ((!container.is(e.target) && container.has(e.target).length === 0) && (!rclWrapper.is(e.target) && rclWrapper.has(e.target).length === 0)) {
            $('#animatedToolesModalNew').addClass('hide-tools-modal');
            $('#animatedToolesModalNew').removeClass('show-rcl');
            $("#rclWrapper").removeClass('tools-dropdown-arrow');
            $('.doc-body-content .table-responsive').show();
            $('body').css("position", "");
            
        } 
    });

    $('#animatedTableOfContent').click(function () {
        $("#toolsWrapper").removeClass('tools-dropdown-arrow');
        $("#rclWrapper").removeClass('tools-dropdown-arrow');
        $('#animatedToolesModalNew').addClass('hide-tools-modal');
        $('#animatedToolesModalNew').removeClass('show-tools');
        $('#animatedToolesModalNew').removeClass('show-rcl');
    });
    $(".modal-close, .reset-modal").click(function () {
        closeModal(this);
    });

    if (!isMobileDevice) {
        $(window).on('resize', getVisible);
    }

    tippy("[data-tooltip-content]", {
        content: function (reference) {
            var id = reference.getAttribute('data-tooltip-content');
            return $(id).get(0);
        },
        trigger: 'click',
        theme: "share",
        arrow: true,
        arrowType: "sharp",
        appendTo: "parent",
        animation: "fade",
        zIndex: 9999,
        interactive: true,
        distance: 20,
        boundary: "window",
        onShown: function () {
            if ($("body .tippy-arrow").length < 2) {
                $("body .tippy-arrow").clone().insertAfter("div.tippy-arrow:last");
            }
        }
    });

    tippy(".link-tooltip-popout", {
        content: $("#link_tooltip").html(),
        placement: "bottom",
        trigger: 'click',
        theme: "link",
        arrow: true,
        arrowType: "sharp",
        appendTo: "parent",
        animation: "fade",
        zIndex: 9999,
        interactive: true,
        distance: 20,
        boundary: "window"
    });


    window.clampText();

    function visibleY(el) {
        var rect = el.getBoundingClientRect(),
            top = rect.top,
            height = rect.height,
            el1 = el.parentNode;
        do {
            rect = el1.getBoundingClientRect();
            if (top <= rect.bottom === false) {
                return false;
            }
            // Check if the element is out of view due to a container scrolling
            if ((top + height) <= rect.top) {
                return false;
            }
            el1 = el1.parentNode;
        } while (el1 !== document.body);
        // Check its within the document viewport
        return top <= document.documentElement.clientHeight;
    }

    function setTocHeight() {
		var newHeight = 0,mobileHeaderHeight,
		$el = $(".footer"),
        scrollTop = $(this).scrollTop(),
        scrollBot = scrollTop + $(this).height(),
        elTop = $el.offset().top,
        elBottom = elTop + $el.outerHeight(),
        visibleTop = elTop < scrollTop ? scrollTop : elTop,
        visibleBottom = elBottom > scrollBot ? scrollBot : elBottom,
		visibleArea = visibleBottom - visibleTop,
        stickyNavbar = $('.navbar-toc').hasClass('sticky-toc'),
        spacerBorder = $('.spacer-border')[0].getBoundingClientRect();
		if (window.innerWidth > 1087) {
			$('.content_page').css('overflow', '');
			$('html').css('overflow', '');
		}
		if (visibleArea > 0 && window.innerWidth > 1087) {
			newHeight = $(window).height() - 70 - visibleArea + 10 + "px";
			$(".toc-content").css("height", newHeight);
		} else if (visibleArea <= 0 && window.innerWidth > 1087 && stickyNavbar) {
			newHeight = $(window).height() - $('.sticky-toc').height() - 15 + "px";
			$(".toc-content").css("height", newHeight);
		} else if (visibleArea <= 0 && window.innerWidth > 1087 && !stickyNavbar) {
			newHeight = $(window).height() - spacerBorder.bottom;
			$(".toc-content").css("height", newHeight);
		} else {
			mobileHeaderHeight = $('.mobile-table-content .title-header').height();
			newHeight = $(window).height() - mobileHeaderHeight - 20 + "px";
			$(".toc-content").css("height", newHeight);
		}
	}
	if ($('#content-page-full-container').length > 0) {
	$(function(){
	  $(window).on("scroll resize", function() {
		setTocHeight();
	  });
	});
}

    if (!isMobileDevice) {
        $(window).on("scroll resize", getVisible);
    }

    if($(window).width() > 768) {
        $('.home .scrollbar-outer').scrollbar({
            "autoScrollSize": true,
            "disableBodyScroll": true
        });
    }

    $('.news-show-more').on("click", function () {
        var showLessText = $(this).attr("data-less-label"),
            showMoreText = $(this).attr("data-more-label");
        if ($(this).siblings('.scrollbar-outer').hasClass('height')) {
            $(this).siblings('.scrollbar-outer').removeClass('height');
            $(this).find("a").text(showMoreText);
            $(this).find(".arrow").removeClass("arrowUp");

        } else {
            $(this).siblings('.scrollbar-outer').addClass('height');
            $(this).find("a").text(showLessText);
            $(this).find(".arrow").addClass("arrowUp");
        }
    });

    clamp('.limitNewsDesc', 4);

    $(".limitpodcast").each(function (index, element) {
        if (isMobileDevice) {
            clamp = 3;
        } else {
            clamp = 2;
        }
        $clamp(element, {
            clamp: clamp,
            useNativeClamp: true
        });
    });

  

    $('.content_page .spacer-line .right_rail_toggle .cross_image').on("click", function() {
        if (!$(this).hasClass("plus_sign")) {
            // for handling refresh
            window.sessionStorage.setItem('_RCLdragWidthFullScreen', 68.33137485311399);
            window.sessionStorage.setItem('_RCLdragWidth',73.21428571428571 );
            window.sessionStorage.setItem('_RCLwidthInPX', 360);
            // for handleing + icon click 
            setTimeout(function() {
                $(".main-body-content .search-filters").width("351px");
                $(".main-body-content .sticky-right-content").width("340px");
            }, 250);
           
        }
        fixedRightContent();
		if ($('.content-page-main').hasClass('toc-open')) {
              toggleToc();
            //   $('.content_page .search-filters').addClass('slide');
                 setTimeout(function(){
                    $('.content_page .search-filters').css('display', '');
                    $('.content_page .spacer-line .cross_image').removeClass("plus_sign");
                    $('.right_rail_toggle').removeClass('hide_arrow');
                 },200);
        } else {
            // $('.content_page .search-filters').toggle('slide');
            setTimeout(function(){
                $('.right_rail_toggle .cross_image').toggleClass('plus_sign');
                $('.right_rail_toggle').toggleClass('hide_arrow');
                $('.content_page .search-filters').toggle();
                
            },200);
        }
      
       
		
		if ($(".slider-content").hasClass("slick-initialized")) {
		    $(".slider-content").addClass('is-faded');
		    setTimeout(function() {
		        $(".slider-content").removeClass('is-faded');
                slickRefresh();
                $('.sub-topic-slick-slider').slick("refresh");
		    }, 200);
		}

		/*$('.content_page .search-filters').toggle('slide');
		setTimeout(function() {
		    $('.sub-topic-slick-slider').slick("refresh");
			$('.right_rail_toggle .cross_image').toggleClass('plus_sign');
			$('.right_rail_toggle').toggleClass('hide_arrow');
		}, 200);*/



	});
    $('#overlay, #navbar-toc').on('click', function (e) {
		e.stopPropagation();
		if ($('.slider-content').hasClass('slick-initialized')) {
			if ($('.slider-content')[0].slick !== undefined) {
				$(".slider-content").addClass('is-faded');
			    setTimeout(function() {
			        $('.slider-content')[0].slick.setPosition();
			    }, 200);
			}
		}
        $('.main-body-content .content-page-main').css('max-width', '');
        document.getElementById("overlay").style.display = "none";
        slickRefresh();
    });
    
    $(function() {
        $('.single-podcast-subscribe .podcasts-contain a').each(function() {
            var maxWidth = $('img', this).outerWidth() + $('span', this).outerWidth();
            $(this).css('max-width', maxWidth + 'px');
        });
    
        // ------------------------------------------------------------------------------
        // ------------------------ Landing Page Scrollbar ------------------------------
        // ------------------------------------------------------------------------------

        if($(window).width() > 768){
            $('.reference-links .menu-items').scrollbar({
                "autoScrollSize": true,
                "disableBodyScroll": true
            });
        }
    });
	
	// ------------------------------------------------------------------------------
	// ------------------------ Handle Download Button ------------------------------
	// ------------------------------------------------------------------------------

	if(isMobile.phone === true && typeof $('.icon-download') !== 'undefined' && $('.icon-download').length > 0) {
		$('.icon-download').remove();
	}

	/* Highlight search keyword on content page */
    if(!window.location.pathname.endsWith("search.html")){
        searchKeyword = localStorage.getItem('searchKeyword');
        if(searchKeyword !== null && searchKeyword !== undefined){
            options = {
                "element": "span",
                "className": "is-emphasized",
                "separateWordSearch": false,
                "acrossElements": true
            };
            $('.doc-body-content').mark(searchKeyword, options);
            localStorage.removeItem('searchKeyword');
        }
    }

    //Adding/removing z-index on click on global search
    $('#search-link').on('click',function(){
        $(this).parents('.fix-menu-onscroll').css('z-index', '101');
    });
    $('.close-global-search').on('click',function(){
         $(this).parents('.fix-menu-onscroll').css('z-index', '9');
    });

});

$( document ).ready(function() {
   // LucidWork Search result take user to Top
         setTimeout(function() {
           if(!window.location.pathname.endsWith("search.html") && $(".doc-body-content > div span.is-emphasized").length) {
             var getScrollPos = $(".doc-body-content > div span.is-emphasized:first").offset().top - 200;
             $(window).scrollTop(getScrollPos);
           }
         }, 1000);
});

$( document ).ready(function() {
	  //Scroll down to the target position on desktop , reducing the height of sticky menu//
      if ($('#content-page-full-container').length) {
	  var targethash = $(location.hash.replace(/\./,'\\.'));
	  if ($(targethash).length) {
	  //Scroll down to the target position on mobile devices , reducing the height of sticky menu //
	    setTimeout(function() {
	    if($('.toc-mobile-bar').hasClass('stickymob')) {
           // MD-15004 Scroll issue wit join page
           if (window.location.href.indexOf('joined.html') !== -1) {
             if (!targethash.children('.title').length) {
               $('html, body').animate({
                 scrollTop: 0
               }, 650);
             }
             else {
                if(window.matchMedia("(max-width: 768px)").matches){
                    $('html, body').animate({
                      scrollTop: targethash.offset().top - ($('.toc-mobile-bar').outerHeight())
                    }, 1000);
                 }else{
               $('html, body').animate({
                scrollTop: targethash.offset().top - ($('.toc-mobile-bar').outerHeight()) - $('nav.navbar-madison').outerHeight() 
               }, 1000);
            }
             }
           }
           else {
            if(window.matchMedia("(max-width: 768px)").matches){
                $('html, body').animate({
                  scrollTop: targethash.offset().top - ($('.toc-mobile-bar').outerHeight())
                }, 1000);
             }else{
           $('html, body').animate({
            scrollTop: targethash.offset().top - ($('.toc-mobile-bar').outerHeight()) - $('nav.navbar-madison').outerHeight() -150
           }, 1000);
        }
           }
             // End
	  }
      else{
            $('html, body').animate({
             scrollTop: targethash.offset().top - ($('.toc-mobile-bar').outerHeight()) - $('nav.navbar-madison').outerHeight() -150
            }, 1000);
          }
	    }, 1010);

	  }
    }
      
      // To remove '>' sign from empty title (For Dev team)
      $(".pgroup-title.title:empty").closest('.pgroup').addClass('empty-title');
      // END for empty title
});

//For new Subject-Matter-Expert as Icon
$(document).ready(function() {
	
  function getSmeData(item) {
      if (item !== undefined) {
          var p = item.find(".info-tooltip-popout"),
              name, sme;
          if (p !== undefined) {
              name = p.find("p");
              sme = "";
              if (name !== undefined) {
                  sme = name.contents().filter(function() {
                      return this.nodeType === 3;
                  });
              }
              return $.trim((sme !== undefined && sme.length > 1 && sme[0].textContent !== undefined) ? sme[0].textContent : "");
          }
      }
  }
  
  function addBaseTopic() {
      if (window.digitalData.page && window.digitalData.page.content && window.digitalData.page.content.topic && !$.isEmptyObject(window.digitalData.rightNav)) {
          window.digitalData.rightNav.topic = window.digitalData.page.content.topic;
      }
  }
  
  $('.subject-matter-icon').on("click", function() {
    $('.subject-matter-div').toggleClass('subject-matter-hide');
  });
  
  // For closing Subject-Matter-Expert after clicked anywhere
  $(document).on("click", function(e) {
    var expertIcon = $('.subject-matter-icon'), expertDiv = $('.subject-matter-div');
    if (!expertIcon.is(e.target) && !expertIcon.has(e.target).length && !expertDiv.has(e.target).length ) {
      $('.subject-matter-div').addClass('subject-matter-hide'); 
    }
  });
  
  $("div.title>a").click(function(event) {
      var target = $(event.currentTarget),
          item;
      if (target !== undefined) {
          item = target.closest(".sme-item");
          if (item !== undefined) {
              window.digitalData.rightNav = {};
              window.digitalData.rightNav.sme = getSmeData(item);
              addBaseTopic();
          }
      }
  });

  function addEllipsis() {
    var maxLength, truncatedText,
    screenWidth = window.innerWidth,
    $username = $('.navbar-item.profile-menu h5 > p'),
    outerWrappperWidth = $(".navbar-madison>.container").width(),
    originalText = $username.text();

    // Define maximum characters based on screen width
    if (screenWidth <= 480) { // Small mobile phones
        maxLength = 10;
    } else 
        if (screenWidth <= 768) { // Medium-sized phones
            maxLength = outerWrappperWidth - 260; // Eliminating the space taken by ham menu , fav dropdown and territory dropdown
            maxLength = Math.floor(maxLength / 9); // get the total number of characters required
    } else if (screenWidth <= 1087) { // Screens up to 1087px (ipads)
            maxLength = outerWrappperWidth - 600;
            maxLength = Math.floor(maxLength / 9); 
    } else { // For larger screens
            return; // Don't modify text for larger screens
            // do we need to add code for desktop
    }

    if (originalText.length > maxLength) {
        truncatedText = originalText.substring(0, maxLength) + '...';
        $username.text(truncatedText);
    }

    $username.css({
        "white-space": "normal",
        "overflow": "visible",
        "max-width": "none"
    });
  }

  if(window.matchMedia("(max-width: 1087px)").matches){
   addEllipsis();
  }

});

// MD-16427 Changes

$("body").on("click",'.profile-menu, .language-menu, .favorites-menu, #search-link',function (e) {
    if($(this).attr('id')==='search-link'){
    // remove all existing dropdown
        $(".body").removeClass("show-language-menu");
        $(".body").removeClass("show-account-menu");
        $(".body").removeClass("show-favorites-menu");
        // End
        e.preventDefault();
        if(window.matchMedia("(max-width: 768px)").matches){
            $('#autocompleteSearch').removeClass('dismiss').addClass('selected');
        }
        else{
            $('#autocompleteSearch').addClass('selected').show();
        }
        
        setTimeout(function() {
            $('.auto-suggestion input').get(0).focus();
        }, 800);
    }
    else{
        if(window.matchMedia("(min-width: 769px)").matches){
            $('#autocompleteSearch').hide();
        }
    }
    if($("body").hasClass("show-mega-menu") === true) {
        $("body").removeClass("show-mega-menu");
        $('.mega-menu-nav').css('top', '');
        $('.mega-menu-backdrop').css('height', '').css('top', '');
        $('html').removeClass("hide-scroll");
    } 
  });
  //replacing #year# placeholder in copyright text with current year
(function () {
      window.updateCopyrightYear = function() {
        var $copyrightDiv = $('.doc-body-copyright'), currentYear = new Date().getFullYear(), updatedContent;
        if($copyrightDiv.length>0){
            updatedContent = $copyrightDiv.html().split('#year#').join(currentYear);
            $copyrightDiv.html(updatedContent);
        }
      };
      window.updateCopyrightYear();
}());
//End

