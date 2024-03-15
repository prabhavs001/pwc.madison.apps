package com.pwc.madison.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.MadisonSystemUserNameProviderService;

@Component(
        service = { MadisonSystemUserNameProviderService.class },
        property = { "service.description=" + "PwC Viewpoint System User Name Provider Service implementation" })
@Designate(ocd = MadisonSystemUserNameProviderServiceImpl.MadisonSystemUserNameProviderServiceConfiguration.class)
public class MadisonSystemUserNameProviderServiceImpl implements MadisonSystemUserNameProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MadisonSystemUserNameProviderServiceImpl.class);
    
    private String fmditaServiceName;

    private String replicationServiceName;

    @ObjectClassDefinition(name = "PwC Viewpoint Service Name provider Configuration")
    public @interface MadisonSystemUserNameProviderServiceConfiguration {

        @AttributeDefinition(name = "FMdita Service Name")
        String fmdita_service_name();

        @AttributeDefinition(name = "Replication Service Name")
        String replication_service_name();
    }

    @Activate
    @Modified
    protected void activate(MadisonSystemUserNameProviderServiceConfiguration config) {
        this.fmditaServiceName = config.fmdita_service_name();
        this.replicationServiceName = config.replication_service_name();
        LOGGER.debug("MadisonDomainsServiceImpl activate() : fmdita service name : {}", fmditaServiceName);
        LOGGER.debug("MadisonDomainsServiceImpl activate() : Replication service name : {}", replicationServiceName);
    }


    @Override
    public String getFmditaServiceUsername() {
        return fmditaServiceName;
    }

    @Override
    public String getReplicationServiceUsername() {
        return replicationServiceName;
    }

}
