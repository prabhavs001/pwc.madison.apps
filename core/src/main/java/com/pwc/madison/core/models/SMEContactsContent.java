package com.pwc.madison.core.models;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * SME Contact component composite multifield content model represents a link which contains contact url.
 */
@Model(adaptables = { Resource.class })
public class SMEContactsContent {

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String contactPath;

    /**
     * @return contact url
     */
    public String getContactPath() {
        return contactPath;
    }
}
