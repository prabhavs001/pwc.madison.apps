(function($, UserRegistration, hideBlur, showBlur) {
		
	$(".remove-login-continue").click(function(){
		$(".user-login").trigger("click", [{"removeSession" : true}]);
	});
	
	$(".remove-login-cancel").click(function(){
        var redirectionUrl = $("#redirect-logout-url").val();	
        UserRegistration.setCookie(UserRegistration.USER_REIRECTION_PATH_COOKIE, redirectionUrl ? redirectionUrl : window.location.href);
        window.location.href = UserRegistration.LOGOUT_REDIRECT_API_PATH;
	});
	
	if(UserRegistration.getCookie(UserRegistration.SHOW_REMOVE_SESSION)){
		$("#removeSessionModal").addClass("is-active");
		showBlur();
		UserRegistration.setCookie(UserRegistration.SHOW_REMOVE_SESSION, undefined, 0);
	}
	
}($, window.UserRegistration, window.hideBlur, window.showBlur));