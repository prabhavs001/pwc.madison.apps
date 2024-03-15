package com.pwc.madison.core.models.impl;

import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.pwc.madison.core.models.MadisonListItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class MadisonPageListItemImpl implements MadisonListItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(MadisonPageListItemImpl.class);

    protected SlingHttpServletRequest request;
    protected Page page;

    public static final String PN_REDIRECT_TARGET = "cq:redirectTarget";


    public MadisonPageListItemImpl(@Nonnull SlingHttpServletRequest request, @Nonnull Page page) {
        this.request = request;
        this.page = page;
        Page redirectTarget = getRedirectTarget(page);
        if (redirectTarget != null && !redirectTarget.equals(page)) {
            this.page = redirectTarget;
        }
    }

    public String getURL() {
        String vanityURL = page.getVanityUrl();
        return StringUtils.isEmpty(vanityURL) ? request.getContextPath() + page.getPath() + ".html" : request.getContextPath() + vanityURL;
    }

    public String getTitle() {
        String title = page.getNavigationTitle();
        if (title == null) {
            title = page.getPageTitle();
        }
        if (title == null) {
            title = page.getTitle();
        }
        if (title == null) {
            title = page.getName();
        }
        return title;
    }

    public String getDescription() {
        return page.getDescription();
    }

    public Calendar getLastModified() {
        return page.getLastModified();
    }

    public String getPath() {
        return page.getPath();
    }


    private Page getRedirectTarget(@Nonnull Page page) {
        Page result = page;
        String redirectTarget;
        PageManager pageManager = page.getPageManager();
        Set<String> redirectCandidates = new LinkedHashSet<>();
        redirectCandidates.add(page.getPath());
        while (result != null && StringUtils.isNotEmpty((redirectTarget = result.getProperties().get(PN_REDIRECT_TARGET, String.class)))) {
            result = pageManager.getPage(redirectTarget);
            if (result != null && !redirectCandidates.add(result.getPath())) {
                LOGGER.warn("Detected redirect loop for the following pages: {}.", redirectCandidates.toString());
                break;
            }

        }
        return result;
    }

}
