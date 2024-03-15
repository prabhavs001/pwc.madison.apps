package com.pwc.madison.core.models;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.json.JSONException;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Model(adaptables = {SlingHttpServletRequest.class})
public class ProgressIndicator {

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource progressIndicatorLinks;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String progressIndicatorTitle;

    private Map<String, String> elements;

    @PostConstruct
    protected void init() throws JSONException {
        if (progressIndicatorLinks != null) {
            Iterator<Resource> iterator = progressIndicatorLinks.listChildren();
            elements = new LinkedHashMap<>();
            while (iterator.hasNext()) {
                Resource value = iterator.next();
                ValueMap valueMap = value.adaptTo(ValueMap.class);
                if (valueMap != null) {
                    elements.put(valueMap.get("id").toString(), valueMap.get("title").toString());
                }
            }
        }

    }

    public String getProgressIndicatorTitle() {
        return progressIndicatorTitle;
    }

    public Map<String, String> getElements() {
        return elements;
    }

}
