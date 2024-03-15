package com.pwc.madison.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.xss.XSSAPI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.security.user.UserProperties;
import com.adobe.granite.security.user.UserPropertiesManager;
import com.adobe.granite.security.user.UserPropertiesService;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentManager;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.day.crx.security.token.TokenCookie;
import com.pwc.madison.core.authorization.constants.ContentAuthorizationConstants;
import com.pwc.madison.core.authorization.enums.AccessLevel;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.authorization.models.ContentAuthorization;
import com.pwc.madison.core.beans.PostProcessing;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.models.LevelModel;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;

/**
 *
 */
public final class MadisonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MadisonUtil.class);

    /**
     * Returns {@link ResourceResolver} of the given sub-service. It returns null in case given
     * {@link ResourceResolverFactory} and subService are null.
     *
     * @param resourceResolverFactory {@link ResourceResolverFactory}
     * @param subService              {@link String} sub-service defined in Sling Apache User Mapping configuration
     * @return {@link ResourceResolver}
     */
    public static ResourceResolver getResourceResolver(final ResourceResolverFactory resourceResolverFactory,
                                                       final String subService) {
        ResourceResolver resourceResolver = null;
        if (null != resourceResolverFactory && null != subService) {
            try {
                final Map<String, Object> authInfo = new HashMap<>();
                authInfo.put(ResourceResolverFactory.SUBSERVICE, subService);
                resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo);
            } catch (final LoginException loginException) {
                LOGGER.error(
                    "MadisonUtil getResourceResolver() : Exception while getting resource resolver for subservice {} : {}",
                    subService, loginException);
            }
        }
        return resourceResolver;
    }

    /**
     * Get HTML URL for the page path with .html as its extension
     *
     * @param pagePath path of the page for which HTML URL is required
     * @return HTML URL which can be directly used in anchor tags or JS
     */
    public static String getUrlForPageResource(String pagePath) {

        pagePath += MadisonConstants.HTML_EXTN;

        return pagePath;
    }

    /**
     * Sets the {@value cookieName} {@link Cookie} to the given {@link SlingHttpServletResponse}.
     *
     * @param response            {@link HttpServletResponse}
     * @param cookieValue         {@link String}
     * @param cookieExpirySecs {@link int}
     * @param path                {@link String}
     * @param cookieName          {@link String}
     */
    public static void setMadisonCookie(final SlingHttpServletResponse response, final String cookieValue,
                                        final int cookieExpirySecs, final String path, final String cookieName, final boolean isSecure) {

        if (null == response || StringUtils.isBlank(cookieName) || StringUtils.isBlank(cookieValue)
            || StringUtils.isBlank(path) || 0 == cookieExpirySecs) {
            return;
        }

        final Cookie userRegMadisonCookie = new Cookie(cookieName, StringUtils.normalizeSpace(cookieValue));
        userRegMadisonCookie.setMaxAge(cookieExpirySecs);
        userRegMadisonCookie.setPath(path);
        userRegMadisonCookie.setSecure(isSecure);
        response.addCookie(userRegMadisonCookie);
    }

    /**
     * Return Share Feature is Enabled based on territory property.
     *
     * @param pagePath                      {@link String}
     * @param shareType                     {@linkplain String}}
     * @param countryTerritoryMapperService {@link CountryTerritoryMapperService}
     * @return true/false {@link Boolean}
     */
    public static boolean isShareEnabled(final String pagePath, final String shareType,
                                         final CountryTerritoryMapperService countryTerritoryMapperService) {

        if (StringUtils.isBlank(pagePath) || StringUtils.isBlank(shareType)) {
            return false;
        }

        final String territoryCode = getTerritoryCodeFromPagePath(pagePath);

        LOGGER.debug("territoryCode:: {}", territoryCode);

        if (StringUtils.isNotBlank(territoryCode) && null != countryTerritoryMapperService
            && null != countryTerritoryMapperService.getTerritoryByTerritoryCode(territoryCode.toUpperCase())) {
            String shareStatus = StringUtils.EMPTY;
            if (MadisonConstants.PN_TWITTER_SHARE.equals(shareType)) {
                shareStatus = countryTerritoryMapperService.getTerritoryByTerritoryCode(territoryCode.toUpperCase())
                    .getTwitterShare();
            } else if (MadisonConstants.PN_LINKEDIN_SHARE.equals(shareType)) {
                shareStatus = countryTerritoryMapperService.getTerritoryByTerritoryCode(territoryCode.toUpperCase())
                    .getLinkedinShare();
            }

            LOGGER.debug("shareType:: {}", shareType);
            LOGGER.debug("shareStatus:: {}", shareStatus);

            if (StringUtils.isNotBlank(shareStatus) && MadisonConstants.TRUE_TEXT.equals(shareStatus)) {
                return true;
            }
        }

        return false;

    }

    /**
     * @param pagePath {@link String}
     * @return territoryCode {@link String}
     */
    public static String getTerritoryCodeFromPagePath(final String pagePath) {
        String territoryCode = StringUtils.EMPTY;
        if (pagePath.matches(MadisonConstants.DITA_CONTENT_SITE_HIERARCHY) && pagePath
            .startsWith(MadisonConstants.PWC_MADISON_CONTENT_BASEPATH + MadisonConstants.DITAROOT_TEXT)) {
            territoryCode = pagePath.split(DITAConstants.FORWARD_SLASH)[4];
        } else if (pagePath.matches(MadisonConstants.HOMEPAGE_CONTENT_SITE_HIERARCHY) && !pagePath
            .startsWith(MadisonConstants.PWC_MADISON_CONTENT_BASEPATH + MadisonConstants.DITAROOT_TEXT)) {
            territoryCode = pagePath.split(DITAConstants.FORWARD_SLASH)[3];
        } else if (pagePath.matches(MadisonConstants.MADISON_DAM_HIERARCHY) && !pagePath
            .startsWith(MadisonConstants.PWC_MADISON_DAM_BASEPATH + MadisonConstants.DITAROOT_TEXT)) {
            territoryCode = pagePath.split(DITAConstants.FORWARD_SLASH)[4];
        } else if (pagePath.startsWith(MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH)) {
            territoryCode = pagePath.split(DITAConstants.FORWARD_SLASH)[4];
        }
        return territoryCode;
    }

    /*
     * Method that convert date into particular format
     *
     * @param date {@link Date}
     * @return formattedDate {@link String}
     */
    public static String getDate(final Date date, String format) {
    	if(date == null) {
    		return null;
    	}
    	SimpleDateFormat formatter = new SimpleDateFormat(format);
    	return formatter.format(date);
    }


    /**
     * +
     * Method returns a boolean for a status check of if the logged in user an Admin or not.
     *
     * @param user
     * @param userManager
     * @return
     */
    public static boolean isAdmin(Authorizable user, UserManager userManager) {
        if (user == null || userManager == null) {
            return false;
        }
        try {
            if (user instanceof User && ((User) user).isAdmin()) {
                return true;
            }
            Authorizable admins = userManager.getAuthorizable(MadisonConstants.USER_GROUPS_ADMINISTRATORS);
            if (admins instanceof Group) {
                return ((Group) admins).isMember(user);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * +
     * Utility method to check if a given Group has either view, edit or Owner permission on a given folder. Once could use this method when you want to
     * determine the permission on a given folder, when the folder permissions are added through Touch UI.
     * The method will return the list of all groups who are the members of the group passed as reference to this method, and which has the exclusive
     * permission on the folder. permissionType parameter is for specifying the role eg; editor,viewer or owner
     *
     * @param currentfolder
     * @param uM
     * @param publisherGroup
     * @param adminResolver
     * @param permissionType
     * @param includeMembers :- this is a flag which determines whether to include members of the groups as well in the response
     * @return
     */
    public static Set<Authorizable> checkIfGroupHasPermissionOnFolder(Resource currentfolder, UserManager uM, Authorizable publisherGroup, ResourceResolver adminResolver, String permissionType, Boolean includeMembers) throws RepositoryException {

        if (currentfolder == null || uM == null || publisherGroup == null || adminResolver == null) {
            return Collections.emptySet();
        }

        // if publisherGroup is not a Group, let's return
        if(!publisherGroup.isGroup()){
            return Collections.emptySet();
        }
        LOGGER.debug("Inside checkIfGroupHasPermissionOnFolder");
        // do the operation with the request resolver
        Resource currentResource = currentfolder;

        Set<Authorizable> memberGroups = new HashSet<>();

        boolean hasPermission = false;
        //check if the resource is a folder
        String resourceType = StringUtils.EMPTY;
        if (currentResource != null) {
            resourceType = (String) currentResource.getValueMap().getOrDefault(JcrConstants.JCR_PRIMARYTYPE,
                org.apache.commons.lang3.StringUtils.EMPTY);
        }

        if (resourceType.contains(MadisonConstants.STR_FOLDER)) {
        	LOGGER.debug("It is a FOLDER");
            // get all the principle names of ths group
            final String path = currentResource.getPath();
            LOGGER.debug("Resource Path" + path);
            final Session session = adminResolver.adaptTo(Session.class);
            LOGGER.debug("Session" + session);
            if (session != null) {
                final AccessControlManager acm = session.getAccessControlManager();
                for (final AccessControlPolicy policy : acm.getPolicies(path)) {
                    if (policy instanceof AccessControlList) {
                        final AccessControlList accessControlList = (AccessControlList) policy;
                        
                        LOGGER.debug("ACL" + accessControlList);

                        if (accessControlList != null) {
                            for (final AccessControlEntry ace : accessControlList.getAccessControlEntries()) {
                            	LOGGER.debug("ACE" + ace);
                                final boolean isEveryone = ace.getPrincipal().equals(getEveryonePrincipal(session));
                                LOGGER.debug("isEveryone" + isEveryone);
                                if (isEveryone) {
                                    continue;
                                }
                                
                                LOGGER.debug("Permission Type" + permissionType);
                                // checks whether the group has specific role eg; editor, viewer or owner
                                if (null != permissionType && !permissionType.isEmpty()) {
                                    if (!ace.getPrincipal().toString().contains(permissionType)) {
                                        continue;
                                    }
                                }
                                
                                LOGGER.debug("After checking permission type");
                                
                                Group macAuthor = (Group) uM.getAuthorizable(ace.getPrincipal());
                                LOGGER.debug("mac author" + macAuthor);
                                //iterate te members of this group
                                Iterator<Authorizable> members = null;
                                if (macAuthor != null) {
                                    members = macAuthor.getDeclaredMembers();
                                }
                                if (members != null) {
                                    while (members.hasNext()) {
                                        LOGGER.debug("mac Authors");
                                        Authorizable member = members.next();
                                        LOGGER.debug("mac member" + member);
                                        if (member instanceof Group && (((Group) publisherGroup).isMember(member) || (publisherGroup.getID().equals(member.getID())))) {
                                            LOGGER.debug("Add mac member");
                                            memberGroups.add(member);
                                            LOGGER.debug("Added mac member");
                                            if(includeMembers){
                                                LOGGER.debug("include members");
                                                addAllMembers((Group) member, memberGroups,null, StringUtils.EMPTY);
                                            }
                                        }
                                        if(member instanceof User && includeMembers){
                                            checkAndAddUser(member,memberGroups,null,publisherGroup, StringUtils.EMPTY);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        LOGGER.debug("Member Groups" + memberGroups.size());
        return memberGroups;
    }

    private static void addAllMembers(Group parentGroup, Set<Authorizable> memberGroups,Set<String> memberGroupIds, String query){
        try {
            Iterator<Authorizable> allMembers = parentGroup.getMembers();
            while (allMembers.hasNext()){
                Authorizable member = allMembers.next();
                if(null != memberGroups){
                    memberGroups.add(member);
                }
                if(null != memberGroupIds){
                    if(query.isEmpty()) {
                        memberGroupIds.add(member.getID());
                    } else if(member.getID().contains(query)){
                        memberGroupIds.add(member.getID());
                    }
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(),e);
        }

    }

    private static void checkAndAddUser(Authorizable member,Set<Authorizable> memberGroups,Set<String> memberGroupIds, Authorizable publisherGroup, String query){
            try {
                if(publisherGroup instanceof Group) {
                    Iterator<Group> allParents = member.memberOf();
                    while(allParents.hasNext()){
                        Group parent = allParents.next();
                        if(publisherGroup.getID().equals(parent.getID())){
                            if(null != memberGroups){
                                memberGroups.add(member);
                            }
                            if(null != memberGroupIds){
                                if(query.isEmpty()) {
                                    memberGroupIds.add(member.getID());
                                } else if(member.getID().contains(query)){
                                    memberGroupIds.add(member.getID());
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (RepositoryException e) {
                LOGGER.error(e.getMessage(),e);
            }
    }

    private static Principal getEveryonePrincipal(final Session session) throws RepositoryException {
        if (session instanceof JackrabbitSession) {
            return ((JackrabbitSession) session).getPrincipalManager().getEveryone();
        } else {
            throw new UnsupportedOperationException(
                "Failed to retrieve everyone principal: JackrabbitSession expected.");
        }
    }

    /**
     * This method gets all groups and users belonging to parentGroup and has particular permission on a given folder
     * @param folderResource
     * @param uM
     * @param parentGroup
     * @param adminResolver
     * @param permissionType
     * @return
     * @throws RepositoryException
     */
    public static Set<String> getAllowedUsers(Resource folderResource, UserManager uM, Authorizable parentGroup, ResourceResolver adminResolver, String permissionType, String query) throws RepositoryException {

        if (folderResource == null || uM == null || adminResolver == null) {
            return Collections.emptySet();
        }

        LOGGER.debug("Inside getAllowedUsers");
        // do the operation with the request resolver
        Resource currentResource = folderResource;

        Set<String> memberGroupsIds = new HashSet<>();

        String resourceType = StringUtils.EMPTY;
        if (currentResource != null) {
            resourceType = (String) currentResource.getValueMap().getOrDefault(JcrConstants.JCR_PRIMARYTYPE,
                    org.apache.commons.lang3.StringUtils.EMPTY);
        }

        //check if the resource is a folder
        if (resourceType.contains(MadisonConstants.STR_FOLDER)) {
            LOGGER.debug("It is a FOLDER");
            // get all the principle names of ths group
            final String path = currentResource.getPath();
            LOGGER.debug("Resource Path" + path);
            final Session session = adminResolver.adaptTo(Session.class);
            if (session != null) {
                final AccessControlManager acm = session.getAccessControlManager();
                for (final AccessControlPolicy policy : acm.getPolicies(path)) {
                    if (policy instanceof AccessControlList) {
                        final AccessControlList accessControlList = (AccessControlList) policy;

                        LOGGER.debug("ACL" + accessControlList);

                        if (accessControlList != null) {
                            for (final AccessControlEntry ace : accessControlList.getAccessControlEntries()) {
                                LOGGER.debug("ACE" + ace);
                                final boolean isEveryone = ace.getPrincipal().equals(getEveryonePrincipal(session));
                                LOGGER.debug("isEveryone" + isEveryone);
                                if (isEveryone) {
                                    continue;
                                }

                                LOGGER.debug("Permission Type" + permissionType);
                                // checks whether the group has specific role eg; editor, viewer or owner
                                if (null != permissionType && !permissionType.isEmpty()) {
                                    if (!ace.getPrincipal().toString().contains(permissionType)) {
                                        continue;
                                    }
                                }

                                LOGGER.debug("After checking permission type");

                                Group macFolderUserGroup = (Group) uM.getAuthorizable(ace.getPrincipal());
                                //iterate te members of this group
                                Iterator<Authorizable> members = macFolderUserGroup.getDeclaredMembers();
                                while (members.hasNext()) {
                                    Authorizable member = members.next();
                                    LOGGER.debug("mac member" + member);
                                    if (member instanceof Group) {
                                        if(((Group) parentGroup).isMember(member)) {
                                            LOGGER.debug("Add mac member");
                                            if(query.isEmpty()){
                                                memberGroupsIds.add(member.getID());
                                            } else if(member.getID().contains(query)) {
                                                memberGroupsIds.add(member.getID());
                                            }
                                            addAllMembers((Group) member, null,memberGroupsIds, query);
                                        }
                                        }
                                    if(member instanceof User){
                                        checkAndAddUser(member,null,memberGroupsIds,parentGroup, query);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return memberGroupsIds;
    }

    /**
     * This method is used to return the current run mode
     *
     * @param slingSettingsService SlingSettingsService
     * @return runmode
     */
    public static String getCurrentRunmode(final SlingSettingsService slingSettingsService) {
        String runMode;
        final Set<String> runModes = slingSettingsService.getRunModes();
        final String publishMode = runModes.contains(Externalizer.PUBLISH) ? Externalizer.PUBLISH : StringUtils.EMPTY;
        runMode = runModes.contains(Externalizer.AUTHOR) ? Externalizer.AUTHOR : publishMode;
        return runMode;
    }


    /**
     * This is used to format date in required format (Used while creating review task)
     *
     * @param deadlineVal
     * @return
     * @throws ParseException
     */
    public static Date FormatDeadline(String deadlineVal) throws ParseException {
        Date deadline = null;
        if (deadlineVal != null && !deadlineVal.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            try {
                deadline = sdf.parse(deadlineVal);
            } catch (ParseException pe) {

                deadlineVal = deadlineVal.substring(0, deadlineVal.indexOf('T'));
                sdf = new SimpleDateFormat("yyyy-MM-dd");
                deadline = sdf.parse(deadlineVal);
            }
        }
        return deadline;
    }

    /**
     * This method is used to assign permissions to the given authorizable on the workflow instance
     *
     * @param user
     * @param wfID
     * @param resourceResolver
     * @param isReview
     * @throws AccessControlException
     * @throws RepositoryException
     */
    public static void assignPermissiontoUser(Authorizable user, String wfID, ResourceResolver resourceResolver, boolean isReview)
        throws AccessControlException, RepositoryException {
        Session session = resourceResolver.adaptTo(Session.class);
        AccessControlManager aMgr = session.getAccessControlManager();
        // create a privilege set
        Privilege[] privileges = new Privilege[]{aMgr.privilegeFromName(Privilege.JCR_READ),
            aMgr.privilegeFromName(Privilege.JCR_WRITE), aMgr.privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES)};
        String[] paths = new String[]{wfID};
        assignPermissionsToUser(user, paths, privileges, resourceResolver);

        paths = new String[]{"/etc/designs/fmdita", "/libs", "/libs/fmdita",
            isReview ? "/var/dxml/reviews" : "/var/dxml/approval", "/etc/fmdita/clientlibs"};
        privileges = new Privilege[]{aMgr.privilegeFromName(Privilege.JCR_READ)};
        assignPermissionsToUser(user, paths, privileges, resourceResolver);
    }


    /**
     * This method is used to populate user metadata in a review workflow instance
     *
     * @param user
     * @param upm
     * @param rootNode
     */
    public static void addUserMetadata(Authorizable user, UserPropertiesManager upm, Node rootNode) {
        Node node = null;
        try {
            node = JcrUtils.getOrCreateByPath(rootNode, user.getID(), false, "nt:unstructured", "nt:unstructured", true);

            final String ID = "id";
            String PROFILE_IMAGE_PATH = "/primary/image.prof.thumbnail.28.png";
            UserProperties profile = upm.getUserProperties(user, UserPropertiesService.PROFILE_PATH);
            if (profile != null) {
                Resource photo = profile.getResource(UserProperties.PHOTOS);
                if (photo != null) {
                    String thumbnail = photo.getPath() + PROFILE_IMAGE_PATH;
                    node.setProperty(UserProperties.PHOTOS, thumbnail);
                }
                node.setProperty(ID, user.getID());
                node.setProperty(UserProperties.DISPLAY_NAME, profile.getDisplayName());
                node.setProperty("lastOnlineTime", 0);
            }
            rootNode.getSession().save();
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static boolean assignPermissionsToUser(Authorizable user, String[] paths, Privilege[] privileges, ResourceResolver resourceResolver) {

        try {
            Session session = resourceResolver.adaptTo(Session.class);
            AccessControlManager aMgr = session.getAccessControlManager();
            for (String path : paths) {
                // create a privilege set with jcr:<permission-level> from privileges
                AccessControlList acl = null;
                // get first applicable policy (for nodes w/o a policy)
                if (null != resourceResolver.getResource(path)) {
                    AccessControlPolicyIterator it = aMgr.getApplicablePolicies(path);
                    while (it.hasNext()) {
                        AccessControlPolicy policy = it.nextAccessControlPolicy();
                        if (policy instanceof AccessControlList) {
                            acl = (AccessControlList) policy;
                            break;
                        }
                    }
                    if (acl == null) {
                        //node already has a policy
                        for (AccessControlPolicy policy : aMgr.getPolicies(path)) {
                            if (policy instanceof AccessControlList) {
                                acl = (AccessControlList) policy;
                                break;
                            }
                        }
                    }
                    // add a new one for the user's principal
                    if (null != acl) {
                        acl.addAccessControlEntry(user.getPrincipal(), privileges);
                    }
                    // the policy must be re-set
                    aMgr.setPolicy(path, acl);
                }
                return true;
            }
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Returns territory code for the path provided
     *
     * @param path {@link String}
     * @return territory code
     */
    public static final String getTerritoryCodeForPath(final String path) {
        return path != null && path.matches(MadisonConstants.MADISON_DAM_AND_CONTENT_HIERARCHY)
                ? path.replaceFirst(MadisonConstants.MADISON_DAM_AND_CONTENT_HIERARCHY, MadisonConstants.REGEX_MATCHING_GROUP_THREE)
                : null;
    }
    
    /**
     * Returns territory code for the XF path provided
     *
     * @param path {@link String}
     * @return territory code
     */
    public static final String getTerritoryCodeForXFPath(final String path) {
        return path != null && path.matches(MadisonConstants.MADISON_XF_HIERARCHY)
                ? path.replaceFirst(MadisonConstants.MADISON_XF_HIERARCHY, MadisonConstants.REGEX_MATCHING_GROUP_ONE)
                : null;
    }

    /**
     * Returns language code for the path provided
     *
     * @param path {@link String}
     * @return language code
     */
    public static final String getLanguageCodeForPath(final String path) {
        return path != null && path.matches(MadisonConstants.MADISON_DAM_AND_CONTENT_HIERARCHY)
                ? path.replaceFirst(MadisonConstants.MADISON_DAM_AND_CONTENT_HIERARCHY, MadisonConstants.REGEX_MATCHING_GROUP_FOUR)
                : null;
    }


    /**
     * Returns locale for the page path provided
     *
     * @param pagePath {@link String}
     * @return locale
     */
    public static final String getLocaleForPath(final String path) {
        final String territoryCode = getTerritoryCodeForPath(path);
        final String languageCode = getLanguageCodeForPath(path);
        return LocaleUtils.getLocale(territoryCode, languageCode);
    }

    /**
     * Checks if the {@link WCMMode} is {@value WCMMode#DISABLED}/Publish or not.
     *
     * @param request
     *            {@link SlingHttpServletRequest}
     * @return true only if the {@link WCMMode} is {@value WCMMode#DISABLED}/Publish
     */
    public static boolean isPublishMode(final SlingHttpServletRequest request) {
        final WCMMode mode = WCMMode.fromRequest(request);
        return mode == WCMMode.DISABLED;
    }

    /**
     * Returns the home page path with HTML extension for the given locale.
     * Note: The home page will be as per madison content hierarchy, it will not consider ditaroot i.e.
     * {@value MadisonConstants#MADISON_BASE_CONTENT_HIERARCHY} + "/territory/language"
     *
     * @param locale {@link String}
     * @return {@link String} null in case provided locale is null.
     */
    public static String getHomePageFromLocale(final String locale) {
        String homePagePath = getHomePagePathFromLocale(locale);
        return null == homePagePath ? null : homePagePath + MadisonConstants.HTML_EXTN;
    }

    /**
     * Returns the home page path for the given locale.
     * Note: The home page will be as per madison content hierarchy, it will not consider ditaroot i.e.
     * {@value MadisonConstants#MADISON_BASE_CONTENT_HIERARCHY} + "/territory/language"
     *
     * @param locale {@link String}
     * @return {@link String} null in case provided locale is null.
     */
    public static String getHomePagePathFromLocale(final String locale) {
        return null == locale ? null
            : MadisonConstants.MADISON_BASE_CONTENT_HIERARCHY + LocaleUtils.getTerritoryFromLocale(locale)
            + MadisonConstants.FORWARD_SLASH + LocaleUtils.getLanguageFromLocale(locale);
    }

    /**
     * Returns the global config page (i.e. the page on which global components like header and footer are authored)
     * for the given locale.
     * <br/>
     * Note: Global config page is the home page for the given locale.
     * {@value MadisonConstants#MADISON_BASE_CONTENT_HIERARCHY} + "/territory/language" (same for dita pages as well)
     *
     * @param locale           {@link String}
     * @param resourceResolver {@link ResourceResolver}
     * @return global config page.
     * @param xssapi
     *            {@link XSSAPI} required for secured logging          
     */
    @Nullable
    public static Page getGlobalConfigPageFromLocale(@Nonnull String locale, @Nonnull ResourceResolver resourceResolver, @Nonnull final XSSAPI xssapi) {
        Page globalConfigPage = null;
        String homePagePath = getHomePagePathFromLocale(locale);
        Resource homePageResource = resourceResolver.getResource(homePagePath);
        if (Objects.nonNull(homePageResource)) {
            globalConfigPage = homePageResource.adaptTo(Page.class);
        }
        LOGGER.debug("getGlobalConfigPageFromLocale -> homePagePath for {} locale is {}", xssapi.encodeForHTML(homePagePath), xssapi.encodeForHTML(locale));

        return globalConfigPage;
    }

    /**
     * Search {@code relativeResourcePath} under page's {@value com.day.cq.commons.jcr.JcrConstants#JCR_CONTENT} of the
     * {@code currentPage} and iterate up to parent page until we find the resource on a page or finish traversing the page
     * hierarchy
     *
     * @param currentPage          page from which searching will start
     * @param relativeResourcePath relative resource path from page's {@value com.day.cq.commons.jcr.JcrConstants#JCR_CONTENT}
     * @return Resource
     */
    @Nullable
    public static Resource getRelativeResourceFromContentTree(@Nonnull Page currentPage, @Nonnull String relativeResourcePath) {
        Resource resource = null;

        Page targetPage = currentPage;
        //iterate until we find the resource on a page or finish traversing the hierarchy
        while (Objects.isNull(resource) && Objects.nonNull(targetPage)) {
            resource = targetPage.getContentResource(relativeResourcePath);
            targetPage = targetPage.getParent();
        }

        if (Objects.nonNull(targetPage)) {
            LOGGER.debug("getRelativeResourceFromContentTree -> found {} on {} when started searching from {}",
                new Object[]{relativeResourcePath, targetPage.getPath(), currentPage.getPath()});
        } else {
            LOGGER.debug("getRelativeResourceFromContentTree -> didn't find {} when starting searching from {}", relativeResourcePath, currentPage.getPath());
        }

        return resource;
    }

    /**
     * Returns configured resource ({@code relativeResourcePath}) for the current page and locale.
     * Look under {@code currentPage}'s {@value com.day.cq.commons.jcr.JcrConstants#JCR_CONTENT} for relative resource ,
     * <br/>
     * if not defined, iterate up content tree to find the resource using {@link MadisonUtil#getRelativeResourceFromContentTree(Page, String)}
     * and check on global config page if not found in content tree as well. Global config page for the current page is
     * calculated using the method {@link MadisonUtil#getGlobalConfigPageFromLocale(String, ResourceResolver)}.
     *
     * @param currentPage          {@link Page}
     * @param relativeResourcePath relative resource path from page's {@value com.day.cq.commons.jcr.JcrConstants#JCR_CONTENT}
     * @param locale               locale to find the global config page
     * @param resourceResolver     {@link ResourceResolver}
     * @param xssapi
     *            {@link XSSAPI} required for secured logging          
     * @return Resource
     */
    @Nullable
    public static Resource getConfiguredResource(@Nonnull Page currentPage, @Nonnull String relativeResourcePath, @Nonnull String locale, @Nonnull ResourceResolver resourceResolver, @Nonnull XSSAPI xssapi) {

        Resource resource = MadisonUtil.getRelativeResourceFromContentTree(currentPage, relativeResourcePath);

        //if resource is still null then look on global config page
        if (Objects.isNull(resource)) {

            Page globalConfigPage = MadisonUtil.getGlobalConfigPageFromLocale(locale, resourceResolver, xssapi);
            resource = Objects.isNull(globalConfigPage) ? null : globalConfigPage.getContentResource(relativeResourcePath);
        }

        if (Objects.isNull(resource)) {
            LOGGER.debug("getInheritedResource -> can't find any configured resource for {} for locale {} for page {}",
                new Object[]{xssapi.encodeForHTML(relativeResourcePath), xssapi.encodeForHTML(locale), xssapi.encodeForHTML(currentPage.getPath())});
        } else {
            LOGGER.debug("getInheritedResource -> found {} for {} for locale {} for page {}",
                new Object[]{xssapi.encodeForHTML(resource.getPath()), xssapi.encodeForHTML(relativeResourcePath), xssapi.encodeForHTML(locale), xssapi.encodeForHTML(currentPage.getPath())});
        }

        return resource;
    }

    /**
     * Returns true if the link represents an internal link.
     * Any string which starts with {@value MadisonConstants#SLASH_CONTENT} will be considered as an internal link.
     * @param pagePath {@link String}
     * @return true if the string is an internal link, false otherwise
     */
    public static Boolean isLinkInternal(final String pagePath){

        if(StringUtils.isNotBlank(pagePath)){
            return pagePath.startsWith(MadisonConstants.SLASH_CONTENT);
        }
        return false;

    }

    /**
     * Parse the tag and get the localized tag title.
     * @param tags
     * @param resolver
     * @param localeStr
     * @return formatted tag
     */
    public static String formatPWCTags(String[] tags, ResourceResolver resolver, String localeStr){
        String formattedTag;
        TagManager tagManager = resolver.adaptTo(TagManager.class);
        Map<String, Set<String>> tagMap = new HashMap<>();
        String localizedTag;
        Locale localeObj = getLocaleObj(localeStr);
        for (String tag : tags) {
            String[] tagArray = null;
            if(null != tagManager){
                Tag tagObj = tagManager.resolve(tag);
                if(null != tagObj){
                /* separating key and value*/
                        tagArray = tag.split("/");
                        if(tagArray.length > 1){
                            Tag key = tagManager.resolve(tagArray[0]);
                            if(null != key){
                                String tagKey = key.getName();
                                if(tagMap.containsKey(tagKey)){
                                    Set<String> existingTagValue = tagMap.get(tagKey);
                                    localizedTag = tagObj.getTitle(localeObj);
                                    existingTagValue.add(localizedTag);
                                    getMultiLevelTags(tagObj,localeObj,key.getPath(),existingTagValue);
                                    tagMap.put(tagKey, existingTagValue);
                                }else{
                                    Set<String> tagValues = new HashSet<String>();
                                    localizedTag = tagObj.getTitle(localeObj);
                                    tagValues.add(localizedTag);
                                    getMultiLevelTags(tagObj,localeObj,key.getPath(),tagValues);
                                    tagMap.put(tagKey,tagValues);
                                }
                            }
                        }
                }
            }
        }
        formattedTag = generateTagString(tagMap);
        return formattedTag;
    }
    
    /**
     * This method creates multi level tag values
     * @param tag
     * @param locale
     * @param base
     * @param values
     */
    private static void getMultiLevelTags(Tag tag,Locale locale,String base,Set<String> values) {
    	Tag parent = tag.getParent();
    	while(parent != null && !parent.getPath().equalsIgnoreCase(base)) {
    		values.add(parent.getTitle(locale));
    		parent = parent.getParent();    		
    	}    	
    }

    /**
     * get locale string and returns locale object
     * @param localeString
     * @return locale object
     */
    public static Locale getLocaleObj(String localeString){
        Locale locale = null;
        if(null != localeString && StringUtils.isNotBlank(localeString)){
            String[] localeArray = localeString.split("_");
            locale = new Locale(localeArray[0], localeArray[1]);
        }
        return locale;
    }

    /**
     * generate tag string in SnP consumable format (example 'media_type:Text|category:Auditing,Cybersecurity and privacy')
     * @param tagMap
     * @return tag string
     */
    private static String generateTagString(Map<String, Set<String>> tagMap){
        StringBuilder tagString = new StringBuilder();
        int firstIndex = 1;
        for (Map.Entry<String, Set<String>> entry: tagMap.entrySet()) {
            tagString.append(entry.getKey());
            tagString.append(":");
            int index = 1;
            for (String tagValue: entry.getValue()) {
                tagString.append(tagValue);
                if(entry.getValue().size() != index){
                    tagString.append(",");
                }
                index++;
            }
            if(tagMap.size() != firstIndex){
                tagString.append("|");
            }
            firstIndex++;
        }
        return tagString.toString();
    }

    /**
     * Creates query to fetch DitaMap path from dita file
     *
     * @param ditaPath
     * @return
     */
    public static Map<String, Object> createDitaMapQuery(String ditaPath) {
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("path", MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH);
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("property", "@jcr:content/" + DITAConstants.PN_FMDITATOPICREFS);
        predicateMap.put("property.value", "," + ditaPath);
        predicateMap.put("p.limit", "-1");
        return predicateMap;
    }

     /**
     * @param visibleToInternal boolean value to determine whether group is visible to internal user or not
     * @param visibleToExternal boolean value to determine whether group is visible to external user or not
     * @return Returns boolean value true or false based on the visibility criteria of groups 
     */
    
    public static boolean isGroupsOrGroupLinksVisible(String[] userType, Boolean isInternalUser) {
        boolean isAuthorizeUser = Boolean.FALSE;
        boolean visibleToInternal = Arrays.asList(userType).contains(MadisonConstants.INTERNAL_USER);
        boolean visibleToExternal = Arrays.asList(userType).contains(MadisonConstants.EXTERNAL_USER);
        visibleToInternal = visibleToExternal ? visibleToInternal : Boolean.TRUE;
        if(visibleToInternal && isInternalUser) {
            isAuthorizeUser = Boolean.TRUE;
        }
        if(visibleToExternal && !isInternalUser) {
            isAuthorizeUser = Boolean.TRUE;
        }
        return isAuthorizeUser;
    }
    
    /**
     * gives homepage for the given page
     * @param page
     * @return homepage
     */
    
    public static Page getHomePage(Page page) {
        if(page != null) {
            String path = page.getPath();
            if(path.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
                String ditaHomePage = StringUtils.EMPTY;
                if(StringUtils.isNotBlank(path.replaceFirst(MadisonConstants.MADISON_PAGE_HIERARCHY, MadisonConstants.REGEX_MATCHING_GROUP_TWO))) {
                    ditaHomePage = MadisonConstants.FORWARD_SLASH + 
                            path.replaceFirst(MadisonConstants.MADISON_PAGE_HIERARCHY, 
                                    MadisonConstants.REGEX_MATCHING_GROUP_FIVE).split(MadisonConstants.FORWARD_SLASH)[1];
                }
                path = path.replaceFirst(MadisonConstants.MADISON_PAGE_HIERARCHY, MadisonConstants.REGEX_MATCHING_GROUP_ONE) + ditaHomePage;
            }
            return page.getPageManager().getPage(path);
        }
        return null;
    }
    
    /**
     * gives homepage from basePath
     * @param page
     * @return homepage
     */
    
    public static Page getHomePageFromBasePath(Page page) {
        if(page != null) {
            String path = page.getPath();
            String basePath = page.getProperties().get(MadisonConstants.DITA_CONTENT_BASE_PATH, String.class);
            if(basePath != null) {
            	path = basePath;
            } else
            if(path.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
                String ditaHomePage = StringUtils.EMPTY;
                if(StringUtils.isNotBlank(path.replaceFirst(MadisonConstants.MADISON_PAGE_HIERARCHY, MadisonConstants.REGEX_MATCHING_GROUP_TWO))) {
                    ditaHomePage = MadisonConstants.FORWARD_SLASH + 
                            path.replaceFirst(MadisonConstants.MADISON_PAGE_HIERARCHY, 
                                    MadisonConstants.REGEX_MATCHING_GROUP_FIVE).split(MadisonConstants.FORWARD_SLASH)[1];
                }
                path = path.replaceFirst(MadisonConstants.MADISON_PAGE_HIERARCHY, MadisonConstants.REGEX_MATCHING_GROUP_ONE) + ditaHomePage;
            }
            return page.getPageManager().getPage(path);
        }
        return null;
    }

    /**
     * Returns territory code for the page URL provided. for ex: given the page URL like
     * http://pwc.madison.com/content/pwc-madison/us/en/mypage.html, the territory code returned is us.
     *
     * @param pageUrl
     *            {@link String}
     * @return territory code
     */
    public static final String getTerritoryCodeForPathUrl(final String pageUrl) {
        return pageUrl != null && pageUrl.matches(MadisonConstants.MADISON_PAGE_URL_REGEX) ? pageUrl.replaceFirst(
                MadisonConstants.MADISON_PAGE_URL_REGEX, MadisonConstants.REGEX_MATCHING_GROUP_TWO) : null;
    }

    /**
     * Returns language code for the page URL provided.for eg: given the page URL like
     * http://pwc.madison.com/content/pwc-madison/us/en/mypage.html, the language code returned is en.
     *
     * @param pageUrl
     *            {@link String}
     * @return language code
     */
    public static final String getLanguageCodeForPathUrl(final String pageUrl) {
        return pageUrl != null && pageUrl.matches(MadisonConstants.MADISON_PAGE_URL_REGEX) ? pageUrl.replaceFirst(
                MadisonConstants.MADISON_PAGE_URL_REGEX, MadisonConstants.REGEX_MATCHING_GROUP_THREE) : null;
    }

    /**
     * Resolves the given path to an {@link Asset} using the provided {@link ResourceResolver}.
     * @param assetPath {@link String}
     * @param resourceResolver {@link ResourceResolver}
     * @return Asset for the given path, null otherwise
     */
    @Nullable
    public static final Asset getAssetFromPath(String assetPath, ResourceResolver resourceResolver){
        Resource assetResource = resourceResolver.getResource(assetPath);
        if (Objects.isNull(assetResource)) {
            LOGGER.debug("Resource doesn't exist, invalid assetPath {}", assetPath);
            return null;
        }

        Asset asset = DamUtil.resolveToAsset(assetResource);
        if (Objects.isNull(asset)) {
            LOGGER.debug("resource {} doesn't points to a valid asset resource.", assetResource.getPath());
            return null;
        }

        return asset;
    }

    /**
     * 
     * @param request
     * @return
     */
    public static String getTokenCookieValue(SlingHttpServletRequest request) {
        Cookie loginToken = request.getCookie(TokenCookie.NAME);
        if (loginToken != null) {
            return loginToken.getValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * This method verifies if the given/users group has permission on given folder
     *
     * @param resolver
     * @param destResource
     * @param principalName
     * @param includeMembers
     * @return territoryGroups
     * @throws RepositoryException
     */
    public static Set<Authorizable> getTerritoryGroups(final ResourceResolver resolver, final Resource destResource,
                                                       final String principalName, final Boolean includeMembers) throws RepositoryException {
        if (null == resolver || null == destResource || null == principalName || null == includeMembers) {
            return Collections.emptySet();
        }
        final UserManager userManager = resolver.adaptTo(UserManager.class);
        final Authorizable authorGroup = userManager.getAuthorizable(principalName);
        // Get all users/userGroups with Editor Role for this subscriber
        Set<Authorizable> group = MadisonUtil
            .checkIfGroupHasPermissionOnFolder(destResource, userManager, authorGroup, resolver,
                MadisonConstants.PN_EDITOR, includeMembers);

        if (group.isEmpty() && !destResource.getName().equals(MadisonConstants.MADISON_DAM_ROOT)) {
            final Resource parentPath = destResource.getParent();
            group = getTerritoryGroups(resolver, parentPath, principalName, includeMembers);
        }
        return group;
    }

    /**
     * @param seconds total seconds in long
     * @return return time in HH:MM:SS
     */
    public static String getTime(long seconds) {
        long hour = TimeUnit.SECONDS.toHours(seconds);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (hour * 60);
        long second = seconds - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        StringBuilder time = new StringBuilder();
        if (hour < 10) {
            time.append("0");
        }
        time.append(hour).append(":");
        if (minute < 10) {
            time.append("0");
        }
        time.append(minute).append(":");
        if (second < 10) {
            time.append("0");
        }
        time.append(second);
        return time.toString();
    }

    /**
     * Returns the given {@link Page}'s {@link ContentAuthorization} information.
     * 
     * @param page
     *            {@link Page}
     * @return {@link ContentAuthorization}
     */
    public static ContentAuthorization getPageContentAuthorization(final Page page) {
        final InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        return getContentAuthorization(inheritanceValueMap);
    }

    /**
     * Returns the given Asset's Metadata {@link Resource}'s {@link ContentAuthorization} information.
     * 
     * @param asset
     *            {@link Resource}
     * @return {@link ContentAuthorization}
     */
    public static ContentAuthorization getAssetContentAuthorization(final Resource assetMetadata) {
        final InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(assetMetadata);
        return getContentAuthorization(inheritanceValueMap);
    }

    /**
     * Returns the given {@link InheritanceValueMap}'s {@link ContentAuthorization} information. It returns information by extracting
     * information from properties i.e {@value ContentAuthorizationConstants#PAGE_PROPERTY_AUDIENCE_TYPE},
     * {@value ContentAuthorizationConstants#PAGE_PROPERTY_ACCESS_LEVEL},
     * {@value ContentAuthorizationConstants#PAGE_PROPERTY_LICENSE} AND
     * {@value ContentAuthorizationConstants#PAGE_PROPERTY_PRIVATE_GROUP}. </br>
     * <b>NOTE: In case the {@value ContentAuthorizationConstants#PAGE_PROPERTY_AUDIENCE_TYPE} property is not present
     * on page, the content is considered to be accessible by all users either logged in or not.</b>
     *
     * @param inheritanceValueMap
     *            {@link InheritanceValueMap}
     * @return {@link ContentAuthorization}
     */
    private static ContentAuthorization getContentAuthorization(final InheritanceValueMap inheritanceValueMap) {
        final ContentAuthorization contentAuthorization = new ContentAuthorization();
        contentAuthorization.setAudienceType(
                inheritanceValueMap.getInherited(ContentAuthorizationConstants.PAGE_PROPERTY_AUDIENCE_TYPE,
                        AudienceType.INTERNAL_AND_EXTERNAL.getValue()));
        String accessLevel = inheritanceValueMap.getInherited(ContentAuthorizationConstants.PAGE_PROPERTY_ACCESS_LEVEL,
                String.class);
        contentAuthorization.setAccessLevel(StringUtils.isBlank(accessLevel)
                && (contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_AND_EXTERNAL.getValue())
                        || contentAuthorization.getAudienceType().equals(AudienceType.EXTERNAL_ONLY.getValue()))
                                ? AccessLevel.FREE.getValue()
                                : accessLevel);
        contentAuthorization.setLicenses(
                inheritanceValueMap.getInherited(ContentAuthorizationConstants.PAGE_PROPERTY_LICENSE, String[].class));
        contentAuthorization.setPrivateGroups(inheritanceValueMap
                .getInherited(ContentAuthorizationConstants.PAGE_PROPERTY_PRIVATE_GROUP, String[].class));
        return contentAuthorization;
    }

    /**
     * Checks whether a given folder is private
     * @param resource
     * @return
     */
    public static boolean isPrivate(Resource resource) {
        if(null!=resource) {
            String path = resource.getPath();
            Session session = resource.getResourceResolver().adaptTo(Session.class);
            try {
                if (null != session) {
                    AccessControlManager acm = session.getAccessControlManager();

                    for (AccessControlPolicy policy : acm.getPolicies(path)) {
                        if (policy instanceof AccessControlList) {
                            AccessControlList accessControlList = (AccessControlList) policy;
                            for (AccessControlEntry ace : accessControlList.getAccessControlEntries()) {
                                boolean isEveryone = ace.getPrincipal().equals(AccessControlUtils.getEveryonePrincipal(session));
                                boolean isJCRALL = false;
                                boolean isAllow = true;
                                for (Privilege privilege : ace.getPrivileges()) {
                                    if (privilege.getName().equalsIgnoreCase("jcr:all")) {
                                        isJCRALL = true;
                                    }
                                }
                                if (ace instanceof JackrabbitAccessControlEntry) {
                                    isAllow = ((JackrabbitAccessControlEntry) ace).isAllow();
                                }

                                if (isEveryone && isJCRALL && !isAllow) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            } catch (RepositoryException e) {
                LOGGER.error("RepositoryException in isPrivate method of MadisonUtil {}", e);
            }
        }
        return false;
    }

    /**
     * Returns locale for the provided page path based on various conditions
     * @param pagepath
     * @return
     */
        
    public static String getLocaleOfCurrentPage(UserRegPagesPathProvidesService userRegPagesPathProvidesService, CountryTerritoryMapperService countryTerritoryMapperService,
            SlingHttpServletRequest slingRequest) {
        String pagePath = slingRequest.getRequestURI();
        if (pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
            return slingRequest.getParameter(MadisonConstants.LOCALE_QUERY_PARAM);
        }
        else if (pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)) {
            return getLocaleForPath(pagePath);
        } else {
            return countryTerritoryMapperService.getDefaultLocale();
        }
    }
	
	/**
     * This Method List the all Replication Agent Manager Name based on pattern.
     *
     * @param agentNameType
     * @param agentMgr
     * @return Agents
     */
    public static List<String> getReplicationAgents(final String agentNameType, final AgentManager agentMgr) {
        final Map<String, Agent> agents = agentMgr.getAgents();
        final List<String> agentNameList = new ArrayList<>();
        for (final Entry<String, Agent> agent : agents.entrySet()) {
            if (agent.getKey().startsWith(agentNameType)) {
                agentNameList.add(agent.getKey());
            }
        }
        return agentNameList;
    }


    /**
     * Returns page title for the current page. <br>
     * Fallback hierarchy for: <br>
     * Dita Pages - pageTitle, jcr:title, node name <br>
     * Normal Pages - navTitle, jcr:title, node name<br><br>
     * <p>
     * Note - All pages having {@value MadisonConstants#DITA_CONTENT_PAGE_TEMPLATE} as template name will be considered
     * a dita page.
     *
     * @param page {@link Page}
     * @return page title
     */
    public static String getPageTitle(Page page) {
        if (Objects.isNull(page)) {
            LOGGER.debug("getPageTitle -> page is null");
            return null;
        }
        String pageTitle;
        if (null != page.getTemplate() && page.getTemplate().getName().equals(MadisonConstants.DITA_CONTENT_PAGE_TEMPLATE)) {
            pageTitle = StringUtils.isNotBlank(page.getPageTitle()) ? page.getPageTitle()
                : page.getTitle();
        } else {
            pageTitle = StringUtils.isNotBlank(page.getNavigationTitle()) ? page.getNavigationTitle()
                : page.getTitle();
        }

        if (StringUtils.isBlank(pageTitle)) {
            pageTitle = page.getName();
        }

        LOGGER.debug("getPageTitle -> pageTitle for {} : {}", page.getPath(), pageTitle);
        return pageTitle;
    }
    
    /**
     * Get entry for passed index
     * @param set
     * @param value
     * @return
     */
    public static String getIndexValue(Set<String> set, int value) {
        Instant begin = Instant.now();
        if (set == null || set.isEmpty()) {
            return null;
        } else if (set.size() < value) {
            String mssg = String.format("index %s > %s size", value, set.size());
            LOGGER.debug(mssg);
            throw new IllegalArgumentException(mssg);
        }
        int result = 0;
        for (String entry : set) {
            if (value == result) {
                Instant done = Instant.now();
                LOGGER.debug("getIndexValue took {} ms to complete",calculateDuration(begin, done));
                return entry;
            }
            result++;
        }
        return null;
    }
    
    /**
     * 
     * @param start
     * @param end
     * @return
     */
    public static long calculateDuration(Instant start,Instant end) {
        Duration between = Duration.between(start, end);
        return between.toMillis();
    }

    /**
     * Creates and stores post-processing log on repository under fmdita history-path
     * @param resolver
     * @param postProcessing
     * @param outputHistoryPath
     * @param session
     */
    public static void storeErrorLog(ResourceResolver resolver, PostProcessing postProcessing, String outputHistoryPath, Session session){
        if(null == resolver || null == postProcessing || StringUtils.isBlank(outputHistoryPath) || null == session){
            return;
        }
        Resource historyResource = resolver.getResource(outputHistoryPath);
        if(null == historyResource){
            return;
        }
        try{
            Node publishOutput = historyResource.adaptTo(Node.class);
            Node logNode = publishOutput.addNode(DITAConstants.POSTPROCESSING_LOG_FILENAME, DamConstants.NT_DAM_ASSET);
            Node jcrNode = logNode.addNode(JcrConstants.JCR_CONTENT, DamConstants.NT_DAM_ASSETCONTENT);
            Node renditionsNode = jcrNode.addNode(DamConstants.RENDITIONS_FOLDER, JcrConstants.NT_FOLDER);
            Node originalNode = renditionsNode.addNode(DamConstants.ORIGINAL_FILE, JcrConstants.NT_FILE);
            Node fileNode = originalNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
            ValueFactory factory = session.getValueFactory();
            fileNode.setProperty(JcrConstants.JCR_DATA, factory.createValue(postProcessing.getException().toString()));
            fileNode.setProperty(JcrConstants.JCR_MIMETYPE, DITAConstants.TEXT_PLAIN);
            resolver.commit();
        }catch (RepositoryException | PersistenceException e){
            LOGGER.error("Error while storing apps.fmdita.incremental-publishg logfile: ", e);
        }
    }

    /**
     * set ditaOTFailure property to true
     * @param failure
     * @param path
     * @param session
     */
    public static void setFailure(boolean failure, String path, Session session)  {
        if(null == session){
            return;
        }
        try{
            Node historyNode = session.getNode(path);
            if(null == historyNode){
                return;
            }
            historyNode.setProperty(DITAConstants.DITAOT_FAILURE, failure);
            session.save();
        }catch (Exception e){
            LOGGER.error("setFailure() - Error while updating generation status: ",e);
        }
    }

    /**
     * returns docContext used for document search
     * @param path
     * @param docContextSearchIdentifier
     * @return pwcDocContext
     */
    public static String getPwcDocContext(String path, ResourceResolver resolver, boolean isPubPoint){
        String pwcDocContext = StringUtils.EMPTY;
        if(StringUtils.isBlank(path)){
            return pwcDocContext;
        }
        if(path.contains(MadisonConstants.HTML_EXTN)){
            path = StringUtils.substringBefore(path, MadisonConstants.HTML_EXTN);
        }
        String basePath = getBasePath(path, resolver);
        if(isLevelSet(basePath, resolver)){
            String basePathHash = HashGeneratorUtils.generateCheckum(basePath, HashGeneratorUtils.TypeOfHash.SHA1);
            if(isPubPoint){
                pwcDocContext = basePathHash;
            }else{
                String level = getStringProperty(path, resolver, DITAConstants.PROPERTY_LEVEL);
                pwcDocContext = StringUtils.isNotBlank(level) ? basePathHash + level + DITAConstants.SEARCH_DOC_DELIMETER: basePathHash;
            }
        }else{
            pwcDocContext = isPubPoint ? truncatePath(basePath) : truncatePath(path);
        }

        return pwcDocContext;
    }

    public static String getBasePath(String path, ResourceResolver resolver){
        String basePath = StringUtils.EMPTY;
        Resource pageResource = resolver.getResource(path.concat(DITAConstants.JCR_CONTENT));
        if(null == pageResource){
            return  basePath;
        }
        ValueMap properties = pageResource.getValueMap();
        if(properties.containsKey(DITAConstants.BASE_PATH)){
            basePath = properties.get(DITAConstants.BASE_PATH, String.class);
        }else{
           String  parentPath = path.substring(0, path.lastIndexOf(DITAConstants.FORWARD_SLASH));
           if(!parentPath.equals(MadisonConstants.MADISON_SITES_ROOT)){
               basePath = getBasePath(parentPath, resolver);
           }
        }
        return basePath;
    }

    public static String getStringProperty(String path, ResourceResolver resolver, String propertyName){
        String propertyValue = StringUtils.EMPTY;
        Resource pageResource = resolver.getResource(path.concat(DITAConstants.JCR_CONTENT));
        if(null == pageResource){
            return  propertyValue;
        }
        ValueMap properties = pageResource.getValueMap();
        if(properties.containsKey(propertyName)){
            propertyValue = properties.get(propertyName, String.class);
        }
        return propertyValue;
    }
    /**
     * Method to getcontentFieldValue
     *
     * @param pageProps {@link ValueMap}
     * @return contentFieldValue {@link String}
     */
    public static String getcontentFieldValue(final ValueMap pageProps) {
        final String contentId = pageProps.get(DITAConstants.META_CONTENT_ID, StringUtils.EMPTY);
        if (contentId.isEmpty()) {
            final String standardSetter = pageProps.get(DITAConstants.META_STANDARD_SETTERS, StringUtils.EMPTY);
            final String pwc_authored_content = Item.getPwcSourceValue();
            return standardSetter.equalsIgnoreCase(pwc_authored_content) ? pageProps.get(DITAConstants.META_CONTENT_TYPE, StringUtils.EMPTY) : standardSetter;
        } else {
            return contentId;
        }
    }

    /**
     * Random number generator
     * @return DocContextSearchIdentifier
     */
    public static String getRandomUUID(){
        return UUID.randomUUID().toString();
    }
    
    /**
     * Returns the formatted revised date of the given {@link Page}. It first checks for
     * {@value DITAConstants#META_REVISION_DATE} if not present, it checks for
     * {@value DITAConstants#META_PUBLICATION_DATE} otherwise return empty formatted date.
     * 
     * @param page
     *            {@link Page}
     * @return {@link String}
     */
    public static String getPageRevisedDate(final Page page) {
        final Date revisedDate = page.getProperties().get(DITAConstants.META_REVISION_DATE,
                page.getProperties().get(DITAConstants.META_PUBLICATION_DATE, Date.class));
        if (null != revisedDate) {
            final SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
            return formatter.format(revisedDate.getTime());
        } else {
            return StringUtils.EMPTY;
        }
    }
    
    public static String getDuration(final Resource assetResource) {
        if(null != assetResource) {
            final Resource metadataResource = assetResource.getChild("jcr:content/metadata");
            if(null != metadataResource) {
                return metadataResource.getValueMap().get("duration", String.class);
            }
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * Method used to fetch date format
     *
     * @return dateFormat {@link String}
     */
    public static String fetchDateFormat(String territory, CountryTerritoryMapperService countryTerritoryMapperService, String defaultDateFormat) {
    	String dateFormat = StringUtils.EMPTY;
		if (territory != null && territory.equals(MadisonConstants.JP_TERRITORY_CODE)) {
			dateFormat = countryTerritoryMapperService.getTerritoryByTerritoryCode(territory).getDateFormat();
		}
		else {
			dateFormat = defaultDateFormat;
		}
		return dateFormat;
    }

    public static void setLevel(ArrayList<String> pathList, ResourceResolver resolver){
        LOGGER.debug("inside setLevel");
        try {
            HashMap<String, LevelModel> levelMap = new HashMap<>();
            String level = StringUtils.EMPTY;
            for (int index = 0; index < pathList.size(); index++) {
                Resource currentResource = resolver.getResource(pathList.get(index).concat(DITAConstants.JCR_CONTENT));
                if(null != currentResource){
                    ModifiableValueMap valueMap = currentResource.adaptTo(ModifiableValueMap.class);
                    if(index == 0){
                        String basePath = getBasePath(pathList.get(index), resolver);
                        level = "0";
                        Resource baseResource = resolver.getResource(basePath.concat(DITAConstants.JCR_CONTENT));
                        if(null != baseResource){
                            ModifiableValueMap BasevalueMap = baseResource.adaptTo(ModifiableValueMap.class);
                            BasevalueMap.put(DITAConstants.PROPERTY_LEVEL, level);
                            LevelModel levelModel = new LevelModel(level);
                            levelMap.put(basePath, levelModel);
                        }
                        level = "0X1";
                        setBasePathIdentifier(pathList.get(index), resolver);
                    }else{
                        if(pathList.get(index).contains(pathList.get(index-1)) && pathList.get(index).replaceFirst(pathList.get(index-1), "").startsWith(DITAConstants.FORWARD_SLASH)){
                            level = levelMap.get(pathList.get(index-1)).getLevel() + DITAConstants.SEARCH_DOC_DELIMETER + "1";
                        }else{
                            level = fetchLevel(pathList.get(index), pathList.get(index-1), levelMap);
                        }
                    }
                    valueMap.put(DITAConstants.PROPERTY_LEVEL, level);
                    LevelModel levelModel = new LevelModel(level);
                    levelMap.put(pathList.get(index), levelModel);
                    LOGGER.debug("Level: {} set for path: {}", level, currentResource.getPath());
                }
            }
        }catch (Exception e){
            LOGGER.error("Error while setting level for doc context search", e);
        }
    }

    private static String fetchLevel(String currentPath, String previousPath, HashMap<String, LevelModel> levelMap){
        String level = StringUtils.EMPTY;
        while (!currentPath.contains(previousPath)){
            previousPath = previousPath.substring(0, previousPath.lastIndexOf(DITAConstants.FORWARD_SLASH));
        }
        if(levelMap.containsKey(previousPath) && currentPath.replaceFirst(previousPath, "").startsWith(DITAConstants.FORWARD_SLASH)){
            level = populateLevelFromParent(previousPath, levelMap);
        }else{
            previousPath = previousPath.substring(0, previousPath.lastIndexOf(DITAConstants.FORWARD_SLASH));
            if(!previousPath.equals(MadisonConstants.MADISON_SITES_ROOT)){
                level = fetchLevel(currentPath, previousPath, levelMap);
            }
        }
        return  level;
    }

    private static String populateLevelFromParent(String parentPath, HashMap<String, LevelModel> levelMap){
        String updatedLevel = StringUtils.EMPTY;
        LevelModel levelModel = levelMap.get(parentPath);
        String level = levelModel.getLevel();
        int counter = levelModel.getCounter();
        counter++;
        levelModel.setCounter(counter);
        levelMap.put(parentPath, levelModel);
        updatedLevel = level + DITAConstants.SEARCH_DOC_DELIMETER + counter;
        return  updatedLevel;
    }

    public static void setBasePathIdentifier(String path, ResourceResolver resolver){
        String basePath = getBasePath(path, resolver);
        Resource baseResource = resolver.getResource(basePath.concat(DITAConstants.JCR_CONTENT));
        if(null != baseResource){
            ModifiableValueMap properties = baseResource.adaptTo(ModifiableValueMap.class);
            properties.put(DITAConstants.PROPERTY_IS_LEVEL_set, true);
        }
    }

    public static String truncatePath(String path){
        String truncatedPath = StringUtils.EMPTY;

        if(StringUtils.isBlank(path)){
            return truncatedPath;
        }
        String clippedBasePath = path.matches(MadisonConstants.MADISON_DOC_CONTEXT_REGEX) ? path.replaceFirst(
                MadisonConstants.MADISON_DOC_CONTEXT_REGEX, MadisonConstants.REGEX_MATCHING_GROUP_TWO) : null;
        if(null == clippedBasePath){
            return  path;
        }
        String[] pathArray = clippedBasePath.split("/");
        for(int index=0; index<pathArray.length; index++){
            if(pathArray[index].length()>DITAConstants.SEARCH_DOC_TRUNCATE_MAX_LIMIT){
                char[] subElementArray = pathArray[index].toCharArray();
                int middleIndex = subElementArray.length % 2 == 0 ? subElementArray.length/2 : (subElementArray.length+1)/2;
                pathArray[index] = String.valueOf(subElementArray[0]) + String.valueOf(subElementArray[middleIndex-1]) + String.valueOf(subElementArray[pathArray[index].length()-1]);
            }
        }
        return String.join("", pathArray);
    }

    private static boolean isLevelSet(String basePath, ResourceResolver resolver){
        boolean isLevelSet = false;
        if(StringUtils.isBlank(basePath) || null == resolver){
            return isLevelSet;
        }
        Resource currentResource = resolver.getResource(basePath.concat(DITAConstants.JCR_CONTENT));
        if(null != currentResource){
            ValueMap properties = currentResource.getValueMap();
            if(properties.containsKey(DITAConstants.PROPERTY_IS_LEVEL_set)){
                isLevelSet = properties.get(DITAConstants.PROPERTY_IS_LEVEL_set, Boolean.class);
            }
        }
        return isLevelSet;
    }

	/**
	 * Method used to find anchor href value of 'pwc-xref' class and containing text within 'xref-info' class
	 * 
	 * @param valueMap {@link ValueMap}
	 * @param typeOfContent {@link String}
	 * 
	 * @return outputArray {@link String}
	 * @throws IOException
	 */
	public static String[] fetchElementsFromHTML(final ValueMap valueMap, final String typeOfContent) throws IOException {
		final String[] outputArray = new String[2];
		final Object flattened = valueMap.get(MadisonConstants.FLATTENED_PROPERTY, Object.class);
		if(flattened instanceof InputStream) {
			final String html = IOUtils.toString((InputStream)flattened, MadisonConstants.UTF_8);
			final Document doc = Jsoup.parseBodyFragment(html, MadisonConstants.UTF_8);
			final Element anchorLink = doc.select(MadisonConstants.HTML_ANCHOR_PWC_XREF).first();
			if (anchorLink != null) {
				outputArray[0] = anchorLink.attr(MadisonConstants.HTML_HREF);
				if (typeOfContent.equals(MadisonConstants.FLATTENED_NEWS) && !anchorLink.ownText().isEmpty()) {
					outputArray[1] = anchorLink.ownText();
				} else {
					final Element divElement = anchorLink.select(MadisonConstants.HTML_XREF_INFO_DIV).first();
					if (divElement != null) {
						outputArray[1] = divElement.text();
					}
				}
			}
		}
		return outputArray;
	}
    /*
    * Method to get current year
     */
    public static String getCurrentYear(){
        LocalDate currentDate = LocalDate.now();
        return String.valueOf(currentDate.getYear());
    }

    /**
     * This method returns true if the runmode is author.
     *
     * @param pSlingSettingsService the sling settings service
     * @return true, if is author
     */
    public static boolean isAuthor(SlingSettingsService pSlingSettingsService) {
        return pSlingSettingsService.getRunModes().contains("author");
    }
}
