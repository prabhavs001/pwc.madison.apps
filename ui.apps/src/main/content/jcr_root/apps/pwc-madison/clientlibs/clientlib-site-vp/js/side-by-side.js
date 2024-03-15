function sideBySideCopyToClipboard(e) {
    var el = document.createElement('textarea');
    el.value = window.location.origin + $(e).parents().find('.opened-card-head').data('sidePageLink');
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
}

var SBSTitleText='',prevScrollPosition=0,opts = {
    slidesToShow: 3,
    slidesToScroll: 1,
    dots: true,
    infinite: false,
    speed: 300,
    slide: ".item",
    prevArrow: "<span class='icon-caret-left' />",
    nextArrow: "<span class='icon-caret-right' />",
    responsive: [

      {
        breakpoint: 767,
        settings: "unslick"
      }
    ]
  };

// returns True if Slick element is Initialized
function isSlickInitialized(ele){
  return $(ele).hasClass('slick-initialized');
}

/* populate data for  Related content slider from the json  */
function relatedSlider(bodycallout, elem) {
  var currentElement = elem,
  getBodyCalloutList = ".getBodyCalloutList.json";
  bodycallout += getBodyCalloutList;
  if(!isSlickInitialized($(currentElement).find(".sub-topic-slick-slider"))){
    $(currentElement).find(".sub-topic-slick-slider").slick(opts);
  }

  return $.ajax({
    type: 'get',
    url: bodycallout,
    dataType: 'json',
    success: function (data) {
      if(data.bodyCalloutItems.length>0){
          $(currentElement).find(".sub-loader.calloutloader").fadeOut('slow');
          $(currentElement).find(".sub-topic-slick-slider").removeClass("sub-op");
          var items = data.bodyCalloutItems,
          calloutItems = data.bodyCalloutItems.length,
          locale = document.querySelector('[name="pageLocale"]').getAttribute('value').split('_')[0];
          Granite.I18n.setLocale(locale);
          $.each(items, function (each) {
            var thumbnail = items[each].thumbnail,
              publishedDate = items[each].publishedDate,
              revisedDate = items[each].revisedDate,
              title = items[each].title,
              description = items[each].description,
              url = items[each].url,
              country = items[each].country,
              contentFieldValue = items[each].contentFieldValue,
              category = items[each].category,
              scope = items[each].scope,
              format = items[each].format,
              imageAlt = items[each].imgAltText,
              detailpage = {},
              calloutAccessLevel = items[each].calloutAccessLevel,
              slide = '<div class="item slide-item single-slide">';
              if (detailpage!==null && detailpage!==undefined){
                  detailpage.title = title;
                  detailpage.detailPublishedDate = publishedDate;
                  detailpage.detailRevisedDate = revisedDate;
                  if(country && contentFieldValue){
                     detailpage.detailContentId = country + " " + contentFieldValue;
                  }
              }
              slide += ' <div id="sub-topic-content-page"><div class="is-block  madison-card-block rcl-slider"><div class="feature-front card__face"><div class="madison-card ">';
              if (thumbnail) {
                  slide += '<article class="rcl-image"><img alt="'+imageAlt+'" src="' + thumbnail + '"></article>';
              }
              slide += '<article>';
              slide += '<span class="date-block">';
              if (publishedDate) {
                  slide += '<p class="date">' + publishedDate + '</p>';
              }
              if (revisedDate) {
                  slide += '<p class="date"> ('+ Granite.I18n.get("Body_Callout_Updated") + ' ' + revisedDate + ')</p>';
              }
			  if (window.location.href.indexOf('joined.html') === -1) {
				slide += '<span class="sub-topic-slider-icon"><a href="' + url + '" id="detailBtn" class="open-link"><img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/open-new-tab.svg" class=""></a>';	
			  } else {
				  slide += '<span class="sub-topic-slider-icon"><a href="' + url + '" id="detailBtn" class="open-link" style="display: none;"><img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/open-new-tab.svg" class=""></a>';
			  }  
              if(format!==null && format==="dita"){
                  slide += '<a href="'+ url +'" data-detail="' + encodeURI(JSON.stringify(detailpage)) + '" class="subtopic-details-btn subtopic-sliderbtn-' + each + '" title="'+ Granite.I18n.get("Body_Callout_Click_Here") +'"><img class="aaa" src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/side-by-side.svg"></a></span></span>';
              }else{
                  slide += '</span></span>';
              }
              slide += '<p class="module-heading">' + title + '</p>';
              if (description) {
                  slide += '<p class="feature-tile-content">' + description + '</p>';
              }
              slide += "<div class='madison-card-exta-info'>";
              if (country && contentFieldValue) {
                  slide += '<p class="brief">' + country + " " + contentFieldValue + '</p>';
              }
              if (category) {
                slide += '<p class="brief lastbrief">' + category + '</p>';
              }
              if (calloutAccessLevel) {
                  if(calloutAccessLevel==="internalOnly"){
                      calloutAccessLevel = Granite.I18n.get("Body_Callout_Internal_User_Text");
                  } else if(calloutAccessLevel==="privateGroup"){
                      calloutAccessLevel = Granite.I18n.get(items[each].privateGroupType);
                  }else{
                      calloutAccessLevel = "";
                  }
                  slide += '<p class="brief"><span class="sub-topic-slider-text-orange">' + calloutAccessLevel + '</span></p>';
              }
              slide += '</div></article>';
              slide += '</div></div></div></div></div>';
              $(currentElement).find(".sub-topic-slick-slider").slick('slickAdd', slide);
            });

            if (calloutItems) {
              if (calloutItems === 2) {
                $(currentElement).find(".sub-topic-slick-slider").addClass("lessSlides").slick('unslick');
                $(currentElement).find(".sub-topic-slick-slider").parents(".sub-topic-container").addClass("twoSlidesCover");
              } else if (calloutItems === 1) {
                $(currentElement).find(".sub-topic-slick-slider").addClass("lessSlides").slick('unslick');
                $(currentElement).find(".sub-topic-slick-slider").parents(".sub-topic-container").addClass("singleSlideCover");
              } else {
                $(currentElement).find(".featured-single").find(".single-slide").hide();
                $(currentElement).find(".featured-single").find('.single-slide:lt(2)').show();
                if ($(window).width() < 767) {
                  $(currentElement).find(".show-toggle").show();
                }
              }
            }
      }else{
          $(currentElement).hide();
      }
    }
  });
}

