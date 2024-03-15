use(['../pwc_dm_utils.js'], function(utils){
    var METADATA_NODE = com.day.cq.dam.api.s7dam.constants.S7damConstants.S7_ASSET_METADATA_NODE,
    	SCENE7_FILE_PROP = METADATA_NODE + "/metadata/dam:scene7File",
        fileReference = properties['./fileReference'],
        rootNode = currentSession.rootNode,
        assetID="",
        AUDIO_LENGTH_PROP = METADATA_NODE + "/metadata/dam:Length",
        length = "",
        mediaInfo = {};
    
    var isWCMDisabled = (com.day.cq.wcm.api.WCMMode.fromRequest(request) == com.day.cq.wcm.api.WCMMode.DISABLED);

    var assetNode = rootNode.getNode(fileReference.substring(1));

    if( assetNode.hasProperty(SCENE7_FILE_PROP)) {
        assetID = assetNode.getProperty(SCENE7_FILE_PROP).getString();
    }

    if(assetNode.hasProperty(AUDIO_LENGTH_PROP)) {
        length = assetNode.getProperty(AUDIO_LENGTH_PROP).getLong();
        if(length) {
            length = utils.getDuration(length);
        }
    }

    if (!isWCMDisabled || properties["./assetID"]=="") {
    	var props = resource.adaptTo(org.apache.sling.api.resource.ModifiableValueMap);
    	//Handle read only access and add only if assetID not present
    	if(props != undefined && !props.containsKey("assetID")){
    		props.put("assetID", assetID);    		
    	}
        resource.getResourceResolver().commit();
    }else {
    	assetID = properties["./assetID"];
    }


    return{
        placeholder: {
        	css: "cq-placeholder "+com.day.cq.wcm.api.components.DropTarget.CSS_CLASS_PREFIX + "image ",
            text: component.title
    	},
        assetID: assetID,
        title: utils.title,
        revisedDate: utils.revisedDate,
        length: length
    };

});
