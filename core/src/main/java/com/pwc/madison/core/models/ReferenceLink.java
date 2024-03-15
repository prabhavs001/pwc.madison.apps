package com.pwc.madison.core.models;

import java.util.List;

/**
 * Reference Link's Model
 * This component would be used for displaying featured terms.
 * Depending the reference link type the respective arraylist's will be populated.
 */
public interface ReferenceLink {

    /**
     * @return Reference Link's Content Type
     */
    String getReferenceLinkType();

    /**
     * This list will be generated when Reference Link type is Helpful Link
     * @return list of helpful links
     */
    List<LinkField> getReferenceLinkList();

    /**
     * This list will be generated when Reference Link type is Suggested content
     * @return return list of suggested content
     */
    List<ReferenceSearchTerms> getSuggestedTermList();

    /**
     * This will process the searchURL
     * @return searchURL
     */
    public String getSearchUrl();
}
