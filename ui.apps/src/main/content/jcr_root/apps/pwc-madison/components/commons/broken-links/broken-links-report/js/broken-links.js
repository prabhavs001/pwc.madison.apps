$(function () {
	var GENERATE_BUTTON = $(".dam-admin-reports-generate"),
		DOWNLOAD_BUTTON = $(".dam-admin-reports-download"),
		DOWNLOAD_BUTTON_JSON = $(".dam-admin-reports-json-download"),
		aemPages = {},
		inputType = $('[name="brokenLinksInputType"]'),
		getFormatJSON,
		formatRefRes,
		fetchAllJson,
		getTopicsAndBrokenPages,
		ajaxData,
		downloadReport;

	$('[name="folderPath"]').closest(".coral-Form-fieldwrapper").show();
	$('[data-granite-coral-multifield-name="topicLinks"]').closest(".coral-Form-fieldwrapper").hide();
	$(".broken-link-container").on("click", ".report-row", function (e) {
		var $elm = $(e.target);
		if (!$elm.hasClass("report-row")) {
			$elm = $elm.parents(".report-row");
		}
		$elm.toggleClass("expanded");
	});

	$(".broken-link-container").on("click", ".accrodion__tabs li", function (e) {
		var $elm = $(this),
			idx = $elm.index(),
			$accrodion = $elm.parents(".accrodion__tabs"),
			$menus = $accrodion.find("li"),
			$panel = $accrodion.nextAll().eq(1).find("li");
		$menus.removeClass("active");
		$elm.addClass("active");
		$panel.removeClass("show").eq(idx).addClass("show");
	});
	$(document).on('change', 'coral-select[name="brokenLinksInputType"]', function (e) {
		var inputType = $(this).val();
		if (inputType === "topics") {
			$('[name="folderPath"]').closest(".coral-Form-fieldwrapper").hide();
			$('[data-granite-coral-multifield-name="topicLinks"]').closest(".coral-Form-fieldwrapper").show();
		} else {
			$('[name="folderPath"]').closest(".coral-Form-fieldwrapper").show();
			$('[data-granite-coral-multifield-name="topicLinks"]').closest(".coral-Form-fieldwrapper").hide();
		}
	});

	Handlebars.registerHelper('assetLink', function (backRef) {
		var assetLink;
		if (backRef.endsWith(".dita")) {
			assetLink = "/libs/fmdita/clientlibs/xmleditor/page.html?src=" + backRef;
		} else if (backRef.endsWith(".ditamap")) {
			assetLink = "/libs/fmdita/ditamapeditor/core/editor.html?path=" + backRef;
		}
		return new Handlebars.SafeString(assetLink);
	});

	//Fetches the  lists in CSV format
	downloadReport = function (pathString, type, fmt) {
		if (fmt && fmt.length > 0) {
			window.location.href = "/bin/pwc/brokenlinks.json?path=" + pathString + "&inputType=" + type + "&fmt=" + fmt;
		} else {
			window.location.href = "/bin/pwc/brokenlinks.csv?path=" + pathString + "&inputType=" + type;
		}
	};

	//Fetches the topic lists and unpublished page references in the metadata
	getTopicsAndBrokenPages = function (pathString, type) {
		$.post("/bin/pwc/brokenlinks.json", {
			inputType: type,
			path: pathString
		}, null, 'json').then(function (aemPagesRes) {
			aemPages = aemPagesRes;

			fetchAllJson().then(function (topicList, formattedRefCallRes, aemPages) {
				$(".broken-links-wait").first().addClass("broken-links-wait-hide");
				var formattedJSON,
					brokenLinksTemplateElement,
					brokenLinksTemplate,
					brokenLinkHTML;

				formattedJSON = getFormatJSON(topicList, formattedRefCallRes, aemPages);
				if (formattedJSON.length > 0) {
					$(".no-content-message").hide();
					$(".broken-list-header").attr("hidden", false);
					brokenLinksTemplateElement = $("#broken-link-template").html();
					brokenLinksTemplate = Handlebars.compile(brokenLinksTemplateElement);
					brokenLinkHTML = brokenLinksTemplate(formattedJSON);
					$(".broken-link-container").append(brokenLinkHTML);
				}
			});
		});
	};

	//Formats the json containing backreferences and brokenlinks
	formatRefRes = function (resp) {
		var refJsonArray = [];
		resp.topics.forEach(function (topic, idx) {
			var refResJson = {},
				summary = topic.summary,
				usedIn = summary.usedIn,
				images = summary.images,
				brokenImages = [],
				backrefs = [],
				brokenRefs = [],
				xrefs = summary.xrefs,
				brokenLinks;
			refResJson.name = topic.path;
			usedIn.forEach(function (backRef, idx) {
				backrefs.push(backRef.path);
			});
			if(images !== undefined){
                images.forEach(function (image, idx) {
                    var link = {};
                    if (!image.linkStatus) {
                        link.path = image.path;
                        link.clz = 'text-red';
                        brokenImages.push(link);
                    } else {
                        link.path = image.path;
                        link.clz = 'text-blue';
                        brokenImages.push(link);
                    }
                });
			}
			xrefs.forEach(function (ref, idx) {
				var link = {};
				if (!ref.linkStatus) {
					link.path = ref.path;
					link.clz = 'text-red';
					brokenRefs.push(link);
				} else {
					link.path = ref.path;
					link.clz = 'text-blue';
					brokenRefs.push(link);
				}
			});
			brokenLinks = brokenRefs.concat(brokenImages);
			refResJson.backwardRefs = backrefs;
			refResJson.brokenLinks = brokenLinks;
			refJsonArray.push(refResJson);
		});
		return refJsonArray;
	};

	//Gets the backward references and broken links
	fetchAllJson = function () {
		var deferred = $.Deferred(),
			aemPagesParams = Object.keys(aemPages).join(","),
			refCallAjax = $.post("/bin/pwc/brokenlinks.json", {
				fmt: 'report',
				path: aemPagesParams
			}, null, 'json');
		$.when(refCallAjax)
			.done(function (refCallAjaxRes) {
				var topicList = Array.from(Object.keys(aemPages)),
					formattedRefCallRes = formatRefRes(refCallAjaxRes);
				deferred.resolve(topicList, formattedRefCallRes, aemPages);
			});
		return deferred.promise();

	};

	//Formats the final json
	getFormatJSON = function (topicList, formattedRefCallRes, aemPages) {
		var result = [];
		topicList.forEach(function (topic, idx) {
			var brokenLinksCount = 0,
				json = {};
			json.name = topic;

			json.brokenLinks = formattedRefCallRes.map(function (ref) {
				if (ref.name === topic) {
                    brokenLinksCount += ref.brokenLinks.filter(function (val) {
                        return val.clz === 'text-red';
                    }).length;
					return ref.brokenLinks;
				}
			}).filter(function (val) {
				return val !== undefined;
			})[0] || [];

			json.backwardRefs = formattedRefCallRes.map(function (ref) {
				if (ref.name === topic) {
					return ref.backwardRefs;
				}
			}).filter(function (val) {
				return val !== undefined;
			})[0] || [];
			json.aemPages = Object.keys(aemPages).map(function (key, index) {
				if (key === topic) {
                    var obj = aemPages[topic],parsedArr =[];
                    obj.forEach(function(val,idx){
                        var data = {};
                        if (!val.linkStatus) {
                            data.path = val.path;
                            data.clz = 'text-red';
                            parsedArr.push(data);
                        } else {
                            data.path = val.path;
                            data.clz = 'text-blue';
                            parsedArr.push(data);
                        }
                    });
                    brokenLinksCount += parsedArr.filter(function(val){
                        return val.clz === 'text-red';
                    }).length;
					return parsedArr;
				}
			}).filter(function (val) {
				return val !== undefined;
			})[0] || [];

			if (brokenLinksCount > 0) {
				json.brokenClass = "text-red";
			} else {
				json.brokenClass = "";
			}
			json.brokenLinksCount = brokenLinksCount;
			result.push(json);
		});


		return result;
	};


	GENERATE_BUTTON.click(function () {
		$(".broken-links-wait").first().removeClass("broken-links-wait-hide");
		$(".broken-link-container").empty();
		var inputTypeVal = inputType.val(),
			topicsPaths = $('[name="topicLinks"]'),
			folderPath = $('[name="folderPath"]').val();
		if (inputTypeVal === "topics" && topicsPaths.length > 0) {
			ajaxData = "";
			topicsPaths.each(function (index, element) {
				if (!ajaxData) {
					ajaxData += $(this).val();
				} else {
					ajaxData += ";" + $(this).val();
				}
			});
			getTopicsAndBrokenPages(ajaxData, inputTypeVal);
		} else if (inputTypeVal === 'folder' && folderPath) {
			getTopicsAndBrokenPages(folderPath, inputTypeVal);
		}
	});

	DOWNLOAD_BUTTON.click(function () {
		var inputTypeVal = inputType.val(),
			topicsPaths = $('[name="topicLinks"]'),
			folderPath = $('[name="folderPath"]').val();
		if (inputTypeVal === "topics" && topicsPaths.length > 0) {
			ajaxData = "";
			topicsPaths.each(function (index, element) {
				if (!ajaxData) {
					ajaxData += $(this).val();
				} else {
					ajaxData += ";" + $(this).val();
				}
			});
			downloadReport(ajaxData, inputTypeVal);
		} else {
			downloadReport(folderPath, inputTypeVal);
		}
	});

	DOWNLOAD_BUTTON_JSON.click(function () {
		var inputTypeVal = inputType.val(),
			topicsPaths = $('[name="topicLinks"]'),
			folderPath = $('[name="folderPath"]').val();
		if (inputTypeVal === "topics" && topicsPaths.length > 0) {
			ajaxData = "";
			topicsPaths.each(function (index, element) {
				if (!ajaxData) {
					ajaxData += $(this).val();
				} else {
					ajaxData += ";" + $(this).val();
				}
			});
			downloadReport(ajaxData, inputTypeVal, 'json');
		} else {
			downloadReport(folderPath, inputTypeVal, 'json');
		}
	});
});