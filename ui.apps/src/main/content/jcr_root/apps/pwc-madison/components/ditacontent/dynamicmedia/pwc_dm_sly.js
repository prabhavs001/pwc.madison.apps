"use strict";

use(['/libs/dam/components/scene7/dynamicmedia/dynamicmedia_sly.js','../pwc_dm_utils.js'], function(dmUse,utils){


    var assetPath = properties['fileReference'],
            assetResource = resource.getResourceResolver().getResource(assetPath),
            duration = "",
            contentId = "",
            revisedDate = "",
            publicationDate = "",
            title = "";

    duration = Packages.com.pwc.madison.core.util.MadisonUtil.getDuration(assetResource);

    dmUse.duration = duration ? utils.getDuration(duration) : "";
    dmUse.thumbnailPath = (getPosterImagePath() === "") ? getImageServerURL(dmUse.isWCMDisabled, dmUse.assetID, dmUse.isRemoteAsset) + properties["./assetID"] : getPosterImagePath();
    dmUse.title = utils.title;
    dmUse.revisedDate = utils.revisedDate;
    return dmUse;
});

var POSTER_IMAGE_PATH = "/jcr:content/renditions/poster.png";

/**
 * @return Poster image path if exists
 */
function getPosterImagePath(){
    var imagePath = "",
        absPosterPath = properties['./fileReference'] + POSTER_IMAGE_PATH,
 	    assetResource = resource.getResourceResolver().getResource(absPosterPath);
    if (assetResource) {
		imagePath = assetResource.getPath();
    }
    return imagePath;
}

function getImageServerURL(isWCMDisabled, assetPath, isRemote){
    var imageServerURL = "";
    if (isWCMDisabled) {
		imageServerURL = properties['imageserverurl'];
    }
    else {
        imageServerURL = request.contextPath + "/is/image/";
        // get publish server
        var assetResource = resource.getResourceResolver().getResource(assetPath);
        if(isRemote && assetResource) {
        	return sling.getService(com.day.cq.dam.api.s7dam.utils.PublishUtils).externalizeImageDeliveryAsset(assetResource, "");
        }
    }
    return imageServerURL;
}

