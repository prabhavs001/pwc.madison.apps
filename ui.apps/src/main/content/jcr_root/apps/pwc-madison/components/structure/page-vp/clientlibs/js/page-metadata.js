(function (document, $) {
    "use strict";

    function toggleLicensesContainerOnAccessValue(accessLevelValue, $licensesContainer, $licensesSelect) {
        if(accessLevelValue === "licensed"){
            $licensesContainer.removeClass("hide");
            $licensesSelect.prop('disabled', false);
        } else {
            $licensesContainer.addClass("hide");
            $licensesSelect.prop('disabled', true);
        }
    }

    function accessLevelShowHide(component, element) {
        var accessLevelValue,
            $licensesContainer = $(".licenses-container"),
            $licensesSelect = $licensesContainer.find("coral-select");

        //in page properties (only) values are not populated in select at the time of execution of this script
        if (component.value) {
            accessLevelValue = component.value;
        } else if (typeof component.getValue === "function") {
            accessLevelValue = component.getValue();
        } else{
            accessLevelValue = $(element).find("coral-select-item[selected]").first().val();
        }

        toggleLicensesContainerOnAccessValue(accessLevelValue, $licensesContainer, $licensesSelect);

    }

    function audienceTypeShowHide(component, element) {
        var audienceTypeValue, accessLevelValue,
            $privateUserGroupsContainer = $(".private-user-groups-container"),
            $privateUserGroupsSelect = $privateUserGroupsContainer.find("coral-select"),
            $accessLevelContainer = $(".access-level-container"),
            $accessLevelSelect = $accessLevelContainer.find("coral-select"),
            $licensesContainer = $(".licenses-container"),
            $licensesSelect = $licensesContainer.find("coral-select");

        //in page properties (only) values are not populated in select at the time of execution of this script
        if (component.value) {
            audienceTypeValue = component.value;
        } else if (typeof component.getValue === "function") {
            audienceTypeValue = component.getValue();
        } else{
            audienceTypeValue = $(element).find("coral-select-item[selected]").first().val();
        }

        if(audienceTypeValue === "internalOnly"){
            $privateUserGroupsContainer.addClass("hide");
            $privateUserGroupsSelect.prop('disabled', true);
            $accessLevelContainer.addClass("hide");
            $accessLevelSelect.prop('disabled', true);
            $licensesContainer.addClass("hide");
            $licensesSelect.prop('disabled', true);
        } else if(audienceTypeValue === "externalOnly" || audienceTypeValue === "internalExternal"){
            $privateUserGroupsContainer.addClass("hide");
            $privateUserGroupsSelect.prop('disabled', true);

            $accessLevelContainer.removeClass("hide");
            $accessLevelSelect.prop('disabled', false);

            //toggle license container as well on the basis of access level value
            accessLevelValue = $accessLevelSelect.find("coral-select-item[selected]").first().val();
            toggleLicensesContainerOnAccessValue(accessLevelValue, $licensesContainer, $licensesSelect);

        } else if (audienceTypeValue === "privateGroup") {
            $privateUserGroupsContainer.removeClass("hide");
            $privateUserGroupsSelect.prop('disabled', false);
            $accessLevelContainer.addClass("hide");
            $accessLevelSelect.prop('disabled', true);
            $licensesContainer.addClass("hide");
            $licensesSelect.prop('disabled', true);
        }
        else {
            $privateUserGroupsContainer.addClass("hide");
            $privateUserGroupsSelect.prop('disabled', true);
            $accessLevelContainer.addClass("hide");
            $accessLevelSelect.prop('disabled', true);
            $licensesContainer.addClass("hide");
            $licensesSelect.prop('disabled', true);
        }

    }

    //Coral.commons.ready handler is called for other coral elements as well like for Path browser. Therefore, need to
    //check if it is called on the correct element or not.

    function audienceTypeShowHideHandler(element) {
        Coral.commons.ready(element, function (component) {
            if(element.length){
                audienceTypeShowHide(component, element);
                component.on("change", function () {
                    audienceTypeShowHide(component, element);
                });
            }
        });
    }

    function accessLevelShowHideHandler(element) {
        Coral.commons.ready(element, function (component) {
            if(element.length){
                accessLevelShowHide(component, element);
                component.on("change", function () {
                    accessLevelShowHide(component, element);
                });
            }
        });
    }

    function disableContentAuthFieldForTemplate(selectedTemplate){

        //disable audience-type field for template landing page with value internalOnly
        var disabledForTemplate = ["/conf/pwc-madison/settings/wcm/templates/templates-landing-page"],
            disabledDefaultValue = "internalOnly", $audienceType = $(".audience-type");

        if(disabledForTemplate.indexOf(selectedTemplate) > -1){
            $audienceType.val(disabledDefaultValue);
            $audienceType.prop('disabled', true);
        }
    }

    // when dialog gets injected
    $(document).on("foundation-contentloaded", function (e) {

        disableContentAuthFieldForTemplate($("input[name=template]").val());

        //custom validation for save buttons in create page wizard and edit page properties
        $("button[type='submit']").click(function() {
            if($(".audience-type").length){
                var ui = $(window).adaptTo("foundation-ui"),
                    $accessLevelSelect = $(".access-level-container coral-select"),
                    $privateUserGroupsSelect = $(".private-user-groups-container coral-select"),
                    $licensesSelect = $(".licenses-container coral-select"), error = false, errorMessage,
                    ok = {
                        text: Granite.I18n.get('OK'),
                        primary: true
                    };

                if( !($accessLevelSelect.prop("disabled") || $accessLevelSelect.val()) ){
                    error = true;
                    errorMessage = "Please select authorized Access Level";
                }

                if( !($privateUserGroupsSelect.prop("disabled") || $privateUserGroupsSelect.val()) ){
                    error = true;
                    errorMessage = "Please select authorized Private User Groups";
                }

                if( !($licensesSelect.prop("disabled") || $licensesSelect.val()) ){
                    error = true;
                    errorMessage = "Please select authorized Licenses";
                }

                if(error) {
                    ui.prompt('Error', errorMessage, 'error', [ok]);
                    return false;
                }
            }

        });

        // if there is already an initial value make sure the according target element becomes visible
        audienceTypeShowHideHandler($(".audience-type", e.target));
        accessLevelShowHideHandler($(".access-level-container coral-select", e.target));
    });

    $(document).on("selected", ".audience-type", function (e) {
        audienceTypeShowHideHandler($(this));
    });

    $(document).on("selected", ".access-level-container coral-select", function (e) {
        accessLevelShowHideHandler($(this));
    });


}(document, Granite.$));
