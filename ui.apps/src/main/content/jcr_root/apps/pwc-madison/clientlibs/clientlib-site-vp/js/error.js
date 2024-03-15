//Unhides the 403 page content when popup is not shown
$(document).ready(function () {
    var winLocation = window.location, sPageURL = winLocation.search.substring(1), sURLVariables = sPageURL!=='' ? sPageURL.split('&') : [];
    if((winLocation.pathname.indexOf("/error/403")>0 && sURLVariables.length<=0) || sURLVariables.includes("wcmmode=disabled")){
        $(".page-404").show();
    }
});
