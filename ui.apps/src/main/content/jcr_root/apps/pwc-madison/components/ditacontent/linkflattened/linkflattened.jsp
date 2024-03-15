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
<%@page import="org.apache.log4j.Logger"%>
<%@page import="com.adobe.fmdita.common.NodeUtils,
                com.adobe.fmdita.common.MiscUtils,
                org.apache.commons.lang.StringUtils,
                com.adobe.fmdita.common.DomUtils,
				com.pwc.madison.core.util.DITALinkUtils,
				com.pwc.madison.core.constants.DITAConstants,
				com.day.cq.search.QueryBuilder,
				com.adobe.fmdita.custom.common.LinkUtils"%>
<%@include file="/libs/foundation/global.jsp"%>
<%@include file="/libs/fmdita/components/dita/common/localization.jsp"%>

<%@page trimDirectiveWhitespaces="true"%>

<%! static Logger LOG = Logger.getLogger(linkflattened_jsp.class); %>

<%
  QueryBuilder queryBuilder = sling.getService(QueryBuilder.class);
  String fixedLink = DITALinkUtils.getFixedSyndicationPeerLink(currentNode, currentPage, resourceResolver, queryBuilder);
  Session session = resourceResolver.adaptTo(Session.class);
  String format = properties.get("format", "dita"), description = StringUtils.EMPTY;
  String keyRef = properties.get("keyref", StringUtils.EMPTY);
  String textData = NodeUtils.getOptimizedStringProperty(properties.get(DITAConstants.FOOTNOTE_TEXT, Property.class), StringUtils.EMPTY);
  String link = properties.get(DITAConstants.PROPERTY_LINK, DITAConstants.HASH_STR);
  String fmguid = properties.get(DITAConstants.FMGUID, StringUtils.EMPTY);
  String linkText = link;
  String bookmark = StringUtils.EMPTY;
  String classVal = NodeUtils.getOutputClass(properties);
  boolean isEdit = WCMMode.fromRequest(request) == WCMMode.EDIT;
  Node topicNode = DITALinkUtils.getTopicNode(currentNode.getParent());
  
  LOG.debug("**********link Value in linkflattened: *************"+fixedLink);
  
  String xtrf = StringUtils.EMPTY;
  try {
      xtrf = topicNode.getProperty("xtrf").getString();
    Element rootElement = MiscUtils.getRootElement(properties);
    NodeList descList = DomUtils.getElementsByClassName("topic/desc", rootElement);
    Element descElement = descList.getLength() > 0 ? (Element)descList.item(0) : null;
    
    if(descElement != null)
      description = descElement.getTextContent();
    } catch(Exception e) {
       description = StringUtils.EMPTY;
    }
  String scope = properties.get("scope", "local");
  String type = properties.get(DITAConstants.PROPERTY_TYPE, StringUtils.EMPTY);
  int bookmarkPos = link.indexOf(DITAConstants.HASH_STR);
  if (bookmarkPos > -1) {
    bookmark = link.substring(bookmarkPos);
    link = link.substring(0, bookmarkPos);
  }
  String ext = FilenameUtils.getExtension(link);
  if (ext.isEmpty() && !link.isEmpty() && scope.equals("local")) {
    link = link + DITAConstants.HTML_EXT;
  }
  linkText = link = link + bookmark;
  if (linkText.lastIndexOf(DITAConstants.FORWARD_SLASH) > -1)
    linkText = linkText.substring(linkText.lastIndexOf(DITAConstants.FORWARD_SLASH) + 1);
  String linkDomain = new URI(link).getHost();
  String target = "_self";
  if((linkDomain != null) && (linkDomain != request.getServerName())) {
      target = "_blank";
  }
  if("external".equals(scope)) {
      target = "_blank";
  }

  String fmguidPeer = StringUtils.EMPTY;
  if(DITAConstants.PEER_SCOPE.equals(scope)) {
      if(link!=null) {
          fmguidPeer = LinkUtils.getGuidForFmLinkStr(session, fixedLink, currentPage.getPath(), pageProperties.get(DITAConstants.BASE_PATH, StringUtils.EMPTY));
          LOG.debug("**********Fmguid for the path: *************"+fmguidPeer);
      }
  }
  
  String keyRefAttr = StringUtils.isNotBlank(keyRef) ?
  String.format("data-keyref=\"%s\"", xssAPI.encodeForHTMLAttr(keyRef)) : StringUtils.EMPTY;
%>
<a class="<%= classVal %>" href="<%= fixedLink %>" data-xtrf="<%=xtrf%>" data-link="<%= fixedLink %>" data-anchor="<%=bookmark%>" title="<%= description %>" data-fmguid="<%= fmguidPeer %>" data-scope="<%= scope %>" data-format="<%= format %>" data-type="<%= type %>" target="<%= target %>"
<%= getLocalizationAttributes(properties) %>><%
    if(textData == null || textData.trim().isEmpty()) {
      %><%= linkText %><%
    }
    ParagraphSystem parSys = ParagraphSystem.create(resource, slingRequest);
    for (Paragraph par: parSys.paragraphs()) {
        %><sling:include resource="<%= par %>"/><%
    }
%></a>