package com.pwc.madison.core.fusionSearch.models;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * SearchFilter POJO that provides getters and setters for the properties of the Fusion search results filters(filter-sort/filter-facets).
 */
public class SearchFilter {

    @Override
    public String toString() {
        return "SearchFilter [id=" + id + ", title=" + title + ", value=" + value + ", order=" + order + ", usOrder=" + usOrder + ", i18nKey="
                + i18nKey + ", translatedTitles=" + translatedTitles + ", navigationListViewSpecific="+Boolean.toString(navigationListViewSpecific)+"]";
    }

    public SearchFilter(String id, String title, String value, String i18nKey, String order, String usOrder, Boolean navigationListViewSpecific) {
        super();
        this.id = id;
        this.title = title;
        this.value = value;
        this.i18nKey = i18nKey;
        this.translatedTitles = new HashMap<String, String>();
        this.order = order;
        this.usOrder = usOrder;
        this.navigationListViewSpecific = navigationListViewSpecific;
    }

    /**
     * Id is path to the search filter that uniquely defines it.
     */
    private String id;
    /**
     * The non localized title of the search filter.
     */
    private String title;
    /**
     * The value of the search filter.
     */
    private String value;
    /**
     * The i18n key to get the localized title of the search filter.
     */
    private String i18nKey;
    /**
     * {@link Map} of the translated titles where key is the locale and value is the translated title as per locale.
     */
    private Map<String, String> translatedTitles;
    /**
     * The order of the search filter to sort the results based on all territories.
     */
    private String order;
    /**
     * The order of the search filter to sort the results based on US territory.
     */
    private String usOrder;
    /**
     * The check to determine if the sort Type is applicable to only Navigation List View (New Implementation)
     */
    private Boolean navigationListViewSpecific = false;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public void setI18nKey(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public Map<String, String> getTranslatedTitles() {
        return translatedTitles;
    }

    public void setTranslatedTitles(Map<String, String> translatedTitles) {
        this.translatedTitles = translatedTitles;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getUsOrder() {
        return usOrder;
    }

    public void setUsOrder(String usOrder) {
        this.usOrder = usOrder;
    }

    public Boolean getNavigationListViewSpecific() {
        return navigationListViewSpecific;
    }

    public void setNavigationListViewSpecific(Boolean navigationListViewSpecific) {
        this.navigationListViewSpecific = navigationListViewSpecific;
    }

    /**
     * Comparator Implementation Used for sorting most popular
     * items according to order key.
     */
    public static class SortSearchFilterResultsByOrder implements Comparator<SearchFilter> {

        @Override
        public int compare(SearchFilter searchFilterFirst, SearchFilter searchFilterSecond) {
            return searchFilterFirst.getOrder().compareTo(searchFilterSecond.getOrder());
        }
    }
}


