package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.QuickAccess;
import com.pwc.madison.core.models.QuickAccessContent;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {QuickAccess.class},
        resourceType = {QuickAccessImpl.RESOURCE_TYPE})
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class QuickAccessImpl implements QuickAccess {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/quick-access";

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource subsectionContent;

    private List<QuickAccessContent> content = new ArrayList<>();

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String title;

    @PostConstruct
    protected void init() {
        if (subsectionContent != null) {
            for (Resource link : subsectionContent.getChildren()) {
                QuickAccessContent quickAccessContent = link.adaptTo(QuickAccessContent.class);
                if (Objects.nonNull(quickAccessContent))
                    content.add(quickAccessContent);
            }
        }
    }

    @Override
    public List<QuickAccessContent> getContent() {
        return content;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
