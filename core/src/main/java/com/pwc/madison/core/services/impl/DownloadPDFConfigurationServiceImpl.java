package com.pwc.madison.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.pwc.madison.core.services.DownloadPDFConfigurationService;
import com.pwc.madison.core.services.impl.DownloadPDFConfigurationServiceImpl.DownloadPDFConfiguration;

@Component(
    service = DownloadPDFConfigurationService.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = DownloadPDFConfiguration.class)
public class DownloadPDFConfigurationServiceImpl implements DownloadPDFConfigurationService {

    private String[] excludedPaths;
    private String[] fullGuidePaths;

    @Activate
    @Modified
    protected void activate(final DownloadPDFConfiguration downloadPDFConfiguration) {
        excludedPaths = downloadPDFConfiguration.excludedPaths();
        fullGuidePaths = downloadPDFConfiguration.fullGuidePaths();
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Download PDF Configuration")
    public @interface DownloadPDFConfiguration {

        @AttributeDefinition(
            name = "Excluded Paths",
            description = "Paths where PDF Download needs to be disabled.",
            type = AttributeType.STRING)
        String[] excludedPaths();

        @AttributeDefinition(
                name = "Full Guide Paths",
                description = "Paths where full guide will be downloaded irrespective of map type.",
                type = AttributeType.STRING)
        String[] fullGuidePaths();

    }

    @Override
    public String[] getExcludedPaths() {
        return excludedPaths;
    }

    @Override
    public String[] getFullGuidePaths() {
        return fullGuidePaths;
    }

}
