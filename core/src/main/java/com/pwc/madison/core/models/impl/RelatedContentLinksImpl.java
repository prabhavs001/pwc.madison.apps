package com.pwc.madison.core.models.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.authorization.models.AuthorizationInformation;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.RelatedContent;
import com.pwc.madison.core.models.RelatedContentLinks;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Sling model for populating related content.
 *
 * @author vhs
 *
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = RelatedContentLinks.class,
    resourceType = RelatedContentLinksImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class RelatedContentLinksImpl implements RelatedContentLinks {

    private static final String EXTENSION_HTML = ".html";

    private static final Logger log = LoggerFactory.getLogger(RelatedContentLinksImpl.class);

    private static final String PN_TEXT = "text";
    private static final String UNDERSCORE_TEXT_NODE_NAME = "_text";
    private static final String INDUSTRY_INSIGHTS = "industry-insights";
    private static final String NODE_TYPE_FAQ = "pwc-faq";
    private static final String NODE_TYPE_TOPIC = "pwc-topic";
    private static final String NODE_TYPE_EXAMPLE = "pwc-example";
    private static final String CONTENT_TYPE_FAQ = "faq";
    private static final String CONTENT_TYPE_EXAMPLE = "example-links";
    private static final String CONTENT_TYPE_INDUSTRY_INSIGHTS_LINKS = "iil";
    private static final String FREQUENTLY_ASKED_QUESTIONS = "frequently-asked-questions";
    private static final String TEMPLATES = "templates-links";
    private static final String RELATED_CONTENT = "inline-links";
    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/related-content-links";
    private static final String CHUNKED_CONTENT_TOPIC_PATH = "/dita/topic";
    private static final String CHUNKED_CONTENT_TOPIC_NODE = "topic";

    @Inject
    SlingSettingsService slingSettingsService;

    @Inject
    SlingHttpServletRequest request;

    @Inject
    SlingHttpServletResponse response;

    @OSGiService(injectionStrategy = InjectionStrategy.OPTIONAL)
    private ContentAuthorizationService contentAuthorizationService;

    @ValueMapValue
    private String itemType;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @SlingObject
    private ResourceResolver resourceResolver;

    List<RelatedContent> relatedContents = null;

    List<String> relatedLinkPaths = new ArrayList<>();

    /**
     * Init Method of Model.
     *
     * @throws ParseException
     */
    @PostConstruct
    protected void init() throws RepositoryException {
        log.debug("Inside IndustryInsightsModelImpl :: {}", getItemType());

        final Session session = resourceResolver.adaptTo(Session.class);
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final List<String> relatedContentNodes = getRelatedContentNodes(session);
        if (null != session && !relatedContentNodes.isEmpty()) {
            relatedContents = new ArrayList<>();
            for (final String relatedNodePath : relatedContentNodes) {
                final Node linkNode = session.getNode(relatedNodePath);
                final RelatedContent relatedContent = getRelatedContentLink(linkNode, session, pageManager);
                if (null != relatedContent) {
                    relatedContents.add(relatedContent);
                }
            }
        }

    }

    /**
     * Method to fetch the title, body and related dita country and populate.
     *
     * @param linkNode
     * @param session
     * @param pageManager
     * @return
     * @throws RepositoryException
     * @throws ParseException
     */
    private RelatedContent getRelatedContentLink(final Node linkNode, final Session session,
            final PageManager pageManager) throws RepositoryException {

        RelatedContent relatedContent = null;
        if (linkNode.hasProperty(DITAConstants.PROPERTY_LINK)) {
            String link = linkNode.getProperty(DITAConstants.PROPERTY_LINK).getString();
            String refGeneratedPagePath = null;
            String fmGuid = null;
            /*
             * Case Scope = Peer , If the DITA-OT could not generate the page for the related DITA, then following code
             * will get the generated page from the cross reference specialization.
             */
            if ((StringUtils.isBlank(link) || link.contains(DITAConstants.DITA_EXTENSION))
                    && linkNode.hasProperty(DITAConstants.FMGUID) && null != resourceResolver) {
                fmGuid = linkNode.getProperty(DITAConstants.FMGUID).getString();
                log.debug("fmguid {}", fmGuid);
                if (StringUtils.isNotEmpty(fmGuid)) {
                    refGeneratedPagePath = DITALinkUtils.getPageFromXrefDita(resourceResolver, fmGuid);
                    if (StringUtils.isBlank(refGeneratedPagePath)) {
                        return relatedContent;
                    } else {
                        // set related Content page path to refGeneratedPagePath
                        if (refGeneratedPagePath.contains(EXTENSION_HTML)) {
                            refGeneratedPagePath = refGeneratedPagePath.replaceAll(EXTENSION_HTML, "");
                            link = refGeneratedPagePath;
                            log.debug("Set related Content page path to refGeneratedPagePath" + link);
                        }
                    }
                }
            }

            if (relatedLinkPaths.contains(link)) {
                return relatedContent;
            }
            relatedLinkPaths.add(link);

            Node titleNode = null;
            Node bodyNode = null;
            Node additionalQuestionNode = null;
            try {
                /*
                 * This is Added for Content authorization of sidebar links.
                 */

                /* Removing #value in the link url */
                if (link.contains(DITAConstants.HASH_STR)) {
                    link = link.substring(0, link.indexOf(DITAConstants.HASH_STR));
                }
                final AuthorizationInformation pageAuthorizationInformation;
                final Page page = pageManager.getPage(link);
                if (page == null) {
                    return relatedContent;
                }

                /* Handling chunked content with defined structure */
                if (session.nodeExists(link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + CHUNKED_CONTENT_TOPIC_PATH)) {
                    bodyNode = session
                            .getNode(link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + CHUNKED_CONTENT_TOPIC_PATH);
                    titleNode = bodyNode.getNode(DITAConstants.TITLE);

                } else {
                    final Node topicBody = session.getNode(link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1);
                    // Nodes for Industry Insights
                    if (CONTENT_TYPE_FAQ.equals(itemType) && null != topicBody && topicBody.hasNode(NODE_TYPE_FAQ)) {
                        // Nodes for FAQ
                        titleNode = session.getNode(
                                link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + DITAConstants.FAQ_QUESTION_TEXT);
                        bodyNode = session.getNode(
                                link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + DITAConstants.FAQ_ANSWER_TEXT);
                        additionalQuestionNode = session.getNode(link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1
                                + DITAConstants.FAQ_ADDITIONAL_QUESTION_TEXT);
                    } else if ((TEMPLATES.equals(itemType) || RELATED_CONTENT.equals(itemType)
                            || CONTENT_TYPE_INDUSTRY_INSIGHTS_LINKS.equals(itemType)) && null != topicBody
                            && topicBody.hasNode(NODE_TYPE_TOPIC)) {
                        // Nodes for TEMPLATES
                        titleNode = session.getNode(
                                link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + DITAConstants.PWC_TOPIC_TITLE);
                        bodyNode = session.getNode(
                                link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + DITAConstants.PWC_TOPIC_BODY);
                    } else if (CONTENT_TYPE_EXAMPLE.equals(itemType) && null != topicBody
                            && topicBody.hasNode(NODE_TYPE_EXAMPLE)) {
                        titleNode = session.getNode(
                                link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + DITAConstants.PWC_EXAMPLE_TITLE);
                        bodyNode = session.getNode(
                                link + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1 + DITAConstants.PWC_EXAMPLE_BODY);
                    } else { // When the node hierarchy does not match any of the above cases. Eg: FASB,AICPA, CTC
                        bodyNode = topicBody;
                        titleNode = getTitleNode(bodyNode);
                    }
                }
                if (null != titleNode && null != bodyNode) {
                    relatedContent = populateRelatedContent(session, link, titleNode, bodyNode, additionalQuestionNode);
                }
            } catch (final PathNotFoundException e) {
                log.error("Error in getRelatedContent() method {}", e);
            }
        }
        return relatedContent;

    }

    /**
     * Method to find the title node under the currentpage's topicbody container
     *
     * @param bodyNode
     * @return Node
     */
    private Node getTitleNode(final Node bodyNode) {
        if (bodyNode != null) {
            try {
                if (bodyNode.hasNode(DITAConstants.TITLE)) {
                    return bodyNode.getNode(DITAConstants.TITLE);
                } else {
                    final NodeIterator childNodes = bodyNode.getNodes();
                    while (childNodes.hasNext()) {
                        return getTitleNode(childNodes.nextNode());
                    }
                }
            } catch (final RepositoryException e) {
                log.error("Error in finding the title node under current page", e);
            }
        }
        return null;
    }

    private RelatedContent populateRelatedContent(final Session session, final String link, final Node titleNode,
            final Node bodyNode, final Node additionalQuestionNode) throws RepositoryException {
        // Get all the DITA meta-data properties from corresponding page
        // properties.
        final boolean ditaInternal = DITAUtils.isDitaTopicInternal(session, link);
        final Map<String, String> metaMap = DITAUtils.getPageMetaDataProperties(session, link);

        String ditaTitleVal = StringUtils.EMPTY;

        if (null != titleNode && titleNode.hasNode(UNDERSCORE_TEXT_NODE_NAME)
                && titleNode.getNode(UNDERSCORE_TEXT_NODE_NAME).hasProperty(PN_TEXT)) {
            ditaTitleVal = titleNode.getNode(UNDERSCORE_TEXT_NODE_NAME).getProperty(PN_TEXT).getString();
        }

        // Populate Country / Territory code
        final String ditaCountry = MadisonUtil.getTerritoryCodeForPath(link);
        final String ditaContentId = metaMap.get(DITAConstants.META_CONTENT_ID);
        final String ditaContentType = metaMap.get(DITAConstants.META_CONTENT_TYPE);
        final String ditaIssuingBody = metaMap.get(DITAConstants.META_STANDARD_SETTERS);
        final String revisionDate = metaMap.get(DITAConstants.META_REVISION_DATE);
        final String publicationDate = metaMap.get(DITAConstants.META_PUBLICATION_DATE);
        // Set publication date if revision date is not present.
        final String date = StringUtils.isNotBlank(revisionDate) ? revisionDate : publicationDate;
        final String dateFormat = countryTerritoryMapperService.getTerritoryByTerritoryCode(ditaCountry)
                .getDateFormat();
        final String ditaPublicationDate = DITAUtils.formatDate(date, dateFormat);

        final String ditaTitle = getRelatedContent(titleNode);
        final String ditaBody = getRelatedContent(bodyNode);
        final ArrayList<String> additionalQuestions = getAdditionalQuestions(additionalQuestionNode);

        final Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
        final String ditaPagePath = null != externalizer
                ? externalizer.externalLink(resourceResolver, MadisonUtil.getCurrentRunmode(slingSettingsService), link)
                        + MadisonConstants.HTML_EXTN
                : StringUtils.EMPTY;

        // Get Content Visibility;
        final boolean isShareViaEmailOnly = DITAUtils.isShareWithMail(link, resourceResolver);
        Resource pageContentResource = resourceResolver.getResource(link + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        final boolean hidePublicationDate = DITAUtils.isHidePublicationDate(pageContentResource).equals(MadisonConstants.YES) ? true : false;
        return new RelatedContent(ditaTitle, resourceResolver.map(ditaBody), ditaInternal, ditaCountry, ditaTitleVal,
                ditaPagePath, ditaContentId, ditaContentType, ditaPublicationDate, isShareViaEmailOnly, ditaIssuingBody,
                additionalQuestions, link, hidePublicationDate);
    }

    private String getRelatedContent(final Node node) throws RepositoryException {

        String relatedContent = StringUtils.EMPTY;
        if (null == node) {
            return relatedContent;
        }
        // Get title node from the page.
        if (node.getName().startsWith(DITAConstants.TITLE) || node.getName().startsWith(DITAConstants.PWC_BODY)
                || node.getName().startsWith(DITAConstants.QUESTION_TEXT)
                || node.getName().startsWith(DITAConstants.ANSWER_TEXT)
                || node.getName().startsWith(DITAConstants.EXAMPLE_BODY)
                || node.getName().startsWith(CHUNKED_CONTENT_TOPIC_NODE)) {
            relatedContent = node.getPath();
        }
        return relatedContent;
    }

    private ArrayList<String> getAdditionalQuestions(final Node additionalQuestionNode) {
        final ArrayList additionalQuestions = new ArrayList<>();
        if (null == additionalQuestionNode) {
            return additionalQuestions;
        }

        /* Get additional question nodes */
        try {
            if (additionalQuestionNode.getName().startsWith(DITAConstants.QUESTION_TEXT)) {
                final Iterator<Node> children = additionalQuestionNode.getNodes();
                while (children.hasNext()) {
                    final Node child = children.next();
                    if (child.hasNode(UNDERSCORE_TEXT_NODE_NAME)) {
                        final String additionalQuestionPath = child.getPath();
                        additionalQuestions.add(additionalQuestionPath);
                    }
                }
            }
        } catch (final RepositoryException e) {
            log.error("Error while fetching additional questions node - RelatedContentLinksImpl", e);
        }
        return additionalQuestions;
    }

    /**
     * Method to get the industry insights nodes from related links.
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    private List<String> getRelatedContentNodes(final Session session) throws RepositoryException {
        final List<String> relatedContentNodes = new ArrayList<>();
        final List<Resource> linkResources = DITAUtils.getRelatedLinkResources(currentPage, resourceResolver);
        for (final Resource linkResource : linkResources) {
            final Iterable<Resource> linklist = linkResource.getChildren();
            for (final Resource link : linklist) {
                if (link.isResourceType(DITAConstants.COMPONENT_DITA_LINKLIST)) {
                    final String relatedContentType = getRelatedContentType(session, link.getPath());
                    if (itemType.equals(relatedContentType)) {
                        relatedContentNodes.addAll(getChildPageLink(link));
                    }
                }
            }
        }
        return relatedContentNodes;
    }

    /**
     * Check if the related-links has 'linklist' node that has the property 'type' matching industry insights.
     *
     * @param session
     * @param linkPath
     * @return
     * @throws RepositoryException
     */
    private String getRelatedContentType(final Session session, final String linkPath) throws RepositoryException {
        String relatedContentType = StringUtils.EMPTY;
        final Node linkNode = session.getNode(linkPath);
        if (null != linkNode && linkNode.hasProperty(DITAConstants.PROPERTY_TYPE)) {
            final String type = linkNode.getProperty(DITAConstants.PROPERTY_TYPE).getString();
            // Check if the xref link of type industry insights.
            if (INDUSTRY_INSIGHTS.equals(type)) {
                relatedContentType = CONTENT_TYPE_INDUSTRY_INSIGHTS_LINKS;
            } else if (FREQUENTLY_ASKED_QUESTIONS.equals(type)) {
                relatedContentType = CONTENT_TYPE_FAQ;
            } else if (TEMPLATES.equals(type)) {
                relatedContentType = TEMPLATES;
            } else if (RELATED_CONTENT.equals(type)) {
                relatedContentType = RELATED_CONTENT;
            } else if (CONTENT_TYPE_EXAMPLE.equals(type)) {
                relatedContentType = CONTENT_TYPE_EXAMPLE;
            }
        }
        return relatedContentType;
    }

    /**
     * Method to get the all the links under linklist node.
     *
     * @param childResource
     * @return
     */
    private List<String> getChildPageLink(final Resource childResource) {
        final List<String> links = new ArrayList<>();
        final Iterator<Resource> iterator = childResource.listChildren();
        while (iterator.hasNext()) {
            final Resource child = iterator.next();
            if (null != child && child.isResourceType(DITAConstants.COMPONENT_DITA_TYPE)) {
                links.add(child.getPath());
            }
        }
        return links;
    }

    @Override
    public List<RelatedContent> getRelatedContents() {
        return relatedContents;
    }

    @Override
    public String getItemType() {
        return itemType;
    }

    @Override
    public String getPwcSourceValue() {
        return DITAConstants.PWC_SOURCE_VALUE;
    }

}