/* populate data for  stacked Layout from the json  */
function stackedLayout(stackLayout, elem) {
  var currentElement = elem,
    getBodyCalloutList = ".getBodyCalloutList.json";

  stackLayout += getBodyCalloutList;
  return $.ajax({
    type: 'get',
    url: stackLayout,
    dataType: 'json',
    success: function (data) {
      if(data.bodyCalloutItems.length>0){
          $(currentElement).find(".sub-loader.calloutloader").fadeOut('slow');
          $(currentElement).find(".sub-topic-slick-slider").removeClass("sub-op");
          var items = data.bodyCalloutItems,
            calloutItems = data.bodyCalloutItems.length,
            locale = document.querySelector('[name="pageLocale"]').getAttribute('value').split('_')[0];
            Granite.I18n.setLocale(locale);
          $.each(items, function (each) {
            var thumbnail = items[each].thumbnail,
              publishedDate = items[each].publishedDate,
              revisedDate = items[each].revisedDate,
              title = items[each].title,
              description = items[each].description,
              url = items[each].url,
              country = items[each].country,
              contentFieldValue = items[each].contentFieldValue,
              category = items[each].category,
              scope = items[each].scope,
              format = items[each].format,
              imageAlt = items[each].imgAltText,
              detailpage = {},
              editorsnote =null,
              calloutAccessLevel = items[each].calloutAccessLevel,
              slide = '<div class="single-slide">';
              if($(currentElement).hasClass("text-layout")){
                editorsnote = items[each].edNote;
              }
              if (detailpage!==null && detailpage!==undefined){
                detailpage.title = title;
                detailpage.detailPublishedDate = publishedDate;
                detailpage.detailRevisedDate = revisedDate;
                if(country && contentFieldValue){
                   detailpage.detailContentId = country + " " + contentFieldValue;
                }
              }
              if(editorsnote){
                slide += '<div class="editor-content"> <span class="editor-note">'+ Granite.I18n.get("Body_Callout_Editor_Note_Label") +': </span>'+editorsnote+'</div>';
              }
              slide += '<div class="is-block madison-card-block"><div class="feature-front card__face"><div class="madison-card "><article class="stack-layout-mobile columns">';
            if (thumbnail) {
              slide += '<div class="column is-4"><img alt="'+imageAlt+'" src="' + thumbnail + '"></div>';
            }
            slide += '<div class="column "><span class="date-block">';
            if (publishedDate) {
              slide += '<p class="date">' + publishedDate + '</p>';
            }
            if (revisedDate) {
              slide += '<p class="date"> ('+Granite.I18n.get("Body_Callout_Updated")+ ' ' + revisedDate + ')</p>';
            }
			if (window.location.href.indexOf('joined.html') === -1) {
				slide += '<span class="sub-topic-slider-icon"><a href="' + url + '" id="detailBtn" class="open-link"><img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/open-new-tab.svg" class=""></a>';
			} else {
				slide += '<span class="sub-topic-slider-icon"><a href="' + url + '" id="detailBtn" class="open-link" style="display: none;"><img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/open-new-tab.svg" class=""></a>';
			}
            if(format!==null && format==="dita"){
                slide += '<a href="'+ url +'" data-detail="' + encodeURI(JSON.stringify(detailpage)) + '" class="subtopic-details-btn subtopic-stackbtn-' + each + '" title="'+ Granite.I18n.get("Body_Callout_Click_Here") +'"><img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/side-by-side.svg"></a></span></span>';
            }else{
                slide += '</span></span>';
            }
            slide += '<p class="module-heading">' + title + '</p>';
            if (description) {
              slide += '<p class="feature-tile-content">' + description + '</p>';
            }
            slide += '<div class="madison-card-exta-info">';
            if (country && contentFieldValue) {
                slide += '<p class="brief">' + country + " " + contentFieldValue + '</p>';
            }
            if (category) {
              slide += '<p class="brief lastbrief">' + category + '</p>';
            }
            if (calloutAccessLevel) {
                if(calloutAccessLevel==="internalOnly"){
                    calloutAccessLevel = Granite.I18n.get("Body_Callout_Internal_User_Text");
                } else if(calloutAccessLevel==="privateGroup"){
                    calloutAccessLevel = Granite.I18n.get(items[each].privateGroupType);
                }else{
                    calloutAccessLevel = "";
                }
                slide += '<p class="brief"><span class="sub-topic-slider-text-orange">' + calloutAccessLevel + '</span></p>';
            }
            slide += '</div></div></article></div></div></div>';

            $(currentElement).find(".single-column-slide").append(window.DOMPurify.sanitize(slide));
          });

          if($(currentElement).hasClass("text-layout")){
            if(calloutItems>1){
              $(currentElement).find(".featured-single").find(".single-slide").hide();
                $(currentElement).find(".featured-single").find('.single-slide:lt(1)').show();
              $(currentElement).find(".show-toggle").show();
            }else{
              $(currentElement).find(".show-toggle").hide();
            }
          }else{
           if(calloutItems>2){
             $(currentElement).find(".featured-single").find(".single-slide").hide();
             /* For text layout showing only one item on load */
             $(currentElement).find(".featured-single").find('.single-slide:lt(2)').show();

             $(currentElement).find(".show-toggle").show();
           }else{
             $(currentElement).find(".show-toggle").hide();
           }
         }
        }else{
          $(currentElement).hide();
        }
    }
  });
}

