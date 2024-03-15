package com.pwc.madison.core.userreg.models;

/**
 * License POJO class that provides getter and setters for the product name and expiry timestamp.
 */
public class License {

    private String code;
    private Long expiryTs;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public Long getExpiryTs() {
        return expiryTs;
    }

    public void setExpiryTs(final Long expiryTs) {
        this.expiryTs = expiryTs;
    }

    public License(final String code, final Long expiryTs) {
        super();
        this.code = code;
        this.expiryTs = expiryTs;
    }

    @Override
    public String toString() {
        return "ProductContent [code=" + code + ", expiryTs=" + expiryTs + "]";
    }

}
