// This file is replacement of "/libs/dam/components/scene7/common/clientlibs/viewer/js/viewerUtils.js"
//  with a check to not load VideoViewer.js from scene7 server.

/*
 * Utility class for promise viewer loader
 */
var S7dmUtils = S7dmUtils || {};

(function(S7dmUtils){
    S7dmUtils = S7dmUtils || {};

    /**
     * Viewer Loader Helper
     */
    S7dmUtils.Viewer = function(){};

    //Max retry for viewer readiness check
    S7dmUtils.Viewer.RETRY_JS_MAX = 50;

    //retry count
    S7dmUtils.Viewer.prototype.retryCount = 0;

    //list of viewers to load
    S7dmUtils.Viewer.prototype.viewerList = null;

    /**
     * @param viewerList viewer list JSON object
     * @return current S7dmUtils.Viewer object
     */
    S7dmUtils.Viewer.prototype.load = function(viewerList, viewerRootPath){
        //Check the viewer loader first so we don't load viewer multiple time
        if (!this.isAllLoaded(viewerList)) {
            this.viewerList = viewerList;
            for (var viewer in viewerList) {
            	//Commenting below condition because we want the VideoViewer.js to be loaded from Scene7 instead of custom ref. MD-12723
                //if(viewer !== "VideoViewer"){
                	$('head').append('<script type="text/javascript" src="' + viewerRootPath + viewerList[viewer] + '"></script>');
                //}
            }
        }
        return this;
    }

    /**
     * Check viewer readiness with retry-loop.
     * When the viewer is ready, the subsequent function for success case gets called;
     * otherwise, we call fail case.
     * @param fn function to deal with success/fail viewer loader in format { success: fn1, fail: fn2}
     */
    S7dmUtils.Viewer.prototype.ready = function(fn){
        if (typeof s7viewers != 'undefined' && (this.viewerList == null || this.isAllLoaded(this.viewerList))) {
            fn.success.call();
        }
        else { //retry until the viewers are all loaded or reached max load time
            var $this = this;
            this.retryCount++;
            if (this.retryCount < S7dmUtils.Viewer.RETRY_JS_MAX) {
                setTimeout(function(){ $this.ready(fn) }, 100);
            }
            else {
                fn.fail.call();
            }
        }
    }


    /**
     * @private
     * @param viewerList viewer list with key as viewer constructor (s7viewers.*)
     * @return true when all the viewers in the list are loaded.
     */
    S7dmUtils.Viewer.prototype.isAllLoaded = function(viewerList) {
        //check basic s7viewers object first - if none, no viewer
        if (typeof s7viewers == 'undefined') {
            return false;
        }
        for (var viewer in viewerList) {
            //check viewer against the list - if one of them is not loaded, then it's not all loaded.
            if (viewer == "Responsive") {
                if (typeof s7responsiveImage == 'undefined') {
                    return false;
                }
            } else if (typeof s7viewers[viewer] == 'undefined') {
                return false;
            }
        }
        return true;
    }
})(S7dmUtils);
