package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pwc.madison.core.models.CitationPattern;
import com.pwc.madison.core.models.CitationPatternResultModel;
import com.pwc.madison.core.models.CitationTextFileRefrenceModel;
import com.pwc.madison.core.services.CitationLinkingService;
import com.pwc.madison.core.services.CitationMapperService;
import com.pwc.madison.core.services.CitationTextToIdConverterService;
import com.pwc.madison.core.services.CitationTextToLinkMapperService;
import com.pwc.madison.core.services.FetchCitationPatternService;
import com.pwc.madison.core.util.MadisonUtil;

/**
 * Servlet to convert <autolink> elements to its corresponding <pwc-xref> from DITA files
 */
@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=Auto Link Citation Text To Link",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/pwc-madison/autolink" })
public class AutolinkerServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 1L;

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	@Reference
	transient CitationMapperService citationMapperService;

	@Reference
	transient CitationTextToIdConverterService citationTextToIdConverterService;

	@Reference
	transient FetchCitationPatternService fetchCitationPatternService;

	@Reference
	transient CitationTextToLinkMapperService citationTextToLinkMapperService;

	@Reference
	transient CitationLinkingService citationLinkingService;
	
	@Reference
	transient XSSAPI xssAPI;

	
	private static final String ROUTINE_NAMES_QUERY_PARAMTER = "routineNames";
	
	private static final String PATH_QUERY_PARAMTER = "path";

	@Override
	protected void doGet(@Nonnull final SlingHttpServletRequest request,
			@Nonnull final SlingHttpServletResponse response) throws ServletException, IOException {

		final List<String> routineNames = Arrays.asList(request.getParameterValues(ROUTINE_NAMES_QUERY_PARAMTER));
		final String searchPath = request.getParameter(PATH_QUERY_PARAMTER);
		LOG.debug("routineNames: {}", xssAPI.encodeForHTML(routineNames.toString()));
		LOG.debug("searchPath: {}", xssAPI.encodeForHTML(searchPath));

		if (routineNames.isEmpty() || StringUtils.isBlank(searchPath)) {
			sendErrorResponse(response);
			return;
		}
		
		final long t1 = System.currentTimeMillis();

		final List<CitationPattern> citationPatterns = citationMapperService
				.getCitationPatternsByRoutineNames(routineNames);

		if (null == citationPatterns || citationPatterns.isEmpty()) {
			sendErrorResponse(response);
			return;
		}
		
		final List<CitationPatternResultModel> citationPatternResults = new ArrayList<CitationPatternResultModel>();

		try {
			for (CitationPattern citationPattern : citationPatterns) {
				//Collect citations from dita topics and add them along with asset path to citationPatternMap
				Map<String, CitationTextFileRefrenceModel> citationPatternMap = fetchCitationPatternService.CollectCitationPattern(citationPattern, searchPath);
				if (citationPatternMap != null) {
					//Convert citation text to anchorId using predefined logic
					citationPatternMap = citationTextToIdConverterService.ConvertCitationTextToId(citationPatternMap, citationPattern);
					LOG.debug("Citation Pattern Map: {}", citationPatternMap);
					//Find anchorId in searchPath and update the assetPath in citationLinkMap
					Map<String, String> citationLinkMap = citationTextToLinkMapperService.CollectCitationTextToLinkMap(citationPatternMap, citationPattern.getScope(), citationPattern, citationPatternResults);
					LOG.debug("Collect Citation Text To Link Map: {}", citationLinkMap);
					if (citationLinkMap != null && !citationLinkMap.isEmpty()) {
						//Update the <autolink> tag with <pwc-xref> tag
						citationLinkingService.LinkCitationText(citationPatternMap, citationLinkMap, citationPattern, citationPatternResults);
					}
				}
			}
			LOG.debug(String.format("Autolinker Servlet took %d milliseconds to complete the process", System.currentTimeMillis() - t1));
			LOG.debug("Citation Pattern Result: {}", citationPatternResults.toString());
			final Gson gson = new Gson();
			response.getWriter().write(gson.toJson(citationPatternResults));

		} catch (final Exception e) {
			LOG.error("Error in AutolinkerServlet Servelet: {}", e);
			sendErrorResponse(response);
		}
	}
	
	/**
     * Method to set 500 error code in the response status and write error response.
     * @param response
     */
    private void sendErrorResponse(final SlingHttpServletResponse response) throws IOException {
        response.setContentType(ContentType.TEXT_HTML.getMimeType());
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Error in Autolinker Servlet");
    }

}
