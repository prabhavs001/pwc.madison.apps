var url, TOC = {},
  chapterToc,
  nextPageLink = $('.cp-nav-item--next'),
  backToLink = $('.cp-nav-item--prev'),
  navBarShow = $('.content-page-nav'),
  tocFound = false,
  hashVal = location.hash,
  HASH_TOP_S = '-tOp-S',
  nonJoinUrl, join, joinurl,
  joinedLink = $('meta[name="pwcJoinedPagePath"]').attr("content"),
  pwcPageId = $('meta[name="pwcPageId"]').attr("content"),
  pageTitle = $(document).find("title").text(),
  joinLevel = $('meta[name="pwcJoinedLevel"]').attr("content"),
  checkedVal = '',
  p2 = $('.goto-truncate-link').get(0);
  responseData=[]
  isNoIdCase=false;

function appendAnchor(anchorTag, joinurl, node_link, currentLevel, nodeId) {
  if (window.sessionStorage.getItem("_joinLevel") && joinurl && currentLevel >= window.sessionStorage.getItem("_joinLevel")) {
    //if existing node link comes with a hash value
    if (node_link.indexOf("#") !== -1) {
      var hashindex = node_link.indexOf("#");
      anchorTag.href = joinurl + node_link.substring(hashindex);
    } else {
      //else add append nodeId with the url 
      if (nodeId) {
        anchorTag.href = joinurl + "#" + nodeId;
      } else {
        isNoIdCase = true;
        anchorTag.href = joinurl;
      }
    }
  } else {
    if(window.location.href.indexOf('joined.html') !== -1){
        if (node_link.indexOf("#") !== -1) {
            var hashindex = node_link.indexOf("#");
            anchorTag.href = joinurl + node_link.substring(hashindex);
          } else {
            //else add append nodeId with the url 
            if (nodeId) {
              anchorTag.href = joinurl + "#" + nodeId;
            } else {
              isNoIdCase = true;
              anchorTag.href = joinurl;
            }
          }
    }
    else{
    anchorTag.href = node_link.replace("/dt/", "/content/pwc-madison/ditaroot/");
    }
  }
  return anchorTag;
} 

TOC.creatMenuTree = function (parent, data, level) {
  
  $.each(data, function (item,ele) {
      
    var divElement = document.createElement('div'),
      anchorTag = document.createElement('a'),
      currentLevel, node_link, toc_path, buttonElement, checkPath, hashindex;
    divElement.className = 'toc-item';
    if (level === "parent-level") {
      divElement.className += ' parent-item index-' + item;
    }
    anchorTag.className = 'expand-link';
    anchorTag.setAttribute('data-level', this.level);
    node_link = this.nodeLink;
    toc_path = this.tocPath;
    if (node_link.startsWith("/dt")) {
      node_link = node_link.replace("/dt", "/content/pwc-madison/ditaroot");
    }
    checkPath = toc_path ? toc_path.startsWith("/dt") : false;
    if (checkPath) {
      toc_path = toc_path.replace("/dt", "/content/pwc-madison/ditaroot");
    }
    anchorTag.setAttribute('data-nodelink', node_link);
    anchorTag.setAttribute('data-toc', this.toc);
    anchorTag.setAttribute('data-title', this.title);
    anchorTag.setAttribute('data-target', this.pageName);
    anchorTag.setAttribute('data-tocpath', toc_path);
    anchorTag.setAttribute('data-pwcdoccontext', this.pathHash);
    anchorTag.setAttribute('data-opentoc', 0);
    anchorTag.setAttribute('data-haschild', this.hasChildren);
    currentLevel = this.level;
    if (this.joinedSectionUrl) {
      join = this.joinedSectionUrl;
      joinurl = join.replace("/dt/", "/content/pwc-madison/ditaroot/");
    }
    if (this.hasChildren || this.childLinks) {
      buttonElement = document.createElement('button');
      buttonElement.innerHTML = '<img src="/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg" alt="Expand">';
      // anchorTag.href = window.DOMPurify.sanitize(node_link);
      anchorTag.innerHTML = this.title;
      divElement.append(buttonElement);
      anchorTag = appendAnchor(anchorTag, joinurl, node_link, currentLevel, this.nodeId);
      divElement.append(anchorTag);

      TOC.creatMenuTree(divElement, this.childLinks, undefined);
    } else {
      // anchorTag.href = window.DOMPurify.sanitize(node_link);
      anchorTag = appendAnchor(anchorTag, joinurl, node_link, currentLevel, this.nodeId);
      anchorTag.innerHTML = this.title;
      divElement.append(anchorTag);
    }

    return parent.append(divElement);

  });

};

function isScrollRequiredCTC(hashUri) {
  try {
    var reg = new RegExp('^(.*)' + HASH_TOP_S + '$');
    return reg.test(hashUri);
  } catch (error) {
    return false;
  }
  return false;
}

