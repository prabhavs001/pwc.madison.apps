<%--
  ADOBE CONFIDENTIAL
  __________________
  Copyright 2016 Adobe Systems Incorporated
  All Rights Reserved.
  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
____________________
--%>

<%@page import="java.net.URI" %>

<%@page session="false"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="com.day.cq.wcm.api.components.IncludeOptions,
                    com.day.cq.wcm.foundation.Paragraph,
                    org.w3c.dom.Element,
                    org.w3c.dom.NodeList,
                    com.day.cq.wcm.foundation.ParagraphSystem,
                    org.apache.commons.io.FilenameUtils,
                    javax.jcr.Session,
					com.day.cq.wcm.api.WCMMode" %>
<%@page import="com.adobe.fmdita.common.NodeUtils,
                com.adobe.fmdita.common.MiscUtils,
                org.apache.commons.lang.StringUtils,
                com.adobe.fmdita.common.DomUtils,
				com.adobe.fmdita.custom.common.LinkUtils"%>
<%@include file="/libs/foundation/global.jsp"%>
<%@include file="/libs/fmdita/components/dita/common/localization.jsp"%>

<%@page trimDirectiveWhitespaces="true"%>

<%
  Session session = resourceResolver.adaptTo(Session.class);
  String format = properties.get("format", "dita"), description = "";
  String keyRef = properties.get("keyref", "");
  String textData = NodeUtils.getOptimizedStringProperty(properties.get("text", Property.class), "");
  String link = properties.get("link", "#");
  String linkText = link;
  String bookmark = "";
  String classVal = NodeUtils.getOutputClass(properties);
  boolean isEdit = WCMMode.fromRequest(request) == WCMMode.EDIT;

  try {
    Element rootElement = MiscUtils.getRootElement(properties);
    NodeList descList = DomUtils.getElementsByClassName("topic/desc", rootElement);
    Element descElement = descList.getLength() > 0 ? (Element)descList.item(0) : null;
    if(descElement != null)
      description = descElement.getTextContent();
    } catch(Exception e) {
       description = "";
    }

  String scope = properties.get("scope", "local");
  String type = properties.get("type", "");

  int bookmarkPos = link.indexOf("#");
  if (bookmarkPos > -1) {
    bookmark = link.substring(bookmarkPos);
    link = link.substring(0, bookmarkPos);
  }

  String ext = FilenameUtils.getExtension(link);
  if (ext.isEmpty() && !link.isEmpty() && scope.equals("local")) {
    link = link + ".html";
  }

  linkText = link = link + bookmark;
  if (linkText.lastIndexOf("/") > -1)
    linkText = linkText.substring(linkText.lastIndexOf("/") + 1);

  String linkDomain = null;
  try{
    linkDomain = new URI(link).getHost();
  }catch(Exception e){
  }
  String target = "_self";
  if((linkDomain != null) && (linkDomain != request.getServerName())) {
      target = "_blank";
  }
  if("external".equals(scope)) {
      target = "_blank";
  }
  
  String keyRefAttr = StringUtils.isNotBlank(keyRef) ?
  String.format("data-keyref=\"%s\"", xssAPI.encodeForHTMLAttr(keyRef)) : "";
  
  if("peer".equals(scope)) {
      if(link!=null) {
		  link = LinkUtils.getUpdatedXrefLink(session,currentNode,link);
          ext = FilenameUtils.getExtension(link);
          if(link.indexOf("#") > 0) {
          	ext =   FilenameUtils.getExtension(link.substring(0,link.indexOf("#")));  
			if (ext.isEmpty() && !link.isEmpty()) {
				link = link.substring(0,link.indexOf("#")) + ".html" + link.substring(link.indexOf("#"));
		    }
	    } else if(ext.isEmpty() && !link.isEmpty()) {
                  link = link + ".html";
       	}
      }
  }
if((!"dita".equals(ext) && resourceResolver.resolve(link) != null && !resourceResolver.resolve(link).isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING)) || isEdit || "external".equals(scope)) {%>
<a class="<%= classVal %>" href="<%= link %>" title="<%= description %>" <%= keyRefAttr %> data-oldlink="true" data-scope="<%= scope %>" data-format="<%= format %>" data-type="<%= type %>" target="<%= target %>" 
<%= getLocalizationAttributes(properties) %>><% } %><%
    ParagraphSystem parSys = ParagraphSystem.create(resource, slingRequest);
    for (Paragraph par: parSys.paragraphs()) {
        %><sling:include resource="<%= par %>"/><%
    }
if((!"dita".equals(ext) && resourceResolver.resolve(link) != null && !resourceResolver.resolve(link).isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING)) || isEdit || "external".equals(scope)) { %></a><% } %>