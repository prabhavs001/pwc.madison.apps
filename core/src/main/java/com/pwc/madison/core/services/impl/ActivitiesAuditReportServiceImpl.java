package com.pwc.madison.core.services.impl;

import com.day.commons.datasource.poolservice.DataSourceNotFoundException;
import com.day.commons.datasource.poolservice.DataSourcePool;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.beans.AuditReportRow;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.ActivitiesAuditReportService;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author sevenkat
 * The Class ActivitiesAuditReportServiceImpl is the OSGi service for Activities Audit Report and it fetches the
 * activity values such as user email id, last modified date, and activity in AEM and if not found it will search it in DB.
 */
@Component(service = ActivitiesAuditReportService.class, immediate = true)
public class ActivitiesAuditReportServiceImpl implements ActivitiesAuditReportService {

    private static final String HOME_USERS = "/home/users";

    private static final String OBJECT_LINK = "object/link";

    private static final String ACTIVITY_RES_TYPE = "granite/activitystreams/components/activity";

    private static final String CQ_TIME = "cq:time";

    private static final String CQ_USERID = "cq:userid";

    private static final String CQ_PATH = "cq:path";

    private static final String VERB = "verb";

    private static final String CQ_AUDIT_EVENT = "cq:AuditEvent";

    private static final String CQ_TYPE = "cq:type";

    private static final String ACTIVITY_NAME = "activity_name";
    private static final String ASSET_TITLE = "asset_title";
    private static final String ASSET_PATH = "asset_path";
    private static final String LOGIN_ID = "login_id";
    private static final String ACTIVITY_DATE = "activity_date";
    private static final String DATA_SOURCE_NAME = "pwcSource";

    @Reference private QueryBuilder queryBuilder;

    @Reference ResourceResolverFactory resolverFactory;

    @Reference private DataSourcePool dataSourcePool;

