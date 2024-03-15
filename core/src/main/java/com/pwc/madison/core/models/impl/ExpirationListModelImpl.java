package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.ExpirationListModel;
import com.pwc.madison.core.models.ExpiryContent;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

/**
 *
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ExpirationListModel.class,
    resourceType = ExpirationListModelImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ExpirationListModelImpl implements ExpirationListModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpirationListModelImpl.class);
    protected static final String RESOURCE_TYPE = "pwc-madison/components/commons/expiration-list";
    private ArrayList<ExpiryContent> expiredNotApprovedList = new ArrayList<>();
    private ArrayList<ExpiryContent> expiredApprovedList = new ArrayList<>();
    private ArrayList<ExpiryContent> expiringList = new ArrayList<>();
    private ResourceResolver madisonServiceUserResolver;
    private UserManager userManager;
    private String loggedInUserId = StringUtils.EMPTY;
    private Authorizable currentUser = null;
    private String currentUserRole = StringUtils.EMPTY;
    private String dateRange = StringUtils.EMPTY;
    private int upperLimit;
    private int lowerLimit;
    private String viewType = StringUtils.EMPTY;
    private String damDitaRootPath = MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH;
    private ArrayList<String> currentUserExpirationGroups = new ArrayList<>();



    @ScriptVariable
    private Page currentPage;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    SlingSettingsService slingSettingsService;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @OSGiService
    private QueryBuilder queryBuilder;

    @PostConstruct
    protected void init() {
        LOGGER.debug("Expiration Report Start");
        dateRange = request.getRequestParameter(MadisonConstants.DATE_RANGE) != null ? request.getRequestParameter(MadisonConstants.DATE_RANGE).toString() : "";
        viewType = request.getRequestParameter(MadisonConstants.VIEW) != null ? request.getRequestParameter(MadisonConstants.VIEW).toString() : "expiringContent";
        if (null != request.getRequestParameter(MadisonConstants.EXPIRATION_CONTENT_PATH) && !request.getParameter(MadisonConstants.EXPIRATION_CONTENT_PATH).toString().equals("")) {
            damDitaRootPath = request.getParameter(MadisonConstants.EXPIRATION_CONTENT_PATH).toString();
        }
        setDateRange(dateRange);
        madisonServiceUserResolver = MadisonUtil.getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
        userManager = madisonServiceUserResolver.adaptTo(UserManager.class);
        Session jcrSession = resourceResolver.adaptTo(Session.class);
        loggedInUserId = jcrSession.getUserID();
        try {
            currentUser = userManager.getAuthorizable(loggedInUserId);
            if (isApprover(loggedInUserId)) {
                currentUserRole = MadisonConstants.APPROVERS_ROLE;
            } else if (isPublisher(loggedInUserId)) {
                currentUserRole = MadisonConstants.PUBLISHERS_ROLE;
            }
            /** exceute only for approvers or publishers */
            if (!currentUserRole.equals("")) {
                ArrayList<ExpiryContent> expiryContentList = getExpiryContent();
                sortExpirationList(expiryContentList);

            }
            LOGGER.debug("Expiration Report End");
        } catch (RepositoryException e) {
            LOGGER.error("Error while getting authorizable. ", e);
        }
    }

    private ArrayList<ExpiryContent> sortExpirationList(ArrayList<ExpiryContent> expirationList){
        for (ExpiryContent expiryContent : expirationList) {
            Date expirationDate = expiryContent.getExpiration();
            if(null != expirationDate){
                if(expirationDate.compareTo(new Date()) < 0 && null == expiryContent.getConfirmationState()){
                    expiredNotApprovedList.add(expiryContent);
                }else if(expirationDate.compareTo(new Date()) < 0 && null != expiryContent.getConfirmationState()){
                    expiredApprovedList.add(expiryContent);
                }else{
                    expiringList.add(expiryContent);
                }
            }
            Collections.sort(expiredNotApprovedList);
            Collections.sort(expiredApprovedList);
        }
        return null;
    }

    /**
     * Checks if the logged-in user is belongs to approver group
     *
     * @param loggedInUserId
     * @return isApprover
     */
    private boolean isApprover(String loggedInUserId) {
        //* check if logged in user is member of main expiration approver group(<territory>-madison-expiration-approver) */
        return isMember(loggedInUserId, MadisonConstants.USER_GROUPS_DITA_EXPIRATION_APPROVER);
    }

    /**
     * Checks if the logged-in user is belongs to publisher group
     *
     * @param loggedInUserId
     * @return isPublisher
     */
    private boolean isPublisher(String loggedInUserId) {
        //* check if logged in user is member of main publisher group(<territory>-madison-publisher) */
        return isMember(loggedInUserId, MadisonConstants.USER_GROUPS_DITA_PUBLISHER);
    }

    /**
     * Checks if the given user is part of the passed group
     *
     * @param loggedInUserId
     * @param groupId
     * @return isMember
     */
    private boolean isMember(String loggedInUserId, String groupId) {
        try {
            Iterator<Group> userGroups = currentUser.memberOf();
            while (userGroups.hasNext()) {
                Group group = userGroups.next();
                if (group.getPrincipal().getName().contains(groupId)) {
                    return true;
                }
            }
            return false;
        } catch (RepositoryException e) {
            LOGGER.error("Error while getting authorizable - isMember(). ", e);
            return false;
        }
    }


    /**
      * Fetchers the Expiry Content list
      *
      * @return expiryList
      */
    private ArrayList<ExpiryContent> getExpiryContent(){
        LOGGER.debug("getExpiryContent start");
        ArrayList<ExpiryContent> expiryList;
        ArrayList<SearchResult> resultList = new ArrayList<>();
        if (null != request.getRequestParameter(MadisonConstants.EXPIRATION_CONTENT_PATH) && !request.getParameter(MadisonConstants.EXPIRATION_CONTENT_PATH).toString().equals("")) {
            damDitaRootPath = request.getParameter(MadisonConstants.EXPIRATION_CONTENT_PATH).toString();
            Map<String, Object> map = createQuery(damDitaRootPath);
            final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                resourceResolver.adaptTo(Session.class));
            final SearchResult searchResult = query.getResult();
            if(null != searchResult){
                resultList.add(searchResult);
            }
        }else{
            Resource resource = resourceResolver.getResource(damDitaRootPath);
            if(null != resource){
                Iterator<Resource> children = resource.listChildren();
                while(children.hasNext()){
                    Resource child = children.next();
                    String path = child.getPath();
                    Map<String, Object> map = createQuery(path);
                    final Query query = queryBuilder.createQuery(PredicateGroup.create(map),
                        resourceResolver.adaptTo(Session.class));
                    LOGGER.debug("query path: {}", path);
                    final SearchResult searchResult = query.getResult();
                    if(null != searchResult){
                        LOGGER.debug("result size: {}", searchResult.getHits().size());
                        resultList.add(searchResult);
                    }
                }
            }
        }
        expiryList = populateExpiryContent(resultList);
        return expiryList;
    }

    /**
     * Creates query to fetch expiration list
     *
     * @return query
     */
    private Map<String, Object> createQuery(String path) {
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("path", path);
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("relativedaterange.property", "jcr:content/metadata/" + DITAConstants.META_EXPIRY_DATE);
        predicateMap.put("relativedaterange.upperBound", upperLimit + "d");
        predicateMap.put("relativedaterange.upperOperation", "<=");
        if (!(viewType.equals("expiringContent") && lowerLimit == 0 && upperLimit == 90)) {
            predicateMap.put("relativedaterange.lowerBound", lowerLimit + "d");
            predicateMap.put("relativedaterange.lowerOperation", ">=");
        }
        if (currentUserRole.equals(MadisonConstants.APPROVERS_ROLE)) {
            predicateMap.put("1_property", "jcr:content/metadata/" + DITAConstants.META_EXPIRY_DATE);
            predicateMap.put("1_property.operation", "exists");
            predicateMap.put("2_property", "jcr:content/metadata/" + DITAConstants.META_EXPIRATION_CONFIRMATION_STATUS);
            predicateMap.put("2_property.operation", "not");
        }else{
            predicateMap.put("property", "jcr:content/metadata/" + DITAConstants.META_EXPIRY_DATE);
            predicateMap.put("property.operation", "exists");
        }
        predicateMap.put("orderby", "@jcr:content/metadata/" + DITAConstants.META_EXPIRY_DATE);
        predicateMap.put("orderby.sort", "asc");
        predicateMap.put("p.limit", "-1");
        return predicateMap;
    }



    /**
     * populates the expiry content list
     *
     * @param resultArrayList
     * @return expiryList
     */
    private ArrayList<ExpiryContent> populateExpiryContent(ArrayList<SearchResult> resultArrayList) {
        LOGGER.debug("populateexpirycontent start");
        final long startTime = System.currentTimeMillis();
        ArrayList<ExpiryContent> expiryList = new ArrayList<>();
        if (currentUserRole.equals(MadisonConstants.APPROVERS_ROLE)) {
            currentUserExpirationGroups = getExpirationApproverGroups();
        }else if(currentUserRole.equals(MadisonConstants.PUBLISHERS_ROLE)){
            currentUserExpirationGroups = getPublisherGroups();
        }
        LOGGER.debug("currentUserExpirationGroups size: {}", currentUserExpirationGroups.size());
        for (SearchResult searchResult : resultArrayList ) {
            final Iterator<Resource> resultResources = searchResult.getResources();
            while (resultResources.hasNext()) {
                Resource resource = resultResources.next();
                //** validate resource is a dita file && if the logged in user has permission to the current resource folder */
                if ( resource.getPath().endsWith(MadisonConstants.SLING_SELECTORS_DITA) && hasPermission(resource.getParent()) ) {
                    ValueMap resourceProperty = resource.getValueMap();
                    String author = resourceProperty.get(DITAConstants.PROPERTY_AUTHOR, "");
                    Resource metaDataResource = resourceResolver.getResource(resource.getPath() + MadisonConstants.METADATA_PATH);
                    if(null != metaDataResource){
                        ExpiryContent expiryContent = metaDataResource.adaptTo(ExpiryContent.class);
                        if(null != expiryContent){
                            expiryContent.setAuthor(author);
                            Date expirationDate = expiryContent.getExpiration();
                            expiryContent.setActive(isActiveContent(expirationDate));
                            expiryList.add(expiryContent);
                        }
                    }
                }
            }
        }
        final long endTime = System.currentTimeMillis();
        LOGGER.debug("populateExpiryContent took {} seconds to complete", (endTime - startTime) / 1000);
        return expiryList;
    }

    private boolean isActiveContent(Date expirationDate){
        boolean active = false;
        if (null != expirationDate) {
            Date today = new Date();
            //** getting hours between expiration date and today */
            long secs = (expirationDate.getTime() - today.getTime()) / 1000;
            long hours = secs / 3600;
            //** check already expired and content expiring in next 24 hours */
            if (viewType.equals(MadisonConstants.EXPIRING_CONTENT) && (expirationDate.compareTo(new Date()) < 0 || (hours > 0 && hours <= 24))) {
                active = true;
            }
        }
        return active;
    }

    /**
     * get expiration approver groups of current user
     *
     * @param role
     * @return expirationApproverGroups
     */
    private ArrayList<String> getExpirationApproverGroups() {
        LOGGER.debug("getExpirationApproverGroups start");
        ArrayList<String> expirationApproverGroups = new ArrayList<>();
        try {
            Iterator<Group> userGroups = currentUser.memberOf();
            while (userGroups.hasNext()) {
                Group group = userGroups.next();
                //* exclude parent expiration approver group *//
                if (!group.getPrincipal().getName().contains(MadisonConstants.USER_GROUPS_DITA_EXPIRATION_APPROVER)) {
                    Iterator<Group> currentGroups = group.memberOf();
                    while(currentGroups.hasNext()){
                        Group currentGroup = currentGroups.next();
                        //* check groups has membership to parent expiration approver group */
                        if(currentGroup.getPrincipal().getName().contains(MadisonConstants.USER_GROUPS_DITA_EXPIRATION_APPROVER)){
                            expirationApproverGroups.add(group.getPrincipal().getName());
                            break;
                        }
                    }
                }
            }
            LOGGER.debug("getExpirationApproverGroups end");
        } catch (RepositoryException e) {
            LOGGER.error("Error while getting authorizable - getExpirationApproverGroups()", e);
        } catch (Exception ex) {
            LOGGER.error("Error while getting authorizable - getExpirationApproverGroups()", ex);
        }
        return expirationApproverGroups;
    }

    /**
     * get publisher groups of the user
     * @return publisherGroups
     */
    private ArrayList<String> getPublisherGroups() {
        ArrayList<String> publisherGroups = new ArrayList<>();
        try {
            Iterator<Group> userGroups = currentUser.memberOf();
            while (userGroups.hasNext()) {
                Group group = userGroups.next();
                //* exclude parent publisher group and default OOTB groups *//
                if (!group.getPrincipal().getName().equals(MadisonConstants.USER_GROUPS_DITA_PUBLISHERS) && !group.getPrincipal().getName().contains(MadisonConstants.USER_GROUPS_DITA_PUBLISHER)) {
                    Iterator<Group> currentGroups = group.memberOf();
                    while(currentGroups.hasNext()){
                        Group currentGroup = currentGroups.next();
                        //* check groups has membership to OOTB publisher group *//
                        if(currentGroup.getPrincipal().getName().equals("publishers")){
                            publisherGroups.add(group.getPrincipal().getName());
                            break;
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error while getting authorizable - getExpirationApproverGroups()", e);
        } catch (Exception ex) {
            LOGGER.error("Error while getting authorizable - getExpirationApproverGroups()", ex);
        }
        return publisherGroups;
    }

    /**
     * This method checks if the approvers/publisher has permission to the giver resource
     *
     * @param resource
     * @return hasPermission
     */
    private boolean hasPermission(Resource resource) {
        for (String expirationGroup : currentUserExpirationGroups) {
            try {
                Set<Authorizable> allowedGroups = MadisonUtil.getTerritoryGroups(madisonServiceUserResolver, resource, expirationGroup, true);
                if (allowedGroups.size() > 0) {
                    return true;
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error while getting authorizable - hasPermission()", e);
            }
        }
        return false;
    }

    /**
     * sets value for dita ranges
     *
     * @param limit
     */
    private void setDateRange(String limit) {
        lowerLimit = 0;
        upperLimit = 90;
        if (!limit.equals("")) {
            String[] limitArray = limit.split("\\|");
            if (limitArray.length > 1) {
                lowerLimit = Integer.parseInt(limitArray[0]);
                upperLimit = Integer.parseInt(limitArray[1]);
            }
        }
    }

    @Override
    public String getCurrentUserRole() {
        return currentUserRole;
    }

    @Override
    public String getLoggedInUserId() {
        return loggedInUserId;
    }

    public ArrayList<ExpiryContent> getExpiredNotApprovedList() {
        return expiredNotApprovedList;
    }

    public ArrayList<ExpiryContent> getExpiredApprovedList() {
        return expiredApprovedList;
    }

    @Override
    public ArrayList<ExpiryContent> getExpiringList() {
        return expiringList;
    }

}
