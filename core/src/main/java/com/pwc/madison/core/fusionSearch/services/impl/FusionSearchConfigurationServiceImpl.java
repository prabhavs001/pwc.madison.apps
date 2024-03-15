package com.pwc.madison.core.fusionSearch.services.impl;

import java.util.Arrays;
import java.util.List;

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

import com.pwc.madison.core.fusionSearch.services.FusionSearchConfigurationService;

import com.pwc.madison.core.fusionSearch.services.impl.FusionSearchConfigurationServiceImpl.FusionSearchConfiguration;

@Component(service = FusionSearchConfigurationService.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = FusionSearchConfiguration.class)
public class FusionSearchConfigurationServiceImpl implements FusionSearchConfigurationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FusionSearchConfigurationServiceImpl.class);

	private String username;

	private String password;

	private String searchEndpoint;

	private String typeaheadEndpoint;

	private String indexingEndpoint;
	
	private String signalEndpoint;
	
	private List <String> indexingLocales;

	@Activate
	@Modified
	protected void activate(final FusionSearchConfiguration fusionSearchConfiguration) {
		username = fusionSearchConfiguration.username();
		password = fusionSearchConfiguration.password();
		searchEndpoint = fusionSearchConfiguration.searchEndpoint();
		typeaheadEndpoint = fusionSearchConfiguration.typeaheadEndpoint();
		indexingEndpoint = fusionSearchConfiguration.indexingEndpoint();
		indexingLocales = Arrays.asList(fusionSearchConfiguration.indexingLocales());
		signalEndpoint = fusionSearchConfiguration.signalEndpoint();
		LOGGER.debug("FusionSearchConfiguration activate() Fusion username is {}", username);
		LOGGER.debug("FusionSearchConfiguration activate() Fusion password is {}", password);
		LOGGER.debug("FusionSearchConfiguration activate() Fusion searchEndpoint is {}", searchEndpoint);
		LOGGER.debug("FusionSearchConfiguration activate() Fusion typeaheadEndpoint is {}", typeaheadEndpoint);
		LOGGER.debug("FusionSearchConfiguration activate() Fusion indexingEndpoint is {}", indexingEndpoint);
		LOGGER.debug("FusionSearchConfiguration activate() Fusion indexingLocales is {}", indexingLocales);
        LOGGER.debug("FusionSearchConfiguration activate() Fusion signalEndpoint is {}", signalEndpoint);
	}

	@ObjectClassDefinition(name = "PwC Viewpoint Fusion Search Configuration")
	public @interface FusionSearchConfiguration {

		@AttributeDefinition(name = "Username", description = "Username for the Fusion Viewpoint Configuration", type = AttributeType.STRING)
		String username();

		@AttributeDefinition(name = "Password", description = "Password for the Fusion Viewpoint Configuration", type = AttributeType.PASSWORD)
		String password();

		@AttributeDefinition(name = "Fusion Search Endpoint", description = "Search Endpoint for the Fusion Viewpoint Configuration", type = AttributeType.STRING)
		String searchEndpoint();

		@AttributeDefinition(name = "Fusion Typeahead Endpoint", description = "Typeahead Endpoint for the Fusion Viewpoint Configuration", type = AttributeType.STRING)
		String typeaheadEndpoint();

		@AttributeDefinition(name = "Fusion Indexing Endpoint", description = "Indexing Endpoint for the Fusion Viewpoint Configuration", type = AttributeType.STRING)
		String signalEndpoint();

        @AttributeDefinition(name = "Fusion Signal Endpoint", description = "Signal Endpoint for the Fusion Viewpoint Configuration", type = AttributeType.STRING)
        String indexingEndpoint();
        
		@AttributeDefinition(name = "Fusion Indexing Locales", description = "Indexing Locales for the Fusion Viewpoint Configuration", type = AttributeType.STRING)
		String[] indexingLocales();

	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getSearchEndpoint() {
		return searchEndpoint;
	}

	@Override
	public String getTypeaheadEndpoint() {
		return typeaheadEndpoint;
	}

	@Override
	public String getIndexingEndpoint() {
		return indexingEndpoint;
	}

	@Override
	public List<String> getIndexingLocales() {
		return indexingLocales;
	}

    @Override
    public String getSignalEndpoint() {
        return signalEndpoint;
    }

}