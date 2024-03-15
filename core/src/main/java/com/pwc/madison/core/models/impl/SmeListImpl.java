package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.models.SmeList;
import com.pwc.madison.core.models.SmeListItem;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = SmeList.class,
        resourceType = SmeListImpl.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SmeListImpl extends MadisonListImpl implements SmeList {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/sme-list";

    @Inject
    SlingHttpServletRequest request;

    @Inject
    private java.util.List<SmeListItem> smeListItems;
    
    @ValueMapValue
    private String itemType;

    /**
     * Init Method of Model.
     */
    @PostConstruct
    protected void init() {
        smeListItems = new ArrayList<>();
        int count = 1;
        Collection<Page> pages = getPages();

        for (Page page : pages) {
            // The UI allows for only 3 items to be displayed under the SME Accordion
            if (page != null && count <= 3) {
                SmeListItem pageItem = page.adaptTo(SmeListItem.class);
                smeListItems.add(pageItem);
            }
            count++;
        }
    }

    @Override
    public Collection<SmeListItem> getSmeListItems() {
        return smeListItems;
    }
    
    @Override
    public String getItemType() {
        return itemType;
    }

}
