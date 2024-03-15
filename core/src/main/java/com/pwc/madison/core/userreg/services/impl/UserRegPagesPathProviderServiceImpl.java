package com.pwc.madison.core.userreg.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.services.MadisonDomainsService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.userreg.services.impl.UserRegPagesPathProviderServiceImpl.UserRegPagesPathConfiguration;

@Component(
        service = UserRegPagesPathProvidesService.class,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = UserRegPagesPathConfiguration.class)
public class UserRegPagesPathProviderServiceImpl implements UserRegPagesPathProvidesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegPagesPathProvidesService.class);

    private String userregTermsAndConditionsPath;
    private String userregPagesBasePath;
    private String userregGatedContentPagePath;

    @Reference
    private MadisonDomainsService domainService;

    @Activate
    @Modified
    protected void Activate(final UserRegPagesPathConfiguration userRegPagesPathConfiguration) {
        userregTermsAndConditionsPath = userRegPagesPathConfiguration.madison_userreg_tnc_page_path();
        userregPagesBasePath = userRegPagesPathConfiguration.madison_userreg_pages_base_path();
        userregGatedContentPagePath = userRegPagesPathConfiguration.madison_userreg_gated_content_page_path();
        LOGGER.debug("UserRegRestService Activate() UserReg Pages Base Path : {}", userregPagesBasePath);
        LOGGER.debug("UserRegRestService Activate() UserReg Terms and Conditions Acceptance Page Path : {}",
                userregTermsAndConditionsPath);
        LOGGER.debug("UserRegRestService Activate() UserReg Gated Content Page Path : {}", userregGatedContentPagePath);
    }

    @ObjectClassDefinition(name = "PwC Viewpoint Core UserReg Pages Path Configuration")
    public @interface UserRegPagesPathConfiguration {

        @AttributeDefinition(
                name = "UserReg Pages Base Path",
                description = "UserReg base path where User registration pages exist")
        String madison_userreg_pages_base_path();

        @AttributeDefinition(
                name = "UserReg Terms and Conditions Acceptance Page Path",
                description = "UserReg page path where the user is redirected to accept terms and condition")
        String madison_userreg_tnc_page_path();

        @AttributeDefinition(
                name = "UserReg Gated Content Page Path",
                description = "UserReg page path where the user is redirected when the user is not logged in and trying to access authorized content")
        String madison_userreg_gated_content_page_path();

    }

    @Override
    public String getTermsAndConditionPagePath() {
        return domainService.getDefaultDomain() + userregTermsAndConditionsPath;
    }

    @Override
    public String getBaseUserregPath() {
        return userregPagesBasePath;
    }

    @Override
    public String getGatedContentpagePath() {
        return domainService.getDefaultDomain() + userregGatedContentPagePath;
    }

}
