$(document).ready(function() {

    //multimedia thumbnail listener
    $(document).on("click", ".dm-overlay", function() {
        var assetId = $(this).find(".dm-thumbnail").data("assetId"),
            $assetModal;
        $assetModal = $("div.modal-multimedia-view[data-asset-id-target='" + assetId + "']").first();
        if ($assetModal.length) {
            $assetModal.addClass("is-active");
            window.showBlur();

            $assetModal.find(".multimedia-view-slider").not('.slick-initialized').slick({
                slidesToShow: 1,
                slidesToScroll: 1,
                dots: false,
                infinite: false,
                speed: 300,
                focusOnSelect: true,
                prevArrow: $("#multimedia-slick-prev-button").html(),
                nextArrow: $("#multimedia-slick-next-button").html()
            });
        }
    });

    /**
     * Set previous and next media details on slick previous and next button respectively.
     * @param {*} prevSlide DOM element representing previous slide of carousel
     * @param {*} nextSlide DOM element representing next slide of carousel
     */
    function setPrevNextButtonDetails(prevSlide, nextSlide) {
        var prevSlideDetails, nextSlideDetails, countryLabelContentType = "", contentIdContentTitle = "";

        if (prevSlide) {
            prevSlideDetails = $(prevSlide).find(".column.details").data();
            if (prevSlideDetails.countryLabel) {
                countryLabelContentType = prevSlideDetails.countryLabel + " ";
            }
            if (prevSlideDetails.contentType) {
                countryLabelContentType += prevSlideDetails.contentType;
            }

            if (prevSlideDetails.contentId) {
                contentIdContentTitle = prevSlideDetails.contentId + " - ";
            }
            if (prevSlideDetails.wrapperPageTitle) {
                contentIdContentTitle += prevSlideDetails.wrapperPageTitle;
            }
        }
        $(".modal-multimedia-view.is-active").find(".prev-country-label-content-type").text(countryLabelContentType);
        $(".modal-multimedia-view.is-active").find(".prev-content-id-content-title").text(contentIdContentTitle);

        countryLabelContentType = "";
        contentIdContentTitle = "";
        if (nextSlide) {
            nextSlideDetails = $(nextSlide).find(".column.details").data();
            if (nextSlideDetails.countryLabel) {
                countryLabelContentType = nextSlideDetails.countryLabel + " ";
            }
            if (nextSlideDetails.contentType) {
                countryLabelContentType += nextSlideDetails.contentType;
            }

            if (nextSlideDetails.contentId) {
                contentIdContentTitle = nextSlideDetails.contentId + " - ";
            }
            if (nextSlideDetails.wrapperPageTitle) {
                contentIdContentTitle += nextSlideDetails.wrapperPageTitle;
            }
        }
        $(".modal-multimedia-view.is-active").find(".next-country-label-content-type").text(countryLabelContentType);
        $(".modal-multimedia-view.is-active").find(".next-content-id-content-title").text(contentIdContentTitle);

    }

    /**
     * Pause all videos and audios available under the given jQuery element
     * @param {*} $element jQuery object
     */
    function pauseMedia($element) {
        $element.find("video, audio").each(function() {
            this.pause();
        });
    }

    /**
     * Trigger media event with media wrapper page title as data. If the $currentSlide element
     * represents a video wrapper page trigger videoSelected event otherwise trigger audioSelected event.
     * @param {*} $currentSlide jQuery object representing current video slide
     */
    function triggerMediaSelectedEvent($currentSlide) {
        var wrapperPageInfo = $currentSlide.find(".column.details").data();

        if (wrapperPageInfo.pageType === "video") {
            $(document).trigger("videoSelected", [{ "title": wrapperPageInfo.wrapperPageTitle }]);
        } else {
            $(document).trigger("audioSelected", [{ "title": wrapperPageInfo.wrapperPageTitle }]);
        }
    }

    $(".multimedia-view-slider").on("init", function(event, slick) {
        var $currentSlide = $(slick.$slides.get(0)),
            firstAudioPlayer;
        triggerMediaSelectedEvent($currentSlide);

        setPrevNextButtonDetails(undefined, slick.$slides.get(1));

        //clamp text after slick initialization
        window.clampTextForSelector(".clamp-text-slick");

        //play the first audio if available when modal is launched
        firstAudioPlayer = $(slick.$slides.get(0)).find("audio").get(0);
        if (firstAudioPlayer) {
            firstAudioPlayer.play();
        }
    });

    $(".multimedia-view-slider").on("beforeChange", function(event, slick, currentSlide, nextSlide) {
        //need this check as beforeChange and afterChange fires even when slide doesn't change.
        if (nextSlide !== currentSlide) {
            var $nextSlide = $(slick.$slides.get(nextSlide));
            triggerMediaSelectedEvent($nextSlide);

            pauseMedia($(slick.$slides.get(currentSlide)));

            //need this check as for negative numbers slick returns list in reverse order
            if (nextSlide > 0) {
                setPrevNextButtonDetails(slick.$slides.get(currentSlide), slick.$slides.get(nextSlide + 1));
            } else {
                setPrevNextButtonDetails(undefined, slick.$slides.get(nextSlide + 1));
            }

        }
    });

    $(".dm-modal-background").click(function() {
        $(".modal").removeClass("is-active");
        window.hideBlur();

        pauseMedia($(this).closest(".modal-multimedia-view"));
    });

    $(".dm-modal-close").click(function() {
        $(".modal").removeClass("is-active");
        window.hideBlur();

        pauseMedia($(this).closest(".modal-multimedia-view"));
    });

    // play/pause audio on click of audio thumbnail
    $(".dm-audio-thumbnail").click(function() {
        var audio = $(this).siblings(".dm-audio").children("audio").get(0);
        if (audio) {
            if (audio.paused) {
                audio.play();
            } else {
                audio.pause();
            }
        }
    });
});
