$(document).ready(function () {
  //Update the Cookie Notice URL as per territory
    setTimeout(function() {
      if((null !== $("#onetrust-policy-text") && $("#onetrust-policy-text").length !==0) || (null !== $(".privacy-notice-link") && $(".privacy-notice-link").length !==0)){
          var countryCode = $('meta[name="pwcCountry"]').attr('content'), langCode = $('meta[name="pwcLang"]').attr('content'), policyLink, url, path, paths, lastPart, correctUrl;
          policyLink = $("#onetrust-policy-text").length !==0 ? $("#onetrust-policy-text").find('a').attr('href') : $(".privacy-notice-link").attr('href');
          if(typeof policyLink !== "undefined"){
              url = new URL(policyLink);
              path = url. pathname;
              paths = path.split('/');
              lastPart = paths[paths.length - 1];
              correctUrl = url.protocol + '//' + url.hostname + '/' + countryCode + '/' + langCode + '/' + lastPart;
              $("#onetrust-policy-text").find('a').attr('href', correctUrl);
              $(".privacy-notice-link").attr('href',correctUrl);
              $("#onetrust-pc-btn-handler, #ot-sdk-btn").click(function(){
                  $("#ot-pc-desc").find('a').attr('href', correctUrl);
              });
          }
        }
    }, 1000);
});
