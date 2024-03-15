package com.pwc.madison.core.models;

import java.util.Comparator;

/**
 * Language POJO that provides getters and setters for the properties of the language.
 */
public class Language implements Comparable<Language> {

    private String langTitle;
    private String code;
    private String locale;
    private String searchAndPromoteID;
    private String displayLanguage;
    private String displayLanguageI18nKey;
    private Integer displayOrder;
    private String homePagePath;
    private Boolean hideLanguage;

    public Language(String langTitle, String code, String locale, String searchAndPromoteID, String displayLanguage, String displayLanguageI18nKey,
                    Integer displayOrder, String homePagePath, Boolean hideLanguage) {
        this.langTitle = langTitle;
        this.code = code;
        this.locale = locale;
        this.searchAndPromoteID = searchAndPromoteID;
        this.displayLanguage = displayLanguage;
        this.displayLanguageI18nKey = displayLanguageI18nKey;
        this.displayOrder = displayOrder;
        this.homePagePath = homePagePath;
        this.hideLanguage = hideLanguage;
    }

    public Language(String langTitle, String code, String locale, String searchAndPromoteID) {
        super();
        this.langTitle = langTitle;
        this.code = code;
        this.locale = locale;
        this.searchAndPromoteID = searchAndPromoteID;
    }
    
    public Language() {
    }

    public String getLangTitle() {
        return langTitle;
    }

    public String getDisplayLanguage() {
        return displayLanguage;
    }

    public void setDisplayLanguage(String displayLanguage) {
        this.displayLanguage = displayLanguage;
    }

    public void setLangTitle(String langTitle) {
        this.langTitle = langTitle;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSearchAndPromoteID() {
        return searchAndPromoteID;
    }

    public void setSearchAndPromoteID(String searchAndPromoteID) {
        this.searchAndPromoteID = searchAndPromoteID;
    }
    
    public String getDisplayLanguageI18nKey() {
		return displayLanguageI18nKey;
	}

	public void setDisplayLanguageI18nKey(String displayLanguageI18nKey) {
		this.displayLanguageI18nKey = displayLanguageI18nKey;
	}
	
	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getHomePagePath() {
        return homePagePath;
    }

    public void setHomePagePath(String homePagePath) {
        this.homePagePath = homePagePath;
    }

    public Boolean getHideLanguage() {
        return hideLanguage;
    }

    public void setHideLanguage(Boolean hideLanguage) {
        this.hideLanguage = hideLanguage;
    }

    @Override
    public String toString() {
        return "Language{" +
            "langTitle='" + langTitle + '\'' +
            ", code='" + code + '\'' +
            ", locale='" + locale + '\'' +
            ", searchAndPromoteID='" + searchAndPromoteID + '\'' +
            ", displayLanguage='" + displayLanguage + '\'' +
            ", displayLanguageI18nKey='" + displayLanguageI18nKey + '\'' +
            ", displayOrder=" + displayOrder +
            ", homePagePath='" + homePagePath + '\'' +
            ", hideLanguage=" + hideLanguage +
            '}';
    }

    @Override
	public int compareTo(Language otherLanguage) {
		   if(this.displayOrder == null) {
			  return 1;
		   } else if(otherLanguage.displayOrder == null) {
			  return -1;
		   } else {
			  return this.displayOrder.compareTo(otherLanguage.displayOrder);   
		   }
	}
}
