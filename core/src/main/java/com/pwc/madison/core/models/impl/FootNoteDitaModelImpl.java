package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.FootNoteDitaModel;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = FootNoteDitaModel.class,
    resourceType = FootNoteDitaModelImpl.RESOURCE_TYPE)
public class FootNoteDitaModelImpl implements FootNoteDitaModel {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/ditacontent/footnote";
    private static final Logger LOGGER = LoggerFactory.getLogger(FootNoteDitaModelImpl.class);
    private static final String UID_SRC = "uidsrc";
    private static final String UID_TARGET = "uidtarget";
    private static final String IDED_FOOTNOTES = "idedfootnotes";
    private static final String ANNON_FOOTNOTES = "anonfootnotes";
    private static final String NUMERIC_ONE = "1";

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String text;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String callout;

    @ValueMapValue
    @Default(values = StringUtils.EMPTY)
    private String id;

    private Boolean show;

    @Self
    private SlingHttpServletRequest slingRequest;

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getCallout() {
        return callout;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Boolean getShow() {
        return this.show;
    }

    @PostConstruct
    protected void init() {
        String uidsrc;
        String uidtarget;

        if (StringUtils.isBlank(callout)) {
            callout = (String) slingRequest.getAttribute(DITAConstants.CALLOUT_TEXT);
            callout = callout == null ? NUMERIC_ONE : callout;

        }
		
			
		// get Footnotes without ID
		List<HashMap<String, Object>> anonFootNotes = (List) slingRequest.getAttribute(ANNON_FOOTNOTES);

		// get Footnotes with ID
		Map<String, Object> idEdfootNotes = (Map) slingRequest.getAttribute(IDED_FOOTNOTES);
		try{
			// get the size of Ided FootNotes
			int idEdFnSize = 0;
			if (null != idEdfootNotes) {
				idEdFnSize = idEdfootNotes.size();
			}

			// if callout is already present in request anonFootNotes, reset value
			if (null != anonFootNotes) {
				for (Map<String, Object> footNoteMap : anonFootNotes) {
					if (footNoteMap.get(DITAConstants.CALLOUT_TEXT).equals(callout)) {
						callout = String.valueOf(anonFootNotes.size() + idEdFnSize + 1);
					}
				}
			}

			// if callout is already present in request idEdfootNotes, reset value
			if (null != idEdfootNotes) {
				for (Entry<String, Object> footNoteEntry : idEdfootNotes.entrySet()) {
					Map<String, Object> fnMap = (Map) footNoteEntry.getValue();
					if (null != fnMap && fnMap.get(DITAConstants.CALLOUT_TEXT).equals(callout)) {
						callout = String.valueOf(anonFootNotes.size() + idEdFnSize + 1);
					}
				}
			}

			// set callout text in request
			slingRequest.setAttribute(DITAConstants.CALLOUT_TEXT, StringUtils.EMPTY + (Integer.parseInt(callout) + 1));
		}catch (NumberFormatException e){
            LOGGER.error("Wrong Authoring on Footnote - callout value contains non-numberic values");
        }catch (Exception e){
            LOGGER.error("Error on FootNote", e);
        }

        long currentMillis = System.currentTimeMillis();
        uidsrc = "fnsrc_" + currentMillis;
        uidtarget = "fntarget_" + (currentMillis + 1);

        if (StringUtils.isBlank(id)) {
            HashMap<String, Object> footNote = new HashMap<>();
            footNote.put(DITAConstants.FOOTNOTE_TEXT, text);
            footNote.put(DITAConstants.CALLOUT_TEXT, callout);
            footNote.put(UID_SRC, uidsrc);
            footNote.put(UID_TARGET, uidtarget);
            footNote.put(DITAConstants.SHOW, true);
            this.show = true;

            if (null == anonFootNotes) {
                anonFootNotes = new ArrayList<HashMap<String, Object>>();
            }
            anonFootNotes.add(footNote);
            slingRequest.setAttribute(ANNON_FOOTNOTES, anonFootNotes);
        } else {
            HashMap<String, Object> footNote = new HashMap<>();
            footNote.put(DITAConstants.FOOTNOTE_TEXT, text);
            footNote.put(DITAConstants.CALLOUT_TEXT, callout);
            footNote.put(UID_SRC, uidsrc);
            footNote.put(UID_TARGET, uidtarget);
            footNote.put(DITAConstants.SHOW, false);
            this.show = false;

            if (null == idEdfootNotes) {
                idEdfootNotes = new HashMap<>();
            }
            idEdfootNotes.put(id, footNote);
            slingRequest.setAttribute(IDED_FOOTNOTES, idEdfootNotes);
        }
    }

}
