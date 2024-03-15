package com.pwc.madison.core.models;

import java.util.Map;

/**
 * Territory Language Mapper Component's Model
 * This Component would be used to map territories with languages
 */
public interface TerritoryLanguageMapper {

    /**
     * @return Map of territory languages with their respective home page path
     */
    Map<String, String> getTerritoryLanguageToHomePageMap();

    /***
     * @return current page territory and language
     */
    String getCurrentTerritoryLanguageCode();
    
    /**
     * Returns the analytics component's name.
     * 
     * @return {@link String}
     */
    String getComponentName();
}
