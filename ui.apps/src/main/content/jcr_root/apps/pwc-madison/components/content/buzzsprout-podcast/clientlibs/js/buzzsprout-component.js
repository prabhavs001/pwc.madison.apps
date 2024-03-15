$(document).on("dialog-ready", function() {

    var DATA_MF_NAME = "data-granite-coral-multifield-name",
        mfName = "./scripts",
        SINGLE_PLAYER_SCRIPT_LIMIT = 3,
        PLAYLIST_VIEW_SCRIPT_LIMIT = 2,
        SINGLE_PLAYER_RENDITION = "singlePlayer",
        PLAYLIST_VIEW_RENDITION = "playlistView",
        RADIO_BUTTON_NAME = "./buzzsproutPlayer",
        singlePlayerRenditionMsg = "Only up-to 3 Buzzsprout scripts are allowed for Single Player rendition",
        playlistPlayerRenditionMsg = "Only up-to 2 Buzzsprout scripts are allowed for Playlist View rendition",
        characterExceedText = "Character limit exceeds for Component Title",
        $multifield = $("[" + DATA_MF_NAME + "='" + mfName + "']");

    $(".cq-dialog-submit").click(function() {

        var $form = $(this).closest("form.foundation-form"),
            count,
            renditionType,
			ui = $(window).adaptTo("foundation-ui");

		count = parseInt($multifield[0]._items.length,10);
        renditionType = $('input[name="./buzzsproutPlayer"]:checked').val();

        if($("[name='./buzzsproutTitle']").val().length > 28){
			ui.alert(characterExceedText);
            return false;
		}

        if ((renditionType === SINGLE_PLAYER_RENDITION) && count > SINGLE_PLAYER_SCRIPT_LIMIT) {
            ui.alert(singlePlayerRenditionMsg);
            return false;
        }

        if ((renditionType === PLAYLIST_VIEW_RENDITION) && count > PLAYLIST_VIEW_SCRIPT_LIMIT) {
            ui.alert(playlistPlayerRenditionMsg);
            return false;
        }

    });

});
