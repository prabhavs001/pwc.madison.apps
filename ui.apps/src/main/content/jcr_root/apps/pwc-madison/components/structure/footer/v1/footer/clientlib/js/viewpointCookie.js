$(document).ready(function() {
  var viewPointCookie, territoryCode, 
      cookieNamePrefix = "vp-cookie-accept-";
  territoryCode = $("#acceptCookie").attr("pageterritory");
  viewPointCookie = $.cookie(cookieNamePrefix + territoryCode);
  
  if(viewPointCookie === undefined) {
     $("#cookieWrapper").show();
  }  
  $("#acceptCookie").click(function() {
   var secure = location.protocol === "https:",
       currentDate = new Date() ,cookieDate;
   //Setting Cookie expiry date.The Cookie will expire after 6 months from the Current Date
   currentDate.setMonth(currentDate.getMonth() + 6);
   cookieDate  = new Date(currentDate);
   $.cookie(cookieNamePrefix + territoryCode, territoryCode, { path: '/',expires: cookieDate , secure: secure });
   $("#cookieWrapper").hide();
  });
});
