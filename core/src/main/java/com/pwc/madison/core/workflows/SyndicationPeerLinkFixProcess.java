package com.pwc.madison.core.workflows;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
    service = WorkflowProcess.class,
    property = { "process.label= Madision - Syndication Peer Links Fix Process" })
public class SyndicationPeerLinkFixProcess implements WorkflowProcess {

    private static final String PN_DESTINATION_PATH = "destinationPath";
    private static final String PN_SOURCE_PATH = "sourcePath";
    private static final String NODE_TYPE_NT_UNSTRUCTURED = "nt:unstructured";
    private static final String GENERATED_PATH = "generatedPath";
    private static final String OUTPUT_NAME = "outputName";
    private static final String IS_SUCCESS = "isSuccess";
    private static final String PN_PROPERTY_OPERATION = "property.operation";
    private static final String PN_PROPERTY = "property";
    private static final String PN_SCOPE = "scope";
    private static final String SCOPE_EXTERNAL = "external";
    private static final String PN_LIMIT = "p.limit";
    private static final String REL_PATH_SEPARATOR = "../";
    private static final String STRING_SPACE = " ";
    private static final String PERIOD_SEPARATOR = "..";
    private static final String REL_PATH_REPLACE_STRING = "\\.\\./";

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Override
    public void execute(final WorkItem item, final WorkflowSession wfsession, final MetaDataMap args)
            throws WorkflowException {

        final long startTime = System.currentTimeMillis();

        ResourceResolver resolver = null;
        Session session = null;
        try {
            final MetaDataMap meta = item.getWorkflowData().getMetaDataMap();

            if (null == meta) {
                log.error("Error in SyndicationPeerLinkFixProcess metadata empty {}", meta);
                return;
            }

            log.info("SyndicationPeerLinkFixProcess metadata info ::: {} ", meta);
            final String outputType = meta.get(IS_SUCCESS, String.class);
            final String outputPath = meta.get(GENERATED_PATH, String.class);
            final String outputName = meta.get(OUTPUT_NAME, String.class);

            if (StringUtils.isBlank(outputType) || StringUtils.isBlank(outputPath)
                    || !outputPath.contains(DITAConstants.HTML_EXT)
                    || !("AEMSITE".equalsIgnoreCase(outputName) || "previewsite".equalsIgnoreCase(outputName))) {
                log.error("Error for generated Page PATH {} for outputName {} which is {}",
                        new Object[] { outputPath, outputName, outputType });
                return;
            }

            final String publishMapPath = (String) item.getWorkflowData().getPayload();
            final String sourcePagePath = outputPath.substring(0, outputPath.indexOf(DITAConstants.HTML_EXT));

            resolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            if (null == resolver) {
                log.error(" ResourceResolver null in SyndicationPeerLinkFixProcess for user {}",
                        madisonSystemUserNameProviderService.getFmditaServiceUsername());
                return;
            }
            session = resolver.adaptTo(Session.class);

            if (StringUtils.isBlank(publishMapPath)) {
                log.error("Error As No publishMapPath found {} for path {}", publishMapPath, sourcePagePath);
                return;
            }
            final String syndicationSource = getSyndicatedPath(publishMapPath, session);
            if (StringUtils.isBlank(syndicationSource)) {
                log.warn("Skiping SyndicationPeerLinkFixProcess for the path {} as this is not syndicated content",
                        publishMapPath);
                return;
            }

            final String query = "SELECT * FROM [nt:base] AS entry WHERE ISDESCENDANTNODE(entry , '" + sourcePagePath
                    + "') AND entry.[sling:resourceType] = 'fmdita/components/dita/link' AND (entry.[scope] = 'peer' OR entry.[scope] = 'external')";
            final Iterator<Resource> result = resolver.findResources(query, javax.jcr.query.Query.JCR_SQL2);
            String link;
            String scope;
            while (result.hasNext()) {
                final Node entryNode = result.next().adaptTo(Node.class);
                String finalLink = StringUtils.EMPTY;
                link = entryNode.hasProperty(DITAConstants.PROPERTY_LINK)
                        ? entryNode.getProperty(DITAConstants.PROPERTY_LINK).getString()
                        : StringUtils.EMPTY;
                scope = entryNode.hasProperty(PN_SCOPE) ? entryNode.getProperty(PN_SCOPE).getString()
                        : StringUtils.EMPTY;
                if (StringUtils.isNotBlank(scope) && SCOPE_EXTERNAL.equals(scope)
                        && link.startsWith(MadisonConstants.PWC_MADISON_DAM_BASEPATH)) {
                    if (resolver.getResource(link) == null) {
                        finalLink = correctContentLink(link, entryNode, resolver, syndicationSource, true);
                    }
                } else if (link.startsWith(MadisonConstants.MADISON_SITES_ROOT)
                        || link.startsWith(MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH)) {
                    String pageLink = link;
                    if (link.contains(DITAConstants.HASH_STR)) {
                        pageLink = link.substring(0, link.indexOf(DITAConstants.HASH_STR));
                    }
                    if (null == resolver.getResource(pageLink)) {
                        finalLink = correctContentLink(link, entryNode, resolver, syndicationSource, false);
                    }
                } else {
                    String tmplink = link.replaceAll(REL_PATH_REPLACE_STRING, STRING_SPACE).trim();
                    tmplink = tmplink.replaceAll(STRING_SPACE, REL_PATH_SEPARATOR);
                    if (tmplink.indexOf(REL_PATH_SEPARATOR) > -1) {
                        finalLink = correctPeerLink(link, tmplink);
                    } else {
                        finalLink = link;
                    }
                    final String sourceTerritory = MadisonUtil.getTerritoryCodeForPath(syndicationSource);
                    final String destinationTerritory = MadisonUtil.getTerritoryCodeForPath(entryNode.getPath());
                    final String sourceLanguage = MadisonUtil.getLanguageCodeForPath(syndicationSource);
                    final String destinationLanguage = MadisonUtil.getLanguageCodeForPath(entryNode.getPath());
                    finalLink = finalLink.replace(
                            MadisonConstants.FORWARD_SLASH + sourceTerritory + MadisonConstants.FORWARD_SLASH + sourceLanguage,
                            MadisonConstants.FORWARD_SLASH + destinationTerritory + MadisonConstants.FORWARD_SLASH + destinationLanguage);
                }
                if (StringUtils.isNotBlank(finalLink)) {
                    entryNode.setProperty(DITAConstants.PROPERTY_LINK, finalLink);
                    log.info("Invalid link: {} corrected to: {} ", link, finalLink);
                }

            }

            if (resolver.hasChanges()) {
                resolver.commit();
            }

        } catch (final Exception e) {
            log.error("Error in SyndicationPeerLinkFixProcess {} ", e);
        } finally {
            if (null != resolver && resolver.isLive()) {
                resolver.close();
            }
            log.error("Total Time taken(in Milisecond) for SyndicationPeerLinkFixProcess :::: {}",
                    System.currentTimeMillis() - startTime);
        }
    }

