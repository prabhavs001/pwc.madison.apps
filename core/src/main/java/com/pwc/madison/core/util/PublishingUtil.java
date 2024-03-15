package com.pwc.madison.core.util;

import com.day.cq.commons.Externalizer;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.*;

public class PublishingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PublishingUtil.class);
    public static final String WRITE = "write";
    private static final String PUBLISH_LISTENER_PATH = "/bin/publishlistener";
    public static final String NT_UNSTRUCTURED = "nt:unstructured";


    /**
     * Generates an API URL based on provided parameters.
     *
     * @param outputType     The type of output.
     * @param externalizer   An instance of Externalizer for URL generation.
     * @param apiType        The type of API. If it is "publishing-status", the operation will be "PUBLISHBEACON",
     *  *                       otherwise, the operation will be "GENERATEOUTPUT".
     * @return               The API URL as a String. If externalizer is null, returns an empty String.
     */
    public static String getAPIUrl(String domainURL, String outputType, Externalizer externalizer, final String apiType) {
        final StringBuilder parameters = new StringBuilder();
        String getRequestApiEndPoint = StringUtils.EMPTY;
        if (externalizer != null) {
            parameters.append(domainURL);
            parameters.append(PUBLISH_LISTENER_PATH);
            parameters.append("?");
            parameters.append("operation=").append(apiType);
            parameters.append("&");
            parameters.append("outputName=");
            parameters.append(outputType);
            parameters.append("&");
            parameters.append("source=");
            getRequestApiEndPoint = parameters.toString();
            LOG.info("Get request api {}", getRequestApiEndPoint);
        }
        return getRequestApiEndPoint;
    }

    public static boolean checkIfUserHasPermission(final ResourceResolver resourceResolver, final String assetPath, String permission){
        boolean hasPermission = false;
        if (permission!=null) {
            if (permission.equals(WRITE)) {
                permission = Privilege.JCR_WRITE;
            } else {
                permission = Privilege.JCR_READ;
            }
        }
        try {
            Session userSession = resourceResolver.adaptTo(Session.class);
            AccessControlManager acMgr = userSession.getAccessControlManager();
            hasPermission = userSession.getAccessControlManager()
                    .hasPrivileges(assetPath, new Privilege[] { acMgr.privilegeFromName(permission) });
        } catch (RepositoryException e) {
            LOG.error("RepositoryException in CheckUserPermissionServlet {}", e);
        }
        return hasPermission;
    }

    /**
     * Read syndication subscribers which are already syndicated for a given source from conf
     *
     * @param sourcePath
     * @param resourceResolver
     * @return
     */
    public static boolean checkIfSourceIsAlreadyInPublishingQueue(final String sourcePath,
                                                                  final ResourceResolver resourceResolver) {

        final Resource autoPublishingQueueResource = resourceResolver.getResource(MadisonConstants.AUTO_PUBLISHING_QUEUE_PATH);

        // configuration not available
        if (null == autoPublishingQueueResource) {
            LOG.debug("Either Auto-Publishing Queue is not available or User does not have access");
            return false;
        }

        final Iterator<Resource> subscribersToBePublishList = autoPublishingQueueResource.listChildren();

        while (subscribersToBePublishList.hasNext()) {
            final Resource autoPublishNode = subscribersToBePublishList.next();
            if (!(autoPublishNode.getName().equals(DITAConstants.REP_POLICY_NODE))) {
                final String sourcePathFromPublishNode = autoPublishNode.getValueMap().get("sourcePath", String.class);

                if (sourcePathFromPublishNode!=null && sourcePathFromPublishNode.equals(sourcePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void createEntryInAutoPublishingQueue(final String nodeName, final Session session) throws RepositoryException {
        // Get the AUTO_PUBLISHING_QUEUE_PATH node
        Node autoPublishingQueueNode = null;
        if (session != null) {
            autoPublishingQueueNode = session.getNode(MadisonConstants.AUTO_PUBLISHING_QUEUE_PATH);
        }

        if (autoPublishingQueueNode != null) {
            autoPublishingQueueNode.addNode(nodeName, NT_UNSTRUCTURED);
        }
    }
}
