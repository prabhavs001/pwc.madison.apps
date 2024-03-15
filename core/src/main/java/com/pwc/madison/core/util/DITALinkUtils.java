package com.pwc.madison.core.util;

import com.adobe.fmdita.custom.common.CommonUtils;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jackrabbit.commons.flat.Rank;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility methods for Dita Link Patching Story - Methods are being re-written by referring Product jar.
 */
public class DITALinkUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DITALinkUtils.class);
    private static final String HASH_SEPARATOR = "#";
    private static final String PN_DESTINATION_PATH = "destinationPath";
    private static final String PN_SOURCE_PATH = "sourcePath";
    private static final String NODE_TYPE_NT_UNSTRUCTURED = "nt:unstructured";
    private static final String TOPIC_RESOURCE_TYPE = "fmdita/components/dita/topic";
    private static final String PN_PROPERTY_OPERATION = "property.operation";
    private static final String PN_PROPERTY = "property";
    private static final String PN_LIMIT = "p.limit";
    private static final String REL_PATH_SEPARATOR = "../";
    private static final String STRING_SPACE = " ";
    private static final String PERIOD_SEPARATOR = "..";
    private static final String REL_PATH_REPLACE_STRING = "\\.\\./";
    private static Comparator<String> order = Rank.comparableComparator();

    /**
     * This method searches guid/xrefnode and returns corresponding page path for ditamap
     *
     * @param resourceResolver
     * @param ditaTopicPath
     * @return
     */

    public static String getPageFromXref(final ResourceResolver resourceResolver, final String ditaTopicPath) {
        if (StringUtils.isNotBlank(ditaTopicPath) && resourceResolver != null) {
            final Node node = getReferenceNode(resourceResolver, Text.escapeIllegalJcrChars(ditaTopicPath));
            String path = StringUtils.EMPTY;
            if (node != null) {
                try {
                    path = node.getPath();
                    LOG.debug("Node {} with key {}", path, ditaTopicPath);
                    if (node.hasProperty(Text.escapeIllegalJcrChars(ditaTopicPath))) {
                        final String uid = node.getProperty(Text.escapeIllegalJcrChars(ditaTopicPath)).getString();
                        return getPageFromXrefDita(resourceResolver, uid);
                    }
                } catch (final RepositoryException e) {
                    LOG.error("Error reading {} from {}", new String[] { ditaTopicPath, path }, e);
                }
            }
        }
        return null;

    }

    /**
     * This method iterates xrefnode and returns corresponding page path for give uuid
     *
     * @param resourceResolver
     * @param key
     * @return
     */

    public static String getPageFromXrefDita(final ResourceResolver resourceResolver, final String key) {
        if (StringUtils.isNotBlank(key)) {
            final Node node = getXREFNode(resourceResolver, key);
            String path = StringUtils.EMPTY;
            if (node != null) {
                try {
                    path = node.getPath();
                    LOG.debug("Node {} with key {}", path, key);
                    if (node.hasProperty(key)) {
                        final String generatedPagePath = node.getProperty(Text.escapeIllegalJcrChars(key))
                                .getValue() != null
                                        ? node.getProperty(Text.escapeIllegalJcrChars(key)).getValue().getString()
                                        : StringUtils.EMPTY;

                        return generatedPagePath;
                    }
                } catch (final RepositoryException e) {
                    LOG.error("Error reading {} from {}", new String[] { key, path }, e);
                }
            }
        }
        return null;
    }

    /**
     * This method returns xrefnode for given uuid
     *
     * @param resourceResolver
     * @param key
     * @return
     */

    private static Node getXREFNode(final ResourceResolver resourceResolver, final String key) {
        if (StringUtils.isNotBlank(key)) {
            final Resource resource = resourceResolver.getResource(DITAConstants.X_PATH_REFERENCES);
            if (resource != null && resource.adaptTo(Node.class) != null) {
                final Node node = resource.adaptTo(Node.class);
                try {
                    final BTreeCustom bTreeCustom = new BTreeCustom(node, order);
                    if (bTreeCustom != null) {
                        return bTreeCustom.getBTreeNodeForKey(key);
                    }
                } catch (final RepositoryException e) {
                    LOG.error("Error in traversing node {}", DITAConstants.X_PATH_REFERENCES, e);
                }
            }
        }
        return null;
    }

    /**
     * This method returns uuid node for given ditamp path
     *
     * @param resourceResolver
     * @param key
     * @return
     */

    private static Node getReferenceNode(final ResourceResolver resourceResolver, final String key) {
        final Resource resource = resourceResolver.getResource(DITAConstants.PATH_TO_GUID);
        if (resource != null && resource.adaptTo(Node.class) != null) {
            final Node node = resource.adaptTo(Node.class);
            try {
                final BTreeCustom bTreeCustom = new BTreeCustom(node, order);
                if (bTreeCustom != null) {
                    return bTreeCustom.getBTreeNodeForKey(key);
                }
            } catch (final RepositoryException e) {
                LOG.error("Error traversing {}", DITAConstants.PATH_TO_GUID, e);
            }
        }
        return null;
    }

    /**
     * This method returns absolute path for given relative path
     *
     * @param resourceResolver
     * @param ditaPath
     * @return
     */
    public static String getAbsPath(final ResourceResolver resourceResolver, final String ditaPath, final String link) {
        if (StringUtils.isNotBlank(ditaPath) && StringUtils.isNotBlank(link) && resourceResolver != null) {
            final Resource folderResource = resourceResolver.getResource(ditaPath);
            if (folderResource != null) {
                final Iterator<Asset> assetIterator = DamUtil.getAssets(folderResource);
                while (assetIterator.hasNext()) {
                    final Asset currentAsset = assetIterator.next();
                    return getFormattedPath(link, getProperty(resourceResolver, currentAsset, "fmditaXrefs"));
                }
            }
        }
        return link;
    }

    /**
     * Get property from resource
     *
     * @param resourceResolver
     * @param asset
     * @param property
     * @return
     */
    private static String[] getProperty(final ResourceResolver resourceResolver, final Asset asset,
            final String property) {
        final Resource resource = resourceResolver
                .getResource(asset.getPath() + MadisonConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        if (resource != null && StringUtils.isNotBlank(property)) {
            final ValueMap properties = resource.getValueMap();
            if (properties != null && properties.containsKey(property)) {
                return properties.get(property, String[].class);
            }
        }
        return null;

    }

    /**
     * get absolute url
     *
     * @param path
     * @return
     */
    private static String getFormattedPath(String path, final String[] fmditaRefs) {
        if (StringUtils.isNotEmpty(path)) {
            if (path.indexOf(HASH_SEPARATOR) > -1) {
                path = path.substring(0, path.indexOf(HASH_SEPARATOR));
            }
            if (path.lastIndexOf("..") > -1) {
                path = StringUtils.substringAfterLast(path, "..");
            }
            path = parseUrl(path, fmditaRefs);
        }
        return path;
    }

    /**
     * parse relative url
     *
     * @param path
     * @return
     */
    private static String parseUrl(final String path, final String[] fmditaRefs) {
        try {
            if (ArrayUtils.isNotEmpty(fmditaRefs)) {
                for (String p : fmditaRefs) {
                    if (StringUtils.contains(p, path)) {
                        if (StringUtils.equalsIgnoreCase(MadisonConstants.COMMA_SEPARATOR,
                                Character.toString(p.charAt(0)))) {
                            p = p.substring(1);
                        }
                        return p;
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Error in formatting", e);
        }

        return path;
    }

    public static String getFixedSyndicationPeerLink(final Node entryNode, final Page currentPage,
            final ResourceResolver resolver, final QueryBuilder queryBuilder)
            throws RepositoryException, PersistenceException {
        final String link = entryNode.hasProperty(DITAConstants.PROPERTY_LINK)
                ? entryNode.getProperty(DITAConstants.PROPERTY_LINK).getString()
                : StringUtils.EMPTY;
        String finalLink = link;
        LOG.debug("link before check for syndication: {}", link);
        final Resource jcr = currentPage.getContentResource();
        ValueMap properties = jcr.adaptTo(ValueMap.class);
        final String basePath = properties.get(DITAConstants.BASE_PATH, String.class);
        final Resource basePageResource = resolver
                .getResource(basePath + DITAConstants.FORWARD_SLASH + NameConstants.NN_CONTENT);
        if (basePageResource == null) {
            return finalLink;
        }
        properties = basePageResource.adaptTo(ValueMap.class);
        final String publishMapPath = properties.get("mapParent", String.class);
        final Session session = resolver.adaptTo(Session.class);
        final String syndicationSource = getSyndicatedPath(publishMapPath, session, queryBuilder);
        if (StringUtils.isBlank(syndicationSource)) {
            return finalLink;
        }

        String tmplink = link.replaceAll(REL_PATH_REPLACE_STRING, STRING_SPACE).trim();
        tmplink = tmplink.replaceAll(STRING_SPACE, REL_PATH_SEPARATOR);
        if (tmplink.indexOf(REL_PATH_SEPARATOR) > -1) {
            finalLink = correctPeerLink(link, tmplink);
        } else {
            finalLink = link;
        }
        final String sourceTerritory = MadisonUtil.getTerritoryCodeForPath(syndicationSource);
        final String destinationTerritory = MadisonUtil.getTerritoryCodeForPath(entryNode.getPath());
        finalLink = finalLink.replace(MadisonConstants.FORWARD_SLASH + sourceTerritory + MadisonConstants.FORWARD_SLASH,
                MadisonConstants.FORWARD_SLASH + destinationTerritory + MadisonConstants.FORWARD_SLASH);
        if (StringUtils.isNotBlank(finalLink) && finalLink.startsWith(MadisonConstants.PWC_MADISON_DAM_BASEPATH)
                && !finalLink.endsWith(DITAConstants.DITA_EXTENSION) && null == resolver.getResource(finalLink)) {
            finalLink = finalLink.replace(
                    MadisonConstants.FORWARD_SLASH + destinationTerritory + MadisonConstants.FORWARD_SLASH,
                    MadisonConstants.FORWARD_SLASH + sourceTerritory + MadisonConstants.FORWARD_SLASH);
        }
        entryNode.setProperty(DITAConstants.PROPERTY_LINK, finalLink);
        LOG.debug("link after check for syndication: {}", finalLink);
        return finalLink;
    }

    private static String correctPeerLink(final String link, final String tmplink) {
        final int linkCount = StringUtils.countMatches(tmplink, REL_PATH_SEPARATOR);
        final String[] split = link.split("\\/");
        final String[] reverseArray = new String[split.length];
        final StringBuilder fixedLink = new StringBuilder();
        boolean checker = true;
        for (int i = split.length - 1; i >= 0; i--) {
            if (split[i].equals(PERIOD_SEPARATOR) && checker) {
                i = i - 2 * linkCount + 1;
                checker = false;
            } else {
                reverseArray[i] = split[i];
            }

        }
        for (int j = 0; j < reverseArray.length; j++) {
            if (reverseArray[j] != null) {
                fixedLink.append(reverseArray[j]);
                if (j != reverseArray.length - 1) {
                    fixedLink.append(MadisonConstants.FORWARD_SLASH);
                }
            }

        }

        return fixedLink.toString();
    }

    private static String getSyndicatedPath(final String publishMapPath, final Session session,
            final QueryBuilder queryBuilder) {

        String syndicatedPath = null;
        try {
            final Map<String, Object> predicateTocMap = new HashMap<>();
            predicateTocMap.put(PN_LIMIT, MadisonConstants.P_LIMIT);
            predicateTocMap.put(DITAConstants.PATH_PROP_NAME, MadisonConstants.CONF_SYNDICATION_SETTINGS_ROOT);
            predicateTocMap.put(DITAConstants.PROPERTY_TYPE, NODE_TYPE_NT_UNSTRUCTURED);
            predicateTocMap.put(PN_PROPERTY, "@isSyndicated");
            predicateTocMap.put(PN_PROPERTY_OPERATION, MadisonConstants.EXISTS);
            final Query querySyndicatedPaths = queryBuilder.createQuery(PredicateGroup.create(predicateTocMap),
                    session);
            final SearchResult searchResultSyndicatedPaths = querySyndicatedPaths.getResult();
            final Iterator<Resource> sydicatedResources = searchResultSyndicatedPaths.getResources();

            while (sydicatedResources.hasNext()) {
                final Resource resource = sydicatedResources.next();
                if (null != resource) {
                    final Node destNode = resource.adaptTo(Node.class);
                    if (null != destNode && destNode.hasProperty(PN_DESTINATION_PATH)
                            && publishMapPath.contains(destNode.getProperty(PN_DESTINATION_PATH).getString())) {
                        LOG.debug("Ditamap :: {} is part of Syndication folder {}", publishMapPath,
                                destNode.getProperty(PN_DESTINATION_PATH).getString());
                        syndicatedPath = destNode.getParent().getProperty(PN_SOURCE_PATH).getString();
                        break;
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Error in checkSyndicatedPaths  {}", e);
            syndicatedPath = null;
        }

        return syndicatedPath;
    }

    public static Node getTopicNode(final Node parentNode) throws RepositoryException {
        final String resourceType = parentNode.hasProperty(DITAConstants.PN_SLING_RESOURCE_TYPE)
                ? parentNode.getProperty(DITAConstants.PN_SLING_RESOURCE_TYPE).getString()
                : null;
        if (org.apache.commons.lang.StringUtils.isNotBlank(resourceType) && TOPIC_RESOURCE_TYPE.equals(resourceType)) {
            return parentNode;
        } else {
            return getTopicNode(parentNode.getParent());
        }
    }

    /**
     * Get Guid from Asset Link
     * @param session
     * @param assetLink
     * @return
     */
    public static String getGuidForFmLink(Session session, String assetLink) {
        String linkFmGuid = "";

        try {
                Node root = session.getNode("/var/dxml/versionreferences");
                BTreeCustom pathbtree = new BTreeCustom(root.getNode("pathToGuid"), order);
                String newXrefPath = "";
                String xrefLinkPropValue = assetLink;
                LOG.debug("xref link: {}", xrefLinkPropValue);

                newXrefPath = xrefLinkPropValue;

                LOG.debug("Link value made absolute: {}", newXrefPath);
                if (newXrefPath.isEmpty()) {
                    newXrefPath = xrefLinkPropValue;
                }

                if (newXrefPath.contains("#")) {
                    newXrefPath = newXrefPath.substring(0, newXrefPath.indexOf("#"));
                    LOG.debug("Link absolute value had anchor id - removed it to get topic path: {}", newXrefPath);
                }


                try {
                    if (session.nodeExists(newXrefPath)) {
                        String newXrefPathAsKey = CommonUtils.fixKey(newXrefPath);
                        Node keyNode = pathbtree.getBTreeNodeForKey(newXrefPathAsKey);
                        String identifier = null;
                        if (keyNode.hasProperty(newXrefPathAsKey)) {
                            identifier = keyNode.getProperty(newXrefPathAsKey).getString();
                            identifier = CommonUtils.fixIdentifier(identifier);
                        }

                        LOG.info("Retreived uuid :{} of scope peer referred topic, key will be: {} ", identifier, newXrefPathAsKey);
                        linkFmGuid = identifier;
                    }
                } catch (RepositoryException var16) {
                    LOG.error("RepositoryException : {}", var16.getMessage());
                    var16.printStackTrace();
                }
        } catch (RepositoryException var17) {
            LOG.error("RepositoryException retrieving link guid from repository: {}" + var17.getMessage());
        } catch (Exception var18) {
            LOG.error("Exception retrieving link guid from btree node: {}" + var18.getMessage());
        }

        return linkFmGuid;
    }


    /**
     * get Updated xref Link for Asset using dam Link Url
     * @param session
     * @param assetPath
     * @return
     */
    public static String getUpdatedXrefLink(Session session, String assetPath) {
        String fmguid = getGuidForFmLink(session,assetPath);
        String xrefFixed = "";
        try {
            Node contentRootNode = session.getNode("/content");
            if (contentRootNode.hasNode("fmditacustom/xrefpathreferences")) {
                Node xrefIndexNode = session.getNode("/content/fmditacustom/xrefpathreferences");
                if (xrefIndexNode != null) {
                    LOG.debug("xrefPropertyVal: {}", fmguid);
                        String xrefFmGuid = fmguid;
                        LOG.debug("xreffmguid: {}", xrefFmGuid);
                        BTreeCustom xrefindexbtree = new BTreeCustom(xrefIndexNode, order);
                        Node keyNode = xrefindexbtree.getBTreeNodeForKey(xrefFmGuid);
                        if (keyNode != null && keyNode.hasProperty(xrefFmGuid)) {
                            xrefFixed = keyNode.getProperty(xrefFmGuid).getString();
                            LOG.debug("The topic with guid : {} is found to be published to AEM sites at: {}", xrefFmGuid, xrefFixed);
                            if (fmguid.contains("#")) {
                                String anchorLinkId = fmguid.substring(fmguid.indexOf("#"), fmguid.length());
                                if (xrefFixed.contains("#")) {
                                    xrefFixed = xrefFixed.substring(0, xrefFixed.indexOf("#"));
                                }
                                xrefFixed = xrefFixed + anchorLinkId;
                            }
                        } else {
                            LOG.debug("The topic with guid : {} is not published to AEM sites yet", xrefFmGuid);
                        }

                    LOG.info("Updated value for xref Link: {}", xrefFixed);
                }
            }
        } catch (RepositoryException var13) {
            LOG.error("RepositoryException retrieving fmguid from repository: {}" + var13.getMessage());
        } catch (Exception var14) {
            LOG.error("Exception retrieving fmguid from btree node: {}" + var14.getMessage());
        }

        return xrefFixed;
    }
}
