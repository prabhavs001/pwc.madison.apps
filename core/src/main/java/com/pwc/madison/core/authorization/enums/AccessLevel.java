package com.pwc.madison.core.authorization.enums;

/**
 * Defines the access level which are assigned to Viewpoint pages for authorization. The access level options are placed
 * in refdata on path "/content/pwc-madison/global/reference-data/authorization/access-level". This Enum have to be in
 * sync with the refdata values.
 */
public enum AccessLevel {

    FREE("free"), PREMIUM("premium"), LICENSED("licensed");

    private String value;

    AccessLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
