(function (document, $) {
   $(document).on("foundation-contentloaded", function () {
      var pwcPublishingPointField, pwcSearchDocumentField, pwcSearchDocumentFieldWrapper, pwcStaticTocField, pwcStaticTocFieldWrapper, pwcHideRelatedContentField, pwcHideRelatedContentFieldWrapper, pwcEnableJoinTOC, pwcEnableJoinTOCWrapper, pwcJoinTOCLevel, pwcJoinTOCLevelWrapper, pwcEnableLiteTOC, pwcEnableLiteTOCWrapper, pwcOverrideGlobalJoinSettings, pwcOverrideGlobalJoinSettingsWrapper, pwcOverrideSeeAlsoField, pwcOverrideSeeAlsoSection, pwcOverrideSeeAlsoSectionWrapper, pwcSeeAlsoSection, pwcSeeAlsoContentTypesDropdown, pwcSeeAlsoContentTypesDropdownWrapper, pwcSeeAlsoSectionWrapper, pwcSeeAlsoSectionMaxCount, pwcSeeAlsoSectionMaxCountWrapper, pwcAutoSetReferencesBtn, pwcAutoSetReferencesBtnWrapper, pwcAutoSetReferencesInfo, pwcAutoSetReferencesInfoWrapper, _ui = $(window).adaptTo("foundation-ui");
      pwcPublishingPointField = $("[name='./jcr:content/metadata/pwc:isPublishingPoint']");
      pwcOverrideSeeAlsoField = $("[name='./jcr:content/metadata/pwc-overrideSeeAlso']");
      pwcSeeAlsoSection = $("[name='./jcr:content/metadata/pwc-seeAlsoSectionEnabled']");
      try {

         //Function to Show/hide SearchWithInDocument field
         function showHideSearchDocument() {
            pwcSearchDocumentField = $("input[name='./jcr:content/metadata/pwc-hideSearchWithInDoc']");
            //If publishingPoint is set to true show SearchWithInDocument field
            if (pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcSearchDocumentField !== null && pwcSearchDocumentField !== undefined) {
                  pwcSearchDocumentFieldWrapper = pwcSearchDocumentField.closest(".coral-Form-fieldwrapper");
                  //Enabled/show SearchWithInDocument field
                  pwcSearchDocumentField.prop('disabled', false);
                  pwcSearchDocumentFieldWrapper.show();
               }
            } else {
               if (pwcSearchDocumentField !== null && pwcSearchDocumentField !== undefined) {
                  pwcSearchDocumentFieldWrapper = pwcSearchDocumentField.closest(".coral-Form-fieldwrapper");
                  //Hide/disabled SearchWithInDocument field
                  pwcSearchDocumentFieldWrapper.hide();
                  pwcSearchDocumentField.prop('disabled', true);
                  //Set SearchWithInDocument field value to "Yes"
                  pwcSearchDocumentField.each(function () {
                     if ($(this).val() === "yes") {
                        $(this).prop('checked', true);
                     }
                  });
               }
            }
         }

         //Function to Show / Hide Static Toc Field
         function showHideStaticTocField() {
            pwcStaticTocField = $("input[name='./jcr:content/metadata/pwc-showStaticToc']");
            //If publishingPoint is set to true show StaticToc field
            if (pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcStaticTocField !== null && pwcStaticTocField !== undefined) {
                  pwcStaticTocFieldWrapper = pwcStaticTocField.closest(".coral-Form-fieldwrapper");
                  //Enabled/show Static Toc field
                  pwcStaticTocField.prop('disabled', false);
                  pwcStaticTocFieldWrapper.show();
               }
            } else {
               if (pwcStaticTocField !== null && pwcStaticTocField !== undefined) {
                  pwcStaticTocFieldWrapper = pwcStaticTocField.closest(".coral-Form-fieldwrapper");
                  //Hide/disabled Static Toc field
                  pwcStaticTocFieldWrapper.hide();
                  pwcStaticTocField.prop('disabled', true);
                  //Set Static Toc field value to "No"
                  pwcStaticTocField.each(function () {
                     if ($(this).val() === "no") {
                        $(this).prop('checked', true);
                     }
                  });
               }
            }
         }

         //Function to Show / Hide Related Content Field
         function showHideRelatedContentField() {
            pwcHideRelatedContentField = $("input[name='./jcr:content/metadata/pwc-hideRelatedContent']");
            //If publishingPoint is set to true show hideRelatedContent field
            if (pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcStaticTocField !== null && pwcStaticTocField !== undefined) {
                  pwcHideRelatedContentFieldWrapper = pwcHideRelatedContentField.closest(".coral-Form-fieldwrapper");
                  pwcHideRelatedContentField.prop('disabled', false);
                  pwcHideRelatedContentFieldWrapper.show();
               }
            } else {
               if (pwcHideRelatedContentField !== null && pwcHideRelatedContentField !== undefined) {
                  pwcHideRelatedContentFieldWrapper = pwcHideRelatedContentField.closest(".coral-Form-fieldwrapper");
                  //Hide/disabled Hide Related Content field
                  pwcHideRelatedContentFieldWrapper.hide();
                  pwcHideRelatedContentField.prop('disabled', true);
                  //Set Hide Related Content field value to "No"
                  pwcHideRelatedContentField.each(function () {
                     if ($(this).val() === "no") {
                        $(this).prop('checked', true);
                     }
                  });
               }
            }
         }

         //Function to Show / Hide Join Section TOC Items
         function showHideJoinedSectionTOCFields() {
            pwcEnableJoinTOC = $("input[name='./jcr:content/metadata/pwc-joinedSectionToc']");
            pwcJoinTOCLevel = $("input[name='./jcr:content/metadata/pwc-joinedSectionTocLevel']");
            pwcOverrideGlobalJoinSettings = $("input[name='./jcr:content/metadata/pwc-overrideGlobalJoinSettings']");
            //If publishingPoint is set to true show Join Section TOC Items
            if (pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcEnableJoinTOC !== null && pwcEnableJoinTOC !== undefined) {
                  pwcEnableJoinTOCWrapper = pwcEnableJoinTOC.closest(".coral-Form-fieldwrapper");
                  pwcEnableJoinTOC.prop('disabled', false);
                  pwcEnableJoinTOCWrapper.show();
               }
               if (pwcJoinTOCLevel !== null && pwcJoinTOCLevel !== undefined) {
                  pwcJoinTOCLevelWrapper = pwcJoinTOCLevel.closest(".coral-Form-fieldwrapper");
                  pwcJoinTOCLevel.prop('disabled', false);
                  pwcJoinTOCLevelWrapper.show();
               }
               if (pwcOverrideGlobalJoinSettings !== null && pwcOverrideGlobalJoinSettings !== undefined) {
                  pwcOverrideGlobalJoinSettingsWrapper = pwcOverrideGlobalJoinSettings.closest(".coral-Form-fieldwrapper");
                  pwcOverrideGlobalJoinSettings.prop('disabled', false);
                  pwcOverrideGlobalJoinSettingsWrapper.show();
               }
            } else {
               if (pwcEnableJoinTOC !== null && pwcEnableJoinTOC !== undefined) {
                  pwcEnableJoinTOCWrapper = pwcEnableJoinTOC.closest(".coral-Form-fieldwrapper");
                  pwcEnableJoinTOCWrapper.hide();
                  pwcEnableJoinTOC.prop('disabled', true);
               }
               if (pwcJoinTOCLevel !== null && pwcJoinTOCLevel !== undefined) {
                  pwcJoinTOCLevelWrapper = pwcJoinTOCLevel.closest(".coral-Form-fieldwrapper");
                  pwcJoinTOCLevelWrapper.hide();
                  pwcJoinTOCLevel.prop('disabled', true);
               }
               if (pwcOverrideGlobalJoinSettings !== null && pwcOverrideGlobalJoinSettings !== undefined) {
                  pwcOverrideGlobalJoinSettingsWrapper = pwcOverrideGlobalJoinSettings.closest(".coral-Form-fieldwrapper");
                  pwcOverrideGlobalJoinSettings.prop('disabled', true);
                  pwcOverrideGlobalJoinSettingsWrapper.hide();
               }
            }
         }
         //Function to Show / Hide Enable Lite TOC
         function showHideEnableLiteTOCField() {
            pwcEnableLiteTOC = $("input[name='./jcr:content/metadata/pwc-loadLiteToc']");
            //If publishingPoint is set to true show Enable Lite TOC
            if (pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcEnableLiteTOC !== null && pwcEnableLiteTOC !== undefined) {
                  pwcEnableLiteTOCWrapper = pwcEnableLiteTOC.closest(".coral-Form-fieldwrapper");
                  pwcEnableLiteTOC.prop('disabled', false);
                  pwcEnableLiteTOCWrapper.show();
               }
            } else {
               if (pwcEnableLiteTOC !== null && pwcEnableLiteTOC !== undefined) {
                  pwcEnableLiteTOCWrapper = pwcEnableLiteTOC.closest(".coral-Form-fieldwrapper");
                  pwcEnableLiteTOCWrapper.hide();
                  pwcEnableLiteTOC.prop('disabled', true);
               }
            }
         }

         function showHideSeeAlsoContentTypesDropdown(){
             pwcSeeAlsoContentTypesDropdown = $("[name='./jcr:content/metadata/pwc-seeAlso-contentType']");
             pwcSeeAlsoContentTypesDropdownWrapper = pwcSeeAlsoContentTypesDropdown.closest(".coral-Form-fieldwrapper");
             //logic to show hide content types dropdown.
             if (pwcSeeAlsoSection !== undefined && pwcSeeAlsoSection.find(':checked').val() === "yes" && pwcPublishingPointField !== null && pwcPublishingPointField.find(':checked').val() === "yes") {
                 if (pwcSeeAlsoContentTypesDropdown !== null && pwcSeeAlsoContentTypesDropdown !== undefined) {
                     pwcSeeAlsoContentTypesDropdown.prop('disabled', false);
                     pwcSeeAlsoContentTypesDropdown.find('button').prop('disabled', false);
                     pwcSeeAlsoContentTypesDropdownWrapper.show();
                 }
             } else {
                 if (pwcSeeAlsoContentTypesDropdown !== null && pwcSeeAlsoContentTypesDropdown !== undefined) {
                     pwcSeeAlsoContentTypesDropdownWrapper.hide();
                     pwcSeeAlsoContentTypesDropdown.find('button').prop('disabled', true);
                     pwcSeeAlsoContentTypesDropdown.prop('disabled', true);
                 }
             }
         }
         //Function to Show / Hide Enable See Also Section Max limit at Publishing Point
         function showHideSeeAlsoSectionLimit() {
            pwcSeeAlsoSectionMaxCount = $("input[name='./jcr:content/metadata/pwc-seeAlsoMaxDisplayCount']");
            //If publishingPoint is set to true show Enable See Also section
            if (pwcSeeAlsoSection !== undefined && pwcSeeAlsoSection.find(':checked').val() === "yes" && pwcPublishingPointField !== null && pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcSeeAlsoSectionMaxCount !== null && pwcSeeAlsoSectionMaxCount !== undefined) {
                  pwcSeeAlsoSectionMaxCountWrapper = pwcSeeAlsoSectionMaxCount.closest(".coral-Form-fieldwrapper");
                  pwcSeeAlsoSectionMaxCount.prop('disabled', false);
                  pwcSeeAlsoSectionMaxCountWrapper.show();
               }
            } else {
               if (pwcSeeAlsoSectionMaxCount !== null && pwcSeeAlsoSectionMaxCount !== undefined) {
                  pwcSeeAlsoSectionMaxCountWrapper = pwcSeeAlsoSectionMaxCount.closest(".coral-Form-fieldwrapper");
                  pwcSeeAlsoSectionMaxCountWrapper.hide();
                  pwcSeeAlsoSectionMaxCount.prop('disabled', true);
               }
            }
         }
         //Function to Show / Hide Enable auto set references button at Publishing Point
         function showHideAutoSetReferencesBtn() {
            pwcAutoSetReferencesBtn = $("#autoSetReferences");
            //If publishingPoint is set to true show Enable See Also section
            if (pwcSeeAlsoSection !== undefined && pwcSeeAlsoSection.find(':checked').val() === "yes" && pwcPublishingPointField !== null && pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcAutoSetReferencesBtn !== null && pwcAutoSetReferencesBtn !== undefined) {
                  pwcAutoSetReferencesBtnWrapper = pwcAutoSetReferencesBtn.closest(".schemaeditor-wrapper");
                  pwcAutoSetReferencesBtn.prop('disabled', false);
                  pwcAutoSetReferencesBtnWrapper.show();
               }
            } else {
               if (pwcAutoSetReferencesBtn !== null && pwcAutoSetReferencesBtn !== undefined) {
                  pwcAutoSetReferencesBtnWrapper = pwcAutoSetReferencesBtn.closest(".schemaeditor-wrapper");
                  pwcAutoSetReferencesBtnWrapper.hide();
                  pwcAutoSetReferencesBtn.prop('disabled', true);
               }
            }
         }
         //Function to Show / Hide auto set references Info at Publishing Point
         function showHideAutoSetReferencesBtnInfo() {
            pwcAutoSetReferencesInfo = $("#autoSetReferencesInfo");
            //If publishingPoint is set to true show Enable See Also section
            if (pwcSeeAlsoSection !== undefined && pwcSeeAlsoSection.find(':checked').val() === "yes" && pwcPublishingPointField !== null && pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcAutoSetReferencesInfo !== null && pwcAutoSetReferencesInfo !== undefined) {
                  pwcAutoSetReferencesInfoWrapper = pwcAutoSetReferencesInfo.closest(".schemaeditor-wrapper");
                  pwcAutoSetReferencesInfo.prop('disabled', false);
                  pwcAutoSetReferencesInfoWrapper.show();
               }
            } else {
               if (pwcAutoSetReferencesInfo !== null && pwcAutoSetReferencesInfo !== undefined) {
                  pwcAutoSetReferencesInfoWrapper = pwcAutoSetReferencesInfo.closest(".schemaeditor-wrapper");
                  pwcAutoSetReferencesInfoWrapper.hide();
                  pwcAutoSetReferencesInfo.prop('disabled', true);
               }
            }
         }
         //Function to Show / Hide Enable See Also Section at Publishing Point
         function showHideSeeAlsoSectionOnMap() {
            // pwcSeeAlsoSection = $("input[name='./jcr:content/metadata/pwc-seeAlsoSectionEnabled']");
            //If publishingPoint is set to true show Enable See Also section
            if (pwcPublishingPointField.find(':checked').val() === "yes") {
               if (pwcSeeAlsoSection !== null && pwcSeeAlsoSection !== undefined) {
                  pwcSeeAlsoSectionWrapper = pwcSeeAlsoSection.closest(".coral-Form-fieldwrapper");
                  pwcSeeAlsoSection.prop('disabled', false);
                  pwcSeeAlsoSectionWrapper.show();
                  showHideSeeAlsoContentTypesDropdown();
                  showHideSeeAlsoSectionLimit();
                  showHideAutoSetReferencesBtn();
                  showHideAutoSetReferencesBtnInfo();
               }
            } else {
               if (pwcSeeAlsoSection !== null && pwcSeeAlsoSection !== undefined) {
                  pwcSeeAlsoSectionWrapper = pwcSeeAlsoSection.closest(".coral-Form-fieldwrapper");
                  pwcSeeAlsoSectionWrapper.hide();
                  pwcSeeAlsoSection.prop('disabled', true);
                  showHideSeeAlsoContentTypesDropdown();
                  showHideSeeAlsoSectionLimit();
                  showHideAutoSetReferencesBtn();
                  showHideAutoSetReferencesBtnInfo();
               }
            }
         }

         //Function to Show / Hide See also section when override see also is selected at DITA Topic
         function showHideSeeAlsoSection() {
            pwcOverrideSeeAlsoSection = $("input[name='./jcr:content/metadata/pwc-usedIn']");
            if (pwcOverrideSeeAlsoField !== undefined && pwcOverrideSeeAlsoField.find(':checked').val() === "yes") {
               if (pwcOverrideSeeAlsoSection !== null && pwcOverrideSeeAlsoSection !== undefined) {
                  pwcOverrideSeeAlsoSectionWrapper = $("#seeAlso");
                  pwcOverrideSeeAlsoSection.prop('disabled', false);
                  pwcOverrideSeeAlsoSectionWrapper.show();
               }
            } else {
               pwcOverrideSeeAlsoSectionWrapper = $("#seeAlso");
               pwcOverrideSeeAlsoSectionWrapper.hide();
               pwcOverrideSeeAlsoSection.prop('disabled', true);
            }
         }

         if (pwcOverrideSeeAlsoField !== null && pwcOverrideSeeAlsoField !== undefined) {
            $(document).on("change", "[name='./jcr:content/metadata/pwc-overrideSeeAlso']", showHideSeeAlsoSection);
            showHideSeeAlsoSection();
         }
          //Function to display information message to click auto set reference button
         function displayAutoSetPromptMsg() {
             if (pwcSeeAlsoSection !== undefined && pwcSeeAlsoSection.find(':checked').val() === "yes") {
                _ui.alert(Granite.I18n.get("Alert"), Granite.I18n.get("Click Auto Set References button if its not done already or if there is any modification in the referenced links within the DITA topics. Auto Set References button must be clicked atleast once to set references links."), "alert");
            }

         }
          
         if (pwcSeeAlsoSection !== null && pwcSeeAlsoSection !== undefined) {
            $(document).on("change", "[name='./jcr:content/metadata/pwc-seeAlsoSectionEnabled']", showHideSeeAlsoSectionLimit);
            $(document).on("change", "[name='./jcr:content/metadata/pwc-seeAlsoSectionEnabled']", showHideSeeAlsoContentTypesDropdown);
            $(document).on("change", "[name='./jcr:content/metadata/pwc-seeAlsoSectionEnabled']", showHideAutoSetReferencesBtn);
            $(document).on("change", "[name='./jcr:content/metadata/pwc-seeAlsoSectionEnabled']", showHideAutoSetReferencesBtnInfo);
            $(document).on("change", "[name='./jcr:content/metadata/pwc-seeAlsoSectionEnabled']", displayAutoSetPromptMsg);
            showHideSeeAlsoContentTypesDropdown();
            showHideSeeAlsoSectionLimit();
            showHideAutoSetReferencesBtn();
            showHideAutoSetReferencesBtnInfo();
         }

         if (pwcPublishingPointField !== null && pwcPublishingPointField !== undefined) {
            $(document).on("change", "[name='./jcr:content/metadata/pwc:isPublishingPoint']", showHideSearchDocument);
            $(document).on("change", "[name='./jcr:content/metadata/pwc:isPublishingPoint']", showHideStaticTocField);
            $(document).on("change", "[name='./jcr:content/metadata/pwc:isPublishingPoint']", showHideRelatedContentField);
            $(document).on("change", "[name='./jcr:content/metadata/pwc:isPublishingPoint']", showHideJoinedSectionTOCFields);
            $(document).on("change", "[name='./jcr:content/metadata/pwc:isPublishingPoint']", showHideEnableLiteTOCField);
            $(document).on("change", "[name='./jcr:content/metadata/pwc:isPublishingPoint']", showHideSeeAlsoSectionOnMap);
            showHideSearchDocument();
            showHideStaticTocField();
            showHideRelatedContentField();
            showHideJoinedSectionTOCFields();
            showHideEnableLiteTOCField();
            showHideSeeAlsoSectionOnMap();
         }

      } catch (error) {}

   });

}(document, Granite.$));
