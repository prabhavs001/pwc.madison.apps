(function(document) {

	var DataAnalytics = {}, isUserLoggedIn = document.cookie
	&& document.cookie.indexOf('vp_ic') !== -1 ? true : false, userInfoCookie, userInfo = "", userInfoCookies, userInformation,licenseInfoCookie,userLicenses=[],licenseArray,meta = {},standalone,
        userAgent, safari , ios, android;

	DataAnalytics.MADISON_USER_PROFILE_LICENSEINFO_COOKIE_NAME = "vp_li";
	DataAnalytics.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME = "viewpoint-profile-userinfo";
	DataAnalytics.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX = "viewpoint-profile-userinfo-";
	DataAnalytics.COMMA = ",";
	DataAnalytics.PLUS_SIGN = "+";
    DataAnalytics.BLANK_SPACE = " ";

	DataAnalytics.setNestedObject = function(object, path, value) {
		var pathList = path.split('.'), length = pathList.length, i, element, localObject = object;
		for (i = 0; i < length - 1; i++) {
			element = pathList[i];
			localObject[element] = localObject[element] || {};
			localObject = localObject[element];
		}
		localObject[pathList[length - 1]] = value;
	};

	DataAnalytics.setAnalyticsValue = function(objectKeys, value) {
		if (window.digitalData) {
			DataAnalytics.setNestedObject(window.digitalData, objectKeys, value);
		}
	};
		
	DataAnalytics.getCookie = function(cname) {
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
	
    // Set User Information
	if (isUserLoggedIn) {
		userInfoCookie = DataAnalytics.getCookie(DataAnalytics.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME);
		if(userInfoCookie) {
			userInfoCookies = userInfoCookie.split(DataAnalytics.COMMA);
			userInfoCookies.forEach(function(index, value){
				userInfo += decodeURIComponent(DataAnalytics.getCookie(DataAnalytics.MADISON_USER_PROFILE_USERINFO_COOKIE_NAME_PREFIX + value)).split(DataAnalytics.PLUS_SIGN).join(DataAnalytics.BLANK_SPACE);
			});
			userInformation = JSON.parse(userInfo);
			if(userInformation){
				licenseInfoCookie = DataAnalytics.getCookie(DataAnalytics.MADISON_USER_PROFILE_LICENSEINFO_COOKIE_NAME);
				if(licenseInfoCookie){
					licenseArray = JSON.parse(licenseInfoCookie);
					if(licenseInfoCookie.length > 0){
						licenseArray.forEach(function (item) {
							if(new Date().getTime() <= item.expiryTs){
								userLicenses.push(item.code);
							}
						});
					}
					userInformation.userLicenses = userLicenses;
				}
                userInformation.contentAccessInfo = null;
                userInformation.countryCode = null;
                userInformation.isInternalUser = null;                
				DataAnalytics.setAnalyticsValue("user.userInfo", userInformation);	
			}
		}
	}
    standalone = window.navigator.standalone;
    userAgent = window.navigator.userAgent.toLowerCase();
    safari = /safari/.test( userAgent );
    ios = /iphone|ipod|ipad/.test( userAgent );
    android = /android/i.test(userAgent);

    if( ios ) {
        meta.operatingSystem = "ios";
        if ( !standalone && safari ) {
            meta.visitSource = "Safari";
        } else if ( standalone && !safari ) {
            meta.visitSource = "App";
        } else if ( !standalone && !safari ) {
            meta.visitSource = "App";
        }
    } else if (android){
        meta.operatingSystem = "android";
        meta.visitSource = "App";
    }
    DataAnalytics.setAnalyticsValue("page.meta.visitSource", meta.visitSource);
    DataAnalytics.setAnalyticsValue("page.meta.operatingSystem", meta.operatingSystem);

    // User Profile
	DataAnalytics.setAnalyticsValue("user.loggedinStatus", isUserLoggedIn);
	
	window.DataAnalytics = DataAnalytics;
	
}(document));