// on load highlighted tree menu 
function getActiveLink(hashId) {
    hashVal = window.location.hash;
    if (isScrollRequiredCTC(hashVal)) {
      hashVal = hashVal.substring(0, hashVal.indexOf(HASH_TOP_S));
    }
     // C1-code responsible for highlighting TOC in case of custom URL
     customUrlEle=document.getElementById(hashVal.split('#').join(''));
     if($(customUrlEle).hasClass('anchor-id') && $(customUrlEle).parents('.doc-body-content').attr('id')){
         hashVal='#'+ $(customUrlEle).parents('.doc-body-content').attr('id');
     }
     // End C1
    var linkPath = location.pathname,
      current, target, image, sibs, firstParent;
    if (linkPath.indexOf("/content/pwc-madison/ditaroot") < 0 && linkPath.startsWith("/dt") >= 0) {
      linkPath = linkPath.replace("/dt", "/content/pwc-madison/ditaroot");
    }
    if(hashId){
        hashVal='#' + hashId;
    }
    current = linkPath + hashVal;
    target = $('.toc-content').find('[href="' + current + '"]');
    if (target.length > 0) {
      activateLink(target,image,sibs,firstParent);
      $(".stick-nav-title").text($("#navbar-toc .toc-content .toc-container .toc-item a.expand-link.active").text());   //On page load, sticky nav title will pick selected TOC title
    }else{
         let result = $('.toc-content').find('[href*="' + current + '"]');
         if (result.length > 0) {
           let firstElehref = result.attr('href'); // for activate mobile anchor also
           let targetNode = $('.toc-content').find('[href="' + firstElehref + '"]');
           activateLink(targetNode, image, sibs, firstParent);
         }
     }
  }
  
if ($("#is-chapter-toc").val() === 'true') {
  $(".toc-content").on("click", ".toc-item > button", function (event) {
    TOC.onClickFunction(event);
  });
}

$(".toc-content").on("click", ".toc-item > a", function (event) {
  $(".stick-nav-title").text($(this).text());   //On click TOC title, sticky nav title will pick selected TOC title
    var desireScroll=this.hash?$(document.getElementById(this.hash.substr(1))).offset().top:null,currentScroll=$(window).scrollTop();
  if(desireScroll<currentScroll){
    // scroll up
    $('#content-page-full-container .doc-body-content').removeClass('top-bottom');
    $('#content-page-full-container .doc-body-content').addClass('bottom-top');
  }
  else{
    // scroll down
    $('#content-page-full-container .doc-body-content').removeClass('bottom-top');
    $('#content-page-full-container .doc-body-content').addClass('top-bottom');
  }
  });

TOC.onClickFunction = function (event) {

  var $this, tocpath, opentoc, getOnLoadUrl, newDiv, $loader, tree, getNodeLink, panel, isOpen;
  $this = $(event.currentTarget);
  tocpath = $this.siblings('a').attr('data-tocpath');
  opentoc = $this.siblings('a').attr('data-opentoc');
  getOnLoadUrl = url.substring(0, url.length - 14);
  if (tocpath !== "undefined" && tocpath !== getOnLoadUrl && opentoc === '0') {

    newDiv = "<div class='toc-item w-loader'><div class='loader'></div></div>";
    $loader = $this.parent().find("a").after(newDiv);

    $(".loader").css({
      "width": "30px",
      "height": "30px"
    });

    getNodeLink = $this.siblings('a').attr("data-nodelink");
    opentoc = $this.siblings('a').attr('data-opentoc', 1);

    $.ajax({
      url: tocpath + '.fetchtoc.json',
      type: 'GET',
      dataType: 'json',
      success: function (response) {
        var i, node_link;
        for (i in response.childLinks) {
          node_link = response.childLinks[i].nodeLink;
          if (node_link.startsWith("/dt")) {
            node_link = node_link.replace("/dt", "/content/pwc-madison/ditaroot");
          }
          if (node_link === getNodeLink) {
            tree = response.childLinks[i].childLinks;
            // recursively tree-menu function calling               
            TOC.creatMenuTree($this.parent(), tree, undefined);
          }
        }
        $(".w-loader").remove();
        $(".loader").remove();
        $this.parent().children('.toc-item').css("display", "flex");
      },
      complete: function (response) {
        if ($this.parent(".parent-item").length) {
          var klass = $this.parent(".parent-item").attr('class'),
            arr = klass.split(/\s+/),
            currentElem = '.' + arr.join('.'),
            thisClone = $this.parent(".parent-item").clone();
          $(".searchdoc-v2").find(currentElem).replaceWith(thisClone);
        }
      }
    });
  }

  isOpen = $this.children('img').attr("src") === '/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/collapse.svg' ? true : false;
  panel = $this.siblings("div.toc-item");
  if (isOpen === false) {
    $this.children('img').attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/collapse.svg");

  } else {
    $this.children('img').attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg");
  }
  panel.slideToggle("fast");
  panel.css("display", "flex");

};

window.TOC = TOC;

