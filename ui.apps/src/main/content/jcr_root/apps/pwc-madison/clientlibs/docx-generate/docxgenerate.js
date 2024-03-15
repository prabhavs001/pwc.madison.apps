/* global console */

(function() {

    "use strict";

    var OUTPUT_VIEW_SELECTOR = 'div[data-foundation-mode-switcher-item-mode="output-view"]',
        PREVIEW_OUTPUT_VIEW_SELECTOR = 'div[data-foundation-mode-switcher-item-mode="preview-output-view"]',
        GENERATAE_BTN_SELECTOR = ".btn-op-generate",
        DOCX_CHECKBOX_SELECTOR = "[data-opname='docx'] .coral-Checkbox-input",
        REPORT_IFRAME = "#report_iframe",
        REPORT_IFRAME_SELECTOR = "#report_iframe",
        IS_DISABLED_CLASS = "is-disabled",
        EDIT_SELECTOR = "#btn-op-edit",
        ui = $(window).adaptTo("foundation-ui");

    // using the ootb coral alert with a different message instead of creating a new one.
	function showSuccess(message){
		$("#alert coral-alert-content").text(message);
		$("coral-actionbar.betty-ActionBar").append($("#alert"));
		$("#alert").css("display","block");
		setTimeout(function(){
			$("#alert").css("display","none");
		}, 3000);
	}
	 
    $(document).ready(function() {
        var button = new Coral.Button().set({
	            label: {
	                innerHTML: "Generate DOCX"
	            },
	            variant: "quiet",
	            icon: "publish",
	            iconSize: "S"
	        });

        // no option to add the class in the button constructor. adding it to the class list
        button.classList.add("coral-Button--graniteActionBar");

        //adding the Generate DOCX Button
        if($(OUTPUT_VIEW_SELECTOR).length > 0) {
            $(OUTPUT_VIEW_SELECTOR).append(button);
        } else {
            $(PREVIEW_OUTPUT_VIEW_SELECTOR).append(button);
            $(PREVIEW_OUTPUT_VIEW_SELECTOR).addClass("foundation-mode-switcher-item-active");
        }

        // call the ootb output generation servlet.
        button.on("click", function(e) {
            var source = window.payload ? window.payload : location.search.replace("?payload=", ""),
                data = {"operation": "GENERATEOUTPUT", "source" : source, "outputName" : "docx"};

            $.ajax({
                url: "/bin/publishlistener",
                data: data
            }).done(function() {
               showSuccess("Output generation started");
            }).fail(function(ajx){
                // mimic actual
                if (ajx.status === 409) {
                  var preset = ajx.responseJSON.OUTPUT_NAME;

                  ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Output generation for {0} is already in progress. Please try again after some time.", [preset], "preset name"), "error");
                } else {
                  ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Failed"), "error");
                }
            });

        });

        //disable edit of docx preset
        $(EDIT_SELECTOR).on("click", function(event) {
            var reportIframe = $(REPORT_IFRAME_SELECTOR),
              preset, dialog;
            try {
              if (reportIframe) {
                preset = $(reportIframe).contents().find("tr.fm_tocOutput.coral-Table-row.selected");
                if (preset && preset.length > 0 && "docx" === $(preset).attr("data-opname")) {
                  $(EDIT_SELECTOR).addClass(IS_DISABLED_CLASS);
                  $("#warningPreviewEdit").hide();
                  dialog = new Coral.Dialog().set({
                    id: 'warningPreviewEdit',
                    variant: 'warning',
                    header: {
                      innerHTML: 'Warning'
                    },
                    content: {
                      innerHTML: 'Editing of DOCX preset not allowed.'
                    },
                    footer: {
                      innerHTML: '<button id="docxEditButton" is="coral-button" variant="default" coral-close>Ok</button>'
                    }
                  });
                  document.body.appendChild(dialog);
                  document.querySelector('#warningPreviewEdit').show();
                  dialog.on('click', '#docxEditButton', function() {
                    $(EDIT_SELECTOR).removeClass(IS_DISABLED_CLASS);
                  });
                  event.stopImmediatePropagation();
                  return false;
                }
              }
            } catch (err) {}
        });
    });

}());