/* clamping the  loaded content from json */
function ajaxClamping() {
  var relatedSliderHeading = $(".related-slider .module-heading"),
  relatedSlidercontent = $(".related-slider .feature-tile-content"),
  stackedHeading = $(".stacked-layout .module-heading,.text-layout .module-heading"),
  stackedcontent = $(".stacked-layout .feature-tile-content,.text-layout .feature-tile-content");

  if (relatedSliderHeading) {
      if ($(window).width() < 769) {
          relatedSliderHeading.each(function(index, value) {
              $clamp(value, {
                  clamp: 2
              });
          });
      } else {
          relatedSliderHeading.each(function(index, value) {
              $clamp(value, {
                  clamp: 3
              });
          });
      }
  }

  if (relatedSlidercontent) {
      relatedSlidercontent.each(function(index, value) {
          $clamp(value, {
              clamp: 3
          });
      });
  }

  if (stackedHeading) {
      stackedHeading.each(function(index, value) {
          $clamp(value, {
              clamp: 2
          });
      });
  }

  if ($(window).width() < 769) {
    if (stackedcontent) {
      stackedcontent.each(function(index, value) {
          $clamp(value, {
              clamp: 3
          });
      });
  }
  }else{
    if (stackedcontent) {
      stackedcontent.each(function(index, value) {
          $clamp(value, {
              clamp: 2
          });
      });
  }
  }
}

function disableClickOnRightSection(){
    $("#right-clone-content").click(false);
    $("#right-clone-content a").css("cursor","default");
}

function sideBySideOpen(){
    var slickElements = $('.main-body-content .sub-topic-slick-slider'),getScrollPos,
    sliders, i, msg = $('.non-clickable-text-desktop').html(), readOnlyMsg = '<div class="non-clickable-text-mobile">' + msg + '</div>';
    if($(".main-body-content .sub-topic-slick-slider.slick-initialized").length){
        //removes slick if it is initialized
        for (i=0;i<slickElements.length;i++){
            if(isSlickInitialized(slickElements[i])){
                $(slickElements[i]).slick('unslick');
            }
        }
    }
    if($(".sub-topic-details-right").find(".non-clickable-text-mobile").length > 0){
        $(".sub-topic-details-right").remove(".non-clickable-text-mobile");
    } else {
        $(".right-title-bar").after(readOnlyMsg);
    }

	//$(".right-title-bar").after(readOnlyMsg);
    if($('body').hasClass('page-vp-inloop')){
        $("#left-clone-content .in-the-loop-template").remove();
        $("#left-clone-content").html($(".in-the-loop-template").clone());
    }else{
	$("#left-clone-content .content-page-container").remove();
    $("#left-clone-content").html($(".content-page-container").clone());
    }

    $("#left-clone-content .print-body-header").find("nav").remove();
    $(".left-clone-content").unmark();
    $(".left-title-bar .title-bar-left-side").text(SBSTitleText);
    $("html").addClass("modal-is-open");
    //scroll to the current position
    $("#sidebysideview").show();
    sliders = $(".content-page-container").find(".sub-topic-slick-slider");
    if(sliders.length){
        for (i=0;i<sliders.length;i++){
            if(!$(sliders[i]).hasClass("lessSlides")){
                $(sliders[i]).slick(opts);
            }
        }

    }
    /*setTimeout(function(){
        $("#left-clone-content").animate({
        scrollTop: $('#left-clone-content #hilightsection').offset().top -200}, "slow");
    }, 100);*/
    $("#left-clone-content").scrollTop(0);
    if($("#left-clone-content").length){
       getScrollPos = $('#left-clone-content #hilightsection').offset().top - 200;
       $("#left-clone-content").scrollTop(getScrollPos);
    }

    // MD-15105 Remove goto content icon from side by side view
     if (window.location.href.indexOf('joined.html') !== -1) {
          $('.title-bar-right-side .open-link').parent('li').hide();
		  // change view only taxt for MD-15160
		  if (window.matchMedia("(min-width: 1088px)").matches) {
			$('.non-clickable-text-desktop').replaceWith('<span class="non-clickable-text-desktop"><span class="non-clickable-text-join">'+ Granite.I18n.get("Join_View_Read_Only_Message") +'</span></span>');
		  }
		  else {
			$('.non-clickable-text-mobile').replaceWith('<div class="non-clickable-text-mobile"><span class="non-clickable-text-join">'+ Granite.I18n.get("Join_View_Read_Only_Message") +'</span></div>');
		  }
        // end change view only taxt for MD-15160
     }
     // End MD-15105 Remove goto content icon from side by side view


}

$('a.xref, a.pwc-xref,.anchor-sbs.text a,.aem-table-component a').hover(function(e){    //To append the Title name in side-by-side view

    var subSection = $($(this).closest('.topic.doc-body-content').find('.title:first')[0]).text(),parentSection = $(this).closest('.topic.doc-body-content').find('.print-body-header .title h1').text(),headTagTitle = $("head>title").text();
    

    if(parentSection){
      SBSTitleText = parentSection;
    } else if(subSection){
      SBSTitleText = subSection;
    } else {
      SBSTitleText = headTagTitle;
    }

    if($('.fix-menu-onscroll').hasClass('fix-top-onscroll')){
        prevScrollPosition = $(window).scrollTop() + $('.fix-menu-onscroll').height()+90;
    }
    else{
        prevScrollPosition = $(window).scrollTop();
    }
  });