function showPreviousNext(childLinks, linkPath) {
  $.each(childLinks, function (key, value) {
    if (tocFound) {
      return false;
    }
    // handle Chuncked paths
    if (value.nodeLink.indexOf("#") !== -1) {
      value.nodeLink = value.nodeLink.substring(0, value.nodeLink.indexOf("#"));
    }
    if (linkPath === value.nodeLink) {
      tocFound = true;
      if (value.toc) {
        nextPageLink.show();
        backToLink.show();
        navBarShow.show();
        backToLink.addClass('show');

        /* Using clamp.js to clip next link and add elipsis */
        if (p2) {
          $clamp(p2, {
            clamp: 1
          });
        }

        /* show seperator */
        $('.backto-link').parent().prev().css('border-right', 'solid 1px #7d7d7d');
      }
      return false;
    } else {
      showPreviousNext(value.childLinks, linkPath);
    }
  });
}


function filterArray(arr,activeUrl) {
    // this function return only current selected node for join view 
    return arr.filter(obj => {
      if (obj.joinedSectionUrl && activeUrl.includes(obj.joinedSectionUrl)) {
        responseData.push(obj);
        return obj;
      } else {
        // Check childLinks array recursively
        if (obj.childLinks) {
          obj.childLinks = filterArray(obj.childLinks,activeUrl);
          return obj.childLinks.length > 0;
        }
        return false;
      }
    });
  }
  

TOC.changeAnchorUrl = function(data){
    data.forEach((ele,index) => {
    // change href for rest of node
    var node_link=ele.nodeLink,joinurl=ele.joinedSectionUrl,nodeId=ele.nodeId
    if (node_link.indexOf("#") !== -1) {
        var hashindex = node_link.indexOf("#");
        data[index].nodeLink = joinurl + node_link.substring(hashindex);
      } else {
        //else add append nodeId with the url 
        if (nodeId) {
            data[index].nodeLink = joinurl + "#" + nodeId;
        } else {
            isNoIdCase = true;
            data[index].nodeLink = joinurl;
        }
      }
      if (ele.hasChildren || ele.childLinks) {
        TOC.changeAnchorUrl(ele.childLinks);
    }      
    });
    return data;
    
}

function appendDefaultId(){
    // get all default id present in join page 
    var IDs = [],anchorTags;
    $('.topic.doc-body-content').each(function(){ IDs.push(this.id); });

    // append ids in all toc anchor

    anchorTags = $('#content-page-full-container .toc-content.is-hidden-mobile.is-hidden-tablet-only .toc-item > a');
    for(let i=0;i<IDs.length;i++){
        let currentAnchorhref=$(anchorTags[i]).attr('href'),rightTitle,leftTitle=$(anchorTags[i]).attr('data-title');
        if(i === 0 && IDs[i] === pwcPageId){
            rightTitle = $(document.getElementById(IDs[i])).find('.title>h1').text()
        }
        else{
            rightTitle = $(document.getElementById(IDs[i])).find('h1.title').text()
        }
        if(rightTitle.trim() == leftTitle.trim() && currentAnchorhref.indexOf('#') === -1){
        $(anchorTags[i]).attr('href',currentAnchorhref + '#' + IDs[i])
        }
    }

}


