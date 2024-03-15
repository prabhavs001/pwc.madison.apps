$(document).ready(function () {
    var isContentTruncated, elem, seeAnswerText, hideAnswerText, answerDivHeight, questionDivHeight, $readMoreLink, answerComponent;
    $('.faqs-topics-slider').slick({
        slidesToShow: 1,
        slidesToScroll: 1,
        dots: false,
        infinite: false,
        speed: 300,
        focusOnSelect: true,
        prevArrow: "<span class='icon-caret-left' />",
        nextArrow: "<span class='icon-caret-right' />",
        responsive: [{
            breakpoint: 1024,
            settings: {
                slidesToShow: 1,
                slidesToScroll: 1,
                infinite: true,
                dots: false
            }
        },
            {
                breakpoint: 600,
                settings: {
                    slidesToShow: 1,
                    slidesToScroll: 1
                }
            },
            {
                breakpoint: 480,
                settings: {
                    slidesToShow: 1,
                    slidesToScroll: 1
                }
            }
        ]
    });

    function answerTruncate(component){
        answerDivHeight = $(component).height();
        $readMoreLink = $(component).next('a');
        if(answerDivHeight > 220){
            $(component).css('height','220px').css('overflow','hidden');
            if($readMoreLink.hasClass("is-hidden")) {
                $readMoreLink.removeClass("is-hidden");
            }
        }
    }

    $(".answer-container-dummy").each(function(index,component){
        answerTruncate(component);
    });

    $(".question-container-dummy").each(function (index,component) {
        questionDivHeight= $(component).height();
        $readMoreLink = $(component).parent().find("#faq-read-more .answer-container-dummy");
        if(questionDivHeight > 50){
            $(component).css('height','50px').css('overflow','hidden');
            if($readMoreLink.hasClass("is-hidden")) {
                $readMoreLink.removeClass("is-hidden");
            }
        }
    });


    $(".faq-toggle-dummy").click(function() {
        elem = $(this).attr("data-more-target");
        seeAnswerText=$(this).data("see-answer");
        hideAnswerText=$(this).data("hide-answer");
        answerComponent = $(this).parent().find("#faq-read-more .answer-container-dummy");
        if ($(this).hasClass("read-more")) {
            //Stuff to do when btn is in the read more state
            $(this).find("i").text(hideAnswerText);
            $(this).parent().find(elem).show();
            $(this).addClass("read-less").removeClass("read-more");
            $(this).find(".icon  span").removeClass("icon-caret-down");
            $(this).find(".icon  span").addClass("icon-caret-up");
            answerTruncate(answerComponent);

        } else {
            //Stuff to do when btn is in the read less state
            $(this).find("i").text(seeAnswerText);
            $(this).parent().find(elem).hide();
            $(this).removeClass("read-less").addClass("read-more");
            $(this).find(".icon  span").removeClass("icon-caret-up");
            $(this).find(".icon  span").addClass("icon-caret-down");
        }
    });
});
