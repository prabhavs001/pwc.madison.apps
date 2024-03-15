(function ($, undefined) {
    "use strict";
    var FULL_CYCLE_REVIEW_BUTTON = "full-cycle-review-button";
    var SIMPLE_REVIEW_BUTTON = "simple-workflow-review-button";
    var REVIEW_DITA_ELEMENT_ID = "review-dita-button";
    var FULL_CYCLE_WORFLOW_TYPE = "full";
    var SIMPLE_CYCLE_WORKFLOW_TYPE = "simple";
    var OOTB_WORKFLOW_TYPE = "ootb";
    var STANDARD_SETTERS = "standardSetters";
    var isButtonVisible = false;
    var isFullCycleButtonVisible = false;
    var isSimpleButtonVisible = false;
    var isCreateButtonVisible = false;

    $("#" + FULL_CYCLE_REVIEW_BUTTON).css("display", "none");
    $("#" + SIMPLE_REVIEW_BUTTON).css("display", "none");
    
	function sanitizeString(string) {
		var tempDiv = document.createElement('div');
		tempDiv.textContent = string;
		return tempDiv.innerHTML;
	};

    getStandardSetters();

    /**
     * Makes ajax call to fetch the standard setters configured in the OSGI
     */
    function getStandardSetters() {
        var targetUrl = "/bin/pwc-madison/fetchSetters.json";
        var ajaxOptions = {
            type: "get",
            url: targetUrl
        };

        var jqxhr = $.ajax(ajaxOptions);
        jqxhr.done(function (resultJson) {
            //The standard setter names are fetched from OSGI and persisted in session storage. 
            sessionStorage.setItem(STANDARD_SETTERS, JSON.stringify(resultJson));
        });
        jqxhr.fail(function (error) {
            console.error("failed to get the standard setter configurations :- ", sanitizeString(error.statusText));
        });
    }

    /**
     * Picks the asset path on selection and determines whether to show or hide the 
     * workflow buttons.
     */
    $(document).on("foundation-selections-change", function (e) {
        isButtonVisible = false;
        isFullCycleButtonVisible = false;
        isSimpleButtonVisible = false;
        isCreateButtonVisible = false;

        setTimeout(function () {
            var selectedAsset = $(".foundation-collection-item.is-selected");
            var multipleItems = selectedAsset.length > 1;
            var path = selectedAsset.data("foundationCollectionItemId");
            standardSetterCheck(path, FULL_CYCLE_WORFLOW_TYPE);
            standardSetterCheck(path, SIMPLE_CYCLE_WORKFLOW_TYPE);
            standardSetterCheck(path, OOTB_WORKFLOW_TYPE);
            if (!multipleItems) {
                if (!path) {
                    return;
                }
                showButton(path);
            } else {
                isButtonVisible = false;
            }
            if (isButtonVisible && isFullCycleButtonVisible) {
                $("#" + FULL_CYCLE_REVIEW_BUTTON).css("display", "block");
            } else {
                $("#" + FULL_CYCLE_REVIEW_BUTTON).css("display", "none");
            }
            if (isButtonVisible && isSimpleButtonVisible) {
                $("#" + SIMPLE_REVIEW_BUTTON).css("display", "block");
            } else {
                $("#" + SIMPLE_REVIEW_BUTTON).css("display", "none");
            }
            if (isButtonVisible && isCreateButtonVisible) {
                $("#" + REVIEW_DITA_ELEMENT_ID).css("display", "block");
            } else {
                $("#" + REVIEW_DITA_ELEMENT_ID).css("display", "none");
            }
        }, 10);
    });

    /**
     * This sets a flag which determines whether to show full cycle and simple review task buttons
     * based on the type of asset selected.
     * @param {*} path 
     */
    function showButton(path) {
        var lowerCasePath = path.toLowerCase();
        var isDitamap = lowerCasePath.endsWith(".ditamap");
        if (isDitamap) {
            isButtonVisible = true;
        } else {
            isButtonVisible = false;
        }
    }

    /**
     * This method sets flags separately for full-cycle and simple review task based 
     * on the allowed standard setters configured in OSGI
     * @param {*} path - selected asset path 
     * @param {*} workflowType - "full" for full-cycle workflow, "simple" for simple review workflow
     */
    function standardSetterCheck(path, workflowType) {
        if (sessionStorage.getItem(STANDARD_SETTERS) && path) {
            var stdSetterString = sessionStorage.getItem(STANDARD_SETTERS);
            var stdSetterJson = JSON.parse(stdSetterString);
            var standardSetterArray;
            if (workflowType === FULL_CYCLE_WORFLOW_TYPE) {
                standardSetterArray = stdSetterJson.fullCycleWorkflowSetters;
                if(standardSetterArray.length > 0){
                $.each(standardSetterArray, function (key, value) {
                    var patt = new RegExp(value);
                    var res = patt.test(path);
                    if (!res) {
                        isFullCycleButtonVisible = true;
                    } else {
                        return false;
                    }
                });
            } else {
                isFullCycleButtonVisible = true;
            }
            } else if (workflowType === SIMPLE_CYCLE_WORKFLOW_TYPE) {
                standardSetterArray = stdSetterJson.simpleWorkflowSetters;
                if(standardSetterArray.length > 0){
                $.each(standardSetterArray, function (key, value) {
                    var patt = new RegExp(value);
                    var res = patt.test(path);
                    if (!res) {
                        isSimpleButtonVisible = true;
                    } else {
                        return false;
                    }
                });
            } else {
                isSimpleButtonVisible = true;
            }
            } else {
                var patt = new RegExp("\/content\/dam\/pwc-madison\/.*");
                var res = patt.test(path);
                if (!res) {
                    isCreateButtonVisible = res;
                }
            }
        }
    }
})(Granite.$);