package com.pwc.madison.core.services.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import com.pwc.madison.core.services.TocContentServiceConfig;
import com.pwc.madison.core.util.DITAUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.eclipse.jetty.http.HttpStatus;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.impl.Link;
import com.pwc.madison.core.services.TocContentService;
import com.pwc.madison.core.util.MadisonUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;

@Component(service = TocContentService.class, immediate = true)
public class TocContentServiceImpl implements TocContentService {


	private static final String PAGE_TITLE = "pageTitle";
	private static final String ID = "id";
	private static final String FMDITA_COMPONENTS_DITA_DELEGATOR = "fmdita/components/dita/delegator";
	private static final String PWC_MADISON_COMPONENTS_DITACONTENT_TOC_JOINED = "pwc-madison/components/ditacontent/toc-joined";
	private static final String TOC_JOINED = "toc-joined";
	private static final String FMDITA_COMPONENTS_DITA_TOPIC = "fmdita/components/dita/topic";
	private static final String DITA_NODE = "dita";
	private static final String SLASH_DITA_SLASH = "/dita/";
	private static final String CONTENTBODY_TOPICBODY = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody";
	private static final String TRUE = "true";
	private static final String CHAPTER_PATHS = "chapterPaths";
	private static final Logger LOGGER = LoggerFactory.getLogger(TocContentServiceImpl.class);
	private static final String PROPERTY_LINK = "link";
	private static final String PROPERTY_NAV_TITLE = "navTitle";
	private static final String PROPERTY_TITLE = "title";
	private static final String PROPERTY_LINK_TEXT = "linkText";
	private static final String PROPERTY_SHOW_TOC = "toc";
	private static final String TOC_VAL_YES = "yes";
	private static final String TOC_VAL_NO = "no";
	private static final String JCR_TITLE = "jcr:title";
	private static final String LINK_TEXT = "linkText";
	private static final String TITLE = "title";
	private static final String FETCHTOC_JSON = ".fetchtoc.json";
	private static final String REP_ACL = "rep:ACL";
	private static final String PATH = "path";
	private static final String P_LIMIT = "p.limit";
	private static final String P_LIMIT_VALUE = "-1";
	private static final String PN_TYPE = "type";
	private static final String CQ_PAGE = "cq:Page";
	private static final String TOC_NODE = "/jcr:content/toc";
	private static final String FORWARD_SLASH = "/";

	private Boolean showEntireToc = false;

    @Reference
    private QueryBuilder queryBuilder;

	@Reference
	private ResourceResolverFactory resolverFactory;

    private List<String> excludedMaps;
    private int targetedLevel;

    public List<String> getExcludedMaps() {
		return excludedMaps;
	}

	@Activate
	public void activate() {
	}

	@Override
	public String getFullTocContentJson(SlingHttpServletRequest request, SlingHttpServletResponse response, int joinedLevel, boolean isJoinViewEnabled)
			throws IOException {
		try {
			final List<Link> tocLinks = new ArrayList<>();
			final ResourceResolver resolver = request.getResourceResolver();
			final String basePagePath = request.getResource().getPath();
			//Get FASB specific configurations
			if (basePagePath.matches(MadisonConstants.FASB_CONTENT_REGEX)){
				targetedLevel = 4;
				excludedMaps = getFasbNumberingConfig(basePagePath, resolver);
			}
			if (StringUtils.isBlank(basePagePath)) {
				LOGGER.error("basepage is blank");
				sendErrorResponse(response);
				return "{}";
			}
			final String tocNodePath = basePagePath + DITAConstants.TOC_NODE_PATH;
			final Resource tocResource = resolver.getResource(tocNodePath);
			if (null == tocResource) {
				LOGGER.error("toc node unavailable in the base page");
				sendErrorResponse(response);
				return "{}";
			}
			final Node tocNode = tocResource.adaptTo(Node.class);
			final NodeIterator entries = tocNode.getNodes();
			if (entries.getSize() == 1) {
				final Node childNode = entries.nextNode();
				final NodeIterator entryNodes = childNode.getNodes();
				while (entryNodes.hasNext()) {
					final Node entryNode = entryNodes.nextNode();
					populateTOCLinks(entryNode, 1, tocLinks, resolver, joinedLevel, isJoinViewEnabled);
				}
			} else {
				while (entries.hasNext()) {
					final Node childNode = entries.nextNode();
					populateTOCLinks(childNode, 1, tocLinks, resolver, joinedLevel, isJoinViewEnabled);
				}
			}
			final Gson gson = new Gson();
			final Link tocParent = new Link(null, null, null, null, null, null, showEntireToc, null);
			tocParent.setChildLinks(tocLinks);
			response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			response.getWriter().write(gson.toJson(tocParent));
		} catch (final RepositoryException e) {
			LOGGER.error("RepositoryException in getFullTocContentJson method {}", e);
			response.getWriter().write("{}");

		}
		return "{}";
	}

	@Override
	public void createChapterTocContentJson(ResourceResolver resolver, String basePagePath, List<Page> pageList, int joinedLevel, boolean isJoinViewEnabled) {
		if (StringUtils.isNotBlank(basePagePath)) {
			try {
				long apiStartTime = System.currentTimeMillis();
				final List<Link> tocLinks = new ArrayList<>();
				final String tocNodePath = basePagePath + DITAConstants.TOC_NODE_PATH;
				//Get FASB specific configurations
				if (basePagePath.matches(MadisonConstants.FASB_CONTENT_REGEX)){
					targetedLevel = 4;
					excludedMaps = getFasbNumberingConfig(basePagePath, resolver);
				}
				final Resource tocResource = resolver.getResource(tocNodePath);
				if (null != tocResource) {
					final Node tocNode = tocResource.adaptTo(Node.class);
					final NodeIterator entries = tocNode.getNodes();
					if (entries.getSize() == 1) {
						final Node childNode = entries.nextNode();
						final NodeIterator entryNodes = childNode.getNodes();
						while (entryNodes.hasNext()) {
							final Node entryNode = entryNodes.nextNode();
							populateTOCLinks(entryNode, 1, tocLinks, resolver, joinedLevel, isJoinViewEnabled);
						}
					} else {
						while (entries.hasNext()) {
							final Node childNode = entries.nextNode();
							populateTOCLinks(childNode, 1, tocLinks, resolver, joinedLevel, isJoinViewEnabled);
						}
					}
					addJson(resolver, tocLinks, pageList, basePagePath);
					if (resolver.hasChanges()) {
						resolver.commit();
					}
				} else {
					LOGGER.error("toc node unavailable in the base page");
				}
				long apiEndTime = System.currentTimeMillis();
				LOGGER.debug("Execution Time {}", (apiEndTime - apiStartTime));
			} catch (final PersistenceException | RepositoryException e) {
				LOGGER.error("Some error in createChapterTocContentJson method {}", e);
			}
		} else {
			LOGGER.error("basepage is blank");
		}
	}

