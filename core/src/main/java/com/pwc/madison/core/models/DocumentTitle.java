package com.pwc.madison.core.models;

import java.util.List;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code DocumentTitle} Sling Model used for the
 * {@code /apps/pwc-madison/components/ditacontent/documenttitle} component.
 *
 */
@ConsumerType
public interface DocumentTitle {

    /**
     * Returns the Section title for the DITA Document.
     *
     * @return Section Title.
     * @see #getSectionTitle()
     *
     */
    public String getSectionTitle();

    /**
     * Returns the Sub title for the DITA Document.
     *
     * @return Sub Title.
     * @see #getSubTitle()
     *
     */
    public String getSubTitle();

    /**
     * Returns the Published Date for the DITA Document.
     *
     * @return Published Date.
     * @see #getPubhlishedDate()
     *
     */
    public String getPubhlishedDate();

    /**
     * Returns the LogoImg Path for the DITA Document.
     *
     * @return Image Path.
     * @see #getImgUrl()
     *
     */
    public String getImgPath();

    /**
     * Returns is InternalUser.
     *
     * @return isInternalUser.
     * @see #getIsInternalUser()
     *
     */
    public Boolean getIsInternalUser();

    /**
     * Returns the revised Date for the DITA Document.
     *
     * @return revised Date.
     * @see #getRevisedDate()
     *
     */
    public String getRevisedDate();

    String getPwcCountry();

    String getSource();

    String getContentType();

    String getPwcSourceValue();

    List<String> getPrivateGroupList();

    public boolean isPrivateUser();

    /**
     * @return hidePublicationdate flag
     */
    boolean isHidePublicationDate();

	boolean isJoinedTopic();
}
