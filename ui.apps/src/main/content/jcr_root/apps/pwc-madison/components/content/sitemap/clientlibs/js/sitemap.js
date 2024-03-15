(function($, $document) {
    "use strict";

    var CONFIGURE_GROUPS = './groups',
        USE_COLUMNS = './useColumn',
        STACK_ON = './stackOn',
        COLUMNS,
        GROUP_LINK_AUTHORING_CLASS = '.group-link-authoring';

    COLUMNS = [{
        value: '',
        content: {
            textContent: ''
        }
    }, {
        value: 'Column 1',
        content: {
            textContent: 'Column 1'
        }
    }, {
        value: 'Column 2',
        content: {
            textContent: 'Column 2'
        }
    }, {
        value: 'Column 3',
        content: {
            textContent: 'Column 3'
        }
    }, {
        value: 'Column 4',
        content: {
            textContent: 'Column 4'
        }
    }, {
        value: 'Column 5',
        content: {
            textContent: 'Column 5'
        }
    }, {
        value: 'Column 6',
        content: {
            textContent: 'Column 6'
        }
    }];

    // function to get used columns and stack on data
    function getUsedColumnsAndStackOn(objects) {

        var _obj = {
                "used_columns": [],
                "used_stack_on": []
            },
            _columnKey = "useColumn",
            _stackOnKey = "stackOn",
            _deletedKey = "jcr:primaryType",
            o;

        delete objects[_deletedKey];

        for (o in objects) {
            if (o && o[_columnKey] !== "") {
                _obj.used_columns.push(objects[o][_columnKey]);
            }

            if (o && o[_stackOnKey] !== "") {
                _obj.used_stack_on.push(objects[o][_stackOnKey]);
            }
        }
        return _obj;
    }

    // function to populate column and stack on field
    function populateColumnsAndStackOnField(destination, stackonFields, usedColumnsData, stackOnData) {

        var useColumnsFields, used_columns;
        useColumnsFields = $('.use-column');
        used_columns = [];

        if (usedColumnsData && usedColumnsData.length > 0) {
            used_columns = usedColumnsData.slice(0);
        } else {
            useColumnsFields.each(function() {
                var item = $(this).find('coral-select-item:selected').text();
                if(item !== "") {
                  used_columns.push(item);
                }
            });
            used_columns.sort();
        }

        // for columns
        destination.each(function(index) {

            var currentElement = this;
            setTimeout(function(){
                var currentEl = currentElement,
                UCD = usedColumnsData,
                ind = index,
                use_Col = used_columns,
                selectedItem = $(currentEl).find('coral-select-item:selected').text() || (UCD && UCD[ind]),
				columns = $.extend(true, [], COLUMNS);

				currentEl.items.clear();
                columns.map(function(item) {

                // set previous selected item
                if ((item.value !== "") && (selectedItem === item.value)) {
                    item.selected = true;
                    currentEl.items.add(item);
                } else
                   if(!( currentEl && use_Col.indexOf(item.content.textContent) >= 0)) {
                       currentEl.items.add(item);
                }
            });
            }, 1000);
        });

        // for stackon
        stackonFields.each(function(index) {

            var current = this;
            setTimeout(function(){
				var currentElement = current,
				stackData = stackOnData,
				ind = index,
				closestUseColumnValue = $(currentElement).closest('.coral3-Multifield-item').find('.use-column').find('coral-select-item:selected').text(),
                selectedValue = $(currentElement).find('coral-select-item:selected').text() || (stackData && stackData[ind]),
                emptyObject = {
                    value: "",
                    content: {
                        textContent: ""
                    }
                },
                use_Col = used_columns;
                currentElement.items.clear();
                currentElement.items.add(emptyObject);
                use_Col.map(function(item) {
                        var object = {
                            value: item,
                            content: {
                                textContent: item
                            }
                        };
                        if (item && item === selectedValue) {
                            object.selected = true;
                        }
                        if (item && closestUseColumnValue !== item) {
                            currentElement.items.add(object);
                        }
                });
            },1000);
        });
    }

    function getConfigureGroupsData() {
        var useColumnSelectFields, stackOnFields,
            data;
        useColumnSelectFields = $('.use-column');
        stackOnFields = $('.stack-on');
        data = getUsedColumnsAndStackOn(window.groupJsonData);
        populateColumnsAndStackOnField(useColumnSelectFields, stackOnFields, data.used_columns, data.used_stack_on);
    }

    function init() {
        getConfigureGroupsData();
    }

    $document.on('dialog-ready', function(event) {

        var $groupLink = $(GROUP_LINK_AUTHORING_CLASS);
        if($groupLink.length){
            init();
        }
    });

    // custom validation
    $.validator.register({
        selector: 'coral-select[name*="' + USE_COLUMNS + '"], coral-select[name*="' + STACK_ON + '"]',
        validate: function(el) {

            var stackOnField, useColumnField;
            stackOnField = el.closest('coral-multifield-item-content').find('.stack-on');
            useColumnField = el.closest('coral-multifield-item-content').find('.use-column');
            if (el.attr('name').indexOf(USE_COLUMNS) > 0) {
                if (!el.val() && !stackOnField.val() && el.find('coral-select-item').length > 1) {
                       return "please select either useColumn or stack on field";
                }
            }
            else
                if (el.attr('name').indexOf(STACK_ON) > 0) {
                    if (!el.val() && !useColumnField.val() && useColumnField.find('coral-select-item').length <= 1) {
                        return "please select stack on field";
                    }
                }
        }
    });

    // event trigger when click on add button of multifield (CONFIGURE GROUPS)
   $(document).on('click', '[data-granite-coral-multifield-name*="' + CONFIGURE_GROUPS + '"] > button', function() {

        var id, useColumnField,
            stackOnField, element;

        useColumnField = $(this).closest('.coral3-Multifield').find(".use-column:last");
        stackOnField = $(this).closest('.coral3-Multifield').find(".stack-on:last");
        populateColumnsAndStackOnField(useColumnField, stackOnField);
    });


    // this event fire when we remove any multifield from configure groups
	$(document).on('coral-collection:remove', function(event) {

        if($(event.originalEvent.detail.item).find('.sitemap-link').length > 0 && $(event.target).hasClass('sitemap-group')){
			var useColumnFields, stackOnFields, currentMultifield,
            currentMultifieldId, addToGroupItem;
        useColumnFields = $('.use-column');
        stackOnFields = $('.stack-on');

        // populate columns and stack on fields when item is remove
        populateColumnsAndStackOnField(useColumnFields, stackOnFields);
        }

    });

    // call function when item change in stack on and usecolumn field
    $document.on("change", '.use-column, .stack-on' ,function(event) {

            var currentUseColumnField, currentUseColumnText,
                currentStackOnField, currentStackOnText,
                useColumnsFields, stackOnFields;

            currentUseColumnField = $(event.target).closest('coral-multifield-item-content').find('.use-column').get(0);
            currentUseColumnText = $(currentUseColumnField).find('coral-select-item:selected').text();
            currentStackOnField = $(event.target).closest('coral-multifield-item-content').find('.stack-on').get(0);
            currentStackOnText = $(currentStackOnField).find('coral-select-item:selected').text();

            useColumnsFields = $('.use-column');
            stackOnFields = $('.stack-on');

            if (currentStackOnText !== "") {
                // if stack on is selected then column pre-selected value is not selected
                $(currentUseColumnField).find('coral-select-item:selected').removeAttr("selected");
                populateColumnsAndStackOnField(useColumnsFields, stackOnFields);
            }

            if (currentUseColumnText !== "") {
                // if useColumn  is selected then column pre-selected value is not selected
                $(currentStackOnField).find('coral-select-item:selected').removeAttr("selected");
                populateColumnsAndStackOnField(useColumnsFields, stackOnFields);
            }
    });
}(jQuery, jQuery(document)));