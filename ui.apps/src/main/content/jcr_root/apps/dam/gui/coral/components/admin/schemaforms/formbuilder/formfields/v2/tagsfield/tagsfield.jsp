<%--

  ADOBE CONFIDENTIAL
  __________________

   Copyright 2012 Adobe Systems Incorporated
   All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.

--%><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false" contentType="text/html" pageEncoding="utf-8"
         import="com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ds.ValueMapResource,
                  org.apache.sling.api.resource.ValueMap,
                  org.apache.sling.api.wrappers.ValueMapDecorator,
                  java.util.HashMap" %><%

    Config cfg = new Config(resource);
	String key = resource.getName();
    ValueMap tagsPickerProperties = new ValueMapDecorator(new HashMap<String, Object>());
    tagsPickerProperties.put("fieldLabel", cfg.get("fieldLabel","Tags"));
    tagsPickerProperties.put("disabled", cfg.get("disabled", false));
    ValueMapResource valueMapResource = new ValueMapResource(resourceResolver, resource.getPath(), "granite/ui/components/coral/foundation/form/textfield", tagsPickerProperties);
	String resourcePathBase = "dam/gui/coral/components/admin/schemaforms/formbuilder/formfieldproperties/";
%>

<div class="formbuilder-content-form">
    <label class="fieldtype coral-Form-fieldlabel">
    <coral-icon icon="tag" size="XS"></coral-icon>
        <%= i18n.get("Tags") %>
    </label>
    <sling:include resource="<%= valueMapResource %>"/>
</div>
<div class="formbuilder-content-properties">

    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key) %>">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/jcr:primaryType") %>" value="nt:unstructured">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/cq-msm-lockable") %>" value="cq:tags">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/multiple") %>" value="true">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/resourceType") %>" value="cq/gui/components/coral/common/form/tagfield">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/sling:resourceType") %>" value="dam/gui/components/admin/schemafield">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/metaType") %>" value="tags">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/renderReadOnly@TypeHint") %>" value="Boolean"/>
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/cq:showOnCreate") %>" value="true"/>
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/cq:showOnCreate@TypeHint") %>" value="Boolean"/>

    <%
        String[] settingsList = {"labelfields", "metadatamappertextfield", "titlefields"};
        for(String settingComponent : settingsList){
            %>
            <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + settingComponent %>"/>
            <%
        }
    %>

    <coral-icon class="delete-field" icon="delete" size="L" href="" data-target-id="<%= xssAPI.encodeForHTMLAttr(key) %>" data-target="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "@Delete") %>"></coral-icon>

</div>
<div class="formbuilder-content-properties-rules">
    <label for="field">
    	<span class="rules-label"><%= i18n.get("Field") %></span>
        <%
            String[] fieldRulesList = {"disableineditmodefields", "showemptyfieldinreadonly"};
            for(String ruleComponent : fieldRulesList){
                %>
                    <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + ruleComponent %>"/>
                <%
            }

        %>
    </label>
    <label for="visibililty">    
        <span class="rules-label"><%= i18n.get("Visibility") %></span>
        <% String visibilityField = "visibilityfields"; %>
        <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + visibilityField %>"/>
    </label>
    <label for="requirement">
    	<span class="rules-label"><%= i18n.get("Requirement") %></span>
        <% String requiredField = "v2/requiredfields"; %>
        <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + requiredField %>"/>
    </label>    
</div>