    @Reference private XSSAPI xssAPI;



    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiesAuditReportServiceImpl.class);

    private static final String DATE_FORMAT = "MM-dd-yyyy hh:mm a";
    String getReportQuery = "EXECUTE [dbo].DisplayTopActivityRecord @target_asset_path=?";
    PreparedStatement preparedStatement = null;

    String globalPath = StringUtils.EMPTY;
    
    List<Object> globalResults = new ArrayList<>();
    
    Long globalPathExpiryTimeStamp = 0L;
            
    public String getGlobalPath() {
        if(globalPathExpiryTimeStamp < System.currentTimeMillis()) {
            globalPath = StringUtils.EMPTY;
        }
        return globalPath; 
    }

    public List<Object> getGlobalResults() {
        return globalResults;
    }



    @Override
    public List<Object> getActivityReport(String path, SlingHttpServletRequest request, int limit, int offset,
            List<Object> results) {
        try (Connection connection = getConnection()) {
            if (null != request) {
                globalPath = path;
                globalPathExpiryTimeStamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30);
                ResourceResolver resolver = MadisonUtil
                        .getResourceResolver(resolverFactory, MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
                Resource inputRes = resolver.getResource(path);
                String assetTitle = populateAssetTitle(inputRes);
                if (null != inputRes) {
                    if (!DamConstants.NT_DAM_ASSET.contentEquals(inputRes.getResourceType())) {
                        final Session session = resolver.adaptTo(Session.class);
                        final Map<String, String> map = new HashMap<>();
                        map.put("path", path);
                        map.put("type", DamConstants.NT_DAM_ASSET);
                        map.put("orderby", "@jcr:content/metadata/dc:title");
                        map.put("p.guessTotal", "true");
                        map.put("p.limit","-1");
                        final Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
                        SearchResult searchResult = query.getResult();
                        for (final Hit hit : searchResult.getHits()) {
                            String assetPath = hit.getPath();
                            assetTitle = populateAssetTitle(resolver.getResource(assetPath));
                            populateSearchResults(assetPath, assetTitle, resolver, results, connection);
                        }
                    } else {
                        populateSearchResults(path, assetTitle, resolver, results, connection);
                    }
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("Exception occurred while finding activity nodes and exception is {} ", e);
        } catch (SQLException e) {
            LOGGER.error("Exception occurred while fetching activity record in DB {} ", e);
        } catch (DataSourceNotFoundException e) {
            LOGGER.error("Exception occurred while connecting to DB {} ", e);
        }
        globalResults = results;
        return results;
    }

    /**
     * @param assetRes
     * @return
     */
    private String populateAssetTitle(Resource assetRes) {
        if (null != assetRes) {
            Resource metaDataRes = assetRes.getChild(MadisonConstants.METADATA_RELATIVE_PATH);
            if (null != metaDataRes && metaDataRes.getValueMap().containsKey(DITAConstants.PROPERTY_TITLE)) {
                return metaDataRes.getValueMap().get(DITAConstants.PROPERTY_TITLE, String.class);
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * @param assetPath
     * @param assetTitle
     * @param resourceResolver
     * @param results
     * @param connection
     * @throws RepositoryException
     */
    private void populateSearchResults(String assetPath, String assetTitle, ResourceResolver resourceResolver,
            List<Object> results, Connection connection)
            throws RepositoryException, SQLException {
        if (null != resourceResolver && StringUtils.isNotBlank(assetPath)) {
            int count=0;
            final Session session = resourceResolver.adaptTo(Session.class);
            // Query Predicate map creation
            final Map<String, String> predicateMap = new HashMap<>();
            predicateMap.put("group.1_group.path", HOME_USERS);
            predicateMap.put("group.1_group.1_property", OBJECT_LINK);
            predicateMap.put("group.1_group.1_property.value", assetPath);
            predicateMap.put("group.1_group.2_property", DITAConstants.PN_SLING_RESOURCE_TYPE);
            predicateMap.put("group.1_group.2_property.value", ACTIVITY_RES_TYPE);
            String groupTwoPath = "/var/audit/com.day.cq.dam" + assetPath;
            predicateMap.put("group.2_group.path", groupTwoPath);
            predicateMap.put("group.2_group.property", CQ_PATH);
            predicateMap.put("group.2_group.property.value", assetPath);
            predicateMap.put("group.p.or", "true");
            predicateMap.put("p.limit", "-1");
            predicateMap.put("orderby", "@jcr:created");
            predicateMap.put("orderby.sort", "desc");
            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
            // execute the query
            SearchResult searchResult = query.getResult();
            List<String> activityList = new ArrayList<>();
            for (Hit hit : searchResult.getHits()) {
                AuditReportRow eachRow = populateReportRow(hit.getPath(), assetPath, assetTitle, resourceResolver,
                        activityList);
                if (null != eachRow) {
                    results.add(eachRow);
                    count++;
                }
            }
            if (count==0) {
                getReportFromDB(assetPath, assetTitle, results, connection);
            }
        }
    }

    /**
     * @param assetPath
     * @param assetTitle
     * @param results
     * @param connection
     */
    public void getReportFromDB(String assetPath, String assetTitle, List<Object> results, Connection connection)
            throws SQLException {
        if (connection != null) {
            preparedStatement = connection.prepareStatement(getReportQuery);
            preparedStatement.setString(1, assetPath);
            try (ResultSet rs = preparedStatement.executeQuery();) {
                while (rs.next()) {
                    AuditReportRow reportRow = new AuditReportRow();
                    reportRow.setActivity(rs.getString(ACTIVITY_NAME));
                    reportRow.setPath(assetPath);
                    reportRow.setCreatedBy(rs.getString(LOGIN_ID));
                    reportRow.setTitle(assetTitle);
                    Timestamp timestamp = rs.getTimestamp(ACTIVITY_DATE);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(timestamp);
                    reportRow.setModifiedDate(getDateTime(cal));
                    results.add(reportRow);
                }
            }
        } else {
            LOGGER.error("Connection is null please check the configuration and DB");
        }
    }

    private Connection getConnection() throws SQLException, DataSourceNotFoundException {
        Connection connection = null;
        try {
            final DataSource dataSource = (DataSource) dataSourcePool.getDataSource(DATA_SOURCE_NAME);
            connection = dataSource.getConnection();
        } catch (SQLException | DataSourceNotFoundException e) {
            LOGGER.error("Exception occurred Getting connection from DataSource {} ", e);
        }
        return connection;
    }

    /**
     * @param activityPath
     * @param assetTitle
     * @param resolver
     * @param activityList
     * @return
     */
    private AuditReportRow populateReportRow(String activityPath, String assetPath, String assetTitle,
            ResourceResolver resolver, List<String> activityList) {
        AuditReportRow row = null;
        String activity = null;
        Resource res = resolver.getResource(activityPath);
        if (null != res) {
            if (CQ_AUDIT_EVENT.equals(res.getResourceType())) {
                activity = res.getValueMap().get(CQ_TYPE, String.class);
                if (!activityList.contains(activity)) {
                    row = new AuditReportRow();
                    row.setActivity(activity);
                    row.setPath(assetPath);
                    row.setCreatedBy(res.getValueMap().get(CQ_USERID, String.class));
                    row.setTitle(assetTitle);
                    row.setModifiedDate(getDateTime(res.getValueMap().get(CQ_TIME, Calendar.class)));
                    activityList.add(activity);
                }
            } else {
                activity = res.getValueMap().get(VERB, String.class);
                if (!activityList.contains(activity)) {
                    row = new AuditReportRow();
                    row.setActivity(activity);
                    row.setTitle(assetTitle);
                    row.setPath(assetPath);
                    row.setCreatedBy(res.getValueMap().get(DITAConstants.PROPERTY_AUTHOR, String.class));
                    row.setModifiedDate(
                            getDateTime(res.getValueMap().get(DITAConstants.META_CREATED_DATE, Calendar.class)));
                    activityList.add(activity);
                }
            }
        }
        return row;
    }

    /**
     * @param cal
     * @return
     */
    private String getDateTime(Calendar cal) {
        if (null != cal) {
            return MadisonUtil.getDate(cal.getTime(), DATE_FORMAT);
        }
        return StringUtils.EMPTY;
    }
}
