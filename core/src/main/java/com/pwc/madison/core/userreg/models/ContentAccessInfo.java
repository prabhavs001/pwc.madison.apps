package com.pwc.madison.core.userreg.models;

import java.util.List;

/**
 * ContentAccessInfo POJO class that provides getter and setters content access information.
 */
public class ContentAccessInfo {
    
    public ContentAccessInfo() {
        super();
    }

    private List<License> licenses;
    private List<String> privateGroups;
    private boolean concurrentLicensedUser;
    private List<String> unavailableLicenses;

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(final List<License> licenses) {
        this.licenses = licenses;
    }

    public List<String> getPrivateGroups() {
        return privateGroups;
    }

    public void setPrivateGroup(final List<String> privateGroups) {
        this.privateGroups = privateGroups;
    }

    public boolean isConcurrentLicensedUser() {
        return concurrentLicensedUser;
    }

    public void setConcurrentLicensedUser(boolean concurrentLicensedUser) {
        this.concurrentLicensedUser = concurrentLicensedUser;
    }

    public List<String> getUnavailableLicenses() {
        return unavailableLicenses;
    }

    public void setUnavailableLicenses(List<String> unavailableLicenses) {
        this.unavailableLicenses = unavailableLicenses;
    }

    @Override
    public String toString() {
        return "ContentAccessInfo [licenses=" + licenses + ", privateGroups=" + privateGroups
                + ", concurrentLicensedUser=" + concurrentLicensedUser + ", unavailableLicenses=" + unavailableLicenses
                + "]";
    }

    public ContentAccessInfo(List<License> licenses, List<String> privateGroups, boolean concurrentLicensedUser,
            List<String> unavailableLicenses) {
        super();
        this.licenses = licenses;
        this.privateGroups = privateGroups;
        this.concurrentLicensedUser = concurrentLicensedUser;
        this.unavailableLicenses = unavailableLicenses;
    }

}