/* toggle showmore/showless of Related contents in mobile */
$(document).on('click', '.releated-content-toggle', function (e) {
  e.preventDefault();
  $(this).siblings(".featured-single").toggleClass("feature-more");
  var featureToggleParent = $(this).siblings(".featured-single").hasClass("feature-more");
  if (featureToggleParent) {
    $(this).siblings(".featured-single").find(".single-slide").show();
    $(this).find("span").text(Granite.I18n.get("Body_Callout_Show_Less"));
    $(this).find(".arrow").addClass("arrowUp");
  } else {
    $(this).find("span").text(Granite.I18n.get("Body_Callout_Show_More"));
    $(this).siblings(".featured-single").find(".single-slide").hide();
    if($(this).parents(".sub-topic-div").hasClass("text-layout")){  //it work when we toggle Less/More
      $(this).siblings(".featured-single").find('.single-slide:lt(1)').show();
    } else {
      $(this).siblings(".featured-single").find('.single-slide:lt(2)').show();
    }
    $(this).find(".arrow").removeClass("arrowUp");
  }
});

function scrollToParticularSection(pageLink){
  var getScrollRightMove = 0,newOne = 0,addingSubheader = 0;
  if(($(window).width() < 1088) )  {
     addingSubheader = $("#left-clone-content").outerHeight() + $('.right-title-bar').outerHeight() + $('.non-clickable-text-mobile').outerHeight() + $('.left-title-bar').outerHeight() + $('.back-sub-topic-details').outerHeight();
     if($("#right-clone-content").length){
        getScrollRightMove =  $("#right-clone-content #"+pageLink.split('#')[1].replace(/\./,'\\.')).offset().top;
        newOne = getScrollRightMove - addingSubheader;
        $("#right-clone-content").scrollTop(newOne);
     }
  }else{
    addingSubheader =  $('.right-title-bar').outerHeight() + $('.back-sub-topic-details').outerHeight();
    if($("#right-clone-content").length){
      getScrollRightMove =  $("#right-clone-content #"+pageLink.split('#')[1].replace(/\./,'\\.')).offset().top;
      newOne = getScrollRightMove - addingSubheader;
      $("#right-clone-content").scrollTop(newOne);
    }
  }
}


function loadCalloutInRHS(){
    var sliders, stackLyt, txtLyt, bodycallout,stackLayout,txtLayout, i, j, k;
    //loads the Callout sections on the RHS of the Side-By-Side view
      if($("#right-clone-content").find(".sub-topic-slick-slider").length>0){
        sliders = $("#right-clone-content .related-slider");
        for(i=0;i<sliders.length;i++){
            bodycallout = $(sliders[i]).data("bodycallout");
            if (typeof bodycallout !== "undefined" && bodycallout !== false) {
             relatedSlider(bodycallout, sliders[i]);
            }
        }
      }
      if($("#right-clone-content").find(".single-column-slide").length>0){
        stackLyt = $("#right-clone-content .stacked-layout");
        for(j=0;j<stackLyt.length;j++){
            stackLayout = $(stackLyt[j]).data("bodycallout");
            if (typeof stackLayout !== "undefined" && stackLayout !== false) {
             stackedLayout(stackLayout,stackLyt[j]);
            }
        }
      }
      if($("#right-clone-content").find(".single-column-slide").length>0){
        txtLyt = $("#right-clone-content .text-layout");
        for(k=0;k<txtLyt.length;k++){
            txtLayout = $(txtLyt[k]).data("bodycallout");
            if (typeof txtLayout !== "undefined" && txtLayout !== false) {
             stackedLayout(txtLayout,txtLyt[k]);
            }
        }
      }
}

// this function is responsible to get the page content
function getPageContent(pageUrl){
    var htmldoc = document.createElement("html"),
    bodyElem, elementToRender, selector,
    isAnchorUrl=false;
    $.ajax({
        type: 'get',
        url: pageUrl,
        dataType: 'html',
        async: true,
        success: function (data) {
            if(data!==null && data!==""){
                htmldoc.innerHTML = window.DOMPurify.sanitize(data, {WHOLE_DOCUMENT: true, ADD_TAGS: ['head', 'meta', 'script', 'input', 'title', 'link'], ADD_ATTR: ['name', 'content', 'property', 'class', 'type', 'value', 'charset', 'rel', 'href', 'src'] });
                bodyElem = $(htmldoc).find("div.topic, .doc-body-content");
                if(pageUrl.indexOf("#") > 0 ){
                    selector = pageUrl.split("#")[1];
                    if($(bodyElem).attr("id") !== selector){
                        selector = "#"+selector.split(".").join("\\.");
                        if($(bodyElem).find(selector).length>0){
                            elementToRender = $(bodyElem).find(selector).get(0).outerHTML;
                            if($(elementToRender).hasClass("anchor-id")){
                                isAnchorUrl = true;
                                elementToRender = bodyElem.get(0).outerHTML;
                            }
                        }else{
                            elementToRender = bodyElem.get(0).outerHTML;
                        }
                    }else{
                        elementToRender = bodyElem.get(0).outerHTML;
                    }
                }else{
                    elementToRender = bodyElem.get(0).outerHTML;
                }
                $('.right-clone-content .opened-card-description').html(elementToRender);
                loadCalloutInRHS();
                if(isAnchorUrl && pageUrl.indexOf("#")>-1){
                    scrollToParticularSection(pageUrl);
                }
            }
        },
        error: function () {
        }
    });
}

