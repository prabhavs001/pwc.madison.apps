package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Servlet that returns the HTML listing users and groups belonging to parent group specific to a territory.
 */
@Component(
    service = Servlet.class,
    property = {
            Constants.SERVICE_DESCRIPTION
                    + "=User picker servlet that gives users/groups under a territory specific group",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/pwc/userpicker",
            "sling.servlet.extensions=" + "html" })
public class UserPickerServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UserPickerServlet.class);
    private static final String ASSET_PATH = "assetPath";
    private static final String Query = "query";
    private static final String GROUP_TYPE = "group";
    private static final String DITA_ROOT = "ditaroot/";
    private static final String ALL_AUTHORS = "allAuthors";
    private static final String AUTHORS = "authors";
    private static final String START = "start";
    private static final String END = "end";
    private static final long serialVersionUid = 1L;
    private ResourceResolver madisonServiceUserResolver;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private XSSAPI xssAPI;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        LOG.debug("Request received");
        final Instant started = Instant.now();
        boolean isAllAuthor = false;
        NavigableSet<String> allAllowedGroupsAndUsers = null;
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                    madisonSystemUserNameProviderService.getFmditaServiceUsername());

            final UserManager userManager = resourceResolver.adaptTo(UserManager.class);

            final RequestParameterMap requestParameterMap = request.getRequestParameterMap();

            response.setContentType("text/html");
            final PrintWriter out = response.getWriter();
            String assetPath = StringUtils.EMPTY;
            String query = StringUtils.EMPTY;
            int start = 0;
            int end = 25;
            if (requestParameterMap.containsKey(ASSET_PATH)) {
                assetPath = URLDecoder.decode(request.getParameter(ASSET_PATH), StandardCharsets.UTF_8.name());
            }
            if (requestParameterMap.containsKey(Query)) {
                query = requestParameterMap.getValue(Query).getString();
            }
            if (requestParameterMap.containsKey(START)) {
                start = Integer.parseInt(requestParameterMap.getValue(START).toString());
            }
            if (requestParameterMap.containsKey(END)) {
                end = Integer.parseInt(requestParameterMap.getValue(END).toString());
            }
            final String groupType = request.getParameter(GROUP_TYPE);
            String territory = "us";
            if (assetPath.contains(DITA_ROOT)) {
                territory = assetPath.split(DITA_ROOT)[1].split("/")[0];
            }
            LOG.debug("territory Path is {}", xssAPI.encodeForHTML(territory));
            LOG.debug("start index----{} end index----{} query----{}", new Object[] { start, end, query });

            final Resource assetRes = resourceResolver.getResource(assetPath);

            if (!groupType.isEmpty()) {
                if (groupType.equals(ALL_AUTHORS)) {
                    isAllAuthor = true;
                    LOG.debug("Get all authors");
                    allAllowedGroupsAndUsers = new TreeSet<>();
                    getAllAuthors(allAllowedGroupsAndUsers, start, end, userManager, query);
                } else if (null != assetRes && !territory.isEmpty()) {
                    final String parentGroup = territory + "-madison-" + groupType;
                    final Authorizable authorizableParent = userManager.getAuthorizable(parentGroup);
                    final Group group = (Group) authorizableParent;
                    if (null != group) {
                        allAllowedGroupsAndUsers = new TreeSet<>();
                        final Resource parentResource = assetRes.getParent();
                        final Instant begin = Instant.now();
                        LOG.debug("Trying to fetch all editors");
                        getAllEditors(parentResource, authorizableParent, out, query, allAllowedGroupsAndUsers);
                        final Instant done = Instant.now();
                        LOG.debug("getAllEditors took {} ms to fetch members",
                                MadisonUtil.calculateDuration(begin, done));
                    }
                }
            }

            if (allAllowedGroupsAndUsers.size() >= 1) {
                LOG.debug("allAllowedGroupsAndUsers size {}", allAllowedGroupsAndUsers.size());
                Iterator<String> filteredUsers = getIterator(isAllAuthor, start, end, allAllowedGroupsAndUsers);
                if (filteredUsers != null) {
                    while (filteredUsers.hasNext()) {
                        final String allowedMember = filteredUsers.next();
                        out.println("<li class=\"coral-SelectList-item coral-SelectList-item--option\" data-value=\""
                                + allowedMember + "\">" + allowedMember + "</li>");
                    }
                    filteredUsers = null;
                }
            }
            final Instant completed = Instant.now();
            LOG.debug("Request took {} ms to complete", MadisonUtil.calculateDuration(started, completed));

        } catch (final RepositoryException e) {
            LOG.error("RepositoryException occured in UserPickerServlet : {}", e);
        }
    }

    /**
     * Get iterator
     * 
     * @param isAllAuthor
     * @param start
     * @param end
     * @return
     */
    private Iterator<String> getIterator(final boolean isAllAuthor, final int start, final int end,
            final NavigableSet<String> allAllowedGroupsAndUsers) {
        Iterator<String> filteredUsers = null;
        if (isAllAuthor) {
            filteredUsers = allAllowedGroupsAndUsers.iterator();
        } else {
            filteredUsers = filterAllowedGroupsAndUsers(allAllowedGroupsAndUsers, start, end);
        }
        return filteredUsers;
    }

    /**
     * Get all author members based on offset
     * 
     * @param allowedGroupsAndUsers
     * @param start
     * @param end
     * @param userMgr
     * @param query
     */
    private void getAllAuthors(final Set<String> allowedGroupsAndUsers, final int start, final int end,
            final UserManager userMgr, final String query) {
        LOG.debug("Trying to fetch all authors");
        final Instant begin = Instant.now();
        if (end <= start || userMgr == null) {
            return;
        }
        try {
            final Iterator<Authorizable> result = userMgr.findAuthorizables(new Query() {
                @Override
                public <T> void build(final QueryBuilder<T> builder) {
                    builder.setScope(AUTHORS, false);
                    if (StringUtils.isNotEmpty(query)) {
                        builder.setCondition(builder.like("rep:authorizableId", query + "%"));
                    }
                    builder.setLimit(start, end - start);
                    builder.setSortOrder("rep:authorizableId", QueryBuilder.Direction.ASCENDING, true);
                }
            });
            while (result.hasNext()) {
                allowedGroupsAndUsers.add(result.next().getID());
            }
            final Instant done = Instant.now();
            LOG.debug("getAllAuthors took {} ms to fetch members", MadisonUtil.calculateDuration(begin, done));
        } catch (final RepositoryException e) {
            LOG.error("Error getting author group");
        }
    }

    /**
     * Get all groups and users having editor permission on any of the parent folders of given ditamap
     *
     * @param parent
     * @param parentGroup
     * @param out
     */
    private void getAllEditors(final Resource parent, final Authorizable parentGroup, final PrintWriter out,
            final String query, final NavigableSet<String> allAllowedGroupsAndUsers) {
        // get the service resource resolver for reading the users under /home etc
        madisonServiceUserResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_GENERIC_SUB_SERVICE);

        final UserManager userManager = madisonServiceUserResolver.adaptTo(UserManager.class);
        final String parentResourceType = (String) parent.getValueMap().getOrDefault(JcrConstants.JCR_PRIMARYTYPE,
                StringUtils.EMPTY);
        try {
            if (parentResourceType.contains(MadisonConstants.STR_FOLDER)) {
                final Set<String> allAllowedMembers = MadisonUtil.getAllowedUsers(parent, userManager, parentGroup,
                        madisonServiceUserResolver, MadisonConstants.PN_EDITOR, query);
                allAllowedGroupsAndUsers.addAll(allAllowedMembers);
                if (!parent.getName().equals(MadisonConstants.MADISON_DAM_ROOT) && !MadisonUtil.isPrivate(parent)) {
                    getAllEditors(parent.getParent(), parentGroup, out, query, allAllowedGroupsAndUsers);
                }
            }
        } catch (final RepositoryException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            // close the service user resolver
            if (madisonServiceUserResolver != null && madisonServiceUserResolver.isLive()) {
                madisonServiceUserResolver.close();
            }
        }
    }

    /**
     * Get subset of members
     * 
     * @param allowedGroupsAndUsers
     * @param start
     * @param end
     * @return
     */
    private Iterator<String> filterAllowedGroupsAndUsers(final NavigableSet<String> allAllowedGroupsAndUsers,
            final int start, final int end) {
        Iterator<String> filteredUsersIterator = null;
        LOG.debug("Filter users in editor list started");
        final Instant begin = Instant.now();
        final String fromValue = MadisonUtil.getIndexValue(allAllowedGroupsAndUsers, start);
        final String toValue = MadisonUtil.getIndexValue(allAllowedGroupsAndUsers,
                allAllowedGroupsAndUsers.size() < end ? allAllowedGroupsAndUsers.size() - 1 : end - 1);
        if (StringUtils.isNoneEmpty(toValue) && StringUtils.isNoneEmpty(fromValue)) {
            NavigableSet<String> tmpSet = allAllowedGroupsAndUsers.subSet(fromValue, true, toValue, true);
            if (tmpSet != null) {
                filteredUsersIterator = tmpSet.iterator();
            }
            tmpSet = null;
        }
        final Instant done = Instant.now();
        LOG.debug("filterAllowedGroupsAndUsers took {} ms to filter members",
                MadisonUtil.calculateDuration(begin, done));
        LOG.debug("Filter users in editor list completed");
        return filteredUsersIterator;
    }

}
