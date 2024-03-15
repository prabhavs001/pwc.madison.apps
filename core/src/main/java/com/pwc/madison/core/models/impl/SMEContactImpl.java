package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.SMEContact;
import com.pwc.madison.core.models.SMEContactsContent;
import com.pwc.madison.core.models.SmeListItem;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { SMEContact.class },
        resourceType = { SMEContactImpl.RESOURCE_TYPE })

@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SMEContactImpl implements SMEContact {

    private static final Logger LOGGER = LoggerFactory.getLogger(SMEContact.class);

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/sme-contact";

    private List<SmeListItem> contacts;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String title;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource contactInfo;

    @Self
    private SlingHttpServletRequest request;

    @PostConstruct
    protected void init() throws LoginException {
        LOGGER.debug("Contacts Resource Node Paths ",contactInfo);
        ResourceResolver resourceResolver = request.getResourceResolver();
        if (contactInfo != null && resourceResolver != null) {
            contacts=new ArrayList<>();
            for(Resource resource : contactInfo.getChildren()){
                SMEContactsContent smeContactsContent = resource.adaptTo(SMEContactsContent.class);
                if(Objects.nonNull(smeContactsContent) ){
                    String contactUrl = smeContactsContent.getContactPath();
                    Resource contactResource = resourceResolver.getResource(contactUrl);
                    SmeListItem contact = Objects.isNull(contactResource) ? null : contactResource.adaptTo(SmeListItem.class);
                    if(Objects.nonNull(contact)) {
                        contacts.add(contact);
                    }
                }
            }
            LOGGER.debug("Contacts List with all information fetch node "+contacts);
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<SmeListItem> getContacts() {
        return contacts;
    }
}
