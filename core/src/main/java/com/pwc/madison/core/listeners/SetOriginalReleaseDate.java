package com.pwc.madison.core.listeners;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.SetOriginalReleaseDateConfiguration;
import com.pwc.madison.core.util.BulkDitaUtil;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

@Component(service = EventListener.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = SetOriginalReleaseDateConfiguration.class)
public class SetOriginalReleaseDate implements EventListener {

    private static final String JCR_CONTENT = "/" + com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;

    private static final String JCR_CONTENT_METADATA = "jcr:content/metadata";
    private static final String[] NODE_TYPES = {MadisonConstants.CQ_PAGE};
    private static final String MAP_PARENT = "mapParent";

    private ObservationManager observationManager;
    @Reference
    private SlingRepository repository;
    @Reference
    private ResourceResolverFactory resolverFactory;
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;
    
    private final Logger logger = LoggerFactory.getLogger(SetOriginalReleaseDate.class);

    private Session session;

    protected void activate(SetOriginalReleaseDateConfiguration configuraion) {
        try {
            session = repository.loginService(madisonSystemUserNameProviderService.getFmditaServiceUsername(), null);
            observationManager = session.getWorkspace().getObservationManager();
            if(null != configuraion && configuraion.serviceEnabled()){
                observationManager.addEventListener(this, Event.NODE_ADDED,
                    MadisonConstants.MADISON_SITES_ROOT, true, null, NODE_TYPES, true);
                logger.debug("*************added JCR event listener");
            }
        }catch (RepositoryException e){
            logger.error("SetOriginalReleaseDate - error while adding JCR event listener", e);
        }
    }

    protected void deactivate(final ComponentContext componentContext) {
        try {
            if (observationManager != null) {
                observationManager.removeEventListener(this);
                logger.debug("*************removed JCR event listener");
            }
        } catch (final RepositoryException re) {
            logger.error("*************error removing the JCR event listener ", re);
        } finally {
            if (session != null) {
                session.logout();
                session = null;
            }
        }
    }

