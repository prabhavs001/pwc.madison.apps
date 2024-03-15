(function(document, $, UserRegistration, DataAnalytics) {

	var $errorPage = $(".data-analytics-page-error"), siteError;
	
	DataAnalytics.errorCodes = {};
	DataAnalytics.errorCodes[404] = '404-page-not-found';
	DataAnalytics.errorCodes[500] = '500-internal-server';
	DataAnalytics.errorCodes[403] = '403-forbidden';

	// Error Page
	if ($errorPage.length) {
		siteError = DataAnalytics.errorCodes[$errorPage.data("analyticsErrorCode")];
		DataAnalytics.setAnalyticsValue("page.pageInfo.siteError", siteError);
	}
	
	DataAnalytics.setUserInfo = function(){
		var userInformation = UserRegistration.getUserInfo();
		if(userInformation){
			DataAnalytics.setAnalyticsValue("user.userInfo", userInformation);
		}
	};
	
    // User Events
    $(document).on("loginStart", function(event, data){
		window.DataAnalytics.setAnalyticsValue("user.event", "login-start");
		if (data.callbackMethod) {
            data.callbackMethod(data);
        }
    });

    $(document).on("loginComplete", function(event){
		DataAnalytics.setAnalyticsValue("user.event", "login-complete");
    });
    $(document).on("registrationStart", function(event){
		DataAnalytics.setAnalyticsValue("user.event", "registration-start");
    });

    $(document).on("registrationComplete", function(event, data){
		DataAnalytics.setAnalyticsValue("user.event", "registration-complete");
		DataAnalytics.setAnalyticsValue("user.userInfo",data);
    });
    
    //NewsLetter Opt In
    $(document).on("newsletterOptIn", function(event, data){
		DataAnalytics.setAnalyticsValue("filter.newsletterOptIn", data.newsletterOptIn);
    });
    
    //Video
    $(document).on("videoSelected", function(event, data){
        DataAnalytics.setAnalyticsValue("digitalData.video.name", data.title);
    });

    $(document).on("videoStarted", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.video.event", "video-start");
    });

    $(document).on("videoCompleted", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.video.event", "video-complete");
    });

    $(document).on("videoTime", function(event, data){
        DataAnalytics.setAnalyticsValue("digitalData.video.time", data.time);
    });

    $(document).on("videoTime75", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.video.event", "video-75-complete");
    });

    $(document).on("videoTime50", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.video.event", "video-50-complete");
    });

    $(document).on("videoTime25", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.video.event", "video-25-complete");
    });

    //Audio
    $(document).on("audioSelected", function(event, data){
        DataAnalytics.setAnalyticsValue("digitalData.podcast.title", data.title);
    });

    $(document).on("audioStarted", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.podcast.event", "podcast-start");
    });

    $(document).on("audioCompleted", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.podcast.event", "podcast-complete");
    });

    $(document).on("audioTime75", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.podcast.event", "podcast-75-complete");
    });

    $(document).on("audioTime50", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.podcast.event", "podcast-50-complete");
    });

    $(document).on("audioTime25", function(event){
        DataAnalytics.setAnalyticsValue("digitalData.podcast.event", "podcast-25-complete");
    });
    
    $(document).on("userDataChange", function(event){
        DataAnalytics.setUserInfo();
    });
    
}(document, $, window.UserRegistration, window.DataAnalytics));
