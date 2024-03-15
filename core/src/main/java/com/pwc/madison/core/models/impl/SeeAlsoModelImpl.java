package com.pwc.madison.core.models.impl;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.pwc.madison.core.util.DITALinkUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.beans.UsedInReferences;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.SeeAlsoModel;

@Model(adaptables = SlingHttpServletRequest.class,
        resourceType = SeeAlsoModelImpl.RESOURCE_TYPE, adapters = SeeAlsoModel.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SeeAlsoModelImpl implements SeeAlsoModel {

	private static final String SLASH_JCR_CONTENT = "/jcr:content";
	private static final String HTML = ".html";
	public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/see-also";
	private static final Logger LOGGER = LoggerFactory.getLogger(SeeAlsoModelImpl.class);

	@ScriptVariable
    private Page currentPage;
    
    @SlingObject
    ResourceResolver resourceResolver;
    
    @ValueMapValue
    private String[] usedInTopics;

	@ValueMapValue
	private String[] seeAlsoContentTypes;
	@OSGiService
	private QueryBuilder queryBuilder;

	@ValueMapValue
    private boolean overrideSeeAlsoEnabled;
    private List<String> allReferencesList = new ArrayList<>();
    
    private String basePath;
    private ValueMap pageContentValueMap;
    private int maxItemsToDisplay = 0;
    private boolean seeAlsoEnabledAtPublishingPoint = false;

    @PostConstruct
    protected void init() {
		if (null != currentPage) {
			pageContentValueMap = currentPage.getContentResource().getValueMap();
            basePath = pageContentValueMap.get("basePath", String.class);
            if (StringUtils.isNotBlank(basePath)) {
				Resource baseContentResource = resourceResolver.getResource(basePath + DITAConstants.FORWARD_SLASH + JcrConstants.JCR_CONTENT);
				if (null != baseContentResource && !ResourceUtil.isNonExistingResource(baseContentResource)) {
					ValueMap valueMap = baseContentResource.adaptTo(ValueMap.class);
					Long maxDisplayCount = valueMap.get(DITAConstants.META_PWC_SEE_ALSO_MAX_DISPLAY_COUNT, Long.class);
					maxItemsToDisplay = null != maxDisplayCount ? maxDisplayCount.intValue() : 0;
					String seeAlsoEnable = valueMap.get(DITAConstants.META_PWC_SEE_ALSO_ENABLED, String.class);
					seeAlsoEnabledAtPublishingPoint = StringUtils.isNotBlank(seeAlsoEnable) && "yes".equalsIgnoreCase(seeAlsoEnable)? true : false;
				}
				allReferencesList = getAllPageReferences();
			}
        }
    }
    
    private List<String> getAllPageReferences() {
		List<String> references = new ArrayList<>();
		long startTime1 = System.currentTimeMillis();
		if(seeAlsoEnabledAtPublishingPoint && null!=usedInTopics) {
			List<String> usedInList =Arrays.asList(usedInTopics);
			if (null!=usedInList && !usedInList.isEmpty()) {
				// Remove duplicates if any
				Set<String> usedInListSet = new HashSet<String>(usedInList);
				// Filter out joined pages and pages whose source is not ss_pwc and private group content
				List<String> filteredList = usedInListSet.stream().filter(item -> {
					Resource pageResource = resourceResolver.getResource(item.split(HTML)[0] + SLASH_JCR_CONTENT);
					if(null!=pageResource) {
						ValueMap contentValueMap = pageResource
								.adaptTo(ValueMap.class);
						boolean isSourcePwC = false;
						if (contentValueMap.containsKey(DITAConstants.META_STANDARD_SETTERS) && "ss_pwc"
								.equals(contentValueMap.get(DITAConstants.META_STANDARD_SETTERS, String.class))) {
							isSourcePwC = true;
						}
						boolean isNonPrivateGroup = true;
						if (contentValueMap.containsKey(DITAConstants.META_AUDIENCE) && "privateGroup"
								.equals(contentValueMap.get(DITAConstants.META_AUDIENCE, String.class))) {
							isNonPrivateGroup = false;
						}
						return !item.contains("-joined") && isSourcePwC && isNonPrivateGroup;
					} else {
						return false;
					}
				}).collect(Collectors.toList());

				LOGGER.debug("Madision - Getting page references are {}", filteredList.toString());

				return filteredList;
			}
		}
		LOGGER.debug("Madision - Getting page references took {} seconds to complete the process", (System.currentTimeMillis() - startTime1) / 1000);

		return references;
	}
    
    public List<UsedInReferences> getRequiredPageReferencesList(boolean isExternalLinkRequired) {
		List<UsedInReferences> referencesList = new ArrayList<>();
		if (!allReferencesList.isEmpty()) {
			List<String> requiredList = new ArrayList<>();
			// Filter out joined pages and pages whose source is not ss_pwc and private group content
			if (isExternalLinkRequired) {
				requiredList = allReferencesList.stream().filter(item -> {
					ValueMap contentValueMap = resourceResolver.getResource(item.split(HTML)[0] + SLASH_JCR_CONTENT)
							.adaptTo(ValueMap.class);
					boolean isExternalLink = false;
					if ((contentValueMap.containsKey(DITAConstants.META_AUDIENCE)
							&& "internalExternal".equals(contentValueMap.get(DITAConstants.META_AUDIENCE, String.class))) || (contentValueMap.containsKey(DITAConstants.META_AUDIENCE)
							&& "externalOnly".equals(contentValueMap.get(DITAConstants.META_AUDIENCE, String.class)))) {
						isExternalLink = true;
					}
					return isExternalLink;
				}).collect(Collectors.toList());
			} else {
				requiredList = allReferencesList;
			}

			// Sort the list based on publications date
			requiredList.sort((path1, path2) -> {
				ValueMap valueMap1 = resourceResolver.getResource(path1.split(HTML)[0] + SLASH_JCR_CONTENT).adaptTo(ValueMap.class);
				ValueMap valueMap2 = resourceResolver.getResource(path2.split(HTML)[0] + SLASH_JCR_CONTENT).adaptTo(ValueMap.class);
				Date date1 = valueMap1.get(DITAConstants.META_REVISION_DATE, Date.class)!=null ? valueMap1.get(DITAConstants.META_REVISION_DATE, Date.class) : valueMap1.get(DITAConstants.META_PUBLICATION_DATE, Date.class);
				Date date2 = valueMap2.get(DITAConstants.META_REVISION_DATE, Date.class)!=null ? valueMap2.get(DITAConstants.META_REVISION_DATE, Date.class) : valueMap2.get(DITAConstants.META_PUBLICATION_DATE, Date.class);
				return Long.compare(date2.getTime(), date1.getTime());
			});

			List<String> finalInternalList = new ArrayList<>();
			if(overrideSeeAlsoEnabled) {
				finalInternalList = requiredList;
			} else {
				finalInternalList = requiredList.size() > maxItemsToDisplay ? requiredList.subList(0, maxItemsToDisplay) : requiredList;
			}
			// Add to the list
	        for(String path : finalInternalList) {
				String title = "";
				ValueMap contentValueMap = resourceResolver.getResource(path.split(HTML)[0] + SLASH_JCR_CONTENT).adaptTo(ValueMap.class);
				//chunked to content case
				if(contentValueMap.containsKey(DITAConstants.CHUNKED_TOPIC_PATHS)){
					Map<String, String> map = new HashMap<>();
					map.put("path", path.split(HTML)[0]);
					map.put("property", "id");
					String anchorId = StringUtils.isNotBlank(path.split(HTML)[1]) ? path.split(HTML)[1].split("#")[1] : "" ;
					map.put("property.value", anchorId);

					Query query = queryBuilder.createQuery(PredicateGroup.create(map), resourceResolver.adaptTo(Session.class));
					SearchResult searchResult = query.getResult();

					try {
						if (searchResult.getHits().size() > 0) {
							Resource resource = searchResult.getHits().get(0).getResource();
							Node node = resource.adaptTo(Node.class);
							if (node.hasNode("title/_text")) {
								Node textNode = node.getNode("title/_text");
								if (textNode.hasProperty("text")) {
									title = textNode.getProperty("text").getString();
								}
							}
						}
					} catch(RepositoryException e){
						LOGGER.error("Madison - Error in fetching title node for chunked to content case : url = {}", path);
					}
				}
				if(StringUtils.isBlank(title)) {
					title = contentValueMap.get("jcr:title", String.class);
				}
	        	String publicationDate = contentValueMap.get(DITAConstants.META_PUBLICATION_DATE, String.class);
	        	String contentId = contentValueMap.get(DITAConstants.META_CONTENT_ID, String.class);
	        	String link = path.contains(HTML) ? path : path + HTML;
	        	UsedInReferences usedInReferences = new UsedInReferences();
	        	usedInReferences.setTitle(title);
	        	usedInReferences.setPublicationDate(publicationDate);
	        	usedInReferences.setContentId(contentId);
	        	usedInReferences.setPath(link);
	        	referencesList.add(usedInReferences);
	        }
		}
        return referencesList;
    }

	@Override
    public List<UsedInReferences> getAllPageReferencesList() {

		return getRequiredPageReferencesList(false);
    }

	@Override
    public List<UsedInReferences> getExternalReferencesList() {

		return getRequiredPageReferencesList(true);
    }
    
    @Override
    public boolean isSeeAlsoEnabledAtPublishingPoint() {
		return this.seeAlsoEnabledAtPublishingPoint;
    }

}
