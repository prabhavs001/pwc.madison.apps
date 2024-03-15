package com.pwc.madison.core.services.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.engine.SlingRequestProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagConstants;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.authorization.models.ContentAuthorization;
import com.pwc.madison.core.beans.SitemapParent;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;
import com.pwc.madison.core.services.RecordsGenerationService;
import com.pwc.madison.core.services.SitemapParentPagesProviderService;
import com.pwc.madison.core.services.impl.RecordsGenerationServiceImpl.RecordsXmlRootPathConfig;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SitemapHelper;

@Component(
        service = RecordsGenerationService.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
@Designate(ocd = RecordsXmlRootPathConfig.class)
public class RecordsGenerationServiceImpl implements RecordsGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsGenerationServiceImpl.class);

    private static final String SITEMAP_TAG_RECORDS = "records";
    private static final String SITEMAP_TAG_RECORD = "record";
    private static final String HTML_MAIN_CLASS = "main";
    private static final String PWC_BODY = "body";
    private static final String PWC_URL = "url";
    private static final String PWC_TITLE = "title";
    private static final String PWC_DESCRIPTION = "desc";
    private static final String PWC_KEYWORDS = "keys";
    private static final String PWC_COUNTRY = "pwcCountry";
    private static final String PWC_LANGUAGE = "pwcLang";
    private static final String PWC_IND = "pwcInd";
    private static final String PWC_TAG = "pwcTag";
    private static final String PWC_RELEASE_DATE = "pwcReleaseDate";
    private static final String PWC_LOCALE = "pwcLocale";
    private static final String TAG_SEPERATOR = "||,";
    private static final String COMMA_SEPERATOR = ",";

    private String[] configuredRootPathPatterns;
    private String[] searchTags;
    private Map<String, List<SitemapParent>> rootPathsMap;
    private String[] removableHtmlClasses;
    private String[] industryTagPaths;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private RequestResponseFactory requestResponseFactory;

    @Reference
    private Replicator replicator;

    @Reference
    private MadisonDomainsService domainService;

    @Reference
    private SlingRequestProcessor requestProcessor;
    
    @Reference
    private SitemapParentPagesProviderService sitemapParentPagesProviderService;

    @Reference
    private QueryBuilder queryBuilder;
    
    @Reference
    private MadisonSystemUserNameProviderService madisonSystemUserNameProviderService;

    @Activate
    @Modified
    protected void Activate(final RecordsXmlRootPathConfig recordsXmlRootPathConfig) {
        LOGGER.info("RecordsGenerationServiceImpl : Entered Activate/Modify");
        configuredRootPathPatterns = recordsXmlRootPathConfig.madison_records_root_paths();
        searchTags = recordsXmlRootPathConfig.madison_records_search_tags();
        removableHtmlClasses = recordsXmlRootPathConfig.madison_removable_html_classes();
        industryTagPaths = recordsXmlRootPathConfig.madison_industry_tag_paths();
        LOGGER.debug("RecordsGenerationServiceImpl Activate() Records XML Root Path Patterns : {}",
                Arrays.asList(configuredRootPathPatterns));
        LOGGER.debug("RecordsGenerationServiceImpl Activate() Records XML Search Tags : {}", Arrays.asList(searchTags));
        LOGGER.debug("RecordsGenerationServiceImpl Activate() Records XML Removable HTML classes from body : {}",
                Arrays.asList(removableHtmlClasses));
        LOGGER.debug("RecordsGenerationServiceImpl Activate() Records XML Industry Tag Paths : {}",
                Arrays.asList(industryTagPaths));
        createParentPagesMap();
    }

    /**
     * Creates pages {@link Map} where key represents the root path where the records XML file is to be created and
     * value represents {@link List} of {@link SitemapParent} i.e list of paths for adding pages to records XML of the
     * key root path.
     */
    private void createParentPagesMap() {
        rootPathsMap = new HashMap<String, List<SitemapParent>>();
        if (null != configuredRootPathPatterns && configuredRootPathPatterns.length > 0) {
            for (final String configuredRootPathPattern : configuredRootPathPatterns) {
                String[] splittedConfiguredString = configuredRootPathPattern.split(":|,");
                rootPathsMap.put(splittedConfiguredString[0], new ArrayList<SitemapParent>());
                for (int index = 1; index < splittedConfiguredString.length; index++) {
                    rootPathsMap.get(splittedConfiguredString[0]).add(new SitemapParent(splittedConfiguredString[index],
                            !splittedConfiguredString[index].contains(MadisonConstants.DITAROOT_TEXT)));
                }
            }
        }
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Records XML Paths Configuration")
    public @interface RecordsXmlRootPathConfig {

        @AttributeDefinition(
                name = "Records XML Root Paths",
                description = "Root path i.e the path where the records file is to be created along with all the paths from where the pages needs to be added to the records XML file. For ex: /content/pwc-madison/us/en:/content/pwc:madison/us/en/path1,/content/pwc:madison/us/en/path2. Each entry in the configuration should be in the format <root_path>:<path1>,<path2>,<path3>",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_records_root_paths();

        @AttributeDefinition(
                name = "Records XML Search Tags",
                description = "List of tags IDs with which pages which are to be added in records XML should be tagged.",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_records_search_tags();

        @AttributeDefinition(
                name = "Records XML Removable HTML classes",
                description = "List of classes to be removed from pages body HTML",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_removable_html_classes();

        @AttributeDefinition(
                name = "Records XML Industry Tag Paths",
                description = "List of tag paths for industry tags",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_industry_tag_paths();

    }

    @Override
    public void generateRecordsXml(final String recordsFileName, final PrintWriter printWriter) {
        if (rootPathsMap.size() > 0) {
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = MadisonUtil.getResourceResolver(resolverFactory,
                        MadisonConstants.MADISON_CONTENT_ADMIN_SUB_SERVICE);
                if (resourceResolver != null) {
                    final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
                    final Session session = resourceResolver.adaptTo(Session.class);
                    final ResourceResolver pageExtractionResourceResolver = MadisonUtil
                            .getResourceResolver(resolverFactory, madisonSystemUserNameProviderService.getFmditaServiceUsername());
                    TagManager tagManager = null;
                    if (null == pageExtractionResourceResolver) {
                        LOGGER.warn(
                                "RecordsGenerationServiceImpl generateRecordsXml() : Getting Resource Resolver {} as null",
                                madisonSystemUserNameProviderService.getFmditaServiceUsername());
                    } else {
                        tagManager = pageExtractionResourceResolver.adaptTo(TagManager.class);
                    }
                    for (final String rootPath : rootPathsMap.keySet()) {
                        startRecordsXmlCreation(rootPath, rootPathsMap.get(rootPath), recordsFileName, printWriter,
                                resourceResolver, session, pageExtractionResourceResolver, tagManager, pageManager);
                    }
                } else {
                    LOGGER.warn(
                            "RecordsGenerationServiceImpl generateRecordsXml() : Getting Resource Resolver {} as null",
                            MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
                }
            } catch (final Exception exception) {
                LOGGER.error(
                        "RecordsGenerationServiceImpl generateRecordsXml() : Exception occurred while creating records xml : {} ",
                        exception);
                if (null != printWriter) {
                    printWriter.println("Exception occurred while creating records xml");
                }
                exception.printStackTrace();
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        } else {
            LOGGER.info(
                    "RecordsGenerationServiceImpl generateRecordsXml() : Records XML is not generated as there are no configured paths.");
            if (null != printWriter) {
                printWriter.println("Records XML is not generated as there are no configured paths.");
            }
        }
    }

    /**
     * Start the creation of records XML file on given root path.
     * 
     * @param rootPath
     *            {@link String} path where the records XML file is created
     * @param pagesPaths
     *            {@link List} of {@link SitemapParent} for which the records XML creation is to be started
     * @param recordsFileName
     *            {@link String} name of the records XML file
     * @param printWriter
     *            {@link PrintWriter} if not null, necessary log info will also be written to ot
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param session
     *            {@link Session}
     * @param pageExtractionResourceResolver
     *            {@link ResourceResolver} of the system user with jcr:read access to root
     * @param tagManager
     *            {@link TagManager}
     * @param pageManager
     *            {@link PageManager}
     */

    void startRecordsXmlCreation(final String rootPath, final List<SitemapParent> pagesPaths,
            final String recordsFileName, final PrintWriter printWriter, final ResourceResolver resourceResolver,
            final Session session, final ResourceResolver pageExtractionResourceResolver, final TagManager tagManager,
            final PageManager pageManager) {
        if (resourceResolver.getResource(rootPath) != null) {
            final String filePath = rootPath + MadisonConstants.FORWARD_SLASH + recordsFileName;
            try {
                long startTime = System.currentTimeMillis();
                LOGGER.info(
                        "RecordsGenerationServiceImpl startRecordsXmlCreation() : Records XML creation starting for root path : {}",
                        rootPath);
                XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XMLStreamWriter stream = outputFactory.createXMLStreamWriter(bos, MadisonConstants.UTF_8);
                stream.writeStartDocument("1.0");
                stream.writeStartElement(SITEMAP_TAG_RECORDS);
                int numberOfPagesAdded = createRecordsXml(pageExtractionResourceResolver, session, stream, pagesPaths,
                        tagManager, pageManager);
                stream.writeEndElement();
                stream.writeEndDocument();
                ValueFactory vf = session.getValueFactory();
                Binary binary = vf.createBinary(new ByteArrayInputStream(bos.toByteArray()));
                SitemapHelper.saveFile(resourceResolver, rootPath, recordsFileName, session, binary, replicator, sitemapParentPagesProviderService.getReplicationAgents());
                LOGGER.info(
                        "RecordsGenerationServiceImpl startRecordsXmlCreation() : Number of pages added for root path : {} : {}",
                        rootPath, numberOfPagesAdded);
                if (null != printWriter) {
                    printWriter.println("Number of pages added for root path " + rootPath + " : " + numberOfPagesAdded);
                }
                LOGGER.info(
                        "RecordsGenerationServiceImpl startRecordsXmlCreation() : Records XML creation completed for root path : {} in seconds {}",
                        rootPath, (System.currentTimeMillis() - startTime) / 1000);
            } catch (XMLStreamException XMLStreamException) {
                LOGGER.error(
                        "RecordsGenerationServiceImpl startRecordsXmlCreation() : XMLStreamException occurred while creating records xml for file path {} : {}",
                        filePath, XMLStreamException);
            } catch (Exception exception) {
                LOGGER.error(
                        "RecordsGenerationServiceImpl startRecordsXmlCreation() : Exception occurred while creating records xml for file path {} : {}",
                        filePath, exception);
                exception.printStackTrace();
            }
        } else {
            LOGGER.info(
                    "RecordsGenerationServiceImpl startRecordsXmlCreation : Records XML is not generated as path {} does not exists",
                    rootPath);
            if (null != printWriter) {
                printWriter.println("Records XML is not generated as path " + rootPath + " does not exists");
            }
        }
    }

    /**
     * Search pages that are needed to be added to records XML and start adding them.
     * 
     * @param pageExtractionResourceResolver
     *            {@link ResourceResolver} of the system user with jcr:read access to root
     * @param session
     *            {@link Session}
     * @param stream
     *            {@link XMLStreamWriter}
     * @param pagesPaths
     *            {@link List} of {@link SitemapParent} for which the records XML creation is to be started
     * @param tagManager
     *            {@link TagManager}
     * @param pageManager
     *            {@link PageManager}
     * @return {@link Integer} number of pages added to records XML
     * @throws XMLStreamException
     *             {@link XMLStreamException}
     * @throws RepositoryException
     *             {@link RepositoryException}
     */
    private int createRecordsXml(final ResourceResolver pageExtractionResourceResolver, final Session session,
            final XMLStreamWriter stream, final List<SitemapParent> pagesPaths, final TagManager tagManager,
            final PageManager pageManager) throws XMLStreamException, RepositoryException {
        final List<Hit> hits = SitemapHelper.getSitemapResourcesHits(session, queryBuilder, pagesPaths, -1, null, null,
                searchTags);
        int numberOfPagesAdded = 0;
        for (final Hit pageHit : hits) {
            final Page page = pageManager.getPage(pageHit.getPath());
            if (null != page) {
                final ContentAuthorization contentAuthorization = MadisonUtil.getPageContentAuthorization(page);
                if (!contentAuthorization.getAudienceType().equals(AudienceType.INTERNAL_ONLY.getValue())
                        && !contentAuthorization.getAudienceType().equals(AudienceType.PRIVATE_GROUP.getValue())) {
                    stream.writeStartElement(SITEMAP_TAG_RECORD);
                    writeMetadataRecordTags(pageHit, stream, tagManager, pageExtractionResourceResolver);
                    stream.writeEndElement();
                    LOGGER.debug("RecordsGenerationServiceImpl : createRecordsXml() : Record added for path {}",
                            pageHit.getPath());
                    numberOfPagesAdded++;
                } else {
                    LOGGER.debug(
                            "RecordsGenerationServiceImpl : createRecordsXml() : Page is not added to records XML as page is either internal or private content {}",
                            pageHit.getPath());
                }
            }
        }
        return numberOfPagesAdded;
    }

    /**
     * Write record metadata tags to the {@link XMLStreamWriter} for the given page's {@link Hit}.
     * 
     * @param pageHit
     *            {@link Hit}
     * @param stream
     *            {@link XMLStreamWriter}
     * @param tagManager
     *            {@link TagManager}
     * @param pageExtractionResourceResolver
     *            {@link ResourceResolver}
     * @throws XMLStreamException
     *             {@link XMLStreamException}
     * @throws RepositoryException
     *             {@link RepositoryException}
     */
    private void writeMetadataRecordTags(final Hit pageHit, final XMLStreamWriter stream, final TagManager tagManager,
            final ResourceResolver pageExtractionResourceResolver) throws XMLStreamException, RepositoryException {
        final Map<String, String> metadataTagMap = new HashMap<String, String>();
        final ValueMap properties = pageHit.getProperties();
        if (!properties.containsKey(NameConstants.PN_REDIRECT_TARGET) && null != pageExtractionResourceResolver) {
            metadataTagMap.put(PWC_BODY,
                    writeHtmlBodyOfGivenPagePath(pageHit.getPath(), pageExtractionResourceResolver));
        }
        metadataTagMap.put(PWC_URL, domainService.getPublishedPageUrl(pageHit.getPath(), true));
        metadataTagMap.put(PWC_TITLE,
                properties.get(NameConstants.PN_PAGE_TITLE, properties.get(NameConstants.PN_TITLE, StringUtils.EMPTY)));
        metadataTagMap.put(PWC_DESCRIPTION, properties.get(JcrConstants.JCR_DESCRIPTION, StringUtils.EMPTY));
        metadataTagMap.put(PWC_KEYWORDS,
                String.join(COMMA_SEPERATOR, properties.get(DITAConstants.META_KEYWORDS, new String[] {})));
        metadataTagMap.put(PWC_COUNTRY, MadisonUtil.getTerritoryCodeForPath(pageHit.getPath()));
        metadataTagMap.put(PWC_LANGUAGE, MadisonUtil.getLanguageCodeForPath(pageHit.getPath()));
        metadataTagMap.put(PWC_LOCALE, MadisonUtil.getLocaleForPath(pageHit.getPath()));
        metadataTagMap.put(PWC_RELEASE_DATE, properties.get(DITAConstants.META_PUBLICATION_DATE, String.class));
        metadataTagMap.put(PWC_TAG,
                getFormattedTagString(properties.get(TagConstants.PN_TAGS, new String[] {}), tagManager));
        metadataTagMap.put(PWC_IND,
                getFormattedIndTagString(properties.get(TagConstants.PN_TAGS, new String[] {}), tagManager));
        for (final String recordTag : metadataTagMap.keySet()) {
            stream.writeStartElement(recordTag);
            stream.writeCharacters(metadataTagMap.get(recordTag));
            stream.writeEndElement();
        }
        metadataTagMap.remove(PWC_BODY);
        LOGGER.debug(
                "RecordsGenerationServiceImpl : writeMetadataRecordTags() : Record with metadata {} added for path {}",
                metadataTagMap, pageHit.getPath());
    }

    /**
     * Returns the text of the HTML body for the given page path. It removes common components headers , footer etc and
     * then extract the text from HTML body's {@Value #HTML_MAIN_CLASS} class tag..
     * 
     * @param pagePath
     *            {@link String}
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @return {@link String}
     */
    private String writeHtmlBodyOfGivenPagePath(final String pagePath, final ResourceResolver resourceResolver) {
        final String pagePathUrl = pagePath + MadisonConstants.HTML_EXTN;
        final HttpServletRequest request = requestResponseFactory.createRequest(HttpConstants.METHOD_GET, pagePathUrl);
        request.setAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME, WCMMode.DISABLED);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final HttpServletResponse response = requestResponseFactory.createResponse(outputStream);
        String pageBody = StringUtils.EMPTY;
        try {
            requestProcessor.processRequest(request, response, resourceResolver);
            final Document document = Jsoup.parseBodyFragment(outputStream.toString(MadisonConstants.UTF_8));
            final Element body = document.body();
            for (final String removableHtmlClass : removableHtmlClasses) {
                final Elements removableElement = body.getElementsByClass(removableHtmlClass);
                removableElement.remove();
            }
            Elements mainContent = body.getElementsByClass(HTML_MAIN_CLASS);
            pageBody = mainContent.text();
            LOGGER.debug(
                    "RecordsGenerationServiceImpl : writeHtmlBodyOfGivenPagePath() : Is able to extract HTML body text for path {}",
                    pagePath);
        } catch (ServletException | IOException exception) {
            LOGGER.error(
                    "RecordsGenerationServiceImpl : writeHtmlBodyOfGivenPagePath() : Exception occured while fetching body of a page for path {} : {}",
                    pagePath, exception);
        }
        return pageBody;
    }

    /**
     * Returns the formatted tag string.
     * 
     * @param tagIds
     *            {@link String[]} tags array for which the formatted tag string is to be generated
     * @param tagManager
     *            {@link TagManager}
     * @return {@link String}
     */
    private String getFormattedTagString(final String[] tagIds, final TagManager tagManager) {
        final List<String> tagTitles = new ArrayList<String>();
        if (null != tagManager) {
            for (final String tagId : tagIds) {
                Tag tag = tagManager.resolve(tagId);
                if (null != tag) {
                    tagTitles.add(tag.getTitle());
                }
            }
        }
        return String.join(TAG_SEPERATOR, tagTitles);
    }

    /**
     * Returns the formatted Industries tag string.
     * 
     * @param tagIds
     *            {@link String[]} tags array for which the formatted industry tag string is to be generated
     * @param tagManager
     *            {@link TagManager}
     * @return {@link String}
     */
    private String getFormattedIndTagString(final String[] tagIds, final TagManager tagManager) {
        final List<String> indTagTitles = new ArrayList<String>();
        if (null != tagManager) {
            for (final String tagId : tagIds) {
                Tag tag = tagManager.resolve(tagId);
                if (null != tag) {
                    final String tagPath = tag.getPath();
                    for (final String industryTagPath : industryTagPaths) {
                        if (tagPath.startsWith(industryTagPath)) {
                            indTagTitles.add(tag.getTitle());
                            break;
                        }
                    }
                }
            }
        }
        return String.join(COMMA_SEPERATOR, indTagTitles);
    }

}
