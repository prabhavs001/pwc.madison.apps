 <%--
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
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
%><%@page session="false" contentType="text/html; charset=utf-8"%><%
%><%@page import="com.adobe.granite.xss.XSSAPI,
      			  com.adobe.granite.ui.components.AttrBuilder,
              com.day.cq.i18n.I18n,
                  com.adobe.granite.ui.components.ComponentHelper,
                  com.adobe.granite.ui.components.Config"%><%
%><%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0"%><%
%><cq:defineObjects /><%
  I18n i18n = new I18n(request);
    Config cfg = new Config(resource, null, null);
	AttrBuilder attrs = new AttrBuilder(slingRequest, xssAPI);
	String text = i18n.getVar(cfg.get("text",String.class));
%><button class="coral-Button dam-admin-reports-json-download"> <i class="coral-Icon coral-Icon--download"></i> <%=xssAPI.encodeForHTML(text)%></button>