package com.pwc.madison.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code PWCTopicHeader} Sling Model used for the
 * {@code /apps/pwc-madison/components/ditacontent/pwctopicheader} component.
 *
 */
@ConsumerType
public interface PWCTopicHeader {

    /**
     * Returns the Numbered Header for the PWC authored topics such as Examples, FAQ etc.
     *
     * @return Numbered Header.
     * @see #getNumberedHeader()
     *
     */
    public String getNumberedHeader();

    /**
     * Returns the Header for the PWC authored topics such as Examples, FAQ etc.
     *
     * @return topic Header.
     * @see #getTopicHeader()
     *
     */
    public String getTopicHeader();

    public Boolean showWrapper();

    /**
     * Returns the Header for the PWC authored topics such as Examples/faq, FAQ etc.
     *
     * @return topic Header.
     * @see #getTopicType()
     *
     */
    public String getTopicType();

    /**
     * Returns the list of pwc-faq question-text resource, FAQ etc.
     *
     * @return QuestionResourceList
     */
    public List<String> getFaqQuestionResourceList();
}
