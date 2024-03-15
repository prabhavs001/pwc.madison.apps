package com.pwc.madison.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.OneTrustCookieConfigurationService;
import com.pwc.madison.core.services.impl.OneTrustCookieConfigurationServiceImpl.OneTrustCookieConfiguration;

@Component(service = OneTrustCookieConfigurationService.class, immediate = true)
@Designate(ocd = OneTrustCookieConfiguration.class)
public class OneTrustCookieConfigurationServiceImpl implements OneTrustCookieConfigurationService{

    private static final Logger LOGGER = LoggerFactory.getLogger(OneTrustCookieConfigurationService.class);

    private String oneTrustScript;
    
    @Activate
    @Modified
    protected void Activate(final OneTrustCookieConfiguration oneTrustCookieConfiguration) {
        oneTrustScript = oneTrustCookieConfiguration.one_trust_script();

        LOGGER.debug("OneTrustCookieConfigurationServiceImpl Activate() One Trust Script : {}",
                oneTrustScript);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint One Trust Configuration")
    public @interface OneTrustCookieConfiguration {

        @AttributeDefinition(name = "One Trust Script", description = "Enter complete one trust script with tags.")
        String one_trust_script();
        
    }


    @Override
    public String getOneTrustScript() {
        return oneTrustScript;
    }

}
