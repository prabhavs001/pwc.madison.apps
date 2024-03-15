package com.pwc.madison.core.services.impl;

import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.pwc.madison.core.beans.BreadcrumbItem;
import com.pwc.madison.core.beans.BreadcrumbItemList;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.BreadcrumbJson;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * BreadcrumbJson service implementation
 */
@Component(
        service = { BreadcrumbJson.class },
        immediate = true,
        property = { "service.description=" + "Provides json of parent pages for breadcrumb" })
public class BreadcrumbJsonImpl implements BreadcrumbJson{

    private final Logger LOGGER = LoggerFactory.getLogger(BreadcrumbJsonImpl.class);
    //this node will not be present on orphan dita pages
    private static final String DITA_PAGE_EXISTENCE_TOPICBODY_NODE_STRUCTURE = "root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody";
    
    @Override
    public String getParentJsonHeirarchyForPagePath(Page page) {
        LOGGER.debug("Entered getParentJsonHeirarchyForPagePath for page {}", page.getPath());
        Gson gson = new Gson();
        BreadcrumbItemList items = new BreadcrumbItemList();
        Page homePage = MadisonUtil.getHomePageFromBasePath(page);
        if(homePage != null) {
            List<BreadcrumbItem> breadcrumbList = new ArrayList<>();
            int homePageDepth = homePage.getDepth();
            Page parentPage = page.getParent();
            int parentPageDepth = parentPage.getDepth();
            while (parentPage != null && homePageDepth <= parentPageDepth) {
                //check to remove orphan dita pages
                Resource ditaPageTopicBodyNode = parentPage.getContentResource(DITA_PAGE_EXISTENCE_TOPICBODY_NODE_STRUCTURE);
                if (Objects.nonNull(ditaPageTopicBodyNode) || !parentPage.getPath().startsWith(MadisonConstants.MADISON_SITES_ROOT)) {
                    BreadcrumbItem breadcrumb = new BreadcrumbItem();
                    String pageTitle = MadisonUtil.getPageTitle(parentPage);
                    breadcrumb.setTitle(pageTitle);
                    String processedlinkPath = MadisonUtil.getUrlForPageResource(parentPage.getPath());
                    breadcrumb.setHref(processedlinkPath);
                    breadcrumb.setOrder(parentPageDepth - homePageDepth);
                    breadcrumb.setAlt(pageTitle);
                    breadcrumbList.add(breadcrumb);
                }
                parentPage = parentPage.getParent();
                parentPageDepth--;
            }
            Collections.sort(breadcrumbList);
            items.setBreadcrumbItemList(breadcrumbList);
        }
        LOGGER.debug("Json array string of parent pages {} ", gson.toJson(items));
        return gson.toJson(items);
    }
}
