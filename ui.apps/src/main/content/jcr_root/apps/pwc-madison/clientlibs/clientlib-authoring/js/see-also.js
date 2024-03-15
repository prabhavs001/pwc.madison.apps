(function (document, $, URL) {
    $(document).on("foundation-contentloaded", function () {

        var contentTypeDropdown, contentTypes=[], pageUrl,topicPath,apiEndpoint,contentTypesParams,selectedValues,checkboxWrapper,pwcOverrideSeeAlsoField;

        contentTypeDropdown = $('.coral-Form-field[name="./jcr:content/metadata/pwc-seeAlso-topic-contentType"]');

        if($('#seeAlso').length>0) {
            $("button[type='submit']").click(function () {
                pwcOverrideSeeAlsoField = $("[name='./jcr:content/metadata/pwc-overrideSeeAlso']");
                if (pwcOverrideSeeAlsoField !== undefined && pwcOverrideSeeAlsoField.find(':checked').val() !== "yes") {
                    contentTypeDropdown.val([]);
                    $('input[name="./jcr:content/metadata/pwc-usedIn"]').prop('checked', false);
                }
            });
        }

        function updateSeeAlsoReferences() {
            pageUrl = new URL(window.location.href);
            topicPath = pageUrl.searchParams.get('item');
            contentTypes = contentTypeDropdown[0].selectedItems.map(obj=>obj.value);
            contentTypesParams = contentTypes.length>0 ? "&contentTypes=" + contentTypes.join("&contentTypes=") : "";
            apiEndpoint = '/bin/pwc-madison/getTopicReferences.json?item=' + topicPath + contentTypesParams;
            selectedValues = [];
            $('.coral3-Checkbox[name="./jcr:content/metadata/pwc-usedIn"] input:checked').each(function() {
                selectedValues.push($(this).val());
            });
            fetch(apiEndpoint)
                .then(response => response.json())
                .then(data => {
                    const containerElement = document.querySelector('.schemaeditor-wrapper[data-path="/conf/global/settings/dam/adminui-extension/metadataschema/pwc/items/tabs/items/tab5/items/coloum/items/usedIn-container/items/used-in"]');
                    containerElement.innerHTML = '';
                    data.forEach((item,index) => {
                        const checkboxElement = document.createElement('coral-checkbox');
                        checkboxElement.setAttribute('name', './jcr:content/metadata/pwc-usedIn');
                        checkboxElement.setAttribute('value', item.path);
                        checkboxElement.setAttribute('data-foundation-validation', '');
                        checkboxElement.setAttribute('data-validation', '');
                        checkboxElement.setAttribute('class', 'coral-Form-field coral3-Checkbox');
                        checkboxElement.setAttribute('aria-disabled', 'false');
                        checkboxElement.setAttribute('aria-invalid', 'false');

                        checkboxElement.innerHTML = `
        <input type="checkbox" handle="input" class="coral3-Checkbox-input" id="coral-id-${index}" value="${item.path}" name="./jcr:content/metadata/pwc-usedIn">
        <span class="coral3-Checkbox-checkmark" handle="checkbox"></span>
        <label class="coral3-Checkbox-description" handle="labelWrapper" for="coral-id-${index}">
          <coral-checkbox-label>
            <div class="see-also-row">
              <span class="see-also-column">${item.title}</span>
              <span class="see-also-column">${item.contentId}</span>
              <span class="see-also-column">${item.path}</span>
              <span class="see-also-column">${item.publicationDate}</span>
            </div>
          </coral-checkbox-label>
        </label>
        <input class="foundation-field-related" type="hidden" name="./jcr:content/metadata/pwc-usedIn@Delete">
      `;

                        containerElement.appendChild(checkboxElement);
                    });
                    checkboxWrapper = $('.schemaeditor-wrapper[data-path="/conf/global/settings/dam/adminui-extension/metadataschema/pwc/items/tabs/items/tab5/items/coloum/items/usedIn-container/items/used-in"]');
                    $(checkboxWrapper).find('.coral3-Checkbox').each(function(){
                        if(selectedValues.includes($(this).val())){
                            $(this).find('input').prop('checked',true);
                        } else {
                            $(this).find('input').prop('checked',false);
                        }
                    });
                })
                .catch(error => console.error('Error:', error));
        }

        if(contentTypeDropdown.length>0){
            $(document).on("change", "[name='./jcr:content/metadata/pwc-seeAlso-topic-contentType']", updateSeeAlsoReferences);
        }

    });

}(document, Granite.$, URL));
