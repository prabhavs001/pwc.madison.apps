package com.pwc.madison.core.servlets;

import com.adobe.granite.rest.Constants;
import com.pwc.madison.core.services.BodyCalloutService;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;

/**
 * Servlet to fetch the list of related content items used in body callout.
 */
@Component(service = Servlet.class,
        property =
                {ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES +"=pwc-madison/components/ditacontent/inbodycallout/pwccalloutblock",
                        ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
                        ServletResolverConstants.SLING_SERVLET_SELECTORS +
                                "=getBodyCalloutList",
                        ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json"})
public class BodyCalloutServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

	@Reference
    private transient BodyCalloutService bodyCalloutService;

    String page = StringUtils.EMPTY;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        response.setContentType(Constants.CT_JSON);
        response.setCharacterEncoding(Constants.DEFAULT_CHARSET);
        response.getWriter().print(getBodyCalloutItemsJson(request, response));


    }

    /**
     * Returns JSON for Body Callout Items.
     *
     * @return
     */
    private String getBodyCalloutItemsJson(SlingHttpServletRequest request, SlingHttpServletResponse response) {
       
        return bodyCalloutService.getBodyCalloutJson(request, response);
    }

}
