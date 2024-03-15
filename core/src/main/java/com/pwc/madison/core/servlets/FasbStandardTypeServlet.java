package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.commons.datasource.poolservice.DataSourceNotFoundException;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.FasbCrossRefService;


/**
 *
 * Servlet to handle request for Standard type and Standard number.
 *
 */
@Component(service=Servlet.class,
property={
		Constants.SERVICE_DESCRIPTION + "=Standard Type Servlet",
		"sling.servlet.paths=/bin/pwc-madison/crossref/typeservlet", "sling.servlet.methods=" + HttpConstants.METHOD_GET
})
public class FasbStandardTypeServlet extends SlingSafeMethodsServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(FasbStandardTypeServlet.class);
	private static final String APPLICATION_JSON = "application/json";

	@Reference
	private transient FasbCrossRefService fasbCrossRefService;
	
	@Activate
	public void activate() {
		LOGGER.info("FasbStandardTypeServlet is activated");
	}

	/****
	 * this method checks requestType and provide response accordingly
	 */
	@Override
	protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {
		LOGGER.debug("FasbStandardTypeServlet initiated");
		final RequestPathInfo pathInfo=   request.getRequestPathInfo();
		final String[] suffixes = pathInfo.getSelectors();
		String outPutjson=null;
		final String requestType=suffixes[0];
		try
		{
			if(null !=requestType)
			{
				if(requestType.equalsIgnoreCase(MadisonConstants.STANDARD_TYPE))
				{
					outPutjson= fasbCrossRefService.getStandardTypes();
				}
				else if(requestType.equalsIgnoreCase(MadisonConstants.STANDARD_NUMBER))
				{
					outPutjson= fasbCrossRefService.getStandardNumbers(request,response);
				}
				LOGGER.debug("outPutjson : "+outPutjson);
				response.setContentType(APPLICATION_JSON);
				response.getWriter().write(outPutjson);
			}
		}catch (final SQLException e) {
			LOGGER.error("SQLException in FasbStandardTypeServlet",e);
			response.setStatus(SlingHttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
		catch (final DataSourceNotFoundException e) {
			LOGGER.error("DataSourceNotFoundException  in FasbStandardTypeServlet",e);
			response.setStatus(SlingHttpServletResponse.SC_SERVICE_UNAVAILABLE);

		}
		catch (final Exception e) {
			LOGGER.error("Exception in FasbStandardTypeServlet",e);
			response.setStatus(SlingHttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}

	}
	
	@Deactivate
	public void deactivate() {
		LOGGER.info("FasbStandardTypeServlet is deactivated");
	}
}
