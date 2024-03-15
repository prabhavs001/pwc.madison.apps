<%--
  ADOBE CONFIDENTIAL

  Copyright 2015 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
--%><%
%><%@include file="/libs/granite/ui/global.jsp"%>
<%@include file="/libs/dam/gui/coral/components/admin/contentrenderer/base/base.jsp"%><%
%><%@page session="false"%><%
%><%@page import="java.util.Date,
                  org.apache.commons.io.FilenameUtils,
                  org.apache.jackrabbit.JcrConstants,
                  org.apache.sling.api.resource.SyntheticResource,
                  com.adobe.granite.security.user.UserPropertiesManager,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ValueMapResourceWrapper,
                  com.day.cq.dam.api.checkout.AssetCheckoutService,
                  com.day.cq.dam.commons.util.StockUtil,
                  com.day.cq.dam.commons.util.DamUtil,
                  com.day.cq.dam.commons.util.UIHelper,
                  com.day.cq.dam.commons.util.S73DHelper" %><%
%><%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0"%><%
%><cq:includeClientLib categories="dam.gui.columnrenderer.columnpreview"/><%
Config cfg = cmp.getConfig();
String path = cmp.getExpressionHelper().getString(cfg.get("path", String.class));

if (path == null) return;

Resource contentResource = resourceResolver.getResource(path);
ResourceBundle resourceBundle = slingRequest.getResourceBundle(slingRequest.getLocale());

if (contentResource == null) return;

String thumbnailUrl = "";
String displayMimeType = "";
String contentFragmentModel = "";
Asset asset = contentResource.adaptTo(Asset.class);
String title = UIHelper.getTitle(contentResource);
if (asset != null && isContentFragment(asset)) {
    title = getFragmentTitle(contentResource, title);
    displayMimeType = i18n.getVar("Content Fragment", "Display Mime Type");
    contentFragmentModel = getFragmentModel(contentResource, "");
}
String name = contentResource.getName();
String status=getStatus(contentResource);


boolean showDetailsButton = cfg.get("showDetailsButton", true);

String mimeType = "";

boolean isDirectory = false;

boolean showOriginalIfNoRenditionAvailable = cfg.get("showOriginalIfNoRenditionAvailable", false);
boolean showOriginalForGifImages = cfg.get("showOriginalForGifImages", false);
request.setAttribute("showOriginalIfNoRenditionAvailable", showOriginalIfNoRenditionAvailable);
request.setAttribute("showOriginalForGifImages", showOriginalForGifImages);
if (asset != null) {
    thumbnailUrl = getCloudThumbnailUrl(asset, 480, showOriginalIfNoRenditionAvailable, showOriginalForGifImages);
    if(thumbnailUrl == null || thumbnailUrl.isEmpty()){
        thumbnailUrl = getThumbnailUrl(asset, 480, showOriginalIfNoRenditionAvailable, showOriginalForGifImages);
    }
    // display mime type is shown on card (in grid view) and in list view
    boolean is3D = S73DHelper.isS73D(resource);

    Resource lookupResource = resourceResolver.getResource("/mnt/overlay/dam/gui/content/assets/jcr:content/mimeTypeLookup");

    //mimeType
    if (is3D) {
        Resource assetContent = resource.getChild("jcr:content");
        displayMimeType = (assetContent != null) ? assetContent.getValueMap().get("dam:s7damType", "").toUpperCase() : "";
        String extension = FilenameUtils.getExtension(resource.getName());
        if (!extension.isEmpty()) {
            displayMimeType += " (" + extension + ")";
        }
    }
    else if (asset.getMimeType() != null) {
        mimeType = asset.getMimeType();
        String ext = mimeType.substring(mimeType.lastIndexOf('/') + 1, mimeType.length());
        if((displayMimeType = UIHelper.lookupMimeType(ext,lookupResource,true)) == null) {
            displayMimeType = "";
        }
        if (displayMimeType.length() == 0 && mimeType.startsWith("image")) {
            displayMimeType = "IMAGE";
        } else if (displayMimeType.length() == 0 && mimeType.startsWith("text")) {
            displayMimeType = "DOCUMENT";
        } else if (displayMimeType.length() == 0 && (mimeType.startsWith("video") || mimeType.startsWith("audio"))) {
            displayMimeType = "MULTIMEDIA";
        } else if (displayMimeType.length() == 0 && mimeType.startsWith("application")) {
            int idx_1 = ext.lastIndexOf('.');
            int idx_2 = ext.lastIndexOf('-');
            int lastWordIdx = (idx_1 > idx_2)?idx_1:idx_2;
            displayMimeType = ext.substring(lastWordIdx+1).toUpperCase();
        }
    }

    if (displayMimeType.equals("SNIPPET")) {
        Resource assetContent = contentResource.getChild("jcr:content");
        if (assetContent.getValueMap().get(DamConstants.DAM_INDESIGN_IS_SNIPPET_TEMPLATE, false) == true) {
            displayMimeType = "SNIPPET TEMPLATE";
        }
    }

    if (displayMimeType.length() == 0 && asset.getName() != null) {
        String filename = asset.getName();
        String ext = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        if((displayMimeType = UIHelper.lookupMimeType(ext,lookupResource,true)) == null) {
            displayMimeType = "";
        }
    }
} else {
    // Path at which the manual thumbnail(if present) exists
    String manualThumbnailPath = contentResource.getPath() + "/jcr:content/manualThumbnail.jpg";
    Resource manualThumbnail = resourceResolver.getResource(manualThumbnailPath);
    if (null != manualThumbnail) {
        thumbnailUrl = Text.escapePath(manualThumbnailPath);
    } else {
        thumbnailUrl = Text.escapePath(contentResource.getPath()) + ".folderthumbnail.jpg";
    }
    isDirectory = true;
}

