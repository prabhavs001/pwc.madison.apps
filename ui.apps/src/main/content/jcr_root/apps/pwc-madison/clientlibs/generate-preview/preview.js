/* global console */

(function() {

    "use strict";

    var OUTPUT_VIEW_SELECTOR = 'div[data-foundation-mode-switcher-item-mode="output-view"]',
        PREVIEW_OUTPUT_VIEW_SELECTOR = 'div[data-foundation-mode-switcher-item-mode="preview-output-view"]',
        REPORT_IFRAME_SELECTOR = "#report_iframe",
        GENERATAE_BTN_SELECTOR = ".btn-op-generate",
        IS_DISABLED_CLASS = "is-disabled",
        EDIT_SELECTOR = "#btn-op-edit",
        SAVE_EDITOR = "#btn-op-save",
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
	
	/**
	 * Method to show modal on event.
	 * 
	 * @param {string}
	 *            id - Modal Id.
	 * @param {string}
	 *            type - Modal Type.
	 * @param {string}
	 *            heading - Modal Heading.
	 * @param {string}
	 *            message - Modal Message.
	 */
    function showModal(id, type, heading, message){
        // create modal definition
        var modalSelector = '#' + id,
            modal;
        if ($(modalSelector).length) {
            $(modalSelector).show();
        }else{
            modal = new Coral.Alert().set({
	            id:id,
				variant: type,
	            style: "width:281px;top:1px;margin-left:40%;position:absolute",
				header: {
					innerHTML: heading
				},
				content: {
					innerHTML: message
				}
			});
	        
	        $("coral-actionbar.betty-ActionBar").append(modal);
	    }
        
    }
	 
    $(document).on("foundation-contentloaded", function() {
        var generateBtn = $(GENERATAE_BTN_SELECTOR),
            button = new Coral.Button().set({
	            id: "generate-preview",
	            label: {
	                innerHTML: "Generate Preview"
	            },
	            variant: "quiet",
	            icon: "preview",
	            iconSize: "S"
	        });

        // no option to add the class in the button constructor. adding it to the class list
        button.classList.add("coral-Button--graniteActionBar");

        if($(OUTPUT_VIEW_SELECTOR).length > 0) {
            $(OUTPUT_VIEW_SELECTOR).append(button);
        } else {
            $(PREVIEW_OUTPUT_VIEW_SELECTOR).append(button);
            $(PREVIEW_OUTPUT_VIEW_SELECTOR).addClass("foundation-mode-switcher-item-active");
        }
        //disable edit of preview preset
        $(EDIT_SELECTOR).on("click", function(event) {
            var reportIframe = $(REPORT_IFRAME_SELECTOR),
              preset, dialog;
            try {
              if (reportIframe) {
                preset = $(reportIframe).contents().find("tr.fm_tocOutput.coral-Table-row.selected");
                if (preset && preset.length > 0 && "previewsite" === $(preset).attr("data-opname")) {
                  $(EDIT_SELECTOR).addClass(IS_DISABLED_CLASS);
                  $("#warningPreviewEdit").hide();
                  dialog = new Coral.Dialog().set({
                    id: 'warningPreviewEdit',
                    variant: 'warning',
                    header: {
                      innerHTML: 'Warning'
                    },
                    content: {
                      innerHTML: 'Editing of preview preset not allowed.'
                    },
                    footer: {
                      innerHTML: '<button id="previewEditButton" is="coral-button" variant="default" coral-close>Ok</button>'
                    }
                  });
                  document.body.appendChild(dialog);
                  document.querySelector('#warningPreviewEdit').show();
                  dialog.on('click', '#previewEditButton', function() {
                    $(EDIT_SELECTOR).removeClass(IS_DISABLED_CLASS);
                  });
                  event.stopImmediatePropagation();
                  return false;
                }
              }
            } catch (err) {}
          });
        
        function showWarning(idxs, headerHtml, contentHtml, buttonIdx, event) {
            var dialog = new Coral.Dialog().set({
              id: idxs,
              variant: 'warning',
              header: {
                innerHTML: headerHtml
              },
              content: {
                innerHTML: contentHtml
              },
              footer: {
                innerHTML: '<button id="generateSiteConfigId" is="coral-button" variant="default" coral-close>Ok</button>'
              }
            });
            document.body.appendChild(dialog);
            document.querySelector('#' + idxs).show();
            event.stopImmediatePropagation();
            return false;
          }

          function checkValidity(event) {
            var report = $(REPORT_IFRAME_SELECTOR),
              preset, tmodal, ditaConsole;
            try {
              if (report) {
                ditaConsole = $(report).contents();
                preset = $(ditaConsole).find("tr.fm_tocOutput.coral-Table-row.selected");
                if (preset && preset.length > 0 && "aemsite" === preset.attr("data-opname")) {
                  tmodal = $(ditaConsole).find("span[data-text='.d.output.active.postPublishWorkflowTitle']").text();
                  if (tmodal) {
                    if (tmodal !== "Madison DITA PostGeneration") {
                      showWarning('selectionId', 'warning', 'Please select Madion DITA PostGenertaion WorkFlow for aem site', 'buttonSelectionId', event);
                    }
                  }
                }
              }
            } catch (err) {}
          }
        
        function isPWCDitaMapPath(){
           return window.location.pathname.indexOf("/content/dam/pwc-madison/ditaroot") > -1;
       }


        $(SAVE_EDITOR).on("click",function(event){
            if(isPWCDitaMapPath()){
                checkValidity(event);
            }
        });
         
        // call the ootb output generation servlet. using the preview site output preset name
        button.on("click", function(e) {
            
           

            var source = window.payload ? window.payload : location.search.replace("?payload=", ""),
                data = {"operation": "GENERATEOUTPUT", "source" : source, "outputName" : "previewsite"};

            $.ajax({
                url: "/bin/pwc-madison/update-preview-output-target",
                data : data
            }).done(function() {
                $.ajax({
                    url: "/bin/publishlistener",
                    data: data
                }).done(function() {
                   showSuccess("Generate preview output started");
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
        });

        // make the generate preview action bar active every time outputs tab is clicked when the default action bar is not present
        $(REPORT_IFRAME_SELECTOR).on("load", function() {
            $(REPORT_IFRAME_SELECTOR).contents().find("#fm_assetDetails coral-tab.report-tab").on("click", function(e) {

                // there is not action bar for outputs tab. so showing the generate preview bar
                if($(OUTPUT_VIEW_SELECTOR).length === 0) {
                    if($(e.currentTarget).hasClass("outputs")) {
                        $(PREVIEW_OUTPUT_VIEW_SELECTOR).removeClass("foundation-mode-switcher-item");
                        $(PREVIEW_OUTPUT_VIEW_SELECTOR).addClass("foundation-mode-switcher-item-active");
                    } else {
                        $(PREVIEW_OUTPUT_VIEW_SELECTOR).addClass("foundation-mode-switcher-item");
                    }
                }
            });
        });
    });
	$(document).on('click', ".btn-op-unpublish", function(e) {

		var dialog = new Coral.Dialog().set({
			id: 'unpublishDialog',
			variant: 'warning',
			header: {
				innerHTML: 'Unpublish Pages'
			},
			content: {
				innerHTML: 'This action will remove the selected content from the live Viewpoint site and render it unpublished in XML add on site. <br>If this is not the expected outcome, hit cancel.'
			},
			footer: {
				innerHTML: '<button id="cancelButton" is="coral-button" variant="default" coral-close>Cancel</button><button id="unpublishButton" is="coral-button" variant="primary">Unpublish</button>'
			}
		});
		document.body.appendChild(dialog);
		document.querySelector('#unpublishDialog').show();

		dialog.on('click', '#unpublishButton', function() {
			var pageUrl = decodeURIComponent(window.location.href),
				mapPath = pageUrl.substring(pageUrl.indexOf("/content/dam/pwc-madison/"), pageUrl.length);
			dialog.hide();

			$.ajax({
				url: "/bin/pwc-madison/unpublish?mapPath=" + mapPath
			}).done(function() {
				showSuccess("Successfully unpublished pages.");
				setTimeout(function() {
					location.reload();
				}, 1000);

			}).fail(function() {
				var alert = new Coral.Alert().set({
					variant: "error",
					style: "width:281px;top:-8px;margin-left:40%",
					header: {
						innerHTML: "ERROR"
					},
					content: {
						innerHTML: "Unable to unpublish pages. Please try again later or contact your administrator.<div style=\"text-align:right\"><button is=\"coral-button\" variant=\"minimal\" coral-close>Close</button></div>"
					}
				});
				$('.foundation-layout-util-maximized-container').prepend(alert);

			});

		});

	});
	$(document).on('click', "#btn-bl-create", function(e) {
        var head = $("#report_iframe").contents().find("head"), css = '<style type="text/css">' +
          '#browse_all{ display:none;}' +
          '</style>';
		$(head).append(css);
    });
}());