$(document).ready(function () {
  var tocChapterPage = $(".content-page-main").attr("data-toc-chapter-page"),
    tocBasePage = $(".content-page-main").attr("data-toc-base-page"),
    linkPath = location.pathname,
    getTocList;

  chapterToc = $("#is-chapter-toc").val();
  if (chapterToc === 'true' && tocChapterPage !== undefined) {
    url = tocChapterPage + '.fetchtoc.json';
    $.ajax({
      url: url,
      type: 'GET',
      dataType: 'json',
      success: function (response) {

        if(window.location.href.indexOf('joined.html') !== -1){
            var activeUrl = location.pathname,filteredData;
            if (activeUrl.startsWith("/content/pwc-madison/ditaroot")) {
                activeUrl = activeUrl.replace("/content/pwc-madison/ditaroot", "/dt");
            }
            filteredData = filterArray(response.childLinks,activeUrl)
            TOC.creatMenuTree($('.toc-container noindex'), responseData, "parent-level");
            if(isNoIdCase){
                appendDefaultId();
            }
          }
          else{
        TOC.creatMenuTree($('.toc-container noindex'), response.childLinks, "parent-level");
          }
        getActiveLink();

        if ($('.searchdoc-v2').length > 0) {
          if (window.searchDoc !== undefined) {
            window.searchDoc.init();
            $(".searchdoc-loader").remove();
          }
        }
        if (linkPath.startsWith('/content/pwc-madison/ditaroot')) {
          linkPath = linkPath.replace("/content/pwc-madison/ditaroot", "/dt");
        }
        showPreviousNext(response.childLinks, linkPath);

        $(".loader,.loader-text").remove();
      }
    });
  } else {
    Handlebars.registerHelper('tocHelper', function (info) {
      var template = Handlebars.compile($('script#toc-content-template').html());
      return template(info);
    });

    /**
     * Populates the toc links after the DOM is loaded so that,
     * toc loading will happen in the background after other components are loaded
     */
    getTocList = function () {
      var pagePath, panel, image, sibs, imageStatus, tocLink, customEvent;
      if (isScrollRequiredCTC(hashVal)) {
        hashVal = hashVal.substring(0, hashVal.indexOf(HASH_TOP_S));
      }
      if (linkPath.indexOf("/content/pwc-madison/ditaroot") < 0 && linkPath.startsWith("/dt") >= 0) {
        linkPath = linkPath.replace("/dt", "/content/pwc-madison/ditaroot");
      }
      pagePath = tocBasePage;
      if (pagePath) {
        $(".searchdoc-loader").show();
        $.get(pagePath + ".fetchtoc.json").then(function (tocJson) {
          $(".loader,.loader-text").remove();
          var tocContainer = $(".toc-content"),
            tocListTemplateElement = $("#toc-content-template").html(),
            tocListTemplate = Handlebars.compile(tocListTemplateElement),
            tocListHtml;

            //   addressing MD-16551 Join View TOC
          if(window.location.href.indexOf('joined.html') !== -1){
            var activeUrl = location.pathname,filteredData;
            if (activeUrl.startsWith("/content/pwc-madison/ditaroot")) {
                activeUrl = activeUrl.replace("/content/pwc-madison/ditaroot", "/dt");
            }
            filteredData = filterArray(tocJson.childLinks,activeUrl)
            tocJson.childLinks = TOC.changeAnchorUrl(responseData);
           
          }

          // Register the list partial that main template uses.
          Handlebars.registerPartial("list", $("#toc-item-list-template").html());

          Handlebars.registerHelper('absPath', function (link) {
            if (link && link.startsWith("/dt")) {
              return link.replace("/dt", "/content/pwc-madison/ditaroot");
            } else {
              return link;
            }
          });
        
          tocListHtml = tocListTemplate(tocJson);

          tocContainer.append(tocListHtml);
          if(isNoIdCase && window.location.href.indexOf('joined.html') !== -1){
            appendDefaultId();
        }
          $('.toc-title').html($('#toc-title').val());
          if (linkPath.startsWith('/content/pwc-madison/ditaroot')) {
            linkPath = linkPath.replace("/content/pwc-madison/ditaroot", "/dt");
          }
          if(window.location.href.indexOf('joined.html') === -1){
          showPreviousNext(tocJson.childLinks, linkPath);
          }

          $(function () {
            var linkPath = location.pathname,
              current, target, image, sibs, isOpen, panel, container, firstParent, hashVal;
             hashVal = window.location.hash;
             if (isScrollRequiredCTC(hashVal)) {
               hashVal = hashVal.substring(0, hashVal.indexOf(HASH_TOP_S));
             }
              // C1-code responsible for highlighting TOC in case of custom URL
              customUrlEle=document.getElementById(hashVal.split('#').join(''));
              if($(customUrlEle).hasClass('anchor-id') && $(customUrlEle).parents('.doc-body-content').attr('id')){
                  hashVal='#'+ $(customUrlEle).parents('.doc-body-content').attr('id');
              }
              // End C1
            if (linkPath.indexOf("/content/pwc-madison/ditaroot") < 0 && linkPath.startsWith("/dt") >= 0) {
              linkPath = linkPath.replace("/dt", "/content/pwc-madison/ditaroot");
            }
            current = linkPath + hashVal;
            target = $('.toc-content').find('[href="' + current + '"]');

            if (target.length > 0) {
              activateLink(target,image,sibs,firstParent);
              $(".stick-nav-title").text($("#navbar-toc .toc-content .toc-container .toc-item a.expand-link.active").text());   //On page load, sticky nav title will pick selected TOC title
            }else{
                 let result = $('.toc-content').find('[href*="' + current + '"]');
                 if (result.length > 0) {
                   let firstElehref = result.attr('href'); // for activate mobile anchor also
                   let targetNode = $('.toc-content').find('[href="' + firstElehref + '"]');
                   activateLink(targetNode, image, sibs, firstParent);
                 }
             }

            $('.toc-content .toc-item > button').on('click', function () {
              isOpen = $(this).children('img').attr("src") === '/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg' ? true : false;
              panel = $(this).siblings("div.toc-item");

              if (isOpen === false) {
                $(this)
                  .children('img')
                  .attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/expand.svg");
              } else {
                $(this)
                  .children('img')
                  .attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/collapse.svg");
              }

              panel.slideToggle("fast");
              panel.css("display", "flex");
            });
            /*if ($('.toc-content').length > 0) {
                container = document.getElementsByClassName('toc-content')[0];
                document.addEventListener('click', function(event) {
                    if ($(event.target).hasClass('icon-ToC') === false) {
                        if (container !== event.target && !container.contains(event.target)) {
                            if ($('.toc-content').css('display') === 'block') {
                                toggleToc();
                            }
                        }
                    }
                });
            }*/
          });

          if (linkPath.startsWith('/dt')) {
            linkPath = linkPath.replace("/dt", "/content/pwc-madison/ditaroot");
          }

          $("#navbar-toc").find(".toc-item a[href^='" + linkPath + hashVal + "']").last().parents('.toc-item').children('button').trigger('click');
          $("#toc-mobile-content").find(".toc-item a[href^='" + linkPath + hashVal + "']").last().parents('.toc-item').children('button').trigger('click');

          tocLink = $('.toc-content').find('[href="' + linkPath + hashVal + '"]');
          if (tocLink.length > 0) {
            $(tocLink).addClass('active');

            panel = $(tocLink).parents("div.toc-item");
            image = $(panel).siblings("button");
            sibs = $(image).siblings("div.toc-item");
            imageStatus = $(image)
              .children('img')
              .attr("src");
            sibs.css("display", "flex");
          }
          if ($('.searchdoc-v2').length > 0) {
            customEvent = document.createEvent("CustomEvent");
            customEvent.initCustomEvent('TOCLoaded', false, false, {});
            document.dispatchEvent(customEvent);
          }
        });
      }
    };
    getTocList();
  }

});

