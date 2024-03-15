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
				com.adobe.fmdita.custom.common.LinkUtils,
				com.pwc.madison.core.util.DITALinkUtils"%>
<%@include file="/libs/foundation/global.jsp"%>
<%@include file="/libs/fmdita/components/dita/common/localization.jsp"%>

<%@page trimDirectiveWhitespaces="true"%>
<%String originalComponent = properties.get("originalComponent","");
Node topicNode = DITALinkUtils.getTopicNode(currentNode.getParent());
  String id = "";
try{
    id = topicNode.getProperty("id").getString();
}
catch(Exception e) {
    }
request.setAttribute("containerID",id);
%>

<sling:include path="<%= currentNode.getPath() %>" resourceType="<%=originalComponent%>"/>