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

import com.pwc.madison.core.services.TutorialScene7VideoConfigurationService;
import com.pwc.madison.core.services.impl.TutorialScene7VideoConfigurationServiceImpl.TutorialScene7VideoConfiguration;

@Component(service = TutorialScene7VideoConfigurationService.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = TutorialScene7VideoConfiguration.class)
public class TutorialScene7VideoConfigurationServiceImpl implements TutorialScene7VideoConfigurationService{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TutorialScene7VideoConfigurationServiceImpl.class);
	
	private String frenchTutorialLink;
	
	private String japaneseTutorialLink;
	
	@Activate
	@Modified
	protected void activate(final TutorialScene7VideoConfiguration tutorialScene7VideoConfiguration) {
		frenchTutorialLink = tutorialScene7VideoConfiguration.frenchVideoLink();
		japaneseTutorialLink = tutorialScene7VideoConfiguration.japaneseVideoLink();
		LOGGER.debug("TutorialScene7VideoConfiguration Activate() French Tutorial link configured is {}", frenchTutorialLink);
		LOGGER.debug("TutorialScene7VideoConfiguration Activate() Japanese Tutorial link configured is {}", japaneseTutorialLink);
	}
	
	@ObjectClassDefinition(name = "PwC Viewpoint Tutorial Scene7 Video Link Configuration")
	public @interface TutorialScene7VideoConfiguration {

		@AttributeDefinition(name = "French Tutorial Scene7 Video Link", description = "Link for French Tutorial Video hosted on Scene7", type = AttributeType.STRING)
		String frenchVideoLink();
		
		@AttributeDefinition(name = "Japanese Tutorial Scene7 Video Link", description = "Link for Japanese Tutorial Video hosted on Scene7", type = AttributeType.STRING)
		String japaneseVideoLink();

	}
	
	@Override
	public String getFrenchTutorialLink() {
		return frenchTutorialLink;
	}
	
	@Override
	public String getJapaneseTutorialLink() {
		return japaneseTutorialLink;
	}

}
