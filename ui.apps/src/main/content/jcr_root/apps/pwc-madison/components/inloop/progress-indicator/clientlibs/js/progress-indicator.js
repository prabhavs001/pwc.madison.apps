(function ($, $document) {
    "use strict";

    $document.off("dialog-ready").on("dialog-ready", function () {

        if ($('.cq-dialog-header').text().trim() === "Progress Indicator") {

            var idList, existingIdList, currentResourcePath, splitArray, i, options, id, count;
            currentResourcePath = $(Granite.author.ContentFrame.iframe).contents().find('.madison-core.progress-indicator > cq').attr('data-path');
            splitArray = currentResourcePath.split('/jcr:content');

            $.ajax({
                url: '/bin/sectionIdList',
                type: 'GET',
                cache: false,
                data: {
                    'path': splitArray[0]
                },
                async: false,
                success: function (data) {
                    idList = data.map.idList.myArrayList;
                    existingIdList = data.map.existingIdList.myArrayList;

                },
                error: function (request, error) {
                }
            });

            if (idList.length > 0) {
                count = 0;
                $(".coral-Form-fieldwrapper").each(function () {
                    if ($(this).find('label').text().trim() === 'Id *') {
                        options = $(this).closest('div').find('.coral3-Select');
                        $(this).find('coral-select-item').closest('coral-selectlist-item').remove();
						for (i = 0; i < idList.length; i++) {
                            $("<coral-select-item>").appendTo(options).val(idList[i]).html(idList[i]);

                        }
                        $(this).find('.coral3-Select coral-select-item').filter('[value="' + existingIdList[count] + '"]').attr('selected', true);
                        count++;
                    }
                });
            }

            $(document).off("click", ".coral3-Select").on("click", ".coral3-Select", function (e) {
                $(".coral-Form-fieldwrapper").each(function () {
                    if ($(this).find('label').text().trim() === 'Id *') {
                        if ($(this).find("coral-select-item").length === 0) {
                            options = $(this).closest('div').find("coral-select");
                            for (i = 0; i < idList.length; i++) {
                                $("<coral-select-item>").appendTo(options).val(idList[i]).html(idList[i]);
                            }
                        }
                    }
                });
                $(this).prop("placeholder", " ");
            });

        }

    });


    $(document).off("change", ".coral3-Select").on("change", ".coral3-Select", function (event) {
        var selectedSection, clickedSection, heading;
        if ($('.cq-dialog-header').text().trim() === "Progress Indicator") {
            selectedSection = $(this).find('coral-select-item:selected').text();
            clickedSection = $(Granite.author.ContentFrame.iframe).contents().find('#' + selectedSection);
            heading = $(this).closest("div").siblings('.coral-Form-fieldwrapper').find('input');
            if (heading.val() === "") {
                heading.val(clickedSection.find(':header').first().text());
            }
            $(heading).checkValidity();
            $(heading).updateErrorUI();
        }
    });

}($, $(document)));
