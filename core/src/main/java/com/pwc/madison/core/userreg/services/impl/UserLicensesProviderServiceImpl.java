package com.pwc.madison.core.userreg.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.services.UserLicensesProviderService;
import com.pwc.madison.core.userreg.services.impl.UserPreferencesProviderServiceImpl.MadisonPreferenceConfiguration;
import com.pwc.madison.core.util.MadisonUtil;

@Component(
        service = { UserLicensesProviderService.class, EventHandler.class },
        immediate = true,
        property = { EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/*",
                EventConstants.EVENT_FILTER
                        + "=(path=/content/pwc-madison/global/reference-data/authorization/licenses/*)" })
public class UserLicensesProviderServiceImpl implements UserLicensesProviderService, EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserLicensesProviderService.class);

    private static final String ITEM_VALUE_PROPERTY = "value";
    private static final String ITEM_LABEL_PROPERTY = "text";

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    private Map<String, String> userLicensesMap;

    @Activate
    @Modified
    protected void Activate(final MadisonPreferenceConfiguration madisonPreferenceConfiguration) {
        LOGGER.info("UserLicensesProviderServiceImpl Activate/Modify");
        createUserLicensesMap();
    }
    /**
     * Creates the User Licenses {@link Map}.
     * 
     */
    private void createUserLicensesMap() {
        LOGGER.info("UserLicensesProviderServiceImpl : Entered createUserLicensesMap");
        ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory,
                MadisonConstants.MADISON_READ_SUB_SERVICE);
        if (null != resourceResolver) {
            Resource licensesParent = resourceResolver.getResource(Constants.USER_LICENSES_PATH);
            userLicensesMap = new HashMap<String, String>();
            if (licensesParent != null) {
                for (Resource license : licensesParent.getChildren()) {
                    userLicensesMap.put(license.getValueMap().get(ITEM_VALUE_PROPERTY, String.class),
                            license.getValueMap().get(ITEM_LABEL_PROPERTY, String.class));
                }
            }
            resourceResolver.close();
        }
        LOGGER.debug("UserLicensesProviderServiceImpl createUserLicensesMap() : License Code To License Title Map {}",
                userLicensesMap);
    };

    @Override
    public void handleEvent(Event event) {
        LOGGER.info("UserLicensesProviderServiceImpl : Entered handleEvent");
        createUserLicensesMap();
    }

    @Override
    public Map<String, String> getLicenseCodeToTitleMap() {
        return userLicensesMap;
    }

}
