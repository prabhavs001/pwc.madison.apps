package com.pwc.madison.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.*;
import javax.jcr.lock.LockManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Revision;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.commons.ReferenceSearch;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.DitaMapDetails;

/**
 * Utility methods for DITA map and related topics
 */
public class DITAUtils {

    private static final String DITA_TOPIC_NODE = "dita/topic";
    private static final String DITA_PWC_TOPIC_NODE = "dita/pwc-topic";
    private static final String JOINED_SECTION_PAGE_NAME = "-joined";
    public static final Logger LOGGER = LoggerFactory.getLogger(DITAUtils.class);

    private DITAUtils() {
    }

    /**
     * Method returns the Published date for the Generated AEM Output page of a DITAMAP
     */
    public static String getDITAMAPPublishedDate(final Page currentPage, final ResourceResolver resourceResolver) {

        if (currentPage == null || resourceResolver == null) {
            return StringUtils.EMPTY;
        }

        // Look for the 'headNode' resource under the current page, the DITA
        // topic metadata get injected inside the 'headNode' by DITA publishing
        // workflow
        final Resource headNode = resourceResolver.getResource(currentPage.getPath())
                .getChild(DITAConstants.DITA_PWC_HEAD_CONTAINER_V1);
        String dateCreated = StringUtils.EMPTY;

        if (headNode != null) {
            // Read the 'critdates' information from the XML data stored at the
            // 'critdates' node ,
            // for e.g.
            // /content/output/sites/305-10-00_ditamap/2122428/jcr:content/root/container/maincontainer/readerrow/docreader/headnode/critdates
            // The code does exactly what OOTB 'critdates' component does
            // '/libs/fmdita/components/dita/critdates'
            final Resource critdates = headNode.getChild(DITAConstants.TOPIC_CREATED_DATE_NODE_NAME);
            if (critdates != null) {
                final String xmlData = critdates.getValueMap().get(DITAConstants.PN_XML_DATA_PROPERTY, String.class);
                final Document doc = convertXMLStringToDocument(xmlData);
                if (doc == null) {
                    return StringUtils.EMPTY;
                }
                final NodeList created = doc.getElementsByTagName(DITAConstants.XML_NODE_CREATED_DATE);
                final Node crItem = created.getLength() > 0 ? created.item(0) : null;
                if (crItem != null && crItem instanceof Element) {
                    final Element elem = (Element) crItem;
                    dateCreated = elem.getAttribute(DITAConstants.XML_ATTRIBUTE_DATE);
                }
            }
        }

        return dateCreated;
    }

    private static Document convertXMLStringToDocument(final String xmlStr) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        final String XML_PARSIN_ERROR_MESSAGE = "Exception while Parsing Serialized DITA XML for Created Date";
        try {
            factory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(xmlStr)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.error(XML_PARSIN_ERROR_MESSAGE, e);
        }

