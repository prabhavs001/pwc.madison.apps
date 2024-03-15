package com.pwc.madison.core.authorization.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.authorization.constants.ContentAuthorizationConstants;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.constants.MadisonConstants;

/**
 * Implementing filter for Viewpoint Asset/Page Request. This filter takes care of T&C and used for implementing content
 * authorization. This filter also allows search bots to crawl through external user accessible content.
 */
@Component(
        service = Filter.class,
        property = { EngineConstants.SLING_FILTER_SCOPE + "=REQUEST",
                EngineConstants.SLING_FILTER_PATTERN + "="
                        + ContentAuthorizationConstants.MADISON_DAM_PAGE_FILTER_HIERARCHY,
                Constants.SERVICE_RANKING + ":Integer=1" })
public class PageAndAssetRequestFilter implements Filter {

    public static final Logger LOGGER = LoggerFactory.getLogger(PageAndAssetRequestFilter.class);

    @Reference
    private ContentAuthorizationService contentAuthorizationService;

    @Reference
    private XSSAPI xssAPI;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        boolean allowFilterChain = true;
        final SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) servletRequest;
        LOGGER.debug("Filtered Path: " + xssAPI.encodeForHTML(slingHttpServletRequest.getRequestURI()));
        String resourcePath = slingHttpServletRequest.getResource().getPath();
        if ((StringUtils.contains(slingHttpServletRequest.getRequestURL(), MadisonConstants.HTML_EXTN) || StringUtils
                .contains(slingHttpServletRequest.getRequestURL(), MadisonConstants.PWC_MADISON_DAM_BASEPATH)) && !(resourcePath.matches(MadisonConstants.MADISON_HOMEPAGE_REGEX + "/(jcr:content|_jcr_content)(.*)"))) {
            LOGGER.info("Authorizing: "+ xssAPI.encodeForHTML(slingHttpServletRequest.getRequestURI()));
            final SlingHttpServletResponse slingHttpServletResponse = (SlingHttpServletResponse) servletResponse;
            LOGGER.debug("PageAndAssetRequestFilter doFilter : Entered Asset/Page Request Filter for resource path : {}",
                    xssAPI.encodeForHTML(resourcePath));
            allowFilterChain = contentAuthorizationService.performContentAuthorization(resourcePath,
                    slingHttpServletRequest, slingHttpServletResponse, false);
        }
        if (allowFilterChain) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

}
