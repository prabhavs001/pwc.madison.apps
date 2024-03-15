$(document).ready(function(){
    $("#full-cycle-review-button").click(function () {
        window.fmdita.createFullCycleReview();
    });

    $("#simple-workflow-review-button").click(function () {
        window.fmdita.createSimpleContentReviewTask();
    });

    $("#collaborate-dita-button").click(function () {
        window.fmdita.createCollaborationTask();
    });

});