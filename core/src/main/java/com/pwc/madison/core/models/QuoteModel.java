package com.pwc.madison.core.models;

import com.adobe.cq.export.json.ComponentExporter;

/**
 * The Interface QuoteModel.
 */
public interface QuoteModel extends ComponentExporter {

    /**
     * Gets the quote text.
     *
     * @return the quote text
     */
    String getQuoteText();

    /**
     * Gets the quote source.
     *
     * @return the quote source
     */
    String getQuoteSource();

    /**
     * Gets the quote author.
     *
     * @return the quote author
     */
    String getQuoteAuthor();
}
