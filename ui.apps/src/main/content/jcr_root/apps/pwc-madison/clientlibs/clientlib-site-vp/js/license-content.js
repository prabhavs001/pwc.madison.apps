(function(document, $, UserRegistration, Vue, hideBlur, showBlur) {

	function initializeLicenseContent() {
		var licenseContentModal = new Vue({
			el : '#animatedHybridModal',
			data : {
				licenses : {},
				show : 'none'
			},
			mounted : function() {
				var licenseContent = UserRegistration.getCookie(UserRegistration.CONTENT_LICENSE_COOKIE),
				completeProfile = UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE);
				if (!completeProfile && licenseContent && UserRegistration.isUserLoggedIn) {
					this.licenses = JSON.parse(window.decodeURIComponent(window.escape(window.atob(licenseContent))));
					this.showModal();
				}
			},
			methods : {
				showModal : function() {
					this.show = 'block';
					$("#hybridModal").animatedModal({
				        color: "rgba(0, 0, 0, 0.85)",
				        animatedIn: "fadeIn",
				        animatedOut: "fadeOut"
				    });
                    showBlur();
				},
				closeModal : function() {
					this.show = 'none';
					UserRegistration.setCookie(UserRegistration.CONTENT_LICENSE_COOKIE, undefined, 0);
					hideBlur();
				}
			}
		});
	}

	initializeLicenseContent();
	$(document).ready(function() {
	    $("#hybridModal").click();
	}); 
	

}(document, $, window.UserRegistration, window.Vue, window.hideBlur,
		window.showBlur));
