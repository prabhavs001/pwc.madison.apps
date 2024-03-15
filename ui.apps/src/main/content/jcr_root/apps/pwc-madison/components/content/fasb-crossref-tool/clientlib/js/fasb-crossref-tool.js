//GLOBAL VARIABLES INITIALIZATION START
var compiledTemplate,
limitDialogVal = $('.result-div').attr('data-limit') ? $('.result-div').attr('data-limit') : 10,
		totalStandardResults,
		skipStandard = 0,
		remainingStandard,
		arrStandardItems = [],
		totalCodificationResults,
		skipCodification = 0,
		remainingCodification,
		isloading = false,
		loaderCheck = false,
		noResultCheck = true,
		arrCodificationItems = [],
		STANDARDTYPE_URL = "/bin/pwc-madison/crossref/typeservlet",
		STANDARDRESULT_URL="/bin/pwc-madison/crossref/searchresult";

//GLOBAL VARIABLES INITIALIZATION END

function sanitizeString(string) {
	var tempDiv = document.createElement('div');
	tempDiv.textContent = string;
	return tempDiv.innerHTML;
}

function escapeString(str){
	if(str.includes('.')){
		str=str.replace(/\./g,"~");
	}
	if(str.includes('/')){
		str=str.replace(/\//g,"$");
	}
	return str;
}

//METHOD TO POPULATE STANDARD NUMBER DROPDOWN
function populateStandardNumber(standardtype) {
	var populateStandardType, standardNumLabel, sortedStandNum, j, standardNumSelector = $('.standardNumber-select');
	$.ajax({
		url: STANDARDTYPE_URL+".standardnumber." + escapeString(standardtype) + ".json",
		success: function (resNum) {
			populateStandardType = resNum;
			standardNumLabel = $('.standardNumber-select').attr('data-select-label');
			sortedStandNum = populateStandardType.standardNumber.sort();
			$('.standardNumber-select').html('');
			$('.standardNumber-select').append('<option value="null">' + sanitizeString(standardNumLabel) + '</option>');
			$(function () {
				$('.standardNumber-select option').filter(function () {
					return ($(this).text() === sortedStandNum[0]);
				}).prop('selected', true);
			});
			for (j = 0; j < sortedStandNum.length; j++) {
				standardNumSelector.append(window.DOMPurify.sanitize('<option value="' +  sortedStandNum[j] + '">' + sortedStandNum[j] + '</option>'));
			}
		}
	});
}

//METHOD TO RESET CHEVRON TO ASC
function resetToAsc() {
	$('.table-header').each(function () {
		if ($(this).hasClass('sort-descending')) {
			$(this).removeClass('sort-descending');
			$(this).addClass('sort-ascending');
		}
	});
}

//METHOD TO SORT IN ASC ORDER
function sortOnAsc(property) {
	return function (a, b) {
		if (a[property] < b[property]) {
			return -1;
		} else if (a[property] > b[property]) {
			return 1;
		} else {
			return 0;
		}
	};
}

//METHOD TO SORT IN DESC ORDER
function sortOnDes(property) {
	return function (a, b) {
		if (a[property] < b[property]) {
			return 1;
		} else if (a[property] > b[property]) {
			return -1;
		} else {
			return 0;
		}
	};
}

//METHOD TO CHECK IF VALUES ARE VALID OR NOT
function isValidValue(data) {
	var isValid = false;
	if (data !== '' && data !== null && data !== "null" && data !== "NA") {
		isValid = true;
	}
	return isValid;
}

//METHOD TO POPULATE STANDARD TYPE DROPDOWN
function populateByStandard() {
	var standardTypeOptions, standardTypeLabel, sortedStandardTypes,i, standardTypeSelector = $('.standardType-select');
	$.ajax({
		url: STANDARDTYPE_URL+".standardtype.data.json",
		success: function (res) {
			standardTypeOptions = res;
			standardTypeLabel = $('.standardType-select').attr("data-option-standTypeLabel");
			sortedStandardTypes = standardTypeOptions.standardType.sort();
			$('.standardType-select').html('');
			$('.standardType-select').append('<option value="null">' + sanitizeString(standardTypeLabel) + '</option>');
			$(function () {
				$('.standardType-select option').filter(function () {
					return ($(this).text() === sortedStandardTypes[0]);
				}).prop('selected', true);
			});
			for (i = 0; i < sortedStandardTypes.length; i++) {
				standardTypeSelector.append(window.DOMPurify.sanitize('<option value="' +  sortedStandardTypes[i] + '">' + sortedStandardTypes[i] + '</option>'));
			}
			populateStandardNumber(sortedStandardTypes[0]);
		}
	});
}

//METHOD TO POPULATE BY-STANDARD RESULTS
function getByStandardResult(standardType, standardNum, skip, limit) {
	if (isValidValue(standardType) && isValidValue(standardNum)) {
		$('.loading-icon-style').show();
		$.ajax({
			url: STANDARDRESULT_URL+'.standardtype.' + standardType + "." + standardNum + "." + limit + "." + skip + ".json",
			async: false,
			success: function (res) {
				$('.loading-icon-style').hide();
				isloading = false;
				var Result = res;
				totalStandardResults = Result.totalCount;
				if (totalStandardResults === 0) {
					noResultCheck = false;
				} else {
					Result.searchResult.map(function (item) {
						arrStandardItems.push(item);
					});
					arrStandardItems.sort(function (a, b) {
						return a.paragraphLabel - b.paragraphLabel;
					});
					$('.populate-results').remove();
					$(compiledTemplate(arrStandardItems)).insertAfter('.search-list .result-div');
					$('.search-list').show();
				}
			},
			error: function (err) {
				$('.loading-icon-style').hide();
				$('.note').html('');
				$('.note').html(sanitizeString($('.note').attr('data-serverErrorMsg')));
				$(".must-enter-body").addClass("is-active");
				$('.search-list').hide();
			}
		});
	} else {
		$('.note').html('');
		$('.note').html(sanitizeString($('.note').attr('data-standardValidMsg')));
		$('.populate-results').remove();
		$('.search-list').hide();
		$(".must-enter-body").addClass("is-active");
	}
}


//METHOD TO POPULATE CODIFICATION RESULTS
function getByCodificationResult(topic, subtopic, section, paragraph, skip, limit) {

	isloading = true;

	if (isValidValue(topic) || isValidValue(subtopic) || isValidValue(section) || isValidValue(paragraph)) {
		$('.loading-icon-style').show();
		$.ajax({
			url: STANDARDRESULT_URL+".codification." + topic + "." + subtopic + "." + section + "." + paragraph + "." + limit + "." + skip + ".json",
			async: false,
			success: function (res) {
				$('.loading-icon-style').hide();
				isloading = false;
				var Result = res;
				totalCodificationResults = Result.totalCount;
				if (totalCodificationResults === 0) {
					noResultCheck = false;

				} else {
					Result.searchResult.map(function (item) {
						arrCodificationItems.push(item);
					});
					arrCodificationItems.sort(function (a, b) {
						return a.paragraphLabel - b.paragraphLabel;
					});
					$('.populate-results').remove();
					$(compiledTemplate(arrCodificationItems)).insertAfter('.search-list .result-div');
					$('.search-list').show();
				}
			},
			error: function (err) {
				$('.loading-icon-style').hide();
				$('.note').html('');
				$('.note').html(sanitizeString($('.note').attr('data-serverErrorMsg')));
				$(".must-enter-body").addClass("is-active");
				$('.search-list').hide();
			}
		});
	} else {
		$('.note').html('');
		$('.note').html(sanitizeString($('.note').attr('data-codificationvalidMsg')));
		$('.populate-results').remove();
		$('.search-list').hide();
		$(".must-enter-body").addClass("is-active");
	}

}

//METHOD TO GET VALUE OF FIELD BY SELECTOR NAME
function getValue(selector) {
	return escapeString(encodeURI($(selector).val()));
}


$(document).ready(function () {
	var standardTypeVal,
	resultTemplate = $('.result-template').html();


	if (resultTemplate) {
		compiledTemplate = Handlebars.compile(resultTemplate);
	}


	//	REMOVING MODAL
	$(".modal-close, .close-must-enter").click(function () {
		$(".modal").removeClass("is-active");
	});

	// POPULATE DROPDOWNS ON-LOAD
	if ($('.js-by-standard').hasClass('current')) {
		$('.fasb-search form .reset-btn').hide();
		populateByStandard();
		$('.standardType-select').change(function () {
			standardTypeVal = $(this).val();
			populateStandardNumber(standardTypeVal);
		});
	}

	//TOGGLE SORTING CHEVRON
	$('.listing .result-div span').click(function () {
		if ($(this).hasClass('sort-ascending')) {
			$(this).removeClass('sort-ascending');
			$(this).addClass('sort-descending');
		} else {
			$(this).removeClass('sort-descending');
			$(this).addClass('sort-ascending');
		}
	});

	//ON CLICK SORT FUNCTIONALITY
	$('.table-header').click(function () {
		if ($('.js-by-standard').hasClass('current')) {
			if ($(this).hasClass('sort-ascending')) {
				$('.populate-results').remove();
				arrStandardItems.sort(sortOnAsc($(this).attr('data-fieldName')));
				$(compiledTemplate(arrStandardItems)).insertAfter('.search-list .result-div');
				$('.search-list').show();
			} else {
				$('.populate-results').remove();
				arrStandardItems.sort(sortOnDes($(this).attr('data-fieldName')));
				$(compiledTemplate(arrStandardItems)).insertAfter('.search-list .result-div');
				$('.search-list').show();
			}
		} else {
			if ($(this).hasClass('sort-ascending')) {
				$('.populate-results').remove();
				arrCodificationItems.sort(sortOnAsc($(this).attr('data-fieldName')));
				$(compiledTemplate(arrCodificationItems)).insertAfter('.search-list .result-div');
				$('.search-list').show();
			} else {
				$('.populate-results').remove();
				arrCodificationItems.sort(sortOnDes($(this).attr('data-fieldName')));
				$(compiledTemplate(arrCodificationItems)).insertAfter('.search-list .result-div');
				$('.search-list').show();
			}
		}
	});
	// FASB Search
	$('.fasb-search .toggle a').click(function (e) {
		$('.loading-icon-style').hide();
		$('.no-result').hide();
		e.preventDefault();
		$('.fasb-search .toggle a').toggleClass('current');
		if ($('.js-by-codification').hasClass('current')) {
			$('.fasb-search form .reset-btn').click();
			$('.fasb-search form .reset-btn').show();
			$('.js-by-codification-form').show();
			$('.js-by-standard-form').hide();
		} else {
			$('.fasb-search form .reset-btn').hide();
			$('.js-by-codification-form').hide();
			$('.js-by-standard-form').show();
			populateByStandard();
		}
		$('.search-list').hide();
	});

	// SUBMIT CLICK FUNCTIONALITY
	$('.fasb-search form .btn.submit').click(function (e) {
		loaderCheck = true;
		if ($('.js-by-standard').hasClass('current')) {
			$('.populate-results').remove();
			$('.no-result').hide();
			skipStandard = 0;
			remainingStandard = 0;
			arrStandardItems = [];
			getByStandardResult(getValue('.standardType-select'), getValue('.standardNumber-select'), 0, limitDialogVal);
			if (totalStandardResults === 0) {
				noResultCheck = false;
				$('.no-result').show();
				$('.search-list').hide();
			}
		}

		if ($('.js-by-codification').hasClass('current')) {
			$('.populate-results').remove();
			$('.no-result').hide();
			skipCodification = 0;
			remainingCodification = 0;
			arrCodificationItems = [];
			getByCodificationResult(getValue('.code-topic') ? getValue('.code-topic') : 'NA', getValue('.code-subtopic') ? getValue('.code-subtopic') : 'NA', getValue('.code-section') ? getValue('.code-section') : 'NA', getValue('.code-paragraph') ? getValue('.code-paragraph') : 'NA', 0, limitDialogVal);
			if (totalCodificationResults === 0) {
				$('.no-result').show();
				$('.search-list').hide();
			}
		}
	});

	//CANCEL BUTTON CLICK FUNCTIONALITY    
	$('.fasb-search form .reset-btn').click(function () {
		$('.code-topic').val('');
		$('.code-subtopic').val('');
		$('.code-section').val('');
		$('code-paragraph').val('');
		$('.search-list').hide();
	});

	// LOAD MORE FUNCTIONALITY
	window.onscroll = function (ev) {
		if (($(window).scrollTop() > ($(document).height() - $(window).height() - 10)) && isloading === false) {
			if (loaderCheck === true && noResultCheck !== false && $(".populate-results").length >= limitDialogVal) {
				resetToAsc();
				if ($('.js-by-standard').hasClass('current')) {
					skipStandard = skipStandard + limitDialogVal;
					remainingStandard = totalStandardResults - skipStandard;
					if (remainingStandard >= limitDialogVal) {
						isloading = true;
						getByStandardResult(getValue('.standardType-select'), getValue('.standardNumber-select'), skipStandard, limitDialogVal);
					} else if (remainingStandard < limitDialogVal && remainingStandard > 0) {
						isloading = true;
						getByStandardResult(getValue('.standardType-select'), getValue('.standardNumber-select'), skipStandard, remainingStandard);
					} else {
						isloading = false;
					}
				}
				if ($('.js-by-codification').hasClass('current')) {
					skipCodification = skipCodification + limitDialogVal;
					remainingCodification = totalCodificationResults - skipCodification;
					if (remainingCodification >= limitDialogVal) {
						isloading = true;
						getByCodificationResult(getValue('.code-topic') ? getValue('.code-topic') : 'NA', getValue('.code-subtopic') ? getValue('.code-subtopic') : 'NA', getValue('.code-section') ? getValue('.code-section') : 'NA', getValue('.code-paragraph') ? getValue('.code-paragraph') : 'NA', skipCodification, limitDialogVal);
					} else if (remainingCodification < limitDialogVal && remainingCodification > 0) {
						isloading = true;
						getByCodificationResult(getValue('.code-topic') ? getValue('.code-topic') : 'NA', getValue('.code-subtopic') ? getValue('.code-subtopic') : 'NA', getValue('.code-section') ? getValue('.code-section') : 'NA', getValue('.code-paragraph') ? getValue('.code-paragraph') : 'NA', remainingCodification, limitDialogVal);
					} else {
						isloading = false;
					}
				}
			}
		}
	};


});