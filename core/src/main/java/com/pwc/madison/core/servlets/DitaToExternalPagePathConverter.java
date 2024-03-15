package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;

/**
 * Returns JSON Response which contains DITA Path
 * and AEM page path in the key value pair. 
 * 
 * @author Mayank
 *
 */
@Component(service = Servlet.class, name = "DITA to External URL Converter Servlet", property = {
		org.osgi.framework.Constants.SERVICE_DESCRIPTION + "= DITA to External URL Converter Servlet",
		"sling.servlet.paths=" + DitaToExternalPagePathConverter.SERVLET_PATH, "sling.servlet.methods=" + HttpConstants.METHOD_POST })
public class DitaToExternalPagePathConverter extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 7270861655022997150L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DitaToExternalPagePathConverter.class);

	public static final String SERVLET_PATH = "/bin/dita/to/externalUrl";
	
    @Reference
    private transient XSSAPI xssApi;

	@Activate
	protected void activate() {
		LOGGER.debug("DitaToExternalPagePathConverter Activated!");
	}

	@Override
	protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {
		boolean responseFlag = false;
		try {
			ResourceResolver resolver = request.getResourceResolver();
			String ditaObject = IOUtils.toString(request.getReader());
			LOGGER.debug("DITA Object: " + ditaObject);
			PrintWriter out = response.getWriter();
			List<String> ditaPathList = new ArrayList<>();
			ObjectMapper mapper = new ObjectMapper();
			ditaPathList = mapper.readValue(ditaObject, new TypeReference<List<String>>() {});			
			if(!ditaPathList.isEmpty()) {
				JSONObject json = new JSONObject();
				ditaPathList.forEach(ditaPath -> {
					try {
						String pagePath = DITAUtils.getFasbPageFromXrefDita(ditaPath, resolver, MadisonConstants.FASB_US_SEARCH_ROOT_PATH, xssApi);
						if(StringUtils.isNotBlank(pagePath) && !StringUtils.endsWith(pagePath, DITAConstants.DITA_EXTENSION) && 
								!StringUtils.endsWith(pagePath, DITAConstants.DITAMAP_EXT)) {
							json.put(ditaPath, pagePath);
						}
					} catch (Exception e) {	
						LOGGER.error("Error in DitaToExternalPagePathConverter : {}", e.getMessage());
					}
				});
				LOGGER.debug("JSON Object: " + json);
				out.println(json);
				responseFlag = true;
			}
		} catch(Exception e) {
			LOGGER.error("Error in doPost() method of DitaToExternalPagePathConverter : {}", e.getMessage());
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
