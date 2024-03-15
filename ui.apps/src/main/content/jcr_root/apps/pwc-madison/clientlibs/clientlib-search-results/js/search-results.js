(function(document, $, UserRegistration, Vue, URL, searchConstants) {

  function initializeSearchResultModal() {

    var searchResultModal = new Vue({
      el : '#search-results-vue',
      data : {
        count : 0,
        text : "",
        disp : "",
        clicks : 0,
        timer : null,
        locale : "",
        resultHTML:'',
        show : "none",
        pageLocale : window.getLocale(),
        maxPerPage : searchConstants.DEFAULT_MAX_PER_PAGE,
        resultPages : [],
        noResultFlag : false,
        isSpecialSearch:false,
        sideBySide:{
          isSideBySideShow:false, // flag for toggle sidebyside or goto content popup show/hide
          currentTaget:'',
          isSideBySideOpen:false, // flag for recognise that sidebyside screen open
          isSearchControlShow:true,
          width:(!window.location.href.includes('&s=c') && ['en-GB', 'en-CA','fr-CA'].includes(document.documentElement.lang) && window.matchMedia("(min-width: 1088px)").matches)?(document.documentElement.lang==='fr-CA')?'68.5%':'73%':'',
          margin:'',
          currentActiveUrl:'',
          currentActiveIndex:0,
          currentActiveTitle:'',
          isGatedContent: false
        },
        isFilterOpen: (!window.location.href.includes('&s=c') && ['en-GB', 'en-CA','fr-CA'].includes(document.documentElement.lang) && window.matchMedia("(min-width: 1088px)").matches)?true:false,
        signallw:{
			query:'',
			fusionQueryId:'',
			buttonType:'',
			buttonValue:'',
			appliedFilters:[],
			stayTimeframe: 0,
			currentPreviewNumber: ''
        },
        privateGroups: "",
        facetFilter : {
           facetsLabelsList : window.getFusionSearchFilterFacetsList(),
           facetsResults : {},
           facetSelectedParamsObj : {},
           filterCount : 0,
           facetApplied : false,
           appliedFilterCount : 0,
           totalAppliedFacetsList : [],
           defaultCountrySelectedFacetValues : [],
           slideOut : true,
           facet_i18n : window.facet_i18n,
           isPwcCountryParamPresent : false,
           allowedCountryList : []
        },
        pagination : {
          paginationPages : [],
          paginationFirst : false,
          paginationLast : false,
          paginationStep : 2,
          paginationPrevDots : 0,
          paginationNextDots : 0,
          startIndex : 0
        },
        suggestion : {
           hasSuggestion : false,
           suggestionText : ""
        },
        searchWithInDoc : {
          docSearchTitle : "",
          searchDocLevel : "",
          subSection : "",
          pwcDocContext : ""
        },
        searchSource : "",
        urlSearchParamData : {},
        audience : "",
        preferredTerritories : "",
        guidance : {
          guidanceCount : 0,
          suggestedGuidanceResults : [],
          guidanceResponseReceived : false,
          maxSuggestionCount : $("#maxSuggestionCount").val() || searchConstants.MAX_SUGGESTION_COUNT,
          maxSuggestGuidanceCount : $("#maxSuggestedGuidance").val() || searchConstants.MAX_SUGGESTED_GUIDANCE_COUNT,
          maxSuggestedGuidanceSlides : $("#maxSuggestedGuidanceSlides").val() || searchConstants.MAX_SUGGESTED_GUIDANCE_COUNT,
          hideSuggestedGuidence:true
        },
        sortFilter : {
           sortList : window.getFusionSearchFilterSortList(),
           selectedSortValue : "",
           selectedSortOption : "",
           showDropDown : false,
           isSortModalOpen : false,
           selectedSortValueInMobile : ""
        }
      },
      mounted : function() {
        
        if(window.location.href.includes('&s=c')){
            $('#search-results-vue').addClass('special-case');
            }
         var url = new URL(location.href), self;
         this.signallw.stayTimeframe = window.parseInt($('#search-results-vue').data('signalstaytimeframe'));
         this.text = url.searchParams.get(searchConstants.QUERYPARAM_Q) || searchConstants.BLANK_SPACE;
         this.disp = url.searchParams.get(searchConstants.QUERYPARAM_DISP) || searchConstants.BLANK_SPACE;
         this.locale = url.searchParams.get(searchConstants.QUERYPARAM_LOCALE) || this.pageLocale;
         this.audience = url.searchParams.get(searchConstants.QUERYPARAM_SPA) || searchConstants.BLANK_SPACE;
         this.pagination.startIndex = url.searchParams.get(searchConstants.QUERYPARAM_START) || 0;
         this.sortFilter.selectedSortValue = url.searchParams.get(searchConstants.QUERYPARAM_SORT) || searchConstants.DEFAULT_SORT_ORDER;
         this.sortFilter.selectedSortValueInMobile = this.sortFilter.selectedSortValue;
         this.maxPerPage = url.searchParams.get(searchConstants.QUERYPARAM_ROWS) || this.maxPerPage;
         this.searchWithInDoc.pwcDocContext = url.searchParams.get(searchConstants.QUERYPARAM_DOC_CONTEXT) || searchConstants.BLANK_SPACE;
         this.searchWithInDoc.docSearchTitle = url.searchParams.get(searchConstants.QUERYPARAM_DOC_SEARCH_TITLE) || searchConstants.BLANK_SPACE;
         this.searchWithInDoc.subSection = url.searchParams.get(searchConstants.QUERYPARAM_SUB_SECTION) || searchConstants.BLANK_SPACE;
         this.searchWithInDoc.searchDocLevel = url.searchParams.get(searchConstants.QUERYPARAM_DOC_LEVEL) || searchConstants.BLANK_SPACE;
         this.searchSource = url.searchParams.get(searchConstants.QUERYPARAM_S) || searchConstants.BLANK_SPACE;
         if(url.searchParams.get(searchConstants.QUERYPARAM_SP_K)) {
            this.facetFilter.isPwcCountryParamPresent = true;
            this.facetFilter.defaultCountrySelectedFacetValues = url.searchParams.get(searchConstants.QUERYPARAM_SP_K).split(searchConstants.PIPE_SEPARATOR);
         }
         this.setUserLocaleAndAudience();
         // MD-17296
         if(document.documentElement.lang === 'en-US'){  
            self=this;    
            ['media_type_s', 'industry_ss'].forEach(function(val){ delete self.facetFilter.facetsLabelsList[val];});
        }
         // End 
         this.createSelectedFacetParamObj(url.searchParams.getAll(searchConstants.QUERYPARAM_FQ),this.facetFilter.facetsLabelsList);
         this.updateSortLabel();
         if (UserRegistration.isUserLoggedIn){
             this.userProfileUpdateEvent();
             if (UserRegistration.recentlyViewed === undefined) {
                 UserRegistration.getRecentlyViewed();
             }
         }
         else{
             this.search();
         }
         if(this.text){
             this.UpdateRecentSearchCookie();
         }
      },
      methods : {
         UpdateRecentSearchCookie : function() {
             var dropCookie, recentSearch = window.encodeURIComponent(this.text), recentSearchCookie = $.cookie(searchConstants.RECENT_SEARCH),
             recentSearches = recentSearchCookie ? recentSearchCookie.split("~") : [], updatedRecentSearches = [], date = new Date(), index;
             dropCookie = undefined === window.OptanonActiveGroups || (typeof window.OptanonActiveGroups === "string" && window.OptanonActiveGroups.includes(',3,'));
             if(!recentSearches.includes(recentSearch) && dropCookie){
                 updatedRecentSearches.push(recentSearch);
                 for(index = 0; index < recentSearches.length; index++){
                     if(updatedRecentSearches.length < this.guidance.maxSuggestionCount){
                         updatedRecentSearches.push(recentSearches[index]);
                     }
                 }
                 $.cookie(searchConstants.RECENT_SEARCH, updatedRecentSearches.join("~"), { expires: new Date(date.setMonth(date.getMonth() + 3)), path: '/', secure: location.protocol === "https:" });
             }
         },
         userProfileUpdateEvent : function() {
             this.setUserLocaleAndAudience();
             this.facetFilter.isPwcCountryParamPresent = false;
             this.setUserPrivateGroups();
             this.search();
         },
         search : function() {
            this.guidance.hideSuggestedGuidence=true;
            this.show = "block";
            var queryParamString = this.getQueryParamString(),
            url = searchConstants.FUSION_SEARCH_ENDPOINT + searchConstants.QUESTION + queryParamString + this.getSignalParams(),
            searchPageUrl = window.location.pathname + searchConstants.QUESTION + queryParamString + this.getExtraQueryParamString() + window.location.hash;
            window.history.pushState({}, "", searchPageUrl);
            document.getElementById("content-loader").style.display = "flex";
            window.scrollTo(0, 0);
               $.ajax({
                  url : url,
                  type : "GET",
                  contentType : 'application/json; charset=utf-8',
                  success : function(response) {
                    this.handleResponseForSignal(response);
                    this.facetFilter.facetsResults = this.filterFacetsOrderByTerritory(response.facet_counts.facet_fields);
                    response.responseHeader.params.fq = this.correctFqForUs(response.responseHeader.params.fq);
                    this.facetFilter.facetSelectedParamsObj = response.responseHeader.params.fq && response.responseHeader.params.fq.length ? {} : this.facetFilter.facetSelectedParamsObj;
                    this.createSelectedFacetParamObj(response.responseHeader.params.fq,this.facetFilter.facetsResults);
                    if(response.response && response.response.docs) {
                        this.resultPages = response.response.docs;
                        this.noResultFlag = this.resultPages.length === 0;
                        this.setRecentlyViewedDateInSearchPages();
                        if(!response.response.docs.length && response.spellcheck && response.spellcheck.collations && response.spellcheck.collations.length > 1){
                            this.suggestion.hasSuggestion = true;
                            this.suggestion.suggestionText = response.spellcheck.collations[1];
                        }
                    }
                    if(!this.isDocSearch){
                        this.getSuggestedGuidanceResults();
                    }
                    this.count = response.response.numFound;
                    //If count = 0 then delete default pwcCountry facet key values
                    if(!this.initialResult){
                        this.facetFilter.facetSelectedParamsObj[searchConstants.COUNTRY_QUERY_PARAM].selectedFacets = [];
                        this.facetFilter.facetSelectedParamsObj[searchConstants.COUNTRY_QUERY_PARAM].appliedFacets = [];
                    }
                    this.createPagination();
                    this.setFacetEvent();
                    this.facetFilter.facetApplied = false;
                    this.facetFilter.appliedFilterCount = this.initialResult ? this.totalFilterCount : 0;
                    this.updateTotalAppliedFacetFilters();
                    this.setDataLayer();
                    if(window.location.href.includes('&s=c')){
                        this.isSpecialSearch=true;
                        if(!this.isMobile()){
                        this.sideBySide.width=this.sideBySide.isSideBySideOpen?this.sideBySide.width:'99%';
                        }
                    }
                    if(this.sideBySide.isSideBySideOpen){
                        this.sideBySideClick(this.resultPages.length?this.resultPages[0]:null,'pagination');
                    }
                    var that=this; // will use that varible to access variable inside setTimeout 
                    if (window.matchMedia("(max-width: 768px)").matches) {
                        $('.sg-show-more > .show-less').hide();
                        $('.sg-show-more > .show-more').show();
                        $('.sg-show-more > .arrow').removeClass('arrowUp');
                    }
                    setTimeout(function() {
                        window.dispatchEvent(new window.CustomEvent("initFavoriteListComponent"));
                        $('.suggested-guidance-slider').slick({
                          slidesToShow: that.isFilterOpen?3.5:4.5,
                          slidesToScroll: 1,
                          dots: false,
                          infinite: false,
                          speed: 300,
                          focusOnSelect: false,
                          prevArrow: $('.search-guidance .prev'),
                          nextArrow: $(".search-guidance .next"), 
                          responsive: [
                            {
                                breakpoint: 1088,
                                settings: {
                                    slidesToShow: 3.5
                                  }
                            }
                        ]
                        });
                        that.guidance.hideSuggestedGuidence=false;
                        document.getElementById("content-loader").style.display = "none";
                    }, "1000");
                  }.bind(this),
                  error : function(err) {
                    this.noResultFlag = true;
                    this.setDataLayer();
                    document.getElementById('content-loader').style.display = 'none';
                  }.bind(this)
               });
         },
         handleResponseForSignal : function(response){
             var activeTargetSearch = $(".new_target .new-filter.active");
             if(activeTargetSearch.length){
                 this.signallw.buttonType = 'target-button';
                 this.signallw.buttonValue = activeTargetSearch.text();
             }
             this.signallw.fusionQueryId = response.responseHeader.params.fusionQueryId;
             this.signallw.query = response.responseHeader.params.qq;
             if(this.signallw.buttonType){
                 this.postSignal('', '', this.signallw.buttonType, this.signallw.buttonType === 'apply-filters' ? this.signallw.appliedFilters : this.signallw.buttonValue);
                 this.signallw.buttonType = '';
                 this.signallw.buttonValue = '';
             }
         },
         getSignalParams : function(){
             var searchType = '', signalParams;
             if(this.signallw.buttonType === "apply-filters"){
                 searchType = "filtered-search";
             }
             else if(this.searchSource && this.searchSource === searchConstants.CURATED_SEARCH_SOURCE_QUERY_PARAM_VALUE) {
                 searchType = "curated";
             }
             else if(this.searchSource && this.searchSource === "st") {
                 searchType = "suggested-term";
             }
             else if(this.searchWithInDoc.pwcDocContext) {
                 searchType = "search-within-doc";
             }
             else if(this.searchSource && this.searchSource === "tt") {
                 searchType = "category";
             }
             else if(!this.searchWithInDoc.pwcDocContext) {
                 searchType = "main";
             }
             if(searchType){
                 signalParams = searchConstants.AND_SIGN + searchConstants.SIGNAL_TRACK_PWC_SEARCH_TYPE + searchConstants.EQUALS_SIGN + searchType;
             }
             if(UserRegistration.isUserLoggedIn){
                 signalParams += searchConstants.AND_SIGN + searchConstants.SIGNAL_TRACK_PWC_USER_COUNTRY + searchConstants.EQUALS_SIGN + UserRegistration.userInfo.countryCode;
             }
             return signalParams + searchConstants.AND_SIGN + searchConstants.SET_COOKIE_URL_PARAM + searchConstants.EQUALS_SIGN + searchConstants.FALSE;
         },
         postSignal : function(docId, resPos, buttonType, buttonValue, type, timeSpent){
             var signalPostData = [{
                 type: 'click',
                 params: {
                     query: this.signallw.query,
                     fusionQueryId : this.signallw.fusionQueryId
                 }
             }];
             if(docId){
                 signalPostData[0].params.docId = docId;
             }
             if(resPos){
                 signalPostData[0].params.res_pos = resPos;
             }
             if(buttonType){
                 signalPostData[0].params.buttonType = buttonType;
             }
             if(buttonValue){
                 signalPostData[0].params.buttonValue = buttonValue;
             }
             if(type){
                 signalPostData[0].type = type;
             }  
             if(timeSpent){
                 signalPostData[0].params.timeSpent = timeSpent;
             }
             $.ajax({
                 url : searchConstants.FUSION_SIGNAL_ENDPOINT + searchConstants.QUESTION + searchConstants.SET_COOKIE_URL_PARAM + searchConstants.EQUALS_SIGN + searchConstants.FALSE,
                 type : "POST",
                 contentType : 'application/json; charset=utf-8',
                 data: JSON.stringify(signalPostData),
                 success : function(response) {
                 }.bind(this),
                 error : function(err) {
                 }.bind(this)
             });
         },
         correctFqForUs : function(fq){
             var index = 0;
             if(this.locale === "en_us" && fq.length){
                for(index = 0; index < fq.length; index++){
                    fq[index] = fq[index].replace("category_ss", "topic_ss");
                }
             }
             return fq;
         },
         getQueryParamString : function(){
             var params = [];
             this.addParamsInSearchList(searchConstants.QUERYPARAM_LOCALE, this.locale, params);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_START, this.pagination.startIndex, params);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_Q, this.text, params);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_DISP, this.disp, params);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_SP_G, this.privateGroups, params);
             if(UserRegistration.isUserLoggedIn && this.audience) {
                 this.addParamsInSearchList(searchConstants.QUERYPARAM_SPA, this.audience, params);
             }
             this.setFilteredDefaultCountrySelectedFacet();
             this.addParamsInSearchList(searchConstants.QUERYPARAM_SP_K, this.facetFilter.defaultCountrySelectedFacetValues.join(searchConstants.PIPE_SEPARATOR), params);
             if(parseInt(this.maxPerPage,10) !== 10) {
                 this.addParamsInSearchList(searchConstants.QUERYPARAM_ROWS, this.maxPerPage, params);
             }
             if(this.sortFilter.selectedSortValue !== searchConstants.DEFAULT_SORT_ORDER){
                 this.addParamsInSearchList(searchConstants.QUERYPARAM_SORT, this.sortFilter.selectedSortValue, params);
             }
             this.addParamsInSearchList(searchConstants.QUERYPARAM_DOC_CONTEXT, this.searchWithInDoc.pwcDocContext, params);
             this.facetQueryParamFormation(params);
             
             return params.join(searchConstants.AND_SIGN);
         },
         setFilteredDefaultCountrySelectedFacet : function() {
             var index = 0, filteredDefaultCountrySelectedFacetValues = [];
             for(index = 0; index < this.facetFilter.defaultCountrySelectedFacetValues.length; index++){
                 if(this.facetFilter.allowedCountryList.includes(this.facetFilter.defaultCountrySelectedFacetValues[index])){
                     filteredDefaultCountrySelectedFacetValues.push(this.facetFilter.defaultCountrySelectedFacetValues[index]);
                 }
             }
             this.facetFilter.defaultCountrySelectedFacetValues = filteredDefaultCountrySelectedFacetValues.length ? filteredDefaultCountrySelectedFacetValues : this.facetFilter.allowedCountryList;
         },
         getExtraQueryParamString : function() {
            var extraParams = [];
            this.addParamsInSearchList(searchConstants.QUERYPARAM_S, this.searchSource, extraParams);
            this.addParamsInSearchList(searchConstants.QUERYPARAM_DOC_SEARCH_TITLE, this.searchWithInDoc.docSearchTitle, extraParams);
            this.addParamsInSearchList(searchConstants.QUERYPARAM_SUB_SECTION, this.searchWithInDoc.subSection, extraParams);
            this.addParamsInSearchList(searchConstants.QUERYPARAM_DOC_LEVEL, this.searchWithInDoc.searchDocLevel, extraParams);
            return extraParams.length ? searchConstants.AND_SIGN + extraParams.join(searchConstants.AND_SIGN) : searchConstants.BLANK_SPACE;
         },
         filterFacetsOrderByTerritory : function(facetsResultObj) {
           facetsResultObj = Object.keys(facetsResultObj).sort(function order(key1 , key2 ) {
             if(this.facetFilter.facetsLabelsList[key1] && this.facetFilter.facetsLabelsList[key2]) {
                if(parseInt(this.facetFilter.facetsLabelsList[key1].order, 10) < parseInt(this.facetFilter.facetsLabelsList[key2].order, 10)) {
                   return -1;
                }
                else if(parseInt(this.facetFilter.facetsLabelsList[key1].order, 10) > parseInt(this.facetFilter.facetsLabelsList[key2].order, 10)) {
                  return +1;
                }
                else {
                 return 0;
                }
             }
            }.bind(this)).reduce(function(Obj, key) {
                Obj[key] = facetsResultObj[key];
                return Obj; },{} );
            return facetsResultObj;
         },
         getSuggestedGuidanceResults : function() {
          var guidanceParams = [] , url, topGuidanceList = [];
             this.addParamsInSearchList(searchConstants.QUERYPARAM_Q, this.text, guidanceParams);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_LOCALE, this.locale, guidanceParams);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_GUIDANCE_COUNT, this.guidance.maxSuggestedGuidanceSlides - 1, guidanceParams);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_SP_K, this.facetFilter.defaultCountrySelectedFacetValues.join(searchConstants.PIPE_SEPARATOR), guidanceParams);
             this.addParamsInSearchList(searchConstants.QUERYPARAM_IS_EXACT_MATCH, true, guidanceParams);             
             if(UserRegistration.isUserLoggedIn && this.audience) {
                 this.addParamsInSearchList(searchConstants.QUERYPARAM_SPA, this.audience, guidanceParams);
             }
             url = searchConstants.FUSION_TYPEHEAD_ENDPOINT + searchConstants.QUESTION + guidanceParams.join(searchConstants.AND_SIGN) + searchConstants.AND_SIGN + searchConstants.SET_COOKIE_URL_PARAM + searchConstants.EQUALS_SIGN + searchConstants.FALSE;
            $.ajax({
               url : url,
               type : "GET",
               contentType : 'application/json; charset=utf-8',
               success : function(response) {
                 this.guidance.suggestedGuidanceResults = response.expanded && response.expanded.Guidance && response.expanded.Guidance.docs &&
                   response.expanded.Guidance.docs.length ? response.expanded.Guidance.docs : [];
                 topGuidanceList = response.response.docs ?
                    response.response.docs.filter(function(item, index) { return item.type === searchConstants.SUGGESTED_GUIDANCE_TYPE;}) : [];
                 if(topGuidanceList.length){
                     this.guidance.suggestedGuidanceResults.unshift(topGuidanceList[0]);
                 }
                 this.guidance.guidanceCount = this.guidance.suggestedGuidanceResults.length;
                 if(this.guidance.guidanceCount){
                     var options = {
                       "element": "span",
                       "className": "is-emphasized"
                     };
                     setTimeout(function() {
                       $('.search-guidance ul').mark(this.text.replace(searchConstants.DOUBLE_QUOTES_REGEX, ''), options);
                       $(".sg-title").each(function (i,element) {
                            if($(element).css('height').split('.')[0]<30){
                                $(element).addClass('single-line');
                                $(element).next('.brief').addClass('one-liner');
                            }
                       }.bind(this));
                     }.bind(this), "1000");
                 }
                 this.guidance.guidanceResponseReceived = true;
               }.bind(this),
               error : function(err) {
                 this.guidance.guidanceResponseReceived = false;
               }.bind(this)
            });
         },
         getReferrer : function(){
           return document.referrer || searchConstants.BLANK_SPACE;
         },
         getContentPath : function(fullURL) {
           var contentPath, url = new URL(fullURL);
           if (url.pathname) {
                 contentPath = searchConstants.BASE_PATH.concat(url.pathname.replace(searchConstants.MADISON_DITA_SHORTEN_NODE,
                    searchConstants.MADISON_DITA_NODE).replace(searchConstants.HTML_EXTENSION, searchConstants.BLANK_SPACE));
           }
           return contentPath;
         },
         setKeyword : function(url, index, callFrom) {
           event.preventDefault();
           var random, searchedSignalData, signalPostData;
           if(callFrom === 'Pop-Up' || (!this.isMobile() && !this.sideBySide.isSideBySideOpen)){
               signalPostData = [{
                   type: 'time-spent-on-page',
                   params: {
                       query: this.signallw.query,
                       fusionQueryId: this.signallw.fusionQueryId,
                       timeSpent: this.signallw.stayTimeframe,
                       res_pos: index,
                       docId: url
                   }
               }];
               random = window.Math.floor((Math.random() * 1000) + 1);
               window.localStorage.setItem(searchConstants.SEARCH_KEYWORD, this.text.replace(/^['"‘’“”]+/, '').replace(/['"‘’“”]+$/, ''));
               this.postSignal(url, index);
               window.localStorage.setItem(searchConstants.SIGNAL_HASH_PREFIX + random, window.JSON.stringify(signalPostData));               
               window.open(url + searchConstants.SIGNAL_HASH_PREFIX + random, '_blank');
           }
           return false;
         },
        searchCopyToClipboard:function(url){
          var copyElement = document.createElement('textarea');
          copyElement.value = url;
          document.body.appendChild(copyElement);
          copyElement.select();
          document.execCommand('copy');
          document.body.removeChild(copyElement);
        },
        isMobile: function () {
          if (window.matchMedia("(min-width: 1088px)").matches) {
            return false;
          } else {
            return true;
          }
        },
        toggleFilter:function(){
            this.isFilterOpen = !this.isFilterOpen;
            var langFrCa = document.documentElement.lang === 'fr-CA';
            if(this.isFilterOpen){
                this.sideBySide.width='73%';
            } else{
                this.sideBySide.width='';
            }

            if(this.isFilterOpen && langFrCa){
              this.sideBySide.width='68.5%';
            }
            $('.suggested-guidance-slider').slick('slickSetOption', 'slidesToShow', this.isFilterOpen?3.5:4.5);
            $('.suggested-guidance-slider').slick('refresh');            

        },
        sortListByColumn:function(columnName){
            var sort = window.getSortListForNavigationSearch(),sortby;
            switch(columnName){
                // handled almost all edge case for string comparision if something wrong please check content node value at crx path '/content/pwc-madison/global/reference-data/fusion-search/filter-sort/items/'
                case 'pagTitle':
                    if(this.sortFilter.selectedSortValue.toLowerCase().trim() === 'title_s asc'){
                        sortby = sort.find(function(p) { return p.value.toLowerCase().trim() === "title_s desc";});
                    }
                    else{
                        sortby = sort.find(function(p) { return p.value.toLowerCase().trim() === "title_s asc";});
                    }
                    this.sortResults(false,sortby);
                    break;
                case 'pubDate':
                    if(this.sortFilter.selectedSortValue.toLowerCase().trim() === 'pwcsortdate_dt asc'){
                        sortby = sort.find(function(p) { return p.value.toLowerCase().trim() === "pwcsortdate_dt desc";});
                    }
                    else{
                        sortby = sort.find(function(p) { return p.value.toLowerCase().trim() === "pwcsortdate_dt asc";});
                    }
                    this.sortResults(false,sortby);
                    break;
                case 'conType':
                     if(this.sortFilter.selectedSortValue.toLowerCase().trim() === 'sortorder_i asc'){
                         sortby = sort.find(function(p) { return p.value.toLowerCase().trim() === 'sortorder_i desc';});
                     }
                     else{
                         sortby = sort.find(function(p) { return p.value.toLowerCase().trim() === 'sortorder_i asc';});
                     }
                     this.sortResults(false,sortby);
                     break;
            }
            
        },
        sendPreviewSignal: function(url, index, random){
            if(this.sideBySide.isSideBySideOpen && random === this.signallw.currentPreviewNumber){
                this.postSignal(url, index, null, null, 'time-spent-on-page', this.signallw.currentPreviewNumber);
            }
        },
        sideBySideClick: function (selectedItem, callFrom, index) {
              var _self = this, filteredHTML, content, headerTitle, leftContainer, activeContainer, scrollMargin, indexedPath, podCastScript = '', currentYear = new Date().getFullYear(), random, $relatedContentImgs;
              // Code Work For Only Mobile Devices
              if (this.isMobile()) {
                  if (callFrom === 'anchor') {
                      if (this.sideBySide.isSideBySideOpen) {
                          // run when user click from side by side page anchor tag 
                          event.preventDefault();
                          this.sideBySide.isSideBySideShow = false;
                      }
                      else {
                          // run when user click from search page anchor tag 
                          this.clicks++;
                          if (this.clicks === 1) {
                              event.preventDefault();
                              this.timer = setTimeout(function () {
                                  //single click
                                  _self.sideBySide.isSideBySideShow = true;
                                  _self.clicks = 0;
                                  return false;
                              }, 700);
                          } else {
                              // double click function
                              this.sideBySide.isSideBySideShow = false;
                              clearTimeout(this.timer);
                              this.clicks = 0;
                          }
                      }
                  }
                  else {
                      // work when user click side by side option from pop-up on search page
                      $('#content-loader').show();
                      // manipulating current screen size
                      this.sideBySide.isSideBySideShow = false;
                      this.sideBySide.isSideBySideOpen = true;
                      this.sideBySide.width = '';
                      this.sideBySide.margin = '30px 0 15px 0';
                      this.sideBySide.isSearchControlShow = false;

                  }
              }
              // Code Work For desktop
              else {
                  if (this.sideBySide.isSideBySideOpen) {
                      if (event) { event.preventDefault(); }
                  }
                  else {
                      if (callFrom === 'anchor') {
                          return false;
                      }
                      $('#content-loader').show();
                      // manipulating current screen size
                      this.sideBySide.isSideBySideOpen = true;
                      this.sideBySide.width = '40%';
                      this.sideBySide.margin = '30px 0 15px 0';
                      this.sideBySide.isSearchControlShow = false;

                  }
              }
              if (callFrom === 'header') {
                  // selectedItem=findFirstItem();
                  selectedItem = this.resultPages.length ? this.resultPages[0] : '';
              }
              this.sideBySide.currentActiveUrl = selectedItem.url;
              this.sideBySide.currentActiveIndex = index;
              this.sideBySide.currentActiveTitle = selectedItem.title;
              random = window.Math.floor((Math.random() * 10000) + 1);
              this.postSignal(selectedItem.url, index, null, null, 'preview-click');
              window.setTimeout(this.sendPreviewSignal, this.signallw.stayTimeframe, selectedItem.url, index, random);
              $.ajax({
                  type: "GET",
                  url: selectedItem.url,
                  //url: 'http://localhost:4502/content/pwc-madison/us/article-7-dec.html',
                  dataType: "html",
                  success: function (data) {
                      filteredHTML = window.DOMPurify.sanitize(data, { WHOLE_DOCUMENT: true, ADD_TAGS: ['head', 'meta', 'script', 'input', 'title', 'link'], ADD_ATTR: ['name', 'content', 'property', 'class', 'type', 'value', 'charset', 'rel', 'href', 'src'] });

                      // <<<----------------Logic for AEM Template---------------->>>  
                      if ($(filteredHTML).find(".in-the-loop-template").length) { 
                          _self.sideBySide.isGatedContent = false;
                          _self.resultHTML = $(filteredHTML).find(".in-the-loop-template").get(0).outerHTML;
                          
                          $(document).on('init reInit afterChange beforeChange', '.related-content_wrapper', function(event, slick, currentSlide, slidesCount) {
                            var currentSlideCount = (currentSlide ? currentSlide : 0) + 1;
                            slidesCount = $('.related-content-section .slick-slide').length;
                            $(".slidersCount").text(currentSlideCount + " / " +slidesCount); 
                          });
                          setTimeout(function(){
                            $('.related-content_wrapper').slick({
                              slidesToShow: 2.5,
                              slidesToScroll: 1,
                              swipe: false,
                              dots: false,
                              infinite: false,
                              speed: 300,
                              prevArrow: $('.related-content_prev'),
                              nextArrow: $('.related-content_next'),
                              responsive: [
                                {
                                  breakpoint: 769,
                                  settings: "unslick"
                              },
                                  {
                                      breakpoint: 767,
                                      settings: {
                                          slidesToShow: 1,
                                          slidesToScroll: 1,
                                          swipe: true // Re-enable swipe for smaller screens
                                      }
                                  }
                              ]
                          }) ; 
                          
                          //Add class to handle invalid image path in related content component 
                          $relatedContentImgs = $('.search-page .side-by-side-right .related-content-section .related-content_card-img');
                          $relatedContentImgs.each(function() {
                            var img = $(this), testImage = new Image();
                            testImage.src = img.attr('src');
                            
                            $(testImage).on('error', function() {
                              img.parent().addClass('invalid-image');
                            });
                          });	
                          
                          }, 100);

                          $(filteredHTML).find('.buzzsprout-podcast').find('script').each(function(index, scriptElement) {
                            $('body').append($(scriptElement));
                          });
                         
                          $('#content-loader').hide();

                      }
                      //<<<-----------------END-------------------->>>
                      else {
                          // creating Right Side Content
                          if ($(filteredHTML).find('.pwc-buzzsprout').find('script').length) {
                              podCastScript = $(filteredHTML).find('.pwc-buzzsprout').find('script').get(0);
                          }
                          // Non-Gated Content
                          if ($(filteredHTML).find(".doc-body-content").length) {
                              if (podCastScript) {
                                  $('body').append(podCastScript);
                              }
                              _self.sideBySide.isGatedContent = false;
                              headerTitle = $(filteredHTML).find(".doc-body-head.columns").get(0).outerHTML;
                              content = $(filteredHTML).find(".content-page-container").get(0).outerHTML;
                              _self.signallw.currentPreviewNumber = random;
                          }
                          // Gated Content code
                          else {
                              indexedPath = $(filteredHTML).filter('meta[name="indexPath"]').attr("content");
                              if (indexedPath && indexedPath.includes('/user/gated-content')) {
                                  // Gated Content code
                                  _self.sideBySide.isGatedContent = true;
                                  headerTitle = "";
                                  content = $(filteredHTML).find(".gateway-body").get(0).outerHTML;
                              }
                              // Display the body content for 403,404 and 500
                              else {
                                  _self.sideBySide.isGatedContent = false;
                                  if ($(filteredHTML).find("div.error").get(0)) {
                                      headerTitle = "";
                                      content = $(filteredHTML).find("div.error").get(0).outerHTML;
                                  } else {
                                      headerTitle = "";
                                      content = $(filteredHTML).find("body.page-vp").get(0).innerHTML;
                                  }

                              }
                          }
                          //update copyright text with dynamic year
                          content = content.split('#year#').join(currentYear);
                          // End Gated Content Code
                          _self.resultHTML = headerTitle + content;
                          window.updateCopyrightYear();
                      }
                      $('#content-loader').hide();

                      // Scroll Left Container to Active Item

                      leftContainer = $(".Search-result-wrapper");
                      activeContainer = $('.module-heading.active');
                      if (Number(_self.maxPerPage) === 20) {
                          scrollMargin = 10;
                      }
                      else {
                          scrollMargin = 25;
                      }

                      leftContainer.animate({ scrollTop: activeContainer.offset().top - leftContainer.offset().top + leftContainer.scrollTop() - scrollMargin, scrollLeft: 0 }, 0);
                      $('.scrollRight').animate({ scrollTop: 0 }, 0);
                      window.scrollTo(0, 0);
                      // End Scroll Left Container to Active Item

                  },
                  error: function () {
                      $('#content-loader').hide();
                  }
              });

        },
        backToOriginal:function(){
            this.signallw.currentPreviewNumber = '';
            var langFrCa = document.documentElement.lang === 'fr-CA';
            this.sideBySide.isSideBySideOpen = false;
            if (this.isFilterOpen) {
                this.sideBySide.width = "73%";
            } else {
                this.sideBySide.width = "";
            }
            if(this.isFilterOpen && langFrCa){
              this.sideBySide.width='68.5%';
            } 
          this.sideBySide.margin = "";
          this.sideBySide.isSearchControlShow = true;
          this.sideBySide.isGatedContent = false;
          this.resultHTML='';
          if(window.location.href.includes('&s=c') && !this.isMobile()){
            this.sideBySide.width=this.sideBySide.isSideBySideOpen?this.sideBySide.width:'99%';
        }
        $('.suggested-guidance-slider').slick('refresh'); // to refresh slick
        },
        mouseOver: function (event) {
          this.sideBySide.currentTaget = event.currentTarget.querySelector("a.an-search-result").getAttribute('href');
          this.sideBySide.isSideBySideShow = true;
        },
        mouseOut: function () {
          this.sideBySide.currentTaget = "";
          this.sideBySide.isSideBySideShow = false;
        },
        adjustWidth: function (e) {
          e.preventDefault();
          var dragging = true,
            resizer = document.getElementsByClassName('resizable')[0], mouseUpHandler, mouseDownHandler, mouseMoveHandler,
            leftSide = resizer.previousElementSibling,
            rightSide = resizer.nextElementSibling,
            currentScreenSize=window.screen.width,
            x = 0, dx, dy, newLeftWidth,
            y = 0,
            leftWidth = 0;
          mouseDownHandler = function (e) {
            // Get the current mouse position
            x = e.clientX;
            y = e.clientY;
            leftWidth = leftSide.getBoundingClientRect().width;

            // Attach the listeners to `document`
            document.addEventListener('mousemove', mouseMoveHandler);
            document.addEventListener('mouseup', mouseUpHandler);
          };

          // Attach the handler
          resizer.addEventListener('mousedown', mouseDownHandler);

          mouseMoveHandler = function (e) {
            // How far the mouse has been moved
             dx = e.clientX - x;
             dy = e.clientY - y;

             newLeftWidth = ((leftWidth + dx) * 100) / resizer.parentNode.getBoundingClientRect().width;
            if (newLeftWidth > 30 && newLeftWidth < 50) {
              leftSide.style.width = newLeftWidth + '%';
              rightSide.style.width = (((currentScreenSize*(100 - newLeftWidth)/100) - 42)*100/currentScreenSize) + '%';
              $('.related-content_wrapper').slick('refresh');       
            }
          };
          mouseUpHandler = function (e) {
            if (dragging === true) {
              dragging = false;
              document.removeEventListener('mousemove', mouseMoveHandler);
              document.removeEventListener('mouseup', mouseUpHandler);
              resizer.removeEventListener('mousedown', mouseDownHandler);

            }

          };
        },
         setView : function(view) {
           this.maxPerPage = view;
           this.pagination.startIndex = 0;
           $('.suggested-guidance-slider').slick('unslick');
           this.search();
         },
         createPagination: function () {
            window.scrollTo(0, 0);
            this.pagination.paginationFirst = false;
            this.pagination.paginationLast = false;
            if (this.paginationSize < this.pagination.paginationStep * 2 + 6) {
                 this.changePaginationPages(1, this.paginationSize + 1);
            }
            else if (this.currentPage < this.pagination.paginationStep * 2 + 1) {
               if((this.pagination.paginationStep * 2 + 4) > 6){
                   this.changePaginationPages(1, 6);
               }
               else {
                   this.changePaginationPages(1, this.pagination.paginationStep * 2 + 4);
               }
               this.pagination.paginationLast = true;
            }
            else if (this.currentPage > this.paginationSize - this.pagination.paginationStep * 2) {
                this.pagination.paginationFirst = true;
                if(((this.paginationSize + 1) - (this.paginationSize - this.pagination.paginationStep * 2 - 2)) > 4) {
                     this.changePaginationPages(this.paginationSize - 4, this.paginationSize + 1);
                }
                else {
                   this.changePaginationPages(this.paginationSize - this.pagination.paginationStep * 2 - 2, this.paginationSize + 1);
                }
            }
            else {
                this.pagination.paginationLast = true;
                this.changePaginationPages(this.currentPage - this.pagination.paginationStep + 1, this.currentPage + this.pagination.paginationStep);
                this.pagination.paginationFirst = true;
            }
         },
         changePaginationPages: function (start, end) {
            var i;
            this.pagination.paginationPages = [];
            for (i = start; i < end; i++) {
                 this.pagination.paginationPages.push(i);
            }
            this.pagination.paginationPrevDots = start - 1;
            this.pagination.paginationNextDots = end;
         },
         changePage: function (newPage) {
            this.pagination.startIndex = Math.ceil((newPage - 1) * this.maxPerPage);
            $('.suggested-guidance-slider').slick('unslick');
            this.search();
         },
         backToTop : function() {
          if(this.sideBySide.isSideBySideOpen){
            $(".Search-result-wrapper").animate({
              scrollTop:0},200);
          }
          else{
           window.scrollTo({
             top: 0,
             behavior: 'smooth'
           });
          }
         },
         setRecentlyViewedDateInSearchPages : function() {
           if (UserRegistration !== undefined && UserRegistration.isUserLoggedIn
                && UserRegistration.recentlyViewed !== undefined && typeof UserRegistration.recentlyViewed !== "string"
                && UserRegistration.recentlyViewed !== "" && UserRegistration.recentlyViewed.length && this.resultPages.length) {
                this.resultPages.forEach(function(item, index) {
                  UserRegistration.recentlyViewed.forEach(function(recent) {
                    var url = new URL(item.url);
                    if(url !== undefined && url.pathname !== undefined){
                        if (searchConstants.BASE_PATH.concat(url.pathname.replace(searchConstants.MADISON_DITA_SHORTEN_NODE,searchConstants.MADISON_DITA_NODE)) === recent.pagePath) {
                            this.resultPages[index].recentlyViewed = true;
                            this.resultPages[index].recentlyViewedDate = recent.itemViewedDate;
                        }
                    }
                  }.bind(this));
                }.bind(this));
           }
         },
         setDataLayer : function() {
           window.digitalData = window.digitalData || {};
           window.digitalData.internalSearch = window.digitalData.internalSearch || {};
           var search = {};
           search[searchConstants.SEARCH_TERM] = this.text;
           search[searchConstants.SEARCH_RESULT_COUNT] = this.count;
           search[searchConstants.SEARCH_EVENT] = !this.noResultFlag ? searchConstants.SEARCH_SUCCESS : searchConstants.SEARCH_FAILURE;
           search[searchConstants.SEARCH_FILTER] = this.getSelectedFilter();
           search[searchConstants.SEARCH_FILTER][searchConstants.SEARCH_EVENT] = window.digitalData.internalSearch.filter && window.digitalData.internalSearch.filter.event ?
             window.digitalData.internalSearch.filter.event : searchConstants.FALSE;
           search[searchConstants.SEARCH_TYPE] = this.getSearchType();
           search[searchConstants.DOC_SEARCH_TITLE] = this.searchWithInDoc.docSearchTitle;
           search[searchConstants.SEARCH_DOC_LEVEL] = this.searchWithInDoc.searchDocLevel;
           window.digitalData.internalSearch = search;
         },
         getSearchType : function() {
             var source = "";
             if(this.searchSource && this.searchSource === searchConstants.CURATED_SEARCH_SOURCE_QUERY_PARAM_VALUE) {
                source = searchConstants.CURATED_SEARCH;
             }
             else if(this.searchWithInDoc.pwcDocContext) {
               source = searchConstants.SEARCH_WITH_IN_DOCUMENT;
             }
             else if(!this.searchWithInDoc.pwcDocContext) {
                source = searchConstants.MAIN_SEARCH;
             }
             return source;
           },
           getSelectedFilter : function() {
             var filter = {};
                 Object.keys(this.facetFilter.facetsResults).forEach(function(facetKey) {
                   if(this.facetFilter.facetSelectedParamsObj[facetKey]) {
                       var facetTypeKey = facetKey.split(searchConstants.FACETS_SPLIT_SEPARATOR)[0];
                       filter[facetTypeKey] = this.facetFilter.facetSelectedParamsObj[facetKey].appliedFacets;
                   }
                 }.bind(this));
             return filter;
           },
           setFacetEvent : function() {
              window.digitalData = window.digitalData || {};
              window.digitalData.internalSearch = window.digitalData.internalSearch || {};
              window.digitalData.internalSearch.filter = window.digitalData.internalSearch.filter || {};
              window.digitalData.internalSearch.filter.event = this.facetFilter.facetApplied ? searchConstants.TRUE : searchConstants.FALSE ;
           },
           setSearchPosition : function(resultIndex) {
             return parseInt(this.pagination.startIndex,10) + 1 + resultIndex;
           },
           toggleDropDown : function() {
             this.sortFilter.showDropDown = !this.sortFilter.showDropDown;
           },
           sortResults : function(hasMobileView, sortElement) {
              if(sortElement) {
                  this.sortFilter.selectedSortOption = sortElement.translatedTitles[this.pageLocale] || sortElement.title;
                  this.sortFilter.selectedSortValue = sortElement.value;
                  this.sortFilter.selectedSortValueInMobile = sortElement.value;
              }
              if(hasMobileView){
                  this.sortFilter.selectedSortValue = this.sortFilter.selectedSortValueInMobile;
                  this.updateSortLabel();
                  this.sortFilter.isSortModalOpen = false;
              }
              $('.suggested-guidance-slider').slick('unslick');
              this.search();
           },
           updateSortLabel : function() {
              this.sortFilter.sortList.forEach(function(sort) {
                if(sort.value === this.sortFilter.selectedSortValue) {
                    this.sortFilter.selectedSortOption = sort.translatedTitles[this.pageLocale] || sort.title;
                }
              }.bind(this));
           },
           closeSortModal : function() {
            this.sortFilter.selectedSortValueInMobile = this.sortFilter.selectedSortValue;
            this.sortFilter.isSortModalOpen = false;
           },
           selectSortOption : function(sortElement) {
             this.sortFilter.selectedSortValueInMobile = sortElement.value;
           },
           openSortModal : function() {
             this.sortFilter.isSortModalOpen = true;
           },
           onClickToggle : function() {
             this.facetFilter.slideOut = !this.facetFilter.slideOut;
           },
           setUserLocaleAndAudience : function() {
                this.urlSearchParamData = $.userInfo.getUserLocale();
                if(this.urlSearchParamData) {
                   this.facetFilter.allowedCountryList = this.urlSearchParamData.countryList;
                   if(!this.facetFilter.isPwcCountryParamPresent){
                       this.facetFilter.defaultCountrySelectedFacetValues = this.urlSearchParamData.countryList;
                   }
                   this.audience = this.urlSearchParamData.audience ? this.urlSearchParamData.audience.value : searchConstants.BLANK_SPACE;
                }
           },
           setUserPrivateGroups : function() {
               if(UserRegistration.userInfo.isInternalUser && UserRegistration.userInfo.contentAccessInfo && UserRegistration.userInfo.contentAccessInfo.privateGroups){
                   this.privateGroups = UserRegistration.userInfo.contentAccessInfo.privateGroups.join('|');
               }
           },
           createSelectedFacetParamObj : function(facetParamList,facetResultList){
                 Object.keys(facetResultList).forEach(function(facetKey){
                  if(this.facetFilter.facetsLabelsList[facetKey]) {
                    var facetFlag = false, facetFilteredList = Array.isArray(facetResultList[facetKey]) ?
                     facetResultList[facetKey].filter(function(element, index) { return this.isEvenFacetIndex(index); }.bind(this)) : [];
                    if(facetKey !== searchConstants.COUNTRY_QUERY_PARAM){
                       if(facetParamList.length) {
                           facetParamList.forEach(function(facet,index) {
                           if(facetKey === facet.split(searchConstants.COLON_SIGN)[0]){
                               facetFlag = true;
                               this.extractFromFacet(facet, facetFilteredList);
                           }
                         }.bind(this));
                       }
                    }
                    if(!facetFlag) {
                          this.$set(this.facetFilter.facetSelectedParamsObj,facetKey,{ selectedFacets : [] , appliedFacets : [] , toggled : true });
                    }
                  }

                 }.bind(this));
                 this.setDefaultFacetCountry();
                 this.facetFilter.filterCount = 0;
           },
           setDefaultFacetCountry : function() {
              this.facetFilter.facetSelectedParamsObj[searchConstants.COUNTRY_QUERY_PARAM].selectedFacets = this.facetFilter.defaultCountrySelectedFacetValues;
              this.facetFilter.facetSelectedParamsObj[searchConstants.COUNTRY_QUERY_PARAM].appliedFacets = this.facetFilter.defaultCountrySelectedFacetValues;
           },
           createFacetParam : function(facet,selectedFacets){
              selectedFacets.forEach(function(facetItem) {
                  this.signallw.appliedFilters.push(facet + "/" + facetItem);
              }.bind(this));
              return facet + searchConstants.FACET_OPEN_SQUARE_BRACKET_SEPARATOR + selectedFacets.join(searchConstants.FACET_OR_SEPARATOR) +
                searchConstants.FACET_CLOSE_SQUARE_BRACKET_SEPARATOR;
           },
           extractFromFacet : function(facetParam, facetFilteredList) {
             var facetSplit = facetParam.split(searchConstants.FACET_OPEN_SQUARE_BRACKET_SEPARATOR) ,
             appliedFacetValue = facetSplit[1].replace(searchConstants.FACET_CLOSE_SQUARE_BRACKET_SEPARATOR,searchConstants.BLANK_SPACE).split(searchConstants.FACET_OR_SEPARATOR),
             selectedFacetValue = facetSplit[1].replace(searchConstants.FACET_CLOSE_SQUARE_BRACKET_SEPARATOR,searchConstants.BLANK_SPACE).split(searchConstants.FACET_OR_SEPARATOR),
             appliedFacetList = facetFilteredList.length ? this.filterFacetList(appliedFacetValue, facetFilteredList) : appliedFacetValue,
             selectedFacetList = facetFilteredList.length ? this.filterFacetList(selectedFacetValue, facetFilteredList) : selectedFacetValue;
             this.$set(this.facetFilter.facetSelectedParamsObj,facetSplit[0],{ appliedFacets : appliedFacetValue , selectedFacets : selectedFacetValue, toggled : true });
           },
           filterFacetList : function(oldFacetList, facetFilteredList) {
              oldFacetList.forEach(function(facetItem, facetIndex) {
                if(!facetFilteredList.includes(facetItem)) {
                    oldFacetList.splice(facetIndex, 1);
                    this.filterFacetList(oldFacetList, facetFilteredList);
                }
              }.bind(this));
              return oldFacetList;
           },
           facetQueryParamFormation : function(searchParamsList){
              this.signallw.appliedFilters = [];
              Object.keys(this.facetFilter.facetSelectedParamsObj).forEach(function(facetKey) {
                if(facetKey !== searchConstants.COUNTRY_QUERY_PARAM && this.facetFilter.facetSelectedParamsObj[facetKey].appliedFacets.length){
                    this.addParamsInSearchList(searchConstants.QUERYPARAM_FQ,this.createFacetParam(facetKey,this.facetFilter.facetSelectedParamsObj[facetKey].appliedFacets),searchParamsList);
                }
              }.bind(this));
           },
           isEvenFacetIndex : function(valueIndex) {
              return (valueIndex%2 === 0);
           },
           isCountryFacetAndAllowed : function(facetKey, facetValue) {
               if(facetKey === "pwcCountry_s" && !this.facetFilter.allowedCountryList.includes(facetValue)){
                   return false;
               }
               return true;
           },
           addParamsInSearchList : function(parameter,value,searchParamsList) {
              if(parameter === searchConstants.QUERYPARAM_START || value){
                 searchParamsList.push(parameter + searchConstants.EQUALS_SIGN + window.encodeURIComponent(value));
              }
           },
           setFacetFilter : function(facetKey,facetValue) {
                  if(this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets.includes(facetValue)) {
                     if((facetKey === searchConstants.COUNTRY_QUERY_PARAM && this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets.length > 1) ||
                         facetKey !== searchConstants.COUNTRY_QUERY_PARAM) {
                      var facetKeyIndex = this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets.indexOf(facetValue);
                      this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets.splice(facetKeyIndex,1);
                      this.facetFilter.filterCount = this.totalFilterCount;
                     }
                  }
                  else {
                    this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets.push(facetValue);
                    this.facetFilter.filterCount = this.totalFilterCount;
                  }
           },
           updateFacetFilter : function() {
                 Object.keys(this.facetFilter.facetSelectedParamsObj).forEach(function(facetKey) {
                     this.facetFilter.facetSelectedParamsObj[facetKey].appliedFacets = this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets;
                 }.bind(this));
                 this.facetFilter.defaultCountrySelectedFacetValues = this.facetFilter.facetSelectedParamsObj[searchConstants.COUNTRY_QUERY_PARAM].appliedFacets;
           },
           applyFilter : function() {
             this.updateFacetFilter();
             this.facetFilter.facetApplied = true;
             this.signallw.buttonType = "apply-filters";
             $('.suggested-guidance-slider').slick('unslick');
             this.search();
           },
           clearAllFilters : function() {
                  Object.keys(this.facetFilter.facetSelectedParamsObj).forEach(function(facetKey) {
                    if(facetKey !== searchConstants.COUNTRY_QUERY_PARAM){
                        this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets = [];
                        this.facetFilter.facetSelectedParamsObj[facetKey].appliedFacets = [];
                        this.facetFilter.defaultCountrySelectedFacetValues = [];
                    }
                  }.bind(this));
              this.signallw.buttonType = "clear-filters";
              $('.suggested-guidance-slider').slick('unslick');
              this.search();
           },
           updateTotalAppliedFacetFilters : function() {
              this.facetFilter.totalAppliedFacetsList = [];
              if(this.initialResult) {
                  Object.keys(this.facetFilter.facetsResults).forEach(function(facetKey) {
                     if(this.facetFilter.facetSelectedParamsObj[facetKey] && this.facetFilter.facetSelectedParamsObj[facetKey].appliedFacets.length){
                          this.facetFilter.facetSelectedParamsObj[facetKey].appliedFacets.forEach(function(facetValue,index) {
                            facetValue = (facetKey === searchConstants.COUNTRY_QUERY_PARAM ? this.displayFacetCountryLabels(facetValue) : facetValue);
                            this.facetFilter.totalAppliedFacetsList.push(facetValue);
                          }.bind(this));
                     }
                  }.bind(this));
              }
           },
           updatePage : function() {
             var url = new URL(window.location.href);
             url.searchParams.set(searchConstants.QUERYPARAM_Q,this.suggestion.suggestionText);
             window.location.href = url;
           },
           displayFacetCountryLabels : function(facetCountryValue) {
              return this.facetFilter.facet_i18n[facetCountryValue] ? this.facetFilter.facet_i18n[facetCountryValue] : facetCountryValue ;
           }

      },
      computed: {
        paginationSize: function () {
          return Math.ceil(this.count/this.maxPerPage);
        },
        currentPage : function () {
          return Math.ceil((this.pagination.startIndex) / this.maxPerPage) + 1;
        },
        initialResult : function() {
          return this.text && this.count > 0 ;
        },
        isDocSearch : function() {
          return this.searchWithInDoc.pwcDocContext ;
        },
        totalFilterCount : function() {
          var totalCount = 0;
              Object.keys(this.facetFilter.facetSelectedParamsObj).forEach(function(facetKey) {
                totalCount = totalCount + this.facetFilter.facetSelectedParamsObj[facetKey].selectedFacets.length;
              }.bind(this));
          return totalCount;
        },
        showGuidance : function() {
          return this.guidance.guidanceCount > 0 && this.guidance.guidanceResponseReceived;
        }
      },
      filters : {
         formatDate : function(value) {
           var d = new Date(value),
           japaneseDateFormat = $('#search-results-vue').data('defaultdateformat');
           return ($.format.date(d, japaneseDateFormat));
         },
         uppercase : function(value) {
           return value.toUpperCase();
         }
        }
      
    });
    window.searchResultModal = searchResultModal;

  }

  initializeSearchResultModal();

}(document, $, window.UserRegistration, window.Vue, window.URL, window.searchConstants));
