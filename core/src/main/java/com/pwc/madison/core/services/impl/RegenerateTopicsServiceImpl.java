package com.pwc.madison.core.services.impl;

import com.pwc.madison.core.services.RegenerateTopicsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the OSGI configuration to configure root DITAMAP against which output regeneration is done in
 * case of fullcycle and simple workflow.
 */
@Component(service = RegenerateTopicsService.class,configurationPolicy= ConfigurationPolicy.REQUIRE)
@Designate(ocd = RegenerateTopicsServiceImpl.RegenerateTopicsRootMapConfig.class)
public class RegenerateTopicsServiceImpl implements RegenerateTopicsService {

    String[] rootDitaMapPaths;

    @Activate
    protected void Activate(RegenerateTopicsRootMapConfig config) {
        rootDitaMapPaths = config.getRootDitaMaps();
    }

    @Override
    public Map<String, String> getRootDitaMaps() {
        Map<String, String> configMap = new HashMap<String, String>();
        for (String ditamap : rootDitaMapPaths) {
            String[] configValue = ditamap.split("=");
            if (configValue.length > 1) {
                configMap.put(configValue[0], configValue[1]);
            }
        }
        return configMap;
    }

    @ObjectClassDefinition(name = "PwC Viewpoint - Regenerate Topics Root Configuration", description = "To configure root Ditamap paths for each content hierarchy")
    public @interface RegenerateTopicsRootMapConfig {
        @AttributeDefinition(name = "Root Ditamap mapper", description = "<Folder name under territory> = <Ditamap name> eg; pwc=pwc_material.US.ditamap")
        String[] getRootDitaMaps();
    }
}
