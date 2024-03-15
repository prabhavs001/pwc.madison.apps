(function ($, $document) {
    var FOUNDATION_CONTENT_LOADED = "foundation-contentloaded",
        FOUNDATION_COLLECTION_ID = "foundation-collection-id",
        LAYOUT_CARD_VIEW = "card",
        EAEM_BANNER_CLASS = "eaem-banner",
        EAEM_BANNER = ".eaem-banner",
        FOUNDATION_COLLECTION_ITEM_ID = "foundation-collection-item-id",
        DAM_ADMIN_CHILD_PAGES = ".cq-damadmin-admin-childpages";
 
    $document.on(FOUNDATION_CONTENT_LOADED, setContentStatus);  
    
    
 
    function setContentStatus(){
        var folderPath = $(DAM_ADMIN_CHILD_PAGES).data(FOUNDATION_COLLECTION_ID);
 
        if(_.isEmpty(folderPath)){
            return;
        }
 
        $.ajax(folderPath + ".3.json").done(updateContentStatusCardView);
    }
 
    function updateContentStatusCardView(pathsObj){
        
        if(_.isEmpty(pathsObj)){
            return;
        }
 
        if(isCardView()){
            addCardViewBanner(pathsObj);
        }
    }
 
 
    function getBannerMissingLinksHtml(assetObj){
        var contentStatus = nestedPluck(assetObj,"jcr:content/metadata/pwc-content-status");
        
        if(_.isUndefined(contentStatus)){
            return;
        }
        return "<coral-card-property style='color:red' title='Content Status' class='"+ EAEM_BANNER_CLASS +"'>" +
        	"<coral-card-property-content>" + contentStatus + "</coral-card-property-content>" +
        "</coral-card-property>" ;
    }
    
    function addCardViewBanner(pathsObj){
        var $container = $(DAM_ADMIN_CHILD_PAGES), $item, assetPath,
            folderPath = $container.data(FOUNDATION_COLLECTION_ID);
 
        _.each(pathsObj, function(assetObj, assetName){
            if(_.isString(assetObj) && isJCRProperty(assetName)){
                return;
            }
 
            assetPath = folderPath + "/" + assetName;
 
            $item = $container.find("[data-" + FOUNDATION_COLLECTION_ITEM_ID + "='" + assetPath + "']");
 
            if(_.isEmpty($item)){
                return;
            }
 
            if(!_.isEmpty($item.find(EAEM_BANNER))){
                return;
            }
            
            $item.find("coral-card-content").append(window.DOMPurify.sanitize(getBannerMissingLinksHtml(assetObj)));
        });
    }
 
    function isCardView(){
        return (getAssetsConsoleLayout() === LAYOUT_CARD_VIEW);
    }
 
    function getAssetsConsoleLayout(){
        var $childPage = $(DAM_ADMIN_CHILD_PAGES),
            foundationLayout = $childPage.data("foundation-layout");
 
        if(_.isEmpty(foundationLayout)){
            return "";
        }
 
        return foundationLayout.layoutId;
    }
 
 
    function startsWith(val, start){
        return val && start && (val.indexOf(start) === 0);
    }
 
    function isJCRProperty(property){
        return (startsWith(property, "jcr:") || startsWith(property, "sling:"));
    }
 
    function nestedPluck(object, key) {
        if (!_.isObject(object) || _.isEmpty(object) || _.isEmpty(key)) {
            return [];
        }
 
        if (key.indexOf("/") === -1) {
            return object[key];
        }
 
        var nestedKeys = _.reject(key.split("/"), function(token) {
            return token.trim() === "";
        }), nestedObjectOrValue = object;
 
        _.each(nestedKeys, function(nKey) {
            if(_.isUndefined(nestedObjectOrValue)){
                return;
            }
 
            if(_.isUndefined(nestedObjectOrValue[nKey])){
                nestedObjectOrValue = undefined;
                return;
            }
 
            nestedObjectOrValue = nestedObjectOrValue[nKey];
        });
 
        return nestedObjectOrValue;
    }
}($, $(document)));