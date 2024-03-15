package com.pwc.madison.core.userreg.models.response;

import java.util.Map;

import com.pwc.madison.core.models.Preference;

/**
 * 
 * Model to represent the response data from Viewpoint Edit language and territory preferences profile.
 *
 */
public class EditPreferencesLanguageTerritoryResponse extends GetUserResponse {

    private Map<String, Preference> territoryToGaapPreferencesMap;

    private Map<String, Preference> territoryToGaasPreferencesMap;

    private Map<String, Preference> industryPreferencesMap;

    private Map<String, Preference> topicPreferencesMap;

    public Map<String, Preference> getTerritoryToGaapPreferencesMap() {
        return territoryToGaapPreferencesMap;
    }

    public void setTerritoryToGaapPreferencesMap(Map<String, Preference> territoryToGaapPreferencesMap) {
        this.territoryToGaapPreferencesMap = territoryToGaapPreferencesMap;
    }

    public Map<String, Preference> getTerritoryToGaasPreferencesMap() {
        return territoryToGaasPreferencesMap;
    }

    public void setTerritoryToGaasPreferencesMap(Map<String, Preference> territoryToGaasPreferencesMap) {
        this.territoryToGaasPreferencesMap = territoryToGaasPreferencesMap;
    }

    public Map<String, Preference> getIndustryPreferencesMap() {
        return industryPreferencesMap;
    }

    public void setIndustryPreferencesMap(final Map<String, Preference> industryPreferencesMap) {
        this.industryPreferencesMap = industryPreferencesMap;
    }

    public Map<String, Preference> getTopicPreferencesMap() {
        return topicPreferencesMap;
    }

    public void setTopicPreferencesMap(final Map<String, Preference> topicPreferencesMap) {
        this.topicPreferencesMap = topicPreferencesMap;
    }

    @Override
    public String toString() {
        return "EditPreferencesLanguageTerritoryResponse [territoryToGaapPreferencesMap="
                + territoryToGaapPreferencesMap + ", territoryToGaasPreferencesMap=" + territoryToGaasPreferencesMap
                + ", industryPreferencesMap=" + industryPreferencesMap + topicPreferencesMap + ", topicPreferencesMap="
                + topicPreferencesMap
                + "]";
    }

    public EditPreferencesLanguageTerritoryResponse(Data data, Map<String, Preference> territoryToGaapPreferencesMap,
            Map<String, Preference> territoryToGaasPreferencesMap,
            final Map<String, Preference> industryPreferencesMap, final Map<String, Preference> topicPreferencesMap) {
        super(data);
        this.territoryToGaapPreferencesMap = territoryToGaapPreferencesMap;
        this.territoryToGaasPreferencesMap = territoryToGaasPreferencesMap;
        this.industryPreferencesMap = industryPreferencesMap;
        this.topicPreferencesMap = topicPreferencesMap;
    }

}