    private String correctContentLink(final String link, final Node entryNode, final ResourceResolver resolver,
            final String syndicationSource, final boolean isAssetReference) {
        String fixedLink = link;
        try {
            final String destinationNodePath = entryNode.getPath();
            final String sourceTerritory = MadisonUtil.getTerritoryCodeForPath(syndicationSource);
            final String sourceLanguage = MadisonUtil.getLanguageCodeForPath(syndicationSource);
            String destinationTerritory = link.split(DITAConstants.FORWARD_SLASH)[4];
            String destinationLanguage = link.split(DITAConstants.FORWARD_SLASH)[5];
            if (isAssetReference) {
                destinationTerritory = MadisonUtil.getTerritoryCodeForPath(link);
                destinationLanguage = MadisonUtil.getLanguageCodeForPath(link);
            }
            final String sourceNodePath = destinationNodePath.replace(
                    MadisonConstants.FORWARD_SLASH + destinationTerritory + MadisonConstants.FORWARD_SLASH + destinationLanguage,
                    MadisonConstants.FORWARD_SLASH + sourceTerritory + MadisonConstants.FORWARD_SLASH + sourceLanguage);
            final Resource sourceNodeResource = resolver.getResource(sourceNodePath);
            if (sourceNodeResource != null) {
                final Node sourceNode = sourceNodeResource.adaptTo(Node.class);
                fixedLink = sourceNode.hasProperty(DITAConstants.PROPERTY_LINK)
                        ? sourceNode.getProperty(DITAConstants.PROPERTY_LINK).getString()
                        : StringUtils.EMPTY;
                if (!isAssetReference) {
                    fixedLink = fixedLink.replace(
                            MadisonConstants.FORWARD_SLASH + sourceTerritory + MadisonConstants.FORWARD_SLASH + sourceLanguage,
                            MadisonConstants.FORWARD_SLASH + destinationTerritory + MadisonConstants.FORWARD_SLASH + destinationLanguage);
                }
            }
        } catch (final RepositoryException e) {
            log.error("Error in getting the node path");
        }

        return fixedLink;
    }

    private String correctPeerLink(final String link, final String tmplink) {
        log.debug("Correcting peer link: {} ", link);
        final int linkCount = StringUtils.countMatches(tmplink, REL_PATH_SEPARATOR);
        final String[] split = link.split("\\/");
        final String[] reverseArray = new String[split.length];
        final StringBuilder fixedLink = new StringBuilder();
        boolean checker = true;
        for (int i = split.length - 1; i >= 0; i--) {
            if (split[i].equals(PERIOD_SEPARATOR) && checker) {
                i = i - (2 * linkCount) + 1;
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

    /**
     * @param publishMapPath
     *            publishMapPath
     * @param session
     *            session
     * @return true/false
     */
    private String getSyndicatedPath(final String publishMapPath, final Session session) {

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
                        log.debug("Ditamap :: {} is part of Syndication folder {}", publishMapPath,
                                destNode.getProperty(PN_DESTINATION_PATH).getString());
                        syndicatedPath = destNode.getParent().getProperty(PN_SOURCE_PATH).getString();
                        break;
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Error in checkSyndicatedPaths  {}", e);
            syndicatedPath = null;
        }

        return syndicatedPath;
    }

}
