$(function() {
    var submitButton = $(".dam-admin-reports-generate"),
        locale = $('[name="locale"]'),
        action = $('[name="actionName"]'),
        url = '/bin/pwc-madison/vp-indexing',
        reportDetails = $('#report-details');

    submitButton.click(function() {
        if (locale.val() !== undefined && action.val() !== undefined) {
            submitButton.attr('disabled', '');

            var data = {};
            data.action = action.val();
            data.locale = locale.val();

            $.ajax({
                url: url,
                type: 'GET',
                data: data,
                contentType: 'application/json',
                success: function(data) {
                    submitButton.removeAttr('disabled');
                    reportDetails.text(JSON.stringify(data, null, '\t'));
                },
                error: function(request, error) {
                    reportDetails.text("Error");
                    submitButton.removeAttr('disabled');
                }
            });

        } else {
            reportDetails.text('Kindly check Fusion configurations before submitting.');
        }
    });
});