package com.pwc.madison.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.ExternalRedirectDomainConfigurationService;
import com.pwc.madison.core.services.impl.ExternalRedirectDomainConfigurationServiceImpl.ExternalRedirectDomainConfiguration;

@Component(service = ExternalRedirectDomainConfigurationService.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ExternalRedirectDomainConfiguration.class)
public class ExternalRedirectDomainConfigurationServiceImpl implements ExternalRedirectDomainConfigurationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalRedirectDomainConfigurationServiceImpl.class);

	private String[] domainList;
	
	private String territories;
	
	private String informDomain;

	@Activate
	@Modified
	protected void activate(final ExternalRedirectDomainConfiguration externalRedirectDomainConfiguration) {
		domainList = externalRedirectDomainConfiguration.domains();
		informDomain = externalRedirectDomainConfiguration.informDomain();
		territories = externalRedirectDomainConfiguration.territoryList();
		LOGGER.debug("ExternalRedirectDomainConfiguration Activate() List of Domains are {}", domainList);
		LOGGER.debug("ExternalRedirectDomainConfiguration Activate() Inform Domain URL is {}", informDomain);
		LOGGER.debug("ExternalRedirectDomainConfiguration Activate() List of Viewpoint Territories requiring custom pop up message are {}", territories);
	}

	@ObjectClassDefinition(name = "PwC Viewpoint External Redirect Domain Configuration")
	public @interface ExternalRedirectDomainConfiguration {

		@AttributeDefinition(name = "List of Domains", description = "List of Domains used by Referrer Header for External Redirect to Viewpoint", type = AttributeType.STRING)
		String[] domains();
		
		@AttributeDefinition(name = "Inform Domain URL", description = "Inform Domain URL for external pop-up message for users coming from Inform")
		String informDomain();
		
		@AttributeDefinition(name = "List of Territories", description = "CSV of Viewpoint Territories which requires custom pop up message for users coming from External Domain to Viewpoint")
		String territoryList();

	}

	@Override
	public String[] getDomainList() {
		return domainList;
	}
	
	@Override
	public String getInformDomain() {
		return informDomain;
	}
	
	@Override
	public String getTerritories() {
		return territories;
	}

}
