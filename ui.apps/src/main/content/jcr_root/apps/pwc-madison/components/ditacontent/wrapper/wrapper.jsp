 <%--
********************************************************************
*
*
*
***********************************************************************
*
* ADOBE CONFIDENTIAL
*
* ___________________
*
* Copyright 2016 Adobe Systems Incorporated
* All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Adobe Systems Incorporated and its suppliers,
*if any.The intellectual and technical concepts contained
* herein are proprietary to Adobe Systems Incorporated and its
* suppliers and may be covered by U.S.and Foreign Patents,
*patents in process, and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Adobe Systems Incorporated.
*********************************************************************
--%>

<%@page session="false"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="com.day.cq.wcm.foundation.Paragraph,
                  com.day.cq.wcm.foundation.ParagraphSystem,
                  org.apache.commons.lang3.StringUtils,
				  com.pwc.madison.core.constants.MadisonConstants"
%>
<%@include file="/libs/foundation/global.jsp"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://starlingbeta.com/taglib" prefix="fmdita" %>

<c:set var="elements" value="${fmdita:getWrapperTags(slingRequest, properties)}" />
<c:set var="abstractTopicTitle"value="<%= xssAPI.encodeForHTML(currentPage.getTitle()) %>" />
<c:out value="${elements[0]}" escapeXml="false"/>
<c:set var="requestedPagePath" value="<%= request.getRequestURI() %>" />

<%
  String isAtGlanceTitle = properties.get("showAtGlanceTitle", "");
  String pwcResponseText = properties.get("showPWCResponse", "");
  String pwcAnalysisText = properties.get("showAnalysisText", "");
  String isCalloutTitle = properties.get("showCalloutTitle", "");
  String isAbstractTitle = properties.get("showPwCAbstractTitle", "");
  String keyPointsText = properties.get("showKeyPoints", "");
  String observationsText = properties.get("showObservations", "");
  String isXbrlElementName = properties.get("xbrlElementName", "");
  String isXbrlReferences = properties.get("xbrlReferences", "");
  String pnum = properties.get("pnum","");
  String linum = properties.get("linum","");

  boolean hasH1title = properties.get("wrapelement","").equals("h1");
  String h1Title = "";
  if(hasH1title && currentNode.hasNode("_text")){
      h1Title = currentNode.getNode("_text").getProperty("text").getString();
  }

  //Below changes are for printing ancestry values specifically before Paragraph number on the FASB site
  boolean hasAscProps = StringUtils.isNoneEmpty(currentPage.getProperties().get("ascTopicNum", "")) &&
                        StringUtils.isNoneEmpty(currentPage.getProperties().get("ascSubtopicNum", "")) &&
                        StringUtils.isNoneEmpty(currentPage.getProperties().get("ascSectionNum", ""));

  boolean isFasbPage = currentPage.getPath().matches(MadisonConstants.FASB_CONTENT_REGEX);
  String ascTopicNum="";
  String ascSubtopicNum="";
  String ascSectionNum= "";
  if(hasAscProps){
      ascTopicNum = currentPage.getProperties().get("ascTopicNum", String[].class)[0];
      ascSubtopicNum = currentPage.getProperties().get("ascSubtopicNum", String[].class)[0];
      ascSectionNum = currentPage.getProperties().get("ascSectionNum", String[].class)[0];
  }
%>
<%
  ParagraphSystem parSys = ParagraphSystem.create(resource, slingRequest);
%>

<%
if(isCalloutTitle != null && "true".equals(isCalloutTitle)){%>
	<strong>
<% } %>

<%
if(isAtGlanceTitle != null && "true".equals(isAtGlanceTitle)){%>
        <sling:include resourceType="/apps/pwc-madison/components/ditacontent/atglancetitle" />
<% } %>

<%
if(StringUtils.isNoneEmpty(pwcResponseText)){%>
        <p><em class="highlight"><%= pwcResponseText %></em></p>
<% } %>
<%
if(StringUtils.isNoneEmpty(keyPointsText)){%>
        <p><strong><%= keyPointsText %></strong></p>
<% } %>
<%
if(StringUtils.isNoneEmpty(observationsText)){%>
        <p><strong><%= observationsText %></strong></p>
<% } %>
<%
if(StringUtils.isNoneEmpty(pwcAnalysisText)){%>
        <p><em class="highlight"><%= pwcAnalysisText %></em></p>
<% } %>
<%
if(StringUtils.isNoneEmpty(isAbstractTitle)){%>
        <p><strong>${abstractTopicTitle}</strong></p>
<% } %>
<%if(StringUtils.isNoneEmpty(isXbrlElementName) && "true".equals(isXbrlElementName)){%><label>Element Name</label><p><% } %>
<%if(StringUtils.isNoneEmpty(isXbrlReferences) && "true".equals(isXbrlReferences)){%>
	<sling:include resourceType="/apps/pwc-madison/components/ditacontent/xbrlreferencesheader" />
<% } %>

<%-- For Non-FASB Pages --%>
<%if(!isFasbPage && StringUtils.isNoneEmpty(pnum)){%>
   <div class="pnum"><%= pnum %></div>
<% } %>
<%-- For FASB Pages --%>
<%if(isFasbPage && hasAscProps && StringUtils.isNoneEmpty(pnum)){%>
        <div class="pnum"><%= ascTopicNum +"-"+ ascSubtopicNum +"-"+ pnum %></div>
<% } %>

<%
if(StringUtils.isNoneEmpty(linum)){%>
        <span class="linum"><%= linum %></span>
<% } %>
<% if(hasH1title){
    if(h1Title.endsWith("Glossary")){
		String glossaryFullNum = h1Title.split(" ")[0];
        String glossaryTitle = h1Title.split(" ")[1];
		String glossarySectionNum = StringUtils.substringAfterLast(glossaryFullNum, "-");
        %>
		<%= (glossarySectionNum +" "+ glossaryTitle) %>
    <% }else {%>
		<%= ascSectionNum +" "+ h1Title%>
<% } }%>
<%
  for (Paragraph par: parSys.paragraphs()){
    %>
    <c:set var="hasH1Title" value="<%= par.getParent().adaptTo(Node.class).getProperty("wrapelement").getString().equals("h1") %>"/>
    <c:set var="parPath" value="<%= par.getPath() %>" />
    <c:choose>
        <%-- Since we have handled the printing of H1 Title above, Following two 'c:when' cases are used to handle the
        exclude printing of "title" and "text" node --%>
        <c:when test="${hasH1Title && fn:endsWith(parPath, '/title/_text')}">
            <c:choose>
                <c:when test="${fn:endsWith(requestedPagePath, '.joinedsection.html')}"></c:when>
                <c:otherwise>
                    <sling:include resource="<%= par %>"/>
                </c:otherwise>
            </c:choose>
        </c:when>
		<c:when test="${hasH1Title && fn:endsWith(parPath, '/title')}">
            <c:choose>
                <c:when test="${fn:endsWith(requestedPagePath, '.joinedsection.html')}"></c:when>
                <c:otherwise>
                    <sling:include resource="<%= par %>"/>
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <sling:include resource="<%= par %>"/>
        </c:otherwise>
	</c:choose>
<% } %>
<%if(StringUtils.isNoneEmpty(isXbrlElementName) && "true".equals(isXbrlElementName)){%></p><% } %>

<% if(isCalloutTitle != null && "true".equals(isCalloutTitle)){%>
	</strong>
<% } %>

<c:out value="${elements[1]}" escapeXml="false"/>