Locale locale = request.getLocale();
String assetDetailsVanity = "/assetdetails.html";
UserPropertiesManager upm = resourceResolver.adaptTo(UserPropertiesManager.class);

long width = 0;
long height = 0;
String size = "";
String modified = null, modifiedBy = "";

boolean isCheckedOut = false;
String checkedOutBy = "";
String formattedCheckedOutBy = "";

boolean isStockAsset = StockUtil.isStockAsset(contentResource);
String stockId = "";
String stockLicense = "";

String[] profileTitleList = new String[]{"","",""};
String[] profilePropertyList = new String[]{"jcr:content/metadataProfile", "jcr:content/imageProfile", "jcr:content/videoProfile"};
String[] profileNamePropertyList = new String[]{"jcr:content/jcr:title", "name", "jcr:content/jcr:title"};
String metadataSchemaDisplayName = "";

PublicationStatus publicationStatus = getPublicationStatus(request, contentResource, i18n);

String publicationState = null;
if (publicationStatus.isLater()) {
    publicationState = "Scheduled for Later";
}
if (publicationStatus.isPending()) {
    publicationState = "Pending";
}
if (null == publicationState && null != publicationStatus.getLastReplicationDate()) {
    publicationState = "<foundation-time type=\"datetime\" value=\"" + publicationStatus.getLastReplicationDate().toInstant().toString() + "\"></foundation-time>";
}

boolean isDeactivated = publicationStatus.getAction() != null && "UNPUBLISH".equalsIgnoreCase(publicationStatus.getAction());

