$(document).ready(function() {

    function fixFeedAlignment() {
        var popHeight = $($('.popular-section .columns').get(0)).outerHeight() + $($('.popular-section > h2')).outerHeight() + parseFloat($('.popular-section section').css('padding-top')) + 16;
        $('.webCastCover').parent('div').height(popHeight);
    }

    fixFeedAlignment();

    $(window).on('resize', function() {
        fixFeedAlignment();
    });

});