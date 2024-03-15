package com.pwc.madison.core.userreg.models;

import java.time.LocalDate;
import java.util.List;

import com.pwc.madison.core.userreg.models.request.UserRegRequest;

/**
 *
 * Model to represent Viewpoint User Profile.
 *
 */
public class UserProfile extends UserRegRequest {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String country;
    private String samaccountNo;
    private Boolean subscribeToWeeklyNewsLetter;
    private Boolean showMultimediaSubtitle;
    private String preferenceView;
    private String company;
    private String title;
    private String territoryCode;
    private String languageCode;
    private long userProfileCookieExpiry;
    private String language;
    private Boolean isInternalUser;
    private List<String> preferredGaas;
    private List<String> preferredLanguages;
    private List<String> preferredTerritories;
    private List<String> preferredGaap;
    private String primaryTerritory;
    private String primaryLanguage;
    private List<AcceptedTerritoryCodeAndVersion> acceptedTerritoryCodeAndVersion;
    private List<String> industryTags;
    private ContentAccessInfo contentAccessInfo;
    private LocalDate lastLoginDate;
    private List<String> preferredTopic;
    private List<String> preferredIndustry;
    private List<String> topicTags;
    private List<String> titleTags;
    private String functionalRoleTitle;
    private List<String> industryTitles;
    private List<String> gaapTags;
    private List<String> gaasTags;
    private String countryCode;
    private String pwcId;
    private String userAccountType;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getSamaccountNo() {
        return samaccountNo;
    }

