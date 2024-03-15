$(document).ready(function () {
    "use strict";
    var checkDom, stopDomCheck = function () {
        clearInterval(checkDom);
    };

    function updateEditorHref(url){
        if(url !== undefined){
            url = "/libs/fmdita/clientlibs/xmleditor/page.html?src=" + encodeURIComponent(url);
            $('#editonxmleditor').attr('onclick', "window.location.href=\'"+ url + "\'");
        }
    }

    /* update xml editor link on click of left rail */
    $('body').on('click', '.item', function() {
        if(!$(this).hasClass('not-to-review')){
            updateEditorHref($(this).attr('data-topic-href'));
        }
    });

    /* update xml editor link on click of main content section */
    $('body').on('click', '.topic-placeholder', function() {
        updateEditorHref($(this).attr('data-href'));
    });

    checkDom = setInterval(function () {
        var baseUrl = $('base').attr('href'),
            contentPath,
            isDomLoaded = $("#controls .center2").length,
            isItemsLoaded = 0, topic;

            if(baseUrl !== undefined && baseUrl !== 'null'){
              if(baseUrl.endsWith('.ditamap')){
                  baseUrl = $(".item.selected").attr('data-topic-href');
                  isItemsLoaded = $('toc-element').find('.item.selected').length;
              }else{
                  baseUrl = $('base').context.baseURI;
                  isItemsLoaded = 1;
              }
            }else if(baseUrl === 'null'){
                isItemsLoaded = 1;
                if($('.topic-placeholder').length > 0){
                    topic = $('.topic-placeholder')[0];
                    baseUrl = $(topic).attr('data-href');
                }
            }
        $("#controls .left").trigger("click");
        if (baseUrl && isDomLoaded > 0 && isItemsLoaded > 0) {
            contentPath = baseUrl.split("/content/")[1];
            stopDomCheck();
            contentPath = encodeURIComponent("/content/" + contentPath);
            $.get("/bin/pwc-madison/checkuserpermission", {
                asset: contentPath,
                permission: "write"
            }).then(function (hasPermission) {
                if (hasPermission !== 'false') {
                    contentPath = "/libs/fmdita/clientlibs/xmleditor/page.html?src=" + contentPath;
                    $("#controls .center2").append("<button id=\"editonxmleditor\" onclick=\"window.location.href='" + contentPath + "'\" _ngcontent-voi-1=\"\" class=\" controlButton   coral-Button  coral-Button--quiet\"><i _ngcontent-voi-1=\"\" class=\"coral-Icon coral-Icon--edit\"></i>Edit in xml editor</button>");
                }
            });
        }
    }, 500);

});