var mailContent, mailContentPart1, mailContentPart2;
function getCompleteURL(relativeURL){
    var url = document.createElement('a');
	if(mailContentPart2){
		url.setAttribute('href', mailContentPart2.split("-  ")[1]);
	}
    if ($('#run-mode').val() !== 'author') {
        relativeURL = relativeURL.replace("/content/pwc-madison/ditaroot", "/dt");
    }
    return url.protocol.concat("//",url.host,relativeURL);
}
function getCompleteMailContent(title, relativeLink){
    var decodedStr = decodeURIComponent(mailContentPart1),
    mailTo = mailContent ? mailContent.substr(mailContent.indexOf("mailto:"), mailContent.indexOf("?")) : "",
    subject = mailContent && mailContentPart1 ? mailContentPart1.substring(mailContentPart1.indexOf("subject="), mailContent.indexOf("&body")) : "",
    body = mailContentPart2 ? mailContentPart2.substring(mailContentPart2.indexOf("body=")).split("=")[1] : "",
    bodyWithTitle = body.split("-  ")[0],
    subjectParts = subject.split(" - ");
    if (encodeURI(bodyWithTitle).indexOf("%0D%0A%0D%0A")>1){
         bodyWithTitle = decodeURI(encodeURI(bodyWithTitle).split("%0D%0A%0D%0A")[0].concat("%0D%0A%0D%0A",title));
    }
    subjectParts[1] = title;
    return mailTo.concat("?",subjectParts.join(" - "),"&body=",bodyWithTitle," -  ",getCompleteURL(relativeLink));
}

