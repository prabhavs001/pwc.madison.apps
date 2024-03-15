package com.pwc.madison.core.authorization.models;

public class ContentAuthorization {

    private String audienceType;
    private String accessLevel;
    private String[] licenses;
    private String[] privateGroups;
    private boolean isInternalTerritory;

    public ContentAuthorization(String audienceType, String accessLevel, String[] licenses, String[] privateGroups, boolean isInternalTerritory) {
        super();
        this.audienceType = audienceType;
        this.accessLevel = accessLevel;
        this.licenses = licenses;
        this.privateGroups = privateGroups;
        this.isInternalTerritory = isInternalTerritory;
    }

    public String getAudienceType() {
        return audienceType;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public String[] getLicenses() {
        return licenses;
    }

    public void setLicenses(String[] licenses) {
        this.licenses = licenses;
    }

    public String[] getPrivateGroups() {
        return privateGroups;
    }

    public void setPrivateGroups(String[] privateGroups) {
        this.privateGroups = privateGroups;
    }

    public void setAudienceType(String audienceType) {
        this.audienceType = audienceType;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public boolean getIsInternalTerritory() {
        return isInternalTerritory;
    }

    public void setIsInternalTerritory(boolean isInternalTerritory) {
        this.isInternalTerritory = isInternalTerritory;
    }

    public ContentAuthorization() {
    }

}
