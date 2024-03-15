/*global fmxml */
$(document).ready(setTimeout(function() {
	
	var getUrlParameter = function getUrlParameter(sParam) {
		var sPageURL = window.location.search.substring(1), sURLVariables = sPageURL.split('&'),
			sParameterName, i;

		for (i = 0; i < sURLVariables.length; i++) {
			sParameterName = sURLVariables[i].split('=');
			if (sParameterName[0] === sParam) {
				return typeof sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
			}
		}
		return false;
	};

	fmxml.commandHandler.subscribe({
        key: 'AUTOLINK_TOPIC',
        next: function() {
			window.open("/apps/pwc-madison/components/report/citation-report.html?payload="+getUrlParameter('src'));
        }

    });

}, 1000));