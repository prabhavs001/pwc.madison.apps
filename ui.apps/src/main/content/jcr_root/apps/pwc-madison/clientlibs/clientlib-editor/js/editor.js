(function($) {
    "use strict";
   var PLACEHOLDER_TEXT, clk, interval=1000, $editor;

   PLACEHOLDER_TEXT = [{
            element: "callout-content",
            value: "<Call out can be added using these elements. Add content here.>"
        },
        {
            element: "pwc-observation",
            value: "<Observation can be added using this element. Add text here.>"
        },
        {
            element: "excerpt-content",
            value: "<Excerpt can be added using these elements. Add content here.>"
        },
        {
            element: "key-points",
            value: "<Keypoints can be added using these elements. Add text or bullet points here.>"
        },
        {
            element: "callout-title",
            value: "<Callout title>"
        },
        {
            element: "excerpt-title",
            value: "<Excerpt title>"
        }
    ];

   function initPlaceholder() {
	    $editor = $("iframe.cke_wysiwyg_frame").contents().find("body");

	    PLACEHOLDER_TEXT.forEach(function(elm) {

	        $editor.find("." + elm.element).attr("data-tcx-placeholder", elm.value);

	    });

	    $('body').on("click keypress", $editor, function(e) {
	        $(e.target).removeClass("tcx-empty-element");
	    });

	    PLACEHOLDER_TEXT.forEach(function(elm) {
	        var elem = $editor.find("." + elm.element);
	        if (elem.length > 0) {
	            if (elm.element === 'key-points' && elem[0].innerHTML === '<br>') {
	                elem.addClass("show-placeholder");
	            } else if (!elem[0].textContent) {

	                elem.addClass("show-placeholder");
	            }
	        }
	    });
	}


	$('body').on("click", 'button[title="Insert Element"]', function() {
	    $("iframe.cke_wysiwyg_frame").contents().find("body").click(function() {
	        //initPlaceholder();
	    });

	});
    
    clk = setInterval(function() {
        if ($("iframe.cke_wysiwyg_frame").contents().find("body").length) {
            var iframe = document.getElementsByClassName('cke_wysiwyg_frame')[0],
            innerDoc = iframe.contentDocument || iframe.contentWindow.document,
            relatedDiv = innerDoc.getElementsByClassName('related-content'), i;
            for(i = 0; i < relatedDiv.length; i++){
                relatedDiv[i].style.display = "none"; 
            }
            clearInterval(clk);
            setTimeout(function() {
                //initPlaceholder();
            }, interval / 2);
        }
    }, interval);

}(jQuery));

