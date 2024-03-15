package com.pwc.madison.core.models.impl;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.FootNoteLink;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = FootNoteLink.class,
    resourceType = FootNoteLinkImpl.RESOURCE_TYPE)
public class FootNoteLinkImpl implements FootNoteLink {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/footnotelink";
    private static final String IDEDFOOTNOTE = "idedfootnotes";

    @ValueMapValue
    @Default(values = "#")
    private String link;

    private String text = StringUtils.EMPTY;
    private String callOut = StringUtils.EMPTY;

    @Self
    private SlingHttpServletRequest slingRequest;

    @PostConstruct
    protected void init() {
        int lastSlash = link.lastIndexOf(MadisonConstants.UNDERSCORE);
        String fnId = lastSlash != -1 ? link.substring(lastSlash + 1) : link;
        HashMap<String, Object> idFootNotes = (HashMap<String, Object>) slingRequest.getAttribute(IDEDFOOTNOTE);
        if (null != idFootNotes) {
            HashMap<String, Object> footNote = (HashMap<String, Object>) idFootNotes.get(fnId);
            if (null != footNote) {
                footNote.put(DITAConstants.SHOW, true);
                text = (String) footNote.get(DITAConstants.FOOTNOTE_TEXT);
                callOut = (String) footNote.get(DITAConstants.CALLOUT_TEXT);
            }
        }
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getCallOut() {
        return callOut;
    }

}