        return doc;
    }

    /**
     *
     * This method converts the InputStream into document by Disabling DTD references
     *
     * @param is
     * @return document
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document convertInputStreamToDocument(final InputStream inputStream)
            throws ParserConfigurationException, SAXException, IOException {
        Document doc = null;
        if (null != inputStream) {
            final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setFeature(DITAConstants.EXTERNAL_GENERAL_ENTITIES, false);
            builderFactory.setFeature(DITAConstants.EXTERNAL_PARAMETER_ENTITIES, false);
            builderFactory.setFeature(DITAConstants.EXTERNAL_DTD_PATH, false);
            builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            builderFactory.setXIncludeAware(false);
            builderFactory.setExpandEntityReferences(false);
            builderFactory.setValidating(false);
            final DocumentBuilder builder = builderFactory.newDocumentBuilder();
            doc = builder.parse(inputStream);
        }
        return doc;
    }

    /**
     * Return the section title for FASB DITAMAP, Section title will have the format of
     * 'Topic-Sub-topic-Secnum-Sectitle'
     */
    public static String getDITASectionTitle(final Page currentPage, final ResourceResolver resourceResolver) {

        final StringBuilder titleBuilder = new StringBuilder();
        String longTitle = getLongTitle(currentPage, resourceResolver);
        if(StringUtils.isNotEmpty(longTitle)){
            titleBuilder.append(longTitle);
        }

        //Get the numbering for long title
        final String numbering = getAncestryValueFromHeadNode(currentPage, resourceResolver);
        if (StringUtils.isNotEmpty(numbering)) {
            titleBuilder.append(" ");
            titleBuilder.append(numbering);
        }

        return titleBuilder.toString();
    }

    /**
     * Returns the Long Title of the page.
     */
    public static String getLongTitle(Page currentPage, ResourceResolver resourceResolver) {
        if (currentPage == null || resourceResolver == null) {
            return StringUtils.EMPTY;
        }

        final StringBuilder titleBuilder = new StringBuilder();
        String longTitle = StringUtils.EMPTY;

        final Resource docReader = resourceResolver.getResource(currentPage.getPath())
                .getChild(DITAConstants.DOCREADER_NODE_RELATIVE_PATH_V1);
        Resource headNode = null;
        if (docReader != null) {
            headNode = docReader.getParent().getChild(DITAConstants.TOPIC_HEAD_NODE_NAME);
        }

        if(headNode != null) {
            final Iterable<Resource> dataNodes = headNode.getChildren();

            for (final Resource dataNode : dataNodes) {
                final ValueMap properties = dataNode.getValueMap();

                final String componentType = properties.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
                        String.class);
                if (StringUtils.isNotBlank(componentType)
                        && componentType.equals(DITAConstants.DITA_COMPONENT_DISCARD)) {
                    if (StringUtils.isNotBlank(properties.get(DITAConstants.PN_SECTION_LONG_TITLE, String.class))) {
                        longTitle = properties.get(DITAConstants.PN_SECTION_LONG_TITLE, String.class);
                    }
                }
            }

            // construct the Section title and return
            // section title will look like 'Long title
            // Topic-Subtopic-Sectionnum' or 'long title topic-subtopic' or
            // 'long title topic'
            if (StringUtils.isNotEmpty(longTitle)) {
                titleBuilder.append(longTitle);
            }
        }
        return titleBuilder.toString();
    }

    public static String getAncestryValueFromHeadNode(Page currentPage, ResourceResolver resourceResolver) {
        // Look for the 'headNode' resource under the current page, the DITA
        // topic metadata get injected inside the 'headNode' by DITA publishing
        // workflow
        final Resource docReader = resourceResolver.getResource(currentPage.getPath())
                .getChild(DITAConstants.DOCREADER_NODE_RELATIVE_PATH_V1);
        Resource headNode = null;

        String sectionNum = StringUtils.EMPTY;
        String subtopicNum = StringUtils.EMPTY;
        String topicNum = StringUtils.EMPTY;

        if (docReader != null) {
            headNode = docReader.getParent().getChild(DITAConstants.TOPIC_HEAD_NODE_NAME);
        }

        final StringBuilder numberingBuilder = new StringBuilder();

        if (headNode != null) {
            // There will be bunch of 'data' nodes under the head node, read
            // through all of them to look for the value of 'TopicNumber'
            // 'Sub Topic' number etc.
            // e.g.
            // /content/output/sites/305-10-00_ditamap/2122428/jcr:content/root/container/maincontainer/readerrow/docreader/headnode/data
            final Iterable<Resource> dataNodes = headNode.getChildren();

            for (final Resource dataNode : dataNodes) {
                // check if the resource type of the node is
                // 'fmdita/components/dita/discard' , data nodes which we are
                // considering here
                // will have the said resource type
                final ValueMap properties = dataNode.getValueMap();

                final String componentType = properties.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
                        String.class);
                if (StringUtils.isNotBlank(componentType)
                        && componentType.equals(DITAConstants.DITA_COMPONENT_DISCARD)) {
                    if (StringUtils.isNotBlank(properties.get(DITAConstants.PN_SECTION_NUMBER, String.class))) {
                        sectionNum = properties.get(DITAConstants.PN_SECTION_NUMBER, String.class);
                    } else if (StringUtils
                            .isNotBlank(properties.get(DITAConstants.PN_SUB_TOPIC_NUMBER, String.class))) {
                        subtopicNum = properties.get(DITAConstants.PN_SUB_TOPIC_NUMBER, String.class);
                    } else if (StringUtils.isNotBlank(properties.get(DITAConstants.PN_TOPIC_NUMBER, String.class))) {
                        topicNum = properties.get(DITAConstants.PN_TOPIC_NUMBER, String.class);
                    }
                }

            }

            if (StringUtils.isNotEmpty(topicNum)) {
                numberingBuilder.append(" ");
                numberingBuilder.append(topicNum);
            }
            if (StringUtils.isNotEmpty(subtopicNum)) {
                numberingBuilder.append("-");
                numberingBuilder.append(subtopicNum);
            }
            if (StringUtils.isNotEmpty(sectionNum)) {
                numberingBuilder.append("-");
                numberingBuilder.append(sectionNum);
            }

        }
        return numberingBuilder.toString();
    }

    /**
     * Return the Ancestry number stored at page content node for FASB DITA file, in the format of
     * 'TopicNum-SubtopicNum-SectionNum'
     */
    public static String getDitaAncestryValue(final Page currentPage, final ResourceResolver resourceResolver){
        if (currentPage == null || resourceResolver == null) {
            return StringUtils.EMPTY;
        }

        String ascSectionNum = StringUtils.EMPTY;
        String ascSubtopicNum = StringUtils.EMPTY;
        String ascTopicNum = StringUtils.EMPTY;

        final StringBuilder ancestryBuilder = new StringBuilder();
        final ValueMap properties = currentPage.getProperties();

        if(properties.containsKey(DITAConstants.PN_ASC_TOPIC_NUMBER)){
            String[] ascTopicNumArray = properties.get(DITAConstants.PN_ASC_TOPIC_NUMBER, String[].class);
            ascTopicNum = ascTopicNumArray[0];
        }

        if(properties.containsKey(DITAConstants.PN_ASC_SUBTOPIC_NUMBER)){
            String[] ascSubtopicNumArray = properties.get(DITAConstants.PN_ASC_SUBTOPIC_NUMBER, String[].class);
            ascSubtopicNum = ascSubtopicNumArray[0];
        }

        if(properties.containsKey(DITAConstants.PN_ASC_SECTION_NUMBER)){
            String[] ascSectionNumArray = properties.get(DITAConstants.PN_ASC_SECTION_NUMBER, String[].class);
            ascSectionNum = ascSectionNumArray[0];
        }

        if (StringUtils.isNotEmpty(ascTopicNum)) {
            ancestryBuilder.append(ascTopicNum);
        }
        if (StringUtils.isNotEmpty(ascSubtopicNum)) {
            ancestryBuilder.append("-");
            ancestryBuilder.append(ascSubtopicNum);
        }
        if (StringUtils.isNotEmpty(ascSectionNum)) {
            ancestryBuilder.append("-");
            ancestryBuilder.append(ascSectionNum);
        }

        return ancestryBuilder.toString();
    }

    /**
     * Method will iterate through all TOPICREFs for the given DITAMAP and check the 'docstatus' for all topics, it
     * return the approval status as true if all topics are approved, false otherwise.
     *
     * @param ditamapPath
     * @param requestResolver
     * @return
     */
    public static boolean checkAllTopicRefsApproved(final String ditamapPath, final ResourceResolver requestResolver) {

        if (StringUtils.isEmpty(ditamapPath) || requestResolver == null) {
            return false;
        }

        boolean allTopicsApproved = false;

        final List<Asset> topicRefs = getTopicRefs(ditamapPath, requestResolver, null);

        if (topicRefs != null && topicRefs.size() > 0) {
            // check the document status for each topic
            final Iterator<Asset> topicIter = topicRefs.iterator();

            while (topicIter.hasNext()) {
                final Asset topic = topicIter.next();

                // get the resource path for the Asset's metadata node , for e.g
                // /content/dam/pwc-madison/ditaroot/us/en/pwc/SAMPLE_bankruptcies_and_liq/SAMPLE_bankruptcies_and_liq/chapter_3_accounting/31-at-a-glance.dita/jcr:content/metadata

                final Resource ditaTopicMetadata = requestResolver.getResource(
                        topic.getPath() + "/" + JcrConstants.JCR_CONTENT + "/" + DamConstants.ACTIVITY_TYPE_METADATA);

                ValueMap metadataMap = null;

                if (ditaTopicMetadata.getValueMap() != null) {
                    metadataMap = ditaTopicMetadata.getValueMap();
                }

                // check the metadata property called 'docstate' for the DITA
                // topic asset
                if (ditaTopicMetadata != null && metadataMap != null) {
                    final String documentState = metadataMap.get(DITAConstants.PN_METADATA_DOCSTATE, String.class);

                    // the topic is considered as approved if it carry either
                    // 'Approved' or 'Done' status
                    if (StringUtils.isNotEmpty(documentState)
                            && (documentState.equals(DITAConstants.DITA_DOCUMENTSTATE_APPROVED)
                                    || documentState.equals(DITAConstants.DITA_DOCUMENTSTATE_DONE))) {
                        allTopicsApproved = true;
                    } else {
                        allTopicsApproved = false;
                    }
                }

            }
        }

        return allTopicsApproved;
    }

    /**
     * Method returns the list of all topic references within a DITAMAP
     *
     * @param ditamapPath
     * @param requestResolver
     * @param topicRefs
     * @return
     */
    public static List<Asset> getTopicRefs(final String ditamapPath, final ResourceResolver requestResolver,
            List<Asset> topicRefs) {

        if (StringUtils.isEmpty(ditamapPath) || requestResolver == null) {
            return null;
        }

        if (topicRefs == null) {
            topicRefs = new ArrayList<>();
        }

        // retrieve the ditamap resource
        final Resource ditaMap = requestResolver.getResource(ditamapPath);

        if (ditaMap != null) {
            // read the values of the property 'fmditaTopicrefs' from the
            // 'jcr:content' node , for e.g.
            // /content/dam/pwc-madison/ditaroot/us/en/pwc/SAMPLE_bankruptcies_and_liq/SAMPLE_bankruptcies_and_liq/bankruptcies_and_liq.US.ditamap/jcr:content
            final Resource ditamapContent = ditaMap.getChild(JcrConstants.JCR_CONTENT);

            // read the properties
            final ValueMap properties = ditamapContent.getValueMap();

            final String[] topicReferences = properties.get(DITAConstants.PN_FMDITATOPICREFS, new String[] {});

            if (topicReferences != null && topicReferences.length > 0) {
                for (String topicRef : topicReferences) {

                    // each topic ref represent a valid DITA Topic or DITAMAP,
                    // so let's recursively read and populate them to a list
                    // The topicReference in CRX is seen as stored with a comma,
                    // hence adding the below condition
                    // for e.g.
                    // ,/content/dam/pwc-madison/ditaroot/us/en/pwc/SAMPLE_bankruptcies_and_liq/SAMPLE_bankruptcies_and_liq/chapter_3_accounting/31-at-a-glance.dita

                    if (topicRef.startsWith(",")) {
                        topicRef = topicRef.split(",")[1];
                    }

                    getTopicRefs(topicRef, requestResolver, topicRefs);
                }
            } else {
                // when the property 'fmditaTopicRefs' is not available for a
                // DITA document, you can safely assume that it's a DITA TOPIC
                // i.e .dita file
                // so adding the topics to the list and returning
                topicRefs.add(ditaMap.adaptTo(Asset.class));
                return topicRefs;
            }

        }
        return topicRefs;
    }

    /**
     * This method fetches all ditamaps referred by a parent ditamap
     *
     * @param ditamapPath
     * @param requestResolver
     * @param ditaMapsRefs
     * @return
     */
    public static List<DitaMapDetails> getDitaMapsRefs(final String ditamapPath, final ResourceResolver requestResolver,
            List<DitaMapDetails> ditaMapsRefs) {
        if (StringUtils.isEmpty(ditamapPath) || requestResolver == null) {
            return null;
        }

        // retrieve the ditamap resource
        final Resource ditaMap = requestResolver.getResource(ditamapPath);

        if (ditaMap != null) {
            // read the values of the property 'fmditaTopicrefs' from the
            // 'jcr:content' node , for e.g.
            // /content/dam/pwc-madison/ditaroot/us/en/pwc/SAMPLE_bankruptcies_and_liq/SAMPLE_bankruptcies_and_liq/bankruptcies_and_liq.US.ditamap/jcr:content
            final Resource ditamapContent = ditaMap.getChild(JcrConstants.JCR_CONTENT);

            // read the properties
            final ValueMap properties = ditamapContent.getValueMap();

            final String[] topicReferences = properties.get(DITAConstants.PN_FMDITATOPICREFS, new String[] {});

            if (topicReferences != null && topicReferences.length > 0) {
                if (ditaMapsRefs == null) {
                    ditaMapsRefs = new ArrayList<>();
                    populateReferencedMapsList(ditaMap, ditaMapsRefs);
                }
                for (String topicRef : topicReferences) {

                    // each topic ref represent a valid DITA Topic or DITAMAP,
                    // so let's recursively read and populate the referenced ditamaps to the List
                    // The topicReference in CRX is seen as stored with a comma,
                    // hence adding the below condition
                    // for e.g.
                    // ,/content/dam/pwc-madison/ditaroot/us/en/pwc/SAMPLE_bankruptcies_and_liq/SAMPLE_bankruptcies_and_liq/chapter_3_accounting/31-at-a-glance.dita

                    if (topicRef.startsWith(",")) {
                        topicRef = topicRef.split(",")[1];
                    }
                    if (topicRef.endsWith(DITAConstants.DITAMAP_EXT)) {
                        final Resource ditamapRes = requestResolver.getResource(topicRef);
                        if (null != ditamapRes) {
                            populateReferencedMapsList(ditamapRes, ditaMapsRefs);
                        }
                    }
                    getDitaMapsRefs(topicRef, requestResolver, ditaMapsRefs);
                }
            }
        }
        return ditaMapsRefs;
    }

    private static void populateReferencedMapsList(final Resource ditamapRes, final List<DitaMapDetails> ditaMapsRefs) {
        final Asset ditaMapAsset = ditamapRes.adaptTo(Asset.class);
        final DitaMapDetails ditaMapDetails = new DitaMapDetails();
        ditaMapDetails.setDitaMapPath(ditaMapAsset.getPath());
        ditaMapDetails.setDitaMapName(ditaMapAsset.getName());
        ditaMapDetails.setLastModifiedDate(ditaMapAsset.getLastModified());
        ditaMapsRefs.add(ditaMapDetails);
    }

    /**
     * This method is used to set the specified doc state on the nodes of the mentioned paths
     *
     * @param paths
     * @param docstate
     * @param adminSession
     * @param save
     * @param isReview
     *            :- this variable is set to false for changing docstatus from Done to Draft(in case of triggering a
     *            simple review task) with out changing status to 0 and without removing the metadata.
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public static void setDocStates(final String paths[], final String docstate, final Session adminSession,
            final boolean save, final boolean isReview, final String wfProgressType)
            throws PathNotFoundException, RepositoryException {
        for (final String path : paths) {
            if (!path.isEmpty()) {
                if (null != adminSession.getNode(path)) {
                    if (null != wfProgressType) {
                        WorkFlowUtil.setStatus(path, adminSession, wfProgressType);
                    }
                    final javax.jcr.Node node = adminSession.getNode(path)
                            .getNode(org.apache.jackrabbit.JcrConstants.JCR_CONTENT + "/metadata");
                    if (null != node) {
                        node.setProperty(DITAConstants.PN_METADATA_DOCSTATE, docstate);
                        // Set the "status" to 0 (not in review) if the doc state is
                        // Done or Draft
                        final javax.jcr.Node jcrNode = node.getParent();
                        if ((docstate.equals(DITAConstants.DITA_DOCUMENTSTATE_DONE)
                                || docstate.equals(DITAConstants.DITA_DOCUMENTSTATE_DRAFT))
                                && jcrNode.hasProperty(DITAConstants.REVIEW_WF_PROP_NAME) && isReview) {
                            jcrNode.setProperty(DITAConstants.STATUS_PROP_NAME, DITAConstants.STATUS_NONE);
                            jcrNode.getProperty(DITAConstants.REVIEW_WF_PROP_NAME).remove();
                        }
                    }
                }
            }
        }
        if (save) {
            adminSession.save();
        }
    }

    /**
     * Fetch the document status of the given topic.
     *
     * @param filePath
     * @param adminSession
     * @return
     */
    public static String getDocState(final String filePath, final Session adminSession) {
        if (null == adminSession || StringUtils.isBlank(filePath)) {
            LOGGER.error("Error getting the doc state :: parameters are null");
            return null;
        }
        final String metadataPath = filePath + "/" + JcrConstants.JCR_CONTENT + "/" + "metadata";
        try {
            final String primaryType = adminSession.getNode(filePath).getProperty(JcrConstants.JCR_PRIMARYTYPE)
                    .getString();
            if (primaryType.equals("sling:OrderedFolder") || primaryType.equals("nt:folder")
                    || primaryType.equals("sling:Folder")) {
                return null;
            }
            final javax.jcr.Node node = adminSession.getNode(metadataPath);
            return node.getProperty(DITAConstants.PN_METADATA_DOCSTATE).getString();
        } catch (final Exception e) {
            LOGGER.warn("Failed to fetch the docstate for the given topic " + filePath, e);
        }
        return null;
    }

    /**
     * Set last published date for the DITA post publishing.
     *
     * @param paths
     * @param lastPublished
     * @param adminSession
     * @throws RepositoryException
     */
    public static void setLastPublishedDate(final String paths[], final String lastPublished,
            final Session adminSession) {
        if (null == paths || paths.length == 0 || lastPublished.isEmpty() || null == adminSession) {
            LOGGER.error("Error setting the last published :: parameters are null");
            return;
        }
        String dita = StringUtils.EMPTY;

        for (final String path : paths) {
            try {
                if (null != adminSession.getNode(path)) {
                    // Capturing the DITA path to local variable in order to log, in case of error.
                    dita = path;
                    final javax.jcr.Node node = adminSession.getNode(path)
                            .getNode(org.apache.jackrabbit.JcrConstants.JCR_CONTENT + "/metadata");
                    if (null != node) {
                        node.setProperty(DITAConstants.PN_METADATA_LAST_PUBLISHED, lastPublished);
                        adminSession.save();
                    }
                }
            } catch (final RepositoryException e) {
                LOGGER.error("Failed to save the last published date for the DITA :" + dita);
            }
        }
    }

    /**
     * Creates separate groups with the participants(reviewer, approver and publisher) dynamically
     *
     * @param authorizableNames
     * @param resourceResolverFactory
     * @param type
     * @param workflowId
     * @return
     */
    public static String createParentGroup(final String[] authorizableNames,
            final ResourceResolverFactory resourceResolverFactory, final String type, final String workflowId, final String fmditaServiceName) {
        final String workId = WorkFlowUtil.getUniqueWorkId(workflowId);
        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                fmditaServiceName);
        final Session session = resourceResolver.adaptTo(Session.class);
        final UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        String parentGroupName = StringUtils.EMPTY;
        try {
            if (type.equals(DITAConstants.REVIEWER)) {
                parentGroupName = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.REVIEWER);
            } else if (type.equals(DITAConstants.APPROVER)) {
                parentGroupName = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.APPROVER);
            } else if (type.equals(DITAConstants.PUBLISHER)) {
                parentGroupName = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.PUBLISHER);
            } else if (type.equals(DITAConstants.REJECTION_LIST)) {
                parentGroupName = DITAConstants.MAC_DEFAULT.concat("-").concat(DITAConstants.REJECTION_LIST);
            }
            if (null != workflowId && !workflowId.isEmpty()) {
                parentGroupName = parentGroupName.concat("-").concat(workId);
            }
            final Group group = userManager.createGroup(new SimplePrincipal(parentGroupName),
                    MadisonConstants.MADISON_USER_GROUPS_ROOT);
            final ValueFactory valueFactory = session.getValueFactory();
            final Value groupNameValue = valueFactory.createValue(parentGroupName, PropertyType.STRING);
            group.setProperty("./profile/givenName", groupNameValue);
            session.save();
            addMembersToGroup(authorizableNames, group, userManager, session);
        } catch (final RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (null != resourceResolver && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
        return parentGroupName;
    }

    /**
     * Add members to a mentioned group
     *
     * @param authorizableNames
     * @param parentGroupName
     * @param userManager
     * @param session
     */
    private static void addMembersToGroup(final String[] authorizableNames, final Group parentGroupName,
            final UserManager userManager, final Session session) {
        try {
            for (final String authorizableName : authorizableNames) {
                final Authorizable authorizable = userManager.getAuthorizable(authorizableName);
                if (null != authorizable) {
                    parentGroupName.addMember(authorizable);
                    session.save();
                }
            }
        } catch (final RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * This method is for deleting dynamically created groups while the execution of worklows
     *
     * @param groupName
     * @param resolverFactory
     */
    public static void deleteGroup(final String groupName, final ResourceResolverFactory resolverFactory, final String fmditaServiceName) {
        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resolverFactory,
                fmditaServiceName);
        if (null != resourceResolver) {
            final UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            final Session session = resourceResolver.adaptTo(Session.class);
            try {
                if (null != groupName && !groupName.isEmpty()) {
                    final Authorizable authorizable = userManager.getAuthorizable(groupName);
                    if (null != authorizable && authorizable instanceof Group) {
                        final Group group = (Group) authorizable;
                        final Iterator<Authorizable> directMembers = group.getDeclaredMembers();
                        while (directMembers.hasNext()) {
                            final Authorizable member = directMembers.next();
                            group.removeMember(member);
                        }
                        final Resource groupResource = resourceResolver.getResource(group.getPath());
                        resourceResolver.delete(groupResource);
                        session.save();
                    }
                }
            } catch (final RepositoryException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (final PersistenceException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                if (null != resourceResolver && resourceResolver.isLive()) {
                    resourceResolver.close();
                }
            }
        }
    }

    private static class SimplePrincipal implements Principal {
        protected final String name;

        public SimplePrincipal(final String name) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("Principal name cannot be blank.");
            }
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof Principal) {
                return name.equals(((Principal) obj).getName());
            }
            return false;
        }
    }

    /**
     * This will get the list of all the meta-data mapped to page.
     *
     * @param session
     * @param link
     * @return
     * @throws RepositoryException
     */
    public static Map<String, String> getPageMetaDataProperties(final Session session, final String link)
            throws RepositoryException {
        final HashMap<String, String> metaMap = new HashMap<>();
        if (null == link || null == session) {
            return metaMap;
        }

        final javax.jcr.Node pageNode = session.getNode(link + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        if (null == pageNode) {
            return metaMap;
        }

        // Populate the country.
        final String country = pageNode.hasProperty(DITAConstants.META_COUNTRY)
                ? pageNode.getProperty(DITAConstants.META_COUNTRY).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_COUNTRY, country);

        // Populate the content type.
        final String contentType = pageNode.hasProperty(DITAConstants.META_CONTENT_TYPE)
                ? pageNode.getProperty(DITAConstants.META_CONTENT_TYPE).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_CONTENT_TYPE, contentType);

        // Populate the content ID.
        final String contentId = pageNode.hasProperty(DITAConstants.META_CONTENT_ID)
                ? pageNode.getProperty(DITAConstants.META_CONTENT_ID).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_CONTENT_ID, contentId);

        // Populate the revision date.
        final String revisionDate = pageNode.hasProperty(DITAConstants.META_REVISION_DATE)
                ? pageNode.getProperty(DITAConstants.META_REVISION_DATE).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_REVISION_DATE, revisionDate);

        // Populate the publication date.
        final String publicationDate = pageNode.hasProperty(DITAConstants.META_PUBLICATION_DATE)
                ? pageNode.getProperty(DITAConstants.META_PUBLICATION_DATE).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_PUBLICATION_DATE, publicationDate);

        // Populate the Audience.
        final String audience = pageNode.hasProperty(DITAConstants.META_AUDIENCE)
                ? pageNode.getProperty(DITAConstants.META_AUDIENCE).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_AUDIENCE, audience);

        // // Populate the Private Group.
        // final String privateGroup = pageNode.hasProperty(DITAConstants.META_PRIVATE_GROUP)
        // ? pageNode.getProperty(DITAConstants.META_PRIVATE_GROUP).getString()
        // : StringUtils.EMPTY;
        // metaMap.put(DITAConstants.META_PRIVATE_GROUP, privateGroup);

        // Populate the Access Level.
        final String accessLevel = pageNode.hasProperty(DITAConstants.META_ACCESS_LEVEL)
                ? pageNode.getProperty(DITAConstants.META_ACCESS_LEVEL).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_ACCESS_LEVEL, accessLevel);

        // // Populate the License.
        // final String license = pageNode.hasProperty(DITAConstants.META_LICENSE)
        // ? pageNode.getProperty(DITAConstants.META_LICENSE).getString()
        // : StringUtils.EMPTY;
        // metaMap.put(DITAConstants.META_LICENSE, license);

        // Populate the Issuing Body.
        final String issuingBody = pageNode.hasProperty(DITAConstants.META_STANDARD_SETTERS)
                ? pageNode.getProperty(DITAConstants.META_STANDARD_SETTERS).getString()
                : StringUtils.EMPTY;
        metaMap.put(DITAConstants.META_STANDARD_SETTERS, issuingBody);

        return metaMap;
    }

    /**
     * Method to get the dita type based on internal/external.
     *
     * @param session
     * @param relatedLink
     * @return
     * @throws RepositoryException
     */
    public static boolean isDitaTopicInternal(final Session session, final String relatedLink)
            throws RepositoryException {
        boolean ditaType = Boolean.FALSE;
        if (null == session || StringUtils.isBlank(relatedLink)
                || !session.nodeExists(relatedLink + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT)) {
            return ditaType;
        }

        final javax.jcr.Node childNode = session
                .getNode(relatedLink + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);

        if (childNode.hasProperty(DITAConstants.META_STANDARD_SETTERS)) {
            final String standardSetterVal = childNode.getProperty(DITAConstants.META_STANDARD_SETTERS).getString();
            if (DITAConstants.PWC_SOURCE_VALUE.equalsIgnoreCase(standardSetterVal)) {
                ditaType = Boolean.TRUE;
            }
        }
        return ditaType;
    }

    /**
     * Method to read any full path of the DITA and get the related cq:Page.
     *
     * @param ditaPath
     * @param resolver
     * @return
     */
    public static String getPageFromXrefDita(final String ditaPath, final ResourceResolver resolver, final XSSAPI xssapi) {
    	return getPagePathFromXrefDita(ditaPath, resolver, "", xssapi);
    }
    
    /**
     * Method to read any full path of the DITA and get the related us fasb cq:Page.
     *
     * @param ditaPath
     * @param resolver
     * @return
     */
    public static String getFasbPageFromXrefDita(final String ditaPath, final ResourceResolver resolver, final String searchRootPath, final XSSAPI xssapi) {
    	return getPagePathFromXrefDita(ditaPath, resolver, searchRootPath, xssapi);
    	}


    private static String getPagePathFromXrefDita(final String ditaPath, final ResourceResolver resolver, final String searchRootPath, final XSSAPI xssapi) {

        if (null == resolver || StringUtils.isBlank(ditaPath)) {
            return StringUtils.EMPTY;
        }
        if (null != xssapi) {
			LOGGER.debug("Getting refrence for ditaPath : " + xssapi.encodeForHTML(ditaPath));
		}
		final ReferenceSearch referenceSearch = new ReferenceSearch();
        referenceSearch.setExact(true);
        referenceSearch.setHollow(false);
        if(StringUtils.isNotBlank(searchRootPath)){
        	referenceSearch.setSearchRoot(searchRootPath);
        }
        
        final Collection<ReferenceSearch.Info> resultSet = referenceSearch.search(resolver, ditaPath).values();
        if (resultSet.isEmpty()) {
            if (!ditaPath.endsWith(MadisonConstants.SLING_SELECTORS_DITAMAP)) {
                return getPageFromDita(ditaPath, resolver, searchRootPath, xssapi);
            }
            return ditaPath;
        }
        final List<Page> refPages = new ArrayList<>(resultSet.size());
        for (final ReferenceSearch.Info info : resultSet) {
            final Page topicPage = info.getPage();
            /* Exclude preview pages and joined section pages */
            if (null != topicPage.getPath()
                && !topicPage.getPath().startsWith(MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH) 
                && !topicPage.getPath().endsWith(JOINED_SECTION_PAGE_NAME)) {
                    refPages.add(topicPage);
            }
        }
        if (!refPages.isEmpty()) {
            refPages.sort(new PageSortComparator());
            LOGGER.debug("Latest modified refrence is =>" + refPages.get(0).getPath());
            return refPages.get(0).getPath();
        } else
            return null;
    }
    
    private static String getPageFromDita(final String ditaPath, final ResourceResolver resolver, final String searchRootPath, final XSSAPI xssapi) {

        if (ditaPath.endsWith(MadisonConstants.SLING_SELECTORS_DITAMAP)) {
            return ditaPath;
        }
        List<String> mapReferences= new ArrayList<>();
        if(StringUtils.isNotBlank(searchRootPath) && searchRootPath.equals(MadisonConstants.FASB_US_SEARCH_ROOT_PATH)) {
        	mapReferences = getFasbMapReference(ditaPath, resolver);
        }else {
        	mapReferences = getMapReference(ditaPath, resolver);
        }
        String pagePath = ditaPath;

        if (mapReferences.isEmpty()) {
            return pagePath;
        }

        for (final String map : mapReferences) {
        	if(StringUtils.isNotBlank(searchRootPath) && searchRootPath.equals(MadisonConstants.FASB_US_SEARCH_ROOT_PATH)) {
        		pagePath = getFasbPageFromXrefDita(map, resolver,searchRootPath, xssapi);
        	}else {
        		pagePath = getPageFromXrefDita(map, resolver, xssapi);
        	}
            if (StringUtils.isNotBlank(pagePath) && !pagePath.equals(map)) {
                break;
            }
        }

        return pagePath;
    }
    
    public static List<String> getFasbMapReference(final String ditaPath, final ResourceResolver resolver) {
        final List<String> mapList = new ArrayList<>();
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("path", MadisonConstants.PWC_MADISON_FASB_DITAROOT_DAM_PATH);
        predicateMap.put("property", "jcr:content/@fmditaTopicrefs");
        predicateMap.put("property.value", "%" + ditaPath + "%");
        predicateMap.put("property.operation", "like");
        final QueryBuilder queryBuilder = resolver.adaptTo(QueryBuilder.class);

        if (null == queryBuilder) {
            return mapList;
        }

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        while (resources.hasNext()) {
            mapList.add(resources.next().getPath());
        }

        return mapList;
    }
    

    public static List<String> getMapReference(final String ditaPath, final ResourceResolver resolver) {
        final List<String> mapList = new ArrayList<>();
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("path", MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH);
        predicateMap.put("property", "jcr:content/@fmditaTopicrefs");
        predicateMap.put("property.value", "%" + ditaPath + "%");
        predicateMap.put("property.operation", "like");
        final QueryBuilder queryBuilder = resolver.adaptTo(QueryBuilder.class);

        if (null == queryBuilder) {
            return mapList;
        }

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        while (resources.hasNext()) {
            mapList.add(resources.next().getPath());
        }

        return mapList;

    }

    private static class PageSortComparator implements Comparator<Page> {

        @Override
        public int compare(final Page e1, final Page e2) {
            return e2.getLastModified().compareTo(e1.getLastModified());
        }
    }

    /**
     * Return Share or Mail Feature based on Content visibility.
     *
     * @param pagePath
     *            {@link String}
     * @param session
     *            {@session Session}
     * @return true/false {@link Boolean}
     * @throws RepositoryException
     */
    public static boolean isShareWithMail(final String pagePath, final ResourceResolver resolver) {

        if (StringUtils.isBlank(pagePath) || null == resolver) {
            return false;
        }

        final PageManager pageManager = resolver.adaptTo(PageManager.class);
        ValueMap properties = null;

        if (pageManager != null) {
            final Page currentPage = pageManager.getPage(pagePath);
            if (currentPage != null) {
                if (currentPage.getContentResource() != null
                        && currentPage.getContentResource().getValueMap() != null) {
                    properties = currentPage.getContentResource().getValueMap();
                }
            }
        }

        // In case of null properties , return true.
        if (null == properties) {
            return true;
        }

        LOGGER.debug("Get all the DITA meta-data properties from corresponding page:: {}", pagePath);

        // Get access related meta-data properties for corresponding page
        final String metaAudience = properties.get(DITAConstants.META_AUDIENCE, String.class);

        // True to show share option and False to show only Mail icon, if properties
        // doesn't exists then true
        // Validate content if marked for Internal Only
        if (StringUtils.isNotBlank(metaAudience) && (DITAConstants.AUDIENCE_INTERNAL_ONLY.equals(metaAudience)
                || DITAConstants.AUDIENCE_PRIVATE.equals(metaAudience))) {
            LOGGER.debug("Content marked for as {} ", metaAudience);
            return false;
        }

        return true;
    }

    /**
     * Method to get the path of the chapter to which the current page's map parent property matches.
     *
     * @param topicRefs
     * @param mapParent
     * @param resolver
     * @param parentRef
     * @return String
     * @throws RepositoryException
     */
    public static String getChapterForATopic(final Value[] topicRefs, final String mapParent,
            final ResourceResolver resolver, final String parentRef) throws RepositoryException {
        String chapterPath = null;
        if (topicRefs == null || mapParent == null || resolver == null) {
            return null;
        }
        for (final Value value : topicRefs) {
            if (value.getString().substring(1).equals(mapParent)) {
                chapterPath = StringUtils.isBlank(parentRef) ? mapParent : parentRef;
                break;
            }
        }
        if (StringUtils.isNotBlank(chapterPath)) {
            return chapterPath.replace(DITAConstants.DITAMAP_EXT, DITAConstants.DITA_EXTENSION);
        } else {
            chapterPath = getChapterPathForNestedChildren(parentRef, topicRefs, mapParent, resolver);
        }
        return chapterPath;
    }

    /**
     * Method to return the chapterPath of a nested child page by recursively iterating through fmditaTopicRefs property
     * to match with the mapParent of the page
     *
     * @param parentRef
     * @param topicRefs
     * @param mapParent
     * @param resolver
     * @return String
     * @throws RepositoryException
     */
    private static String getChapterPathForNestedChildren(String parentRef, final Value[] topicRefs,
            final String mapParent, final ResourceResolver resolver) throws RepositoryException {
        Resource topicRefResource;
        javax.jcr.Node topicRefNode;
        String chapterPath = null;
        for (final Value value : topicRefs) {
            if (value.getString().endsWith(DITAConstants.DITAMAP_EXT)) {
                if (StringUtils.isBlank(parentRef)) {
                    parentRef = value.getString().substring(1);
                }
                topicRefResource = resolver.getResource(
                        value.getString().substring(1) + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
                if (topicRefResource != null) {
                    topicRefNode = topicRefResource.adaptTo(javax.jcr.Node.class);
                    final Value[] childTopicRefs = topicRefNode.getProperty(DITAConstants.PN_FMDITATOPICREFS)
                            .getValues();
                    chapterPath = getChapterForATopic(childTopicRefs, mapParent, resolver, parentRef);
                    if (null != chapterPath) {
                        chapterPath = chapterPath.replace(DITAConstants.DITAMAP_EXT, DITAConstants.DITA_EXTENSION);
                        break;
                    }
                }
                parentRef = null;
            }

        }
        return chapterPath;
    }

    /**
     * Method to return if page is of type bookmap based on the dita_class property
     *
     * @param sourcePath
     * @param resolver
     * @return boolean
     */
    public static boolean isBookMap(final String sourcePath, final ResourceResolver resolver) {
        final Resource metaDataResource = resolver.getResource(sourcePath + DITAConstants.FORWARD_SLASH
                + JcrConstants.JCR_CONTENT + DITAConstants.FORWARD_SLASH + DITAConstants.METADATA_NAME);
        if (metaDataResource == null) {
            return false;
        }
        final javax.jcr.Node metaDataNode = metaDataResource.adaptTo(javax.jcr.Node.class);
        try {
            if (metaDataNode.hasProperty(DITAConstants.PROPERTY_DITA_CLASS)) {
                return metaDataNode.getProperty(DITAConstants.PROPERTY_DITA_CLASS).getString()
                        .contains(DITAConstants.DITA_TYPE_BOOKMAP);
            }
        } catch (final RepositoryException e) {
            LOGGER.error("Error in finding the map type", e);
            return false;
        }
        return false;
    }

    /**
     * Checks if is TOC hidden.
     *
     * @param entryNode
     *            the entry node
     * @return true, if is TOC hidden
     * @throws RepositoryException
     *             the repository exception
     */
    public static boolean isTOCHidden(final javax.jcr.Node entryNode) throws RepositoryException {
        return null != entryNode && entryNode.hasProperty("toc")
                && StringUtils.equalsIgnoreCase("no", entryNode.getProperty("toc").getString());
    }

    /**
     * Checks if is download enabled.
     *
     * @param sourcePath
     *            the source path
     * @param resolver
     *            the resolver
     * @return true, if is download enabled
     */
    public static boolean isDownloadEnabled(final String sourcePath, final ResourceResolver resolver) {
        final Resource metaDataResource = resolver.getResource(sourcePath + DITAConstants.FORWARD_SLASH
                + JcrConstants.JCR_CONTENT + DITAConstants.FORWARD_SLASH + DITAConstants.METADATA_NAME);
        if (metaDataResource == null) {
            return false;
        }
        final javax.jcr.Node metaDataNode = metaDataResource.adaptTo(javax.jcr.Node.class);
        try {
            if (null != metaDataNode && metaDataNode.hasProperty(DITAConstants.META_DISABLE_PDF_DWNLD)) {
                return StringUtils.equalsIgnoreCase("no",
                        metaDataNode.getProperty(DITAConstants.META_DISABLE_PDF_DWNLD).getString());
            } else {
                return true;
            }
        } catch (final RepositoryException e) {
            LOGGER.error("Error in getting metadata node for {}", sourcePath, e);
            return true;
        }
    }

    /**
     * Checks if is chunked.
     *
     * @param pageNode
     *            the page node
     * @return true, if is chunked
     * @throws RepositoryException
     *             the repository exception
     */
    public static boolean isChunked(final javax.jcr.Node pageNode) throws RepositoryException {
        if (null == pageNode) {
            return Boolean.FALSE;
        }
        return pageNode.hasProperty(DITAConstants.CHUNKED_TOPIC_PATHS);
    }

    /**
     * Gets the copyright text from tag.
     *
     * @param tagManager
     * @param copyrightReferences
     * @return List<String>
     */
    public static List<String> getCopyrightTextFromTag(final TagManager tagManager, final Value[] copyrightReferences,
            final Locale locale) {
        final List<String> copyrightText = new ArrayList<>();
        if (tagManager == null || copyrightReferences == null) {
            return copyrightText;
        }
        for (final Value copyrightReference : copyrightReferences) {
            try {
                final Tag copyrightTag = tagManager.resolve(copyrightReference.getString());
                if (null != copyrightTag) {
                    final Resource copyrightResource = copyrightTag.adaptTo(Resource.class);
                    if (copyrightResource != null) {
                        final ValueMap tagProps = copyrightResource.adaptTo(ValueMap.class);
                        if (null != tagProps) {
                            final String languageCode = locale.getLanguage();
                            final String tagTitle = getLocalizedTagTitle(tagProps, languageCode);
                            copyrightText.add(tagTitle);
                        }
                    }
                }
            } catch (final RepositoryException e) {
                LOGGER.error("Error in getting the copyright text from tag", e);
            }
        }
        return copyrightText;

    }

    /**
     * Gets the localized tag title.
     *
     * @param tagProps
     * @param languageCode
     * @return the localized
     */
    private static String getLocalizedTagTitle(final ValueMap tagProps, final String languageCode) {
        if (null != tagProps.get("jcr:title." + languageCode, String.class) && !languageCode.equals("en")) {
            return tagProps.get("jcr:title." + languageCode, String.class);
        } else {
            return tagProps.get("jcr:title", String.class);
        }

    }

    /**
     * Method to create/increment a revision/revision for the DITA/DITAMAP.
     *
     * @param path
     * @param label
     * @param comment
     * @param resource
     * @param session
     * @throws Exception
     */
    public static void createRevision(final String path, String label, final String comment, final Resource resource,
            final Session session) throws Exception {

        // Return if any of the mandatory arguments are null/empty
        if (null == resource || path.isEmpty() || null == session) {
            LOGGER.error("Error creating revision: resource/session/path is null");
            return;
        }
        if (label == null) {
            label = StringUtils.EMPTY;
        }
        final LockManager lockManager = session.getWorkspace().getLockManager();
        final Asset asset = resource.adaptTo(Asset.class);
        // Null check for the asset.
        if (null == asset) {
            LOGGER.error("Error creating revision: asset adaption is null");
            return;
        }

        Revision revision;
        if (lockManager.isLocked(path)) {
            lockManager.unlock(path);
            revision = asset.createRevision(label, comment);
            lockManager.lock(path, true, false, Long.MAX_VALUE, session.getUserID());
        } else {
            revision = asset.createRevision(label, comment);
        }
        final Version ver = revision.getVersion();
        final String verName = ver.getName();
        final String addedlabel = revision.getLabel();
        // Update the version history with the latest version label by removing the existing.
        if (label.isEmpty() && addedlabel.contains(verName)) {
            final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(path);
            versionHistory.removeVersionLabel(verName);
        }

        if (session.getNode(path + "/" + JcrConstants.JCR_CONTENT).hasProperty("fmdita-newfile")) {
            session.getNode(path + "/" + JcrConstants.JCR_CONTENT).getProperty("fmdita-newfile").remove();
        }
    }

    /**
     * Strip timezone and pick only date and formats it into PWC date format to avoid timezone issues.
     *
     * @param dateTimeString
     * @return formattedDateString
     */
    public static String formatDate(final String dateTimeString, final String outputFormat) {
        String formattedDateString = null;
        if (org.apache.commons.lang.StringUtils.isBlank(dateTimeString)
                && org.apache.commons.lang.StringUtils.isNotBlank(outputFormat)) {
            return formattedDateString;
        }
        final SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd");
        final SimpleDateFormat outputFormatter = new SimpleDateFormat(outputFormat);
        try {
            final String dateString = dateTimeString.substring(0, dateTimeString.indexOf("T"));
            final Date date = inputFormatter.parse(dateString);
            formattedDateString = outputFormatter.format(date);
        } catch (final Exception e) {
            LOGGER.error("Excpeiton while formatting date:", e);
        }
        return formattedDateString;
    }

    public static String getSourcePath(final String currentResourcePath, final String[] subscriberList,
            final ResourceResolver resolver, final String inputPath) {
        String sourcePath = StringUtils.EMPTY;
        if (subscriberList.length < 1 || StringUtils.isBlank(currentResourcePath) || null == resolver
                || StringUtils.isBlank(inputPath)) {
            return sourcePath;
        }
        /*
         * get source syndication content-path by replacing destination-path with source-path on current resource path
         */
        for (final String subscriber : subscriberList) {
            if (currentResourcePath.startsWith(subscriber)) {
                final String sourceResourcePath = currentResourcePath.replace(subscriber, inputPath);
                if (null != resolver.getResource(sourceResourcePath)) {
                    sourcePath = sourceResourcePath;
                    break;
                }
            }
        }
        return sourcePath;
    }

    /**
     * This method is get the all the pages for Referenced asset.
     *
     * @param assetPath
     * @param resourceResolver
     * @return pagePaths
     */
    public static List<String> getPagePathsFromDita(String assetPath, final ResourceResolver resourceResolver) {

        if (assetPath.contains(" ")) {
            assetPath = assetPath.trim().replaceAll(" ", "%20");
        }
        final List<String> referencesList = new ArrayList<>();

        if (StringUtils.isBlank(assetPath) || null == resourceResolver) {
            LOGGER.debug("Either resourceResolver is null or assetPath is empty");
            return referencesList;
        }

        final ReferenceSearch referenceSearch = new ReferenceSearch();
        final Map<String, ReferenceSearch.Info> references = referenceSearch.search(resourceResolver, assetPath);
        final String territoryCode = MadisonUtil.getTerritoryCodeForPath(assetPath);
        final String languageCode = MadisonUtil.getLanguageCodeForPath(assetPath);
        for (final Map.Entry<String, ReferenceSearch.Info> entry : references.entrySet()) {
            final ReferenceSearch.Info info = entry.getValue();
            /* excluding preview page references */
            final String pagePath = info.getPagePath();
            if (!pagePath.startsWith(MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH)) {
                final ValueMap valueMap = info.getPage().getProperties();
                if (valueMap.containsKey(DITAConstants.PN_SLING_RESOURCE_TYPE)
                        && DITAConstants.PWC_MADISON_COMPONENTS_STRUCTURE_PAGE_VP
                                .equalsIgnoreCase(valueMap.get(DITAConstants.PN_SLING_RESOURCE_TYPE, String.class))
                        && territoryCode.equalsIgnoreCase(MadisonUtil.getTerritoryCodeForPath(pagePath))
                        && languageCode.equalsIgnoreCase(MadisonUtil.getLanguageCodeForPath(pagePath))) {

                    if (valueMap.containsKey(DITAConstants.SOURCE_PATH)
                            && assetPath.equalsIgnoreCase(valueMap.get(DITAConstants.SOURCE_PATH, String.class))
                            || valueMap.containsKey(DITAConstants.PN_EFFECTIVE_SOURCE_PATH)
                                    && assetPath.equalsIgnoreCase(
                                            valueMap.get(DITAConstants.PN_EFFECTIVE_SOURCE_PATH, String.class))
                            || valueMap.containsKey(DITAConstants.CHUNKED_TOPIC_PATHS)
                                    && Arrays.asList(valueMap.get(DITAConstants.CHUNKED_TOPIC_PATHS, String[].class))
                                            .contains(assetPath)) {
                        referencesList.add(info.getPagePath());
                    }
                }
            }
        }
        return referencesList;
    }

    /**
     * Gets the topic node for chunked path.
     *
     * @param pageNode
     *            the page node
     * @param resolver
     *            the resolver
     * @return the chunked path
     * @throws RepositoryException
     *             the repository exception
     */
    public static javax.jcr.Node getTopicNode(final javax.jcr.Node topicBodyNode, final ResourceResolver resolver)
            throws RepositoryException {
        if (null != topicBodyNode) {
            if (topicBodyNode.hasNode(DITA_PWC_TOPIC_NODE)) {
                return topicBodyNode.getNode(DITA_PWC_TOPIC_NODE);
            } else if (topicBodyNode.hasNode(DITA_TOPIC_NODE)) {
                return topicBodyNode.getNode(DITA_TOPIC_NODE);
            } else {
                final long topicIterationStart = System.currentTimeMillis();
                final javax.jcr.Node topic = iterateToTopicNode(topicBodyNode);
                final long topicIterationEnd = System.currentTimeMillis();
                LOGGER.debug("Topic Iteration took {} ms", topicIterationEnd - topicIterationStart);
                return topic;
            }
        }
        return null;
    }

    private static javax.jcr.Node iterateToTopicNode(final javax.jcr.Node topicBodyNode) throws RepositoryException {
        if (null == topicBodyNode) {
            return null;
        }
        final NodeIterator childNodes = topicBodyNode.getNodes();
        javax.jcr.Node topicNode = null;
        while (childNodes.hasNext()) {
            final javax.jcr.Node child = childNodes.nextNode();
            if (child.hasProperty(DITAConstants.PN_SLING_RESOURCE_TYPE)
                    && child.getProperty(DITAConstants.PN_SLING_RESOURCE_TYPE).getValue().toString()
                            .equals(DITAConstants.DITA_TOPIC_RESOURCE_TYPE)) {
                return child;
            }
            topicNode = iterateToTopicNode(child);
            if (null != topicNode) {
                return topicNode;
            }
        }
        return null;
    }

    public static String isHidePublicationDate(final Resource page) {
        if (null != page) {
            final InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(page);
            return inheritanceValueMap.getInherited(DITAConstants.META_HIDE_PUBLICATION_DATE, MadisonConstants.NO);
        }
        return MadisonConstants.NO;
    }

    public static String getConditionalSectionTitle(final String path, final ResourceResolver resolver,
            final XSSAPI xssapi) {
        LOGGER.debug("TOC Path: {}", xssapi.encodeForHTML(path));
        String sectionTitle = StringUtils.EMPTY;
        if (StringUtils.isBlank(path)) {
            return sectionTitle;
        }
        final Resource pageResource = resolver.getResource(path);
        if (null == pageResource) {
            return sectionTitle;
        }
        final Page page = pageResource.adaptTo(Page.class);
        if (null == page) {
            return sectionTitle;
        }
        final ValueMap metaMap = page.getProperties();
        sectionTitle = getDITASectionTitle(page, resolver);
        if (StringUtils.isEmpty(sectionTitle)) {
            sectionTitle = metaMap.containsKey(DITAConstants.META_CONTENT_ID)
                    ? metaMap.get(DITAConstants.META_CONTENT_ID, String.class)
                    : StringUtils.EMPTY;
        }
        final String source = metaMap.containsKey(DITAConstants.META_STANDARD_SETTERS)
                ? metaMap.get(DITAConstants.META_STANDARD_SETTERS, String.class)
                : StringUtils.EMPTY;
        final String contentType = metaMap.containsKey(DITAConstants.META_CONTENT_TYPE)
                ? metaMap.get(DITAConstants.META_CONTENT_TYPE, String.class)
                : StringUtils.EMPTY;
        if (source.equals(DITAConstants.PWC_SOURCE_VALUE) || source.equals("")) {
            sectionTitle = sectionTitle.equals("") ? contentType : sectionTitle;
        } else {
            sectionTitle = sectionTitle.equals("") ? source : sectionTitle;
        }
        return sectionTitle;
    }

    /**
     * Check if SearchWithInDocument Icon is Visible.
     *
     * @param pagePath
     *            {@link String}
     * @param resolver
     *            {@resourceResolver resolver}
     * @return true/false {@link Boolean}
     * @throws RepositoryException
     */
    public static boolean isHideSearchWithInDocIcon(final String pagePath, final ResourceResolver resolver)
            throws RepositoryException {

        if (StringUtils.isBlank(pagePath) || null == resolver) {
            return true;
        }

        // Get access related meta-data properties for corresponding page
        final String basePath = MadisonUtil.getBasePath(pagePath, resolver);

        LOGGER.debug("Get all the DITA meta-data properties from corresponding page:: {}", pagePath);

        if (StringUtils.isNotBlank(basePath)) {

            final Resource parentPageResource = resolver.getResource(basePath.concat(DITAConstants.JCR_CONTENT));

            if (parentPageResource != null) {

                final ValueMap properties = parentPageResource.getValueMap();

                if (properties != null) {

                    // Search WithIn Doc Property exist only when isPublishingPoint Property is True
                    final String isPublishingPointProperty = properties
                            .containsKey(DITAConstants.PN_IS_PUBLISHING_POINTS)
                                    ? properties.get(DITAConstants.PN_IS_PUBLISHING_POINTS, String.class)
                                    : MadisonConstants.NO;

                    // True to hide Search WithIn Doc Icon and False to show Icon
                    // If Properties does not exists then true
                    final String hideSearchWithInDocProperty = MadisonConstants.YES.equals(isPublishingPointProperty)
                            && properties.containsKey(DITAConstants.META_HIDE_SEARCH_WITH_IN_DOC)
                                    ? properties.get(DITAConstants.META_HIDE_SEARCH_WITH_IN_DOC, String.class)
                                    : MadisonConstants.YES;

                    return MadisonConstants.YES.equals(hideSearchWithInDocProperty);

                }
            }
        }

        return true;
    }

    /**
     * Check to Show Static TOC.
     *
     * @param pagePath
     *            {@link String}
     * @param resolver
     *            {@resourceResolver resolver}
     * @return true/false {@link Boolean}
     * @throws RepositoryException
     */
    public static boolean showStaticToc(final String pagePath, final ResourceResolver resolver)
            throws RepositoryException {

        if (StringUtils.isBlank(pagePath) || null == resolver) {
            return true;
        }

        // Get access related meta-data properties for corresponding page
        final String basePath = MadisonUtil.getBasePath(pagePath, resolver);

        LOGGER.debug("Get all the DITA meta-data properties from corresponding page:: {}", pagePath);

        if (StringUtils.isNotBlank(basePath)) {

            final Resource parentPageResource = resolver.getResource(basePath.concat(DITAConstants.JCR_CONTENT));

            if (parentPageResource != null) {

                final ValueMap properties = parentPageResource.getValueMap();

                if (properties != null) {

                    // Apply Static TOC Property exist only when isPublishingPoint Property is True
                    final String isPublishingPointProperty = properties
                            .containsKey(DITAConstants.PN_IS_PUBLISHING_POINTS)
                            ? properties.get(DITAConstants.PN_IS_PUBLISHING_POINTS, String.class)
                            : MadisonConstants.NO;

                    // True to Show Static TOC and False to hide Static TOC
                    // If Properties does not exists then Static TOC will be hidden By default
                    final String showStaticTocProperty = MadisonConstants.YES.equals(isPublishingPointProperty)
                            && properties.containsKey(DITAConstants.META_SHOW_STATIC_TOC)
                            ? properties.get(DITAConstants.META_SHOW_STATIC_TOC, String.class)
                            : MadisonConstants.NO;

                    return MadisonConstants.YES.equals(showStaticTocProperty);

                }
            }
        }

        return false;
    }
    

    /**
     * Method to return the list of related-link resources under the given page.
     * 
     * @param currentPage
     *            {@link Page}
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @return {@link List} of {@link Resource}
     */
    public static List<Resource> getRelatedLinkResources(final Page currentPage,
            final ResourceResolver resourceResolver) {
        final List<Resource> linkResources = new ArrayList<>();
        final String rootPath = currentPage.getPath() + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1;
        final Resource resource = currentPage.adaptTo(Resource.class);
        if (resource == null) {
            return linkResources;
        }
        final String query = "SELECT * FROM [nt:unstructured] AS topic WHERE ISDESCENDANTNODE(topic , '" + rootPath
                + "') AND topic.[sling:resourceType] = '" + DITAConstants.COMPONENT_DITA_RELATED + "'";
        final Iterator<Resource> result = resourceResolver.findResources(query, javax.jcr.query.Query.JCR_SQL2);
        while (result.hasNext()) {
            final Resource linkResource = result.next();
            linkResources.add(linkResource);
        }
        return linkResources;
    }
    
    public static List<String> getTopicReferences(final String ditaPath, final ResourceResolver resolver, String lookupPath) {
        final List<String> topicsList = new ArrayList<>();
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("path", MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH);
        predicateMap.put("property", "jcr:content/@fmditaXrefs");
        predicateMap.put("property.value", "%" + ditaPath + "%");
        predicateMap.put("property.operation", "like");
        final QueryBuilder queryBuilder = resolver.adaptTo(QueryBuilder.class);

        if (null == queryBuilder) {
            return topicsList;
        }

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        while (resources.hasNext()) {
        	topicsList.add(resources.next().getPath());
        }

        return topicsList;

    }
    
    public static List<String> getPublishingPointDitaMapFromTopic(final String ditaPath, final ResourceResolver resolver, String lookupPath) {
        final List<String> topicsList = new ArrayList<>();
        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put("p.limit", "-1");
        predicateMap.put("type", "dam:Asset");
        predicateMap.put("path", MadisonConstants.PWC_MADISON_DITAROOT_DAM_PATH);
        predicateMap.put("property", "jcr:content/@fmditaXrefs");
        predicateMap.put("property.value", "%" + ditaPath + "%");
        predicateMap.put("property.operation", "like");
        final QueryBuilder queryBuilder = resolver.adaptTo(QueryBuilder.class);

        if (null == queryBuilder) {
            return topicsList;
        }

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap),
                resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        while (resources.hasNext()) {
        	topicsList.add(resources.next().getPath());
        }

        return topicsList;

    }
    
    /**
     * This method is get the all the pages for Referenced asset within the given territory.
     *
     * @param assetPath
     * @param resourceResolver
     * @param territoryCode 
     * @return pagePaths
     */
    public static List<String> getPagePathsFromDitaWithinTerritory(String assetPath, final ResourceResolver resourceResolver, String territoryCode) {

        if (assetPath.contains(" ")) {
            assetPath = assetPath.trim().replaceAll(" ", "%20");
        }
        final List<String> referencesList = new ArrayList<>();

        if (StringUtils.isBlank(assetPath) || null == resourceResolver) {
            LOGGER.debug("Either resourceResolver is null or assetPath is empty in method getPagePathsFromDitaWithinTerritory");
            return referencesList;
        }

        final ReferenceSearch referenceSearch = new ReferenceSearch();
        final Map<String, ReferenceSearch.Info> references = referenceSearch.search(resourceResolver, assetPath);
        for (final Map.Entry<String, ReferenceSearch.Info> entry : references.entrySet()) {
            final ReferenceSearch.Info info = entry.getValue();
            /* excluding preview page references */
            final String pagePath = info.getPagePath();
            if (!pagePath.startsWith(MadisonConstants.PWC_MADISON_PREVIEW_BASEPATH)) {
                final ValueMap valueMap = info.getPage().getProperties();
                if (valueMap.containsKey(DITAConstants.PN_SLING_RESOURCE_TYPE)
                        && DITAConstants.PWC_MADISON_COMPONENTS_STRUCTURE_PAGE_VP
                                .equalsIgnoreCase(valueMap.get(DITAConstants.PN_SLING_RESOURCE_TYPE, String.class))
                        && territoryCode.equalsIgnoreCase(MadisonUtil.getTerritoryCodeForPath(pagePath))) {

                    if (valueMap.containsKey(DITAConstants.SOURCE_PATH)
                            && assetPath.equalsIgnoreCase(valueMap.get(DITAConstants.SOURCE_PATH, String.class))
                            || valueMap.containsKey(DITAConstants.PN_EFFECTIVE_SOURCE_PATH)
                                    && assetPath.equalsIgnoreCase(
                                            valueMap.get(DITAConstants.PN_EFFECTIVE_SOURCE_PATH, String.class))
                            || valueMap.containsKey(DITAConstants.CHUNKED_TOPIC_PATHS)
                                    && Arrays.asList(valueMap.get(DITAConstants.CHUNKED_TOPIC_PATHS, String[].class))
                                            .contains(assetPath)) {
                        referencesList.add(info.getPagePath());
                    }
                }
            }
        }
        return referencesList;
    }

}
