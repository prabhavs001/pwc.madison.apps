package com.pwc.madison.core.util;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class to containing custom render conditions (graniteRenderCondition)
 */
public final class TemplateRenderConditionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateRenderConditionUtil.class);

    private static final String PAGE_PROPERTIES = "wcm/core/content/sites/properties";
    private static final String ITEM_PARAMETER = "item";

    /**
     * Check if the current page is created from any of the given templates.
     *
     * @param slingHttpServletRequest {@link SlingHttpServletRequest}
     * @param httpServletRequest      {@link HttpServletRequest}
     * @param templatePaths           template paths
     * @return true if the page is created from any of the template defined in {@code templatePaths}, false otherwise
     */
    public static boolean isTemplate(SlingHttpServletRequest slingHttpServletRequest, HttpServletRequest httpServletRequest,
                                     String[] templatePaths) {

        // error if any of the passed params is null.
        if (slingHttpServletRequest == null || httpServletRequest == null || ArrayUtils.isEmpty(templatePaths)) {

            LOGGER.error("slingHttpServletRequest -> {}, httpServletRequest -> {}, templatePaths -> {}",
                new Object[]{slingHttpServletRequest, httpServletRequest, Arrays.toString(templatePaths)});

            throw new IllegalArgumentException("One of the passed parameters is null.");
        }

        // the dialog is a page properties dialog (doesn't handle create page wizard)
        if (StringUtils.contains(httpServletRequest.getPathInfo(), PAGE_PROPERTIES)) {

            // get the actual page path
            String pagePath = httpServletRequest.getParameter(ITEM_PARAMETER);
            // get page template path and check it
            return Optional.ofNullable(slingHttpServletRequest.getResourceResolver())
                .map(resourceResolver -> resourceResolver.getResource(pagePath))
                .map(pageResource -> pageResource.adaptTo(Page.class))
                .map(Page::getTemplate)
                .map(Template::getPath)
                .map(path -> ArrayUtils.contains(templatePaths, path))
                .orElse(false);
        }

        return false;
    }
}
