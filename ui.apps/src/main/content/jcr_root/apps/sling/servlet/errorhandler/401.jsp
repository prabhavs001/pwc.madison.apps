<%@page session="false" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%
    Integer scObject = (Integer) request.getAttribute("javax.servlet.error.status_code");
    int statusCode = (scObject != null)
            ? scObject.intValue()
            : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    response.setStatus(statusCode);
    response.setContentType("text/html"); 
    response.setCharacterEncoding("utf-8");
%>
<html>
    <head><title>Unauthorized</title></head>
    <body>
        <h1>Unauthorized</h1>
	</body>
</html>