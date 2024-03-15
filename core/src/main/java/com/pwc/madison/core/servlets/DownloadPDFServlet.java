package com.pwc.madison.core.servlets;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.i18n.I18n;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.impl.FootNoteImpl;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.models.UserProfile;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.pwc.madison.core.constants.MadisonConstants.TAG_TITLE_CURRENT_YEAR_PLACEHOLDER;

/**
 * Servlet class to fetch html content for PDF creation
 */
@Component(
    immediate = false,
    service = Servlet.class,
    property = { Constants.SERVICE_DESCRIPTION + "=PDF Download Servlet",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.selectors=dwnldpdf",
            "sling.servlet.resourceTypes=" + "cq:Page", "sling.servlet.extensions=" + "html" })
public class DownloadPDFServlet extends SlingAllMethodsServlet {

	@Reference
    private transient RequestResponseFactory requestResponseFactory;

    @Reference
    private transient UserRegRestService userregRestService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient ContentAuthorizationService contentAuthorizationService;

    @Reference
    private transient CryptoSupport cryptoSupport;

    @Reference
    private transient CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private transient UserPreferencesProviderService userPreferencesProviderService;

    /** Service to process requests through Sling */
    @Reference
    private transient SlingRequestProcessor requestProcessor;

    @Reference
    private transient SlingSettingsService slingSettingsService;
    
