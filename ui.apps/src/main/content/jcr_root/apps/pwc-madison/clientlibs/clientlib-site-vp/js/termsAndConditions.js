$("#accept-tnc").click(function(event){
	event.preventDefault();
	var acceptTncData = {
			territoryCode : window.UserRegistration.getQueryParameterValue('territory'),
			localeAccepted : window.UserRegistration.getQueryParameterValue('locale'),
			referrerAccepted : window.UserRegistration.getQueryParameterValue('referrer')
	};
	$.ajax({
		url : window.UserRegistration.ACCEPT_TNC_END,
		data : JSON.stringify(acceptTncData),
		type : "POST",
		contentType : 'application/json; charset=utf-8',
		success: function (result) {
			if(result && result.referrer) {
				window.location.href = window.UserRegistration.sanitizeString(result.referrer + window.location.hash);
			}
		},
		error : function(xhr,
				httpStatusMessage,
				customErrorMessage) {
			if (xhr.status === window.UserRegistration.STATUS_CODE_UNAUTHORIZED) {
				window.localStorage.removeItem(window.UserRegistration.FAVORITE_LIST_SESSION_STORAGE);
				window.localStorage.removeItem(window.UserRegistration.FAVORITE_LIST_DOT_STATUS_LOCAL_STORAGE);
				window.location.href = window.UserRegistration.sanitizeString($(".reject-tnc").attr("href"));
			}
			$('.error-modal').addClass('is-active');
            $('.error-modal').find('.remark > p').text(window.getUserRegErrorMessage("acceptTncError"));
		}
	});
});

$(document).ready(function(){
	if($("body").find("#present-tnc").length > 0){
		$("body").addClass("terms-and-conditions");
		$("#search-link").css('pointer-events', 'none');
	}
    $(window).scroll(function() {
        $(".accept-tnc").addClass("stick-return");
        if($(window).scrollTop() + $(window).height() > ($(document).height() - 100) ) {
            $(".accept-tnc").removeClass("stick-return");
        }
	});
});