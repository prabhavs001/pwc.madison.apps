package com.pwc.madison.core.services;

import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.userreg.models.User;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
/**
 * This Service used by WebcastPodcast Homepage component that
 * displays content based upon user authorizations
 */
public interface WebcastPodcastService {

    /**
     * Method to return List Item object after setting its properties
     *
     * @param request {@link SlingHttpServletRequest}
     * @param user {@link User}
     * @param resolver {@link ResourceResolver}
     * @param list {@link List<Item>}
     * @param dateFormat {@link String}
     * @return item {@link List<Item>}
     */
    public List<Item> fetchContent(final SlingHttpServletRequest request, final User user, final ResourceResolver resolver, final List<Item> list, final String dateFormat);

    /**
     * Method to return Fallback Item object after setting its properties
     *
     * @param fallbackItem {@link Item}
     * @param resourceResolver {@link ResourceResolver}
     * @param dateFormat {@link String}
     * @return item {@link Item}
     */
	public Item fetchFallbackItems(final Item fallbackItem, final ResourceResolver resourceResolver, final String dateFormat);
    
}