function checkSearchbox() {
  if ($('#cst-search').val().length) {
    $('.close-cst').css({
      'display': 'block'
    });
    $('.arrow').css({
      'display': 'inline-block'
    });
    $('#cst-search').css({ 'width': '61%' })

  } else {
    $('.arrow').css({
      'display': 'none'
    });
    $('.close-cst').css({
      'display': 'none'
    });
    $('#cst-search').css({ 'width': '100%' })
  }
}

//To Highlight TOC when we use "Search within page" functionality
function debounce(func, timeout = 1000){   //Used debounce to exicute stacked sequence order, after every certain interval of time
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => { func.apply(this, args); }, timeout);
  };
}
function saveInput(){
  changeTocActive();
  $('#cst-search').focus();   //To re-focuse on input box after every letter typed out
}
const processChanges = debounce(() => saveInput());   //Exicute both functions

var finder = {
  content: '#content-page-full-container .content-page-container', // Replace that id with your seaching container

  wrapper: '#icon-Search-btn',

  scrollOffset: function () {
    return 0;
  },

  activate: function () {
    setTimeout(function () {
      $('#icon-search-box').addClass('active');
      $('#cst-search').focus();
      if ($('#cst-search').val()) {
        finder.findTerm($('#cst-search').val());
      }
      $('#cst-search').on('input', function () {
        if ($(this).val().length>2){
        finder.findTerm($(this).val());
        }
        else{
          $(finder.content).unhighlight();
        }
      });
    }, 50);
  },

  closeFinder: function () {
    $(finder.content).unhighlight();
    $('#cst-search').val('');
    checkSearchbox();
  },

  resultsCount: 0,

  currentResult: 0,

  findTerm: function (term) {
    // highlight results
    $(finder.content).unhighlight();
    $(finder.content).highlight(term);

    // count results
    finder.resultsCount = $('.highlight').length;

    if (finder.resultsCount) {
      // there are results, scroll to first one
      finder.currentResult = 1;
      finder.scrollToCurrent();
    } else {
      // no results
      finder.currentResult = 0;
    }

    // term not found
    if (!finder.resultsCount && term) {
      $('#finderInput').addClass('not-found');
    } else {
      $('#finderInput').removeClass('not-found');
    }

    finder.updateCurrent();
  },

  scrollToCurrent: function (btnName) {
    var i = finder.currentResult - 1;
    $('.highlight').removeClass('active');
    $('.highlight:eq(' + i + ')').addClass('active');
    if(btnName === 'next' && findDeviceType()){
        $('html, body').animate({
        scrollTop: $(".highlight.active").offset().top - 100
        });
    }
    else{
        $('html, body').animate({
            scrollTop: $(".highlight.active").offset().top - 200
        });
    }
    
   
  },

  prevResult: function () {
    if (finder.resultsCount) {
      if (finder.currentResult > 1) {
        finder.currentResult--;
      } else {
        finder.currentResult = finder.resultsCount;
      }
      finder.scrollToCurrent('prev');
    }

    finder.updateCurrent();
  },

  nextResult: function () {
    if (finder.resultsCount) {
      if (finder.currentResult < finder.resultsCount) {
        finder.currentResult++;
      } else {
        finder.currentResult = 1;
      }
      finder.scrollToCurrent('next');
    }

    finder.updateCurrent();
  },

  updateCurrent: function () {
    if ($('#finderInput').val()) {
      if (!$('#finderCount').length) {
        var countElem = $('<span />')
          .attr({
            'id': 'finderCount',
            'class': 'finder-count'
          })
          .insertAfter('#finderInput');
      }
      setTimeout(function () {
        $('#finderCount').text(finder.currentResult + ' / ' + finder.resultsCount);
      }, 50);
    } else {
      $('#finderCount').remove();
    }
  }
};

function findDeviceType() {
  // return true if device is desktop
  return window.matchMedia("(min-width: 1088px)").matches;
}

function isIPhone() {
   return navigator.userAgent.includes('iPhone');
 }

function showHideSearchbox() {

  // e.preventDefault();
  if ($('#icon-search-box:visible').length) {
    finder.closeFinder();
    $('#icon-search-box').hide();
    $('.icon-search-active').removeClass('icon-search-active');
  }
  else {
    $('#icon-search-box').show();
    $('#icon-Search-btn').addClass('icon-search-active');
  }
}

