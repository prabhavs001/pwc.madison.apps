package com.pwc.madison.core.services.impl;

import java.util.*;

import com.day.cq.i18n.I18n;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.PodcastWidget;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.services.impl.CountryTerritoryMapperServiceImpl.MadisonTeritoryConfiguration;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
    service = { CountryTerritoryMapperService.class, EventHandler.class },
    immediate = true,
    property = { EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/*",
            EventConstants.EVENT_FILTER + "=(path=/content/pwc-madison/global/reference-data/territories/*)" })
@Designate(ocd = MadisonTeritoryConfiguration.class)
public class CountryTerritoryMapperServiceImpl implements CountryTerritoryMapperService, EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountryTerritoryMapperService.class);

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference
    private XSSAPI xssAPI;

    private String territoryNodesBasePath;
    private Map<String, Territory> countryToTerritoryMap;
    private Map<String, Territory> territoryCodeToTerritoryMap;
    private String defaultLocale;
    private String defaultTerritoryCode;

    @Activate
    @Modified
    protected void Activate(final MadisonTeritoryConfiguration madisonTeritoryConfiguration) {
        LOGGER.info("CountryTerritoryMapperService : Entered Activate/Modify");
        territoryNodesBasePath = madisonTeritoryConfiguration.madison_territory_base_path();
        LOGGER.debug("CountryTerritoryMapperService Activate() Madison Territory Base Path : {}",
                territoryNodesBasePath);
        createCountryTerritoryMap();
        defaultLocale = madisonTeritoryConfiguration.madison_default_locale();
        defaultTerritoryCode = madisonTeritoryConfiguration.madison_default_territory();
        LOGGER.debug("CountryTerritoryMapperService Activate() Madison Default Locale : {}", defaultLocale);
        LOGGER.debug("CountryTerritoryMapperService Activate() Madison Default Territory Code : {}",
                defaultTerritoryCode);
    }

    @Override
    public Territory getTerritoryByCountry(final String countryCode) {
        Territory territory = null;
        if (countryToTerritoryMap != null && countryCode != null) {
            territory = countryToTerritoryMap.get(countryCode.trim().toUpperCase());
        }
        return territory;
    }

    @Override
    public Map<String, Territory> getCountryToTerritoryMap() {
        return countryToTerritoryMap;
    }

    @Override
    public String getCountryToTerritoryMapJson() {
        return new Gson().toJson(countryToTerritoryMap);
    }

    @Override
    public Territory getTerritoryByTerritoryCode(final String territoryCode) {
        Territory territory = null;
        LOGGER.debug("Entered getTerritoryByTerritoryCode() : territoryCode {} : territoryCodeToTerritoryMap {}",
                xssAPI.encodeForHTML(territoryCode), territoryCodeToTerritoryMap);
        if (territoryCodeToTerritoryMap != null && territoryCode != null) {
            territory = territoryCodeToTerritoryMap.get(territoryCode.trim().toLowerCase());
        }
        return territory;
    }

    @Override
    public Map<String, Territory> getTerritoryCodeToTerritoryMap() {
        return territoryCodeToTerritoryMap;
    }

    @Override
    public String getTerritoryCodeToTerritoryMapJson() {
        return new Gson().toJson(territoryCodeToTerritoryMap);
    }

    @Override
    public Territory getTerritoryByCountry(final String countryCode, final String defaultTerritoryCode) {
        Territory territory = getTerritoryByCountry(countryCode);
        if (territory == null) {
            territory = getTerritoryByTerritoryCode(defaultTerritoryCode);
        }
        return territory;
    }

    @Override
    public Territory getTerritoryByTerritoryCode(final String territoryCode, final String defaultTerritoryCode) {
        Territory territory = getTerritoryByTerritoryCode(territoryCode);
        if (territory == null) {
            territory = getTerritoryByTerritoryCode(defaultTerritoryCode);
        }
        return territory;
    }

    /**
     * Creates the country code to {@link Territory} {@link Map} and the territory code to {@link Territory}
     * {@link Map}.
     */
    private void createCountryTerritoryMap() {
        countryToTerritoryMap = null;
        territoryCodeToTerritoryMap = null;
        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_READ_SUB_SERVICE);
        if (null != resourceResolver) {
            final Resource territoryBasePathResource = resourceResolver.getResource(territoryNodesBasePath);
            if (territoryBasePathResource != null) {
                countryToTerritoryMap = new HashMap<>();
                territoryCodeToTerritoryMap = new HashMap<>();
                for (final Resource territoryResource : territoryBasePathResource.getChildren()) {
                    final Territory territory = territoryResource.adaptTo(Territory.class);
                    if (territory.getCountries() != null) {
                        for (final String country : territory.getCountries()) {
                            countryToTerritoryMap.put(country.toUpperCase(), territory);
                        }
                    }
                    territoryCodeToTerritoryMap.put(territory.getTerritoryCode(), territory);
                }
                territoryCodeToTerritoryMap = sortByTerritoryTitle(territoryCodeToTerritoryMap);
            }
            resourceResolver.close();
        }
        LOGGER.debug("CountryTerritoryMapperService createCountryTerritoryMap() : Country to territory Map {}",
                countryToTerritoryMap);
        LOGGER.debug("CountryTerritoryMapperService createCountryTerritoryMap() : Territory Code to Territory Map {}",
                territoryCodeToTerritoryMap);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Territory Configuration")
    public @interface MadisonTeritoryConfiguration {

        @AttributeDefinition(
            name = "UserReg Rest API Base URL",
            description = "Content path under which territory's reference data is stored")
        String madison_territory_base_path() default "/content/pwc-madison/global/reference-data/territories";

        @AttributeDefinition(
            name = "UserReg Default Locale",
            description = "This locale is used if the user selects a country that does not have a matching Madison territory.")
        String madison_default_locale();

        @AttributeDefinition(
            name = "UserReg Default Territory Code",
            description = "This territory code is used if the user selects a country that does not have a matching Madison territory.")
        String madison_default_territory();
    }

    @Override
    public void handleEvent(final Event event) {
        createCountryTerritoryMap();
    }

    @Override
    public String getLocaleFromCountry(final String countryCode) {
        final Territory territory = getTerritoryByCountry(countryCode);
        return territory != null ? territory.getDefaultLocale() : getDefaultLocale();
    }

    @Override
    public String getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public String getDefaultTerritoryCode() {
        return defaultTerritoryCode;
    }

    @Override
    public Territory getTerritoryByCountryCode(final String countryCode, final boolean returnDefault) {
        LOGGER.debug("Entered getTerritoryByCountryCode() : countryCode {} : returnDefault : {}",
                xssAPI.encodeForHTML(countryCode), returnDefault);
        Territory territory = getTerritoryByCountry(countryCode);
        if (territory == null && returnDefault) {
            territory = getTerritoryByTerritoryCode(getDefaultTerritoryCode());
        }
        LOGGER.debug("Returning territory {} from  getTerritoryByCountryCode()", territory);
        return territory;
    }

    /**
     * Returns {@link Map<String, Territory>} after sorting it by {@link Territory} title.
     *
     * @param unsortMap
     *            {@link Map} unsorted
     * @return {@link Map} sorted
     */
    private static Map<String, Territory> sortByTerritoryTitle(final Map<String, Territory> unsortMap) {

        final List<Map.Entry<String, Territory>> list = new LinkedList<>(
                unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Territory>>() {
            @Override
            public int compare(final Map.Entry<String, Territory> object1, final Map.Entry<String, Territory> object2) {
                return object1.getValue().getTerritoryName().compareTo(object2.getValue().getTerritoryName());
            }
        });

        final Map<String, Territory> sortedMap = new LinkedHashMap<>();
        for (final Map.Entry<String, Territory> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    @Override
    public List<PodcastWidget> getPodcastListByTerritoryLocale(final String countryCode, final String localeCode) {
        final List<PodcastWidget> podcastList = new ArrayList<>();

        if (StringUtils.isBlank(countryCode) || StringUtils.isBlank(localeCode)) {
            return podcastList;
        }

        final ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_READ_SUB_SERVICE);
        if (null == resourceResolver) {
            LOGGER.error("Permission for the user {} to get podcast widgets is not available ",
                    MadisonConstants.MADISON_READ_SUB_SERVICE);
            return podcastList;
        }

        final String podcastResPath = territoryNodesBasePath + MadisonConstants.FORWARD_SLASH + countryCode
                + MadisonConstants.FORWARD_SLASH + localeCode + MadisonConstants.FORWARD_SLASH
                + MadisonConstants.PODCAST;
        final Resource podcastWidgetResource = resourceResolver.getResource(podcastResPath);

        if (null == podcastWidgetResource) {
            LOGGER.error("No resource available for the podcast path {}", podcastResPath);
            return podcastList;
        }

        for (final Resource podcastResource : podcastWidgetResource.getChildren()) {
            final PodcastWidget podcastWidget = podcastResource.adaptTo(PodcastWidget.class);
            podcastList.add(podcastWidget);
        }

        return podcastList;
    }

    @Override
    public String getSelectedCountryNameByDefaultLocale(final SlingHttpServletRequest request,String countryCode){
        String countryName = StringUtils.EMPTY;
        if(countryCode != null){
            final Locale locale = new Locale(getDefaultLocale());
            final ResourceBundle resourceBundle = request.getResourceBundle(locale);
            final I18n i18n = new I18n(resourceBundle);
            String countryInfo = i18n.get("UserReg_Country_List").replaceAll(MadisonConstants.OPTION_VALUE_ELEMENT,"").replaceAll(MadisonConstants.CLOSING_OPTION_ELEMENT,"");
            String[] countryList = countryInfo.split(MadisonConstants.WHITE_SPACE_SEPERATOR);
            for (int i=0; i < countryList.length; i++){
                 String[] countryData = countryList[i].split(MadisonConstants.GREATER_THAN_SIGN);
                 if(countryData[0].replace(MadisonConstants.DOUBLE_QUOTES_REGEX, "").equalsIgnoreCase(countryCode)){
                     countryName = countryData[1];
                     return countryName;
                 }
            }
        }
        return countryName;
    }
}
