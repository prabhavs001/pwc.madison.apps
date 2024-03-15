package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.TocModel;
import com.pwc.madison.core.services.TocContentService;
import com.pwc.madison.core.util.MadisonUtil;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = TocModel.class,
       resourceType = TocModelImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
          extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class TocModelImpl implements TocModel {

    private static final String WHITESPACES_REGEX = "\\s";
	protected static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/toc";
    private static final String PN_TOC = "toc";
    private static final String TOC_VAL_YES = "yes";
    private static final String CHAPTER_PATHS = "chapterPaths";
    private static final Logger LOGGER = LoggerFactory.getLogger(TocModelImpl.class);
    private final String entry = "/entry";
    Session session = null;
    @SlingObject
    ResourceResolver resourceResolver;
    private String tocEntry = "/jcr:content/toc";
    @ScriptVariable
    private Page currentPage;

    private String basePath;
    
    @OSGiService
    private TocContentService tocContentService;

    @PostConstruct
    protected void init() {
        session = resourceResolver.adaptTo(Session.class);
        basePath = StringUtils.EMPTY;
        if (null != currentPage) {
            final ValueMap properties = currentPage.getContentResource().getValueMap();
            basePath = properties.get("basePath", String.class);
            LOGGER.debug("basePath is {} ", basePath);
        }
    }

    @Override
    public String getBasePath() {
        return basePath;
    }
    
    @Override
    public String getChapterTocBasePath() {
    	String chapterTocBasePath = StringUtils.EMPTY;
    	final long t1 = System.currentTimeMillis();
    	Resource baseContentResource = resourceResolver.getResource(basePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
		if (null != baseContentResource && !ResourceUtil.isNonExistingResource(baseContentResource)) {
			ValueMap baseValueMap = baseContentResource.adaptTo(ValueMap.class);
			String chapters = baseValueMap.get(CHAPTER_PATHS, String.class);
			if(StringUtils.isNotBlank(chapters)) {
				final Gson gson = new Gson();
				Map<String, String> chaptersMap = gson.fromJson(chapters, new TypeToken<LinkedHashMap<String, String>>() {}.getType());
				chapterTocBasePath = getChapterPath(chaptersMap);
			}
		}
        LOGGER.debug("chapter TocBase Path is  {} ", chapterTocBasePath);
        LOGGER.debug(String.format("Total time taken in milliseconds is %d for finding the chapter toc base path", System.currentTimeMillis() - t1));
        return StringUtils.isBlank(chapterTocBasePath) ? basePath : chapterTocBasePath;
    }
    
    private String getChapterPath(Map<String, String> chaptersMap) {
    	String path = StringUtils.EMPTY;
		if(null != chaptersMap && !chaptersMap.isEmpty()) {
			String[] array = currentPage.getPath().replace(basePath+"/", "").split("/");
			PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
			Page tempPage = pageManager.getContainingPage(basePath+"/"+array[0]);
	    	for(int i = 0; i <= array.length ; i++) {
	    		if (null != tempPage) {
					String title = MadisonUtil.getPageTitle(tempPage);
					String chapterPath = getPathFromMap(title, chaptersMap);
					if (StringUtils.isNotBlank(chapterPath)) {
						path = chapterPath;
						break;
					}
					if(i+1 < array.length) {
						tempPage = pageManager.getContainingPage(tempPage.getPath()+"/"+array[i+1]);
					}
				}
	    	}
	    	if(StringUtils.isBlank(path)) {
				Optional<Entry<String, String>> firstEntry = chaptersMap.entrySet().stream().findFirst();
				if(firstEntry.isPresent()) {
					Entry<String, String> firstItem = firstEntry.get();
					path = firstItem.getValue();
				}
			}
		}
		return path;
	}

	private String getPathFromMap(String title, Map<String, String> chaptersMap) {
		String path = StringUtils.EMPTY;
		String titleWithoutSapces = title.replaceAll(WHITESPACES_REGEX, StringUtils.EMPTY);
		for (Map.Entry<String, String> entry : chaptersMap.entrySet()) {
			String key = entry.getKey();
			String keyWithoutSpaces = key.replaceAll(WHITESPACES_REGEX, StringUtils.EMPTY);
			if(titleWithoutSapces.equals(keyWithoutSpaces)) {
				path = entry.getValue();
				break;
			}
		}
		return path;
	}

	@Override
    public boolean isChapterToc() {
        boolean isChapterToc = false;
		if (StringUtils.isNotBlank(basePath)) {
			Resource baseContentResource = resourceResolver.getResource(basePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
			if (null != baseContentResource && !ResourceUtil.isNonExistingResource(baseContentResource)) {
				ValueMap valueMap = baseContentResource.adaptTo(ValueMap.class);
				if (TOC_VAL_YES.equals(valueMap.get(DITAConstants.META_LOAD_LITE_TOC, String.class))) {
					isChapterToc = true;
				}
			}
		}
        return isChapterToc;
    }

    @Override
    public Boolean showToc() {
        Boolean showToc = false;
        try {
            if (null != session && StringUtils.isNotBlank(basePath) && session.nodeExists(basePath + tocEntry)) {
                Node rootNode = null;
                rootNode = session.getNode(basePath + tocEntry);
                if (rootNode.getNodes().getSize() <= 1 && session.nodeExists(basePath + tocEntry + entry)) {
                    rootNode = session.getNode(basePath + tocEntry + entry);
                    tocEntry = tocEntry + entry;
                    if (session.nodeExists(basePath + tocEntry + entry)) {
                        if (rootNode.hasProperty(PN_TOC)) {
                            String tocVal = rootNode.getProperty(PN_TOC).getString();
                            if (tocVal.equals(TOC_VAL_YES)) {
                                showToc = true;
                            } else {
                                if (session.nodeExists(basePath + tocEntry + entry)) {
                                    showToc = setTocFlag(rootNode, showToc);
                                }
                            }
                        } else {
                            showToc = true;
                        }
                    }
                } else {
                    showToc = setTocFlag(rootNode, showToc);
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("TocModelImpl: RepositoryException occurred in showToc method {}", e.getMessage());
        }
        return showToc;
    }

    private Boolean setTocFlag(Node rootNode, Boolean showToc) {
        NodeIterator nodeIterator = null;
        try {
            nodeIterator = rootNode.getNodes();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                if (node.hasProperty(PN_TOC)) {
                    String tocVal = node.getProperty(PN_TOC).getString();
                    if (tocVal.equals(TOC_VAL_YES)) {
                        showToc = true;
                    }
                } else if (!(node.getName().equals(DITAConstants.REP_POLICY_NODE))) {
                    showToc = true;
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("TocModelImpl: RepositoryException occurred in setTocFlag method {}", e.getMessage());
        }
        return showToc;
    }
    
    private String getChapterPath(List<Link> fullTocList, String basePath) {
    	String chapterPath = StringUtils.EMPTY;
        for(Link link : fullTocList) {
        	String path = getPath(link.getJcrTitle(), currentPage, basePath);
        	if(StringUtils.isNotBlank(path)) {
        		chapterPath = path;
        		break;
        	}
        }
        return chapterPath;
    }
    
    private String getPath(String title, Page page, String basePath) {
    	String path = StringUtils.EMPTY;
    	List<Page> pageList = new ArrayList<>();
    	String[] array = page.getPath().replace(basePath, "").split("/");
    	Page tempPage = page;
    	for(int i = 0; i <= array.length ; i++) {
    		if(null != tempPage && null != tempPage.getTitle() && tempPage.getTitle().equals(title)) {
    			pageList.add(tempPage);
    		}
    		if (null != tempPage) {
				Page parent = tempPage.getParent();
				tempPage = parent;
			}
    	}
    	if(!pageList.isEmpty()) {
    		pageList.sort((p1, p2) -> { return (p1.getPath().length() - p2.getPath().length());});
    		path = pageList.get(0).getPath();
    	}
		return path;
    }
}
