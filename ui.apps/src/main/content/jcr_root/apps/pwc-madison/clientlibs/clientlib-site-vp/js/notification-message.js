//----------------------------------------------------------------
//-------------------- HomePage Nav Message ----------------------
//----------------------------------------------------------------
var notificationMessageKey = "notification-message-", notificationMessageElement = $(".navbar-message"), notificationMessage = notificationMessageElement.remove(".hide-nav-message").html();

if (typeof window.getLocale === "function") {
    notificationMessageKey += window.getLocale();
}

function navMessageVisible() {
	$('body').addClass('navbar-message-visible');
	var messageHeight, totalHeight;
	if($("body").hasClass("show-mega-menu")) {
		totalHeight = messageHeight + $('.navbar-madison.primary').outerHeight();
	
		$('.mega-menu-nav').css('top', messageHeight + 'px');
		$('.mega-menu-backdrop').css('height', ($(window).outerHeight() - totalHeight) + 'px').css('top', totalHeight + 'px');
	}
	
	setTimeout(function() {
		messageHeight = $('.subnav .navbar-message p').outerHeight() + parseInt($('.subnav .navbar-message').css('padding-bottom'), 10) + parseInt($('.subnav .navbar-message').css('padding-top'), 10);
		
		//63 is set css height of subnav for mobile view
		messageHeight = messageHeight < 63 ? 63 : messageHeight;
	
		$('.navbar.subnav').css('height', messageHeight + 'px');
		$('.navbar.subnav .container').css('height', messageHeight + 'px');      
		if($("body").hasClass("show-mega-menu")) {
			totalHeight = messageHeight + $('.navbar-madison.primary').outerHeight();
			$('.mega-menu-nav').css('top', messageHeight + 'px');
			$('.mega-menu-backdrop').css('height', ($(window).outerHeight() - totalHeight) + 'px').css('top', totalHeight + 'px');
		}
	}, 100);
}

function hideNavMessage(doClose) {
	if(doClose) {
		$('.navbar-message').remove();
	}
	
	$('body').removeClass('navbar-message-visible');
	$('.navbar.subnav').css('height', '');
	$('.navbar.subnav .container').css('height', '');
	
	if($("body").hasClass("show-mega-menu") && $(window).outerWidth() >= 769) {
		$('.mega-menu-nav').css('top', $('.navbar.subnav').outerHeight() + 'px');
		$('.mega-menu-backdrop').css('height', ($(window).outerHeight() - $('.fix-menu-onscroll').outerHeight()) + 'px').css('top', $('.fix-menu-onscroll').outerHeight() + 'px');
	
		setTimeout(function() {
			$('.mega-menu-nav').css('top', $('.navbar.subnav').outerHeight() + 'px');
			$('.mega-menu-backdrop').css('height', ($(window).outerHeight() - $('.fix-menu-onscroll').outerHeight()) + 'px').css('top', $('.fix-menu-onscroll').outerHeight() + 'px');
		}, 300);
	} else {
		$('.mega-menu-nav').css('top', '');
		$('.mega-menu-backdrop').css('height', '').css('top', '');  
	}
	
	window.tocMenuFix();
}

function adjustNavMessage() {
	if($(window).outerWidth() > 768 && $('.navbar-message').length > 0) {
		navMessageVisible();
	} else {
		hideNavMessage();
	}
}

$(document).ready(function () {
	
	if(notificationMessageElement.length){
		
		if(window.isHomePage === 'true') {
			var lastAcceptedMessage = window.localStorage.getItem(notificationMessageKey);
			if(lastAcceptedMessage !== notificationMessage) {
			    setTimeout(function() {
			        adjustNavMessage();
			    }, 300); 
			    $(window).on('resize', function() {
			        adjustNavMessage();
			    });
			    
			    $('.hide-nav-message').on('click', function() {
                    window.localStorage.setItem(notificationMessageKey, notificationMessage);
			        hideNavMessage(true);
			    });
			}
			else {
				$(".navbar-message").remove();
			}
		}
		else {
			$(".navbar-message").remove();
		}    
	}
	
});