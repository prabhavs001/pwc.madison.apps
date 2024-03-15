$(document).ready(function() {
    function checkInternalUser() {
        var exitTimer = 0;
        if(window.UserRegistration.userInfo && window.UserRegistration.userInfo.isInternalUser) {
            $('#see-also-internal').show();
            $('#see-also-external').hide();
        } else if (exitTimer > 10) {
            return;
        } else {
        exitTimer = exitTimer+1;
            setTimeout(function(){
                checkInternalUser();
            }, 50); 
        }
    }
    
    checkInternalUser();

});