Node contentRscNode = contentResource.adaptTo(Node.class);
if (!isDirectory) {
Node metadataNode = contentRscNode.getNode(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER);

try {
    width = Long.valueOf(DamUtil.getValue(metadataNode, DamConstants.TIFF_IMAGEWIDTH, DamUtil.getValue(metadataNode, DamConstants.EXIF_PIXELXDIMENSION, "")));
    height = Long.valueOf(DamUtil.getValue(metadataNode, DamConstants.TIFF_IMAGELENGTH, DamUtil.getValue(metadataNode, DamConstants.EXIF_PIXELYDIMENSION, "")));
} catch(NumberFormatException nfe) {
    //eat it as this is non-image asset
}

long bytes = Long.valueOf(DamUtil.getValue(metadataNode, "dam:size", "0"));
if (bytes == 0 && asset.getOriginal() != null) {
    bytes = asset.getOriginal().getSize();
}
if (bytes != 0) {
size = UIHelper.getSizeLabel(bytes, slingRequest);
}
    //lastModified & lastModifiedBy
        long lastModification = asset.getLastModified();
        if (lastModification == 0) {
            ValueMap contentResourceVM = contentResource.adaptTo(ValueMap.class);
            Calendar created = contentResourceVM.get("jcr:created", Calendar.class);
            lastModification = (null != created) ? created.getTimeInMillis() : 0;
        }
        modified = "<foundation-time type=\"datetime\" value=\"" + new Date(lastModification).toInstant().toString() + "\"></foundation-time>";
        String modifier = asset.getModifier();
        if (StringUtils.isNotBlank(modifier)) {
            String storedFormattedName = (String) request.getAttribute(modifier);
            if (StringUtils.isBlank(storedFormattedName)) {
                modifiedBy = AuthorizableUtil.getFormattedName(contentResource.getResourceResolver(), modifier);
                request.setAttribute(modifier, modifiedBy);
            } else {
                modifiedBy = storedFormattedName;
            }

        } else {
            modifier = "";
        }
        // Also check asset modifier should not be empty. see CQ-39542
        if (!"".equals(modifier) && upm.getUserProperties(modifier, "profile") == null) {
            modifiedBy = i18n.get("External User");
        }

    AssetCheckoutService assetCheckoutService = sling.getService(AssetCheckoutService.class);

    if (isStockAsset) {
        String propStockId = JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER + "/" + StockUtil.PN_STOCK_ID;
        if (contentRscNode.hasProperty(propStockId)) {
            stockId = contentRscNode.getProperty(propStockId).getValue().getString();
        }
        String propStockLicense = JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER + "/" + StockUtil.PN_STOCK_LICENSE;
        if (contentRscNode.hasProperty(propStockLicense)) {
            stockLicense = contentRscNode.getProperty(propStockLicense).getValue().getString();
        }
    }

} else {
     // Processing profile details
    for(int i=0; i<3; i++) {
        if (contentRscNode.hasProperty(profilePropertyList[i])) {
            String profilePath = contentRscNode.getProperty(profilePropertyList[i]).getValue().getString();
            if(profilePath.trim().isEmpty()){
                continue;
            }
            Resource res = resourceResolver.getResource(profilePath);
            if (res != null) {
                Node node = res.adaptTo(Node.class);
                if (node != null) {
                    profileTitleList[i] = node.getName();
                    if (node.hasProperty(profileNamePropertyList[i])) {
                        String jcrTitle = node.getProperty(profileNamePropertyList[i]).getValue().getString();
                        if (jcrTitle != null && !jcrTitle.trim().isEmpty()) {
                            profileTitleList [i] = jcrTitle;
                        }
                    }
                }
            }
        }
    }

    // Metadata Schema details
    String metadataSchema = "";
    if (contentRscNode.hasProperty("jcr:content/metadataSchema")) {
        metadataSchema = contentRscNode.getProperty("jcr:content/metadataSchema").getValue().getString();
        if(!metadataSchema.isEmpty() && metadataSchema.indexOf("/metadataschema/") != -1) {
            metadataSchemaDisplayName = metadataSchema.substring(metadataSchema.indexOf("/metadataschema/") + ("/metadataschema/").length());
        }
    }

}




