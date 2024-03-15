package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import com.pwc.madison.core.util.DITALinkUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.google.common.collect.Lists;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.services.SeeAlsoService;

@Component(service = SeeAlsoService.class, immediate = true)
public class SeeAlsoServiceImpl implements SeeAlsoService {
	
	private static final String SLASH_JCR_CONTENT = "/jcr:content";

	private static final String SEE_ALSO_RESOURCE_TYPE = "pwc-madison/components/ditacontent/see-also";

	private static final String SEE_ALSO = "see-also";

	private static final Logger LOGGER = LoggerFactory.getLogger(SeeAlsoServiceImpl.class);
	
	@Reference
    private QueryBuilder queryBuilder;
	
	@Override
	public void addSeeAlsoSection(ResourceResolver resolver, String sourcePath) {
		long startTime1 = System.currentTimeMillis();
		List<Resource> pageResourceList = getPageResources(sourcePath, resolver);
		
		LOGGER.debug("inside addSeeAlsoSection method of SeeAlsoServiceImpl");
		for(Resource pageRes : pageResourceList) {
			String rootPath = pageRes.getPath() + SLASH_JCR_CONTENT;
			Resource pageContentRes = pageRes.getChild("jcr:content");
			if(null != pageContentRes && !ResourceUtil.isNonExistingResource(pageContentRes)) {
				ValueMap pageContentValueMap = pageContentRes.adaptTo(ValueMap.class);
				boolean overrideSeeAlsoEnabled = isOverrideSeeAlsoEnabled(pageContentValueMap);
				String[] seeAlsoContentTypes = getSeeAlsoContentTypes(pageContentValueMap);
				List<String> chunkedTopicsList = new ArrayList<>();
				if(pageContentValueMap.containsKey(DITAConstants.CHUNKED_TOPIC_PATHS)) {
					String[] chunkedTopics = pageContentValueMap.get(DITAConstants.CHUNKED_TOPIC_PATHS, String[].class);
					chunkedTopicsList = null != chunkedTopics ? Arrays.asList(chunkedTopics) : new ArrayList<>();
				}
				List<String> usedInList = new ArrayList<>();
				if(overrideSeeAlsoEnabled) {
					if(pageContentValueMap.containsKey(DITAConstants.META_PWC_SEE_ALSO_USED_IN)) {
						String[] usedInArray = pageContentValueMap.get(DITAConstants.META_PWC_SEE_ALSO_USED_IN, String[].class);
						usedInList = Arrays.asList(usedInArray);
					}
				} else {
	    			if (pageContentValueMap.containsKey(DITAConstants.META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS)) {
	    				String[] defaultUsedInArray  = pageContentValueMap.get(DITAConstants.META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS, String[].class);
	    				usedInList = Arrays.asList(defaultUsedInArray);
					}
				}
				String[] usedInArray = usedInList.toArray(new String[0]);
				
				final String query = "SELECT * FROM [nt:unstructured] AS topic WHERE ISDESCENDANTNODE(topic , '" + rootPath
		                + "') AND topic.[sling:resourceType] = '" + DITAConstants.DITA_TOPIC_RESOURCE_TYPE + "'";
		        final Iterator<Resource> result = resolver.findResources(query, javax.jcr.query.Query.JCR_SQL2);
		        List<String> pathList = new ArrayList<>();
		        while (result.hasNext()) {
		            Resource topicResource = result.next();
		            if(!chunkedTopicsList.isEmpty()) {
		            	ValueMap titleNodeValueMap = getTitleNodeProperties(topicResource);
		            	updateSeeAlsoForChunkedTopics(titleNodeValueMap, chunkedTopicsList, resolver,seeAlsoContentTypes, topicResource);
					} else {
						addUpdateSeeAlso(overrideSeeAlsoEnabled, usedInArray,seeAlsoContentTypes, topicResource, resolver);
					}
		            pathList.add(topicResource.getPath());
		        }
		        if(resolver.hasChanges()) {
		        	try {
						resolver.commit();
					} catch (PersistenceException e) {
						LOGGER.error("PersistenceException occured while saving the changes after see-also node addition addSeeAlsoSection method of SeeAlsoServiceImpl {}", e.getMessage());
					}
		        }
		        if(pathList.size() > 1) {
			        // Sort the list based on number of nodes in the path
		        	pathList.sort((path1, path2) -> {
						String[] resArr1 = path1.split(DITAConstants.FORWARD_SLASH);
						String[] resArr2 = path2.split(DITAConstants.FORWARD_SLASH);
						Integer size1 = Integer.valueOf(resArr1.length);
				        Integer size2 = Integer.valueOf(resArr2.length);
						return size1.compareTo(size2);
					});
		        	String parentTopicPath = pathList.get(0);
			        Resource parentTopicRes = resolver.getResource(parentTopicPath);
			        Iterator<Resource> parentTopicListItr = parentTopicRes.listChildren();
			        List<Resource> parentTopicsChildrenList = Lists.newArrayList(parentTopicListItr);
			        int firstChildTopicIndex = 0;
			        for(Resource item : parentTopicsChildrenList) {
			        	ValueMap topicValueMap = item.adaptTo(ValueMap.class);
			        	firstChildTopicIndex = firstChildTopicIndex + 1;
			        	if(DITAConstants.DITA_TOPIC_RESOURCE_TYPE.equals(topicValueMap.get(DITAConstants.PN_SLING_RESOURCE_TYPE, String.class))) {
			        		break;
			        	}
			        }
			        boolean reOrderingDone = false;
			        if(SEE_ALSO.equals(parentTopicsChildrenList.get(parentTopicsChildrenList.size() - 1).getName()) && firstChildTopicIndex > 0) {
			        	Resource seeAlsoNode = parentTopicsChildrenList.remove(parentTopicsChildrenList.size() - 1);
			        	parentTopicsChildrenList.add(firstChildTopicIndex - 1, seeAlsoNode);
			        	reOrderingDone = true;
			        }
			        if(reOrderingDone) {
			        	List<String> nodeNameList = new ArrayList<>();
			        	for(Resource res : parentTopicsChildrenList) {
			        		nodeNameList.add(res.getName());
			        	}
			        	try {
							JcrUtil.setChildNodeOrder(parentTopicRes.adaptTo(Node.class), nodeNameList.toArray(new String[0]));
						} catch (RepositoryException e) {
							LOGGER.error("RepositoryException occured while reordering after see-also node addition addSeeAlsoSection method of SeeAlsoServiceImpl {}", e.getMessage());
						}
			        }
		        }
			} else {
				LOGGER.debug("JCR Content node is not available for page {}", pageRes.getPath());
			}
			
		}
		LOGGER.debug("Madision - adding see also node took {} seconds to complete the process", (System.currentTimeMillis() - startTime1) / 1000);
	}

