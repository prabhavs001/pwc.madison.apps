$(document).ready(function () {
    if ($('.suggested-guidance') !== undefined) {
        $('.suggested-guidance').hide();
    }
    minCharSuggestionCount = 2;
    suggestedGuidanceTimeout = 400;
    regex = /["']/g;
    var CacheManager = (function () {


        return {

            Cs: [],

            options: {

                id: "_global"
            },


            init: function (ids) {
                var o = this;
                $.each(ids, function (i, val) {
                    o.setCache(val);
                });
            },

            getId: function (id) {
                return (typeof id != "undefined") ? id : this.options.id;
            },

            getById: function (cid) {
                var d = null;
                var o = this;

                for (var i = 0; i < o.Cs.length; i++) {
                    for (var q in o.Cs[i]) {
                        if (o.Cs[i][q] && o.Cs[i][q].c && o.Cs[i][q].c.id == cid) {
                            d = o.Cs[i][q];

                        }

                    }

                }
                return d;
            },

            getData: function (id, query) {
                var o = this;
                id = o.getId(id);
                return (this.check(id, query)) ? o.Cs[id][query] : null;
            },

            setCache: function (id) {
                var o = this;
                id = o.getId(id);
                o.Cs[id] = {};

            },

            add: function (id, query, ops) {

                var o = this;
                if (o.Cs.length && o.Cs[id]) {
                    o.Cs[id][query] = ops;
                    return true;
                }

                return false;

            },

            clear: function (id, query) {
                var o = this;
                if (o.Cs[id] && o.Cs[id][query]) {
                    o.Cs[id][query] = {};
                }
            },

            clearAll: function (id) {
                var o = this;
                if (o.Cs.length && o.Cs[id]) {
                    o.Cs[id] = {};
                }
            },

            check: function (id, query) {
                var o = this;
                return (id && o.Cs[id] && o.Cs[id][query] && typeof o.Cs[id][query] !== "undefined");
            }
        }
    });

    function Generator() { };

    Generator.prototype.rand = Math.floor(Math.random() * 26) + Date.now();

    Generator.prototype.getId = function () {
        return this.rand++;
    };
    idGen = new Generator();

    // get window width
    function windowWidth() {
      var winWidth = window.innerWidth
      || document.documentElement.clientWidth
      || document.body.clientWidth;

      return winWidth;
    }

    $('.auto-suggestion input').on('focus', function () {
    	var value = $(this).val() !== undefined ? $(this).val() : "";
            if ($("#searchform").attr('search-guidance') != 'disabled' && $('.recent-search-results ul').length > 0 && $('.recent-search-results ul').text() !== '' && $.trim(value).length === 0) {
                $('.recent-search-results').show();
            }
    });
    
    var inp = $(".auto-suggestion input"),
    clear = $(".clear-input");

    $(inp).on("input", function(){
        $(clear).toggle(!!this.value);
    });
    
    $(clear).on("touchstart click", function(e) {
        e.preventDefault();
        $(inp).val("").trigger("input");
        $(inp).val("").focus();
    });

    if ($('#maxSuggestionCount') !== undefined) {
        maxSuggestionCount = $('#maxSuggestionCount').val();
        maxSuggestionCount = parseInt(maxSuggestionCount, 10);
    }
    if ($('#minCharSuggestionCount') !== undefined) {
        minCharSuggestionCount = $('#minCharSuggestionCount').val();
        minCharSuggestionCount = parseInt(minCharSuggestionCount, 10);
    }
    if ($('#maxSuggestedGuidance') !== undefined) {
    	maxSuggestedGuidanceCount = $('#maxSuggestedGuidance').val();
        maxSuggestedGuidanceCount = parseInt(maxSuggestedGuidanceCount, 10);
    }
    if ($('#suggestedGuidanceTimeout') !== undefined) {
        suggestedGuidanceTimeout = $('#suggestedGuidanceTimeout').val();
        suggestedGuidanceTimeout = parseInt(suggestedGuidanceTimeout, 10);
    }
    if ($('.recent-search-results') !== undefined) {
        $('.recent-search-results').hide();
    }
    if ($('input[name="pageLocale"]') !== undefined) {
        pageLocale = $('input[name="pageLocale"]').val();
    }
    searchDocumentPlaceHolderText = 'Search within document...';
    searchPlaceHolderText = Granite.I18n.get("search_type_your_search") + "...";
    if ($('#searchPlaceHolder') !== undefined && $('#searchPlaceHolder').val() != 'Search_Within_Document') {
    	searchDocumentPlaceHolderText = $('#searchPlaceHolder').val();
    }
    placeHolderText = searchPlaceHolderText;
    
    function clearInput(){
    	if ($("input[name='q']") !== undefined) {
            $("input[name='q']").val("");
        }
    }
    
    function getUserProfile() {
        if ($.userInfo) {
            var userSearchParams = {};
            try {
                if ($.isEmptyObject(window.userSearchParams)) {
                    /*set collection*/
                    var userSearchParamsData = $.userInfo.getUserLocale();
                    if (userSearchParamsData.locale && userSearchParamsData.countryList) {
                        userSearchParams.colParamValue = userSearchParamsData.countryList.join('|');
                        userSearchParams.colParam = userSearchParamsData.locale.col;
                    }

                    if (userSearchParamsData.audience) {
                        userSearchParams.audParamValue = userSearchParamsData.audience.value;
                        userSearchParams.audParam = userSearchParamsData.audience.param;
                    }
                    window.userSearchParams = userSearchParams;
                }
            } catch (error) {

            }
        }
    }
    
    function parseEncodedValue(value){
    	try {
        	value = unescape(value);
		} catch (err) {
		}
    	return value;
    }

    if ($('.navbar-search #search-link').length) {
        if(window.matchMedia("(max-width: 768px)").matches){
            // mobile
        var modal = $('.navbar-search #search-link').animatedModal({
            color: 'rgba(256, 256, 256, 1)',
            beforeOpen: function () {
                placeHolderText = searchPlaceHolderText;
                getRecentSearches();
                initializeModalSearchBar();
                populateSearchKeyword();
                $('.auto-suggestion input').focus();
                if($('body').hasClass('content_page')){
                	 clearInput();
                    
                }
            },
           
        });
    }
    else{
        getRecentSearches();
        initializeModalSearchBar();
        populateSearchKeyword();
        $('.auto-suggestion input').focus();
        if($('body').hasClass('content_page')){
             clearInput();
        }
    }
        
        cache = new CacheManager();
        cache.init([1]);
        completeTimer = null;
        $('#autocompleteSearch input').on('paste keyup', function (e) {
            if(e.type === 'paste' && e.originalEvent.clipboardData){
                 var keyword =  $.trim(e.originalEvent.clipboardData.getData("text/plain"));
            }
            else{
                var keyword = $.trim($(this).val());
            }
            var content = $(this).closest('.global-content');
            if (completeTimer) {
                clearTimeout(completeTimer);
            }
            completeTimer = setTimeout(function () {
                if (keyword.length >= minCharSuggestionCount) {
                    getSuggestedTerms(keyword, content);
                    content.find('.recent-search-results').hide();
                    content.find('.search-results').show();
                    content.find('.submit-btm').css('display','flex')
                } else if (keyword.length === 0) {
                    if ($("#searchform").attr('search-guidance') != 'disabled' && $('.recent-search-results ul').length > 0 && $('.recent-search-results ul').text() !== '') {
                        content.find('.recent-search-results').show();
                    }
                    content.find('.search-results').hide();
                } else {
                    content.find('.recent-search-results').hide();
                    content.find('.search-results').hide();
                }
            }, suggestedGuidanceTimeout);
        });
    }

    $("body").on("click", ".close-global-search", function (e) {
        if(window.matchMedia("(max-width: 768px)").matches){
            $('#autocompleteSearch').removeClass('selected').addClass('dismiss');
            $('html').css('overflow','');
            $('body').css('overflow','');
        }
        else{
            $('#autocompleteSearch').hide();
        }
      });

    function initializeModalSearchBar() {
        $('.recent-search-results').hide();
        $('.suggestions ul').empty();
        $('.suggestions').hide();
        $('.guidance ul').empty();
        $('.guidance').hide();
        $('.submit-btm').hide();
    }
    
    function urlParams(name){
        var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
        if (results === null){
           return null;
        }
        else {
           var queryValue = results[1] ? results[1].replace('/\+/g') : '';
           return parseEncodedValue(queryValue) || 0;
        }
    }

    /* Prepopulate search keyword on search input field */
    function populateSearchKeyword(){
    	var keyword;
    	try {
    		if($(".search-results .header h2") && $(".search-results .header h2").length && $(".search-results .header h2").text() && $(".search-results .header h2").text() !=''){
        		keyword = $(".search-results .header h2").text();
        	}
    		//Fallback
    		else if(URLSearchParams != undefined){
				var params = new URLSearchParams(window.location.href);
				if(params && params.has("q")){
					keyword = params.get("q");
					
				}
			}
		} catch (err) {
			//IE Support
			keyword = urlParams('q');
		}
        if(keyword !== null){
            $('.auto-suggestion').find('input[name="q"]').val(keyword);
            $(clear).show();
        }
    }
    
    $('.auto-suggestion input').on('blur', function () {
        $(this).attr('placeholder', placeHolderText);
    });

    /**
     * Append terms for suggestions
     * @param {*} data 
     * @param {*} keyword 
     * @param {*} content 
     */
    function appendSuggestedTerms(data, keyword, content) {
        $('.suggestions').hide();
        $('.guidance').hide();
        var suggestedData;
        var suggestedTermsArray = [];
        var suggestedGuidanceArray = [];
        if(data.response.docs != null) {
			suggestedData = data.response.docs;
			if (suggestedData && suggestedData.length > 0) {
            	cache.add(1, keyword, data);
				$(suggestedData).each(function(i, val) {
                    if(val.type==='Suggested_Term') {
                        suggestedTermsArray.push(val.guidanceTerms);
                    }
                    if(val.type==='Guidance') {
                        suggestedGuidanceArray.push(val);
                    }
                });
            }
            if(data.expanded.Suggested_Term != null) {
                suggestedData = data.expanded.Suggested_Term.docs;
                if (suggestedData && suggestedData.length > 0) {
                    $(suggestedData).each(function(i, val) {
                        if(val.type==='Suggested_Term') {
                            suggestedTermsArray.push(val.guidanceTerms);
                        }
                	});
                }
            }
            if ($('.suggestions ul').length > 0) {
            	$('.suggestions ul').html(renderArray(suggestedTermsArray, keyword));
                if ($('.suggestions ul').text() !== '') {
                	$('.suggestions').show();
                }
            }
            let list = content.find('.results ul');
            list.mark(keyword.replace(regex, ''));
        }
        if (suggestedTermsArray.length === 0) {
        	$('.suggestions').hide();
        }
        if (suggestedGuidanceArray.length > 0) {
        	appendGuidanceData(data, suggestedGuidanceArray, keyword, content);
        }
    }

    /**
     * Append html for guidance terms
     * @param {*} data 
     * @param {*} keyword 
     * @param {*} content
     */
    function appendGuidanceData(data, suggestedGuidanceArray, keyword, content) {
        if(data.expanded != null && data.expanded.Guidance != null && data.expanded.Guidance.docs != null) {
            var data = data.expanded.Guidance.docs;
            var html = "";
            if (data && data.length > 0) {
                if ($('.guidance ul').length > 0) {
                    $(data).each(function(i, val) {
                        suggestedGuidanceArray.push(val);
                    });
                }
            }
        }
        var suggestedGuidanceArrayLength = suggestedGuidanceArray.length;
		$(suggestedGuidanceArray).each(function(i, val) {
			if (i == maxSuggestedGuidanceCount) {
            	return false;
            }
            let v = "<li class='thin-row'><a href='" + val.url + "'" + ">" + val.guidanceTitle + "</a></li>";
            suggestedGuidanceArrayLength > 1 ? html += v : html = v;
        });
        if ($('.guidance ul').length > 0) {
        	$('.guidance ul').html(window.DOMPurify.sanitize(html));
        }
        $('.guidance').show();
        let list = content.find('.results ul');
        list.mark(keyword.replace(regex, ''));
    }

    function getUserProfileData(data) {
        if (typeof window.userSearchParams != 'undefined') {
            if (typeof window.userSearchParams.colParam != 'undefined' && typeof window.userSearchParams.colParamValue != 'undefined') {
                data[window.userSearchParams.colParam] = window.userSearchParams.colParamValue;
            }
            if (typeof window.userSearchParams.audParam != 'undefined' && typeof window.userSearchParams.audParamValue != 'undefined') {
                data[window.userSearchParams.audParam] = window.userSearchParams.audParamValue;
            }
        }
    }

    /**
     * Get suggestions data
     * @param {*} keyword 
     * @param {*} content 
     */
    function getSuggestedTerms(keyword, content) {
        if($("#searchform").attr('search-guidance') != 'disabled'){
            if (cache.check(1, keyword)) {
                let data = cache.getData(1, keyword);
                appendSuggestedTerms(data, keyword, content);
            } else {
	            getUserProfile();
                var data = {}, expand = 'expand.rows';
                data.q = keyword;
                data.locale = pageLocale;
                data[expand] = maxSuggestedGuidanceCount > maxSuggestionCount ? maxSuggestedGuidanceCount-1 : maxSuggestionCount-1;
                getUserProfileData(data);
                data._cookie= "false";
                $.ajax({
                    url: '/bin/pwc-madison/vp-typeahead',
                    type: 'GET',
                    data: data,
                    contentType : 'application/json; charset=utf-8',
                    success: function (data) {
                        if(data.expanded != null) {
                        	appendSuggestedTerms(data, keyword, content);
                        }
                    },
                    error: function (request, error) { }
                });
            }
        }
    }

    /**
     * Get recent searchs
     */
    function getRecentSearches() {
        $('.recent-search-results').hide();
        $('.recent-search-results ul').empty();
        let recentSearchCookie = $.cookie('vsrecentsearches');
        if(recentSearchCookie){
             let data = recentSearchCookie.split('~');
             if (data && data.length > 0 && $('.recent-search-results ul').length > 0) {
                        $('.recent-search-results ul').html(renderRecentSearches(data));
                    }
        }
    }

    /**
     * Render recent search data
     * @param {*} data 
     */
    function renderRecentSearches(data) {
        var html = "";
        if (data && data.length > 0) {
            $(data).each(function (i, val) {
                let vsearchparam = window.UserRegistration.sanitizeString(val).replace("'","%27"), v = "<a href='" + $('#searchform').attr('action') + "?q=" + vsearchparam + "'" + ">" + window.UserRegistration.sanitizeString(decodeURIComponent(val)) + "</a>";
                html += "<li>" + v + "</li>";
            });
        }
        return html;
    }

    /**
     * Render autocomplete array as html
     * @param {*} data 
     * @param {*} query 
     */
    function renderArray(suggestedTermsArray, query) {
        var html = "";
        if (suggestedTermsArray && suggestedTermsArray.length > 0) {
            $(suggestedTermsArray).each(function (i, val) {
                if(i == maxSuggestionCount) {
                    return false;
                }
				let v = "<a href='" + $('#searchform').attr('action') + "?q=" + val + "&s=st'" + ">" + val + "</a>";
                html += "<li>" + v + "</li>";
            });
        }
        return html;
    }

    /**
     * Check if input string is
     * encoded
     * @param {*} str 
     */
    function checkEncodeURI(str) {
    	return /\%[0-9a-f]{2}/i.test(str)
    }

    $('#searchform').submit(function (e) {
        if ($(".global-search input[name='q']") && ($(".global-search input[name='q']").val() == '' || $.trim($(".global-search input[name='q']").val()).length < minCharSuggestionCount)) {
            e.preventDefault();
        }
    });

    // -------------------------------------------------------------------------------------
    // ------------------------------ Search Within A Doc v2 -------------------------------
    // -------------------------------------------------------------------------------------

    var chapterToc = $("#is-chapter-toc").val();

    if (chapterToc === 'false') {

        document.addEventListener("TOCLoaded", function() {
            if($('.searchdoc-v2').length > 0) {
                searchDoc.init();
                $(".searchdoc-loader").remove();
            }
        }, self);

    }

    document.addEventListener("TOCOpened", function() {
        if(searchDoc !== undefined){
            searchDoc.closeSearchDoc();
        }
    }, self);

    var searchDoc = (function() {
        var initialize = function() {
            _attachToc();

            $('.searchdoc-v2').addClass('is-hidden');
            $('.search-doc-link').removeClass('is-initializing');
        }

        // -------------- Clone TOC markup into document search ----------------------
        var _attachToc = function() {
            let clone = document.querySelector('.toc-content').cloneNode(true);
            clone.setAttribute( 'id', 'toc-clone');
            document.querySelector('.searchdoc-body').appendChild(clone);
            $('.searchdoc-body .toc-content')
                .css('display', '')
                .removeClass('is-hidden-mobile is-hidden-tablet-only toc-content')
                .addClass('searchdocv2-content');

            $('#toc-clone a.expand-link').each(function(i) {
                $(this).attr('data-search-clone-index', i);
            });

            _attachDesktopToc();
            _attachHandlers();

            //open active search items
            $('.searchdoc-v2 .expand-link.active').each(function() {
                $(this).parents("div.toc-item").each(function() {
                    var image = $(this).find("> button");
                    var sibs = $(image).siblings("div.toc-item");
                    $(image)
                        .children('img')
                        .attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/collapse.svg");
                    sibs.css("display", "flex");
                });
            })
        }

        var _attachDesktopToc = function() {
            let clone = document.querySelector('.searchdoc-v2').cloneNode(true);
            clone.setAttribute( 'id', 'toc-clone');
            document.querySelector('.share_icons').appendChild( clone );
            $('.share_icons .searchdoc-v2')
                .removeClass('searchdoc-v2--mobile')
                .addClass('searchdoc-v2--desktop');
        }

        function _toggleTocItem(el) {
            for(i=0; i <= el.length; i++) {
                var isOpen = $(el[i]).children('img').attr("src") === '/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg' ? true : false;
                var panel = $(el[i]).siblings("div.toc-item");

                if(isOpen === false) {
                    panel.addClass('is-open');
                    $(el[i])
                        .children('img')
                        .attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg");
                } else {
                    panel.removeClass('is-open');
                    $(el[i])
                        .children('img')
                        .attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/collapse.svg");
                }

                panel.slideToggle("fast");
                panel.css("display", "flex");
            }
        }

        // -------------- attach handlers ----------------------
        var _attachHandlers = function() {
            $('.search-doc-link').on('click touchend', function(e) {
                e.preventDefault();
                e.stopPropagation();

                // To close Subject-Matter-Expert
                $('.subject-matter-div').addClass('subject-matter-hide');
                
                if ($('.toc-content').css('display') === 'block') {
                    toggleToc();
                }

                if ($('.search-doc-link').hasClass('is-initializing') !== true) {
                    if ($('.search-doc-link').hasClass('active')) {
                        _closeSearchDoc();
                    } else {
                        _openSearchDoc();
                    }
                }
            });

            //add/remove focus to ensure correct autocomplete behavior
            $('.searchdoc-body').on('scroll', function(e) {
                if ($(this).find("[type='text']:focus").length > 0) {
                    e.preventDefault();
                    e.stopPropagation();

                    var focusedEl = $(this).find(":focus");
                    focusedEl.blur();
                }
            });

            $('.searchdoc-header .icon-close-popup').on('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                _closeSearchDoc();
            })


            $('.searchdoc-v2').on('click', '.toc-item a', function(e) {
                e.preventDefault();
                e.stopPropagation();


                if ($(this).parents('.searchdoc-v2--mobile').length > 0) {

                    if (chapterToc === 'false') {
                        _itemActivate($('.searchdoc-v2--desktop').find('*[data-search-clone-index="' + $(this).attr('data-search-clone-index') + '"]')[0]);
                    } else {
                        _itemActivate(e.currentTarget);
                    }


                } else if ($(this).parents('.searchdoc-v2--desktop').length > 0) {
                    if (chapterToc === 'false') {
                        _itemActivate($('.searchdoc-v2--mobile').find('*[data-search-clone-index="' + $(this).attr('data-search-clone-index') + '"]')[0]);
                    } else {
                        _itemActivate(e.currentTarget);
                    }

                }

                if ($(e.target).parent('.toc-item').hasClass('show-searchdoc') === false) {
                    _itemActivate(this);
                }
            });

            $('.searchdoc-v2').on('click', '.toc-item button', function(e) {

                if (chapterToc === 'true' && window.TOC !== undefined) {
                    try {
                        $this = $(this);
                        var klass = $(this).parents(".parent-item").attr('class');
                        var arr = klass.split(/\s+/);
                        var currentElem = '.' + arr.join('.');

                        window.TOC.onClickFunction(e);
                        setTimeout(function() {
                            if ($this.parents('.searchdoc-v2--mobile').length > 0) {
                                var target = $('.searchdoc-v2--desktop');
                                var currentbutton = $('.searchdoc-v2--mobile').find(currentElem);

                            } else if ($this.parents('.searchdoc-v2--desktop').length > 0) {
                                var target = $('.searchdoc-v2--mobile');
                                var currentbutton = $('.searchdoc-v2--desktop').find(currentElem);
                            } else {

                            }
                            var currentNode = currentbutton.clone();
                            target.find(currentElem).replaceWith(currentNode);
                        }, 1000);



                    } catch (err) {
                        console.log("Error: " + err);
                    }
                }

                if ($(e.target).parent('.toc-item').hasClass('show-searchdoc') === true) {
                    e.preventDefault();
                    e.stopPropagation();

                    $('#searchdocv2-form.showForm').removeClass('showForm');

                    setTimeout(function() {
                        $('#searchdocv2-form.showingForm').removeClass('showingForm');
                    }, 255)
                    $('.show-searchdoc').removeClass('show-searchdoc');
                    $('.searchbox-clone').remove();
                }


                if (chapterToc === 'false') {
                    var cloneIndex = parseInt($(this).siblings('*[data-search-clone-index]').attr('data-search-clone-index'));
                    _toggleTocItem($('*[data-search-clone-index="' + cloneIndex + '"]').siblings('button'));
                }

            });


            $('.searchdoc-v2').on('submit', 'form', function(e) {
                if ($('[type="text"]', this).val().length < minCharSuggestionCount) {
                    e.preventDefault();
                    e.stopPropagation();
                }
            });

        $('.searchdoc-v2').on('click', '.searchsubmit', function(el) {
          var searchDocLevel;
          if($(el.target).parents('.searchdoc-body').length === 0){
              searchDocLevel = _fetchLevelFromSearchDoc();
              $(el.target).siblings('input[name="docLevel"]').val(searchDocLevel);
          }else{
            var pagePath = $(el.target).parent().prev().find('a').attr('href'),
            tocEntryNodePath = $(el.target).parent().prev().find('a').data('target');
            searchDocLevel = _fetchLevelFromSearchDoc(tocEntryNodePath);
            $(el.target).siblings('input[name="docLevel"]').val(searchDocLevel);

            $.get('/bin/pwc/getsubsection',
            {path: pagePath},
            null, 'text').then(function(response){
              var _searchDocForm = $(el.target).closest('.searchdoc-v2 #searchdocv2-form-clone');
              $(el.target).siblings('input[name="SubSection"]').val(response);
              _searchDocForm.submit();
              
            });
          }
      });

            $('.searchdoc-all').on('click touchend', function(e) {
                e.preventDefault();
                e.stopPropagation();

                _handleShowDocSearch()
            });

            $(window).on('resize', function() {
                fixedSearchPosAdjust();
            });

            //close on outside click
            $(document).on('click', function(e) {
                if($(e.target).parents('.searchdoc-v2').length === 0 && $('.searchdoc-v2.is-hidden').length === 0 && windowWidth() >= 1088) {
                    e.preventDefault();
                    e.stopPropagation();
                    $('.searchdoc-v2').addClass('is-hidden');
                    $('.search-doc-link').removeClass('active');
                    _closeSearchDoc();
                }
            });

        }

        // ------------- adjust fixed search position ---------------
        var fixedSearchPosAdjust = function() {
            if(windowWidth() >= 1088) {
                if($('.navbar-toc.sticky-toc').length > 0) {
                    var searchDocEl = $('.searchdoc-v2.searchdoc-v2--scroll');
                    var desktopBtn = $('.toc-action-navbar .share_icons');
                    var rightVal = desktopBtn.outerWidth() + parseInt($('.navbar-toc.sticky-toc .toc-action-navbar').css('margin-right'), 10) - 56;
                    searchDocEl.css('right', rightVal);
                } else {
                    $('.searchdoc-v2.searchdoc-v2--scroll').css('right', '');
                }
            } else {
                $('.searchdoc-v2.searchdoc-v2--mobile').removeClass('searchdoc-v2--scroll');
            }
        }

        // ------------ document global search handler --------------
        var _handleShowDocSearch = function() {
            $('.show-searchdoc').removeClass('show-searchdoc');
            $('.searchbox-clone').remove();
            $('.searchdoc-v2 #searchdocv2-form').addClass('showingForm');
            $('.searchdoc-v2  .expand-link').removeClass('active');
            var pubPointDocContext = $('#pubpointdoccontext').val();
            var pubPointDocSearchTitle = $('.searchdoc-all').html();
            var _searchDocForm = $('.searchdoc-v2 #searchdocv2-form');
            _searchDocForm.each(function(){
                $(this).find('input[name="pwcDocContext"]').val(pubPointDocContext);
                $(this).find('input[name="docSearchTitle"]').val(pubPointDocSearchTitle);
            });
            setTimeout(function() {
                $('.searchdoc-v2 #searchdocv2-form').addClass('showForm');
            }, 5);
        }

      //---------------Fetch Level From Search WIth In Doc TOC---------------
      var _fetchLevelFromSearchDoc = function(tocEntryNodePath){
        var tocEntry = "/jcr:content/toc/",hyphenSelector="-",level='',
         entryNodesList=[],entry="entry",entryRegex=/entry\d+$/,
         forwardSlashSelector="/";
         if(tocEntryNodePath !== null && tocEntryNodePath !== undefined && tocEntryNodePath.indexOf(tocEntry) > -1){
            entryNodesList = tocEntryNodePath.split(tocEntry)[1].split(forwardSlashSelector);
            if(entryNodesList.length > 0){
              entryNodesList.forEach(function(node,index){
                  if(node.match(entryRegex)){
                      var digit = node.replace(entry,'');
                      digit = parseInt(digit) + 2;
                      level+= (index < entryNodesList.length - 1) ? (digit + hyphenSelector) : digit;
                  }
                  else{
                    level+= (index < entryNodesList.length - 1) ? (1 + hyphenSelector) : 1;
                  }
              })

            }
         }
         else{
          level=0;
         }
         return level;
      }

      // -------------- activate searchdoc item ----------------------
      var _itemActivate = function(el) {
        var parentEl, pwcDocContext, docSearchTitle;
        pwcDocContext = el.getAttribute('data-pwcdoccontext');
        docSearchTitle = el.getAttribute('data-title');
        parentEl = $(el).parents('.searchdoc-v2');

            //remove existing search nodes
            if($('#searchdocv2-form.showForm', parentEl).length > 0) {
                $('#searchdocv2-form.showForm', parentEl).removeClass('showForm');

                setTimeout(function() {
                    $('#searchdocv2-form.showingForm', parentEl).removeClass('showingForm');
                }, 255)
            }

            $('.expand-link', parentEl).removeClass('active');
            $('.show-searchdoc', parentEl).removeClass('show-searchdoc');
            $('.searchbox-clone', parentEl).remove();

            var btn = $(el).siblings('button');

            if (chapterToc === 'false') {
                var isOpen = $(btn).children('img').attr("src") === '/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg' ? true : false;
                var panel = $(btn).siblings("div.toc-item");

                if (isOpen === false) {
                    $(btn)
                        .children('img')
                        .attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg");

                    panel.slideToggle("fast");
                    panel.css("display", "flex");
                }
            }

            //create search node
            var tocEl = $(el).parent('.toc-item');
            var newClasses = "";

            tocEl.addClass('show-searchdoc');
            document.querySelector('#searchdocv2-form>input[name="pwcDocContext"]').value = pwcDocContext;
            document.querySelector('#searchdocv2-form>input[name="docSearchTitle"]').value = docSearchTitle;
            let clone = document.querySelector('#searchdocv2-form').cloneNode(true);

            if($('> button', tocEl).length > 0) {
                newClasses = 'searchbox-clone is-fade';
            } else {
                newClasses = 'searchbox-clone searchbox-clone--haschildren is-fade';
            }

            clone.setAttribute( 'class', newClasses)
        clone.setAttribute( 'id', 'searchdocv2-form-clone');

            tocEl.after(clone);

            setTimeout(function() {
                $('.searchbox-clone', parentEl).removeClass('searchbox-clone--haschildren');
                $('.searchbox-clone.is-fade', parentEl).removeClass('is-fade');
            }, 5)
        }

        // -------------- close searchdoc (exit modal) ----------------------
        var _closeSearchDoc = function() {
            $('.searchdoc-v2').addClass('is-hiding');
            $('.search-doc-link').removeClass('active');
            setTimeout(function() {
                $('.searchdoc-v2').removeClass('is-hiding');
                $('.searchdoc-v2').addClass('is-hidden');
            }, 255);
        }

        var _openSearchDoc = function() {
            $('.searchdoc-v2').addClass('is-hiding');
            $('.searchdoc-v2').removeClass('is-hidden');
            $('.search-doc-link').addClass('active');
            setTimeout(function() {
                $('.searchdoc-v2').removeClass('is-hiding');
            }, 5);

            setTimeout(function() {
                for(i = 0; i <= $(".searchdoc-v2 .searchdoc-body").length; i++) {
                    var parent = $(".searchdoc-v2 .searchdoc-body")[i];
                    if(typeof $(parent).find(".active")[0] !== "undefined") {
                      var scrollOffset = $(parent).find(".active")[0].offsetTop - 8;

                      if(typeof parent !== 'undefined' && scrollOffset > 50) {
                        parent.scrollTo({
                          top: scrollOffset - 80,
                          behavior: "smooth"
                        });
                      }
                    }
                  }
            }, 500);
        }

        return {
            init: initialize,
            fixedSearchAdjust: fixedSearchPosAdjust,
            closeSearchDoc: _closeSearchDoc
        }
    }());

    window.searchDoc = searchDoc;

});	    
