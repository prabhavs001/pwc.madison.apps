$(document).ready(function () {
    $('[name="./jcr:content/metadata/pwc-expirationDate"]').on('change', function () {		
        $("input[name='./jcr:content/metadata/pwc-expiryConfirmationState']").val('');
        $("input[name='./jcr:content/metadata/pwc-approvedRejectedBy']").val('');
        $("input[name='./jcr:content/metadata/pwc-approvedRejectedDate']").val('');
    });

});