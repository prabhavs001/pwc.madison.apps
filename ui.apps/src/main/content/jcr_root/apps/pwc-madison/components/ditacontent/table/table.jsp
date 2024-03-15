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

<%@page session="false"%>
<%@include file="/libs/foundation/global.jsp"%>

<%@page import="org.w3c.dom.Document"%>

<%@page import="com.adobe.fmdita.common.NodeUtils"%>
<%@page import="java.util.Locale,java.util.ResourceBundle"%>
<%@page import="com.day.cq.i18n.I18n"%>

<%@include file="/libs/fmdita/components/common/tablestack.jsp"%>
<%@include file="/libs/fmdita/components/dita/common/localization.jsp"%>

<% final Locale pageLocale = currentPage.getLanguage(false); 
final ResourceBundle resourceBundle = slingRequest.getResourceBundle(pageLocale); 
I18n i18n = new I18n(resourceBundle); 
%>
<%
addTableMap(request);

String classVal = NodeUtils.getOutputClass(properties);
String colSep = properties.get("colsep", "");
if(colSep.equals("1"))
  classVal += " colsep";
String rowSep = properties.get("rowsep", "");
if(rowSep.equals("1"))
  classVal += " rowsep ";
String vAlign = properties.get("valign", "");
classVal += vAlign;
String outputClass = properties.get("outputclass", "");
pageContext.setAttribute("outputClass", outputClass);
classVal.trim();
String classAttr = "";
  if(!classVal.isEmpty())
    classAttr = " class=\"" + classVal +" "+outputClass + "\"";
%>
<div class="responsive-table-container table-shading">
	<div class="table-responsive table-top-gap">
		<table <%= classAttr + " " + getLocalizationAttributes(properties)%>>
			<cq:include
				script="/libs/fmdita/components/dita/delegator/delegator.jsp" />
		</table>
	</div>
    <c:set var="outputclass" value = "${outputClass}"/>
    <c:if test = "${fn:contains(outputclass,'show-modal')}"> 
        <a class='inline-link js-modal-trigger' href="javascript:void(0);"
            data-toggle="modal" data-target="#responsive-table-modal" id="table-modal-link"> <span
            class='inline-img'>
                <span class="icon-view-table" class="viewtable-icon"></span>
        </span><%= i18n.get("Common_Document_Body_View_Table") %>
        </a>
    </c:if>
</div>
<%
  popTableMap(request);
%>