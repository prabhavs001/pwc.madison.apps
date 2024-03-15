package com.pwc.madison.core.userreg.models.request;

/**
 * 
 * Model to represent accept tnc request data to send to UserReg Rest tnc API.
 *
 */
public class UserAcceptTncRequest extends UserRegRequest {

    private String territoryCode;

    private String localeAccepted;

    private String referrerAccepted;

    private String versionAccepted;

    public String getTerritoryCode() {
        return territoryCode;
    }

    public void setTerritoryCode(final String territoryCode) {
        this.territoryCode = territoryCode;
    }

    public String getLocaleAccepted() {
        return localeAccepted;
    }

    public void setLocaleAccepted(final String localeAccepted) {
        this.localeAccepted = localeAccepted;
    }

    public String getReferrerAccepted() {
        return referrerAccepted;
    }

    public void setReferrerAccepted(final String referrerAccepted) {
        this.referrerAccepted = referrerAccepted;
    }

    public String getVersionAccepted() {
        return versionAccepted;
    }

    public void setVersionAccepted(final String versionAccepted) {
        this.versionAccepted = versionAccepted;
    }

    public UserAcceptTncRequest(final String territoryCode, final String localeAccepted,
            final String referrerAccepted, final String versionAccepted) {
        super();
        this.territoryCode = territoryCode;
        this.localeAccepted = localeAccepted;
        this.referrerAccepted = referrerAccepted;
        this.versionAccepted = versionAccepted;
    }

}
