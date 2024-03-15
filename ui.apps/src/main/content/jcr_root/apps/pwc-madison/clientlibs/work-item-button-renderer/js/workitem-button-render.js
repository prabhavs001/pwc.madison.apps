$(document).ready(function () {
    var modelTitle = $('[name="modelTitle"]').val(),
        regenerateButton = $(".inbox-details-workitem-action--regenerate"),
        workflowId = $('[name="workflowId"]').val(),
        contentPath = $('[name="contentPath"]').val(),
        inlineReviewButton = $(".inbox-details-workitem-inline-review"),
        ditamapsReviewButton = $(".inbox-details-workitem-ditamaps-review");
    if ((!modelTitle || (modelTitle !== "PwC Fullcyle Workflow" && modelTitle !== "PwC Simplified WF")) && regenerateButton) {
        regenerateButton.hide();
    } else if (regenerateButton) {
        regenerateButton.click(function () {
            if (workflowId) {
                window.open("/apps/fmdita/report/publishing-points-listing.html?ditamap=" + contentPath + "&workflowId=" + workflowId);
            }
        });
    }

    if ((!modelTitle || (modelTitle !== "PwC Fullcyle Workflow" && modelTitle !== "PwC Simplified WF")) && inlineReviewButton) {
        inlineReviewButton.hide();
    } else if (inlineReviewButton) {
        $.get("/bin/pwc/checkforreviewitems", {
            workflowId: workflowId,
            isInlineButton: "true"
        }, null, 'json').then(function (res) {
            if (res.isRender) {
                inlineReviewButton.show();
                inlineReviewButton.click(function () {
                    if (workflowId) {
                        window.open(res.reviewPage);
                    }
                });
            } else {
                inlineReviewButton.hide();
            }
        });
    }

    if ((!modelTitle || (modelTitle !== "PwC Fullcyle Workflow" && modelTitle !== "PwC Simplified WF")) && ditamapsReviewButton) {
        ditamapsReviewButton.hide();
    } else if (ditamapsReviewButton) {
        $.get("/bin/pwc/checkforreviewitems", {
            workflowId: workflowId
        }, null, 'json').then(function (res) {
            if (res.isRender) {
                ditamapsReviewButton.show();
                ditamapsReviewButton.click(function () {
                    if (workflowId) {
                        window.open("/apps/fmdita/report/map-version-history.html?workflowId=" + workflowId);
                    }
                });
            } else {
                ditamapsReviewButton.hide();
            }
        });
    }
});