var isIE11 = window.navigator !== undefined && window.navigator.msSaveOrOpenBlob !== undefined;

function showSubMenuList(element) {
  if (element.find('.mega-menu-sub').length) {
    var menuHtml = element.find('.mega-menu-sub ul').html();
    if (menuHtml !== '') {
      $('.mega-menu-sub-box ul').html(menuHtml);
      $('.mega-menu-sub-box').show();
    } else {
      $('.mega-menu-sub-box ul').html('');
      $('.mega-menu-sub-box').hide();
    }
  } else {
    $('.mega-menu-sub-box ul').html('');
    $('.mega-menu-sub-box').hide();
  }
}

$(document).ready(function () {
  var isMobile = window.matchMedia("only screen and (max-width: 768px)").matches,
    menuItem = $('.mega-menu-sub > ul > li a');
  menuItem.each(function (index, value) {
    $clamp(value, {
      clamp: 2
    });
  });

  $('body').addClass('body');

  $(".navbar-hamburger > a").click(function () {
	if($("body").hasClass("show-mega-menu") === true) {
		$("body").removeClass("show-mega-menu");
		$('.mega-menu-nav').css('top', '');
		$('.mega-menu-backdrop').css('height', '').css('top', '');
		$('html').removeClass("hide-scroll");
	} else {
        // remove all existing dropdown
        $(".body").removeClass("show-language-menu");
	    $(".body").removeClass("show-account-menu");
	    $(".body").removeClass("show-favorites-menu");
        if(window.matchMedia("(min-width: 769px)").matches){
            $('#autocompleteSearch').hide();
        }
        // End
		$("body").addClass("show-mega-menu");
		$('.mega-menu-backdrop').css('height', ($(window).outerHeight() - $('.fix-menu-onscroll').outerHeight()) + 'px').css('top', $('.fix-menu-onscroll').outerHeight() + 'px');
		$('.mega-menu-nav').css('top', $('.navbar.subnav').outerHeight() + 'px');
		$('html').addClass("hide-scroll");
		if($(window).outerWidth() <= 768) {
			window.hideNavMessage();
		}
	}
  });

  if (isMobile) {
    $(".mega-menu-nav li > a").click(function() {
        var listCount = $(this).parent().find(".mega-menu-sub > ul > li").length,a,b;
        if (listCount <= 20) {
          $(this).parent().find(".mega-menu-sub > ul").css({"grid-template-columns": "repeat(2, 1fr)", "display": "grid", "grid-template-rows": "repeat(10, 82px)", "grid-auto-flow": "column", "grid-gap": "8px" });
        } else {
          a = listCount / 2;
          b = Math.round(a);
          $(this).parent().find(".mega-menu-sub > ul").css({"grid-template-columns": "repeat(2, 1fr)", "display": "grid", "grid-template-rows": "repeat("+b+", 82px)", "grid-auto-flow": "column", "grid-gap": "8px" });
        }
    });
    $(".submenu-link > a").click(function () {
      $(this)
        .parent()
        .parent()
        .find(".mega-menu-sub")
        .removeClass("active");
      $(".body").addClass("show-mega-menu-mobile");
      $(this)
        .parent()
        .find(".mega-menu-sub")
        .addClass("active");
      $(this)
        .parent()
        .find(".mega-menu-sub")
        .show();
    });

    $(".mega-menu-sub .submenu-link>a").click(function () {
      $(this).parent().toggleClass('mobile-view');
      var current_dropdown = $(this).parent().find(".mega-sub-sub");
      $(".mega-sub-sub").not(current_dropdown).slideUp();
      current_dropdown.slideToggle();
      return false;
    });
  } else {

    $("body")
      .find(".mega-menu-sub")
      .hide();
    $(".mega-menu-nav > ul > li > a").hover(function () {
      $('.mega-menu-nav li').removeClass('hover');
      $(this).parent().addClass('hover');
      if (
        $(this)
          .parent()
          .find(".mega-menu-sub").length
      ) {
        var menuHtml = $(this)
          .parent()
          .find(".mega-menu-sub ul")
          .html(),listCount,a,b;
        if (menuHtml !== "") {
          $(".mega-menu-sub-box ul").html(menuHtml);
          $(".mega-menu-sub-box").show();
          listCount = $(this).parent().find(".mega-menu-sub > ul > li").length;
           if (listCount <= 30) {
                $(this).parents().find(".mega-menu-sub > ul").css({"grid-template-columns": "repeat(3, 1fr)", "display": "grid", "grid-template-rows": "repeat(10, 82px)", "grid-auto-flow": "column", "grid-gap": "8px" });
                if(isIE11 && listCount <= 10){
                    $(this).parents().find(".mega-menu-sub > ul").css({"column-count":"1","column-gap":"20px" });
                }
                if(isIE11 && (listCount > 10 && listCount<=20)){
                    $(this).parents().find(".mega-menu-sub > ul").css({"column-count":"2","column-gap":"20px" });
                }
                if(isIE11 && listCount > 20){
                    $(this).parents().find(".mega-menu-sub > ul").css({"column-count":"3","column-gap":"20px" });
                }
           } else {
               a = listCount / 3;
               if (Number.isInteger(a)) {
                  $(this).parents().find(".mega-menu-sub > ul").css({"grid-template-columns": "repeat(3, 1fr)", "display": "grid", "grid-template-rows": "repeat("+a+", 82px)", "grid-auto-flow": "column", "grid-gap": "8px" });
                  if(isIE11){
                    $(this).parents().find(".mega-menu-sub > ul").css({"column-count":"3","column-gap":"20px" });
                  }
               } else {
                   a = a + 1;
                   b = Math.trunc(a);
                   $(this).parents().find(".mega-menu-sub > ul").css({"grid-template-columns": "repeat(3, 1fr)", "display": "grid", "grid-template-rows": "repeat("+b+", 82px)", "grid-auto-flow": "column", "grid-gap": "8px" });
                   if(isIE11){
                    $(this).parents().find(".mega-menu-sub > ul").css({"column-count":"3","column-gap":"20px" });
                   }
               }
           }
          $(".mega-menu-sub-box .submenu-link").on("click", function() {
            var offset = $(this).position(),
              margins = parseInt($(this).css('margin-left'), 10) + parseInt($(this).css('margin-right'), 10),
              leftPadding = offset.left + $('a', this).outerWidth() + margins,
              topPadding = $(this).innerHeight(),
              windowWidth = $(window).width(),
              rightWidth = ($(window).width() - leftPadding),
              subListCount,a,b,megaSub = $(this).find(".mega-sub-sub");

            if (megaSub.css('display') === 'block') {
                 megaSub.hide();
                 $(this).children("a").removeClass("active-menu");
            } else {
                 $(".mega-sub-sub").hide();
                 megaSub.show();
                 $(".mega-menu-sub-box .submenu-link").children("a").removeClass("active-menu");
                 $(this).children("a").addClass("active-menu");
            }
            if (leftPadding < (windowWidth * 0.6)) {
              $(this).find(".mega-sub-sub").css("left", leftPadding + 8);
              $(this).find(".mega-sub-sub").css("width", rightWidth);
              subListCount = $(this).find(".mega-sub-sub li").length;
              if (subListCount <= 20) {
                 $(this).find(".mega-sub-sub > ul").css({"grid-template-columns": "repeat(2, 1fr)", "display": "grid", "grid-template-rows": "repeat(10, 82px)", "grid-auto-flow": "column", "grid-gap": "8px" });
              } else {
                 a = subListCount / 2;
                 b = Math.round(a);
                 $(this).find(".mega-sub-sub > ul").css({"grid-template-columns": "repeat(2, 1fr)", "display": "grid", "grid-template-rows": "repeat("+b+", 82px)", "grid-auto-flow": "column", "grid-gap": "8px" });
              }
            }else if(leftPadding > (windowWidth * 0.6) &&  leftPadding < (windowWidth * 0.8) ){
              $(this).find(".mega-sub-sub").css("left", leftPadding + 8);
              $(this).find(".mega-sub-sub").css("width", rightWidth);
              $(this).find(".mega-sub-sub").addClass("sinlge_nav_item");

            } else if (leftPadding > (windowWidth * 0.8)) {
              $(this).css("position", "relative");
              $(this).find(".mega-sub-sub").css("top", topPadding);
              $(this).find(".mega-sub-sub").addClass("sinlge_nav_item last_nav_item");
            }
          });
        } else {
          $(".mega-menu-sub-box ul").html("");
          $(".mega-menu-sub-box").hide();
        }
      } else {
        $(".mega-menu-sub-box ul").html("");
        $(".mega-menu-sub-box").hide();
      }
    });

  }

  $(".close-sub-menu").click(function () {
    $(".body.show-mega-menu-mobile").removeClass("show-mega-menu-mobile");
  });

  $(".close-mega-menu").click(function () {
    $(".body.show-mega-menu").removeClass("show-mega-menu");
    $("html").removeClass("hide-scroll");
  });

  $(".close-mega-menu").click(function () {
    $(".body.show-mega-menu-mobile").removeClass("show-mega-menu-mobile");
    $("html").removeClass("hide-scroll");
  });
  
});
