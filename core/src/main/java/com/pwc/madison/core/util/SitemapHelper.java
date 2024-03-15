package com.pwc.madison.core.util;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.eval.JcrPropertyPredicateEvaluator;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.TagConstants;
import com.day.cq.wcm.api.NameConstants;
import com.pwc.madison.core.beans.SitemapParent;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.jackrabbit.spi.commons.query.QueryConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The utils contains common methods used by Sitemap File creation service.
 */
public final class SitemapHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapHelper.class);

    private static final String NS = "http://www.sitemaps.org/schemas/sitemap/0.9";
    private static final String SITEMAP_TAG_URL = "url";
    private static final String SITEMAP_TAG_LOC = "loc";
    private static final String SITEMAP_TAG_SITEMAP = "sitemap";
    private static final String SITEMAP_TAG_LASTMOD = "lastmod";
    private static final String FORWARD_SLASH = "/";
    private static final String META_ROBOTS = "pwc-metaRobots";
    private static final String NO_INDEX = "noindex";
    private static final String NO_INDEX_NO_FOLLOW = "noindex,nofollow";
    private static final String NO_INDEX_FOLLOW = "noindex,follow";
    private static final String HIDDEN_FROM_SITE_SEARCH = "pwc-hiddenFromSiteSearch";
    private static final String ACTIVATED = "Activate";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String AUDIT_LOG_PATH = "/var/audit/com.day.cq.wcm.core.page";
    private static final String REPLICATION_LOG_PATH = "/var/audit/com.day.cq.replication";
    private static final String ASCENDING = "asc";
    private static final String PAGE_DELETED = "PageDeleted";
    private static final String DELETE = "Delete";
    private static final String CQ_AUDIT_EVENT = "cq:AuditEvent";
    private static final String AT = "@";
    private static final String DITA_PAGE_EXISTENCE_TOPICBODY_NODE_STRUCTURE = "jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody";

    public static final String DEACTIVATED = "Deactivate";
    public static final String CQ_TYPE = "cq:type";
    public static final String CQ_TIME = "cq:time";

    private SitemapHelper() {
    }

    /**
     * Activate file at the given file path using {@link Replicator}.
     * 
     * @param session
     *            {@link Session}
     * @param filePath
     *            {@link String} path to the file which is to be replicated
     * @param replicator
     *            {@link Replicator} requires to replicate given file
     * @param allowedReplicationAgents
     *            {@link List} of {@link String}
     */
    public static void activateFile(final Session session, final String filePath, final Replicator replicator, final List<String> allowedReplicationAgents) {
        try {
            replicator.replicate(session, ReplicationActionType.ACTIVATE, filePath, prepareOptions(allowedReplicationAgents));
        } catch (final ReplicationException replicationException) {
            LOGGER.error("SitemapHelper activateFile() : Replication Exception occured while replicating file {} : {} ",
                    filePath, replicationException);
        }
    }
    
    /**
     * Returns {@link ReplicationOptions} after add {@link AgentFilter} to it.
     * 
     * @param allowedReplicationAgents
     *            {@link List} of {@link String}
     * @return {@link ReplicationOptions}
     */
    private static ReplicationOptions prepareOptions(final List<String> allowedReplicationAgents) {
        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setFilter(new AgentFilter() {
            @Override
            public boolean isIncluded(Agent agent) {
                return allowedReplicationAgents.contains(agent.getId());
            }
        });
        return replicationOptions;
    }

    /**
     * Writes {@value #SITEMAP_TAG_URL} element to XML write of the Sitemap file which contains childs
     * {@value #SITEMAP_TAG_LOC} element with text of given loc and {@value #SITEMAP_TAG_LASTMOD} element with text of
     * given last modified date.
     * 
     * @param streamWriter
     *            {@link XMLStreamWriter}
     * @param loc
     *            {@link string} URL of page
     * @param lastModified
     *            {@link Date} last modified date of page
     * @param dateFormat
     *            {@link SimpleDateFormat}
     * @throws XMLStreamException
     *             {@link XMLStreamException}
     */
    public static void writeLOC(final XMLStreamWriter streamWriter, final String loc, final Calendar lastModified,
            final SimpleDateFormat dateFormat) throws XMLStreamException {
        if (!loc.contains(DITAConstants.JOINED+DITAConstants.HTML_EXT)) {
			streamWriter.writeStartElement(NS, SITEMAP_TAG_URL);
			streamWriter.writeStartElement(NS, SITEMAP_TAG_LOC);
			streamWriter.writeCharacters(loc);
			streamWriter.writeEndElement();
			streamWriter.writeStartElement(NS, SITEMAP_TAG_LASTMOD);
			streamWriter.writeCharacters(dateFormat.format(lastModified.getTime()));
			streamWriter.writeEndElement();
			streamWriter.writeEndElement();
		}
    }

    /**
     * Writes {@value #SITEMAP_TAG_SITEMAP} element to XML write of the Sitemap file which contains childs
     * {@value #SITEMAP_TAG_LOC} element with text of given loc and {@value #SITEMAP_TAG_LASTMOD} element with text of
     * given last modified date.
     * 
     * @param streamWriter
     *            {@link XMLStreamWriter}
     * @param loc
     *            {@link string} URL of sitemap file
     * @param lastModified
     *            {@link Calendar} last modified date of sitemap file
     * @param dateFormat
     *            {@link SimpleDateFormat}
     * @throws XMLStreamException
     *             {@link XMLStreamException}
     */
    public static void writeSitemap(final XMLStreamWriter streamWriter, final String loc, final Calendar lastModified,
            final SimpleDateFormat dateFormat) throws XMLStreamException {
        if (!loc.contains(DITAConstants.JOINED+DITAConstants.HTML_EXT)) {
			streamWriter.writeStartElement(NS, SITEMAP_TAG_SITEMAP);
			streamWriter.writeStartElement(NS, SITEMAP_TAG_LOC);
			streamWriter.writeCharacters(loc);
			streamWriter.writeEndElement();
			streamWriter.writeStartElement(NS, SITEMAP_TAG_LASTMOD);
			streamWriter.writeCharacters(dateFormat.format(lastModified.getTime()));
			streamWriter.writeEndElement();
			streamWriter.writeEndElement();
		}
    }

    /**
     * Saves the file with given {@link Binary} data with given file name under parent root in the repository.
     * 
     * @param resourceResolver
     *            {@link ResourceResolver} must have permission of reading and replication under Madison Site
     * @param parentRoot
     *            {@link String} the path under which the file is to be saved
     * @param fileName
     *            {@link String} name of the file
     * @param session
     *            {@link Session}
     * @param binary
     *            {@link Binary} data
     * @param replicator
     *            {@link Replicator} requires to replicate given file
     */
    public static void saveFile(final ResourceResolver resourceResolver, final String parentRoot, final String fileName,
            final Session session, final Binary binary, final Replicator replicator, final List<String> allowedReplicationAgents) {
        final String filePath = parentRoot + MadisonConstants.FORWARD_SLASH + fileName;
        Node rootNode = resourceResolver.resolve(parentRoot).adaptTo(Node.class);
        if (null != rootNode) {
            try {
                Node resNode = null;
                if (!rootNode.hasNode(fileName)) {
                    Node sitemapNode = rootNode.addNode(fileName, JcrConstants.NT_FILE);
                    resNode = sitemapNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
                } else {
                    resNode = rootNode.getNode(fileName).getNode(JcrConstants.JCR_CONTENT);
                }
                resNode.setProperty(JcrConstants.JCR_DATA, binary);
                resNode.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
                resNode.setProperty(JcrConstants.JCR_LAST_MODIFIED_BY,
                        MadisonConstants.MADISON_CONTENT_ADMIN_SUB_SERVICE);
                session.save();
                LOGGER.debug("SitemapHelper saveFile() : Saving {} file on repository", filePath);
                activateFile(session, filePath, replicator, allowedReplicationAgents);
            } catch (RepositoryException repositoryException) {
                LOGGER.error(
                        "SitemapHelper saveFile() :  Repository Exception occurred while saving sitemap xml file {} : {}",
                        filePath, repositoryException);
            }
        }
    }

    /**
     * Returns a list of page {@link Hit} after performing search query to repository for the pages under the paths
     * given in {@link List} of {@link SitemapParent}.
     * 
     * A page hit is returned in the list as result of query if
     * <ul>
     * <li>page exists under or is at path given in {@link List} of {@link SitemapParent}
     * <li>If page exists under ditaroot path then it must contain
     * {@value #DITA_PAGE_EXISTENCE_TOPICBODY_NODE_STRUCTURE} structure under the page
     * <li>{@value NameConstants#PN_HIDE_IN_NAV} property should not present on page
     * <li>{@value NameConstants#PN_DELETED} property should not present on page
     * <li>{@value #HIDDEN_FROM_SITE_SEARCH} property should not present on page
     * <li>{@value ResourceResolver#PROPERTY_RESOURCE_TYPE} property should present on page
     * <li>The value of the {@value NameConstants#PN_PAGE_LAST_REPLICATION_ACTION} on page should be {@value #ACTIVATED}
     * <li>{@value #META_ROBOTS} property should not exist on page or if exists should not equal to {@value #NO_INDEX}
     * and {@value #NO_INDEX_FOLLOW} and {@value #NO_INDEX_NO_FOLLOW}
     * <li>If repliactionLowerBound and repliactionUpperBound date strings are not null then, property
     * {@value ReplicationStatus#NODE_PROPERTY_LAST_REPLICATED} date value on page must exist between the given date
     * bounds
     * </ul>
     * 
     * @param session
     *            {@link Session}
     * @param queryBuilder
     *            {@link QueryBuilder}
     * @param parentPages
     *            {@link List} of {@link SitemapParent}
     * @param limit
     *            {@link Integer} defines the number of results to be returned
     * @param repliactionLowerBound
     *            {@link String} of date
     * @param repliactionUpperBound
     *            {@link String} of date
     * @param searchTags
     *            {@link String[]} arrays of tag IDs with which the searched resources should be tagged. It can be null
     *            for sitemap XML creation and may contain values for records XML creation
     * @return {@link List} of {@link Hit}
     */
    public static List<Hit> getSitemapResourcesHits(final Session session, final QueryBuilder queryBuilder,
            List<SitemapParent> parentPages, final Integer limit, final String repliactionLowerBound,
            final String repliactionUpperBound, final String[] searchTags) {
        List<Hit> hits = new ArrayList<Hit>();
        int finalLimit = limit;
        try {
            for (int listCounter = 0; listCounter < parentPages.size(); listCounter++) {
                final Map<String, String> map = new HashMap<String, String>();
                map.put("group.p.or", TRUE);
                map.put("1_group.p.or", TRUE);
                map.put("2_group.p.or", TRUE);
                map.put("path", parentPages.get(listCounter).getParentPagePath());
                if (!parentPages.get(listCounter).isIncludeParentPagePath()) {
                    map.put("nodeExists.exists", DITA_PAGE_EXISTENCE_TOPICBODY_NODE_STRUCTURE);
                }
                map.put("type", NameConstants.NT_PAGE);
                map.put("group.1_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + NameConstants.PN_HIDE_IN_NAV);
                map.put("group.1_property.value", FALSE);
                map.put("group.1_property.operation", JcrPropertyPredicateEvaluator.OP_EXISTS);
                map.put("group.2_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + NameConstants.PN_HIDE_IN_NAV);
                map.put("group.2_property.value", TRUE);
                map.put("group.2_property.operation", JcrPropertyPredicateEvaluator.OP_UNEQUALS);
                map.put("3_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + NameConstants.PN_DELETED);
                map.put("3_property.value", FALSE);
                map.put("3_property.operation", JcrPropertyPredicateEvaluator.OP_EXISTS);
                map.put("4_property",
                        JcrConstants.JCR_CONTENT + FORWARD_SLASH + NameConstants.PN_PAGE_LAST_REPLICATION_ACTION);
                map.put("4_property.value", ACTIVATED);
                map.put("1_group.5_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + HIDDEN_FROM_SITE_SEARCH);
                map.put("1_group.5_property.value", FALSE);
                map.put("1_group.5_property.operation", JcrPropertyPredicateEvaluator.OP_EXISTS);
                map.put("1_group.6_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + HIDDEN_FROM_SITE_SEARCH);
                map.put("1_group.6_property.value", TRUE);
                map.put("1_group.6_property.operation", JcrPropertyPredicateEvaluator.OP_UNEQUALS);
                map.put("7_property",
                        JcrConstants.JCR_CONTENT + FORWARD_SLASH + ResourceResolver.PROPERTY_RESOURCE_TYPE);
                map.put("7_property.value", TRUE);
                map.put("7_property.operation", JcrPropertyPredicateEvaluator.OP_EXISTS);
                map.put("2_group.8_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + META_ROBOTS);
                map.put("2_group.8_property.and", TRUE);
                map.put("2_group.8_property.1_value", NO_INDEX);
                map.put("2_group.8_property.2_value", NO_INDEX_NO_FOLLOW);
                map.put("2_group.8_property.3_value", NO_INDEX_FOLLOW);
                map.put("2_group.8_property.operation", JcrPropertyPredicateEvaluator.OP_UNEQUALS);
                map.put("2_group.9_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + META_ROBOTS);
                map.put("2_group.9_property.value", FALSE);
                map.put("2_group.9_property.operation", JcrPropertyPredicateEvaluator.OP_EXISTS);
                if (null != searchTags && searchTags.length > 0) {
                    map.put("10_property", JcrConstants.JCR_CONTENT + FORWARD_SLASH + TagConstants.PN_TAGS);
                    map.put("10_property.and", FALSE);
                    for (int searchIndex = 0; searchIndex < searchTags.length; searchIndex++) {
                        map.put("10_property." + (searchIndex + 1) + "_value", searchTags[searchIndex]);
                    }
                }
                if (repliactionLowerBound != null && repliactionUpperBound != null) {
                    map.put("daterange.property",
                            JcrConstants.JCR_CONTENT + FORWARD_SLASH + ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED);
                    map.put("daterange.lowerBound", repliactionLowerBound);
                    map.put("daterange.lowerOperation", QueryConstants.OP_NAME_GT_GENERAL);
                    map.put("daterange.upperBound", repliactionUpperBound);
                    map.put("daterange.upperOperation", QueryConstants.OP_NAME_LE_GENERAL);
                }
                map.put("p.limit", Integer.toString(finalLimit));
                Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
                SearchResult result = query.getResult();
                List<Hit> currentHits = result.getHits();
                if(null != currentHits && currentHits.size() > 0) {
                    hits.addAll(currentHits);  
                }
                if(finalLimit != -1) {
                    finalLimit = finalLimit - currentHits.size();
                }
            }
            return hits;
        } catch (Exception exception) {
            LOGGER.error(
                    "SitemapHelper getSitemapResourcesHits() : Exception occured while finding Sitemap Resources for parent pages root {} : {}",
                    parentPages, exception);
        }
        return null;
    }

    /**
     * Returns a list of {@link Hit} after performing search query to repository for the pages under the paths given in
     * {@link List} of {@link SitemapParent}.
     * 
     * A hit is returned in the list as result of query if
     * <ul>
     * <li>Node exists under path {@value #AUDIT_LOG_PATH} OR {@value #REPLICATION_LOG_PATH} and relative paths given
     * in{@link List} of {@link SitemapParent}
     * <li>The value of the {@value #CQ_TYPE} on node should be {@value #PAGE_DELETED} OR {@value #DEACTIVATED}
     * <li>The property {@value #CQ_TIME} date value on node must exist between the given repliactionLowerBound and
     * repliactionUpperBound
     * </ul>
     * 
     * @param session
     *            {@link Session}
     * @param queryBuilder
     *            {@link QueryBuilder}
     * @param parentPages
     *            {@link List} of {@link SitemapParent}
     * @param repliactionLowerBound
     *            {@link String} of date
     * @param repliactionUpperBound
     *            {@link String} of date
     * @return {@link List} of {@link Hit}
     */
    public static List<Hit> getSitemapDeleteResourcesHits(final Session session, final QueryBuilder queryBuilder,
            List<SitemapParent> parentPages, final String repliactionLowerBound, final String repliactionUpperBound) {
        try {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("group.p.or", TRUE);
            for (int listCounter = 0; listCounter < parentPages.size(); listCounter++) {
                map.put("group." + (listCounter + 1) + "_path",
                        REPLICATION_LOG_PATH + parentPages.get(listCounter).getParentPagePath());
                map.put("group." + (listCounter + parentPages.size() + 1) + "_path",
                        AUDIT_LOG_PATH + parentPages.get(listCounter).getParentPagePath());
            }
            map.put("type", CQ_AUDIT_EVENT);
            map.put("property", CQ_TYPE);
            map.put("property.1_value", DEACTIVATED);
            map.put("property.2_value", PAGE_DELETED);
            map.put("property.3_value", DELETE);
            map.put("daterange.property", CQ_TIME);
            map.put("daterange.lowerBound", repliactionLowerBound);
            map.put("daterange.lowerOperation", QueryConstants.OP_NAME_GT_GENERAL);
            map.put("daterange.upperBound", repliactionUpperBound);
            map.put("daterange.upperOperation", QueryConstants.OP_NAME_LE_GENERAL);
            map.put("orderby", AT + CQ_TIME);
            map.put("orderby.sort", ASCENDING);
            map.put("p.limit", "-1");
            Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
            SearchResult result = query.getResult();
            return result.getHits();
        } catch (Exception exception) {
            LOGGER.error(
                    "SitemapHelper getSitemapDeleteResourcesHits() : Exception occured while finding Sitemap Resources for parent pages root {} : {}",
                    parentPages, exception);
        }
        return null;
    }

}
