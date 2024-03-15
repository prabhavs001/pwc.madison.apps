(function (document, $) {
    "use strict";
    var $doc = $(document), _ui = $(window).adaptTo("foundation-ui");
    $doc.on('foundation-contentloaded', function (e) {
        var _pwcAudienceField, _pwcAccessField, _pwcLicenseField,_pwcLicenseFieldWrapper, _unpublishButton, _ui, _currentAssetPath;
        _pwcAudienceField = document.querySelector('[name="./jcr:content/metadata/pwc-audience"]');
        _pwcAccessField = document.querySelector('[name="./jcr:content/metadata/pwc-access"]');
         _pwcLicenseField = document.querySelector('[name="./jcr:content/metadata/pwc-license"]');
         _unpublishButton = $('button[trackingelement=unpublish]');
         _currentAssetPath = $('form').attr('data-formid');
        try {
            function checkDocState(){
                $.get("/bin/pwc/getDocState", {
                    "path": _currentAssetPath
                }, function(result){
                    /* show unpublish button only when the docstate is Approved or Published */
                    if((result === "Published") && _unpublishButton !== undefined) {
                        _unpublishButton.removeAttr('disabled');
                    }
                }, 'text');
            }    

            function onUnpublishSuccess(){
                _unpublishButton.attr('disabled','');
                _ui.notify(Granite.I18n.get("Success"), Granite.I18n.get("Docstate set to unpublished"), "success");
            }

            _unpublishButton.on('click', function(){
                var refCallAjax;
                if(_currentAssetPath !== undefined){
                    refCallAjax = $.post("/bin/pwc/setDocState", {
                        "path": _currentAssetPath,
                        "docstate" : 'Unpublished'
                    }, onUnpublishSuccess(), 'text')
                    .fail(function(){
                        _ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Failed to update docstate property"), "error");
                    });
                }
            });

            /* check docstate on load to show unpublish button */
            checkDocState();

            if (_pwcAudienceField !== undefined && _pwcAudienceField !== null ) {
                _pwcAudienceField.on('change', function (e) {
                _pwcAccessField.value = '';
                _pwcAccessField.trigger("change");
                    if (e.target.value !== 'externalOnly' && e.target.value !== 'internalExternal') {
                        if (_pwcAccessField && typeof _pwcLicenseField !== undefined && _pwcLicenseField && !_pwcLicenseField.hasAttribute('disabled')) {
                            _pwcLicenseFieldWrapper = _pwcLicenseField.parentElement;
                            /**hide and disable license field */
                            if (_pwcLicenseFieldWrapper && _pwcLicenseFieldWrapper.className && 'coral-Form-fieldwrapper' === _pwcLicenseFieldWrapper.className) {
                                _pwcLicenseField.value = '';
                                _pwcLicenseField.required=false;
                                _pwcLicenseFieldWrapper.hidden = true;
                            }
                        }
                    }
                    else {
                        if (_pwcAccessField && _pwcAccessField.value === 'licensed' && typeof _pwcLicenseField !== undefined && _pwcLicenseField) {
                            _pwcLicenseFieldWrapper = _pwcLicenseField.parentElement;
                            if (_pwcLicenseFieldWrapper && _pwcLicenseFieldWrapper.className && 'coral-Form-fieldwrapper' === _pwcLicenseFieldWrapper.className) {
                                _pwcLicenseFieldWrapper.hidden = false;
                            }
                        }
                    }
                });
            }

            if (_pwcAccessField !== null && _pwcAccessField !== undefined) {
                _pwcAccessField.on('change', function (e) {
                    if (e.target.value !== 'licensed') {
                        if (typeof _pwcLicenseField !== undefined && _pwcLicenseField && !_pwcLicenseField.hasAttribute('disabled')) {
                            /**clear license field */
                                _pwcLicenseField.value = '';
                        }
                    }
                    else{
                        _pwcLicenseField.required=true;
                    }
                });
            }
        } catch (error) {

        }

    });

}(document, Granite.$));