$(document).on("dialog-ready", function() {
    var featureContentTileComponent, checkboxElement;
    featureContentTileComponent = $('.featuredContentTile');

    if(featureContentTileComponent.length) {
            $(featureContentTileComponent).find('coral-select').on('change', function() {
	        checkboxElement = $(this).closest('coral-multifield-item-content').find('coral-checkbox[name="./openInNewWindow"]');
                if(this.value === 'regular') {  
                  $(checkboxElement).css('display', 'block');
                } else {
                  $(checkboxElement).css('display', 'none'); 
                }   
            });
	}

});
