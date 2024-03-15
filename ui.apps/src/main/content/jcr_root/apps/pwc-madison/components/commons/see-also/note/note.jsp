<%--
  Cuatom note section for see also table header.
--%><%@page session="false" %><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page import="org.apache.sling.api.resource.ValueMap,
    			org.apache.commons.lang3.StringUtils,
                  com.adobe.granite.xss.XSSAPI" %>

    <div>
    <%

    String propertyName="note";

    ValueMap properties = resource.adaptTo(ValueMap.class);

    String propertyValue = properties.get(propertyName, "");

    String note =  i18n.getVar(propertyValue);
	String title = note.substring(0, StringUtils.ordinalIndexOf(note, "|", 1));
	String contentId = note.substring(StringUtils.ordinalIndexOf(note, "|", 1) + 1, StringUtils.ordinalIndexOf(note, "|", 2));
	String ditaPath = note.substring(StringUtils.ordinalIndexOf(note, "|", 2) + 1, StringUtils.ordinalIndexOf(note, "|", 3));
	String publicationDate = note.substring(StringUtils.ordinalIndexOf(note, "|", 3) + 1);

    %><h4 class="coral-Heading coral-Heading--4"><div class="see-also-header">
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(title) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(contentId) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(ditaPath) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(publicationDate) %></span>
                            </div></h4>

    </div>
    <%
%>