    @Override
    public void onEvent(final EventIterator events) {
        logger.debug("Inside OnEvent ::::: START ");
        if(null == events){
            return;
        }
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
            if (null == resourceResolver) {
                logger.error("resourceResolver is null for the User {}", madisonSystemUserNameProviderService.getFmditaServiceUsername());
                return;
            }
            final HashSet<String> topicPathSet = new HashSet<>();
            final HashSet<String> mapPathSet = new HashSet<>();
            while (events.hasNext()) {
                final Event event = events.nextEvent();
                String nodePath = event.getPath();
                logger.debug("SetOriginalReleaseDate - Event triggered for - {}", nodePath);
                if(nodePath.endsWith(JCR_CONTENT)){
                    continue;
                }
                String assetPath = getSourcePath(nodePath, resourceResolver);
                if(StringUtils.isBlank(assetPath)){
                    continue;
                }
                if(assetPath.endsWith(DITAConstants.DITA_EXTENSION)){
                    if(isChunked(nodePath, resourceResolver)){
                        String mapParent = getMapParent(nodePath, resourceResolver);
                        if(StringUtils.isNotBlank(mapParent)){
                            mapPathSet.add(mapParent);
                        }
                    }else{
                        topicPathSet.add(assetPath);
                    }

                }else if(assetPath.endsWith(DITAConstants.DITAMAP_EXT) && isChunked(nodePath, resourceResolver)){
                    mapPathSet.add(assetPath);
                }
            }

            /* Iterate through ditamaps and get all topics referenced*/
            for (String mapPath : mapPathSet) {
                List <String> topics = fetchAllTopicsFromDitamap(mapPath, session, resourceResolver);
                for (String topic : topics) {
                    if (StringUtils.isNotBlank(topic)) {
                        /* Adding to new set to avoid duplicate topics */
                        topicPathSet.add(topic);
                    }
                }
            }

            /* Set original release date to final topic set */
            for (String assetPath : topicPathSet) {
                setOriginalReleaseDate(assetPath, resourceResolver);
            }

            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }
            logger.debug("Inside OnEvent ::::: END");
        } catch (final Exception e) {
            logger.error("Error in SetOriginalReleaseDate  {}", e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }

    }

    /**
     * Fetch Ditamap path from the given page
     * @param pagePath
     * @param resolver
     * @return publishingDitamapPath
     */
    private String getSourcePath(String pagePath, ResourceResolver resolver){
        String path = StringUtils.EMPTY;
        Object sourcePathObject = getProperty(pagePath.concat(JCR_CONTENT), DITAConstants.SOURCE_PATH, resolver);
        if(null == sourcePathObject){
            return  path;
        }
        if(sourcePathObject instanceof String){
            path = sourcePathObject.toString();
        }
        return path;
    }

    /**
     * Sets current timestamp as originalReleaseDate
     * @param assetPath
     * @param resolver
     * @throws RepositoryException
     */
    private void setOriginalReleaseDate(String assetPath, ResourceResolver resolver) throws RepositoryException {
        if(StringUtils.isBlank(assetPath) || null == resolver){
            return;
        }
        final Calendar currentDate = Calendar.getInstance();
        Resource resource = resolver.getResource(assetPath);
        if(null == resource){
            return;
        }
        Node asset = resource.adaptTo(Node.class);
        if(null == asset){
            return;
        }
        Node metadataNode = asset.getNode(JCR_CONTENT_METADATA);
        if(null == metadataNode){
            return;
        }
        if( !metadataNode.hasProperty(DITAConstants.META_ORIGINAL_RELEASE_DATE) || (metadataNode.hasProperty(DITAConstants.META_ORIGINAL_RELEASE_DATE) && StringUtils.isBlank(metadataNode.getProperties(DITAConstants.META_ORIGINAL_RELEASE_DATE).toString()))){
            asset.getNode(JCR_CONTENT_METADATA).setProperty(DITAConstants.META_ORIGINAL_RELEASE_DATE, currentDate);
            logger.debug("SetOriginalReleaseDate - Added original release Date to - {}", asset.getPath());
        }
    }

    /**
     * Checks if the provided page is generated out of chunked Ditamap
     * @param pagePath
     * @param resolver
     * @return isChunked
     */
    private boolean isChunked(String pagePath, ResourceResolver resolver){
        boolean chunked = false;
        Object topicTitlesObject = getProperty(pagePath.concat(JCR_CONTENT), MadisonConstants.PROPERTY_TOPIC_TITLE, resolver);
        if(null == topicTitlesObject){
            return chunked;
        }
        if( (topicTitlesObject instanceof  String[]) && ((String[])topicTitlesObject).length > 1 ){
            chunked = true;
        }
        return chunked;
    }

    /**
     * Returns the property value of the given property
     * @param path
     * @param property
     * @param resolver
     * @return propertyValue
     */
    private Object getProperty(String path, String property, ResourceResolver resolver){
        Object propertyValue = null;
        if(StringUtils.isBlank(path) || null == resolver){
            return propertyValue;
        }
        Resource resource = resolver.getResource(path);
        if(null == resource){
            return propertyValue;
        }
        ValueMap propertyMap = resource.getValueMap();
        if(propertyMap.containsKey(property)){
            propertyValue = propertyMap.get(property);
        }
        return propertyValue;
    }

    /**
     * Method to fetch all the topics from the given DITAMAP recursively.
     *
     * @param ditaMap
     * @param session
     * @return
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     */
    private final List<String> fetchAllTopicsFromDitamap(final String ditaMap,
                                                               final Session session, ResourceResolver resolver) throws RepositoryException {
        final List<String> topics = new ArrayList<>();
        // Fetch all the ditamap topic references.
        final Value[] fmditaTopicrefs = BulkDitaUtil.getTopicRefs(ditaMap, session);
        if (null == fmditaTopicrefs || fmditaTopicrefs.length == 0) {
            logger.error(
                "DITAMAP does not have any topics or sub maps to create a version. Cannot proceed with dita status update.");
            return topics;
        }
        // Fetch all topics from ditamap recursively and populate.
        fetchAllTopics(fmditaTopicrefs, topics, session, resolver);
        return topics;
    }

    /**
     * Method to get all the topics from the root ditamap and sub ditamaps.
     *
     * @param topicRefs
     * @param topics
     * @param session
     * @return
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void fetchAllTopics(final Value[] topicRefs, final List<String> topics,
                                       final Session session, ResourceResolver resolver) throws RepositoryException {
        if(topicRefs == null) {
            return;
        }
        for (Value topic : topicRefs) {
            String dita = topic.getString().replaceFirst(MadisonConstants.COMMA_SEPARATOR, StringUtils.EMPTY);
            if(StringUtils.isNotBlank(dita)) {
                if(dita.endsWith(DITAConstants.DITA_EXTENSION) && isApproved(dita, resolver)) {
                    topics.add(dita);
                } else if(dita.endsWith(DITAConstants.DITAMAP_EXT)) {
                    Value[] topicSubRefs = BulkDitaUtil.getTopicRefs(dita, session);
                    fetchAllTopics(topicSubRefs, topics, session, resolver);
                }
            }
        }
    }

    /**
     * Checks if the topic is approved
     * @param ditaPath
     * @param resolver
     * @return
     */
    private boolean isApproved(String ditaPath, ResourceResolver resolver){
        boolean approved = false;
        String docState;
        Object docStateObject = getProperty(ditaPath.concat("/").concat(JCR_CONTENT_METADATA), DITAConstants.PN_METADATA_DOCSTATE, resolver);
        if(null == docStateObject){
            return  approved;
        }
        if(docStateObject instanceof String){
            docState = docStateObject.toString();
            if(docState.equalsIgnoreCase(DITAConstants.DITA_DOCUMENTSTATE_APPROVED)){
                approved = true;
            }
        }
        return  approved;
    }

    /**
     * Returns mapParent property value
     * @param pagePath
     * @param resolver
     * @return mapParent
     */
    private String getMapParent(String pagePath, ResourceResolver resolver){
        String mapParent = StringUtils.EMPTY;
        Object mapParentObject = getProperty(pagePath.concat(JCR_CONTENT), MAP_PARENT, resolver);
        if(null == mapParentObject){
            return mapParent;
        }
        if(mapParentObject instanceof  String){
            /* Replacing semicolon from mapParent property(e.g. /content/dam/pwc-madison/ditaroot/us/en/pwc/test-content/toc-test.ditamap;) */
            mapParent = mapParentObject.toString().replace(MadisonConstants.SEMI_COLON_SEPARATOR, StringUtils.EMPTY);
        }
        return mapParent;
    }

}
