package com.pwc.madison.core.services.impl;

import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.beans.SitemapParent;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.services.SitemapGenerationService;
import com.pwc.madison.core.services.SitemapParentPagesProviderService;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SitemapHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = SitemapGenerationService.class)
public class SitemapGenerationServiceImpl implements SitemapGenerationService {

    private final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationServiceImpl.class);

    private static final String SITEMAP_TAG_URLSET = "urlset";
    private static final String XML_EXTENSION = ".xml";
    private static final String INCREMENTAL = "Incremental";
    private static final String SITE_MAP_DATE_FORMAT = "yyyy-MM-dd'T'HH:mmXXX";
    private static final String DELETE_SITEMAP_DATE_FORMAT = "E, dd MMM yyyy HH:mm:ss z";
    private static final String PATHS_PROPERTY = "paths";
    private static final String CQ_PATH = "cq:path";
    private static final String ACTIVATED = "Activate";
    private static final String MADISON_BASE_PATH = "/content/pwc-madison";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Replicator replicator;

    @Reference
    private MadisonDomainsService domainService;

    @Reference
    private SitemapParentPagesProviderService sitemapParentPagesProviderService;

    @Reference
    private XSSAPI xssapi;

    @Reference
    private QueryBuilder queryBuilder;
    PrintWriter printWriter = null;

    final SimpleDateFormat dateFormat = new SimpleDateFormat(SITE_MAP_DATE_FORMAT);
    final SimpleDateFormat deleteSitemapDeleteFormat = new SimpleDateFormat(DELETE_SITEMAP_DATE_FORMAT);

    @Override
    public void generateSitemapXml(final String fileName, final int totalSitemapFiles, final int urlsPerSitemapFiles,
                                   final Integer replicationMinutes, final Integer urlsPerLanguage, String territory, PrintWriter writer) {
        printWriter = writer;
        generateSitemap(fileName, totalSitemapFiles, urlsPerSitemapFiles, replicationMinutes, urlsPerLanguage, false, territory);
    }

    @Override
    public void generateDeleteSitemap(String fileName, Integer replicationMinutes, Integer urlsPerLanguage, String language, PrintWriter writer) {
        printWriter = writer;
        generateSitemap(fileName, 0, 0, replicationMinutes, urlsPerLanguage, true, language);
    }

    /**
     * Generates Sitemap files with given filename for Viewpoint site under each of the homepage of the site like
     * /content/pwc-madison/<territory>/<language>.
     *  @param fileName
     *            {@link String} name of the sitemap file
     * @param totalSitemapFiles
     *            {@link Integer} number of Sitemap files per locale
     * @param urlsPerSitemapFiles
 *            {@link Integer} number of URL entries per sitemap file
     * @param replicationMinutes
*            {@link Integer} if given incremental sitemap files are created. Can be null in case general sitemap
*            files are to be created. replication minutes defines the minutes taken from current time which is to
*            identify pages that got replicated in last replication minutes.
     * @param urlsPerLanguage
*            {@link Integer} number of URL entries per language
     * @param createDeleteSitemap
*            {@link Boolean} whether delete sitemap is to be created or nor
     * @param territoryOrLang
*            {@link String} to create territory specific file
     */
    private void generateSitemap(final String fileName, final int totalSitemapFiles, final int urlsPerSitemapFiles,
                                 final Integer replicationMinutes, final Integer urlsPerLanguage, final boolean createDeleteSitemap, final String territoryOrLang) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = MadisonUtil.getResourceResolver(resolverFactory,
                    MadisonConstants.MADISON_CONTENT_ADMIN_SUB_SERVICE);
            if (resourceResolver != null) {
                final Session session = resourceResolver.adaptTo(Session.class);
                startSitemapCreation(fileName, totalSitemapFiles, urlsPerSitemapFiles, replicationMinutes,
                        urlsPerLanguage, resourceResolver, session, createDeleteSitemap, territoryOrLang);
            } else {
                LOGGER.warn("SitemapGenerationServiceImpl generateSitemapXml() : Getting Resource Resolver {} as null",
                        MadisonConstants.MADISON_GENERIC_SUB_SERVICE);
            }
        }
        catch (final Exception exception) {
            LOGGER.error(
                    "SitemapGenerationServiceImpl generateSitemapXml() : Exception occurred while creating {} sitemap xml : {} ",
                    replicationMinutes != null ? INCREMENTAL : StringUtils.EMPTY, exception);
            printToResponse("Error while sitemap creation::" + exception.getMessage());
            exception.printStackTrace();
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /**
     * Generates Sitemap XML files with given filename for Viewpoint site under each of the homepage of the site like
     * /content/pwc-madison/<territory>/<language>.
     *
     * @param fileName
     *            {@link String} name of the sitemap file
     * @param totalSitemapFiles
     *            {@link Integer} number of Sitemap files per locale
     * @param urlsPerSitemapFiles
     *            {@link Integer} number of URL entries per sitemap file
     * @param replicationMinutes
     *            {@link Integer} if given incremental sitemap files are created. Can be null in case general sitemap
     *            files are to be created. replication minutes defines the minutes taken from current time which is to
     *            identify pages that got replicated in last replication minutes.
     * @param urlsPerLanguage
     *            {@link Integer} number of URL entries per language
     * @param resourceResolver
     *            {@link ResourceResolver} must have permission of reading and replication under Viewpoint Site
     * @param session
     *            {@link Session}
     * @param territoryOrLang
     *            {@link String}
     */
    private void startSitemapCreation(final String fileName, final int totalSitemapFiles, final int urlsPerSitemapFiles,
                                      final Integer replicationMinutes, final Integer urlsPerLanguage, final ResourceResolver resourceResolver,
                                      final Session session, final boolean createDeleteSitemap, final String territoryOrLang) {
        AtomicBoolean siteMapCreated = new AtomicBoolean(false);
        String territoryPage = MADISON_BASE_PATH + "/" + territoryOrLang;
        if (replicationMinutes != null) {
            final Calendar currentCalendar = Calendar.getInstance();
            final Date lastReplicateLowerBoundDate = new Date(
                    currentCalendar.getTimeInMillis() - replicationMinutes * 60 * 1000);
            if (createDeleteSitemap) {
                final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
                sitemapParentPagesProviderService.getDeleteSitmapParentPagesMap().entrySet().forEach((entry) -> {
                    boolean startSitemapCreation = StringUtils.isBlank(territoryOrLang) || (StringUtils.isNotBlank(territoryOrLang) && entry.getKey().equals(territoryOrLang));
                    if (startSitemapCreation) {
                        startDeleteSitemapCreation(resourceResolver, session, pageManager, fileName, entry.getValue(),
                                dateFormat.format(lastReplicateLowerBoundDate),
                                dateFormat.format(currentCalendar.getTime()), urlsPerLanguage, entry.getKey());
                        siteMapCreated.set(true);
                    }
                });
            } else {
                sitemapParentPagesProviderService.getIncrementalSitmapParentPagesMap().entrySet().forEach((entry) -> {
                    Integer limitPerMap = urlsPerLanguage;
                    for (final Entry<String, List<SitemapParent>> languageEntry : entry.getValue().entrySet()) {
                        boolean startSitemapCreation = StringUtils.isBlank(territoryOrLang)
                                || (StringUtils.isNotBlank(territoryOrLang)
                                && languageEntry.getKey().startsWith(territoryPage)
                                && resourceResolver.getResource(languageEntry.getKey()) != null);
                        if (startSitemapCreation) {
                            limitPerMap = startSitemapCreation(resourceResolver, session, totalSitemapFiles,
                                    urlsPerSitemapFiles, fileName, languageEntry.getValue(), limitPerMap,
                                    dateFormat.format(lastReplicateLowerBoundDate),
                                    dateFormat.format(currentCalendar.getTime()), limitPerMap, languageEntry.getKey());
                            siteMapCreated.set(true);
                        }
                    }
                });
            }
        } else {
            sitemapParentPagesProviderService.getSitmapParentPagesMap().entrySet().forEach((entry) -> {
                boolean startSitemapCreation = StringUtils.isBlank(territoryOrLang)
                        || (StringUtils.isNotBlank(territoryOrLang)
                        && entry.getKey().startsWith(territoryPage)
                        && resourceResolver.getResource(entry.getKey()) != null);
                if (startSitemapCreation) {
                    startSitemapCreation(resourceResolver, session, totalSitemapFiles, urlsPerSitemapFiles, fileName,
                            entry.getValue(), -1, null, null, null, entry.getKey());
                    siteMapCreated.set(true);
                }
            });
        }
        if (!siteMapCreated.get()) {
            printToResponse("ERROR::Check the territoryByLanguage path or check the content in repository");
            LOGGER.info("Check the territoryByLanguage path or check the content in repository");
        }
    }

    /**
     * print the message to response if hit from servlet
     * @param message String
     **/
    private void printToResponse(String message) {
        if (printWriter != null) {
            printWriter.println(xssapi.encodeForHTML(message));
        }
    }

    /**
     * Starts the sitemap files creation for each of the parent pages List given.
     *
     * @param resourceResolver
     *            {@link ResourceResolver} must have permission of reading and replication under Viewpoint Site
     * @param session
     *            {@link session}
     * @param totalSitemapFiles
     *            {@link Integer} number of Sitemap files per locale
     * @param urlsPerSitemapFiles
     *            {@link Integer} number of URL entries per sitemap file
     * @param sitemapFileName
     *            {@link String} name of the sitemap XML file
     * @param parentPagesList
     *            {@link List} of {@link SitemapParent} list of root paths which are to be used to search pages for
     *            sitemap XML file
     * @param limit
     *            {@link Integer} defines the number of results to be returned
     * @param repliactionLowerBound
     *            {@link String} of date to include pages replicated after this given date
     * @param repliactionUpperBound
     *            {@link String} of date to include pages replicated before or on this given date
     * @param limitPerMap
     *            {@link Integer} number of URL entries per language
     * @param parentPath
     *            {link String} the parent path below which sitemap files are to be created
     * @return {@link Integer} new limitPerMap value subtracting the number of pages added to sitemap till now for a
     *         language
     */
    private Integer startSitemapCreation(final ResourceResolver resourceResolver, final Session session,
            final int totalSitemapFiles, final int urlsPerSitemapFiles, final String sitemapFileName,
            List<SitemapParent> parentPagesList, final Integer limit, final String repliactionLowerBound,
            final String repliactionUpperBound, Integer limitPerMap, final String parentPath) {
        if (resourceResolver.getResource(parentPath) != null) {
            final String fileName = sitemapFileName + XML_EXTENSION;
            final String filePath = parentPath + MadisonConstants.FORWARD_SLASH + fileName;
            try {
                XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XMLStreamWriter stream = outputFactory.createXMLStreamWriter(bos, MadisonConstants.UTF_8);
                stream.writeStartDocument("1.0");
                stream.writeStartElement("", MadisonConstants.SITEMAP_TAG_SITEMAP_INDEX, MadisonConstants.NS);
                stream.writeNamespace("", MadisonConstants.NS);
                limitPerMap = createSitemapsForParent(parentPath, sitemapFileName, parentPagesList, resourceResolver,
                        totalSitemapFiles, urlsPerSitemapFiles, stream, limit, repliactionLowerBound,
                        repliactionUpperBound, session, limitPerMap);
                stream.writeEndElement();
                stream.writeEndDocument();
                ValueFactory vf = session.getValueFactory();
                Binary binary = vf.createBinary(new ByteArrayInputStream(bos.toByteArray()));
                SitemapHelper.saveFile(resourceResolver, parentPath, fileName, session, binary, replicator, sitemapParentPagesProviderService.getReplicationAgents());
                printToResponse("file created at path :" + parentPath);
            } catch (XMLStreamException XMLStreamException) {
                LOGGER.error(
                        "SitemapGenerationServiceImpl startSitemapCreation() : XMLStreamException occurred while creating generic sitemap xml for file path {} : {}",
                        filePath, XMLStreamException);
            } catch (Exception exception) {
                LOGGER.error(
                        "SitemapGenerationServiceImpl startSitemapCreation() : Exception occurred while creating sitemap xml for file path {} : {}",
                        filePath, exception);
                exception.printStackTrace();
            }
        }
        return limitPerMap;
    }

    /**
     * Create sitemap XML file for given parentRoot.
     *
     * @param parentRoot
     *            {@link String} under which the sitemap XML file is to be created.
     * @param fileName
     *            {@link String} name of the sitemap XML file
     * @param parentPagesList
     *            {@link List} of {@link SitemapParent} list of root paths which are to be used to search pages for
     *            sitemap XML file
     * @param resourceResolver
     *            {@link ResourceResolver} must have permission of reading and replication under Viewpoint Site
     * @param totalSitemapFiles
     *            {@link Integer} number of sitemap files per locale
     * @param urlsPerSitemapFiles
     *            {@link Integer} number of URL entries per sitemap file
     * @param parentStreamWriter{@link
     *            {@link XMLStreamWriter} of the parent sitemap Index
     * @param limit
     *            {@link Integer} defines the number of results to be returned
     * @param repliactionLowerBound
     *            {@link String} of date to include pages replicated after this given date
     * @param repliactionUpperBound
     *            {@link String} of date to include pages replicated before or on this given date
     * @param session
     *            {@link Session}
     * @param limitPerMap
     *            {@link Integer} number of URL entries per language
     * @return {@link Integer} new limitPerMap value subtracting the number of pages added to sitemap till now for a
     *         language
     */
    private Integer createSitemapsForParent(final String parentRoot, final String fileName,
            final List<SitemapParent> parentPagesList, final ResourceResolver resourceResolver,
            final int totalSitemapFiles, final int urlsPerSitemapFiles, final XMLStreamWriter parentStreamWriter,
            final Integer limit, final String repliactionLowerBound, final String repliactionUpperBound,
            final Session session, Integer limitPerMap) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("IncrementalSitemapXmlGeneratorScheduler :  run() Time Taken by Incremental Sitemap Creation in seconds {}", (System.currentTimeMillis() - startTime)/1000);

        LOGGER.info(
                "SitemapGenerationServiceImpl createSitemapForParent() : {} Sitemap creation starting for parent root path : {}",
                null != repliactionLowerBound ? INCREMENTAL : StringUtils.EMPTY, parentRoot);
        List<Hit> hits = SitemapHelper.getSitemapResourcesHits(session, queryBuilder, parentPagesList, limit,
                repliactionLowerBound, repliactionUpperBound, null);
        LOGGER.info("SitemapGenerationServiceImpl createSitemapForParent() : {} No. of hits: {}",
                null != repliactionLowerBound ? INCREMENTAL : StringUtils.EMPTY, hits.size());
        Iterator<Hit> searchHitIterator = hits == null ? null : hits.iterator();
        for (int counter = 1; counter <= totalSitemapFiles; counter++) {
            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            final String sitemapFileName = fileName + counter + XML_EXTENSION;
            final String filePath = parentRoot + MadisonConstants.FORWARD_SLASH + sitemapFileName;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XMLStreamWriter stream = outputFactory.createXMLStreamWriter(bos, MadisonConstants.UTF_8);
                stream.writeStartDocument("1.0");
                stream.writeStartElement("", SITEMAP_TAG_URLSET, MadisonConstants.NS);
                stream.writeNamespace("", MadisonConstants.NS);
                if (searchHitIterator != null) {
                    if (counter == 1) {
                        int numOfParentPagesAdded = addParentPagesInSiteMap(resourceResolver, repliactionLowerBound,
                                repliactionUpperBound, stream, parentPagesList);
                        if (Objects.nonNull(limitPerMap)) {
                            limitPerMap = limitPerMap - numOfParentPagesAdded;
                        }
                        limitPerMap = addPages(stream, totalSitemapFiles, urlsPerSitemapFiles - numOfParentPagesAdded,
                                searchHitIterator, counter, limitPerMap);
                    } else {
                        limitPerMap = addPages(stream, totalSitemapFiles, urlsPerSitemapFiles, searchHitIterator,
                                counter, limitPerMap);
                    }
                }
                stream.writeEndElement();
                stream.writeEndDocument();
                ValueFactory vf = session.getValueFactory();
                Binary binary = vf.createBinary(new ByteArrayInputStream(bos.toByteArray()));
                SitemapHelper.saveFile(resourceResolver, parentRoot, sitemapFileName, session, binary, replicator, sitemapParentPagesProviderService.getReplicationAgents());
                SitemapHelper.writeSitemap(parentStreamWriter, domainService.getPublishedPageUrl(filePath, false),
                        Calendar.getInstance(), dateFormat);
            } catch (XMLStreamException XMLStreamException) {
                LOGGER.error(
                        "SitemapGenerationServiceImpl createSitemapForParent() : XMLStreamException occurred while creating generic sitemap xml for file path {} : {}",
                        filePath, XMLStreamException);
            } catch (Exception exception) {
                LOGGER.error(
                        "SitemapGenerationServiceImpl createSitemapForParent() : Exception occurred while creating sitemap xml for file path {} : {}",
                        filePath, exception);
                exception.printStackTrace();
            }
        }
        LOGGER.info(
                "SitemapGenerationServiceImpl createSitemapForParent() : {} Sitemap creation completed for parent root path : {}",
                null != repliactionLowerBound ? INCREMENTAL : StringUtils.EMPTY, parentRoot + " in seconds : " + (System.currentTimeMillis() - startTime)/1000);
        return limitPerMap;
    }

    /**
     * Add parent pages in the sitemap (create/delete) xml. This method is required because 'self' predicate has been
     * removed to optimize query.
     *
     * @param resourceResolver
     *            {@link ResourceResolver}
     * @param repliactionLowerBound
     *            {@link String} of date to include pages replicated after this given date
     * @param repliactionUpperBound
     *            {@link String} of date to include pages replicated before or on this given date
     * @param createSiteMapStream
     *            {@link XMLStreamWriter} of create site map file, null in case of delete site map
     * @param parentPagesList
     *            {@link List} of {@link SitemapParent} list of root paths which are to be used to search pages for
     *            sitemap file
     * @return number of pages added in sitemap
     * @throws XMLStreamException
     */
    private Integer addParentPagesInSiteMap(ResourceResolver resourceResolver, String repliactionLowerBound,
            String repliactionUpperBound, XMLStreamWriter createSiteMapStream, List<SitemapParent> parentPagesList)
            throws XMLStreamException {
        int numOfParentPages = 0;
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Date replicationLowerBoundDate = null, replicationUpperBoundDate = null;
        try {
            if (Objects.nonNull(repliactionLowerBound)) {
                replicationLowerBoundDate = dateFormat.parse(repliactionLowerBound);
            }
            if (Objects.nonNull(repliactionUpperBound)) {
                replicationUpperBoundDate = dateFormat.parse(repliactionUpperBound);
            }
        } catch (ParseException e) {
            LOGGER.error("Error in parsing replication date", e);
        }
        for (int i = 0; i < parentPagesList.size(); i++) {
            Page page = pageManager.getPage(parentPagesList.get(i).getParentPagePath());

            if (Objects.isNull(page)) {
                continue;
            }

            ValueMap pageProperties = page.getProperties();
            String lastReplicationAction = pageProperties.get(NameConstants.PN_PAGE_LAST_REPLICATION_ACTION,
                    String.class);
            Calendar lastReplicated = pageProperties.get(NameConstants.PN_PAGE_LAST_REPLICATED, Calendar.class);
            Calendar lastModified = page.getLastModified();

            if (lastModified != null && ACTIVATED.equals(lastReplicationAction)
                    && (Objects.isNull(replicationUpperBoundDate)
                    || (lastReplicated.getTime().after(replicationLowerBoundDate)
                    && lastReplicated.getTime().before(replicationUpperBoundDate)))) {
                SitemapHelper.writeLOC(createSiteMapStream, domainService.getPublishedPageUrl(page.getPath(), true),
                        lastModified, dateFormat);
                numOfParentPages++;
            }
        }
        return numOfParentPages;
    }

    /**
     * Iterates over the nested children of the given parent page to write them to sitemap XML file.
     *
     * @param stream
     *            {@link XMLStreamWriter}
     * @param totalSitemapFiles
     *            {@link Integer} number of Sitemap files per locale
     * @param urlsPerSitemapFiles
     *            {@link Integer} number of URL entries per sitemap file
     * @param searchHitIterator
     *            {@link Iterator} of {@link Hit}
     * @param counter
     *            {@link Integer} counter representing the file number for which URLS being added to sitemap
     * @param limitPerMap
     *            {@link Integer} number of URL entries per language
     * @return {@link Integer} new limitPerMap value subtracting the number of pages added to sitemap till now for a
     *         language
     * @throws XMLStreamException
     *             {@link XMLStreamException}
     */
    private Integer addPages(final XMLStreamWriter stream, final int totalSitemapFiles, final int urlsPerSitemapFiles,
            Iterator<Hit> searchHitIterator, final int counter, Integer limitPerMap) throws XMLStreamException {
        int urlCounter = 1;
        while (searchHitIterator.hasNext() && (urlCounter <= urlsPerSitemapFiles || counter == totalSitemapFiles)
                && (limitPerMap == null || limitPerMap != 0)) {
            final Hit pageHit = searchHitIterator.next();
            try {
                ValueMap pageValueMap = pageHit.getProperties();
                Calendar lastModified = pageValueMap.get(NameConstants.PN_PAGE_LAST_MOD,
                        pageValueMap.get(NameConstants.PN_LAST_MOD, Calendar.class));
                if (lastModified != null) {
                    SitemapHelper.writeLOC(stream, domainService.getPublishedPageUrl(pageHit.getPath(), true),
                            lastModified, dateFormat);
                    urlCounter++;
                    if (limitPerMap != null) {
                        limitPerMap--;
                    }
                }
            } catch (RepositoryException repositoryException) {
                LOGGER.error(
                        "SitemapGenerationServiceImpl addPages() : Repository Exception occurred while getting properties from search Hit for path {} : {} ",
                        pageHit, repositoryException);
            }
        }
        return limitPerMap;
    }

    /**
     * Starts the delete sitemap files creation for each of the parent pages List given.
     *
     * @param resourceResolver
     *            {@link ResourceResolver} must have permission of reading and replication under Viewpoint Site
     * @param session
     *            {@link session}
     * @param pageManager
     *            {@link PageManager}
     * @param sitemapFileName
     *            {@link String} name of the sitemap file
     * @param sitemapParentsMap
     *            {@link Map} where key is {@link String} and value is {@link List} of {@link SitemapParent} list of
     *            root paths which are to be used to search pages for delete sitemap file
     * @param repliactionLowerBound
     *            {@link String} of date to include pages deactivated/delete after this given date
     * @param repliactionUpperBound
     *            {@link String} of date to include pages deactivated/delete before or on this given date
     * @param limitPerMap
     *            {@link Integer} number of URL entries per language
     */
    private void startDeleteSitemapCreation(final ResourceResolver resourceResolver, final Session session,
            final PageManager pageManager, final String sitemapFileName,
            Map<String, List<SitemapParent>> sitemapParentsMap, final String repliactionLowerBound,
            final String repliactionUpperBound, final Integer urlsPerLanguage, final String language) {
        try {
            Integer limitPerMap = new Integer(urlsPerLanguage);
            final StringBuilder deleteFileBuilder = new StringBuilder();
            ValueFactory vf = session.getValueFactory();
            String filePath = StringUtils.EMPTY;
            for (final Entry<String, List<SitemapParent>> languageEntry : sitemapParentsMap.entrySet()) {
                if (resourceResolver.getResource(languageEntry.getKey()) != null) {
                    long startTime = System.currentTimeMillis();
                    LOGGER.info(
                            "SitemapGenerationServiceImpl startDeleteSitemapCreation() : Delete Sitemap creation starting for parent root path : {}",
                            languageEntry.getKey());
                    filePath = StringUtils.isNotBlank(filePath) ? filePath : languageEntry.getKey();
                    List<Hit> hits = SitemapHelper.getSitemapDeleteResourcesHits(session, queryBuilder,
                            languageEntry.getValue(), repliactionLowerBound, repliactionUpperBound);
                    LOGGER.info("SitemapGenerationServiceImpl startDeleteSitemapCreation() : No. of hits: {}",
                            hits.size());
                    if (hits != null) {
                        limitPerMap = addDeletePages(pageManager, deleteFileBuilder, hits.iterator(), limitPerMap,
                                urlsPerLanguage.intValue() == limitPerMap.intValue());
                    }
                    LOGGER.info(
                            "SitemapGenerationServiceImpl startDeleteSitemapCreation() : Delete Sitemap creation completed for parent root path {} in seconds : {}",
                            languageEntry.getKey(), (System.currentTimeMillis() - startTime) / 1000);
                    printToResponse("Delete Sitemap creation completed for parent root path "+languageEntry.getKey());
                }
            }
            Binary binary = vf.createBinary(new ByteArrayInputStream(deleteFileBuilder.toString().getBytes()));
            SitemapHelper.saveFile(resourceResolver, filePath, sitemapFileName, session, binary, replicator, sitemapParentPagesProviderService.getReplicationAgents());
        } catch (Exception exception) {
            LOGGER.error(
                    "SitemapGenerationServiceImpl startDeleteSitemapCreation() : Exception occurred while creating delete sitemap for lanuage {} : {}",
                    language, exception);
            exception.printStackTrace();
        }

    }

    /**
     * Iterates over the given Hit Iterator and add entry to the given {@link StringBuilder}.
     *
     * @param pageManager
     *            {@link PageManager}
     * @param stringBuilder
     *            {@link StringBuilder}
     * @param searchHitIterator
     *            {@link Iterator} of {@link Hit}
     * @param limitPerMap
     *            {@link Integer} number of URL entries per language
     * @return {@link Integer} new limitPerMap value subtracting the number of pages added to sitemap till now for a
     *         language
     */
    private Integer addDeletePages(final PageManager pageManager, final StringBuilder stringBuilder,
            final Iterator<Hit> searchHitIterator, Integer limitPerMap, boolean isFirstEntry) {
        final Set<String> uniquePageSet = new HashSet<String>();
        while (searchHitIterator.hasNext() && limitPerMap != 0) {
            final Hit pageHit = searchHitIterator.next();
            try {
                final ValueMap pageValueMap = pageHit.getProperties();
                final String pagePath = pageValueMap.get(CQ_PATH, String.class);
                final String[] pagePaths = pageValueMap.get(PATHS_PROPERTY, String[].class);
                if (null != pagePaths && pagePaths.length > 0) {
                    int count = 0;
                    while (count != pagePaths.length && limitPerMap != 0) {
                        if (addDeletePage(uniquePageSet, pageManager, stringBuilder, pagePaths[count], isFirstEntry)) {
                            limitPerMap--;
                            isFirstEntry = false;
                        }
                        count++;
                    }
                } else {
                    if (addDeletePage(uniquePageSet, pageManager, stringBuilder, pagePath, isFirstEntry)) {
                        limitPerMap--;
                        isFirstEntry = false;
                    }
                }
            } catch (RepositoryException repositoryException) {
                LOGGER.error(
                        "SitemapGenerationServiceImpl addDeletePages() : Repository Exception occurred while getting properties from search Hit for path {} : {} ",
                        pageHit, repositoryException);
            }
        }
        return limitPerMap;
    }

    /**
     * Add the given delete entry for given page path only if page entry is not already added && either page does not
     * exist or if exists then is not activated. Returns true if page entry is added otherwise false.
     *
     * @param uniquePageSet
     *            {@link Set}
     * @param pageManager
     *            {@link PageManager}
     * @param stringBuilder
     *            {@link StringBuilder}
     * @param pagePath
     *            {@link String}
     * @param isFirstEntry
     *            {@link boolean}
     * @return {@link Boolean}
     */
    private boolean addDeletePage(final Set<String> uniquePageSet, final PageManager pageManager,
            final StringBuilder stringBuilder, final String pagePath, final boolean isFirstEntry) {
        final Page page = pageManager.getPage(pagePath);
        if (!uniquePageSet.contains(pagePath) && (null == page || !ACTIVATED
                .equals(page.getProperties().get(NameConstants.PN_PAGE_LAST_REPLICATION_ACTION, String.class)))) {
            if (isFirstEntry) {
                stringBuilder.append(
                        String.format("date %s\n", deleteSitemapDeleteFormat.format(Calendar.getInstance().getTime())));
            }
            stringBuilder.append(String.format("delete %s\n", domainService.getPublishedPageUrl(pagePath, true)));
            uniquePageSet.add(pagePath);
            return true;
        }
        return false;
    }

}
