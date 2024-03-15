package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.google.gson.Gson;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.Externalizer;
import com.day.cq.search.QueryBuilder;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.util.MadisonUtil;
import com.pwc.madison.core.util.SeeAlsoUtil;

@Component(
    service = Servlet.class,
    property = { Constants.SERVICE_DESCRIPTION + "=Get Topic Referred in other DITA topics Servlet",
            "sling.servlet.methods=" + HttpConstants.METHOD_GET,
            "sling.servlet.paths=" + "/bin/pwc-madison/getTopicReferences" })
public class GetDitaTopicUsedInReferencesServlet extends SlingSafeMethodsServlet {

	private static final String PIPE = "|";
	private static final long serialVersionUID = 1L;
	public static final String JSON = "json";
	public static final String HTML = "html";
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Reference
    private transient XSSAPI xssApi;
    @Reference
    private Externalizer externalizer;
    
    @Reference
    private QueryBuilder queryBuilder;
    
    transient ValueMap valueMap;
    transient List<Resource> resourceList;
	private List<Map<String,String>> usedInReferencesInfo;

	@Override
    protected void doGet(@Nonnull final SlingHttpServletRequest request,
            @Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {

		String extension = request.getRequestPathInfo().getExtension();
		if(extension.equals(JSON)){
			response.setContentType(com.pwc.madison.core.userreg.Constants.CONTENT_TYPE_JSON);
			response.setCharacterEncoding(com.pwc.madison.core.userreg.Constants.UTF_8_ENCODING);
		}

		final ResourceResolver resourceResolver = request.getResourceResolver();
		resourceList = new ArrayList<>();
		final PrintWriter writer = response.getWriter();

		if (null == resourceResolver) {
			writer.write("Unable to get service resolver to process data. Please check the permissions\n");
			writer.close();
			return;
		}

		final String topicPath = request.getParameter("item");
		LOGGER.debug("Dita Path : {}", xssApi.encodeForHTML(topicPath));
		if (StringUtils.isBlank(topicPath)) {
			response.setStatus(500);
			writer.write(StringUtils.EMPTY);
			writer.close();
			return;
		}
		String topicTerritory = MadisonUtil.getTerritoryCodeForPath(topicPath);
		List<String> refList = new ArrayList<>();
		String cookieValue = MadisonUtil.getTokenCookieValue(request);
		String endApi = SeeAlsoUtil.getPostUrl(resourceResolver, externalizer);
		refList = SeeAlsoUtil.getUsedInReferencePages(topicPath, cookieValue, endApi);

		// Check for syndication cases
		if(refList.isEmpty()) {
			String sourcePathForSubscriber = SeeAlsoUtil.getSourcePathForSubscriber(topicPath, resourceResolver.adaptTo(Session.class), queryBuilder);
			if(StringUtils.isNotEmpty(sourcePathForSubscriber)) {
				String sourceTopicTerritory = MadisonUtil.getTerritoryCodeForPath(sourcePathForSubscriber);
				String sourceTopicPath = topicPath.replace("/"+topicTerritory + "/", "/"+sourceTopicTerritory + "/");
				refList = SeeAlsoUtil.getUsedInReferencePages(sourceTopicPath, cookieValue, endApi);
			}
			// convert all to within the same territory as current topic
			if(!refList.isEmpty()) {
				refList = SeeAlsoUtil.getTopicsCorrespondingToCurrentTopic(resourceResolver, refList, topicTerritory);
			}
		}
		LOGGER.debug("Madision - All backward references ", refList.toString());
		// Filter the list
		RequestParameter[] contentTypes = request.getRequestParameters("contentTypes");
		ValueMap topicValueMap = resourceResolver.getResource(topicPath+"/jcr:content/metadata").adaptTo(ValueMap.class);
		String[] contentTypesFromTopic = topicValueMap.get(DITAConstants.META_PWC_SEE_ALSO_TOPIC_CONTENT_TYPE, String[].class);
		List<String> filteredList = refList.stream()
                .filter(item -> {
					ValueMap valueMap1 = resourceResolver.getResource(item + "/jcr:content/metadata").adaptTo(ValueMap.class);
					boolean isSourcePwC = false;
					if(valueMap1.containsKey(DITAConstants.META_STANDARD_SETTERS) && "ss_pwc".equals(valueMap1.get(DITAConstants.META_STANDARD_SETTERS, String.class))) {
						isSourcePwC = true;
					}
					boolean isNonPrivateGroup = true;
					if (valueMap1.containsKey(DITAConstants.META_AUDIENCE) && "privateGroup"
							.equals(valueMap1.get(DITAConstants.META_AUDIENCE, String.class))) {
						isNonPrivateGroup = false;
					}
					boolean allowedContentType = false;
					if((contentTypes==null || ArrayUtils.isEmpty(contentTypes)) && extension.equals(JSON)){
						allowedContentType = true;
					} else if((contentTypes==null || ArrayUtils.isEmpty(contentTypes)) && (contentTypesFromTopic==null || ArrayUtils.isEmpty(contentTypesFromTopic))&&(extension.equals(HTML))){
						allowedContentType = true;
					} else if (ArrayUtils.isNotEmpty(contentTypes) && Arrays.stream(contentTypes).anyMatch(param -> param.getString().equals(valueMap1.get(DITAConstants.META_CONTENT_TYPE, String.class))) && extension.equals(JSON) ) {
						allowedContentType = true;
					} else if(topicValueMap.containsKey(DITAConstants.META_PWC_SEE_ALSO_TOPIC_CONTENT_TYPE) && Arrays.stream(contentTypesFromTopic).anyMatch(contentType -> contentType.equals(valueMap1.get(DITAConstants.META_CONTENT_TYPE, String.class))) && extension.equals(HTML) ){
						allowedContentType = true;
					}
					return item.endsWith(DITAConstants.DITA_EXTENSION) && isSourcePwC && allowedContentType && topicTerritory.equals(MadisonUtil.getTerritoryCodeForPath(item)) && isNonPrivateGroup;
				})
                .collect(Collectors.toList());
		// Sort the list
		filteredList.sort((path1, path2) -> {
			ValueMap valueMap1 = resourceResolver.getResource(path1 + "/jcr:content/metadata").adaptTo(ValueMap.class);
			ValueMap valueMap2 = resourceResolver.getResource(path2 + "/jcr:content/metadata").adaptTo(ValueMap.class);
			Date date1 = valueMap1.get(DITAConstants.META_REVISION_DATE, Date.class)!=null ? valueMap1.get(DITAConstants.META_REVISION_DATE, Date.class) : valueMap1.get(DITAConstants.META_PUBLICATION_DATE, Date.class);
			Date date2 = valueMap2.get(DITAConstants.META_REVISION_DATE, Date.class)!=null ? valueMap2.get(DITAConstants.META_REVISION_DATE, Date.class) : valueMap2.get(DITAConstants.META_PUBLICATION_DATE, Date.class);
			return Long.compare(date2.getTime(), date1.getTime());
		});
		usedInReferencesInfo = new ArrayList<>();
		for (String path : filteredList) {
			valueMap = new ValueMapDecorator(new HashMap<>());
			Map<String,String> ditaInfoMap = new HashMap<>();
			Resource ditaResource = resourceResolver.getResource(path + "/jcr:content");
			if(null != ditaResource) {
				ValueMap ditaValueMap = ditaResource.adaptTo(ValueMap.class);
				ValueMap resourceMetadata = ditaResource.getChild("metadata").adaptTo(ValueMap.class);
				String publicationDate = StringUtils.EMPTY;
				String contentId = StringUtils.EMPTY;
				if(null != resourceMetadata) {
					Date date = resourceMetadata.get(DITAConstants.META_PUBLICATION_DATE, Date.class);
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					publicationDate = formatter.format(date.getTime());
					contentId = resourceMetadata.get(DITAConstants.META_CONTENT_ID, String.class);
				}
				valueMap.put("value", path);
				contentId = StringUtils.isBlank(contentId) ? "" : contentId;
				valueMap.put("text", ditaValueMap.get("fmditaTitle") + PIPE + contentId + PIPE + path + PIPE + publicationDate);
				ditaInfoMap.put("path",path);
				ditaInfoMap.put("title", (String) ditaValueMap.get("fmditaTitle"));
				ditaInfoMap.put("contentId",contentId);
				ditaInfoMap.put("publicationDate",publicationDate);
				usedInReferencesInfo.add(ditaInfoMap);
				resourceList.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), "nt:unstructured", valueMap));
			}
		}
		if(extension.equals(JSON)){
			final Gson gson = new Gson();
			response.getWriter().write(gson.toJson(usedInReferencesInfo));
		} else {
			/*Create a DataSource that is used to populate the drop-down control*/
			DataSource dataSource = new SimpleDataSource(resourceList.iterator());
			request.setAttribute(DataSource.class.getName(), dataSource);
		}
    }

}
