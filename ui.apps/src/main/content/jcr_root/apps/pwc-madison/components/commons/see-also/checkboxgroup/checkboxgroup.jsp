<%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
          import="java.util.Iterator,
    			  org.apache.commons.lang3.StringUtils,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Field,
                  com.adobe.granite.ui.components.Tag,
				  org.apache.sling.api.resource.ValueMap,
				  org.apache.sling.api.wrappers.ValueMapDecorator,
				  com.adobe.granite.ui.components.ds.ValueMapResource,
				  java.util.HashMap,
				  org.apache.sling.api.resource.ResourceMetadata" %>
<%

    Iterator<Resource> itemIterator = cmp.getItemDataSource().iterator();
	if (itemIterator != null && itemIterator.hasNext()) {
        while(itemIterator.hasNext()) {
			ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
            vm.putAll(resource.getValueMap());
            vm.putAll(itemIterator.next().getValueMap());
            Resource checkboxResource = new ValueMapResource(resourceResolver, resource.getPath(), "/apps/pwc-madison/components/commons/see-also/checkbox", vm);
            %>
			<sling:include resource="<%= checkboxResource %>" />
			<%
        }
    }
%>