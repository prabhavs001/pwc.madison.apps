package com.pwc.madison.core.models;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Reference Link's composite multifield for search terms which represents the title of search term.
 */
@Model(adaptables = {Resource.class})
public class ReferenceSearchTerms {

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String suggestedTerm;

    /**
     * @return Reference Search Term Title
     */
    public String getSuggestedTerm() {
        return suggestedTerm;
    }

}
