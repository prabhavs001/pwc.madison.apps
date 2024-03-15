/* global window, observer */

(function() {

    "use strict";

    var roles = ["publishers"],
        REVIEW_TAB_SELECTOR = ".right-wrapper coral-tab:eq(1)", // couldn't find a better selector for review tab. no class or id
        CHANGES_TAB_SELECTOR = "coral-tab.review-change-tab",
        REVIEW_PANEL_SELECTOR = ".inline-review-panel",
        CHANGES_PANEL_SELECTOR = ".review-changes-panel",
        MutationObserver = window.MutationObserver,
        observer;

    function check() {
	    var reviewTab = document.querySelector(CHANGES_TAB_SELECTOR);

        if(reviewTab) {
            // disconnect the observer once the tabs are added to the dom
            observer.disconnect();

            $.ajax({
                url : "/bin/pwc-madison/role-rendercondition",
                data : { roles: roles, allowAdmin : false },
                success : function(response) {
                    if(response === "true") {
                        // hide the review/changes tabs and panels for publishers
                        $(REVIEW_TAB_SELECTOR).hide();
                        $(CHANGES_TAB_SELECTOR).hide();
                        $(REVIEW_PANEL_SELECTOR).hide();
                        $(CHANGES_PANEL_SELECTOR).hide();
                    }
                }
            });
        }
	}

	observer = new MutationObserver(check);
	observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    });

}());