package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import com.adobe.cq.export.json.ExporterConstants;
import com.pwc.madison.core.models.TocJoinedPagesModel;

@Model(adaptables = SlingHttpServletRequest.class,
        resourceType = TocJoinedPagesModelImpl.RESOURCE_TYPE, adapters = TocJoinedPagesModel.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TocJoinedPagesModelImpl implements TocJoinedPagesModel {

	public static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/toc-joined";

	@ValueMapValue
    private String joinedTopics;

    private List<String> joinedPagesTopicsList = new ArrayList<>();

    @PostConstruct
    protected void init() {
    	if(StringUtils.isNotBlank(joinedTopics)) {
    		String[] array = joinedTopics.split(",");
    		joinedPagesTopicsList = Arrays.asList(array);
    	}
    }
	@Override
	public List<String> getJoinedPagesTopics() {
		return joinedPagesTopicsList;
	}

}
