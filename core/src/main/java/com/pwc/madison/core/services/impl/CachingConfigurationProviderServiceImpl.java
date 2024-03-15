package com.pwc.madison.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.CachingConfigurationProviderService;
import com.pwc.madison.core.services.impl.CachingConfigurationProviderServiceImpl.CachingConfiguration;

@Component(
        service = CachingConfigurationProviderService.class,
        immediate = true)
@Designate(ocd = CachingConfiguration.class)
public class CachingConfigurationProviderServiceImpl implements CachingConfigurationProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachingConfigurationProviderService.class);

    private boolean isCachingEnabled;
    private String headerHtmlPath;

    @Activate
    @Modified
    protected void Activate(final CachingConfiguration cachingConfiguration) {
        isCachingEnabled = cachingConfiguration.madison_caching_enabled();
        headerHtmlPath = cachingConfiguration.madison_caching_header_html_path();
        LOGGER.debug("CachingConfigurationProviderService Activate() Is Dispatcher Caching enabled with AEM : {}",
                isCachingEnabled);
        LOGGER.debug("CachingConfigurationProviderService Activate() Header Component HTML Path : {}",
                headerHtmlPath);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Caching Configuration")
    public @interface CachingConfiguration {

        @AttributeDefinition(
                name = "Is Caching Enabled",
                description = "Enable the checkbox if dispatcher caching is enabled with AEM instance")
        boolean madison_caching_enabled() default false;
        
        @AttributeDefinition(
                name = "Header Component Html File Path",
                description = "Give the path of the header component html file")
        String madison_caching_header_html_path();

    }

    @Override
    public boolean isCachingEnabled() {
        return isCachingEnabled;
    }

    @Override
    public String getHeaderHtmlPath() {
        return headerHtmlPath;
    }

}
