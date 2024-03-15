package com.pwc.madison.core.models.impl;

import com.pwc.madison.core.models.QuoteModel;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.designer.Style;

/**
 * The Class QuoteModelImpl.
 */
@Model(adaptables = { SlingHttpServletRequest.class,
        Resource.class }, adapters = { QuoteModel.class,ComponentExporter.class}, resourceType = QuoteModelImpl.RESOURCE_TYPE, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class QuoteModelImpl implements QuoteModel {

    /** The Constant RESOURCE_TYPE. */
    public static final String RESOURCE_TYPE = "pwc-madison/components/inloop/quotebox";

    /** The current style. */
    @ScriptVariable
    protected Style currentStyle;

    /** The quote text. */
    @ValueMapValue
    private String quoteText;

    /** The quote author. */
    @ValueMapValue
    private String quoteAuthor;

    /** The quote source. */
    @ValueMapValue
    private String quoteSource;

    /* (non-Javadoc)
     * @see com.pwc.modernized.model.QuoteModel#getQuoteText()
     */
    @Override
    public String getQuoteText() {
        return quoteText;
    }

    /* (non-Javadoc)
     * @see com.pwc.modernized.model.QuoteModel#getQuoteSource()
     */
    @Override
    public String getQuoteSource() {
        return quoteSource;
    }

    /* (non-Javadoc)
     * @see com.pwc.modernized.model.QuoteModel#getQuoteAuthor()
     */
    @Override
    public String getQuoteAuthor() {
        return quoteAuthor;
    }

    /**
     * Gets the checks if is footer enabled.
     *
     * @return the checks if is footer enabled
     */
    public Boolean getIsFooterEnabled() {
        return StringUtils.isNotBlank(this.quoteAuthor) || StringUtils.isNotBlank(this.quoteSource);
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }

}