%><coral-columnview-preview><coral-columnview-preview-content>
    <%
   String metaRT = cfg.get("metaResourceType", String.class);
   if (metaRT != null) {
        %><sling:include resource="<%= contentResource %>" resourceType="<%= metaRT %>" /><%
   }
   %>
    <coral-columnview-preview-asset>
        <img src="<%= xssAPI.getValidHref(thumbnailUrl) %>" alt="<%=xssAPI.encodeForHTMLAttr(UIHelper.getAltText(contentResource))%>">
    </coral-columnview-preview-asset>
    <coral-columnView-preview-label><%= i18n.get("Title") %></coral-columnView-preview-label>
    <coral-columnview-preview-value><%= xssAPI.encodeForHTML(title) %></coral-columnview-preview-value><%
    if (name != null && !name.equals(title)) {%>
        <coral-columnView-preview-label><%= i18n.get("Name") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.encodeForHTML(name) %></coral-columnview-preview-value><%
    }
    if (modified != null && modifiedBy!= null && modifiedBy.trim().length() > 0) {
             %><coral-columnView-preview-label><%= i18n.get("Modified") %></coral-columnView-preview-label>
            <coral-columnview-preview-value><%= xssAPI.filterHTML(modified) %></coral-columnview-preview-value>

            <coral-columnView-preview-label><%= i18n.get("Modified By") %></coral-columnView-preview-label>
            <coral-columnview-preview-value><%= xssAPI.encodeForHTML(modifiedBy) %></coral-columnview-preview-value><%
    } %>

    <%if (!isDirectory && isCheckedOut) {
            %><coral-columnView-preview-label><%= i18n.get("Checked Out By") %></coral-columnView-preview-label>
            <coral-columnview-preview-value><%= xssAPI.encodeForHTML(formattedCheckedOutBy) %></coral-columnview-preview-value><%
    }%>

    <%if (width != 0 && height != 0) {
        %><coral-columnView-preview-label><%= i18n.get("Dimensions") %></coral-columnView-preview-label>
          <coral-columnview-preview-value><%= xssAPI.encodeForHTML(width + " x " + height+" px") %></coral-columnview-preview-value><%
    }%>

    <%if (publicationState != null && !isDeactivated) {%>
        <coral-columnView-preview-label><%= i18n.get("Publication") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.filterHTML(i18n.getVar(publicationState)) %></coral-columnview-preview-value>
    <%}%>

    <%if (publicationState != null && isDeactivated) {%>
        <coral-columnView-preview-label><%= i18n.get("Un-publication") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.filterHTML(i18n.getVar(publicationState)) %></coral-columnview-preview-value>
    <%}%>

        <%if (size.length() > 0) {    %>
          <coral-columnView-preview-label><%= i18n.get("Size") %></coral-columnView-preview-label>
              <coral-columnview-preview-value><%= size %></coral-columnview-preview-value><%
         }%>

    <%if (!isDirectory && displayMimeType != null) {
        %><coral-columnView-preview-label><%= i18n.get("Type") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.encodeForHTML(displayMimeType) %></coral-columnview-preview-value><%
    }%>
    <%if (!isDirectory && isStockAsset) {
        %><coral-columnView-preview-label><%= i18n.get("Source") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= i18n.get("Adobe Stock") %></coral-columnview-preview-value><%
    }%>
    <%if (!isDirectory && !stockId.isEmpty()) {
        %><coral-columnView-preview-label><%= i18n.get("Adobe Stock File#") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= stockId %></coral-columnview-preview-value><%
    }%>
    <%if (!isDirectory && !stockLicense.isEmpty()) {
        %><coral-columnView-preview-label><%= i18n.get("Adobe Stock License") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= stockLicense %></coral-columnview-preview-value><%
    }%>    
    <%if (isDirectory && !profileTitleList[0].trim().isEmpty()) {
        %><coral-columnView-preview-label><%= i18n.get("Metadata Profile") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.encodeForHTML(profileTitleList[0].trim()) %></coral-columnview-preview-value><%
    }%>
    <%if (isDirectory && !profileTitleList[1].trim().isEmpty()) {
        %><coral-columnView-preview-label><%= i18n.get("Image Profile") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.encodeForHTML(profileTitleList[1].trim()) %></coral-columnview-preview-value><%
    }%>
    <%if (isDirectory && !profileTitleList[2].trim().isEmpty()) {
        %><coral-columnView-preview-label><%= i18n.get("Video Profile") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.encodeForHTML(profileTitleList[2].trim()) %></coral-columnview-preview-value><%
    }%>
    <%if (isDirectory && !metadataSchemaDisplayName.isEmpty()) {
        %><coral-columnView-preview-label><%= i18n.get("Metadata Schema") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.encodeForHTML(metadataSchemaDisplayName) %></coral-columnview-preview-value><%
    }
    if (!contentFragmentModel.isEmpty()) {
        %><coral-columnView-preview-label><%= i18n.get("Model") %></coral-columnView-preview-label>
        <coral-columnview-preview-value><%= xssAPI.encodeForHTML(contentFragmentModel) %></coral-columnview-preview-value><%
    }%>
    <%if (status!=null) { %>
    	<coral-columnView-preview-label>Status</coral-columnView-preview-label>
    	<coral-columnview-preview-value style="color:red"><%= xssAPI.encodeForHTML(status) %></coral-columnview-preview-value><%
    }%>
    <%

        Tag tag = cmp.consumeTag();
    AttrBuilder metaAttrs = tag.getAttrs();
    metaAttrs.addBoolean("hidden", true);
    metaAttrs.addClass("foundation-collection-assets-meta");
    metaAttrs.add("data-foundation-collection-meta-title", title);
    metaAttrs.add("data-foundation-collection-meta-folder", isDirectory);

    AttrBuilder imgAttrs = new AttrBuilder(request, xssAPI);
    imgAttrs.addClass("foundation-collection-meta-thumbnail");
    imgAttrs.addHref("src", thumbnailUrl);

    %><div <%= metaAttrs %>>
        <img <%= imgAttrs %>>
    </div><%
    if (!isDirectory) {
        String assetDetailsLink = getAssetDetailLink(request, slingRequest);
        Resource wrapper = new ValueMapResourceWrapper(resource, "granite/ui/components/coral/foundation/collection/action") {
            public Resource getChild(String relPath) {
                if ("data".equals(relPath)) {
                    SyntheticResource sres = new SyntheticResource(this.getResourceResolver(), "dummy", null);
                    Resource dataWrapper = new ValueMapResourceWrapper(sres, "granite/ui/components/coral/foundation");
                    ValueMap dataVm = dataWrapper.adaptTo(ValueMap.class);
                    dataVm.put("href", assetDetailsLink );
                    return dataWrapper;
                } else {
                    return super.getChild(relPath);
                }
            }
        };
        ValueMap vm = wrapper.adaptTo(ValueMap.class);
        vm.put("granite:rel", "dam-asset-column-preview");
        vm.put("text", i18n.get("More Details"));
    %><% if(showDetailsButton) { %>
            <div id="asset-details-link-wrapper">
            <sling:include resource="<%= wrapper %>"/>
            </div><%
        }
    }
    %>
