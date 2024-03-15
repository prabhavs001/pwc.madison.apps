package com.pwc.madison.core.services;

import org.apache.sling.caconfig.annotation.Configuration;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;

@Configuration(label = "FASB Maps Exclusion Configuration", description = "Configuration to maintain the list of maps that needs to be excluded from numbering")
public @interface TocContentServiceConfig {

    @AttributeDefinition(name = "List of Map path (with generic numbering) and Level", description = "Add the relative path of Map (that has generic numbering) and level with pipe symbol(|) as a separator, which will be excluded from the TOC numbering. Eg. Codification/Assets/Investments|2", type = AttributeType.STRING)
    String[] excludedMaps() default {"Codification/Assets/Investments|2","Codification/Expenses/Compensation|2","Codification/Industry/Contractors|2","Codification/Industry/Entertainment|2","Codification/Industry/Extractive_Activities|2","Codification/Industry/Financial_Services|2","Codification/Industry/Plan_Accounting|2","Codification/Industry/Real_Estate|2"};
}