    public void setSamaccountNo(final String samaccountNo) {
        this.samaccountNo = samaccountNo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPreferenceView() {
        return preferenceView;
    }

    public void setPreferenceView(final String preferencesView) {
        preferenceView = preferencesView;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(final String company) {
        this.company = company;
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

    public Boolean getShowMultimediaSubtitle() {
        return showMultimediaSubtitle;
    }

    public void setShowMultimediaSubtitle(final Boolean showMultimediaSubtitle) {
        this.showMultimediaSubtitle = showMultimediaSubtitle;
    }

    public String getTerritoryCode() {
        return territoryCode;
    }

    public void setTerritoryCode(String territory) {
        this.territoryCode = territory;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public long getUserProfileCookieExpiry() {
        return userProfileCookieExpiry;
    }

    public void setUserProfileCookieExpiry(long userProfileCookieExpiry) {
        this.userProfileCookieExpiry = userProfileCookieExpiry;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public UserProfile() {
    }

    public Boolean getIsInternalUser() {
        return isInternalUser;
    }

    public void setIsInternalUser(Boolean isInternalUser) {
        this.isInternalUser = isInternalUser;
    }

    public List<String> getPreferredLanguages() {
        return preferredLanguages;
    }

    public void setPreferredLanguages(List<String> preferredLanguages) {
        this.preferredLanguages = preferredLanguages;
    }

    public List<String> getPreferredGaas() {
        return preferredGaas;
    }

    public void setPreferredGass(List<String> preferredGaas) {
        this.preferredGaas = preferredGaas;
    }

    public List<String> getPreferredTerritories() {
        return preferredTerritories;
    }

    public void setPreferredTerritories(List<String> preferredTerritories) {
        this.preferredTerritories = preferredTerritories;
    }

    public List<String> getPreferredGaap() {
        return preferredGaap;
    }

    public void setPreferredGaap(List<String> preferredGaap) {
        this.preferredGaap = preferredGaap;
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

    public List<AcceptedTerritoryCodeAndVersion> getAcceptedTerritoryCodeAndVersion() {
        return acceptedTerritoryCodeAndVersion;
    }

    public void setAcceptedTerritoryCodeAndVersion(
            List<AcceptedTerritoryCodeAndVersion> acceptedTerritoryCodeAndVersion) {
        this.acceptedTerritoryCodeAndVersion = acceptedTerritoryCodeAndVersion;
    }

    public List<String> getIndustryTags() {
        return industryTags;
    }

    public void setIndustryTags(final List<String> industryTags) {
        this.industryTags = industryTags;
    }

    public ContentAccessInfo getContentAccessInfo() {
        return contentAccessInfo;
    }

    public void setContentAccessInfo(final ContentAccessInfo contentAccessInfo) {
        this.contentAccessInfo = contentAccessInfo;
    }

    public LocalDate getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public List<String> getPreferredTopic() {
        return preferredTopic;
    }

    public void setPreferredTopic(final List<String> preferredTopic) {
        this.preferredTopic = preferredTopic;
    }

    public List<String> getPreferredIndustry() {
        return preferredIndustry;
    }

    public void setPreferredIndustry(final List<String> preferredIndustry) {
        this.preferredIndustry = preferredIndustry;
    }

    public List<String> getTopicTags() {
        return topicTags;
    }

    public void setTopicTags(List<String> topicTags) {
        this.topicTags = topicTags;
    }

    public List<String> getTitleTags() {
        return titleTags;
    }

    public void setTitleTags(List<String> titleTags) {
        this.titleTags = titleTags;
    }

    public void setPreferredGaas(List<String> preferredGaas) {
        this.preferredGaas = preferredGaas;
    }

    public String getPwcId() {
        return pwcId;
    }

    public void setPwcId(String pwcId) {
        this.pwcId = pwcId;
    }

    public String getUserAccountType() {
        return userAccountType;
    }

    public void setUserAccountType(String userAccountType) {
        this.userAccountType = userAccountType;
    }

    public List<String> getGaapTags() {
        return gaapTags;
    }

    public void setGaapTags(List<String> gaapTags) {
        this.gaapTags = gaapTags;
    }

    public List<String> getGaasTags() {
        return gaasTags;
    }

    public void setGaasTags(List<String> gaasTags) {
        this.gaasTags = gaasTags;
    }

    public String getFunctionalRoleTitle() {
        return functionalRoleTitle;
    }

    public void setFunctionalRoleTitle(String functionalRoleTitle) {
        this.functionalRoleTitle = functionalRoleTitle;
    }

    public List<String> getIndustryTitles() {
        return industryTitles;
    }

    public void setIndustryTitles(List<String> industryTitles) {
        this.industryTitles = industryTitles;
    }

    public UserProfile(Integer id, String firstName, String lastName, String email, String country, String samaccountNo,
            Boolean subscribeToWeeklyNewsLetter, Boolean showMultimediaSubtitle, String preferenceView, String company,
            String title, String territoryCode, String languageCode, long userProfileCookieExpiry, String language,
            Boolean isInternalUser, List<String> preferredGaas, List<String> preferredLanguages,
            List<String> preferredTerritories, List<String> preferredGaap, String primaryTerritory,
            String primaryLanguage, List<AcceptedTerritoryCodeAndVersion> acceptedTerritoryCodeAndVersion,
            List<String> industryTags, ContentAccessInfo contentAccessInfo, LocalDate lastLoginDate,
            List<String> preferredTopic, List<String> preferredIndustry, List<String> topicTags, List<String> titleTags,
            String functionalRoleTitle, List<String> industryTitles, List<String> gaapTags, List<String> gaasTags,
            String countryCode, String pwcId, String userAccountType) {
        super();
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.country = country;
        this.samaccountNo = samaccountNo;
        this.subscribeToWeeklyNewsLetter = subscribeToWeeklyNewsLetter;
        this.showMultimediaSubtitle = showMultimediaSubtitle;
        this.preferenceView = preferenceView;
        this.company = company;
        this.title = title;
        this.territoryCode = territoryCode;
        this.languageCode = languageCode;
        this.userProfileCookieExpiry = userProfileCookieExpiry;
        this.language = language;
        this.isInternalUser = isInternalUser;
        this.preferredGaas = preferredGaas;
        this.preferredLanguages = preferredLanguages;
        this.preferredTerritories = preferredTerritories;
        this.preferredGaap = preferredGaap;
        this.primaryTerritory = primaryTerritory;
        this.primaryLanguage = primaryLanguage;
        this.acceptedTerritoryCodeAndVersion = acceptedTerritoryCodeAndVersion;
        this.industryTags = industryTags;
        this.contentAccessInfo = contentAccessInfo;
        this.lastLoginDate = lastLoginDate;
        this.preferredTopic = preferredTopic;
        this.preferredIndustry = preferredIndustry;
        this.topicTags = topicTags;
        this.titleTags = titleTags;
        this.functionalRoleTitle = functionalRoleTitle;
        this.industryTitles = industryTitles;
        this.gaapTags = gaapTags;
        this.gaasTags = gaasTags;
        this.countryCode = countryCode;
        this.pwcId = pwcId;
        this.userAccountType = userAccountType;
    }

    @Override
    public String toString() {
        return "UserProfile [id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email
                + ", country=" + country + ", samaccountNo=" + samaccountNo + ", subscribeToWeeklyNewsLetter="
                + subscribeToWeeklyNewsLetter + ", showMultimediaSubtitle=" + showMultimediaSubtitle
                + ", preferenceView=" + preferenceView + ", company=" + company + ", title=" + title
                + ", territoryCode=" + territoryCode + ", languageCode=" + languageCode + ", userProfileCookieExpiry="
                + userProfileCookieExpiry + ", language=" + language + ", isInternalUser=" + isInternalUser
                + ", preferredGaas=" + preferredGaas + ", preferredLanguages=" + preferredLanguages
                + ", preferredTerritories=" + preferredTerritories + ", preferredGaap=" + preferredGaap
                + ", primaryTerritory=" + primaryTerritory + ", primaryLanguage=" + primaryLanguage
                + ", acceptedTerritoryCodeAndVersion=" + acceptedTerritoryCodeAndVersion + ", industryTags="
                + industryTags + ", contentAccessInfo=" + contentAccessInfo + ", lastLoginDate=" + lastLoginDate
                + ", preferredTopic=" + preferredTopic + ", preferredIndustry=" + preferredIndustry + ", topicTags="
                + topicTags + ", titleTags=" + titleTags + ", functionalRoleTitle=" + functionalRoleTitle
                + ", industryTitles=" + industryTitles + ", gaapTags=" + gaapTags + ", gaasTags=" + gaasTags
                + ", countryCode=" + countryCode + ", pwcId=" + pwcId + ", userAccountType=" + userAccountType + "]";
    }

}
