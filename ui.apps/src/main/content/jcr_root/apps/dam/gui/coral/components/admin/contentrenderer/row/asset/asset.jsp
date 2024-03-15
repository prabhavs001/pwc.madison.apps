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
--%>
<%
%><%@page import="org.apache.sling.api.resource.Resource,
                 com.day.cq.dam.commons.util.UIHelper,
                 java.util.List,com.day.cq.dam.scene7.api.constants.Scene7Constants"%><%
%><%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0"%><%
%><%@include file="/libs/dam/gui/coral/components/admin/contentrenderer/base/init/assetBase.jsp"%><%
%><%@include file="/libs/dam/gui/coral/components/admin/contentrenderer/base/insightBase.jsp"%><%
%><%@include file="/libs/dam/gui/coral/components/admin/contentrenderer/row/common/common.jsp"%><% 
boolean showOriginalIfNoRenditionAvailable = (request!=null && request.getAttribute("showOriginalIfNoRenditionAvailable")!=null) ? (Boolean)request.getAttribute("showOriginalIfNoRenditionAvailable") : false;
boolean showOriginalForGifImages = (request!=null && request.getAttribute("showOriginalForGifImages")!=null) ? (Boolean)request.getAttribute("showOriginalForGifImages") : false;
boolean isOmniSearchRequest = request.getAttribute(IS_OMNISEARCH_REQUEST) != null ? (boolean) request.getAttribute(IS_OMNISEARCH_REQUEST) : false;
boolean isSnippetRequest = request.getAttribute(IS_SNIPPET_REQUEST) != null ? (boolean) request.getAttribute(IS_SNIPPET_REQUEST) : false;
String thumbnailUrl = getCloudThumbnailUrl(asset, 48, showOriginalIfNoRenditionAvailable, showOriginalForGifImages);
if(thumbnailUrl == null || thumbnailUrl.isEmpty()){
    thumbnailUrl = request.getContextPath() + requestPrefix
            + getThumbnailUrl(asset, 48, showOriginalIfNoRenditionAvailable, showOriginalForGifImages) + "?ch_ck=" + ck + requestSuffix;
}
//Override default thumbnail for set when there is manual thumbnail defined
if (dmSetManualThumbnailAsset != null) {
    thumbnailUrl = getCloudThumbnailUrl(asset, 1280, showOriginalIfNoRenditionAvailable, showOriginalForGifImages);
    if(thumbnailUrl == null || thumbnailUrl.isEmpty()) {
        thumbnailUrl = request.getContextPath() + requestPrefix
                + getThumbnailUrl(asset, 1280, showOriginalIfNoRenditionAvailable, showOriginalForGifImages) + "?ch_ck=" + ck + requestSuffix;
    }
} else if (dmRemoteThumbnail != null && !dmRemoteThumbnail.isEmpty()) {
    thumbnailUrl = dmRemoteThumbnail + "?wid=48&ch_ck=" + ck + requestSuffix;
}

String assetActionRels = StringUtils.join(
  UIHelper.getAssetActionRels(
    UIHelper.ActionRelsResourceProperties.create(isAssetExpired, isSubAssetExpired, isContentFragment, 
      isArchive, isSnippetTemplate, isDownloadable, isStockAsset, isStockAssetLicensed, isStockAccessible, canFindSimilar),
    UIHelper.ActionRelsUserProperties.create(hasJcrRead, hasJcrWrite, hasAddChild, canEdit, canAnnotate, isDAMAdmin),
    UIHelper.ActionRelsRequestProperties.create(isOmniSearchRequest,isLiveCopy)), 
  " ");

request.setAttribute("actionRels", actionRels.concat(" " + assetActionRels));
if (allowNavigation) {
attrs.addClass("foundation-collection-navigator");
}
%>
<cq:include script="link.jsp"/>
<%

    if (request.getAttribute("com.adobe.assets.card.nav")!=null){
        navigationHref =  (String) request.getAttribute("com.adobe.assets.card.nav");
    }
attrs.add("data-foundation-collection-navigator-href", xssAPI.getValidHref(navigationHref));

attrs.add("is", "coral-table-row");
attrs.add("data-item-title", resourceTitle);
attrs.add("data-item-type", type);