	private void addJson(ResourceResolver resolver, final List<Link> tocLinks, final List<Page> tocPagelist, String basePagePath) {
		final Gson gson = new Gson();
		try {
			Map<String, Page> actualPageMap = getActualPageList(tocLinks, tocPagelist, basePagePath, resolver);
			for (Map.Entry<String,Page> entry : actualPageMap.entrySet()) {
				String pageTitle = entry.getKey();
				Page page = entry.getValue();
				final List<Link> chapterTocLinks = new ArrayList<>();
				for (Link link : tocLinks) {
					String linkTitle = link.getJcrTitle();
					String tocPath = null != actualPageMap.get(linkTitle) ? actualPageMap.get(linkTitle).getPath() : StringUtils.EMPTY;
					if (pageTitle.equals(linkTitle)) {
						final Link currentChapter = new Link(link.getPath(), link.getTitle(), link.getLevel(), null,
								link.getPageName(), link.getNodeLink(), link.getToc(), link.getPathHash());
						currentChapter.setChildLinks(link.getChildLinks());
						currentChapter.setHasChildren(null != link.getChildLinks() && !link.getChildLinks().isEmpty());
						currentChapter.setTocPath(tocPath);
						if(StringUtils.isNotBlank(link.getJoinedTocPagePath())) {
							currentChapter.setJoinedTocPagePath(link.getJoinedTocPagePath());
						}
						chapterTocLinks.add(currentChapter);
					} else {
						final Link notCurrentChapter = new Link(link.getPath(), link.getTitle(), link.getLevel(), null,
								link.getPageName(), link.getNodeLink(), link.getToc(), link.getPathHash());
						notCurrentChapter.setTocPath(tocPath);
						notCurrentChapter.setHasChildren(null != link.getChildLinks() && !link.getChildLinks().isEmpty());
						chapterTocLinks.add(notCurrentChapter);
					}
				}
				if (!chapterTocLinks.isEmpty()) {
					final Link tocParent = new Link(null, null, null, null, null, null, showEntireToc, null);
					tocParent.setChildLinks(chapterTocLinks);
					String json = gson.toJson(tocParent);
					createJsonFile(resolver, page, json, page.getParent().adaptTo(Node.class));
				} else {
					LOGGER.debug("No toc list");
				}
			}
			Map<String, String> chapterPagesMap = new LinkedHashMap<>();
			for (Map.Entry<String, Page> entry : actualPageMap.entrySet()) {
				String title = entry.getKey();
				Page page = entry.getValue();
				chapterPagesMap.put(title, page.getPath());
			}
			if(!chapterPagesMap.isEmpty()) {
				String chapterPathsJson = gson.toJson(chapterPagesMap);
				Resource baseTocResource = resolver.getResource(basePagePath + FORWARD_SLASH + JcrConstants.JCR_CONTENT);
				if(null != baseTocResource && !ResourceUtil.isNonExistingResource(baseTocResource)) {
					ModifiableValueMap modifiableValueMap = baseTocResource.adaptTo(ModifiableValueMap.class);
					modifiableValueMap.put(CHAPTER_PATHS, chapterPathsJson);
				}
			}
		} catch (ItemExistsException e) {
			LOGGER.error("ItemExistsException occured {}", e);
		} catch (PathNotFoundException e) {
			LOGGER.error("PathNotFoundException occured {}", e);
		} catch (VersionException e) {
			LOGGER.error("VersionException occured {}", e);
		} catch (ConstraintViolationException e) {
			LOGGER.error("ConstraintViolationException occured {}", e);
		} catch (LockException e) {
			LOGGER.error("LockException occured {}", e);
		} catch (ValueFormatException e) {
			LOGGER.error("ValueFormatException occured {}", e);
		} catch (RepositoryException e) {
			LOGGER.error("RepositoryException occured {}", e);
		}
	}

	private Map<String, Page> getActualPageList(List<Link> tocLinks, List<Page> tocPagelist, String basePagePath, ResourceResolver resolver) {
		Map<String, Page> actualPageMap = new HashMap<>();
		Map<String, List<Page>> map = new HashMap<>();
		for(Link link : tocLinks) {
			for(Page page : tocPagelist) {
				String title = link.getJcrTitle();
				String pageTitle = MadisonUtil.getPageTitle(page);
				if(null != pageTitle && pageTitle.equals(title)) {
					if(map.containsKey(link.getTitle())) {
						List<Page> list = map.get(title);
						list.add(page);
						map.put(title, list);
					} else {
						List<Page> list = new ArrayList<>();
						list.add(page);
						map.put(title, list);
					}
				}
			}
		}
		// get actual chapter list
		for (Map.Entry<String, List<Page>> entry : map.entrySet()) {
			String title = entry.getKey();
			List<Page> list = entry.getValue();
			if(1 == list.size()) {
				actualPageMap.put(title, list.get(0));
			} else {
				Collections.sort(list, (p1, p2) -> { return (p1.getPath().length() - p2.getPath().length());});
				if(basePagePath.equals(list.get(0).getPath())) {
					actualPageMap.put(title, list.get(1));
					LOGGER.debug("after sorting the second element is {}", list.get(1).getPath());
				} else {
					actualPageMap.put(title, list.get(0));
					LOGGER.debug("after sorting the first element is {}", list.get(0).getPath());
				}
			}
		}
		// as fallback add the missing chapter path if any.
		if(actualPageMap.size() != tocLinks.size()) {
			for(Link link : tocLinks) {
				String title = link.getJcrTitle();
				if(null == actualPageMap.get(title)) {
					String path = link.getNodeLink().split(".html")[0].replace(DITAConstants.DITA_CONTENT_ROOT_SHORT, DITAConstants.DITA_CONTENT_ROOT);
					String str = path.replace(basePagePath, StringUtils.EMPTY);
					String[] arr = str.split("/");
					String chapterNodePath;
					if(arr.length > 3) {
						chapterNodePath = basePagePath + str.substring(0, StringUtils.ordinalIndexOf(str, "/", 3));
					} else {
						chapterNodePath = path;
					}
					Resource resource = resolver.getResource(chapterNodePath);
					if(null != resource && !ResourceUtil.isNonExistingResource(resource)) {
						actualPageMap.put(title, resource.adaptTo(Page.class));
					}
				}
			}
		}
		Map<String, Page> orderedActualPageMap = new LinkedHashMap<>();
		for(Link link : tocLinks) {
			String title = link.getJcrTitle();
			Page page = actualPageMap.get(title);
			if(null != page) {
				orderedActualPageMap.put(title, page);
			}
		}
		return orderedActualPageMap;
	}

