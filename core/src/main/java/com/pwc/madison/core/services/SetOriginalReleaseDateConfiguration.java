package com.pwc.madison.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Config for controlling original-release-date setting code
 *
 */
@ObjectClassDefinition(name = "PwC Viewpoint Set Original Release Date Config", description = "Configuration for controlling original-release-date listener")
public @interface SetOriginalReleaseDateConfiguration {


    /**
     * serviceEnabled
     *
     * @return serviceEnabled
     */
    @AttributeDefinition(name = "Enabled", description = "Enable/Disable listener to set original-release-date", type = AttributeType.BOOLEAN)
    boolean serviceEnabled() default true;


}
