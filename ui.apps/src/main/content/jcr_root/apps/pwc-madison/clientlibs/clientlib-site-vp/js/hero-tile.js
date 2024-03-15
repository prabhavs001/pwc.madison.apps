$(document).ready(function () {

    //takes data attribute for hero slider (seconds), multiply by 1000 (miliseconds)
    var slickIncrement = parseInt($('.banner-right-block').data('slick-increment-seconds'),0) * 1000,
        currentRightBlockSlide,
        bgArticle,
        slickInit =  function(){

        $('.banner-right-block').slick({
            dots: true,
            infinite: true,
            fade: true,
            speed: 500,
            slidesToShow: 1,
            slidesToScroll: 1,
            prevArrow: '<button class="slick-prev" aria-label="Previous" type="button"></button>',
            nextArrow: '<button class="slick-next" aria-label="Next" type="button"></button>',
            autoplay: true,
            autoplaySpeed: slickIncrement,
            responsive: [
                {
                    breakpoint: 769,
                    settings: {
                        arrows: false
                    }
                }
            ]
        });

    };


    setTimeout(function () {

        var moduleBrief, clampHeroSubhead, subHeader, $articleEle, clampVal,
            isMobile = window.matchMedia("only screen and (max-width: 768px)").matches, isIE11 = !!navigator.userAgent.match(/rv\:11\./);
        moduleBrief = $(".hero-tile .brief");
        moduleBrief.each(function (index, value) {
            $clamp(value, {
                clamp: 2
            });
        });
        
        //------ For Dev: This replaces existing ".hero-sub-heading" clamp code found in Dev environment ------
        clampHeroSubhead = 2;
        if (isIE11) {
            clampHeroSubhead = 3;
        }

        subHeader = $('.hero-tile .hero-sub-heading').get(0);
        if (subHeader) {
            $clamp(subHeader, {
                clamp: clampHeroSubhead
            });

            $(window).on('resize', function () {
                $clamp(subHeader, {
                    clamp: clampHeroSubhead
                });
            });
        }

        $('.register-mobile-nav').on('init', function(slick) {
            setTimeout(function() {
                $('.register-mobile-nav').addClass('slick-initialized');
            }, 200);
        });

        $('.banner-right-block').on('init', function() {
            setTimeout(function() {
                $('.banner-right-block').addClass('slick-initialized');
            }, 200);
        });

        $articleEle = $('.banner-right-block article');
        $articleEle.each(function (index, eachHeading) {
            var heading = $(eachHeading).find(".module-heading"),
                sq_image = $(eachHeading).hasClass('square_image_rendition'),
                vd_image = $(eachHeading).hasClass('video_image_rendition');
            if (sq_image === true) {
                clampVal = isMobile ? 5 : 3;
            }else if(vd_image === true) {
                clampVal = isMobile ? 3 : 2;
            } else {
                clampVal = 2;
            }
            heading.each(function (index, value) {
                $clamp(value, {clamp: clampVal});
            });
        });
    }, 0);

	$('.banner-right-block').on('init', function (slick) {

	  var currentArticle = $(this).find('article.slick-current'), bgArticle;
	  if(jQuery(".slick-slide").hasClass("slick-active")){
		  bgArticle =  $(currentArticle).attr("bg-article");
		  if(bgArticle){
			  $(this).siblings(".hero-overlay").css("background", "url(" + bgArticle + ")");
			  $(this).parents(".thick-border").addClass('backgound_img');
		  }else{
			  $(this).parents(".thick-border").removeAttr("style");
			  $(this).parents(".thick-border").removeClass('backgound_img');
		  }
	  }
	});

	window.addEventListener("slickInit", this.slickInit);
	slickInit();
	currentRightBlockSlide = $('.banner-right-block').slick('slickCurrentSlide');
	bgArticle =  $('.banner-right-block .slick-slide[data-slick-index="'+currentRightBlockSlide+'"] article').attr("bg-article");

	$('.banner-right-block').on('beforeChange', function(event, slick, currentSlide, nextSlide){

	  if(jQuery(".slick-slide").hasClass("slick-active")){
		  var bgArticle =  $(this).find('.slick-slide[data-slick-index="'+ nextSlide+'"]').attr("bg-article");
		   if(bgArticle){
			  $(this).siblings(".hero-overlay").css("background", "url(" + bgArticle + ")");
			  $(this).parents(".thick-border").addClass('backgound_img');
		  }else{
			  $(this).parents(".thick-border").removeAttr("style");
			  $(this).parents(".thick-border").removeClass('backgound_img');
		  }
	  }
	});
    setTimeout(function () {
        var numItems = $('.hero .slick-list.draggable .slick-track .slick-slide').length;
        if (numItems === 1) {
            $('.hero .slick-list.draggable .slick-track').css("width", "100%");
            $('.hero .slick-list.draggable .slick-track .slick-slide').css("width", "100%");
        }
    }, 10);
});
