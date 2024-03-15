package com.pwc.madison.core.authorization.enums;

/**
 * Defines the audience types which are assigned to Viewpoint pages for authorization. The Audience type options are
 * placed in refdata on path "/content/pwc-madison/global/reference-data/authorization/audience-type". This Enum have to
 * be in sync with the refdata values.
 */
public enum AudienceType {

    INTERNAL_ONLY("internalOnly"),
    EXTERNAL_ONLY("externalOnly"),
    INTERNAL_AND_EXTERNAL("internalExternal"),
    PRIVATE_GROUP("privateGroup");

    private String value;

    AudienceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
