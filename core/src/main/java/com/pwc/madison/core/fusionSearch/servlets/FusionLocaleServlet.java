package com.pwc.madison.core.fusionSearch.servlets;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.pwc.madison.core.fusionSearch.services.FusionSearchConfigurationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.Servlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to populate locale dropdown for Fusion Indexing Utility
 */
@Component(service = Servlet.class,
property = {Constants.SERVICE_DESCRIPTION + "=Datasource servlet to populate locale dropdown for Fusion Indexing Utility",
        ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=pwc-madison/utilities/dialog/locales",
        ServletResolverConstants.SLING_SERVLET_METHODS + "=" +  HttpConstants.METHOD_GET
})
public class FusionLocaleServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(FusionLocaleServlet.class);

    private static final String ITEM_VALUE_PROPERTY = "value";
    private static final String ITEM_LABEL_PROPERTY = "text";
    private static final String COLON_SEPARATOR = ":";

	@Reference
	private FusionSearchConfigurationService fusionSearchConfigurationService;

    protected void doGet(final SlingHttpServletRequest request, SlingHttpServletResponse response){
        ResourceResolver requestResourceResolver = request.getResourceResolver();
        request.setAttribute(DataSource.class.getName(), getDataSource(requestResourceResolver));
    }

    /**
     * returns datastore
     * @param resolver
     * @return datastore
     */
    private DataSource getDataSource(ResourceResolver resolver){
        List<String> locales = fusionSearchConfigurationService.getIndexingLocales();
        return new SimpleDataSource(fetchLocales(locales, resolver).iterator());
    }

    /**
     * populates list of options resource for locale dropdown
     * @param spIDs
     * @param resolver
     * @return spIDList
     */
	private ArrayList<Resource> fetchLocales(List<String> locales, ResourceResolver resolver) {
		ArrayList<Resource> localeList = new ArrayList<>();
		if (null == locales) {
			return localeList;
		}
		try {
			for (String locale : locales) {
				
				String[] value = locale.split(COLON_SEPARATOR);
                if(value.length > 1) {
                    ValueMap valueMap = new ValueMapDecorator(new HashMap<>());
                    valueMap.put(ITEM_LABEL_PROPERTY, value[0]);
                    valueMap.put(ITEM_VALUE_PROPERTY, value[1]);
                    Resource optionsResource = new ValueMapResource(resolver, new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED, valueMap);
                    if(null != optionsResource){
                    	localeList.add(optionsResource);
                    }
                }
			}
		} catch (Exception e) {
			LOG.error("Error while fetching locales", e);
		}
		return localeList;
	}
}
