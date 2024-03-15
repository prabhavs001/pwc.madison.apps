(function($, $document) {
    var COMP_SELECTOR = '.s7dm-dynamic-media',
        multimediaMessages = $("#multimedia-messages").data();

    function addWatchStateMessage(viewer, s7event) {
        var $multimediaModal = $("#" + viewer.containerId).closest(".modal-multimedia-item"),
            $videoMessage = $multimediaModal.find(".video-message");
        if (s7event && s7event.state) {
            if (s7event.state.state === 13) {
                //on first time videoPlayer time returns NaN
                if(isNaN(viewer.videoplayer.getCurrentTime()) || viewer.videoplayer.getCurrentTime() === 0){
                    $(document).trigger("videoStarted");
                }
                $multimediaModal.removeClass("modal-related-content");
            } else if (s7event.state.state === 14) {
                $videoMessage.text(multimediaMessages.continueVideoMessage);
                $multimediaModal.addClass("modal-related-content");
            } else if (s7event.state.state === 40) {
                $(document).trigger("videoCompleted");
                $videoMessage.text(multimediaMessages.thankYouVideoMessage);
                $multimediaModal.addClass("modal-related-content");
            }
        }
    }

    function s7sdkReady(viewer, s7sdk) {

        var container = viewer.container, $modalSlickSlide;

        function videoPlayerInitialized() {
            container.removeEventListener(s7sdk.event.ResizeEvent.ADDED_TO_LAYOUT, videoPlayerInitialized);

            viewer.videoplayer.addEventListener(s7sdk.event.StatusEvent.NOTF_VIEW_READY, function(event) {
                //play the first video of the carousel
                $modalSlickSlide = $("#" + viewer.containerId).closest(".slick-slide");
                if($modalSlickSlide.data("slickIndex") === 0){
                    viewer.videoplayer.play();
                }
            });

            viewer.videoplayer.addEventListener(s7sdk.event.CapabilityStateEvent.NOTF_VIDEO_CAPABILITY_STATE, function(event) {
                addWatchStateMessage(viewer, event.s7event);
            });

            viewer.videoplayer.addEventListener(s7sdk.event.VideoEvent.NOTF_CURRENT_TIME, function(event) {
                var videoDurationInSec = Math.ceil(viewer.videoplayer.getDuration() / 1000),
                currentTime, viewedPercentage;
                if (event.s7event && event.s7event.data) {
                    currentTime = event.s7event.data;
                    //change ms to sec
                    currentTime = Math.floor(currentTime / 1000);

                    $(document).trigger("videoTime", [{ "time": currentTime }]);

                    viewedPercentage = currentTime / videoDurationInSec;
                    if (viewedPercentage >= 0.75 && viewedPercentage < 0.80) {
                        $(document).trigger("videoTime75");
                    } else if (viewedPercentage >= 0.50 && viewedPercentage < 0.55) {
                        $(document).trigger("videoTime50");
                    } else if (viewedPercentage >= 0.25 && viewedPercentage < 0.30) {
                        $(document).trigger("videoTime25");
                    }
                }
            });

        }

        if (container.isInLayout()) {
            videoPlayerInitialized();
        } else {
            container.addEventListener(s7sdk.event.ResizeEvent.ADDED_TO_LAYOUT, videoPlayerInitialized, false);
        }
    }

    function getViewer(compId) {
        if (!compId) {
            return;
        }

        return new window.Promise(function(resolve, reject) {
            var INTERVAL;
            INTERVAL = setInterval(function() {
                var viewer = S7dmUtils[compId];

                if (!viewer || !viewer.initializationComplete) {
                    return;
                }

                clearInterval(INTERVAL);

                resolve(viewer);
            }, 100);
        });
    }

    function handleViewer(viewer) {
        if (!viewer instanceof s7viewers.VideoViewer) {
            return;
        }

        var s7sdk = window.s7classic.s7sdk;

        viewer.s7params.addEventListener(s7sdk.Event.SDK_READY, function() {
            s7sdkReady(viewer, s7sdk);
        }, false);
    }

    function findViewer() {
        $(COMP_SELECTOR).each(function() {
            getViewer($(this).attr('id')).then(handleViewer);
        });
    }

    $document.ready(findViewer);

}(jQuery, jQuery(document)));
