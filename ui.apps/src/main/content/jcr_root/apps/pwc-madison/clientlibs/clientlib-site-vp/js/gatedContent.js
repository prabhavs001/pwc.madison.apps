(function(document, $, hideBlur, showBlur, UserRegistration){

		var GatedContent = {};

		if($("#gatedModal").length && $("#gated-content-referrer-url").val() && UserRegistration.isUserLoggedIn) {
			window.location.href = UserRegistration.sanitizeString($("#gated-content-referrer-url").val() + window.location.hash);
	    }
		
		function closeGatedModal() {
			$("#gatedModal").removeClass("is-active");
			hideBlur();
		}	

		GatedContent.checkGatedContent = function () {
			if($("#gated-content-redirect-url").val() && !UserRegistration.isUserLoggedIn) {
				$(".gated-content-modal").append($("#gatedModal"));
				$('#gatedModal').addClass("is-active");
				showBlur();
			}
		};

        $(".gateway-buttons .login, .gateway-message .login").click(function(e) {
           closeGatedModal();
           if (jQuery(e.target).is('a')) {
             event.preventDefault();
           }
           $(".user-login").trigger("click");
        });

        $(".gateway-buttons .register").click(function(e) {
            closeGatedModal();
            UserRegistration.UserRegisterRedirection(e);
        });

        $(".gateway-buttons .go-to-homepage").click(function(e) {
           closeGatedModal();
        });

		window.GatedContent = GatedContent;
        
}(document, $, window.hideBlur, window.showBlur, window.UserRegistration));
