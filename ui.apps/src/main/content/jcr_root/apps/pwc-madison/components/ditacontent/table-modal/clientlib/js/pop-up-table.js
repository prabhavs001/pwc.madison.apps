$(document).ready(function() {
	$('body').on('click','#table-modal-link', function() {
        $('.pwc-table-modal').addClass("is-active");
		var $srcTables = $(this).parent().find('.table-responsive');
		if ($srcTables !== 'undefined' || $srcTables !== null){
			$srcTables.each(function(){
				if($(this).parents("table")[0]===undefined){
					$("#table-mdl").html($(this).clone());
				}
			});
		}
	});
});