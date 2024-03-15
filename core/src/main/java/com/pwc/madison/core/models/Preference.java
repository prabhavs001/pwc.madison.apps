package com.pwc.madison.core.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Preference POJO that provides getters and setters for the properties of the User registration preferences(GAAP/GAAS).
 */
public class Preference {

    @Override
    public String toString() {
        return "Preference [id=" + id + ", title=" + title + ", mappedTerritory=" + mappedTerritory + ", i18nKey="
                + i18nKey + ", tags=" + Arrays.toString(tags) + ", translatedTitles=" + translatedTitles + "]";
    }

    public Preference(String id, String title, String mappedTerritory, String i18nKey, String[] tags) {
        super();
        this.id = id;
        this.title = title;
        this.mappedTerritory = mappedTerritory;
        this.i18nKey = i18nKey;
        this.tags = tags;
        this.translatedTitles = new HashMap<String, String>();
    }

    /**
     * Id is path to the preference that uniquely defines it.
     */
    private String id;
    /**
     * The non localized title of the preference.
     */
    private String title;
    /**
     * The territory to which the preference is mapped to.
     */
    private String mappedTerritory;
    /**
     * The i18n key to get the localized title of the preference.
     */
    private String i18nKey;
    /**
     * The {@link Staing[]} tags allocated to the preference.
     */
    private String[] tags;
    /**
     * {@link Map} of the translated titles where key is the locale and value is the translated title as per locale.
     */
    private Map<String, String> translatedTitles;

    public Map<String, String> getTranslatedTitles() {
        return translatedTitles;
    }

    public void setTranslatedTitles(Map<String, String> translatedTitles) {
        this.translatedTitles = translatedTitles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMappedTerritory() {
        return mappedTerritory;
    }

    public void setMappedTerritory(String mappedTerritory) {
        this.mappedTerritory = mappedTerritory;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public void setI18nKey(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

}
