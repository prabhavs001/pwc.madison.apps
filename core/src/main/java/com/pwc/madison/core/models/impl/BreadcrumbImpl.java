package com.pwc.madison.core.models.impl;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.models.Breadcrumb;
import com.pwc.madison.core.services.BreadcrumbJson;


@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { Breadcrumb.class },
        resourceType = { BreadcrumbImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BreadcrumbImpl implements Breadcrumb{
    
    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/breadcrumb";
    
    @ScriptVariable
    private Page currentPage;
    
    @OSGiService
    private BreadcrumbJson breadcrumbJson;

    private String breadcrumb;
    @PostConstruct
    protected void init() {
        breadcrumb = breadcrumbJson.getParentJsonHeirarchyForPagePath(currentPage);
    }
    @Override
    public String getJsonForBreadcrumb() {
        return breadcrumb;
    }

}