/* to open the side by side layout of related content */
$(document).on('click', '.subtopic-details-btn', function (e) {
  e.preventDefault();
  var sideFlag = true,
  className = '.'+$(this).attr('class').split(' ').join('.'),
  dataDetail = JSON.parse(decodeURI($(this).data("detail"))),
  pageLink = $(e.currentTarget).attr("href"),
  detailTitle, detailDescription, detailPublishedDate, detailRevisedDate,detailContentId, content, copyLink;
  if($(this).parents('#sub-topic-details-page').length) {
    sideFlag = false;
  }
  $('#sub-topic-details-page').find('#hilightsection').removeAttr('id');
  $(this).attr('id', 'hilightsection');
  if(sideFlag){
    sideBySideOpen();
  }
  if(dataDetail){
      detailTitle = dataDetail.title;
      $(".right-title-bar [aria-label=Mail]").attr('href',encodeURI(getCompleteMailContent(detailTitle, pageLink)));
      getPageContent(pageLink);
      detailPublishedDate = dataDetail.detailPublishedDate;
      detailRevisedDate = dataDetail.detailRevisedDate;
      detailContentId =dataDetail.detailContentId;
      copyLink = pageLink;
      if ($('#run-mode').val() !== 'author') {
          copyLink = pageLink.replace("/content/pwc-madison/ditaroot", "/dt");
      }
      content = '<div class="opened-card-head" data-side-page-link="' + copyLink + '"><div class="opened-card-title-number"></div><div class="opened-card-title">' + detailTitle + '</div>';
      $(".title-bar-right-side .share_icons .open-link").attr("href", copyLink);
      if(detailPublishedDate || detailRevisedDate){
        content += '<div class="opened-card-date">';
      }
      if(detailPublishedDate) {
        content += Granite.I18n.get("Side_By_Side_Publication_Date")+': ' + detailPublishedDate +' ';
      }
      if(detailRevisedDate){
        content += '('+Granite.I18n.get("Side_By_Side_Updated")+' ' + detailRevisedDate + ')';
      }
      if(detailPublishedDate || detailRevisedDate){
        content += '</div>';
      }
      if(detailContentId){
          content += '<div class="in-depth">'+detailContentId+'</div>';
      }
      content += '</div>';
      //content += '<div class="favorites-toggle"><div><span><div class="favorite-toggle-tooltip">'+Granite.I18n.get("Side_By_Side_Add_To_Favorites")+'</div><div class="favorite-toggle-error"><p>An error occurred, please try again.</p></div></span></div></div>';
      content += '<div class="opened-card-body-content"><div class="opened-card-description"><div class="sub-loader calloutloader" ></div></div></div>';
      $(".right-title-bar .title-bar-left-side").text(detailTitle);
      $("#right-clone-content").html(content);
  }

  if(sideFlag){
    disableClickOnRightSection();
  }
});
function navigateTo(e, link){
	var toLink = link, sideView, targethash;
    if(toLink.indexOf(window.location.pathname)>=0){
      e.preventDefault();
      sideView = $("#sidebysideview");
      $('#left-clone-content #hilightsection').removeAttr('id');
      $("#right-clone-content,.right-title-bar .title-bar-left-side,.right-title-bar .title-bar-left-side,#left-clone-content").empty();
      sideView.hide();
      if($('body').hasClass('page-vp-inloop')){
        $('.page-vp-inloop #hilightsection').removeAttr('id');
        }
      $('.content-page-main #hilightsection').removeAttr('id');
      $("html").removeClass("modal-is-open");
	  window.location = toLink;
	  //Scroll down to the target position on desktop , reducing the height of sticky menu//
	  targethash = $(window.DOMPurify.sanitize(location.hash.replace(/\./,'\\.')));
	  if ($(targethash).length) {
	  //Scroll down to the target position on mobile devices , reducing the height of sticky menu //
	    setTimeout(function() {
	      if($('.toc-mobile-bar').hasClass('stickymob')) {
              $('html, body').animate({
                  scrollTop: targethash.offset().top - ($('.toc-mobile-bar').outerHeight())
              }, 1000);
          }
	    }, 1010);

	  }
    }else{
        window.location = toLink;
    }
}
/* For Inline-Link Side-by-Side Tooltip */
if($(document).find('meta[name="pwcDisableInlineLinks"]').attr("content") !== 'yes'){
var inlineClicked = 0 , inlineLinkContent , getHref;

inlineLinkContent = '<div id="side-by-side-content-hover" class="side-by-side-content-hover">';
inlineLinkContent +=  '<a href="#" class="direct-link "><span><img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/open-new-tab.svg" class="side-by-side-content-hover-icon"></span> '+Granite.I18n.get("Hover_Go_To_Content")+'</a>';
inlineLinkContent +=  '<a href="#" class="linkcallout " style="margin-left: 10px;"><span><img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/side-by-side.svg" class="side-by-side-content-hover-icon"></span> '+Granite.I18n.get("Hover_Side_By_Side_View")+'</a>';
inlineLinkContent += '</div>';

  tippy("a.pwc-xref,a.xref, .page-vp-inloop .anchor-sbs.text a,.page-vp-inloop .aem-table-component a", {
    content:  $(inlineLinkContent).html(),
    placement: "top",
    theme: "inlineLink",
    arrow: false,
    zIndex: 9999,
    interactive: true,
    distance: 15,
    boundary: "window",
    followCursor:"initial",
    hideOnClick: true,
    onMount:function(instance){
      var dataScope, linkPath;
	  dataScope = $(instance.reference).attr("data-scope");
      linkPath = $(instance.reference).attr('href');
      tippy.hideAll({ exclude: instance });
      if(dataScope === 'external' || $(instance.reference)[0].host !== window.location.host || linkPath.includes('/content/dam/')){
        $(instance.popper).hide();
      }
	  /* checking if page is joined, hide go to content link */
      if (window.location.href.indexOf('joined.html') !== -1) {
		$(instance.popper).find(".direct-link").hide();
      }
    },
    /* to open the side by side layout for a hyperlink content */
    onShown: function(instance) {
      var toLink = $(instance.reference).attr('href') ,hashlink;
		$(".direct-link").click(function(e){
            navigateTo(e,toLink);
		  });
      $(".linkcallout").one("click",function(e){
        e.preventDefault();
        $(instance.reference).attr('id', 'hilightsection');
        $(instance.reference).find(".xref-info div").attr('id', 'hilightsection');
        /*if (toLink.indexOf('#') !== -1) {
            hashlink = toLink.split('#')[1].replace(/\./,'\\.');
        }*/
        sideBySideOpen();
        instance.hide();
        $(".right-clone-content").html("<div class='sub-loader calloutloader' >");
        $.ajax({
          type: "GET",
          url: toLink,
          dataType: "html",
          success: function (data) {
            e.preventDefault();
			var  htmldoc, detailSubtitle, detailTitle, detailPublishedDate, detailRevisedDate,content,detailContentIdOrType,detailsPwcCountry,elementToRender, bodyElem, indexedPath, modalElem, errorBodyElem, selector, copyLink, isAnchorUrl=false, $relatedContentImgs;
			htmldoc =  document.createElement("html");
            if(data!==null && data!==""){
                htmldoc.innerHTML = window.DOMPurify.sanitize(data, {WHOLE_DOCUMENT: true, ADD_TAGS: ['head', 'meta', 'script', 'input', 'title', 'link'], ADD_ATTR: ['name', 'content', 'property', 'class', 'type', 'value', 'charset', 'rel', 'href', 'src'] });
                // <<<----------------Logic for AEM Template---------------->>>  
                if ($(htmldoc).find(".in-the-loop-template").length) { // will change the class once we get class and div wrapper from aem
                    // code has to be write for removing toolbar from content if it will not move out of container div
                    elementToRender = $(htmldoc).find(".in-the-loop-template").get(0).outerHTML;
                    detailTitle = $(htmldoc).find('title').text();
                    $(".right-title-bar [aria-label=Mail]").attr('href',encodeURI(getCompleteMailContent(detailTitle, toLink)));
                    $(".right-title-bar .title-bar-left-side").text(detailTitle);
                    copyLink = toLink;
                    // if ($('#run-mode').val() !== 'author') {
                    //     copyLink = toLink.replace("/content/pwc-madison/ditaroot", "/dt");
                    // }
                    content = '<div class="opened-card-head" data-side-page-link="' + copyLink + '"><div class="opened-card-title">' + detailTitle + '</div>';
                    $(".title-bar-right-side .share_icons .open-link").attr("href", copyLink);
                    content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                    $("#right-clone-content").html(content);

                    $('#right-clone-content .related-content_wrapper').on('init reInit afterChange beforeChange', function(event, slick, currentSlide, slidesCount) {
                        var currentSlideCount = (currentSlide ? currentSlide : 0) + 1;
                        slidesCount = $('#right-clone-content .related-content-section .slick-slide').length;
                        $("#right-clone-content .slidersCount").text(currentSlideCount + " / " +slidesCount); 
                      });

                      setTimeout(function(){
                        $('#right-clone-content .related-content_wrapper').slick({
                            slidesToShow: 2.5,
                            slidesToScroll: 1,
                            swipe:false,
                            dots: false,
                            infinite: false,
                            speed: 300,
                            prevArrow: $('.related-content_prev'),
                            nextArrow: $('.related-content_next'),
                            responsive: [
                                {
                                    breakpoint: 3000,
                                    settings: "unslick"
                                },
                                {
                                    breakpoint: 768,
                                    settings: {
                                        slidesToShow: 1,
                                        slidesToScroll: 1,
                                        swipe:true
                                        }
                                }
                            ]
                          });

                      //Add class to handle invalid image path in related content component 
                      $relatedContentImgs = $('#right-clone-content .related-content-section .related-content_card-img, #left-clone-content .related-content-section .related-content_card-img');
                      $relatedContentImgs.each(function() {
                        var img = $(this), testImage = new Image();
                        testImage.src = img.attr('src');
                        
                        $(testImage).on('error', function() {
                          img.parent().addClass('invalid-image');
                        });
                      });	

                      }, 100);
                }
                //<<<-----------------END-------------------->>>
                else{
                    bodyElem = $(htmldoc).find("div.topic, .doc-body-content");
                    if(bodyElem.get(0)){
                        if(toLink.indexOf("#") > 0 ){
                            selector = toLink.split("#")[1];
                            if($(bodyElem).attr("id") !== selector){
                                selector = "#"+selector.split(".").join("\\.");
                                if($(bodyElem).find(selector).length>0){
                                    elementToRender = $(bodyElem).find(selector).get(0).outerHTML;
                                    if($(elementToRender).hasClass("anchor-id")){
                                        isAnchorUrl = true;
                                        elementToRender = bodyElem.get(0).outerHTML;
                                    }
                                }else{
                                    elementToRender = bodyElem.get(0).outerHTML;
                                }
                            }else{
                                elementToRender = bodyElem.get(0).outerHTML;
                            }
                        }else{
                            elementToRender = bodyElem.get(0).outerHTML;
                        }
                        detailTitle = $(htmldoc).find('title').text();
                        $(".right-title-bar [aria-label=Mail]").attr('href',encodeURI(getCompleteMailContent(detailTitle, toLink)));
                        detailPublishedDate = $(htmldoc).find('meta[name="pwcReleaseDate"]').attr("content");
                        detailRevisedDate = $(htmldoc).find('meta[name="Last-Modified"]').attr("content");
                        detailsPwcCountry = $(htmldoc).find('meta[name="pwcCountry"]').attr("content");
                        if ($(htmldoc).find('meta[name="pwcContentId"]').length) {
                            detailContentIdOrType = $(htmldoc).find('meta[name="pwcContentId"]').attr("content");
                        } else {
                            detailContentIdOrType = $(htmldoc).find('meta[name="pwcContentType"]').attr("content");
                        }
                        $(".right-title-bar .title-bar-left-side").text(detailTitle);
                        copyLink = toLink;
                        if ($('#run-mode').val() !== 'author') {
                            copyLink = toLink.replace("/content/pwc-madison/ditaroot", "/dt");
                        }
                        content = '<div class="opened-card-head" data-side-page-link="' + copyLink + '"><div class="opened-card-title">' + detailTitle + '</div>';
                        $(".title-bar-right-side .share_icons .open-link").attr("href", copyLink);
                        if(detailPublishedDate || detailRevisedDate){
                            content += '<div class="opened-card-date">';
                        }
                        if(detailPublishedDate) {
                            content += Granite.I18n.get("Side_By_Side_Publication_Date")+': ' + detailPublishedDate +' ';
                        }
                        if(detailRevisedDate){
                            content += '('+Granite.I18n.get("Side_By_Side_Updated")+' ' + detailRevisedDate + ')';
                        }
                        if(detailPublishedDate || detailRevisedDate){
                            content += '</div>';
                        }
                        if(detailsPwcCountry && detailContentIdOrType){
                        content += '<div class="in-depth"><span class="is-uppercase">'+detailsPwcCountry+'</span>'+' '+detailContentIdOrType+'</div>';
                        }
                        content += '</div>';
                        //content += '<div class="favorites-toggle"><div><span><div class="favorite-toggle-tooltip">'+Granite.I18n.get("Side_By_Side_Add_To_Favorites")+'</div><div class="favorite-toggle-error"><p>An error occurred, please try again.</p></div></span></div></div>';
                        content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                        $("#right-clone-content").html(content);
                    /* if (toLink.indexOf('#') !== -1) {
                            $("#right-clone-content").animate({
                                scrollTop: $('#right-clone-content #'+hashlink).offset().top -200}, "slow");
                        }*/
                        loadCalloutInRHS();
                    }else{
                        indexedPath = $(htmldoc).find('meta[name="indexPath"]').attr("content");
                        if(indexedPath && indexedPath.includes('/user/gated-content')){
                            modalElem = $(htmldoc).find("div.gateway-body");
                            if(modalElem.get(0)){
                                elementToRender = modalElem.get(0).outerHTML;
                                content = '<div><div class="opened-card-head"></div>';
                                content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                                $("#right-clone-content").html(content);
                                $('#right-clone-content .gateway-body').addClass("is-active");
                                $('#right-clone-content .gateway-body').css('position', 'absolute');
                                //$('.right-title-bar').hide();
                            }

                        }else if(indexedPath && indexedPath.includes('contentType=internalOnly')){
                            modalElem=$(htmldoc).find('#access-restricted.gateway-body');
                            if(modalElem.get(0)){
                                // C1 -: to hide concurrent & perseat popup element 
                                $(htmldoc).find('#access-restricted.gateway-body [data-usertype="concurrent"]').hide();
                                $(htmldoc).find('#access-restricted.gateway-body [data-usertype="perseat"]').hide();
                                // C1 end
                                elementToRender=modalElem.get(0).outerHTML;
                                content = '<div><div class="opened-card-head"></div>';
                                content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                                $("#right-clone-content").html(content);
                                $('#right-clone-content .gateway-body').addClass("is-active");
                                $('#right-clone-content .gateway-body').css('position', 'absolute');
                            }
                        }else if(indexedPath && indexedPath.includes('userType=perseat')){
                            modalElem=$(htmldoc).find('#access-restricted.gateway-body');
                            if(modalElem.get(0)){
                                // C1 -: to hide concurrent & internalOnly popup element 
                                $(htmldoc).find('#access-restricted.gateway-body [data-usertype="concurrent"]').hide();
                                $(htmldoc).find('#access-restricted.gateway-body [data-contenttype="internalOnly"]').hide();
                                // C1 end
                                elementToRender=modalElem.get(0).outerHTML;
                                content = '<div><div class="opened-card-head"></div>';
                                content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                                $("#right-clone-content").html(content);
                                $('#right-clone-content .gateway-body').addClass("is-active");
                                $('#right-clone-content .gateway-body').css('position', 'absolute');
                            }
                        }else if(indexedPath && indexedPath.includes('userType=concurrent')){
                            modalElem=$(htmldoc).find('#access-restricted.gateway-body');
                            if(modalElem.get(0)){
                                // C1 -: to hide other perseat & internal popup element 
                                $(htmldoc).find('#access-restricted.gateway-body [data-contenttype="internalOnly"]').hide();
                                $(htmldoc).find('#access-restricted.gateway-body [data-usertype="perseat"]').hide();
                                // C1 end
                                elementToRender=modalElem.get(0).outerHTML;
                                content = '<div><div class="opened-card-head"></div>';
                                content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                                $("#right-clone-content").html(content);
                                $('#right-clone-content .gateway-body').addClass("is-active");
                                $('#right-clone-content .gateway-body').css('position', 'absolute');
                            }
                        }else {
                            // Display the body content for 403,404 and 500
                            errorBodyElem = $(htmldoc).find("div.error");
                            if(errorBodyElem.get(0)){
                                elementToRender = errorBodyElem.get(0).outerHTML;
                                detailTitle = $(htmldoc).find('title').text();
                                $(".right-title-bar .title-bar-left-side").text(detailTitle);
                                content = '<div><div class="opened-card-head"></div>';
                                content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                                $("#right-clone-content").html(content);
                                $("#right-clone-content").find(".page-404").show();
                            } else{
                                elementToRender = $(htmldoc).find("body.page-vp").get(0).innerHTML;
                                detailTitle = $(htmldoc).find('title').text();
                                $(".right-title-bar .title-bar-left-side").text(detailTitle);
                                content = '<div><div class="opened-card-head"></div>';
                                content += '<div class="opened-card-body-content"><div class="opened-card-description">' + elementToRender + '</div></div></div>';
                                $("#right-clone-content").html(content);
                            }

                        }
                    }
                }
				if(isAnchorUrl && toLink.indexOf("#")>-1){
                    scrollToParticularSection(toLink);
                }
            }
          }
        });
      });
    }
  });

  $("a.xref, a.pwc-xref").click(function(e){
        if(!($(this).attr("data-scope") === 'external' || $(this)[0].host !== window.location.host || $(e.currentTarget).attr("href").includes('/content/dam/'))){
            if(($(window).width() < 1088) )  {
                e.preventDefault();

                getHref = $(this).attr('href');

                     inlineClicked++;
                     setTimeout(function(){
                       if ( inlineClicked === 1 ) {
                         e.preventDefault();
                         inlineClicked = 0;
                       } else if (inlineClicked === 2) {
                          if (window.location.href.indexOf('joined.html') !== -1) {
                            e.preventDefault();
                          } else {
                          window.location.href = getHref;
                          }
                          inlineClicked = 0;
                       }
                     }, 1000);

            }else{
              if (window.location.href.indexOf('joined.html') !== -1) {
                e.preventDefault();
              }
              else {
              navigateTo(e, $(e.currentTarget).attr("href"));
              }
            }
        }
    });
    $(".open-link").click(function(e){
        navigateTo(e, $(e.currentTarget).attr("href"));
    });
}
/* For Inline-Link Side-by-Side Tooltip End*/

