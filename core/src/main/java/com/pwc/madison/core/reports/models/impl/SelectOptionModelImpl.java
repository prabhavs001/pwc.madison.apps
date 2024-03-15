/**
 * 
 */
package com.pwc.madison.core.reports.models.impl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.reports.models.SelectOptionModel;
import com.pwc.madison.core.services.ReportService;

@Model(adaptables = SlingHttpServletRequest.class, adapters = SelectOptionModel.class)
public class SelectOptionModelImpl implements SelectOptionModel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectOptionModelImpl.class);

    /**
     * Property Field
     */
    @ValueMapValue
    private String property;

    /**
     * DataSource Field
     */
    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String datasource;
    

    /**
     * Request Attribute result
     */
    @RequestAttribute
    private Resource result;

    /**
     * Report Service Reference
     */
    @Inject
    private ReportService reportService;

    /**
     * ReourceResolver reference
     */
    @SlingObject
    private ResourceResolver resourceResolver;

    /**
     * DisplayText value for select option
     */
    private Object displayText;

    public SelectOptionModelImpl() {
    }

    /**
     * Constructor Method
     * 
     * @param result
     *            request attribute result
     * @param property
     *            property field
     * @throws RepositoryException
     */
    public SelectOptionModelImpl(Resource result, String property) throws RepositoryException {
        this.result = result;
        this.property = property;
        init();
    }

    /**
     * Init Method
     * 
     * @throws RepositoryException
     */
    @PostConstruct
    private void init() throws RepositoryException {
        displayText = result != null ? result.getValueMap().get(property) : null;
        String[] tmpArr;
        if(displayText instanceof String[]) {
            tmpArr = (String[]) displayText;
            LOGGER.debug("Property is multi {}",displayText);
        }
        else {
            tmpArr =  new String[] {String.valueOf(displayText)};
            LOGGER.debug("Property is single {}",displayText);
        }
            displayText = reportService.getOptionTextValue(resourceResolver, datasource, tmpArr);
    }

    @Override
    public Object getDisplayText() {
        return displayText;
    }

}
