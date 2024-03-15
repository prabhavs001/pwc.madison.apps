(function(document, $, UserRegistration, Vue, hideBlur, showBlur) {

  function initializeExtendSessionTimeout() {
     var extendSessionTimeout = new Vue({
        el : '#timeOutWarningPopup',
        data : {
          sessionData : {},
          cookieExpiryTime : '',
          sessionCounter : '',
          show : 'none'
        },
        mounted : function() {
            this.handlePopupModal();
        },
        methods : {
          handlePopupModal : function() {
             var licenseSessionContent = UserRegistration.getCookie(UserRegistration.EXTEND_CONCURRENT_LICENSE_SESSION_COOKIE);
               if(licenseSessionContent && UserRegistration.isUserLoggedIn){
                   this.sessionData = JSON.parse(window.decodeURIComponent(window.escape(window.atob(licenseSessionContent))));
                   this.cookieExpiryTime = this.sessionData && this.sessionData[UserRegistration.COOKIE_EXPIRY_TIME_KEY] ? this.sessionData[UserRegistration.COOKIE_EXPIRY_TIME_KEY] : '';
                   this.sessionCounter = this.sessionData && this.sessionData[UserRegistration.EXTEND_SESSION_COUNTER_KEY] ? this.sessionData[UserRegistration.EXTEND_SESSION_COUNTER_KEY] : '';
                   if(this.cookieExpiryTime !== '' && this.sessionCounter < UserRegistration.EXTEND_SESSION_COUNTER_LIMIT){
                       //this setTimeout is set to show the popup before 5 min from the cookieExpiryDate
                        setTimeout(this.showModal,new Date(this.cookieExpiryTime) - new Date() - 5 * 60 * 1000 );
                   }
               }
          },
          showModal : function() {
            this.show = 'block';
            $("#timeOutPopup").animatedModal({
               color: "rgba(0, 0, 0, 0.85)",
               animatedIn: "fadeIn",
               animatedOut: "fadeOut"
            });
            showBlur();
            $("#timeOutPopup").click();
            //this setTimeout is set to close the popup after the cookie got expired
            setTimeout(this.closeModal,new Date(this.cookieExpiryTime) - new Date());
          },
          updateLicenseSessionCookie : function() {
            //deleting CookieExpiryTime Key from Extend Session Cookie
             delete this.sessionData[UserRegistration.COOKIE_EXPIRY_TIME_KEY];
             var secure = location.protocol === "https:",
             encodedLiceSessionData = window.btoa(window.unescape(encodeURIComponent(JSON.stringify(this.sessionData))));
             $.cookie(UserRegistration.EXTEND_CONCURRENT_LICENSE_SESSION_COOKIE, encodedLiceSessionData, { path: '/',expires: new Date(this.cookieExpiryTime) , secure: secure });
          },
          closeModal : function() {
            this.show = 'none';
            hideBlur();
          },
          extendSessionTimeout : function() {
            var licenseSessionContent = UserRegistration.getCookie(UserRegistration.EXTEND_CONCURRENT_LICENSE_SESSION_COOKIE),liceSessionCounter;
            if(licenseSessionContent && UserRegistration.isUserLoggedIn){
                this.sessionData = JSON.parse(window.decodeURIComponent(window.escape(window.atob(licenseSessionContent))));
                liceSessionCounter = this.sessionData && this.sessionData[UserRegistration.EXTEND_SESSION_COUNTER_KEY] ? this.sessionData[UserRegistration.EXTEND_SESSION_COUNTER_KEY] : '';
                if(this.sessionData && this.sessionData[UserRegistration.COOKIE_EXPIRY_TIME_KEY] && liceSessionCounter !== '' && this.sessionCounter === liceSessionCounter){
                    this.updateLicenseSessionCookie();
                    $.ajax({
                        url : UserRegistration.EXTEND_MADISON_USER_SESSION_API_PATH,
                        type : "GET",
                        contentType : 'application/json; charset=utf-8',
                        success : function(result) {
                           this.closeModal();
                           this.handlePopupModal();
                        }.bind(this)
                    });
                } else {
                  this.closeModal();
                }
            }
          }
        }

     });
  }
   initializeExtendSessionTimeout();


}(document, $, window.UserRegistration, window.Vue, window.hideBlur,
		window.showBlur));