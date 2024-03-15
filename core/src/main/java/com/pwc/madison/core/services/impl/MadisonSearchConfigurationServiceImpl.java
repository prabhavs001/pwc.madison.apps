package com.pwc.madison.core.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.MadisonSearchConfigurationService;

@Component(
        service = { MadisonSearchConfigurationService.class },
        property = { "service.description=" + "PwC Viewpoint Search Service Configuration implementation" })
@Designate(ocd = MadisonSearchConfigurationServiceImpl.MadisonSearchServiceConfiguration.class)
public class MadisonSearchConfigurationServiceImpl implements MadisonSearchConfigurationService {
    
    private static final String DELIMITER = ":";
    private String[] searchAndPromoteId;
    private static final Logger LOGGER = LoggerFactory.getLogger(MadisonSearchConfigurationServiceImpl.class);
    private Map<String, String> snpAccountMapping;
    
    @Override
    public String getSnPId(String territory) {
        if(StringUtils.isNoneBlank(territory) && snpAccountMapping != null && !snpAccountMapping.isEmpty()) {
            String mapKey = StringUtils.upperCase(territory);
            return snpAccountMapping.containsKey(mapKey) ? snpAccountMapping.get(mapKey) : StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }
    
    @ObjectClassDefinition(name = "PwC Viewpoint Search Configuration")
    public @interface MadisonSearchServiceConfiguration {

        @AttributeDefinition(
            name = "Search And Promote Account Id",
            description = "Search And Promote Account Id to get suggested terms, format locale:snpId (eg en_us:sp12345678)",
            type = AttributeType.STRING,
            cardinality = Integer.MAX_VALUE)
        String[] searchAndPromoteId();
    }
    
    @Activate
    @Modified
    protected void activate(MadisonSearchServiceConfiguration config) {
        this.searchAndPromoteId = config.searchAndPromoteId();
        addSnPAccountMappings();
    }

    private void addSnPAccountMappings() {
       snpAccountMapping = new HashMap<String, String>();
       if(ArrayUtils.isNotEmpty(searchAndPromoteId)) {
           for(String val : searchAndPromoteId) {
               if(StringUtils.containsAny(DELIMITER, val)) {
                   String[] value = StringUtils.split(val, DELIMITER);
                   if(ArrayUtils.isNotEmpty(value) && value.length > 1) {
                       String territory = StringUtils.upperCase(value[0]);
                       LOGGER.debug("locale {} SnP Account {}",territory,value[1]);
                       snpAccountMapping.put(territory, value[1]);
                   }
               }
           }
       }
    }

}
