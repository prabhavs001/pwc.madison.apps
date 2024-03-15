package com.pwc.madison.core.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.services.LiveCopyConfigurationProvider;
import com.pwc.madison.core.services.impl.LiveCopyConfigurationProvideImpl.LiveCopyConfiguration;

@Component(immediate = true, service = LiveCopyConfigurationProvider.class)
@Designate(ocd = LiveCopyConfiguration.class)
public class LiveCopyConfigurationProvideImpl implements LiveCopyConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveCopyConfigurationProvideImpl.class);
    private static final String TERRITORY_PLACEHOLDER = "<territory>";

    private Map<String, String[]> notificationTerritoryToAuthorizablesMap;
    private String defaultNotificationAuthorizables;

    @Activate
    @Modified
    protected void Activate(final LiveCopyConfiguration liveCopyConfiguration) {
        LOGGER.info("LiveCopyConfigurationProvideImpl : Entered Activate/Modify");
        String[] notificationTerritoryToAuthorizablesList = liveCopyConfiguration
                .madison_notification_authorizable_territory_list();
        defaultNotificationAuthorizables = liveCopyConfiguration
                .madison_default_notification_authorizable_territory_map();
        LOGGER.debug("LiveCopyConfigurationProvideImpl Activate() Territory User/Group List for Notification : {}",
                notificationTerritoryToAuthorizablesList);
        LOGGER.debug(
                "LiveCopyConfigurationProvideImpl Activate() Default Territory User/Group List for Notification : {}",
                defaultNotificationAuthorizables);
        createNotificationTerritoryToAuthorizablesMap(notificationTerritoryToAuthorizablesList);
    }

    private void createNotificationTerritoryToAuthorizablesMap(String[] notificationTerritoryToAuthorizablesList) {
        notificationTerritoryToAuthorizablesMap = new HashMap<String, String[]>();
        if (notificationTerritoryToAuthorizablesList != null) {
            for (String territoryToAuthorizables : notificationTerritoryToAuthorizablesList) {
                int colonIndex = territoryToAuthorizables.indexOf(MadisonConstants.COLON);
                if (colonIndex > 0) {
                    String territory = territoryToAuthorizables.substring(0, colonIndex);
                    String authorizables = territoryToAuthorizables.substring(colonIndex + 1,
                            territoryToAuthorizables.length());
                    notificationTerritoryToAuthorizablesMap.put(territory,
                            authorizables.split(MadisonConstants.COMMA_SEPARATOR));
                }
            }
        }
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Live Copy Configuration")
    public @interface LiveCopyConfiguration {

        @AttributeDefinition(
                name = "Territory User/Group List for Notification",
                description = "Viewpoint group/user id's for which the live copy notification should be send by territory. Each territory list is defined by one entry in list like <territory code>:<comma separated group/user ids>. Example: us:us-territory-editor,us-territory-site-manager.",
                type = AttributeType.STRING,
                cardinality = Integer.MAX_VALUE)
        String[] madison_notification_authorizable_territory_list();

        @AttributeDefinition(
                name = "Default Territory User/Group List for Notification",
                description = "Defualt Viewpoint group/user id's for which the live copy notification should be send by territory if no list is defined for territory in configuration. Default value is <territory>-territory-editor,<territory>-territory-site-manager where <territory> placeholder will be replaced by territory.",
                type = AttributeType.STRING)
        String madison_default_notification_authorizable_territory_map() default "<territory>-territory-editor,<territory>-territory-site-manager";

    }

    @Override
    public String[] getNotificationAuthorizablesByTerritory(final String territoryCode) {
        return notificationTerritoryToAuthorizablesMap.containsKey(territoryCode)
                ? notificationTerritoryToAuthorizablesMap.get(territoryCode)
                : defaultNotificationAuthorizables.replace(TERRITORY_PLACEHOLDER, territoryCode)
                        .split(MadisonConstants.COMMA_SEPARATOR);
    }

}
