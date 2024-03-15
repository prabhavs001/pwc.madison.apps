package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;
import org.json.JSONArray;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.AlertsTileService;


/**
 * Servlet to Set and Get dismissed Alerts Cookie
 *
 * @author Divanshu
 */
@Component(
		service = Servlet.class,
		name = "Madison DismissedAlerts cookie servlet",
		property = { org.osgi.framework.Constants.SERVICE_DESCRIPTION + "=Madison User Dismissed Alerts cookie actions",
				"sling.servlet.paths=/bin/pwc-madison/DismissCookie", "sling.servlet.methods=" + HttpConstants.METHOD_POST,
				"sling.servlet.paths=/bin/pwc-madison/DismissCookie", "sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class DismissAlertsCookieActionServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	/** Default Logger */
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


	@Reference
	AlertsTileService alertsTileService;

	@Reference
	private XSSAPI xssAPI;

	@Override
	protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {
		try {
			List<String> cookieList = Arrays.asList(request.getParameterValues("dismissedPagesValue[]"));
			JSONArray cookieArr = new JSONArray(cookieList);
			String refererPage = new URI(request.getHeader("Referer")).getPath();
			LOGGER.debug("========================DismissAlertsCookieActionServlet() =============== REFERER PATH = :: {}", xssAPI.encodeForHTML(refererPage));
			alertsTileService.setPageCookie(response, MadisonConstants.COOKIE_EXPIRE_HOURS, MadisonConstants.DISMISS_PAGE_COOKIE_NAME, cookieArr,
					refererPage);
			response.flushBuffer();
		}catch(Exception e) {
			LOGGER.error("Exception in :: Post method :: DismissAlertsCookieActionServlet() :: {}",e);
		}

	}

	@Override
	protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {
		try {
			final PrintWriter out = response.getWriter();
			final String cookieValue = alertsTileService.getAlertsCookie(request,MadisonConstants.DISMISS_PAGE_GLOBAL_COOKIE_NAME);
			if (cookieValue.isEmpty()) {
				response.setStatus(SlingHttpServletResponse.SC_NO_CONTENT);
			} else {
				response.setStatus(SlingHttpServletResponse.SC_OK);
				response.setContentType(MadisonConstants.CONTENTTYPE_JSON);
				out.println(xssAPI.getValidJSON(cookieValue, StringUtils.EMPTY));
			}
			response.flushBuffer();
		}catch(Exception e) {
			LOGGER.error("Exception in :: Get method :: DismissAlertsCookieActionServlet() :: {}",e);
		}
	}

}
