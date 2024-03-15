<%@page session="false" contentType="text/html; charset=utf-8"%><%
%><%@page import="javax.jcr.Node,
                  javax.jcr.Session,
                  javax.jcr.RepositoryException,
                  com.adobe.granite.security.user.UserPropertiesManager,
                  com.adobe.granite.security.user.UserPropertiesService,
                  com.adobe.granite.security.user.UserProperties,
                  com.adobe.granite.security.user.util.AuthorizableUtil,
                  java.security.Principal,                  
                  javax.jcr.security.Privilege,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.resource.collection.ResourceCollection,
                  com.day.cq.dam.commons.util.UIHelper,
                  com.day.cq.commons.jcr.JcrUtil,
                  com.adobe.granite.ui.components.Config,
                  org.apache.sling.api.resource.ResourceResolverFactory,
                  org.apache.sling.api.resource.ResourceResolver,
                  com.day.cq.i18n.I18n,
                  org.apache.sling.jcr.base.util.AccessControlUtil,
                  org.apache.jackrabbit.api.security.JackrabbitAccessControlList,
                  org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry,
                  org.apache.jackrabbit.util.Text,
                  javax.jcr.security.AccessControlEntry,
                  org.apache.jackrabbit.api.security.user.UserManager,
                  org.apache.jackrabbit.api.security.user.Authorizable,
                  javax.jcr.security.AccessControlManager,
                  org.apache.sling.api.resource.LoginException,
                  org.apache.jackrabbit.api.security.user.Group,
                  org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils,
                  org.apache.sling.tenant.Tenant,
                  java.util.List,
                  java.util.Iterator,
                  java.util.Map,
                  java.util.HashMap,
                  java.util.Map.Entry,
                  java.util.ArrayList,
				  java.util.regex.Pattern,
                  java.util.Collections"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%><%
