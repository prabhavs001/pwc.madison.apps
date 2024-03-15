$(document).on("dialog-ready", function() {
    $(".cq-dialog-submit").click(function() {
        var columnControlComponent, elements, ui, sum = 0;
        columnControlComponent = $(this).closest('.coral-Form--vertical').find('.columnControlComponent');
        if(columnControlComponent.length) {
            ui = $(window).adaptTo("foundation-ui");
            elements = columnControlComponent.find('input[name="./column"]');

            elements.each(function(index, item){
                if(item.value) {
                  sum = sum + parseInt(item.value, 10);
                }
            });

            if(elements && elements.length && sum !== 12) {
                ui.alert("Sum of all fields should be 12");
                return false;
            }
		}
    });
});