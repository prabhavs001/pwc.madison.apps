var columns, item, i, featuredTextHeadingMobile, featuredTextDescriptionMobile, featuredTextHeading, featuredTextDescription, currentSlide, currentSlideCount, slidesCount, $slider = $('.related-content_wrapper'),$relatedContentImgs = $('.related-content-section .related-content_card-img');

$(document).ready(function () {

	//To show the Total & Current slides number in mobile only
    $slider.on('init reInit afterChange beforeChange', function(event, slick, currentSlide, slidesCount) {
		currentSlideCount = (currentSlide ? currentSlide : 0) + 1;
		slidesCount = $('.related-content-section .slick-slide').length;
		$(".slidersCount").text(currentSlideCount + " / " +slidesCount); 
    });

    columns = $(".highlights-slider > .column");
	for (i = 0; i < columns.length; i += 2) {
		columns.slice(i, i + 2).wrapAll("<div class='columns is-flex'></div>");
	}

	item = $(".highlights-slider > .columns");
	for (i = 0; i < item.length; i += 2) {
		item.slice(i, i + 2).wrapAll("<div class='slide-item'></div>");
	}
	
	

	$('.highlights-slider').slick({
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
				breakpoint: 768,
				settings: {
					slidesToShow: 1,
					slidesToScroll: 1,
					infinite: true,
					slickGoTo: 1,
					arrows: false
				}
			}
		]
	});

	

    $('.related-content_wrapper').on('init reInit afterChange beforeChange', function(event, slick, currentSlide, slidesCount) {
        currentSlideCount = (currentSlide ? currentSlide : 0) + 1;
        slidesCount = $('.related-content-section .slick-slide').length;
        $(".slidersCount").text(currentSlideCount + " / " +slidesCount); 
      });


	$('.related-content_wrapper').slick({
		slidesToShow: 3,
		slidesToScroll: 1,
		swipe:false,
		dots: false,
		infinite: false,
		speed: 300,
		prevArrow: $('.related-content_prev'),
		nextArrow: $('.related-content_next'),
        responsive: [
            {
                breakpoint: 3000,
                settings: "unslick"
            },
            {
                breakpoint: 768,
                settings: {
                    slidesToShow: 1,
					slidesToScroll: 1,
					swipe:true
                    }
            }
        ]
	});

	//Add class to handle invalid image path in related content component 
	$relatedContentImgs.each(function() {
		var img = $(this), testImage = new Image();
		testImage.src = img.attr('src');
	  
		$(testImage).on('error', function() {
			img.parent().addClass('invalid-image');
		});
	  });	  

    // Add class for not having summary on Desktop
    var feature_items = $('.featured .flip_cover');
	feature_items.each(function (index, eachfeature) {
		var feature_summary = $(eachfeature).find(".feature-tile-hover").length;
		if (!feature_summary) {
			$(this).addClass("no_fsummary");
		}
	});

	// Flip icon for Desktop rendition
	$('.flip_icon').click(function () {
		var flip_off, flip_on;
		if ($(this).parent(".no_fsummary").length) {
			event.preventDefault();
		} else {
			event.preventDefault();
			$(this).parents(".flip_cover").find(".madison-card-block").toggleClass("feature_flip");
			$(this).toggleClass("flip_toggle_btn");
			flip_off = "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/flip_off.svg";
			flip_on = "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/flip_on.svg";
			if ($(this).hasClass("flip_toggle_btn")) {
				$(this).find("img").attr("src", flip_on);
			} else {
				$(this).find("img").attr("src", flip_off);
			}
		}
	});

    $('.flip_cover').mouseover(
        function() {
          var flip_on = "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/flip_on.svg";
          $(this).children().find("img").not('article img').attr("src", flip_on);
        }
    );

    $('.flip_cover').mouseleave(
        function() {
          var flip_off = "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/flip_off.svg";
          $(this).children().find("img").not('article img').attr("src", flip_off);
        }
    );

    // Hide buttons on ready document
	$('.show-less').hide();
	$('.click-for-less').hide();

	//Hide More/Less button for Summary text not available
    $('.featured-mobile .summary-unavailable').each(function (index, value) {
		$(this).siblings('.feature-item-toggle').hide();
    });


	// Expand/Collapse the tiles
	$('.feature-item-toggle').on("click", function (e) {
		e.preventDefault();
		$(this).parents(".madison-card").toggleClass("feature-toggle");
		var featureToggleParent = $(this).parents(".madison-card").hasClass("feature-toggle");
		if (featureToggleParent) {
			$(this).find(".click-for-more").hide();
			$(this).find(".click-for-less").show();
			$(this).find("span").removeClass("icon-caret-down");
			$(this).find("span").addClass("icon-caret-up");
		} else {
			$(this).find(".click-for-less").hide();
			$(this).find(".click-for-more").show();
			$(this).find("span").removeClass("icon-caret-up");
			$(this).find("span").addClass("icon-caret-down");
		}
	});


	// Show more/less the tiles
	$('.feature-show-more').on("click", function (e) {
		e.preventDefault();
		$(this).parents(".featured-mobile").toggleClass("feature-more");
		var featureToggleParent = $(this).parents(".featured-mobile").hasClass("feature-more");
		if (featureToggleParent) {
			$(".featured-mobile > .column").show();
			$(this).find(".show-more").hide();
			$(this).find(".show-less").show();
			$(this).find(".arrow").addClass("arrowUp");
		} else {
			$(this).find(".show-less").hide();
			$(this).find(".show-more").show();
			$('.featured-mobile > .column').hide();
			$('.featured-mobile > .column:lt(3)').show();
			$(this).find(".arrow").removeClass("arrowUp");
		}
	});

	// Show more/less tiles for Suggested guidance in mobile view
    $("body").on("click", ".sg-show-more", function (e) {
		e.preventDefault();
		$(this).parents(".results.search-guidance").toggleClass("feature-more");
		var sgToggleParent = $(this).parents(".results.search-guidance").hasClass("feature-more");
		if (sgToggleParent) {
			$(".results.search-guidance .thin-row").show();
			$(this).find(".show-more").hide();
			$(this).find(".show-less").show();
			$(this).find(".arrow").addClass("arrowUp");
		} else {
			$(this).find(".show-less").hide();
			$(this).find(".show-more").show();
			$('.results.search-guidance .thin-row').hide();
			$('.results.search-guidance .thin-row:lt(3)').show();
			$(this).find(".arrow").removeClass("arrowUp");
		}
	});

	featuredTextHeadingMobile = $('.featured-mobile .module-heading');
    featuredTextDescriptionMobile = $('.featured-mobile .feature-tile-content');
    featuredTextHeading = $('.featured-desktop-rendition .module-heading, .featured-desktop-rendition .highlights-container .module-heading');
	featuredTextDescription = $('.featured-desktop-rendition .feature-tile-content, .featured-desktop-rendition .highlights-container .feature-tile-content');

    //Mobile Title and Description clamping for both homepage rendition
	featuredTextDescriptionMobile.each(function (index, value) {

		$clamp(value, {	clamp: 3 });

		if ($(this).hasClass("no-abstract")) {
			$clamp(featuredTextHeadingMobile.get(index), { clamp: 5	});
		} else { 
            $clamp(featuredTextHeadingMobile.get(index), { clamp: 3	});
        }
	});

    //Desktop Title and Description clamping for homepage and landing page renditions
	featuredTextDescription.each(function (index, value) {

        var featureImage = $(this).parent().find('article img').length;

        // Desktop Desc When Image Vs No Image
        if(featureImage) {
			$clamp(value, {	clamp: 4 });
		} else {
			$clamp(value, {	clamp: 6 });
		}

        if ($(this).hasClass("no-abstract")) {
			$clamp(featuredTextHeading.get(index), { clamp: 5 });
		} else {
            // Desktop Heading When Image Vs No Image
            if(featureImage) {
				$clamp(featuredTextHeading.get(index), { clamp: 3 });
			} else {
				$clamp(featuredTextHeading.get(index), { clamp: 4 });
			}
		}

	});


});
