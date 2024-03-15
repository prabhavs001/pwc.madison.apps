package com.pwc.madison.core.reports;

import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Model(adaptables = SlingHttpServletRequest.class)
public class SyndicatedContentTerritoryCellValue {

    private String territory;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    @Optional
    private Resource result;

    @PostConstruct
    private void init() {
        territory = StringUtils.EMPTY;
        if (result != null) {
            String path = result.getPath();
            territory = MadisonUtil.getTerritoryCodeForPath(path);
        }
    }

    public String getTerritory() {
        return territory;
    }

}