$(document).ready(function () {
  var isDesktop, con, content, elem;
  isDesktop = findDeviceType();
  if (window.sessionStorage.getItem("_joinLevel")) {
    checkedVal = "checked";
  }
  if (isDesktop) {
    // Remove Empty li from DOM
    $('#navbar-toc .share_icons li').each(function () {
      if (!$(this)[0].childElementCount) {
        $(this).remove();
      }
    })
    // End Remove Empty li from DOM
  }
  if (window.location.href.indexOf('joined.html') === -1) {
	// $('.favorites-toggle').remove();
    if (!joinedLink) { // when join is disable 
       // mobile
       $('.quick-links .icon-Join').hide();
     }
     else {
       // apend join btn in share tool box for desktop
       $('#navbar-toc .icon-Join').parent().show();
     }
     
  }
  if (window.location.href.indexOf('joined.html') !== -1) {
	// remove favourite button 
    // $('.favorites-toggle').remove();
    $('.badge-favorited-container').remove();
    $('section .container .main-body-content .title').addClass('join-page-header');
    $('body').addClass('join-page');
    // End remove favourite button
    if (isDesktop) {
      $('.toc-action-navbar .icon-share.share-tooltip-popout').parents('li').hide();
      $('.toc-action-navbar .icon-link.link-tooltip-popout').parents('li').hide();
	  $('.toc-action-navbar .icon-mail').parents('li').hide();
      $('.share_icons .favorites-toggle').parents('li').hide();
	  $('#navbar-toc .icon-Join').parent().hide();
      $('.stick-nav-title').remove();
      // replacing existing search within document with search in page icon
	  if ($('.icon-magnifier-document').parents('li').length){
        $('.icon-magnifier-document').parents('li').replaceWith('<li><a id="icon-Search-btn" href="javascript:void(0);" data-title="'+ Granite.I18n.get("Join_View_Search_Placeholder") + '"  class="icon-Search"></a></li>');
      }
      else{
        $('#navbar-toc .share_icons').prepend('<li><a id="icon-Search-btn" href="javascript:void(0);" data-title="'+ Granite.I18n.get("Join_View_Search_Placeholder") + '"  class="icon-Search"></a></li>')
      }
      //End  replacing existing search within document with search in page icon

      // apending search 
      con = '<div id="icon-search-box" ><input id="cst-search" type="text" name="q" placeholder="'+ Granite.I18n.get("Join_View_Search_Placeholder") + '" onkeyup="processChanges()"><i class="arrow up"  onclick="processChanges()"></i><i class="arrow down"  onclick="processChanges()"></i><i class="close-cst"></i></div>';
      $('.icon-Search').parent().prepend(con);
      $('.icon-Search').parent().css({
        'display': 'flex'
      });
      // end appending search
      // Implement Title content for sticky toc
      content = '<div id="navbar-title" class="joint-navbar-title">';
      //content += '<div class="join-container"><span class="join-title"><b>' + pageTitle + '</b></span> <span class="nav-bar-close-line"></span> <span class="nav-bar-close">'+ Granite.I18n.get("Join_View_Close_Button") + '</span></div>';
      content += '<div class="join-container"><span class="join-title"><b>' + pageTitle + '</b></span></div>';
      content += '<span class="join-desc">'+ Granite.I18n.get("Join_View_Read_Only_Message") + '</span>';
      content += '</div>';
      $('#navbar-toc').children('div .toc-action-navbar').children('div .toc_icon').append(content);
      // End Title Implementation
      // css changes
      $('.toc_icon').css({
        'min-width': '85%',
        'width': '85%',
        'flex': '0 0 0%'
      });
      $('.icon-Search').parent().parent('ul.share_icons').parent().css({
        'margin-left': 'auto'
      });
      $('.arrow').css({
        'display': 'none'
      });
      $('.close-cst').css({
        'display': 'none'
      });
      // End css changes
    } else {
      // Mobile view
      $('.toc-mobile-bar>div').css({
        'width': '100%'
      });
      $('#toolsWrapper').hide();
      $('#rclWrapper').hide();
      $('.quick-links .icon-Join').hide();
      elem = '<div id="navbar-title-mobile"><div>';
      elem += '<ul class="share_icons_mobile">';
      elem += '<li><a id="icon-Search-btn" href="javascript:void(0);" data-title="'+ Granite.I18n.get("Join_View_Search_Placeholder") + '"  class="icon-Search"></a></li>';
      //elem += '<li><a href="#" onclick="printPDF(this);" data-title="download"  class="icon-download"></a></li>';
      if(!isIPhone()){
         elem += '<li><a href="#" onclick="print_info_modal();" data-title="print"  class="icon-print"></a></li>';
       }
      //elem += '<span class="nav-bar-close_mobile">'+ Granite.I18n.get("Join_View_Close_Button") + '</span></ul></div></div>';
      elem += '</ul></div></div>';
      $('#toolsWrapper').parent().append(elem);
      con = '<div id="icon-search-box" ><input id="cst-search" type="text" name="q" placeholder="'+ Granite.I18n.get("Join_View_Search_Placeholder") + '"><i class="arrow up"></i><i class="arrow down"></i><i class="close-cst"></i></div>';
      $('.share_icons_mobile').parent().append(con);
	  // appending Join-view-Header title 
      content = '<div id="navbar-title-mob" class="joint-navbar-title-mobile">';
      content += '<div class="join-container-mobile"><span class="join-title-mobile"><b>' + pageTitle + '</b></span></div>';
      content += '<span class="join-desc-mobile">'+ Granite.I18n.get("Join_View_Read_Only_Message") + '</span>';
      content += '</div>';
      $('#navbar-title-mobile').append(content);
      // end appending join view header title
      adjustContentHeader();
    }
  }
});

