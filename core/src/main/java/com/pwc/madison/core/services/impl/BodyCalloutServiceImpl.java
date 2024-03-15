package com.pwc.madison.core.services.impl;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.commons.Externalizer;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.google.gson.Gson;
import com.pwc.madison.core.authorization.models.AuthorizationInformation;
import com.pwc.madison.core.authorization.services.ContentAuthorizationService;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.models.impl.CalloutDetailedContent;
import com.pwc.madison.core.models.impl.CalloutModel;
import com.pwc.madison.core.models.BodyCalloutListPojo;
import com.pwc.madison.core.models.BodyCalloutPojo;
import com.pwc.madison.core.services.BodyCalloutService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.userreg.models.User;
import com.pwc.madison.core.userreg.services.UserPreferencesProviderService;
import com.pwc.madison.core.userreg.services.UserRegRestService;
import com.pwc.madison.core.userreg.utils.UserInformationUtil;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.xss.XSSAPI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static com.pwc.madison.core.constants.MadisonConstants.HASH;
import static com.pwc.madison.core.models.impl.CalloutModel.*;

@Component(service = BodyCalloutService.class, immediate = true)
@Designate(ocd = BodyCalloutServiceImpl.BodyCalloutConfig.class)
public class BodyCalloutServiceImpl implements BodyCalloutService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BodyCalloutServiceImpl.class);
    private static final String GET = "GET";
    public static final String HTML_EXTENSION = ".html";
    public static final String PROPERTY_SCOPE = "scope";
    public static final String PROPERTY_FORMAT = "format";
    public static final String BODY_SELECTOR = "div.topic.doc-body-content";

    @Reference
    private CryptoSupport cryptoSupport;

    @Reference
    private UserRegRestService userRegRestService;

    @Reference
    private CountryTerritoryMapperService countryTerritoryMapperService;

    @Reference
    private UserPreferencesProviderService userPreferencesProviderService;

    @Reference
    private XSSAPI xssapi;

    /**
     * Service to create HTTP Servlet requests and responses
     */
    @Reference
    private RequestResponseFactory requestResponseFactory;

    /**
     * Service to process requests through Sling
     */
    @Reference
    private SlingRequestProcessor requestProcessor;
    
    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingSettingsService slingSettingsService;

    private int bodyCalloutItemsCount = 5;

    private String environment;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ContentAuthorizationService contentAuthorizationService;

    @Activate
    protected void activate(BodyCalloutConfig config) {
        int noOfCallout = config.getNoOfCallout();
        if(noOfCallout >0) {
            bodyCalloutItemsCount = noOfCallout;
        }
        environment = MadisonUtil.getCurrentRunmode(slingSettingsService);
    }

    @Override
    public String getBodyCalloutJson(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String resourcePath = request.getRequestPathInfo().getResourcePath();
        ResourceResolver resourceResolver  = MadisonUtil.getResourceResolver(resolverFactory,
                MadisonConstants.MADISON_CONTENT_ADMIN_SUB_SERVICE);
        LOGGER.debug("resolver is  {}", resourceResolver);
        Resource resource = null != resourceResolver ? resourceResolver.getResource(resourcePath) : null;
        BodyCalloutListPojo bodyCalloutListPojo = new BodyCalloutListPojo();
        try {
			if (null != resource && !ResourceUtil.isNonExistingResource(resource)) {
			    Iterator<Resource> listChildren = resource.listChildren();
			    List<BodyCalloutPojo> bodyCalloutPojoList = new ArrayList<>();
			    while (listChildren.hasNext()) {
			        Resource res = listChildren.next();
			        Resource pwc_xref = res.getChild(DITAConstants.DITA_TAG_PWC_XREF);
			        if(pwc_xref!=null) {
			            Node linkNode = pwc_xref.adaptTo(Node.class);
			            try {
			                if (linkNode.hasProperty(DITAConstants.PROPERTY_LINK)) {
                                String link = linkNode.getProperty(DITAConstants.PROPERTY_LINK).getString();
			                    CalloutModel calloutModel = res.adaptTo(CalloutModel.class);

			                    if (null != calloutModel && (calloutModel.getLink() != null && StringUtils.isNotBlank(calloutModel.getLink())) && calloutModel.getScope().equalsIgnoreCase(EXTERNAL_LINK)) {
			                        if(bodyCalloutListPojo.getSize()<bodyCalloutItemsCount) {
			                            bodyCalloutPojoList.add(new BodyCalloutPojo(calloutModel));
			                        }
			                    } else if (null != calloutModel && (calloutModel.getLink() != null && StringUtils.isNotBlank(calloutModel.getLink())) && (calloutModel.getScope().equalsIgnoreCase(INTERNAL_LINK) && !calloutModel.getFormat().equalsIgnoreCase(DITA))) {
			                        if(bodyCalloutListPojo.getSize()<bodyCalloutItemsCount) {
			                            bodyCalloutPojoList.add(new BodyCalloutPojo(calloutModel));
			                        }
			                    } else if (null != calloutModel && (calloutModel.getLink() != null && StringUtils.isNotBlank(calloutModel.getLink())) && calloutModel.getFormat().equalsIgnoreCase(DITA) && isAccessible(request, response, resourceResolver, calloutModel)) {
			                        if (StringUtils.isNotBlank(calloutModel.getLink())) {
			                            // updates the Translated value of the ContentFieldValue
			                            link = calloutModel.getLink();
			                            String cfv = calloutModel.getContentFieldValue();

			                            if (link != null && cfv != null) {
			                                PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
			                                Page currentPage = pageManager.getContainingPage(request.getResource());
			                                String pagePath = currentPage.getPath();

			                                calloutModel.setTranslatedContentFieldValue(getTranslatedText(request, pagePath, cfv));
			                            }
			                        }
			                        if(bodyCalloutListPojo.getSize()<bodyCalloutItemsCount) {
			                            bodyCalloutPojoList.add(new BodyCalloutPojo(calloutModel));
			                        }
			                    }
			                }
			            } catch (RepositoryException e) {
			                e.printStackTrace();
			            }
			        }
			        if(bodyCalloutListPojo.getSize()<bodyCalloutItemsCount){
			        bodyCalloutListPojo.setBodyCalloutItems(bodyCalloutPojoList);}
			    }
			}
		} catch (Exception e) {
			LOGGER.error("Exception occured in getBodyCalloutJson method  {}", e);
		} finally {
			if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
		}
        Gson gson = new Gson();
        return gson.toJson(bodyCalloutListPojo);


    }

    private boolean isAccessible(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                 ResourceResolver resourceResolver, CalloutModel calloutModel) {
        boolean isAccessibleToCurrentUser = false;
        if(contentAuthorizationService != null && environment.equals(Externalizer.AUTHOR)){
            isAccessibleToCurrentUser = true;
        }else if(contentAuthorizationService != null && environment.equals(Externalizer.PUBLISH)){
            String link = calloutModel.getLink();
            if (isSubTopic(link)) {
                link = link.split(HASH)[0];
            }
            link = link.split(HTML_EXTENSION)[0];
            LOGGER.debug("isAccessible() method - page link :: {}", link);
            Resource referedResource = resourceResolver.getResource(link + DITAConstants.JCR_CONTENT);
            if (null != referedResource && !ResourceUtil.isNonExistingResource(referedResource)) {
                User user = UserInformationUtil.getUser(request, false, userRegRestService, cryptoSupport, response, true,
                        countryTerritoryMapperService, userPreferencesProviderService, false, false, xssapi);
                LOGGER.debug("isAccessible() method - UserProfile Details :: {}", Objects.isNull(user));
                Page linkedPage = resourceResolver.getResource(link).adaptTo(Page.class);
                AuthorizationInformation pageAuthorizationInformation = contentAuthorizationService.getUserAuthorization(linkedPage, user);
                isAccessibleToCurrentUser = pageAuthorizationInformation.isAuthorized();
                LOGGER.debug("isAccessible() method - isAccessibleToCurrentUser :: {}", isAccessibleToCurrentUser);
            }
        }else if(contentAuthorizationService == null && environment.equals(Externalizer.AUTHOR)){
            isAccessibleToCurrentUser = true;
        }else if(contentAuthorizationService == null && environment.equals(Externalizer.PUBLISH)){
            LOGGER.debug("sAccessible() method - ContentAuthorizationService is null");
        }else{
            LOGGER.info("isAccessible() method - {} Environment not supported", environment);
        }
        return isAccessibleToCurrentUser;
    }

    private void getPageDetailedContent(SlingHttpServletRequest request, CalloutModel calloutModel) {
        String pageUrl = calloutModel.getLink();
        boolean isSubTopic = isSubTopic(pageUrl);
        String subTopicSelector = StringUtils.EMPTY;
        if (isSubTopic) {
            String pageLink = pageUrl.split(HASH)[0] + DITAConstants.HTML_EXT;
            subTopicSelector = pageUrl.split(HASH)[1];
            pageUrl = pageLink;
        } else {
            pageUrl += HTML_EXTENSION;
        }
        CalloutDetailedContent calloutDetailedContent = new CalloutDetailedContent();
        String pageContent = null;
        pageContent = getLinkedPageContent(request, pageUrl);
        LOGGER.debug("getPageDetailedContent() method - pageContent empty check :: {}", StringUtils.isEmpty(pageContent));
        if(pageContent!=null){
            final Document doc = Jsoup.parseBodyFragment(pageContent, MadisonConstants.UTF_8);
            if (isSubTopic && !StringUtils.isBlank(subTopicSelector)) {
                Element bodyContentElement = doc.getElementById(subTopicSelector);
                calloutDetailedContent.setPageContent(bodyContentElement.toString());
            } else {
                Elements bodyContentElements = doc.select(BODY_SELECTOR);
                calloutDetailedContent.setPageContent(bodyContentElements.toString());
            }
        }
        calloutModel.setCalloutDetailedContent(calloutDetailedContent);
    }

    private String getLinkedPageContent(SlingHttpServletRequest request, String pageUrl) {
        /* Adding PageContent selector */
        String pageUrlWithSelector = pageUrl.split(MadisonConstants.HTML_EXTN)[0]+MadisonConstants.PAGE_CONTENT_EXTN;

        /* Setup request */
        HttpServletRequest req = requestResponseFactory.createRequest(GET, pageUrlWithSelector);
        WCMMode.DISABLED.toRequest(req);

        /* Setup response */
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse resp = requestResponseFactory.createResponse(out);

        /* Process request through Sling */
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            LOGGER.debug("getLinkedPageContent() method - getResource :: {}",resourceResolver.getResource(pageUrl.split(HTML_EXTENSION)[0]));
            requestProcessor.processRequest(req, resp, resourceResolver);
        } catch (ServletException e) {
            LOGGER.error("ServletException in getLinkedPageContent() method {}", e);
        } catch (IOException e) {
            LOGGER.error("IOException in getLinkedPageContent() method {}", e);
        }
        return out.toString();
    }

    /**
     * Method used to get Metadata/Properties of a resource
     *
     * @return item {@link Item}
     */
    public Item getPageMetadata(Resource pageResource, String format) {
        Item item = null;
        if (pageResource != null && pageResource instanceof Resource) {
            ValueMap pageProperties = null;
            Page pageItem = pageResource.adaptTo(Page.class);
            pageProperties = pageItem.getProperties();
            String standardSetter = pageProperties.get(DITAConstants.META_STANDARD_SETTERS, StringUtils.EMPTY);
            String contentType = pageProperties.get(DITAConstants.META_CONTENT_TYPE, StringUtils.EMPTY);
            item = new Item();
            item.setStandardSetterType(standardSetter);
            item.setContentType(contentType);
        }
        return item;
    }

    /**
     * Method used to take decision for contentField Value
     *
     * @return contentFieldValue {@link String}
     */
    public String getContentFieldValue(String contentId, String standardSetter, String PwcAuthored, String
            contentType) {
        String contentFieldValue = StringUtils.EMPTY;
        if (contentId.isEmpty()) {
            if (standardSetter.equalsIgnoreCase(PwcAuthored))
                contentFieldValue = contentType;
            else
                contentFieldValue = standardSetter;
        } else
            contentFieldValue = contentId;
        return contentFieldValue;
    }

    /**
     * Method used to translate the text
     *
     * @return contentFieldValue {@link String}
     */
    private String getTranslatedText(SlingHttpServletRequest request, String pagePath, String text) {
        Locale locale = new Locale(MadisonUtil.getLocaleForPath(pagePath));
        ResourceBundle resourceBundle = request.getResourceBundle(locale);
        I18n i18n = new I18n(resourceBundle);
        String translatedText = i18n.get(text);
        translatedText = translatedText != null && !translatedText.isEmpty() ? translatedText : "";
        return translatedText;
    }

    // returns True if the link is a subtopic link
    public boolean isSubTopic(String link) {
        return link.contains(HASH) ? Boolean.TRUE : Boolean.FALSE;
    }

    @ObjectClassDefinition(
            name = "PwC Viewpoint BodyCallout Configuration")
    public @interface BodyCalloutConfig {

        @AttributeDefinition(name = "Max Items to be displayed",
                description = "This configuration allows to provide number of callout items to show in Callout Block",
                type = AttributeType.INTEGER,
                defaultValue = "5",
                cardinality = Integer.MAX_VALUE)
        int getNoOfCallout();
    }
}
