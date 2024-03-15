/**
 * ALERT TILE FUNCTIONALITY SCRIPT
 * Custom functionality that drives 
 * client side data populate architecture
 * for Alert tile component over 
 * Homepage
 * @author Divanshu
 */

$(document).ready(function () {
	var alertsListSize, showAlertsCompare,clampLines, HTTP_DETAILS, getExpiry, setDismissedAlertsCookie, ajaxCall, showAlerts, checkCloseCookie, setDismissCookie, removeSlideOnDismiss, message, hideComponent, isContentAvailable, mobileSlideRemove, sanitizeString, keepCookie;
	alertsListSize = $('.dialog-messages').attr('data-size');
	isContentAvailable = $('.dialog-messages').attr('data-iscontentavailable');
    keepCookie = null !== window.OptanonActiveGroups && typeof window.OptanonActiveGroups === "string" && window.OptanonActiveGroups.includes(',3,');
	HTTP_DETAILS = {
		READ_COOKIE_URL: "/bin/pwc-madison/ReadCookie",
		DISMISS_COOKIE_URL: "/bin/pwc-madison/DismissCookie",
		METHOD_GET: "GET",
		METHOD_POST: "POST",
		RESPONSE_NO_CONTENT: 204,
		RESPONSE_SUCCESS: 200
	};
	sanitizeString = function (string) {
		var tempDiv = document.createElement('div');
		tempDiv.textContent = string;
		return tempDiv.innerHTML;
	};
	getExpiry = function (exDays) {
		var d, expires;
		d = new Date();
		d.setTime(d.getTime() + (exDays * 24 * 60 * 60 * 1000));
		expires = "expires=" + d.toUTCString();
		return expires;
	};
	setDismissedAlertsCookie = function () {
		var dismissedAlertsGlobalCookie = ';path=/';
		document.cookie.split(";").forEach(function (item) {
			if (item.includes("pwc-dismissed-alerts=")) {
				dismissedAlertsGlobalCookie = item.split("=")[1] + ";path=/";
                return;
			}
		});
		document.cookie = "pwc-dismissed-alerts-global=" + dismissedAlertsGlobalCookie;
	};
	ajaxCall = function (url, httpMethod, dataToPost, onSuccess, onError) {
		$.ajax({
			type: httpMethod,
			async: true,
			data: dataToPost,
			url: url,
			success: function (data, textStatus, xhr) {
				if (onSuccess !== null) {
					onSuccess(data, textStatus, xhr);
				}
			},
			error: function (err) {
				if (onError !== null) {
					onError(err);
				}
			}
		});
	};
	clampLines = function (selector,noOfLines) {
		var limitedword = $(selector);
		limitedword.each(function (index, value) {
			$clamp(value, {
			  clamp: noOfLines
			});
		  });
	};
	showAlerts = function () {
		$('.navbar.dark-subnav.is-hidden-mobile.is-hidden-tablet-only').show();
		$('.register-top-mobile.is-hidden-desktop-only.is-hidden-widescreen-only.is-hidden-fullhd').show();
	};
	checkCloseCookie = function () {
		var isCloseTrue = false;
		document.cookie.split(";").forEach(function (item) {
			if (item.includes("close")) {
				if (item.split("=")[1] === "true") {
					isCloseTrue = true;
				}
			}
		});
		return isCloseTrue;
	};
	setDismissCookie = function (cookie) {
		var data = { "dismissedPagesValue": cookie };
		ajaxCall(HTTP_DETAILS.DISMISS_COOKIE_URL, HTTP_DETAILS.METHOD_POST, data, setDismissedAlertsCookie, null);
	};
	removeSlideOnDismiss = function (itemsCount, currentElement) {
		var slideIndex = currentElement.index();
		$('.dark-subnav-slick-slider').slick('slickRemove', slideIndex);
		if (slideIndex >= 1) {
			if (itemsCount > 0) {
				$('.dark-subnav-slick-slider').slick("slickGoTo", --slideIndex);
				$('.steps-text .current').text($('.dark-subnav-slick-slider').slick("slickCurrentSlide") + 1);
				$('.steps-text .total').text(itemsCount - 1);
			} else {
				hideComponent();
			}
		} else if (slideIndex === 0) {
			if (itemsCount > 1) {
				$('.dark-subnav-slick-slider').slick("slickGoTo", slideIndex);
				$('.steps-text .current').text($('.dark-subnav-slick-slider').slick("slickCurrentSlide") + 1);
				$('.steps-text .total').text(itemsCount - 1);
			} else {
				hideComponent();
			}
		}
	};
	mobileSlideRemove = function () {
		var mobileCarousel = $('.register-mobile-nav'),
			slideIndex = mobileCarousel.slick('slickCurrentSlide'), slick;
		mobileCarousel.slick('slickRemove', slideIndex);
		slick = mobileCarousel.slick('getSlick');
		$('.m-total-count').text(slick.slideCount);
		$('.m-current-count').each(function (index, value) {
			var val = $(value).text();
			if (val > slideIndex) {
				$(value).text(val - 1);
			}
		});
		if(slick.slideCount === 0){
		    hideComponent();
		}
	};
	hideComponent = function () {
		$('.navbar.dark-subnav.is-hidden-mobile.is-hidden-tablet-only').hide();
		$('.register-top-mobile.is-hidden-desktop-only.is-hidden-widescreen-only.is-hidden-fullhd').hide();
	};
	showAlertsCompare = $('.dark-subnav-slick-slider').length !== 0 && alertsListSize > 0 && !checkCloseCookie();
	if (showAlertsCompare && keepCookie) {
		showAlerts();
	}
	if (checkCloseCookie()) {
		hideComponent();
	}
	
	$(document).on('click', '#btnCross, #btnCrossMobile', function () {
		var expiresVal = getExpiry(365), secure = window.location.protocol === "https:" ? ";secure" : "";
		document.cookie = "close = " + true + ";" + expiresVal + ";path=/" + secure;
		hideComponent();
	});
	$(".dark-subnav .alert-close-btn").click(function () {
		var dismissedContentArr = [],
			allAlertsArr = [],
			$allAlerts = $('.dark-subnav-slick-slider').find('.slick-slide'), dismissCallSuccess, ctaLink, pagePath;
		$allAlerts.each(function () {
			ctaLink = $(this).find('.ctaLink');
			pagePath = ctaLink.attr('data-pagepath');
			if (pagePath) {
				allAlertsArr.push(pagePath);
			}
		});
		dismissCallSuccess = function (data, textStatus, xhr) {
			if (xhr.status === HTTP_DETAILS.RESPONSE_NO_CONTENT || xhr.status === HTTP_DETAILS.RESPONSE_SUCCESS) {
			    if(xhr.status === HTTP_DETAILS.RESPONSE_SUCCESS) {
			        dismissedContentArr = dismissedContentArr.concat(data);
			    }
				dismissedContentArr = dismissedContentArr.concat(allAlertsArr);
				setDismissCookie(dismissedContentArr);
				hideComponent();
			}
		};
		ajaxCall(HTTP_DETAILS.DISMISS_COOKIE_URL, HTTP_DETAILS.METHOD_GET, null, dismissCallSuccess, null);
	});
	$('.register-top-mobile .alert-close-btn').click(function () {
		var dismissedContentArr = [],
			allAlertsArr = [],
			$allAlerts = $('.register-mobile-nav.slick-slider').find('.slick-slide'), dismissCallMobileSuccess, ctaLink, pagePath;
		$allAlerts.each(function () {
			ctaLink = $(this).find('.ctaLink-mobile');
			pagePath = ctaLink.attr('data-pagepath');
			if (pagePath) {
				allAlertsArr.push(pagePath);
			}
		});
		dismissCallMobileSuccess = function (data, textStatus, xhr) {
			if (xhr.status === HTTP_DETAILS.RESPONSE_NO_CONTENT || xhr.status === HTTP_DETAILS.RESPONSE_SUCCESS) {
				if (xhr.status === HTTP_DETAILS.RESPONSE_SUCCESS) {
					dismissedContentArr = dismissedContentArr.concat(data);
				}
				dismissedContentArr = dismissedContentArr.concat(allAlertsArr);
				setDismissCookie(dismissedContentArr);
				hideComponent();
			}
		};
		ajaxCall(HTTP_DETAILS.DISMISS_COOKIE_URL, HTTP_DETAILS.METHOD_GET, null, dismissCallMobileSuccess, null);
	});
	$(".ctaLink").click(function () {
		var clickedPath = $(this).attr('data-pagePath'),
			currentElement = $($(this).parents('.slick-slide')),
			dismissedContentArr = [],
			itemsCount = $('.dark-subnav-slick-slider').find('.slick-slide').length, dismissCallSuccess;
		dismissCallSuccess = function (data, textStatus, xhr) {
			if (xhr.status === HTTP_DETAILS.RESPONSE_NO_CONTENT) {
				dismissedContentArr.push(clickedPath);
				setDismissCookie(dismissedContentArr);
				removeSlideOnDismiss(itemsCount, currentElement);
			} else if (xhr.status === HTTP_DETAILS.RESPONSE_SUCCESS) {
				dismissedContentArr = data;
				dismissedContentArr.push(clickedPath);
				setDismissCookie(dismissedContentArr);
				removeSlideOnDismiss(itemsCount, currentElement);
			}
		};
		setDismissedAlertsCookie();
		ajaxCall(HTTP_DETAILS.DISMISS_COOKIE_URL, HTTP_DETAILS.METHOD_GET, null, dismissCallSuccess, null);
	});
	$('.ctaLink-mobile').click(function () {
		var clickedPath = $(this).attr('data-pagePath'),
			dismissedContentArr = [],
			dismissCallMobileSuccess;
		dismissCallMobileSuccess = function (data, textStatus, xhr) {
			if (xhr.status === HTTP_DETAILS.RESPONSE_NO_CONTENT) {
				dismissedContentArr.push(clickedPath);
				setDismissCookie(dismissedContentArr);
				mobileSlideRemove();
			} else if (xhr.status === HTTP_DETAILS.RESPONSE_SUCCESS) {
				dismissedContentArr = data;
				dismissedContentArr.push(clickedPath);
				setDismissCookie(dismissedContentArr);
				mobileSlideRemove();
			}
		};
		setDismissedAlertsCookie();
		ajaxCall(HTTP_DETAILS.DISMISS_COOKIE_URL, HTTP_DETAILS.METHOD_GET, null, dismissCallMobileSuccess, null);
	});
    if(keepCookie) {
	    setDismissedAlertsCookie();
    }
});
