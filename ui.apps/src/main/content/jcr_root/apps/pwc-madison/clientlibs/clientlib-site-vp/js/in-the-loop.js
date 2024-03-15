
/*
    logic related to article template
*/
var allElementIds;
function highlightEle(element) {
    // <<----------------highlight selected Ele in Progress indicator---------------->>
    var currentHighlightedEle = $('.progress-indicator-list-items .progress-indicator-list-item.active');
    currentHighlightedEle.removeClass('active');

    element.addClass('active');
    // <<-----------------------End-------------------------->>
}

function findTargetEle(hashId) {
// <<-------Responsible for finding element id in progress indicator bar------------->>
    var elementList = $('li.progress-indicator-list-item'), targetEle;

    targetEle = elementList.filter(function (index, ele) {
        return $(ele).find('a').attr('href') === hashId;
    });

    if (targetEle) {
        highlightEle(targetEle);
    }
    // <<-------End------------->>

}

function getAllElementsWithId(){
    // <<----------Responsible for finding wrappers with the same ID as the one currently displayed in the progress indicator.--------------->>
    var progressIndicatorWrapper = $('#progress-indicator a'),idList,wrapperList;
    idList = progressIndicatorWrapper.map(function(index,ele){return ele.hash.substring(1);});
    wrapperList = idList.map(function(i,eleme){return $('#'+eleme)[0];});
    return wrapperList;

}

function scrollingElementDetectionMethod(event) {
    // <<-------Responsible for finding element id that is in viewport & append it in url------------->>
    var navbar = document.getElementById('navbar-toc'), virtualBoxLwrLmt = navbar.getBoundingClientRect().bottom, virtualBoxUprLmt = virtualBoxLwrLmt + 90,
        curentElementInsideBox, elementId, currentEleTop, currentEleBottom;
    navbar = document.querySelector('.aem-component-toolbar');
    virtualBoxLwrLmt = navbar.getBoundingClientRect().bottom;
    virtualBoxUprLmt = virtualBoxLwrLmt + 80;

    
    curentElementInsideBox = allElementIds.filter(function (index, ele) {
        currentEleTop = ele.getBoundingClientRect().top;
        currentEleBottom = ele.getBoundingClientRect().bottom;
        if((currentEleTop > virtualBoxLwrLmt && currentEleTop < virtualBoxUprLmt && currentEleTop < window.innerHeight)){
            return true;
        }else if(currentEleTop < virtualBoxLwrLmt && currentEleBottom > virtualBoxLwrLmt ){
            return true;
        }else{
            return false;
        }
    });
    if (curentElementInsideBox.length) {
        elementId = curentElementInsideBox[0].getAttribute('id');
        if (elementId) {
            history.pushState({}, "", '#' + elementId);
            findTargetEle('#' + elementId);
            return false;
        }
    }
}

function showHideProgressIndicator() {
    var progressIndicator, teaserCompoent;
    teaserCompoent = $('.in-the-loop-template .teaser');
    progressIndicator = $('.progress-indicator');

    if (teaserCompoent.length) {
        if (teaserCompoent[0].getBoundingClientRect().bottom < 156) {
            progressIndicator.addClass('show');
        } else {
            progressIndicator.removeClass('show');
        }
    } else if($(window).scrollTop() !== 0) {

        progressIndicator.addClass('show');
    }
    else{
        progressIndicator.removeClass('show');
    }
    
}

$(document).on('click', 'a.progress-indicator-link', function (event) {
    var currentClickedEle = $(event.target).parents('li');
    highlightEle(currentClickedEle);
});

$(document).ready(function () {
    // <<-----------------------Progress Indicator events--------------------->>
    if ($('body').hasClass('page-vp-inloop') && $('#progress-indicator').length) {

        var currentHashId = window.location.hash,clickedOnScrollbar;

        if (currentHashId) {
            findTargetEle(currentHashId);
        }

        $(window).on('hashchange', function (e) {
            currentHashId = window.location.hash;
            findTargetEle(currentHashId);
        });

        allElementIds = getAllElementsWithId();



        // <<----------------User Manual Scroll Event---------------------------->>

        window.addEventListener('wheel', function () {
            scrollingElementDetectionMethod();
        }); // Mouse or touchpad scroll
        window.addEventListener('keyup', function (event) {
            if (event.code === 'ArrowUp' || event.code === 'ArrowDown') {
                scrollingElementDetectionMethod(); // Keyboard up/down keys
            }
        });

        // function for scrollbar click detection
         clickedOnScrollbar = function (mouseX) {
            if ($(window).innerWidth() <= mouseX && $(window).outerWidth() >= mouseX) {
                return true;
            }
        };

        // detect click for mousedown event
        $(document).mousedown(function (e) {
            if (clickedOnScrollbar(e.clientX)) {
                scrollingElementDetectionMethod();
            }
        });

        $(window).on('resize', function () {
            showHideProgressIndicator();
            scrollingElementDetectionMethod();
        });

        $(window).on('scroll', function () {
            showHideProgressIndicator();
        });

        // <<-------------Mobile progress indicator button click----------------->>
        $(document).on('click','.icon-container',function(){
            var iconSpan = document.querySelector('.icon-container span');

            iconSpan.classList.toggle('dots');
            iconSpan.classList.toggle('cross');
            $('.progress-indicator-wrapper').toggle();

        });

    }

    // <<-----------------------------END------------------------------------>>

});