%><%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0"%><%
%><%@taglib prefix="ui" uri="http://www.adobe.com/taglibs/granite/ui/1.0"%><%
%><cq:defineObjects /><%
    Config cfg = new Config(resource);
    I18n i18n = new I18n(slingRequest);
    Resource currentResource = UIHelper.getCurrentSuffixResource(slingRequest);

    if (currentResource == null) {
        return;
    }

    //if the current user is owner, then show the members to edit
    AccessControlManager acm = resourceResolver.adaptTo(Session.class).getAccessControlManager();
    boolean isOwner = UIHelper.hasPermission(acm, currentResource, Privilege.JCR_WRITE);
    Tenant tenant = resourceResolver.adaptTo(Tenant.class);
    String context = "mac";
    String tenantId = "default";
    if (tenant != null) {
        tenantId = tenant.getId();
    }
    String grpPrefix = context + "-" + tenantId + "-";
    Map<Authorizable, String> allMembers = new HashMap<Authorizable, String>();
    final String nameDisplayOrder = i18n.get("{0} {1}","name display order: {0} is the given (first) name, {1} the family (last) name","givenName middleName","familyName");
    if (isOwner) {
        //use the user session to show the members
        Session userSession = null;
        ResourceResolver userResolver = null;
        AccessControlManager acMgr = null;
        UserManager um = null;
        UserPropertiesService upService = null;
        UserPropertiesManager upm = null;

        try {
            userResolver = resource.getResourceResolver();
            userSession = userResolver.adaptTo(Session.class);
            acMgr = userSession.getAccessControlManager();
            um = AccessControlUtil.getUserManager(userSession);
            upService = sling.getService(UserPropertiesService.class);
            upm = upService.createUserPropertiesManager(userSession, resourceResolver);
            JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(acMgr, currentResource.getPath());
            if (acl != null) {%>
            <table is="coral-table" selectable class="members-table users-table">
                <tbody is="coral-table-body">
            <%
                AccessControlEntry[] accessControlEntries = acl.getAccessControlEntries();
                for (AccessControlEntry ace:accessControlEntries) {
                    Principal principal = ace.getPrincipal();
                    Authorizable authorizable = um.getAuthorizable(principal);

                    if (ace instanceof JackrabbitAccessControlEntry) {
                        boolean isAllow = ((JackrabbitAccessControlEntry) ace).isAllow();
                        if (!isAllow || authorizable == null) {
                            continue;
                        }
                    }
                    if (!isPartOfFolderMemberGroup(authorizable, currentResource, grpPrefix)) {
                        continue;
                    }
                    Privilege []privileges = ace.getPrivileges();
                    String privs[] = new String[privileges.length];
                    for (int i=0;i<privs.length; i++) {
                        privs[i] = privileges[i].getName();
                    }
                    String i18nrole = "";
                    String role = getRole(privs);

                    if (role.equals("")) {                        
                        continue;
                    }
                    List<Authorizable> users = Collections.singletonList(authorizable);
                    Iterator<Authorizable> it = users.iterator();

                    if (authorizable.isGroup()) {
                        it = ((Group)authorizable).getDeclaredMembers();
                    }

                    while (it.hasNext()) {
                        Authorizable member = it.next();
                        String existingRole = allMembers.get(member);
                        if (existingRole == null || "Viewer".equals(existingRole)) {
                            allMembers.put(member, role);
                        }
                    }
                }


                for (Map.Entry<Authorizable, String> entry : allMembers.entrySet()) {
                    Authorizable member = entry.getKey();
                    String role = entry.getValue();
                    UserProperties up = upm.getUserProperties(member, "profile");
                    String name = AuthorizableUtil.getFormattedName(userResolver, member, nameDisplayOrder);
                    String email = getEmail(up);
                    String image = (up == null) ? "" : up.getResourcePath(UserProperties.PHOTOS, "/primary/image", "");
                    if (image == null || image.equals("")) {
                        if (member.isGroup()) {
                            image = "/libs/granite/security/clientlib/themes/default/resources/sample-group-thumbnail.36.png";
                        } else {
                            image = "/libs/granite/security/clientlib/themes/default/resources/sample-user-thumbnail.36.png";
                        }
                    }
                    image  = request.getContextPath() + image;
                    String i18nrole = "";
                    if (role.equals("Owner")) {
                        i18nrole = i18n.get("Owner");
                    } else if(role.equals("Editor")) {
                        i18nrole = i18n.get("Editor");
                    } else if(role.equals("Viewer")) {
                        i18nrole = i18n.get("Viewer");
                    }
                    %>

			    <tr is="coral-table-row">
			    	<td class="avatar" is="coral-table-cell"><img src="<%=xssAPI.getValidHref(image)%>" width = "42"></td>
			    	<td class="name" is="coral-table-cell"><%= xssAPI.encodeForHTML(name) %>
			    	    <input type="hidden" name="teamMemberPrincipalName" value="<%=xssAPI.encodeForHTMLAttr(member.getID())%>">
			    	</td>
                    <% if(!email.trim().isEmpty()){ %>
                        <td class="useremail" is="coral-table-cell"><span class="greyText"></span> <%=xssAPI.encodeForHTML(email)%></td>
                    <% }else{ %>
                        <td class="userid" is="coral-table-cell"><span class="greyText"></span> <%=xssAPI.encodeForHTML(member.getID())%></td>
                    <% } %>

			    	<td class="role greyText" is="coral-table-cell">
			    		<span> <%=i18nrole %></span>
			    		<input type="hidden" name="teamMemberRole" value="<%= xssAPI.encodeForHTMLAttr(role.toLowerCase()) %>">
			    	</td>
			    	<td class="remove" is="coral-table-cell">
                        <button is="coral-button" variant="quiet" icon="close" iconsize="XS"></button>
                    </td>
			    </tr>
                    <%
                }
            }%>
            </tbody>
            </table>
            <%

        } catch (RepositoryException e) {
            log("exception while using user session", e);
        }
    }
%>

<%!
String getRole(String privs[]) {
    String role = "";
    String ownerPrivs = "jcr:modifyAccessControl";
    String editorPrivs = "jcr:removeChildNodes";
    String viewerPrivs = "jcr:read";
    if (contains(privs, ownerPrivs)) {
        return "Owner";
    } else if (contains(privs, editorPrivs)) {
       return "Editor"; 
    }  else if (contains(privs, viewerPrivs)) {
        return "Viewer";
    }   
    return role;
}

boolean contains(String array[], String s) {
    boolean contains = false;
    if (array != null && s != null) {
        for (int i=0; i< array.length; i++) {
            if (array[i].equals(s)) {
              contains = true;
              break;
            }
        }
    }
    return contains;
}

private String getEmail(UserProperties up){
    try {
        return up != null? up.getProperty(UserProperties.EMAIL, "", String.class) : "";
    } catch (RepositoryException e) {
       return "";
    }
}

private Boolean isPartOfFolderMemberGroup(Authorizable authorizable, Resource currentResource, String grpPrefix) {
    try {
        String folderName = JcrUtil.createValidName(currentResource.getName()).replaceAll("-", "");
        String ownerGroupName = grpPrefix + folderName + "[0-9]*-owner";
        String editorGroupName = grpPrefix + folderName + "[0-9]*-editor";
        String viewerGroupName = grpPrefix + folderName+"[0-9]*";
        if (!authorizable.isGroup()) {
            return false;
        }
        String principalName = authorizable.getPrincipal().getName();

        return (Pattern.matches(ownerGroupName,principalName) || Pattern.matches(editorGroupName,principalName) || Pattern.matches(viewerGroupName,principalName));

    } catch (RepositoryException re) {
    }
    return false;
}
%>