// dynamically adjust content header 

function adjustContentHeader() {
  var stickyHeaderHeight = $('.toc-mobile-bar').height();
  $('.main-body-content .content-page-main').css({ 'padding-top': stickyHeaderHeight + 10 });
}

//end of dynamically adjust content header 

$('#animatedTooles').parent().on("click", ".nav-bar-close_mobile", function (e) {
  window.close();
});

$("#navbar-toc").on("click", ".nav-bar-close", function (e) {
  window.close();
});

// Search Within Page Fuctionality

$(document).ready(function () {
  $('#icon-Search-btn').click(function () {
	adjustContentHeader();
	finder.activate();
  });

  $(document).mousedown(function (event) {
    if (event.which === 1) {
      switch ($(event.target).attr('class') || $(event.target).parents().attr('class')) {
        case 'close-cst':
          finder.closeFinder();
          break;
        case 'arrow up':
          finder.prevResult();
          break;
        case 'arrow down':
          finder.nextResult();
          break;
        case 'icon-Search':
          showHideSearchbox();
          break;
        case 'icon-Search icon-search-active':
          showHideSearchbox();
          break;
        default:
          return true;
      }
    }
  });
});

// Highligting Result

jQuery.extend({
  highlight: function (node, re, nodeName, className) {
    var match, highlight, wordNode, wordClone, i;
    if (node.nodeType === 3) {
      match = node.data.match(re);
      if (match) {
        highlight = document.createElement(nodeName || 'span');
        highlight.className = className || 'highlight';
        wordNode = node.splitText(match.index);
        wordNode.splitText(match[0].length);
        wordClone = wordNode.cloneNode(true);
        highlight.appendChild(wordClone);
        wordNode.parentNode.replaceChild(highlight, wordNode);
        return 1; //skip added node in parent
      }
    } else if ((node.nodeType === 1 && node.childNodes) && // only element nodes that have children
      !/(script|style)/i.test(node.tagName) && // ignore script and style nodes
      !(node.tagName === nodeName.toUpperCase() && node.className === className)) { // skip if already highlighted
      for (i = 0; i < node.childNodes.length; i++) {
        i += jQuery.highlight(node.childNodes[i], re, nodeName, className);
      }
    }
    return 0;
  }
});

jQuery.fn.unhighlight = function (options) {
  var settings = {
    className: 'highlight',
    element: 'span'
  };
  jQuery.extend(settings, options);

  return this.find(settings.element + "." + settings.className).each(function () {
    var parent = this.parentNode;
    parent.replaceChild(this.firstChild, this);
    parent.normalize();
  }).end();
};

jQuery.fn.highlight = function (words, options) {
  var settings = {
    className: 'highlight',
    element: 'span',
    caseSensitive: false,
    wordsOnly: false
  }, flag, pattern, re, matchregx;
  jQuery.extend(settings, options);

  if (words.constructor === String) {
    words = [words];
  }
  words = jQuery.grep(words, function (word, i) {
    return word !== '';
  });
  words = jQuery.map(words, function (word, i) {
    return word.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
  });
  if (words.length === 0) {
    return this;
  }

  flag = settings.caseSensitive ? "" : "i";
  pattern = "(" + words.join("|") + ")";
  if (settings.wordsOnly) {
    pattern = "\\b" + pattern + "\\b";
  }
  re = new RegExp(pattern, flag);

  return this.each(function () {
    jQuery.highlight(this, re, settings.element, settings.className);
  });
};


//  End Search Within Page Fuctionality

$('body').on('keyup', '#cst-search', function (e) {
  checkSearchbox();
});


// function which trigger on click of join button 
 function joinbtnclick() {
   // check if node link has pageid nnnn
   var currentUrl = $(".toc-open .toc-content .toc-container .toc-scrollto a.expand-link.active").attr('href')||window.location.href , hashindex;
   if (currentUrl.indexOf("#") !== -1) {
     hashindex = currentUrl.indexOf("#");
     window.open(
       joinedLink + currentUrl.substring(hashindex),
       '_blank' // <- This is what makes it open in a new window.
     );
   } else {
     //else add append nodeId with the url 
     if (joinedLink.indexOf("#") === -1) {
       if (pwcPageId) {
         window.open(
           joinedLink + "#" + pwcPageId,
           '_blank' // <- This is what makes it open in a new window.
         );
       } else {
         window.open(
           joinedLink,
           '_blank' // <- This is what makes it open in a new window.
         );
       }

     }
   }
 }
 


