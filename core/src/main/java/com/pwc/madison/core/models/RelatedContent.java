package com.pwc.madison.core.models;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class RelatedContent {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedContent.class);

    private final String ditaTitle;
    private final String ditaBody;
    private final boolean ditaInternal;
    private final String ditaCountry;
    private final String ditaPageUrl;
    private String encodedDitaPageUrl = StringUtils.EMPTY;
    private final String ditaTitleVal;
    private final String ditaContentId;
    private final String ditaContentType;
    private final String ditaPublicationDate;
    private final boolean isShareViaEmailOnly;
    private final String ditaIssuingBody;
    private final ArrayList<String> additionalQuestions;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
    private final String pagePath;
    private final boolean hidePublicationDate;


    public RelatedContent(final String ditaTitle, final String ditaBody, final boolean ditaExternal,
                          final String ditaCountry, final String ditaTitleVal, final String ditaPageUrl, final String ditaContentId,
                          final String ditaContentType, final String ditaPublicationDate, boolean isShareViaEmailOnly, final String ditaIssuingBody, final ArrayList<String> additionalQuestions,String pagePath, final boolean hidePublicationDate) {
        super();
        this.ditaTitle = ditaTitle;
        this.ditaBody = ditaBody;
        ditaInternal = ditaExternal;
        this.ditaTitleVal = ditaTitleVal;
        this.ditaPageUrl = ditaPageUrl;
        this.ditaCountry = ditaCountry;
        this.ditaContentId = ditaContentId;
        this.ditaContentType = ditaContentType;
        this.ditaPublicationDate = ditaPublicationDate;
        this.isShareViaEmailOnly = isShareViaEmailOnly;
        this.ditaIssuingBody = ditaIssuingBody;
        this.additionalQuestions = additionalQuestions;
        try{
            if(StringUtils.isNotBlank(ditaPageUrl)){
                encodedDitaPageUrl = URLEncoder.encode(ditaPageUrl, "UTF-8");
            }
        }catch (UnsupportedEncodingException e) {
            LOGGER.error("Error Encoding the Page URL::: {}", e);
        }
        this.pagePath = pagePath;
        this.hidePublicationDate = hidePublicationDate;

    }

    public String getDitaIssuingBody() {
        return ditaIssuingBody;
    }

    public String getDitaTitle() {
        return ditaTitle;
    }

    public String getDitaBody() {
        return ditaBody;
    }

    public boolean isDitaInternal() {
        return ditaInternal;
    }

    public String getDitaPageUrl() {
        return ditaPageUrl;
    }

    public String getEncodedTitle() {
        try {
            return  URIUtil.encodeAll(ditaTitleVal, "UTF-8");
        } catch (URIException e) {
            LOG.error("Error getting encoded title",e);
        }
        return ditaTitleVal;
    }

    public String getDitaTitleVal() {
        return ditaTitleVal;
    }

    public String getDitaCountry() {
        return ditaCountry;
    }

    public String getDitaContentId() {
        return ditaContentId;
    }

    public String getDitaContentType() {
        return ditaContentType;
    }

    public String getDitaPublicationDate() {
        return ditaPublicationDate;
    }

	public boolean isShareViaEmailOnly() {
		return isShareViaEmailOnly;
	}

    public String getEncodedDitaPageUrl() { return encodedDitaPageUrl; }

    public ArrayList<String> getAdditionalQuestions() { return additionalQuestions; }

    public String getPagePath() {
        return pagePath;
    }

    public boolean isHidePublicationDate() {
        return hidePublicationDate;
    }

}
