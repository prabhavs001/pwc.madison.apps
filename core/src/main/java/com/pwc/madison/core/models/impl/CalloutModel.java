package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.fmdita.custom.common.LinkUtils;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.pwc.madison.core.authorization.enums.AudienceType;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.models.Item;
import com.pwc.madison.core.models.Territory;
import com.pwc.madison.core.services.BodyCalloutService;
import com.pwc.madison.core.services.CountryTerritoryMapperService;
import com.pwc.madison.core.util.DITALinkUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.Session;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.pwc.madison.core.constants.MadisonConstants.*;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
          extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class CalloutModel {

	public static final String DITA = "dita";
	private static final String DITA_DATE_FORMAT_YYYY_MM_DD = "yyyy-mm-dd";

	private static final Logger LOGGER = LoggerFactory.getLogger(CalloutModel.class);
	
    private static final String CALLOUT_DATE_DISPLAY_FORMAT_DD_MMM_YYYY = "dd MMM yyyy";
	public static final String HTML = "html";
	public static final String CALLOUT_INPUT_DATE_FORMAT = "yyyy-MM-dd";
	public static final String EXTERNAL_LINK = "external";
	public static final String INTERNAL_LINK = "internal";
	public static final String PDF = "PDF";
	public static final String BODY_CALLOUT_I18N_PREFIX = "Body_Callout_";
	public static final int ZERO_INDEX = 0;
	public static final int FIRST_INDEX = 1;

	@SlingObject
    private Resource resource;
    
    @ChildResource(name = "pwc-callout-dates")
    private Resource datesRes;

    @ChildResource(name = "pwc-callout-title")
    private Resource titleRes;

    @ChildResource(name = "pwc-callout-desc")
    private Resource descRes;

    @ChildResource(name = "pwc-xref")
    private Resource linkRes;
    
    @ChildResource(name = "pwc-callout-ednote")
    private Resource edNoteRes;
    
    @ChildResource(name = "pwc-callout-image")
    private Resource imageRes;

	@ChildResource(name = "pwc-callout-image/alt")
	private Resource altRes;
    
    @ValueMapValue
	@Default(values="")
    private String category;

    @ValueMapValue
	@Default(values="")
    private String contentId;
    
    @ValueMapValue
	@Default(values="dita")
    private String format;

    @ValueMapValue
	@Default(values="internal")
    private String scope;
    
    private String publicationDate;
    private String revisionDate;
    private String title;
    private String description;
    private String link = "";
    private String edNote	;
    private String image;
	private String altText;
	private String contentFieldValue;
	private String country;
	private String pageLink = "";
	private String calloutAccessLevel = "";
	private String privateGroupType = "";

	private CalloutDetailedContent calloutDetailedContent;

	@OSGiService
	private BodyCalloutService bodyCalloutService;

	@OSGiService
	private CountryTerritoryMapperService countryTerritoryMapperService;
    
	@PostConstruct
	protected void init() {
		PageManager pageManager;
		Page page;
		ValueMap pageProperties = null;

		if(null != linkRes) {
			ValueMap valueMap = linkRes.getValueMap();
			String scope = valueMap.getOrDefault(PROPERTY_SCOPE, StringUtils.EMPTY).toString();
			if(format.equalsIgnoreCase(DITA)) {
				String fmGuid = valueMap.getOrDefault(DITAConstants.FMGUID, StringUtils.EMPTY).toString();
				LOGGER.debug("fmguid {}", fmGuid);
				ResourceResolver resourceResolver = resource.getResourceResolver();
				try {
					String updatedXrefLink = LinkUtils.getUpdatedXrefLink(resourceResolver.adaptTo(Session.class), linkRes.adaptTo(Node.class), valueMap.getOrDefault(PROPERTY_LINK, StringUtils.EMPTY).toString());
					String refGeneratedPagePath = StringUtils.isNotEmpty(fmGuid) ? updatedXrefLink : StringUtils.EMPTY;
					link = StringUtils.isEmpty(refGeneratedPagePath) ? valueMap.getOrDefault(PROPERTY_LINK, StringUtils.EMPTY).toString() : refGeneratedPagePath;
					// adds HTML extension to the URL, to neutralize all url
					if(StringUtils.isNotEmpty(link) && !link.contains(HTML_EXTN)){
						if(link.split(HASH).length>1) {
							String subTopic = link.split(HASH)[FIRST_INDEX];
							link = link.split(HASH)[ZERO_INDEX].concat(HTML_EXTN);
							link = link.concat(HASH).concat(subTopic);
						}else{
							link = link.split(HASH)[ZERO_INDEX].concat(HTML_EXTN);
						}
					}
					if(isSubTopic(link)){
						pageLink = link.split(HASH)[ZERO_INDEX];
						pageLink = pageLink.split(HTML_EXTN)[ZERO_INDEX];
					}else{
						pageLink = link.split(HTML_EXTN)[ZERO_INDEX];
					}

				}catch (NoSuchElementException exception){
					LOGGER.error("CalloutModel :: getPageFromXrefDita() method returned NoSuchElementException exception {}", exception);
				}
			}else if(format.equalsIgnoreCase(PDF) && scope.equalsIgnoreCase(INTERNAL_LINK)){
				link = valueMap.getOrDefault(PROPERTY_UUID, StringUtils.EMPTY).toString();
			}else if(format.equalsIgnoreCase(HTML) || format.equalsIgnoreCase(PDF)){
				link = valueMap.getOrDefault(LINK, StringUtils.EMPTY).toString();
			}else{
				pageLink = link;
			}
		}

		if(link != null  && StringUtils.isNotBlank(link) && (scope.equalsIgnoreCase(INTERNAL_LINK) && format.equalsIgnoreCase(DITA))) {

			LOGGER.debug("Page link is {}", pageLink);
			if(null != resource && null != resource.getResourceResolver() && null != resource.getResourceResolver().getResource(pageLink)) {
				page = resource.getResourceResolver().getResource(pageLink).adaptTo(Page.class);
				pageProperties = null != page ? page.getProperties() : null;
			} else {
				LOGGER.debug("Could not get page properties for the link {}", link);
			}
		}

		Resource ditaContentResource = null;
		if(link!=null && StringUtils.isNotBlank(link) && (scope.equalsIgnoreCase(INTERNAL_LINK) && format.equalsIgnoreCase(DITA))) {
			Resource res = resource.getResourceResolver().getResource(pageLink + DITAConstants.JCR_CONTENT);
			if (null != res && !ResourceUtil.isNonExistingResource(res)) {
				ditaContentResource = res;
			}
		}
		setDates(ditaContentResource);
		
		if(null != imageRes) {
			ValueMap valueMap = imageRes.getValueMap();
			image  = valueMap.getOrDefault(PROPERTY_IMAGE_PATH, StringUtils.EMPTY).toString();

		}

		if(null != altRes){
			ValueMap valueMap = altRes.getValueMap();
			altText = valueMap.getOrDefault(DITAConstants.DITA_TAG_ALT_TEXT, StringUtils.EMPTY).toString();
		}

		if(null != titleRes) {
			ValueMap valueMap = titleRes.getValueMap();
			title  = valueMap.getOrDefault(MadisonConstants.TITLE_NODE, StringUtils.EMPTY).toString();
			if(StringUtils.isBlank(title) && null != ditaContentResource && null != pageProperties) {
				title = (pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, StringUtils.EMPTY).isEmpty()
					? pageProperties.get(JcrConstants.JCR_TITLE, String.class)
					: pageProperties.get(MadisonConstants.PWC_PAGE_TITLE, String.class));
			}
		}
		if(null != descRes) {
			ValueMap valueMap = descRes.getValueMap();
			description  = valueMap.getOrDefault(DITAConstants.DESCRIPTION, StringUtils.EMPTY).toString();
			if(StringUtils.isBlank(description) && null != ditaContentResource && null != pageProperties) {
				description = pageProperties.get(JcrConstants.JCR_DESCRIPTION, StringUtils.EMPTY);
			}
		}
		if(null != edNoteRes) {
			ValueMap valueMap = edNoteRes.getValueMap();
			edNote  = valueMap.getOrDefault(PROPERTY_ED_NOTE, StringUtils.EMPTY).toString();
		}

		if(format != null && (link!=null && !link.isEmpty()) && scope.equals(INTERNAL_LINK) && (format.equalsIgnoreCase(DITA) || format.equalsIgnoreCase(HTML))){
			ResourceResolver resourceResolver = resource.getResourceResolver();
			Resource linkedPageResource = resourceResolver.getResource(pageLink);
			if (linkedPageResource != null) {
				setContentFieldValue(linkedPageResource, format);
			}
		}

		if(link != null  && StringUtils.isNotBlank(link) && (scope.equalsIgnoreCase(INTERNAL_LINK) && format.equalsIgnoreCase(DITA))){
			// setting country field
			String calloutCountry = getCountry();
			country = StringUtils.isEmpty(calloutCountry) ? StringUtils.EMPTY : calloutCountry;
		}

		if((contentId != null || StringUtils.isBlank(contentId)) && pageProperties!=null) {
			contentId = pageProperties.getOrDefault(MadisonConstants.PWC_CONTENT_ID, StringUtils.EMPTY).toString();
		}

		if((calloutAccessLevel != null || StringUtils.isBlank(calloutAccessLevel)) && pageProperties!=null) {
			calloutAccessLevel = pageProperties.getOrDefault(DITAConstants.META_AUDIENCE, StringUtils.EMPTY).toString();
			if(calloutAccessLevel.equalsIgnoreCase(AudienceType.PRIVATE_GROUP.getValue())){
				String groupType = Arrays.asList((String[])pageProperties.getOrDefault(DITAConstants.META_PRIVATE_GROUP, StringUtils.EMPTY)).get(ZERO_INDEX);
				if(StringUtils.isNotEmpty(groupType)) {
					privateGroupType = BODY_CALLOUT_I18N_PREFIX +groupType;
				}
			}
		}
	}

	private void setDates(Resource ditaContentResource) {
		if(null != datesRes) {
			ValueMap valueMap = datesRes.getValueMap();
			publicationDate  = valueMap.getOrDefault(PROPERTY_PUBLICATION_DATE, StringUtils.EMPTY).toString();
			revisionDate  = valueMap.getOrDefault(PROPERTY_REVISION_DATE, StringUtils.EMPTY).toString();
			DateFormat dateFormat = new SimpleDateFormat(CALLOUT_DATE_DISPLAY_FORMAT_DD_MMM_YYYY);
			if(scope.equalsIgnoreCase(INTERNAL_LINK) && ditaContentResource!=null) {
				PageManager pageManager = ditaContentResource.getResourceResolver().adaptTo(PageManager.class);
				Page containingPage = pageManager.getContainingPage(resource);
				String territoryCode = MadisonUtil.getTerritoryCodeFromPagePath(containingPage.getPath());
				Territory territory = countryTerritoryMapperService.getTerritoryByTerritoryCode(territoryCode);
				final String territoryLowerCase = territory != null ? territory.getTerritoryCode().toLowerCase() : StringUtils.EMPTY;
				dateFormat = new SimpleDateFormat(MadisonUtil.fetchDateFormat(territoryLowerCase, countryTerritoryMapperService, CALLOUT_DATE_DISPLAY_FORMAT_DD_MMM_YYYY));
			}
			try {
				if(StringUtils.isNotBlank(publicationDate)) {
					publicationDate = dateFormat.format(new SimpleDateFormat(CALLOUT_INPUT_DATE_FORMAT).parse(publicationDate));
				} else if(null != ditaContentResource) {
					Date date = ditaContentResource.getValueMap().get(MadisonConstants.PWC_PUBLICATION_DATE, Date.class);
					publicationDate  = date!=null ? dateFormat.format(date) : StringUtils.EMPTY;
				}
				if(StringUtils.isNotBlank(revisionDate)) {
					revisionDate = dateFormat.format(new SimpleDateFormat(CALLOUT_INPUT_DATE_FORMAT).parse(revisionDate));
				} else if(null != ditaContentResource) {
					Date date = ditaContentResource.getValueMap().get(MadisonConstants.PWC_REVISED_DATE, Date.class);
					revisionDate  = date!=null ? dateFormat.format(date) : StringUtils.EMPTY;
				}
			} catch (ParseException e) {
				LOGGER.error("ParseException occured in CalloutModel {}", e.getMessage());
			}
		}
	}
	
	public String getCategory() {
		return category;
	}

	public String getContentId() {
		return contentId;
	}
	
	public String getFormat() {
		return format;
	}

	public String getScope() {
		return scope;
	}
	
	public String getPublicationDate() {
		return publicationDate;
	}

	public String getRevisionDate() {
		return revisionDate;
	}
    
	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}
    
	public String getLink() {
		return link;
	}
	
	public String getEdNote() {
		return edNote;
	}
	
	public String getImage() {
		return image;
	}

	public String getAltText() {
		return altText;
	}

	public CalloutDetailedContent getCalloutDetailedContent() {
		return calloutDetailedContent;
	}

	public void setCalloutDetailedContent(CalloutDetailedContent calloutDetailedContent) {
		this.calloutDetailedContent = calloutDetailedContent;
	}

	public String getContentFieldValue() {
		return contentFieldValue;
	}

	private void setContentFieldValue(Resource linkRes, String format) {
		Item item = bodyCalloutService.getPageMetadata(linkRes, format);
		if(item != null) {
			this.contentFieldValue = bodyCalloutService.getContentFieldValue(contentId, item.getStandardSetterType(), Item.getPwcSourceValue(), item.getContentType());
		}
	}
	public void setTranslatedContentFieldValue(String translatedText) {
		this.contentFieldValue = translatedText;
	}

	public String getCountry() {
		return country = MadisonUtil.getTerritoryCodeFromPagePath(pageLink).toUpperCase();
	}

	// returns True if the link is a subtopic link
	public boolean isSubTopic(String link){
		return link.contains(HASH) ? Boolean.TRUE : Boolean.FALSE;
	}

	public String getCalloutAccessLevel() {
		return calloutAccessLevel;
	}

	public String getPrivateGroupType() {
		return privateGroupType;
	}

	public void setPrivateGroupType(String privateGroupType) {
		this.privateGroupType = privateGroupType;
	}
}