$(window).on('hashchange', function (e) {
  //hilight toc link when hash link changes 
  getActiveLink();
  if ($(".toc-open .toc-content .toc-container").hasClass('hasScrolled')) {
    let activeEle=$(".toc-open .toc-content .toc-container .toc-scrollto a.expand-link.active")

    if(activeEle.length !== 0){
        $(".toc-open .toc-content .toc-container").scrollTo(activeEle);
    }
}
});

function activateLink(ele, image, sibs, firstParent){
  if (ele.length > 0) {
    $('.toc-content .active').removeClass('active');
    ele.each(function (index, t) {
      $(t).addClass('active');
      firstParent = $(t).parent(".toc-item").parent('.toc-item');

      $(firstParent).addClass('toc-scrollto');

      $(t).parents("div.toc-item").each(function () {
        image = $(this).siblings("button");
        sibs = $(image).siblings("div.toc-item");

        $(image).children('img').attr("src", "/etc.clientlibs/pwc-madison/clientlibs/clientlib-site-vp/resources/images/collapse.svg");
        sibs.css("display", "flex");

      });
    });
  }
}

var lastActiveEle;
function changeTocActive(event) {
        // Handle the scroll event here
        if($('.toc-container .toc-item').length && !$('.content-page-main').hasClass('no-toc')){
            const box =document.getElementById('navbar-toc'),virtualBoxLwrLmt = box.getBoundingClientRect().bottom,virtualBoxUprLmt = virtualBoxLwrLmt + 90;
        // const divs  =  $('.content-page-container').find('.title').parent().filter((index,ele)=>ele.hasAttribute('id') && $(ele).hasClass('topic doc-body-content'));
        const divs  =  $('.content-page-container').find('.topic.doc-body-content').filter((index,ele)=>ele.hasAttribute('id') && $(ele).hasClass('topic doc-body-content'));
        for (let i = 0; i < divs.length; i++) {
            const currentContainer = divs[i],currentEleLwrLmt = currentContainer.getBoundingClientRect().top,currentEleUprLmt = currentContainer.getBoundingClientRect().bottom;
          if ( currentEleLwrLmt >= virtualBoxLwrLmt && currentEleLwrLmt <= virtualBoxUprLmt && currentEleLwrLmt < window.innerHeight) {
            if( window.location.hash==='#'+currentContainer.id){
                getActiveLink();
            }
            window.location.hash='#'+currentContainer.id;
        
            return false
          }
         
        }
        for (let i = divs.length-1; i >=0 ; i--) {
            const currentContainer = divs[i],currentEleLwrLmt = currentContainer.getBoundingClientRect().top,currentEleUprLmt = currentContainer.getBoundingClientRect().bottom;
            if ( currentEleLwrLmt <= virtualBoxUprLmt && currentEleUprLmt >= virtualBoxLwrLmt && currentEleLwrLmt < window.innerHeight) {
                if(lastActiveEle === currentContainer.id){
                    return false;
                }
                getActiveLink(currentContainer.id);
                if (window.location.href.indexOf('joined.html') == -1) {
                    $('.stick-nav-title').text($(".toc-open .toc-content .toc-container .toc-scrollto a.expand-link.active").attr('data-title'));
                    } 
                if ($(".toc-open .toc-content .toc-container").hasClass('hasScrolled')) {
                  let activeEle=$(".toc-open .toc-content .toc-container .toc-scrollto a.expand-link.active");
              
                  if(activeEle.length !== 0){
                      $(".toc-open .toc-content .toc-container").scrollTo(activeEle);
                  }
              }
              lastActiveEle = currentContainer.id;                    
              return false
            }
         
        }
        
    }
      }
      const scrollDetected = debounceScroll(() => executeElementDetectionMethod());
      window.addEventListener('wheel', scrollDetected); // Mouse or touchpad scroll
      window.addEventListener('keyup', function(event) {
        if (event.code === 'ArrowUp' || event.code === 'ArrowDown') {
            scrollDetected(event); // Keyboard up/down keys
        }
      });
    
    
    
      // function for scrollbar click detection
    var clickedOnScrollbar = function(mouseX){
    if( $(window).innerWidth() <= mouseX && $(window).outerWidth() >= mouseX){
      return true;
    }
    }
    
    // detect click for mousedown event
    $(document).mousedown(function(e){
    if( clickedOnScrollbar(e.clientX) ){
        scrollDetected();
    }
    });

  $('.toc-resize').mousedown(function(e) {
    e.preventDefault();
    $(document).mousemove(function(ex) {
        scrollDetected();
    });
  });

  $(window).on('resize', function() {
    scrollDetected();
});

function debounceScroll(func, timeout = 300){   //Used debounce to exicute stacked sequence order, after every certain interval of time
    let timer;
    return (...args) => {
      clearTimeout(timer);
      timer = setTimeout(() => { func.apply(this, args); }, timeout);
    };
  }
  function executeElementDetectionMethod(){
    changeTocActive();
    
  }

      

