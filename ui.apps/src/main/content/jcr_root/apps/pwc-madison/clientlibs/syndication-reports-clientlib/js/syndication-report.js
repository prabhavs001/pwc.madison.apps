/*jslint devel: true */
/*global Blob */
/*jshint onevar: false */
$(function() {

	$(".validation-modal").hide();

	var showDialogBox = function(classString) {
		document.querySelector('.' + classString).show();
	},
	clickHandler = function(selector, apiUrl, fileName){
	    var folderPath, ajaxOptions, jqxhr;
		
		$(selector).attr("disabled", true);
		$(".syndication-console-wait").removeClass("hidden");
		folderPath = $(".source-folder-path").val();
		if (!folderPath) {
			showDialogBox("validation-modal");
			$(selector).attr("disabled", false);
			$(".syndication-console-wait").addClass("hidden");
		}
		else {
			//  window.location.href = "/bin/pwc/syndication/failure/report.csv?path=" + folderPath;
			ajaxOptions = {
				type: "get",
				data: {
				path: folderPath
				},
			url: apiUrl
			};
			jqxhr = $.ajax(ajaxOptions);
			jqxhr.done(function(result) {
				$(selector).attr("disabled", false);
				$(".syndication-console-wait").addClass("hidden");
					var blob = new Blob([result]),
					link;
					//check for IE
					if (window.navigator !== undefined && window.navigator.msSaveOrOpenBlob) {
						link = $("#export-csv");
						link.click(function() {
							window.navigator.msSaveOrOpenBlob(blob, fileName);
						});
					} else {
						link = document.createElement('a');
						link.href = window.URL.createObjectURL(blob);
						link.download = fileName;
					}
					link.click();
				});
				jqxhr.fail(function(error) {
					$(selector).attr("disabled", false);
					$(".syndication-console-wait").addClass("hidden");
					showDialogBox("error-modal");
				});
			}
	},
	failureCsvDownloadBtn = ".download-csv-button",
	publishDetailsCsvDownloadBtn = ".download-publish-details-csv-button";
	$(failureCsvDownloadBtn).click(function() {
		clickHandler(failureCsvDownloadBtn, "/bin/pwc/syndication/failure/report.csv", "Syndication_Failure_Report.csv");
	});
	$(publishDetailsCsvDownloadBtn).click(function() {
		clickHandler(publishDetailsCsvDownloadBtn,"/bin/pwc/syndication/publish/report.csv", "Syndication_Publish_Details.csv");
	});
});
