package com.pwc.madison.core.models.impl;


import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.models.ContactsCollectionModel;
import com.pwc.madison.core.models.SmeListItem;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = ContactsCollectionModel.class,
        resourceType = ContactsCollectionModelImpl.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ContactsCollectionModelImpl implements ContactsCollectionModel{
    public static final String RESOURCE_TYPE = "pwc-madison/components/inloop/contact-collection";

    @ValueMapValue
    private List<String> pages;

    @ScriptVariable
    private ResourceResolver resolver;

    @Inject
    private List<SmeListItem> contacts;

    @ValueMapValue
    private String heading;

    @PostConstruct
    protected void init() {
        contacts = new ArrayList<>();
        if(pages!=null) {
            for (String path :
                    pages) {
                Resource resource = resolver.getResource(path);
                SmeListItem contact = Objects.isNull(resource) ? null : resource.adaptTo(SmeListItem.class);
                if (Objects.nonNull(contact)) {
                    contacts.add(contact);
                }
            }
        }
    }

    /**
     * returns SME item info
     * @return
     */
    @Override
    public List<SmeListItem> getContacts() {
        return contacts;
    }

    @Override
    public String getHeading(){
        return heading;
    }
}