com.adobe.granite.workflow.status.WorkflowStatus workflowStatus = resource.adaptTo(com.adobe.granite.workflow.status.WorkflowStatus.class);
List<com.adobe.granite.workflow.exec.Workflow> workflows = workflowStatus.getWorkflows(false);

request.setAttribute("com.adobe.assets.meta.attributes", metaAttrs);

PublicationStatus publicationStatus = getPublicationStatus(request, i18n);

final String nameDisplayOrder = i18n.get("{0} {1}", "name display order: {0} is the given (first) name, {1} the family (last) name", "givenName middleName", "familyName");
String pwcStatus=getStatus(resource);
String pwcDocState = getDocState(resource);

%>
<tr <%= attrs.build() %>>
    <td is="coral-table-cell" coral-table-rowselect><%
        if (isArchive) {
            %><coral-icon class="foundation-collection-item-thumbnail" icon="fileZip" size="S"></coral-icon><%
        } else {%>
            <img class="foundation-collection-item-thumbnail" src="<%= xssAPI.getValidHref(thumbnailUrl)%>" alt="" style="width: auto; height: auto; max-width: 3rem; max-height: 3rem;"><%
        }%>
    </td>
    <% if(!isSnippetRequest) { %>
    <td is="coral-table-cell" role="rowheader" class="dam-test-collection-item-name" value="<%= xssAPI.encodeForHTMLAttr(resource.getName()) %>">
        <%= xssAPI.encodeForHTML(resource.getName()) %>
    </td>
    <% } %>
    <td class="foundation-collection-item-title dam-test-collection-item-title" is="coral-table-cell" value="<%= xssAPI.encodeForHTMLAttr(resourceAbsTitle) %>"><%= xssAPI.encodeForHTML(resourceAbsTitle) %>
        <cq:include script = "status.jsp"/>
    </td>
    <% if(!isSnippetRequest) { %>
    <td is="coral-table-cell" value="<%= displayLanguage %>"><%= displayLanguage %></td>
    <% } %>
    <% if(!isSnippetRequest) { %>
    	<td is="coral-table-cell" value="<%= pwcStatus %>" style="color:red"><%= pwcStatus %></td>
        <td class="encodingstatus foundation-collection-item-title" is="coral-table-cell">
            <cq:include script = "encodingstatus.jsp"/>
        </td>
    <% } %>
    <td is="coral-table-cell" class="type" value="<%= xssAPI.encodeForHTMLAttr(displayMimeType) %>">
        <%= xssAPI.encodeForHTML(displayMimeType) %>
        <% if (isLiveCopy) { %><div class="foundation-layout-util-subtletext"><%= xssAPI.encodeForHTML(i18n.get("Live Copy")) %></div><% } %>
    </td>
    <td is="coral-table-cell" class="dam-test-collection-item-dimension" value="<%= width %>"><%= xssAPI.encodeForHTML(resolution)%></td>
    <td is="coral-table-cell" class="dam-test-collection-item-size" value="<%= bytes %>"><%= size %></td>
    <td is="coral-table-cell" value="0"></td> <!-- Adding a placeholder column for content Fragment model -->
    <% if(!isSnippetRequest) { %>
    <cq:include script = "rating.jsp"/>
    <td is="coral-table-cell" value="<%= assetUsageScore %>"><%= assetUsageScore %></td>
    <td is="coral-table-cell" value="<%= assetImpressionScore %>"><%= assetImpressionScore %></td>
    <td is="coral-table-cell" value="<%= assetClickScore %>"><%= assetClickScore %></td>
    <% } %>
    <td is="coral-table-cell" class="dam-test-collection-item-created" value="<%= xssAPI.encodeForHTMLAttr(Long.toString(createdLong)) %>"><%
        if (createdStr != null) {
            %><foundation-time type="datetime" value="<%= xssAPI.encodeForHTMLAttr(createdStr) %>"></foundation-time><%
        }
    %></td>
    <td is="coral-table-cell" class="dam-test-collection-item-modified" value="<%= xssAPI.encodeForHTMLAttr(Long.toString(assetLastModification)) %>"><%
        if (lastModified != null) {
            %><foundation-time type="datetime" value="<%= xssAPI.encodeForHTMLAttr(lastModified) %>"></foundation-time><%

            // Modified-after-publish indicator
                if (publishDateInMillis > 0 && publishDateInMillis < assetLastModification) {
                String modifiedAfterPublishStatus = i18n.get("Modified since last publication");
                %><coral-icon icon="alert" style = "margin-left: 5px;" size="XS" title="<%= xssAPI.encodeForHTMLAttr(modifiedAfterPublishStatus) %>"></coral-icon><%
            }

            %><div class="foundation-layout-util-subtletext"><%= xssAPI.encodeForHTML(lastModifiedBy) %></div><%
        }
    %></td>
    <td is="coral-table-cell"
        value="<%= xssAPI.encodeForHTML(pwcDocState) %>">
        <span><%= xssAPI.encodeForHTML(pwcDocState) %></span>
        <cq:include script = "applicableRelationships.jsp"/>
    </td>
    <td is="coral-table-cell" value="<%= workflows.size() %>">
        <% if (workflows.size() > 0) { %>
        <a class="cq-timeline-control" data-cq-timeline-control-filter="workflows" href="#">
            <foundation-workflowstatus variant="<%= isWorkflowFailed(workflows) ? "error" : "default" %>">
                <%
                    for (com.adobe.granite.workflow.exec.Workflow w : workflows) { %>
                <foundation-workflowstatus-item
                        author="<%= xssAPI.encodeForHTMLAttr(AuthorizableUtil.getFormattedName(resourceResolver, w.getInitiator(), nameDisplayOrder)) %>"
                        timestamp="<%= xssAPI.encodeForHTMLAttr(w.getTimeStarted().toInstant().toString()) %>">
                    <%= xssAPI.encodeForHTML(i18n.getVar(w.getWorkflowModel().getTitle())) %></foundation-workflowstatus-item>

                <% } %>
            </foundation-workflowstatus>
        </a>
        <% } %>
    </td>
    <td is="coral-table-cell" value="<%= isCheckedOut ? xssAPI.encodeForHTMLAttr(checkedOutByFormatted) : "0" %>">
        <%
            // Checkout Status
            if (isCheckedOut) {
                String titleDisplay = i18n.get("Checked Out By {0}", "name inserted to variable", checkedOutByFormatted);
                %><coral-icon icon="lockOn" style = "margin-left: 5px;" size="XS" title="<%= xssAPI.encodeForHTMLAttr(titleDisplay) %>"><%
            }
        %>
    </td>
    <td is="coral-table-cell" value="<%= commentsCount %>"><%= commentsCount %></td>
    <td is="coral-table-cell" value="0"></td>   <!--Adding a placeholder column for metadata profile-->
    <% if(!isSnippetRequest) { %>
    <td is="coral-table-cell" value="0"></td>   <!--Adding a placeholder column for image profile-->
    <td is="coral-table-cell" value="0"></td>   <!--Adding a placeholder column for video profile-->
    <cq:include script = "reorder.jsp"/>
    <% } %>
    <cq:include script = "meta.jsp"/>
</tr><%!
     private boolean isWorkflowFailed(List<com.adobe.granite.workflow.exec.Workflow> workflows) {
         final String SUBTYPE_FAILURE_ITEM = "FailureItem";

         for (com.adobe.granite.workflow.exec.Workflow workflow : workflows) {
             List<com.adobe.granite.workflow.exec.WorkItem> workItems = workflow.getWorkItems();
             for (com.adobe.granite.workflow.exec.WorkItem workItem : workItems) {
                 if (SUBTYPE_FAILURE_ITEM.equals(workItem.getItemSubType())) {
                     return true;
                 }
             }
         }
         return false;
     }

	private String getStatus(Resource res) {
		 Resource metaRes = res.getChild("jcr:content/metadata");
		 if(null!=metaRes){
			 return metaRes.getValueMap().get("pwc-content-status", "");
		 }
		 return "";
	}

	private String getDocState(Resource res) {
    		 Resource metaRes = res.getChild("jcr:content/metadata");
    		 if(null!=metaRes){
                     return metaRes.getValueMap().get("docstate", "");
    		 }
    		 return "";
    	}
 %>
