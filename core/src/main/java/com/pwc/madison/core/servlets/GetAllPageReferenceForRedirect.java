package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.util.DITAUtils;


/**
 * Returns JSON Response for all the page references in batch size which
 * contains DITA Path and AEM page path in the key value pair.
 * 
 */
@Component(service = Servlet.class, name = "Get All Page Reference for Redirect Servlet", property = {
		org.osgi.framework.Constants.SERVICE_DESCRIPTION + "= Get All Page Reference for Redirect Servlet",
		"sling.servlet.paths=" + GetAllPageReferenceForRedirect.SERVLET_PATH, "sling.servlet.methods=" + HttpConstants.METHOD_POST })
public class GetAllPageReferenceForRedirect extends SlingAllMethodsServlet {
	
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GetAllPageReferenceForRedirect.class);

	public static final String SERVLET_PATH = "/bin/dita/pageReference";
	
	@Activate
	protected void activate() {
		LOGGER.debug("GetAllPageReferenceForRedirect Activated!");
	}
	
	@Override
	protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {
		boolean responseFlag = false;
		try {
			ResourceResolver resolver = request.getResourceResolver();
			String ditaObject = request.getParameter("ditaObject");
			PrintWriter out = response.getWriter();
			List<String> ditaPathList = new ArrayList<>();
			ObjectMapper mapper = new ObjectMapper();
			ditaPathList = mapper.readValue(ditaObject, new TypeReference<List<String>>() {});			
			if(!ditaPathList.isEmpty()) {
				JSONObject json = new JSONObject();
				ditaPathList.forEach(ditaPath -> {
					try {
						final List<String> pagePathsFromDita = DITAUtils.getPagePathsFromDita(ditaPath, resolver);
			            String pagePath = Arrays.toString(pagePathsFromDita.toArray());
						if(StringUtils.isNotBlank(pagePath) && !StringUtils.endsWith(pagePath, DITAConstants.DITA_EXTENSION) && 
								!StringUtils.endsWith(pagePath, DITAConstants.DITAMAP_EXT)) {
							json.put(ditaPath, pagePath);
							LOGGER.debug("Got pagaPath: {} for ditaPath: {}", pagePath, ditaPath);
						}
					} catch (Exception e) {			
						LOGGER.error("Error in GetAllPageReferenceForRedirectServlet : {}", e.getMessage());
					}
				});
				LOGGER.debug("GetAllPageReferenceForRedirectServlet JSON Object: " + json);
				out.println(json);
				responseFlag = true;
			}
		} catch(Exception e) {
			LOGGER.error("Error in doPost() method of GetAllPageReferenceForRedirect : {}", e.getMessage());
		} 
		if(responseFlag) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.flushBuffer();
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);	
			response.flushBuffer();
		}
	}
	
}
