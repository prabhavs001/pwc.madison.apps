package com.pwc.madison.core.util;

import org.apache.commons.lang3.StringUtils;

import com.pwc.madison.core.constants.MadisonConstants;

public final class LocaleUtils {

    private static final String LOCALE_SEPARATOR = "_";

    public static final String getLocale(String territoryCode, String languageCode) {
        String locale = null;
        territoryCode = MadisonConstants.UK_TERRITORY_CODE.equals(territoryCode) ? MadisonConstants.GB_TERRITORY_CODE : territoryCode;
        if (StringUtils.isNotBlank(territoryCode) && StringUtils.isNotBlank(languageCode)) {
            locale = languageCode + LOCALE_SEPARATOR + territoryCode;
            locale = locale.toLowerCase();
        }
        return locale;
    }

    public static final String getLanguageFromLocale(String locale) {
        String languageCode = null;
        if (locale != null && locale.contains(LOCALE_SEPARATOR)) {
            languageCode = locale.substring(0, locale.indexOf(LOCALE_SEPARATOR));
        }
        return languageCode;
    }

    public static final String getTerritoryFromLocale(String locale) {
        String territoryCode = null;
        if (locale != null && locale.contains(LOCALE_SEPARATOR)) {
            territoryCode = locale.substring(locale.indexOf(LOCALE_SEPARATOR) + 1, locale.length());
        }
        return MadisonConstants.GB_TERRITORY_CODE.equals(territoryCode) ? MadisonConstants.UK_TERRITORY_CODE : territoryCode;
    }
}