	private void createJsonFile(ResourceResolver resolver, Page page, String json, Node node)
			throws RepositoryException {
		Resource chapterTocResource = resolver
				.getResource(page.getPath() + FETCHTOC_JSON);
		if (null != chapterTocResource && !ResourceUtil.isNonExistingResource(chapterTocResource)) {
			Node jsonNode = chapterTocResource.adaptTo(Node.class);
			jsonNode.remove();
			try {
				resolver.commit();
			} catch (PersistenceException e) {
				LOGGER.error("PersistenceException occured in createJsonFile method {}", e);
			}
			// Create the node
			create(page, json, node);
		} else {
			create(page, json, node);
		}
	}

	private void create(Page page, String json, Node node) {
		try {
			InputStream is = new ByteArrayInputStream(json.getBytes());
			javax.jcr.ValueFactory valueFactory = node.getSession().getValueFactory();
			javax.jcr.Binary contentValue = valueFactory.createBinary(is);
			String fileName = page.getName() + FETCHTOC_JSON;
			Node fileNode = node.addNode(fileName, JcrConstants.NT_FILE);
			fileNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
			Node resNode = fileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
			resNode.setProperty(JcrConstants.JCR_MIMETYPE, MadisonConstants.CONTENTTYPE_JSON);
			resNode.setProperty(JcrConstants.JCR_DATA, contentValue);
			Calendar lastModified = Calendar.getInstance();
			lastModified.setTimeInMillis(lastModified.getTimeInMillis());
			resNode.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);
		} catch (UnsupportedRepositoryOperationException e) {
			LOGGER.error("UnsupportedRepositoryOperationException occured {}", e);
		} catch (ItemExistsException e) {
			LOGGER.error("ItemExistsException occured {}", e);
		} catch (PathNotFoundException e) {
			LOGGER.error("PathNotFoundException occured {}", e);
		} catch (NoSuchNodeTypeException e) {
			LOGGER.error("NoSuchNodeTypeException occured {}", e);
		} catch (LockException e) {
			LOGGER.error("LockException occured {}", e);
		} catch (VersionException e) {
			LOGGER.error("VersionException occured {}", e);
		} catch (ConstraintViolationException e) {
			LOGGER.error("ConstraintViolationException occured {}", e);
		} catch (ValueFormatException e) {
			LOGGER.error("ValueFormatException occured {}", e);
		} catch (RepositoryException e) {
			LOGGER.error("RepositoryException occured {}", e);
		}
	}

	@Override
	public List<Link> getFullTocContent(ResourceResolver resolver, String basePath, int joinedLevel, boolean isJoinViewEnabled) {
			final List<Link> tocLinks = new ArrayList<>();
			try {
				final String tocNodePath = basePath + DITAConstants.TOC_NODE_PATH;
				final Resource tocResource = resolver.getResource(tocNodePath);
				final Node tocNode = tocResource.adaptTo(Node.class);
				final NodeIterator entries = tocNode.getNodes();
				//Get FASB specific configurations
				if (basePath.matches(MadisonConstants.FASB_CONTENT_REGEX)){
					targetedLevel = 4;
					excludedMaps = getFasbNumberingConfig(basePath, resolver);
				}
				if (entries.getSize() == 1) {
					final Node childNode = entries.nextNode();
					final NodeIterator entryNodes = childNode.getNodes();
					while (entryNodes.hasNext()) {
						final Node entryNode = entryNodes.nextNode();
						populateTOCLinks(entryNode, 1, tocLinks, resolver, joinedLevel, isJoinViewEnabled);
					}
				} else {
					while (entries.hasNext()) {
						final Node childNode = entries.nextNode();
						populateTOCLinks(childNode, 1, tocLinks, resolver, joinedLevel, isJoinViewEnabled);
					}
				}
			} catch (RepositoryException e) {
				LOGGER.error("RepositoryException occured in getFullTocContent method {}", e);
			}
		return tocLinks;
	}

	/**
	 * Populate the TOC link object and add them to the list
	 *
	 * @param entryNode
	 * @param level
	 * @param currentList
	 * @param joinedLevel
	 * @param isJoinViewEnabled
	 * @param isJoinedSectionEnabled
	 */
	private void populateTOCLinks(final Node entryNode, final int level, final List<Link> currentList,
			ResourceResolver resolver, int joinedLevel, boolean isJoinViewEnabled) {
		Node childNode;
		List<Link> childLinks = null;
		try {
			if (entryNode.hasNodes()) {
				childLinks = new ArrayList<>();
				final NodeIterator entries = entryNode.getNodes();
				while (entries.hasNext()) {
					childNode = entries.nextNode();
					if (!childNode.isNodeType(REP_ACL)) {
						populateTOCLinks(childNode, level + 1, childLinks, resolver, joinedLevel, isJoinViewEnabled);
					}
				}
			}
			final String linkPath = entryNode.hasProperty(PROPERTY_LINK)
					? entryNode.getProperty(PROPERTY_LINK).getString()
					: DITAConstants.HASH_STR;
			String title = entryNode.hasProperty(PROPERTY_NAV_TITLE)
					? entryNode.getProperty(PROPERTY_NAV_TITLE).getString()
					: StringUtils.EMPTY;
            if (StringUtils.isBlank(title) && entryNode.getPath().matches(MadisonConstants.IASBV2_CONTENT_REGEX)) {
                String currentPagePath = StringUtils.substringBefore(linkPath, MadisonConstants.HTML_EXTN);
                Resource currentPageResource = resolver.getResource(currentPagePath);
                if (null == currentPagePath) {
                    return;
                }
                Page currentPage = currentPageResource.adaptTo(Page.class);
                if (null == currentPage) {
                    return;
                }

                if (StringUtils.isNotEmpty(currentPage.getProperties().get("pageTitle",""))) {
                    title = currentPage.getProperties().get("pageTitle","");
                }
            }
            if (StringUtils.isBlank(title)) {
				title = entryNode.hasProperty(PROPERTY_LINK_TEXT)
						? entryNode.getProperty(PROPERTY_LINK_TEXT).getString()
						: StringUtils.EMPTY;
			}
			String jcrTitle = entryNode.hasProperty(PROPERTY_TITLE) ? entryNode.getProperty(PROPERTY_TITLE).getString()
					: StringUtils.EMPTY;
			if (StringUtils.isBlank(title)) {
				title = jcrTitle;
			}
			final String showTocString = entryNode.hasProperty(PROPERTY_SHOW_TOC)
					? entryNode.getProperty(PROPERTY_SHOW_TOC).getString()
					: TOC_VAL_YES;
			final Boolean showToc = showTocString.equals(TOC_VAL_NO) ? false : true;
			if (showToc && !showEntireToc) {
				showEntireToc = true;
			}
			String linkHash = StringUtils.EMPTY;
			if (!DITAConstants.HASH_STR.equalsIgnoreCase(linkPath)) {
				String link = getLink(entryNode, linkPath, resolver);
				linkHash = MadisonUtil.getPwcDocContext(link, resolver, false);
				LOGGER.debug("LinkHASH:: {}", linkHash);
			}

			String ancestryValue = StringUtils.EMPTY;
			if (entryNode.getPath().matches(MadisonConstants.FASB_CONTENT_REGEX)) {
				String regexFromExtractedMaps = getRegexFromExtractedMapsList(linkPath, level);
				if(!regexFromExtractedMaps.isEmpty()) {
					int changedMapLevel = Integer.parseInt(regexFromExtractedMaps.split("\\|")[1]);
					ancestryValue = level > changedMapLevel ? getAncestryValues(linkPath, level, resolver) : StringUtils.EMPTY;
				}else {
					ancestryValue = getAncestryValues(linkPath, level, resolver);
				}
				if (!DITAConstants.HASH_STR.equalsIgnoreCase(linkPath)) {
					String pageNameWithExt = StringUtils.substringAfterLast(linkPath, DITAConstants.FORWARD_SLASH);
					String pageName = linkPath.contains(MadisonConstants.HTML_EXTN) ? StringUtils.substringBefore(pageNameWithExt, MadisonConstants.HTML_EXTN) : StringUtils.EMPTY;
					if(StringUtils.isNotEmpty(pageName)){
						if(!regexFromExtractedMaps.isEmpty()){
							targetedLevel=5;
						}else {
							targetedLevel=4;
						}
						if(level == targetedLevel) {
							String currentPagePath = StringUtils.substringBefore(linkPath, MadisonConstants.HTML_EXTN);
							Resource currentPageResource = resolver.getResource(currentPagePath);
							if (null == currentPagePath) {
								return;
							}
							Page currentPage = currentPageResource.adaptTo(Page.class);
							if (null == currentPage) {
								return;
							}

							String longTitle = StringUtils.isNotBlank(DITAUtils.getLongTitle(currentPage, resolver)) ? DITAUtils.getLongTitle(currentPage, resolver) : jcrTitle;
							title = longTitle;
						}
					}
				}
				if (StringUtils.isNotEmpty(ancestryValue)) {
					StringBuilder titleBuilder = new StringBuilder(ancestryValue);
					titleBuilder.append(" ");
					titleBuilder.append(title);
					title = titleBuilder.toString();
				}
			}

			final Link link = new Link(entryNode.getPath(), title, level, null, entryNode.getIdentifier(), linkPath,
					showToc, linkHash);
			link.setChildLinks(childLinks);
			link.setJcrTitle(jcrTitle);
			// Set Joined section page url
			if (isJoinViewEnabled) {
				String sourcePagePath = StringUtils.substringBefore(linkPath, MadisonConstants.HTML_EXTN);
				Resource resource = resolver.getResource(sourcePagePath + FORWARD_SLASH + JcrConstants.JCR_CONTENT);
				if (null != resource && !ResourceUtil.isNonExistingResource(resource)) {
					ValueMap valueMap = resource.adaptTo(ValueMap.class);
					String joinedUrl = valueMap.containsKey(MadisonConstants.JOINED_PAGE_URL)
							? valueMap.get(MadisonConstants.JOINED_PAGE_URL, String.class)
							: StringUtils.EMPTY;
					if (StringUtils.isNotBlank(joinedUrl)) {
						link.setJoinedTocPagePath(joinedUrl);
					}
				}

			}
			currentList.add(link);
		} catch (final RepositoryException e) {
			LOGGER.error("RepositoryException in populating the TOC Links {}", e);
		} catch (Exception e) {
			LOGGER.error("Exception in populating the TOC Links {}", e);
		}

	}

	private String getAncestryValues(String linkFullPath, int level, ResourceResolver resolver) {
		LOGGER.debug("Inside getAncestryValues");
		if(Objects.isNull(linkFullPath)) {
			return null;
		}else{
			StringBuilder ancestryBuilder = new StringBuilder();
			if(StringUtils.isNotEmpty(linkFullPath)){
				try {
					String linkPath = linkFullPath.split(DITAConstants.HTML_EXT)[0];
					if (!DITAConstants.HASH_STR.equalsIgnoreCase(linkPath)) {
						Resource linkResource = resolver.getResource(linkPath);
						if(Objects.isNull(linkResource)) {
							LOGGER.error("Following resource not found", linkPath);
							return StringUtils.EMPTY;
						}
						Node currentPageNode = linkResource.adaptTo(Node.class);
						Node currentPageContentNode = currentPageNode.getNode(JcrConstants.JCR_CONTENT);

						String ascTopicNum = StringUtils.EMPTY;
						String ascSubtopicNum = StringUtils.EMPTY;
						String ascSectionNum = StringUtils.EMPTY;

						if(currentPageContentNode.hasProperty(DITAConstants.PN_ASC_TOPIC_NUMBER)){
							Property property = currentPageContentNode.getProperty(DITAConstants.PN_ASC_TOPIC_NUMBER);
							if(property.isMultiple()){
								ascTopicNum = property.getValues()[0].getString();
							}else{
								ascTopicNum = property.getString();
							}
						}

						if(currentPageContentNode.hasProperty(DITAConstants.PN_ASC_SUBTOPIC_NUMBER)){
							Property property = currentPageContentNode.getProperty(DITAConstants.PN_ASC_SUBTOPIC_NUMBER);
							if(property.isMultiple()){
								ascSubtopicNum = property.getValues()[0].getString();
							}else{
								ascSubtopicNum = property.getString();
							}
						}

						if(currentPageContentNode.hasProperty(DITAConstants.PN_ASC_SECTION_NUMBER)){
							Property property = currentPageContentNode.getProperty(DITAConstants.PN_ASC_SECTION_NUMBER);
							if(property.isMultiple()){
								ascSectionNum = property.getValues()[0].getString();
							}else{
								ascSectionNum = property.getString();
							}
						}

						// changedNumbering and excludedMaps are for handling Generic Numbering for eg. 91X
						String extractedMapString = getRegexFromExtractedMapsList(linkFullPath, level);
						boolean changedNumbering = !extractedMapString.isEmpty();
						if(changedNumbering){

							int changedMapLevel = Integer.parseInt(extractedMapString.split("\\|")[1]);

							//Below is for handling generic numbering pattern (eg. 91X)
							if (StringUtils.isNotEmpty(ascTopicNum) && level >= changedMapLevel+1 && level < changedMapLevel+4) {
								ancestryBuilder.append(ascTopicNum);
							}
							if (StringUtils.isNotEmpty(ascSubtopicNum) && level >= changedMapLevel+2 && level < changedMapLevel+4) {
								ancestryBuilder.append("-");
								ancestryBuilder.append(ascSubtopicNum);
							}
							if (StringUtils.isNotEmpty(ascSectionNum) && level >= changedMapLevel+3 && level < changedMapLevel+4) {
								ancestryBuilder.append("-");
								ancestryBuilder.append(ascSectionNum);
							}

							if(ancestryBuilder.toString().isEmpty() && (level>=changedMapLevel+1 && level<changedMapLevel+4)){
								ancestryBuilder.append(DITAUtils.getAncestryValueFromHeadNode(resolver.getResource(linkPath.split(DITAConstants.HASH_STR)[0].split(DITAConstants.HTML_EXT)[0]).adaptTo(Page.class), resolver));
							}
						}else {
							//Below is for handling common numbering pattern (ex. 805)
							if (StringUtils.isNotEmpty(ascTopicNum) && level >= 2 && level < 5) {
								ancestryBuilder.append(ascTopicNum);
							}
							if (StringUtils.isNotEmpty(ascSubtopicNum) && level >= 3 && level < 5) {
								ancestryBuilder.append("-");
								ancestryBuilder.append(ascSubtopicNum);
							}
							if (StringUtils.isNotEmpty(ascSectionNum) && level >= 4 && level < 5) {
								ancestryBuilder.append("-");
								ancestryBuilder.append(ascSectionNum);
							}
							if(ancestryBuilder.toString().isEmpty() && (level>=2 && level<5)){
								ancestryBuilder.append(DITAUtils.getAncestryValueFromHeadNode(resolver.getResource(linkPath.split(DITAConstants.HASH_STR)[0].split(DITAConstants.HTML_EXT)[0]).adaptTo(Page.class), resolver));
							}
						}
					}
				}catch (Exception e) {
					LOGGER.error("Exception while getting Ancestry values {}", e);
					return StringUtils.EMPTY;
				}
			}
			return ancestryBuilder.toString();
		}
	}

	private String getRegexFromExtractedMapsList(String linkFullPath, int level) {
		for (String regex: excludedMaps) {
			String mappath = regex.split("\\|")[0];
			int iterationMapLevel = Integer.parseInt(regex.split("\\|")[1]);
			if(linkFullPath.matches(MadisonConstants.FASB_CONTENT_REGEX)) {
				if (linkFullPath.contains(mappath) && level >= iterationMapLevel) {
					return regex;
				}
			}
		}
		return StringUtils.EMPTY;
	}

	/**
	 * Method to set 500 error code in the response status and write error response.
	 *
	 * @param response
	 * @throws IOException
	 */
	private void sendErrorResponse(final SlingHttpServletResponse response) throws IOException {
		response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
		response.getWriter().write("{}");
	}

	/**
	 * Method to get parent page link if topic head TOC entry is found
	 *
	 * @param entryNode
	 * @param linkPath
	 * @param resolver
	 * @return
	 * @throws RepositoryException
	 */
	private String getLink(Node entryNode, String linkPath, ResourceResolver resolver) throws RepositoryException {
		LOGGER.debug("Inside getLink");
		String link = StringUtils.EMPTY;
		String basePath = StringUtils.EMPTY;
		if (entryNode.hasProperty(LINK_TEXT)) {
			link = linkPath;
		} else if (StringUtils.isNotBlank(linkPath)) {
			try {
				LOGGER.debug("topic-head node found: {}", entryNode.getPath());
				String entryNodeTitle = entryNode.getProperty(TITLE).getString();
				if (linkPath.contains(MadisonConstants.HTML_EXTN)) {
					linkPath = StringUtils.substringBefore(linkPath, MadisonConstants.HTML_EXTN);
				}
				Resource linkResource = resolver.getResource(linkPath);
				if (null == linkResource) {
					return linkPath;
				}
				Node currentPageNode = linkResource.adaptTo(Node.class);
				Node currentPageContentNode = currentPageNode.getNode(JcrConstants.JCR_CONTENT);
				if (currentPageContentNode.hasProperty(DITAConstants.BASE_PATH)) {
					basePath = currentPageContentNode.getProperty(DITAConstants.BASE_PATH).getString();
				}
				link = recursivelyCheckPage(linkPath, basePath, entryNodeTitle, resolver);
				LOGGER.debug("link: {}", link);
				link = StringUtils.isBlank(link) ? linkPath : link;
			} catch (Exception e) {
				LOGGER.error("Exception while handling topic-head scenario {}", e);
				return linkPath;
			}
		}
		return link;
	}

	private String recursivelyCheckPage(String linkPath, String basePath, String entryNodeTitle,
			ResourceResolver resolver) throws RepositoryException {
		String pagePath = StringUtils.EMPTY;
		Resource linkResource = resolver.getResource(linkPath);
		if (null == linkResource) {
			return pagePath;
		}
		Node currentPageNode = linkResource.adaptTo(Node.class);
		if (!currentPageNode.getPath().equalsIgnoreCase(basePath)) {
			if (currentPageNode.hasNode(JcrConstants.JCR_CONTENT)) {
				Node contentNode = currentPageNode.getNode(JcrConstants.JCR_CONTENT);
				if (contentNode.hasProperty(JCR_TITLE)) {
					String pageTitle = contentNode.getProperty(JCR_TITLE).getString();
					if (pageTitle.equalsIgnoreCase(entryNodeTitle)) {
						pagePath = currentPageNode.getPath();
					} else {
						pagePath = recursivelyCheckPage(currentPageNode.getParent().getPath(), basePath, entryNodeTitle,
								resolver);
					}
				}
			}
		} else {
			return pagePath;
		}
		return pagePath;
	}

	@Override
	public String getChapterTocContentJson(SlingHttpServletRequest request, SlingHttpServletResponse response, int joinedLevel, boolean isJoinViewEnabled)
			throws IOException {
			Page currentChapterPage = null;
			final ResourceResolver resolver = request.getResourceResolver();
			String basePagePath = request.getResource().getPath();
			if (StringUtils.isBlank(basePagePath)) {
				LOGGER.error("basepage is blank");
				sendErrorResponse(response);
				return "{}";
			}
			Resource currentChapterTocResource = resolver.getResource(basePagePath);
            if(null != currentChapterTocResource && !ResourceUtil.isNonExistingResource(currentChapterTocResource)) {
            	currentChapterPage = currentChapterTocResource.adaptTo(Page.class);
            }
			basePagePath = getActualBasePath(resolver, basePagePath);
			//
			List<Link> tocLinks = getFullTocContent(resolver, basePagePath, joinedLevel, isJoinViewEnabled);
			final Map<String, Object> predicateMap = getPredicateMap(basePagePath);
            final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), resolver.adaptTo(Session.class));
            final SearchResult searchResult = query.getResult();

            final Iterator<Resource> resources = searchResult.getResources();
            ArrayList<Page> pageList = new ArrayList<>();
            while (resources.hasNext()) {
                final Resource resource = resources.next();
                pageList.add(resource.adaptTo(Page.class));
            }
			Map<String, Page> actualPageMap = getActualPageList(tocLinks, pageList, basePagePath, resolver);
			final Gson gson = new Gson();
			Resource chapterTocResource = resolver.getResource(basePagePath);
            if(null != chapterTocResource && !ResourceUtil.isNonExistingResource(chapterTocResource) && null != currentChapterPage) {
				final List<Link> chapterTocLinks = new ArrayList<>();
				for (Link link : tocLinks) {
					String linkTitle = link.getJcrTitle();
					String nodeLink = link.getNodeLink().replace(DITAConstants.DITA_CONTENT_ROOT_SHORT, DITAConstants.DITA_CONTENT_ROOT);
					String tocPath = null != actualPageMap.get(linkTitle) ? actualPageMap.get(linkTitle).getPath() : StringUtils.EMPTY;
					if (nodeLink.contains(currentChapterPage.getPath())) {
						final Link currentChapter = new Link(link.getPath(), link.getTitle(), link.getLevel(), null,
								link.getPageName(), link.getNodeLink(), link.getToc(), link.getPathHash());
						currentChapter.setChildLinks(link.getChildLinks());
						currentChapter.setHasChildren(null != link.getChildLinks() && !link.getChildLinks().isEmpty());
						currentChapter.setTocPath(tocPath);
						chapterTocLinks.add(currentChapter);
					} else {
						final Link notCurrentChapter = new Link(link.getPath(), link.getTitle(), link.getLevel(), null,
								link.getPageName(), link.getNodeLink(), link.getToc(), link.getPathHash());
						notCurrentChapter.setTocPath(tocPath);
						notCurrentChapter.setHasChildren(null != link.getChildLinks() && !link.getChildLinks().isEmpty());
						chapterTocLinks.add(notCurrentChapter);
					}
				}
				if (!chapterTocLinks.isEmpty()) {
					final Link tocParent = new Link(null, null, null, null, null, null, showEntireToc, null);
					tocParent.setChildLinks(chapterTocLinks);
					String json = gson.toJson(tocParent);
					response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            		response.getWriter().write(gson.fromJson(json, JsonObject.class).toString());
				} else {
					LOGGER.error("Chapter toc is not available");
    				sendErrorResponse(response);
    				response.getWriter().write("{}");
    				return "{}";
				}
            }
		return "{}";
	}

    private String getActualBasePath(ResourceResolver resolver, String basePagePath) {
    	Resource resource = resolver.getResource(basePagePath);
    	Session session = resolver.adaptTo(Session.class);
    	if(null != resource && !ResourceUtil.isNonExistingResource(resource)) {
    		Page page = resource.adaptTo(Page.class);
    		if(null != page) {
    			return getBasePath(session, page);
    		}
    	}
		return null;
	}

    private String getBasePath(Session session, Page page) {
        if (null != page) {
			try {
				if (session.nodeExists(page.getPath() + TOC_NODE)) {
					return page.getPath();
				}
			} catch (RepositoryException e) {
				LOGGER.error("RepositoryException occured in getBasePath method {}", e);
			}
		}
		return getBasePath(session, page.getParent());
    }

	private Map<String, Object> getPredicateMap(final String outputPath) {

        final Map<String, Object> predicateMap = new HashMap<>();
        predicateMap.put(P_LIMIT, P_LIMIT_VALUE);
        predicateMap.put(PATH, outputPath);
        predicateMap.put(PN_TYPE, CQ_PAGE);

        return predicateMap;
    }

	@Override
    public void createAndUpdateJoinedSectionPage(ResourceResolver resolver, String basePath, int joinedLevel, Map<Integer, String> overrideJoinMap, boolean isJoinViewEnabled) {
		long startTime = System.currentTimeMillis();
		List<Link> fullTocContentList = getFullTocContent(resolver, basePath, joinedLevel, isJoinViewEnabled);
		// Clear the join url from page if already set
		clearJoin(fullTocContentList, resolver, new HashSet<>());
		setJoinedTocPage(fullTocContentList, joinedLevel, resolver, overrideJoinMap, 0, new ArrayList<>());
    	long endTime = System.currentTimeMillis();
    	LOGGER.debug("Madision - Creation and update for joined section page took {} seconds to complete the process", (endTime - startTime) / 1000);
	}

    private void clearJoin(List<Link> childLinks, ResourceResolver resolver, Set<String> setOfPages) {
    	for(Link link : childLinks) {
			String sourcePagePath = getFullPagePath(link.getNodeLink());
			if (StringUtils.isNotBlank(sourcePagePath) && !setOfPages.contains(sourcePagePath)) {
				Resource resource = resolver.getResource(sourcePagePath);
				if (null != resource && !ResourceUtil.isNonExistingResource(resource)) {
					Page sourcePage = resource.adaptTo(Page.class);
					if (null != sourcePage) {
						setOfPages.add(sourcePagePath);
						LOGGER.debug("Clear Join Method, Source Page Path is- {}", sourcePagePath);
						if (null != sourcePage.getContentResource()) {
							ModifiableValueMap sourcePageValueMap = sourcePage.getContentResource()
									.adaptTo(ModifiableValueMap.class);
							if (sourcePageValueMap.containsKey(MadisonConstants.JOINED_PAGE_URL)) {
								sourcePageValueMap.remove(MadisonConstants.JOINED_PAGE_URL);
							}
							if (sourcePageValueMap.containsKey(MadisonConstants.JOINED_SECTION_LEVEL)) {
								sourcePageValueMap.remove(MadisonConstants.JOINED_SECTION_LEVEL);
							}
						}
					}
					LOGGER.debug("Clear Join Method, done");
				}
			}
			if(null != link.getChildLinks()) {
    			clearJoin(link.getChildLinks(), resolver, setOfPages);
    		}
    	}

	}

	private void setJoinedTocPage(List<Link> childLinks, int joinedLevel, ResourceResolver resolver,
			Map<Integer, String> overrideJoinMap, int actualJoinLevel, List<String> subLevelList) {
		if (null != childLinks) {
			for (int i = 0; i < childLinks.size(); i++) {
				Link link = childLinks.get(i);
				// Reset the level for each chapter to the global
				if (link.getLevel() == 1) {
					actualJoinLevel = joinedLevel;
					subLevelList.clear();
				}
				if (overrideJoinMap.containsKey(i + 1) && link.getLevel() == 1) {
					String levelAndpath = overrideJoinMap.get(i + 1);
					try {
						if (levelAndpath.contains(MadisonConstants.COMMA_SEPARATOR)) {
							String[] subLevels = levelAndpath.split(MadisonConstants.COMMA_SEPARATOR);
							subLevelList = new ArrayList<>(Arrays.asList(subLevels));
						} else if (levelAndpath.contains(MadisonConstants.FORWARD_SLASH)) {
							subLevelList.add(levelAndpath);
						} else {
							actualJoinLevel = Integer.parseInt(levelAndpath);
						}
					} catch (NumberFormatException e) {
						LOGGER.error(
								"For level wrong number provided in string format, cannot convert from String to Integer  {}",
								e.getMessage());
					}
				}
				// Different join levels within a chapter case
				if (!subLevelList.isEmpty()) {
					actualJoinLevel = 0;
					for (String listItem : subLevelList) {
						String path = listItem.split(MadisonConstants.HASH)[0];
						path = path.contains(DITAConstants.DITA_CONTENT_ROOT)
								? path.replace(DITAConstants.DITA_CONTENT_ROOT, DITAConstants.DITA_CONTENT_ROOT_SHORT)
								: path;
						if (link.getNodeLink().contains(path) && listItem.endsWith(Integer.toString(link.getLevel()))) {
							actualJoinLevel = link.getLevel();
							break;
						}
					}
				}

				if (link.getLevel() == actualJoinLevel && actualJoinLevel > 0) {
					// Create joined page and set joined page path in source page
					createJoinedPage(link.getChildLinks(), link.getNodeLink(), resolver, actualJoinLevel, link.getTitle());
				} else {
					setJoinedTocPage(link.getChildLinks(), joinedLevel, resolver, overrideJoinMap, actualJoinLevel, subLevelList);
				}
			}
		}
	}

	private void createJoinedPage(List<Link> links, String linkPath, ResourceResolver resolver, int joinedLevel, String joinViewTitle) {
		String sourcePagePath = getFullPagePath(linkPath);
		Resource resource = resolver.getResource(sourcePagePath);
		if(null != resource && !ResourceUtil.isNonExistingResource(resource)) {
			Page sourcePage = resource.adaptTo(Page.class);
			PageManager pageManager = resolver.adaptTo(PageManager.class);
			try {
				// If joined page already exist, delete it.
				Page page = pageManager.getContainingPage(sourcePagePath + DITAConstants.JOINED);
				if(null != page) {
					pageManager.delete(page, false, true);
				}
				long startTime = System.currentTimeMillis();
				Page joinedPage = pageManager.copy(sourcePage, sourcePagePath + DITAConstants.JOINED, null, true, true, true);
				LOGGER.debug("Copy operation took {} Milliseconds to complete the process", (System.currentTimeMillis() - startTime));
				//Set page meta tags where the joined is enabled
				ModifiableValueMap sourcePageValueMap = sourcePage.getContentResource().adaptTo(ModifiableValueMap.class);
				sourcePageValueMap.put(MadisonConstants.JOINED_PAGE_URL, joinedPage.getPath()+MadisonConstants.HTML_EXTN);
				sourcePageValueMap.put(MadisonConstants.JOINED_SECTION_LEVEL, joinedLevel);
				// Make joined page hidden from site search
				ModifiableValueMap joinedPageValueMap = joinedPage.getContentResource().adaptTo(ModifiableValueMap.class);
				joinedPageValueMap.put(DITAConstants.META_PWC_HIDDEN_FROM_SITE_SEARCH, TRUE);
				// Update the title
				joinedPageValueMap.put(JCR_TITLE, joinViewTitle);
				joinedPageValueMap.put(PAGE_TITLE, joinViewTitle);
				//Merge the content of child pages to the joined page
				long startTime1 = System.currentTimeMillis();
				mergeContent(links, joinedPage, resolver, pageManager, sourcePagePath, joinedLevel);
				LOGGER.debug("Madision - Merging the child pages content to joined page took {} seconds to complete the process", (System.currentTimeMillis() - startTime1) / 1000);
			} catch (WCMException e) {
				LOGGER.error("WCMException occured in createJoinedPage of TocContentServiceImpl {}", e.getMessage());
			}
		}
	}

	private String getFullPagePath(String linkPath) {
		String linkFullPath = linkPath.contains(DITAConstants.DITA_CONTENT_ROOT_SHORT) ? linkPath.replace(DITAConstants.DITA_CONTENT_ROOT_SHORT, DITAConstants.DITA_CONTENT_ROOT) : linkPath;
		return StringUtils.substringBefore(linkFullPath, MadisonConstants.HTML_EXTN);
	}

	private void mergeContent(List<Link> links, Page joinedPage, ResourceResolver resolver, PageManager pageManager, String sourcePagePath, int joinedLevel) {
		Resource resource = resolver.getResource(joinedPage.getPath() + CONTENTBODY_TOPICBODY);
		if(null != resource && !ResourceUtil.isNonExistingResource(resource)) {
			Node topicBodyNode = resource.adaptTo(Node.class);
			try {
				Resource sourceTopicResource = null;
				Set<String> setOfPagePaths = new HashSet<>();
				List<String> joinedTopicPaths = new ArrayList<>();
				setOfPagePaths.add(sourcePagePath);// This page content is already cloned
				Resource pwcTopicResource = getTopicResource(joinedPage.getContentResource().getPath(), resolver);
				// Get the topic node name
				String topicNodeName = null != pwcTopicResource ? pwcTopicResource.getName() : StringUtils.EMPTY;
				if(!topicBodyNode.hasNode(DITA_NODE)) {
					Node ditaNode = topicBodyNode.addNode(DITA_NODE);
					ditaNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, FMDITA_COMPONENTS_DITA_DELEGATOR);
					ditaNode.setProperty("outputclassflag", "false");
					ditaNode.setProperty("class", DITA_NODE);
					if(null != pwcTopicResource && !ResourceUtil.isNonExistingResource(pwcTopicResource)) {
						String destiNationPath = joinedPage.getPath() + CONTENTBODY_TOPICBODY+SLASH_DITA_SLASH + topicNodeName;
						long startTime = System.currentTimeMillis();
						Resource pwcTopicResource1 = pageManager.move(pwcTopicResource, destiNationPath, null, false, true, null);
						LOGGER.debug("Move operation took {} Milliseconds to complete the process", (System.currentTimeMillis() - startTime));
						sourceTopicResource = pwcTopicResource1;
						if(null != links) {
							copyChildPagesContent(links, pwcTopicResource1, pageManager, resolver, setOfPagePaths, joinedTopicPaths, joinedPage.getPath(), joinedLevel);
						}
					}
				} else {
					Resource pwcTopicRes = resolver.getResource(joinedPage.getPath() + CONTENTBODY_TOPICBODY+SLASH_DITA_SLASH + topicNodeName);
					sourceTopicResource = pwcTopicRes;
					if(null != pwcTopicRes && null != links && !ResourceUtil.isNonExistingResource(pwcTopicRes)) {
						copyChildPagesContent(links, pwcTopicRes, pageManager, resolver, setOfPagePaths, joinedTopicPaths, joinedPage.getPath(), joinedLevel);
					}
				}
				if(null != sourceTopicResource && !ResourceUtil.isNonExistingResource(sourceTopicResource) && !joinedTopicPaths.isEmpty()) {
					Node sourceTopicNode = sourceTopicResource.adaptTo(Node.class);
					Node joinedNode = sourceTopicNode.addNode(TOC_JOINED);
					joinedNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, PWC_MADISON_COMPONENTS_DITACONTENT_TOC_JOINED);
					joinedNode.setProperty("joinedTopics", StringUtils.join(joinedTopicPaths, ","));
				}
			} catch (ItemExistsException e) {
				LOGGER.error("ItemExistsException while merging the content {}", e.getMessage());
			} catch (PathNotFoundException e) {
				LOGGER.error("PathNotFoundException while merging the content {}", e.getMessage());
			} catch (VersionException e) {
				LOGGER.error("VersionException while merging the content {}", e.getMessage());
			} catch (ConstraintViolationException e) {
				LOGGER.error("ConstraintViolationException while merging the content {}", e.getMessage());
			} catch (LockException e) {
				LOGGER.error("LockException while merging the content {}", e.getMessage());
			} catch (ValueFormatException e) {
				LOGGER.error("ValueFormatException while merging the content {}", e.getMessage());
			} catch (RepositoryException e) {
				LOGGER.error("RepositoryException while merging the content {}", e.getMessage());
			} catch (WCMException e) {
				LOGGER.error("WCMException while merging the content {}", e.getMessage());
			}
		}

	}

	private void copyChildPagesContent(List<Link> links, Resource pwcTopicResource,
			PageManager pageManager, ResourceResolver resolver, Set<String> setOfPagePaths, List<String> joinedTopicPaths, String joinPagePath, int joinedLevel) {
		for(int i = 0; i < links.size(); i++) {
			Link link = links.get(i);
			String pagePath = getFullPagePath(link.getNodeLink());
			LOGGER.debug("copyChildPagesContent, Node Link Path is {}", link.getNodeLink());
			LOGGER.debug("copyChildPagesContent, Page Path is {}", pagePath);
			Resource pageResource = resolver.getResource(pagePath);
			if(StringUtils.isNotBlank(pagePath) && null != pageResource && !ResourceUtil.isNonExistingResource(pageResource) && !setOfPagePaths.contains(pageResource.getPath())) {
				setOfPagePaths.add(pageResource.getPath());
				Page sourcePage = pageResource.adaptTo(Page.class);
				if (null != sourcePage && null != sourcePage.getContentResource()) {
					ModifiableValueMap sourcePageValueMap = sourcePage.getContentResource()
							.adaptTo(ModifiableValueMap.class);
					sourcePageValueMap.put(MadisonConstants.JOINED_PAGE_URL, joinPagePath + MadisonConstants.HTML_EXTN);
					sourcePageValueMap.put(MadisonConstants.JOINED_SECTION_LEVEL, joinedLevel);
					// Get resource to be copied
					Resource resource1 = getTopicResource(sourcePage.getContentResource().getPath(), resolver);
					if (null != resource1) {
						joinedTopicPaths.add(resource1.getPath());
						LOGGER.debug("copyChildPagesContent, Joined Topic Path is {}", resource1.getPath());
					}
				}
			}
			if(null != link.getChildLinks()) {
					copyChildPagesContent(link.getChildLinks(), pwcTopicResource, pageManager, resolver, setOfPagePaths, joinedTopicPaths, joinPagePath, joinedLevel);
			}

		}

	}

	private Resource getTopicResource(String sourcePagePath, ResourceResolver resolver) {
		final Map<String, Object> predicateMap = new HashMap<>();
		long startTime = System.currentTimeMillis();
        predicateMap.put(P_LIMIT, P_LIMIT_VALUE);
        predicateMap.put(PATH, sourcePagePath);
        predicateMap.put("property", JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
        predicateMap.put("property.value", FMDITA_COMPONENTS_DITA_TOPIC);

        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        List<Resource> resList = Lists.newArrayList(resources);
        resList.sort((Resource res1, Resource res2) -> ((Integer)res1.getPath().split("/").length).compareTo((Integer)res2.getPath().split("/").length));
        LOGGER.debug("Retreiving topic node under {} took {} seconds to complete the process",sourcePagePath, (System.currentTimeMillis() - startTime) / 1000);
		return resList.isEmpty() ? null : resList.get(0);
	}

    public List<String> getFasbNumberingConfig(String resourcePath, ResourceResolver resolver){
		Resource resource = resolver.getResource(resourcePath);
		if (resource != null) {
			ConfigurationBuilder configurationBuilder = resource.adaptTo(ConfigurationBuilder.class);
			if(configurationBuilder != null){
				TocContentServiceConfig tocContentServiceConfig = configurationBuilder.as(TocContentServiceConfig.class);
				LOGGER.debug(Arrays.toString(tocContentServiceConfig.excludedMaps()));
				return Arrays.asList(tocContentServiceConfig.excludedMaps());
			}
		}

		return null;
	}
}