	private String[] getSeeAlsoContentTypes(ValueMap pageContentValueMap) {
		return pageContentValueMap.get(DITAConstants.META_PWC_SEE_ALSO_CONTENT_TYPE, String[].class);
	}

	private void addUpdateSeeAlso(boolean overrideSeeAlsoEnabled, String[] usedInArray, String[] seeAlsoContentTypes, Resource topicResource, ResourceResolver resolver) {
		
		Resource seeAlsoRes = topicResource.getChild(SEE_ALSO);
		Node seeAlsoNode;
		List<String> usedInList = new ArrayList<>();
		for(String path : usedInArray) {
			if(path.endsWith(DITAConstants.DITA_EXTENSION)) {
				String pageUrl = DITALinkUtils.getUpdatedXrefLink(resolver.adaptTo(Session.class),path);
				Resource contentRes = resolver.getResource(pageUrl.split(".html")[0] + SLASH_JCR_CONTENT);
				if(null != contentRes && !ResourceUtil.isNonExistingResource(contentRes)) {
					usedInList.add(pageUrl);
				} else {
					LOGGER.debug("Madision - Invalid page reference- {} found for dita topic {}", pageUrl, path);
				}
			}
		}
		if(null == seeAlsoRes || ResourceUtil.isNonExistingResource(seeAlsoRes)) {
			Node topicNode = topicResource.adaptTo(Node.class);
			try {
				seeAlsoNode = topicNode.addNode(SEE_ALSO);
				seeAlsoNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, SEE_ALSO_RESOURCE_TYPE);
				seeAlsoNode.setProperty("usedInTopics", usedInList.toArray(new String[0]));
				seeAlsoNode.setProperty("overrideSeeAlsoEnabled", overrideSeeAlsoEnabled);
				seeAlsoNode.setProperty("seeAlsoContentTypes", seeAlsoContentTypes);
			} catch (ItemExistsException e) {
				LOGGER.error("ItemExistsException occured while adding see also node {}", e.getMessage());
			} catch (PathNotFoundException e) {
				LOGGER.error("PathNotFoundException occured while adding see also node {}", e.getMessage());
			} catch (VersionException e) {
				LOGGER.error("VersionException occured while adding see also node {}", e.getMessage());
			} catch (ConstraintViolationException e) {
				LOGGER.error("ConstraintViolationException occured while adding see also node {}", e.getMessage());
			} catch (LockException e) {
				LOGGER.error("LockException occured while adding see also node {}", e.getMessage());
			} catch (RepositoryException e) {
				LOGGER.error("RepositoryException occured while adding see also node {}", e.getMessage());
			}
		} else {
			seeAlsoNode = seeAlsoRes.adaptTo(Node.class);
			try {
				seeAlsoNode.setProperty("usedInTopics", usedInArray);
				seeAlsoNode.setProperty("overrideSeeAlsoEnabled", overrideSeeAlsoEnabled);
				seeAlsoNode.setProperty("seeAlsoContentTypes", seeAlsoContentTypes);
			} catch (ValueFormatException e) {
				LOGGER.error("ValueFormatException occured while adding see also node {}", e.getMessage());
			} catch (VersionException e) {
				LOGGER.error("VersionException occured while adding see also node {}", e.getMessage());
			} catch (LockException e) {
				LOGGER.error("LockException occured while adding see also node {}", e.getMessage());
			} catch (ConstraintViolationException e) {
				LOGGER.error("ConstraintViolationException occured while adding see also node {}", e.getMessage());
			} catch (RepositoryException e) {
				LOGGER.error("RepositoryException occured while adding see also node {}", e.getMessage());
			}
		}
	}

	private void updateSeeAlsoForChunkedTopics(ValueMap titleNodeValueMap, List<String> chunkedTopicsList,
			ResourceResolver resolver,String[] seeAlsoContentTypes, Resource topicResource) {
		String text = null != titleNodeValueMap ? titleNodeValueMap.get("text", String.class) : StringUtils.EMPTY;
		for(String ditaTopic : chunkedTopicsList) {
			Resource metaDataRes = resolver.getResource(ditaTopic + "/jcr:content/metadata");
			if(null != metaDataRes && !ResourceUtil.isNonExistingResource(metaDataRes)) {
				ValueMap metaValueMap = metaDataRes.adaptTo(ValueMap.class);
				String title = metaValueMap.get("dc:title", String.class);
				boolean isTitleSame = false;
				if(StringUtils.isNotBlank(text) && text.equals(title)) {
					isTitleSame = true;
				}
				if(!isTitleSame) {
					Resource ditaContentRes = resolver.getResource(ditaTopic + SLASH_JCR_CONTENT);
					ValueMap ditaContentValueMap = ditaContentRes.adaptTo(ValueMap.class);
					String fmDitaTitle = ditaContentValueMap.get("fmditaTitle", String.class);
					if(StringUtils.isNotBlank(text) && text.equals(fmDitaTitle)) {
						isTitleSame = true;
					}
				}
				if(isTitleSame) {
					if(isOverrideSeeAlsoEnabled(metaValueMap)) {
						addUpdateSeeAlso(isOverrideSeeAlsoEnabled(metaValueMap), metaValueMap.get(DITAConstants.META_PWC_SEE_ALSO_USED_IN, String[].class), seeAlsoContentTypes, topicResource, resolver);
					} else {
						if (metaValueMap.containsKey(DITAConstants.META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS)) {
		    				String[] defaultUsedInArray  = metaValueMap.get(DITAConstants.META_PWC_SEE_ALSO_DEFAULT_USED_IN_REFS, String[].class);
		    				addUpdateSeeAlso(isOverrideSeeAlsoEnabled(metaValueMap), defaultUsedInArray, seeAlsoContentTypes, topicResource, resolver);
						}
					}
					break;
				}
			}
		}
	}

	private ValueMap getTitleNodeProperties(Resource topicResource) {
		Resource titleRes = topicResource.getChild("title/_text");
		ValueMap valueMap = null != titleRes && !ResourceUtil.isNonExistingResource(titleRes) ? titleRes.adaptTo(ValueMap.class) : null;
		return valueMap;
	}

	private boolean isOverrideSeeAlsoEnabled(ValueMap pageContentValueMap) {
		return pageContentValueMap
				.containsKey(DITAConstants.META_PWC_SEE_ALSO_OVERRIDE_SEE_ALSO)
				&& "yes".equalsIgnoreCase(
						pageContentValueMap.get(DITAConstants.META_PWC_SEE_ALSO_OVERRIDE_SEE_ALSO, String.class))
								? true
								: false;
	}
	
	private List<Resource> getPageResources(String sourcePath, ResourceResolver resolver) {
		final Map<String, Object> predicateMap = new HashMap<>();
		long startTime = System.currentTimeMillis();
        predicateMap.put("type", "cq:Page");
        predicateMap.put("path", sourcePath);
        predicateMap.put("property", "@jcr:content/"+JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
        predicateMap.put("property.value", DITAConstants.PWC_MADISON_COMPONENTS_STRUCTURE_PAGE_VP);
        predicateMap.put("p.limit", "-1");
        
        final Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), resolver.adaptTo(Session.class));
        final SearchResult searchResult = query.getResult();

        final Iterator<Resource> resources = searchResult.getResources();
        List<Resource> resList = Lists.newArrayList(resources);
        LOGGER.debug("Retreiving vp pages under {} took {} seconds to complete the process",sourcePath, (System.currentTimeMillis() - startTime) / 1000);
		return resList;
	}

}
