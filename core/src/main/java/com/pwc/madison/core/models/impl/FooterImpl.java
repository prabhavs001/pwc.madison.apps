/*
 * Model class for populating the authorable footer component fields.
 */
package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.xss.XSSAPI;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Footer;
import com.pwc.madison.core.models.FooterLink;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.services.UserRegPagesPathProvidesService;
import com.pwc.madison.core.util.MadisonUtil;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { Footer.class },
        resourceType = { FooterImpl.RESOURCE_TYPE },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)

@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class FooterImpl implements Footer {

    protected static final String RESOURCE_TYPE = "pwc-madison/components/structure/footer";
    private static final String ANALYTICS_HEADER_COMPONENT_NAME = "footer";

    private List<FooterLink> secondRowLinks= new ArrayList<>();
    //Adding the second row for the footer component

    private String copyRight;

    private int publishingYear = 2019;

    private String pageLocale;

    private Resource footerResource;

    @ScriptVariable
    private ResourceResolver resolver;

    @OSGiService
    private UserRegPagesPathProvidesService userRegPagesPathProvidesService;
    
    @OSGiService
    private CountryTerritoryMapperService countryTerritoryMapperService;
    
    @OSGiService
    private XSSAPI xssapi;

    @Self
    private SlingHttpServletRequest request;

    private String tutorialLinkText;

    @ScriptVariable
    private Page currentPage;

    private List<FooterLink> footerLinks = new ArrayList<>();
    private String pageTerritoryCode;
    private String siteName = " PwC.";
    private String cookieEnabled;

    @PostConstruct
    protected void init() {

        String pagePath = currentPage.getPath();
        if(pagePath.startsWith(userRegPagesPathProvidesService.getBaseUserregPath())) {
            pageTerritoryCode = request.getParameter(MadisonConstants.TERRITORY_QUERY_PARAM);
            pageLocale = request.getParameter(MadisonConstants.LOCALE_QUERY_PARAM);
        } else if(pagePath.matches(MadisonConstants.MADISON_PAGE_HIERARCHY)){
            pageTerritoryCode = MadisonUtil.getTerritoryCodeFromPagePath(currentPage.getPath());
            pageLocale = MadisonUtil.getLocaleForPath(pagePath);
        } else {
        	pageLocale = countryTerritoryMapperService.getDefaultLocale();
            pageTerritoryCode = countryTerritoryMapperService.getDefaultTerritoryCode();
        }
        
        final Territory territory = countryTerritoryMapperService.getTerritoryByTerritoryCode(pageTerritoryCode);
        if(territory != null) {
        	cookieEnabled = territory.getCookieEnabled();	
        }
        
        setFooterResource();
        if(Objects.nonNull(footerResource)) {
            ValueMap footerValueMap = footerResource.getValueMap();
            copyRight = footerValueMap.get(COPYRIGHT_PROPERTY_NAME, String.class);
            if(footerValueMap.get(PUBLISH_YEAR_PROPERTY_NAME, String.class) != null){
                publishingYear = footerValueMap.get(PUBLISH_YEAR_PROPERTY_NAME,Integer.class);
            }
            Resource footerMenuLinksResource = footerResource.getChild(FOOTER_MENU_LINK_PROPERTY_NAME);
            if (footerMenuLinksResource != null) {
                Iterator<Resource> iterator = footerMenuLinksResource.listChildren();
                while (iterator.hasNext()) {
                    FooterLink footerLinkItem;
                    Resource value = iterator.next();
                    if (value != null && value.adaptTo(FooterLink.class) != null) {
                        footerLinkItem = Objects.requireNonNull(value.adaptTo(FooterLink.class));
                        footerLinks.add(footerLinkItem);
                    }
                }
            }
            //Initializing Array for second row of the footer component
            Resource footerSecondRowLinks = footerResource.getChild(FOOTER_SECOND_ROW_LINK_PROPERTY_NAME);
            if (footerSecondRowLinks != null){
                Iterator<Resource> iterator = footerSecondRowLinks.listChildren();
                while(iterator.hasNext()){
                    FooterLink eachitem;
                    Resource eachvalue = iterator.next();
                    if(eachvalue != null && eachvalue.adaptTo(FooterLink.class) != null){
                        eachitem = Objects.requireNonNull(eachvalue.adaptTo(FooterLink.class));
                        secondRowLinks.add(eachitem);
                    }
                }
            }
            //Initializing Array for second row of the footer component

            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (publishingYear < currentYear) {
                copyRight = String.valueOf(publishingYear + " - " + currentYear) + siteName + copyRight;
            } else {
                copyRight = String.valueOf(publishingYear) + siteName + copyRight;
            }

            tutorialLinkText = footerValueMap.get(TUTORIAL_LINK_TEXT_PROPERTY_NAME, "");
        }

    }

    private void setFooterResource() {
        footerResource = MadisonUtil.getConfiguredResource(currentPage, MadisonConstants.FOOTER_RELATIVE_PATH_FROM_PAGE, pageLocale, resolver, xssapi);
    }

    @Override
    public List<FooterLink> getFooterLinks() {
        return footerLinks;
    }

    @Override
    public String getCopyRight() {
        return copyRight;
    }

    @Override
    public String getCurrentPageTerritory() {
        return pageTerritoryCode;
    }

    //Getting the footer link array for the second row
    @Override
    public List<FooterLink> getSecondRowLinks(){return secondRowLinks;}

	@Override
	public String getCookieEnabled() {
		return cookieEnabled;
	}

    @Override
    public String getTutorialLinkText() {
        return tutorialLinkText;
    }

    @Override
    public String getComponentName() {
        return ANALYTICS_HEADER_COMPONENT_NAME;
    }

}
