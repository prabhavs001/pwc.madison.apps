$(document).ready(function () {
   $(".contact a").click(function (event) {
       event.stopPropagation();
   });

    $(function() {
        tippy(".sme-tippy", {
            content: function(reference) {
                var id = reference.getAttribute('data-id');
                return $(id).get(0);
            },
            trigger: 'click',
            theme: "share",
            arrow: true,
            arrowType: "sharp",
            animation: "fade",
            zIndex: 9999,
            interactive: true,
            distance: 20,
            maxWidth: 600,
            boundary: "window",
            onShown: function() {
                if ($("body .tippy-arrow").length < 2) {
                    $("body .tippy-arrow").clone().insertAfter("div.tippy-arrow:last");
                }
            }
        });
    });
});