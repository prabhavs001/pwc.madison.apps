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
                  org.apache.commons.lang3.StringUtils"
 %>
 <%@include file="/libs/foundation/global.jsp"%>
 <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
 <%@ taglib uri="http://starlingbeta.com/taglib" prefix="fmdita" %>

 <%

     String startString = properties.get("start", "start(1)");

     int endIndex = startString.lastIndexOf(")");

 %>
 
 <c:set var="listType" value="<%= xssAPI.encodeForHTML(properties.get("outputclass", "1")) %>" />
 <c:set var="startInt" value="<%= xssAPI.encodeForHTML(startString.substring(6, endIndex)) %>" />

 <ol type="${listType}" start="${startInt}">
     <%
         ParagraphSystem parSys = ParagraphSystem.create(resource, slingRequest);

         for (Paragraph par: parSys.paragraphs()){
     %><sling:include resource="<%= par %>"/><%
     }
 %>
 </ol>