</coral-columnview-preview-content></coral-columnview-preview><%!

private boolean isContentFragment(Asset asset) {
    Resource resource = asset.adaptTo(Resource.class);
    Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
    boolean isFragment = false;
    if (contentResource != null) {
        ValueMap contentProps = contentResource.adaptTo(ValueMap.class);
        isFragment = contentProps.get("contentFragment", false);
    }
    if (isFragment) {
        // check if editor is available - otherwise, handle as normal asset
        ResourceResolver resolver = resource.getResourceResolver();
        Resource editorRsc =
                resolver.getResource("/libs/dam/cfm/admin/content/fragment-editor");
        isFragment = (editorRsc != null);
    }
    return isFragment;
}

private String getFragmentThumbnailUrl(Asset asset) {
    String thumbnailUrl;
    Resource resource = asset.adaptTo(Resource.class);
    Resource thumbnailRsc = resource.getChild("jcr:content/thumbnail.png");
    if (thumbnailRsc != null) {
        // use the existing thumbnail
        Calendar createdCal = thumbnailRsc.getValueMap()
                .get(JcrConstants.JCR_CREATED, Calendar.class);
        Resource contentResource = thumbnailRsc.getChild(JcrConstants.JCR_CONTENT);
        Calendar lastModifiedCal = createdCal;
        if (contentResource != null) {
            lastModifiedCal = contentResource.getValueMap()
                    .get(JcrConstants.JCR_LASTMODIFIED, createdCal);
        }
        long lastModified =
                (lastModifiedCal != null ? lastModifiedCal.getTimeInMillis() : -1);
        thumbnailUrl = Text.escapePath(thumbnailRsc.getPath()) + "?_ck=" + lastModified;
    } else {
        // default thumbnail
        thumbnailUrl = Text.escapePath("/libs/dam/cfm/admin/content/static/thumbnail_fragment.png");
    }
    return thumbnailUrl;
}

private String getFragmentTitle(Resource resource, String defaultTitle) {
    Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
    ValueMap props = contentResource.getValueMap();
    return props.get("jcr:title", defaultTitle);
}

private String getFragmentModel(Resource resource, String defaultType) {
    Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
    ValueMap props = contentResource.getValueMap();
    String typePath = props.get("data/cq:model", "");
    if( typePath.length() > 0 ) {
        Resource typeResource = resource.getResourceResolver().getResource(typePath);
        if( typeResource != null && typeResource.getChild(JcrConstants.JCR_CONTENT) != null) {
            typeResource = typeResource.getChild(JcrConstants.JCR_CONTENT);
            props = typeResource.adaptTo(ValueMap.class);
            return props.get("jcr:title", "");
        }
        return typePath;
    }
    return defaultType;
}
private String getStatus(Resource res) {
	 Resource metaRes = res.getChild("jcr:content/metadata");
	 if(null!=metaRes){
		 return metaRes.getValueMap().get("pwc-content-status", String.class);
	 }
	 return null;
}

private String getAssetDetailLink(HttpServletRequest request, SlingHttpServletRequest slingRequest) {
    String contextPath = request.getContextPath();
    String contentPath = slingRequest.getRequestPathInfo().getSuffix();
    String assetDetailLink = contextPath + "/assetdetails.html" + contentPath;
    return assetDetailLink;
}
%>
