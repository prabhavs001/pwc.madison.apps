package com.pwc.madison.core.userreg.models.request;

/**
 *
 * Model to represent login request data to send to UserReg register API.
 *
 */
public class RegisterRequest extends UserRegRequest {

    private String email;
    private String country;
    private String language;
    private String title;
    private String industry;
    private Boolean subscribeToWeeklyNewsLetter;
    private Boolean tncAccepted;
    private String referer;
    private String locale;
    private String territoryCode;
    private String primaryTerritory;
    private UserAcceptTncRequest userAcceptTnc;
    private String isCompleteProfile;

    public String getIsCompleteProfile() {
		return isCompleteProfile;
	}

	public void setIsCompleteProfile(String isCompleteProfile) {
		this.isCompleteProfile = isCompleteProfile;
	}

    public String getTerritoryCode() {
        return territoryCode;
    }

    public void setTerritoryCode(String territory) {
        this.territoryCode = territory;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }
   
    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Boolean getSubscribeToWeeklyNewsLetter() {
        return subscribeToWeeklyNewsLetter;
    }

    public void setSubscribeToWeeklyNewsLetter(final Boolean subscribeToWeeklyNewsLetter) {
        this.subscribeToWeeklyNewsLetter = subscribeToWeeklyNewsLetter;
    }

    public Boolean getTncAccepted() {
        return tncAccepted;
    }

    public void setTncAccepted(final Boolean tncAccepted) {
        this.tncAccepted = tncAccepted;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getPrimaryTerritory() {
        return primaryTerritory;
    }

    public void setPrimaryTerritory(String primaryTerritory) {
        this.primaryTerritory = primaryTerritory;
    }

    public UserAcceptTncRequest getUserAcceptTnc() {
        return userAcceptTnc;
    }

    public void setUserAcceptTnc(UserAcceptTncRequest userAcceptTnc) {
        this.userAcceptTnc = userAcceptTnc;
    }

    @Override
    public String toString() {
        return "RegisterRequest [email=" + email +
                ", country=" + country + ", language=" + language + ", title="
                + title +", industry=" + industry + ", subscribeToWeeklyNewsLetter=" + subscribeToWeeklyNewsLetter + ", tncAccepted="
                + tncAccepted + ", referer=" + referer + ", locale=" + locale + ", territoryCode=" + territoryCode
                + ", primaryTerritory=" + primaryTerritory + ", userAcceptTncRequest=" + userAcceptTnc + ", isCompleteProfile="+isCompleteProfile+"]";
    }

    public RegisterRequest(String email, String country,
            String language, String title, String industry, Boolean subscribeToWeeklyNewsLetter, Boolean tncAccepted,
            String referer, String locale, String territoryCode, String primaryTerritory,
            UserAcceptTncRequest userAcceptTnc, String isCompleteProfile) {
        super();
        this.email = email;
        this.country = country;
        this.language = language;
        this.title = title;
        this.industry = industry;
        this.subscribeToWeeklyNewsLetter = subscribeToWeeklyNewsLetter;
        this.tncAccepted = tncAccepted;
        this.referer = referer;
        this.locale = locale;
        this.territoryCode = territoryCode;
        this.primaryTerritory = primaryTerritory;
        this.userAcceptTnc = userAcceptTnc;
        this.isCompleteProfile = isCompleteProfile;
    }

}
