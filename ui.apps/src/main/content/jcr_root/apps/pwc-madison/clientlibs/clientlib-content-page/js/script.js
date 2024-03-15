$(function() {
	var itemPath, madisonCookie, decodedCookie, name, cookies, cookie, index;

	/*Cookie Extract Method*/
    function getCookie(cookieName) {
      name = cookieName + "=";
      decodedCookie = decodeURIComponent(document.cookie);
      cookies = decodedCookie.split(';');
      for(index=0; index<cookies.length; index++) {
        cookie = cookies[index];
        while (cookie.charAt(0) === ' ') {
          cookie = cookie.substring(1);
        }
        if (cookie.indexOf(name) === 0) {
          return cookie.substring(name.length, cookie.length);
        }
      }
      return "";
    }

    itemPath = window.location.pathname;
	madisonCookie = getCookie("vp_pc");    
    if(document.readyState === 'interactive') {         
        if(madisonCookie) {
            setTimeout(function(){
				$.ajax({
                    url: "/bin/pwc-madison/recentlyviewed",
                    type: "POST",
                    data: {itemPath: itemPath},
                    success: function(res) {
                    },
                    error: function(er) {
                    }
               });
			}, 3000);
        }
    }
});
