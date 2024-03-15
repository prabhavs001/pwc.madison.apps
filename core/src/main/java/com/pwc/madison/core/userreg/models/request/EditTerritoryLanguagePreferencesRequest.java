package com.pwc.madison.core.userreg.models.request;

import java.util.List;

/**
 * 
 * Model to represent data to send to UserReg Rest edit territory and language preferences request API.
 *
 */
public class EditTerritoryLanguagePreferencesRequest extends UserRegRequest {

    private List<String> preferredLanguages;
    private List<String> preferredTerritories;
    private String primaryTerritory;
    private String primaryLanguage;
    private String localeString;

    public List<String> getPreferredLanguages() {
        return preferredLanguages;
    }

    public void setPreferredLanguages(List<String> preferredLanguages) {
        this.preferredLanguages = preferredLanguages;
    }

    public List<String> getPreferredTerritories() {
        return preferredTerritories;
    }

    public void setPreferredTerritories(List<String> preferredTerritories) {
        this.preferredTerritories = preferredTerritories;
    }

    public String getPrimaryTerritory() {
        return primaryTerritory;
    }

    public void setPrimaryTerritory(String primaryTerritory) {
        this.primaryTerritory = primaryTerritory;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public String getLocaleString() {
        return localeString;
    }

    public void setLocaleString(String locale) {
        this.localeString = locale;
    }

    public EditTerritoryLanguagePreferencesRequest(List<String> preferredLanguages, List<String> preferredTerritories,
            String primaryTerritory, String primaryLanguage, String localeString) {
        super();
        this.preferredLanguages = preferredLanguages;
        this.preferredTerritories = preferredTerritories;
        this.primaryTerritory = primaryTerritory;
        this.primaryLanguage = primaryLanguage;
        this.localeString = localeString;
    }

}
