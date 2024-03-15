$(document).ready(function () {
    $(".language-menu").click(function() {
        $(".body").removeClass("show-account-menu");
        $(".body").removeClass("show-favorites-menu");
        $(".body").toggleClass("show-language-menu");
    });

});