function copyMailIconToSideBySide(){
    if($(".share-tooltip-popout").length>0){
        $(".share-tooltip-popout")[0].click();
        $(".right-title-bar .share_icons").append("<li>"+$("#toolbar_tip_home [aria-label=Mail]")[0].outerHTML+"</li>");
        mailContent = decodeURIComponent($("#toolbar_tip_home [aria-label=Mail]").attr('href'));
        mailContentPart1 = mailContent.length>1 ? mailContent.substring(mailContent.indexOf("subject="), mailContent.indexOf("&body")) : mailContent.join("-  ");
        mailContentPart2 = mailContent.length>1 ? mailContent.substring(mailContent.indexOf("body=")) : "";
        $(".share-tooltip-popout")[0].click();
    } else {
		/* pwc audiance internalOnly and private group case handled*/
        $(".right-title-bar .share_icons").append("<li>"+$("#navbar-toc .icon-mail").parent().parent().html()+"</li>");
        mailContent = decodeURIComponent($("#toolbar_tip_home [aria-label=Mail]").attr('href'));
        mailContentPart1 = mailContent.length>1 ? mailContent.substring(mailContent.indexOf("subject="), mailContent.indexOf("&body")) : mailContent.join("-  ");
        mailContentPart2 = mailContent.length>1 ? mailContent.substring(mailContent.indexOf("body=")) : "";
    }

}

