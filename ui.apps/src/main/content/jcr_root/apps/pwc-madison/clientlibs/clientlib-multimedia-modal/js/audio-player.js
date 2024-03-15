$(function() {

    var $audio = $(".dm-audio audio"),
        multimediaMessages = $("#multimedia-messages").data();

    $audio.on("pause", function() {
        var $multimediaModal = $(this).closest(".modal-multimedia-item"),
            $audioMessage = $multimediaModal.find(".video-message");
        $audioMessage.text(multimediaMessages.continueAudioMessage);
        $multimediaModal.addClass("modal-related-content");
    });

    $audio.on("play", function() {
        if(this.currentTime === 0){
            $(document).trigger("audioStarted");
        }
        var $multimediaModal = $(this).closest(".modal-multimedia-item");
        $multimediaModal.removeClass("modal-related-content");
    });

    $audio.on("ended", function() {
        $(document).trigger("audioCompleted");
        var $multimediaModal = $(this).closest(".modal-multimedia-item"),
            $audioMessage = $multimediaModal.find(".video-message");
        $audioMessage.text(multimediaMessages.thankYouAudioMessage);
        $multimediaModal.addClass("modal-related-content");
    });

    $audio.on("timeupdate", function() {
        var audioDurationInSec = Math.ceil(this.duration), currentTime = this.currentTime, viewedPercentage;
        currentTime = Math.floor(currentTime);
        $(document).trigger("audioTime", [{ "time": currentTime }]);

        viewedPercentage = currentTime / audioDurationInSec;
        if (viewedPercentage >= 0.75 && viewedPercentage < 0.80) {
            $(document).trigger("audioTime75");
        } else if (viewedPercentage >= 0.50 && viewedPercentage < 0.55) {
            $(document).trigger("audioTime50");
        } else if (viewedPercentage >= 0.25 && viewedPercentage < 0.30) {
            $(document).trigger("audioTime25");
        }
    });

});
