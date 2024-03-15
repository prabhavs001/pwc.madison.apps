<%--
  Copyright 1997-2009 Day Management AG
  Barfuesserplatz 6, 4001 Basel, Switzerland
  All Rights Reserved.

  This software is the confidential and proprietary information of
  Day Management AG, ("Confidential Information"). You shall not
  disclose such Confidential Information and shall use it only in
  accordance with the terms of the license agreement you entered into
  with Day.

  ==============================================================================

  Default error handler

--%><%@page session="false" pageEncoding="utf-8"
         import="com.day.cq.wcm.api.WCMMode,
                    java.io.PrintWriter,
                    org.apache.sling.api.SlingConstants,
                    org.apache.sling.settings.SlingSettingsService,
                    org.apache.sling.api.request.RequestProgressTracker,
                    org.apache.sling.api.request.ResponseUtil,
                    org.apache.commons.lang3.StringEscapeUtils" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%

    String message = (String) request.getAttribute("javax.servlet.error.message");
    Integer scObject = (Integer) request.getAttribute("javax.servlet.error.status_code");
    boolean isAuthorMode = WCMMode.fromRequest(request) != WCMMode.DISABLED && !sling.getService(SlingSettingsService.class).getRunModes().contains("publish");
    
    int statusCode = (scObject != null)
            ? scObject.intValue()
            : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    if (message == null) {
        message = statusToString(statusCode);
    }
    
    response.setStatus(statusCode);
    response.setContentType("text/html"); 
    response.setCharacterEncoding("utf-8");
        
%><!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
<html>
    <head><title><%= statusCode %> <%= StringEscapeUtils.escapeHtml4(message) %></title></head>
    <body>
        <h1><%= StringEscapeUtils.escapeHtml4(message) %></h1>
        <p>Cannot serve request to <%= StringEscapeUtils.escapeHtml4(request.getRequestURI()) %><%
        if (isAuthorMode && request.getAttribute(SlingConstants.ERROR_SERVLET_NAME) instanceof String) {
            %> in <%= StringEscapeUtils.escapeHtml4((String)request.getAttribute(SlingConstants.ERROR_SERVLET_NAME)) %><%
        } else {
            %> on this server<%
        }
        %></p>
        
        <%
        if (isAuthorMode) {
            // write the exception message
            final PrintWriter escapingWriter = new PrintWriter(ResponseUtil.getXmlEscapingWriter(out));
    
            // dump the stack trace
            if (request.getAttribute(SlingConstants.ERROR_EXCEPTION) instanceof Throwable) {
                Throwable throwable = (Throwable) request.getAttribute(SlingConstants.ERROR_EXCEPTION);
                out.println("<h3>Exception:</h3>");
                out.println("<pre>");
                out.flush();
                printStackTrace(escapingWriter, throwable);
                escapingWriter.flush();
                out.println("</pre>");
            }

            // dump the request progress tracker
            if (slingRequest != null) {
                RequestProgressTracker tracker = slingRequest.getRequestProgressTracker();
                out.println("<h3>Request Progress:</h3>");
                out.println("<pre>");
                out.flush();
                tracker.dump(escapingWriter);
                escapingWriter.flush();
                out.println("</pre>");
            }
        }
        %>

        <hr>
    </body>
</html>