    @Reference
    private transient XSSAPI xssapi;

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadPDFServlet.class);
    private static final String RESOURCE_TYPE_FMDITA_TOPIC = "fmdita/components/dita/topic";
    private static final String NODE_RELATED_LINKS = "related-links";
    private static final String TYPE_INLINE_LINKS = "inline-links";
    private static final String TYPE_FAQ_LINKS = "frequently-asked-questions";
    private static final String TYPE_INSIGHT_LINKS = "industry-insights";
    private static final String TYPE_TEMPLATE_LINKS = "templates-links";
    private static final String TYPE_EXAMPLE_LINKS = "example-links";
    private static final String HTML_ROOT_DIV = "<div id='root'>";
    private static final String HTML_DIV_CLOSURE = "</div>";
    private static final String TOPIC_COPYRIGHT_DIV = "<div class='doc-body-copyright'>";
    private static final String FOOTER_COPYRIGHT_DIV = "<div class=\"copyright-protected\">";
    private static final String PRINT_DISCLAIMER_DIV = "<div style=\"border-top:solid;margin-top:10px;margin-bottom:5px\"></div>";
    private static final String I18N_KEY_RELATED_CONTENT = "PDF_Dwnld_Related_Content";
    private static final String I18N_KEY_SME = "PDF_Dwnld_SME";
    private static final String I18N_KEY_FAQ = "PDF_Dwnld_FAQ";
    private static final String I18N_KEY_TEMPLATES = "PDF_Dwnld_Templates";
    private static final String I18N_KEY_EXAMPLE = "PDF_Dwnld_Examples";
    private static final String I18N_KEY_INDUSTRY_INSIGHTS = "PDF_Dwnld_Industry_Insights";
    private static final String I18N_KEY_PRINT_DISCLAIMER = "printDisclaimer";
    private static final String RUN_MODE_AUTHOR = "author";
    private static final String LOG_SEPARATOR = "-------------------------------------------------------------------------------------";
    private static final String HTML_START_TAG = "<html>";
    private static final String HTML_END_TAG = "</html>";
    private static final String PROPERTY_SHOW_TOC = "showToc";
    private static final String TOPIC_FOOTNOTE_DIV = "<div class='footnotes'>";



    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        try {
            final long startTime = System.currentTimeMillis();
            final String downloadPDFPath = request.getResource().getPath();
            final ResourceBundle resourceBundle = request
                    .getResourceBundle(MadisonUtil.getLocaleObj(MadisonUtil.getLocaleForPath(downloadPDFPath)));
            final I18n i18n = new I18n(resourceBundle);
            final StringBuilder html = new StringBuilder();
            final ResourceResolver resolver = request.getResourceResolver();
            final List<String> selectorList = Arrays.asList(request.getRequestPathInfo().getSelectors());
            html.append(HTML_ROOT_DIV);
            final Resource baseResource = resolver.getResource(downloadPDFPath);
            if (baseResource == null) {
                sendErrorResponse(response);
                return;
            }
            final long getUserStart = System.currentTimeMillis();
            final User user = UserInformationUtil.getUser(request, true, userregRestService, cryptoSupport, response,
                    true, countryTerritoryMapperService, userPreferencesProviderService, false, true, xssapi);
            final long getUserEnd = System.currentTimeMillis();
            LOGGER.debug("getUser call took: {} ms", (getUserEnd - getUserStart));
            final Set<String> runMode = slingSettingsService.getRunModes();
            final boolean isAuthor = runMode.contains(RUN_MODE_AUTHOR);
            if (!isAuthor) {
                if (user.isUserLoggedIn()) {
                    final UserProfile userProfile = user.getUserProfile();
                    if (userProfile != null && !userProfile.getIsInternalUser()
                            && (selectorList.contains("i_n") || selectorList.contains("i_p"))) {
                        sendErrorResponse(response);
                        return;
                    }
                } else if (selectorList.contains("i_e_p") || selectorList.contains("i_e_l")) {
                    sendErrorResponse(response);
                    return;
                }
            }

            String pageContent = getPDFContentFromPage(baseResource.adaptTo(Node.class), request, response,
                    resolver, i18n, user, isAuthor);
            pageContent = pageContent.replace(TAG_TITLE_CURRENT_YEAR_PLACEHOLDER,MadisonUtil.getCurrentYear());
            html.append(pageContent);
            html.append(HTML_DIV_CLOSURE);
            html.append(PRINT_DISCLAIMER_DIV);
            html.append(FOOTER_COPYRIGHT_DIV);
            html.append("<p>" + i18n.get(I18N_KEY_PRINT_DISCLAIMER) + "</p>");
            html.append(HTML_DIV_CLOSURE);
            final long endTime = System.currentTimeMillis();
            LOGGER.debug("Download Servlet took {} seconds to complete the process", (endTime - startTime) / 1000);
            response.setContentType(ContentType.TEXT_HTML.getMimeType());
            response.getWriter().write(html.toString());
        } catch (ServletException | RepositoryException e) {
            LOGGER.error("Exception in doGet method", e);
            sendErrorResponse(response);

        }
    }

    /**
     * Method to return pdf content with title, body and related content from all pages and their children for a given
     * root page
     *
     * @param parentNode
     * @param request
     * @param resolver
     * @param i18n
     * @return String
     * @throws RepositoryException
     * @throws IOException
     * @throws ServletException
     */
    private String getPDFContentFromPage(final Node parentNode, final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final ResourceResolver resolver, final I18n i18n, final User user,
            final boolean isAuthor) throws RepositoryException, IOException, ServletException {
        Node childNode;
        final StringBuilder html = new StringBuilder();
        if (!parentNode.hasNodes()) {
            return html.toString();
        }
        final NodeIterator iter = parentNode.getNodes();
        if (iter.getSize() == 1 && parentNode.hasNode(DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1.substring(1))) {
            return html.append(getDetailsFromPageBody(resolver, request, response, parentNode, i18n, user, isAuthor))
                    .toString();
        }
        // Handle chunked content case
        final Resource pageResource = resolver.getResource(parentNode.getPath());
        final Page page = pageResource.adaptTo(Page.class);
        if(null != page && DITAUtils.isChunked(page.getContentResource().adaptTo(Node.class))) {
            return html.append(getDetailsFromPageBody(resolver, request, response, parentNode, i18n, user, isAuthor))
                    .toString();
        }
        while (iter.hasNext()) {
            childNode = iter.nextNode();
            if (!childNode.getPath().contains(DITAConstants.JOINED) && childNode.isNodeType(MadisonConstants.CQ_PAGE) && !isTocHiddenForPage(childNode,resolver)) {
                final long pageStart = System.currentTimeMillis();
                html.append(getDetailsFromPageBody(resolver, request, response, childNode, i18n, user, isAuthor));
                final long pageEnd = System.currentTimeMillis();
                LOGGER.debug("PAGE: {}  - TOOK {} ms", childNode.getPath(), (pageEnd - pageStart));
                LOGGER.debug(LOG_SEPARATOR);
            }
        }
        String htmlString = html.toString();
        /* remove inner HTML sections to avoid UI breaking while printing */
        if(htmlString.indexOf(HTML_START_TAG)>-1 && htmlString.indexOf(HTML_END_TAG)>-1){
            htmlString = htmlString.replaceAll("((?s)\\<html\\>.*?\\<\\/html\\>)", "");
        }
        return htmlString;
    }
    
    /**
     * Checks if is toc hidden for page.
     *
     * @param childNode the child node
     * @param resolver the resolver
     * @return true, if is toc hidden for page
     * @throws RepositoryException the repository exception
     */
    private boolean isTocHiddenForPage(final Node childNode,final ResourceResolver resolver) throws RepositoryException {
        boolean tocHidden = Boolean.FALSE;
        if(null != resolver && null != resolver.getResource(childNode.getPath())) {
            final Page page = resolver.getResource(childNode.getPath()).adaptTo(Page.class);
            if(null != page) {
                ValueMap properties = page.getProperties();
                tocHidden = properties.containsKey(PROPERTY_SHOW_TOC) && StringUtils.equalsIgnoreCase("no", properties.get(PROPERTY_SHOW_TOC, String.class));
                LOGGER.debug("ShowTOC for page {} is {}",page.getPath(),tocHidden);
            }
        }
        return tocHidden;
    }

    /**
     * Method to populate the PDF title, body and related content for different types of templates and return the html.
     *
     * @param resolver
     * @param request
     * @param childNode
     * @param i18n
     * @return String
     * @throws RepositoryException
     * @throws IOException
     * @throws ServletException
     */
    private String getDetailsFromPageBody(final ResourceResolver resolver, final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final Node childNode, final I18n i18n, final User user,
            final boolean isAuthor) throws RepositoryException, IOException, ServletException {
        Resource topicBodyResource;
        Node topicBodyNode;
        final Resource pageResource = resolver.getResource(childNode.getPath());
        final Page page = pageResource.adaptTo(Page.class);
        final StringBuilder detailsHtml = new StringBuilder();
        if (isAuthor || contentAuthorizationService.getUserAuthorization(page, user).isAuthorized()) {
            final String bodyPath = childNode.getPath() + DITAConstants.DITA_PWC_TOPIC_CONTAINER_V1;
            Node templateNode;
            topicBodyResource = resolver.getResource(bodyPath);
            if (topicBodyResource != null) {
                topicBodyNode = topicBodyResource.adaptTo(Node.class);
                final NodeIterator childNodes = topicBodyNode.getNodes();
                if (childNodes.hasNext()) {
                    templateNode = childNodes.nextNode();
                    /*
                     * Getting class property of a topic node to form a container div with that class enclosing the main
                     * body content
                     */
                    final String topicClass = getTopicClass(templateNode);
                   
                    //Handle chunking
                    if(DITAUtils.isChunked(page.getContentResource().adaptTo(Node.class))){
                        Node topicNode = DITAUtils.getTopicNode(topicBodyNode, resolver);
                        if(topicNode != null) {
                            fetchTitle(request, detailsHtml, topicNode);
                        }
                    }
                    else{
                        fetchTitle(request, detailsHtml, templateNode);
                    }
                    
                    final NodeIterator contentNodes = templateNode.getNodes();
                    Node detailNode;
                    while (contentNodes.hasNext()) {
                        detailNode = contentNodes.nextNode();
                        if (!NODE_RELATED_LINKS.equals(detailNode.getName())
                                && !DITAConstants.TITLE.equals(detailNode.getName())) {
                            detailsHtml.append("<div class=\"" + topicClass + "\">");
                            final long bodyStart = System.currentTimeMillis();
                            detailsHtml
                                    .append(getHtml(detailNode.getPath() + MadisonConstants.DWNLD_PDF_EXTN, request));
                            final long bodyEnd = System.currentTimeMillis();
                            LOGGER.debug("get HTML for BODY took {} ms", (bodyEnd - bodyStart));
                            detailsHtml.append(HTML_DIV_CLOSURE);
                        }
                    }
                    detailsHtml.append(getFootNotesContent(pageResource));
                    detailsHtml.append(getCopyRightContent(childNode, resolver));
                    detailsHtml.append(getCompleteRelatedContentHTML(childNode.getPath(), templateNode.getPath(),
                            request, resolver, i18n));
                }
            }
        }
        if (childNode.getNodes().getSize() > 1 && (null != page && !DITAUtils.isChunked(page.getContentResource().adaptTo(Node.class)))) {
            detailsHtml.append(getPDFContentFromPage(childNode, request, response, resolver, i18n, user, isAuthor));
        }
        return detailsHtml.toString();
    }

    private void fetchTitle(final SlingHttpServletRequest request, final StringBuilder detailsHtml, Node templateNode)
            throws IOException, ServletException, RepositoryException, PathNotFoundException {
        if(templateNode.hasNode(DITAConstants.TITLE)) {
            detailsHtml.append(
                    "<div class=\"ditadocumentheader aem-GridColumn aem-GridColumn--default--12\"><div id=\"madison-title-web\">");
            final long titleStart = System.currentTimeMillis();
            detailsHtml.append(getHtml(
                    templateNode.getNode(DITAConstants.TITLE).getPath() + MadisonConstants.DWNLD_PDF_EXTN,
                    request));
            final long titleEnd = System.currentTimeMillis();
            LOGGER.debug("get HTML for TITLE took {} ms", (titleEnd - titleStart));
            detailsHtml.append(HTML_DIV_CLOSURE);
            detailsHtml.append("<div class=\"spacer-border space-bar-top\"></div>");
            detailsHtml.append(HTML_DIV_CLOSURE);
        }
    }
    
    

    /**
     * Method to return the class property for the given topic node
     *
     * @param templateNode
     * @return String
     */
    private String getTopicClass(final Node templateNode) {
        String topicClass = "topic doc-body-content";
        try {
            if (templateNode.hasProperty(DITAConstants.PN_SLING_RESOURCE_TYPE)
                    && templateNode.hasProperty(DITAConstants.PN_CLASS_NAME)) {
                final String resourceType = templateNode.getProperty(DITAConstants.PN_SLING_RESOURCE_TYPE).getString();
                final String classValue = templateNode.getProperty(DITAConstants.PN_CLASS_NAME).getString();
                if (!RESOURCE_TYPE_FMDITA_TOPIC.equals(resourceType)) {
                    topicClass = classValue;
                }
            }
        } catch (final RepositoryException e) {
            LOGGER.error("Error in finding the topic class", e);
        }
        return topicClass;
    }

    /**
     * Method to return the copyright HTML for each topic by reading from tag
     *
     * @param pageNode
     * @param resolver
     * @return String
     */
    private String getCopyRightContent(final Node pageNode, final ResourceResolver resolver) {
        final long copyrightStart = System.currentTimeMillis();
        final StringBuilder copyrightContent = new StringBuilder();
        try {
            final Node jcrNode = pageNode.getNode(JcrConstants.JCR_CONTENT);
            if (jcrNode.hasProperty(DITAConstants.META_COPYRIGHT)) {
                final Value[] copyrightReferences = jcrNode.getProperty(DITAConstants.META_COPYRIGHT).getValues();
                final TagManager tagManager = resolver.adaptTo(TagManager.class);
                final List<String> copyrightTexts = DITAUtils.getCopyrightTextFromTag(tagManager, copyrightReferences,
                        MadisonUtil.getLocaleObj(MadisonUtil.getLocaleForPath(pageNode.getPath())));
                if (copyrightTexts.isEmpty()) {
                    return StringUtils.EMPTY;
                }
                copyrightContent.append(TOPIC_COPYRIGHT_DIV);
                for (final String text : copyrightTexts) {
                    copyrightContent.append("<p>" + text + "</p>");
                }
                copyrightContent.append(HTML_DIV_CLOSURE);

            }
        } catch (final RepositoryException e) {
            LOGGER.error("Error in getting copyright text from tag", e);
        }
        final long copyrightEnd = System.currentTimeMillis();
        LOGGER.debug("Copyright content took: {} ms", (copyrightEnd - copyrightStart));
        return copyrightContent.toString();
    }
    
    /**
     * Returns the footnote html.
     * @param pageResource {@link Resource} Resource of page
     * @return {@link String}
     */
    private String getFootNotesContent(final Resource pageResource) {
        final long footNoteStart = System.currentTimeMillis();
        final StringBuilder footNoteContent = new StringBuilder();
        Map<String, String> footNoteMap = new LinkedHashMap<>();
        FootNoteImpl.getFootNoteMapForPage(pageResource, footNoteMap);
        if (footNoteMap.size() > 0) {
            footNoteContent.append(TOPIC_FOOTNOTE_DIV);
            footNoteMap.values().forEach(v -> footNoteContent.append(v));
            footNoteContent.append(HTML_DIV_CLOSURE);
        } else {
            return StringUtils.EMPTY;
        }
        final long footNoteEnd = System.currentTimeMillis();
        LOGGER.debug("FootNote content took: {} ms", (footNoteEnd - footNoteStart));
        return footNoteContent.toString();
    }

    /**
     * Method to populate return a map containing with all types of related content
     *
     * @param templateNodePath
     * @param resolver
     * @return Map<String,List<String[]>>
     * @throws RepositoryException
     */
    private Map<String, List<String>> populateRelatedContentMap(final String templateNodePath,
            final ResourceResolver resolver, final SlingHttpServletRequest request) throws RepositoryException {
        final Map<String, List<String>> relatedLinksMap = new HashMap<>();
        final Resource relatedLinksResource = resolver
                .getResource(templateNodePath + DITAConstants.FORWARD_SLASH + NODE_RELATED_LINKS);
        if (relatedLinksResource != null) {
            final Node relatedLinksNode = relatedLinksResource.adaptTo(Node.class);
            final NodeIterator linkNodes = relatedLinksNode.getNodes();
            while (linkNodes.hasNext()) {
                final Node linkNode = linkNodes.nextNode();
                final String type = linkNode.hasProperty(DITAConstants.PROPERTY_TYPE)
                        ? linkNode.getProperty(DITAConstants.PROPERTY_TYPE).getString()
                        : StringUtils.EMPTY;
                switch (type) {
                    case TYPE_INLINE_LINKS:
                    case StringUtils.EMPTY:
                        relatedLinksMap.put(TYPE_INLINE_LINKS,
                                populateRelatedContentLinks(linkNode.getNodes(), request));
                        break;
                    case TYPE_FAQ_LINKS:
                        relatedLinksMap.put(TYPE_FAQ_LINKS, populateRelatedContentLinks(linkNode.getNodes(), request));
                        break;
                    case TYPE_INSIGHT_LINKS:
                        relatedLinksMap.put(TYPE_INSIGHT_LINKS,
                                populateRelatedContentLinks(linkNode.getNodes(), request));
                        break;
                    case TYPE_EXAMPLE_LINKS:
                        relatedLinksMap.put(TYPE_EXAMPLE_LINKS,
                                populateRelatedContentLinks(linkNode.getNodes(), request));
                        break;
                    case TYPE_TEMPLATE_LINKS:
                        relatedLinksMap.put(TYPE_TEMPLATE_LINKS,
                                populateRelatedContentLinks(linkNode.getNodes(), request));
                        break;
                    default:
                        break;
                }
            }
        }

        return relatedLinksMap;
    }

    /**
     * Method to return the HTML format of complete list of available related content
     *
     * @param childNodePath
     * @param resolver
     * @param i18n
     * @return String
     */
    private String getCompleteRelatedContentHTML(final String childNodePath, final String templateNodePath,
            final SlingHttpServletRequest request, final ResourceResolver resolver, final I18n i18n) {
        final long rclStart = System.currentTimeMillis();
        final StringBuilder html = new StringBuilder("<div class=\"linklist relatedlinks\">");
        try {
            final Map<String, List<String>> relatedContentMap = populateRelatedContentMap(templateNodePath, resolver,
                    request);
            if (relatedContentMap.containsKey(TYPE_INLINE_LINKS)) {
                html.append(populateRelatedContentHTML(i18n.get(I18N_KEY_RELATED_CONTENT),
                        relatedContentMap.get(TYPE_INLINE_LINKS)));
            }
            if (relatedContentMap.containsKey(TYPE_INSIGHT_LINKS)) {
                html.append(populateRelatedContentHTML(i18n.get(I18N_KEY_INDUSTRY_INSIGHTS),
                        relatedContentMap.get(TYPE_INSIGHT_LINKS)));
            }
            if (relatedContentMap.containsKey(TYPE_TEMPLATE_LINKS)) {
                html.append(populateRelatedContentHTML(i18n.get(I18N_KEY_TEMPLATES),
                        relatedContentMap.get(TYPE_TEMPLATE_LINKS)));
            }
            if (relatedContentMap.containsKey(TYPE_FAQ_LINKS)) {
                html.append(populateRelatedContentHTML(i18n.get(I18N_KEY_FAQ), relatedContentMap.get(TYPE_FAQ_LINKS)));
            }
            if (relatedContentMap.containsKey(TYPE_EXAMPLE_LINKS)) {
                html.append(populateRelatedContentHTML(i18n.get(I18N_KEY_EXAMPLE),
                        relatedContentMap.get(TYPE_EXAMPLE_LINKS)));
            }
            html.append(getSMEContentHTML(i18n.get(I18N_KEY_SME), childNodePath, resolver));
            html.append(HTML_DIV_CLOSURE);
        } catch (final RepositoryException e) {
            LOGGER.error("Exception in getting related content list", e);
        }
        final long rclEnd = System.currentTimeMillis();
        LOGGER.debug("get Related content took {} ms", (rclEnd - rclStart));
        return html.toString();
    }

    /**
     * Method to populate and return the related content title and anchor tag
     *
     * @param type
     * @param links
     * @return String
     */
    private String populateRelatedContentHTML(final String type, final List<String> links) {
        final StringBuilder innerHTML = new StringBuilder();
        if (!links.isEmpty()) {
            innerHTML.append("<h3 style=\"font-weight:bold\">" + type + "</h3><ul>");
            for (final String link : links) {
                innerHTML.append("<li>" + link + "</li>");
            }
            innerHTML.append("</ul>");
        }
        return innerHTML.toString();
    }

    /**
     * Method to populate and return the complete list of SME details in HTML format
     *
     * @param title
     * @param childNodePath
     * @param resolver
     * @return String
     */
    private String getSMEContentHTML(final String title, final String childNodePath, final ResourceResolver resolver) {
        final StringBuilder smeHTML = new StringBuilder();
        final Resource contentResource = resolver
                .getResource(childNodePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        if (contentResource != null) {
            final Node contentNode = contentResource.adaptTo(Node.class);
            try {
                if (contentNode.hasProperty(MadisonConstants.SME_PROPERTY)) {
                    smeHTML.append("<h3 style=\"font-weight:bold\">" + title + "</h3><ul>");
                    if (contentNode.getProperty(MadisonConstants.SME_PROPERTY).isMultiple()) {
                        final Value[] smePaths = contentNode.getProperty(MadisonConstants.SME_PROPERTY).getValues();
                        for (final Value smePage : smePaths) {
                            smeHTML.append(getSMEDetails(smePage.getString(), resolver));
                        }
                    } else {
                        smeHTML.append(getSMEDetails(contentNode.getProperty(MadisonConstants.SME_PROPERTY).getString(),
                                resolver));
                    }

                    smeHTML.append("</ul>");
                }
            } catch (IllegalStateException | RepositoryException e) {
                LOGGER.error("Error in forming SME HTML", e);
            }
        }
        return smeHTML.toString();
    }

    /**
     * Method to return the HTML format of SME details like name, email and phone
     *
     * @param pagePath
     * @param resolver
     * @return String
     */
    private String getSMEDetails(final String pagePath, final ResourceResolver resolver) {
        final StringBuilder smeDetails = new StringBuilder();
        final Resource contentResource = resolver
                .getResource(pagePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
        if (contentResource != null) {
            final Node contentNode = contentResource.adaptTo(Node.class);
            smeDetails.append("<li>");
            try {
                smeDetails.append(contentNode.hasProperty(DITAConstants.PROPERTY_FIRST_NAME)
                        ? contentNode.getProperty(DITAConstants.PROPERTY_FIRST_NAME).getString() + " "
                        : StringUtils.EMPTY);

                smeDetails
                        .append(contentNode.hasProperty(DITAConstants.PROPERTY_LAST_NAME)
                                ? contentNode.getProperty(DITAConstants.PROPERTY_LAST_NAME).getString()
                                        + MadisonConstants.COMMA_SEPARATOR
                                : StringUtils.EMPTY);
                smeDetails
                        .append(contentNode.hasProperty(DITAConstants.PROPERTY_EMAIL)
                                ? contentNode.getProperty(DITAConstants.PROPERTY_EMAIL).getString()
                                        + MadisonConstants.COMMA_SEPARATOR
                                : StringUtils.EMPTY);
                smeDetails.append(contentNode.hasProperty(DITAConstants.PROPERTY_PHONE)
                        ? contentNode.getProperty(DITAConstants.PROPERTY_PHONE).getString()
                        : StringUtils.EMPTY);
            } catch (final RepositoryException e) {
                LOGGER.error("Error in getting SME properties", e);
            }
            smeDetails.append("</li>");

        }
        return smeDetails.toString();

    }

    /**
     * Method to populate and return the list of related content links
     *
     * @param iter
     * @return List<String[]>
     * @throws RepositoryException
     */
    private List<String> populateRelatedContentLinks(final NodeIterator iter, final SlingHttpServletRequest request)
            throws RepositoryException {
        final List<String> relatedContentList = new ArrayList<>();
        Node detailNode;
        while (iter.hasNext()) {
            detailNode = iter.nextNode();
            if (detailNode.hasProperty(MadisonConstants.NODE_PROP_TEXT)) {
                try {
                    relatedContentList.add(getHtml(detailNode.getPath() + MadisonConstants.DWNLD_PDF_EXTN, request));
                } catch (final IOException | ServletException e) {
                    LOGGER.error("Error in getting related content HTML", e);
                }
            }
        }
        return relatedContentList;
    }

    /**
     * Method to form the HTML source of the node path passed
     *
     * @param nodePath
     * @param request
     * @return String
     * @throws IOException
     * @throws ServletException
     */
    private String getHtml(final String nodePath, final SlingHttpServletRequest request)
            throws IOException, ServletException {
        final HttpServletRequest req = requestResponseFactory.createRequest("GET", nodePath);
        WCMMode.DISABLED.toRequest(req);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final HttpServletResponse resp = requestResponseFactory.createResponse(out);
        requestProcessor.processRequest(req, resp, request.getResourceResolver());
        return out.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * Method to set 500 error code in the response status and write error response.
     *
     * @param response
     * @throws IOException
     */
    private void sendErrorResponse(final SlingHttpServletResponse response) throws IOException {
        response.setContentType(ContentType.TEXT_HTML.getMimeType());
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Error in generating the PDF");
    }

}
