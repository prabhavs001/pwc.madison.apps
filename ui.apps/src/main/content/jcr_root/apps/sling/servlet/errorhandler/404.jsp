<%@page session="false" pageEncoding="utf-8"
         import="com.pwc.madison.core.services.VpToVpRedirectionService" %>
<%@include file="/libs/foundation/global.jsp" %>
<c:choose>
    <c:when test="${fn:startsWith(pageContext.request.requestURI, '/content/pwc-madison') || fn:startsWith(pageContext.request.requestURI, '/content/dam/pwc-madison') || fn:startsWith(pageContext.request.requestURI, '/content/madison-ums')}">
    	<%
    		String redirectPath = sling.getService(VpToVpRedirectionService.class).getRedirectPath(request.getRequestURI());
	        if(null != redirectPath){
	        	response.setStatus(301);
		        response.setHeader( "Location", redirectPath);
		        response.setHeader( "Connection", "close");
	        }
	        else {
        %>
       <%@include file="/apps/acs-commons/components/utilities/errorpagehandler/404.jsp" %>
       <% } %>
    </c:when>
    <c:otherwise>
        <%@include file="/apps/sling/servlet/errorhandler/default-custom.jsp" %>
    </c:otherwise>
</c:choose>