$(document).ready(function() {
    /*code for fetching the json link from the markup*/
    
    var sliders = $(".related-slider"),
    stackLyt = $(".stacked-layout"),
    txtLyt = $(".text-layout"),
    bodycallout,stackLayout,txtLayout,i,j,k;
    for(i=0;i<sliders.length;i++){
        bodycallout = $(sliders[i]).data("bodycallout");
        if (typeof bodycallout !== "undefined" && bodycallout !== false) {
         relatedSlider(bodycallout, sliders[i]);
        }
    }

    for(j=0;j<stackLyt.length;j++){
        stackLayout = $(stackLyt[j]).data("bodycallout");
        if (typeof stackLayout !== "undefined" && stackLayout !== false) {
         stackedLayout(stackLayout,stackLyt[j]);
        }
    }

    for(k=0;k<txtLyt.length;k++){
        txtLayout = $(txtLyt[k]).data("bodycallout");
        if (typeof txtLayout !== "undefined" && txtLayout !== false) {
         stackedLayout(txtLayout,txtLyt[k]);
        }
    }

    $(document).ajaxComplete(function(event,xhr,settings){
     ajaxClamping();
     //openSidebySide();
    });
    copyMailIconToSideBySide();

});

/* Side by side view closing */
$(".backtodoc").click(function () {
	//$('.right-title-bar').show();
  var sideView = $("#sidebysideview");
  $('#left-clone-content #hilightsection').removeAttr('id');
  $("#right-clone-content,.right-title-bar .title-bar-left-side,.right-title-bar .title-bar-left-side,#left-clone-content").empty();

  $("#left-clone-content").scrollTop(0);
  sideView.hide();
  $("html, body").animate({
    scrollTop: prevScrollPosition
  }, 200);
  $('.content-page-main #hilightsection').removeAttr('id');
  if($('body').hasClass('page-vp-inloop')){
    $('.page-vp-inloop #hilightsection').removeAttr('id');
    }
  $("html").removeClass("modal-is-open");
});

