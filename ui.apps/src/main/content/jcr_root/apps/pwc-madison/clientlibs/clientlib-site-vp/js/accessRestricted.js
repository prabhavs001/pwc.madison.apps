(function(document, $, hideBlur, showBlur, UserRegistration, AccessRestriction){

    	var isInternalUser, getUrlParameter, hasSameOrigin, referrerPath;
    	if(UserRegistration.getUserInfo()){
            isInternalUser = UserRegistration.getUserInfo().userAccountType.indexOf("internal")===0 ? true : false;
            if($("#access-restricted").length>0 && UserRegistration.isUserLoggedIn && isInternalUser) {
				referrerPath = $("#gated-content-referrer-url").val();
                if ($('#run-mode').val() !== 'author') {
                    referrerPath = referrerPath.replace("/content/pwc-madison/ditaroot", "/dt");
                }
                window.location.href = UserRegistration.sanitizeString(referrerPath + window.location.hash);
            }
        }

        hasSameOrigin = function (url1, url2){
            if($("#gated-content-referrer-url").val().startsWith("/content/pwc-madison") || $("#gated-content-referrer-url").val().startsWith("/dt")){
				return true;
            }else{
                var currentPageUrl = new URL(window.location.href);
                if($("#gated-content-referrer-url").val()){
            		refPageUrl = new URL($("#gated-content-referrer-url").val());
                    return currentPageUrl.origin === refPageUrl.origin;
                }
            }
        };
        if($("#access-restricted").length>0 && UserRegistration.isUserLoggedIn && !isInternalUser){
			if(hasSameOrigin(window.location.href, $("#gated-content-referrer-url").val())){
                $("#gated-content-referrer-url").val($("#gated-content-referer-header-url").val());
            }
        }

        getUrlParameter = function getUrlParameter(sParam) {
            var sPageURL = window.location.search.substring(1), isSanitizedString = sPageURL.indexOf("&amp;")>0, sURLVariables = isSanitizedString ? sPageURL.split('&amp;') : sPageURL.split('&'),
                sParameterName, i;

            for (i = 0; i < sURLVariables.length; i++) {
                sParameterName = sURLVariables[i].split('=');
                if (sParameterName[0] === sParam) {
                    return typeof sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
                }
            }
            return false;
        };

		function closePopup() {
			$("#access-restricted").removeClass("is-active");
			hideBlur();
		}

		function openPopup() {
		    if($("#access-restricted").length>0 && ($("#access-restricted-content-redirect-url").val() && (!UserRegistration.isUserLoggedIn && (getUrlParameter("userType").length>0 || getUrlParameter("contentType").length>0)) || (UserRegistration.isUserLoggedIn && !isInternalUser))) {
                $(".gated-content-modal").append($("#access-restricted"));
                var userType = getUrlParameter("userType"), contentType = getUrlParameter("contentType");
                if(contentType === "internalOnly"){
                    console.log("showing popup");
                    $("[data-contentType="+contentType+"]").show();
                    $("[data-userType=concurrent]").hide();
                    $("[data-userType=perseat]").hide();
                }
                else if(userType === "perseat"){
                    $("[data-userType="+userType+"]").show();
                    $("[data-userType=concurrent]").hide();
                    $("[data-contentType=internalOnly]").hide();
                }else if(userType === "concurrent"){
                    $("[data-userType="+userType+"]").show();
                    $("[data-userType=perseat]").hide();
                    $("[data-contentType=internalOnly]").hide();
                }else{
                    closePopup();
                }
                $('#access-restricted').addClass("is-active");
                showBlur();
            }
		}

        $(".gateway-buttons .login, .gateway-buttons .go-to-homepage").click(function(e) {
           closePopup();
        });

        $("#access-restricted .modal-close").click(function(e) {
           closePopup();
           if($("#gated-content-referer-header-url").val()) {
               var refererHeaderURL = $("#gated-content-referer-header-url").val().indexOf("/user/gated-content")<0 ? $("#gated-content-referer-header-url").val() : $("#access-restricted-content-redirect-url").val();
               window.location.href = UserRegistration.sanitizeString(refererHeaderURL || $("#access-restricted-content-redirect-url").val() || window.location.href);
           }else{
               window.location.href = UserRegistration.sanitizeString($("#access-restricted-content-redirect-url").val());
           }
        });

        openPopup();
}(document, $, window.hideBlur, window.showBlur, window.UserRegistration, window.AccessRestriction));
