$(function () {

    $(".output-modal").hide();
	$(".validation-modal").hide();

    if($("#autolink-disabled").length !== 0) {
		$(".autolink-button").attr("disabled", true);
    }

    var showDialogBox = function (classString) {
        document.querySelector('.' + classString).show();
    };

    function getUrlVars() {
        var vars = [], hash, hashes, i;
        hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for(i = 0; i < hashes.length; i++)
        {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    }

    function getSelectedRoutines() {
		var routines = [];
        $('.citation-routine-list-container input[name="citation-routines"]:checked').each(function () {
			routines.push(this.value);
        });
        return routines;
    }

    function populateTable(result) {
        Coral.commons.ready(result_table, function (table){
            table.items.clear();
            result.forEach(function (item) {
                var row = table.items.add({});
                row.appendChild(new Coral.Table.Cell().set({
                  content: {
                    textContent : item.patternName
                  }
                }));
                row.appendChild(new Coral.Table.Cell().set({
                  content: {
                    textContent : item.sourcePath
                  }
                }));
                row.appendChild(new Coral.Table.Cell().set({
                  content: {
                    textContent : item.targetPath
                  }
                }));
                row.appendChild(new Coral.Table.Cell().set({
                  content: {
                    textContent : item.status
                  }
                }));
                row.appendChild(new Coral.Table.Cell().set({
                  content: {
                    textContent : item.failureReason
                  }
                }));
            });
        });
    }

	$(".autolink-button").click(function () {
		var path, selectedRoutines, ajaxOptions, jqxhr;
        $(".citation-report-form button.autolink-button").attr("disabled", true);
        $(".citation-report-wrapper .citation-console-wait").removeClass("hidden");
        path = decodeURIComponent(getUrlVars().payload);
        selectedRoutines = getSelectedRoutines();
        if(selectedRoutines.length===0) {
            showDialogBox("validation-modal");
            $(".citation-report-form button.autolink-button").attr("disabled", false);
            $(".citation-report-wrapper .citation-console-wait").addClass("hidden");
        }
        else {
        ajaxOptions = {
                type: "get",
                data: {
                    routineNames: selectedRoutines,
                    path: path
                },
                url: "/bin/pwc-madison/autolink"
            };
            jqxhr = $.ajax(ajaxOptions);
            jqxhr.done(function (result) {
				//$(".citation-report-form button.autolink-button").attr("disabled", false);
                $(".citation-report-wrapper .citation-console-wait").addClass("hidden");
				populateTable(JSON.parse(result));
				showDialogBox("output-modal");
            });
            jqxhr.fail(function (error) {
                $(".citation-report-wrapper .citation-console-wait").addClass("hidden");
				showDialogBox("error-modal");
            });
        }
    });

});