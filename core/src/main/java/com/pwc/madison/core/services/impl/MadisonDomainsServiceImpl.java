package com.pwc.madison.core.services.impl;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.MadisonDomainsService;

@Component(
        service = { MadisonDomainsService.class },
        property = { "service.description=" + "PwC Viewpoint Domains Service implementation" })
@Designate(ocd = MadisonDomainsServiceImpl.MadisonDomainsServiceConfiguration.class)
public class MadisonDomainsServiceImpl implements MadisonDomainsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MadisonDomainsService.class);

    private static final String MADISON_BASE_PATH = "/content/pwc-madison";
    private static final String MADISON_DITA_NODE = "/ditaroot/";
    private static final String MADISON_DITA_SHORTEN_NODE = "/dt/";

    private String defaultDomain;

    private String publishDomain;

    @ObjectClassDefinition(name = "PwC Viewpoint Domains Configuration")
    public @interface MadisonDomainsServiceConfiguration {

        @AttributeDefinition(name = "Default Domain", description = "Default domain")
        String default_domain();

        @AttributeDefinition(name = "Publish Domain", description = "Domain for publisher")
        String publish_domain();
    }

    @Activate
    @Modified
    protected void activate(MadisonDomainsServiceConfiguration config) {
        this.defaultDomain = config.default_domain();
        this.publishDomain = config.publish_domain();
        LOGGER.debug("MadisonDomainsServiceImpl activate() : Default Domain : {}", defaultDomain);
        LOGGER.debug("MadisonDomainsServiceImpl activate() : Publish Domain : {}", publishDomain);
    }

    @Override
    public String getDefaultDomain() {
        return defaultDomain;
    }

    @Override
    public String getPublishDomain() {
        return publishDomain;
    }

    @Override
    public String getPublishedPageUrl(final String pagePath, final boolean addExtension) {
        if (null != pagePath) {
            return publishDomain + pagePath.replace(MADISON_BASE_PATH, StringUtils.EMPTY).replace(MADISON_DITA_NODE,
                    MADISON_DITA_SHORTEN_NODE) + (addExtension ? MadisonConstants.HTML_EXTN : StringUtils.EMPTY);
        }
        return null;
    }

}
