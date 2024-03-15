package com.pwc.madison.core.models;

import java.util.Map;

import com.google.gson.Gson;

/**
 * Territory POJO that provides getters and setters for the properties of the territory.
 */
public class Territory {

    private String territoryCode;
    private String territoryName;
    private String[] countries;
    private String defaultLocale;
    private Map<String, Language> localeToLanguageMap;
    private String territoryI18nKey;
    private String helpLink;
    private String cookieEnabled;
    private String twitterShare;
    private String linkedinShare;
    private String termsAndConditionsVersion;
    private String dateFormat;
    private String designation;
    private Boolean contentTypeSortingEnabled = false;

    public Territory(final String territoryCode, final String territoryName, final String[] countries,
            final String defaultLocale, final Map<String, Language> localeToLanguageMap, final String territoryI18nKey,
            final String helpLink, final String cookieEnabled, final String twitterShare, final String linkedinShare,
            final String termsAndConditionsVersion, final String dateFormat, final String designation, final Boolean contentTypeSortingEnabled) {
        super();
        this.territoryCode = territoryCode;
        this.territoryName = territoryName;
        this.countries = countries;
        this.defaultLocale = defaultLocale;
        this.localeToLanguageMap = localeToLanguageMap;
        this.territoryI18nKey = territoryI18nKey;
        this.helpLink = helpLink;
        this.cookieEnabled = cookieEnabled;
        this.twitterShare = twitterShare;
        this.linkedinShare = linkedinShare;
        this.termsAndConditionsVersion = termsAndConditionsVersion;
        this.dateFormat = dateFormat;
        this.designation = designation;
        this.contentTypeSortingEnabled = contentTypeSortingEnabled;
    }

    public String getCookieEnabled() {
        return cookieEnabled;
    }

    public void setCookieEnabled(final String cookieEnabled) {
        this.cookieEnabled = cookieEnabled;
    }

    public Map<String, Language> getLocaleToLanguageMap() {
        return localeToLanguageMap;
    }

    public void setLocaleToLanguageMap(final Map<String, Language> localeToLanguageMap) {
        this.localeToLanguageMap = localeToLanguageMap;
    }

    public Territory() {
    }

    public String getTerritoryCode() {
        return territoryCode;
    }

    public void setTerritoryCode(final String territoryCode) {
        this.territoryCode = territoryCode;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public void setTerritoryName(final String territoryName) {
        this.territoryName = territoryName;
    }

    public String[] getCountries() {
        return countries;
    }

    public void setCountries(final String[] countries) {
        this.countries = countries;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(final String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public String getTerritoryI18nKey() {
        return territoryI18nKey;
    }

    public void setTerritoryI18nKey(final String territoryI18nKey) {
        this.territoryI18nKey = territoryI18nKey;
    }

    public String getHelpLink() {
        return helpLink;
    }

    public void setHelpLink(final String helpLink) {
        this.helpLink = helpLink;
    }

    public String getTwitterShare() {
        return twitterShare;
    }

    public String getLinkedinShare() {
        return linkedinShare;
    }

    public void setTwitterShare(final String twitterShare) {
        this.twitterShare = twitterShare;
    }

    public void setLinkedinShare(final String linkedinShare) {
        this.linkedinShare = linkedinShare;
    }

    public String getTermsAndConditionsVersion() {
        return termsAndConditionsVersion;
    }

    public void setTermsAndConditionsVersion(final String termsAndConditionsVersion) {
        this.termsAndConditionsVersion = termsAndConditionsVersion;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(final String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Boolean getContentTypeSortingEnabled() {
        return contentTypeSortingEnabled;
    }

    public void setContentTypeSortingEnabled(Boolean contentTypeSortingEnabled) {
        this.contentTypeSortingEnabled = contentTypeSortingEnabled;
    }
}
