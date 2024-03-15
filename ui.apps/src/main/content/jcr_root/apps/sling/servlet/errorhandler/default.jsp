<%@page session="false"%>
<%@include file="/libs/foundation/global.jsp" %>
<c:choose>
    <c:when test="${fn:startsWith(pageContext.request.requestURI, '/content/pwc-madison')  || fn:startsWith(pageContext.request.requestURI, '/content/dam/pwc-madison') || fn:startsWith(pageContext.request.requestURI, '/content/madison-ums')}">
       <%@include file="/apps/acs-commons/components/utilities/errorpagehandler/default.jsp" %>
    </c:when>
    <c:otherwise>
        <%@include file="/apps/sling/servlet/errorhandler/default-custom.jsp" %>
    </c:otherwise>
</c:choose>
