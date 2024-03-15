/*global CustomEvent */
(function(document, $) {

	var UserRegistration = {}, showLogin = false, lwsd, lwsdo, hash, getContentAccessInfo = true;

	UserRegistration.EDIT_PROFILE_API_END = "/bin/userreg/editprofile";
	UserRegistration.GET_USER_API_END = "/bin/userreg/getuser";
	UserRegistration.LOGOUT_API_PATH = "/bin/userreg/logout";
	UserRegistration.COMPLETE_PROFILE_API_PATH = "/bin/userreg/completeprofile";
	UserRegistration.USER_REIRECTION_PATH_COOKIE = "madison-redirection";
	UserRegistration.COMMA = ",";
	UserRegistration.PLUS_SIGN = "+";
	UserRegistration.BLANK_SPACE = " ";
	UserRegistration.AT_SYMBOL = "@";
	UserRegistration.EDIT_PREFERENCES_TERRITORY_LANGUAGE_END = "/bin/userreg/preferences/territory-lang";
	UserRegistration.EDIT_PREFERENCES_CONTENT_API_END = "/bin/userreg/preferences/content";
	UserRegistration.STATUS_CODE_UNAUTHORIZED = 401;
	UserRegistration.ACCEPT_TNC_END = "/bin/userreg/accepttnc";
	UserRegistration.GET_RECENTLY_VIEWED_END = "/bin/pwc-madison/getrecentlyviewed";
	UserRegistration.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME = "viewpoint-profile-userinfo";
	UserRegistration.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX = "viewpoint-profile-userinfo-";
	UserRegistration.IS_GATED_ACTIVATION_PAGE_QUERY_PARAM = "isGatedActivationPage";
	UserRegistration.REFERRER_QUERY_PARAM = "referrer";
	UserRegistration.FAVORITE_LIST_LOCAL_STORAGE = "favorite-list-data";
	UserRegistration.FAVORITE_LIST_DOT_STATUS_LOCAL_STORAGE = "favorite-list-dot-status";
	UserRegistration.FAVORITE_LIST_ADD_END = "/bin/userreg/addfavoritelist";
	UserRegistration.FAVORITE_LIST_DELETE_END = "/bin/userreg/deletefavoritelist";
	UserRegistration.FAVORITE_LIST_GETALL_END = "/bin/userreg/getfavoritelist";
	UserRegistration.PAGE_HASH_VALUE = "hash";
	UserRegistration.CONTENT_LICENSE_COOKIE = "vp_sli";
	UserRegistration.EXTEND_MADISON_USER_SESSION_API_PATH = "/bin/userreg/extend/session";
	UserRegistration.EXTEND_CONCURRENT_LICENSE_SESSION_COOKIE = "vp_ecs";
	UserRegistration.EXTEND_SESSION_COUNTER_KEY = "sessionCounter";
	UserRegistration.COOKIE_EXPIRY_TIME_KEY = "cookieExpiryTimeInMs";
	UserRegistration.EXTEND_SESSION_COUNTER_LIMIT = 3;
	UserRegistration.EXTERNAL_USER_AUTHENTICATION_TOKEN_QUERY_PARAMETER_NAME = "extauth";
	UserRegistration.USER_LOGIN_COMPLETE_COOKIE = "vp_lc";
	UserRegistration.LOGOUT_REDIRECT_API_PATH = "/bin/userreg/redirect";
	UserRegistration.COMPLETE_PROFILE_COOKIE = "vp_cp";
	UserRegistration.SHOW_LOGIN="showLogin";
	UserRegistration.SHOW_REMOVE_SESSION = "vp_rs";
	UserRegistration.ANCHOR_SHOW_LOGIN = "#showLogin";
	UserRegistration.ANCHOR_SHOW_LOGIN_POSTFIX = "-showLogin";
	UserRegistration.INDUSTRY_LIST_API_END = "/bin/pwc-madison/getIndustries";
	UserRegistration.SIGNAL_HASH_PREFIX = "#lw-";
    UserRegistration.FUSION_SIGNAL_ENDPOINT = "/bin/pwc-madison/vp-signal?_cookie=false";

	UserRegistration.getCurrentUser = function(successCallback, errorCallback) {
		$.ajax({
			url : UserRegistration.GET_USER_API_END,
			type : "GET",
			contentType : 'application/json; charset=utf-8',
			success : function(result) {
				UserRegistration.userProfile = result.data.userProfile;
				/* event to notify angular about the user-profile update */
				//window.dispatchEvent(new CustomEvent('user-profile-update', {}));
				var customEvent = document.createEvent("CustomEvent");
				customEvent.initCustomEvent('user-profile-update', false, false,{
				});
				document.dispatchEvent(customEvent);
				if (successCallback) {
					successCallback(result);
				}
			},
			error : function(xhr, httpStatusMessage, customErrorMessage) {
                if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
					UserRegistration.performActionOnUnauthorized();
                }
				if (errorCallback) {
					errorCallback(xhr);
				}
			}
		});
	};

    UserRegistration.getRecentlyViewed = function(successCallback, errorCallback) {
        $.ajax({
            url: UserRegistration.GET_RECENTLY_VIEWED_END,
            type: "GET",
            contentType: 'application/json; charset=utf-8',
            success: function(result) {
                UserRegistration.recentlyViewed = result;
                if (successCallback) {
                    successCallback(result);
                }
            },
            error: function(xhr, httpStatusMessage, customErrorMessage) {
                if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
					UserRegistration.performActionOnUnauthorized();
                }
                if (errorCallback) {
                    errorCallback(xhr);
                }
            }
        });
    };

	UserRegistration.getQueryParameterValue = function(parameter) {
		var currentUrl = window.location.href.replace(window.location.hash, ""), parameters = currentUrl.slice(
				currentUrl.indexOf('?') + 1).split('&'), keyValue, parameterValue;
		$.each(parameters, function(index, value) {
			keyValue = value.split("=");
			if (keyValue[0] === parameter) {
				parameterValue = keyValue[1];
			}
		});
		return parameterValue ? decodeURI(parameterValue) : parameterValue;
	};

	UserRegistration.removeQueryParameters = function(rParameters) {
		var currentUrl = window.location.href.replace(window.location.hash, ""), parameters = currentUrl.indexOf('?') > -1 ? currentUrl.slice(
				currentUrl.indexOf('?') + 1).split('&') : [], keyValue, url = currentUrl
				.split("?")[0], newParameters = [];
		$.each(parameters, function(index, value) {
			keyValue = value.split("=");
			if (rParameters.indexOf(keyValue[0]) < 0) {
				newParameters.push(value);
			}
		});
		return url + (newParameters.length > 0 ? "?" + newParameters.join("&") : "") + window.location.hash;
	};
	
	UserRegistration.addQueryParameter = function(parameter, value) {
		var currentUrl = window.location.href.replace(window.location.hash, ""), parameters = currentUrl.indexOf('?') > -1 ? currentUrl.slice(
				currentUrl.indexOf('?') + 1).split('&') : [], url = currentUrl
				.split("?")[0], qParameter = parameter + "=" + value;
		return (url.substr(0, url.indexOf('#')) || url) + "?" +  qParameter + (parameters.length > 0 ? "&" + parameters.join("&")  : "") + window.location.hash;
	};
	
	UserRegistration.isEmailValid = function(email) {
		return (/^\w+([\.\-']?\w+)*@\w+([\.\-]?\w+)*(\.\w{2,63})+$/).test(email);
	};

    UserRegistration.isNameValid = function(name) {
		return (/^[0-9a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñйçčæšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð.,\-_’' ]{0,64}$/).test(name);
    };
	
	UserRegistration.setCookie = function(cname, cvalue, exdays) {
		var currentDate = new Date(), expires, secure;
		currentDate.setTime(currentDate.getTime() + (exdays*24*60*60*1000));
		expires = "expires="+ currentDate.toUTCString();
		secure = window.location.protocol === "https:" ? ";secure" : "";
		document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/" + secure;
	};
	
	UserRegistration.getCookie = function(cname) {
		  var name = cname + "=", 
		      decodedCookie = decodeURIComponent(document.cookie),
		      ca = decodedCookie.split(';'), c, i;
		  for(i = 0; i <ca.length; i++) {
		      c = ca[i];
		      while (c.charAt(0) === ' ') {
		          c = c.substring(1);
		      }
		      if (c.indexOf(name) === 0) {
		          return c.substring(name.length, c.length);
		      }
		  }
		  return "";
	};

	UserRegistration.isBlacklistedDomain = function(email) {
		var blacklistedDomains = $('input[name=blacklisted-domains]').val().split(UserRegistration.COMMA),
        index, regex, domain;
        for(index = 0; index < blacklistedDomains.length; index++) {
            try {
                regex = new RegExp(blacklistedDomains[index]);
                domain = email.substring(email.lastIndexOf(UserRegistration.AT_SYMBOL) +1);
                if(regex.test(domain)) {
                    return true;
                }
            } catch(err) {

            }
        }
        return false;
	};
	
	UserRegistration.getUserInfo = function(updateInfo) {
		if(UserRegistration.isUserLoggedIn) {
			if(UserRegistration.userInfo && !updateInfo) {
				return UserRegistration.userInfo;
			}
			else {
				var userInfoCookie = UserRegistration.getCookie(UserRegistration.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME), userInfoCookies, userInfo = "";
				if(userInfoCookie) {
					userInfoCookies = userInfoCookie.split(UserRegistration.COMMA);
					$.each(userInfoCookies, function(index, value){
						userInfo += decodeURIComponent(UserRegistration.getCookie(UserRegistration.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX + value)).split(UserRegistration.PLUS_SIGN).join(UserRegistration.BLANK_SPACE);
					});
					UserRegistration.userInfo = JSON.parse(userInfo);
                    if(!UserRegistration.userInfo.contentAccessInfo && getContentAccessInfo){
                        UserRegistration.getCurrentUser(UserRegistration.setUserInfo);
                    }
                    else {
                        getContentAccessInfo = false;
                    }
					return UserRegistration.userInfo;
				}
				else{
					UserRegistration.getCurrentUser(UserRegistration.setUserInfo);
				}
			}
		}
	};
	
	UserRegistration.setUserInfo = function() {
		if(UserRegistration.isUserLoggedIn) {
			UserRegistration.getUserInfo(true);
			$(document).trigger("userDataChange");
		}
	};
	
    if(UserRegistration.isUserLoggedIn) {
        UserRegistration.getUserInfo(true);
        $(document).trigger("userDataChange");
    }
	
	UserRegistration.sanitizeString = function (string) {
		var tempDiv = document.createElement('div');
		tempDiv.textContent = string;
		return tempDiv.innerHTML;
	};

	UserRegistration.getReferrer = function() {
	    var isPremiumPageRequest = UserRegistration.getQueryParameterValue("isPremium");
        if(isPremiumPageRequest === 'true') {
            return UserRegistration.removeQueryParameters(["isPremium", "refererHeader"]);
            //return UserRegistration.addQueryParameter(UserRegistration.IS_GATED_ACTIVATION_PAGE_QUERY_PARAM, true);
        }
        else {
          return window.origin + $("#gated-content-redirect-url").val() + window.location.hash;
        }

	};

		
	$(document).on("click", ".user-login", function(event, data) {
		$(document).trigger("loginStart", [{"callbackMethod" : UserRegistration.UserLoginRedirection, "event" : event, "removeSession" : data && data.removeSession}]);
	});
	    
	$(document).on("click", ".user-register", function(event) {
        UserRegistration.UserRegisterRedirection(event);
    });

	UserRegistration.isUserLoggedIn = document.cookie
			&& document.cookie.indexOf('vp_ic') !== -1 ? true : false;

	UserRegistration.UserLoginRedirection = function(data) {
		setTimeout(function() {
	        UserRegistration.setCookie(UserRegistration.USER_REIRECTION_PATH_COOKIE, UserRegistration.removeQueryParameters([UserRegistration.SHOW_LOGIN]));
		    window.location.href = $(data.event.target).data("href") + (data.removeSession ? "?deleteTokens=1" : "");
		}, 1000);
	};

	UserRegistration.UserRegisterRedirection = function(event) {
		$(document).trigger("registrationStart");
	    window.location.href = $(event.target).data("registerLink") + encodeURIComponent($("#gatedModal").length ? UserRegistration.getReferrer() : window.location.href);
	};

	$(document).on("click", ".userreg-modal-close", function(event){
		window.closeModal(event.target);
	});
            
    UserRegistration.currentTimeInMinutes = function(){
        return Math.floor(Date.now() / 60000);
    };
    
    UserRegistration.performActionOnUnauthorized = function(){
		window.localStorage.removeItem(UserRegistration.FAVORITE_LIST_SESSION_STORAGE);
		window.localStorage.removeItem(UserRegistration.FAVORITE_LIST_DOT_STATUS_LOCAL_STORAGE);
		location.reload();
    };
    
    UserRegistration.handleHashForLogin = function(){
        showLogin = false;
        if(window.location.hash){
            if(window.location.hash === UserRegistration.ANCHOR_SHOW_LOGIN){
                window.location.hash = "";
                window.history.pushState({}, "", window.location.href.replace("#", ""));
                showLogin = true;
            }
            else if(window.location.hash.endsWith(UserRegistration.ANCHOR_SHOW_LOGIN_POSTFIX)){
                window.location.hash = UserRegistration.sanitizeString(window.location.hash.replace(UserRegistration.ANCHOR_SHOW_LOGIN_POSTFIX, ""));
                showLogin = true;
            }
            if(!UserRegistration.isUserLoggedIn && showLogin){
                $(".user-login").trigger("click");
            }
        }
    };

	$(document).ready(function () {
		// Redirect to Login if url contains showLogin parameter
		if(UserRegistration.getQueryParameterValue(UserRegistration.SHOW_LOGIN)){
			$(".user-login").trigger("click");
		}

		// Setting login-complete analytics event
		if(UserRegistration.getCookie(UserRegistration.USER_LOGIN_COMPLETE_COOKIE)){
			$(document).trigger("loginComplete");
			UserRegistration.setCookie(UserRegistration.USER_LOGIN_COMPLETE_COOKIE, undefined, 0);
		}
		
		UserRegistration.handleHashForLogin();
		
		$(window).on("hashchange", function(){
			UserRegistration.handleHashForLogin();
		});
	});
	
	UserRegistration.postSignalCall = function(){
        $.ajax({
            url : UserRegistration.FUSION_SIGNAL_ENDPOINT,
            type : "POST",
            contentType : 'application/json; charset=utf-8',
            data: lwsd,
            success : function(response) {
            },
            error : function(err) {
            }
        });
    };
	
    hash = window.location.hash;
    lwsd = window.localStorage.getItem(hash);
    if(hash.startsWith(UserRegistration.SIGNAL_HASH_PREFIX) && lwsd){
        lwsdo = window.JSON.parse(lwsd);
        if(lwsdo[0].params.docId + hash === window.location.href){
            window.location.hash = "";
            window.history.pushState({}, "", window.location.href.replace("#", ""));
            window.localStorage.removeItem(hash);
            window.setTimeout(UserRegistration.postSignalCall, lwsdo[0].params.timeSpent);
        }
    }

	window.UserRegistration = UserRegistration;
}(document, $));

