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
         import="org.apache.sling.api.resource.ValueMap" %><%

	ValueMap fieldProperties = resource.adaptTo(ValueMap.class);
	String key = resource.getName();
    String resourcePathBase = "dam/gui/coral/components/admin/schemaforms/formbuilder/formfieldproperties/";
%>
<!-- This is Custom Multi value Path Field Added with RootPath "/content/pwc-madison"  -->
<div class="formbuilder-content-form">
    <label class="fieldtype coral-Form-fieldlabel">
        <coral-icon icon="text" size="XS"></coral-icon>
        <%= i18n.get("Multi Value Path Field") %>
    </label>
    <sling:include resource="<%= resource %>" resourceType="granite/ui/components/coral/foundation/form/pathfield"/>
</div>
<div class="formbuilder-content-properties">

    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key) %>">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/jcr:primaryType") %>" value="nt:unstructured">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/resourceType") %>" value="granite/ui/components/coral/foundation/form/multifield">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/sling:resourceType") %>" value="dam/gui/components/admin/schemafield">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/granite:data/metaType") %>" value="mvpath">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/field") %>">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/field/jcr:primaryType") %>" value="nt:unstructured">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/field/sling:resourceType") %>" value="granite/ui/components/coral/foundation/form/pathfield">
    <input type="hidden" name="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "/field/rootPath") %>" value="/content/pwc-madison">



    <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + "labelfields"%>"/>
    <%request.setAttribute("cq.dam.metadataschema.builder.field.relativeresource", "field"); %>
    <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + "metadatamappertextfield"%>"/>
    <%request.removeAttribute("cq.dam.metadataschema.builder.field.relativeresource"); %>
    <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + "placeholderfields"%>"/>

    <%request.removeAttribute("cq.dam.metadataschema.builder.field.relativeresource"); %>

    <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + "titlefields" %>" />

    <coral-icon class="delete-field" icon="delete" size="L" href="" data-target-id="<%= xssAPI.encodeForHTMLAttr(key) %>" data-target="<%= xssAPI.encodeForHTMLAttr("./items/" + key + "@Delete") %>"></coral-icon>
</div>
<div class="formbuilder-content-properties-rules">
    <label for="field">
    	<span class="rules-label"><%= i18n.get("Field") %></span>
        <%
            String[] fieldRulesList = {"showemptyfieldinreadonly"};
            for(String ruleComponent : fieldRulesList){
                %>
                    <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + ruleComponent %>"/>
                <%
            }

        %>
    </label>  
    <label for="requirement">
    	<span class="rules-label"><%= i18n.get("Requirement") %></span>
        <% String requiredField = "v2/requiredfields"; %>
        <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + requiredField %>"/>
    </label> 
    <label for="visibililty">    
        <span class="rules-label"><%= i18n.get("Visibility") %></span>
        <% String visibilityField = "visibilityfields"; %>
        <sling:include resource="<%= resource %>" resourceType="<%= resourcePathBase + visibilityField %>"/>
    </label>     
</div>
