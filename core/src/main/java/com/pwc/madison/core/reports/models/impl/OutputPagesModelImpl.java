/**
 *
 */
package com.pwc.madison.core.reports.models.impl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.pwc.madison.core.reports.models.OutputPagesModel;
import com.pwc.madison.core.services.OutputPagesService;

/**
 * Model Class to get the number of output pages
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = OutputPagesModel.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class OutputPagesModelImpl implements OutputPagesModel {

    /**
     * SlingHttpServletRequest Request
     */
    @Self
    private SlingHttpServletRequest request;

    /**
     * OutputPagesService service
     */
    @Inject
    private OutputPagesService outputPagesService;

    /**
     * Request Attribute result
     */
    @RequestAttribute
    private Resource result;

    /**
     * Number of pages
     */
    private Object numberOfPages;

    /**
     * Number of topics
     */
    private Object topicsCount;

    public OutputPagesModelImpl() {
    }

    public OutputPagesModelImpl(final Resource result) {
        this.result = result;
        init();
    }

    /**
     * Init Method
     */
    @PostConstruct
    private void init() {

        final ResourceResolver resolver = request.getResourceResolver();

        if (null != result) {

            final String assetPath = result.getPath();

            // get the number of pages
            numberOfPages = outputPagesService.getNumberOfPages(resolver, assetPath);
            topicsCount = outputPagesService.getTopicsCount(resolver, assetPath);
        }

    }

    @Override
    public Object getNumberOfPages() {
        return numberOfPages;
    }

    @Override
    public Object getTopicsCount() {
        return topicsCount;
    }

}
