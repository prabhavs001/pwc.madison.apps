package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.TopicFaqTile;
import com.pwc.madison.core.models.TopicFaqTileContent;
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
        adapters = {TopicFaqTile.class},
        resourceType = {TopicFaqTileImpl.RESOURCE_TYPE})
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TopicFaqTileImpl implements TopicFaqTile {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/topic-faq-tile";

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String title;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource faqs;

    private List<TopicFaqTileContent> topicFaqTileContentList = new ArrayList<>();

    @PostConstruct
    protected void init() {
        if (Objects.nonNull(faqs)) {
            for (Resource faqResource : faqs.getChildren()) {
                TopicFaqTileContent topicFaqTileContent = faqResource.adaptTo(TopicFaqTileContent.class);
                if (Objects.nonNull(topicFaqTileContent))
                    topicFaqTileContentList.add(topicFaqTileContent);
            }
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<TopicFaqTileContent> getTopicFaqTileContentList() {
        return topicFaqTileContentList;
    }
}
