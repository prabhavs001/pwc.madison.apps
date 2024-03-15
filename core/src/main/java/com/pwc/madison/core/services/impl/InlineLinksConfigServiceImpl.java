package com.pwc.madison.core.services.impl;

import com.pwc.madison.core.services.InlineLinksConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = InlineLinksConfigService.class, immediate = true)
@Designate(ocd = InlineLinksConfigServiceImpl.InlineLinksConfig.class)
public class InlineLinksConfigServiceImpl implements InlineLinksConfigService {
	
	private static final String NO = "no";
	private static final String YES = "yes";
	private boolean disableInlineLinks;

    @Activate
    protected void activate(InlineLinksConfig config) {
    	disableInlineLinks = config.inlineLinksDisabled();
    }

    @Override
    public String getDisableInlineLinks() {
    	
    	return disableInlineLinks ? YES : NO;
    }

    @ObjectClassDefinition(
            name = "PwC Viewpoint Inline Links Configuration")
    public @interface InlineLinksConfig {

    	/**
    	 * inlineLinksDisabled
    	 * 
    	 * @return inlineLinksDisabled
    	 */
    	@AttributeDefinition(name = "Disable Inline Links", description = "Disable Inline Links from pages", type = AttributeType.BOOLEAN)
    	boolean inlineLinksDisabled() default false;
    }
}
